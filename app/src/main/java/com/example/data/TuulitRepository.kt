package com.example.data

import com.example.domain.CycleRecord
import com.example.domain.DailyLog
import com.example.domain.PainLog
import com.example.domain.SampleTuulitAiProvider
import com.example.domain.TuulitAiProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class TuulitRepository(
    private val dao: TuulitDao,
    val preferences: UserPreferencesRepository,
    val aiProvider: TuulitAiProvider = SampleTuulitAiProvider(),
) {
    val cyclesFlow: Flow<List<CycleRecord>> = dao.observeCycles().map { list -> list.map { it.toDomain() } }
    val dailyLogsFlow: Flow<List<DailyLog>> = dao.observeDailyLogs().map { list -> list.map { it.toDomain() } }
    val painLogsFlow: Flow<List<PainLog>> = dao.observePainLogs().map { list -> list.map { it.toDomain() } }
    val survivalKitFlow: Flow<List<SurvivalKitItemEntity>> = dao.observeSurvivalKit()
    val checklistFlow: Flow<List<EmergencyChecklistItemEntity>> = dao.observeChecklist()
    val achievementsFlow: Flow<List<AchievementEntity>> = dao.observeAchievements()
    val trustedContactsFlow: Flow<List<TrustedContactEntity>> = dao.observeTrustedContacts()
    val gameScoresFlow: Flow<List<GameScoreEntity>> = dao.observeGameScores()

    suspend fun ensureDefaultUtilityListsSeeded() {
        if (dao.observeChecklist().first().isEmpty()) {
            val defaults = listOf(
                "Pads, tampons, or menstrual cup",
                "Spare underwear",
                "Unscented cleansing wipes",
                "Heat patch or pain-relief essentials",
                "Reusable pouch or storage bag",
                "Water bottle & light snack"
            )
            defaults.forEach { dao.upsertChecklistItem(EmergencyChecklistItemEntity(title = it, packed = false)) }
        }
        if (dao.observeSurvivalKit().first().isEmpty()) {
            val starterKit = listOf(
                SurvivalKitItemEntity(title = "Heating pad or warm compress", category = "Comfort", detail = "15–20 minutes over lower abdomen"),
                SurvivalKitItemEntity(title = "5-minute guided breathing", category = "Activity", detail = "Inhale 4s, hold 4s, exhale 6s in Tuulit Care"),
                SurvivalKitItemEntity(title = "Banana Catch quick round", category = "Game", detail = "Light distraction in Tuulit Play"),
                SurvivalKitItemEntity(title = "Warm herbal tea & dark chocolate", category = "Food", detail = "Gentle comfort snack"),
            )
            starterKit.forEach { dao.upsertSurvivalKitItem(it) }
        }
    }

    suspend fun saveCycle(record: CycleRecord) {
        dao.upsertCycle(
            CycleRecordEntity(
                id = record.id,
                startEpochDay = record.startEpochDay,
                endEpochDay = record.endEpochDay
            )
        )
        unlockAchievement("first_cycle", "First Cycle Tracked", "Recorded your first cycle start date.")
    }

    suspend fun deleteCycle(cycleId: Long) = dao.deleteCycle(cycleId)

    suspend fun saveDailyLog(log: DailyLog) {
        dao.upsertDailyLog(DailyLogEntity.fromDomain(log))
        if (log.moods.isNotEmpty()) {
            unlockAchievement("first_mood", "First Mood Check-In", "Checked in with how you were feeling.")
        }
        if (log.journalText.isNotBlank()) {
            unlockAchievement("first_journal", "First Journal Note", "Saved a private reflection in Tuulit.")
        }
    }

    suspend fun deleteDailyLog(epochDay: Long) = dao.deleteDailyLog(epochDay)

    suspend fun savePainLog(log: PainLog) {
        dao.insertPainLog(
            PainLogEntity(
                id = log.id,
                epochDay = log.epochDay,
                intensity = log.intensity,
                location = log.location,
                painType = log.painType,
                durationHours = log.durationHours,
                medicationTaken = log.medicationTaken,
                impactedWorkOrStudy = log.impactedWorkOrStudy,
                impactedExercise = log.impactedExercise,
                impactedSleep = log.impactedSleep,
                notes = log.notes
            )
        )
        unlockAchievement("first_pain_log", "Body Advocate", "Logged a structured entry in your Pain Journal.")
    }

    suspend fun deletePainLog(id: Long) = dao.deletePainLog(id)

    suspend fun upsertSurvivalKitItem(item: SurvivalKitItemEntity) {
        dao.upsertSurvivalKitItem(item)
        unlockAchievement("survival_kit", "Survival Kit Ready", "Personalized your Period Survival Kit.")
    }

    suspend fun deleteSurvivalKitItem(id: Long) = dao.deleteSurvivalKitItem(id)

    suspend fun upsertChecklistItem(item: EmergencyChecklistItemEntity) = dao.upsertChecklistItem(item)

    suspend fun deleteChecklistItem(id: Long) = dao.deleteChecklistItem(id)

    suspend fun unlockAchievement(code: String, title: String, description: String) {
        dao.unlockAchievement(AchievementEntity(code = code, title = title, description = description))
    }

    suspend fun upsertTrustedContact(contact: TrustedContactEntity) = dao.upsertTrustedContact(contact)

    suspend fun deleteTrustedContact(id: Long) = dao.deleteTrustedContact(id)

    suspend fun recordGameResult(gameId: String, score: Int) {
        val existing = dao.getGameScore(gameId)
        val best = maxOf(existing?.highScore ?: 0, score)
        val plays = (existing?.playsCount ?: 0) + 1
        dao.upsertGameScore(GameScoreEntity(gameId = gameId, highScore = best, playsCount = plays))
        unlockAchievement("played_game", "Playful Break", "Took a distraction break in Tuulit Play.")
    }

    suspend fun wipeAllData() {
        dao.clearCycles()
        dao.clearDailyLogs()
        dao.clearPainLogs()
        dao.clearSurvivalKit()
        dao.clearChecklist()
        dao.clearAchievements()
        dao.clearTrustedContacts()
        dao.clearGameScores()
        preferences.clearAllPreferences()
    }

    fun formatExportCsv(
        cycles: List<CycleRecord>,
        dailyLogs: List<DailyLog>,
        painLogs: List<PainLog>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("TUULIT HEALTH & SYMPTOM EXPORT")
        sb.appendLine("Note: Informational personal log only. Not a medical diagnosis.")
        sb.appendLine()
        sb.appendLine("SECTION: CYCLES")
        sb.appendLine("StartDate,EndDate")
        cycles.sortedBy { it.startEpochDay }.forEach { c ->
            sb.appendLine("${epochDayToIso(c.startEpochDay)},${c.endEpochDay?.let { epochDayToIso(it) } ?: "Ongoing"}")
        }
        sb.appendLine()
        sb.appendLine("SECTION: DAILY LOGS")
        sb.appendLine("Date,Flow,Energy(1-5),Sleep(1-5),Moods,Symptoms")
        dailyLogs.sortedByDescending { it.epochDay }.forEach { l ->
            val moods = l.moods.joinToString("; ")
            val symptoms = l.symptoms.entries.joinToString("; ") { "${it.key} (${it.value.label})" }
            sb.appendLine("${epochDayToIso(l.epochDay)},${l.flow.label},${l.energyLevel},${l.sleepQuality},\"$moods\",\"$symptoms\"")
        }
        sb.appendLine()
        sb.appendLine("SECTION: PAIN JOURNAL")
        sb.appendLine("Date,Intensity(1-10),Location,Type,DurationHours,Medication,DailyImpact,Notes")
        painLogs.sortedByDescending { it.epochDay }.forEach { p ->
            val impacts = listOfNotNull(
                if (p.impactedWorkOrStudy) "Work/Study" else null,
                if (p.impactedExercise) "Exercise" else null,
                if (p.impactedSleep) "Sleep" else null
            ).joinToString("; ").ifBlank { "None" }
            sb.appendLine("${epochDayToIso(p.epochDay)},${p.intensity},\"${p.location}\",\"${p.painType}\",${p.durationHours},\"${p.medicationTaken}\",\"$impacts\",\"${p.notes.replace("\"", "'")}\"")
        }
        return sb.toString()
    }

    companion object {
        fun epochDayToIso(epochDay: Long): String {
            val millis = epochDay * 86_400_000L
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            return sdf.format(Date(millis))
        }

        fun epochDayToReadable(epochDay: Long): String {
            val millis = epochDay * 86_400_000L
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            return sdf.format(Date(millis))
        }

        fun todayEpochDay(): Long {
            val now = System.currentTimeMillis()
            val offset = TimeZone.getDefault().getOffset(now)
            return (now + offset) / 86_400_000L
        }
    }
}
