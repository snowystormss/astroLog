package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.MilitaryTimePicker
import com.example.ui.components.DatePickerRow
import com.example.ui.components.CityAutocompleteField
import com.example.data.UserProfile

import com.example.ui.components.CosmicBackground

@Composable
fun OnboardingScreen(
    viewModel: MainViewModel,
    onProfileCreated: (String, String, String, String, Double, Double) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var birthMonth by rememberSaveable { mutableStateOf("") }
    var birthDay by rememberSaveable { mutableStateOf("") }
    var birthYear by rememberSaveable { mutableStateOf("") }
    var birthHour by rememberSaveable { mutableStateOf("") }
    var birthMinute by rememberSaveable { mutableStateOf("") }
    var locationQuery by rememberSaveable { mutableStateOf("") }
    var latitude by rememberSaveable { mutableDoubleStateOf(0.0) }
    var longitude by rememberSaveable { mutableDoubleStateOf(0.0) }
    var unknownTime by rememberSaveable { mutableStateOf(false) }
    var isResolvingLoc by remember { mutableStateOf(false) }

    val isFormValid = name.isNotBlank() && birthMonth.isNotBlank() && birthDay.isNotBlank() && birthYear.length == 4 && locationQuery.isNotBlank() && (unknownTime || (birthHour.isNotBlank() && birthMinute.isNotBlank()))

    CosmicBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Welcome to AstroLog",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Discover your cosmic blueprint.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge
            )
            
            Spacer(modifier = Modifier.height(48.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                maxLines = 1,
                label = { Text("Your Name", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
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
                verticalAlignment = Alignment.CenterVertically,
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

            Button(
                onClick = {
                    if (isFormValid && !isResolvingLoc) {
                        val birthDate = "$birthMonth/$birthDay/$birthYear"
                        val finalTime = if (unknownTime) "Unknown (12:00 PM)" else "$birthHour:$birthMinute"
                        
                        if (latitude != 0.0 || longitude != 0.0) {
                            onProfileCreated(name, birthDate, finalTime, locationQuery, latitude, longitude)
                        } else {
                            // Automatic background resolution so user never gets an incorrect 0,0 chart
                            isResolvingLoc = true
                            viewModel.resolveLocation(locationQuery, { resolvedAddr, lat, lon ->
                                isResolvingLoc = false
                                onProfileCreated(name, birthDate, finalTime, resolvedAddr, lat, lon)
                            }, {
                                isResolvingLoc = false
                                // Fallback with current query
                                onProfileCreated(name, birthDate, finalTime, locationQuery, latitude, longitude)
                            })
                        }
                    }
                },
                enabled = isFormValid && !isResolvingLoc,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                if (isResolvingLoc) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Aligning Celestial Coordinates...", fontSize = 15.sp, color = Color.White)
                } else {
                    Text("Reveal My Chart", fontSize = 16.sp, color = if (isFormValid) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
}
