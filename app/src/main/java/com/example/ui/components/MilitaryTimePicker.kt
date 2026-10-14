package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MilitaryTimePicker(
    hour: String,
    minute: String,
    onHourChange: (String) -> Unit,
    onMinuteChange: (String) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Hour Dropdown
        var hourDropdownExpanded by remember { mutableStateOf(false) }
        
        val displayHour = remember(hour) {
            val h = hour.toIntOrNull() ?: 12
            val amPm = when {
                h == 0 -> "12AM"
                h == 12 -> "12PM"
                h > 12 -> "${h - 12}PM"
                else -> "${h}AM"
            }
            if (hour.isEmpty()) "" else "$hour ($amPm)"
        }

        ExposedDropdownMenuBox(
            expanded = hourDropdownExpanded && enabled,
            onExpandedChange = { if (enabled) hourDropdownExpanded = it },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = displayHour,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                maxLines = 1,
                label = { Text("Hour", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = hourDropdownExpanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled),
                enabled = enabled
            )
            ExposedDropdownMenu(
                expanded = hourDropdownExpanded,
                onDismissRequest = { hourDropdownExpanded = false }
            ) {
                for (i in 0..23) {
                    val formatted = i.toString().padStart(2, '0')
                    val amPm = when {
                        i == 0 -> "12AM"
                        i == 12 -> "12PM"
                        i > 12 -> "${i - 12}PM"
                        else -> "${i}AM"
                    }
                    DropdownMenuItem(
                        text = { Text("$formatted ($amPm)") },
                        onClick = {
                            onHourChange(formatted)
                            hourDropdownExpanded = false
                        }
                    )
                }
            }
        }

        // Minute Dropdown
        var minuteDropdownExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = minuteDropdownExpanded && enabled,
            onExpandedChange = { if (enabled) minuteDropdownExpanded = it },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = minute,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                maxLines = 1,
                label = { Text("Minute", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = minuteDropdownExpanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled),
                enabled = enabled
            )
            ExposedDropdownMenu(
                expanded = minuteDropdownExpanded,
                onDismissRequest = { minuteDropdownExpanded = false }
            ) {
                for (i in 0..59) {
                    val formatted = i.toString().padStart(2, '0')
                    DropdownMenuItem(
                        text = { Text(formatted) },
                        onClick = {
                            onMinuteChange(formatted)
                            minuteDropdownExpanded = false
                        }
                    )
                }
            }
        }
    }
}
