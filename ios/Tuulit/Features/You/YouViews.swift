import SwiftUI
import UIKit
import UserNotifications
import TuulitCore

struct YouView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c

    var body: some View {
        let s = model.settings
        let snap = model.snapshot
        let name = model.name.isBlank ? "You" : model.name
        ScreenScroll(bottomPad: 40) {
            AppBar(onBack: { model.pop() })
            HStack(spacing: 14) {
                Avatar(name: name, size: 56)
                VStack(alignment: .leading, spacing: 2) {
                    T(name, .headline)
                    T(snap.cycles.first.map { "Tracking since \(YearMonth.of(epochDay: $0.startEpochDay).title) · \(snap.cycles.count) \(snap.cycles.count == 1 ? "cycle" : "cycles")" }
                      ?? "No cycles logged yet", .caption, c.inkMuted)
                }
                Spacer()
                Mascot(s.bananaMode.mood, size: 52)
            }
            .padding(18)
            .background(c.roseSoft, in: RoundedRectangle(cornerRadius: Radius.lg, style: .continuous))
            .padding(.top, 4)
            VStack(spacing: 0) {
                ListRow("Cycle settings", icon: Icons.cycle, trailingText: "\(s.cycleLength) · \(s.periodLength) days", chevron: true) { model.push(.youCycle) }
                ListRow("Banana Mode", chevron: true, action: { model.push(.youBanana) }, leading: { Mascot(s.bananaMode.mood, size: 28) })
                    .overlay(alignment: .trailing) { T(s.bananaMode.title, .bodySm, c.inkMuted).padding(.trailing, 26).allowsHitTesting(false) }
                ListRow("Trusted people", icon: Icons.users, chevron: true) { model.push(.connect) }
                ListRow("Privacy and security", icon: Icons.shield, chevron: true) { model.push(.youPrivacy) }
                ListRow("Notifications", icon: Icons.bell, chevron: true) { model.push(.youNotifications) }
                ListRow("Appearance and accessibility", icon: Icons.eye, trailingText: s.themeMode == .LIGHT ? "Light" : s.themeMode == .DARK ? "Warm dark" : "System",
                        chevron: true) { model.push(.youAppearance) }
                ListRow("Account and backup", icon: Icons.download, trailingText: model.auth.state.isSignedIn ? "Signed in" : "Not set up", chevron: true) { model.push(.youAccount) }
                ListRow("Help and about", icon: Icons.info, chevron: true, last: true) { model.push(.youHelp) }
            }
            .padding(.horizontal, 16)
            .background(c.oat, in: RoundedRectangle(cornerRadius: Radius.lg, style: .continuous))
            .padding(.top, 14)
        }
    }
}

struct CycleSettingsView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c
    @State private var cycle = 28
    @State private var period = 5
    @State private var reg = CycleRegularity.NOT_SURE
    @State private var name = ""
    @State private var loaded = false

    var body: some View {
        ScreenScroll(bottomPad: 40) {
            AppBar("Cycle settings", onBack: { model.pop() })
            T("Tuulit uses these until it has learned from your logs.", .bodySm, c.inkMuted).padding(.vertical, 8)
            Field(label: "Name", text: Binding(get: { name }, set: { name = $0.take(40) }), placeholder: "First name or nickname",
                  helper: "Only shown to you and people you connect with.")
            Stepper(value: $cycle, in: 15...60) { HStack { T("Cycle length", .subtitle); Spacer(); T("\(cycle) days", .title) } }.padding(.top, 16)
            Stepper(value: $period, in: 1...14) { HStack { T("Period length", .subtitle); Spacer(); T("\(period) days", .title) } }.padding(.top, 12)
            T("How regular are they?", .subtitle).padding(.top, 18).padding(.bottom, 8)
            FlowLayout { ForEach(CycleRegularity.allCases, id: \.self) { r in Chip(r.label, selected: reg == r) { reg = r } } }
            TButton("Save", fullWidth: true) {
                model.settingsStore.setCycleDefaults(cycle, period, reg)
                if name != model.settings.displayName {
                    model.settingsStore.setDisplayName(name)
                    Task { await model.auth.updateDisplayName(name) }
                }
                model.showToast("Cycle settings saved"); model.pop()
            }
            .padding(.top, 24)
        }
        .onAppear {
            guard !loaded else { return }
            let s = model.settings
            cycle = s.cycleLength; period = s.periodLength; reg = s.regularity; name = s.displayName; loaded = true
        }
    }
}

struct BananaModeView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c
    var body: some View {
        let s = model.settings
        ScreenScroll(bottomPad: 40) {
            AppBar("Banana Mode", onBack: { model.pop() })
            T("Your banana keeps you company in the mood you choose.", .bodySm, c.inkMuted).padding(.vertical, 8)
            ForEach(Array(BananaModePersona.allCases.enumerated()), id: \.element) { i, p in
                ListRow(p.title, sub: p.tagline, last: i == BananaModePersona.allCases.count - 1, action: { model.settingsStore.setBananaMode(p) },
                        leading: { Mascot(p.mood, size: 48) })
                    .overlay(alignment: .trailing) { if s.bananaMode == p { TIcon(Icons.check).accessibilityLabel("Selected") } }
                    .accessibilityAddTraits(s.bananaMode == p ? [.isSelected] : [])
            }
            SettingRow(title: "Today is not the day", sub: "A quiet view with four gentle actions until midnight", isOn: s.notTheDayOn == Dates.today(), last: true) { on in
                model.settingsStore.setNotTheDay(on ? Dates.today() : nil)
            }
            .padding(.top, 16)
        }
    }
}

struct PrivacyView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c
    @Environment(\.openURL) private var openURL
    @State private var pinSheet = false
    @State private var wipe = false

    var body: some View {
        let s = model.settings
        ScreenScroll(bottomPad: 40) {
            AppBar("Privacy", onBack: { model.pop() })
            Notice("Stored only on this phone", "Your logs live in an encrypted file on this device. Nothing leaves it unless you back up, use Ask or share with someone.",
                   icon: Icons.shield).padding(.top, 8)
            SettingRow(title: "App lock", sub: s.appLockEnabled ? "PIN required when opening Tuulit" : "Off", isOn: s.appLockEnabled) { on in
                if on { pinSheet = true } else { model.settingsStore.setPin(nil); model.showToast("App lock off") }
            }
            .padding(.top, 8)
            SettingRow(title: "Discreet notifications", sub: "No cycle words on the lock screen", isOn: s.discreetNotifications) { model.settingsStore.setDiscreet($0) }
            SettingRow(title: "Personal answers in Ask", sub: "Shares a summary (cycle length, phase, top symptoms). Never notes or dates.", isOn: s.aiContextOptIn) {
                model.settingsStore.setAiContext($0)
            }
            ShareLink(item: CSVExport(csv: model.tracking.exportCSV()), preview: SharePreview("Tuulit export")) {
                HStack(spacing: 12) {
                    TIcon(Icons.download, size: 20).frame(width: 40, height: 40).background(c.peachSoft, in: RoundedRectangle(cornerRadius: 14))
                    VStack(alignment: .leading) { T("Export my data", .body).fontWeight(.semibold); T("CSV of logs and pain entries", .bodySm, c.inkMuted) }
                    Spacer()
                    TIcon(Icons.chevR, size: 18, color: c.inkFaint)
                }
                .padding(.vertical, 12)
            }
            Hairline()
            ListRow("Trusted people", sub: "Who can see what", icon: Icons.users, chevron: true) { model.push(.connect) }
            ListRow("Delete all data on this phone", sub: "Asks you to confirm first", chevron: true, last: true, titleColor: c.alert) { wipe = true }
            TButton("Read the privacy policy", kind: .text) {
                if let u = URL(string: Bundle.main.object(forInfoDictionaryKey: "TuulitPrivacyURL") as? String ?? "") { openURL(u) }
            }
            .padding(.top, 12)
        }
        .sheet(isPresented: $pinSheet) { PinSetupSheet().presentationDetents([.large]) }
        .confirmationDialog("Delete all Tuulit data?", isPresented: $wipe, titleVisibility: .visible) {
            Button("Delete", role: .destructive) { model.auth.signOut(); model.wipeThisDevice() }
            Button("Keep my data", role: .cancel) {}
        } message: {
            Text("Every log, pain entry, setting and voice note on this phone will be erased. This can't be undone. A cloud backup, if you have one, is not affected; delete it in Account.")
        }
    }
}

/// Two-step PIN creation.
struct PinSetupSheet: View {
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c
    @Environment(\.dismiss) private var dismiss
    @State private var first: String?
    @State private var pin = ""
    @State private var error: String?

    var body: some View {
        VStack(spacing: 0) {
            T(first == nil ? "Choose a 4–6 digit PIN" : "Enter it again", .headline).padding(.top, 28)
            T("You'll need it each time you open Tuulit. It can't be recovered, but you can always delete the app's data.", .caption, c.inkMuted, align: .center)
                .padding(.top, 6).padding(.horizontal, 20)
            PinDots(filled: pin.count, total: max(4, pin.count)).padding(.vertical, 18)
            if let error { T(error, .bodySm, c.alert).padding(.bottom, 8) }
            Spacer()
            Keypad(onDigit: { d in if pin.count < 6 { pin += d }; error = nil }, onDelete: { if !pin.isEmpty { pin.removeLast() } }, onSubmit: submit)
                .padding(.bottom, 24)
        }
        .background(c.cream)
    }

    private func submit() {
        if let f = first {
            if f == pin { model.settingsStore.setPin(pin); model.unlocked = true; dismiss(); model.showToast("App lock on") }
            else { error = "Those didn't match. Start again."; first = nil; pin = "" }
        } else if let v = Validation.pin(pin) { error = v }
        else { first = pin; pin = "" }
    }
}

struct NotificationsView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c
    @Environment(\.openURL) private var openURL
    @State private var allowed = true

    private func hm(_ m: Int) -> String { String(format: "%02d:%02d", m / 60, m % 60) }

    var body: some View {
        let s = model.settings
        ScreenScroll(bottomPad: 40) {
            AppBar("Notifications", onBack: { model.pop() })
            if !allowed {
                Notice(title: "Notifications are off for Tuulit", message: "Allow them so reminders can reach you.") {
                    TButton("Allow notifications", kind: .text) {
                        Task {
                            let ok = await model.reminders.requestPermission()
                            allowed = ok
                            if !ok, let u = URL(string: UIApplication.openNotificationSettingsURLString) { openURL(u) }
                        }
                    }
                }
                .padding(.top, 8)
            }
            Panel(padding: 16) {
                T("Lock screen preview", .caption, c.inkMuted).padding(.bottom, 8)
                HStack(alignment: .top, spacing: 10) {
                    Mascot(.warm, size: 28)
                    VStack(alignment: .leading) {
                        T("Tuulit", .label)
                        T(s.discreetNotifications ? ReminderPlanner.discreetBody : "Your period may start in \(s.periodLeadDays) days", .bodySm)
                    }
                }
            }
            .padding(.top, 8)
            SettingRow(title: "Period coming up", sub: "\(s.periodLeadDays) days before · at check-in time", isOn: s.notifyPeriod) { on in enable { model.settingsStore.setNotifyPeriod(on, leadDays: s.periodLeadDays) } }
            if s.notifyPeriod {
                FlowLayout { ForEach(1...3, id: \.self) { d in Chip("\(d) \(d == 1 ? "day" : "days") before", selected: s.periodLeadDays == d) { model.settingsStore.setNotifyPeriod(true, leadDays: d) } } }
                    .padding(.vertical, 8)
            }
            SettingRow(title: "Evening check-in", sub: "\(hm(s.checkInMinuteOfDay)) · only if you haven't logged · skipped days are fine", isOn: s.notifyCheckIn) { on in
                enable { model.settingsStore.setNotifyCheckIn(on, minuteOfDay: s.checkInMinuteOfDay) }
            }
            if s.notifyCheckIn {
                FlowLayout { ForEach([18 * 60, 20 * 60, 21 * 60 + 30], id: \.self) { m in Chip(hm(m), selected: s.checkInMinuteOfDay == m) { model.settingsStore.setNotifyCheckIn(true, minuteOfDay: m) } } }
                    .padding(.vertical, 8)
            }
            SettingRow(title: "Discreet notifications", sub: "No cycle words on the lock screen", isOn: s.discreetNotifications) { model.settingsStore.setDiscreet($0) }
            T("Quiet hours", .subtitle).padding(.top, 16).padding(.bottom, 8)
            FlowLayout {
                ForEach([("22:00–08:00", 22 * 60, 8 * 60), ("23:00–07:00", 23 * 60, 7 * 60), ("21:00–09:00", 21 * 60, 9 * 60), ("Off", 0, 0)], id: \.0) { q in
                    Chip(q.0, selected: s.quietStartMinute == q.1 && s.quietEndMinute == q.2) { model.settingsStore.setQuietHours(q.1, q.2) }
                }
            }
            T("Tuulit never sends streak or “you missed a day” alerts.", .caption, c.inkFaint).padding(.top, 16)
        }
        .task { allowed = await model.reminders.isAllowed() }
    }

    private func enable(_ apply: @escaping () -> Void) {
        apply()
        Task { if !(await model.reminders.isAllowed()) { allowed = await model.reminders.requestPermission() } }
    }
}

struct AppearanceView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c
    var body: some View {
        let s = model.settings
        ScreenScroll(bottomPad: 40) {
            AppBar("Appearance", onBack: { model.pop() })
            T("Theme", .subtitle).padding(.top, 8).padding(.bottom, 8)
            Segmented(options: ["System", "Light", "Warm dark"], selection: Binding(
                get: { ThemeMode.allCases.firstIndex(of: s.themeMode) ?? 1 }, set: { model.settingsStore.setTheme(ThemeMode.allCases[$0]) }))
            SettingRow(title: "Reduced motion", sub: "Cross-fades only; the breathing guide stays still", isOn: s.reducedMotion, last: true) { model.settingsStore.setReducedMotion($0) }
                .padding(.top, 12)
            T("Text size follows your iPhone's Text Size setting.", .caption, c.inkFaint).padding(.top, 12)
        }
    }
}

struct AccountView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c
    @State private var syncing = false
    @State private var deleteSheet = false

    var body: some View {
        ScreenScroll(bottomPad: 40) {
            AppBar("Account and backup", onBack: { model.pop() })
            switch model.auth.state {
            case .unavailable:
                Notice("Accounts aren't available in this build", FirebaseService.notConfiguredMessage).padding(.top, 8)
            case .signedOut:
                T("Back up your data", .display).padding(.top, 8)
                T("An account lets you restore Tuulit on a new phone, use it on Android and iPhone, and use Ask and Connect. Tuulit works fully without one.",
                  .bodySm, c.inkMuted).padding(.top, 8).padding(.bottom, 20)
                TButton("Create an account", fullWidth: true) { model.push(.auth(.register)) }
                TButton("Sign in", kind: .text, fullWidth: true) { model.push(.auth(.signIn)) }.padding(.top, 8)
            case let .signedIn(_, email, verified, _):
                ListRow(email, sub: verified ? "Email confirmed" : "Email not confirmed yet", icon: Icons.mail)
                VerifyEmailNotice().padding(.vertical, 8)
                if !model.sync.isOnline { Notice("You're offline", "Your logs are safe on this phone and back up when you reconnect.", icon: Icons.wifi).padding(.bottom, 8) }
                ListRow("Back up now", sub: model.settings.lastSyncAt > 0 ? "Last backup \(Dates.day(Dates.epochDay(ofMillis: model.settings.lastSyncAt)))" : "Not backed up yet",
                        icon: Icons.refresh, action: {
                            syncing = true
                            Task {
                                let r = await model.sync.sync(fullPull: true)
                                syncing = false
                                switch r {
                                case .done: model.showToast("Backed up")
                                case .failed(let m): model.showToast(m)
                                case .skipped: model.showToast("Nothing to back up")
                                }
                            }
                        }, trailing: { if syncing { ProgressView() } })
                ListRow("Sign out", sub: "Your data stays on this phone", icon: Icons.user, action: { model.auth.signOut(); model.showToast("Signed out") })
                ListRow("Delete account", sub: "Removes your account, cloud backup and connections", last: true, titleColor: c.alert) { deleteSheet = true }
                T("Backups are stored in Firestore under your account only. Only you can read them. The same account works on Android.", .caption, c.inkFaint).padding(.top, 12)
            }
        }
        .sheet(isPresented: $deleteSheet) { DeleteAccountSheet().presentationDetents([.medium, .large]) }
    }
}

private struct DeleteAccountSheet: View {
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c
    @Environment(\.dismiss) private var dismiss
    @State private var password = ""
    @State private var error: String?
    @State private var busy = false
    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            T("Delete your account?", .headline).padding(.top, 24)
            T("This removes your account, cloud backup, voice-note backups, invites and connections, and erases data on this phone. It can't be undone.", .body, c.inkMuted)
                .padding(.top, 8).padding(.bottom, 16)
            Field(label: "Confirm your password", text: $password, secure: true, error: error, contentType: .password)
            HStack(spacing: 10) {
                TButton("Keep my account", kind: .quiet, fullWidth: true) { dismiss() }
                TButton("Delete", kind: .danger, loading: busy, enabled: !password.isEmpty, fullWidth: true) {
                    busy = true
                    Task {
                        if let e = await model.deleteAccount(password: password) { error = e } else { dismiss(); model.showToast("Account deleted") }
                        busy = false
                    }
                }
            }
            .padding(.top, 20)
            Spacer()
        }
        .padding(.horizontal, 20)
        .background(c.cream)
    }
}

/// Shown while the email address is unverified. Polls until verified.
struct VerifyEmailNotice: View {
    @Environment(AppModel.self) private var model
    @State private var sent = false
    @State private var msg: String?
    var body: some View {
        if case let .signedIn(uid, email, verified, _) = model.auth.state, !verified {
            Notice(title: "Confirm your email", message: msg ?? "We sent a link to \(email). Confirming keeps your account recoverable.") {
                TButton(sent ? "Sent" : "Send again", kind: .text, enabled: !sent) {
                    Task { let r = await model.auth.sendVerification(); sent = r == nil; msg = r }
                }
            }
            .task(id: uid) {
                while !Task.isCancelled { try? await Task.sleep(nanoseconds: 15_000_000_000); await model.auth.refresh() }
            }
        }
    }
}

struct HelpView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c
    @Environment(\.openURL) private var openURL
    var body: some View {
        let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? ""
        ScreenScroll(bottomPad: 40) {
            AppBar("Help and about", onBack: { model.pop() })
            Mascot(.warm, size: 72).padding(.top, 8)
            T("Tuulit \(version)", .headline).padding(.top, 10)
            T("A menstrual health and wellness companion. Predictions are estimates and can't be used as contraception. Tuulit gives general information, never a diagnosis.",
              .bodySm, c.inkMuted).padding(.top, 8).padding(.bottom, 12)
            GroupTitle("How predictions work")
            T("Tuulit averages your recent cycles, giving more weight to the latest, and widens the window when your cycles vary. The fertile window is estimated as the days around 14 days before your next predicted period.",
              .bodySm, c.inkMuted).padding(.bottom, 12)
            GroupTitle("When to get help")
            T("If you have severe pain, very heavy bleeding, fever, fainting, or thoughts of harming yourself, contact a clinician or emergency services.",
              .bodySm, c.inkMuted).padding(.bottom, 12)
            ListRow("Privacy policy", icon: Icons.shield, chevron: true) {
                if let u = URL(string: Bundle.main.object(forInfoDictionaryKey: "TuulitPrivacyURL") as? String ?? "") { openURL(u) }
            }
            ListRow("Fonts", sub: "Fraunces and Plus Jakarta Sans, SIL Open Font License 1.1", icon: Icons.book, last: true)
        }
    }
}
