package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Deck
import com.example.data.model.Flashcard
import com.example.ui.theme.*
import com.example.ui.viewmodel.FlashMasterViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StudySessionScreen(
    viewModel: FlashMasterViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeDeck by viewModel.activeDeck.collectAsState()
    val studyCards by viewModel.activeStudyCards.collectAsState()
    val index by viewModel.currentCardIndex.collectAsState()
    val isFlipped by viewModel.isCardFlipped.collectAsState()
    val correctCount by viewModel.correctCount.collectAsState()
    val incorrectCount by viewModel.incorrectCount.collectAsState()

    val isFocusModActive by viewModel.isFocusMode.collectAsState()
    val isTimedModeActive by viewModel.isTimedMode.collectAsState()
    val remainingSecs by viewModel.timerRemainingSeconds.collectAsState()

    // Dialog flags
    var showAddNoteDialog by remember { mutableStateOf<Flashcard?>(null) }

    val context = LocalContext.current

    if (activeDeck == null || studyCards.isEmpty() || index >= studyCards.size) {
        // Safe check or empty
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        LaunchedEffect(Unit) {
            onBack()
        }
        return
    }

    val cardColor = remember {
        try { Color(android.graphics.Color.parseColor(activeDeck!!.colorHex)) } 
        catch (e: Exception) { PrimaryLight }
    }

    val activeCard = studyCards[index]

    // Timed Mode Clock Engine
    if (isTimedModeActive) {
        LaunchedEffect(index, isFlipped) {
            while (viewModel.timerRemainingSeconds.value > 0 && !isFlipped) {
                delay(1000)
                viewModel.timerRemainingSeconds.value -= 1
            }
            if (viewModel.timerRemainingSeconds.value <= 0 && !isFlipped) {
                // Auto Flip card on clock timeout
                viewModel.flipCard()
            }
        }
    }

    // Realistic card flip rotation degree state
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "FlipRotation"
    )

    // Gesture Swipe math accumulation
    var dragAccumulatedX by remember { mutableStateOf(0f) }

    Scaffold(
        topBar = {
            if (!isFocusModActive) {
                TopAppBar(
                    title = {
                        Column {
                            Text(text = "Studying: ${activeDeck!!.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(text = "${index + 1} of ${studyCards.size} cards", style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit Study")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.toggleCardFavorite(activeCard) }) {
                            Icon(
                                imageVector = if (activeCard.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Add Favorite",
                                tint = if (activeCard.isFavorite) HardOrange else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .pointerInput(index) {
                    // Gesture dragging engine
                    detectDragGestures(
                        onDragEnd = {
                            if (dragAccumulatedX > 150f) {
                                // Swipe right -> Correct
                                viewModel.answerCard(4)
                                Toast.makeText(context, "✅ Correct!", Toast.LENGTH_SHORT).show()
                            } else if (dragAccumulatedX < -150f) {
                                // Swipe left -> Incorrect
                                viewModel.answerCard(1)
                                Toast.makeText(context, "❌ Incorrect", Toast.LENGTH_SHORT).show()
                            }
                            dragAccumulatedX = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragAccumulatedX += dragAmount.x
                        }
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Study Session Options header
            if (!isFocusModActive) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StudySettingToggle(
                            label = "Shuffle",
                            active = !viewModel.isSequentialMode.value,
                            onClick = {
                                viewModel.isSequentialMode.value = !viewModel.isSequentialMode.value
                                Toast.makeText(context, "Modes updated. Applied starting next session.", Toast.LENGTH_SHORT).show()
                            }
                        )
                        StudySettingToggle(
                            label = "Timer",
                            active = isTimedModeActive,
                            onClick = { viewModel.isTimedMode.value = !viewModel.isTimedMode.value }
                        )
                        StudySettingToggle(
                            label = "Focus Mode",
                            active = isFocusModActive,
                            onClick = { viewModel.isFocusMode.value = !viewModel.isFocusMode.value }
                        )
                    }
                }
            } else {
                // Small indicator in Focus mode to exit focus
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit Study")
                    }
                    Text(
                        text = "${index + 1} / ${studyCards.size}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = { viewModel.isFocusMode.value = false }) {
                        Icon(imageVector = Icons.Default.Visibility, contentDescription = "Show controls")
                    }
                }
            }

            // Timed Mode circular representation
            if (isTimedModeActive && !isFlipped) {
                val progress = remainingSecs.toFloat() / viewModel.timedModeLimitSeconds.value
                Box(
                    modifier = Modifier.padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { progress },
                        color = cardColor,
                        trackColor = cardColor.copy(alpha = 0.1f),
                        modifier = Modifier.size(42.dp),
                        strokeWidth = 4.dp
                    )
                    Text(text = "$remainingSecs", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            }

            // Realistic Flip Card Surface Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp)
                    .graphicsLayer {
                        rotationY = rotation
                        cameraDistance = 12f * density
                    }
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .combinedClickable(
                        onClick = { viewModel.flipCard() },
                        onLongClick = { showAddNoteDialog = activeCard }
                    )
                    .testTag("study_flashcard_canvas")
            ) {
                // If rotated beyond 90 degrees, show Back, else show Front side
                if (rotation <= 90f) {
                    // Front side representation
                    CardFrontView(activeCard = activeCard)
                } else {
                    // Back side representation (mirrored back to compensate rotation projection!)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationY = 180f }
                    ) {
                        CardBackView(activeCard = activeCard, cardColor = cardColor)
                    }
                }
            }

            // Swipe instructions label
            Text(
                text = "💡 Tap card to flip. Swipe Right (✅ Correct) or Swipe Left (❌ Incorrect).",
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )

            // Dynamic grading button actions
            StudySessionGradingControls(
                isFlipped = isFlipped,
                onFlipClick = { viewModel.flipCard() },
                onChooseScore = { score ->
                    viewModel.answerCard(score)
                }
            )
        }
    }

    // Add study note dialog dynamically
    showAddNoteDialog?.let { card ->
        var noteInput by remember { mutableStateOf(card.notes) }
        AlertDialog(
            onDismissRequest = { showAddNoteDialog = null },
            title = { Text("Update Study Notes", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text("Card Personal Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateFlashcard(card.copy(notes = noteInput))
                    showAddNoteDialog = null
                    Toast.makeText(context, "Note updated!", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddNoteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CardFrontView(activeCard: Flashcard) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.ContactSupport,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = activeCard.question,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "Tap to Flip",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                ),
                color = MaterialTheme.colorScheme.primaryContainer
            )
        }
    }
}

@Composable
fun CardBackView(activeCard: Flashcard, cardColor: Color) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Campaign,
            contentDescription = null,
            tint = CorrectGreen.copy(alpha = 0.2f),
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = activeCard.answer,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = CorrectGreen),
            textAlign = TextAlign.Center
        )

        if (activeCard.example.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Example Usage:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = cardColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = activeCard.example,
                        fontSize = 13.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (activeCard.notes.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Study Notes:",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = activeCard.notes,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun StudySessionGradingControls(
    isFlipped: Boolean,
    onFlipClick: () -> Unit,
    onChooseScore: (Int) -> Unit
) {
    AnimatedContent(
        targetState = isFlipped,
        transitionSpec = {
            slideInVertically { height -> height } + fadeIn() togetherWith
                    slideOutVertically { height -> height } + fadeOut()
        },
        label = "GradingControls"
    ) { flipped ->
        if (!flipped) {
            // Flip card action button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = onFlipClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("reveal_answer_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.FlipCameraAndroid, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reveal Answer", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        } else {
            // Study Rating Choices
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StudyGradingButton(
                    label = "Incorrect",
                    score = 1,
                    icon = Icons.Default.Cancel,
                    color = IncorrectRed,
                    onClick = { onChooseScore(1) },
                    testTag = "grade_incorrect",
                    modifier = Modifier.weight(1f)
                )

                StudyGradingButton(
                    label = "Hard",
                    score = 3,
                    icon = Icons.Default.Psychology,
                    color = HardOrange,
                    onClick = { onChooseScore(3) },
                    testTag = "grade_hard",
                    modifier = Modifier.weight(1f)
                )

                StudyGradingButton(
                    label = "Correct",
                    score = 4,
                    icon = Icons.Default.CheckCircle,
                    color = CorrectGreen,
                    onClick = { onChooseScore(4) },
                    testTag = "grade_correct",
                    modifier = Modifier.weight(1f)
                )

                StudyGradingButton(
                    label = "Easy",
                    score = 5,
                    icon = Icons.Default.Star,
                    color = EasyBlue,
                    onClick = { onChooseScore(5) },
                    testTag = "grade_easy",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun Modifier.fillModifier(): Modifier = this

@Composable
fun StudyGradingButton(
    label: String,
    score: Int,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .testTag(testTag)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun StudySettingToggle(
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        border = if (active) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}
