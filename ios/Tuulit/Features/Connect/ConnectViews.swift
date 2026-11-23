import SwiftUI
import FirebaseFirestore
import TuulitCore

/// Sign-in gate shared by every Connect screen. Renders the reason and returns false when not ready.
private struct ConnectGate: View {
    @Environment(AppModel.self) private var model
    var showSignIn = true
    var body: some View {
        switch model.auth.state {
        case .unavailable: Notice("Connect needs accounts", FirebaseService.notConfiguredMessage).padding(.top, 12)
        case .signedOut:
            Notice(title: "Sign in to connect", message: "Both of you need a Tuulit account so updates go only to the right person.") {
                if showSignIn { TButton("Sign in or create an account", kind: .text) { model.push(.auth(.signIn)) } }
            }
            .padding(.top, 12)
        case .signedIn: EmptyView()
        }
    }
}

struct ConnectView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c
    @State private var editSos = false

    var body: some View {
        ScreenScroll(bottomPad: 40) {
            AppBar("Connect", onBack: { model.pop() })
            Illustration(kind: .heart, size: 96, bg: c.roseSoft).padding(.top, 6)
            T("Let someone in, on your terms.", .display).padding(.top, 14)
            T("They only see what you switch on. Symptoms, moods and notes are never shared.", .bodySm, c.inkMuted).padding(.top, 6)
            ConnectGate()
            if model.auth.state.isSignedIn {
                if !model.sync.isOnline { Notice("You're offline", "Connect updates will go out when you're back online.", icon: Icons.wifi).padding(.top, 12) }
                GroupTitle("People who can see your updates").padding(.top, 20)
                if model.connect.myPeople.isEmpty { T("No one yet.", .bodySm, c.inkMuted).padding(.vertical, 8) }
                ForEach(Array(model.connect.myPeople.enumerated()), id: \.element.id) { i, p in
                    let on = [p.permissions.periodSoon ? "period soon" : nil, p.permissions.periodStarted ? "period started" : nil,
                              p.permissions.roughDay ? "rough-day signal" : nil, p.permissions.phase ? "phase" : nil].compactMap { $0 }
                    ListRow(p.memberName, sub: (p.relationship.isBlank ? "Trusted person" : p.relationship) + " · " + (on.isEmpty ? "sees nothing yet" : "sees " + on.joined(separator: ", ")),
                            chevron: true, last: i == model.connect.myPeople.count - 1, action: { model.push(.connectPerson(p.id)) },
                            leading: { Avatar(name: p.memberName) })
                }
                TButton("Invite someone", kind: .quiet, icon: Icons.plus, fullWidth: true) { model.push(.connectInvite) }.padding(.top, 12)
                GroupTitle("People sharing with you").padding(.top, 24)
                if model.connect.sharingWithMe.isEmpty { T("If someone invites you, enter their code here.", .bodySm, c.inkMuted).padding(.vertical, 8) }
                ForEach(Array(model.connect.sharingWithMe.enumerated()), id: \.element.id) { i, p in
                    ListRow(p.ownerName, sub: "Shares updates with you", chevron: true, last: i == model.connect.sharingWithMe.count - 1,
                            action: { model.push(.partner(p.id)) }, leading: { Avatar(name: p.ownerName) })
                }
                TButton("I have an invite code", kind: .text) { model.push(.connectAccept) }
                GroupTitle("SOS messages").padding(.top, 24)
                ForEach(Array(model.settings.sosMessages.enumerated()), id: \.offset) { i, m in ListRow("“\(m)”", last: i == model.settings.sosMessages.count - 1) }
                TButton("Edit SOS messages", kind: .text, icon: Icons.edit) { editSos = true }
            }
        }
        .sheet(isPresented: $editSos) { EditSOSSheet().presentationDetents([.large]) }
    }
}

private struct EditSOSSheet: View {
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c
    @Environment(\.dismiss) private var dismiss
    @State private var drafts: [String] = []
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 10) {
                T("Your SOS messages", .headline).padding(.top, 24)
                T("Sent with one tap from Tuulit SOS. Keep them short.", .bodySm, c.inkMuted).padding(.bottom, 6)
                ForEach(drafts.indices, id: \.self) { i in
                    Field(label: "Message \(i + 1)", text: Binding(get: { drafts[i] }, set: { drafts[i] = $0.take(140) }))
                }
                TButton("Save", enabled: drafts.contains { !$0.isBlank }, fullWidth: true) {
                    model.settingsStore.setSosMessages(drafts); dismiss(); model.showToast("SOS messages saved")
                }
                .padding(.top, 6)
            }
            .padding(.horizontal, 20).padding(.bottom, 24)
        }
        .background(c.cream)
        .onAppear {
            if drafts.isEmpty {
                let m = model.settings.sosMessages
                drafts = m + Array(repeating: "", count: max(0, 4 - m.count))
            }
        }
    }
}

struct InviteView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c
    @State private var invite: Invite?
    @State private var error: String?
    @State private var busy = false

    var body: some View {
        ScreenScroll(bottomPad: 40) {
            AppBar(close: true, onBack: { model.pop() })
            T("Invite your person", .display).padding(.top, 8)
            T("They install Tuulit and enter this code. They'll only see what you switch on, and you can disconnect any time.", .bodySm, c.inkMuted)
                .padding(.top, 8).padding(.bottom, 20)
            ConnectGate(showSignIn: false)
            if model.auth.state.isSignedIn {
                if let inv = invite {
                    Panel(padding: 28) {
                        VStack(spacing: 8) {
                            T("Invite code", .caption, c.inkMuted)
                            T(String(inv.code.prefix(3)) + " " + String(inv.code.suffix(3)), .numeral)
                                .accessibilityLabel(inv.code.map(String.init).joined(separator: " "))
                                .textSelection(.enabled)
                            T("Expires \(Dates.short(Dates.epochDay(ofMillis: inv.expiresAt))), 24 hours from now", .caption, c.inkFaint)
                        }
                        .frame(maxWidth: .infinity)
                    }
                    T("When they accept, you'll get a notification. Everything starts switched off until you choose what they can see.", .bodySm, c.inkMuted).padding(.top, 16)
                    ShareLink(item: "Join me on Tuulit so I can share period updates with you. Open Connect → \"I have an invite code\" and enter \(inv.code).") {
                        HStack { TIcon(Icons.share, size: 18, color: c.onInk); T("Send invite", .label, c.onInk) }
                            .frame(maxWidth: .infinity, minHeight: 52).background(c.ink, in: Capsule())
                    }
                    .padding(.top, 20)
                } else {
                    TButton("Create invite code", loading: busy, fullWidth: true) {
                        busy = true
                        Task {
                            do { invite = try await model.connect.createInvite(ownerName: model.name) } catch { self.error = error.localizedDescription }
                            busy = false
                        }
                    }
                }
                if let error { Notice("Couldn't create a code", error, kind: .medical).padding(.top, 12) }
            }
        }
    }
}

struct AcceptInviteView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c
    @State private var code = ""
    @State private var name = ""
    @State private var relationship = "Partner"
    @State private var error: String?
    @State private var busy = false

    var body: some View {
        ScreenScroll(bottomPad: 40) {
            AppBar(close: true, onBack: { model.pop() })
            T("Enter an invite code", .display).padding(.top, 8)
            T("You'll see only the updates they choose to share.", .bodySm, c.inkMuted).padding(.top, 8).padding(.bottom, 20)
            ConnectGate()
            if model.auth.state.isSignedIn {
                Field(label: "Invite code", text: Binding(get: { code }, set: { code = String($0.uppercased().filter { $0.isLetter || $0.isNumber }.prefix(6)); error = nil }),
                      placeholder: "e.g. 4K8P2Q", keyboard: .asciiCapable)
                Field(label: "Your name, as they'll see it", text: Binding(get: { name }, set: { name = $0.take(40) })).padding(.top, 14)
                T("You are their…", .bodySm, c.inkMuted).padding(.top, 14).padding(.bottom, 6)
                FlowLayout { ForEach(["Partner", "Friend", "Family", "Other"], id: \.self) { r in Chip(r, selected: relationship == r) { relationship = r } } }
                if let error { Notice("That didn't work", error, kind: .medical).padding(.top, 12) }
                TButton("Connect", loading: busy, fullWidth: true) { accept() }.padding(.top, 20)
            }
        }
        .onAppear { if name.isEmpty { name = model.settings.displayName } }
    }

    private func accept() {
        if let v = Validation.inviteCode(code) { error = v; return }
        busy = true
        Task {
            do {
                try await model.connect.acceptInvite(code, myName: name, relationship: relationship)
                model.carePlay.unlock(.CONNECTED)
                model.showToast("Connected")
                model.pop()
            } catch { self.error = error.localizedDescription }
            busy = false
        }
    }
}

/// One trusted person: what they can see, and disconnecting.
struct PersonView: View {
    let id: String
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c
    @State private var confirm = false

    var body: some View {
        let p = model.connect.myPeople.first { $0.id == id }
        ScreenScroll(bottomPad: 40) {
            AppBar(p?.memberName ?? "Trusted person", onBack: { model.pop() })
            if let p {
                HStack(spacing: 12) {
                    Avatar(name: p.memberName, size: 56)
                    VStack(alignment: .leading) {
                        T(p.memberName, .headline)
                        T((p.relationship.isBlank ? "Trusted person" : p.relationship) + " · connected", .caption, c.inkMuted)
                    }
                }
                .padding(.vertical, 12)
                GroupTitle("\(p.memberName) can see").padding(.top, 8)
                SettingRow(title: "Period coming up", sub: "A heads-up 2 days before", isOn: p.permissions.periodSoon) { v in update(p) { $0.periodSoon = v } }
                SettingRow(title: "Period started", sub: "With tips on how to help", isOn: p.permissions.periodStarted) { v in update(p) { $0.periodStarted = v } }
                SettingRow(title: "Rough-day signal", sub: "When you send an SOS", isOn: p.permissions.roughDay) { v in update(p) { $0.roughDay = v } }
                SettingRow(title: "Cycle phase", sub: "Never symptoms, mood or notes", isOn: p.permissions.phase, last: true) { v in update(p) { $0.phase = v } }
                T("Changes apply straight away. Turning something off removes it from their view.", .caption, c.inkFaint).padding(.top, 8)
                if !confirm {
                    TButton("Disconnect \(p.memberName)", kind: .text) { confirm = true }.padding(.top, 20)
                } else {
                    HStack(spacing: 10) {
                        TButton("Keep", kind: .quiet, fullWidth: true) { confirm = false }
                        TButton("Disconnect", kind: .danger, fullWidth: true) {
                            Task { try? await model.connect.disconnect(p); model.showToast("Disconnected"); model.pop() }
                        }
                    }
                    .padding(.top, 16)
                }
            } else if model.connect.loaded {
                EmptyState(title: "This connection has ended", message: "They may have disconnected.")
            } else {
                ProgressView().frame(maxWidth: .infinity).padding(.top, 40)
            }
        }
        .onAppear { model.carePlay.unlock(.CONNECTED) }
    }

    private func update(_ p: Connection, _ change: (inout SharePermissions) -> Void) {
        var perms = p.permissions
        change(&perms)
        Task {
            do { try await model.connect.updatePermissions(p, perms, prediction: model.snapshot.prediction) }
            catch { model.showToast(error.localizedDescription) }
        }
    }
}

/// What a member sees for someone who shares with them: one status line, how to help, replies.
struct PartnerView: View {
    let id: String
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c
    @State private var status: SharedStatus?
    @State private var listener: ListenerRegistration?
    @State private var confirm = false

    var body: some View {
        let conn = model.connect.sharingWithMe.first { $0.id == id }
        let name = conn?.ownerName ?? status?.ownerName ?? "Your person"
        let today = Dates.today()
        let started = status?.periodStartedOn
        let soon = status?.periodSoonOn
        let rough = status?.roughDayAt.map { Dates.epochDay(ofMillis: $0) == today } ?? false
        ScreenScroll(bottomPad: 40) {
            AppBar(onBack: { model.pop() })
            if model.connect.loaded && conn == nil {
                EmptyState(title: "This connection has ended", message: "They may have disconnected.")
            } else {
                HStack(spacing: 12) {
                    Mascot(started != nil || rough ? .cramps : .warm, size: 44)
                    VStack(alignment: .leading) { Overline("Shared by \(name)"); T("Today", .subtitle) }
                }
                let headline: String = {
                    if let s = started { return s == today ? "\(name)'s period started today." : "\(name) is on day \(today - s + 1) of their period." }
                    if let s = soon { return "\(name)'s period may start \(s - today == 0 ? "today" : "in \(s - today) days")." }
                    if let ph = status?.phase { return "\(name) is in the \(ph.lowercased())." }
                    return "Nothing to share right now."
                }()
                T(headline, .display).padding(.top, 22)
                if rough { T("They sent a rough-day signal\(status?.lastNote.map { ": “\($0)”" } ?? ".")", .body, c.inkMuted).padding(.top, 8) }
                if started != nil || rough || soon != nil {
                    GroupTitle("Ways to help today").padding(.top, 22)
                    ListRow("Warm up a heat pack", sub: "Heat helps cramps more than people expect", icon: Icons.heat)
                    ListRow("Take care of dinner", sub: "Energy is often lowest on day 1", icon: Icons.food)
                    ListRow("Ask, don't guess", sub: "“Company or quiet?” works well", icon: Icons.ask, last: true)
                }
                GroupTitle("Send a little love").padding(.top, 18).padding(.bottom, 8)
                FlowLayout {
                    ForEach(["Thinking of you", "On my way with snacks", "Want company or quiet?"], id: \.self) { m in
                        Chip(m) {
                            guard let cn = conn else { return }
                            Task {
                                do { try await model.connect.sendReply(cn, message: m); model.showToast("Sent") }
                                catch { model.showToast(error.localizedDescription) }
                            }
                        }
                    }
                }
                T("You only see what \(name) chooses to share.", .caption, c.inkFaint).padding(.top, 16)
                TButton("Take the couple quiz", kind: .text, icon: Icons.quiz) { model.push(.quiz("couple_support")) }
                if let cn = conn {
                    if !confirm { TButton("Stop receiving updates", kind: .text) { confirm = true } }
                    else {
                        HStack(spacing: 10) {
                            TButton("Keep", kind: .quiet, fullWidth: true) { confirm = false }
                            TButton("Disconnect", kind: .danger, fullWidth: true) { Task { try? await model.connect.disconnect(cn); model.pop() } }
                        }
                    }
                }
            }
        }
        .onAppear { listener = model.connect.sharedStatus(id) { s in status = s } }
        .onDisappear { listener?.remove(); listener = nil }
    }
}
