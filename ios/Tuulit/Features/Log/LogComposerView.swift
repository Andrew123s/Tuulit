import SwiftUI
import TuulitCore

/// The log composer, opened by the centre Log button. Every tap saves (text is debounced),
/// exactly like Android's LogComposerSheet.
struct LogComposerView: View {
    let startDay: Int
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c
    @Environment(\.dismiss) private var dismiss
    @State private var day: Int = 0
    @State private var log = DailyLog(epochDay: 0)
    @State private var loadedDay: Int? = nil
    @State private var moreOpen = false
    @State private var notesOpen = false
    @State private var showAllSymptoms = false
    @State private var newMood = ""
    @State private var newSymptom = ""
    @State private var confirmDelete = false
    @State private var firstChange = true
    @State private var textSave: Task<Void, Never>?

    private let symptomIcons: [String: IconDef] = [
        "Cramps": Icons.pain, "Headache": Icons.info, "Back pain": Icons.stretch, "Breast tenderness": Icons.care, "Bloating": Icons.food,
        "Acne": Icons.spark, "Fatigue": Icons.moon, "Nausea": Icons.alert, "Food cravings": Icons.food, "Sleep problems": Icons.moon,
        "Mood changes": Icons.breath,
    ]

    var body: some View {
        let today = Dates.today()
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                HStack {
                    IconBtn(Icons.chevL, "Previous day") { day -= 1 }
                    Spacer()
                    VStack(spacing: 2) {
                        T(day == today ? "Today" : day == today - 1 ? "Yesterday" : Dates.day(day), .headline).accessibilityAddTraits(.isHeader)
                        T("\(Dates.day(day)) · saves as you tap", .caption, c.inkMuted)
                    }
                    Spacer()
                    IconBtn(Icons.chevR, "Next day", enabled: day < today) { day += 1 }
                }
                .padding(.top, 20)

                if loadedDay == day { sections(today) }
            }
            .padding(.horizontal, 20).padding(.bottom, 30)
        }
        .scrollDismissesKeyboard(.interactively)
        .background(c.cream)
        .onAppear { if day == 0 { day = startDay } }
        .onChange(of: day, initial: false) { _, d in load(d) }
        .task { if loadedDay == nil { day = startDay; load(startDay) } }
        .sensoryFeedback(.selection, trigger: log.flow)
    }

    private func load(_ d: Int) {
        textSave?.cancel()
        firstChange = true
        log = model.tracking.dailyLog(d) ?? DailyLog(epochDay: d)
        loadedDay = d
    }

    private func change(debounce: Bool = false, _ f: (inout DailyLog) -> Void) {
        f(&log)
        let next = log
        textSave?.cancel()
        let save = {
            model.tracking.saveDailyLog(next)
            model.carePlay.unlock(.FIRST_LOG)
            if next.flow.level >= FlowIntensity.LIGHT.level { model.carePlay.unlock(.FIRST_PERIOD) }
        }
        if debounce {
            textSave = Task { try? await Task.sleep(nanoseconds: 600_000_000); if !Task.isCancelled { save() } }
        } else {
            save()
            if firstChange { model.showToast("Saved") }
        }
        firstChange = false
    }

    @ViewBuilder private func sections(_ today: Int) -> some View {
        GroupTitle("Period").padding(.top, 18)
        HStack {
            ForEach(FlowIntensity.allCases, id: \.self) { f in
                FlowStep(flow: f, on: log.flow == f) { change { $0.flow = f } }
                if f != .HEAVY { Spacer(minLength: 0) }
            }
        }
        if log.flow.level >= FlowIntensity.LIGHT.level {
            TButton("My period ended today", kind: .text) { model.tracking.logPeriodEnd(day); model.showToast("Period end saved") }
        }

        GroupTitle("Mood").padding(.top, 18)
        HStack {
            ForEach(1...5, id: \.self) { s in
                let on = log.moodScore == s
                Button { change { $0.moodScore = $0.moodScore == s ? 0 : s } } label: {
                    VStack(spacing: 2) {
                        MoodBlob(score: s, selected: on, size: 52)
                        T(TrackingCatalog.moodFaceLabels[s - 1], .caption, on ? c.ink : c.inkMuted)
                    }.frame(maxWidth: .infinity)
                }
                .buttonStyle(PressScale())
                .accessibilityLabel(TrackingCatalog.moodFaceLabels[s - 1])
                .accessibilityAddTraits(on ? [.isSelected] : [])
            }
        }
        FlowLayout(spacing: 8) {
            ForEach(uniq(TrackingCatalog.defaultMoods + model.settings.customMoods), id: \.self) { m in
                Chip(m, selected: log.moods.contains(m)) {
                    change { l in if l.moods.contains(m) { l.moods.removeAll { $0 == m } } else { l.moods.append(m) } }
                }
            }
        }
        .padding(.top, 10)
        HStack {
            Field(label: "", text: $newMood, placeholder: "Add your own mood", submitLabel: .done, onSubmit: addMood)
            TButton("Add", kind: .text, enabled: !newMood.isBlank, action: addMood)
        }
        .padding(.top, 8)

        GroupTitle("Symptoms").padding(.top, 18)
        T("Tap to add. Tap again for moderate, then severe.", .caption, c.inkFaint).padding(.bottom, 8)
        let all = uniq(TrackingCatalog.defaultSymptoms + model.settings.customSymptoms)
        let shown = showAllSymptoms ? all : Array(all.prefix(8))
        LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 8), count: 4), spacing: 8) {
            ForEach(shown, id: \.self) { name in symptomTile(name) }
        }
        if !showAllSymptoms && all.count > 8 { TButton("All symptoms", kind: .text) { showAllSymptoms = true } }
        if showAllSymptoms {
            HStack {
                Field(label: "", text: $newSymptom, placeholder: "Add your own symptom", onSubmit: addSymptom)
                TButton("Add", kind: .text, enabled: !newSymptom.isBlank, action: addSymptom)
            }
        }

        DisclosureRow(title: "Energy, sleep, water", detail: log.energyLevel > 0 ? "Energy \(log.energyLevel)" : nil, open: $moreOpen)
        if moreOpen {
            VStack(alignment: .leading, spacing: 0) {
                HStack {
                    T("Energy", .bodySm, c.inkMuted); Spacer()
                    T(TrackingCatalog.energyLabels.first { $0.0 == log.energyLevel }?.1 ?? "Not set", .caption, c.inkMuted)
                }
                EnergyBars(value: log.energyLevel) { v in change { $0.energyLevel = $0.energyLevel == v ? 0 : v } }
                T("Sleep quality", .bodySm, c.inkMuted).padding(.top, 14)
                FlowLayout(spacing: 8) {
                    ForEach(0..<5, id: \.self) { i in
                        Chip(TrackingCatalog.sleepLabels[i], selected: log.sleepQuality == i + 1) {
                            change { $0.sleepQuality = $0.sleepQuality == i + 1 ? 0 : i + 1 }
                        }
                    }
                }
                .padding(.top, 6)
                HStack {
                    T("Water: \(log.waterGlasses) glasses", .bodySm, c.inkMuted)
                    Spacer()
                    IconBtn(Icons.minus, "One glass less") { change { $0.waterGlasses = max(0, $0.waterGlasses - 1) } }
                    IconBtn(Icons.plus, "One more glass") { change { $0.waterGlasses = min(30, $0.waterGlasses + 1) } }
                }
                .padding(.top, 14)
            }
            .padding(.bottom, 12)
        }
        DisclosureRow(title: "Medication, notes and voice", detail: nil, open: $notesOpen)
        if notesOpen {
            VStack(alignment: .leading, spacing: 12) {
                Field(label: "Medication or supplements", text: textBinding(\.medicationNote, 200), placeholder: "e.g. ibuprofen 400 mg")
                Field(label: "Movement", text: textBinding(\.exerciseNote, 200), placeholder: "e.g. a short walk")
                Field(label: "Private note", text: textBinding(\.journalText, 4000), placeholder: "Anything you want to remember", multiline: true)
                VoiceJournal(day: day, hasKey: log.voiceNoteKey != nil,
                             onTranscript: { text in change { $0.journalText = ($0.journalText + " " + text).trimmed } },
                             onBackedUp: { key in change { $0.voiceNoteKey = key } })
            }
        }
        TButton("Done", kind: .banana, icon: Icons.check, fullWidth: true) {
            model.showToast(log.isEmpty ? "Nothing logged for this day" : "Saved for \(Dates.short(day))")
            dismiss()
        }
        .padding(.top, 20)
        .accessibilityIdentifier("log.done")
        if !log.isEmpty && !confirmDelete {
            TButton("Clear this day", kind: .text) { confirmDelete = true }.padding(.top, 12)
        }
        if confirmDelete {
            HStack(spacing: 10) {
                TButton("Keep it", kind: .quiet, fullWidth: true) { confirmDelete = false }
                TButton("Clear day", kind: .danger, fullWidth: true) {
                    model.tracking.deleteDailyLog(day); model.media.deleteLocal(day)
                    confirmDelete = false; load(day); model.showToast("Day cleared")
                }
            }
            .padding(.top, 8)
        }
    }

    private func uniq(_ a: [String]) -> [String] { var seen = Set<String>(); return a.filter { seen.insert($0).inserted } }

    private func textBinding(_ key: WritableKeyPath<DailyLog, String>, _ limit: Int) -> Binding<String> {
        Binding(get: { log[keyPath: key] }, set: { v in change(debounce: true) { $0[keyPath: key] = v.take(limit) } })
    }

    private func addMood() {
        let m = newMood.trimmed
        guard !m.isEmpty else { return }
        model.settingsStore.addCustomMood(m)
        change { if !$0.moods.contains(m) { $0.moods.append(m) } }
        newMood = ""
    }

    private func addSymptom() {
        let s = newSymptom.trimmed
        guard !s.isEmpty else { return }
        model.settingsStore.addCustomSymptom(s)
        change { $0.symptoms[s] = .MILD }
        newSymptom = ""
    }

    private func symptomTile(_ name: String) -> some View {
        let sev = log.symptoms[name]
        let on = sev != nil
        return Button {
            change { l in
                switch l.symptoms[name] {
                case nil: l.symptoms[name] = .MILD
                case .MILD: l.symptoms[name] = .MODERATE
                case .MODERATE: l.symptoms[name] = .SEVERE
                case .SEVERE: l.symptoms[name] = nil
                }
            }
        } label: {
            VStack(spacing: 6) {
                TIcon(symptomIcons[name] ?? Icons.spark, size: 22, color: on ? c.lilacDeep : c.inkMuted)
                T(name, .caption, align: .center, lines: 2).minimumScaleFactor(0.85)
                HStack(spacing: 3) {
                    ForEach(0..<3, id: \.self) { i in Circle().fill(c.lilacDeep.opacity(on && i < (sev?.level ?? 0) ? 1 : (on ? 0.25 : 0))).frame(width: 7, height: 7) }
                }
            }
            .frame(maxWidth: .infinity, minHeight: 92)
            .padding(.horizontal, 4)
            .background(on ? c.fertileSoft : c.sand, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 18).stroke(on ? c.fertile : .clear, lineWidth: 2))
        }
        .buttonStyle(PressScale())
        .accessibilityLabel("\(name), \(sev.map { $0.label.lowercased() } ?? "not logged")")
        .accessibilityHint("Tap to change severity")
    }
}

private struct FlowStep: View {
    let flow: FlowIntensity
    let on: Bool
    let action: () -> Void
    @Environment(\.palette) private var c
    var body: some View {
        Button(action: action) {
            VStack(spacing: 6) {
                ZStack {
                    Circle().fill(on ? (flow == .NONE ? c.tan : c.period) : (flow == .NONE ? c.sand : c.periodSoft)).frame(width: 54, height: 54)
                    if flow == .NONE { TIcon(Icons.close, size: 18) }
                    else { TIcon(Icons.drop, size: CGFloat(10 + flow.level * 4), color: on ? c.onPeriod : c.periodDeep) }
                }
                T(flow == .NONE ? "None" : flow.label, .caption, on ? c.ink : c.inkMuted, lines: 1)
            }
            .frame(width: 60)
        }
        .buttonStyle(PressScale())
        .accessibilityLabel(flow == .NONE ? "No flow" : "\(flow.label) flow")
        .accessibilityAddTraits(on ? [.isSelected] : [])
    }
}

struct EnergyBars: View {
    let value: Int
    let onSet: (Int) -> Void
    @Environment(\.palette) private var c
    var body: some View {
        HStack(alignment: .bottom, spacing: 8) {
            ForEach(1...5, id: \.self) { i in
                Button { onSet(i) } label: {
                    RoundedRectangle(cornerRadius: 10, style: .continuous).fill(i <= value ? c.banana : c.sand).frame(height: CGFloat(14 + i * 8))
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Energy \(i), \(TrackingCatalog.energyLabels[i - 1].1)")
                .accessibilityAddTraits(i == value ? [.isSelected] : [])
            }
        }
        .frame(height: 56)
        .padding(.top, 6)
    }
}

private struct DisclosureRow: View {
    let title: String
    let detail: String?
    @Binding var open: Bool
    @Environment(\.palette) private var c
    var body: some View {
        Button { withAnimation(.easeOut(duration: 0.2)) { open.toggle() } } label: {
            VStack(spacing: 0) {
                HStack {
                    T(title, .subtitle)
                    Spacer()
                    if let detail { T(detail, .bodySm, c.inkMuted) }
                    TIcon(Icons.chevD, size: 18, color: c.inkFaint).rotationEffect(.degrees(open ? 180 : 0))
                }
                .frame(minHeight: 56)
                Hairline()
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityValue(open ? "Expanded" : "Collapsed")
    }
}
