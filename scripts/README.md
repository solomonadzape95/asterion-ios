# Asterion scripts

## Offline content database

The app reads all novels and chapters from a local SQLite database,
`asterion-content.sqlite`, bundled into the app target. That file is **not**
committed (it is ~1.9 GB and gitignored). Regenerate it from the original
PostgreSQL dump:

```bash
python3 scripts/pgdump_to_sqlite.py /path/to/asterion-content.sql asterion-content.sqlite
```

This streams the `novels` and `chapters` COPY blocks out of the dump, decodes
Postgres text-format escaping, and writes a SQLite database with an index on
`chapters(novel_id, chapter_number)`.

After generating it, make sure `asterion-content.sqlite` sits at the repo root
so the Xcode "Resources" build phase can bundle it into `Asterion.app`.

## Notes

- The app is a **Mac Catalyst** app, ad-hoc signed to run locally (no 7-day
  expiry). `rebuild-asterion.sh` rebuilds + redeploys it after code changes.
- Fully offline: no backend, no login. User data (library, bookmarks, reading
  progress, preferences) is stored locally on-device.
- The iOS widget and Live Activities were removed for the Mac build.
- Content source: `LocalContentStore.swift`. User data: `LocalUserStore.swift`.
