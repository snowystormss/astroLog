package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.example.data.CityLocation
import com.example.data.CitySearchService

@Composable
fun CityAutocompleteField(
    query: String,
    onQueryChange: (String) -> Unit,
    latitude: Double,
    longitude: Double,
    onLocationSelected: (CityLocation) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Birth City, State, Country",
    isResolvingOnline: Boolean = false,
    onTriggerOnlineSearch: (() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    var userDismissedSuggestions by remember { mutableStateOf(false) }

    val suggestions = remember(query) {
        if (query.trim().length >= 2) {
            CitySearchService.search(query, limit = 5)
        } else {
            emptyList()
        }
    }

    val showDropdown = isFocused && !userDismissedSuggestions && suggestions.isNotEmpty()
    val hasValidCoordinates = latitude != 0.0 || longitude != 0.0

    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    userDismissedSuggestions = false
                    onQueryChange(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                        if (focusState.isFocused) {
                            userDismissedSuggestions = false
                        }
                    },
                singleLine = true,
                maxLines = 1,
                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                placeholder = { Text("e.g. Los Angeles, Paris, Tokyo") },
                shape = RoundedCornerShape(12.dp),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = if (hasValidCoordinates) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (isResolvingOnline) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (query.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                onQueryChange("")
                                userDismissedSuggestions = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear location query",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = if (hasValidCoordinates) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                )
            )

            // Autocomplete Dropdown Popup
            DropdownMenu(
                expanded = showDropdown,
                onDismissRequest = { userDismissedSuggestions = true },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.98f))
                    .clip(RoundedCornerShape(14.dp)),
                properties = PopupProperties(focusable = false)
            ) {
                Text(
                    text = "Matching Cities",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                suggestions.forEach { city ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = city.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = city.displaySubtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = city.coordinatesDisplay,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            userDismissedSuggestions = true
                            onLocationSelected(city)
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    )
                }

                if (onTriggerOnlineSearch != null && query.trim().length >= 3) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp).padding(end = 6.dp)
                                )
                                Text(
                                    text = "Search exact online coordinates for '$query'",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        onClick = {
                            userDismissedSuggestions = true
                            onTriggerOnlineSearch()
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    )
                }
            }
        }

        // Selected coordinates pill badge
        AnimatedVisibility(
            visible = hasValidCoordinates,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            val latDir = if (latitude >= 0) "N" else "S"
            val lonDir = if (longitude >= 0) "E" else "W"
            val coordStr = "${"%.2f".format(kotlin.math.abs(latitude))}°$latDir, ${"%.2f".format(kotlin.math.abs(longitude))}°$lonDir"

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Verified Coordinates",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Exact Natal Coordinates: $coordStr",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}
