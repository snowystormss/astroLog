package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness3
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.UserProfile
import com.example.ui.PlanetCalculator
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

@Composable
fun CosmicTimeline(profile: UserProfile, modifier: Modifier = Modifier, onViewCalendar: () -> Unit = {}) {
    val haptic = LocalHapticFeedback.current
    val events = remember(profile) {
        val y = profile.birthDate.split("-").getOrNull(0)?.toIntOrNull() ?: 2000
        val m = profile.birthDate.split("-").getOrNull(1)?.toIntOrNull() ?: 1
        val d = profile.birthDate.split("-").getOrNull(2)?.toIntOrNull() ?: 1
        val natalPlanets = PlanetCalculator.calculatePlanets(y, m, d)

        val cal = Calendar.getInstance()
        PlanetCalculator.calculateUpcomingEvents(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
            natalPlanets
        )
    }
    
    var showOnlyMajor by remember { mutableStateOf(false) }

    if (events.isNotEmpty()) {
        Column(modifier = modifier.fillMaxWidth().animateContentSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Cosmic Timeline",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Major Only", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = showOnlyMajor,
                        onCheckedChange = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showOnlyMajor = it 
                        },
                        modifier = Modifier.scale(0.8f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            TimelineLegend()
            Spacer(modifier = Modifier.height(8.dp))
            
            val filteredEvents = if (showOnlyMajor) events.filter { it.intensity == "MAJOR" } else events

            if (filteredEvents.isEmpty()) {
                 Text("No major shifts coming up.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                events.forEach { event ->
                    val isVisible = !showOnlyMajor || event.intensity == "MAJOR"
                    val isLastVisible = event == filteredEvents.lastOrNull()
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        val typeColor = when (event.type) {
                            "ECLIPSE" -> Color(0xFFFF5252)
                            "LUNAR" -> if (event.name.contains("Full Moon")) Color(0xFFE0E0E0) else Color(0xFF757575)
                            "PLANETARY" -> Color(0xFF00E5FF)
                            "PERSONAL" -> Color(0xFFFFD740)
                            else -> Color(0xFFB388EB)
                        }
                        TimelineItem(event = event, typeColor = typeColor, isLast = isLastVisible)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onViewCalendar,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.Event, contentDescription = "Full Calendar")
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Full Calendar")
            }
        }
    }
}

@Composable
fun TimelineItem(event: PlanetCalculator.AstroEvent, typeColor: Color, isLast: Boolean) {
    val formatter = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }
    val dateStr = formatter.format(event.dateMillis)

    val icon = when (event.type) {
        "ECLIPSE" -> Icons.Default.Brightness7
        "LUNAR" -> if (event.name.contains("Full Moon")) Icons.Default.Brightness3 else Icons.Default.NightsStay
        "PLANETARY" -> Icons.Default.Public
        "PERSONAL" -> Icons.Default.Stars
        else -> Icons.Default.Event
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        // Left Column: Date & Line
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp)) {
            Text(
                text = dateStr,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            // Icon Badge
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(typeColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = typeColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            
            if (!isLast) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(80.dp) // Fixed height to simulate timeline line
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Right Column: Content
        Column(modifier = Modifier.weight(1f).padding(bottom = 24.dp)) {
            Text(
                text = event.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = event.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Status Badge
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(typeColor.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = event.type,
                    style = MaterialTheme.typography.labelSmall,
                    color = typeColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TimelineLegend() {
    val legendItems = listOf(
        "Eclipse" to Color(0xFFFF5252),
        "Lunar" to Color(0xFF757575),
        "Planetary" to Color(0xFF00E5FF),
        "Personal" to Color(0xFFFFD740)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        legendItems.forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label, 
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
