package app.tuulit.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import app.tuulit.domain.CycleRecord
import app.tuulit.domain.DailyLog
import app.tuulit.domain.FlowIntensity
import app.tuulit.domain.PainLog
import app.tuulit.domain.SymptomSeverity
import java.util.UUID

/*
 * Every synced table carries `updatedAt` (epoch millis) and a `deleted` tombstone so that
 * changes and deletions can be pushed to Firestore and merged last-write-wins on restore.
 */

fun newId(): String = UUID.randomUUID().toString()

@Entity(tableName = "cycles", indices = [Index(value = ["startEpochDay"], unique = true)])
data class CycleEntity(
    @PrimaryKey val id: String = newId(),
    val startEpochDay: Long,
    val endEpochDay: Long? = null,
    val updatedAt: Long,
    val deleted: Boolean = false,
) {
    fun toDomain() = CycleRecord(id = id, startEpochDay = startEpochDay, endEpochDay = endEpochDay)
}

@Entity(tableName = "daily_logs")
data class DailyLogEntity(
    @PrimaryKey val epochDay: Long,
    val flow: String = FlowIntensity.NONE.name,
    /** "Cramps:2|Bloating:1" */
    val symptoms: String = "",
    /** "Calm|Tired" */
    val moods: String = "",
    /** 1..5 face rating, 0 = unset */
    val moodScore: Int = 0,
    val energyLevel: Int = 0,
    val sleepQuality: Int = 0,
    val waterGlasses: Int = 0,
    val exerciseNote: String = "",
    val medicationNote: String = "",
    val journalText: String = "",
    /** R2 object key of a kept voice note, if the user chose to keep the recording. */
    val voiceNoteKey: String? = null,
    val updatedAt: Long,
    val deleted: Boolean = false,
) {
    fun toDomain(): DailyLog = DailyLog(
        epochDay = epochDay,
        flow = FlowIntensity.fromName(flow),
        symptoms = decodeSymptoms(symptoms),
        moods = moods.split("|").map { it.trim() }.filter { it.isNotEmpty() },
        moodScore = moodScore,
        energyLevel = energyLevel,
        sleepQuality = sleepQuality,
        waterGlasses = waterGlasses,
        exerciseNote = exerciseNote,
        medicationNote = medicationNote,
        journalText = journalText,
        voiceNoteKey = voiceNoteKey,
        updatedAtMillis = updatedAt,
    )

    companion object {
        fun fromDomain(log: DailyLog, now: Long) = DailyLogEntity(
            epochDay = log.epochDay,
            flow = log.flow.name,
            symptoms = encodeSymptoms(log.symptoms),
            moods = log.moods.joinToString("|") { it.replace("|", " ").trim() },
            moodScore = log.moodScore,
            energyLevel = log.energyLevel,
            sleepQuality = log.sleepQuality,
            waterGlasses = log.waterGlasses,
            exerciseNote = log.exerciseNote,
            medicationNote = log.medicationNote,
            journalText = log.journalText,
            voiceNoteKey = log.voiceNoteKey,
            updatedAt = now,
        )

        fun encodeSymptoms(map: Map<String, SymptomSeverity>) =
            map.entries.joinToString("|") { (k, v) -> "${k.replace("|", " ").replace(":", " ").trim()}:${v.level}" }

        fun decodeSymptoms(raw: String): Map<String, SymptomSeverity> = raw.split("|")
            .mapNotNull { entry ->
                val parts = entry.split(":")
                val name = parts.getOrNull(0)?.trim().orEmpty()
                if (name.isEmpty()) null else name to SymptomSeverity.fromLevel(parts.getOrNull(1)?.toIntOrNull() ?: 1)
            }.toMap()
    }
}

@Entity(tableName = "pain_logs", indices = [Index("epochDay")])
data class PainLogEntity(
    @PrimaryKey val id: String = newId(),
    val epochDay: Long,
    val intensity: Int,
    val location: String,
    val painType: String,
    val durationHours: Int,
    val medicationTaken: String,
    val impactedWorkOrStudy: Boolean,
    val impactedExercise: Boolean,
    val impactedSleep: Boolean,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deleted: Boolean = false,
) {
    fun toDomain() = PainLog(
        id = id, epochDay = epochDay, intensity = intensity, location = location, painType = painType,
        durationHours = durationHours, medicationTaken = medicationTaken, impactedWorkOrStudy = impactedWorkOrStudy,
        impactedExercise = impactedExercise, impactedSleep = impactedSleep, notes = notes, createdAtMillis = createdAt,
    )

    companion object {
        fun fromDomain(p: PainLog, now: Long) = PainLogEntity(
            id = p.id.ifEmpty { newId() }, epochDay = p.epochDay, intensity = p.intensity.coerceIn(1, 10),
            location = p.location, painType = p.painType, durationHours = p.durationHours.coerceIn(0, 72),
            medicationTaken = p.medicationTaken, impactedWorkOrStudy = p.impactedWorkOrStudy,
            impactedExercise = p.impactedExercise, impactedSleep = p.impactedSleep, notes = p.notes,
            createdAt = if (p.createdAtMillis > 0) p.createdAtMillis else now, updatedAt = now,
        )
    }
}

/** Personal Comfort Library item. Kind is "comfort" (what works for me) or "kit" (period bag checklist). */
@Entity(tableName = "comfort_items")
data class ComfortItemEntity(
    @PrimaryKey val id: String = newId(),
    val kind: String,
    val title: String,
    val detail: String = "",
    val icon: String = "care",
    val checked: Boolean = false,
    val helpedCount: Int = 0,
    val sessionId: String? = null,
    val position: Int = 0,
    val updatedAt: Long,
    val deleted: Boolean = false,
)

@Entity(tableName = "care_completions", indices = [Index("sessionId")])
data class CareCompletionEntity(
    @PrimaryKey val id: String = newId(),
    val sessionId: String,
    val completedAt: Long,
    val fullyCompleted: Boolean,
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val code: String,
    val unlockedAt: Long,
)

@Entity(tableName = "game_scores")
data class GameScoreEntity(
    @PrimaryKey val gameId: String,
    val bestScore: Int,
    val plays: Int,
    val updatedAt: Long,
)

/** AI conversations stay on the device. Only the question being asked leaves it. */
@Entity(tableName = "ai_messages", indices = [Index("conversationId")])
data class AiMessageEntity(
    @PrimaryKey val id: String = newId(),
    val conversationId: String,
    val role: String,
    val text: String,
    val tool: String = "chat",
    val urgency: String = "NONE",
    val usedContext: Boolean = false,
    val failed: Boolean = false,
    val createdAt: Long,
)
