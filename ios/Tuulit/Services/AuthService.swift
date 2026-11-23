import Foundation
import FirebaseCore
import FirebaseAuth
import FirebaseFirestore
import FirebaseAppCheck
import TuulitCore

/// Firebase is optional at runtime, exactly like Android: without GoogleService-Info.plist the app
/// runs in local-only mode and every account feature explains why it's unavailable.
final class FirebaseService {
    static let notConfiguredMessage = "Accounts aren't set up in this build yet. Add the Firebase configuration (see docs/IOS_FIREBASE_SETUP.md)."

    let isConfigured: Bool

    init(enabled: Bool = true) {
        guard enabled, Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil else { isConfigured = false; return }
        if FirebaseApp.app() == nil {
            #if DEBUG
            AppCheck.setAppCheckProviderFactory(AppCheckDebugProviderFactory())
            #else
            AppCheck.setAppCheckProviderFactory(TuulitAppCheckFactory())
            #endif
            FirebaseApp.configure()
            // The encrypted local store is the offline cache; Firestore's own disk cache stays off so
            // health data is never held in a second, unencrypted place.
            let settings = Firestore.firestore().settings
            settings.cacheSettings = MemoryCacheSettings()
            Firestore.firestore().settings = settings
        }
        isConfigured = FirebaseApp.app() != nil
    }

    var auth: Auth? { isConfigured ? Auth.auth() : nil }
    var firestore: Firestore? { isConfigured ? Firestore.firestore() : nil }

    func appCheckToken() async -> String? {
        guard isConfigured else { return nil }
        return try? await AppCheck.appCheck().token(forcingRefresh: false).token
    }
}

/// App Attest on devices that support it, DeviceCheck otherwise.
final class TuulitAppCheckFactory: NSObject, AppCheckProviderFactory {
    func createProvider(with app: FirebaseApp) -> AppCheckProvider? {
        if #available(iOS 14.0, *) { return AppAttestProvider(app: app) }
        return DeviceCheckProvider(app: app)
    }
}

enum AuthState: Equatable {
    case unavailable
    case signedOut
    case signedIn(uid: String, email: String, verified: Bool, displayName: String)

    var uid: String? { if case let .signedIn(uid, _, _, _) = self { return uid }; return nil }
    var isSignedIn: Bool { uid != nil }
}

/// Result of an auth action: nil on success, otherwise a message ready to show.
typealias AuthFailure = String

/// Email and password accounts on the shared Firebase project, so the same login works on
/// Android and iPhone. Port of Android `AuthRepository`.
@MainActor
@Observable
final class AuthService: TokenSource {
    private(set) var state: AuthState
    private let firebase: FirebaseService
    private let api: APIClient
    private var handle: AuthStateDidChangeListenerHandle?

    init(firebase: FirebaseService, api: APIClient) {
        self.firebase = firebase
        self.api = api
        state = firebase.isConfigured ? AuthService.map(Auth.auth().currentUser) : .unavailable
        if let auth = firebase.auth {
            handle = auth.addStateDidChangeListener { [weak self] _, user in
                Task { @MainActor in self?.state = AuthService.map(user) }
            }
        }
        api.tokens = self
    }

    private static func map(_ u: User?) -> AuthState {
        guard let u else { return .signedOut }
        return .signedIn(uid: u.uid, email: u.email ?? "", verified: u.isEmailVerified, displayName: u.displayName ?? "")
    }

    nonisolated func idToken() async -> String? {
        guard let user = await MainActor.run(body: { Auth.auth().currentUser }) else { return nil }
        return try? await user.getIDToken()
    }

    nonisolated func appCheckToken() async -> String? { await firebase.appCheckToken() }

    var currentUid: String? { firebase.isConfigured ? Auth.auth().currentUser?.uid : nil }

    func register(email: String, password: String, displayName: String) async -> AuthFailure? {
        guard let auth = firebase.auth else { return FirebaseService.notConfiguredMessage }
        if let e = Validation.email(email) { return e }
        if let e = Validation.password(password) { return e }
        return await guarded {
            let result = try await auth.createUser(withEmail: email.trimmed, password: password)
            if !displayName.isBlank {
                let change = result.user.createProfileChangeRequest()
                change.displayName = displayName.trimmed
                try await change.commitChanges()
            }
            _ = await self.sendVerification()
            // Welcome email is best-effort through the Worker + Brevo; registration never fails because of it.
            _ = try? await self.api.post("/v1/email/welcome", ["displayName": displayName.trimmed])
            self.state = AuthService.map(auth.currentUser)
        }
    }

    func signIn(email: String, password: String) async -> AuthFailure? {
        guard let auth = firebase.auth else { return FirebaseService.notConfiguredMessage }
        if let e = Validation.email(email) { return e }
        if password.isEmpty { return "Enter your password." }
        return await guarded { _ = try await auth.signIn(withEmail: email.trimmed, password: password) }
    }

    func signOut() { try? firebase.auth?.signOut() }

    /// Firebase's own reset email: free, rate-limited by Google and needs no secret.
    func sendPasswordReset(email: String) async -> AuthFailure? {
        guard let auth = firebase.auth else { return FirebaseService.notConfiguredMessage }
        if let e = Validation.email(email) { return e }
        return await guarded { try await auth.sendPasswordReset(withEmail: email.trimmed) }
    }

    /// Branded Brevo email through the Worker first; Firebase's built-in email as the fallback.
    func sendVerification() async -> AuthFailure? {
        guard let user = firebase.auth?.currentUser else { return "Sign in first." }
        if (try? await api.post("/v1/email/verification", [:])) != nil { return nil }
        return await guarded { try await user.sendEmailVerification() }
    }

    func refresh() async {
        guard let user = firebase.auth?.currentUser else { return }
        try? await user.reload()
        _ = try? await user.getIDTokenResult(forcingRefresh: true)
        state = AuthService.map(firebase.auth?.currentUser)
    }

    func updateDisplayName(_ name: String) async {
        guard let user = firebase.auth?.currentUser else { return }
        let change = user.createProfileChangeRequest()
        change.displayName = name.trimmed
        try? await change.commitChanges()
        state = AuthService.map(firebase.auth?.currentUser)
    }

    /// Re-checks the password before a destructive action such as account deletion.
    func reauthenticate(password: String) async -> AuthFailure? {
        guard let user = firebase.auth?.currentUser else { return "Sign in first." }
        guard let email = user.email else { return "This account has no email." }
        return await guarded { _ = try await user.reauthenticate(with: EmailAuthProvider.credential(withEmail: email, password: password)) }
    }

    private func guarded(_ block: () async throws -> Void) async -> AuthFailure? {
        do { try await block(); return nil } catch { return AuthService.friendly(error) }
    }

    static func friendly(_ error: Error) -> String {
        let ns = error as NSError
        guard ns.domain == AuthErrorDomain, let code = AuthErrorCode(rawValue: ns.code) else {
            return ns.localizedDescription.isEmpty ? "Something went wrong. Please try again." : ns.localizedDescription
        }
        switch code {
        case .weakPassword: return "That password is too easy to guess. Try a longer one."
        case .emailAlreadyInUse: return "There's already an account with that email. Try signing in."
        case .userNotFound: return "We couldn't find an account with that email."
        case .wrongPassword, .invalidCredential: return "That email and password don't match."
        case .requiresRecentLogin: return "For your security, please enter your password again."
        case .tooManyRequests: return "Too many attempts. Please wait a few minutes."
        case .networkError: return "You're offline. Check your connection and try again."
        case .invalidEmail: return "That doesn't look like an email address."
        default: return ns.localizedDescription
        }
    }
}
