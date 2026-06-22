package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyStatistic
import com.example.data.model.Deck
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.FlashMasterViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    viewModel: FlashMasterViewModel,
    onCreateDeckClick: () -> Unit,
    onContinueStudyingClick: (Deck) -> Unit,
    onReviewMistakesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val decks by viewModel.decks.collectAsState()
    val allCards by viewModel.allCards.collectAsState()
    val statistics by viewModel.allStatistics.collectAsState()
    
    val totalDecksCount = decks.size
    val totalCardsCount = allCards.size
    
    // Stats Calculations
    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val todayStats = statistics.find { it.date == todayStr }
    val cardsStudiedToday = todayStats?.cardsStudied ?: 0
    val streakOfStudy = todayStats?.streak ?: statistics.lastOrNull()?.streak ?: 0
    
    val masteredCards = allCards.count { it.repetitions >= 3 && !it.isIncorrect }
    val dueCards = allCards.count { it.nextReviewTime <= System.currentTimeMillis() }
    
    val scrollState = rememberScrollState()
    
    // Quotes list
    val quote = remember {
        val quotes = listOf(
            "Develop a passion for learning. If you do, you will never cease to grow.",
            "The secret of getting ahead is getting started.",
            "Spaced repetition is the key to expanding your mental palace.",
            "Continuous learning is the minimum requirement for success in any field.",
            "Your mind is a muscle - exercise it card by card!"
        )
        quotes.random()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .screenBackground()
    ) {
        HeroHeader(
            title = "FlashMaster",
            subtitle = "Welcome back, scholar! Ready to optimize your retention today?",
            actionIcon = Icons.Default.EmojiEvents,
            onActionClick = {},
            testTagSuffix = "home"
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // Motivational Quote Card
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FormatQuote,
                    contentDescription = "Quote",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = quote,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Study Overview Grid
        Text(
            text = "Study Overview",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OverviewMetricCard(
                title = "Studied Today",
                value = cardsStudiedToday.toString(),
                icon = Icons.Default.CheckCircle,
                color = CorrectGreen,
                modifier = Modifier.weight(1f)
            )
            OverviewMetricCard(
                title = "Study Streak",
                value = "$streakOfStudy Days",
                icon = Icons.Default.LocalFireDepartment,
                color = HardOrange,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OverviewMetricCard(
                title = "Mastered",
                value = masteredCards.toString(),
                icon = Icons.Default.OfflineBolt,
                color = PrimaryLight,
                modifier = Modifier.weight(1f)
            )
            OverviewMetricCard(
                title = "Due Reviews",
                value = dueCards.toString(),
                icon = Icons.Default.Timer,
                color = IncorrectRed,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Progress Card
        ProgressAnalyticsCard(
            masteredCount = masteredCards,
            totalCount = totalCardsCount,
            learningScore = viewModel.calculateLearningScore(),
            weeklyStats = statistics.takeLast(7)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Actions Row
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionButton(
                label = "Create Deck",
                icon = Icons.Default.AddBox,
                color = PrimaryLight,
                onClick = onCreateDeckClick,
                testTag = "quick_create_deck",
                modifier = Modifier.weight(1f)
            )
            QuickActionButton(
                label = "Study",
                icon = Icons.Default.PlayArrow,
                color = SecondaryLight,
                onClick = {
                    val target = decks.maxByOrNull { it.lastStudied } ?: decks.firstOrNull()
                    if (target != null) {
                        onContinueStudyingClick(target)
                    } else {
                        onCreateDeckClick()
                    }
                },
                testTag = "quick_continue_study",
                modifier = Modifier.weight(1f)
            )
            QuickActionButton(
                label = "Review Errors",
                icon = Icons.Default.NewReleases,
                color = IncorrectRed,
                onClick = onReviewMistakesClick,
                testTag = "quick_review_mistakes",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun OverviewMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-0.5).sp
                )
            )
        }
    }
}

@Composable
fun ProgressAnalyticsCard(
    masteredCount: Int,
    totalCount: Int,
    learningScore: Int,
    weeklyStats: List<DailyStatistic>,
    modifier: Modifier = Modifier
) {
    val progressPercent = if (totalCount > 0) (masteredCount * 100) / totalCount else 0
    val isDark = isSystemInDarkTheme()

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Smart Learning Progress",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else HeadingLight
                        )
                    )
                    Text(
                        text = "Score based on mastery and consistency",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isDark) Color.White.copy(alpha = 0.75f) else BodyLight
                        )
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = if (isDark) Color.White.copy(alpha = 0.15f) else PrimaryLight.copy(alpha = 0.12f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$learningScore",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color.White else PrimaryLight
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Progress bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Mastery Level ($masteredCount/$totalCount cards)",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color.White.copy(alpha = 0.9f) else BodyLight
                    )
                )
                Text(
                    text = "$progressPercent%",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else PrimaryLight
                    )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { if (totalCount > 0) masteredCount.toFloat() / totalCount else 0f },
                color = if (isDark) Color.White else PrimaryLight,
                trackColor = if (isDark) Color.White.copy(alpha = 0.2f) else PrimaryLight.copy(alpha = 0.12f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            // Dynamic mini calendar/weekly stats visualizer
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "My Study Performance (Last 7 Days)",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White.copy(alpha = 0.8f) else BodyLight
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val limit = 7
                val paddedList = if (weeklyStats.size >= limit) {
                    weeklyStats.takeLast(limit)
                } else {
                    val needed = limit - weeklyStats.size
                    List(needed) { DailyStatistic(date = "Day $it", cardsStudied = 0) } + weeklyStats
                }

                paddedList.forEach { stat ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        val active = stat.cardsStudied > 0
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (active) {
                                        if (isDark) Color.White else PrimaryLight
                                    } else {
                                        if (isDark) Color.White.copy(alpha = 0.15f) else PrimaryLight.copy(alpha = 0.08f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (active) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF2A2475) else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        val dayLabel = try {
                            val formatInput = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val d = formatInput.parse(stat.date)
                            if (d != null) {
                                SimpleDateFormat("EEE", Locale.getDefault()).format(d)
                            } else {
                                "-"
                            }
                        } catch (e: Exception) {
                            stat.date.takeLast(2)
                        }
                        Text(
                            text = dayLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = if (isDark) Color.White.copy(alpha = 0.7f) else BodyLight.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }

@Composable
fun QuickActionButton(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    Card(
        modifier = modifier
            .testTag(testTag)
            .appCardClickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) color.copy(alpha = 0.15f) else Color.White
        ),
        border = BorderStroke(
            width = if (isDark) 1.dp else 0.75.dp,
            color = if (isDark) color.copy(alpha = 0.3f) else PrimaryLight.copy(alpha = 0.15f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(color, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (isDark) Color.White else color
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

