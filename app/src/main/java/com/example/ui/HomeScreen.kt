package com.example.ui

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.togetherWith
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserProfile

import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    profile: UserProfile
) {
    val dailyFeed by viewModel.dailyFeed.collectAsStateWithLifecycle()
    val dailyAffirmation by viewModel.dailyAffirmation.collectAsStateWithLifecycle()
    val allInsights by viewModel.allInsights.collectAsStateWithLifecycle()
    var showChat by remember { mutableStateOf(false) }
    val chatViewModel: ChatViewModel = viewModel()

    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val isOnlineFlow = remember { com.example.util.isNetworkAvailableFlow(context) }
    val isOnline by isOnlineFlow.collectAsState(
        initial = com.example.util.isNetworkAvailable(context)
    )

    LaunchedEffect(profile) {
        viewModel.checkAndGenerateHoroscope(profile)
    }

    val prominentSign = remember(profile) {
        val y = profile.birthDate.split("/").getOrNull(2)?.toIntOrNull() ?: 2000
        val m = profile.birthDate.split("/").getOrNull(0)?.toIntOrNull() ?: 1
        val d = profile.birthDate.split("/").getOrNull(1)?.toIntOrNull() ?: 1
        val planets = PlanetCalculator.calculatePlanets(y, m, d)
        planets["Sun"]?.sign ?: "Aries"
    }

    val rawFeedItems = remember(dailyFeed) {
        val rawInsights = dailyFeed?.content?.split("|||")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
        rawInsights.map { parseInsight(it) }
    }
    
    val upcomingEvents = remember {
        val cal = java.util.Calendar.getInstance()
        PlanetCalculator.calculateUpcomingEvents(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.DAY_OF_MONTH)).filter { it.dateMillis - System.currentTimeMillis() < 5L * 24 * 60 * 60 * 1000 }
    }
    
    val feedItems = remember(rawFeedItems, upcomingEvents) {
        val list = rawFeedItems.toMutableList()
        if (upcomingEvents.isNotEmpty()) {
             val eventSlides = upcomingEvents.map { event -> 
                 val diffDays = (event.dateMillis - System.currentTimeMillis()) / (24.0 * 60 * 60 * 1000)
                 val titlePrefix = if (diffDays < 1.5) "TODAY: " else "UPCOMING: "
                 InsightSlide(
                     title = titlePrefix + event.name, 
                     advice = event.description, 
                     deepDive = "This major astrological occurrence influences collective shifts, emotional tides, and cosmic potentials. Sync it to your calendar to stay aligned.",
                     timeMillis = event.dateMillis
                 )
             }
             list.addAll(0, eventSlides)
        }
        list
    }

    androidx.compose.animation.AnimatedContent(
        targetState = (dailyFeed == null),
        transitionSpec = {
            val enterSlow = androidx.compose.animation.core.spring<Float>(stiffness = androidx.compose.animation.core.Spring.StiffnessLow, dampingRatio = 0.85f)
            val exitSlow = androidx.compose.animation.core.spring<Float>(stiffness = androidx.compose.animation.core.Spring.StiffnessLow, dampingRatio = 0.85f)
            (androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(500)) + 
             androidx.compose.animation.scaleIn(initialScale = 0.95f, animationSpec = enterSlow))
            .togetherWith(
                androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(400)) + 
                androidx.compose.animation.scaleOut(targetScale = 1.05f, animationSpec = exitSlow)
            )
        },
        label = "HomeScreenLoadingTransition"
    ) { isLoading ->
        if (isLoading) {
            com.example.ui.components.CosmicBackground {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (!isOnline) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                            androidx.compose.material3.Icon(Icons.Default.Warning, contentDescription = "Offline", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Please connect to Wi-Fi", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("We need an internet connection to foresee your charts.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    } else {
                        com.example.ui.components.PremiumLoadingUI(
                            messages = listOf("Foreseeing charts...", "Consulting the stars...", "Analyzing alignments...", "Tracing cosmic geometries..."),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        } else {
            androidx.compose.animation.AnimatedContent(
                targetState = showChat,
                transitionSpec = {
                    val enter = androidx.compose.animation.core.spring<Float>(stiffness = androidx.compose.animation.core.Spring.StiffnessLow, dampingRatio = 0.8f)
                    val exit = androidx.compose.animation.core.spring<Float>(stiffness = androidx.compose.animation.core.Spring.StiffnessLow, dampingRatio = 0.8f)
                    
                    if (targetState) {
                        (androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) + 
                         androidx.compose.animation.scaleIn(initialScale = 0.92f, animationSpec = enter))
                        .togetherWith(
                            androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300)) + 
                            androidx.compose.animation.scaleOut(targetScale = 1.08f, animationSpec = exit)
                        )
                    } else {
                        (androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) + 
                         androidx.compose.animation.scaleIn(initialScale = 1.08f, animationSpec = enter))
                        .togetherWith(
                            androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300)) + 
                            androidx.compose.animation.scaleOut(targetScale = 0.92f, animationSpec = exit)
                        )
                    }
                },
                label = "FramerMotionTransition"
            ) { isChat ->
                if (isChat) {
                    BackHandler { showChat = false }
                    ChatScreen(chatViewModel = chatViewModel, profile = profile, insights = allInsights, settings = userSettings, onClose = { showChat = false })
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val lunarEventAlert by viewModel.lunarEventAlert.collectAsStateWithLifecycle()
                        
                        val pagerState = rememberPagerState(pageCount = { feedItems.size })
                        VerticalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                            SlidePage(
                                slide = feedItems[page],
                                pageIndex = page,
                                affirmationText = dailyAffirmation?.content,
                                prominentSign = prominentSign,
                                pageOffset = pageOffset
                            )
                        }
                        
                        if (lunarEventAlert != null) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 48.dp)
                                    .background(
                                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f),
                                        RoundedCornerShape(24.dp)
                                    )
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.tertiary,
                                        RoundedCornerShape(24.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = lunarEventAlert!!,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "fab_pulse")
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 0.95f,
                            targetValue = 1.05f,
                            animationSpec = androidx.compose.animation.core.infiniteRepeatable<Float>(
                                animation = androidx.compose.animation.core.tween(1500, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                            ),
                            label = "fab_pulse_scale"
                        )
                        
                        FloatingActionButton(
                            onClick = { showChat = true },
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(24.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Cosmic Chat", modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}

fun exportToCalendar(context: android.content.Context, title: String, description: String, timeMillis: Long? = null) {
    val intent = android.content.Intent(android.content.Intent.ACTION_INSERT).apply {
        data = android.provider.CalendarContract.Events.CONTENT_URI
        putExtra(android.provider.CalendarContract.Events.TITLE, title)
        putExtra(android.provider.CalendarContract.Events.DESCRIPTION, description)
        if (timeMillis != null) {
            putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, timeMillis)
            putExtra(android.provider.CalendarContract.EXTRA_EVENT_ALL_DAY, true)
        }
    }
    context.startActivity(intent)
}

data class InsightSlide(val title: String, val advice: String, val deepDive: String, val timeMillis: Long? = null)

fun parseInsight(block: String): InsightSlide {
    val cleanBlock = block.replace(Regex("\\*\\*"), "") // Remove markdown bolding
    val titleMatch = Regex("(?i)TITLE:\\s*(.+)").find(cleanBlock)
    val adviceMatch = Regex("(?i)ADVICE:\\s*(.+)").find(cleanBlock)
    val deepDiveMatch = Regex("(?i)DEEP_DIVE:\\s*([\\s\\S]+)").find(cleanBlock)

    var title = titleMatch?.groupValues?.get(1)?.trim() ?: "INSIGHT"
    var advice = adviceMatch?.groupValues?.get(1)?.trim() 
    
    var deepDive = deepDiveMatch?.groupValues?.get(1)?.trim() ?: cleanBlock

    if (advice == null) {
        val lines = cleanBlock.lines().map { it.trim() }.filter { it.isNotEmpty() }
        advice = lines.find { !it.uppercase().startsWith("TITLE:") && !it.uppercase().startsWith("DEEP_DIVE:") } ?: ""
    }

    title = title.replace(Regex("(?i)^(TITLE|ADVICE|DEEP_DIVE):\\s*"), "")
    advice = advice.replace(Regex("(?i)^(TITLE|ADVICE|DEEP_DIVE):\\s*"), "")
    deepDive = deepDive.replace(Regex("(?i)^(TITLE|ADVICE|DEEP_DIVE):\\s*"), "")

    return InsightSlide(title, advice, deepDive)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlidePage(slide: InsightSlide, pageIndex: Int, affirmationText: String?, prominentSign: String, pageOffset: Float) {
    val isDark = com.example.ui.theme.LocalIsDarkTheme.current
    val darkSlideColors = listOf(
        Color(0xFF0C0A09), // Black obsidian
        Color(0xFF16100D), // Deep cocoa black
        Color(0xFF1D140F), // Espresso brown
        Color(0xFF140D0A), // Black coffee
        Color(0xFF231710), // Rich dark brown
        Color(0xFF19110D)  // Deep chestnut black
    )
    val lightSlideColors = listOf(
        Color(0xFFFAF7F2), // Soft warm cream
        Color(0xFFFFFDF9), // Warm white
        Color(0xFFF7F1E6), // Almond cream
        Color(0xFFFCF9F4), // Pure ivory
        Color(0xFFF4ECE0), // Oat cream
        Color(0xFFFDFBF7)  // Pearl cream
    )
    val bgColor = if (isDark) darkSlideColors[pageIndex % darkSlideColors.size] else lightSlideColors[pageIndex % lightSlideColors.size]
    
    var showDeepDive by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val scale = 1f - (kotlin.math.abs(pageOffset) * 0.15f).coerceIn(0f, 1f)
    val alpha = 1f - (kotlin.math.abs(pageOffset) * 0.5f).coerceIn(0f, 1f)

    com.example.ui.components.CosmicBackground(
        modifier = Modifier.fillMaxSize(),
        customBackgroundColor = bgColor,
        parallaxOffset = pageOffset
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                    translationY = pageOffset * size.height * 0.2f
                }
        ) {
        if (pageIndex == 0 && !affirmationText.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                DailyAffirmationWidget(text = affirmationText, prominentSign = prominentSign)
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = slide.title.uppercase(),
                color = if (isDark) Color(0xFFD49B6A) else Color(0xFF7A4E2D),
                style = MaterialTheme.typography.labelLarge,
                letterSpacing = 3.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = slide.advice,
                color = if (isDark) Color(0xFFF7F0EB) else Color(0xFF281C16),
                style = MaterialTheme.typography.headlineMedium.copy(
                    lineHeight = 44.sp,
                    letterSpacing = (-0.5).sp
                ),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val context = androidx.compose.ui.platform.LocalContext.current
            Button(
                onClick = { showDeepDive = true },
                modifier = Modifier.height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color(0xFFF7F0EB).copy(alpha = 0.12f) else Color(0xFF281C16).copy(alpha = 0.08f),
                    contentColor = if (isDark) Color(0xFFF7F0EB) else Color(0xFF281C16)
                )
            ) {
                Text(
                    text = "DIVE DEEPER",
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            
            IconButton(
                onClick = { 
                    val sendIntent = android.content.Intent().apply {
                        action = android.content.Intent.ACTION_SEND
                        putExtra(android.content.Intent.EXTRA_TEXT, "${slide.title.uppercase()}\n\n${slide.advice}\n\nShared from Cosmos")
                        type = "text/plain"
                    }
                    context.startActivity(android.content.Intent.createChooser(sendIntent, "Share Insight"))
                },
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        if (isDark) Color(0xFFF7F0EB).copy(alpha = 0.12f) else Color(0xFF281C16).copy(alpha = 0.08f), 
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.Share, 
                    contentDescription = "Share", 
                    tint = if (isDark) Color(0xFFF7F0EB) else Color(0xFF281C16)
                )
            }
            
            if (slide.timeMillis != null || slide.title.contains("TIMING") || slide.title.contains("FUTURE")) {
                Button(
                    onClick = { exportToCalendar(context, slide.title, slide.advice, slide.timeMillis) },
                    modifier = Modifier.height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Default.Event, contentDescription = "Add to Calendar")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ADD TO CALENDAR",
                        style = MaterialTheme.typography.labelLarge,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
        }
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
                    .padding(horizontal = 32.dp, vertical = 16.dp)
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
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 32.sp
                    ),
                    color = if (isDark) Color(0xFFF7F0EB) else Color(0xFF281C16)
                )
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }
}

@Composable
fun DailyAffirmationWidget(text: String, prominentSign: String) {
    val isDark = com.example.ui.theme.LocalIsDarkTheme.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E1612).copy(alpha = 0.85f) else Color(0xFFFFFFFF).copy(alpha = 0.9f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDark) Color(0xFF5E4535).copy(alpha = 0.5f) else Color(0xFFE2D6C8)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(if (isDark) Color(0xFF382315) else Color(0xFFF3ECE2), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Affirmation Sparkle",
                    tint = if (isDark) Color(0xFFD49B6A) else Color(0xFF7A4E2D),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "COSMIC AFFIRMATION",
                        color = if (isDark) Color(0xFFC5B4A8) else Color(0xFF6B584B),
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "•",
                        color = if (isDark) Color(0xFF5E4535) else Color(0xFFE2D6C8),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = prominentSign.uppercase(),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "\"$text\"",
                    color = if (isDark) Color(0xFFF7F0EB) else Color(0xFF281C16),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 18.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                )
            }
        }
    }
}
