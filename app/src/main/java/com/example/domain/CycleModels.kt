package com.example.domain

enum class FlowIntensity(val label: String, val level: Int) {
    NONE("No flow", 0),
    SPOTTING("Spotting", 1),
    LIGHT("Light", 2),
    MEDIUM("Medium", 3),
    HEAVY("Heavy", 4);

    companion object {
        fun fromName(raw: String?): FlowIntensity =
            entries.firstOrNull { it.name == raw } ?: NONE
    }
}

enum class SymptomSeverity(val label: String, val level: Int) {
    MILD("Mild", 1),
    MODERATE("Moderate", 2),
    SEVERE("Severe", 3);

    companion object {
        fun fromLevel(level: Int): SymptomSeverity =
            entries.firstOrNull { it.level == level } ?: MILD
    }
}

enum class CyclePhase(val title: String, val shortSummary: String) {
    MENSTRUAL(
        "Menstrual Phase",
        "Your body is shedding the uterine lining. Comfort, rest, and warmth often feel welcome."
    ),
    FOLLICULAR(
        "Follicular Phase",
        "Following your period, estrogen typically begins rising. Experiences vary from person to person."
    ),
    ESTIMATED_OVULATION(
        "Estimated Ovulation Window",
        "An approximate mid-cycle window based on your logs. Estimates cannot be used as contraception."
    ),
    LUTEAL(
        "Luteal Phase",
        "The time between estimated ovulation and your next period. Some people notice shifts in energy or sleep."
    ),
    UNKNOWN(
        "Getting to Know Your Cycle",
        "Log your period dates whenever you are ready so Tuulit can calculate personalized estimates."
    )
}

enum class BananaModePersona(
    val title: String,
    val tagline: String,
    val focusHint: String
) {
    NEUTRAL("Warm Banana", "Calm, balanced everyday companion", "Balanced care & tracking"),
    CHILL("Chill Banana", "Slow down, breathe, and soften the day", "Relaxation & breathing"),
    CRAMPS("Cramps Banana", "Cozy warmth and gentle pain support", "Cramp relief & comfort"),
    SLEEPY("Sleepy Banana", "Low-energy rest and quiet winding down", "Sleep support & low effort"),
    DRAMA("Drama Banana", "Witty distractions and lighthearted play", "Games & playful quizzes"),
    GYM("Gym Banana", "Gentle stretching and comfortable movement", "Stretching & light mobility"),
    HAPPY("Happy Banana", "Celebrating small wins without pressure", "Milestones & bright moments");

    companion object {
        fun fromName(raw: String?): BananaModePersona =
            entries.firstOrNull { it.name == raw } ?: NEUTRAL
    }
}

enum class CycleRegularity(val label: String) {
    REGULAR("Mostly regular"),
    SOMEWHAT_VARIABLE("Somewhat variable"),
    IRREGULAR("Irregular / unpredictable"),
    NOT_SURE("Not sure yet");

    companion object {
        fun fromName(raw: String?): CycleRegularity =
            entries.firstOrNull { it.name == raw } ?: NOT_SURE
    }
}

data class CycleRecord(
    val id: Long = 0L,
    val startEpochDay: Long,
    val endEpochDay: Long? = null,
)

data class DailyLog(
    val epochDay: Long,
    val flow: FlowIntensity = FlowIntensity.NONE,
    val symptoms: Map<String, SymptomSeverity> = emptyMap(),
    val moods: List<String> = emptyList(),
    val energyLevel: Int = 0, // 0 = unset, 1..5
    val sleepQuality: Int = 0, // 0 = unset, 1..5
    val waterGlasses: Int = 0,
    val exerciseNote: String = "",
    val stressLevel: Int = 0, // 0 = unset, 1..5
    val medicationNote: String = "",
    val optionalIntimateNote: String = "",
    val journalText: String = "",
    val updatedAtMillis: Long = 0L,
)

data class PainLog(
    val id: Long = 0L,
    val epochDay: Long,
    val intensity: Int, // 1..10
    val location: String,
    val painType: String,
    val durationHours: Int,
    val medicationTaken: String,
    val impactedWorkOrStudy: Boolean,
    val impactedExercise: Boolean,
    val impactedSleep: Boolean,
    val notes: String,
    val createdAtMillis: Long = 0L,
)

data class CyclePrediction(
    val hasRecordedCycles: Boolean,
    val isCurrentlyMenstruating: Boolean,
    val currentCycleDay: Int?,
    val currentPhase: CyclePhase,
    val averageCycleLength: Int,
    val averagePeriodDuration: Int,
    val variabilityDays: Int,
    val isIrregularEstimate: Boolean,
    val nextPeriodStartEpochDay: Long?,
    val nextPeriodRangeMinDays: Int?,
    val nextPeriodRangeMaxDays: Int?,
    val predictedPeriodDays: Set<Long>,
    val estimatedFertileDays: Set<Long>,
    val estimatedOvulationDay: Long?,
    val loggedPeriodDays: Set<Long>,
    val estimateSummary: String,
)

data class PhaseMoodSummary(
    val phase: CyclePhase,
    val topMoods: List<String>,
    val averageEnergy: Double?,
    val topSymptoms: List<String>,
    val sampleDays: Int,
)

data class PainPatternAlert(
    val hasRecurringSeverePain: Boolean,
    val consecutiveHighPainCycles: Int,
    val averageSevereIntensity: Double,
    val impactedActivitiesCount: Int,
    val summaryMessage: String,
)

data class InsightsData(
    val completedCyclesCount: Int,
    val averageCycleLength: Int?,
    val averagePeriodDuration: Int?,
    val cycleVariabilityDays: Int?,
    val cycleLengthsHistory: List<Pair<String, Int>>,
    val symptomFrequency: List<Pair<String, Int>>,
    val moodFrequency: List<Pair<String, Int>>,
    val averageEnergy: Double?,
    val averageSleep: Double?,
    val phaseMoodMap: List<PhaseMoodSummary>,
    val painPatternAlert: PainPatternAlert?,
    val hasEnoughDataForTrends: Boolean,
    val dataDrivenNotes: List<String>,
)

object TrackingCatalog {
    val defaultSymptoms = listOf(
        "Cramps",
        "Headache",
        "Back pain",
        "Breast tenderness",
        "Bloating",
        "Acne",
        "Fatigue",
        "Nausea",
        "Diarrhea",
        "Constipation",
        "Dizziness",
        "Food cravings",
        "Appetite changes",
        "Sleep problems",
        "Mood changes",
    )

    val defaultMoods = listOf(
        "Happy",
        "Calm",
        "Energetic",
        "Neutral",
        "Emotional",
        "Sad",
        "Irritated",
        "Anxious",
        "Stressed",
        "Low",
    )

    val energyLabels = listOf(
        1 to "Very low",
        2 to "Low",
        3 to "Normal",
        4 to "High",
        5 to "Very high",
    )

    val painLocations = listOf(
        "Lower abdomen",
        "Lower back",
        "Pelvic sides",
        "Thighs",
        "Head / temples",
        "Full body ache"
    )

    val painTypes = listOf(
        "Cramping",
        "Dull ache",
        "Sharp / stabbing",
        "Throbbing",
        "Pressure / heaviness"
    )
}
