package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Achievement
import com.example.data.model.DailyStatistic
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.FlashMasterViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StatisticsScreen(
    viewModel: FlashMasterViewModel,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.allStatistics.collectAsState()
    val achievements by viewModel.achievements.collectAsState()
    val sessions by viewModel.allSessions.collectAsState()
    val allCards by viewModel.allCards.collectAsState()
    val isDark = isSystemInDarkTheme()

    // Aggregate values
    val totalStudiedCards = stats.sumOf { it.cardsStudied }
    val totalCorrect = stats.sumOf { it.correctCount }
    val totalIncorrect = stats.sumOf { it.incorrectCount }
    val totalStudyHours = stats.sumOf { it.studyTimeMs } / (1000f * 60f * 60f)

    val accuracy = if (totalCorrect + totalIncorrect > 0) {
        (totalCorrect.toFloat() / (totalCorrect + totalIncorrect)) * 100
    } else {
        0f
    }

    val masteredCards = allCards.count { it.repetitions >= 3 && !it.isIncorrect }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .screenBackground()
    ) {
        item {
            HeroHeader(
                title = "Study Analytics",
                subtitle = "Measure your daily performance, accuracy thresholds, and unlockable badges.",
                actionIcon = Icons.Default.InsertChart,
                onActionClick = {},
                testTagSuffix = "analytics"
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Accuracy Rate circular card
                GlassCard(
                    modifier = Modifier.weight(1.1f)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Recall Accuracy",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(90.dp)) {
                            CircularProgressIndicator(
                                progress = { accuracy / 100f },
                                color = CorrectGreen,
                                trackColor = CorrectGreen.copy(alpha = 0.1f),
                                strokeWidth = 8.dp,
                                modifier = Modifier.fillMaxSize()
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${accuracy.toInt()}%",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                                )
                                Text(
                                    text = "Correct",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                    }
                }

                // Cards Mastered overview
                GlassCard(
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Mastery Levels",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Icon(imageVector = Icons.Default.Verified, contentDescription = null, tint = SecondaryLight, modifier = Modifier.size(42.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "$masteredCards cards",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, color = SecondaryLight),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "SM-2 Level 3+",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Mini Analytics statistics grid
        item {
            Spacer(modifier = Modifier.height(16.dp))
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatSummaryTile(
                        label = "Total Studied",
                        value = "$totalStudiedCards",
                        icon = Icons.Default.ImportContacts,
                        color = PrimaryLight
                    )
                    StatSummaryTile(
                        label = "Study Hours",
                        value = "%.1f hrs".format(totalStudyHours),
                        icon = Icons.Default.AccessTime,
                        color = SecondaryLight
                    )
                    StatSummaryTile(
                        label = "Sessions",
                        value = "${sessions.size}",
                        icon = Icons.Default.FitnessCenter,
                        color = HardOrange
                    )
                }
            }
        }

        // Custom drawn visual bar chart of last 7 study records
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Study Duration & Trends",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Studied cards log (Recent recordings)",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (stats.isEmpty() || stats.sumOf { it.cardsStudied } == 0) {
                        Text(
                            text = "Study cards to accumulate weekly graph metrics.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        )
                    } else {
                        // Custom bar chart using standard compose boxes
                        val last7Stats = stats.takeLast(7)
                        val maxStudied = last7Stats.maxOfOrNull { it.cardsStudied }?.coerceAtLeast(1) ?: 1
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            last7Stats.forEach { dayStat ->
                                val heightFactor = dayStat.cardsStudied.toFloat() / maxStudied
                                val barWeight = (heightFactor * 100).coerceAtLeast(6f).dp

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "${dayStat.cardsStudied}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(20.dp)
                                            .height(barWeight)
                                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(PrimaryLight, SecondaryLight)
                                                )
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = dayStat.date.takeLast(2), // shows day number
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Achievements gamification checklist section
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Earned Medals & Badges",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (achievements.isEmpty()) {
            item {
                CircularProgressIndicator()
            }
        } else {
            items(achievements) { badge ->
                AchievementRowItem(achievement = badge)
            }
        }

        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun StatSummaryTile(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun AchievementRowItem(achievement: Achievement) {
    val active = achievement.isUnlocked
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (active) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Glowing or greyed medal representation
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) Brush.linearGradient(listOf(HardOrange.copy(alpha = 0.2f), EasyBlue.copy(alpha = 0.2f)))
                        else SolidColor(MaterialTheme.colorScheme.surfaceVariant)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🏅",
                    fontSize = 24.sp,
                    modifier = Modifier.graphicsLayer { alpha = if (active) 1f else 0.4f }
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = achievement.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            if (active) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Unlocked",
                    tint = CorrectGreen,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
