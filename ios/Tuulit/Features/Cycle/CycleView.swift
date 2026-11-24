import SwiftUI
import TuulitCore

/// Cycle: phase summary, month calendar with legend and day detail, history, and links to
/// Insights and the Pain Journal.
struct CycleView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c
    @State private var seg = 0
    @State private var month = YearMonth.now()
    @State private var selected = Dates.today()

    var body: some View {
        let snap = model.snapshot
        let p = snap.prediction
        let logged = snap.loggedDays
        ScreenScroll {
            CycleSummary(snap: snap)
            Segmented(options: ["Month", "History"], selection: $seg).padding(.top, 18).padding(.bottom, 14)
            if seg == 0 {
                MonthHeader(month: month, onPrev: { month = month.plus(months: -1) }, onNext: { month = month.plus(months: 1) })
                MonthGrid(month: month, marks: { DayMarks.of($0, p, logged: logged, today: snap.today) }, selected: selected,
                          onSelect: { selected = $0 }, onLongPress: { d in if d <= snap.today { model.openLog(d) } })
                    .gesture(DragGesture(minimumDistance: 30).onEnded { v in
                        if v.translation.width < -60 { month = month.plus(months: 1) } else if v.translation.width > 60 { month = month.plus(months: -1) }
                    })
                Legend().padding(.top, 10)
                T("Predictions are estimates and can't be used as contraception.", .caption, c.inkFaint).padding(.top, 6)
                Hairline().padding(.top, 12)
                dayDetail(snap)
                ListRow("Insights and Mood Map", icon: Icons.chart, chevron: true) { model.push(.insights) }
                ListRow("Pain Journal", icon: Icons.pain, trailingText: "\(snap.painLogs.count) \(snap.painLogs.count == 1 ? "entry" : "entries")",
                        chevron: true, last: true) { model.push(.pain) }
            } else {
                historyList(snap)
            }
        }
    }

    private func chipColor(_ part: String) -> Color {
        if part.contains("period") || part.contains("flow") { return c.periodSoft }
        if part.contains("fertile") || part.contains("ovulation") { return c.fertileSoft }
        if part.contains("mood") { return c.peachSoft }
        return c.sand
    }

    private func dayDetail(_ snap: CycleSnapshot) -> some View {
        let p = snap.prediction
        var parts: [String] = []
        if p.loggedPeriodDays.contains(selected) { parts.append("period") }
        if let l = snap.log(selected) {
            if l.flow != .NONE { parts.append(l.flow.label.lowercased() + " flow") }
            if l.moodScore > 0 { parts.append("mood \(TrackingCatalog.moodFaceLabels[l.moodScore - 1].lowercased())") }
            for k in l.symptoms.keys.sorted().prefix(3) { parts.append("\(l.symptoms[k]!.label.lowercased()) \(k.lowercased())") }
            if l.energyLevel > 0 { parts.append("energy \(l.energyLevel)") }
        }
        if p.predictedPeriodDays.contains(selected) { parts.append("predicted period") }
        if selected == p.estimatedOvulationDay { parts.append("estimated ovulation") }
        else if p.estimatedFertileDays.contains(selected) { parts.append("fertile window (estimate)") }

        return HStack(alignment: .center) {
            VStack(alignment: .leading, spacing: 4) {
                T(Dates.day(selected), .title)
                if parts.isEmpty {
                    T(selected > snap.today ? "Nothing predicted for this day" : "Nothing logged yet", .bodySm, c.inkMuted)
                } else {
                    FlowLayout(spacing: 6) {
                        ForEach(parts, id: \.self) { part in
                            T(part, .caption).padding(.horizontal, 10).padding(.vertical, 5)
                                .background(chipColor(part), in: Capsule())
                        }
                    }
                    .padding(.top, 4)
                }
            }
            Spacer()
            if selected <= snap.today { IconBtn(Icons.edit, "Log this day") { model.openLog(selected) } }
        }
        .padding(16)
        .background(c.oat, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
        .padding(.vertical, 12)
    }

    @ViewBuilder private func historyList(_ snap: CycleSnapshot) -> some View {
        if snap.cycles.isEmpty {
            EmptyState(title: "No cycles yet", message: "Log a period with the Log button and it will appear here.")
        } else {
            let sorted = snap.cycles.sorted { $0.startEpochDay > $1.startEpochDay }
            ForEach(Array(sorted.enumerated()), id: \.element.id) { i, cyc in
                let length = i > 0 ? sorted[i - 1].startEpochDay - cyc.startEpochDay : nil
                let periodDays = cyc.endEpochDay.map { $0 - cyc.startEpochDay + 1 }
                let title = Dates.short(cyc.startEpochDay) + (cyc.endEpochDay.map { " – " + Dates.short($0) } ?? (i == 0 ? " – now" : ""))
                let sub = [length.map { "\($0)-day cycle" } ?? (i == 0 ? "Current cycle, day \(snap.today - cyc.startEpochDay + 1)" : nil),
                           periodDays.map { "period \($0) days" }].compactMap { $0 }.joined(separator: " · ")
                ListRow(title, sub: sub.isEmpty ? nil : sub, last: i == sorted.count - 1)
            }
            TButton("Edit or delete periods", kind: .text, icon: Icons.edit) { model.push(.history) }.padding(.top, 8)
        }
    }
}

/// Phase-tinted summary card: phase, day, next period and averages.
private struct CycleSummary: View {
    let snap: CycleSnapshot
    @Environment(\.palette) private var c
    var body: some View {
        let p = snap.prediction
        let ph = c.phase(p.hasRecordedCycles ? p.currentPhase : .UNKNOWN)
        VStack(alignment: .leading, spacing: 4) {
            Overline(p.hasRecordedCycles ? "Cycle \(snap.cycles.count) · day \(p.currentCycleDay ?? 1)" : "Your cycle", c.inkMuted)
            T(p.hasRecordedCycles ? "\(ph.label) phase" : "Let's begin", .display).accessibilityAddTraits(.isHeader)
            T(p.hasRecordedCycles ? (p.currentPhase.shortSummary.components(separatedBy: ". ").first ?? "") + "." : "Log a period and your phases and predictions appear here.",
              .bodySm, c.inkMuted)
            if p.hasRecordedCycles {
                HStack(spacing: 8) {
                    stat(p.nextPeriodStartEpochDay.map(Dates.short) ?? "–", "Next period")
                    stat("\(p.averageCycleLength) d", "Avg cycle")
                    stat("\(p.averagePeriodDuration) d", "Avg period")
                }
                .padding(.top, 10)
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(alignment: .topTrailing) {
            ZStack(alignment: .topTrailing) {
                ph.soft
                Blob(seed: 2).fill(ph.fill.opacity(0.35)).frame(width: 150, height: 150).offset(x: 40, y: -30)
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: Radius.xl, style: .continuous))
        .padding(.top, 14)
    }

    private func stat(_ value: String, _ label: String) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            T(value, .title, lines: 1).minimumScaleFactor(0.7)
            T(label, .caption, c.inkMuted)
        }
        .padding(.horizontal, 12).padding(.vertical, 10)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(c.oat, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
        .accessibilityElement(children: .combine)
    }
}

/// Period history with editing and deletion of each record.
struct HistoryView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c
    @State private var editing: CycleRecord?

    var body: some View {
        let cycles = model.snapshot.cycles.sorted { $0.startEpochDay > $1.startEpochDay }
        ScreenScroll(bottomPad: 40) {
            AppBar("Period history", onBack: { model.pop() })
            T("Tap a period to fix its dates or remove it. Predictions update straight away.", .bodySm, c.inkMuted).padding(.vertical, 8)
            if cycles.isEmpty { EmptyState(title: "No periods logged", message: "Use the Log button to record your period.") }
            ForEach(Array(cycles.enumerated()), id: \.element.id) { i, cyc in
                ListRow("\(Dates.day(cyc.startEpochDay)) → \(cyc.endEpochDay.map(Dates.day) ?? "ongoing")", icon: Icons.drop, chevron: true,
                        last: i == cycles.count - 1) { editing = cyc }
            }
        }
        .sheet(item: $editing) { cyc in EditPeriodSheet(cycle: cyc).presentationDetents([.medium, .large]) }
    }
}


private struct EditPeriodSheet: View {
    let cycle: CycleRecord
    @Environment(AppModel.self) private var model
    @Environment(\.palette) private var c
    @Environment(\.dismiss) private var dismiss
    @State private var start = 0
    @State private var end: Int? = nil
    @State private var error: String?
    @State private var confirmDelete = false
    @State private var ready = false

    var body: some View {
        let today = Dates.today()
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                T("Edit period", .headline).padding(.top, 24)
                DayStepper(label: "Started", day: $start, max: today)
                DayStepper(label: "Ended", day: Binding(get: { end ?? start }, set: { end = $0 }), max: today, enabled: end != nil)
                TButton(end == nil ? "Set an end date" : "Still ongoing", kind: .text) { end = end == nil ? start + 4 : nil }
                if let error { Notice("Can't save", error, kind: .medical).padding(.vertical, 8) }
                if !confirmDelete {
                    HStack(spacing: 10) {
                        TButton("Delete", kind: .quiet, icon: Icons.trash, fullWidth: true) { confirmDelete = true }
                        TButton("Save", fullWidth: true) {
                            if let e = model.tracking.editCycle(id: cycle.id, start: start, end: end) { error = e }
                            else { dismiss(); model.showToast("Period updated") }
                        }
                    }
                    .padding(.top, 12)
                } else {
                    T("Delete this period? Its daily logs stay; only the period record is removed.", .bodySm, c.inkMuted).padding(.top, 12)
                    HStack(spacing: 10) {
                        TButton("Keep it", kind: .quiet, fullWidth: true) { confirmDelete = false }
                        TButton("Delete period", kind: .danger, fullWidth: true) {
                            model.tracking.deleteCycle(id: cycle.id); dismiss(); model.showToast("Period deleted")
                        }
                    }
                    .padding(.top, 12)
                }
            }
            .padding(.horizontal, 20).padding(.bottom, 24)
        }
        .background(c.cream)
        .onAppear { if !ready { start = cycle.startEpochDay; end = cycle.endEpochDay; ready = true } }
    }
}

struct DayStepper: View {
    let label: String
    @Binding var day: Int
    let max: Int
    var enabled = true
    @Environment(\.palette) private var c
    var body: some View {
        HStack {
            T(label, .subtitle, enabled ? c.ink : c.inkFaint)
            Spacer()
            IconBtn(Icons.minus, "Day earlier", enabled: enabled) { day -= 1 }
            T(Dates.day(day), .title, enabled ? c.ink : c.inkFaint).padding(.horizontal, 8).frame(minWidth: 110)
            IconBtn(Icons.plus, "Day later", enabled: enabled && day < max) { day += 1 }
        }
        .padding(.top, 14)
        .accessibilityElement(children: .contain)
        .accessibilityLabel("\(label): \(Dates.long(day))")
    }
}
