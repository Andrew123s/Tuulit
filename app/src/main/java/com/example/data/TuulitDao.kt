package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TuulitDao {
    // Cycles
    @Query("SELECT * FROM cycle_records ORDER BY startEpochDay ASC")
    fun observeCycles(): Flow<List<CycleRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCycle(cycle: CycleRecordEntity): Long

    @Query("DELETE FROM cycle_records WHERE id = :cycleId")
    suspend fun deleteCycle(cycleId: Long)

    // Daily Logs
    @Query("SELECT * FROM daily_logs ORDER BY epochDay DESC")
    fun observeDailyLogs(): Flow<List<DailyLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyLog(log: DailyLogEntity)

    @Query("DELETE FROM daily_logs WHERE epochDay = :epochDay")
    suspend fun deleteDailyLog(epochDay: Long)

    // Pain Logs
    @Query("SELECT * FROM pain_logs ORDER BY epochDay DESC, createdAtMillis DESC")
    fun observePainLogs(): Flow<List<PainLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPainLog(log: PainLogEntity): Long

    @Query("DELETE FROM pain_logs WHERE id = :id")
    suspend fun deletePainLog(id: Long)

    // Survival Kit
    @Query("SELECT * FROM survival_kit_items ORDER BY pinned DESC, id ASC")
    fun observeSurvivalKit(): Flow<List<SurvivalKitItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSurvivalKitItem(item: SurvivalKitItemEntity)

    @Query("DELETE FROM survival_kit_items WHERE id = :id")
    suspend fun deleteSurvivalKitItem(id: Long)

    // Emergency Checklist
    @Query("SELECT * FROM emergency_checklist_items ORDER BY id ASC")
    fun observeChecklist(): Flow<List<EmergencyChecklistItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChecklistItem(item: EmergencyChecklistItemEntity)

    @Query("DELETE FROM emergency_checklist_items WHERE id = :id")
    suspend fun deleteChecklistItem(id: Long)

    // Achievements
    @Query("SELECT * FROM achievements ORDER BY unlockedAtMillis DESC")
    fun observeAchievements(): Flow<List<AchievementEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun unlockAchievement(achievement: AchievementEntity)

    // Trusted Contacts
    @Query("SELECT * FROM trusted_contacts ORDER BY id ASC")
    fun observeTrustedContacts(): Flow<List<TrustedContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrustedContact(contact: TrustedContactEntity)

    @Update
    suspend fun updateTrustedContact(contact: TrustedContactEntity)

    @Query("DELETE FROM trusted_contacts WHERE id = :id")
    suspend fun deleteTrustedContact(id: Long)

    // Game Scores
    @Query("SELECT * FROM game_scores")
    fun observeGameScores(): Flow<List<GameScoreEntity>>

    @Query("SELECT * FROM game_scores WHERE gameId = :gameId LIMIT 1")
    suspend fun getGameScore(gameId: String): GameScoreEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGameScore(score: GameScoreEntity)

    // Full Privacy Wipe
    @Query("DELETE FROM cycle_records")
    suspend fun clearCycles()

    @Query("DELETE FROM daily_logs")
    suspend fun clearDailyLogs()

    @Query("DELETE FROM pain_logs")
    suspend fun clearPainLogs()

    @Query("DELETE FROM survival_kit_items")
    suspend fun clearSurvivalKit()

    @Query("DELETE FROM emergency_checklist_items")
    suspend fun clearChecklist()

    @Query("DELETE FROM achievements")
    suspend fun clearAchievements()

    @Query("DELETE FROM trusted_contacts")
    suspend fun clearTrustedContacts()

    @Query("DELETE FROM game_scores")
    suspend fun clearGameScores()
}
