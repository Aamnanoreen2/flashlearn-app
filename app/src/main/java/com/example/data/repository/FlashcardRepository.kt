package com.example.data.repository

import com.example.data.db.FlashMasterDatabase
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FlashcardRepository(private val db: FlashMasterDatabase) {

    private val deckDao = db.deckDao()
    private val cardDao = db.flashcardDao()
    private val sessionDao = db.studySessionDao()
    private val statsDao = db.dailyStatisticDao()
    private val achievementDao = db.achievementDao()

    // --- Flows ---
    val allDecks: Flow<List<Deck>> = deckDao.getAllDecks()
    val allCards: Flow<List<Flashcard>> = cardDao.getAllCards()
    val favoriteCards: Flow<List<Flashcard>> = cardDao.getFavoriteCards()
    val mistakeCards: Flow<List<Flashcard>> = cardDao.getMistakeCards()
    val allSessions: Flow<List<StudySession>> = sessionDao.getAllSessions()
    val allStatistics: Flow<List<DailyStatistic>> = statsDao.getAllStatistics()
    val allAchievements: Flow<List<Achievement>> = achievementDao.getAllAchievements()

    fun getCardsForDeck(deckId: Int): Flow<List<Flashcard>> = cardDao.getCardsForDeck(deckId)
    fun getDueCards(deckId: Int, now: Long = System.currentTimeMillis()): Flow<List<Flashcard>> = 
        cardDao.getDueCards(deckId, now)

    // --- Core Operations ---
    suspend fun getDeckById(deckId: Int): Deck? = withContext(Dispatchers.IO) {
        deckDao.getDeckById(deckId)
    }

    suspend fun insertDeck(deck: Deck): Int = withContext(Dispatchers.IO) {
        val id = deckDao.insertDeck(deck).toInt()
        checkAchievements()
        id
    }

    suspend fun updateDeck(deck: Deck) = withContext(Dispatchers.IO) {
        deckDao.updateDeck(deck)
    }

    suspend fun deleteDeck(deck: Deck) = withContext(Dispatchers.IO) {
        // delete all associated card entries
        cardDao.deleteCardsByDeckId(deck.id)
        deckDao.deleteDeck(deck)
    }

    suspend fun getCardById(cardId: Int): Flashcard? = withContext(Dispatchers.IO) {
        cardDao.getCardById(cardId)
    }

    suspend fun insertCard(card: Flashcard): Int = withContext(Dispatchers.IO) {
        val id = cardDao.insertCard(card).toInt()
        recalculateDeckProgress(card.deckId)
        id
    }

    suspend fun updateCard(card: Flashcard) = withContext(Dispatchers.IO) {
        cardDao.updateCard(card)
        recalculateDeckProgress(card.deckId)
    }

    suspend fun deleteCard(card: Flashcard) = withContext(Dispatchers.IO) {
        cardDao.deleteCard(card)
        recalculateDeckProgress(card.deckId)
    }

    suspend fun updateDeckFavorite(deckId: Int, isFav: Boolean) = withContext(Dispatchers.IO) {
        deckDao.updateFavorite(deckId, isFav)
    }

    // --- Mistake Management ---
    suspend fun clearAllMistakes() = withContext(Dispatchers.IO) {
        cardDao.clearAllMistakes()
    }

    suspend fun clearMistakesByDeckId(deckId: Int) = withContext(Dispatchers.IO) {
        cardDao.clearMistakesByDeckId(deckId)
    }

    // --- Spaced Repetition Core ---
    suspend fun recordCardReview(cardId: Int, score: Int) = withContext(Dispatchers.IO) {
        val card = cardDao.getCardById(cardId) ?: return@withContext
        val updatedCard = calculateSM2(card, score)
        cardDao.updateCard(updatedCard)
        recalculateDeckProgress(card.deckId)
    }

    private fun calculateSM2(card: Flashcard, score: Int): Flashcard {
        // score: 1 = Incorrect, 3 = Hard, 4 = Correct, 5 = Easy
        val q = score.coerceIn(0, 5)
        val newRepetitions: Int
        val newIntervalDays: Int
        val newEasinessFactor: Float

        if (q >= 4) { // Correct or Easy responses
            newRepetitions = card.repetitions + 1
            newIntervalDays = when (newRepetitions) {
                1 -> 1
                2 -> 6
                else -> Math.round(card.intervalDays * card.easinessFactor).coerceAtLeast(1)
            }
            // Modify easiness factor
            val efDelta = 0.1f - (5f - q) * (0.08f + (5f - q) * 0.02f)
            newEasinessFactor = (card.easinessFactor + efDelta).coerceAtLeast(1.3f)
        } else if (q == 3) { // Hard response: correct but difficult
            newRepetitions = card.repetitions + 1
            newIntervalDays = 1 // Reappear soon (e.g., tomorrow)
            newEasinessFactor = (card.easinessFactor - 0.15f).coerceAtLeast(1.3f)
        } else { // Incorrect responses
            newRepetitions = 0
            newIntervalDays = 0 // Reappear immediately or today
            newEasinessFactor = card.easinessFactor // Keep the same difficulty
        }

        val nextReview = System.currentTimeMillis() + (newIntervalDays * 24L * 60L * 60L * 1000L)

        return card.copy(
            repetitions = newRepetitions,
            intervalDays = newIntervalDays,
            easinessFactor = newEasinessFactor,
            nextReviewTime = nextReview,
            isIncorrect = (q < 4) // Marked incorrect if score is less than Correct (4)
        )
    }

    // --- Study Sessions & Daily Statistics Tracking ---
    suspend fun recordStudySession(
        deckId: Int,
        durationMs: Long,
        totalCards: Int,
        correctCount: Int,
        incorrectCount: Int
    ) = withContext(Dispatchers.IO) {
        // Save study session entry
        val score = if (totalCards > 0) (correctCount * 100) / totalCards else 0
        val session = StudySession(
            deckId = deckId,
            durationMs = durationMs,
            totalCardsStudied = totalCards,
            correctCount = correctCount,
            incorrectCount = incorrectCount,
            score = score
        )
        sessionDao.insertSession(session)

        // Update deck's last studied timestamp
        val deck = deckDao.getDeckById(deckId)
        if (deck != null) {
            deckDao.updateDeck(deck.copy(lastStudied = System.currentTimeMillis()))
        }

        // Update Daily Summary Statistics
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val existingStats = statsDao.getStatisticByDate(todayStr)
        
        // Calculate streak
        val yesterdayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
            Date(System.currentTimeMillis() - 24L * 60L * 60L * 1000L)
        )
        val yesterdayStats = statsDao.getStatisticByDate(yesterdayStr)
        val lastStreak = yesterdayStats?.streak ?: 0
        val currentStreak = if (existingStats != null) {
            existingStats.streak
        } else {
            // New day study session increases streak
            lastStreak + 1
        }

        if (existingStats != null) {
            val updatedStats = existingStats.copy(
                cardsStudied = existingStats.cardsStudied + totalCards,
                correctCount = existingStats.correctCount + correctCount,
                incorrectCount = existingStats.incorrectCount + incorrectCount,
                studyTimeMs = existingStats.studyTimeMs + durationMs,
                streak = currentStreak
            )
            statsDao.insertStatistic(updatedStats)
        } else {
            val newStats = DailyStatistic(
                date = todayStr,
                cardsStudied = totalCards,
                correctCount = correctCount,
                incorrectCount = incorrectCount,
                studyTimeMs = durationMs,
                streak = currentStreak.coerceAtLeast(1)
            )
            statsDao.insertStatistic(newStats)
        }

        recalculateDeckProgress(deckId)
        checkAchievements()
    }

    private suspend fun recalculateDeckProgress(deckId: Int) {
        val deck = deckDao.getDeckById(deckId) ?: return
        val cards = cardDao.getCardsForDeck(deckId).firstOrNull() ?: return
        
        val progress = if (cards.isNotEmpty()) {
            // Count cards mastered (e.g. repetition level of 4+ and not incorrect)
            val masteredCount = cards.count { it.repetitions >= 3 && !it.isIncorrect }
            masteredCount.toFloat() / cards.size
        } else {
            0f
        }
        
        deckDao.updateDeck(deck.copy(progress = progress))
    }

    // --- Achievements Logic ---
    suspend fun checkAchievements() {
        val decks = deckDao.getAllDecks().firstOrNull() ?: emptyList()
        val cards = cardDao.getAllCards().firstOrNull() ?: emptyList()
        val sessions = sessionDao.getAllSessions().firstOrNull() ?: emptyList()
        val stats = statsDao.getAllStatistics().firstOrNull() ?: emptyList()

        val unlockedKeys = mutableListOf<String>()

        // 🎖️ First Deck
        if (decks.isNotEmpty()) {
            unlockedKeys.add("first_deck")
        }

        // 🎖️ 7-Day Streak
        val currentStreak = stats.lastOrNull()?.streak ?: 0
        if (currentStreak >= 7) {
            unlockedKeys.add("streak_7")
        }

        // 🎖️ 100 Cards Studied
        val totalStudied = stats.sumOf { it.cardsStudied }
        if (totalStudied >= 100) {
            unlockedKeys.add("cards_100")
        }

        // 🎖️ 1000 Reviews
        if (totalStudied >= 1000) {
            unlockedKeys.add("reviews_1000")
        }

        // 🎖️ Perfect Session (score = 100 with at least 5 cards studied)
        val hasPerfect = sessions.any { it.totalCardsStudied >= 5 && it.score == 100 }
        if (hasPerfect) {
            unlockedKeys.add("perfect_session")
        }

        // 🎖️ Deck Master (At least one deck with >= 5 cards in which all cards are mastered/progress = 1.0f)
        val hasMasteredDeck = decks.any { it.progress >= 0.95f && cards.count { c -> c.deckId == it.id } >= 5 }
        if (hasMasteredDeck) {
            unlockedKeys.add("deck_master")
        }

        // Apply unlocks dynamically
        for (key in unlockedKeys) {
            val ach = achievementDao.getAchievementByKey(key)
            if (ach != null && !ach.isUnlocked) {
                achievementDao.updateAchievement(ach.copy(isUnlocked = true, unlockedAt = System.currentTimeMillis()))
            }
        }
    }

    // --- Database Population (On First Launch) ---
    suspend fun populateInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val existingDecks = deckDao.getAllDecks().firstOrNull()
        if (!existingDecks.isNullOrEmpty()) return@withContext

        // Initialize Achievements
        val defaultAchievements = listOf(
            Achievement(key = "first_deck", title = "First Deck", description = "Create or study your first deck!"),
            Achievement(key = "streak_7", title = "7-Day Streak", description = "Achieve a study streak of 7 days in a row!"),
            Achievement(key = "cards_100", title = "Thorough Student", description = "Study a total of 100 flashcards!"),
            Achievement(key = "reviews_1000", title = "Spaced Repetitive Master", description = "Complete 1000 total flashcard reviews!"),
            Achievement(key = "perfect_session", title = "Perfect Score", description = "Get a 100% score checking 5 or more cards in a single study session!"),
            Achievement(key = "deck_master", title = "Deck Master", description = "Master an action-packed deck with at least 5 flashcards!")
        )
        for (ach in defaultAchievements) {
            if (achievementDao.getAchievementByKey(ach.key) == null) {
                achievementDao.insertAchievement(ach)
            }
        }

        // Create Sample Deck 1: Biology
        val bioDeckId = deckDao.insertDeck(
            Deck(
                name = "🎓 Human Biology",
                description = "Learn about cellular parts, enzymes, plant systems, and body functions.",
                category = "Science",
                colorHex = "#2196F3", // Blue
                iconName = "school"
            )
        ).toInt()

        cardDao.insertCards(listOf(
            Flashcard(deckId = bioDeckId, question = "What is considered the powerhouse of the cell?", answer = "The Mitochondria.", example = "Glucose + Oxygen -> CO2 + Water + ATP", notes = "Produces high-yield ATP through cellular respiration.", tags = "Biology,Cellular"),
            Flashcard(deckId = bioDeckId, question = "What green pigment assists plants in capturing energy from sunlight during photosynthesis?", answer = "Chlorophyll.", example = "Found inside plant chloroplasts.", notes = "Absorbs red and blue bands of light.", tags = "PlantScience,Photosynthesis"),
            Flashcard(deckId = bioDeckId, question = "Which endocrine organ produces and publishes insulin?", answer = "The Pancreas.", example = "Secreted via cells known as pancreatic islets (Beta cells).", notes = "Regulates blood glucose homeostatically.", tags = "Anatomy,Endocrine"),
            Flashcard(deckId = bioDeckId, question = "What are the foundational structural build-blocks of proteins?", answer = "Amino Acids.", example = "Examples include Alanine, Valine, Serine.", notes = "Organisms make use of 20 standard amino acids.", tags = "Biochemistry,Proteins")
        ))

        // Create Sample Deck 2: Computer Networks
        val netDeckId = deckDao.insertDeck(
            Deck(
                name = "💻 Computer Networks",
                description = "Internet structures, socket standards, port registries, and OSI layers.",
                category = "Computer Science",
                colorHex = "#3F51B5", // Indigo
                iconName = "computer"
            )
        ).toInt()

        cardDao.insertCards(listOf(
            Flashcard(deckId = netDeckId, question = "What is DNS short for, and what primary duty does it perform?", answer = "Domain Name System. It acts as an Internet phonebook, matching alphabetical domains to physical IP addresses.", example = "Resolves google.com -> 142.250.190.46", notes = "Runs primarily on UDP port 53.", tags = "Internet,DNS"),
            Flashcard(deckId = netDeckId, question = "Name the two dominant network transport protocols in the internet suite.", answer = "TCP (Transmission Control Protocol) and UDP (User Datagram Protocol).", example = "TCP: Web Browsing, UDP: Live Streaming", notes = "TCP is reliable and ordered; UDP is connectionless and low-overhead.", tags = "OSI,Protocols"),
            Flashcard(deckId = netDeckId, question = "Which registered network port is standard for secure web communication over SSL/TLS?", answer = "Port 443 (HTTPS).", example = "https://example.com utilizes this automatically.", notes = "Encrypted web protocols avoid sniffing vectors.", tags = "Security,Ports")
        ))

        // Create Sample Deck 3: French Vocabulary
        val vocabularyDeckId = deckDao.insertDeck(
            Deck(
                name = "🥖 French Vocabulary",
                description = "Master common conversational French vocabulary words.",
                category = "Languages",
                colorHex = "#9C27B0", // Purple
                iconName = "language"
            )
        ).toInt()

        cardDao.insertCards(listOf(
            Flashcard(deckId = vocabularyDeckId, question = "How do you translate and say 'Thank you very much' in French?", answer = "Merci beaucoup.", example = "Merci beaucoup pour votre aide!", notes = "Pronounced: mair-see boh-coo", tags = "French,Greetings"),
            Flashcard(deckId = vocabularyDeckId, question = "Translate the keyword 'Bonjour' and indicate when it is used.", answer = "It means 'Hello' or 'Good morning'. It is the standard polite greeting used throughout the day.", example = "Bonjour, monsieur!", notes = "Can be used formally or informally.", tags = "French,Basics"),
            Flashcard(deckId = vocabularyDeckId, question = "What French noun is used for 'The bread'?", answer = "Le pain.", example = "Je voudrais acheter le pain.", notes = "Masculine noun.", tags = "French,Food")
        ))

        // Trigger streak / stats creation for today
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        if (statsDao.getStatisticByDate(todayStr) == null) {
            statsDao.insertStatistic(DailyStatistic(date = todayStr, cardsStudied = 0, streak = 0))
        }

        // Sync initial progress
        recalculateDeckProgress(bioDeckId)
        recalculateDeckProgress(netDeckId)
        recalculateDeckProgress(vocabularyDeckId)
    }
}
