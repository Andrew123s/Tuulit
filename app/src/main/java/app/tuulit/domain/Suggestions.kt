package app.tuulit.domain

/** One suggestion shown on Today. [sessionId] or [route] says where tapping it goes. */
data class Suggestion(
    val kicker: String,
    val title: String,
    val body: String,
    val sessionId: String? = null,
    val route: String? = null,
)

/** Rule-based suggestion engine. Reads the current phase and today's log; no network involved. */
object SuggestionEngine {

    fun forToday(prediction: CyclePrediction, today: DailyLog?, hourOfDay: Int): Suggestion {
        val symptoms = today?.symptoms.orEmpty()
        val evening = hourOfDay >= 19
        return when {
            symptoms.containsKey("Cramps") -> Suggestion(
                "You logged cramps", "Heat and slow breathing",
                "Five minutes that can ease the squeeze.", sessionId = CareContent.heatAndEase.id,
            )
            evening && (symptoms.containsKey("Sleep problems") || (today?.sleepQuality ?: 0) in 1..2) -> Suggestion(
                "For tonight", "A 10-minute wind-down", "You logged poor sleep recently.",
                sessionId = CareContent.sleepWindDown.id,
            )
            today?.moods.orEmpty().any { it in setOf("Anxious", "Stressed", "Irritated") } -> Suggestion(
                "For right now", "Box breathing", "Two minutes to take the edge off.",
                sessionId = CareContent.boxBreathing.id,
            )
            (today?.energyLevel ?: 0) in 1..2 -> Suggestion(
                "Low energy today", "Keep it light", "A short distraction might help more than pushing through.",
                route = "care/play",
            )
            prediction.isCurrentlyMenstruating -> Suggestion(
                "During your period", "Heat and ease", "Warmth and rest are often welcome now.",
                sessionId = CareContent.heatAndEase.id,
            )
            prediction.currentPhase == CyclePhase.LUTEAL -> Suggestion(
                "For the luteal phase", "Ten minutes of gentle stretching",
                "Can ease bloating and back tension.", sessionId = CareContent.gentleStretches.id,
            )
            prediction.currentPhase == CyclePhase.FOLLICULAR -> Suggestion(
                "For the follicular phase", "Learn what's happening", "Energy often rises after your period. Here's why.",
                route = "ask/chat/explain?topic=follicular",
            )
            prediction.currentPhase == CyclePhase.ESTIMATED_OVULATION -> Suggestion(
                "Mid-cycle", "What ovulation feels like", "Some people notice a twinge or a shift in energy.",
                route = "ask/chat/explain?topic=ovulation",
            )
            else -> Suggestion(
                "Getting started", "Log your period when it comes",
                "Predictions appear once Tuulit knows one start date.", route = "log",
            )
        }
    }
}

/** What the Today hero shows. Built from the prediction; never hardcoded. */
data class TodayHero(val lead: String, val number: String?, val unit: String, val caption: String)

object TodayHeroBuilder {
    fun build(p: CyclePrediction, today: Long, format: (Long) -> String): TodayHero {
        if (!p.hasRecordedCycles) {
            return TodayHero("Let's get to know your cycle.", null, "", "Log your next period and predictions will appear here.")
        }
        val phaseLine = "day ${p.currentCycleDay ?: "–"}, ${p.currentPhase.title.lowercase()}"
        if (p.isCurrentlyMenstruating) {
            val start = p.loggedPeriodDays.filter { it <= today }.let { days ->
                var d = today
                while (days.contains(d - 1)) d--
                d
            }
            val dayOfPeriod = (today - start + 1).toInt()
            return TodayHero("Your period", dayOfPeriod.toString(), if (dayOfPeriod == 1) "st day" else ordinal(dayOfPeriod) + " day",
                "Day ${p.currentCycleDay} of your cycle · logged")
        }
        val next = p.nextPeriodStartEpochDay ?: return TodayHero("", null, "", phaseLine)
        val days = (next - today).toInt()
        val rangeText = if (p.isIrregularEstimate) {
            "${p.nextPeriodRangeMinDays}–${p.nextPeriodRangeMaxDays} days · estimate · $phaseLine"
        } else {
            "${format(next)}–${format(next + p.averagePeriodDuration - 1)} · estimate · $phaseLine"
        }
        return when {
            days < 0 -> TodayHero("Your period may be late by", (-days).toString(), if (days == -1) " day" else " days",
                "Cycles shift. Log it when it starts · $phaseLine")
            days == 0 -> TodayHero("Your period is likely", null, "today", rangeText)
            else -> TodayHero("Your period is likely in", days.toString(), if (days == 1) " day" else " days", rangeText)
        }
    }

    private fun ordinal(n: Int): String {
        val suffix = if (n % 100 in 11..13) "th" else when (n % 10) { 1 -> "st"; 2 -> "nd"; 3 -> "rd"; else -> "th" }
        return suffix
    }
}
