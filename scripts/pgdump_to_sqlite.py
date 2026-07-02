#!/usr/bin/env python3
"""
Stream a PostgreSQL pg_dump (plain text) into a SQLite database.

Only the `novels` and `chapters` COPY blocks are extracted. Postgres COPY
text-format escaping is decoded (\\N -> NULL, \\t \\n \\r \\\\ etc.).

Usage: pgdump_to_sqlite.py <input.sql> <output.sqlite>
"""
import sys
import sqlite3

# Postgres COPY text-format escape decoding.
_ESCAPES = {
    "\\": "\\",
    "t": "\t",
    "n": "\n",
    "r": "\r",
    "b": "\b",
    "f": "\f",
    "v": "\v",
}


def unescape(field: str):
    if field == "\\N":
        return None  # SQL NULL
    if "\\" not in field:
        return field
    out = []
    it = iter(range(len(field)))
    i = 0
    n = len(field)
    while i < n:
        c = field[i]
        if c == "\\" and i + 1 < n:
            nxt = field[i + 1]
            out.append(_ESCAPES.get(nxt, nxt))
            i += 2
        else:
            out.append(c)
            i += 1
    return "".join(out)


def parse_columns(header_line: str):
    # e.g. COPY public.novels (id, title, ...) FROM stdin;
    inside = header_line.split("(", 1)[1].rsplit(")", 1)[0]
    return [c.strip() for c in inside.split(",")]


def main():
    src, dst = sys.argv[1], sys.argv[2]

    conn = sqlite3.connect(dst)
    cur = conn.cursor()
    cur.executescript(
        """
        PRAGMA journal_mode = OFF;
        PRAGMA synchronous = OFF;
        DROP TABLE IF EXISTS novels;
        DROP TABLE IF EXISTS chapters;
        CREATE TABLE novels (
            id INTEGER PRIMARY KEY,
            title TEXT, novel_url TEXT, author TEXT, rank TEXT,
            total_chapters INTEGER, views TEXT, bookmarks TEXT, status TEXT,
            genres TEXT, summary TEXT, chapters_url TEXT, image_url TEXT,
            rating REAL, last_scraped TEXT, created_at TEXT, updated_at TEXT
        );
        CREATE TABLE chapters (
            id INTEGER PRIMARY KEY,
            novel_id INTEGER, chapter_number INTEGER, url TEXT,
            title TEXT, content TEXT, created_at TEXT, updated_at TEXT
        );
        """
    )

    current = None        # 'novels' | 'chapters' | None
    columns = None
    placeholders = None
    batch = []
    BATCH = 2000
    counts = {"novels": 0, "chapters": 0}

    def flush():
        if not batch:
            return
        sql = f"INSERT INTO {current} ({','.join(columns)}) VALUES ({placeholders})"
        cur.executemany(sql, batch)
        batch.clear()

    with open(src, "r", encoding="utf-8", newline="\n") as f:
        for line in f:
            if current is None:
                if line.startswith("COPY public.novels "):
                    current = "novels"
                    columns = parse_columns(line)
                    placeholders = ",".join("?" * len(columns))
                elif line.startswith("COPY public.chapters "):
                    current = "chapters"
                    columns = parse_columns(line)
                    placeholders = ",".join("?" * len(columns))
                continue

            # inside a COPY block
            if line.startswith("\\.") and (line == "\\.\n" or line == "\\."):
                flush()
                conn.commit()
                current = None
                columns = None
                continue

            row = line[:-1] if line.endswith("\n") else line
            fields = row.split("\t")
            if len(fields) != len(columns):
                # Malformed line (shouldn't happen with valid COPY); skip loudly.
                sys.stderr.write(
                    f"WARN {current}: expected {len(columns)} cols, got {len(fields)}\n"
                )
                continue
            batch.append(tuple(unescape(x) for x in fields))
            counts[current] += 1
            if len(batch) >= BATCH:
                flush()
                if counts[current] % 50000 == 0:
                    print(f"  {current}: {counts[current]:,} rows", flush=True)

    conn.commit()
    print("Building indexes...", flush=True)
    cur.executescript(
        """
        CREATE INDEX IF NOT EXISTS idx_chapters_novel ON chapters(novel_id, chapter_number);
        """
    )
    conn.commit()
    conn.execute("PRAGMA optimize;")
    conn.close()
    print(f"DONE. novels={counts['novels']:,} chapters={counts['chapters']:,}", flush=True)


if __name__ == "__main__":
    main()
