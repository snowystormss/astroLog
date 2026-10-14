package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerRow(
    month: String,
    day: String,
    year: String,
    onMonthChange: (String) -> Unit,
    onDayChange: (String) -> Unit,
    onYearChange: (String) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Month Dropdown
        var monthExpanded by remember { mutableStateOf(false) }
        val monthNames = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        val displayMonth = month.toIntOrNull()?.let { if (it in 1..12) monthNames[it - 1] else month } ?: month

        ExposedDropdownMenuBox(
            expanded = monthExpanded && enabled,
            onExpandedChange = { if (enabled) monthExpanded = it },
            modifier = Modifier.weight(1.4f)
        ) {
            OutlinedTextField(
                value = displayMonth,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                maxLines = 1,
                label = { Text("Month", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthExpanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled),
                enabled = enabled
            )
            ExposedDropdownMenu(
                expanded = monthExpanded,
                onDismissRequest = { monthExpanded = false }
            ) {
                for (i in 1..12) {
                    val formatted = i.toString().padStart(2, '0')
                    DropdownMenuItem(
                        text = { Text(monthNames[i - 1]) },
                        onClick = {
                            onMonthChange(formatted)
                            monthExpanded = false
                        }
                    )
                }
            }
        }

        // Day Dropdown
        var dayExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = dayExpanded && enabled,
            onExpandedChange = { if (enabled) dayExpanded = it },
            modifier = Modifier.weight(0.8f)
        ) {
            OutlinedTextField(
                value = day,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                maxLines = 1,
                label = { Text("DD", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayExpanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled),
                enabled = enabled
            )
            ExposedDropdownMenu(
                expanded = dayExpanded,
                onDismissRequest = { dayExpanded = false }
            ) {
                for (i in 1..31) {
                    val formatted = i.toString().padStart(2, '0')
                    DropdownMenuItem(
                        text = { Text(formatted) },
                        onClick = {
                            onDayChange(formatted)
                            dayExpanded = false
                        }
                    )
                }
            }
        }

        // Year Text Field
        OutlinedTextField(
            value = year,
            onValueChange = { 
                if (it.length <= 4 && it.all { char -> char.isDigit() }) onYearChange(it) 
            },
            singleLine = true,
            maxLines = 1,
            label = { Text("YYYY", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
            modifier = Modifier.weight(1f),
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }
}
