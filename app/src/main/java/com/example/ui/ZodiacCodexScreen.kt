package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CosmicBackground

data class CodexItem(
    val title: String,
    val subtitle: String,
    val badge: String,
    val summary: String,
    val details: String,
    val tags: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZodiacCodexScreen(
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    val tabTitles = listOf("Signs", "Planets", "Houses", "Aspects")

    val signsData = remember { getZodiacSignsData() }
    val planetsData = remember { getPlanetsData() }
    val housesData = remember { getHousesData() }
    val aspectsData = remember { getAspectsData() }

    val currentItems = when (selectedTab) {
        0 -> signsData
        1 -> planetsData
        2 -> housesData
        else -> aspectsData
    }

    val filteredItems = remember(currentItems, searchQuery) {
        if (searchQuery.isBlank()) currentItems else {
            val q = searchQuery.trim().lowercase()
            currentItems.filter { item ->
                item.title.lowercase().contains(q) ||
                item.subtitle.lowercase().contains(q) ||
                item.badge.lowercase().contains(q) ||
                item.summary.lowercase().contains(q) ||
                item.details.lowercase().contains(q) ||
                item.tags.any { it.lowercase().contains(q) }
            }
        }
    }

    CosmicBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = "Zodiac Codex",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Ancient Fundamentals & Interpretations",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                placeholder = { Text("Search signs, houses, or planets...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                )
            )

            // Primary Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }

            // List of Items
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filteredItems.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No entries matching '$searchQuery'",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(filteredItems, key = { it.title + it.badge }) { item ->
                        CodexCard(item = item)
                    }
                }
            }
        }
    }
}

@Composable
fun CodexCard(item: CodexItem) {
    var expanded by remember { mutableStateOf(false) }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = item.badge,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = item.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        text = item.details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item.tags.forEach { tag ->
                            SuggestionChip(
                                onClick = {},
                                label = { Text(tag, fontSize = 11.sp) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                ),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (expanded) "Show less ▲" else "Tap to expand deep lore ▼",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

// -------------------------------------------------------------
// TEXTBOOK KNOWLEDGE BASES (Sakoian & Acker, Kris Brandt Riske)
// -------------------------------------------------------------

fun getZodiacSignsData(): List<CodexItem> = listOf(
    CodexItem(
        title = "Aries (The Ram)",
        subtitle = "March 21 – April 19 • Ruled by Mars",
        badge = "Cardinal Fire",
        summary = "Key Phrase: 'I am' • Keyword: Initiative. The first sign of the zodiac represents primal action, courage, pioneering drive, and self-assertion.",
        details = "Aries marks the vernal equinox—the start of spring. Driven by Mars, Arians act with decisive initiative, competitive vigor, and fearless leadership. The head and face are their biological rule.\n\n• Strengths: Bold leadership, high energy, confidence, trailblazing.\n• Shadow Side: Impulsiveness, combative temper, limited follow-through.\n• Decanates: 1st (0°-10° Mars/Aries), 2nd (11°-20° Sun/Leo), 3rd (21°-30° Jupiter/Sagittarius).\n• Polarity: Opposite Libra. Learns partnership, diplomacy, and cooperation.",
        tags = listOf("Fire", "Cardinal", "Mars", "Head", "Diamond")
    ),
    CodexItem(
        title = "Taurus (The Bull)",
        subtitle = "April 20 – May 20 • Ruled by Venus",
        badge = "Fixed Earth",
        summary = "Key Phrase: 'I have' • Keyword: Stability. The anchor of the earth signs embodies enduring patience, material security, sensory appreciation, and unwavering loyalty.",
        details = "Taurus represents the physical substance of existence. Venus bestows an eye for beauty, fine craft, and tactile comfort. Governs the throat, vocal cords, and neck.\n\n• Strengths: Steadfast reliability, practicality, artistic taste, financial acumen.\n• Shadow Side: Inflexibility, possessiveness, resistance to necessary change.\n• Decanates: 1st (0°-10° Venus/Taurus), 2nd (11°-20° Mercury/Virgo), 3rd (21°-30° Saturn/Capricorn).\n• Polarity: Opposite Scorpio. Learns emotional transformation and release.",
        tags = listOf("Earth", "Fixed", "Venus", "Throat", "Emerald")
    ),
    CodexItem(
        title = "Gemini (The Twins)",
        subtitle = "May 21 – June 20 • Ruled by Mercury",
        badge = "Mutable Air",
        summary = "Key Phrase: 'I think' • Keyword: Versatility. The intellectual explorer excels in communication, quick mental connections, curiosity, and rapid adaptation.",
        details = "Gemini is the neural network of the zodiac. Governed by messenger Mercury, Geminis process information at lightning speed, connecting diverse concepts, people, and media. Governs arms, hands, and lungs.\n\n• Strengths: Mental agility, wit, multilingual talent, curious explorer.\n• Shadow Side: Scattered focus, superficiality, nervous tension, gossiping.\n• Decanates: 1st (0°-10° Mercury/Gemini), 2nd (11°-20° Venus/Libra), 3rd (21°-30° Uranus/Aquarius).\n• Polarity: Opposite Sagittarius. Learns deep philosophical synthesis.",
        tags = listOf("Air", "Mutable", "Mercury", "Lungs", "Tourmaline")
    ),
    CodexItem(
        title = "Cancer (The Crab)",
        subtitle = "June 21 – July 22 • Ruled by the Moon",
        badge = "Cardinal Water",
        summary = "Key Phrase: 'I feel' • Keyword: Receptivity. The emotional protector of the zodiac rules home, roots, ancestral memory, and profound intuitive empathy.",
        details = "Cancer marks the summer solstice. Ruled by the Moon, Cancerians experience life through tides of emotion, creating secure sanctuaries for loved ones. Governs the stomach and breasts.\n\n• Strengths: Deep empathy, protective loyalty, domestic sanctuary, powerful intuition.\n• Shadow Side: Defensive retreats, mood swings, clutching the past, oversensitivity.\n• Decanates: 1st (0°-10° Moon/Cancer), 2nd (11°-20° Pluto/Scorpio), 3rd (21°-30° Neptune/Pisces).\n• Polarity: Opposite Capricorn. Learns emotional maturity and objective self-discipline.",
        tags = listOf("Water", "Cardinal", "Moon", "Stomach", "Pearl")
    ),
    CodexItem(
        title = "Leo (The Lion)",
        subtitle = "July 23 – August 22 • Ruled by the Sun",
        badge = "Fixed Fire",
        summary = "Key Phrase: 'I will' • Keyword: Illumination. The royal sovereign represents heart-centered creativity, dignity, magnetic warmth, and regal self-expression.",
        details = "Leo radiates the creative life force of our solar center. Fixed fire grants sustained loyalty, generosity, and dramatic showmanship. Governs the heart and spine.\n\n• Strengths: Magnanimity, radiant charisma, loyalty, inspirational leadership.\n• Shadow Side: Fragile pride, egocentricity, authoritarian dominance, drama.\n• Decanates: 1st (0°-10° Sun/Leo), 2nd (11°-20° Jupiter/Sagittarius), 3rd (21°-30° Mars/Aries).\n• Polarity: Opposite Aquarius. Learns egalitarian humility and collective collaboration.",
        tags = listOf("Fire", "Fixed", "Sun", "Heart", "Ruby")
    ),
    CodexItem(
        title = "Virgo (The Virgin)",
        subtitle = "August 23 – September 22 • Ruled by Mercury",
        badge = "Mutable Earth",
        summary = "Key Phrase: 'I analyze' • Keyword: Service. The meticulous craftsman of the zodiac brings order, analytical discrimination, purity, and practical devotion.",
        details = "Virgo is the harvest maiden holding wheat. Governed by Mercury's earth polarity, Virgos master detailed methodologies, health systems, and practical service. Governs the digestive system and nervous pathways.\n\n• Strengths: Methodical precision, discernment, devotion to helpfulness, problem-solving.\n• Shadow Side: Chronic worry, paralysis by analysis, excessive self-criticism.\n• Decanates: 1st (0°-10° Mercury/Virgo), 2nd (11°-20° Saturn/Capricorn), 3rd (21°-30° Venus/Taurus).\n• Polarity: Opposite Pisces. Learns spiritual surrender and universal faith.",
        tags = listOf("Earth", "Mutable", "Mercury", "Digestion", "Sapphire")
    ),
    CodexItem(
        title = "Libra (The Scales)",
        subtitle = "September 23 – October 22 • Ruled by Venus",
        badge = "Cardinal Air",
        summary = "Key Phrase: 'I balance' • Keyword: Harmony. The master of relationships embodies justice, diplomatic consensus, aesthetic refinement, and graceful partnership.",
        details = "Libra marks the autumnal equinox where day and night are in equal balance. Venus gives a quest for social beauty, arbitration, and moral fairness. Governs the kidneys and lower back.\n\n• Strengths: Diplomatic mediation, elegance, fairness, partnership-oriented grace.\n• Shadow Side: Indecision, people-pleasing at the cost of truth, conflict avoidance.\n• Decanates: 1st (0°-10° Venus/Libra), 2nd (11°-20° Uranus/Aquarius), 3rd (21°-30° Mercury/Gemini).\n• Polarity: Opposite Aries. Learns individual courage and self-reliance.",
        tags = listOf("Air", "Cardinal", "Venus", "Kidneys", "Opal")
    ),
    CodexItem(
        title = "Scorpio (The Scorpion)",
        subtitle = "October 23 – November 21 • Ruled by Pluto (Mars)",
        badge = "Fixed Water",
        summary = "Key Phrase: 'I desire' • Keyword: Intensity. The transformative alchemist commands emotional depth, unshakeable willpower, investigative acuity, and rebirth.",
        details = "Scorpio penetrates beneath all pretenses to uncover hidden truths. Dual-ruled by transformative Pluto and martial Mars, Scorpios possess extraordinary psychic endurance. Governs the reproductive and regenerative systems.\n\n• Strengths: Psychological penetration, unwavering loyalty, strategic brilliance, resilience.\n• Shadow Side: Suspicion, vindictiveness, power struggles, possessiveness.\n• Decanates: 1st (0°-10° Pluto/Scorpio), 2nd (11°-20° Neptune/Pisces), 3rd (21°-30° Moon/Cancer).\n• Polarity: Opposite Taurus. Learns grounded peace, gentleness, and non-attachment.",
        tags = listOf("Water", "Fixed", "Pluto", "Regeneration", "Topaz")
    ),
    CodexItem(
        title = "Sagittarius (The Archer)",
        subtitle = "November 22 – December 21 • Ruled by Jupiter",
        badge = "Mutable Fire",
        summary = "Key Phrase: 'I see' • Keyword: Expansion. The truth-seeking philosopher seeks limitless horizons through higher learning, optimism, travel, and wisdom.",
        details = "Represented by the Centaur releasing an arrow to the stars. Jupiter's expansive influence inspires broad religious, ethical, and metaphysical quests. Governs hips, thighs, and sciatic nerve.\n\n• Strengths: Optimistic vision, honesty, philosophical wisdom, love of liberty.\n• Shadow Side: Dogmatism, blunt tactlessness, restlessness, overpromising.\n• Decanates: 1st (0°-10° Jupiter/Sagittarius), 2nd (11°-20° Mars/Aries), 3rd (21°-30° Sun/Leo).\n• Polarity: Opposite Gemini. Learns grounded attention to factual nuance.",
        tags = listOf("Fire", "Mutable", "Jupiter", "Thighs", "Turquoise")
    ),
    CodexItem(
        title = "Capricorn (The Mountain Goat)",
        subtitle = "December 22 – January 19 • Ruled by Saturn",
        badge = "Cardinal Earth",
        summary = "Key Phrase: 'I use' • Keyword: Ambition. The master builder of institutions commands disciplined perseverance, worldly achievement, and enduring legacy.",
        details = "Capricorn marks the winter solstice. Governed by Saturn, Capricorns climb the summit through integrity, strategic patience, and responsibility. Governs bones, joints, and knees.\n\n• Strengths: Iron discipline, executive capability, dependability, long-term vision.\n• Shadow Side: Austerity, pessimism, emotional coldness, calculating materialism.\n• Decanates: 1st (0°-10° Saturn/Capricorn), 2nd (11°-20° Venus/Taurus), 3rd (21°-30° Mercury/Virgo).\n• Polarity: Opposite Cancer. Learns emotional warmth, family nurturing, and vulnerability.",
        tags = listOf("Earth", "Cardinal", "Saturn", "Bones", "Garnet")
    ),
    CodexItem(
        title = "Aquarius (The Water Bearer)",
        subtitle = "January 20 – February 18 • Ruled by Uranus (Saturn)",
        badge = "Fixed Air",
        summary = "Key Phrase: 'I know' • Keyword: Unconventional. The visionary reformer pours the waters of truth, scientific innovation, humanitarian ideals, and freedom.",
        details = "Aquarius stands outside convention to observe the collective future. Uranus brings sudden flashes of progressive insight, while traditional co-ruler Saturn anchors systemic logic. Governs ankles, shins, and circulation.\n\n• Strengths: Original intellect, humanitarian ideals, inventive vision, egalitarianism.\n• Shadow Side: Aloof detachment, eccentric stubbornness, emotional distance.\n• Decanates: 1st (0°-10° Uranus/Aquarius), 2nd (11°-20° Mercury/Gemini), 3rd (21°-30° Venus/Libra).\n• Polarity: Opposite Leo. Learns warm personal affection and heartfelt recognition.",
        tags = listOf("Air", "Fixed", "Uranus", "Ankles", "Amethyst")
    ),
    CodexItem(
        title = "Pisces (The Fishes)",
        subtitle = "February 19 – March 20 • Ruled by Neptune (Jupiter)",
        badge = "Mutable Water",
        summary = "Key Phrase: 'I believe' • Keyword: Compassion. The mystical culmination of the zodiac dissolves boundaries through universal love, imagination, and spiritual surrender.",
        details = "Two fish swimming in opposite directions represent the mortal self and divine consciousness. Ruled by Neptune, Pisces is deeply receptive to psychic currents and artistic inspiration. Governs feet and the lymphatic system.\n\n• Strengths: Boundless empathy, creative imagination, mystical intuition, selflessness.\n• Shadow Side: Escapism, boundary confusion, self-sacrificing martyrdom, vagueness.\n• Decanates: 1st (0°-10° Neptune/Pisces), 2nd (11°-20° Moon/Cancer), 3rd (21°-30° Pluto/Scorpio).\n• Polarity: Opposite Virgo. Learns grounding in daily practical discipline.",
        tags = listOf("Water", "Mutable", "Neptune", "Feet", "Aquamarine")
    )
)

fun getPlanetsData(): List<CodexItem> = listOf(
    CodexItem(
        title = "The Sun ☉",
        subtitle = "Core Identity & The Conscious Will",
        badge = "Luminary",
        summary = "The center of your personal solar system represents your conscious ego, vitality, self-expression, and life purpose.",
        details = "Rules Leo and the 5th House. In the birth chart, the Sun is your fuel: it indicates where you strive to shine, your internal motivation, and your basic character foundation.",
        tags = listOf("Sun", "Identity", "Vitality", "Leo")
    ),
    CodexItem(
        title = "The Moon ☽",
        subtitle = "Emotions, Instincts & Comfort Zone",
        badge = "Luminary",
        summary = "The reflective luminary governs unconscious reactions, daily habits, emotional security, family heritage, and the mother archetype.",
        details = "Rules Cancer and the 4th House. Your Moon sign reveals how you self-soothe, what makes you feel safe, your instinctive gut reactions, and your domestic nature.",
        tags = listOf("Moon", "Emotions", "Intuition", "Cancer")
    ),
    CodexItem(
        title = "Mercury ☿",
        subtitle = "The Intellect & Communication Style",
        badge = "Personal",
        summary = "The messenger god governs reasoning, problem-solving, speech, writing, information absorption, and short-distance travel.",
        details = "Rules Gemini (mental agility) and Virgo (practical precision). Mercury is the lens through which you analyze and articulate reality.",
        tags = listOf("Mercury", "Mind", "Speech", "Gemini", "Virgo")
    ),
    CodexItem(
        title = "Venus ♀",
        subtitle = "Love, Values, Beauty & Aesthetics",
        badge = "Personal",
        summary = "Known as the 'Lesser Benefic', Venus governs what you attract, your aesthetic tastes, partnership harmony, and personal spending habits.",
        details = "Rules Taurus (resources & sensuality) and Libra (social grace & balance). It shows how you express romantic affection and what you value most in companionship.",
        tags = listOf("Venus", "Love", "Money", "Taurus", "Libra")
    ),
    CodexItem(
        title = "Mars ♂",
        subtitle = "Energy, Ambition, Action & Desire",
        badge = "Personal",
        summary = "The red planet of drive and courage governs how you assert yourself, take initiative, confront obstacles, and channel passions.",
        details = "Rules Aries (initiative) and traditional ruler of Scorpio (endurance). Mars represents your fighting spirit and how you turn desires into concrete achievements.",
        tags = listOf("Mars", "Action", "Courage", "Aries", "Scorpio")
    ),
    CodexItem(
        title = "Jupiter ♃",
        subtitle = "Expansion, Fortune & Philosophy",
        badge = "Social",
        summary = "The 'Greater Benefic' governs abundance, higher education, spiritual wisdom, optimism, ethics, and fortunate opportunities.",
        details = "Rules Sagittarius and traditional ruler of Pisces. Jupiter expands whatever it touches, revealing where you experience luck and broad life perspectives.",
        tags = listOf("Jupiter", "Luck", "Expansion", "Sagittarius")
    ),
    CodexItem(
        title = "Saturn ♄",
        subtitle = "Karma, Discipline & Life Structure",
        badge = "Social",
        summary = "The great teacher and taskmaster governs maturity, boundaries, perseverance, career achievements, and karmic life lessons.",
        details = "Rules Capricorn and traditional ruler of Aquarius. The Saturn Return (every ~29.5 years) marks major milestones of adult responsibility and structural alignment.",
        tags = listOf("Saturn", "Discipline", "Karma", "Capricorn")
    ),
    CodexItem(
        title = "Uranus ♅",
        subtitle = "Awakening, Innovation & Rebellion",
        badge = "Generational",
        summary = "The higher octave of Mercury represents sudden epiphanies, breakthrough technologies, nonconformity, and individual freedom.",
        details = "Modern ruler of Aquarius. Uranus shatters outdated structures to awaken original thought and humanitarian progression.",
        tags = listOf("Uranus", "Freedom", "Insight", "Aquarius")
    ),
    CodexItem(
        title = "Neptune ♆",
        subtitle = "Dreams, Mysticism & Universal Compassion",
        badge = "Generational",
        summary = "The higher octave of Venus dissolves boundaries to reveal transcendent art, spirituality, psychic perception, and unconditional love.",
        details = "Modern ruler of Pisces. Inspires profound creative vision, while requiring discernment against illusion, escapism, and deception.",
        tags = listOf("Neptune", "Spirituality", "Art", "Pisces")
    ),
    CodexItem(
        title = "Pluto ♇",
        subtitle = "Transformation, Power & Rebirth",
        badge = "Generational",
        summary = "The god of the underworld represents the deepest cycles of psychological death, purification, unyielding willpower, and monumental rebirth.",
        details = "Modern ruler of Scorpio. Operates like cosmic pruning—eliminating obsolete facets of life so that authentic power emerges.",
        tags = listOf("Pluto", "Power", "Rebirth", "Scorpio")
    ),
    CodexItem(
        title = "North & South Nodes ☊ ☋",
        subtitle = "The Axis of Destiny & Karmic Heritage",
        badge = "Karmic Axis",
        summary = "The Moon's orbital nodes represent the soul's compass: the South Node is ingrained comfort, while the North Node is the path of evolutionary growth.",
        details = "Sakoian & Acker note that the North Node has a Jupiterian expansion influence (unfamiliar territory to master), while the South Node has a Saturnian connotation (mastered talents, but a potential rut).",
        tags = listOf("Nodes", "Destiny", "Karma", "Growth")
    ),
    CodexItem(
        title = "Chiron ⚷",
        subtitle = "The Wounded Healer",
        badge = "Centaur",
        summary = "Represents your deepest core vulnerability and the transcendent empathy, wisdom, and medicine you offer to others.",
        details = "Discovered in 1977 between Saturn and Uranus. Chiron acts as a bridge between worldly limitation and spiritual breakthrough, showing where pain becomes purpose.",
        tags = listOf("Chiron", "Healing", "Wisdom", "Compassion")
    )
)

fun getHousesData(): List<CodexItem> = listOf(
    CodexItem(
        title = "1st House (The Ascendant / East)",
        subtitle = "Identity, Selfhood, Physical Body",
        badge = "Angular • Trinity of Life",
        summary = "Natural Sign: Aries • Natural Ruler: Mars. Represents your immediate personal energy, appearance, early environment, and the lens through which you meet the world.",
        details = "The first house cusp is the Ascendant (Rising Sign). Planets here strongly color your personality, vitality, and primary approach to initiating action.",
        tags = listOf("Self", "Ascendant", "Life", "Aries")
    ),
    CodexItem(
        title = "2nd House",
        subtitle = "Personal Resources, Income, Values",
        badge = "Succedent • Trinity of Wealth",
        summary = "Natural Sign: Taurus • Natural Ruler: Venus. Governs earned income, possessions, personal financial security, self-worth, and tangible assets.",
        details = "Reveals how you make, manage, and spend money. Indicates your relationship with material comfort and what you value most in life.",
        tags = listOf("Money", "Assets", "Wealth", "Taurus")
    ),
    CodexItem(
        title = "3rd House",
        subtitle = "Communication, Siblings, Daily Mind",
        badge = "Cadent • Trinity of Association",
        summary = "Natural Sign: Gemini • Natural Ruler: Mercury. Governs verbal and written expression, immediate community, siblings, short trips, and early education.",
        details = "Shows your learning style, how your neural pathways organize thoughts, and daily communications with relatives and neighbors.",
        tags = listOf("Communication", "Mind", "Siblings", "Gemini")
    ),
    CodexItem(
        title = "4th House (Imum Coeli / North)",
        subtitle = "Home, Family, Roots, Ancestry",
        badge = "Angular • Trinity of Endings",
        summary = "Natural Sign: Cancer • Natural Ruler: Moon. The bottom of the chart represents your private sanctuary, psychological roots, parents, and late-life fulfillment.",
        details = "The 4th house cusp is the IC. It reflects childhood conditioning, home ownership, family heritage, and your deep emotional sanctuary.",
        tags = listOf("Home", "Family", "IC", "Cancer")
    ),
    CodexItem(
        title = "5th House",
        subtitle = "Creativity, Romance, Children, Joy",
        badge = "Succedent • Trinity of Life",
        summary = "Natural Sign: Leo • Natural Ruler: Sun. The arena of heart-centered joy, leisure, dating, artistic self-expression, sports, and offspring.",
        details = "Where you play and experience the thrill of romance. Governs creative brainchildren and speculative ventures.",
        tags = listOf("Creativity", "Love", "Children", "Leo")
    ),
    CodexItem(
        title = "6th House",
        subtitle = "Daily Work, Health, Service, Routine",
        badge = "Cadent • Trinity of Wealth",
        summary = "Natural Sign: Virgo • Natural Ruler: Mercury. Governs daily job tasks, coworker relationships, nutrition, hygiene, physical wellness, and pets.",
        details = "Represents how you refine your skills and maintain your physical body. Shows work ethic, craftsmanship, and service to others.",
        tags = listOf("Work", "Health", "Service", "Virgo")
    ),
    CodexItem(
        title = "7th House (The Descendant / West)",
        subtitle = "Committed Partnerships & Contracts",
        badge = "Angular • Trinity of Association",
        summary = "Natural Sign: Libra • Natural Ruler: Venus. Governs one-on-one relationships, marriage, business partners, open adversaries, and diplomatic mediation.",
        details = "The 7th house cusp is the Descendant. It reveals what you seek in a partner and how you handle cooperation, contracts, and mutual compromises.",
        tags = listOf("Partnership", "Marriage", "Descendant", "Libra")
    ),
    CodexItem(
        title = "8th House",
        subtitle = "Shared Resources, Sex, Transformation",
        badge = "Succedent • Trinity of Endings",
        summary = "Natural Sign: Scorpio • Natural Ruler: Pluto. Governs joint finances, partner's assets, loans, taxes, inheritance, deep intimacy, and rebirth.",
        details = "The polar opposite of the 2nd house. Involves psychological surrender, shared power, and transforming material resources.",
        tags = listOf("Transformation", "Joint Money", "Intimacy", "Scorpio")
    ),
    CodexItem(
        title = "9th House",
        subtitle = "Higher Learning, Philosophy, Travel",
        badge = "Cadent • Trinity of Life",
        summary = "Natural Sign: Sagittarius • Natural Ruler: Jupiter. The realm of the higher mind: university studies, religion, publishing, cross-cultural exploration, and ethics.",
        details = "Shows how you formulate your worldview, life philosophy, and search for truth. Governs long-distance voyages and international ties.",
        tags = listOf("Philosophy", "Travel", "Higher Mind", "Sagittarius")
    ),
    CodexItem(
        title = "10th House (Midheaven / South)",
        subtitle = "Career, Public Reputation, Legacy",
        badge = "Angular • Trinity of Wealth",
        summary = "Natural Sign: Capricorn • Natural Ruler: Saturn. The apex of the chart shows career achievements, worldly ambitions, status, authority, and public recognition.",
        details = "The 10th house cusp is the Midheaven (MC). It signifies your life calling, relationship to authority, and what you will be remembered for in society.",
        tags = listOf("Career", "Midheaven", "Legacy", "Capricorn")
    ),
    CodexItem(
        title = "11th House",
        subtitle = "Friendships, Groups, Hopes & Wishes",
        badge = "Succedent • Trinity of Association",
        summary = "Natural Sign: Aquarius • Natural Ruler: Uranus. Governs social networks, clubs, humanitarian movements, lifelong aspirations, and allies.",
        details = "Reveals how you participate in collective endeavors. Represents your future-oriented goals and the friends who support your growth.",
        tags = listOf("Friends", "Networks", "Vision", "Aquarius")
    ),
    CodexItem(
        title = "12th House",
        subtitle = "Subconscious, Karma, Seclusion, Renewal",
        badge = "Cadent • Trinity of Endings",
        summary = "Natural Sign: Pisces • Natural Ruler: Neptune. The mystical sanctuary of hidden strengths, spiritual dreams, karma, meditation, and inner retreat.",
        details = "Rules what operates behind the scenes, hospitals, retreats, and self-renewal. Where you release ego to connect with universal consciousness.",
        tags = listOf("Karma", "Subconscious", "Solitude", "Pisces")
    )
)

fun getAspectsData(): List<CodexItem> = listOf(
    CodexItem(
        title = "Conjunction (0°)",
        subtitle = "Orb: 8° – 10° • Keyword: Intensity & Fusion",
        badge = "Major Aspect",
        summary = "Two planets occupy the exact same degree. Their energies fuse completely into a single powerful dynamic.",
        details = "The most potent aspect in astrology. Can be harmonious (Venus conjunct Jupiter) or challenging (Mars conjunct Saturn) depending on the nature of the planets involved.",
        tags = listOf("0°", "Fusion", "Conjunction")
    ),
    CodexItem(
        title = "Sextile (60°)",
        subtitle = "Orb: 6° • Keyword: Opportunity & Growth",
        badge = "Major Aspect",
        summary = "Two signs apart (compatible elements like Fire/Air or Earth/Water). Represents mental flow, talent, and opportunities that require initiative.",
        details = "Unlike the trine, a sextile does not hand you benefits automatically—you must take active advantage of the doors it opens.",
        tags = listOf("60°", "Opportunity", "Sextile")
    ),
    CodexItem(
        title = "Square (90°)",
        subtitle = "Orb: 8° – 10° • Keyword: Challenge & Action",
        badge = "Major Aspect",
        summary = "Three signs apart (same modality like Cardinal/Cardinal). Creates constructive tension, obstacles to overcome, and dynamic achievement.",
        details = "Squares force character building. Kris Riske notes that squares provide the 90% perspiration that transforms raw potential into legendary worldly success.",
        tags = listOf("90°", "Friction", "Action", "Square")
    ),
    CodexItem(
        title = "Trine (120°)",
        subtitle = "Orb: 8° – 10° • Keyword: Ease & Harmonious Flow",
        badge = "Major Aspect",
        summary = "Four signs apart (same element: Fire/Fire, Water/Water). Bestows natural gifts, good fortune, and effortless cooperation.",
        details = "The energy flows smoothly without resistance. Sakoian & Acker caution that without squares or Saturnian discipline, trines can breed complacency.",
        tags = listOf("120°", "Harmonious", "Trine")
    ),
    CodexItem(
        title = "Opposition (180°)",
        subtitle = "Orb: 8° – 10° • Keyword: Polarity & Awareness",
        badge = "Major Aspect",
        summary = "Six signs apart directly across the zodiac. Highlights relationship dynamics, seeing oneself in others, and the search for balance.",
        details = "Often experienced through another person or external circumstance. The lesson is synthesis, cooperation, and meeting in the middle.",
        tags = listOf("180°", "Polarity", "Opposition")
    ),
    CodexItem(
        title = "Inconjunct / Quincunx (150°)",
        subtitle = "Orb: 2° – 3° • Keyword: Adjustment & Realignment",
        badge = "Minor Aspect",
        summary = "Five signs apart (unrelated elements and modalities, e.g. Aries and Virgo). Requires ongoing subtle adjustments.",
        details = "These two planetary energies share neither element nor modality. Creates an itch that must be consciously integrated through patience.",
        tags = listOf("150°", "Adjustment", "Quincunx")
    ),
    CodexItem(
        title = "Grand Trine Configuration",
        subtitle = "Pyramid of Flow in a Single Element",
        badge = "Major Pattern",
        summary = "Three planets mutually trining each other (120°), forming an equilateral triangle across all three signs of one element.",
        details = "Fire Grand Trines bring creative inspiration; Earth brings material prosperity; Air enhances intellectual brilliance; Water deepens psychic empathy.",
        tags = listOf("Configuration", "Grand Trine", "Talent")
    ),
    CodexItem(
        title = "T-Square Configuration",
        subtitle = "Dynamic Crucible of Achievement",
        badge = "Major Pattern",
        summary = "Two planets in opposition both squaring a third planet (the apex/focal planet), forming the letter 'T'.",
        details = "The apex planet is the release valve for immense energy. Famous high achievers and leaders often possess prominent T-Squares.",
        tags = listOf("Configuration", "T-Square", "Drive")
    ),
    CodexItem(
        title = "Yod ('The Finger of God')",
        subtitle = "Two Sextiles to an Apex Inconjunct",
        badge = "Karmic Pattern",
        summary = "Two planets in a harmonious 60° sextile, both forming 150° inconjuncts to a single apex planet.",
        details = "Points directly to a specific fateful mission. The apex planet holds the key to resolving deep karmic tension through specialized purpose.",
        tags = listOf("Configuration", "Yod", "Destiny")
    ),
    CodexItem(
        title = "Mutual Reception",
        subtitle = "Planetary Energetic Alliance",
        badge = "Harmonizer",
        summary = "When two planets each occupy the sign ruled by the other (e.g. Sun in Cancer and Moon in Leo, or Jupiter in Virgo and Mercury in Sagittarius).",
        details = "Sakoian & Acker emphasize that mutual reception functions like an honorary trine—the planets trade strength back and forth, softening even hard aspects.",
        tags = listOf("Dignity", "Mutual Reception", "Alliance")
    )
)
