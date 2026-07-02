import Foundation
import SQLite3

/// Reads novels and chapters from a bundled SQLite database (converted from the
/// original PostgreSQL content dump). Fully offline — no network access.
///
/// The database file `asterion-content.sqlite` must be added to the app target
/// as a bundled resource. It is opened read-only.
actor LocalContentStore {
    static let shared = LocalContentStore()

    enum StoreError: LocalizedError {
        case databaseNotFound
        case openFailed(String)
        case notFound

        var errorDescription: String? {
            switch self {
            case .databaseNotFound:
                return "Content database (asterion-content.sqlite) was not found in the app bundle."
            case .openFailed(let message):
                return "Failed to open content database: \(message)"
            case .notFound:
                return "The requested item was not found in the local content database."
            }
        }
    }

    // SQLite wants a destructor sentinel for transient bound strings.
    private static let SQLITE_TRANSIENT = unsafeBitCast(-1, to: sqlite3_destructor_type.self)

    private var db: OpaquePointer?

    // MARK: - Connection

    private func connection() throws -> OpaquePointer {
        if let db { return db }
        guard let url = Bundle.main.url(forResource: "asterion-content", withExtension: "sqlite") else {
            throw StoreError.databaseNotFound
        }
        var handle: OpaquePointer?
        let flags = SQLITE_OPEN_READONLY | SQLITE_OPEN_NOMUTEX
        let result = sqlite3_open_v2(url.path, &handle, flags, nil)
        guard result == SQLITE_OK, let handle else {
            let message = handle.map { String(cString: sqlite3_errmsg($0)) } ?? "code \(result)"
            if let handle { sqlite3_close(handle) }
            throw StoreError.openFailed(message)
        }
        db = handle
        return handle
    }

    // MARK: - Public API (mirrors APIClient content methods)

    func fetchNovels(limit: Int, offset: Int, search: String) throws -> [Novel] {
        let db = try connection()
        let trimmed = search.trimmingCharacters(in: .whitespacesAndNewlines)

        let sql: String
        if trimmed.isEmpty {
            sql = """
            SELECT \(Self.novelColumns) FROM novels
            ORDER BY (rank IS NULL), CAST(rank AS INTEGER), id
            LIMIT ? OFFSET ?
            """
        } else {
            sql = """
            SELECT \(Self.novelColumns) FROM novels
            WHERE title LIKE ? ESCAPE '\\'
            ORDER BY (rank IS NULL), CAST(rank AS INTEGER), id
            LIMIT ? OFFSET ?
            """
        }

        let stmt = try prepare(db, sql)
        defer { sqlite3_finalize(stmt) }

        var index: Int32 = 1
        if !trimmed.isEmpty {
            bindText(stmt, index, "%\(escapeLike(trimmed))%"); index += 1
        }
        sqlite3_bind_int(stmt, index, Int32(limit)); index += 1
        sqlite3_bind_int(stmt, index, Int32(offset))

        var novels: [Novel] = []
        while sqlite3_step(stmt) == SQLITE_ROW {
            novels.append(readNovel(stmt))
        }
        return novels
    }

    func fetchNovel(id: String) throws -> Novel {
        let db = try connection()
        guard let intId = Int64(id) else { throw StoreError.notFound }
        let sql = "SELECT \(Self.novelColumns) FROM novels WHERE id = ? LIMIT 1"
        let stmt = try prepare(db, sql)
        defer { sqlite3_finalize(stmt) }
        sqlite3_bind_int64(stmt, 1, intId)
        guard sqlite3_step(stmt) == SQLITE_ROW else { throw StoreError.notFound }
        return readNovel(stmt)
    }

    func fetchChapters(novelId: String, limit: Int, offset: Int) throws -> PaginatedResponse<Chapter> {
        let db = try connection()
        guard let intId = Int64(novelId) else { throw StoreError.notFound }

        // Total count for pagination metadata.
        var total = 0
        let countStmt = try prepare(db, "SELECT COUNT(*) FROM chapters WHERE novel_id = ?")
        sqlite3_bind_int64(countStmt, 1, intId)
        if sqlite3_step(countStmt) == SQLITE_ROW {
            total = Int(sqlite3_column_int64(countStmt, 0))
        }
        sqlite3_finalize(countStmt)

        // Chapter list metadata does not include content (kept light for lists).
        let sql = """
        SELECT id, chapter_number, title, url FROM chapters
        WHERE novel_id = ?
        ORDER BY chapter_number, id
        LIMIT ? OFFSET ?
        """
        let stmt = try prepare(db, sql)
        defer { sqlite3_finalize(stmt) }
        sqlite3_bind_int64(stmt, 1, intId)
        sqlite3_bind_int(stmt, 2, Int32(limit))
        sqlite3_bind_int(stmt, 3, Int32(offset))

        var chapters: [Chapter] = []
        while sqlite3_step(stmt) == SQLITE_ROW {
            let id = String(sqlite3_column_int64(stmt, 0))
            let number = Int(sqlite3_column_int64(stmt, 1))
            let title = columnText(stmt, 2) ?? ""
            let url = columnText(stmt, 3)
            chapters.append(Chapter(id: id, chapterNumber: number, title: title, content: nil, url: url))
        }

        let returned = chapters.count
        let hasNext = offset + returned < total
        let meta = PaginatedResponse<Chapter>.Meta(
            count: returned,
            total: total,
            page: nil,
            pageSize: limit,
            totalPages: limit > 0 ? Int(ceil(Double(total) / Double(limit))) : nil,
            hasNextPage: hasNext,
            hasPreviousPage: offset > 0,
            limit: limit,
            offset: offset
        )
        return PaginatedResponse(data: chapters, meta: meta)
    }

    func fetchChapter(id: String) throws -> Chapter {
        let db = try connection()
        guard let intId = Int64(id) else { throw StoreError.notFound }
        let sql = "SELECT id, chapter_number, title, url, content FROM chapters WHERE id = ? LIMIT 1"
        let stmt = try prepare(db, sql)
        defer { sqlite3_finalize(stmt) }
        sqlite3_bind_int64(stmt, 1, intId)
        guard sqlite3_step(stmt) == SQLITE_ROW else { throw StoreError.notFound }
        return Chapter(
            id: String(sqlite3_column_int64(stmt, 0)),
            chapterNumber: Int(sqlite3_column_int64(stmt, 1)),
            title: columnText(stmt, 2) ?? "",
            content: columnText(stmt, 4),
            url: columnText(stmt, 3)
        )
    }

    // MARK: - Row mapping

    private static let novelColumns =
        "id, title, author, rank, total_chapters, views, bookmarks, status, genres, summary, image_url, rating"

    private func readNovel(_ stmt: OpaquePointer?) -> Novel {
        let id = String(sqlite3_column_int64(stmt, 0))
        let title = columnText(stmt, 1) ?? ""
        let author = columnText(stmt, 2)
        let rank = columnText(stmt, 3)
        let totalChapters: String? = {
            if sqlite3_column_type(stmt, 4) == SQLITE_NULL { return nil }
            return String(sqlite3_column_int64(stmt, 4))
        }()
        let views = columnText(stmt, 5)
        let bookmarks = columnText(stmt, 6)
        let status = columnText(stmt, 7)
        let genres = parsePostgresArray(columnText(stmt, 8))
        let summary = columnText(stmt, 9)
        let imageUrl = columnText(stmt, 10)
        let rating: Double? = sqlite3_column_type(stmt, 11) == SQLITE_NULL
            ? nil
            : sqlite3_column_double(stmt, 11)

        return Novel(
            id: id,
            title: title,
            author: author,
            rank: rank,
            totalChapters: totalChapters,
            views: views,
            bookmarks: bookmarks,
            status: status,
            genres: genres,
            summary: summary,
            imageUrl: imageUrl,
            rating: rating
        )
    }

    // MARK: - SQLite helpers

    private func prepare(_ db: OpaquePointer, _ sql: String) throws -> OpaquePointer? {
        var stmt: OpaquePointer?
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else {
            throw StoreError.openFailed(String(cString: sqlite3_errmsg(db)))
        }
        return stmt
    }

    private func bindText(_ stmt: OpaquePointer?, _ index: Int32, _ value: String) {
        sqlite3_bind_text(stmt, index, value, -1, Self.SQLITE_TRANSIENT)
    }

    private func columnText(_ stmt: OpaquePointer?, _ column: Int32) -> String? {
        guard sqlite3_column_type(stmt, column) != SQLITE_NULL,
              let cString = sqlite3_column_text(stmt, column) else { return nil }
        return String(cString: cString)
    }

    private func escapeLike(_ value: String) -> String {
        value
            .replacingOccurrences(of: "\\", with: "\\\\")
            .replacingOccurrences(of: "%", with: "\\%")
            .replacingOccurrences(of: "_", with: "\\_")
    }

    // MARK: - Postgres array parsing

    /// Parses a Postgres text-array literal, e.g. `{Comedy,Drama,"Slice of Life"}`.
    private func parsePostgresArray(_ raw: String?) -> [String]? {
        guard var s = raw else { return nil }
        s = s.trimmingCharacters(in: .whitespaces)
        guard s.hasPrefix("{"), s.hasSuffix("}") else {
            return s.isEmpty ? [] : [s]
        }
        s.removeFirst()
        s.removeLast()
        if s.isEmpty { return [] }

        var result: [String] = []
        var current = ""
        var inQuotes = false
        var escaped = false
        var iterator = s.makeIterator()

        while let ch = iterator.next() {
            if escaped {
                current.append(ch)
                escaped = false
                continue
            }
            switch ch {
            case "\\":
                escaped = true
            case "\"":
                inQuotes.toggle()
            case "," where !inQuotes:
                result.append(current.trimmingCharacters(in: .whitespaces))
                current = ""
            default:
                current.append(ch)
            }
        }
        result.append(current.trimmingCharacters(in: .whitespaces))
        return result.filter { !$0.isEmpty }
    }
}
