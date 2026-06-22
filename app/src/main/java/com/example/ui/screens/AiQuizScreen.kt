package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Deck
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.components.HeroHeader
import com.example.ui.theme.*
import com.example.ui.viewmodel.FlashMasterViewModel
import com.example.ui.viewmodel.QuizQuestion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiQuizScreen(
    deck: Deck,
    viewModel: FlashMasterViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val loading by viewModel.aiLoading.collectAsState()
    val error by viewModel.aiError.collectAsState()
    val questions by viewModel.aiQuizQuestions.collectAsState()

    var currentQuestionIndex by remember { mutableStateOf(0) }
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var scoreCount by remember { mutableStateOf(0) }
    var quizCompleted by remember { mutableStateOf(false) }

    // On open: request question list if empty
    LaunchedEffect(deck) {
        if (questions.isEmpty() && !loading && error == null) {
            viewModel.generateAiQuizForDeck(deck)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "AI Study Assistant", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.clearQuizState()
                        onBack()
                    }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (loading) {
                // AI Quiz Generating Loading Page
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = TertiaryLight,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Gemini is Analyzing Flashcards...",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Synthesizing custom multi-choice options and educational explanations based on '${deck.name}' definitions. Stand by!",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else if (error != null) {
                // Error card
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyPlaceholder(
                        icon = Icons.Default.AutoAwesome,
                        title = "AI Assistant Failed",
                        subtitle = error ?: "Check network configuration.",
                        actionText = "Try Again",
                        onActionClick = { viewModel.generateAiQuizForDeck(deck) }
                    )
                }
            } else if (quizCompleted) {
                // Results screen
                QuizResultsView(
                    score = scoreCount,
                    total = questions.size,
                    deckName = deck.name,
                    onReplay = {
                        currentQuestionIndex = 0
                        selectedOption = null
                        scoreCount = 0
                        quizCompleted = false
                        viewModel.generateAiQuizForDeck(deck)
                    },
                    onBack = {
                        viewModel.clearQuizState()
                        onBack()
                    }
                )
            } else if (questions.isNotEmpty()) {
                val activeQuestion = questions[currentQuestionIndex]
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Question Info Header
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Question ${currentQuestionIndex + 1} of ${questions.size}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TertiaryLight)
                            )
                            Text(
                                text = "Score: $scoreCount",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = CorrectGreen)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { (currentQuestionIndex.toFloat()) / questions.size },
                            color = TertiaryLight,
                            trackColor = TertiaryLight.copy(alpha = 0.15f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Question Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Text(
                                text = activeQuestion.question,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, lineHeight = 24.sp),
                                modifier = Modifier.padding(20.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Multiple-Choice Options
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            activeQuestion.options.forEach { option ->
                                val selectedOfCurrentUser = selectedOption == option
                                val alreadyAnswered = selectedOption != null
                                val isCorrectOption = option == activeQuestion.correctAnswer
                                
                                val containerColor = when {
                                    alreadyAnswered && isCorrectOption -> CorrectGreen.copy(alpha = 0.15f)
                                    selectedOfCurrentUser && !isCorrectOption -> IncorrectRed.copy(alpha = 0.15f)
                                    selectedOfCurrentUser -> TertiaryLight.copy(alpha = 0.1f)
                                    else -> MaterialTheme.colorScheme.surface
                                }
                                
                                val borderStrokeColor = when {
                                    alreadyAnswered && isCorrectOption -> CorrectGreen
                                    selectedOfCurrentUser && !isCorrectOption -> IncorrectRed
                                    selectedOfCurrentUser -> TertiaryLight
                                    else -> Color.Transparent
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = if (borderStrokeColor != Color.Transparent) 2.dp else 0.dp,
                                            color = borderStrokeColor,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable(enabled = !alreadyAnswered) {
                                            selectedOption = option
                                            if (option == activeQuestion.correctAnswer) {
                                                scoreCount += 1
                                            }
                                        }
                                        .testTag("quiz_option_${option.take(15)}"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = containerColor)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (selectedOfCurrentUser) borderStrokeColor 
                                                    else MaterialTheme.colorScheme.surfaceVariant
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (alreadyAnswered && isCorrectOption) {
                                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            } else if (selectedOfCurrentUser && !isCorrectOption) {
                                                Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(text = option, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                    }
                                }
                            }
                        }

                        // Explanation banner
                        if (selectedOption != null) {
                            Spacer(modifier = Modifier.height(20.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedOption == activeQuestion.correctAnswer) CorrectGreen.copy(alpha = 0.08f) 
                                    else IncorrectRed.copy(alpha = 0.08f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = if (selectedOption == activeQuestion.correctAnswer) "Correct Explanation!" else "Keep Learning! Correct Answer: ${activeQuestion.correctAnswer}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (selectedOption == activeQuestion.correctAnswer) CorrectGreen else IncorrectRed
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = activeQuestion.explanation, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    // Next Question footer triggers
                    if (selectedOption != null) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                if (currentQuestionIndex + 1 < questions.size) {
                                    currentQuestionIndex += 1
                                    selectedOption = null
                                } else {
                                    quizCompleted = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("quiz_next_btn"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TertiaryLight)
                        ) {
                            Text(
                                text = if (currentQuestionIndex + 1 < questions.size) "Next Question" else "Finish Quiz",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            } else {
                // Pre-launch fallback
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun QuizResultsView(
    score: Int,
    total: Int,
    deckName: String,
    onReplay: () -> Unit,
    onBack: () -> Unit
) {
    val scorePercentage = if (total > 0) (score * 100) / total else 0
    val ratingText = when {
        scorePercentage >= 90 -> "Outstanding masterly efforts! 🏆"
        scorePercentage >= 70 -> "Great performance! 🏅"
        else -> "Keep studying! Cards make you strong. 👍"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(TertiaryLight.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SentimentVerySatisfied,
                    contentDescription = null,
                    tint = TertiaryLight,
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = "Quiz Finished!",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Subject: $deckName",
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "$score / $total correct",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold, color = TertiaryLight)
            )
            Text(
                text = "$scorePercentage% Score",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = ratingText,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onReplay,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TertiaryLight)
            ) {
                Text("Study AI Quiz Again", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Return to Deck")
            }
        }
    }
}
