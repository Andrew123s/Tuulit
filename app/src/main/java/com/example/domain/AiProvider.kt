package com.example.domain

import kotlinx.coroutines.delay

enum class MythVerdict(val label: String) {
    FACT("Fact"),
    MYTH("Myth"),
    NUANCED("It Depends")
}

data class AiChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val safetyWarning: String? = null,
    val followUpPrompts: List<String> = emptyList(),
    val isSampleResponse: Boolean = false,
)

data class MythCheckResult(
    val claim: String,
    val verdict: MythVerdict,
    val explanation: String,
    val medicalNote: String,
    val isSampleResponse: Boolean = true,
)

data class AppointmentPrepSummary(
    val title: String,
    val cycleOverview: String,
    val painSummary: String,
    val symptomHighlights: List<String>,
    val suggestedQuestions: List<String>,
    val isSampleResponse: Boolean = true,
)

data class AiContextSnapshot(
    val cycleDay: Int?,
    val phase: CyclePhase,
    val recentSymptoms: List<String>,
    val recentMoods: List<String>,
    val averageEnergy: Double?,
)

/**
 * Deterministic client-side red-flag symptom detector.
 * Runs before any AI response to ensure urgent medical warning signs are never overlooked.
 */
object MedicalSafetyGuard {
    private val urgentPatterns = listOf(
        "faint", "passed out", "passing out", "soaking through", "pad every hour",
        "tampon every hour", "fever", "101", "102", "chest pain", "shortness of breath",
        "sudden severe", "unbearable pain", "worst pain", "pregnant and bleeding",
        "can't stand", "vomiting nonstop", "hurting myself", "suicide", "self harm"
    )

    fun inspectForUrgentWarning(input: String): String? {
        val lower = input.lowercase()
        val matched = urgentPatterns.any { lower.contains(it) }
        return if (matched) {
            "Some symptoms you mentioned—such as severe sudden pain, heavy bleeding soaking through protection hourly, fever, fainting, or distress—deserve prompt in-person medical evaluation. If you feel unsafe or acutely unwell, please contact urgent care or local emergency services right away."
        } else null
    }
}

/**
 * Replaceable AI provider abstraction.
 * Live Gemini inference is powered by Firebase AI Logic when Firebase is enabled;
 * currently backed by SampleTuulitAiProvider with clearly labeled educational previews.
 */
interface TuulitAiProvider {
    val isLiveCloudAiEnabled: Boolean

    suspend fun askTuulit(
        question: String,
        context: AiContextSnapshot?,
        includeContext: Boolean
    ): AiChatMessage

    suspend fun explainBody(
        topic: String,
        followUpStyle: String? = null
    ): AiChatMessage

    suspend fun checkHealthMyth(claim: String): MythCheckResult

    suspend fun buildAppointmentPrep(
        prediction: CyclePrediction,
        insights: InsightsData,
        painLogs: List<PainLog>,
        userConcern: String
    ): AppointmentPrepSummary
}

class SampleTuulitAiProvider : TuulitAiProvider {
    override val isLiveCloudAiEnabled: Boolean = false

    override suspend fun askTuulit(
        question: String,
        context: AiContextSnapshot?,
        includeContext: Boolean
    ): AiChatMessage {
        delay(320)
        val safety = MedicalSafetyGuard.inspectForUrgentWarning(question)
        val q = question.lowercase()

        val contextNote = if (includeContext && context != null) {
            buildString {
                append("\n\n[Using your opt-in cycle context: ")
                append(context.phase.title)
                if (context.cycleDay != null) append(", day ${context.cycleDay}")
                if (context.recentSymptoms.isNotEmpty()) {
                    append("; recent logs include ${context.recentSymptoms.take(3).joinToString(", ")}")
                }
                append(" — keeping in mind that not every symptom is caused by your cycle.]")
            }
        } else ""

        val body = when {
            q.contains("cramp") || q.contains("pain") ->
                "Menstrual cramps happen when natural chemical messengers called prostaglandins signal the uterus to contract. Gentle warmth, comfortable resting positions, and hydration can ease mild discomfort. If pain regularly stops you from working, studying, or sleeping, it is always worth discussing with a healthcare clinician—severe pain should never be brushed off as normal."
            q.contains("bloat") ->
                "Pre-period bloating is very common and often linked to normal shifts in progesterone and fluid retention, as well as digestive slowing. Drinking water steadily, gentle walking, and eating balanced meals can feel more comfortable. Persistent or severe abdominal swelling outside your cycle is worth checking with a clinician."
            q.contains("tired") || q.contains("fatigue") || q.contains("exhaust") ->
                "Fatigue can show up around your period or luteal phase due to shifting hormones, lighter sleep, or heavy bleeding affecting iron levels. At the same time, low energy can also stem from stress, dehydration, thyroid changes, or poor sleep unrelated to your cycle. If exhaustion lingers across cycles, a routine check-in with a doctor can help."
            q.contains("irregular") || q.contains("late") || q.contains("skip") ->
                "Cycles naturally shift by several days due to stress, illness, travel, sleep changes, or hormonal variation. A single late period is common; however, consistently unpredictable cycles, missed periods for several months, or sudden changes are good reasons to check in with a healthcare professional."
            q.contains("mood") || q.contains("anxious") || q.contains("sad") || q.contains("pmdd") ->
                "Emotional changes before or during a period can be influenced by hormonal shifts, pain, poor sleep, or everyday life stress—so we never assume feelings are purely hormonal. If mood changes feel intense or disruptive every luteal phase (sometimes associated with PMDD), tracking them across 2–3 cycles gives a clinician clear information to support you."
            else ->
                "Thanks for asking. Every body experiences menstrual cycles differently, and noticing patterns over time is one of the most helpful tools you have. For personalized medical advice or diagnosis, always partner with a qualified healthcare clinician."
        }

        return AiChatMessage(
            id = "ai_${System.currentTimeMillis()}",
            isUser = false,
            text = "[Sample Preview — Enable Firebase AI Logic for live Gemini responses]\n\n$body$contextNote",
            safetyWarning = safety,
            followUpPrompts = listOf(
                "Explain it simply",
                "What can I do?",
                "Is this common?",
                "When should I worry?"
            ),
            isSampleResponse = true
        )
    }

    override suspend fun explainBody(topic: String, followUpStyle: String?): AiChatMessage {
        delay(280)
        val safety = MedicalSafetyGuard.inspectForUrgentWarning(topic)
        val lower = topic.lowercase()

        val base = when {
            lower.contains("bloat") ->
                "Before a period starts, progesterone levels rise and then drop, which can slow digestion slightly and cause your body to hold onto extra water and salt. That temporary fluid shift is what creates the heavy or bloated feeling."
            lower.contains("cramp") ->
                "During menstruation, the uterus releases prostaglandins to help shed its lining. Higher prostaglandin levels create stronger muscle contractions, which temporarily reduce blood flow and trigger cramping sensations."
            lower.contains("fatigue") || lower.contains("sleep") ->
                "Body temperature rises slightly during the luteal phase and hormone shifts can lighten deep sleep, while heavy flow can gradually lower iron stores. Everyday stress and workload can add to that tiredness too."
            lower.contains("ovulation") ->
                "Ovulation occurs when an ovary releases an egg, roughly two weeks before the next period starts—though exact timing varies widely between people and cycles. Calendar estimates cannot be relied upon for contraception."
            lower.contains("clot") || lower.contains("flow") ->
                "Small blood clots (smaller than a quarter) during heavier flow days happen when natural anticoagulants do not break down blood before it leaves the uterus. Larger or frequent clots with very heavy bleeding warrant medical review."
            else ->
                "Your menstrual cycle involves ongoing communication between the brain and ovaries across four general phases: menstrual, follicular, ovulation, and luteal. Individual symptoms and timing vary widely."
        }

        val tailored = when (followUpStyle) {
            "Explain it simply" -> "In plain terms: $base"
            "What can I do?" -> "Practical comfort ideas: Try a warm compress, steady hydration, gentle movement if it feels good, and logging severity in Tuulit so you can see what helps you personally. Never treat wellness tips as medical care for severe symptoms."
            "Is this common?" -> "Yes—many people experience variations of this, though intensity ranges from barely noticeable to disruptive. Common does not mean you have to simply endure severe discomfort."
            "When should I worry?" -> "Check in with a healthcare clinician if symptoms suddenly worsen, interfere regularly with work, school, or sleep, involve soaking through a pad or tampon every hour, or come with fever, dizziness, or fainting."
            else -> base
        }

        return AiChatMessage(
            id = "explain_${System.currentTimeMillis()}",
            isUser = false,
            text = "[Sample Preview — Enable Firebase AI Logic for live Gemini responses]\n\n$tailored",
            safetyWarning = safety,
            followUpPrompts = listOf(
                "Explain it simply",
                "What can I do?",
                "Is this common?",
                "When should I worry?"
            ),
            isSampleResponse = true
        )
    }

    override suspend fun checkHealthMyth(claim: String): MythCheckResult {
        delay(260)
        val c = claim.lowercase()
        return when {
            c.contains("28") -> MythCheckResult(
                claim = claim,
                verdict = MythVerdict.MYTH,
                explanation = "A 28-day cycle is only an average textbook figure. Healthy adult cycles commonly range from about 21 to 35 days and can vary by several days from month to month.",
                medicalNote = "Tracking your own baseline is far more useful than comparing to 28 days."
            )
            c.contains("exercise") || c.contains("swim") || c.contains("workout") -> MythCheckResult(
                claim = claim,
                verdict = MythVerdict.MYTH,
                explanation = "You do not have to avoid movement or swimming during your period. Gentle movement can even feel relieving for some people—the right choice depends entirely on your comfort and energy.",
                medicalNote = "Listen to your body and rest whenever you feel fatigued or uncomfortable."
            )
            c.contains("severe pain") || c.contains("normal to be in severe") -> MythCheckResult(
                claim = claim,
                verdict = MythVerdict.MYTH,
                explanation = "While mild cramping is common, severe pain that stops you from working, studying, or sleeping is not something you should have to accept without evaluation.",
                medicalNote = "Conditions such as endometriosis, fibroids, or adenomyosis can cause severe period pain and deserve proper medical care."
            )
            c.contains("pregnant") && c.contains("period") -> MythCheckResult(
                claim = claim,
                verdict = MythVerdict.NUANCED,
                explanation = "While the chance is lower during menstruation, pregnancy can still occur—especially in shorter cycles because sperm can survive up to 5 days in the reproductive tract.",
                medicalNote = "Period tracking estimates are never reliable contraception."
            )
            else -> MythCheckResult(
                claim = claim,
                verdict = MythVerdict.NUANCED,
                explanation = "Menstrual and reproductive health claims often oversimplify how varied human bodies are. Evidence-based medical guidance considers your individual cycle history and symptoms.",
                medicalNote = "Enable Firebase AI Logic for live Gemini fact-checking, or verify with a healthcare professional."
            )
        }
    }

    override suspend fun buildAppointmentPrep(
        prediction: CyclePrediction,
        insights: InsightsData,
        painLogs: List<PainLog>,
        userConcern: String
    ): AppointmentPrepSummary {
        delay(250)
        val cycleOverview = buildString {
            append("Recorded completed cycles: ${insights.completedCyclesCount}. ")
            append("Estimated average cycle length: ${insights.averageCycleLength ?: prediction.averageCycleLength} days. ")
            append("Estimated average period duration: ${insights.averagePeriodDuration ?: prediction.averagePeriodDuration} days.")
            insights.cycleVariabilityDays?.let { append(" Variability observed: ~$it days.") }
        }

        val painSummary = if (painLogs.isEmpty()) {
            "No dedicated Pain Journal entries recorded yet."
        } else {
            val avg = painLogs.map { it.intensity }.average()
            val max = painLogs.maxOf { it.intensity }
            val impacts = painLogs.count { it.impactedWorkOrStudy || it.impactedSleep || it.impactedExercise }
            "${painLogs.size} pain entries logged. Average intensity ${String.format("%.1f", avg)}/10 (peak $max/10). Impacted daily life on $impacts recorded occasions."
        }

        val topSymptoms = insights.symptomFrequency.take(4).map { "${it.first} (${it.second} days)" }
            .ifEmpty { listOf("No recurring symptoms logged yet") }

        val questions = buildList {
            if (userConcern.isNotBlank()) {
                add("My main concern to discuss: \"${userConcern.trim()}\"")
            }
            add("Based on my cycle length and variability, do my patterns look within a typical range for me?")
            if (painLogs.any { it.intensity >= 7 }) {
                add("My pain has reached 7/10 or higher and affected daily activities—what evaluations or treatment options should we consider?")
            }
            add("Which warning signs should prompt me to follow up sooner?")
            add("Are there any routine labs (such as iron/ferritin or thyroid checks) worth discussing based on my symptoms?")
        }

        return AppointmentPrepSummary(
            title = "Tuulit Appointment Preparation Sheet",
            cycleOverview = cycleOverview,
            painSummary = painSummary,
            symptomHighlights = topSymptoms,
            suggestedQuestions = questions,
            isSampleResponse = true
        )
    }
}
