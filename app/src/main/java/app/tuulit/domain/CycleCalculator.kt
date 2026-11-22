package app.tuulit.domain

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Pure Kotlin cycle calculation and pattern analysis engine.
 * Never hardcodes predictions; derives estimates dynamically from recorded cycles and user defaults.
 */
object CycleCalculator {

    private const val MIN_VALID_CYCLE_DAYS = 18
    private const val MAX_VALID_CYCLE_DAYS = 60
    private const val DEFAULT_LUTEAL_OFFSET = 14

    fun calculatePrediction(
        todayEpochDay: Long,
        cycles: List<CycleRecord>,
        dailyLogs: List<DailyLog>,
        fallbackCycleLength: Int,
        fallbackPeriodDuration: Int,
        regularity: CycleRegularity,
    ): CyclePrediction {
        val sortedCycles = cycles.sortedBy { it.startEpochDay }
        val loggedPeriodDays = buildLoggedPeriodDays(sortedCycles, dailyLogs, todayEpochDay, fallbackPeriodDuration)

        if (sortedCycles.isEmpty()) {
            val isMenstruatingFromLogs = loggedPeriodDays.contains(todayEpochDay)
            return CyclePrediction(
                hasRecordedCycles = false,
                isCurrentlyMenstruating = isMenstruatingFromLogs,
                currentCycleDay = if (isMenstruatingFromLogs) 1 else null,
                currentPhase = if (isMenstruatingFromLogs) CyclePhase.MENSTRUAL else CyclePhase.UNKNOWN,
                averageCycleLength = fallbackCycleLength.coerceIn(21, 45),
                averagePeriodDuration = fallbackPeriodDuration.coerceIn(2, 10),
                variabilityDays = 0,
                isIrregularEstimate = regularity == CycleRegularity.IRREGULAR,
                nextPeriodStartEpochDay = null,
                nextPeriodRangeMinDays = null,
                nextPeriodRangeMaxDays = null,
                predictedPeriodDays = emptySet(),
                estimatedFertileDays = emptySet(),
                estimatedOvulationDay = null,
                loggedPeriodDays = loggedPeriodDays,
                estimateSummary = "Log your last period start date to see personalized cycle estimates."
            )
        }

        // Compute completed cycle lengths (gap between consecutive start dates)
        val rawLengths = sortedCycles.zipWithNext { a, b -> (b.startEpochDay - a.startEpochDay).toInt() }
            .filter { it in MIN_VALID_CYCLE_DAYS..MAX_VALID_CYCLE_DAYS }

        val recentLengths = rawLengths.takeLast(6)
        val averageCycleLength = if (recentLengths.isNotEmpty()) {
            // Weighted average giving slightly higher weight to more recent cycles
            var weightedSum = 0.0
            var totalWeight = 0.0
            recentLengths.forEachIndexed { index, len ->
                val weight = (index + 1).toDouble()
                weightedSum += len * weight
                totalWeight += weight
            }
            (weightedSum / totalWeight).roundToInt().coerceIn(21, 45)
        } else {
            fallbackCycleLength.coerceIn(21, 45)
        }

        val variabilityDays = if (recentLengths.size >= 2) {
            (recentLengths.max() - recentLengths.min())
        } else {
            0
        }

        val isIrregular = regularity == CycleRegularity.IRREGULAR || variabilityDays >= 7

        val recordedDurations = sortedCycles.mapNotNull { c ->
            c.endEpochDay?.let { end -> ((end - c.startEpochDay).toInt() + 1).coerceIn(1, 14) }
        }.takeLast(6)

        val averagePeriodDuration = if (recordedDurations.isNotEmpty()) {
            recordedDurations.average().roundToInt().coerceIn(2, 10)
        } else {
            fallbackPeriodDuration.coerceIn(2, 10)
        }

        val latestCycle = sortedCycles.last()
        val daysSinceLatestStart = (todayEpochDay - latestCycle.startEpochDay).toInt()
        val currentCycleDay = if (daysSinceLatestStart >= 0) daysSinceLatestStart + 1 else 1

        val isCurrentlyMenstruating = loggedPeriodDays.contains(todayEpochDay) ||
            (latestCycle.endEpochDay == null && daysSinceLatestStart in 0 until averagePeriodDuration)

        // Next estimated start — roll forward if latest start is far in the past
        var nextStart = latestCycle.startEpochDay + averageCycleLength
        var safetyCounter = 0
        while (nextStart < todayEpochDay - 7 && safetyCounter < 24) {
            nextStart += averageCycleLength
            safetyCounter++
        }

        val daysUntilNext = (nextStart - todayEpochDay).toInt()
        val halfSpread = if (isIrregular) maxOf(3, variabilityDays / 2) else 0
        val rangeMin = (daysUntilNext - halfSpread).coerceAtLeast(0)
        val rangeMax = (daysUntilNext + halfSpread).coerceAtLeast(rangeMin)

        // Generate 2 upcoming predicted periods for calendar visualization
        val predictedDays = mutableSetOf<Long>()
        for (cycleOffset in 0..1) {
            val start = nextStart + (cycleOffset.toLong() * averageCycleLength)
            for (d in 0 until averagePeriodDuration) {
                val day = start + d
                if (!loggedPeriodDays.contains(day)) {
                    predictedDays.add(day)
                }
            }
        }

        // Estimated ovulation & fertile window (~14 days prior to next estimated period)
        val ovulationDay = nextStart - DEFAULT_LUTEAL_OFFSET
        val fertileDays = mutableSetOf<Long>()
        for (offset in -4L..1L) {
            val candidate = ovulationDay + offset
            if (!loggedPeriodDays.contains(candidate) && !predictedDays.contains(candidate)) {
                fertileDays.add(candidate)
            }
        }

        val currentPhase = when {
            isCurrentlyMenstruating -> CyclePhase.MENSTRUAL
            todayEpochDay in fertileDays || todayEpochDay == ovulationDay -> CyclePhase.ESTIMATED_OVULATION
            todayEpochDay < ovulationDay - 4 -> CyclePhase.FOLLICULAR
            else -> CyclePhase.LUTEAL
        }

        val estimateSummary = when {
            isCurrentlyMenstruating -> {
                val periodDay = (daysSinceLatestStart + 1).coerceAtLeast(1)
                "Period day $periodDay logged. Take things at your own pace today."
            }
            daysUntilNext == 0 -> "Your next period is estimated around today."
            daysUntilNext < 0 -> {
                val overdue = abs(daysUntilNext)
                "Your estimated period window passed about $overdue ${if (overdue == 1) "day" else "days"} ago. Cycle timing naturally shifts."
            }
            isIrregular -> "Your next period may arrive in approximately $rangeMin–$rangeMax days (estimate)."
            daysUntilNext == 1 -> "Your period may arrive around tomorrow (estimate)."
            else -> "Your period may arrive in approximately $daysUntilNext days (estimate)."
        }

        return CyclePrediction(
            hasRecordedCycles = true,
            isCurrentlyMenstruating = isCurrentlyMenstruating,
            currentCycleDay = currentCycleDay,
            currentPhase = currentPhase,
            averageCycleLength = averageCycleLength,
            averagePeriodDuration = averagePeriodDuration,
            variabilityDays = variabilityDays,
            isIrregularEstimate = isIrregular,
            nextPeriodStartEpochDay = nextStart,
            nextPeriodRangeMinDays = rangeMin,
            nextPeriodRangeMaxDays = rangeMax,
            predictedPeriodDays = predictedDays,
            estimatedFertileDays = fertileDays,
            estimatedOvulationDay = ovulationDay,
            loggedPeriodDays = loggedPeriodDays,
            estimateSummary = estimateSummary,
        )
    }

    fun buildLoggedPeriodDays(
        sortedCycles: List<CycleRecord>,
        dailyLogs: List<DailyLog>,
        todayEpochDay: Long,
        fallbackPeriodDuration: Int
    ): Set<Long> {
        val result = mutableSetOf<Long>()
        sortedCycles.forEach { cycle ->
            val endDay = cycle.endEpochDay ?: minOf(
                todayEpochDay,
                cycle.startEpochDay + (fallbackPeriodDuration.coerceIn(2, 10) - 1)
            ).coerceAtLeast(cycle.startEpochDay)
            var d = cycle.startEpochDay
            while (d <= endDay) {
                result.add(d)
                d++
            }
        }
        dailyLogs.filter { it.flow.level >= FlowIntensity.LIGHT.level }.forEach {
            result.add(it.epochDay)
        }
        return result
    }

    fun computeInsights(
        cycles: List<CycleRecord>,
        dailyLogs: List<DailyLog>,
        painLogs: List<PainLog>,
        fallbackCycleLength: Int,
        fallbackPeriodDuration: Int,
    ): InsightsData {
        val sortedCycles = cycles.sortedBy { it.startEpochDay }
        val completedLengths = sortedCycles.zipWithNext { a, b ->
            val len = (b.startEpochDay - a.startEpochDay).toInt()
            a.startEpochDay to len
        }.filter { it.second in MIN_VALID_CYCLE_DAYS..MAX_VALID_CYCLE_DAYS }

        val avgLength = if (completedLengths.isNotEmpty()) {
            completedLengths.map { it.second }.average().roundToInt()
        } else null

        val variability = if (completedLengths.size >= 2) {
            completedLengths.maxOf { it.second } - completedLengths.minOf { it.second }
        } else null

        val durations = sortedCycles.mapNotNull { c ->
            c.endEpochDay?.let { end -> ((end - c.startEpochDay).toInt() + 1).coerceIn(1, 14) }
        }
        val avgDuration = if (durations.isNotEmpty()) durations.average().roundToInt() else null

        val cycleHistoryLabels = completedLengths.takeLast(6).mapIndexed { index, pair ->
            "Cycle ${index + 1}" to pair.second
        }

        val symptomCounts = mutableMapOf<String, Int>()
        val moodCounts = mutableMapOf<String, Int>()
        dailyLogs.forEach { log ->
            log.symptoms.keys.forEach { s -> symptomCounts[s] = (symptomCounts[s] ?: 0) + 1 }
            log.moods.forEach { m -> moodCounts[m] = (moodCounts[m] ?: 0) + 1 }
        }

        val energyEntries = dailyLogs.mapNotNull { if (it.energyLevel in 1..5) it.energyLevel else null }
        val sleepEntries = dailyLogs.mapNotNull { if (it.sleepQuality in 1..5) it.sleepQuality else null }

        // Cycle Mood Map grouped by estimated phase for each logged day
        val phaseBuckets = mutableMapOf<CyclePhase, MutableList<DailyLog>>()
        if (sortedCycles.isNotEmpty()) {
            val effectiveLength = avgLength ?: fallbackCycleLength.coerceIn(21, 45)
            val effectiveDuration = avgDuration ?: fallbackPeriodDuration.coerceIn(2, 10)
            dailyLogs.forEach { log ->
                val cycleForDay = sortedCycles.lastOrNull { it.startEpochDay <= log.epochDay }
                if (cycleForDay != null) {
                    val dayInCycle = (log.epochDay - cycleForDay.startEpochDay).toInt() + 1
                    val phase = when {
                        log.flow.level >= FlowIntensity.LIGHT.level || dayInCycle <= effectiveDuration -> CyclePhase.MENSTRUAL
                        dayInCycle in (effectiveLength - 16)..(effectiveLength - 12) -> CyclePhase.ESTIMATED_OVULATION
                        dayInCycle < (effectiveLength - 16) -> CyclePhase.FOLLICULAR
                        else -> CyclePhase.LUTEAL
                    }
                    phaseBuckets.getOrPut(phase) { mutableListOf() }.add(log)
                }
            }
        }

        val orderedPhases = listOf(
            CyclePhase.MENSTRUAL,
            CyclePhase.FOLLICULAR,
            CyclePhase.ESTIMATED_OVULATION,
            CyclePhase.LUTEAL
        )

        val phaseMoodMap = orderedPhases.map { phase ->
            val logs = phaseBuckets[phase].orEmpty()
            val pMoods = logs.flatMap { it.moods }.groupingBy { it }.eachCount()
                .entries.sortedByDescending { it.value }.take(3).map { it.key }
            val pSymptoms = logs.flatMap { it.symptoms.keys }.groupingBy { it }.eachCount()
                .entries.sortedByDescending { it.value }.take(3).map { it.key }
            val pEnergy = logs.mapNotNull { if (it.energyLevel in 1..5) it.energyLevel else null }
                .takeIf { it.isNotEmpty() }?.average()
            PhaseMoodSummary(
                phase = phase,
                topMoods = pMoods,
                averageEnergy = pEnergy,
                topSymptoms = pSymptoms,
                sampleDays = logs.size
            )
        }

        val painPattern = detectPainPattern(sortedCycles, painLogs)
        val hasEnoughData = completedLengths.size >= 2 || dailyLogs.size >= 7

        val notes = mutableListOf<String>()
        if (hasEnoughData) {
            symptomCounts.maxByOrNull { it.value }?.let { (symptom, count) ->
                if (count >= 2) {
                    notes.add("$symptom is your most frequently logged symptom ($count recorded days).")
                }
            }
            val lutealEnergy = phaseMoodMap.firstOrNull { it.phase == CyclePhase.LUTEAL }?.averageEnergy
            val follicularEnergy = phaseMoodMap.firstOrNull { it.phase == CyclePhase.FOLLICULAR }?.averageEnergy
            if (lutealEnergy != null && follicularEnergy != null && lutealEnergy + 0.6 < follicularEnergy) {
                notes.add("Your recorded energy has tended to be lower during the luteal phase than earlier in your cycle.")
            }
            if (variability != null && variability >= 7) {
                notes.add("Your logged cycles have varied by $variability days, so Tuulit uses wider estimate windows.")
            }
        }

        return InsightsData(
            completedCyclesCount = completedLengths.size,
            averageCycleLength = avgLength,
            averagePeriodDuration = avgDuration,
            cycleVariabilityDays = variability,
            cycleLengthsHistory = cycleHistoryLabels,
            symptomFrequency = symptomCounts.entries.sortedByDescending { it.value }.take(6).map { it.key to it.value },
            moodFrequency = moodCounts.entries.sortedByDescending { it.value }.take(6).map { it.key to it.value },
            averageEnergy = energyEntries.takeIf { it.isNotEmpty() }?.average(),
            averageSleep = sleepEntries.takeIf { it.isNotEmpty() }?.average(),
            phaseMoodMap = phaseMoodMap,
            painPatternAlert = painPattern,
            hasEnoughDataForTrends = hasEnoughData,
            dataDrivenNotes = notes,
        )
    }

    fun detectPainPattern(cycles: List<CycleRecord>, painLogs: List<PainLog>): PainPatternAlert? {
        if (painLogs.isEmpty()) return null
        val severeLogs = painLogs.filter { it.intensity >= 7 }
        if (severeLogs.isEmpty()) return null

        val sortedCycles = cycles.sortedBy { it.startEpochDay }
        val cyclesWithSeverePain = if (sortedCycles.isNotEmpty()) {
            sortedCycles.count { cycle ->
                val nextStart = sortedCycles.firstOrNull { it.startEpochDay > cycle.startEpochDay }?.startEpochDay
                    ?: (cycle.startEpochDay + 35)
                severeLogs.any { it.epochDay in cycle.startEpochDay until nextStart }
            }
        } else {
            if (severeLogs.size >= 3) 3 else severeLogs.size
        }

        val impactedCount = severeLogs.count { it.impactedWorkOrStudy || it.impactedExercise || it.impactedSleep }
        val avgSevere = severeLogs.map { it.intensity }.average()
        val recurring = cyclesWithSeverePain >= 2 || severeLogs.size >= 3

        val msg = if (recurring) {
            "You have logged high-intensity pain (avg ${String.format("%.1f", avgSevere)}/10) across $cyclesWithSeverePain ${if (cyclesWithSeverePain == 1) "cycle" else "cycles"}" +
                if (impactedCount > 0) " with impact on daily activities. Consider sharing a Pain Summary with a healthcare clinician." else "."
        } else {
            "You have logged ${severeLogs.size} high-pain ${if (severeLogs.size == 1) "entry" else "entries"}. Continue logging to spot recurring patterns."
        }

        return PainPatternAlert(
            hasRecurringSeverePain = recurring,
            consecutiveHighPainCycles = cyclesWithSeverePain,
            averageSevereIntensity = avgSevere,
            impactedActivitiesCount = impactedCount,
            summaryMessage = msg,
        )
    }
}
