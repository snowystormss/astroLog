package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.MilitaryTimePicker
import com.example.ui.components.DatePickerRow
import com.example.ui.components.HoroscopeCard
import com.example.data.UserProfile
import com.example.data.SavedProfile
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextAlign
import com.example.ui.components.PremiumLoadingUI
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompatibilityScreen(
    viewModel: MainViewModel,
    profile: UserProfile
) {
    val savedProfiles by viewModel.savedProfiles.collectAsStateWithLifecycle()
    var isAddingNewProfile by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<SavedProfile?>(null) }
    var selectedProfile by remember { mutableStateOf<SavedProfile?>(null) }
    val compatibilityInsight by remember(selectedProfile) { 
        if (selectedProfile != null) viewModel.getCompatibilityInsightFor(selectedProfile!!.id) else null 
    }?.collectAsStateWithLifecycle(initialValue = null) ?: remember { mutableStateOf(null) }

    com.example.ui.components.CosmicBackground {
        androidx.compose.animation.AnimatedContent(
            targetState = when {
                isAddingNewProfile || editingProfile != null -> "ADD_EDIT"
                selectedProfile != null -> "DETAIL"
                else -> "LIST"
            },
            label = "CompatibilityScreenTransition"
        ) { state ->
            when (state) {
                "ADD_EDIT" -> {
                    AddNewPersonScreen(
                        viewModel = viewModel,
                        initialProfile = editingProfile,
                        onCancel = { 
                            isAddingNewProfile = false
                            editingProfile = null
                        },
                        onSave = { name, date, time, loc, type, lat, lon ->
                            viewModel.saveSavedProfile(
                                id = editingProfile?.id ?: 0,
                                name = name, 
                                birthDate = date, 
                                birthTime = time, 
                                birthLocation = loc, 
                                relationshipType = type,
                                latitude = lat,
                                longitude = lon
                            )
                            isAddingNewProfile = false
                            editingProfile = null
                        }
                    )
                }
                "DETAIL" -> {
                    CompatibilityDetailScreen(
                        viewModel = viewModel,
                        userProfile = profile,
                        partner = selectedProfile!!,
                        insightText = compatibilityInsight?.content,
                        onBack = { selectedProfile = null }
                    )
                }
                "LIST" -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        contentPadding = PaddingValues(vertical = 32.dp)
                    ) {
                        item {
                            Text(
                                text = "Compatibility",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold
                            )
                Spacer(modifier = Modifier.height(8.dp))
                if (savedProfiles.isEmpty()) {
                    Text(
                        text = "Foresee your compatibility. Add the people in your life.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            items(savedProfiles, key = { it.id }) { p ->
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { selectedProfile = p },
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    var menuExpanded by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(if (p.relationshipType == "Romantic") Icons.Default.Favorite else Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(p.name, fontWeight = FontWeight.Bold)
                            Text("${p.relationshipType} • ${p.birthDate}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Edit") },
                                    onClick = {
                                        menuExpanded = false
                                        editingProfile = p
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        menuExpanded = false
                                        viewModel.deleteSavedProfile(p.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { isAddingNewProfile = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Person")
                }
            }
        }
    }
}
        }
    }
}

data class CompatibilitySlide(val title: String, val summary: String, val deepDive: String)

fun parseCompatibility(block: String): CompatibilitySlide {
    val cleanBlock = block.replace(Regex("\\*\\*"), "") // Remove markdown bolding
    val titleMatch = Regex("(?i)TITLE:\\s*(.+)").find(cleanBlock)
    val summaryMatch = Regex("(?i)SUMMARY:\\s*(.+)").find(cleanBlock)
    val dateMatch = Regex("(?i)DATE:\\s*(.+)").find(cleanBlock)
    val deepDiveMatch = Regex("(?i)DEEP_DIVE:\\s*([\\s\\S]+)").find(cleanBlock)

    var title = titleMatch?.groupValues?.get(1)?.trim() ?: "CONNECTION"
    var summary = summaryMatch?.groupValues?.get(1)?.trim() 
        ?: dateMatch?.groupValues?.get(1)?.trim() 

    var deepDive = deepDiveMatch?.groupValues?.get(1)?.trim() ?: cleanBlock
    
    if (summary == null) {
        val lines = cleanBlock.lines().map { it.trim() }.filter { it.isNotEmpty() }
        summary = lines.find { !it.uppercase().startsWith("TITLE:") && !it.uppercase().startsWith("DEEP_DIVE:") } ?: ""
    }

    title = title.replace(Regex("(?i)^(TITLE|SUMMARY|DATE|DEEP_DIVE):\\s*"), "")
    summary = summary.replace(Regex("(?i)^(TITLE|SUMMARY|DATE|DEEP_DIVE):\\s*"), "")
    deepDive = deepDive.replace(Regex("(?i)^(TITLE|SUMMARY|DATE|DEEP_DIVE):\\s*"), "")

    return CompatibilitySlide(title, summary, deepDive)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompatibilityDetailScreen(
    viewModel: MainViewModel,
    userProfile: UserProfile,
    partner: SavedProfile,
    insightText: String?,
    onBack: () -> Unit
) {
    val timingsInsight by viewModel.getTimingsInsightFor(partner.id).collectAsStateWithLifecycle()
    var showDeepDive by remember { mutableStateOf(false) }

    LaunchedEffect(partner.id) {
        if (insightText == null) {
            viewModel.loadOrGenerateCompatibility(userProfile, partner)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("You × ${partner.name}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(partner.relationshipType, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.loadOrGenerateCompatibility(userProfile, partner, force = true) }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Recalculate", modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp))
                }
            }
        }
        
        androidx.compose.animation.Crossfade(
            targetState = insightText == null,
            label = "CompatibilityLoadingCrossfade"
        ) { isLoading ->
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    PremiumLoadingUI(
                        messages = listOf("Calculating cosmic bonds...", "Aligning destinies...", "Tracing karmic lines...", "Measuring twin flame signatures..."),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val dateParts1 = userProfile.birthDate.split("/")
                    val m1 = dateParts1.getOrNull(0)?.toIntOrNull() ?: 1
                    val d1 = dateParts1.getOrNull(1)?.toIntOrNull() ?: 1
                    val y1 = dateParts1.getOrNull(2)?.toIntOrNull() ?: 2000
                    val h1 = if (!userProfile.birthTime.startsWith("Unknown")) {
                        val tp = userProfile.birthTime.split(":")
                        (tp.getOrNull(0)?.toDoubleOrNull() ?: 12.0) + ((tp.getOrNull(1)?.toDoubleOrNull() ?: 0.0) / 60.0)
                    } else 12.0
                    val p1Planets = remember(y1, m1, d1, h1, userProfile.latitude, userProfile.longitude) {
                        PlanetCalculator.calculatePlanets(y1, m1, d1, h1, userProfile.latitude, userProfile.longitude)
                    }
                    val p1Sun = p1Planets["Sun"]?.sign ?: "Aries"

                    val dateParts2 = partner.birthDate.split("/")
                    val m2 = dateParts2.getOrNull(0)?.toIntOrNull() ?: 1
                    val d2 = dateParts2.getOrNull(1)?.toIntOrNull() ?: 1
                    val y2 = dateParts2.getOrNull(2)?.toIntOrNull() ?: 2000
                    val h2 = if (!partner.birthTime.startsWith("Unknown")) {
                        val tp = partner.birthTime.split(":")
                        (tp.getOrNull(0)?.toDoubleOrNull() ?: 12.0) + ((tp.getOrNull(1)?.toDoubleOrNull() ?: 0.0) / 60.0)
                    } else 12.0
                    val p2Planets = remember(y2, m2, d2, h2, partner.latitude, partner.longitude) {
                        PlanetCalculator.calculatePlanets(y2, m2, d2, h2, partner.latitude, partner.longitude)
                    }
                    val p2Sun = p2Planets["Sun"]?.sign ?: "Taurus"

                    val score = (p1Sun.hashCode() + p2Sun.hashCode()).absoluteValue % 4
                    val compatibilityLabel = if (partner.relationshipType == "Romantic") {
                        when(score) {
                            0 -> "Karmic"
                            1 -> "Soul-bound"
                            2 -> "Passionate"
                            else -> "Soulmates"
                        }
                    } else {
                        when(score) {
                            0 -> "Challenging"
                            1 -> "Good friends"
                            2 -> "Best friends"
                            else -> "Soulmates"
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = compatibilityLabel.uppercase(),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }

                item {
                    Text("TIMINGS", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start=24.dp, bottom=16.dp), color=MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 2.sp)
                    
                    androidx.compose.animation.Crossfade(
                        targetState = timingsInsight == null,
                        label = "TimingsLoadingCrossfade"
                    ) { isTimingsLoading ->
                        if (isTimingsLoading) {
                            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                PremiumLoadingUI(
                                    messages = listOf("Aligning chronologies...", "Reading current transits...", "Forecasting joint cycles..."),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            val tBlocks = timingsInsight!!.content.split("|||").map { it.trim() }.filter { it.isNotEmpty() }
                            val tSlides = tBlocks.map { parseCompatibility(it) }
                            
                            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                                tSlides.forEach { t ->
                                    TimingCardSection(t)
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }

                item {
                    Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                        Button(
                            onClick = { showDeepDive = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Text("Deep Dive: Potential Life Story")
                        }
                    }
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }

            if (showDeepDive && insightText != null) {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    onDismissRequest = { showDeepDive = false },
                    sheetState = sheetState
                ) {
                    val blocks = insightText.split("|||").map { it.trim() }.filter { it.isNotEmpty() }
                    val slides = blocks.map { parseCompatibility(it) }
                    val pagerState = rememberPagerState(pageCount = { slides.size })
                    
                    val isDark = com.example.ui.theme.LocalIsDarkTheme.current
                    val darkSlideColors = listOf(
                        Color(0xFF140E0B), Color(0xFF1F1510), Color(0xFF18110D),
                        Color(0xFF271A12), Color(0xFF1B130E), Color(0xFF221711)
                    )
                    val lightSlideColors = listOf(
                        Color(0xFFFFFFFF), Color(0xFFFAF7F2), Color(0xFFF5EFE6),
                        Color(0xFFFFFDF9), Color(0xFFF2EBE1), Color(0xFFFAF5ED)
                    )
                    val slideColors = if (isDark) darkSlideColors else lightSlideColors

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("THE BOND", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start=24.dp, bottom=16.dp), color=MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 2.sp)

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxWidth().height(480.dp),
                            contentPadding = PaddingValues(horizontal = 32.dp),
                            pageSpacing = 16.dp
                        ) { page ->
                            val bgColor = slideColors[page % slideColors.size]
                            CompatibilitySlideCard(slides[page], bgColor)
                        }
                        Spacer(modifier = Modifier.height(48.dp))
                    }
                }
            }
        }
        }
    }
}

@Composable
fun TimingCardSection(slide: CompatibilitySlide) {
    var isExpanded by remember { mutableStateOf(false) }
    
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded }.animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
            Text(slide.title.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(slide.summary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(slide.deepDive, style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text("TAP TO EXPAND", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompatibilitySlideCard(slide: CompatibilitySlide, bgColor: Color) {
    var showDeepDive by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDark = com.example.ui.theme.LocalIsDarkTheme.current

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .clickable { showDeepDive = true },
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = bgColor),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDark) Color(0xFF5E4535).copy(alpha = 0.4f) else Color(0xFFE2D6C8)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp)) {
            Column(
                modifier = Modifier.align(Alignment.Center).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = slide.title.uppercase(),
                    color = if (isDark) Color(0xFFD49B6A) else Color(0xFF7A4E2D),
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = slide.summary,
                    color = if (isDark) Color(0xFFF7F0EB) else Color(0xFF281C16),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        lineHeight = 36.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Text(
                text = "TAP TO DECODE",
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 2.sp,
                color = if (isDark) Color(0xFFC5B4A8) else Color(0xFF6B584B),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
            )
        }
    }

    if (showDeepDive) {
        ModalBottomSheet(
            onDismissRequest = { showDeepDive = false },
            sheetState = sheetState,
            containerColor = if (isDark) Color(0xFF18120F) else Color(0xFFFFFFFF),
            dragHandle = { BottomSheetDefaults.DragHandle(color = if (isDark) Color(0xFFD49B6A).copy(alpha = 0.4f) else Color(0xFF7A4E2D).copy(alpha = 0.3f)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = slide.title.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isDark) Color(0xFFD49B6A) else Color(0xFF7A4E2D),
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = slide.deepDive,
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 32.sp),
                    color = if (isDark) Color(0xFFF7F0EB) else Color(0xFF281C16)
                )
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }
}

@Composable
fun AddNewPersonScreen(viewModel: MainViewModel, initialProfile: SavedProfile?, onCancel: () -> Unit, onSave: (String, String, String, String, String, Double, Double) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initialProfile?.name ?: "") }
    
    val initialDateParts = initialProfile?.birthDate?.split("/")
    var month by rememberSaveable { mutableStateOf(initialDateParts?.getOrNull(0) ?: "") }
    var day by rememberSaveable { mutableStateOf(initialDateParts?.getOrNull(1) ?: "") }
    var year by rememberSaveable { mutableStateOf(initialDateParts?.getOrNull(2) ?: "") }
    
    val isUnknown = initialProfile?.birthTime?.startsWith("Unknown") == true
    val initialTimeParts = if (!isUnknown) initialProfile?.birthTime?.split(":") else null
    var hour by rememberSaveable { mutableStateOf(initialTimeParts?.getOrNull(0) ?: "") }
    var minute by rememberSaveable { mutableStateOf(initialTimeParts?.getOrNull(1) ?: "") }
    var unknownTime by rememberSaveable { mutableStateOf(isUnknown) }

    var locationQuery by rememberSaveable { mutableStateOf(initialProfile?.birthLocation ?: "") }
    var latitude by rememberSaveable { mutableStateOf(initialProfile?.latitude ?: 0.0) }
    var longitude by rememberSaveable { mutableStateOf(initialProfile?.longitude ?: 0.0) }
    var isResolvingLoc by remember { mutableStateOf(false) }
    var locError by remember { mutableStateOf(false) }
    
    var relationshipType by rememberSaveable { mutableStateOf(initialProfile?.relationshipType ?: "Friendship") }

    val isFormValid = name.isNotBlank() && month.isNotBlank() && day.isNotBlank() && year.length == 4 && locationQuery.isNotBlank() && (unknownTime || (hour.isNotBlank() && minute.isNotBlank()))

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(24.dp)
    ) {
        item {
            Button(onClick = onCancel, modifier = Modifier.height(36.dp)) { Text("Cancel") }
            Spacer(modifier = Modifier.height(24.dp))
            Text(if (initialProfile != null) "Edit Person" else "Add Person", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            
            OutlinedTextField(
                value = name, 
                onValueChange = { name = it }, 
                label = { Text("Name", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }, 
                singleLine = true,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            DatePickerRow(month = month, day = day, year = year, onMonthChange = { month = it }, onDayChange = { day = it }, onYearChange = { year = it })
            Spacer(modifier = Modifier.height(16.dp))

            MilitaryTimePicker(hour = hour, minute = minute, onHourChange = { hour = it }, onMinuteChange = { minute = it }, enabled = !unknownTime)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = unknownTime, onCheckedChange = { unknownTime = it })
                Text("I don't know their birth time")
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = locationQuery,
                onValueChange = { 
                    locationQuery = it
                    latitude = 0.0
                    longitude = 0.0
                    locError = false
                },
                singleLine = true,
                maxLines = 1,
                label = { Text("Birth City, State, Country", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            
            if (latitude != 0.0 || longitude != 0.0) {
                Text(
                    text = "Coordinates: ${"%.2f".format(latitude)}, ${"%.2f".format(longitude)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )
            } else {
                TextButton(
                    onClick = {
                        isResolvingLoc = true
                        locError = false
                        viewModel.resolveLocation(locationQuery, { formatted, lat, lon ->
                            locationQuery = formatted
                            latitude = lat
                            longitude = lon
                            isResolvingLoc = false
                        }, {
                            isResolvingLoc = false
                            locError = true
                        })
                    },
                    enabled = !isResolvingLoc && locationQuery.isNotBlank()
                ) {
                    if (isResolvingLoc) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Finding coordinates...")
                    } else {
                        Text("Verify Coordinates (Required)")
                    }
                }
                if (locError) {
                    Text(
                        text = "Unable to find. Try a larger nearby city.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            Text("Relationship Type")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FilterChip(
                    selected = relationshipType == "Friendship",
                    onClick = { relationshipType = "Friendship" },
                    label = { Text("Friendship") }
                )
                FilterChip(
                    selected = relationshipType == "Romantic",
                    onClick = { relationshipType = "Romantic" },
                    label = { Text("Romantic") }
                )
            }
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (isFormValid) {
                        val birthDate = "$month/$day/$year"
                        val finalTime = if (unknownTime) "Unknown (12:00 PM)" else "$hour:$minute"
                        onSave(name, birthDate, finalTime, locationQuery, relationshipType, latitude, longitude)
                    }
                },
                enabled = isFormValid,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Save & Calculate")
            }
        }
    }
}

