package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryLight
import com.example.ui.theme.SecondaryLight
import com.example.ui.theme.TertiaryLight
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun getAdaptiveGradient(): Brush {
    val isDark = isSystemInDarkTheme()
    return if (isDark) {
        Brush.linearGradient(
            colors = listOf(Color(0xFF312E81), Color(0xFF6D28D9)) // Deep Indigo → Violet
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0xFF4F46E5), Color(0xFF3B82F6)) // Indigo → Blue
        )
    }
}

@Composable
fun getAdaptiveSecondaryGradient(): Brush {
    val isDark = isSystemInDarkTheme()
    return if (isDark) {
        Brush.linearGradient(
            colors = listOf(Color(0xFF0F172A), Color(0xFF581C87)) // Navy → Purple
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)) // Indigo → Purple
        )
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val cardBg = if (isDark) {
        Color(0xFF1E293B) // Premium solid opaque Slate 800 (no glassmorphic transparency)
    } else {
        Color.Transparent // Transparent container so inner gradient is fully visible with glass effect
    }
    
    val borderColor = if (isDark) {
        Color(0xFF334155) // Solid Slate 700 border
    } else {
        Color(0x3378A0FF) // rgba(120, 160, 255, 0.2) subtle blue outline
    }
    
    val elevationValue = if (isDark) 1.dp else 8.dp
    val cardModifier = if (isDark) {
        modifier
    } else {
        modifier.shadow(
            elevation = elevationValue,
            shape = RoundedCornerShape(24.dp),
            clip = false,
            spotColor = Color(0x145078C8),
            ambientColor = Color(0x145078C8)
        )
    }
    
    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(if (isDark) 1.dp else 0.75.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 1.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier
                .then(
                    if (isDark) Modifier
                    else Modifier.background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xB3FFFFFF), // rgba(255,255,255,0.7) for top inner highlight
                                Color(0x66E6F0FF)  // rgba(230,240,255,0.4) for bottom reflection
                            )
                        )
                    )
                )
                .padding(20.dp),
            content = content
        )
    }
}

@Composable
fun HeroHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionIcon: ImageVector? = null,
    onActionClick: (() -> Unit)? = null,
    testTagSuffix: String = ""
) {
    val isDark = isSystemInDarkTheme()
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(top = 22.dp, bottom = 18.dp, start = 20.dp, end = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(0.8f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = (-0.5).sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (actionIcon != null && onActionClick != null) {
                IconButton(
                    onClick = onActionClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .testTag("header_action_$testTagSuffix")
                ) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = "Header action",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(getAdaptiveGradient()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "F",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    testTag: String = ""
) {
    Surface(
        onClick = { if (enabled) onClick() },
        enabled = enabled,
        modifier = modifier
            .testTag(testTag)
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp)),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (enabled) {
                        getAdaptiveGradient()
                    } else {
                        Brush.linearGradient(
                            colors = listOf(Color.Gray.copy(alpha = 0.4f), Color.Gray.copy(alpha = 0.4f))
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                     text = text,
                     style = MaterialTheme.typography.titleMedium.copy(
                         color = Color.White,
                         fontWeight = FontWeight.Bold
                     )
                )
            }
        }
    }
}

@Composable
fun EmptyPlaceholder(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onActionClick,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = actionText)
            }
        }
    }
}

@Composable
fun Modifier.screenBackground(): Modifier {
    val isDark = isSystemInDarkTheme()
    return if (isDark) {
        this.background(MaterialTheme.colorScheme.background)
    } else {
        this.background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFF7F9FC), // soft off-white
                    Color(0xFFEEF4FF)  // very light blue
                )
            )
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.appCardClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: androidx.compose.ui.semantics.Role? = null,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = this.composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "card_scale"
    )
    val translationY by animateFloatAsState(
        targetValue = if (isPressed && enabled) 4f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "card_translation_elevation"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.translationY = translationY
        }
        .combinedClickable(
            interactionSource = interactionSource,
            indication = androidx.compose.foundation.LocalIndication.current,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onLongClickLabel = onLongClickLabel,
            onLongClick = onLongClick,
            onDoubleClick = onDoubleClick,
            onClick = onClick
        )
}

