import Foundation

/// Local, offline replacement for the former user-data backend + Clerk auth.
/// Persists library, bookmarks, reading progress, reading history, preferences,
/// and a lightweight profile to a JSON file in Application Support.
///
/// All values are returned as the same types the network `APIClient` used to
/// return, so the call sites in `APIClient` (and therefore the views) are
/// unchanged.
actor LocalUserStore {
    static let shared = LocalUserStore()

    /// Stable id for the single local reader.
    static let localUserId = "local-reader"

    private struct State: Codable {
        var profileUsername: String? = "Reader"
        var profileEmail: String? = nil
        var profileAvatarUrl: String? = nil

        var readingGoal: Int = 10
        var darkMode: Bool = true
        var notificationsOn: Bool = false
        var fontSizePref: String = "medium"

        /// novelId -> date added
        var library: [String: Date] = [:]
        var bookmarks: [AsterionBookmark] = []
        /// novelId -> latest progress
        var progress: [String: ReadingProgress] = [:]
        var history: [AsterionReadingHistoryEntry] = []

        let createdAt: Date
    }

    private let fileManager = FileManager.default
    private let decoder = JSONDecoder()
    private let encoder = JSONEncoder()
    private var cached: State?

    // MARK: - Persistence

    private var fileURL: URL {
        let base = fileManager.urls(for: .applicationSupportDirectory, in: .userDomainMask).first
            ?? fileManager.urls(for: .documentDirectory, in: .userDomainMask).first!
        let dir = base.appendingPathComponent("asterion_user", isDirectory: true)
        if !fileManager.fileExists(atPath: dir.path) {
            try? fileManager.createDirectory(at: dir, withIntermediateDirectories: true)
        }
        return dir.appendingPathComponent("user_state.json")
    }

    private func state() -> State {
        if let cached { return cached }
        if let data = try? Data(contentsOf: fileURL),
           let decoded = try? decoder.decode(State.self, from: data) {
            cached = decoded
            return decoded
        }
        let fresh = State(createdAt: Date())
        cached = fresh
        persist(fresh)
        return fresh
    }

    private func mutate(_ block: (inout State) -> Void) {
        var current = state()
        block(&current)
        cached = current
        persist(current)
    }

    private func persist(_ state: State) {
        if let data = try? encoder.encode(state) {
            try? data.write(to: fileURL, options: .atomic)
        }
    }

    // MARK: - Profile

    func profile() -> AsterionUserProfile {
        let s = state()
        return AsterionUserProfile(
            id: Self.localUserId,
            clerkUserId: Self.localUserId,
            email: s.profileEmail,
            username: s.profileUsername,
            avatarUrl: s.profileAvatarUrl,
            createdAt: s.createdAt,
            updatedAt: Date()
        )
    }

    func updateProfile(email: String?, username: String?, avatarUrl: String?) -> AsterionUserProfile {
        mutate { s in
            if let email { s.profileEmail = email }
            if let username { s.profileUsername = username }
            if let avatarUrl { s.profileAvatarUrl = avatarUrl }
        }
        return profile()
    }

    func stats() -> APIClient.UserStats {
        let s = state()
        let chaptersRead = Set(s.history.map { $0.chapterId }).count
        return APIClient.UserStats(
            chaptersRead: chaptersRead,
            novelsInProgress: s.progress.count,
            bookmarks: s.bookmarks.count
        )
    }

    // MARK: - Reading progress

    func readingProgress(novelId: String) -> ReadingProgress? {
        state().progress[novelId]
    }

    func allReadingProgress() -> [ReadingProgress] {
        Array(state().progress.values).sorted {
            ($0.updatedAt ?? .distantPast) > ($1.updatedAt ?? .distantPast)
        }
    }

    func upsertReadingProgress(
        novelId: String,
        chapterId: String,
        currentLine: Int,
        totalLines: Int,
        percentage: Double?
    ) -> ReadingProgress {
        let pct = percentage ?? (totalLines > 0 ? (Double(currentLine) / Double(totalLines)) * 100 : 0)
        let now = Date()
        let progress = ReadingProgress(
            id: "\(Self.localUserId)-\(novelId)",
            userId: Self.localUserId,
            novelId: novelId,
            chapterId: chapterId,
            currentLine: currentLine,
            totalLines: totalLines,
            percentage: pct,
            updatedAt: now
        )
        mutate { s in
            s.progress[novelId] = progress
            // Record a history entry (most recent first, capped).
            let entry = AsterionReadingHistoryEntry(
                id: UUID().uuidString,
                userId: Self.localUserId,
                novelId: novelId,
                chapterId: chapterId,
                visitedAt: now,
                createdAt: now,
                updatedAt: now
            )
            s.history.removeAll { $0.novelId == novelId && $0.chapterId == chapterId }
            s.history.insert(entry, at: 0)
            if s.history.count > 500 { s.history.removeLast(s.history.count - 500) }
        }
        return progress
    }

    func readingHistory(limit: Int, offset: Int) -> [AsterionReadingHistoryEntry] {
        let all = state().history
        guard offset < all.count else { return [] }
        return Array(all[offset..<min(offset + limit, all.count)])
    }

    // MARK: - Library

    func library() -> [AsterionLibraryNovel] {
        state().library
            .map { novelId, date in
                AsterionLibraryNovel(
                    id: "\(Self.localUserId)-\(novelId)",
                    userId: Self.localUserId,
                    novelId: novelId,
                    createdAt: date,
                    updatedAt: date
                )
            }
            .sorted { ($0.createdAt ?? .distantPast) > ($1.createdAt ?? .distantPast) }
    }

    func addToLibrary(novelId: String) -> AsterionLibraryNovel {
        let now = Date()
        mutate { s in
            if s.library[novelId] == nil { s.library[novelId] = now }
        }
        let date = state().library[novelId] ?? now
        return AsterionLibraryNovel(
            id: "\(Self.localUserId)-\(novelId)",
            userId: Self.localUserId,
            novelId: novelId,
            createdAt: date,
            updatedAt: date
        )
    }

    func removeFromLibrary(novelId: String) -> Bool {
        var removed = false
        mutate { s in
            removed = s.library.removeValue(forKey: novelId) != nil
        }
        return removed
    }

    // MARK: - Bookmarks

    func bookmarks() -> [AsterionBookmark] {
        state().bookmarks.sorted { ($0.createdAt ?? .distantPast) > ($1.createdAt ?? .distantPast) }
    }

    func createBookmark(novelId: String, chapterId: String, note: String?) -> AsterionBookmark {
        let now = Date()
        let bookmark = AsterionBookmark(
            id: UUID().uuidString,
            userId: Self.localUserId,
            novelId: novelId,
            chapterId: chapterId,
            note: note,
            createdAt: now,
            updatedAt: now
        )
        mutate { s in s.bookmarks.append(bookmark) }
        return bookmark
    }

    func deleteBookmark(id: String) -> Bool {
        var removed = false
        mutate { s in
            let before = s.bookmarks.count
            s.bookmarks.removeAll { $0.id == id }
            removed = s.bookmarks.count != before
        }
        return removed
    }

    // MARK: - Preferences

    func preferences() -> AsterionUserPreferences {
        let s = state()
        return AsterionUserPreferences(
            id: Self.localUserId,
            userId: Self.localUserId,
            readingGoal: s.readingGoal,
            darkMode: s.darkMode,
            notificationsOn: s.notificationsOn,
            fontSizePref: s.fontSizePref,
            createdAt: s.createdAt,
            updatedAt: Date()
        )
    }

    func updatePreferences(
        readingGoal: Int?,
        darkMode: Bool?,
        notificationsOn: Bool?,
        fontSizePref: String?
    ) -> AsterionUserPreferences {
        mutate { s in
            if let readingGoal { s.readingGoal = readingGoal }
            if let darkMode { s.darkMode = darkMode }
            if let notificationsOn { s.notificationsOn = notificationsOn }
            if let fontSizePref { s.fontSizePref = fontSizePref }
        }
        return preferences()
    }

    // MARK: - Reset

    func reset() {
        let fresh = State(createdAt: Date())
        cached = fresh
        persist(fresh)
    }
}
