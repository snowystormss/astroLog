package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
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
    val elementalBalance by viewModel.elementalBalance.collectAsStateWithLifecycle()
    val dispositorResult by viewModel.dispositorResult.collectAsStateWithLifecycle()
    val aspectDetails by viewModel.aspectDetails.collectAsStateWithLifecycle()
    val detailedPlanets by viewModel.detailedPlanets.collectAsStateWithLifecycle()
    val houseCusps by viewModel.houseCusps.collectAsStateWithLifecycle()

    val houseSystem = userSettings?.houseSystem ?: "WHOLE_SIGN"

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
                        val htmlContent = "<html><body><h1>Astrology Report</h1><p>$content</p></body></html>"
                        stream.write(htmlContent.toByteArray())
                    }
                    android.widget.Toast.makeText(context, "Saved report to Word doc", android.widget.Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Failed to export Word doc", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { sourceUri ->
            try {
                val inputStream = context.contentResolver.openInputStream(sourceUri)
                val file = java.io.File(context.filesDir, "avatar_${System.currentTimeMillis()}.jpg")
                val outputStream = java.io.FileOutputStream(file)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
                viewModel.updateAvatar(file.absolutePath)
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Failed to save avatar", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = profile.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        val birthInfo = if (profile.birthTime.startsWith("Unknown")) {
                            "${profile.birthDate} • ${profile.birthLocation}"
                        } else {
                            "${profile.birthDate} at ${profile.birthTime} • ${profile.birthLocation}"
                        }
                        Text(
                            text = birthInfo,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                        Text(
                            text = if (houseSystem.uppercase() == "PLACIDUS") "System: Placidus Houses" else "System: Whole Sign Houses",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { expanded = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("App Settings") },
                                onClick = {
                                    expanded = false
                                    onNavigateToSettings()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Backup & Sync") },
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
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            .clip(CircleShape)
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
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
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

            // 1. Natal Chart Wheel with House Cusps & Angles
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Natal Chart Wheel",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (houseSystem.uppercase() == "PLACIDUS") "Placidus" else "Whole Sign",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                com.example.ui.components.NatalChartWheel(
                    profile = profile,
                    houseSystem = houseSystem,
                    onPlanetClick = { planet, sign ->
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
                    }
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            // 2. Elemental & Modality Composition (Authoritative Textbook Scoring)
            if (elementalBalance != null) {
                item {
                    ElementalModalityCard(balance = elementalBalance!!)
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            // 3. Dispositor & Mutual Reception Analysis
            if (dispositorResult != null && (dispositorResult!!.mutualReceptions.isNotEmpty() || dispositorResult!!.finalDispositor != null)) {
                item {
                    DispositorCard(dispositor = dispositorResult!!)
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            // 4. Your Big Three
            item {
                Text(
                    "Your Big Three",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
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
                    PlanetCalculator.calculatePlanets(year, month, day, hour, profile.latitude, profile.longitude)
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

            // 5. Complete Natal Placements (with Houses & Essential Dignities)
            item {
                Text(
                    "Natal Chart Placements",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                PlanetPlacementsList(
                    profile = profile,
                    detailedPlanets = detailedPlanets
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            // 6. Major Natal Aspects & Applying/Separating Dynamics
            if (aspectDetails.isNotEmpty()) {
                item {
                    AspectsListCard(aspects = aspectDetails)
                    Spacer(modifier = Modifier.height(32.dp))
                }
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
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun ElementalModalityCard(balance: ElementalBalance) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Elemental & Modality Composition",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Grounded in Sakoian & Acker weighted celestial point analysis.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Elements Breakdown
            Text(
                text = "Triplicities (Elements)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ElementProgressChip(name = "Fire 🔥", pct = balance.fire, color = Color(0xFFE53935), modifier = Modifier.weight(1f))
                ElementProgressChip(name = "Earth 🌿", pct = balance.earth, color = Color(0xFF43A047), modifier = Modifier.weight(1f))
                ElementProgressChip(name = "Air 💨", pct = balance.air, color = Color(0xFF00ACC1), modifier = Modifier.weight(1f))
                ElementProgressChip(name = "Water 💧", pct = balance.water, color = Color(0xFF1E88E5), modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Modalities Breakdown
            Text(
                text = "Quadruplicities (Modalities)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModalityChip(name = "Cardinal ⚡", desc = "Initiating", pct = balance.cardinal, modifier = Modifier.weight(1f))
                ModalityChip(name = "Fixed 💎", desc = "Steadfast", pct = balance.fixed, modifier = Modifier.weight(1f))
                ModalityChip(name = "Mutable 🌀", desc = "Adaptable", pct = balance.mutable, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Polarities
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "Masculine (Yang/Action): ${balance.masculine}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Feminine (Yin/Receptive): ${balance.feminine}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = balance.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun ElementProgressChip(name: String, pct: Int, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "$pct%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = color)
        }
    }
}

@Composable
fun ModalityChip(name: String, desc: String, pct: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Text(text = "$pct%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(text = desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }
    }
}

@Composable
fun DispositorCard(dispositor: DispositorResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Dispositor Dynamics & Mutual Receptions",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (dispositor.finalDispositor != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "👑", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Final Dispositor: ${dispositor.finalDispositor} (rules all other planetary energies)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (dispositor.mutualReceptions.isNotEmpty()) {
                dispositor.mutualReceptions.forEach { (p1, p2) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🔄", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mutual Reception: $p1 ↔ $p2 (planets empower each other's expressions)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlanetPlacementsList(
    profile: UserProfile,
    partner: SavedProfile? = null,
    detailedPlanets: List<DetailedPlanetInfo> = emptyList()
) {
    val glyphMap = mapOf(
        "Sun" to "☉", "Moon" to "☽", "Mercury" to "☿", "Venus" to "♀",
        "Mars" to "♂", "Jupiter" to "♃", "Saturn" to "♄", "Uranus" to "♅",
        "Neptune" to "♆", "Pluto" to "♇", "North Node" to "☊", "South Node" to "☋",
        "Chiron" to "⚷", "Lilith" to "⚸", "Part of Fortune" to "⊕", "Ascendant" to "ASC", "Midheaven" to "MC"
    )

    if (detailedPlanets.isNotEmpty() && partner == null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                detailedPlanets.forEachIndexed { idx, item ->
                    val glyph = glyphMap[item.name] ?: ""
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1.3f)) {
                            Text(
                                text = glyph,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(28.dp)
                            )
                            Column {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "House ${item.house}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1.2f)) {
                            Text(
                                text = "${item.degree}°${item.minute}' ${item.sign}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            when (item.dignity) {
                                DignityType.DOMICILE -> Text("⭐ Domicile", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                                DignityType.EXALTATION -> Text("🔺 Exalted", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2196F3), fontWeight = FontWeight.Bold)
                                DignityType.DETRIMENT -> Text("⚠️ Detriment", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)
                                DignityType.FALL -> Text("🔻 Fall", style = MaterialTheme.typography.labelSmall, color = Color(0xFFF44336), fontWeight = FontWeight.Bold)
                                DignityType.PEREGRINE -> {}
                            }
                        }
                    }
                    if (idx < detailedPlanets.size - 1) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    }
                }
            }
        }
    } else {
        // Fallback or Partner view
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
                    Text(planetName, fontWeight = FontWeight.Bold, modifier = if (partner != null) Modifier.weight(1f) else Modifier)
                    if (partner != null) {
                        Text("${pos1.degree}° ${pos1.sign}", modifier = Modifier.weight(1.5f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        val pos2 = planets2?.get(planetName)
                        Text(if (pos2 != null) "${pos2.degree}° ${pos2.sign}" else "-", modifier = Modifier.weight(1.5f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                    } else {
                        Text("${pos1.degree}° ${pos1.sign}")
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            }
        }
    }
}

@Composable
fun AspectsListCard(aspects: List<AspectDetail>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Major Natal Aspects (Ptolemaic Orbs)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Applying aspects intensify throughout life; separating aspects reflect innate integration.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            val displayAspects = aspects.take(12)
            displayAspects.forEachIndexed { idx, a ->
                val aspectColor = when (a.aspectName) {
                    "Trine" -> Color(0xFF4CAF50)
                    "Sextile" -> Color(0xFF00BCD4)
                    "Square" -> Color(0xFFE91E63)
                    "Opposition" -> Color(0xFFF44336)
                    "Conjunction" -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text(
                            text = "${a.planet1} ${a.aspectName} ${a.planet2}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (a.isApplying) "Applying (intensifying)" else "Separating (integrating)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (a.isApplying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = aspectColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, aspectColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "${"%.1f".format(a.orb)}° orb",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = aspectColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (idx < displayAspects.size - 1) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                }
            }
        }
    }
}
