package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.CycleRecord
import com.example.domain.DailyLog
import com.example.domain.FlowIntensity
import com.example.domain.PainLog
import com.example.domain.SymptomSeverity

@Entity(tableName = "cycle_records")
data class CycleRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val startEpochDay: Long,
    val endEpochDay: Long? = null,
) {
    fun toDomain() = CycleRecord(id = id, startEpochDay = startEpochDay, endEpochDay = endEpochDay)
}

@Entity(tableName = "daily_logs")
data class DailyLogEntity(
    @PrimaryKey val epochDay: Long,
    val flowName: String = FlowIntensity.NONE.name,
    val symptomsSerialized: String = "", // format: "Cramps:2|Bloating:1"
    val moodsSerialized: String = "", // format: "Calm|Tired"
    val energyLevel: Int = 0,
    val sleepQuality: Int = 0,
    val waterGlasses: Int = 0,
    val exerciseNote: String = "",
    val stressLevel: Int = 0,
    val medicationNote: String = "",
    val optionalIntimateNote: String = "",
    val journalText: String = "",
    val updatedAtMillis: Long = System.currentTimeMillis(),
) {
    fun toDomain(): DailyLog {
        val symptomsMap = symptomsSerialized
            .split("|")
            .filter { it.isNotBlank() }
            .mapNotNull { entry ->
                val parts = entry.split(":")
                val name = parts.getOrNull(0)?.trim().orEmpty()
                val level = parts.getOrNull(1)?.toIntOrNull() ?: 1
                if (name.isNotBlank()) name to SymptomSeverity.fromLevel(level) else null
            }.toMap()

        val moodsList = moodsSerialized
            .split("|")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return DailyLog(
            epochDay = epochDay,
            flow = FlowIntensity.fromName(flowName),
            symptoms = symptomsMap,
            moods = moodsList,
            energyLevel = energyLevel,
            sleepQuality = sleepQuality,
            waterGlasses = waterGlasses,
            exerciseNote = exerciseNote,
            stressLevel = stressLevel,
            medicationNote = medicationNote,
            optionalIntimateNote = optionalIntimateNote,
            journalText = journalText,
            updatedAtMillis = updatedAtMillis
        )
    }

    companion object {
        fun fromDomain(log: DailyLog): DailyLogEntity {
            val symptomsStr = log.symptoms.entries.joinToString("|") { (k, v) ->
                "${k.replace("|", " ").replace(":", " ")}:${v.level}"
            }
            val moodsStr = log.moods.joinToString("|") { it.replace("|", " ") }
            return DailyLogEntity(
                epochDay = log.epochDay,
                flowName = log.flow.name,
                symptomsSerialized = symptomsStr,
                moodsSerialized = moodsStr,
                energyLevel = log.energyLevel,
                sleepQuality = log.sleepQuality,
                waterGlasses = log.waterGlasses,
                exerciseNote = log.exerciseNote,
                stressLevel = log.stressLevel,
                medicationNote = log.medicationNote,
                optionalIntimateNote = log.optionalIntimateNote,
                journalText = log.journalText,
                updatedAtMillis = System.currentTimeMillis()
            )
        }
    }
}

@Entity(tableName = "pain_logs")
data class PainLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
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
    val createdAtMillis: Long = System.currentTimeMillis(),
) {
    fun toDomain() = PainLog(
        id = id,
        epochDay = epochDay,
        intensity = intensity,
        location = location,
        painType = painType,
        durationHours = durationHours,
        medicationTaken = medicationTaken,
        impactedWorkOrStudy = impactedWorkOrStudy,
        impactedExercise = impactedExercise,
        impactedSleep = impactedSleep,
        notes = notes,
        createdAtMillis = createdAtMillis
    )
}

@Entity(tableName = "survival_kit_items")
data class SurvivalKitItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val category: String, // Comfort, Game, Activity, Food, Routine, Note
    val detail: String = "",
    val pinned: Boolean = true,
)

@Entity(tableName = "emergency_checklist_items")
data class EmergencyChecklistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val packed: Boolean = false,
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val code: String,
    val title: String,
    val description: String,
    val unlockedAtMillis: Long = System.currentTimeMillis(),
)

@Entity(tableName = "trusted_contacts")
data class TrustedContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val relationship: String,
    val inviteCode: String,
    val sharePeriodApproaching: Boolean = true,
    val sharePeriodOngoing: Boolean = true,
    val shareCyclePhase: Boolean = false,
    val shareMoods: Boolean = false,
    val shareSymptoms: Boolean = false,
    val allowSosRequests: Boolean = true,
)

@Entity(tableName = "game_scores")
data class GameScoreEntity(
    @PrimaryKey val gameId: String,
    val highScore: Int,
    val playsCount: Int,
)
