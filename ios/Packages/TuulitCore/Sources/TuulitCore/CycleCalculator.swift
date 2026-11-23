import Foundation

/// Counts in first-seen order and sorts stably, like Kotlin's LinkedHashMap + `sortedByDescending`,
/// so ties resolve the same way on both platforms.
struct OrderedCounter {
    private(set) var keys: [String] = []
    private(set) var counts: [String: Int] = [:]

    mutating func add(_ key: String) {
        if counts[key] == nil { keys.append(key); counts[key] = 0 }
        counts[key]! += 1
    }

    /// Descending by count; ties keep first-seen order.
    func sortedDescending() -> [NamedCount] {
        keys.enumerated()
            .sorted { a, b in
                let ca = counts[a.element]!, cb = counts[b.element]!
                return ca != cb ? ca > cb : a.offset < b.offset
            }
            .map { NamedCount($0.element, counts[$0.element]!) }
    }

    /// First key with the maximum count (Kotlin `maxByOrNull`).
    func maxEntry() -> NamedCount? {
        var best: NamedCount?
        for k in keys where best == nil || counts[k]! > best!.count { best = NamedCount(k, counts[k]!) }
        return best
    }
}

private func average(_ values: [Int]) -> Double { Double(values.reduce(0, +)) / Double(values.count) }
/// Kotlin `roundToInt()` for the non-negative values used here.
private func roundToInt(_ x: Double) -> Int { Int(x.rounded(.toNearestOrAwayFromZero)) }

/// Port of Android `CycleCalculator`. Never hardcodes predictions; derives estimates from recorded
/// cycles and the person's defaults. Every branch and constant matches the Kotlin source.
public enum CycleCalculator {
    static let minValidCycleDays = 18
    static let maxValidCycleDays = 60
    static let defaultLutealOffset = 14

    public static func calculatePrediction(
        todayEpochDay: Int,
        cycles: [CycleRecord],
        dailyLogs: [DailyLog],
        fallbackCycleLength: Int,
        fallbackPeriodDuration: Int,
        regularity: CycleRegularity
    ) -> CyclePrediction {
        let sortedCycles = cycles.sorted { $0.startEpochDay < $1.startEpochDay }
        let loggedPeriodDays = buildLoggedPeriodDays(sortedCycles: sortedCycles, dailyLogs: dailyLogs,
                                                     todayEpochDay: todayEpochDay, fallbackPeriodDuration: fallbackPeriodDuration)

        if sortedCycles.isEmpty {
            let menstruating = loggedPeriodDays.contains(todayEpochDay)
            return CyclePrediction(
                hasRecordedCycles: false,
                isCurrentlyMenstruating: menstruating,
                currentCycleDay: menstruating ? 1 : nil,
                currentPhase: menstruating ? .MENSTRUAL : .UNKNOWN,
                averageCycleLength: fallbackCycleLength.clamped(21, 45),
                averagePeriodDuration: fallbackPeriodDuration.clamped(2, 10),
                variabilityDays: 0,
                isIrregularEstimate: regularity == .IRREGULAR,
                nextPeriodStartEpochDay: nil, nextPeriodRangeMinDays: nil, nextPeriodRangeMaxDays: nil,
                predictedPeriodDays: [], estimatedFertileDays: [], estimatedOvulationDay: nil,
                loggedPeriodDays: loggedPeriodDays,
                estimateSummary: "Log your last period start date to see personalized cycle estimates."
            )
        }

        let rawLengths = zip(sortedCycles, sortedCycles.dropFirst())
            .map { $1.startEpochDay - $0.startEpochDay }
            .filter { (minValidCycleDays...maxValidCycleDays).contains($0) }
        let recentLengths = Array(rawLengths.suffix(6))

        let averageCycleLength: Int
        if !recentLengths.isEmpty {
            var weightedSum = 0.0, totalWeight = 0.0
            for (index, len) in recentLengths.enumerated() {
                let weight = Double(index + 1)
                weightedSum += Double(len) * weight
                totalWeight += weight
            }
            averageCycleLength = roundToInt(weightedSum / totalWeight).clamped(21, 45)
        } else {
            averageCycleLength = fallbackCycleLength.clamped(21, 45)
        }

        let variabilityDays = recentLengths.count >= 2 ? recentLengths.max()! - recentLengths.min()! : 0
        let isIrregular = regularity == .IRREGULAR || variabilityDays >= 7

        let recordedDurations = Array(sortedCycles.compactMap { c -> Int? in
            c.endEpochDay.map { ($0 - c.startEpochDay + 1).clamped(1, 14) }
        }.suffix(6))
        let averagePeriodDuration = recordedDurations.isEmpty
            ? fallbackPeriodDuration.clamped(2, 10)
            : roundToInt(average(recordedDurations)).clamped(2, 10)

        let latestCycle = sortedCycles.last!
        let daysSinceLatestStart = todayEpochDay - latestCycle.startEpochDay
        let currentCycleDay = daysSinceLatestStart >= 0 ? daysSinceLatestStart + 1 : 1

        let isCurrentlyMenstruating = loggedPeriodDays.contains(todayEpochDay) ||
            (latestCycle.endEpochDay == nil && daysSinceLatestStart >= 0 && daysSinceLatestStart < averagePeriodDuration)

        // Next estimated start: roll forward if the latest start is far in the past.
        var nextStart = latestCycle.startEpochDay + averageCycleLength
        var safetyCounter = 0
        while nextStart < todayEpochDay - 7 && safetyCounter < 24 {
            nextStart += averageCycleLength
            safetyCounter += 1
        }

        let daysUntilNext = nextStart - todayEpochDay
        let halfSpread = isIrregular ? max(3, variabilityDays / 2) : 0
        let rangeMin = max(daysUntilNext - halfSpread, 0)
        let rangeMax = max(daysUntilNext + halfSpread, rangeMin)

        // Two upcoming predicted periods for the calendar.
        var predictedDays = Set<Int>()
        for cycleOffset in 0...1 {
            let start = nextStart + cycleOffset * averageCycleLength
            for d in 0..<averagePeriodDuration where !loggedPeriodDays.contains(start + d) {
                predictedDays.insert(start + d)
            }
        }

        // Estimated ovulation and fertile window (~14 days before the next estimated period).
        let ovulationDay = nextStart - defaultLutealOffset
        var fertileDays = Set<Int>()
        for offset in -4...1 {
            let candidate = ovulationDay + offset
            if !loggedPeriodDays.contains(candidate) && !predictedDays.contains(candidate) { fertileDays.insert(candidate) }
        }

        let currentPhase: CyclePhase
        if isCurrentlyMenstruating { currentPhase = .MENSTRUAL }
        else if fertileDays.contains(todayEpochDay) || todayEpochDay == ovulationDay { currentPhase = .ESTIMATED_OVULATION }
        else if todayEpochDay < ovulationDay - 4 { currentPhase = .FOLLICULAR }
        else { currentPhase = .LUTEAL }

        let estimateSummary: String
        if isCurrentlyMenstruating {
            estimateSummary = "Period day \(max(daysSinceLatestStart + 1, 1)) logged. Take things at your own pace today."
        } else if daysUntilNext == 0 {
            estimateSummary = "Your next period is estimated around today."
        } else if daysUntilNext < 0 {
            let overdue = abs(daysUntilNext)
            estimateSummary = "Your estimated period window passed about \(overdue) \(overdue == 1 ? "day" : "days") ago. Cycle timing naturally shifts."
        } else if isIrregular {
            estimateSummary = "Your next period may arrive in approximately \(rangeMin)–\(rangeMax) days (estimate)."
        } else if daysUntilNext == 1 {
            estimateSummary = "Your period may arrive around tomorrow (estimate)."
        } else {
            estimateSummary = "Your period may arrive in approximately \(daysUntilNext) days (estimate)."
        }

        return CyclePrediction(
            hasRecordedCycles: true,
            isCurrentlyMenstruating: isCurrentlyMenstruating,
            currentCycleDay: currentCycleDay,
            currentPhase: currentPhase,
            averageCycleLength: averageCycleLength,
            averagePeriodDuration: averagePeriodDuration,
            variabilityDays: variabilityDays,
            isIrregularEstimate: isIrregular,
            nextPeriodStartEpochDay: nextStart,
            nextPeriodRangeMinDays: rangeMin,
            nextPeriodRangeMaxDays: rangeMax,
            predictedPeriodDays: predictedDays,
            estimatedFertileDays: fertileDays,
            estimatedOvulationDay: ovulationDay,
            loggedPeriodDays: loggedPeriodDays,
            estimateSummary: estimateSummary
        )
    }

    public static func buildLoggedPeriodDays(sortedCycles: [CycleRecord], dailyLogs: [DailyLog],
                                             todayEpochDay: Int, fallbackPeriodDuration: Int) -> Set<Int> {
        var result = Set<Int>()
        for cycle in sortedCycles {
            let endDay = cycle.endEpochDay ??
                max(min(todayEpochDay, cycle.startEpochDay + (fallbackPeriodDuration.clamped(2, 10) - 1)), cycle.startEpochDay)
            if endDay >= cycle.startEpochDay { for d in cycle.startEpochDay...endDay { result.insert(d) } }
        }
        for log in dailyLogs where log.flow.level >= FlowIntensity.LIGHT.level { result.insert(log.epochDay) }
        return result
    }

    public static func computeInsights(cycles: [CycleRecord], dailyLogs: [DailyLog], painLogs: [PainLog],
                                       fallbackCycleLength: Int, fallbackPeriodDuration: Int) -> InsightsData {
        let sortedCycles = cycles.sorted { $0.startEpochDay < $1.startEpochDay }
        let completedLengths: [(start: Int, length: Int)] = zip(sortedCycles, sortedCycles.dropFirst())
            .map { (start: $0.startEpochDay, length: $1.startEpochDay - $0.startEpochDay) }
            .filter { (minValidCycleDays...maxValidCycleDays).contains($0.length) }

        let avgLength: Int? = completedLengths.isEmpty ? nil : roundToInt(average(completedLengths.map { $0.length }))
        let variability: Int? = completedLengths.count >= 2
            ? completedLengths.map { $0.length }.max()! - completedLengths.map { $0.length }.min()! : nil

        let durations = sortedCycles.compactMap { c -> Int? in c.endEpochDay.map { ($0 - c.startEpochDay + 1).clamped(1, 14) } }
        let avgDuration: Int? = durations.isEmpty ? nil : roundToInt(average(durations))

        let cycleHistory = completedLengths.suffix(6).enumerated().map { NamedCount("Cycle \($0.offset + 1)", $0.element.length) }

        var symptomCounts = OrderedCounter(), moodCounts = OrderedCounter()
        for log in dailyLogs {
            for s in log.symptoms.keys.sorted() { symptomCounts.add(s) }
            for m in log.moods { moodCounts.add(m) }
        }
        let energyEntries = dailyLogs.map(\.energyLevel).filter { (1...5).contains($0) }
        let sleepEntries = dailyLogs.map(\.sleepQuality).filter { (1...5).contains($0) }

        // Cycle Mood Map: each logged day bucketed by its estimated phase.
        var buckets: [CyclePhase: [DailyLog]] = [:]
        if !sortedCycles.isEmpty {
            let effectiveLength = avgLength ?? fallbackCycleLength.clamped(21, 45)
            let effectiveDuration = avgDuration ?? fallbackPeriodDuration.clamped(2, 10)
            for log in dailyLogs {
                guard let cycle = sortedCycles.last(where: { $0.startEpochDay <= log.epochDay }) else { continue }
                let dayInCycle = log.epochDay - cycle.startEpochDay + 1
                let phase: CyclePhase
                if log.flow.level >= FlowIntensity.LIGHT.level || dayInCycle <= effectiveDuration { phase = .MENSTRUAL }
                else if (effectiveLength - 16...effectiveLength - 12).contains(dayInCycle) { phase = .ESTIMATED_OVULATION }
                else if dayInCycle < effectiveLength - 16 { phase = .FOLLICULAR }
                else { phase = .LUTEAL }
                buckets[phase, default: []].append(log)
            }
        }

        let ordered: [CyclePhase] = [.MENSTRUAL, .FOLLICULAR, .ESTIMATED_OVULATION, .LUTEAL]
        let phaseMoodMap = ordered.map { phase -> PhaseMoodSummary in
            let logs = buckets[phase] ?? []
            var pm = OrderedCounter(), ps = OrderedCounter()
            for l in logs { l.moods.forEach { pm.add($0) }; l.symptoms.keys.sorted().forEach { ps.add($0) } }
            let energy = logs.map(\.energyLevel).filter { (1...5).contains($0) }
            return PhaseMoodSummary(phase: phase,
                                    topMoods: pm.sortedDescending().prefix(3).map(\.name),
                                    averageEnergy: energy.isEmpty ? nil : average(energy),
                                    topSymptoms: ps.sortedDescending().prefix(3).map(\.name),
                                    sampleDays: logs.count)
        }

        let painPattern = detectPainPattern(cycles: sortedCycles, painLogs: painLogs)
        let hasEnoughData = completedLengths.count >= 2 || dailyLogs.count >= 7

        var notes: [String] = []
        if hasEnoughData {
            if let top = symptomCounts.maxEntry(), top.count >= 2 {
                notes.append("\(top.name) is your most frequently logged symptom (\(top.count) recorded days).")
            }
            let luteal = phaseMoodMap.first { $0.phase == .LUTEAL }?.averageEnergy
            let follicular = phaseMoodMap.first { $0.phase == .FOLLICULAR }?.averageEnergy
            if let l = luteal, let f = follicular, l + 0.6 < f {
                notes.append("Your recorded energy has tended to be lower during the luteal phase than earlier in your cycle.")
            }
            if let v = variability, v >= 7 {
                notes.append("Your logged cycles have varied by \(v) days, so Tuulit uses wider estimate windows.")
            }
        }

        return InsightsData(
            completedCyclesCount: completedLengths.count,
            averageCycleLength: avgLength,
            averagePeriodDuration: avgDuration,
            cycleVariabilityDays: variability,
            cycleLengthsHistory: Array(cycleHistory),
            symptomFrequency: Array(symptomCounts.sortedDescending().prefix(6)),
            moodFrequency: Array(moodCounts.sortedDescending().prefix(6)),
            averageEnergy: energyEntries.isEmpty ? nil : average(energyEntries),
            averageSleep: sleepEntries.isEmpty ? nil : average(sleepEntries),
            phaseMoodMap: phaseMoodMap,
            painPatternAlert: painPattern,
            hasEnoughDataForTrends: hasEnoughData,
            dataDrivenNotes: notes
        )
    }

    public static func detectPainPattern(cycles: [CycleRecord], painLogs: [PainLog]) -> PainPatternAlert? {
        if painLogs.isEmpty { return nil }
        let severe = painLogs.filter { $0.intensity >= 7 }
        if severe.isEmpty { return nil }

        let sortedCycles = cycles.sorted { $0.startEpochDay < $1.startEpochDay }
        let cyclesWithSeverePain: Int
        if !sortedCycles.isEmpty {
            cyclesWithSeverePain = sortedCycles.filter { cycle in
                let nextStart = sortedCycles.first { $0.startEpochDay > cycle.startEpochDay }?.startEpochDay ?? (cycle.startEpochDay + 35)
                return severe.contains { $0.epochDay >= cycle.startEpochDay && $0.epochDay < nextStart }
            }.count
        } else {
            cyclesWithSeverePain = severe.count >= 3 ? 3 : severe.count
        }

        let impacted = severe.filter { $0.impactedWorkOrStudy || $0.impactedExercise || $0.impactedSleep }.count
        let avgSevere = average(severe.map(\.intensity))
        let recurring = cyclesWithSeverePain >= 2 || severe.count >= 3

        let msg: String
        if recurring {
            msg = "You have logged high-intensity pain (avg \(String(format: "%.1f", avgSevere))/10) across \(cyclesWithSeverePain) \(cyclesWithSeverePain == 1 ? "cycle" : "cycles")" +
                (impacted > 0 ? " with impact on daily activities. Consider sharing a Pain Summary with a healthcare clinician." : ".")
        } else {
            msg = "You have logged \(severe.count) high-pain \(severe.count == 1 ? "entry" : "entries"). Continue logging to spot recurring patterns."
        }
        return PainPatternAlert(hasRecurringSeverePain: recurring, consecutiveHighPainCycles: cyclesWithSeverePain,
                                averageSevereIntensity: avgSevere, impactedActivitiesCount: impacted, summaryMessage: msg)
    }
}
