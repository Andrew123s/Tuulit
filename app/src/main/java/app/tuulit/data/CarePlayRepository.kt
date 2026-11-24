package app.tuulit.data

import app.tuulit.data.local.AchievementEntity
import app.tuulit.data.local.CareCompletionEntity
import app.tuulit.data.local.ComfortItemEntity
import app.tuulit.data.local.GameScoreEntity
import app.tuulit.data.local.TuulitDao
import app.tuulit.domain.Achievement
import app.tuulit.domain.CareContent
import app.tuulit.domain.Dates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class GameScore(val gameId: String, val best: Int, val plays: Int)
data class UnlockedAchievement(val achievement: Achievement, val at: Long)

/** Comfort Library, Care session completions, game scores and achievements. */
class CarePlayRepository(
    private val dao: TuulitDao,
    private val changes: ChangeListener,
    private val now: () -> Long = { Dates.nowMillis() },
) {
    val comfortItems: Flow<List<ComfortItemEntity>> = dao.observeComfortItems()
    val gameScores: Flow<Map<String, GameScore>> = dao.observeGameScores().map { l -> l.associate { it.gameId to GameScore(it.gameId, it.bestScore, it.plays) } }
    val achievements: Flow<List<UnlockedAchievement>> = dao.observeAchievements().map { l ->
        l.mapNotNull { e -> runCatching { Achievement.valueOf(e.code) }.getOrNull()?.let { UnlockedAchievement(it, e.unlockedAt) } }
    }

    /**
     * First-run library: a starting bag checklist the person can edit or delete. Only seeded once.
     * Fixed IDs and updatedAt = 0 mean an untouched seed is never pushed, and a second device
     * (Android or iPhone) can't duplicate it; the same rule is used by the iOS app.
     */
    suspend fun seedLibraryIfEmpty() {
        if (dao.comfortItemCount() > 0) return
        SEED_TITLES.forEachIndexed { i, t ->
            dao.upsertComfortItem(ComfortItemEntity(id = SEED_IDS[i], kind = "kit", title = t, icon = "check", position = i, updatedAt = 0L))
        }
    }

    companion object {
        val SEED_TITLES = listOf("Pads or tampons", "Spare underwear", "Painkillers", "Wipes")
        val SEED_IDS = SEED_TITLES.indices.map { "seed-kit-$it" }
    }

    suspend fun addComfortItem(kind: String, title: String, detail: String, icon: String) {
        dao.upsertComfortItem(ComfortItemEntity(kind = kind, title = title.trim().take(80), detail = detail.trim().take(160), icon = icon, updatedAt = now()))
        changes.onLocalChange()
    }

    suspend fun updateComfortItem(item: ComfortItemEntity) {
        dao.upsertComfortItem(item.copy(updatedAt = now()))
        changes.onLocalChange()
    }

    suspend fun toggleKitItem(id: String) {
        val i = dao.comfortItem(id) ?: return
        dao.upsertComfortItem(i.copy(checked = !i.checked, updatedAt = now()))
        changes.onLocalChange()
    }

    suspend fun deleteComfortItem(id: String) {
        val i = dao.comfortItem(id) ?: return
        dao.upsertComfortItem(i.copy(deleted = true, updatedAt = now()))
        changes.onLocalChange()
    }

    /** Records a finished (or ended-early) session and credits the matching library item. */
    suspend fun completeSession(sessionId: String, fully: Boolean): List<Achievement> {
        dao.insertCompletion(CareCompletionEntity(sessionId = sessionId, completedAt = now(), fullyCompleted = fully))
        if (!fully) return emptyList()
        val session = CareContent.byId(sessionId)
        val item = dao.comfortItemForSession(sessionId)
        if (item != null) {
            dao.upsertComfortItem(item.copy(helpedCount = item.helpedCount + 1, updatedAt = now()))
        } else if (session != null) {
            dao.upsertComfortItem(ComfortItemEntity(kind = "comfort", title = session.title, detail = session.summary,
                icon = session.icon, helpedCount = 1, sessionId = sessionId, updatedAt = now()))
        }
        changes.onLocalChange()
        val unlocked = mutableListOf<Achievement>()
        unlock(Achievement.FIRST_SESSION)?.let { unlocked += it }
        if (dao.completedSessionCount() >= 5) unlock(Achievement.FIVE_SESSIONS)?.let { unlocked += it }
        return unlocked
    }

    /** Saves a finished game. Returns true when it's a new personal best. */
    suspend fun recordGame(gameId: String, score: Int): Boolean {
        val prev = dao.gameScore(gameId)
        val best = maxOf(prev?.bestScore ?: 0, score)
        dao.upsertGameScore(GameScoreEntity(gameId, best, (prev?.plays ?: 0) + 1, now()))
        unlock(Achievement.FIRST_GAME)
        changes.onLocalChange()
        return prev != null && score > prev.bestScore || prev == null && score > 0
    }

    suspend fun bestScore(gameId: String): Int = dao.gameScore(gameId)?.bestScore ?: 0

    /** Unlocks once. Returns the achievement when it was newly unlocked. */
    suspend fun unlock(a: Achievement): Achievement? {
        val inserted = dao.unlock(AchievementEntity(a.name, now()))
        if (inserted != -1L) changes.onLocalChange()
        return a.takeIf { inserted != -1L }
    }
}
