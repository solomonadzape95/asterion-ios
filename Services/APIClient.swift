import Foundation
import Combine

struct AsterionUserProfile: Identifiable, Codable, Hashable {
    let id: String
    let clerkUserId: String
    let email: String?
    let username: String?
    let avatarUrl: String?
    let createdAt: Date?
    let updatedAt: Date?
}

struct AsterionBookmark: Identifiable, Codable, Hashable {
    let id: String
    let userId: String
    let novelId: String
    let chapterId: String
    let note: String?
    let createdAt: Date?
    let updatedAt: Date?
}

struct AsterionReadingHistoryEntry: Identifiable, Codable, Hashable {
    let id: String
    let userId: String
    let novelId: String
    let chapterId: String
    let visitedAt: Date?
    let createdAt: Date?
    let updatedAt: Date?
}

struct AsterionLibraryNovel: Identifiable, Codable, Hashable {
    let id: String
    let userId: String
    let novelId: String
    let createdAt: Date?
    let updatedAt: Date?
}

struct AsterionUserPreferences: Identifiable, Codable, Hashable {
    let id: String
    let userId: String
    let readingGoal: Int
    let darkMode: Bool
    let notificationsOn: Bool
    let fontSizePref: String
    let createdAt: Date?
    let updatedAt: Date?
}

struct PaginatedResponse<T: Decodable>: Decodable {
    let data: [T]
    let meta: Meta?

    struct Meta: Decodable {
        let count: Int?
        let total: Int?
        let page: Int?
        let pageSize: Int?
        let totalPages: Int?
        let hasNextPage: Bool?
        let hasPreviousPage: Bool?
        let limit: Int?
        let offset: Int?
    }
}

@MainActor
final class APIClient: ObservableObject {
    private let contentBaseURL = URL(string: "https://scraper-production-8f07.up.railway.app")!
    private let userBaseURL: URL = APIClient.resolveUserBaseURL()
    private var sessionToken: String?

    private func debugLog(_ message: String) {
        #if DEBUG
        print("[APIClient] \(message)")
        #endif
    }

    init() {
        debugLog("Resolved user API base URL: \(userBaseURL.absoluteString)")
    }

    func setSessionToken(_ token: String?) {
        self.sessionToken = token
        debugLog("Session token updated. tokenPresent=\(token != nil)")
    }

    // MARK: - Novels & Chapters (local, offline)
    //
    // Content is served from a bundled SQLite database (LocalContentStore),
    // converted from the original PostgreSQL content dump. These methods keep
    // the same signatures as the former network calls so the views are unchanged.

    private let content = LocalContentStore.shared
    private let userStore = LocalUserStore.shared

    func fetchNovels(limit: Int = 30, offset: Int = 0, search: String = "") async throws -> [Novel] {
        try await content.fetchNovels(limit: limit, offset: offset, search: search)
    }

    func fetchNovel(id: String) async throws -> Novel {
        try await content.fetchNovel(id: id)
    }

    func fetchChapters(novelId: String, limit: Int = 100, offset: Int = 0) async throws -> PaginatedResponse<Chapter> {
        try await content.fetchChapters(novelId: novelId, limit: limit, offset: offset)
    }

    func fetchChapter(id: String) async throws -> Chapter {
        try await content.fetchChapter(id: id)
    }

    // MARK: - Auth

    func authenticateWithApple(identityToken: String, appleUserId: String, email: String?) async throws -> (String, User) {
        let url = contentBaseURL.appending(path: "/auth/apple")
        struct RequestBody: Encodable {
            let identityToken: String
            let appleUserId: String
            let email: String?
        }
        struct ResponseBody: Decodable {
            let sessionToken: String
            let user: User
        }
        let response: ResponseBody = try await request(
            url: url,
            method: "POST",
            body: RequestBody(identityToken: identityToken, appleUserId: appleUserId, email: email)
        )
        return (response.sessionToken, response.user)
    }

    // MARK: - User Data

    struct UserStats: Decodable {
        let chaptersRead: Int
        let novelsInProgress: Int
        let bookmarks: Int
    }

    func fetchMyStats() async throws -> UserStats {
        await userStore.stats()
    }

    func fetchMyProfile() async throws -> AsterionUserProfile {
        await userStore.profile()
    }

    func updateMyProfile(email: String? = nil, username: String? = nil, avatarUrl: String? = nil) async throws -> AsterionUserProfile {
        await userStore.updateProfile(email: email, username: username, avatarUrl: avatarUrl)
    }

    func fetchReadingProgress(novelId: String) async throws -> ReadingProgress? {
        await userStore.readingProgress(novelId: novelId)
    }

    func fetchAllReadingProgress() async throws -> [ReadingProgress] {
        await userStore.allReadingProgress()
    }

    func upsertReadingProgress(
        novelId: String,
        chapterId: String,
        currentLine: Int,
        totalLines: Int,
        percentage: Double? = nil
    ) async throws -> ReadingProgress {
        await userStore.upsertReadingProgress(
            novelId: novelId,
            chapterId: chapterId,
            currentLine: currentLine,
            totalLines: totalLines,
            percentage: percentage
        )
    }

    func fetchBookmarks() async throws -> [AsterionBookmark] {
        await userStore.bookmarks()
    }

    func fetchMyLibrary() async throws -> [AsterionLibraryNovel] {
        await userStore.library()
    }

    func addNovelToLibrary(novelId: String) async throws -> AsterionLibraryNovel {
        await userStore.addToLibrary(novelId: novelId)
    }

    @discardableResult
    func removeNovelFromLibrary(novelId: String) async throws -> Bool {
        await userStore.removeFromLibrary(novelId: novelId)
    }

    func fetchReadingHistory(limit: Int = 20, offset: Int = 0) async throws -> [AsterionReadingHistoryEntry] {
        await userStore.readingHistory(limit: limit, offset: offset)
    }

    func fetchMyPreferences() async throws -> AsterionUserPreferences {
        await userStore.preferences()
    }

    func updateMyPreferences(
        readingGoal: Int? = nil,
        darkMode: Bool? = nil,
        notificationsOn: Bool? = nil,
        fontSizePref: String? = nil
    ) async throws -> AsterionUserPreferences {
        await userStore.updatePreferences(
            readingGoal: readingGoal,
            darkMode: darkMode,
            notificationsOn: notificationsOn,
            fontSizePref: fontSizePref
        )
    }

    func createBookmark(novelId: String, chapterId: String, note: String? = nil) async throws -> AsterionBookmark {
        await userStore.createBookmark(novelId: novelId, chapterId: chapterId, note: note)
    }

    @discardableResult
    func deleteBookmark(id: String) async throws -> Bool {
        await userStore.deleteBookmark(id: id)
    }

    // MARK: - Networking

    private struct DataWrapper<T: Decodable>: Decodable {
        let data: T
    }

    private func request<T: Decodable, B: Encodable>(
        url: URL,
        method: String = "GET",
        body: B? = nil,
        retryOnUnauthorized: Bool = true
    ) async throws -> T {
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        if let sessionToken {
            request.setValue("Bearer \(sessionToken)", forHTTPHeaderField: "Authorization")
        }
        if let body {
            request.httpBody = try JSONEncoder().encode(body)
        }
        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await URLSession.shared.data(for: request)
        } catch {
            if let urlError = error as? URLError {
                debugLog(
                    "Transport failure \(method) \(url.absoluteString): code=\(urlError.code.rawValue) reason=\(urlError.localizedDescription)"
                )
            } else {
                debugLog("Transport failure \(method) \(url.absoluteString): \(error.localizedDescription)")
            }
            throw error
        }
        guard let httpResponse = response as? HTTPURLResponse else {
            throw URLError(.badServerResponse)
        }
        if httpResponse.statusCode == 401, retryOnUnauthorized {
            debugLog("Received 401 for \(method) \(url.absoluteString). Attempting token refresh.")
            let refreshed = await refreshSessionTokenIfPossible()
            if refreshed {
                debugLog("Token refresh succeeded. Retrying request \(method) \(url.absoluteString).")
                return try await self.request(
                    url: url,
                    method: method,
                    body: body,
                    retryOnUnauthorized: false
                )
            }
            debugLog("Token refresh failed for \(method) \(url.absoluteString).")
        }
        guard 200..<300 ~= httpResponse.statusCode else {
            throw makeHTTPError(
                statusCode: httpResponse.statusCode,
                data: data,
                url: url,
                method: method
            )
        }
        return try Self.makeDecoder().decode(T.self, from: data)
    }

    private func request<T: Decodable>(url: URL) async throws -> T {
        try await request(url: url, method: "GET", body: Optional<String>.none)
    }

    private func requestNoBody<T: Decodable>(
        url: URL,
        method: String,
        retryOnUnauthorized: Bool = true
    ) async throws -> T {
        var request = URLRequest(url: url)
        request.httpMethod = method
        if let sessionToken {
            request.setValue("Bearer \(sessionToken)", forHTTPHeaderField: "Authorization")
        }
        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await URLSession.shared.data(for: request)
        } catch {
            if let urlError = error as? URLError {
                debugLog(
                    "Transport failure \(method) \(url.absoluteString): code=\(urlError.code.rawValue) reason=\(urlError.localizedDescription)"
                )
            } else {
                debugLog("Transport failure \(method) \(url.absoluteString): \(error.localizedDescription)")
            }
            throw error
        }
        guard let httpResponse = response as? HTTPURLResponse else {
            throw URLError(.badServerResponse)
        }
        if httpResponse.statusCode == 401, retryOnUnauthorized {
            debugLog("Received 401 for \(method) \(url.absoluteString). Attempting token refresh.")
            let refreshed = await refreshSessionTokenIfPossible()
            if refreshed {
                debugLog("Token refresh succeeded. Retrying request \(method) \(url.absoluteString).")
                return try await requestNoBody(
                    url: url,
                    method: method,
                    retryOnUnauthorized: false
                )
            }
            debugLog("Token refresh failed for \(method) \(url.absoluteString).")
        }
        guard 200..<300 ~= httpResponse.statusCode else {
            throw makeHTTPError(
                statusCode: httpResponse.statusCode,
                data: data,
                url: url,
                method: method
            )
        }
        return try Self.makeDecoder().decode(T.self, from: data)
    }

    private func makeHTTPError(statusCode: Int, data: Data, url: URL, method: String) -> NSError {
        let responseBody = String(data: data, encoding: .utf8)?
            .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        #if DEBUG
        if !responseBody.isEmpty {
            print("[APIClient] \(method) \(url.absoluteString) -> \(statusCode): \(responseBody)")
        } else {
            print("[APIClient] \(method) \(url.absoluteString) -> \(statusCode)")
        }
        #endif

        var userInfo: [String: Any] = [
            NSLocalizedDescriptionKey: "HTTP \(statusCode) for \(method) \(url.path())",
        ]
        if !responseBody.isEmpty {
            userInfo[NSLocalizedFailureReasonErrorKey] = responseBody
            userInfo["responseBody"] = responseBody
        }
        return NSError(domain: "APIClient", code: statusCode, userInfo: userInfo)
    }

    private func refreshSessionTokenIfPossible() async -> Bool {
        // No remote auth in the offline build; nothing to refresh.
        return false
    }

    private static func makeDecoder() -> JSONDecoder {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .custom { decoder in
            let container = try decoder.singleValueContainer()
            let value = try container.decode(String.self)
            let isoFormatter = ISO8601DateFormatter()
            isoFormatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
            if let date = isoFormatter.date(from: value) {
                return date
            }
            let fallbackFormatter = ISO8601DateFormatter()
            fallbackFormatter.formatOptions = [.withInternetDateTime]
            if let date = fallbackFormatter.date(from: value) {
                return date
            }
            throw DecodingError.dataCorruptedError(in: container, debugDescription: "Invalid ISO8601 date: \(value)")
        }
        return decoder
    }

    private static func resolveUserBaseURL() -> URL {
        let rawConfigured: String? = {
            if let direct = Bundle.main.object(forInfoDictionaryKey: "USER_API_BASE_URL") as? String {
                return direct
            }

            // Some generated Info.plist variants can nest custom keys by underscores:
            // USER_API_BASE_URL -> USER -> API -> BASE -> URL
            if let user = Bundle.main.infoDictionary?["USER"] as? [String: Any],
               let api = user["API"] as? [String: Any],
               let base = api["BASE"] as? [String: Any],
               let nested = base["URL"] as? String
            {
                return nested
            }
            return nil
        }()

        if let rawConfigured {
            let configured = rawConfigured.trimmingCharacters(in: .whitespacesAndNewlines)
            if !configured.isEmpty {
                if let directURL = URL(string: configured), directURL.scheme != nil {
                    return directURL
                }
                if let httpsURL = URL(string: "https://\(configured)") {
                    return httpsURL
                }
                #if DEBUG
                print("[APIClient] Invalid USER_API_BASE_URL value: \(configured). Falling back to production.")
                #endif
            }
        }

        #if DEBUG
        print("[APIClient] USER_API_BASE_URL not found. Falling back to production.")
        #endif
        return URL(string: "https://asterion-ios-production.up.railway.app")!
    }
}
