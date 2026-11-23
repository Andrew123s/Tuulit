import SwiftUI
import TuulitCore

/// Care: Comfort, Play and Library in one tab, each with its own coloured hero.
struct CareView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c

    var body: some View {
        @Bindable var m = model
        let snap = model.snapshot
        let crampy = snap.todayLog?.symptoms["Cramps"] != nil
        let head: (String, String, Color, MascotMood) = {
            switch model.careSegment {
            case 0: return (crampy ? "You logged cramps today" : "Tuulit Care", CareContent.heroHeadline(snap.todayLog), c.peachSoft, crampy ? .cramps : .chill)
            case 1: return ("Tuulit Play", "Two minutes of not thinking about it.", c.bananaSoft, .happy)
            default: return ("Your library", "What works for you.", c.roseSoft, .warm)
            }
        }()
        ScreenScroll {
            ZStack(alignment: .leading) {
                head.2
                Blob(seed: 4).fill(c.oat.opacity(0.6)).frame(width: 150, height: 150).frame(maxWidth: .infinity, alignment: .trailing).offset(x: 30)
                Mascot(head.3, size: 92).frame(maxWidth: .infinity, alignment: .trailing).padding(.trailing, 18)
                VStack(alignment: .leading, spacing: 6) {
                    Overline(head.0, c.toffee)
                    T(head.1, .headline).accessibilityAddTraits(.isHeader)
                }
                .padding(.leading, 22).padding(.vertical, 22).padding(.trailing, 120)
            }
            .clipShape(RoundedRectangle(cornerRadius: Radius.xl, style: .continuous))
            .padding(.top, 14)
            Segmented(options: ["Comfort", "Play", "Library"], selection: $m.careSegment).padding(.vertical, 16)
            switch model.careSegment {
            case 0: ComfortSection()
            case 1: PlayHub()
            default: LibrarySection()
            }
        }
    }
}

private enum CareCat: String, CaseIterable {
    case all = "All", cramps = "Cramps", relax = "Relaxation", sleep = "Sleep", move = "Movement", mood = "Mood", breath = "Breathing"
    func matches(_ s: CareSession) -> Bool {
        switch self {
        case .all: return true
        case .cramps: return s.category == "Cramps"
        case .relax: return s.category == "Calm" || s.category == "Sleep"
        case .sleep: return s.category == "Sleep"
        case .move: return s.id == "gentle_stretches"
        case .mood: return s.category == "Mood"
        case .breath: return s.breathing != nil
        }
    }
}

extension Palette {
    func sessionColor(_ s: CareSession) -> Color {
        switch s.category { case "Cramps": return periodSoft; case "Calm", "Sleep": return fertileSoft; case "Mood": return roseSoft; default: return peachSoft }
    }
}

private struct ComfortSection: View {
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c
    @State private var cat = CareCat.all
    @State private var flagsOpen = false

    var body: some View {
        let snap = model.snapshot
        let hero = CareContent.heroFor(snap.todayLog, snap.prediction.currentPhase)
        let _ = model.dataVersion
        let mine = model.carePlay.comfortItems.filter { $0.kind == "comfort" }.sorted { $0.helpedCount > $1.helpedCount }.prefix(3)
        VStack(alignment: .leading, spacing: 0) {
            // Featured experience
            VStack(alignment: .leading, spacing: 0) {
                HStack(alignment: .center) {
                    VStack(alignment: .leading, spacing: 4) {
                        Overline("Made for today · \(hero.minutesLabel)", c.toffee)
                        T(hero.title, .display)
                        T(hero.summary, .bodySm, c.inkMuted).padding(.top, 2)
                    }
                    Spacer(minLength: 8)
                    Illustration(kind: illoFor(hero.icon), size: 110, bg: c.oat)
                }
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(Array(hero.steps.prefix(3).enumerated()), id: \.offset) { i, st in
                            T("\(i + 1)  \(st.title)", .caption, lines: 1).padding(.horizontal, 10).padding(.vertical, 6).background(c.oat, in: Capsule())
                        }
                    }
                }
                .padding(.top, 14)
                TButton("Start", icon: Icons.breath, fullWidth: true) { model.push(.session(hero.id)) }.padding(.top, 16)
            }
            .padding(20)
            .background(c.sessionColor(hero), in: RoundedRectangle(cornerRadius: Radius.xl, style: .continuous))

            T("Find what you need", .title).padding(.top, 26).padding(.bottom, 10)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) { ForEach(CareCat.allCases, id: \.self) { k in Chip(k.rawValue, selected: cat == k) { cat = k } } }
            }
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    ForEach(CareContent.all.filter { cat.matches($0) }) { s in
                        Button { model.push(.session(s.id)) } label: {
                            VStack(alignment: .leading, spacing: 4) {
                                Illustration(kind: illoFor(s.icon), size: 72, bg: c.oat)
                                T(s.title, .title, lines: 2).padding(.top, 8)
                                T("\(s.category) · \(s.minutesLabel)", .caption, c.inkMuted)
                                Spacer(minLength: 0)
                            }
                            .padding(16)
                            .frame(width: 168, height: 222, alignment: .topLeading)
                            .background(c.sessionColor(s), in: RoundedRectangle(cornerRadius: Radius.lg, style: .continuous))
                        }
                        .buttonStyle(PressScale())
                        .accessibilityLabel("\(s.title), \(s.category), \(s.minutesLabel)")
                    }
                }
            }
            .padding(.top, 14)
            .animation(.easeOut(duration: 0.2), value: cat)

            HStack {
                T("Your comfort library", .title)
                Spacer()
                TButton("Open", kind: .text) { model.careSegment = 2 }
            }
            .padding(.top, 26)
            if mine.isEmpty {
                T("Finish a session or add what helps you, and it shows up here.", .bodySm, c.inkMuted).padding(.top, 4)
            } else {
                VStack(spacing: 0) {
                    ForEach(Array(mine.enumerated()), id: \.element.id) { i, item in
                        ListRow(item.title, sub: item.helpedCount > 0 ? "Helped \(item.helpedCount) \(item.helpedCount == 1 ? "time" : "times")" : (item.detail.isBlank ? nil : item.detail),
                                last: i == mine.count - 1, action: item.sessionId.map { sid in { model.push(.session(sid)) } },
                                leading: { Illustration(kind: illoFor(item.icon), size: 40, bg: c.peachSoft) })
                    }
                }
                .padding(.horizontal, 16)
                .background(c.oat, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
                .padding(.top, 6)
            }

            Button { withAnimation { flagsOpen.toggle() } } label: {
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        TIcon(Icons.alert, color: c.alert)
                        T("When to see a doctor", .subtitle)
                        Spacer()
                        TIcon(Icons.chevD, size: 20).rotationEffect(.degrees(flagsOpen ? 180 : 0))
                    }
                    if flagsOpen {
                        ForEach(CareContent.redFlags, id: \.self) { f in T("•  \(f)", .bodySm) }
                        T("If any of these happen, contact a clinician or emergency services.", .caption, c.inkMuted).padding(.top, 2)
                    }
                }
                .padding(16)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(c.alertSoft, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
            }
            .buttonStyle(.plain)
            .padding(.top, 20)
            .accessibilityValue(flagsOpen ? "Expanded" : "Collapsed")
        }
    }
}

private struct LibrarySection: View {
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c
    @State private var adding: String?
    @State private var note = ""
    @State private var noteLoaded = false

    var body: some View {
        let _ = model.dataVersion
        let items = model.carePlay.comfortItems
        let comfort = items.filter { $0.kind == "comfort" }
        let kit = items.filter { $0.kind == "kit" }
        VStack(alignment: .leading, spacing: 0) {
            GroupTitle("My comfort list")
            if comfort.isEmpty { T("Finish a Care session or add what helps you, and it appears here.", .bodySm, c.inkMuted).padding(.vertical, 8) }
            ForEach(comfort) { i in
                let sub = [i.detail.isBlank ? nil : i.detail, i.helpedCount > 0 ? "Helped \(i.helpedCount) \(i.helpedCount == 1 ? "time" : "times")" : nil].compactMap { $0 }.joined(separator: " · ")
                ListRow(i.title, sub: sub.isEmpty ? nil : sub, icon: Icons.named(i.icon), action: i.sessionId.map { sid in { model.push(.session(sid)) } }, trailing: {
                    IconBtn(Icons.trash, "Remove \(i.title)") { model.carePlay.deleteComfortItem(id: i.id); model.showToast("Removed") }
                })
            }
            TButton("Add something that helps", kind: .text, icon: Icons.plus) { adding = "comfort" }
            GroupTitle("Period survival kit").padding(.top, 18)
            ForEach(kit) { i in
                HStack(spacing: 12) {
                    Button { model.carePlay.toggleKitItem(id: i.id) } label: {
                        HStack(spacing: 12) {
                            TIcon(i.checked ? Icons.check : Icons.kit, color: i.checked ? c.ink : c.inkFaint)
                            T(i.title, .body).strikethrough(i.checked, color: c.inkFaint)
                            Spacer()
                        }
                        .frame(minHeight: 52).contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(i.title)
                    .accessibilityValue(i.checked ? "Packed" : "Not packed")
                    .accessibilityAddTraits(.isToggle)
                    IconBtn(Icons.trash, "Remove \(i.title)") { model.carePlay.deleteComfortItem(id: i.id) }
                }
                Hairline()
            }
            TButton("Add an item", kind: .text, icon: Icons.plus) { adding = "kit" }
            GroupTitle("Sleep routine").padding(.top, 18)
            Field(label: "", text: Binding(get: { note }, set: { note = $0.take(500) }), placeholder: "What helps you wind down?", multiline: true)
            if note != model.settings.sleepNote {
                TButton("Save routine", kind: .text) { model.settingsStore.setSleepNote(note); model.showToast("Routine saved") }
            }
        }
        .onAppear { if !noteLoaded { note = model.settings.sleepNote; noteLoaded = true } }
        .sheet(item: Binding(get: { adding.map { AddKind(kind: $0) } }, set: { adding = $0?.kind })) { k in
            AddComfortSheet(kind: k.kind).presentationDetents([.medium])
        }
    }
}

private struct AddKind: Identifiable { let kind: String; var id: String { kind } }

private struct AddComfortSheet: View {
    let kind: String
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c
    @Environment(\.dismiss) private var dismiss
    @State private var title = ""
    @State private var detail = ""
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            T(kind == "kit" ? "Add to your kit" : "Add something that helps", .headline).padding(.top, 24)
            Field(label: "Name", text: Binding(get: { title }, set: { title = $0.take(80) }),
                  placeholder: kind == "kit" ? "e.g. spare tampons" : "e.g. hot water bottle and ginger tea")
            if kind == "comfort" {
                Field(label: "When it helps (optional)", text: Binding(get: { detail }, set: { detail = $0.take(160) }), placeholder: "e.g. day 1–2 cramps")
            }
            TButton("Add", enabled: !title.isBlank, fullWidth: true) {
                model.carePlay.addComfortItem(kind: kind, title: title, detail: detail, icon: kind == "kit" ? "kit" : "care")
                dismiss(); model.showToast("Added")
            }
            Spacer()
        }
        .padding(.horizontal, 20)
        .background(c.cream)
    }
}

/// Full-screen guided session: breathing circle, step timer, pause, next and finish.
struct SessionView: View {
    let id: String
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c
    @Environment(\.tuulitReducedMotion) private var reduced
    @State private var stepIndex = 0
    @State private var remaining = 0
    @State private var paused = false
    @State private var done = false
    @State private var phaseLabel = "Breathe in"
    @State private var circle: CGFloat = 0.72
    @State private var started = false
    @State private var unlockedTitle: String?

    var body: some View {
        if let session = CareContent.byId(id) { content(session) }
        else { EmptyState(title: "Session not found", message: "It may have been updated.") }
    }

    private func content(_ session: CareSession) -> some View {
        let elapsed = session.steps.prefix(stepIndex).reduce(0) { $0 + $1.seconds } + (session.steps[stepIndex].seconds - remaining)
        return VStack(spacing: 0) {
            HStack {
                IconBtn(Icons.close, "Close") {
                    if !done { model.carePlay.completeSession(session.id, fully: false) }
                    model.pop()
                }
                Spacer()
                T(done ? "Done" : String(format: "%d:%02d of %@", elapsed / 60, elapsed % 60, session.minutesLabel), .caption, c.inkMuted)
                Spacer()
                Color.clear.frame(width: 44, height: 44)
            }
            .padding(.top, 6)
            Spacer()
            if done {
                Mascot(.happy, size: 140)
                T("Well done.", .display).padding(.top, 16)
                T("\(session.title) is now in your Comfort Library.", .body, c.inkMuted, align: .center).padding(.top, 8)
            } else {
                if let pattern = session.breathing {
                    ZStack {
                        Circle().fill(c.bananaSoft).frame(width: 240, height: 240).scaleEffect(reduced ? 1 : circle)
                        Circle().fill(c.banana).frame(width: 160, height: 160).scaleEffect(reduced ? 1 : circle)
                        T(phaseLabel, .headline, c.onBanana)
                    }
                    .frame(width: 240, height: 240)
                    .accessibilityElement(children: .combine)
                    .accessibilityLabel(phaseLabel)
                    .accessibilityAddTraits(.updatesFrequently)
                    .padding(.bottom, 28)
                    .task(id: paused) { await breathe(pattern) }
                }
                let step = session.steps[stepIndex]
                T(step.title, .title, align: .center)
                T(step.detail, .bodySm, c.inkMuted, align: .center).padding(.top, 6).padding(.horizontal, 12)
                if let pattern = session.breathing { T(pattern.label, .caption, c.inkFaint).padding(.top, 10) }
            }
            Spacer()
            if !done { StepProgress(steps: session.steps.count, done: stepIndex + 1).padding(.bottom, 20) }
            if done {
                TButton("Back to Care", fullWidth: true) { model.pop() }
            } else {
                HStack(spacing: 10) {
                    TButton(paused ? "Resume" : "Pause", kind: .quiet, fullWidth: true) { paused.toggle() }
                    TButton(stepIndex < session.steps.count - 1 ? "Next step" : "Finish", kind: .quiet, fullWidth: true) { next(session) }
                }
            }
        }
        .padding(.horizontal, 20).padding(.bottom, 20)
        .background(c.cream.ignoresSafeArea())
        .onAppear { if !started { remaining = session.steps[0].seconds; started = true } }
        .task(id: "\(paused)-\(done)-\(stepIndex)") { await tick(session) }
        .onChange(of: done) { _, d in
            if d {
                let unlocked = model.carePlay.completeSession(session.id, fully: true)
                model.showToast(unlocked.first.map { "\($0.title) · added to your library" } ?? "Nice. Saved to your library")
            }
        }
        .sensoryFeedback(.success, trigger: done)
        .sensoryFeedback(.selection, trigger: stepIndex)
    }

    private func next(_ s: CareSession) {
        if stepIndex < s.steps.count - 1 { stepIndex += 1; remaining = s.steps[stepIndex].seconds } else { done = true }
    }

    private func tick(_ s: CareSession) async {
        while !paused && !done && !Task.isCancelled {
            try? await Task.sleep(nanoseconds: 1_000_000_000)
            if Task.isCancelled || paused || done { return }
            if remaining > 1 { remaining -= 1 } else { next(s); return }
        }
    }

    private func breathe(_ p: BreathingPattern) async {
        while !paused && !done && !Task.isCancelled {
            phaseLabel = "Breathe in"
            withAnimation(reduced ? nil : .linear(duration: Double(p.inhale))) { circle = 1 }
            try? await Task.sleep(nanoseconds: UInt64(p.inhale) * 1_000_000_000)
            if Task.isCancelled { return }
            if p.hold > 0 { phaseLabel = "Hold"; try? await Task.sleep(nanoseconds: UInt64(p.hold) * 1_000_000_000) }
            if Task.isCancelled { return }
            phaseLabel = "Breathe out"
            withAnimation(reduced ? nil : .linear(duration: Double(p.exhale))) { circle = 0.72 }
            try? await Task.sleep(nanoseconds: UInt64(p.exhale) * 1_000_000_000)
        }
    }
}
