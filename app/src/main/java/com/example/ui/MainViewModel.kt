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
import com.example.BuildConfig
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
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onError()
                }
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

            // 2. Do not fabricate coordinates with hashCode(). Report error so user can pick a verified city.
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                onError()
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
                
            val (p1M, p1D, p1Y) = parseDate(profile.birthDate)
            val p1Hour = parseTime(profile.birthTime)
            val (p2M, p2D, p2Y) = parseDate(partner.birthDate)
            val p2Hour = parseTime(partner.birthTime)

            val p1Planets = PlanetCalculator.calculatePlanets(p1Y, p1M, p1D, p1Hour, profile.latitude, profile.longitude)
            val p2Planets = PlanetCalculator.calculatePlanets(p2Y, p2M, p2D, p2Hour, partner.latitude, partner.longitude)

            val p1Name = profile.name.ifBlank { "You" }
            val p2Name = partner.name.ifBlank { "Partner" }

            val p1Sun = p1Planets["Sun"]?.sign ?: "Aries"
            val p1Moon = p1Planets["Moon"]?.sign ?: "Aries"
            val p1Venus = p1Planets["Venus"]?.sign ?: "Aries"
            val p1Mars = p1Planets["Mars"]?.sign ?: "Aries"
            val p1Mercury = p1Planets["Mercury"]?.sign ?: "Aries"

            val p2Sun = p2Planets["Sun"]?.sign ?: "Aries"
            val p2Moon = p2Planets["Moon"]?.sign ?: "Aries"
            val p2Venus = p2Planets["Venus"]?.sign ?: "Aries"
            val p2Mars = p2Planets["Mars"]?.sign ?: "Aries"
            val p2Mercury = p2Planets["Mercury"]?.sign ?: "Aries"

            val synastryAspects = PlanetCalculator.calculateSynastry(p1Planets, p2Planets)
            val (synastryLabel, synastryScore) = PlanetCalculator.calculateSynastryCompatibility(
                p1Planets, p2Planets, partner.relationshipType == "Romantic"
            )

            val primaryAspectStr = synastryAspects.firstOrNull()?.let {
                "${it.planet1} ${it.aspectName} ${it.planet2} (orb ${"%.1f".format(it.orb)}°)"
            } ?: "Solar & Lunar Symmetry"

            val compFallback = """
                TITLE: Cosmic Resonance
                SUMMARY: $p1Name ($p1Sun) and $p2Name ($p2Sun) — $synastryLabel bond ($synastryScore% cosmic affinity).
                DEEP_DIVE: The interaction between a $p1Sun Sun and a $p2Sun Sun establishes a powerful energetic circuit. With $primaryAspectStr actively influencing your charts, your core life paths reflect and challenge each other. The elemental relationship between your Suns fosters mutual growth, encouraging both individuals to anchor their authentic sovereignty while building a shared foundation.
                |||
                TITLE: Emotional Reflection
                SUMMARY: Lunar sanctuary between $p1Name's $p1Moon Moon and $p2Name's $p2Moon Moon.
                DEEP_DIVE: Emotional intimacy is guided by your lunar placements. $p1Name navigates emotional needs through the lens of $p1Moon, while $p2Name processes through $p2Moon. By honoring each other's instinctual rhythms and creating non-judgmental space, your inner worlds weave a deeply healing psychological sanctuary.
                |||
                TITLE: Venusian Alignment
                SUMMARY: Romantic attraction, affection, and values ($p1Venus Venus & $p2Venus Venus).
                DEEP_DIVE: In romance and aesthetics, $p1Name expresses love through $p1Venus while $p2Name receives and values love through $p2Venus. Combined with Mars in $p1Mars and $p2Mars, your romantic chemistry contains both tender appreciation and stimulating creative passion.
                |||
                TITLE: Mental Synchronicity
                SUMMARY: Communication matrix between Mercury in $p1Mercury and Mercury in $p2Mercury.
                DEEP_DIVE: How you share ideas, resolve debates, and coordinate daily logistics is rooted in your Mercury placements. Mercury in $p1Mercury meets Mercury in $p2Mercury, ensuring that curiosity, humor, and active listening become bridges rather than barriers.
                |||
                TITLE: Karmic Connection
                SUMMARY: Saturnian lessons, structural loyalty, and mutual soul evolution.
                DEEP_DIVE: Every enduring connection carries karmic responsibilities. Celestial ties between Saturn and the lunar nodes highlight that your meeting is not coincidental. Through patience, accountability, and compassionate boundaries, you transform immediate friction into unwavering mutual loyalty.
                |||
                TITLE: Future Horizon
                SUMMARY: The mutual trajectory leads toward shared creation and sovereign empowerment.
                DEEP_DIVE: As your dynamic deepens, celestial geometry indicates that the focus of your connection matures from immediate attraction into collaborative creation. Together, you inspire each other to step into your highest potentials.
            """.trimIndent()

            val now = java.time.LocalDate.now()
            val quarters = listOf(
                Pair(now, now.plusMonths(3)),
                Pair(now.plusMonths(3), now.plusMonths(6)),
                Pair(now.plusMonths(6), now.plusMonths(9)),
                Pair(now.plusMonths(9), now.plusMonths(12))
            )

            val timingTitles = listOf(
                "Celestial Alignment & Foundation",
                "Saturnine Growth & Realignment",
                "Jovian Expansion & Abundance",
                "Deep Planetary Synthesis"
            )

            val timingsFallback = quarters.mapIndexed { idx, (start, end) ->
                val midDate = start.plusDays(45)
                val midTransits = PlanetCalculator.calculatePlanets(midDate.year, midDate.monthValue, midDate.dayOfMonth, 12.0)
                val tJup = midTransits["Jupiter"]?.sign ?: "Gemini"
                val tSat = midTransits["Saturn"]?.sign ?: "Pisces"
                val tMars = midTransits["Mars"]?.sign ?: "Leo"
                val dateLabel = "${start.month.name.take(3)} ${start.year} — ${end.month.name.take(3)} ${end.year}"
                val title = timingTitles[idx]
                val deepDive = when (idx) {
                    0 -> "During this current quarter, transiting Jupiter in $tJup and Saturn in $tSat activate key placements between $p1Name and $p2Name. Outward opportunities encourage shared alignment, opening doors for transparent communication and deepening mutual trust."
                    1 -> "As Saturn holds focus in $tSat, the celestial weather calls for patience and grounded dedication. This is a constructive period for defining healthy boundaries, organizing joint projects, and reinforcing the enduring strength of your connection."
                    2 -> "Expansive Jovian energies dominate with Jupiter in $tJup, sparking optimism, spontaneous adventures, and shared creative inspiration. Romantic trips, new social circles, or shared endeavors are highly supported under this cycle."
                    3 -> "Mars in $tMars catalyzes forward action, testing and elevating the maturity of your bond. Integrating lessons from the past cycle solidifies this connection as a profound source of mutual empowerment."
                    else -> "Transiting planetary cycles continue to support the conscious evolution of your shared path."
                }
                """
                    TITLE: $title
                    DATE: $dateLabel
                    DEEP_DIVE: $deepDive
                """.trimIndent()
            }.joinToString("\n|||\n")

            repository.saveInsightLocally("COMPATIBILITY_${partner.id}", compFallback)
            repository.saveInsightLocally("TIMINGS_${partner.id}", timingsFallback)
            generatingCompatibility.remove(partner.id)
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

    private fun parseDate(dateStr: String): Triple<Int, Int, Int> {
        val parts = if (dateStr.contains("/")) dateStr.split("/") else dateStr.split("-")
        return if (dateStr.contains("-") && parts.size == 3 && parts[0].length == 4) {
            val y = parts[0].toIntOrNull() ?: 2000
            val m = parts[1].toIntOrNull() ?: 1
            val d = parts[2].toIntOrNull() ?: 1
            Triple(m, d, y)
        } else {
            val m = parts.getOrNull(0)?.toIntOrNull() ?: 1
            val d = parts.getOrNull(1)?.toIntOrNull() ?: 1
            val y = parts.getOrNull(2)?.toIntOrNull() ?: 2000
            Triple(m, d, y)
        }
    }

    private fun parseTime(timeStr: String): Double {
        if (timeStr.startsWith("Unknown", ignoreCase = true)) return 12.0
        val parts = timeStr.split(":")
        var h = parts.getOrNull(0)?.toDoubleOrNull() ?: 12.0
        val mStr = parts.getOrNull(1) ?: "0"
        val m = mStr.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0
        val lower = timeStr.lowercase()
        if (lower.contains("pm") && h < 12.0) h += 12.0
        if (lower.contains("am") && h == 12.0) h = 0.0
        return (h + m / 60.0).coerceIn(0.0, 23.99)
    }

    private fun generateDailyHoroscopes(profile: UserProfile) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            val (birthMonth, birthDay, birthYear) = parseDate(profile.birthDate)
            val birthHour = parseTime(profile.birthTime)
            val natalPlanets = PlanetCalculator.calculatePlanets(
                birthYear, birthMonth, birthDay, birthHour, profile.latitude, profile.longitude
            )
            val nSun = natalPlanets["Sun"]?.sign ?: "Aries"
            val nMoon = natalPlanets["Moon"]?.sign ?: "Aries"
            val nVenus = natalPlanets["Venus"]?.sign ?: "Aries"
            val nMars = natalPlanets["Mars"]?.sign ?: "Aries"

            val today = java.time.LocalDate.now()
            val transits = PlanetCalculator.calculatePlanets(
                today.year, today.monthValue, today.dayOfMonth, 12.0, profile.latitude, profile.longitude
            )
            val lunarPhase = PlanetCalculator.getLunarPhase(today.year, today.monthValue, today.dayOfMonth)
            val transitAspects = PlanetCalculator.calculateTransitAspects(transits, natalPlanets)

            val tSun = transits["Sun"] ?: PlanetCalculator.PlanetPosition("Aries", 0, 0.0)
            val tMoon = transits["Moon"] ?: PlanetCalculator.PlanetPosition("Aries", 0, 0.0)
            val tVenus = transits["Venus"] ?: PlanetCalculator.PlanetPosition("Aries", 0, 0.0)
            val tMars = transits["Mars"] ?: PlanetCalculator.PlanetPosition("Aries", 0, 0.0)
            val tMercury = transits["Mercury"] ?: PlanetCalculator.PlanetPosition("Aries", 0, 0.0)

            val primaryAspect = transitAspects.firstOrNull()

            val dailyFeedText = """
                TITLE: Solar Current: Sun in ${tSun.sign} ${tSun.degree}°
                ADVICE: Anchor your $nSun focus into deliberate, purposeful action.
                DEEP_DIVE: The transiting Sun advances through ${tSun.sign} at ${tSun.degree}°, illuminating your conscious willpower and outward creative endeavors. For a native with Sun in $nSun, today's solar radiation encourages authentic self-expression, decisive initiative, and leadership.
                |||
                TITLE: Lunar Rhythm: ${lunarPhase.name} in ${lunarPhase.moonSign}
                ADVICE: ${lunarPhase.description}
                DEEP_DIVE: Today's emotional tides are governed by the ${lunarPhase.name} at ${tMoon.degree}° of ${lunarPhase.moonSign}. When intersecting your natal $nMoon Moon, this lunar cycle calls for emotional honesty, mindful boundaries, and listening closely to your instinctual radar.
                |||
                TITLE: ${if (primaryAspect != null) "Transit: ${primaryAspect.transitPlanet} ${primaryAspect.aspectName} Natal ${primaryAspect.natalPlanet}" else "Mercury in ${tMercury.sign}: Mental Flow"}
                ADVICE: ${primaryAspect?.interpretation ?: "Cultivate crisp communication, curiosity, and strategic discernment."}
                DEEP_DIVE: ${if (primaryAspect != null) "Today's planetary geometry forms an active ${primaryAspect.aspectName.lowercase()} between transiting ${primaryAspect.transitPlanet} and your natal ${primaryAspect.natalPlanet} (orb ${"%.1f".format(primaryAspect.orb)}°). This celestial aspect acts as a focused catalyst, clearing away hesitation and highlighting a key growth vector." else "Transiting Mercury at ${tMercury.degree}° of ${tMercury.sign} brings sharp cognitive clarity, easing negotiations and complex analysis."}
                |||
                TITLE: Venus & Mars Alignment
                ADVICE: Balance the diplomatic grace of Venus with Mars's assertive drive.
                DEEP_DIVE: Transiting Venus in ${tVenus.sign} and Mars in ${tMars.sign} create a dynamic interplay between attraction, values, and ambitious execution. Channel your natal $nMars Mars courage through focused tasks while honoring your $nVenus Venus appreciation for harmony and meaningful connections.
                |||
                TITLE: Cosmic Synthesis
                ADVICE: Trust the natural rhythm of universal timing and walk with grounded poise.
                DEEP_DIVE: Synthesizing today's real-time transits reveals an invitation to harmonize outward ambition with inward reflection. By balancing the solar vitality of ${tSun.sign} with the intuitive lessons of the ${lunarPhase.moonSign} Moon, you navigate today with celestial confidence.
            """.trimIndent()

            repository.saveInsightLocally("DAILY_FEED", dailyFeedText)
            isGeneratingDailyHoroscopes = false
        }
    }

    private fun generateDailyAffirmations(profile: UserProfile) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            val (birthMonth, birthDay, birthYear) = parseDate(profile.birthDate)
            val birthHour = parseTime(profile.birthTime)
            val natalPlanets = PlanetCalculator.calculatePlanets(
                birthYear, birthMonth, birthDay, birthHour, profile.latitude, profile.longitude
            )
            val nSun = natalPlanets["Sun"]?.sign ?: "Aries"
            val nMoon = natalPlanets["Moon"]?.sign ?: "Aries"

            val today = java.time.LocalDate.now()
            val lunarPhase = PlanetCalculator.getLunarPhase(today.year, today.monthValue, today.dayOfMonth)

            val affirmations = listOf(
                "Under the ${lunarPhase.name} in ${lunarPhase.moonSign}, I ground my $nSun vitality in deep cosmic trust and step forward with fearless clarity.",
                "I honor the emotional wisdom of my $nMoon Moon, welcoming inner peace and allowing the universe to guide today's unfolding.",
                "Guided by the active transits in ${lunarPhase.moonSign}, I release doubt, align with abundant blessings, and radiate centered confidence.",
                "My heart and mind are in sacred harmony, channeling my $nSun passion into empowering creations and uplifting connections.",
                "I walk in rhythm with the celestial spheres, trusting that every challenge is transforming into enduring wisdom and strength.",
                "I am a radiant conduit of cosmic light, embracing the gifts of this ${lunarPhase.name} and walking my true path with serenity."
            )
            val dayOfYear = today.dayOfYear
            val localAffirmation = affirmations[dayOfYear % affirmations.size]
            repository.saveInsightLocally("DAILY_AFFIRMATION", localAffirmation)
            isGeneratingDailyAffirmations = false
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

            val fullFallbackText = "$pDesc\n\n$fallbackDeepDive"
            repository.saveInsightLocally(cacheKey, fullFallbackText)
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                onResult(fullFallbackText)
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

    fun generate100YearAstrologyReport(profile: UserProfile, onResult: (String) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            val markdown = AstrologyTimelineGenerator.generate100YearMarkdownReport(profile)
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                onResult(markdown)
            }
        }
    }
}
