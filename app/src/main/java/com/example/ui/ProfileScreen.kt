package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.UserProfile
import com.example.data.SavedProfile
import com.example.ui.components.HoroscopeCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    profile: UserProfile,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToSyncSettings: () -> Unit = {},
    onNavigateToCalendar: () -> Unit = {}
) {
    val insights by viewModel.profileInsights.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    var showDeepDive by remember { mutableStateOf(false) }
    var showPlanetDeepDive by remember { mutableStateOf(false) }
    
    var planetDeepDivePlanet by remember { mutableStateOf<String?>(null) }
    var planetDeepDiveText by remember { mutableStateOf<String?>(null) }
    var isLoadingDeepDive by remember { mutableStateOf(false) }

    var isExporting by remember { mutableStateOf(false) }
    var reportContentToExport by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val exportDocLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/msword")
    ) { uri ->
        uri?.let { destUri ->
            reportContentToExport?.let { content ->
                try {
                    context.contentResolver.openOutputStream(destUri)?.use { stream ->
                        stream.write(content.toByteArray())
                    }
                    android.widget.Toast.makeText(context, "Exported successfully!", android.widget.Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Export failed.", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { destUri ->
            try {
                val inputStream = context.contentResolver.openInputStream(destUri)
                if (inputStream != null) {
                    val file = java.io.File(context.filesDir, "avatar_${System.currentTimeMillis()}.jpg")
                    val outputStream = java.io.FileOutputStream(file)
                    inputStream.copyTo(outputStream)
                    inputStream.close()
                    outputStream.close()
                    viewModel.updateAvatar(file.absolutePath)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(context, "Failed to update avatar", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    com.example.ui.components.CosmicBackground(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(vertical = 32.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cosmic Profile",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                
                var expanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit / Recalculate Chart") },
                            onClick = {
                                expanded = false
                                onNavigateToSettings()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export to Google Doc") },
                            onClick = {
                                expanded = false
                                if (com.example.util.isNetworkAvailable(context)) {
                                    isExporting = true
                                    viewModel.generateAstrologyReportForExport(profile, insights) { report ->
                                        isExporting = false
                                        if (report.startsWith("Error") || report.startsWith("API Key")) {
                                            android.widget.Toast.makeText(context, report, android.widget.Toast.LENGTH_LONG).show()
                                        } else {
                                            reportContentToExport = report
                                            exportDocLauncher.launch("Astrology_Report_${profile.name}.doc")
                                        }
                                    }
                                } else {
                                    android.widget.Toast.makeText(context, "Please connect to Wi-Fi to export.", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sync Settings") },
                            onClick = {
                                expanded = false
                                onNavigateToSyncSettings()
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            
            // Avatar Section
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, androidx.compose.foundation.shape.CircleShape)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (profile.avatarUri != null) {
                        coil.compose.AsyncImage(
                            model = java.io.File(profile.avatarUri),
                            contentDescription = "Profile Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Add Avatar",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape)
                            .padding(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Avatar",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Text("Natal Chart", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            com.example.ui.components.NatalChartWheel(profile = profile, onPlanetClick = { planet, sign ->
                if (com.example.util.isNetworkAvailable(context)) {
                    planetDeepDivePlanet = planet
                    isLoadingDeepDive = true
                    showPlanetDeepDive = true
                    viewModel.getPlanetDeepDive(planet, sign, profile) { result ->
                        planetDeepDiveText = result
                        isLoadingDeepDive = false
                    }
                } else {
                    android.widget.Toast.makeText(context, "Please connect to Wi-Fi to load deep dive.", android.widget.Toast.LENGTH_SHORT).show()
                }
            })
            Spacer(modifier = Modifier.height(32.dp))
        }

        item {
            Text("Your Big Three", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))

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
                com.example.ui.PlanetCalculator.calculatePlanets(year, month, day, hour, profile.latitude, profile.longitude)
            }

            val sunSign = planets["Sun"]?.sign ?: "Unknown"
            val moonSign = planets["Moon"]?.sign ?: "Unknown"
            val risingSign = planets["Ascendant"]?.sign ?: "Unknown"

            fun getTraits(sign: String): String = when (sign) {
                "Aries" -> "Courageous, determined, confident, enthusiastic, optimistic, honest, passionate."
                "Taurus" -> "Reliable, patient, practical, devoted, responsible, stable."
                "Gemini" -> "Gentle, affectionate, curious, adaptable, ability to learn quickly and exchange ideas."
                "Cancer" -> "Tenacious, highly imaginative, loyal, emotional, sympathetic, persuasive."
                "Leo" -> "Creative, passionate, generous, warm-hearted, cheerful, humorous."
                "Virgo" -> "Loyal, analytical, kind, hardworking, practical."
                "Libra" -> "Cooperative, diplomatic, gracious, fair-minded, social."
                "Scorpio" -> "Resourceful, brave, passionate, stubborn, a true friend."
                "Sagittarius" -> "Generous, idealistic, great sense of humor."
                "Capricorn" -> "Responsible, disciplined, self-control, good managers."
                "Aquarius" -> "Progressive, original, independent, humanitarian."
                "Pisces" -> "Compassionate, artistic, intuitive, gentle, wise, musical."
                else -> "Mysterious and unknown traits."
            }

            com.example.ui.components.ZodiacConstellationCard(
                sign = sunSign,
                title = "Core Identity (Sun)",
                traits = getTraits(sunSign)
            )
            Spacer(modifier = Modifier.height(16.dp))

            com.example.ui.components.ZodiacConstellationCard(
                sign = moonSign,
                title = "Emotional Self (Moon)",
                traits = getTraits(moonSign)
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (!isUnknown) {
                com.example.ui.components.ZodiacConstellationCard(
                    sign = risingSign,
                    title = "Outer Persona (Rising)",
                    traits = getTraits(risingSign)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Text("Natal Chart Placements", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            PlanetPlacementsList(profile = profile)
            Spacer(modifier = Modifier.height(32.dp))
        }

        item {
            com.example.ui.components.PlanetaryWeatherCard(profile = profile, settings = userSettings)
            Spacer(modifier = Modifier.height(32.dp))
        }

        item {
            com.example.ui.components.CosmicTimeline(
                profile = profile,
                onViewCalendar = { onNavigateToCalendar() }
            )
            Spacer(modifier = Modifier.height(32.dp))
        }

        item {
            Button(
                onClick = { showDeepDive = true },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Deep Dive: Life Story")
            }
        }
    }

    if (showDeepDive) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showDeepDive = false },
            sheetState = sheetState
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 64.dp)
            ) {
                item {
                    Text("Potential Life Story", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(24.dp))
                }
                if (insights.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            com.example.ui.components.PremiumLoadingUI(
                                messages = listOf("Reading the akashic records...", "Synthesizing natal geometry...", "Tracing planetary nodes..."),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    items(insights) { insight ->
                        val modifierItem = Modifier
                        val icon = when (insight.type) {
                            "SUMMARY" -> Icons.Default.Star
                            "CHILDHOOD" -> Icons.Default.Face
                            "YOUNG_ADULTHOOD" -> Icons.Default.Person
                            "FUTURE" -> Icons.AutoMirrored.Filled.ArrowForward
                            "ASPECTS" -> Icons.Default.Favorite
                            else -> Icons.Default.Star
                        }
                        val title = when (insight.type) {
                            "SUMMARY" -> "Chart Summary"
                            "CHILDHOOD" -> "Childhood Roots"
                            "YOUNG_ADULTHOOD" -> "Young Adulthood"
                            "FUTURE" -> "Themes for the Future"
                            "ASPECTS" -> "Natal Chart Aspects"
                            else -> "Insight"
                        }
        
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val onExport: (() -> Unit)? = if (insight.type == "FUTURE") {
                            {
                                com.example.ui.exportToCalendar(context, "Astrological Future Timing", insight.content)
                            }
                        } else null
                        val onShare: (() -> Unit) = {
                            val sendIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, "$title\n\n${insight.content}\n\nShared from Cosmos")
                                type = "text/plain"
                            }
                            context.startActivity(android.content.Intent.createChooser(sendIntent, "Share Insight"))
                        }
                        HoroscopeCard(title = title, content = insight.content, icon = icon, modifier = modifierItem, onExport = onExport, onShare = onShare)
                        Spacer(modifier = modifierItem.height(16.dp))
                    }
                }
            }
        }
    }

        if (showPlanetDeepDive && planetDeepDivePlanet != null) {
            AlertDialog(
                onDismissRequest = { showPlanetDeepDive = false },
                title = { 
                    Text(text = "Cosmic Insight: $planetDeepDivePlanet", fontWeight = FontWeight.Bold) 
                },
                text = {
                    if (isLoadingDeepDive) {
                        Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                            com.example.ui.components.PremiumLoadingUI(
                                messages = listOf("Consulting the archives...", "Aligning celestial data...", "Reading deep cosmic history..."),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Text(text = planetDeepDiveText ?: "Unable to read stars.", style = MaterialTheme.typography.bodyMedium)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPlanetDeepDive = false }) {
                        Text("Close")
                    }
                }
            )
        }

        if (isExporting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun PlanetPlacementsList(profile: UserProfile, partner: SavedProfile? = null) {
    val dateParts1 = profile.birthDate.split("/")
    val month1 = dateParts1.getOrNull(0)?.toIntOrNull() ?: 1
    val day1 = dateParts1.getOrNull(1)?.toIntOrNull() ?: 1
    val year1 = dateParts1.getOrNull(2)?.toIntOrNull() ?: 2000

    val isUnknown1 = profile.birthTime.startsWith("Unknown")
    val hour1 = if (!isUnknown1) {
        val timeParts = profile.birthTime.split(":")
        val h = timeParts.getOrNull(0)?.toDoubleOrNull() ?: 12.0
        val m = timeParts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
        h + (m / 60.0)
    } else {
        12.0
    }
    
    val planets1 = remember(year1, month1, day1, hour1, profile.latitude, profile.longitude) {
        PlanetCalculator.calculatePlanets(year1, month1, day1, hour1, profile.latitude, profile.longitude)
    }

    var planets2: Map<String, PlanetCalculator.PlanetPosition>? = null
    if (partner != null) {
        val dateParts2 = partner.birthDate.split("/")
        val month2 = dateParts2.getOrNull(0)?.toIntOrNull() ?: 1
        val day2 = dateParts2.getOrNull(1)?.toIntOrNull() ?: 1
        val year2 = dateParts2.getOrNull(2)?.toIntOrNull() ?: 2000

        val isUnknown2 = partner.birthTime.startsWith("Unknown")
        val hour2 = if (!isUnknown2) {
            val timeParts = partner.birthTime.split(":")
            val h = timeParts.getOrNull(0)?.toDoubleOrNull() ?: 12.0
            val m = timeParts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
            h + (m / 60.0)
        } else {
            12.0
        }
        planets2 = remember(year2, month2, day2, hour2, partner.latitude, partner.longitude) {
            PlanetCalculator.calculatePlanets(year2, month2, day2, hour2, partner.latitude, partner.longitude)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (partner != null) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Planet", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(profile.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Text(partner.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
            HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
        }

        planets1.forEach { (planetName, pos1) ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(planetName, fontWeight = FontWeight.Bold, modifier=if (partner!=null) Modifier.weight(1f) else Modifier)
                if (partner != null) {
                    Text("${pos1.degree}° ${pos1.sign}", modifier = Modifier.weight(1.5f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    val pos2 = planets2?.get(planetName)
                    Text(if (pos2 != null) "${pos2.degree}° ${pos2.sign}" else "-", modifier = Modifier.weight(1.5f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                } else {
                    Text("${pos1.degree}° ${pos1.sign}")
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.1f))
        }
    }
}
