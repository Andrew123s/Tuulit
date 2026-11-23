import SwiftUI
import TuulitCore

/// Onboarding from the approved design: welcome, last period, cycle basics, then privacy and
/// reminders. Signing in is optional and offered on the welcome screen.
struct OnboardingFlow: View {
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c
    @State private var step = 0
    @State private var lastStart: Int?
    @State private var cycleLen = 28
    @State private var periodLen = 5
    @State private var regularity = CycleRegularity.REGULAR
    @State private var signIn = false

    var body: some View {
        Group {
            switch step {
            case 0: welcome
            case 1: LastPeriodStep(selected: $lastStart, onBack: { step = 0 }, onNext: { step = 2 }, onSkip: { lastStart = nil; step = 2 })
            case 2: CycleBasicsStep(cycleLen: $cycleLen, periodLen: $periodLen, regularity: $regularity, onBack: { step = 1 }, onNext: { step = 3 })
            default: PrivacyStep(lastStart: lastStart, cycleLen: cycleLen, periodLen: periodLen, regularity: regularity, onBack: { step = 2 })
            }
        }
        .padding(.horizontal, 20).padding(.bottom, 16)
        .background(c.cream.ignoresSafeArea())
        .animation(.easeOut(duration: 0.2), value: step)
        .fullScreenCover(isPresented: $signIn) {
            AuthView(startMode: .signIn, onDone: { signIn = false }).environment(\.palette, c)
        }
    }

    private var welcome: some View {
        VStack(alignment: .leading, spacing: 0) {
            ZStack {
                Blob(seed: 2).fill(c.peachSoft).frame(width: 280, height: 280)
                Blob(seed: 4).fill(c.bananaSoft).frame(width: 150, height: 150).offset(x: 90, y: -90)
                Mascot(.happy, size: 200)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .accessibilityHidden(true)
            T("Your cycle, understood. Your hard days, handled.", .display).accessibilityAddTraits(.isHeader)
            T("Tracking, comfort and honest answers, kept private on your phone.", .body, c.inkMuted).padding(.top, 12).padding(.bottom, 28)
            TButton("Get started", fullWidth: true) { step = 1 }.accessibilityIdentifier("onboarding.start")
            TButton("I already have an account", kind: .text, fullWidth: true) { signIn = true }.padding(.top, 8)
        }
    }
}

private struct StepBar: View {
    let done: Int
    let onBack: () -> Void
    var onSkip: (() -> Void)? = nil
    var body: some View {
        HStack {
            IconBtn(Icons.back, "Back", action: onBack)
            StepProgress(steps: 3, done: done).padding(.horizontal, 12)
            if let onSkip { TButton("Skip", kind: .text, action: onSkip) } else { Color.clear.frame(width: 44, height: 44) }
        }
        .padding(.top, 8)
    }
}

private struct LastPeriodStep: View {
    @Binding var selected: Int?
    let onBack: () -> Void
    let onNext: () -> Void
    let onSkip: () -> Void
    @Environment(\.palette) private var c
    @State private var month = YearMonth.now()

    var body: some View {
        let today = Dates.today()
        VStack(alignment: .leading, spacing: 0) {
            StepBar(done: 1, onBack: onBack, onSkip: onSkip)
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    T("When did your last period start?", .display).padding(.top, 20).accessibilityAddTraits(.isHeader)
                    T("A rough guess is fine. You can change it later.", .bodySm, c.inkMuted).padding(.top, 8).padding(.bottom, 18)
                    MonthHeader(month: month, nextEnabled: month < YearMonth.now(), onPrev: { month = month.plus(months: -1) }, onNext: { month = month.plus(months: 1) })
                    MonthGrid(month: month, marks: { d in DayMarks(period: d == selected, today: d == today && d != selected) }, selected: nil,
                              disableAfter: today, onSelect: { selected = $0 })
                }
            }
            FlowLayout {
                Chip("I'm not sure", action: onSkip)
                Chip("I don't have periods right now", action: onSkip)
            }
            .padding(.vertical, 16)
            TButton("Continue", enabled: selected != nil, fullWidth: true, action: onNext).accessibilityIdentifier("onboarding.continue")
        }
    }
}

private struct CycleBasicsStep: View {
    @Binding var cycleLen: Int
    @Binding var periodLen: Int
    @Binding var regularity: CycleRegularity
    let onBack: () -> Void
    let onNext: () -> Void
    @Environment(\.palette) private var c

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            StepBar(done: 2, onBack: onBack, onSkip: onNext)
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    T("How long are your cycles, usually?", .display).padding(.top, 20).padding(.bottom, 28).accessibilityAddTraits(.isHeader)
                    HStack {
                        IconBtn(Icons.minus, "Fewer days", enabled: cycleLen > 15) { cycleLen -= 1 }
                        HStack(alignment: .lastTextBaseline, spacing: 4) { T("\(cycleLen)", .numeral); T("days", .headline, c.inkMuted) }.frame(maxWidth: .infinity)
                        IconBtn(Icons.plus, "More days", enabled: cycleLen < 60) { cycleLen += 1 }
                    }
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel("Cycle length")
                    .accessibilityValue("\(cycleLen) days")
                    .accessibilityAdjustableAction { d in cycleLen = (cycleLen + (d == .increment ? 1 : -1)).clamped(15, 60) }
                    Hairline().padding(.vertical, 20)
                    Stepper(value: $periodLen, in: 1...14) { HStack { T("Period length", .subtitle); Spacer(); T("\(periodLen) days", .title) } }
                    Hairline().padding(.vertical, 20)
                    T("How regular are they?", .subtitle).padding(.bottom, 10)
                    FlowLayout {
                        ForEach([(CycleRegularity.REGULAR, "Mostly regular"), (.SOMEWHAT_VARIABLE, "They vary"), (.IRREGULAR, "Unpredictable"), (.NOT_SURE, "Not sure")], id: \.0) { item in
                            Chip(item.1, selected: regularity == item.0) { regularity = item.0 }
                        }
                    }
                }
            }
            Notice("Not sure?", "Keep the defaults. Tuulit learns from what you log.").padding(.vertical, 16)
            TButton("Continue", fullWidth: true, action: onNext).accessibilityIdentifier("onboarding.continue")
        }
    }
}

private struct PrivacyStep: View {
    let lastStart: Int?
    let cycleLen: Int
    let periodLen: Int
    let regularity: CycleRegularity
    let onBack: () -> Void
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c
    @State private var reminders = true
    @State private var discreet = true
    @State private var aiContext = false
    @State private var showPin = false
    @State private var saving = false

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            StepBar(done: 3, onBack: onBack)
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    T("Your data stays on this phone.", .display).padding(.top, 20).accessibilityAddTraits(.isHeader)
                    T("Nothing leaves it unless you choose to share or back up. You can change all of this in You → Privacy.", .bodySm, c.inkMuted)
                        .padding(.top, 8).padding(.bottom, 12)
                    SettingRow(title: "Gentle reminders", sub: "Before your period. Never guilt.", isOn: reminders) { on in
                        reminders = on
                        if on { Task { if !(await model.reminders.requestPermission()) { reminders = false } } }
                    }
                    SettingRow(title: "Discreet notifications", sub: "No cycle words on the lock screen", isOn: discreet) { discreet = $0 }
                    SettingRow(title: "Personal answers in Ask", sub: "Lets Ask use a summary of your cycle data", isOn: aiContext) { aiContext = $0 }
                    ListRow("App lock", sub: model.settings.appLockEnabled ? "PIN set" : "Set a PIN now or later", icon: Icons.lock, chevron: true, last: true) { showPin = true }
                }
            }
            TButton("Start using Tuulit", kind: .banana, loading: saving, fullWidth: true) {
                saving = true
                if let s = lastStart { model.tracking.addPastPeriod(start: s, typicalLength: periodLen, today: Dates.today()) }
                model.unlocked = true
                model.settingsStore.completeOnboarding(cycleLength: cycleLen, periodLength: periodLen, regularity: regularity,
                                                       reminders: reminders, discreet: discreet, aiContext: aiContext)
            }
            .accessibilityIdentifier("onboarding.finish")
        }
        .sheet(isPresented: $showPin) { PinSetupSheet() }
    }
}
