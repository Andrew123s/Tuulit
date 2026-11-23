import SwiftUI
import TuulitCore

/// Chooses onboarding, the lock screen or the app, applies the theme and reduced motion, and
/// hides the screen in the app switcher when app lock is on.
struct RootView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.colorScheme) private var systemScheme
    @Environment(\.accessibilityReduceMotion) private var systemReduceMotion
    @Environment(\.scenePhase) private var phase

    private var dark: Bool {
        switch model.settings.themeMode {
        case .SYSTEM: return systemScheme == .dark
        case .LIGHT: return false
        case .DARK: return true
        }
    }

    var body: some View {
        let palette = dark ? Palette.dark : Palette.light
        ZStack {
            palette.cream.ignoresSafeArea()
            if !model.settings.onboardingCompleted {
                OnboardingFlow()
            } else if model.settings.appLockEnabled && !model.unlocked {
                LockView()
            } else {
                ShellView()
            }
            if model.settings.appLockEnabled && phase != .active {
                // Privacy cover for the app switcher snapshot.
                ZStack { palette.cream; Mascot(.warm, size: 96) }.ignoresSafeArea()
            }
        }
        .environment(\.palette, palette)
        .environment(\.tuulitReducedMotion, systemReduceMotion || model.settings.reducedMotion)
        .preferredColorScheme(model.settings.themeMode == .SYSTEM ? nil : (dark ? .dark : .light))
        .tint(palette.period)
    }
}

/// App lock (PIN). Checked against a PBKDF2 hash; the PIN is never stored.
struct LockView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c
    @State private var pin = ""
    @State private var wrong = 0
    @State private var lockedUntil = Date.distantPast
    @State private var checking = false

    var body: some View {
        VStack {
            Mascot(.warm, size: 56).padding(.top, 56)
            T("Enter your PIN", .headline).padding(.top, 16)
            PinDots(filled: pin.count, total: max(4, pin.count)).padding(.top, 20)
            if wrong > 0 {
                T(Date() < lockedUntil ? "Too many tries. Wait 30 seconds." : "That PIN didn't match.", .bodySm, c.alert).padding(.top, 12)
            }
            Spacer()
            Keypad(onDigit: { d in
                guard Date() >= lockedUntil, pin.count < 6 else { return }
                pin += d
                if pin.count >= 4 { verify(final: pin.count == 6) }
            }, onDelete: { if !pin.isEmpty { pin.removeLast() } }, onSubmit: { if pin.count >= 4 { verify(final: true) } })
            .padding(.bottom, 24)
        }
        .padding(.horizontal, 20)
        .sensoryFeedback(.error, trigger: wrong)
    }

    private func verify(final: Bool) {
        guard !checking else { return }
        checking = true
        let attempt = pin
        Task.detached(priority: .userInitiated) {
            let ok = await MainActor.run { model.settingsStore }.verifyPin(attempt)
            await MainActor.run {
                checking = false
                if ok { model.unlocked = true; return }
                if final {
                    wrong += 1; pin = ""
                    if wrong % 5 == 0 { lockedUntil = Date().addingTimeInterval(30) }
                }
            }
        }
    }
}
