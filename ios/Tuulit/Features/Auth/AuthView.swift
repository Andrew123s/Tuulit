import SwiftUI
import TuulitCore

/// Sign in, create account and password reset on the shared Firebase project, so the same
/// account works on Android and iPhone.
struct AuthView: View {
    let startMode: AuthMode
    var onDone: (() -> Void)? = nil
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c
    @State private var mode: AuthMode = .signIn
    @State private var email = ""
    @State private var password = ""
    @State private var name = ""
    @State private var busy = false
    @State private var error: String?
    @State private var info: String?
    @State private var emailErr: String?
    @State private var pwErr: String?
    @State private var started = false
    @FocusState private var focus: Int?

    var body: some View {
        VStack(spacing: 0) {
            AppBar(close: true, onBack: { close() })
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Mascot(mode == .register ? .happy : .warm, size: 72).padding(.top, 4)
                    T(mode == .signIn ? "Welcome back" : mode == .register ? "Back up your data" : "Reset your password", .headline).padding(.top, 12)
                    T(mode == .signIn ? "Sign in to restore your backup, use Ask and connect with your person. The same account works on Android."
                      : mode == .register ? "An account lets you restore Tuulit on a new phone, use it on Android and iPhone, and use Ask and Connect. It's optional: Tuulit works fully without one."
                      : "Enter your email and we'll send a link to choose a new password.", .bodySm, c.inkMuted).padding(.top, 8).padding(.bottom, 20)
                    if !model.firebase.isConfigured { Notice("Accounts aren't available yet", FirebaseService.notConfiguredMessage).padding(.bottom, 16) }
                    if mode == .register {
                        Field(label: "Name (optional)", text: Binding(get: { name }, set: { name = $0.take(40) }), placeholder: "First name or nickname",
                              helper: "Only shown to you and people you connect with.", contentType: .givenName, submitLabel: .next, onSubmit: { focus = 1 })
                            .focused($focus, equals: 0)
                            .padding(.bottom, 14)
                    }
                    Field(label: "Email", text: Binding(get: { email }, set: { email = $0.trimmed; emailErr = nil }), placeholder: "you@example.com", error: emailErr,
                          keyboard: .emailAddress, contentType: .emailAddress, submitLabel: mode == .forgot ? .send : .next,
                          onSubmit: { if mode == .forgot { submit() } else { focus = 2 } })
                        .focused($focus, equals: 1)
                        .accessibilityIdentifier("auth.email")
                    if mode != .forgot {
                        Field(label: "Password", text: Binding(get: { password }, set: { password = $0; pwErr = nil }),
                              placeholder: mode == .register ? "At least 10 characters, with a number" : "", secure: true, error: pwErr,
                              contentType: mode == .register ? .newPassword : .password, submitLabel: .go, onSubmit: submit)
                            .focused($focus, equals: 2)
                            .padding(.top, 14)
                    }
                    if let error { Notice("That didn't work", error, kind: .medical).padding(.top, 12) }
                    if let info { Notice("Check your inbox", info).padding(.top, 12) }
                }
            }
            .scrollDismissesKeyboard(.interactively)
            TButton(mode == .signIn ? "Sign in" : mode == .register ? "Create account" : "Send reset link", loading: busy,
                    enabled: model.firebase.isConfigured, fullWidth: true, action: submit)
            switch mode {
            case .signIn:
                TButton("Forgot password?", kind: .text, fullWidth: true) { mode = .forgot; error = nil }.padding(.top, 4)
                TButton("Create an account instead", kind: .text, fullWidth: true) { mode = .register; error = nil }
            case .register:
                TButton("Sign in instead", kind: .text, fullWidth: true) { mode = .signIn; error = nil }.padding(.top, 4)
            case .forgot:
                TButton("Back to sign in", kind: .text, fullWidth: true) { mode = .signIn; error = nil }.padding(.top, 4)
            }
        }
        .padding(.horizontal, 20).padding(.bottom, 12)
        .background(c.cream.ignoresSafeArea())
        .onAppear { if !started { mode = startMode; started = true } }
    }

    private func close() { if let onDone { onDone() } else { model.pop() } }

    private func submit() {
        error = nil; info = nil
        emailErr = Validation.email(email)
        pwErr = mode == .register ? Validation.password(password) : mode == .signIn ? (password.isEmpty ? "Enter your password." : nil) : nil
        guard emailErr == nil, pwErr == nil else { return }
        busy = true
        Task {
            let r: AuthFailure?
            switch mode {
            case .signIn: r = await model.auth.signIn(email: email, password: password)
            case .register:
                r = await model.auth.register(email: email, password: password, displayName: name)
                if r == nil && !name.isBlank { model.settingsStore.setDisplayName(name) }
            case .forgot: r = await model.auth.sendPasswordReset(email: email)
            }
            busy = false
            if let r { error = r }
            else if mode == .forgot { info = "If an account exists for \(email), a reset link is on its way."; mode = .signIn }
            else { close() }
        }
    }
}
