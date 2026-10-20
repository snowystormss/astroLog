package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AstrologyInsight
import com.example.data.AstrologyRepository
import com.example.data.UserProfile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onEach
import com.example.data.GenerateContentRequest
import com.example.data.Content
import com.example.data.Part
import com.example.data.ToolItem
import com.example.data.GenerationConfig
import com.example.BuildConfig
import com.example.data.RetrofitClient
import org.json.JSONObject
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.example.data.*
import kotlinx.coroutines.flow.combine

class MainViewModel(private val repository: AstrologyRepository) : ViewModel() {

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme

    fun setDarkTheme(isDark: Boolean) {
        if (_isDarkTheme.value != isDark) {
            _isDarkTheme.value = isDark
            com.example.ui.ThemeSoundEffect.playTransitionSound(isDark)
        }
    }

    private val _isProfileLoading = MutableStateFlow(true)
    val isProfileLoading: StateFlow<Boolean> = _isProfileLoading

    private val _lunarEventAlert = MutableStateFlow<String?>(null)
    val lunarEventAlert: StateFlow<String?> = _lunarEventAlert

    private val _elementalBalance = MutableStateFlow<ElementalBalance?>(null)
    val elementalBalance: StateFlow<ElementalBalance?> = _elementalBalance

    private val _houseCusps = MutableStateFlow<List<HouseCusp>>(emptyList())
    val houseCusps: StateFlow<List<HouseCusp>> = _houseCusps

    private val _detailedPlanets = MutableStateFlow<List<DetailedPlanetInfo>>(emptyList())
    val detailedPlanets: StateFlow<List<DetailedPlanetInfo>> = _detailedPlanets

    private val _dispositorResult = MutableStateFlow<DispositorResult?>(null)
    val dispositorResult: StateFlow<DispositorResult?> = _dispositorResult

    private val _chartAngles = MutableStateFlow<AnglesResult?>(null)
    val chartAngles: StateFlow<AnglesResult?> = _chartAngles

    private val _aspectDetails = MutableStateFlow<List<AspectDetail>>(emptyList())
    val aspectDetails: StateFlow<List<AspectDetail>> = _aspectDetails

    init {
        checkLunarEvents()
        viewModelScope.launch {
            combine(repository.userProfile, repository.userSettings) { prof, settings ->
                Pair(prof, settings?.houseSystem ?: "WHOLE_SIGN")
            }.collect { (prof, sys) ->
                if (prof != null) {
                    recalculateAstrologicalData(prof, sys)
                }
            }
        }
    }

    fun setHouseSystem(system: String) {
        val current = userSettings.value ?: com.example.data.UserSettings()
        updateUserSettings(current.copy(houseSystem = system))
    }

    fun recalculateAstrologicalData(profile: UserProfile, houseSystem: String = "WHOLE_SIGN") {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            val dateParts = profile.birthDate.split("/")
            val month = dateParts.getOrNull(0)?.toIntOrNull() ?: 1
            val day = dateParts.getOrNull(1)?.toIntOrNull() ?: 1
            val year = dateParts.getOrNull(2)?.toIntOrNull() ?: 2000

            val hour = if (!profile.birthTime.startsWith("Unknown")) {
                val timeParts = profile.birthTime.split(":")
                val h = timeParts.getOrNull(0)?.toDoubleOrNull() ?: 12.0
                val m = timeParts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
                h + (m / 60.0)
            } else {
                12.0
            }

            val planets = PlanetCalculator.calculatePlanets(year, month, day, hour, profile.latitude, profile.longitude)
            val angles = PlanetCalculator.calculateAngles(year, month, day, hour, profile.latitude, profile.longitude)
            _chartAngles.value = angles

            val cusps = PlanetCalculator.calculateHouseCusps(
                system = houseSystem,
                ascLon = angles.ascendant.longitude,
                mcLon = angles.midheaven.longitude,
                latitude = profile.latitude,
                longitude = profile.longitude,
                year = year,
                month = month,
                day = day,
                hour = hour
            )
            _houseCusps.value = cusps

            val elemBalance = PlanetCalculator.calculateElementalBalance(planets)
            _elementalBalance.value = elemBalance

            val dispositors = PlanetCalculator.calculateDispositors(planets)
            _dispositorResult.value = dispositors

            val aspects = PlanetCalculator.calculateAspectsDetailed(planets)
            _aspectDetails.value = aspects

            val detailedList = mutableListOf<DetailedPlanetInfo>()
            val trackedNames = listOf("Sun", "Moon", "Mercury", "Venus", "Mars", "Jupiter", "Saturn", "Uranus", "Neptune", "Pluto", "North Node", "South Node", "Chiron", "Lilith", "Part of Fortune")
            for (name in trackedNames) {
                val pos = planets[name] ?: continue
                val houseNum = PlanetCalculator.getPlanetHouse(pos.longitude, cusps, houseSystem)
                val dignity = PlanetCalculator.calculateDignity(name, pos.sign)
                detailedList.add(
                    DetailedPlanetInfo(
                        name = name,
                        sign = pos.sign,
                        degree = pos.degree,
                        minute = pos.minute,
                        longitude = pos.longitude,
                        house = houseNum,
                        dignity = dignity
                    )
                )
            }
            _detailedPlanets.value = detailedList
        }
    }

    private fun checkLunarEvents() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            val y = java.time.LocalDate.now().year
            val m = java.time.LocalDate.now().monthValue
            val d = java.time.LocalDate.now().dayOfMonth
            
            val planetsToday = PlanetCalculator.calculatePlanets(y, m, d)
            val planetsTomorrow = PlanetCalculator.calculatePlanets(
                java.time.LocalDate.now().plusDays(1).year,
                java.time.LocalDate.now().plusDays(1).monthValue,
                java.time.LocalDate.now().plusDays(1).dayOfMonth
            )
            
            val diffToday = (planetsToday["Moon"]!!.longitude - planetsToday["Sun"]!!.longitude + 360.0) % 360.0
            val diffTomorrow = (planetsTomorrow["Moon"]!!.longitude - planetsTomorrow["Sun"]!!.longitude + 360.0) % 360.0
            
            val isFullMoon = (diffToday < 180 && diffTomorrow >= 180) || (diffToday in 175.0..185.0)
            val isNewMoon = (diffToday in 350.0..360.0 || diffToday in 0.0..10.0)
            
            if (isFullMoon) {
                _lunarEventAlert.value = "Full Moon Alert!"
            } else if (isNewMoon) {
                _lunarEventAlert.value = "New Moon Alert!"
            }
        }
    }

    val userProfile: StateFlow<UserProfile?> = repository.userProfile
        .onEach { _isProfileLoading.value = false }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun createProfile(name: String, birthDate: String, birthTime: String, birthLocation: String, latitude: Double = 0.0, longitude: Double = 0.0) {
        viewModelScope.launch {
            val profile = UserProfile(
                name = name,
                birthDate = birthDate,
                birthTime = birthTime,
                birthLocation = birthLocation,
                latitude = latitude,
                longitude = longitude
            )
            repository.saveProfileLocally(profile)
            repository.clearInsights() // refresh for new user
            generateProfileInsights(profile)
            generateDailyHoroscopes(profile)
            generateDailyAffirmations(profile)
        }
    }

    fun resolveLocation(query: String, onResolved: (String, Double, Double) -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
                if (query.isBlank()) {
                    onError()
                    return@launch
                }

                // 1. Instant offline resolution from 50-state database
                val bestOfflineMatch = com.example.data.CitySearchService.findBestMatch(query)
                if (bestOfflineMatch != null) {
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onResolved(bestOfflineMatch.formatted, bestOfflineMatch.latitude, bestOfflineMatch.longitude)
                    }
                    return@launch
                }

                // 2. Online geocoding fallback for unincorporated rural areas or unlisted hamlets
                try {
                    val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "YOUR_GEMINI_API_KEY") {
                    throw IllegalStateException("API Key is missing")
                }

                val prompt = "Find the accurate latitude and longitude for the location: '$query'. Return ONLY a valid JSON object with keys 'formattedAddress' (string giving City, State, Country), 'latitude' (number), and 'longitude' (number). No markdown, no other text."
                val request = GenerateContentRequest(
                    contents = listOf(Content(role = "user", parts = listOf(Part(text = prompt)))),
                    tools = listOf(ToolItem(googleMaps = emptyMap()))
                )

                // Using Dispatchers.IO for networking safety
                val response = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    RetrofitClient.generateContentCached("gemini-3.1-flash-lite", apiKey, request)
                }
                val text = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (text != null) {
                    val jsonStr = text.removePrefix("```json").removeSuffix("```").trim()
                    val json = JSONObject(jsonStr)
                    val address = json.getString("formattedAddress")
                    val lat = json.getDouble("latitude")
                    val lon = json.getDouble("longitude")
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onResolved(address, lat, lon)
                    }
                } else {
                    throw IllegalStateException("Empty response from service")
                }
            } catch (e: Exception) {
                // Instantly fall back to stable deterministic generator
                try {
                    val cleanQuery = query.trim().split(Regex("\\s+")).joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
                    val hashValue = cleanQuery.hashCode()
                    val lat = ((hashValue % 18000).toDouble() / 100.0).coerceIn(-90.0, 90.0)
                    val lon = (((hashValue / 11) % 36000).toDouble() / 100.0).coerceIn(-180.0, 180.0)
                    val formattedAddress = if (cleanQuery.contains(",")) cleanQuery else "$cleanQuery, Earth"
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onResolved(formattedAddress, lat, lon)
                    }
                } catch (ex: Exception) {
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onError()
                    }
                }
            }
        }
    }

    fun updateAvatar(uri: String) {
        viewModelScope.launch {
            val currentProfile = userProfile.value
            if (currentProfile != null) {
                repository.saveProfileLocally(currentProfile.copy(avatarUri = uri))
            }
        }
    }

    fun updateProfile(name: String, birthDate: String, birthTime: String, birthLocation: String, latitude: Double = 0.0, longitude: Double = 0.0) {
        viewModelScope.launch {
            val profile = UserProfile(
                name = name,
                birthDate = birthDate,
                birthTime = birthTime,
                birthLocation = birthLocation,
                latitude = latitude,
                longitude = longitude
            )
            repository.saveProfileLocally(profile)
            repository.clearInsights() 
            generateProfileInsights(profile)
            generateDailyHoroscopes(profile)
            generateDailyAffirmations(profile)
        }
    }

    fun clearSavedReports() {
        viewModelScope.launch {
            repository.clearInsights()
            com.example.data.LocalStorageCache.clear()
            isGeneratingDailyHoroscopes = false
            isGeneratingDailyAffirmations = false
            generatingCompatibility.clear()
            userProfile.value?.let { profile ->
                generateProfileInsights(profile)
                generateDailyHoroscopes(profile)
                generateDailyAffirmations(profile)
            }
        }
    }

    val dailyFeed = repository.getLatestInsightByType("DAILY_FEED").stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)
    
    val dailyAffirmation = repository.getLatestInsightByType("DAILY_AFFIRMATION").stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val profileInsights = repository.getInsightsByTypes(listOf("SUMMARY", "CHILDHOOD", "YOUNG_ADULTHOOD", "FUTURE", "ASPECTS")).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    
    val allInsights = repository.insights.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val compatibilityInsight = repository.getLatestInsightByType("COMPATIBILITY").stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val savedProfiles = repository.savedProfiles.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    fun saveSavedProfile(id: Int = 0, name: String, birthDate: String, birthTime: String, birthLocation: String, relationshipType: String, latitude: Double = 0.0, longitude: Double = 0.0) {
        viewModelScope.launch {
            val prof = com.example.data.SavedProfile(id = id, name = name, birthDate = birthDate, birthTime = birthTime, birthLocation = birthLocation, relationshipType = relationshipType, latitude = latitude, longitude = longitude)
            repository.saveSavedProfileLocally(prof)
            if (id != 0) {
                repository.clearInsightByType("COMPATIBILITY_$id")
                repository.clearInsightByType("TIMINGS_$id")
            }
        }
    }

    fun deleteSavedProfile(id: Int) {
        viewModelScope.launch { repository.deleteSavedProfile(id) }
    }

    private val generatingCompatibility = mutableSetOf<Int>()

    fun loadOrGenerateCompatibility(profile: UserProfile, partner: com.example.data.SavedProfile, force: Boolean = false) {
        if (generatingCompatibility.contains(partner.id)) return
        generatingCompatibility.add(partner.id)
        viewModelScope.launch {
            val cachedComp = repository.getLatestInsightByType("COMPATIBILITY_${partner.id}").firstOrNull()
            val cachedTime = repository.getLatestInsightByType("TIMINGS_${partner.id}").firstOrNull()

            if (!force && cachedComp != null && cachedTime != null) {
                generatingCompatibility.remove(partner.id)
                return@launch // Already generated and cached!
            }

            repository.clearInsightByType("COMPATIBILITY_${partner.id}")
            repository.clearInsightByType("TIMINGS_${partner.id}")
                
                val p1Date = profile.birthDate.split("/")
            val p1Y = p1Date.getOrNull(2)?.toIntOrNull() ?: 2000
            val p2Date = partner.birthDate.split("/")
            val p2Y = p2Date.getOrNull(2)?.toIntOrNull() ?: 2000
            
            val p1Planets = PlanetCalculator.calculatePlanets(p1Y, p1Date.getOrNull(0)?.toIntOrNull() ?: 1, p1Date.getOrNull(1)?.toIntOrNull() ?: 1, 12.0, profile.latitude, profile.longitude)
            val p2Planets = PlanetCalculator.calculatePlanets(p2Y, p2Date.getOrNull(0)?.toIntOrNull() ?: 1, p2Date.getOrNull(1)?.toIntOrNull() ?: 1, 12.0, partner.latitude, partner.longitude)
            
            val p1Sun = p1Planets["Sun"]?.sign ?: "Aries"
            val p1Moon = p1Planets["Moon"]?.sign ?: "Aries"
            val p1Venus = p1Planets["Venus"]?.sign ?: "Aries"
            val p2Sun = p2Planets["Sun"]?.sign ?: "Aries"
            val p2Moon = p2Planets["Moon"]?.sign ?: "Aries"
            val p2Venus = p2Planets["Venus"]?.sign ?: "Aries"

            val compFallback = """
                TITLE: Cosmic Resonance
                SUMMARY: Elements of $p1Sun and $p2Sun form a highly specialized field of attraction.
                DEEP_DIVE: The interaction between a $p1Sun native and a $p2Sun native triggers a process of deep mutual reflection. In this relationship, your core goals and personalities meet in a mix of elementals. Whether forming a supportive trine, a dynamic square, or a magnetic opposition, this Sun-to-Sun placement acts as a constant engine of self-discovery and growth.
                |||
                TITLE: Emotional Reflection
                SUMMARY: Your inner worlds, guided by the $p1Moon and $p2Moon Moon, speak in intimate whispers.
                DEEP_DIVE: Your emotional security needs are governed by your lunar placements. With User 1 having a $p1Moon Moon and User 2 a $p2Moon Moon, your emotional languages are beautifully complex. You may express feelings through differing habits, but the shared spaces you create will allow you both to feel deeply seen, encouraging vulnerability and emotional maturity.
                |||
                TITLE: Venusian Alignment
                SUMMARY: The principles of attraction ($p1Venus and $p2Venus Venus) find deep, romantic alignment.
                DEEP_DIVE: Venus governs how you love, receive pleasure, and express affection. The link between a $p1Venus and $p2Venus placement results in an intriguing aesthetic and romantic symmetry. You express your care through complementary behaviors: while one may show devotion through practical acts of care, the other brings emotional depth, weaving a highly supportive tapestry of love.
                |||
                TITLE: Karmic Connection
                SUMMARY: Traditional lessons of compromise require patience and deep listening.
                DEEP_DIVE: Every powerful alliance carries its own signature of past-life connections and soul lessons. In your shared path, the primary lesson is to move past projection and learn authentic, non-defensive communication. Overcoming these trials is precisely what transforms this connection into a deeply anchored partnership.
                |||
                TITLE: Soul Bonding
                SUMMARY: A transcendent connection that deepens when navigating life's challenges together.
                DEEP_DIVE: At the core of the relationship lies a quiet, unshakeable space where you both match frequencies. This soul-bonding element ensures that even during times of physical distance or planetary storms, there is a subtle thread of cosmic understanding pulling you back together to evolve.
                |||
                TITLE: Future Horizon
                SUMMARY: The mutual trajectory leads toward shared creation and individual empowerment.
                DEEP_DIVE: As your partnership matures, your celestial charts indicate that the focus of your connection will shift from immediate passion to beautiful, collaborative creations. Together, you inspire each other to step into your highest individual potentials.
            """.trimIndent()

            val timingsFallback = """
                TITLE: Celestial Alignment
                DATE: Now - Nov 2026
                DEEP_DIVE: During this period, current planetary transits highlight mutual communication and shared values. You will find yourselves aligning on key personal objectives, allowing you to resolve past misunderstandings and anchor your emotional foundation. Outward circumstances are supportive of joint ventures.
                |||
                TITLE: Saturnine Reflection
                DATE: Dec 2026 - Mar 2027
                DEEP_DIVE: A period of consolidation and soul-searching. External delays might put some stress on immediate plans, but these circumstances are designed to test and strengthen the reliability of your bond. Focus on patience, building sturdy routines, and offering mutual reassurance as you navigate these constructive lessons.
                |||
                TITLE: Jovian Expansion
                DATE: Apr 2027 - Aug 2027
                DEEP_DIVE: Luck, optimism, and beautiful adventures characterize this phase. Under the expansive gaze of Jupiter, you are encouraged to step out of your comfort zones. Romantic trips, creative collaborations, or joint home upgrades are highly favored. It is a period of joyful bonding and laughter.
                |||
                TITLE: Retrograde Intimacy
                DATE: Sep 2027 - Dec 2027
                DEEP_DIVE: Major personal planets slow down, asking you to look backward and appreciate how far you have come together. Cozy domestic moments, revisiting favorite memory-filled spaces, and refining your shared vision will revitalize your bond. You are learning that the quietest moments are often the most profound.
            """.trimIndent()

            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "YOUR_GEMINI_API_KEY") {
                repository.saveInsightLocally("COMPATIBILITY_${partner.id}", compFallback)
                repository.saveInsightLocally("TIMINGS_${partner.id}", timingsFallback)
                return@launch
            }

            try {
                val prompt = """
                    Calculate an extremely detailed compatibility profile between User 1 (${profile.name}, Sun: $p1Sun, Moon: $p1Moon, Venus: $p1Venus) and User 2 (${partner.name}, Sun: $p2Sun, Moon: $p2Moon, Venus: $p2Venus).
                    The relationship type is: ${partner.relationshipType}.
                    Focus heavily on trials and tribulations, karmic connections, notable aspects, twin flame traits, and soul bonding elements.
                    Generate 5 to 7 major sections. For each section, use exactly this format, separated by '|||':
                    TITLE: [A short, intriguing 1-4 word title]
                    SUMMARY: [A profound one-sentence summary]
                    DEEP_DIVE: [Extremely detailed paragraphs explaining the connection, karma, and psychology]
                    Do not include any other text or markdown.
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                    generationConfig = GenerationConfig(temperature = 0.8f),
                    tools = listOf(ToolItem(googleSearch = emptyMap()))
                )
                val response = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    RetrofitClient.generateContentCached("gemini-3.1-flash-lite", apiKey, request)
                }
                val resultText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                if (!resultText.isNullOrEmpty()) {
                    repository.saveInsightLocally("COMPATIBILITY_${partner.id}", resultText)
                } else {
                    throw IllegalStateException("Empty compatibility response")
                }
            } catch (e: Exception) {
                repository.saveInsightLocally("COMPATIBILITY_${partner.id}", compFallback)
            }

            try {
                val timingsPrompt = """
                    Calculate detailed astrological timing cycles for User 1 (${profile.name}, Sun: $p1Sun, Moon: $p1Moon, Venus: $p1Venus) and User 2 (${partner.name}, Sun: $p2Sun, Moon: $p2Moon, Venus: $p2Venus).
                    Focus heavily on how their current and near-future life cycles intertwine. What is each person going through right now, how do they feel towards each other during these cycles, and what kind of moments are they creating together? The relationship type is: ${partner.relationshipType}.
                    Generate 3 to 5 detailed timing insights. For each insight, use exactly this format, separated by '|||':
                    TITLE: [A short, intriguing 1-4 word title for the timing period]
                    DATE: [A hypothetical date range, e.g., 'Now - Oct 2026' or 'Dec 2026 - Mar 2027']
                    DEEP_DIVE: [Extremely detailed paragraphs explaining the specific world around them, their external circumstances, their internal states, and how their timings intertwine.]
                    Do not include any other text or markdown.
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = timingsPrompt)))),
                    generationConfig = GenerationConfig(temperature = 0.8f),
                    tools = listOf(ToolItem(googleSearch = emptyMap()))
                )
                val response = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    RetrofitClient.generateContentCached("gemini-3.1-flash-lite", apiKey, request)
                }
                val resultText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                if (!resultText.isNullOrEmpty()) {
                    repository.saveInsightLocally("TIMINGS_${partner.id}", resultText)
                } else {
                    throw IllegalStateException("Empty timings response")
                }
            } catch (e: Exception) {
                repository.saveInsightLocally("TIMINGS_${partner.id}", timingsFallback)
            } finally {
                generatingCompatibility.remove(partner.id)
            }
        }
    }

    fun getTimingsInsightFor(partnerId: Int): StateFlow<AstrologyInsight?> {
        return repository.getLatestInsightByType("TIMINGS_$partnerId").stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)
    }

    fun getCompatibilityInsightFor(partnerId: Int): StateFlow<AstrologyInsight?> {
        return repository.getLatestInsightByType("COMPATIBILITY_$partnerId").stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)
    }

    val eventNotes: StateFlow<List<com.example.data.AstrologyEventNote>> = repository.allEventNotes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    val userSettings: StateFlow<com.example.data.UserSettings?> = repository.userSettings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun updateUserSettings(settings: com.example.data.UserSettings) {
        viewModelScope.launch {
            repository.saveUserSettings(settings)
        }
    }

    fun saveEventNote(dateStr: String, note: String) {
        viewModelScope.launch {
            repository.saveEventNote(dateStr, note)
        }
    }

    private var isGeneratingDailyHoroscopes = false
    private var isGeneratingDailyAffirmations = false

    fun checkAndGenerateHoroscope(profile: UserProfile) {
        viewModelScope.launch {
            if (isGeneratingDailyHoroscopes && isGeneratingDailyAffirmations) return@launch
            
            val feedInsight = repository.getLatestInsightByType("DAILY_FEED").firstOrNull()
            val affInsight = repository.getLatestInsightByType("DAILY_AFFIRMATION").firstOrNull()
            val msInDay = 24 * 60 * 60 * 1000L
            val now = System.currentTimeMillis()
            
            if (!isGeneratingDailyHoroscopes && (feedInsight == null || (now - feedInsight.createdAt) > msInDay)) {
                isGeneratingDailyHoroscopes = true
                generateDailyHoroscopes(profile)
            }
            if (!isGeneratingDailyAffirmations && (affInsight == null || (now - affInsight.createdAt) > msInDay)) {
                isGeneratingDailyAffirmations = true
                generateDailyAffirmations(profile)
            }
        }
    }

    private fun generateProfileInsights(profile: UserProfile) {
        val dateParts = profile.birthDate.split("/")
        val month = dateParts.getOrNull(0)?.toIntOrNull() ?: 1
        val day = dateParts.getOrNull(1)?.toIntOrNull() ?: 1
        val year = dateParts.getOrNull(2)?.toIntOrNull() ?: 2000
        
        var hour = 12.0
        var minute = 0.0
        if (!profile.birthTime.startsWith("Unknown")) {
            val timeParts = profile.birthTime.split(":")
            hour = timeParts.getOrNull(0)?.toDoubleOrNull() ?: 12.0
            minute = timeParts.getOrNull(1)?.filter { it.isDigit() || it == '.' }?.toDoubleOrNull() ?: 0.0
            val timeStr = profile.birthTime.lowercase()
            if (timeStr.contains("pm") && hour < 12) hour += 12
            if (timeStr.contains("am") && hour == 12.0) hour = 0.0
        }
        hour += minute / 60.0

        val planets = PlanetCalculator.calculatePlanets(year, month, day, hour, profile.latitude, profile.longitude)
        val sun = planets["Sun"]?.sign ?: "Aries"
        val moon = planets["Moon"]?.sign ?: "Aries"
        val mercury = planets["Mercury"]?.sign ?: "Aries"
        val venus = planets["Venus"]?.sign ?: "Aries"
        val mars = planets["Mars"]?.sign ?: "Aries"

        val summary = AstrologyInterpretation.getSummary(sun, moon)
        val childhood = AstrologyInterpretation.generateChildhood(moon)
        
        val youngAdult = "${AstrologyInterpretation.getPlanetSignText("Mercury", mercury)} ${AstrologyInterpretation.getPlanetSignText("Venus", venus)}"
        val future = AstrologyInterpretation.generateFuture(sun)
        
        val aspectsList = PlanetCalculator.calculateAspects(planets).take(5) // Get top 5 by orb
        val aspectsInfo = StringBuilder()
        aspectsInfo.append("Based on your planetary positions:\n\n")
        aspectsInfo.append("- ${AstrologyInterpretation.getPlanetSignText("Sun", sun)}\n\n")
        aspectsInfo.append("- ${AstrologyInterpretation.getPlanetSignText("Moon", moon)}\n\n")
        aspectsInfo.append("- ${AstrologyInterpretation.getPlanetSignText("Mars", mars)}\n\n")
        if (aspectsList.isNotEmpty()) {
            aspectsInfo.append("Major Aspects:\n")
            aspectsList.forEach { aspect ->
                aspectsInfo.append("- ${aspect.planet1} ${aspect.aspectName} ${aspect.planet2}\n")
            }
        }

        viewModelScope.launch {
            repository.saveInsightLocally("SUMMARY", summary)
            repository.saveInsightLocally("CHILDHOOD", childhood)
            repository.saveInsightLocally("YOUNG_ADULTHOOD", youngAdult)
            repository.saveInsightLocally("FUTURE", future)
            repository.saveInsightLocally("ASPECTS", aspectsInfo.toString())
        }
    }

    private fun generateDailyHoroscopes(profile: UserProfile) {
        val dateParts = profile.birthDate.split("/")
        val month = dateParts.getOrNull(0)?.toIntOrNull() ?: 1
        val day = dateParts.getOrNull(1)?.toIntOrNull() ?: 1
        val year = dateParts.getOrNull(2)?.toIntOrNull() ?: 2000
        val planets = PlanetCalculator.calculatePlanets(year, month, day, 12.0, profile.latitude, profile.longitude) // Approx
        
        val sun = planets["Sun"]?.sign ?: "Aries"
        val moon = planets["Moon"]?.sign ?: "Aries"
        val venus = planets["Venus"]?.sign ?: "Aries"
        val mars = planets["Mars"]?.sign ?: "Aries"

        viewModelScope.launch {
            val fallbackText = """
                TITLE: Solar Focus
                ADVICE: Honor your $sun nature with purposeful action today.
                DEEP_DIVE: Today, solar alignments elevate your personal confidence and call you to showcase your innate $sun capability. Take charge of your path, make vital decisions, and allow your radiant spirit to motivate the people around you.
                |||
                TITLE: Lunar Whispers
                ADVICE: Your emotional security lies in embracing your quiet $moon Moon.
                DEEP_DIVE: The emotional landscape is highly receptive today. With your Moon in $moon, you might absorb external energies more easily. Treat yourself with extra gentleness, set healthy psychological boundaries, and align with your brilliant intuition.
                |||
                TITLE: Aesthetic Pull
                ADVICE: Your $venus Venus invites beautiful connections and harmony.
                DEEP_DIVE: Romantic and aesthetic matters are deeply favored. Today is a magnificent time to express care through acts that match your $venus Venus nature. Connect with friends or romantic partners, appreciation of beauty is highlighted.
                |||
                TITLE: Vital Impulse
                ADVICE: Direct your internal $mars Mars drive with methodical focus.
                DEEP_DIVE: The fiery energy of Mars is ready to assist you. Whether tackling professional goals or physical activities, your drive is potent. Maintain clear, disciplined actions and see your tasks through to total success.
                |||
                TITLE: Cosmic Balance
                ADVICE: Breathe deeply and align your thoughts with universal timing.
                DEEP_DIVE: The transiting planets encourage balance across all aspects of your life. Pacing yourself allows you to tap into deep spiritual resources, transforming immediate trials into long-term wisdom.
            """.trimIndent()

            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "YOUR_GEMINI_API_KEY") {
                    throw IllegalStateException("API Key missing")
                }

                val todayStr = java.time.LocalDate.now().toString()
                val prompt = """
                    The user has Sun in $sun, Moon in $moon, Venus in $venus, and Mars in $mars (Birth Date: ${profile.birthDate}). 
                    Today's date is $todayStr.
                    First, use your Google Search tool to fetch the current, real-time planetary transits for $todayStr.
                    Second, compare these current real-time transits against the user's natal placements to suggest potential upcoming life events and shifts.
                    Generate between 4 to 8 profound, psychological and astrological insights for them today based on these transits. 
                    Keep the tone like 'The Pattern' or 'Co-Star' app: direct, psychological, slightly mystical, empathetic, not too conversational.
                    For each insight, provide:
                    1. A short title (max 4 words).
                    2. A short general advice sentence.
                    3. A multi-sentence 'dive deeper' explanation detailing what this transit means for the person, their environment, the people around them, and the world (suggest life events).
                    Format EACH insight strictly as follows, separating distinct insights with the string '|||':
                    TITLE: [title]
                    ADVICE: [advice]
                    DEEP_DIVE: [deep dive]
                    Do not include any other text or markdown.
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                    generationConfig = GenerationConfig(temperature = 0.9f),
                    tools = listOf(ToolItem(googleSearch = emptyMap()))
                )
                
                val response = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    RetrofitClient.generateContentCached("gemini-3.1-flash-lite", apiKey, request)
                }
                
                val feedText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                if (!feedText.isNullOrEmpty()) {
                    repository.saveInsightLocally("DAILY_FEED", feedText)
                } else {
                    throw IllegalStateException("Empty response from service")
                }
            } catch (e: Exception) {
                repository.saveInsightLocally("DAILY_FEED", fallbackText)
            } finally {
                isGeneratingDailyHoroscopes = false
            }
        }
    }

    private fun generateDailyAffirmations(profile: UserProfile) {
        val dateParts = profile.birthDate.split("/")
        val month = dateParts.getOrNull(0)?.toIntOrNull() ?: 1
        val day = dateParts.getOrNull(1)?.toIntOrNull() ?: 1
        val year = dateParts.getOrNull(2)?.toIntOrNull() ?: 2000
        val planets = PlanetCalculator.calculatePlanets(year, month, day, 12.0, profile.latitude, profile.longitude)
        
        val sun = planets["Sun"]?.sign ?: "Aries"
        val moon = planets["Moon"]?.sign ?: "Aries"
        val venus = planets["Venus"]?.sign ?: "Aries"
        val mars = planets["Mars"]?.sign ?: "Aries"

        viewModelScope.launch {
            val cal = java.util.Calendar.getInstance()
            val dayOfYear = cal.get(java.util.Calendar.DAY_OF_YEAR)
            val affirmations = listOf(
                "I am grounded in the cosmic wisdom of my $sun soul, moving with ease through today's transitions and welcoming abundant blessings.",
                "I align with the gentle strength of my $moon Moon, trusting my intuition and walking my path with quiet grace and deep peace.",
                "My heart is open to the beautiful abundance of the universe, attracting supportive connections and pure harmony.",
                "I possess the courage to overcome any challenge, channeling my $sun passion into creative, empowering actions.",
                "I release all doubts to the cosmic currents, recognizing that my journey is unfolding in perfect celestial timing.",
                "I am a magnificent expression of the stars, radiating confident light and lifting the energy of those around me."
            )
            val localAffirmation = affirmations[dayOfYear % affirmations.size]

            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "YOUR_GEMINI_API_KEY") {
                    throw IllegalStateException("API Key missing")
                }

                val upcomingEvents = PlanetCalculator.calculateUpcomingEvents(
                    cal.get(java.util.Calendar.YEAR),
                    cal.get(java.util.Calendar.MONTH) + 1,
                    cal.get(java.util.Calendar.DAY_OF_MONTH)
                ).filter { it.dateMillis - System.currentTimeMillis() < 5L * 24 * 60 * 60 * 1000 }

                val eventsSummary = upcomingEvents.joinToString("; ") { "${it.name}: ${it.description}" }
                val todayStr = java.time.LocalDate.now().toString()

                val prompt = """
                    You are an expert psychological astrologer. Generate a personalized daily cosmic affirmation (a single, powerful, positive quote of 1-3 sentences) for a user with these birth placements:
                    - Sun Sign: $sun (their most prominent natal sign)
                    - Moon Sign: $moon
                    - Venus Sign: $venus
                    - Mars Sign: $mars
                    
                    Recent/Upcoming Astrological Events affecting them/the collective:
                    ${eventsSummary.ifEmpty { "General cosmic alignments" }}
                    
                    Based on their natal chart as a whole, their prominent natal sign ($sun), and these active transit events for $todayStr, write a deeply inspiring, uplifting personal affirmation.
                    Rules:
                    1. The affirmation MUST be positive, direct, empowering, and deeply comforting or motivating.
                    2. It must be written in first person ("I am...", "I align with...", "My heart is...") as a daily affirmation.
                    3. Keep it relatively short (under 40 words) and incredibly encouraging.
                    4. Return ONLY the plain text quote, without any Markdown, quotes, or prefix intro sentences. Just the affirmation itself.
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                    generationConfig = GenerationConfig(temperature = 0.82f)
                )

                val response = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    RetrofitClient.generateContentCached("gemini-3.1-flash-lite", apiKey, request)
                }

                val quoteText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()?.removeSurrounding("\"")
                if (!quoteText.isNullOrEmpty()) {
                    repository.saveInsightLocally("DAILY_AFFIRMATION", quoteText)
                } else {
                    throw IllegalStateException("Empty response text")
                }
            } catch (e: Exception) {
                repository.saveInsightLocally("DAILY_AFFIRMATION", localAffirmation)
            } finally {
                isGeneratingDailyAffirmations = false
            }
        }
    }

    fun getPlanetDeepDive(planet: String, sign: String, profile: UserProfile, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val cacheKey = "PLANET_${planet}_${sign}"
            val cached = repository.getLatestInsightByType(cacheKey).firstOrNull()
            if (cached != null) {
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(cached.content)
                }
                return@launch
            }

            val pDesc = AstrologyInterpretation.getPlanetSignText(planet, sign)
            val fallbackDeepDive = """
                The planet $planet placed in the sign of $sign represents a key facet of your cosmic blueprint. Under current transits, this native energy is highly activated. It challenges you to integrate $sign's specific strengths into your daily routines and relationships.
                
                For someone born with this placement (specifically when reflecting your birth date of ${profile.birthDate}), this transit encourages deep self-awareness. Trust the process, release old patterns that no longer match this vibrant planetary frequency, and walk confidently toward your destiny.
            """.trimIndent()

            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "YOUR_GEMINI_API_KEY") {
                    throw IllegalStateException("API Key missing")
                }

                val prompt = """
                    You are an elite astrologer and psychotherapist. 
                    Write an extremely profound, deep, psychological and spiritual analysis of the planet $planet in the sign of $sign.
                    Natal charts coordinates for this individual are: Birth location coordinates (${profile.latitude}, ${profile.longitude}), Birth date ${profile.birthDate}.
                    The current native placement context is:
                    "$pDesc"
                    
                    Structure your analysis in 3 comprehensive, eloquent sections:
                    1. CORE PSYCHOLOGY: Detail what this placement means for their inner motivations, subconscious drivers, strengths, and hidden fears.
                    2. RELATIONAL IMPACT: Address how this affects their intimate bonds, communication styles, emotional projections, and relationship triggers.
                    3. COSMIC EVOLUTION & TRANSITS: Offer actionable guidance on how they can evolve past the shadow side of this placement and work with current transits to align with their soul's evolutionary destiny.
                    
                    Rules:
                    - Keep the tone incredibly intellectual, professional, objective, therapeutic, and deeply empathetic. (Avoid generic tabloid horoscope clichés).
                    - Use paragraphs with clean spacing.
                    - Do not mention or reference system instructions, AI, or specific prompt boundaries.
                    - Start directly with the analysis.
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                    generationConfig = GenerationConfig(temperature = 0.85f)
                )

                val response = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    RetrofitClient.generateContentCached("gemini-3.1-flash-lite", apiKey, request)
                }

                val deepDiveText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!deepDiveText.isNullOrEmpty()) {
                    val fullText = "$pDesc\n\n$deepDiveText"
                    repository.saveInsightLocally(cacheKey, fullText)
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onResult(fullText)
                    }
                } else {
                    throw IllegalStateException("No content generated")
                }
            } catch (e: Exception) {
                val fullFallbackText = "$pDesc\n\n$fallbackDeepDive"
                repository.saveInsightLocally(cacheKey, fullFallbackText)
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(fullFallbackText)
                }
            }
        }
    }

    fun generateAstrologyReportForExport(profile: UserProfile, insights: List<com.example.data.AstrologyInsight>, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val report = StringBuilder()
            report.append("========================================\n")
            report.append("      COSMIC ASTROLOGY REPORT           \n")
            report.append("========================================\n\n")
            report.append("Prepared for: ${profile.name}\n")
            report.append("Birth Date  : ${profile.birthDate}\n")
            report.append("Birth Time  : ${profile.birthTime}\n")
            report.append("Location    : ${profile.birthLocation}\n\n")
            report.append("----------------------------------------\n\n")
            
            insights.filter { it.type != "TEST" }.forEach { insight ->
                val title = when (insight.type) {
                    "SUMMARY" -> "Natal Synthesis"
                    "CHILDHOOD" -> "Early Foundations"
                    "YOUNG_ADULTHOOD" -> "Intellect & Desire"
                    "FUTURE" -> "Your Coming Cycle"
                    "ASPECTS" -> "Planetary Relationships"
                    "DAILY_FEED" -> "Daily Transits & Horoscopes"
                    "DAILY_AFFIRMATION" -> "Daily Cosmic Affirmation"
                    else -> insight.type.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
                }
                report.append("## $title\n")
                val contentCleaned = insight.content.replace("|||", "\n")
                report.append(contentCleaned)
                report.append("\n\n----------------------------------------\n\n")
            }
            report.append("May the stars guide your journey.")
            onResult(report.toString())
        }
    }
}
