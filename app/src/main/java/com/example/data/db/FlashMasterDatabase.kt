package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.*

@Database(
    entities = [
        Deck::class,
        Flashcard::class,
        StudySession::class,
        DailyStatistic::class,
        Achievement::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FlashMasterDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun dailyStatisticDao(): DailyStatisticDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        @Volatile
        private var INSTANCE: FlashMasterDatabase? = null

        fun getDatabase(context: Context): FlashMasterDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FlashMasterDatabase::class.java,
                    "flash_master_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
