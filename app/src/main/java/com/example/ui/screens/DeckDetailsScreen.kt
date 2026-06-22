package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Deck
import com.example.data.model.Flashcard
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.components.GradientButton
import com.example.ui.components.GlassCard
import com.example.ui.components.screenBackground
import com.example.ui.components.appCardClickable
import com.example.ui.theme.*
import com.example.ui.viewmodel.FlashMasterViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DeckDetailsScreen(
    deck: Deck,
    viewModel: FlashMasterViewModel,
    onBack: () -> Unit,
    onStartStudy: (Deck, List<Flashcard>) -> Unit,
    onStartAiQuiz: (Deck) -> Unit,
    modifier: Modifier = Modifier
) {
    val cards by viewModel.getCardsForDeck(deck.id).collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }

    // Dialog flags
    var showAddCardDialog by remember { mutableStateOf(false) }
    var cardToEdit by remember { mutableStateOf<Flashcard?>(null) }
    var showBulkImportDialog by remember { mutableStateOf(false) }

    val filteredCards = remember(cards, searchQuery) {
        if (searchQuery.isBlank()) cards
        else cards.filter {
            it.question.contains(searchQuery, ignoreCase = true) ||
                    it.answer.contains(searchQuery, ignoreCase = true) ||
                    it.tags.contains(searchQuery, ignoreCase = true) ||
                    it.notes.contains(searchQuery, ignoreCase = true)
        }
    }

    val context = LocalContext.current
    val systemColor = remember {
        try { Color(android.graphics.Color.parseColor(deck.colorHex)) } 
        catch (e: Exception) { PrimaryLight }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = deck.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showBulkImportDialog = true }) {
                        Icon(imageVector = Icons.Default.Input, contentDescription = "Bulk Import")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddCardDialog = true },
                containerColor = systemColor,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_card_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Flashcard")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .screenBackground()
        ) {
            // Deck Overview Info
            val isDarkOverview = isSystemInDarkTheme()
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = deck.category,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = systemColor)
                )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (deck.description.isNotBlank()) deck.description else "No description specified.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Buttons Action Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (cards.isEmpty()) {
                                    Toast.makeText(context, "Add cards first to start studying!", Toast.LENGTH_SHORT).show()
                                } else {
                                    onStartStudy(deck, cards)
                                }
                            },
                            modifier = Modifier
                                .weight(1.2f)
                                .height(48.dp)
                                .testTag("study_deck_now"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = systemColor)
                        ) {
                            Icon(imageVector = Icons.Default.School, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Study (${cards.size})", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                if (cards.size < 2) {
                                    Toast.makeText(context, "At least 2 cards are required to play Gemini AI Quiz!", Toast.LENGTH_SHORT).show()
                                } else {
                                    onStartAiQuiz(deck)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("study_ai_assistant"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = TertiaryLight, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("AI Quiz", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

            // Search Bar Input
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search cards by word, definition or tags...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(52.dp)
                    .testTag("search_cards_input"),
                shape = RoundedCornerShape(14.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Cards list
            if (filteredCards.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyPlaceholder(
                        icon = Icons.Default.FilterNone,
                        title = "No Flashcards Found",
                        subtitle = if (cards.isEmpty()) "Start creating individual cards using the '+' button, or import in bulk!" 
                        else "No cards corresponds to your current search query."
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredCards) { card ->
                        FlashcardListItem(
                            card = card,
                            onEditClick = { cardToEdit = card },
                            onDeleteClick = { viewModel.deleteFlashcard(card) },
                            onDoubleTap = { viewModel.toggleCardFavorite(card) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    // Add / Edit Card Dialog
    if (showAddCardDialog || cardToEdit != null) {
        val editing = cardToEdit != null
        CardFormDialog(
            card = cardToEdit,
            onDismiss = {
                showAddCardDialog = false
                cardToEdit = null
            },
            onSave = { q, a, ex, n, t, diff ->
                if (editing && cardToEdit != null) {
                    viewModel.updateFlashcard(
                        cardToEdit!!.copy(
                            question = q,
                            answer = a,
                            example = ex,
                            notes = n,
                            tags = t,
                            difficulty = diff
                        )
                    )
                } else {
                    viewModel.addFlashcard(deck.id, q, a, ex, n, t, diff)
                }
                showAddCardDialog = false
                cardToEdit = null
            }
        )
    }

    // Bulk Import Dialog
    if (showBulkImportDialog) {
        BulkImportDialog(
            onDismiss = { showBulkImportDialog = false },
            onImport = { content ->
                val ok = viewModel.importBulkCards(deck.id, content)
                if (ok) {
                    Toast.makeText(context, "Successfully imported cards!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "No matches found. Verify notation splitter format.", Toast.LENGTH_LONG).show()
                }
                showBulkImportDialog = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FlashcardListItem(
    card: Flashcard,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDoubleTap: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val diffColor = when (card.difficulty) {
        "Easy" -> CorrectGreen
        "Hard" -> HardOrange
        else -> SecondaryLight
    }

    val isDarkItem = isSystemInDarkTheme()
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .appCardClickable(
                onClick = { expanded = !expanded },
                onDoubleClick = onDoubleTap
            )
            .testTag("card_list_item_${card.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Topic Word/Question
                Text(
                    text = card.question,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (card.isFavorite) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Starred",
                            tint = HardOrange,
                            modifier = Modifier
                                .size(22.dp)
                                .padding(end = 4.dp)
                        )
                    }
                    
                    // Difficulty indicator pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(diffColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = card.difficulty,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 9.sp),
                            color = diffColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle Tag markers
            if (card.tags.isNotBlank()) {
                val tagList = card.tags.split(",")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tagList.take(3).forEach { t ->
                        Text(
                            text = "#${t.trim()}",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }

            // Expanded Definition reveals Backside + notes
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Text(
                        text = "Answer / Definition:",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Text(
                        text = card.answer,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                    )

                    if (card.example.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Usage / Example:",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = card.example,
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        )
                    }

                    if (card.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Notes:",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = card.notes,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Options buttons row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onEditClick) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Card", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = onDeleteClick) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Card", tint = IncorrectRed)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CardFormDialog(
    card: Flashcard?,
    onDismiss: () -> Unit,
    onSave: (q: String, a: String, ex: String, notes: String, tags: String, diff: String) -> Unit
) {
    var question by remember { mutableStateOf(card?.question ?: "") }
    var answer by remember { mutableStateOf(card?.answer ?: "") }
    var example by remember { mutableStateOf(card?.example ?: "") }
    var notes by remember { mutableStateOf(card?.notes ?: "") }
    var tags by remember { mutableStateOf(card?.tags ?: "") }
    var difficulty by remember { mutableStateOf(card?.difficulty ?: "Medium") }

    val difficulties = listOf("Easy", "Medium", "Hard")

    var isQuestionError by remember { mutableStateOf(false) }
    var isAnswerError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = if (card != null) "Edit Flashcard" else "Add Flashcard", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = question,
                    onValueChange = {
                        question = it
                        isQuestionError = it.isBlank()
                    },
                    label = { Text("Front Side (Question, Word) *") },
                    isError = isQuestionError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_dialog_q_input")
                )
                if (isQuestionError) {
                    Text("Front question is required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = answer,
                    onValueChange = {
                        answer = it
                        isAnswerError = it.isBlank()
                    },
                    label = { Text("Back Side (Answer, Definition) *") },
                    isError = isAnswerError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_dialog_a_input")
                )
                if (isAnswerError) {
                    Text("Back answer is required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = example,
                    onValueChange = { example = it },
                    label = { Text("Example usage (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Additional Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    placeholder = { Text("e.g. Vocabulary, Biology") },
                    label = { Text("Comma separated Tags (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))
                
                // Difficulty Level Select
                Text("Select Difficulty Level", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    difficulties.forEach { diff ->
                        val active = difficulty == diff
                        FilterChoiceChip(
                            selected = active,
                            onClick = { difficulty = diff },
                            label = diff,
                            activeColor = when (diff) {
                                "Easy" -> CorrectGreen
                                "Hard" -> HardOrange
                                else -> SecondaryLight
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (question.isBlank()) isQuestionError = true
                    if (answer.isBlank()) isAnswerError = true
                    if (question.isNotBlank() && answer.isNotBlank()) {
                        onSave(question, answer, example, notes, tags, difficulty)
                    }
                },
                modifier = Modifier.testTag("card_dialog_save")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun FilterChoiceChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(38.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) activeColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, activeColor) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (selected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
fun BulkImportDialog(
    onDismiss: () -> Unit,
    onImport: (content: String) -> Unit
) {
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Bulk Flashcard Import", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Type or paste your data here. Separate each card's Frontside and Backside of answer using '|' or ',' symbol.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Format example:\nCPU | Central Processing Unit\nRAM | Random Access Memory",
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = MaterialTheme.colorScheme.primary)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Import Content Data") },
                    placeholder = { Text("FrontSide | BackSide Answer") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .testTag("bulk_import_textarea")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onImport(content) },
                enabled = content.isNotBlank()
            ) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
