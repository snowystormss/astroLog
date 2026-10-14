package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.WbIridescent
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.UserProfile
import com.example.ui.PlanetCalculator
import com.example.ui.exportToCalendar
import java.util.Calendar

@Composable
fun PlanetaryWeatherCard(profile: UserProfile, settings: com.example.data.UserSettings?, modifier: Modifier = Modifier) {
    val todayAndUpcomingTransits = remember(profile, settings) {
        val y = profile.birthDate.split("-").getOrNull(0)?.toIntOrNull() ?: 2000
        val m = profile.birthDate.split("-").getOrNull(1)?.toIntOrNull() ?: 1
        val d = profile.birthDate.split("-").getOrNull(2)?.toIntOrNull() ?: 1
        val natalPlanets = PlanetCalculator.calculatePlanets(y, m, d)

        val cal = Calendar.getInstance()
        var events = PlanetCalculator.calculateUpcomingEvents(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
            natalPlanets,
            days = 3 // Look closely at the next 3 days
        )
        
        if (settings != null) {
            events = events.filter { event ->
                when (event.type) {
                    "RETROGRADE" -> settings.notifyRetrogrades
                    "MAJOR_ASPECT" -> settings.notifyMajorAspects
                    "LUNAR_PHASE" -> settings.notifyLunarPhases
                    "PERSONAL" -> settings.notifyPersonalTransits
                    else -> true
                }
            }.toMutableList()
        }
        
        events
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val context = LocalContext.current

    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WbIridescent, contentDescription = "Weather", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.graphicsLayer { scaleX = pulseScale; scaleY = pulseScale })
                Spacer(modifier = Modifier.width(8.dp))
                Text("Planetary Weather", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (todayAndUpcomingTransits.isEmpty()) {
                Text(
                    "The cosmic weather is mostly calm for you today. A good time for steady progress.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            } else {
                Text(
                    "Active Transits & Shifts:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                todayAndUpcomingTransits.take(3).forEach { event ->
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.Top) {
                            Icon(
                                Icons.Default.NotificationsActive, 
                                null, 
                                tint = if (event.intensity == "MAJOR") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp).padding(top = 2.dp).graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(event.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(event.description, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (event.intensity == "MAJOR") {
                            IconButton(
                                onClick = {
                                    exportToCalendar(
                                        context = context,
                                        title = "Astrology: ${event.name}",
                                        description = event.description
                                    )
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Event,
                                    contentDescription = "Export to Calendar",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
