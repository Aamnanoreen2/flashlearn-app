package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiClient
import com.example.data.model.*
import com.example.data.repository.FlashcardRepository
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@JsonClass(generateAdapter = true)
data class QuizQuestion(
    @Json(name = "question") val question: String,
    @Json(name = "options") val options: List<String>,
    @Json(name = "correctAnswer") val correctAnswer: String,
    @Json(name = "explanation") val explanation: String
)

@JsonClass(generateAdapter = true)
data class ExportedData(
    @Json(name = "deck") val deck: Deck,
    @Json(name = "cards") val cards: List<Flashcard>
)

class FlashMasterViewModel(
    private val repository: FlashcardRepository,
    private val application: Application
) : ViewModel() {

    private val sharedPrefs = application.getSharedPreferences("flashmaster_prefs", Context.MODE_PRIVATE)

    // --- Theme Settings Persistence ---
    val themeMode = MutableStateFlow(sharedPrefs.getString("theme_mode", "system") ?: "system")

    fun setThemeMode(mode: String) {
        themeMode.value = mode
        sharedPrefs.edit().putString("theme_mode", mode).apply()
        isDarkMode.value = when (mode) {
            "dark" -> true
            "light" -> false
            else -> null
        }
    }

    // --- Database flows ---
    val decks = repository.allDecks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allCards = repository.allCards.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val favoriteCards = repository.favoriteCards.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val mistakeCards = repository.mistakeCards.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allSessions = repository.allSessions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allStatistics = repository.allStatistics.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val achievements = repository.allAchievements.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Active Study Session State ---
    private val _activeDeck = MutableStateFlow<Deck?>(null)
    val activeDeck: StateFlow<Deck?> = _activeDeck.asStateFlow()

    private val _activeStudyCards = MutableStateFlow<List<Flashcard>>(emptyList())
    val activeStudyCards: StateFlow<List<Flashcard>> = _activeStudyCards.asStateFlow()

    private val _currentCardIndex = MutableStateFlow(0)
    val currentCardIndex: StateFlow<Int> = _currentCardIndex.asStateFlow()

    private val _isCardFlipped = MutableStateFlow(false)
    val isCardFlipped: StateFlow<Boolean> = _isCardFlipped.asStateFlow()

    private val _correctCount = MutableStateFlow(0)
    val correctCount: StateFlow<Int> = _correctCount.asStateFlow()

    private val _incorrectCount = MutableStateFlow(0)
    val incorrectCount: StateFlow<Int> = _incorrectCount.asStateFlow()

    private val _sessionStartTime = MutableStateFlow(0L)

    // Study Preferences
    val isFocusMode = MutableStateFlow(false)
    val isTimedMode = MutableStateFlow(false)
    val isSequentialMode = MutableStateFlow(false)
    val timedModeLimitSeconds = MutableStateFlow(60) // 60 seconds per card by default
    val timerRemainingSeconds = MutableStateFlow(60)

    // --- AI Quiz State ---
    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError.asStateFlow()

    private val _aiQuizQuestions = MutableStateFlow<List<QuizQuestion>>(emptyList())
    val aiQuizQuestions: StateFlow<List<QuizQuestion>> = _aiQuizQuestions.asStateFlow()

    // --- Theme Settings Persistence via Mem State ---
    val isDarkMode = MutableStateFlow<Boolean?>(
        when (sharedPrefs.getString("theme_mode", "system")) {
            "dark" -> true
            "light" -> false
            else -> null
        }
    )

    init {
        // Pre-populate data on first launch
        viewModelScope.launch {
            repository.populateInitialDataIfEmpty()
        }
    }

    fun getCardsForDeck(deckId: Int): Flow<List<Flashcard>> = repository.getCardsForDeck(deckId)

    // --- Deck Operations ---
    fun addDeck(name: String, description: String, category: String, colorHex: String, iconName: String) {
        viewModelScope.launch {
            repository.insertDeck(
                Deck(
                    name = name,
                    description = description,
                    category = category,
                    colorHex = colorHex,
                    iconName = iconName
                )
            )
        }
    }

    fun updateDeck(deck: Deck) {
        viewModelScope.launch {
            repository.updateDeck(deck)
        }
    }

    fun duplicateDeck(deck: Deck) {
        viewModelScope.launch {
            val originalCards = repository.getCardsForDeck(deck.id).firstOrNull() ?: emptyList()
            val newDeckId = repository.insertDeck(
                deck.copy(
                    id = 0,
                    name = "${deck.name} (Copy)",
                    createdAt = System.currentTimeMillis()
                )
            )
            val newCards = originalCards.map {
                it.copy(
                    id = 0,
                    deckId = newDeckId,
                    createdAt = System.currentTimeMillis(),
                    repetitions = 0,
                    intervalDays = 0,
                    easinessFactor = 2.5f,
                    nextReviewTime = System.currentTimeMillis()
                )
            }
            newCards.forEach { repository.insertCard(it) }
        }
    }

    fun deleteDeck(deck: Deck) {
        viewModelScope.launch {
            repository.deleteDeck(deck)
        }
    }

    // --- Flashcard Operations ---
    fun addFlashcard(deckId: Int, question: String, answer: String, example: String, notes: String, tags: String, difficulty: String) {
        viewModelScope.launch {
            repository.insertCard(
                Flashcard(
                    deckId = deckId,
                    question = question,
                    answer = answer,
                    example = example,
                    notes = notes,
                    tags = tags,
                    difficulty = difficulty
                )
            )
        }
    }

    fun updateFlashcard(card: Flashcard) {
        viewModelScope.launch {
            repository.updateCard(card)
        }
    }

    fun deleteFlashcard(card: Flashcard) {
        viewModelScope.launch {
            repository.deleteCard(card)
        }
    }

    fun toggleCardFavorite(card: Flashcard) {
        viewModelScope.launch {
            repository.updateCard(card.copy(isFavorite = !card.isFavorite))
        }
    }

    // --- Spaced Repetition Study Loop ---
    fun startStudySession(deck: Deck, studyModeCards: List<Flashcard>) {
        _activeDeck.value = deck
        val finalCards = if (isSequentialMode.value) studyModeCards else studyModeCards.shuffled()
        _activeStudyCards.value = finalCards
        _currentCardIndex.value = 0
        _isCardFlipped.value = false
        _correctCount.value = 0
        _incorrectCount.value = 0
        _sessionStartTime.value = System.currentTimeMillis()
        timerRemainingSeconds.value = timedModeLimitSeconds.value
    }

    fun flipCard() {
        _isCardFlipped.value = !_isCardFlipped.value
    }

    fun answerCard(score: Int) {
        // score: 1 = Incorrect, 3 = Hard, 4 = Correct, 5 = Easy
        viewModelScope.launch {
            val currentCards = _activeStudyCards.value
            val index = _currentCardIndex.value
            if (index < currentCards.size) {
                val card = currentCards[index]
                
                // Track counts
                if (score >= 4) {
                    _correctCount.value += 1
                } else {
                    _incorrectCount.value += 1
                }

                // Call Repo to calculate next SM-2 interval
                repository.recordCardReview(card.id, score)

                // Check end criteria
                if (index + 1 < currentCards.size) {
                    _currentCardIndex.value = index + 1
                    _isCardFlipped.value = false
                    timerRemainingSeconds.value = timedModeLimitSeconds.value
                } else {
                    // End list was studied, finalize session records
                    endStudySession()
                }
            }
        }
    }

    fun endStudySession() {
        val deck = _activeDeck.value ?: return
        val startTime = _sessionStartTime.value
        if (startTime == 0L) return

        val duration = System.currentTimeMillis() - startTime
        val total = _correctCount.value + _incorrectCount.value

        viewModelScope.launch {
            repository.recordStudySession(
                deckId = deck.id,
                durationMs = duration,
                totalCards = total,
                correctCount = _correctCount.value,
                incorrectCount = _incorrectCount.value
            )
            _activeDeck.value = null
            _activeStudyCards.value = emptyList()
            _currentCardIndex.value = 0
            _sessionStartTime.value = 0L
        }
    }

    // --- Mistake Review Actions ---
    fun clearAllMistakes() {
        viewModelScope.launch {
            repository.clearAllMistakes()
        }
    }

    fun clearMistakesForDeck(deckId: Int) {
        viewModelScope.launch {
            repository.clearMistakesByDeckId(deckId)
        }
    }

    // --- Bulk Import Text Format (CSV / Question|Answer) ---
    fun importBulkCards(deckId: Int, content: String): Boolean {
        if (content.isBlank()) return false
        val cardsToInsert = mutableListOf<Flashcard>()
        val lines = content.split("\n")
        
        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isBlank()) continue
            
            // Standard splitters: '|' or ','
            val parts = if (trimmedLine.contains("|")) {
                trimmedLine.split("|")
            } else if (trimmedLine.contains(",")) {
                trimmedLine.split(",")
            } else {
                continue
            }
            
            if (parts.size >= 2) {
                val q = parts[0].trim()
                val a = parts[1].trim()
                if (q.isNotEmpty() && a.isNotEmpty()) {
                    cardsToInsert.add(
                        Flashcard(
                            deckId = deckId,
                            question = q,
                            answer = a,
                            notes = if (parts.size > 2) parts[2].trim() else ""
                        )
                    )
                }
            }
        }

        if (cardsToInsert.isNotEmpty()) {
            viewModelScope.launch {
                cardsToInsert.forEach { repository.insertCard(it) }
            }
            return true
        }
        return false
    }

    // --- JSON Export / Import Decks (Offline Storage) ---
    fun exportDeckToJsonString(deck: Deck, cards: List<Flashcard>): String? {
        return try {
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val adapter = moshi.adapter(ExportedData::class.java)
            adapter.toJson(ExportedData(deck, cards))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun importDeckFromJson(context: Context, uri: Uri): String? {
        return try {
            val contentResolver = context.contentResolver
            val stringBuilder = StringBuilder()
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stringBuilder.append(line)
                    }
                }
            }
            
            val jsonStr = stringBuilder.toString()
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val adapter = moshi.adapter(ExportedData::class.java)
            val data = adapter.fromJson(jsonStr) ?: return "Empty JSON data format."
            
            viewModelScope.launch {
                val importedDeckId = repository.insertDeck(
                    data.deck.copy(
                        id = 0,
                        name = "${data.deck.name} (Imported)",
                        createdAt = System.currentTimeMillis()
                    )
                )
                val cardsWithNewDeckId = data.cards.map {
                    it.copy(
                        id = 0,
                        deckId = importedDeckId,
                        createdAt = System.currentTimeMillis(),
                        repetitions = 0,
                        intervalDays = 0,
                        easinessFactor = 2.5f,
                        nextReviewTime = System.currentTimeMillis(),
                        isIncorrect = false
                    )
                }
                cardsWithNewDeckId.forEach { repository.insertCard(it) }
            }
            
            null // success, return null error message
        } catch (e: Exception) {
            e.message ?: "Failed parsing input stream."
        }
    }

    // --- AI Quiz Generator via Gemini Service ---
    fun generateAiQuizForDeck(deck: Deck) {
        viewModelScope.launch {
            _aiLoading.value = true
            _aiError.value = null
            _aiQuizQuestions.value = emptyList()

            try {
                val cardsList = repository.getCardsForDeck(deck.id).firstOrNull() ?: emptyList()
                if (cardsList.size < 2) {
                    _aiError.value = "At least 2 flashcards are required in this deck to run Gemini AI Quiz generation."
                    return@launch
                }

                // build prompt
                val summaryText = cardsList.take(20).joinToString("\n") { card ->
                    "Question: ${card.question} | Answer: ${card.answer}"
                }

                val prompt = """
                    I have a flashcard study deck named "${deck.name}".
                    Here is a subset of the flashcard cards in this deck:
                    $summaryText
                    
                    Generate a 4-question Multiple Choice test covering this subject. Each question object must contain four options of choices, indicate the exact correct option as 'correctAnswer', and supply a friendly explanation covering that knowledge point.
                """.trimIndent()

                val systemPrompt = """
                    You are an intelligent, highly engaging study assistant, and professional teacher.
                    Your goal is to turn the user's provided list of flashcard content into a multiple-choice chemistry/vocabulary/history test.
                    Compose exactly 4 unique multi-choice questions.
                    
                    Return ONLY a valid JSON array of objects. Do NOT output markdown code blocks (such as ```json or ```). Return ONLY the direct raw JSON output.
                    Each object schema must exactly correspond to this JSON template:
                    {
                        "question": "The question prompt?",
                        "options": ["First Choice", "Second Choice", "Third Choice", "Fourth Choice"],
                        "correctAnswer": "The exact correct option copy here",
                        "explanation": "Brief explanation covering this topic"
                    }
                """.trimIndent()

                val contentOutput = GeminiClient.generateStudyMaterials(prompt, systemPrompt)
                val parsed = parseQuizJson(contentOutput)
                
                if (parsed.isNotEmpty()) {
                    _aiQuizQuestions.value = parsed
                } else {
                    _aiError.value = "Failed to parse generated responses. Please try again."
                }
            } catch (e: Exception) {
                _aiError.value = "Connection error: " + (e.message ?: "API unavailable")
            } finally {
                _aiLoading.value = false
            }
        }
    }

    private fun parseQuizJson(jsonStr: String): List<QuizQuestion> {
        val cleaned = jsonStr.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        return try {
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val type = Types.newParameterizedType(List::class.java, QuizQuestion::class.java)
            val adapter = moshi.adapter<List<QuizQuestion>>(type)
            adapter.fromJson(cleaned) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun clearQuizState() {
        _aiQuizQuestions.value = emptyList()
        _aiError.value = null
        _aiLoading.value = false
    }

    // --- Smart Learning Score Calculation ---
    fun calculateLearningScore(): Int {
        val cards = allCards.value
        val hasCards = cards.isNotEmpty()
        if (!hasCards) return 0
        
        val stats = allStatistics.value
        val currentStreak = stats.lastOrNull()?.streak ?: 0
        
        // Mastery percentage
        val masteredCount = cards.count { it.repetitions >= 3 && !it.isIncorrect }
        val masteryRatio = masteredCount.toFloat() / cards.size
        
        // Learning score out of 100
        val masteryScore = (masteryRatio * 60).toInt() // up to 60 points
        val streakScore = (currentStreak * 5).coerceAtMost(25) // up to 25 points
        val cardVolumeScore = (cards.size * 0.5f).coerceAtMost(15f).toInt() // up to 15 points
        
        return (masteryScore + streakScore + cardVolumeScore).coerceIn(0, 100)
    }

    companion object {
        fun provideFactory(repository: FlashcardRepository, application: Application): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(FlashMasterViewModel::class.java)) {
                    return FlashMasterViewModel(repository, application) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
