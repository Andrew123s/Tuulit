package app.tuulit.data.remote

import android.util.Log
import app.tuulit.data.local.AchievementEntity
import app.tuulit.data.local.ComfortItemEntity
import app.tuulit.data.local.CycleEntity
import app.tuulit.data.local.DailyLogEntity
import app.tuulit.data.local.GameScoreEntity
import app.tuulit.data.local.PainLogEntity
import app.tuulit.data.local.PreferencesRepository
import app.tuulit.data.local.TuulitDao
import app.tuulit.domain.Dates
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

sealed interface SyncResult {
    object Skipped : SyncResult
    data class Done(val pushed: Int, val pulled: Int) : SyncResult
    data class Failed(val message: String) : SyncResult
}

/**
 * Backs up the encrypted local database to Firestore under users/{uid}/..., which only that
 * user can read or write (see firebase/firestore.rules). Writes are batched and only rows
 * changed since the last sync are sent, to stay well inside the Spark plan's daily quotas.
 */
class SyncRepository(
    private val firebase: Firebase,
    private val auth: AuthRepository,
    private val dao: TuulitDao,
    private val prefs: PreferencesRepository,
) {
    private val mutex = Mutex()

    suspend fun sync(fullPull: Boolean = false): SyncResult = mutex.withLock {
        val db = firebase.firestore ?: return SyncResult.Skipped
        val uid = auth.currentUid ?: return SyncResult.Skipped
        try {
            val startedAt = Dates.nowMillis()
            val last = prefs.current().lastSyncAt
            val pulled = if (fullPull || last == 0L) pull(db, uid) else 0
            val pushed = push(db, uid, if (fullPull) 0 else last)
            prefs.setLastSync(startedAt)
            SyncResult.Done(pushed, pulled)
        } catch (e: Exception) {
            Log.w("TuulitSync", "sync failed", e)
            SyncResult.Failed(e.message ?: "Sync failed")
        }
    }

    private fun userDoc(db: FirebaseFirestore, uid: String) = db.collection("users").document(uid)

    private suspend fun push(db: FirebaseFirestore, uid: String, since: Long): Int {
        val root = userDoc(db, uid)
        val writes = mutableListOf<Pair<com.google.firebase.firestore.DocumentReference, Map<String, Any?>>>()
        dao.cyclesChangedSince(since).forEach { c ->
            writes += root.collection("cycles").document(c.id) to mapOf(
                "startEpochDay" to c.startEpochDay, "endEpochDay" to c.endEpochDay, "updatedAt" to c.updatedAt, "deleted" to c.deleted)
        }
        dao.logsChangedSince(since).forEach { l ->
            writes += root.collection("dailyLogs").document(l.epochDay.toString()) to mapOf(
                "epochDay" to l.epochDay, "flow" to l.flow, "symptoms" to l.symptoms, "moods" to l.moods, "moodScore" to l.moodScore,
                "energyLevel" to l.energyLevel, "sleepQuality" to l.sleepQuality, "waterGlasses" to l.waterGlasses,
                "exerciseNote" to l.exerciseNote, "medicationNote" to l.medicationNote, "journalText" to l.journalText,
                "voiceNoteKey" to l.voiceNoteKey, "updatedAt" to l.updatedAt, "deleted" to l.deleted)
        }
        dao.painChangedSince(since).forEach { p ->
            writes += root.collection("painLogs").document(p.id) to mapOf(
                "epochDay" to p.epochDay, "intensity" to p.intensity, "location" to p.location, "painType" to p.painType,
                "durationHours" to p.durationHours, "medicationTaken" to p.medicationTaken, "impactedWorkOrStudy" to p.impactedWorkOrStudy,
                "impactedExercise" to p.impactedExercise, "impactedSleep" to p.impactedSleep, "notes" to p.notes,
                "createdAt" to p.createdAt, "updatedAt" to p.updatedAt, "deleted" to p.deleted)
        }
        dao.comfortChangedSince(since).forEach { i ->
            writes += root.collection("comfort").document(i.id) to mapOf(
                "kind" to i.kind, "title" to i.title, "detail" to i.detail, "icon" to i.icon, "checked" to i.checked,
                "helpedCount" to i.helpedCount, "sessionId" to i.sessionId, "position" to i.position, "updatedAt" to i.updatedAt, "deleted" to i.deleted)
        }
        dao.gamesChangedSince(since).forEach { g ->
            writes += root.collection("games").document(g.gameId) to mapOf("bestScore" to g.bestScore, "plays" to g.plays, "updatedAt" to g.updatedAt)
        }
        dao.achievements().filter { it.unlockedAt > since }.forEach { a ->
            writes += root.collection("achievements").document(a.code) to mapOf("unlockedAt" to a.unlockedAt)
        }
        writes.chunked(400).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { (ref, data) -> batch.set(ref, data) }
            batch.commit().await()
        }
        if (writes.isNotEmpty() || since == 0L) {
            userDoc(db, uid).set(mapOf("lastBackupAt" to FieldValue.serverTimestamp()), SetOptions.merge()).await()
        }
        return writes.size
    }

    /** Restores from the cloud, keeping whichever copy of a row was changed most recently. */
    private suspend fun pull(db: FirebaseFirestore, uid: String): Int {
        val root = userDoc(db, uid)
        var n = 0
        root.collection("cycles").get().await().documents.forEach { d ->
            val remote = CycleEntity(d.id, d.long("startEpochDay"), d.getLong("endEpochDay"), d.long("updatedAt"), d.getBoolean("deleted") == true)
            val local = dao.cycle(d.id)
            if (local == null || local.updatedAt < remote.updatedAt) {
                // A different local cycle may already own this start date: the newer one wins.
                val sameStart = dao.cycleStartingOn(remote.startEpochDay)
                if (sameStart == null || sameStart.id == remote.id || sameStart.updatedAt < remote.updatedAt) {
                    if (sameStart != null && sameStart.id != remote.id) dao.upsertCycle(sameStart.copy(deleted = true, startEpochDay = -sameStart.startEpochDay - 1, updatedAt = remote.updatedAt))
                    dao.upsertCycle(remote); n++
                }
            }
        }
        root.collection("dailyLogs").get().await().documents.forEach { d ->
            val remote = DailyLogEntity(
                epochDay = d.long("epochDay"), flow = d.str("flow"), symptoms = d.str("symptoms"), moods = d.str("moods"),
                moodScore = d.int("moodScore"), energyLevel = d.int("energyLevel"), sleepQuality = d.int("sleepQuality"),
                waterGlasses = d.int("waterGlasses"), exerciseNote = d.str("exerciseNote"), medicationNote = d.str("medicationNote"),
                journalText = d.str("journalText"), voiceNoteKey = d.getString("voiceNoteKey"), updatedAt = d.long("updatedAt"),
                deleted = d.getBoolean("deleted") == true)
            val local = dao.dailyLog(remote.epochDay)
            if (local == null || local.updatedAt < remote.updatedAt) { dao.upsertDailyLog(remote); n++ }
        }
        root.collection("painLogs").get().await().documents.forEach { d ->
            val remote = PainLogEntity(d.id, d.long("epochDay"), d.int("intensity"), d.str("location"), d.str("painType"), d.int("durationHours"),
                d.str("medicationTaken"), d.getBoolean("impactedWorkOrStudy") == true, d.getBoolean("impactedExercise") == true,
                d.getBoolean("impactedSleep") == true, d.str("notes"), d.long("createdAt"), d.long("updatedAt"), d.getBoolean("deleted") == true)
            val local = dao.painLog(d.id)
            if (local == null || local.updatedAt < remote.updatedAt) { dao.upsertPainLog(remote); n++ }
        }
        root.collection("comfort").get().await().documents.forEach { d ->
            val remote = ComfortItemEntity(d.id, d.str("kind"), d.str("title"), d.str("detail"), d.str("icon").ifEmpty { "care" },
                d.getBoolean("checked") == true, d.int("helpedCount"), d.getString("sessionId"), d.int("position"), d.long("updatedAt"),
                d.getBoolean("deleted") == true)
            val local = dao.comfortItem(d.id)
            if (local == null || local.updatedAt < remote.updatedAt) { dao.upsertComfortItem(remote); n++ }
        }
        root.collection("games").get().await().documents.forEach { d ->
            val local = dao.gameScore(d.id)
            val best = maxOf(local?.bestScore ?: 0, d.int("bestScore"))
            dao.upsertGameScore(GameScoreEntity(d.id, best, maxOf(local?.plays ?: 0, d.int("plays")), maxOf(local?.updatedAt ?: 0, d.long("updatedAt"))))
        }
        root.collection("achievements").get().await().documents.forEach { d ->
            dao.unlock(AchievementEntity(d.id, d.long("unlockedAt")))
        }
        return n
    }

    /** Deletes the user's cloud backup (used before account deletion and by "Delete cloud backup"). */
    suspend fun deleteCloudData(): Boolean {
        val db = firebase.firestore ?: return false
        val uid = auth.currentUid ?: return false
        val root = userDoc(db, uid)
        for (c in listOf("cycles", "dailyLogs", "painLogs", "comfort", "games", "achievements", "devices")) {
            var docs = root.collection(c).limit(400).get().await().documents
            while (docs.isNotEmpty()) {
                val batch = db.batch()
                docs.forEach { batch.delete(it.reference) }
                batch.commit().await()
                docs = root.collection(c).limit(400).get().await().documents
            }
        }
        root.delete().await()
        prefs.setLastSync(0)
        return true
    }

    private fun DocumentSnapshot.long(k: String) = getLong(k) ?: 0L
    private fun DocumentSnapshot.int(k: String) = (getLong(k) ?: 0L).toInt()
    private fun DocumentSnapshot.str(k: String) = getString(k).orEmpty()
}
