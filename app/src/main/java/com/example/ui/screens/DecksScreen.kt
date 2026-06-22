package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Deck
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.FlashMasterViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun DecksScreen(
    viewModel: FlashMasterViewModel,
    onDeckClick: (Deck) -> Unit,
    modifier: Modifier = Modifier
) {
    val decks by viewModel.decks.collectAsState()
    val allCards by viewModel.allCards.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Recently Studied") }
    var showOnlyFavorites by remember { mutableStateOf(false) }

    // Dialog state
    var showCreateDialog by remember { mutableStateOf(false) }
    var deckToEdit by remember { mutableStateOf<Deck?>(null) }

    // Floating Context Menu
    var showMenuForDeck by remember { mutableStateOf<Deck?>(null) }

    // Search and filter logic
    val filteredDecks = remember(decks, searchQuery, selectedFilter, showOnlyFavorites, allCards) {
        var baseList = decks.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true) ||
                    it.category.contains(searchQuery, ignoreCase = true)
        }

        if (showOnlyFavorites) {
            baseList = baseList.filter { it.isFavorite }
        }

        when (selectedFilter) {
            "Alphabetical" -> baseList.sortedBy { it.name }
            "Most Cards" -> baseList.sortedByDescending { deck -> allCards.count { it.deckId == deck.id } }
            "Least Cards" -> baseList.sortedBy { deck -> allCards.count { it.deckId == deck.id } }
            else -> baseList.sortedByDescending { it.lastStudied } // Recently Studied / default
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_deck_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Deck")
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
            HeroHeader(
                title = "My Decks",
                subtitle = "Manage your topics, review frequencies, and browse study materials.",
                actionIcon = Icons.Default.FilterList,
                onActionClick = { /* Click toggle */ },
                testTagSuffix = "decks"
            )

            // Search Bar & Favorite Filter row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name or category...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("search_decks_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                FilterIconButton(
                    icon = if (showOnlyFavorites) Icons.Default.Star else Icons.Default.StarBorder,
                    active = showOnlyFavorites,
                    onClick = { showOnlyFavorites = !showOnlyFavorites },
                    testTag = "decks_fav_filter"
                )
            }

            // Quick Sort Filters chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf("Recently Studied", "Alphabetical", "Most Cards")
                filters.forEach { filterItem ->
                    val active = selectedFilter == filterItem
                    FilterChip(
                        selected = active,
                        onClick = { selectedFilter = filterItem },
                        label = { Text(filterItem, fontSize = 11.sp) },
                        modifier = Modifier.height(32.dp)
                    )
                }
            }

            // Decks List
            if (filteredDecks.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyPlaceholder(
                        icon = Icons.Default.LayersClear,
                        title = "No Decks Found",
                        subtitle = "Create a custom flashcard deck using the '+' FAB button to begin study preparation."
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredDecks) { deck ->
                        val cardCount = allCards.count { it.deckId == deck.id }
                        
                        DeckCard(
                            deck = deck,
                            cardCount = cardCount,
                            onClick = { onDeckClick(deck) },
                            onLongClick = { showMenuForDeck = deck },
                            onFavClick = {
                                viewModel.updateDeck(deck.copy(isFavorite = !deck.isFavorite))
                            }
                        )
                    }
                    // Bottom padding spacer inside grid
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    // Context Action Sheet / Dialog
    showMenuForDeck?.let { deck ->
        AlertDialog(
            onDismissRequest = { showMenuForDeck = null },
            title = { Text(text = deck.name, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    ContextMenuItem(Icons.Default.School, "Study Now", onClick = {
                        showMenuForDeck = null
                        onDeckClick(deck) // nav and trigger study
                    })
                    HorizontalDivider()
                    ContextMenuItem(Icons.Default.Edit, "Edit Deck Info", onClick = {
                        showMenuForDeck = null
                        deckToEdit = deck
                    })
                    HorizontalDivider()
                    ContextMenuItem(Icons.Default.FileCopy, "Duplicate Deck", onClick = {
                        showMenuForDeck = null
                        viewModel.duplicateDeck(deck)
                    })
                    HorizontalDivider()
                    ContextMenuItem(Icons.Default.Delete, "Delete Deck", textColor = MaterialTheme.colorScheme.error, onClick = {
                        showMenuForDeck = null
                        viewModel.deleteDeck(deck)
                    })
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMenuForDeck = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Create / Edit Deck Dialog
    if (showCreateDialog || deckToEdit != null) {
        val editing = deckToEdit != null
        DeckFormDialog(
            deck = deckToEdit,
            onDismiss = {
                showCreateDialog = false
                deckToEdit = null
            },
            onSave = { name, desc, category, colorHex, icon ->
                if (editing && deckToEdit != null) {
                    viewModel.updateDeck(
                        deckToEdit!!.copy(
                            name = name,
                            description = desc,
                            category = category,
                            colorHex = colorHex,
                            iconName = icon
                        )
                    )
                } else {
                    viewModel.addDeck(name, desc, category, colorHex, icon)
                }
                showCreateDialog = false
                deckToEdit = null
            }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun DeckCard(
    deck: Deck,
    cardCount: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFavClick: () -> Unit
) {
    val cardColor = try {
        Color(android.graphics.Color.parseColor(deck.colorHex))
    } catch (e: Exception) {
        PrimaryLight
    }

    val isDark = isSystemInDarkTheme()
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .appCardClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("deck_item_card_${deck.id}")
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Deck Category & Icon indicator
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(cardColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIcon(deck.iconName),
                        contentDescription = deck.category,
                        tint = cardColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                IconButton(
                    onClick = onFavClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (deck.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (deck.isFavorite) HardOrange else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Name
            Text(
                text = deck.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Category Label
            Text(
                text = deck.category,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                ),
                maxLines = 1
            )

            Spacer(modifier = Modifier.weight(1f))

            // Cards count & progress meter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "$cardCount cards",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                )
                Text(
                    text = "${(deck.progress * 100).toInt()}% mastered",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { deck.progress },
                color = cardColor,
                trackColor = cardColor.copy(alpha = 0.15f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
        }
    }
}

@Composable
fun ContextMenuItem(
    icon: ImageVector,
    label: String,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconColor(icon, textColor))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = textColor)
    }
}

private fun iconColor(icon: ImageVector, default: Color): Color {
    return if (icon == Icons.Default.Delete) IncorrectRed else default.copy(alpha = 0.7f)
}

@Composable
fun FilterIconButton(
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(52.dp)
            .testTag(testTag)
            .border(
                1.dp,
                if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        color = if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (active) HardOrange else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DeckFormDialog(
    deck: Deck?,
    onDismiss: () -> Unit,
    onSave: (name: String, desc: String, category: String, colorHex: String, icon: String) -> Unit
) {
    var name by remember { mutableStateOf(deck?.name ?: "") }
    var desc by remember { mutableStateOf(deck?.description ?: "") }
    var category by remember { mutableStateOf(deck?.category ?: "General") }
    var selectedColor by remember { mutableStateOf(deck?.colorHex ?: "#3F51B5") }
    var selectedIcon by remember { mutableStateOf(deck?.iconName ?: "school") }

    val colors = listOf("#3F51B5", "#2196F3", "#9C27B0", "#4CAF50", "#E53935", "#FF9800")
    val icons = listOf("school", "computer", "language", "biotech", "menu_book")

    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = if (deck != null) "Edit Deck" else "Create Deck", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = it.isBlank()
                    },
                    label = { Text("Deck Name *") },
                    isError = nameError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("deck_dialog_name_input")
                )
                if (nameError) {
                    Text("Deck name is required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (e.g. Science, Language)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Color Selection
                Text("Select Theme Color", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colors.forEach { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        val active = selectedColor == hex
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    if (active) 3.dp else 0.dp,
                                    if (active) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Icon selection
                Text("Select Icon Category", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    icons.forEach { iconKey ->
                        val active = selectedIcon == iconKey
                        IconButton(
                            onClick = { selectedIcon = iconKey },
                            modifier = Modifier
                                .background(
                                    if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(iconKey),
                                contentDescription = null,
                                tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                    } else {
                        onSave(name, desc, category, selectedColor, selectedIcon)
                    }
                },
                modifier = Modifier.testTag("deck_dialog_save")
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

fun getCategoryIcon(key: String): ImageVector {
    return when (key) {
        "computer" -> Icons.Default.Computer
        "language" -> Icons.Default.Translate
        "biotech" -> Icons.Default.Biotech
        "menu_book" -> Icons.Default.MenuBook
        else -> Icons.Default.School
    }
}
