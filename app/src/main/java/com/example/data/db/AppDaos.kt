package com.example.data.db

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {
    @Query("SELECT * FROM decks ORDER BY createdAt DESC")
    fun getAllDecks(): Flow<List<Deck>>

    @Query("SELECT * FROM decks WHERE id = :id")
    suspend fun getDeckById(id: Int): Deck?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: Deck): Long

    @Update
    suspend fun updateDeck(deck: Deck)

    @Delete
    suspend fun deleteDeck(deck: Deck)

    @Query("UPDATE decks SET isFavorite = :isFav WHERE id = :id")
    suspend fun updateFavorite(id: Int, isFav: Boolean)
}

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcards WHERE deckId = :deckId ORDER BY createdAt DESC")
    fun getCardsForDeck(deckId: Int): Flow<List<Flashcard>>

    @Query("SELECT * FROM flashcards WHERE id = :id")
    suspend fun getCardById(id: Int): Flashcard?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: Flashcard): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<Flashcard>)

    @Update
    suspend fun updateCard(card: Flashcard)

    @Delete
    suspend fun deleteCard(card: Flashcard)

    @Query("DELETE FROM flashcards WHERE deckId = :deckId")
    suspend fun deleteCardsByDeckId(deckId: Int)

    @Query("SELECT * FROM flashcards ORDER BY createdAt DESC")
    fun getAllCards(): Flow<List<Flashcard>>

    @Query("SELECT * FROM flashcards WHERE isFavorite = 1")
    fun getFavoriteCards(): Flow<List<Flashcard>>

    @Query("SELECT * FROM flashcards WHERE isIncorrect = 1")
    fun getMistakeCards(): Flow<List<Flashcard>>

    @Query("UPDATE flashcards SET isIncorrect = 0")
    suspend fun clearAllMistakes()

    @Query("UPDATE flashcards SET isIncorrect = 0 WHERE deckId = :deckId")
    suspend fun clearMistakesByDeckId(deckId: Int)

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId AND nextReviewTime <= :now")
    fun getDueCards(deckId: Int, now: Long): Flow<List<Flashcard>>
}

@Dao
interface StudySessionDao {
    @Query("SELECT * FROM study_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<StudySession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySession): Long
}

@Dao
interface DailyStatisticDao {
    @Query("SELECT * FROM statistics ORDER BY date ASC")
    fun getAllStatistics(): Flow<List<DailyStatistic>>

    @Query("SELECT * FROM statistics WHERE date = :date LIMIT 1")
    suspend fun getStatisticByDate(date: String): DailyStatistic?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatistic(statistic: DailyStatistic)
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements")
    fun getAllAchievements(): Flow<List<Achievement>>

    @Query("SELECT * FROM achievements WHERE `key` = :key LIMIT 1")
    suspend fun getAchievementByKey(key: String): Achievement?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: Achievement)

    @Update
    suspend fun updateAchievement(achievement: Achievement)
}
