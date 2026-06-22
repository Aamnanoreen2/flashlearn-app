package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Deck
import com.example.data.model.Flashcard
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.ui.components.*
import com.example.ui.theme.CorrectGreen
import com.example.ui.theme.HardOrange
import com.example.ui.theme.IncorrectRed
import com.example.ui.theme.SecondaryLight
import com.example.ui.theme.PrimaryLight
import androidx.compose.foundation.BorderStroke
import com.example.ui.viewmodel.FlashMasterViewModel

@Composable
fun StudyScreen(
    viewModel: FlashMasterViewModel,
    onStartStudy: (Deck, List<Flashcard>) -> Unit,
    modifier: Modifier = Modifier
) {
    val decks by viewModel.decks.collectAsState()
    val allCards by viewModel.allCards.collectAsState()
    val mistakes by viewModel.mistakeCards.collectAsState()
    val favorites by viewModel.favoriteCards.collectAsState()

    val context = LocalContext.current

    // Due calculation
    val dueCardsCount = allCards.count { it.nextReviewTime <= System.currentTimeMillis() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .screenBackground()
    ) {
        HeroHeader(
            title = "Study Hub",
            subtitle = "Focus on weak points and complete daily reviews to maintain your memory streak.",
            actionIcon = Icons.Default.OfflineBolt,
            onActionClick = {},
            testTagSuffix = "study_hub"
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Study due cards immediately!
            item {
                val isDark = isSystemInDarkTheme()
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("due_reviews_banner")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Daily Spaced Reviews",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Ready to recall based on learning intervals",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(IncorrectRed.copy(alpha = 0.2f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "$dueCardsCount Due",
                                color = IncorrectRed,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (dueCardsCount == 0) {
                                Toast.makeText(context, "All caught up on spaced repetition reviews! Try regular deck study.", Toast.LENGTH_SHORT).show()
                            } else {
                                // Start session on due cards! Using a dummy deck placeholder or the first deck
                                val targetDeck = decks.firstOrNull { d -> allCards.any { c -> c.deckId == d.id && c.nextReviewTime <= System.currentTimeMillis() } } 
                                    ?: decks.firstOrNull()
                                if (targetDeck != null) {
                                    val dueCards = allCards.filter { it.nextReviewTime <= System.currentTimeMillis() }
                                    onStartStudy(targetDeck, dueCards)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("start_due_study_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.DirectionsRun, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Spaced Reviews", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Mistake Review Section
            item {
                Text(
                    text = "Mistake Review Mode",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                val isDarkLocal = isSystemInDarkTheme()
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NewReleases,
                                contentDescription = null,
                                tint = IncorrectRed,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Review Errors",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${mistakes.size} cards failed in study",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                        
                        if (mistakes.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    viewModel.clearAllMistakes()
                                    Toast.makeText(context, "Mistakes log cleared!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.testTag("clear_mistakes_btn")
                            ) {
                                Text("Clear All", color = IncorrectRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (mistakes.isEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "🎉 Outstanding! No logged mistake cards. Study decks to generate spaced error records.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = CorrectGreen, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                // Make a dummy deck representing mistakes
                                val dummyDeck = Deck(id = -10, name = "Mistakes Review", colorHex = "#E53935", iconName = "menu_book")
                                onStartStudy(dummyDeck, mistakes)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("review_mistakes_now_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IncorrectRed)
                        ) {
                            Icon(imageVector = Icons.Default.PlaylistPlay, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Review Failed Cards", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Favorites study option
            item {
                Text(
                    text = "Personal Favorites",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                val isDarkFavorites = isSystemInDarkTheme()
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = HardOrange,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Starred Flashcards",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${favorites.size} starred review cards",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                    }

                    if (favorites.isEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Star important cards while reading deck details by double tapping them.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val dummyDeck = Deck(id = -20, name = "My Starred Cards", colorHex = "#FF9800", iconName = "star")
                                onStartStudy(dummyDeck, favorites)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("study_favorites_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HardOrange)
                        ) {
                            Icon(imageVector = Icons.Default.Grade, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Study Starred Only", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
