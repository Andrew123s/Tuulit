package app.tuulit.data.remote

import app.tuulit.data.local.AiMessageEntity
import app.tuulit.data.local.TuulitDao
import app.tuulit.domain.CyclePrediction
import app.tuulit.domain.Dates
import app.tuulit.domain.InsightsData
import app.tuulit.domain.MedicalSafetyGuard
import app.tuulit.domain.UrgencyLevel
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

enum class AiTool(val wire: String) { CHAT("chat"), EXPLAIN("explain"), MYTH("myth"), APPOINTMENT("appointment") }

/**
 * Minimal, consented summary of the person's data. Only built when "Personal answers in Ask"
 * is on, and it never contains notes, journal text or dates of individual entries.
 */
data class AiContext(
    val cycleDay: Int?,
    val phase: String,
    val avgCycleLength: Int?,
    val avgPeriodLength: Int?,
    val variabilityDays: Int?,
    val topSymptoms: List<String>,
    val severePainCycles: Int,
) {
    fun toJson() = JSONObject()
        .put("cycleDay", cycleDay ?: JSONObject.NULL)
        .put("phase", phase)
        .put("avgCycleLength", avgCycleLength ?: JSONObject.NULL)
        .put("avgPeriodLength", avgPeriodLength ?: JSONObject.NULL)
        .put("variabilityDays", variabilityDays ?: JSONObject.NULL)
        .put("topSymptoms", JSONArray(topSymptoms))
        .put("severePainCycles", severePainCycles)

    companion object {
        fun from(p: CyclePrediction, i: InsightsData) = AiContext(
            cycleDay = p.currentCycleDay, phase = p.currentPhase.title, avgCycleLength = i.averageCycleLength,
            avgPeriodLength = i.averagePeriodDuration, variabilityDays = i.cycleVariabilityDays,
            topSymptoms = i.symptomFrequency.take(4).map { it.first },
            severePainCycles = i.painPatternAlert?.consecutiveHighPainCycles ?: 0,
        )
    }
}

data class AiReply(val text: String, val urgency: UrgencyLevel, val verdict: String?)

/**
 * Sends questions to the Worker's /v1/ai endpoint, which talks to the configured AI provider.
 * Conversations are stored only on this device.
 */
class AiRepository(private val api: ApiClient, private val dao: TuulitDao) {

    fun conversation(id: String): Flow<List<AiMessageEntity>> = dao.observeConversation(id)

    suspend fun clear(conversationId: String) = dao.clearConversation(conversationId)

    /** Stores the question, asks the server, stores the answer. Throws [ApiError] on failure. */
    suspend fun ask(conversationId: String, tool: AiTool, question: String, context: AiContext?): AiReply {
        val local = MedicalSafetyGuard.assess(question)
        dao.upsertMessage(AiMessageEntity(conversationId = conversationId, role = "user", text = question.trim(),
            tool = tool.wire, urgency = local.level.name, createdAt = Dates.nowMillis()))
        val history = dao.conversation(conversationId).takeLast(12)
        val body = JSONObject()
            .put("tool", tool.wire)
            .put("messages", JSONArray().apply {
                history.filter { !it.failed }.forEach { put(JSONObject().put("role", it.role).put("content", it.text)) }
            })
            .put("locale", java.util.Locale.getDefault().toLanguageTag())
        if (context != null) body.put("context", context.toJson())
        try {
            val res = api.post("/v1/ai", body)
            val urgency = runCatching { UrgencyLevel.valueOf(res.optString("urgency", "NONE")) }.getOrDefault(UrgencyLevel.NONE)
            val worst = if (urgency.ordinal > local.level.ordinal) urgency else local.level
            val reply = AiReply(res.optString("text").trim(), worst, res.optString("verdict").takeIf { it.isNotEmpty() })
            dao.upsertMessage(AiMessageEntity(conversationId = conversationId, role = "assistant", text = reply.text,
                tool = tool.wire, urgency = worst.name, usedContext = context != null, createdAt = Dates.nowMillis()))
            return reply
        } catch (e: ApiError) {
            dao.upsertMessage(AiMessageEntity(conversationId = conversationId, role = "assistant", text = e.message.orEmpty(),
                tool = tool.wire, urgency = local.level.name, failed = true, createdAt = Dates.nowMillis()))
            throw e
        }
    }
}
