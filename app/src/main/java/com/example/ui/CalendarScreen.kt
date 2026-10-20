package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Brightness3
import androidx.compose.material.icons.filled.AutoAwesome
import com.example.data.UserProfile
import java.text.SimpleDateFormat
import java.util.Calendar
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.draw.scale
import java.util.Locale

@Composable
fun CalendarScreen(viewModel: MainViewModel, profile: UserProfile) {
    val haptic = LocalHapticFeedback.current
    var selectedCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    val displayCalendar = remember(selectedCalendar) { selectedCalendar.clone() as Calendar }
    
    val allNotes by viewModel.eventNotes.collectAsStateWithLifecycle()

    val monthYearFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val yyyyMMddFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val mmmDdYyyyFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val yyyyMMFormat = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()) }
    
    val currentMonthStr = remember(displayCalendar) {
        monthYearFormat.format(displayCalendar.time)
    }

    val daysInMonth = displayCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfMonth = remember(displayCalendar) {
        val c = displayCalendar.clone() as Calendar
        c.set(Calendar.DAY_OF_MONTH, 1)
        c.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 7 = Saturday
    }

    var selectedDay by remember { mutableStateOf<Int?>(null) }
    
    // We will calculate the events for this month once
    val monthlyEvents = remember(displayCalendar, profile) {
        val (y, m, d) = if (profile.birthDate.contains("/")) {
            val parts = profile.birthDate.split("/")
            Triple(
                parts.getOrNull(2)?.toIntOrNull() ?: 2000,
                parts.getOrNull(0)?.toIntOrNull() ?: 1,
                parts.getOrNull(1)?.toIntOrNull() ?: 1
            )
        } else {
            val parts = profile.birthDate.split("-")
            Triple(
                parts.getOrNull(0)?.toIntOrNull() ?: 2000,
                parts.getOrNull(1)?.toIntOrNull() ?: 1,
                parts.getOrNull(2)?.toIntOrNull() ?: 1
            )
        }

        val hour = if (!profile.birthTime.startsWith("Unknown")) {
            val timeParts = profile.birthTime.split(":")
            val h = timeParts.getOrNull(0)?.toDoubleOrNull() ?: 12.0
            val min = timeParts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
            h + (min / 60.0)
        } else {
            12.0
        }

        val natalPlanets = PlanetCalculator.calculatePlanets(y, m, d, hour, latitude = profile.latitude, longitude = profile.longitude)
        
        // We calculate days starting from the 1st of the month
        val cal = displayCalendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        PlanetCalculator.calculateUpcomingEvents(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
            natalPlanets,
            days = daysInMonth
        )
    }

    var isListView by remember { mutableStateOf(false) }

    com.example.ui.components.CosmicBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(32.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = currentMonthStr, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val c = displayCalendar.clone() as Calendar
                    c.add(Calendar.MONTH, -1)
                    selectedCalendar = c
                    selectedDay = null
                }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                }
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val c = displayCalendar.clone() as Calendar
                    c.add(Calendar.MONTH, 1)
                    selectedCalendar = c
                    selectedDay = null
                }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                }
                IconButton(onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    isListView = !isListView 
                }) {
                    Icon(if (isListView) Icons.Default.CalendarMonth else Icons.AutoMirrored.Filled.ViewList, contentDescription = "Toggle View")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        AnimatedContent(
            targetState = isListView,
            transitionSpec = {
                androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) togetherWith androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300))
            },
            label = "viewStateToggle"
        ) { targetIsList ->
            if (!targetIsList) {
            // Days of week
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                    Text(text = it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val totalCells = firstDayOfMonth - 1 + daysInMonth
            val rows = (totalCells + 6) / 7
            
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                for (row in 0 until rows) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        for (col in 0 until 7) {
                            val cellIndex = row * 7 + col
                            val day = cellIndex - (firstDayOfMonth - 1) + 1
                            if (day in 1..daysInMonth) {
                                val c = displayCalendar.clone() as Calendar
                                c.set(Calendar.DAY_OF_MONTH, day)
                                c.set(Calendar.HOUR_OF_DAY, 12)
                                c.set(Calendar.MINUTE, 0)
                                c.set(Calendar.SECOND, 0)
                                val dayStart = c.timeInMillis
                                c.add(Calendar.DAY_OF_MONTH, 1)
                                val dayEnd = c.timeInMillis
                                
                                val dayEvents = monthlyEvents.filter { it.dateMillis in dayStart until dayEnd }
                                val dateStr = yyyyMMddFormat.format(dayStart)
                                val note = allNotes.find { it.dateStr == dateStr }
                                
                                val isSelected = selectedDay == day
                                val hasEvent = dayEvents.isNotEmpty()
                                val hasNote = note != null && note.note.isNotEmpty()
                                val bgColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
                                
                                val scale by animateFloatAsState(
                                    targetValue = if (isSelected) 1.15f else 1.0f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                    label = "scale"
                                )
                                
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .scale(scale)
                                        .clip(CircleShape)
                                        .background(bgColor)
                                        .clickable { 
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedDay = day 
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = day.toString(), style = MaterialTheme.typography.bodyMedium, color = contentColor)
                                        Row {
                                            if (hasEvent) {
                                                Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary))
                                            }
                                            if (hasNote) {
                                                if (hasEvent) Spacer(modifier = Modifier.width(2.dp))
                                                Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(if (isSelected) MaterialTheme.colorScheme.onPrimary else Color(0xFFFFD740)))
                                            }
                                        }
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.size(40.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (selectedDay != null) {
                val c = displayCalendar.clone() as Calendar
                c.set(Calendar.DAY_OF_MONTH, selectedDay!!)
                c.set(Calendar.HOUR_OF_DAY, 12)
                c.set(Calendar.MINUTE, 0)
                c.set(Calendar.SECOND, 0)
                val dayStart = c.timeInMillis
                c.add(Calendar.DAY_OF_MONTH, 1)
                val dayEnd = c.timeInMillis
                val dayEvents = monthlyEvents.filter { it.dateMillis in dayStart until dayEnd }
                val dateStr = yyyyMMddFormat.format(dayStart)
                val note = allNotes.find { it.dateStr == dateStr }

                var isEditingNote by remember { mutableStateOf(false) }
                var editNoteText by remember(note) { mutableStateOf(note?.note ?: "") }
                
                Text(
                    "Events on ${mmmDdYyyyFormat.format(dayStart)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 24.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item {
                        val lunarPhase = remember(dayStart) {
                            val cal = displayCalendar.clone() as Calendar
                            cal.timeInMillis = dayStart
                            PlanetCalculator.getLunarPhase(
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH) + 1,
                                cal.get(Calendar.DAY_OF_MONTH)
                            )
                        }
                        LunarCycleTracker(phaseInfo = lunarPhase, profile = profile)
                    }

                    if (dayEvents.isEmpty()) {
                        item {
                            Text("No major astrological events calculated for this day.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    } else {
                        items(dayEvents.size, key = { index -> "dayEvent_$index" }) { index ->
                            val event = dayEvents[index]
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(event.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(event.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Personal Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (!isEditingNote) {
                                IconButton(onClick = { isEditingNote = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Note")
                                }
                            }
                        }
                        if (isEditingNote) {
                            OutlinedTextField(
                                value = editNoteText,
                                onValueChange = { editNoteText = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Write down your intentions, feelings, or interpretations...") },
                                minLines = 3
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { 
                                    isEditingNote = false 
                                    editNoteText = note?.note ?: ""
                                }) {
                                    Text("Cancel")
                                }
                                Button(onClick = {
                                    viewModel.saveEventNote(dateStr, editNoteText)
                                    isEditingNote = false
                                }) {
                                    Text("Save Note")
                                }
                            }
                        } else {
                            if ((note?.note ?: "").isEmpty()) {
                                Text("No notes added.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Text(note?.note ?: "", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Text("Select a day to view its astrology and your notes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 24.dp)) {
                val eventsByDate = monthlyEvents.groupBy { 
                     yyyyMMddFormat.format(it.dateMillis)
                }
                
                val notesThisMonth = allNotes.filter { note ->
                    note.dateStr.startsWith(yyyyMMFormat.format(displayCalendar.time)) && note.note.isNotEmpty()
                }
                
                val allDatesWithActivity = (eventsByDate.keys + notesThisMonth.map { it.dateStr }).distinct().sorted()

                if (allDatesWithActivity.isEmpty()) {
                    item {
                        Text("No astrological events or personal notes this month.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                items(allDatesWithActivity.size, key = { index -> allDatesWithActivity[index] }) { index ->
                    val dateStr = allDatesWithActivity[index]
                    val dayEvents = eventsByDate[dateStr] ?: emptyList()
                    val note = allNotes.find { it.dateStr == dateStr }
                    
                    val dateParsed = yyyyMMddFormat.parse(dateStr)
                    val dateToDisplay = mmmDdYyyyFormat.format(dateParsed ?: java.util.Date())
                    
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(dateToDisplay, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            dayEvents.forEach { event ->
                                Text("• ${event.name}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(event.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            
                            if (note != null && note.note.isNotEmpty()) {
                                if (dayEvents.isNotEmpty()) Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Edit, contentDescription = "Note", modifier = Modifier.size(16.dp), tint = Color(0xFFFFD740))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Note", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFFFFD740))
                                }
                                Text(note.note, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
}
}

@Composable
fun LunarCycleTracker(phaseInfo: PlanetCalculator.LunarPhaseInfo, profile: com.example.data.UserProfile) {
    val dateParts = profile.birthDate.split("/")
    val month = dateParts.getOrNull(0)?.toIntOrNull() ?: 1
    val day = dateParts.getOrNull(1)?.toIntOrNull() ?: 1
    val year = dateParts.getOrNull(2)?.toIntOrNull() ?: 2000

    val isUnknown = profile.birthTime.startsWith("Unknown")
    val hour = if (!isUnknown) {
        val timeParts = profile.birthTime.split(":")
        val h = timeParts.getOrNull(0)?.toDoubleOrNull() ?: 12.0
        val m = timeParts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
        h + (m / 60.0)
    } else {
        12.0
    }
    
    val planets = remember(year, month, day, hour, profile.latitude, profile.longitude) {
        PlanetCalculator.calculatePlanets(year, month, day, hour, profile.latitude, profile.longitude)
    }
    val userMoonSign = planets["Moon"]?.sign ?: "Unknown"

    // Simple element matching for basic interpretation
    val getElement = { sign: String ->
        when (sign) {
            "Aries", "Leo", "Sagittarius" -> "Fire"
            "Taurus", "Virgo", "Capricorn" -> "Earth"
            "Gemini", "Libra", "Aquarius" -> "Air"
            "Cancer", "Scorpio", "Pisces" -> "Water"
            else -> "Unknown"
        }
    }

    val currentMoonElement = getElement(phaseInfo.moonSign)
    val userMoonElement = getElement(userMoonSign)

    val astrologicalSignificance = remember(phaseInfo.moonSign, userMoonSign) {
        if (phaseInfo.moonSign == userMoonSign) {
            "Lunar Return: The Moon is in your natal sign! Emotions feel familiar, authentic, and heightened. A powerful time for self-care and manifesting personal goals."
        } else if (currentMoonElement == userMoonElement && currentMoonElement != "Unknown") {
            "Harmonious Flow: The current Moon in ${phaseInfo.moonSign} shares your emotional element ($userMoonElement). Expect to feel more supported, inspired, and intuitively aligned."
        } else {
            "Growth Period: The ${phaseInfo.moonSign} Moon challenges your natal $userMoonSign Moon to explore new emotional perspectives. Use this energy to step out of your comfort zone."
        }
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = androidx.compose.material3.CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.NightsStay, contentDescription = "Lunar Phase Tracker", tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Lunar Tracker", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                Text(phaseInfo.moonSign, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(20.dp))
            
            // Progress Track
            val progress = (phaseInfo.angle / 360.0).toFloat()
            Box(modifier = Modifier.fillMaxWidth().height(32.dp), contentAlignment = Alignment.CenterStart) {
                // Background Track
                Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant))
                // Moon indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .wrapContentWidth(Alignment.End)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        // Drawing the moon phase roughly
                        Icon(
                            imageVector = Icons.Default.Brightness3,
                            contentDescription = "Moon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(phaseInfo.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(phaseInfo.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            
            Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "Significance", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Personal Significance", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(astrologicalSignificance, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
        }
    }
}
