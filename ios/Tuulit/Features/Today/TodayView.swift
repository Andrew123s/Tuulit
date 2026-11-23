import SwiftUI
import TuulitCore

/// Today: greeting, phase hero with the cycle ring, quick actions, mood check-in, a suggestion,
/// "What would help right now?", the optional account prompt and Tuulit SOS.
struct TodayView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c

    var body: some View {
        let snap = model.snapshot
        let s = snap.settings
        if s.notTheDayOn == snap.today {
            NotTheDayView()
        } else {
            content(snap)
        }
    }

    private func content(_ snap: CycleSnapshot) -> some View {
        let p = snap.prediction
        let ph = c.phase(p.hasRecordedCycles ? p.currentPhase : .UNKNOWN)
        let hour = Dates.hourNow()
        let timeGreeting = (5...11).contains(hour) ? "Good morning" : (12...17).contains(hour) ? "Good afternoon" : (18...22).contains(hour) ? "Good evening" : "Hello"
        let subline: String = {
            if p.isCurrentlyMenstruating { return "Take it slow today. You're doing great." }
            if snap.todayLog == nil { return "A little check-in with yourself?" }
            if p.currentPhase == .FOLLICULAR { return "Energy often lifts around now." }
            if p.currentPhase == .LUTEAL { return "Be extra gentle with yourself this week." }
            return "Thanks for checking in today."
        }()
        let name = model.name

        return ScreenScroll {
            HStack(alignment: .center, spacing: 12) {
                VStack(alignment: .leading, spacing: 2) {
                    T(name.isBlank ? "\(timeGreeting), sunshine" : "\(timeGreeting), \(name)", .headline).accessibilityAddTraits(.isHeader)
                    T(subline, .bodySm, c.inkMuted)
                }
                Spacer()
                Avatar(name: name.isBlank ? "You" : name, size: 44) { model.push(.you) }
            }
            .padding(.top, 10)

            hero(snap, ph)
            quickActions
            moodCard(snap)
            if snap.settings.suggestionDismissedOn != snap.today { suggestion(snap) }
            needSomething
            if model.auth.state == .signedOut && !snap.cycles.isEmpty && !snap.settings.accountPromptDismissed {
                ListRow("Back up with an account", sub: "Restore Tuulit on a new phone. Optional.", icon: Icons.shield, chevron: true, last: true) { model.push(.auth(.register)) }
                    .padding(.top, 12)
                TButton("No thanks", kind: .text) { model.settingsStore.dismissAccountPrompt() }
            }
            TButton("Tuulit SOS", kind: .text, icon: Icons.sos) { model.sheet = .sos }.padding(.top, 8)
        }
    }

    private func hero(_ snap: CycleSnapshot, _ ph: PhaseStyle) -> some View {
        let p = snap.prediction
        let hero = TodayHeroBuilder.build(p, today: snap.today, format: Dates.short)
        let showStart = !p.isCurrentlyMenstruating && (!p.hasRecordedCycles || (p.nextPeriodStartEpochDay ?? Int.max) - snap.today <= 3)
        return ZStack(alignment: .topLeading) {
            RoundedRectangle(cornerRadius: Radius.xl, style: .continuous).fill(ph.soft)
            GeometryReader { g in
                Blob(seed: 1).fill(ph.fill.opacity(0.35)).frame(width: 170, height: 170).position(x: g.size.width - 35, y: 45)
                Blob(seed: 3).fill(c.oat.opacity(0.55)).frame(width: 120, height: 120).position(x: 20, y: g.size.height - 20)
            }
            .clipShape(RoundedRectangle(cornerRadius: Radius.xl, style: .continuous))
            .accessibilityHidden(true)
            Button { model.sheet = .bananaMode } label: { Mascot(snap.settings.bananaMode.mood, size: 52) }
                .padding(14)
                .accessibilityLabel("Change Banana Mode")
            VStack(spacing: 0) {
                if p.hasRecordedCycles {
                    CycleRing(prediction: p, cycleStart: snap.currentCycleStart, today: snap.today, size: 236) {
                        VStack(spacing: 0) {
                            T(hero.lead.hasPrefix("Your ") ? String(hero.lead.dropFirst(5)).capitalizedFirst : hero.lead, .caption, c.inkMuted, align: .center)
                            if let n = hero.number {
                                T(n, .numeral).scaleEffect(1.1)
                                T(hero.unit.trimmed, .subtitle, c.inkMuted)
                            } else {
                                T(hero.unit, .display)
                            }
                        }
                        .frame(maxWidth: 150)
                    }
                    HStack(spacing: 8) {
                        Circle().fill(ph.fill).frame(width: 10, height: 10)
                        T("\(ph.label) phase · day \(p.currentCycleDay ?? 1)", .label)
                    }
                    .padding(.horizontal, 14).padding(.vertical, 8)
                    .background(c.oat, in: Capsule())
                    .padding(.top, 14)
                    if let next = p.nextPeriodStartEpochDay, !p.isCurrentlyMenstruating {
                        T(p.isIrregularEstimate ? "Next period in \(p.nextPeriodRangeMinDays ?? 0)–\(p.nextPeriodRangeMaxDays ?? 0) days · estimate"
                                                : "Next period around \(Dates.short(next)) · estimate", .caption, c.inkMuted, align: .center)
                            .padding(.top, 8)
                    }
                } else {
                    Mascot(.happy, size: 120).padding(.top, 18)
                    T("Let's get to know your cycle.", .display, align: .center).padding(.top, 8)
                    T("Log your next period and your cycle ring fills in.", .bodySm, c.inkMuted, align: .center).padding(.top, 6)
                }
                if showStart {
                    TButton("My period started", kind: .banana, icon: Icons.drop) { periodStarted(snap.today) }
                        .padding(.top, 16)
                        .accessibilityIdentifier("today.periodStarted")
                }
                if p.isCurrentlyMenstruating {
                    TButton("Log today's flow", icon: Icons.drop) { model.openLog(snap.today) }.padding(.top, 16)
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 22).padding(.horizontal, 20)
        }
        .padding(.top, 18)
    }

    private func periodStarted(_ day: Int) {
        model.tracking.logPeriodStart(day)
        model.carePlay.unlock(.FIRST_PERIOD)
        model.showToast("Period logged") {
            if var l = model.tracking.dailyLog(day) { l.flow = .NONE; model.tracking.saveDailyLog(l) }
        }
    }

    private var quickActions: some View {
        HStack {
            QuickAction(label: "Log period", icon: Icons.drop, bg: c.periodSoft, tint: c.periodDeep) { model.openLog(model.snapshot.today) }
            Spacer(minLength: 4)
            QuickAction(label: "Symptoms", icon: Icons.spark, bg: c.fertileSoft, tint: c.lilacDeep) { model.openLog(model.snapshot.today) }
            Spacer(minLength: 4)
            QuickAction(label: "Feel better", icon: Icons.care, bg: c.roseSoft, tint: c.periodDeep) { model.sheet = .needSomething }
            Spacer(minLength: 4)
            QuickAction(label: "Calendar", icon: Icons.cycle, bg: c.bananaSoft, tint: c.toffee) { model.tab = .cycle }
        }
        .padding(.top, 18)
    }

    private func moodCard(_ snap: CycleSnapshot) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                T("How are you feeling?", .title)
                Spacer()
                TButton("Full log", kind: .text) { model.openLog(snap.today) }
            }
            HStack {
                ForEach(1...5, id: \.self) { score in
                    let on = snap.todayLog?.moodScore == score
                    Button { setMood(snap.today, score) } label: {
                        VStack(spacing: 4) {
                            MoodBlob(score: score, selected: on, size: 54)
                            T(TrackingCatalog.moodFaceLabels[score - 1], .caption, on ? c.ink : c.inkMuted)
                        }
                        .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(PressScale())
                    .accessibilityLabel(TrackingCatalog.moodFaceLabels[score - 1])
                    .accessibilityAddTraits(on ? [.isSelected] : [])
                }
            }
        }
        .padding(18)
        .background(c.oat, in: RoundedRectangle(cornerRadius: Radius.lg, style: .continuous))
        .padding(.top, 24)
        .sensoryFeedback(.selection, trigger: snap.todayLog?.moodScore)
    }

    private func setMood(_ day: Int, _ score: Int) {
        var l = model.tracking.dailyLog(day) ?? DailyLog(epochDay: day)
        l.moodScore = l.moodScore == score ? 0 : score
        model.tracking.saveDailyLog(l)
        model.carePlay.unlock(.FIRST_LOG)
    }

    private func suggestion(_ snap: CycleSnapshot) -> some View {
        let sug = SuggestionEngine.forToday(snap.prediction, snap.todayLog, hourOfDay: Dates.hourNow())
        let illo: Illo = {
            if sug.sessionId?.contains("sleep") == true { return .moon }
            if sug.sessionId?.contains("heat") == true { return .heat }
            if sug.sessionId?.contains("stretch") == true { return .stretch }
            if sug.sessionId?.contains("breath") == true { return .breath }
            if sug.route?.hasPrefix("care/play") == true { return .spark }
            return .sun
        }()
        let open = {
            if let id = sug.sessionId { model.push(.session(id)) }
            else if let r = sug.route { model.go(r) }
        }
        return Group {
            HStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 2) {
                    Overline(sug.kicker, c.toffee)
                    T(sug.title, .title).padding(.top, 2)
                    T(sug.body, .bodySm, c.inkMuted)
                    Button("Not today") { model.settingsStore.dismissSuggestion(snap.today) }
                        .tStyle(.label, c.toffee).padding(.top, 10)
                }
                Spacer(minLength: 0)
                Illustration(kind: illo, size: 84, bg: c.oat)
            }
            .padding(18)
            .background(c.bananaSoft, in: RoundedRectangle(cornerRadius: Radius.lg, style: .continuous))
            .contentShape(Rectangle())
            .onTapGesture(perform: open)
        }
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isButton)
        .accessibilityAction(named: "Not today") { model.settingsStore.dismissSuggestion(snap.today) }
        .padding(.top, 16)
    }

    private var needSomething: some View {
        Button { model.sheet = .needSomething } label: {
            HStack(spacing: 14) {
                Illustration(kind: .heart, size: 56, bg: c.oat)
                VStack(alignment: .leading, spacing: 2) {
                    T("What would help right now?", .subtitle)
                    T("Pain, mood, rest or a distraction", .bodySm, c.inkMuted)
                }
                Spacer()
                TIcon(Icons.chevR, size: 20)
            }
            .padding(18)
            .background(c.roseSoft, in: RoundedRectangle(cornerRadius: Radius.lg, style: .continuous))
        }
        .buttonStyle(PressScale())
        .padding(.top, 14)
    }
}

private struct QuickAction: View {
    let label: String
    let icon: IconDef
    let bg: Color
    let tint: Color
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            VStack(spacing: 6) {
                TIcon(icon, size: 26, color: tint).frame(width: 58, height: 58).background(bg, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
                T(label, .caption, lines: 1).minimumScaleFactor(0.8)
            }
            .frame(width: 78)
        }
        .buttonStyle(PressScale())
        .accessibilityLabel(label)
    }
}

/// Today Is Not the Day: no tab bar, no numbers, four gentle actions. Ends at midnight.
struct NotTheDayView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c
    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack { Spacer(); TButton("Back to normal view", kind: .text) { model.settingsStore.setNotTheDay(nil) } }
            Mascot(.sleepy, size: 128).padding(.top, 28)
            T("Take it slow today.", .display).padding(.top, 16)
            T("Everything else can wait. Here's all you need.", .body, c.inkMuted).padding(.top, 8)
            Spacer()
            VStack(spacing: 10) {
                row("Log how I feel", Icons.edit) { model.openLog(Dates.today()) }
                row("Help me relax", Icons.breath) { model.push(.session("slow_breathing")) }
                row("Distract me", Icons.play) { model.go("care/play") }
                row("Tell my person", Icons.sos) { model.sheet = .sos }
            }
            .padding(.bottom, 24)
        }
        .padding(.horizontal, 20)
        .background(c.cream.ignoresSafeArea())
    }
    private func row(_ label: String, _ icon: IconDef, _ action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 12) { TIcon(icon); T(label, .label); Spacer() }
                .padding(.horizontal, 22).frame(minHeight: 60)
                .background(c.sand, in: Capsule())
        }
        .buttonStyle(PressScale())
    }
}

extension String {
    var capitalizedFirst: String { prefix(1).uppercased() + dropFirst() }
}
