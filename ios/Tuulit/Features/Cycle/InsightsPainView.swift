import SwiftUI
import UniformTypeIdentifiers
import TuulitCore

/// A CSV export the system share sheet can save or send. Built on demand, never stored.
struct CSVExport: Transferable {
    let csv: String
    static var transferRepresentation: some TransferRepresentation {
        FileRepresentation(exportedContentType: .commaSeparatedText) { item in
            let url = FileManager.default.temporaryDirectory.appendingPathComponent("tuulit-export-\(Dates.iso(Dates.today())).csv")
            try item.csv.data(using: .utf8)!.write(to: url, options: [.atomic, .completeFileProtection])
            return SentTransferredFile(url)
        }
    }
}

struct InsightsView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c

    var body: some View {
        let snap = model.snapshot
        let i = snap.insights
        let lengths = i.cycleLengthsHistory.map(\.count)
        ScreenScroll(bottomPad: 40) {
            AppBar(title: "Insights", onBack: { model.pop() }) {
                ShareLink(item: CSVExport(csv: model.tracking.exportCSV()), preview: SharePreview("Tuulit export")) {
                    TIcon(Icons.download, size: 20).frame(width: 44, height: 44).background(c.sand, in: Circle())
                }
                .accessibilityLabel("Export my data as CSV")
            }
            if i.completedCyclesCount < 2 {
                T("Insights", .display).padding(.top, 10)
                EmptyState(title: "Building your baseline",
                           message: i.completedCyclesCount == 0 ? "Log two periods and your cycle patterns will appear here." : "Log one more period and your patterns will appear here.")
            } else {
                T("Your cycles run \(lengths.min()!)–\(lengths.max()!) days.", .display).padding(.top, 10)
                let typical = lengths.allSatisfy { (21...35).contains($0) }
                T(typical ? "That's within the typical range. Based on \(i.completedCyclesCount) cycles." : "Some cycles fall outside 21–35 days. Worth noting for a check-up.",
                  .bodySm, c.inkMuted).padding(.top, 6).padding(.bottom, 18)
                Hairline()
                HStack(spacing: 0) {
                    stat("\(i.averageCycleLength.map(String.init) ?? "–") d", "Avg cycle")
                    Rectangle().fill(c.hairline).frame(width: 1, height: 70)
                    stat("\(i.averagePeriodDuration.map(String.init) ?? "–") d", "Avg period").padding(.leading, 14)
                    Rectangle().fill(c.hairline).frame(width: 1, height: 70)
                    stat("±\((i.cycleVariabilityDays ?? 0) / 2) d", "Variation").padding(.leading, 14)
                }
                Hairline()
                CycleBars(history: i.cycleLengthsHistory, avg: i.averageCycleLength ?? 28, currentDay: snap.prediction.currentCycleDay).padding(.top, 20)
            }
            T("Cycle Mood Map", .subtitle).padding(.top, 24).padding(.bottom, 10)
            let moods = topMoods(i.phaseMoodMap)
            if moods.isEmpty {
                T("Log moods for a few days in different phases and the map fills in.", .bodySm, c.inkMuted)
            } else {
                MoodMap(moods: moods, map: i.phaseMoodMap)
            }
            if !i.symptomFrequency.isEmpty {
                T("Most logged symptoms", .subtitle).padding(.top, 24).padding(.bottom, 10)
                let maxCount = Double(i.symptomFrequency.map(\.count).max() ?? 1)
                ForEach(i.symptomFrequency.prefix(5), id: \.name) { f in freqBar(f.name, f.count, Double(f.count) / maxCount) }
            }
            if !i.dataDrivenNotes.isEmpty {
                GroupTitle("Observed patterns").padding(.top, 24)
                ForEach(Array(i.dataDrivenNotes.enumerated()), id: \.offset) { idx, n in
                    ListRow(n, icon: Icons.spark, last: idx == i.dataDrivenNotes.count - 1)
                }
            }
            T("Patterns describe your own logs. They aren't a diagnosis.", .caption, c.inkFaint).padding(.top, 16)
        }
        .onAppear { model.carePlay.unlock(.INSIGHTS_EXPLORED) }
    }

    private func topMoods(_ map: [PhaseMoodSummary]) -> [String] {
        var order: [String] = []; var counts: [String: Int] = [:]
        for m in map.flatMap(\.topMoods) { if counts[m] == nil { order.append(m) }; counts[m, default: 0] += 1 }
        return order.enumerated().sorted { counts[$0.element]! != counts[$1.element]! ? counts[$0.element]! > counts[$1.element]! : $0.offset < $1.offset }
            .prefix(4).map(\.element)
    }

    private func stat(_ value: String, _ label: String) -> some View {
        VStack(alignment: .leading, spacing: 0) { T(value, .title); T(label, .caption, c.inkMuted) }
            .padding(.vertical, 14).frame(maxWidth: .infinity, alignment: .leading)
            .accessibilityElement(children: .combine)
    }

    private func freqBar(_ name: String, _ count: Int, _ fraction: Double) -> some View {
        HStack {
            T(name, .bodySm, lines: 1).frame(width: 110, alignment: .leading)
            GeometryReader { g in
                ZStack(alignment: .leading) {
                    Capsule().fill(c.sand)
                    Capsule().fill(c.toffee).frame(width: g.size.width * max(0.05, min(1, fraction)))
                }
            }
            .frame(height: 8)
            T("\(count)", .caption, c.inkMuted).frame(width: 24)
        }
        .padding(.vertical, 5)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(name), \(count) days")
    }
}

private struct CycleBars: View {
    let history: [NamedCount]
    let avg: Int
    let currentDay: Int?
    @Environment(\.palette) private var c

    var body: some View {
        let values = history.map(\.count) + (currentDay.map { [$0] } ?? [])
        let maxV = max(values.max() ?? avg, avg) + 2
        let minV = max((values.min() ?? avg) - 8, 0)
        let h: (Int) -> CGFloat = { v in 16 + CGFloat(v - minV) / CGFloat(max(1, maxV - minV)) * 90 }
        ZStack(alignment: .bottom) {
            Rectangle().fill(c.inkFaint).frame(height: 1.5).padding(.bottom, 20 + h(avg)).frame(maxHeight: .infinity, alignment: .bottom)
            HStack(alignment: .bottom, spacing: 10) {
                ForEach(Array(history.suffix(6).enumerated()), id: \.offset) { idx, item in bar("\(item.count)", "C\(idx + 1)", h(item.count), now: false) }
                if let d = currentDay { bar("\(d)", "Now", h(d), now: true) }
            }
        }
        .frame(height: 160)
        .overlay(alignment: .topTrailing) { T("avg \(avg)", .caption, c.inkMuted) }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Cycle lengths: \(history.map { "\($0.count) days" }.joined(separator: ", ")). Average \(avg) days.")
    }

    private func bar(_ value: String, _ label: String, _ height: CGFloat, now: Bool) -> some View {
        VStack(spacing: 6) {
            T(value, .caption, now ? c.ink : c.inkMuted)
            UnevenRoundedRectangle(topLeadingRadius: 8, bottomLeadingRadius: 4, bottomTrailingRadius: 4, topTrailingRadius: 8)
                .fill(now ? c.period : c.peach).frame(maxWidth: 28).frame(height: height)
            T(label, .caption, c.inkFaint)
        }
        .frame(maxWidth: .infinity)
    }
}

private struct MoodMap: View {
    let moods: [String]
    let map: [PhaseMoodSummary]
    @Environment(\.palette) private var c

    var body: some View {
        let phases: [CyclePhase] = [.MENSTRUAL, .FOLLICULAR, .ESTIMATED_OVULATION, .LUTEAL]
        VStack(spacing: 0) {
            HStack(spacing: 0) {
                Color.clear.frame(width: 72, height: 1)
                ForEach(["Period", "Follic.", "Ovul.", "Luteal"], id: \.self) { T($0, .caption, c.inkMuted, align: .center).frame(maxWidth: .infinity) }
            }
            ForEach(moods, id: \.self) { mood in
                HStack(spacing: 0) {
                    T(mood, .caption).frame(width: 72, alignment: .leading)
                    ForEach(phases, id: \.self) { ph in
                        let rank = map.first { $0.phase == ph }?.topMoods.firstIndex(of: mood) ?? -1
                        let size: CGFloat = rank == 0 ? 22 : rank == 1 ? 15 : rank == 2 ? 10 : 4
                        Circle().fill(rank >= 0 ? c.toffee : c.hairline).frame(width: size, height: size).frame(maxWidth: .infinity, minHeight: 24)
                    }
                }
                .padding(.vertical, 6)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel("\(mood): " + phases.compactMap { ph in
                    (map.first { $0.phase == ph }?.topMoods.contains(mood) ?? false) ? ph.title : nil
                }.joined(separator: ", "))
            }
            T("Bigger dots mean that mood was logged more often in that phase.", .caption, c.inkFaint).padding(.top, 6)
        }
    }
}

struct PainJournalView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c
    @State private var intensity = 0
    @State private var location: String?
    @State private var type: String?
    @State private var work = false
    @State private var exercise = false
    @State private var sleep = false
    @State private var relief = ""
    @State private var notes = ""
    @State private var hours = 2
    @State private var more = false
    @State private var error: String?

    var body: some View {
        let snap = model.snapshot
        let warn = MedicalSafetyGuard.assess(notes)
        ScreenScroll(bottomPad: 40) {
            AppBar("Pain Journal", onBack: { model.pop() })
            Panel(padding: 18) {
                HStack {
                    T("How bad is it right now?", .subtitle)
                    Spacer()
                    if intensity > 0 { T("\(intensity)/10", .title, c.period) }
                }
                HStack(spacing: 4) {
                    ForEach(1...10, id: \.self) { n in
                        let on = n == intensity, fill = n < intensity
                        Button { intensity = n; error = nil } label: {
                            T("\(n)", .label, on ? c.onPeriod : c.ink).frame(maxWidth: .infinity, minHeight: 44)
                                .background(on ? c.period : fill ? c.periodSoft : c.sand, in: RoundedRectangle(cornerRadius: 8))
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel("Pain \(n) of 10")
                        .accessibilityAddTraits(on ? [.isSelected] : [])
                    }
                }
                .padding(.top, 12)
                .sensoryFeedback(.selection, trigger: intensity)
                GroupTitle("Where").padding(.top, 16)
                FlowLayout { ForEach(TrackingCatalog.painLocations, id: \.self) { l in Chip(l, selected: location == l) { location = location == l ? nil : l } } }
                GroupTitle("It stopped me from").padding(.top, 14)
                FlowLayout {
                    Chip("Work or study", selected: work) { work.toggle() }
                    Chip("Exercise", selected: exercise) { exercise.toggle() }
                    Chip("Sleep", selected: sleep) { sleep.toggle() }
                }
                TButton(more ? "Fewer details" : "More details", kind: .text) { more.toggle() }
                if more {
                    GroupTitle("Type of pain")
                    FlowLayout { ForEach(TrackingCatalog.painTypes, id: \.self) { t in Chip(t, selected: type == t) { type = type == t ? nil : t } } }
                    HStack {
                        T("Lasted about \(hours) \(hours == 1 ? "hour" : "hours")", .bodySm, c.inkMuted)
                        Spacer()
                        IconBtn(Icons.minus, "Shorter") { hours = max(0, hours - 1) }
                        IconBtn(Icons.plus, "Longer") { hours = min(72, hours + 1) }
                    }
                    .padding(.top, 12)
                    Field(label: "Relief tried", text: Binding(get: { relief }, set: { relief = $0.take(120) }), placeholder: "e.g. heat, ibuprofen").padding(.top, 12)
                    Field(label: "Notes for you or your clinician", text: Binding(get: { notes }, set: { notes = $0.take(500) }), multiline: true).padding(.top, 12)
                }
                if warn.level == .URGENT || warn.level == .EMERGENCY || intensity >= 9 {
                    Notice(warn.headline.isEmpty ? "Pain this severe deserves care" : warn.headline,
                           warn.message.isEmpty ? "If painkillers don't help or you also have fever, fainting or heavy bleeding, please see a doctor today." : warn.message,
                           kind: .medical).padding(.top, 12)
                }
                if let error { T(error, .caption, c.alert).padding(.top, 8) }
                TButton("Save entry", fullWidth: true) { save() }.padding(.top, 16)
            }
            .padding(.top, 8)
            if let a = snap.insights.painPatternAlert, a.hasRecurringSeverePain {
                Notice(title: "Severe pain \(a.consecutiveHighPainCycles) cycles in a row", message: "Worth raising with a clinician.") {
                    TButton("Prepare for the appointment", kind: .text) { model.push(.askChat(tool: "appointment", topic: nil)) }
                }
                .padding(.top, 14)
            }
            if snap.painLogs.isEmpty {
                T("Your entries will appear here.", .bodySm, c.inkMuted).padding(.top, 16)
            } else {
                GroupTitle("History").padding(.top, 20)
                ForEach(Array(snap.painLogs.enumerated()), id: \.element.id) { idx, l in
                    let impacts = [l.impactedWorkOrStudy ? "missed work" : nil, l.impactedExercise ? "no exercise" : nil, l.impactedSleep ? "poor sleep" : nil].compactMap { $0 }
                    let sub = ([l.location] + impacts + [l.medicationTaken]).filter { !$0.isBlank }.joined(separator: " · ")
                    HStack(spacing: 12) {
                        T("\(l.intensity)", .title, c.period).frame(width: 32)
                        VStack(alignment: .leading, spacing: 2) {
                            T(Dates.short(l.epochDay) + (l.painType.isEmpty ? "" : " · " + l.painType.lowercased()), .body).fontWeight(.semibold)
                            if !sub.isEmpty { T(sub, .bodySm, c.inkMuted) }
                        }
                        Spacer()
                        IconBtn(Icons.trash, "Delete entry") {
                            model.tracking.deletePainLog(id: l.id)
                            model.showToast("Entry deleted") { model.tracking.savePainLog(l) }
                        }
                    }
                    .padding(.vertical, 10)
                    if idx < snap.painLogs.count - 1 { Hairline() }
                }
            }
        }
    }

    private func save() {
        guard intensity > 0 else { error = "Choose a number from 1 to 10."; return }
        model.tracking.savePainLog(PainLog(epochDay: model.snapshot.today, intensity: intensity, location: location ?? "", painType: type ?? "",
                                           durationHours: hours, medicationTaken: relief.trimmed, impactedWorkOrStudy: work, impactedExercise: exercise,
                                           impactedSleep: sleep, notes: notes.trimmed))
        model.carePlay.unlock(.PAIN_JOURNAL)
        intensity = 0; location = nil; type = nil; work = false; exercise = false; sleep = false; relief = ""; notes = ""; more = false
        model.showToast("Pain entry saved")
    }
}
