package app.tuulit.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TuulitDao {
    // Cycles
    @Query("SELECT * FROM cycles WHERE deleted = 0 ORDER BY startEpochDay ASC")
    fun observeCycles(): Flow<List<CycleEntity>>

    @Query("SELECT * FROM cycles WHERE deleted = 0 ORDER BY startEpochDay ASC")
    suspend fun cycles(): List<CycleEntity>

    @Query("SELECT * FROM cycles WHERE startEpochDay = :day LIMIT 1")
    suspend fun cycleStartingOn(day: Long): CycleEntity?

    @Query("SELECT * FROM cycles WHERE id = :id LIMIT 1")
    suspend fun cycle(id: String): CycleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCycle(cycle: CycleEntity)

    // Daily logs
    @Query("SELECT * FROM daily_logs WHERE deleted = 0 ORDER BY epochDay DESC")
    fun observeDailyLogs(): Flow<List<DailyLogEntity>>

    @Query("SELECT * FROM daily_logs WHERE epochDay = :day LIMIT 1")
    suspend fun dailyLog(day: Long): DailyLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyLog(log: DailyLogEntity)

    // Pain logs
    @Query("SELECT * FROM pain_logs WHERE deleted = 0 ORDER BY epochDay DESC, createdAt DESC")
    fun observePainLogs(): Flow<List<PainLogEntity>>

    @Query("SELECT * FROM pain_logs WHERE id = :id LIMIT 1")
    suspend fun painLog(id: String): PainLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPainLog(log: PainLogEntity)

    // Comfort library
    @Query("SELECT * FROM comfort_items WHERE deleted = 0 ORDER BY kind, position, updatedAt")
    fun observeComfortItems(): Flow<List<ComfortItemEntity>>

    @Query("SELECT * FROM comfort_items WHERE id = :id LIMIT 1")
    suspend fun comfortItem(id: String): ComfortItemEntity?

    @Query("SELECT * FROM comfort_items WHERE sessionId = :sessionId AND deleted = 0 LIMIT 1")
    suspend fun comfortItemForSession(sessionId: String): ComfortItemEntity?

    @Query("SELECT COUNT(*) FROM comfort_items")
    suspend fun comfortItemCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertComfortItem(item: ComfortItemEntity)

    // Care completions
    @Insert
    suspend fun insertCompletion(c: CareCompletionEntity)

    @Query("SELECT COUNT(*) FROM care_completions WHERE fullyCompleted = 1")
    suspend fun completedSessionCount(): Int

    @Query("SELECT * FROM care_completions ORDER BY completedAt DESC")
    fun observeCompletions(): Flow<List<CareCompletionEntity>>

    // Achievements
    @Query("SELECT * FROM achievements ORDER BY unlockedAt DESC")
    fun observeAchievements(): Flow<List<AchievementEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun unlock(a: AchievementEntity): Long

    // Games
    @Query("SELECT * FROM game_scores")
    fun observeGameScores(): Flow<List<GameScoreEntity>>

    @Query("SELECT * FROM game_scores WHERE gameId = :id LIMIT 1")
    suspend fun gameScore(id: String): GameScoreEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGameScore(s: GameScoreEntity)

    // AI messages (local only)
    @Query("SELECT * FROM ai_messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun observeConversation(conversationId: String): Flow<List<AiMessageEntity>>

    @Query("SELECT * FROM ai_messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    suspend fun conversation(conversationId: String): List<AiMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessage(m: AiMessageEntity)

    @Query("DELETE FROM ai_messages WHERE conversationId = :conversationId")
    suspend fun clearConversation(conversationId: String)

    // Sync: rows changed since the last successful push (tombstones included)
    @Query("SELECT * FROM cycles WHERE updatedAt > :since")
    suspend fun cyclesChangedSince(since: Long): List<CycleEntity>

    @Query("SELECT * FROM daily_logs WHERE updatedAt > :since")
    suspend fun logsChangedSince(since: Long): List<DailyLogEntity>

    @Query("SELECT * FROM pain_logs WHERE updatedAt > :since")
    suspend fun painChangedSince(since: Long): List<PainLogEntity>

    @Query("SELECT * FROM comfort_items WHERE updatedAt > :since")
    suspend fun comfortChangedSince(since: Long): List<ComfortItemEntity>

    @Query("SELECT * FROM game_scores WHERE updatedAt > :since")
    suspend fun gamesChangedSince(since: Long): List<GameScoreEntity>

    @Query("SELECT * FROM achievements")
    suspend fun achievements(): List<AchievementEntity>

    // Export
    @Query("SELECT * FROM daily_logs WHERE deleted = 0 ORDER BY epochDay ASC")
    suspend fun allDailyLogs(): List<DailyLogEntity>

    @Query("SELECT * FROM pain_logs WHERE deleted = 0 ORDER BY epochDay ASC")
    suspend fun allPainLogs(): List<PainLogEntity>

    // Account deletion / wipe
    @Query("DELETE FROM cycles") suspend fun clearCycles()
    @Query("DELETE FROM daily_logs") suspend fun clearDailyLogs()
    @Query("DELETE FROM pain_logs") suspend fun clearPainLogs()
    @Query("DELETE FROM comfort_items") suspend fun clearComfort()
    @Query("DELETE FROM care_completions") suspend fun clearCompletions()
    @Query("DELETE FROM achievements") suspend fun clearAchievements()
    @Query("DELETE FROM game_scores") suspend fun clearGames()
    @Query("DELETE FROM ai_messages") suspend fun clearMessages()

    @Transaction
    suspend fun wipe() {
        clearCycles(); clearDailyLogs(); clearPainLogs(); clearComfort()
        clearCompletions(); clearAchievements(); clearGames(); clearMessages()
    }
}
