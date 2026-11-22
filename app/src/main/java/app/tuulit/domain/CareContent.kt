package app.tuulit.domain

/** A guided Care session. Steps run in order; each has a duration in seconds. */
data class CareSession(
    val id: String,
    val title: String,
    val category: String,
    val icon: String,
    val summary: String,
    val steps: List<SessionStep>,
    val breathing: BreathingPattern? = null,
) {
    val totalSeconds: Int get() = steps.sumOf { it.seconds }
    val minutesLabel: String get() = "${(totalSeconds + 59) / 60} min"
}

data class SessionStep(val title: String, val detail: String, val seconds: Int)

/** Breathing pace in seconds. */
data class BreathingPattern(val inhale: Int, val hold: Int, val exhale: Int) {
    val cycleSeconds: Int get() = inhale + hold + exhale
    val label: String get() = if (hold > 0) "$inhale in, $hold hold, $exhale out" else "$inhale in, $exhale out"
}

object CareContent {

    val heatAndEase = CareSession(
        id = "heat_and_ease", title = "Heat and ease", category = "Cramps", icon = "heat",
        summary = "Warmth plus slow breathing relaxes the uterine muscle.",
        steps = listOf(
            SessionStep("Get warm", "Place a hot water bottle or heat pad on your lower belly or lower back. Warm, not hot.", 60),
            SessionStep("Knees to chest", "Lie on your side and draw your knees gently towards your chest.", 60),
            SessionStep("Breathe slowly", "Breathe in for 4, out for 6. Let your belly soften on each out-breath.", 120),
            SessionStep("Sip something warm", "Ginger or peppermint tea, or warm water. Stay still a little longer.", 60),
        ),
        breathing = BreathingPattern(4, 0, 6),
    )

    val slowBreathing = CareSession(
        id = "slow_breathing", title = "Slow breathing", category = "Calm", icon = "breath",
        summary = "A longer out-breath tells your body it is safe to slow down.",
        steps = listOf(SessionStep("Breathe with the circle", "In as it grows, out as it shrinks.", 180)),
        breathing = BreathingPattern(4, 0, 6),
    )

    val boxBreathing = CareSession(
        id = "box_breathing", title = "Box breathing", category = "Calm", icon = "breath",
        summary = "Four even sides. Useful when you feel on edge.",
        steps = listOf(SessionStep("Breathe in a square", "In 4, hold 4, out 4.", 120)),
        breathing = BreathingPattern(4, 4, 4),
    )

    val gentleStretches = CareSession(
        id = "gentle_stretches", title = "Gentle stretches", category = "Cramps", icon = "stretch",
        summary = "Slow movement for cramps, bloating and lower-back tension.",
        steps = listOf(
            SessionStep("Child's pose", "Knees wide, sit back on your heels, arms long. Breathe into your lower back.", 60),
            SessionStep("Cat and cow", "On hands and knees, slowly round then gently arch your back.", 90),
            SessionStep("Supine twist", "On your back, drop both knees to one side. Switch halfway.", 120),
            SessionStep("Legs up the wall", "Rest your legs against a wall, arms relaxed.", 120),
            SessionStep("Butterfly", "Sit with soles together, knees falling open. Stay soft.", 90),
        ),
    )

    val sleepWindDown = CareSession(
        id = "sleep_wind_down", title = "Sleep wind-down", category = "Sleep", icon = "moon",
        summary = "Ten quiet minutes to help your body switch off.",
        steps = listOf(
            SessionStep("Lights low", "Dim the room and put your phone face-down after this session.", 60),
            SessionStep("Body scan", "Move your attention slowly from your toes to your head, softening each part.", 240),
            SessionStep("Breathe 4-7-8", "In for 4, hold for 7, out for 8. Four rounds.", 180),
            SessionStep("Let thoughts pass", "If a worry comes up, name it and let it drift past.", 120),
        ),
        breathing = BreathingPattern(4, 7, 8),
    )

    val moodReset = CareSession(
        id = "mood_reset", title = "Mood reset", category = "Mood", icon = "care",
        summary = "Two minutes to step out of a low or tense moment.",
        steps = listOf(
            SessionStep("Name it", "Say to yourself what you feel. Naming a feeling softens it.", 30),
            SessionStep("5-4-3-2-1", "Notice 5 things you see, 4 you can touch, 3 you hear, 2 you smell, 1 you taste.", 60),
            SessionStep("One kind thing", "Choose one small kind thing to do for yourself next.", 30),
        ),
    )

    val all = listOf(heatAndEase, slowBreathing, boxBreathing, gentleStretches, sleepWindDown, moodReset)
    fun byId(id: String) = all.firstOrNull { it.id == id }

    /** Picks the hero session for Care from what was logged today. */
    fun heroFor(today: DailyLog?, phase: CyclePhase): CareSession = when {
        today == null -> if (phase == CyclePhase.MENSTRUAL) heatAndEase else slowBreathing
        today.symptoms.containsKey("Cramps") || today.symptoms.containsKey("Back pain") -> heatAndEase
        today.symptoms.containsKey("Sleep problems") || today.sleepQuality in 1..2 -> sleepWindDown
        today.moods.any { it in setOf("Sad", "Low", "Emotional") } -> moodReset
        today.moods.any { it in setOf("Anxious", "Stressed", "Irritated") } -> boxBreathing
        today.symptoms.containsKey("Bloating") -> gentleStretches
        phase == CyclePhase.MENSTRUAL -> heatAndEase
        else -> slowBreathing
    }

    fun heroHeadline(today: DailyLog?): String = when {
        today == null -> "A little care goes a long way."
        today.symptoms.containsKey("Cramps") -> "Let's ease those cramps."
        today.symptoms.containsKey("Sleep problems") || today.sleepQuality in 1..2 -> "Let's help you rest."
        today.moods.any { it in setOf("Sad", "Low", "Emotional") } -> "Be gentle with yourself today."
        today.moods.any { it in setOf("Anxious", "Stressed", "Irritated") } -> "Let's take the edge off."
        else -> "A little care goes a long way."
    }

    val redFlags = listOf(
        "Soaking through a pad or tampon every hour for several hours",
        "Pain that painkillers don't touch, or that stops you walking",
        "Fever, fainting or vomiting with your period",
        "Sudden sharp pain on one side",
        "Bleeding when you could be pregnant",
        "Periods that stop for 3+ months when not expected",
    )
}
