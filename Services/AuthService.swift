import Combine
import Foundation
import OSLog

@MainActor
final class AuthService: NSObject, ObservableObject {
    @Published var currentUser: User?
    @Published var sessionToken: String?
    @Published var authError: String?

    private let keychain = KeychainHelper()
    private let tokenKey = "asterion.session.token"
    private let userIdKey = "asterion.user.id"
    private let logger = Logger(subsystem: "Asterion", category: "Auth")

    private func debugPrint(_ message: String) {
        #if DEBUG
        print("[AuthService] \(message)")
        #endif
    }

    var isSignedIn: Bool {
        currentUser != nil
    }

    func restoreSession() async {
        sessionToken = keychain.read(key: tokenKey)
        debugPrint("Restore session completed. tokenPresent=\(sessionToken != nil)")
    }

    func signOut() {
        // Offline, single-user build: there is no remote account to sign out of,
        // so this is intentionally inert (kept so ProfileView compiles unchanged).
        // Local reading data can be cleared via LocalUserStore.reset() if desired.
        logger.info("Sign out requested (no-op in offline build).")
        debugPrint("Sign out requested (no-op in offline build).")
    }

    func persistSession(token: String, user: User) {
        logger.info("Persisting session for userId: \(user.id, privacy: .public)")
        debugPrint("Persisting session for userId: \(user.id)")
        sessionToken = token
        currentUser = user
        keychain.save(key: tokenKey, value: token)
        keychain.save(key: userIdKey, value: user.id)
    }

    /// Establishes the single local reader account. Keeps its original name so
    /// existing call sites (AsterionApp, ProfileView) are unchanged, but no
    /// longer touches Clerk — the app is always "signed in" locally and offline.
    func syncClerkSession() async {
        let existingId = keychain.read(key: userIdKey)
        let userId = existingId ?? LocalUserStore.localUserId
        if existingId == nil {
            keychain.save(key: userIdKey, value: userId)
        }
        currentUser = User(
            id: userId,
            appleUserId: nil,
            email: nil,
            username: "Reader",
            pfpUrl: nil,
            bookmarks: []
        )
        sessionToken = userId
        authError = nil
        logger.info("Local reader session established.")
        debugPrint("Local reader session established. userId=\(userId)")
    }

    func syncUserProfileToBackend(using apiClient: APIClient) async {
        guard let user = currentUser else {
            debugPrint("Profile sync skipped: no signed-in user.")
            return
        }
        debugPrint("Starting backend profile sync for userId: \(user.id)")
        do {
            _ = try await apiClient.updateMyProfile(
                email: user.email,
                username: user.username,
                avatarUrl: user.pfpUrl
            )
            debugPrint("Backend profile sync succeeded for userId: \(user.id)")
        } catch {
            authError = "Signed in, but failed to sync profile to backend."
            debugPrint("Backend profile sync failed for userId: \(user.id): \(error.localizedDescription)")
        }
    }
}
