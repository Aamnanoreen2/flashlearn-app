package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "decks")
data class Deck(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String = "",
    val category: String = "General",
    val colorHex: String = "#3F51B5", // Hex code string
    val iconName: String = "school", // Identifier for icon
    val lastStudied: Long = 0L,
    val progress: Float = 0f,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "flashcards")
data class Flashcard(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val deckId: Int,
    val question: String,
    val answer: String,
    val example: String = "",
    val notes: String = "",
    val tags: String = "", // Comma-separated tags
    val difficulty: String = "Medium", // Easy, Medium, Hard
    val isFavorite: Boolean = false,
    val isIncorrect: Boolean = false, // True if marked incorrect in study session
    val createdAt: Long = System.currentTimeMillis(),
    
    // Spaced repetition (SM-2 algorithm parameters)
    val easinessFactor: Float = 2.5f,
    val intervalDays: Int = 0,
    val repetitions: Int = 0,
    val nextReviewTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_sessions")
data class StudySession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val deckId: Int,
    val startTime: Long = System.currentTimeMillis(),
    val durationMs: Long = 0L,
    val totalCardsStudied: Int = 0,
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val score: Int = 0 // study score performance
)

@Entity(tableName = "statistics")
data class DailyStatistic(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // format: "YYYY-MM-DD"
    val cardsStudied: Int = 0,
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val studyTimeMs: Long = 0L,
    val streak: Int = 0
)

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val key: String, // uniqu key (e.g. "first_deck", "streak_7", "cards_100")
    val title: String,
    val description: String,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null
)
