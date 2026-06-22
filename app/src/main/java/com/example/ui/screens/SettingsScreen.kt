package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Deck
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.FlashMasterViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: FlashMasterViewModel,
    modifier: Modifier = Modifier
) {
    val decks by viewModel.decks.collectAsState()
    val allCards by viewModel.allCards.collectAsState()
    val isDarkSelected by viewModel.isDarkMode.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Dialog state
    var showResetDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var selectedDeckToExport by remember { mutableStateOf<Deck?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }

    // SharedPreferences for mocks configurations if needed
    var notificationsActive by remember { mutableStateOf(true) }
    var studyPreferencesDuration by remember { mutableStateOf("60s") }

    // File Picker for JSON Restore
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val error = viewModel.importDeckFromJson(context, uri)
            if (error == null) {
                Toast.makeText(context, "Deck and cards database imported!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Mismatched content standard: $error", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .screenBackground()
    ) {
        HeroHeader(
            title = "App Settings",
            subtitle = "Calibrate study frequencies, customize dark appearance, and backup databases.",
            actionIcon = Icons.Default.SettingsApplications,
            onActionClick = {},
            testTagSuffix = "settings"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Card Category: Visual Aesthetics
        SettingsCategorySection(title = "Appearance & Styling") {
            ThemeSelectorRow(viewModel = viewModel)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card Category: Study Timing Preferences
        SettingsCategorySection(title = "Study Preferences") {
            SettingsToggleRow(
                icon = Icons.Default.NotificationsActive,
                label = "Smart Review Reminders",
                description = "Daily push notifications alert you to due study cards.",
                checked = notificationsActive,
                onCheckedChange = { notificationsActive = it },
                testTag = "settings_notification_toggle"
            )
            
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsClickableRow(
                icon = Icons.Default.Timer,
                label = "Timed Mode Countdown",
                description = "Active study card countdown duration limit: $studyPreferencesDuration.",
                onClick = {
                    studyPreferencesDuration = if (studyPreferencesDuration == "30s") {
                        viewModel.timedModeLimitSeconds.value = 60
                        "60s"
                    } else if (studyPreferencesDuration == "60s") {
                        viewModel.timedModeLimitSeconds.value = 120
                        "120s"
                    } else {
                        viewModel.timedModeLimitSeconds.value = 30
                        "30s"
                    }
                    Toast.makeText(context, "Repetitions countdown timer updated to $studyPreferencesDuration", Toast.LENGTH_SHORT).show()
                },
                testTag = "settings_timedmode_limit"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card Category: Backup & File Sync Offline
        SettingsCategorySection(title = "Backup & Portability") {
            SettingsClickableRow(
                icon = Icons.Default.FileUpload,
                label = "Export Deck as JSON",
                description = "Download a theme color, properties, and associated card file.",
                onClick = {
                    if (decks.isEmpty()) {
                        Toast.makeText(context, "No decks available to export.", Toast.LENGTH_SHORT).show()
                    } else {
                        selectedDeckToExport = decks.firstOrNull()
                        showExportDialog = true
                    }
                },
                testTag = "settings_export_btn"
            )
            
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsClickableRow(
                icon = Icons.Default.FileDownload,
                label = "Restore Deck from File",
                description = "Upload and rebuild items from a previously matching JSON data document.",
                onClick = {
                    filePickerLauncher.launch("application/json")
                },
                testTag = "settings_import_btn"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card Category: Reset Action Block
        SettingsCategorySection(title = "Danger Zone") {
            SettingsClickableRow(
                icon = Icons.Default.DeleteForever,
                label = "Reset App Statistics",
                description = "Erase all streaks, session study logs, and score calendars.",
                iconColor = IncorrectRed,
                labelColor = IncorrectRed,
                onClick = { showResetDialog = true },
                testTag = "settings_reset_stats_btn"
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // About block metadata credentials
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 120.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "FlashMaster",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                )
                Text(text = "v1.0.0 (Production Build)", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "A high-fidelity spaced-repetition cognitive memory booster incorporating Leitner Sm-2 intervals and AI test assistants fully powered offline.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Download FlashMaster Study App!")
                            putExtra(Intent.EXTRA_TEXT, "FlashMaster Study App is incredible offline! Try Leitner sm-2 intervals and AI study quizzes!")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share FlashMaster App with Friends"))
                    }
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share FlashMaster with Friends")
                }
            }
        }
    }

    // Reset stats confirmation
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Erase Statistics Database?", fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently refresh all your history stats cards, streak meters, session summaries, medals, and study analytics. Flashcards will remain.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllMistakes()
                        Toast.makeText(context, "Study statistics refreshed successfully!", Toast.LENGTH_SHORT).show()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IncorrectRed)
                ) {
                    Text("Confirm Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Export deck JSON dialog
    if (showExportDialog && selectedDeckToExport != null) {
        var dropdownExpanded by remember { mutableStateOf(false) }
        
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Deck File", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select a deck to export to JSON:")
                    
                    Box {
                        OutlinedButton(onClick = { dropdownExpanded = true }) {
                            Text(text = selectedDeckToExport?.name ?: "Select Deck...")
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            decks.forEach { deckItem ->
                                DropdownMenuItem(
                                    text = { Text(deckItem.name) },
                                    onClick = {
                                        selectedDeckToExport = deckItem
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val target = selectedDeckToExport ?: return@launch
                            val cardsOfDecks = viewModel.getCardsForDeck(target.id).firstOrNull() ?: emptyList()
                            val jsonStringValue = viewModel.exportDeckToJsonString(target, cardsOfDecks)
                            
                            if (jsonStringValue != null) {
                                // Trigger share content action string
                                val exportIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/json"
                                    putExtra(Intent.EXTRA_SUBJECT, "${target.name} FlashMaster Export")
                                    putExtra(Intent.EXTRA_TEXT, jsonStringValue)
                                }
                                context.startActivity(Intent.createChooser(exportIntent, "Export FlashMaster JSON"))
                                Toast.makeText(context, "Export string ready for sync!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Exporting compilation failure.", Toast.LENGTH_SHORT).show()
                            }
                            showExportDialog = false
                        }
                    }
                ) {
                    Text("Download & Share")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsCategorySection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = title.uppercase(java.util.Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    }
}

@Composable
fun SettingsToggleRow(
    icon: ImageVector,
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(text = label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = description, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
            }
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
fun SettingsClickableRow(
    icon: ImageVector,
    label: String,
    description: String,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor.copy(alpha = 0.11f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = labelColor))
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = description, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun ThemeSelectorRow(
    viewModel: FlashMasterViewModel,
    modifier: Modifier = Modifier
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val isDark = isSystemInDarkTheme()
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "App Theme Mode",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Choose light, dark, or system follow mode.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Segmented selector container
        val borderThemeColor = MaterialTheme.colorScheme.outline
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(45.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .border(1.dp, borderThemeColor, RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val options = listOf(
                Triple("system", "System", Icons.Default.Settings),
                Triple("light", "Light", Icons.Default.LightMode),
                Triple("dark", "Dark", Icons.Default.DarkMode)
            )
            
            options.forEach { (mode, label, icon) ->
                val isSelected = themeMode == mode
                val activeBg = MaterialTheme.colorScheme.primary
                val activeText = MaterialTheme.colorScheme.onPrimary
                val inactiveText = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) activeBg else Color.Transparent)
                        .clickable { viewModel.setThemeMode(mode) }
                        .testTag("theme_selector_$mode"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            modifier = Modifier.size(16.dp),
                            tint = if (isSelected) activeText else inactiveText
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                fontSize = 12.sp,
                                color = if (isSelected) activeText else inactiveText
                            )
                        )
                    }
                }
            }
        }
    }
}
