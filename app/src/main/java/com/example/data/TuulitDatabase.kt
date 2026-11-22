package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CycleRecordEntity::class,
        DailyLogEntity::class,
        PainLogEntity::class,
        SurvivalKitItemEntity::class,
        EmergencyChecklistItemEntity::class,
        AchievementEntity::class,
        TrustedContactEntity::class,
        GameScoreEntity::class,
    ],
    version = 1,
    exportSchema = false
)
abstract class TuulitDatabase : RoomDatabase() {
    abstract fun tuulitDao(): TuulitDao

    companion object {
        @Volatile
        private var INSTANCE: TuulitDatabase? = null

        fun getInstance(context: Context): TuulitDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TuulitDatabase::class.java,
                    "tuulit_local.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
