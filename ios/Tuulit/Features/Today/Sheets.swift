import SwiftUI
import TuulitCore

private struct Need: Identifiable, Equatable {
    let label: String, icon: IconDef, kicker: String, title: String, body: String, primary: String, secondary: String?
    var id: String { label }
}

private let needs: [Need] = [
    Need(label: "I'm in pain", icon: Icons.pain, kicker: "For pain, try this first", title: "Heat, then slow breathing",
         body: "A warm compress on your lower belly with 4-6 breathing for 5 minutes.", primary: "Start 5 min", secondary: "Log cramps"),
    Need(label: "Exhausted", icon: Icons.moon, kicker: "When you're running on empty", title: "Make today a quiet day",
         body: "Today Is Not the Day hides everything except four gentle actions until midnight.", primary: "Quiet mode on", secondary: nil),
    Need(label: "Feeling low", icon: Icons.care, kicker: "For a low moment", title: "Two-minute mood reset",
         body: "Name the feeling, ground yourself, choose one kind thing.", primary: "Start 2 min", secondary: "Log my mood"),
    Need(label: "On edge", icon: Icons.breath, kicker: "To take the edge off", title: "Box breathing",
         body: "In 4, hold 4, out 4. Two minutes.", primary: "Start 2 min", secondary: nil),
    Need(label: "Bored", icon: Icons.play, kicker: "Two minutes off", title: "Play something",
         body: "Banana Catch, Word Puzzle and six more. Personal bests, never streaks.", primary: "Open Play", secondary: nil),
    Need(label: "Can't sleep", icon: Icons.moon, kicker: "For tonight", title: "Sleep wind-down",
         body: "Ten quiet minutes: body scan and 4-7-8 breathing.", primary: "Start 10 min", secondary: nil),
    Need(label: "A question", icon: Icons.ask, kicker: "Ask Tuulit", title: "Clear answers about your body",
         body: "General information, never a diagnosis.", primary: "Open Ask", secondary: nil),
    Need(label: "Tell my person", icon: Icons.sos, kicker: "", title: "", body: "", primary: "", secondary: nil),
]

/// I Need Something: triage in one sheet. Picking a feeling reveals one gentle next step.
struct NeedSomethingSheet: View {
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c
    @Environment(\.dismiss) private var dismiss
    @State private var picked: Need?

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                T("What would help right now?", .headline).padding(.top, 24)
                T("Pick the closest. Tuulit takes you to one gentle next step.", .bodySm, c.inkMuted).padding(.top, 6).padding(.bottom, 18)
                LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 8), count: 4), spacing: 8) {
                    ForEach(needs) { n in
                        let on = picked == n
                        Button {
                            if n.label == "Tell my person" { model.sheet = .sos } else { picked = n }
                        } label: {
                            VStack(spacing: 6) {
                                TIcon(n.icon, size: 22, color: on ? c.periodDeep : c.inkMuted)
                                T(n.label, .caption, align: .center, lines: 2).minimumScaleFactor(0.85)
                            }
                            .frame(maxWidth: .infinity, minHeight: 72)
                            .padding(.horizontal, 4)
                            .background(on ? c.peach : c.sand, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
                        }
                        .buttonStyle(PressScale())
                        .accessibilityAddTraits(on ? [.isSelected] : [])
                    }
                }
                if let n = picked {
                    Hairline().padding(.top, 12).padding(.bottom, 16)
                    Overline(n.kicker, c.toffee)
                    T(n.title, .title).padding(.vertical, 4)
                    T(n.body, .bodySm, c.inkMuted).padding(.bottom, 16)
                    HStack(spacing: 10) {
                        if let sec = n.secondary {
                            TButton(sec, kind: .quiet, fullWidth: true) { dismiss(); model.openLog(model.snapshot.today) }
                        }
                        TButton(n.primary, fullWidth: true) { act(n) }
                    }
                }
            }
            .padding(.horizontal, 20).padding(.bottom, 24)
        }
        .background(c.cream)
        .sensoryFeedback(.selection, trigger: picked)
    }

    private func act(_ n: Need) {
        dismiss()
        switch n.label {
        case "I'm in pain": model.push(.session(CareContent.heatAndEase.id))
        case "Exhausted": model.settingsStore.setNotTheDay(Dates.today()); model.tab = .today
        case "Feeling low": model.push(.session(CareContent.moodReset.id))
        case "On edge": model.push(.session(CareContent.boxBreathing.id))
        case "Bored": model.go("care/play")
        case "Can't sleep": model.push(.session(CareContent.sleepWindDown.id))
        case "A question": model.go("ask")
        default: break
        }
    }
}

/// Tuulit SOS. Connected people with "Rough-day signal" on get a push via the Worker; otherwise
/// the message opens in the system share sheet. Nothing sends without a tap.
struct SOSSheet: View {
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c
    @Environment(\.dismiss) private var dismiss
    @State private var targetIndex = 0
    @State private var sending = false

    var body: some View {
        let eligible = model.connect.myPeople.filter { $0.permissions.roughDay }
        let person = eligible.isEmpty ? nil : eligible[targetIndex % eligible.count]
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                T("Send a quick message", .headline).padding(.top, 24)
                if let person {
                    HStack(spacing: 12) {
                        Avatar(name: person.memberName, size: 36)
                        VStack(alignment: .leading) {
                            T(person.memberName, .subtitle)
                            T(person.relationship.isBlank ? "Trusted person" : person.relationship, .caption, c.inkMuted)
                        }
                        Spacer()
                        if eligible.count > 1 { TButton("Change", kind: .text) { targetIndex += 1 } }
                    }
                    .padding(.top, 14).padding(.bottom, 6)
                } else {
                    T(model.auth.state.isSignedIn ? "No one has Rough-day signal switched on yet, so this opens your share sheet."
                                                  : "This opens your share sheet with the text ready. Connect a trusted person to send it in Tuulit.",
                      .bodySm, c.inkMuted).padding(.top, 8).padding(.bottom, 6)
                }
                ForEach(Array(model.settings.sosMessages.enumerated()), id: \.offset) { i, msg in
                    if let person {
                        ListRow("“\(msg)”", last: i == model.settings.sosMessages.count - 1, action: { send(person, msg) }, trailing: {
                            if sending { ProgressView() } else { TIcon(Icons.send, size: 20, color: c.inkMuted) }
                        })
                    } else {
                        ShareLink(item: msg) {
                            HStack { T("“\(msg)”", .body); Spacer(); TIcon(Icons.send, size: 20, color: c.inkMuted) }
                                .padding(.vertical, 14)
                        }
                        Hairline()
                    }
                }
                T(person.map { "Sends a notification to \($0.memberName)'s phone." } ?? "Nothing sends without you.", .caption, c.inkFaint).padding(.top, 14)
                TButton("Edit messages or people", kind: .text) { dismiss(); model.push(.connect) }
            }
            .padding(.horizontal, 20).padding(.bottom, 24)
        }
        .background(c.cream)
    }

    private func send(_ person: Connection, _ msg: String) {
        guard !sending else { return }
        sending = true
        Task {
            do {
                try await model.connect.sendSos(person, message: msg, prediction: model.snapshot.prediction)
                model.showToast("Sent to \(person.memberName)")
            } catch {
                model.showToast(error.localizedDescription)
            }
            sending = false
            dismiss()
        }
    }
}

struct BananaModeSheet: View {
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                T("Banana Mode", .headline).padding(.top, 24)
                T("Choose the mood your banana keeps you company in.", .bodySm, c.inkMuted).padding(.top, 6).padding(.bottom, 8)
                ForEach(BananaModePersona.allCases, id: \.self) { p in
                    ListRow(p.title, sub: p.tagline, action: { model.settingsStore.setBananaMode(p); dismiss() }, leading: {
                        Mascot(p.mood, size: 44)
                    })
                    .overlay(alignment: .trailing) { if model.settings.bananaMode == p { TIcon(Icons.check, size: 20).accessibilityLabel("Selected") } }
                }
                ListRow("Today is not the day", sub: "Hide everything but four gentle actions until midnight", icon: Icons.moon, last: true, action: {
                    model.settingsStore.setNotTheDay(Dates.today()); dismiss()
                })
            }
            .padding(.horizontal, 20).padding(.bottom, 24)
        }
        .background(c.cream)
    }
}
