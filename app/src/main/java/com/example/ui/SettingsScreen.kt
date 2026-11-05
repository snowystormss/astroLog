package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.MilitaryTimePicker
import com.example.ui.components.DatePickerRow
import com.example.ui.components.CityAutocompleteField
import com.example.data.UserProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    profile: UserProfile,
    onBack: () -> Unit = {},
    onNavigateToCodex: () -> Unit = {},
    onNavigateToModelHub: () -> Unit = {}
) {
    var name by rememberSaveable { mutableStateOf(profile.name) }
    
    val initialDateParts = profile.birthDate.split("/")
    var birthMonth by rememberSaveable { mutableStateOf(initialDateParts.getOrElse(0) { "" }) }
    var birthDay by rememberSaveable { mutableStateOf(initialDateParts.getOrElse(1) { "" }) }
    var birthYear by rememberSaveable { mutableStateOf(initialDateParts.getOrElse(2) { "" }) }
    
    val initialTimeParts = if (profile.birthTime.contains("Unknown")) listOf("", "") else {
        val parts = profile.birthTime.split(":")
        if (parts.size >= 2) parts else listOf("", "")
    }
    
    var birthHour by rememberSaveable { mutableStateOf(initialTimeParts[0].take(2)) }
    var birthMinute by rememberSaveable { mutableStateOf(initialTimeParts[1].take(2)) }
    
    var unknownTime by rememberSaveable { mutableStateOf(profile.birthTime.contains("Unknown")) }
    
    var locationQuery by rememberSaveable { mutableStateOf(profile.birthLocation) }
    var latitude by rememberSaveable { mutableDoubleStateOf(profile.latitude) }
    var longitude by rememberSaveable { mutableDoubleStateOf(profile.longitude) }
    var isResolvingLoc by remember { mutableStateOf(false) }

    val isFormValid = name.isNotBlank() && birthMonth.isNotBlank() && birthDay.isNotBlank() && birthYear.length == 4 && locationQuery.isNotBlank() && (unknownTime || (birthHour.isNotBlank() && birthMinute.isNotBlank()))

    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val settings = userSettings ?: com.example.data.UserSettings()
    val context = androidx.compose.ui.platform.LocalContext.current

    var isExporting100Year by remember { mutableStateOf(false) }
    var generatedReportMarkdown by remember { mutableStateOf<String?>(null) }
    var showPreviewDialog by remember { mutableStateOf(false) }
    var previewMarkdownContent by remember { mutableStateOf<String?>(null) }

    val createDocLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        uri?.let { destUri ->
            generatedReportMarkdown?.let { mdContent ->
                try {
                    context.contentResolver.openOutputStream(destUri)?.use { out ->
                        out.write(mdContent.toByteArray(Charsets.UTF_8))
                    }
                    android.widget.Toast.makeText(context, "100-Year Life Dossier (.md) exported successfully!", android.widget.Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Failed to save dossier: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    com.example.ui.components.CosmicBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(vertical = 32.dp)
        ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.padding(end = 8.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go Back")
                }
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Zodiac Codex Card
        item {
            Card(
                onClick = onNavigateToCodex,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = "Zodiac Codex",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Zodiac Codex & Guide",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Explore the 12 signs, decans, planets, houses & geometric aspects.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Open Codex",
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .rotate(180f),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // On-Device AI & 4B Model Hub Card
            Card(
                onClick = onNavigateToModelHub,
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer, androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = "On-Device AI Host",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "On-Device AI & 4B Model Hub",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Host 4B & under quantized models (.gguf), monitor RAM, and manage 16k context.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Open Model Hub",
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .rotate(180f),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Theme Toggle
        item {
            Column(modifier = Modifier) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                        Text(
                            text = "Theme Appearance",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isDarkTheme) "Dark Mode (Black + Brown)" else "Light Mode (Cream + White)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { viewModel.setDarkTheme(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Astrological House System Section
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Astrological House System",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Choose how the 12 astrological houses are divided for natal charts and placements.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                val currentSystem = settings.houseSystem.ifBlank { "WHOLE_SIGN" }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Whole Sign Button
                    val isWholeSign = currentSystem.uppercase() == "WHOLE_SIGN"
                    Card(
                        onClick = { viewModel.setHouseSystem("WHOLE_SIGN") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isWholeSign) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (isWholeSign) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = androidx.compose.ui.Alignment.Start
                        ) {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                RadioButton(
                                    selected = isWholeSign,
                                    onClick = { viewModel.setHouseSystem("WHOLE_SIGN") }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Whole Sign",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isWholeSign) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Classical Hellenistic method. Every house spans an entire 30° zodiac sign from the Ascendant.",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isWholeSign) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    // Placidus Button
                    val isPlacidus = currentSystem.uppercase() == "PLACIDUS"
                    Card(
                        onClick = { viewModel.setHouseSystem("PLACIDUS") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isPlacidus) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (isPlacidus) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = androidx.compose.ui.Alignment.Start
                        ) {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                RadioButton(
                                    selected = isPlacidus,
                                    onClick = { viewModel.setHouseSystem("PLACIDUS") }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Placidus",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPlacidus) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Modern quadrant semi-arc trisection. Unequal houses reflecting diurnal & nocturnal motion.",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isPlacidus) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        item {
            Column(modifier = Modifier) {

            Text(
                text = "Transit Notifications",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Retrogrades", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = settings.notifyRetrogrades,
                    onCheckedChange = { viewModel.updateUserSettings(settings.copy(notifyRetrogrades = it)) }
                )
            }
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Major Aspects", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = settings.notifyMajorAspects,
                    onCheckedChange = { viewModel.updateUserSettings(settings.copy(notifyMajorAspects = it)) }
                )
            }
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Lunar Phases", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = settings.notifyLunarPhases,
                    onCheckedChange = { viewModel.updateUserSettings(settings.copy(notifyLunarPhases = it)) }
                )
            }
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Personal Transits", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = settings.notifyPersonalTransits,
                    onCheckedChange = { viewModel.updateUserSettings(settings.copy(notifyPersonalTransits = it)) }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Daily Reminder",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Enable Daily Notifications", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = settings.dailyReminderEnabled,
                    onCheckedChange = { 
                        viewModel.updateUserSettings(settings.copy(dailyReminderEnabled = it))
                        // We do not cancel the worker because it also fetches daily background updates regardless of notifications.
                        if (it) {
                            com.example.ui.NotificationHelper.scheduleDailyReminder(context, settings.dailyReminderTime)
                        }
                    }
                )
            }
            if (settings.dailyReminderEnabled) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Reminder Time", style = MaterialTheme.typography.bodyLarge)
                    var expandedTimeDropdown by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { expandedTimeDropdown = true }) {
                            Text(settings.dailyReminderTime)
                        }
                        DropdownMenu(
                            expanded = expandedTimeDropdown,
                            onDismissRequest = { expandedTimeDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Morning (8:00 AM)") },
                                onClick = { 
                                    viewModel.updateUserSettings(settings.copy(dailyReminderTime = "Morning"))
                                    com.example.ui.NotificationHelper.scheduleDailyReminder(context, "Morning")
                                    expandedTimeDropdown = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Night (8:00 PM)") },
                                onClick = { 
                                    viewModel.updateUserSettings(settings.copy(dailyReminderTime = "Night"))
                                    com.example.ui.NotificationHelper.scheduleDailyReminder(context, "Night")
                                    expandedTimeDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Chatbot Preferences",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Audio Feedback (TTS)", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = settings.enableAudioFeedback,
                    onCheckedChange = { viewModel.updateUserSettings(settings.copy(enableAudioFeedback = it)) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Data, Export & Backup Section
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Data, Export & Backup",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Export and archive your lifelong astrological data and planetary cycles.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                // 100-Year Life Astrology Export Card
                val birthYr = parseBirthYear(profile.birthDate)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, androidx.compose.foundation.shape.CircleShape),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HistoryEdu,
                                    contentDescription = "100-Year Export",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "100-Year Life Astrology Dossier",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text(
                                        text = "Ages 0 to 100 • $birthYr – ${birthYr + 100} • .md",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Exports a complete chronological life ledger in Markdown (.md) text dating from the start of your birth ($birthYr) through 100 years ($birthYr – ${birthYr + 100}). Includes 101 annual Hellenistic profections, active Time Lords, 3 Saturn Returns, 8 Jupiter Returns, Chiron Return, Uranus opposition, and celestial life milestones.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (isExporting100Year) {
                            Row(
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Calculating 100-Year Life Timeline...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        isExporting100Year = true
                                        viewModel.generate100YearAstrologyReport(profile) { md ->
                                            isExporting100Year = false
                                            generatedReportMarkdown = md
                                            val sanitized = profile.name.trim().replace("\\s+".toRegex(), "_")
                                            createDocLauncher.launch("AstroLog_100_Year_Timeline_${sanitized}_$birthYr.md")
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .height(48.dp)
                                        .testTag("export_100_year_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FileDownload,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Export .md", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        isExporting100Year = true
                                        viewModel.generate100YearAstrologyReport(profile) { md ->
                                            isExporting100Year = false
                                            generatedReportMarkdown = md
                                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(android.content.Intent.EXTRA_SUBJECT, "100-Year Cosmic Astrology Dossier - ${profile.name}")
                                                putExtra(android.content.Intent.EXTRA_TEXT, md)
                                            }
                                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share 100-Year Dossier"))
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .height(48.dp)
                                        .testTag("share_100_year_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Share", fontSize = 13.sp)
                                }

                                TextButton(
                                    onClick = {
                                        isExporting100Year = true
                                        viewModel.generate100YearAstrologyReport(profile) { md ->
                                            isExporting100Year = false
                                            previewMarkdownContent = md
                                            showPreviewDialog = true
                                        }
                                    },
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("preview_100_year_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Preview", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        item {
            Column(modifier = Modifier) {
            Text(
                text = "Edit Information",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Update your birth information. This will regenerate your insights.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))

            val savedProfiles by viewModel.savedProfiles.collectAsStateWithLifecycle()
            var expandedPresetDropdown by remember { mutableStateOf(false) }

            if (savedProfiles.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = expandedPresetDropdown,
                    onExpandedChange = { expandedPresetDropdown = !expandedPresetDropdown },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = "Load a Saved Profile",
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        maxLines = 1,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPresetDropdown) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedPresetDropdown,
                        onDismissRequest = { expandedPresetDropdown = false }
                    ) {
                        savedProfiles.forEach { savedProfile ->
                            DropdownMenuItem(
                                text = { Text(savedProfile.name) },
                                onClick = {
                                    name = savedProfile.name
                                    val dateParts = savedProfile.birthDate.split("/")
                                    birthMonth = dateParts.getOrElse(0) { "" }
                                    birthDay = dateParts.getOrElse(1) { "" }
                                    birthYear = dateParts.getOrElse(2) { "" }
                                    
                                    val timeParts = if (savedProfile.birthTime.contains("Unknown")) listOf("", "") else {
                                        val parts = savedProfile.birthTime.split(":")
                                        if (parts.size >= 2) parts else listOf("", "")
                                    }
                                    birthHour = timeParts[0].take(2)
                                    birthMinute = timeParts.getOrNull(1)?.take(2) ?: ""
                                    unknownTime = savedProfile.birthTime.contains("Unknown")

                                    locationQuery = savedProfile.birthLocation
                                    latitude = savedProfile.latitude
                                    longitude = savedProfile.longitude
                                    
                                    expandedPresetDropdown = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            }
        }

        item {
            Column(modifier = Modifier) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Your Name", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                singleLine = true,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            DatePickerRow(
                month = birthMonth,
                day = birthDay,
                year = birthYear,
                onMonthChange = { birthMonth = it },
                onDayChange = { birthDay = it },
                onYearChange = { birthYear = it }
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            MilitaryTimePicker(
                hour = birthHour,
                minute = birthMinute,
                onHourChange = { birthHour = it },
                onMinuteChange = { birthMinute = it },
                enabled = !unknownTime
            )

            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = unknownTime,
                    onCheckedChange = { unknownTime = it }
                )
                Text(
                    text = "I don't know my birth time",
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            CityAutocompleteField(
                query = locationQuery,
                onQueryChange = {
                    locationQuery = it
                    latitude = 0.0
                    longitude = 0.0
                },
                latitude = latitude,
                longitude = longitude,
                onLocationSelected = { city ->
                    locationQuery = city.formatted
                    latitude = city.latitude
                    longitude = city.longitude
                },
                isResolvingOnline = isResolvingLoc,
                onTriggerOnlineSearch = {
                    isResolvingLoc = true
                    viewModel.resolveLocation(locationQuery, { formatted, lat, lon ->
                        locationQuery = formatted
                        latitude = lat
                        longitude = lon
                        isResolvingLoc = false
                    }, {
                        isResolvingLoc = false
                    })
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (isFormValid && !isResolvingLoc) {
                            val birthDate = "$birthMonth/$birthDay/$birthYear"
                            val finalTime = if (unknownTime) "Unknown (12:00 PM)" else "$birthHour:$birthMinute"
                            if (latitude != 0.0 || longitude != 0.0) {
                                viewModel.saveSavedProfile(
                                    name = name,
                                    birthDate = birthDate,
                                    birthTime = finalTime,
                                    birthLocation = locationQuery,
                                    relationshipType = "Friend/Family",
                                    latitude = latitude,
                                    longitude = longitude
                                )
                            } else {
                                isResolvingLoc = true
                                viewModel.resolveLocation(locationQuery, { resAddr, lat, lon ->
                                    isResolvingLoc = false
                                    viewModel.saveSavedProfile(
                                        name = name,
                                        birthDate = birthDate,
                                        birthTime = finalTime,
                                        birthLocation = resAddr,
                                        relationshipType = "Friend/Family",
                                        latitude = lat,
                                        longitude = lon
                                    )
                                }, {
                                    isResolvingLoc = false
                                    viewModel.saveSavedProfile(
                                        name = name,
                                        birthDate = birthDate,
                                        birthTime = finalTime,
                                        birthLocation = locationQuery,
                                        relationshipType = "Friend/Family",
                                        latitude = latitude,
                                        longitude = longitude
                                    )
                                })
                            }
                        }
                    },
                    enabled = isFormValid && !isResolvingLoc,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save as Preset", fontSize = 14.sp)
                }

                Button(
                    onClick = {
                        if (isFormValid && !isResolvingLoc) {
                            val birthDate = "$birthMonth/$birthDay/$birthYear"
                            val finalTime = if (unknownTime) "Unknown (12:00 PM)" else "$birthHour:$birthMinute"
                            if (latitude != 0.0 || longitude != 0.0) {
                                viewModel.updateProfile(name, birthDate, finalTime, locationQuery, latitude, longitude)
                                onBack()
                            } else {
                                isResolvingLoc = true
                                viewModel.resolveLocation(locationQuery, { resAddr, lat, lon ->
                                    isResolvingLoc = false
                                    viewModel.updateProfile(name, birthDate, finalTime, resAddr, lat, lon)
                                    onBack()
                                }, {
                                    isResolvingLoc = false
                                    viewModel.updateProfile(name, birthDate, finalTime, locationQuery, latitude, longitude)
                                    onBack()
                                })
                            }
                        }
                    },
                    enabled = isFormValid && !isResolvingLoc,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    if (isResolvingLoc) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Locating...", fontSize = 14.sp)
                    } else {
                        Text("Update Info", fontSize = 14.sp, color = if (isFormValid) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(
                onClick = {
                    viewModel.clearSavedReports()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Text("Clear Saved Reports", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            }
        }
    }

    if (showPreviewDialog && previewMarkdownContent != null) {
        val birthYr = parseBirthYear(profile.birthDate)
        AlertDialog(
            onDismissRequest = { showPreviewDialog = false },
            title = {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "100-Year Dossier Preview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                    Text(
                        text = "Centennial Span: $birthYr — ${birthYr + 100} (101 Life Years • 10 Decades)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) {
                        LazyColumn(modifier = Modifier.padding(12.dp)) {
                            item {
                                Text(
                                    text = previewMarkdownContent ?: "",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPreviewDialog = false
                        generatedReportMarkdown = previewMarkdownContent
                        val sanitized = profile.name.trim().replace("\\s+".toRegex(), "_")
                        createDocLauncher.launch("AstroLog_100_Year_Timeline_${sanitized}_$birthYr.md")
                    }
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export .md")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPreviewDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
}

fun parseBirthYear(birthDate: String): Int {
    val clean = birthDate.trim()
    val parts = if (clean.contains("/")) clean.split("/") else clean.split("-")
    if (parts.isEmpty()) return 2000
    return if (parts[0].length == 4) {
        parts[0].toIntOrNull() ?: 2000
    } else {
        parts.lastOrNull()?.toIntOrNull() ?: 2000
    }
}
