package app.tuulit.data

import app.tuulit.data.local.PreferencesRepository
import app.tuulit.data.local.UserSettings
import app.tuulit.domain.CycleCalculator
import app.tuulit.domain.CyclePrediction
import app.tuulit.domain.CycleRecord
import app.tuulit.domain.DailyLog
import app.tuulit.domain.Dates
import app.tuulit.domain.InsightsData
import app.tuulit.domain.PainLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

/** Everything the tracking screens derive from: real records plus calculated predictions. */
data class CycleSnapshot(
    val loaded: Boolean = false,
    val today: Long = Dates.today(),
    val settings: UserSettings = UserSettings(),
    val cycles: List<CycleRecord> = emptyList(),
    val logs: List<DailyLog> = emptyList(),
    val painLogs: List<PainLog> = emptyList(),
    val prediction: CyclePrediction = CycleCalculator.calculatePrediction(Dates.today(), emptyList(), emptyList(), 28, 5,
        app.tuulit.domain.CycleRegularity.NOT_SURE),
    val insights: InsightsData = CycleCalculator.computeInsights(emptyList(), emptyList(), emptyList(), 28, 5),
) {
    val todayLog: DailyLog? get() = logs.firstOrNull { it.epochDay == today }
    fun log(day: Long): DailyLog? = logs.firstOrNull { it.epochDay == day }
}

/** Shared, process-wide snapshot so every screen agrees on today's numbers. */
class CycleState(scope: CoroutineScope, tracking: TrackingRepository, prefs: PreferencesRepository) {
    /** Ticks at midnight so "today" rolls over while the app is open. */
    private val todayFlow = flow {
        while (true) {
            emit(Dates.today())
            val now = java.time.LocalDateTime.now(Dates.clock)
            val untilMidnight = java.time.Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).toMillis()
            delay(untilMidnight.coerceAtLeast(1_000) + 500)
        }
    }

    val snapshot: StateFlow<CycleSnapshot> = combine(
        todayFlow, prefs.settings, tracking.cycles, tracking.dailyLogs, tracking.painLogs,
    ) { today, s, cycles, logs, pain ->
        CycleSnapshot(
            loaded = true, today = today, settings = s, cycles = cycles, logs = logs, painLogs = pain,
            prediction = CycleCalculator.calculatePrediction(today, cycles, logs, s.cycleLength, s.periodLength, s.regularity),
            insights = CycleCalculator.computeInsights(cycles, logs, pain, s.cycleLength, s.periodLength),
        )
    }.stateIn(scope, SharingStarted.Eagerly, CycleSnapshot())
}
