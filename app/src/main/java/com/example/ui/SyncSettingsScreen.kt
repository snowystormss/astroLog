package com.example.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SyncSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE) }

    var syncPersonal by remember { mutableStateOf(sharedPrefs.getBoolean("sync_personal", true)) }
    var syncLunar by remember { mutableStateOf(sharedPrefs.getBoolean("sync_lunar", true)) }
    var syncTransits by remember { mutableStateOf(sharedPrefs.getBoolean("sync_transits", false)) }

    fun updatePref(key: String, value: Boolean) {
        sharedPrefs.edit().putBoolean(key, value).apply()
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.padding(end = 8.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go Back")
                }
                Text(
                    text = "Sync Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Manage which astrological events are automatically sent to your device's calendar.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(32.dp))
        }

        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Personal Timings", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Events mapped specifically from your birth chart, such as major profections or upcoming life chapters.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = syncPersonal,
                        onCheckedChange = { 
                            syncPersonal = it
                            updatePref("sync_personal", it)
                        }
                    )
                }
            }

            OutlinedCard(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Lunar Events", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("New Moons, Full Moons, and Eclipses that signify collective shifts and emotional tides.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = syncLunar,
                        onCheckedChange = { 
                            syncLunar = it
                            updatePref("sync_lunar", it)
                        }
                    )
                }
            }

            OutlinedCard(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Planetary Transits", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Major movements like Retrogrades, conjunctions, and ingresses of slower-moving planets.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = syncTransits,
                        onCheckedChange = { 
                            syncTransits = it
                            updatePref("sync_transits", it)
                        }
                    )
                }
            }
        }
    }
}
}
