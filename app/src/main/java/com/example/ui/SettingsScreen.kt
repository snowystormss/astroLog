package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.MilitaryTimePicker
import com.example.ui.components.DatePickerRow
import com.example.data.UserProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    profile: UserProfile,
    onBack: () -> Unit = {}
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
    val context = androidx.compose.ui.platform.LocalContext.current

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

        item {
            Column(modifier = Modifier) {
            val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
            val settings = userSettings ?: com.example.data.UserSettings()

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

            OutlinedTextField(
                value = locationQuery,
                onValueChange = { 
                    locationQuery = it
                    latitude = 0.0
                    longitude = 0.0
                },
                singleLine = true,
                maxLines = 1,
                label = { Text("Birth City, State, Country", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            
            if (latitude != 0.0 || longitude != 0.0) {
                Text(
                    text = "Coordinates: ${"%.2f".format(latitude)}, ${"%.2f".format(longitude)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )
            } else {
                TextButton(
                    onClick = {
                        isResolvingLoc = true
                        viewModel.resolveLocation(locationQuery, { formatted, lat, lon ->
                            locationQuery = formatted
                            latitude = lat
                            longitude = lon
                            isResolvingLoc = false
                        }, {
                            isResolvingLoc = false
                        })
                    },
                    enabled = locationQuery.isNotBlank() && !isResolvingLoc
                ) {
                    Text(if (isResolvingLoc) "Locating..." else "Find Coordinates")
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (isFormValid) {
                            val birthDate = "$birthMonth/$birthDay/$birthYear"
                            val finalTime = if (unknownTime) "Unknown (12:00 PM)" else "$birthHour:$birthMinute"
                            val location = locationQuery
                            viewModel.saveSavedProfile(
                                name = name,
                                birthDate = birthDate,
                                birthTime = finalTime,
                                birthLocation = location,
                                relationshipType = "Friend/Family",
                                latitude = latitude,
                                longitude = longitude
                            )
                        }
                    },
                    enabled = isFormValid,
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
                        if (isFormValid) {
                            val birthDate = "$birthMonth/$birthDay/$birthYear"
                            val finalTime = if (unknownTime) "Unknown (12:00 PM)" else "$birthHour:$birthMinute"
                            val location = locationQuery
                            viewModel.updateProfile(name, birthDate, finalTime, location, latitude, longitude)
                            onBack()
                        }
                    },
                    enabled = isFormValid,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text("Update Info", fontSize = 14.sp, color = if (isFormValid) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
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
}
}
