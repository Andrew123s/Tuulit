package app.tuulit.data

import app.tuulit.data.local.CycleEntity
import app.tuulit.data.local.DailyLogEntity
import app.tuulit.data.local.PainLogEntity
import app.tuulit.data.local.TuulitDao
import app.tuulit.domain.CycleRecord
import app.tuulit.domain.DailyLog
import app.tuulit.domain.Dates
import app.tuulit.domain.FlowIntensity
import app.tuulit.domain.PainLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Notified after every local write so the sync layer can push changes when signed in. */
fun interface ChangeListener { fun onLocalChange() }

/**
 * Cycles, daily logs and pain entries. Room is the source of truth; every write stamps
 * `updatedAt` and deletes become tombstones so they can be synced.
 */
class TrackingRepository(
    private val dao: TuulitDao,
    private val changes: ChangeListener,
    private val now: () -> Long = { Dates.nowMillis() },
) {
    val cycles: Flow<List<CycleRecord>> = dao.observeCycles().map { l -> l.map { it.toDomain() } }
    val dailyLogs: Flow<List<DailyLog>> = dao.observeDailyLogs().map { l -> l.map { it.toDomain() } }
    val painLogs: Flow<List<PainLog>> = dao.observePainLogs().map { l -> l.map { it.toDomain() } }

    suspend fun dailyLog(day: Long): DailyLog? = dao.dailyLog(day)?.takeIf { !it.deleted }?.toDomain()

    /** Saves a day's log and keeps the cycle table consistent with the flow that was logged. */
    suspend fun saveDailyLog(log: DailyLog) {
        val previous = dao.dailyLog(log.epochDay)?.takeIf { !it.deleted }?.toDomain()
        val clean = log.copy(
            moods = log.moods.map { it.trim() }.filter { it.isNotEmpty() }.distinct(),
            energyLevel = log.energyLevel.coerceIn(0, 5),
            sleepQuality = log.sleepQuality.coerceIn(0, 5),
            moodScore = log.moodScore.coerceIn(0, 5),
            waterGlasses = log.waterGlasses.coerceIn(0, 30),
            journalText = log.journalText.take(4000),
        )
        if (clean.isEmpty()) {
            dao.dailyLog(log.epochDay)?.let { dao.upsertDailyLog(it.copy(deleted = true, updatedAt = now())) }
        } else {
            dao.upsertDailyLog(DailyLogEntity.fromDomain(clean, now()))
        }
        val wasFlow = (previous?.flow?.level ?: 0) >= FlowIntensity.LIGHT.level
        val isFlow = clean.flow.level >= FlowIntensity.LIGHT.level
        if (isFlow && !wasFlow) reconcileFlowAdded(log.epochDay)
        if (!isFlow && wasFlow) reconcileFlowRemoved(log.epochDay)
        changes.onLocalChange()
    }

    suspend fun deleteDailyLog(day: Long) {
        val existing = dao.dailyLog(day) ?: return
        val hadFlow = FlowIntensity.fromName(existing.flow).level >= FlowIntensity.LIGHT.level
        dao.upsertDailyLog(existing.copy(deleted = true, updatedAt = now()))
        if (hadFlow) reconcileFlowRemoved(day)
        changes.onLocalChange()
    }

    /** "My period started" from Today or onboarding. Duplicate-safe. */
    suspend fun logPeriodStart(day: Long) {
        val existing = dao.dailyLog(day)?.takeIf { !it.deleted }?.toDomain() ?: DailyLog(epochDay = day)
        if (existing.flow.level < FlowIntensity.LIGHT.level) {
            saveDailyLog(existing.copy(flow = FlowIntensity.MEDIUM))
        } else {
            reconcileFlowAdded(day)
            changes.onLocalChange()
        }
    }

    /**
     * Records a past period from onboarding ("When did your last period start?") without
     * inventing any daily flow entries. The end is only set once that period is clearly over.
     */
    suspend fun addPastPeriod(start: Long, typicalLength: Int, today: Long) {
        if (start > today) return
        if (dao.cycles().any { kotlin.math.abs(it.startEpochDay - start) < 10 }) return
        val end = start + typicalLength.coerceIn(1, 14) - 1
        val tomb = dao.cycleStartingOn(start)
        val entity = CycleEntity(id = tomb?.id ?: app.tuulit.data.local.newId(), startEpochDay = start,
            endEpochDay = if (end < today) end else null, updatedAt = now())
        dao.upsertCycle(entity)
        changes.onLocalChange()
    }

    suspend fun logPeriodEnd(day: Long) {
        val c = dao.cycles().lastOrNull { it.startEpochDay <= day } ?: return
        dao.upsertCycle(c.copy(endEpochDay = day.coerceAtLeast(c.startEpochDay), updatedAt = now()))
        changes.onLocalChange()
    }

    /** Edits a cycle from History. Returns an error message when the edit is invalid. */
    suspend fun editCycle(id: String, start: Long, end: Long?): String? {
        if (end != null && end < start) return "The end can't be before the start."
        if (end != null && end - start > 14) return "A period longer than 15 days isn't supported. Check the dates."
        if (start > Dates.today()) return "The start can't be in the future."
        val clash = dao.cycles().firstOrNull { it.id != id && kotlin.math.abs(it.startEpochDay - start) < 10 }
        if (clash != null) return "Another period starts within 10 days of that date."
        val c = dao.cycle(id) ?: return "That cycle no longer exists."
        dao.upsertCycle(c.copy(startEpochDay = start, endEpochDay = end, updatedAt = now()))
        changes.onLocalChange()
        return null
    }

    suspend fun deleteCycle(id: String) {
        val c = dao.cycle(id) ?: return
        dao.upsertCycle(c.copy(deleted = true, updatedAt = now()))
        changes.onLocalChange()
    }

    suspend fun savePainLog(p: PainLog) {
        dao.upsertPainLog(PainLogEntity.fromDomain(p, now()))
        changes.onLocalChange()
    }

    suspend fun deletePainLog(id: String) {
        val e = dao.painLog(id) ?: return
        dao.upsertPainLog(e.copy(deleted = true, updatedAt = now()))
        changes.onLocalChange()
    }

    // --- cycle reconciliation -------------------------------------------------------------

    private suspend fun reconcileFlowAdded(day: Long) {
        val cycles = dao.cycles()
        // Inside or right after an existing period: extend it.
        val current = cycles.lastOrNull { it.startEpochDay <= day && day - it.startEpochDay < 12 }
        if (current != null) {
            val end = current.endEpochDay
            if (end != null && day > end) dao.upsertCycle(current.copy(endEpochDay = day, updatedAt = now()))
            return
        }
        // Logged a day or three before a period that already exists: move its start earlier.
        val following = cycles.firstOrNull { it.startEpochDay in (day + 1)..(day + 3) }
        if (following != null) {
            dao.upsertCycle(following.copy(startEpochDay = day, updatedAt = now()))
            return
        }
        val tomb = dao.cycleStartingOn(day)
        if (tomb != null) {
            dao.upsertCycle(tomb.copy(deleted = false, endEpochDay = null, updatedAt = now()))
        } else {
            dao.upsertCycle(CycleEntity(startEpochDay = day, updatedAt = now()))
        }
    }

    private suspend fun reconcileFlowRemoved(day: Long) {
        val cycle = dao.cycles().lastOrNull { it.startEpochDay <= day && day - it.startEpochDay < 12 } ?: return
        val flowDays = (cycle.startEpochDay..(cycle.startEpochDay + 11)).filter { d ->
            d != day && dao.dailyLog(d)?.let { !it.deleted && FlowIntensity.fromName(it.flow).level >= FlowIntensity.LIGHT.level } == true
        }
        when {
            flowDays.isEmpty() -> dao.upsertCycle(cycle.copy(deleted = true, updatedAt = now()))
            day == cycle.startEpochDay -> dao.upsertCycle(cycle.copy(startEpochDay = flowDays.first(), updatedAt = now()))
            cycle.endEpochDay == day -> dao.upsertCycle(cycle.copy(endEpochDay = flowDays.last(), updatedAt = now()))
        }
    }

    // --- export -----------------------------------------------------------------------------

    /** CSV of every log and pain entry. Built on demand; never written anywhere except the share cache. */
    suspend fun exportCsv(): String {
        val sb = StringBuilder()
        sb.appendLine("type,date,flow,mood_score,moods,symptoms,energy,sleep,water,medication,notes,pain_intensity,pain_location,pain_type,pain_hours,impact")
        dao.allDailyLogs().map { it.toDomain() }.forEach { l ->
            sb.appendLine(listOf(
                "day", Dates.iso(l.epochDay), l.flow.label, l.moodScore.toString(), l.moods.joinToString(";"),
                l.symptoms.entries.joinToString(";") { "${it.key} (${it.value.label})" }, l.energyLevel.toString(),
                l.sleepQuality.toString(), l.waterGlasses.toString(), l.medicationNote, l.journalText, "", "", "", "", "",
            ).joinToString(",") { csv(it) })
        }
        dao.allPainLogs().map { it.toDomain() }.forEach { p ->
            val impact = listOfNotNull("work/study".takeIf { p.impactedWorkOrStudy }, "exercise".takeIf { p.impactedExercise }, "sleep".takeIf { p.impactedSleep })
            sb.appendLine(listOf(
                "pain", Dates.iso(p.epochDay), "", "", "", "", "", "", "", p.medicationTaken, p.notes,
                p.intensity.toString(), p.location, p.painType, p.durationHours.toString(), impact.joinToString(";"),
            ).joinToString(",") { csv(it) })
        }
        return sb.toString()
    }

    private fun csv(v: String): String {
        val safe = if (v.startsWith("=") || v.startsWith("+") || v.startsWith("-") || v.startsWith("@")) "'$v" else v
        return if (safe.any { it == ',' || it == '"' || it == '\n' }) "\"" + safe.replace("\"", "\"\"") + "\"" else safe
    }
}

fun DailyLog.isEmpty(): Boolean = flow == FlowIntensity.NONE && symptoms.isEmpty() && moods.isEmpty() && moodScore == 0 &&
    energyLevel == 0 && sleepQuality == 0 && waterGlasses == 0 && exerciseNote.isBlank() && medicationNote.isBlank() &&
    journalText.isBlank() && voiceNoteKey == null
