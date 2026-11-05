package com.example.ui

import com.example.data.UserProfile
import kotlin.math.roundToInt

/**
 * Generates an exhaustive, beautifully formatted 100-Year Life Astrological Dossier
 * in Markdown (.md) format spanning from the user's birth year through age 100.
 *
 * Implements:
 * 1. Natal Baseline Snapshot & Essential Dignities
 * 2. 100-Year Master Planetary Returns & Astronomical Cycles (Saturn, Jupiter, Uranus, Chiron, Nodal, Progressed Moon)
 * 3. 100-Year Annual Profection Ledger (Ages 0 to 100) with Time Lords and Activated Houses
 * 4. Decadal Life Chapter Syntheses (Decades 1 through 10)
 * 5. Astrological Reference Guide & Profection Glossary
 */
object AstrologyTimelineGenerator {

    data class LifeMilestone(
        val age: Int,
        val year: Int,
        val title: String,
        val description: String,
        val category: String // "SATURN", "JUPITER", "MIDLIFE", "CHIRON", "URANUS", "NODAL", "PROGRESSED_MOON"
    )

    private val HOUSE_NAMES = listOf(
        "1st House (Self, Vitality & Identity)",
        "2nd House (Finances, Resources & Values)",
        "3rd House (Communication, Siblings & Local Circles)",
        "4th House (Home, Family & Roots)",
        "5th House (Creativity, Children & Romance)",
        "6th House (Daily Health, Work & Routines)",
        "7th House (Partnerships, Marriage & Contracts)",
        "8th House (Shared Assets, Transformation & Rebirth)",
        "9th House (Higher Knowledge, Travel & Philosophy)",
        "10th House (Career, Public Standing & Calling)",
        "11th House (Community, Hopes & Social Alliances)",
        "12th House (Solitude, Subconscious & Spiritual Healing)"
    )

    private val HOUSE_THEMES = listOf(
        "Initiating fresh identity cycles, physical vitality, new personal directions.",
        "Monetary focus, earning power, resource management, personal self-worth.",
        "Learning, writing, close connections, skill development, mental agility.",
        "Domestic sanctuary, ancestral grounding, real estate, emotional security.",
        "Artistic expression, joy, romantic passions, playfulness, progeny.",
        "Refining daily habits, physical wellness, service, work ethic.",
        "One-on-one commitments, professional collaborations, relational harmony.",
        "Psychological depth, mutual investments, inheritance, regenerative change.",
        "Long-distance travel, spiritual studies, publication, worldview expansion.",
        "Vocational peaks, public visibility, leadership, legacy construction.",
        "Networking, collaborative dreams, humanitarian ideals, group synergy.",
        "Inner retreat, meditation, karmic release, surrender and spiritual replenishment."
    )

    private val TRADITIONAL_RULERS = mapOf(
        "Aries" to "Mars",
        "Taurus" to "Venus",
        "Gemini" to "Mercury",
        "Cancer" to "Moon",
        "Leo" to "Sun",
        "Virgo" to "Mercury",
        "Libra" to "Venus",
        "Scorpio" to "Mars (Modern: Pluto)",
        "Sagittarius" to "Jupiter",
        "Capricorn" to "Saturn",
        "Aquarius" to "Saturn (Modern: Uranus)",
        "Pisces" to "Jupiter (Modern: Neptune)"
    )

    fun generate100YearMarkdownReport(profile: UserProfile): String {
        val dateParts = if (profile.birthDate.contains("/")) {
            profile.birthDate.split("/")
        } else {
            profile.birthDate.split("-")
        }

        val (birthMonth, birthDay, birthYear) = if (dateParts.size >= 3) {
            if (dateParts[0].length == 4) {
                // yyyy-MM-dd
                Triple(
                    dateParts[1].toIntOrNull() ?: 1,
                    dateParts[2].toIntOrNull() ?: 1,
                    dateParts[0].toIntOrNull() ?: 2000
                )
            } else {
                // MM/dd/yyyy
                Triple(
                    dateParts[0].toIntOrNull() ?: 1,
                    dateParts[1].toIntOrNull() ?: 1,
                    dateParts[2].toIntOrNull() ?: 2000
                )
            }
        } else {
            Triple(1, 1, 2000)
        }

        val birthHour = run {
            val raw = profile.birthTime.trim()
            if (raw.isBlank() || raw.startsWith("Unknown", ignoreCase = true)) {
                12.0
            } else {
                val isPm = raw.contains("PM", ignoreCase = true)
                val isAm = raw.contains("AM", ignoreCase = true)
                val cleaned = raw.replace("(?i)am|pm".toRegex(), "").trim()
                val parts = cleaned.split(":")
                var h = parts.getOrNull(0)?.toDoubleOrNull() ?: 12.0
                val m = parts.getOrNull(1)?.toDoubleOrNull() ?: 0.0

                if (isPm && h < 12.0) h += 12.0
                if (isAm && h == 12.0) h = 0.0
                (h + (m / 60.0)).coerceIn(0.0, 23.99)
            }
        }

        // Calculate natal baseline
        val natalPlanets = PlanetCalculator.calculatePlanets(
            birthYear, birthMonth, birthDay, birthHour,
            profile.latitude, profile.longitude
        )
        val angles = PlanetCalculator.calculateAngles(
            birthYear, birthMonth, birthDay, birthHour,
            profile.latitude, profile.longitude
        )

        val ascSign = angles.ascendant.sign
        val ascIndex = PlanetCalculator.ZODIAC_SIGNS.indexOf(ascSign).let { if (it >= 0) it else 0 }

        val endYear = birthYear + 100

        // Build milestone map via full 101-year ephemeris calculations
        val milestones = calculateCentennialMilestones(
            birthYear, birthMonth, birthDay, birthHour,
            profile.latitude, profile.longitude, natalPlanets
        )

        val sb = StringBuilder()

        // ----------------------------------------------------
        // Title & Metadata
        // ----------------------------------------------------
        sb.append("# 🌌 COSMIC LIFE LEDGER: 100-YEAR ASTROLOGICAL TIMELINE\n\n")
        sb.append("> **Centennial Planetary Horizons & Lifelong Transit Dossier**  \n")
        sb.append("> Covering a complete human century from birth through 100 years of age.\n\n")

        sb.append("### 👤 Chart Bearer Details\n\n")
        sb.append("| Parameter | Information |\n")
        sb.append("| :--- | :--- |\n")
        sb.append("| **Name** | ${profile.name} |\n")
        sb.append("| **Birth Date** | ${profile.birthDate} |\n")
        sb.append("| **Birth Time** | ${profile.birthTime} |\n")
        sb.append("| **Birth Coordinates** | ${"%.4f".format(profile.latitude)}°, ${"%.4f".format(profile.longitude)}° (${profile.birthLocation}) |\n")
        sb.append("| **Rising Sign (Ascendant)** | $ascSign (${angles.ascendant.degree}° ${angles.ascendant.minute}') |\n")
        sb.append("| **Sun Sign** | ${natalPlanets["Sun"]?.sign ?: profile.sunSign} (${natalPlanets["Sun"]?.degree}° ${natalPlanets["Sun"]?.minute}') |\n")
        sb.append("| **Moon Sign** | ${natalPlanets["Moon"]?.sign ?: profile.moonSign} (${natalPlanets["Moon"]?.degree}° ${natalPlanets["Moon"]?.minute}') |\n")
        sb.append("| **Midheaven (MC)** | ${angles.midheaven.sign} (${angles.midheaven.degree}° ${angles.midheaven.minute}') |\n")
        sb.append("| **Centennial Range** | **$birthYear — $endYear** (Ages 0 to 100) |\n\n")

        sb.append("---\n\n")

        // ----------------------------------------------------
        // Section 1: Natal Foundation Blueprint
        // ----------------------------------------------------
        sb.append("## 🪐 Part 1: Natal Planetary Foundation (Age 0 Baseline)\n\n")
        sb.append("The celestial positions at your exact birth provide the immutable blueprint against which all lifelong transits, profections, and returns resonate.\n\n")
        sb.append("| Celestial Body | Zodiac Sign | Exact Degree | House | Dignity | Traditional Ruler |\n")
        sb.append("| :--- | :--- | :--- | :--- | :--- | :--- |\n")

        val planetOrder = listOf("Sun", "Moon", "Mercury", "Venus", "Mars", "Jupiter", "Saturn", "Uranus", "Neptune", "Pluto", "North Node", "South Node", "Chiron")
        planetOrder.forEach { planetName ->
            val pos = natalPlanets[planetName]
            if (pos != null) {
                val dignity = PlanetCalculator.calculateDignity(planetName, pos.sign).name
                val ruler = TRADITIONAL_RULERS[pos.sign] ?: "Unknown"
                // Approximate house in Whole Sign
                val pSignIndex = PlanetCalculator.ZODIAC_SIGNS.indexOf(pos.sign)
                val houseNum = if (pSignIndex >= 0) ((pSignIndex - ascIndex + 12) % 12) + 1 else 1
                sb.append("| **$planetName** | ${pos.sign} | ${pos.degree}° ${"%02d".format(pos.minute)}' | House $houseNum | $dignity | $ruler |\n")
            }
        }
        sb.append("\n---\n\n")

        // ----------------------------------------------------
        // Section 2: Master Centennial Planetary Cycles & Returns Catalog
        // ----------------------------------------------------
        sb.append("## ⏳ Part 2: Master Centennial Life Cycles Catalog (Ages 0 to 100)\n\n")
        sb.append("Astrological life cycles follow precise celestial mathematics. Below is your complete catalog of major planetary returns and life thresholds across 100 years.\n\n")

        sb.append("### 1. The Great Saturn Cycles (Structure, Vocation & Boundaries • ~29.5-Year Orbit)\n")
        sb.append("- **Age ~7 (${birthYear + 7})**: *Saturn Opening Square* — First awareness of societal rules, school discipline, and structured responsibility.\n")
        sb.append("- **Age ~14–15 (${birthYear + 14}–${birthYear + 15})**: *Saturn Opposition* — Teenage identity individuation and boundary testing.\n")
        sb.append("- **Age ~21–22 (${birthYear + 21}–${birthYear + 22})**: *Saturn Closing Square* — Transitioning to legal adulthood and financial reality.\n")
        sb.append("- **Age 29–30 (${birthYear + 29}–${birthYear + 30})**: **⚡ 1ST SATURN RETURN** — The definitive threshold of true adulthood. Career restructuring, shedding illusions, and solidifying lifelong foundations.\n")
        sb.append("- **Age ~36–37 (${birthYear + 36}–${birthYear + 37})**: *Saturn Waxing Square* — Mid-course adjustment of post-Return ambitions.\n")
        sb.append("- **Age ~43–44 (${birthYear + 43}–${birthYear + 44})**: *Saturn 2nd Opposition* — Reassessing life's balance during the midlife awakening.\n")
        sb.append("- **Age ~51–52 (${birthYear + 51}–${birthYear + 52})**: *Saturn Waning Square* — Pruning non-essentials in preparation for elder masterhood.\n")
        sb.append("- **Age 58–60 (${birthYear + 58}–${birthYear + 60})**: **⚡ 2ND SATURN RETURN** — The gateway to Elder Wisdom. Retirement of performative striving, stepping into master craftsmanship and mentorship.\n")
        sb.append("- **Age ~73–74 (${birthYear + 73}–${birthYear + 74})**: *Saturn 3rd Opposition* — Deep philosophical peace and legacy stewardship.\n")
        sb.append("- **Age 87–89 (${birthYear + 87}–${birthYear + 89})**: **⚡ 3RD SATURN RETURN** — The Sage Return. Complete worldly transcendence and spiritual clarity.\n\n")

        sb.append("### 2. The Jupiter Cycles of Expansion & Wisdom (~11.86-Year Orbit)\n")
        val jupiterReturns = listOf(12, 24, 36, 48, 60, 72, 84, 96)
        jupiterReturns.forEachIndexed { idx, age ->
            val yr = birthYear + age
            val cycleName = when (idx + 1) {
                1 -> "Adolescent Horizon Expansion"
                2 -> "Young Adulthood Launching & Exploration"
                3 -> "Mid-Career Maturation & Influence"
                4 -> "Social Leadership & Philanthropic Reach"
                5 -> "Senior Fruition (Concurring with 2nd Saturn Return)"
                6 -> "Elder Wisdom Sharing & Philosophical Peace"
                7 -> "Spiritual Re-Ascension (Concurring with Uranus Return)"
                8 -> "Century Patriarch / Matriarch Sage Harvest"
                else -> "Expansive Synthesis"
            }
            sb.append("- **Age $age ($yr)**: **Jupiter Return #${idx + 1}** — *$cycleName*. Rekindles optimism, brings cosmic gifts, and broadens philosophical perspective.\n")
        }
        sb.append("\n")

        sb.append("### 3. The Midlife Transformation Trio (Ages 38–45)\n")
        sb.append("- **Age ~38–42 (${birthYear + 38}–${birthYear + 42})**: *Pluto Square Pluto* — Psychological underworld journey; reclaiming dormant personal power and eliminating outgrown identities.\n")
        sb.append("- **Age ~40–42 (${birthYear + 40}–${birthYear + 42})**: *Neptune Square Neptune* — Dissolution of ego illusions and material disenchantment, opening deep spiritual empathy.\n")
        sb.append("- **Age ~41–43 (${birthYear + 41}–${birthYear + 43})**: **⚡ URANUS OPPOSITION (The Midlife Awakening)** — Breaking routines, sudden shifts in lifestyle or vocational calling, demand for total personal authenticity.\n\n")

        sb.append("### 4. Chiron & Uranus Returns (Healing & Complete Planetary Cycles)\n")
        sb.append("- **Age ~50–51 (${birthYear + 50}–${birthYear + 51})**: **Chiron Return** — Transmutation of your deepest natal wound into your greatest source of healing wisdom for others.\n")
        sb.append("- **Age ~84 (${birthYear + 84})**: **⚡ URANUS RETURN (84-Year Orbit)** — Uranus completes a full journey around the zodiac. Profound mental clarity, liberation from social conditioning, and cosmic homecoming.\n\n")

        sb.append("### 5. The Nodal Dharmic Helix (The 18.6-Year Karmic Destiny Cycle)\n")
        val nodalReturns = listOf(18, 37, 56, 74, 93)
        nodalReturns.forEachIndexed { i, age ->
            val yr = birthYear + age
            sb.append("- **Age ~$age ($yr)**: **Nodal Return #${i + 1}** — Alignment with your North Node evolutionary dharma; meeting destiny contacts and purposeful milestones.\n")
        }
        sb.append("\n---\n\n")

        // ----------------------------------------------------
        // Section 3: 100-Year Chronological Life Ledger
        // ----------------------------------------------------
        sb.append("## 📜 Part 3: The 100-Year Chronological Life Ledger (Ages 0 to 100)\n\n")
        sb.append("Every year of life activates a specific House in your chart via Hellenistic Annual Profections. The ruler of that sign becomes your **Lord of the Year**, serving as your primary planetary guide for that 12-month chapter.\n\n")

        for (decade in 0..9) {
            val startAge = decade * 10
            val endAge = if (decade == 9) 100 else startAge + 9
            val startYr = birthYear + startAge
            val endYr = birthYear + endAge

            val decadeTheme = getDecadeTheme(decade)

            sb.append("### 🏛️ Decade ${decade + 1}: Ages $startAge to $endAge ($startYr – $endYr)\n")
            sb.append("**Decade Theme**: *$decadeTheme*\n\n")

            sb.append("| Age | Year | Profection House | Active Sign | Lord of Year | Planetary Milestones & Chapter Guidance |\n")
            sb.append("| :--- | :--- | :--- | :--- | :--- | :--- |\n")

            for (age in startAge..endAge) {
                val calYear = birthYear + age
                val houseZeroIndex = age % 12
                val houseNumber = houseZeroIndex + 1
                val signIndex = (ascIndex + houseZeroIndex) % 12
                val activeSign = PlanetCalculator.ZODIAC_SIGNS[signIndex]
                val timeLord = TRADITIONAL_RULERS[activeSign] ?: "Unknown"

                // Check for milestones in this year
                val yearMilestones = milestones.filter { it.age == age }
                val milestoneText = if (yearMilestones.isNotEmpty()) {
                    yearMilestones.joinToString(" • ") { "**${it.title}**: ${it.description}" }
                } else {
                    HOUSE_THEMES[houseZeroIndex]
                }

                sb.append("| **$age** | $calYear | House $houseNumber | $activeSign | **$timeLord** | $milestoneText |\n")
            }
            sb.append("\n")
        }

        sb.append("---\n\n")

        // ----------------------------------------------------
        // Section 4: Astrological Reference Appendix
        // ----------------------------------------------------
        sb.append("## 📚 Part 4: Centennial Reference Guide & Profection Methodology\n\n")
        sb.append("### How to Use Your Annual Profection Ledger\n")
        sb.append("1. **Determine Your Current Age**: Find your current year row in the ledger above to identify your active Profection House.\n")
        sb.append("2. **Identify Your Lord of the Year**: The planet ruling the active sign is the primary planetary ruler of your year. When that planet is triggered by current transits, it produces significant turning points.\n")
        sb.append("3. **Examine the Active House**: The topics governed by that house will be the primary classroom of your experience throughout that birthday year.\n\n")

        sb.append("### House Meanings in Profections\n")
        for (i in 0..11) {
            sb.append("- **${HOUSE_NAMES[i]}**: ${HOUSE_THEMES[i]}\n")
        }

        sb.append("\n### Epilogue\n")
        sb.append("Your 100-year chart is a map of unfolding potential, not a rigid script. May this centennial dossier illuminate your journey through every season of life.\n\n")
        sb.append("*Generated by AstroLog • Complete On-Device Centennial Ephemeris Calculation.*\n")

        return sb.toString()
    }

    private fun getDecadeTheme(decade: Int): String {
        return when (decade) {
            0 -> "Foundational Roots, Family Nurturance & Physical Growth"
            1 -> "Identity Exploration, Adolescence & 1st Jupiter/Saturn Thresholds"
            2 -> "Emerging Independence, Career Launch & The Definitive 1st Saturn Return"
            3 -> "Material Construction, Social Integration & The 3rd Jupiter Expansion"
            4 -> "The Midlife Awakening: Uranus Opposition, Deep Transformation & Power Reclamation"
            5 -> "The Chiron Healing Gateway, Vocational Mastery & The 2nd Saturn Return"
            6 -> "Senior Fruition, Mentorship & Harvesting Decades of Experience"
            7 -> "Elder Leadership, Philosophical Depth & Legacy Stewardship"
            8 -> "The Uranus Return Milestone: Revolutionary Liberation & Spiritual Clarity"
            9 -> "Transcendence, Centennial Wisdom, Ancestral Completion & Cosmic Peace"
            else -> "Celestial Evolution"
        }
    }

    private fun aspectDistance(lon1: Double, lon2: Double): Double {
        var diff = kotlin.math.abs(lon1 - lon2) % 360.0
        if (diff > 180.0) diff = 360.0 - diff
        return diff
    }

    private fun calculateCentennialMilestones(
        birthYear: Int,
        birthMonth: Int,
        birthDay: Int,
        birthHour: Double,
        latitude: Double,
        longitude: Double,
        natalPlanets: Map<String, PlanetCalculator.PlanetPosition>
    ): List<LifeMilestone> {
        val list = mutableListOf<LifeMilestone>()

        // Precalculate 101 annual transits across the century (ages 0 to 100)
        val annualTransits = (0..100).map { age ->
            val calYear = birthYear + age
            PlanetCalculator.calculatePlanets(calYear, birthMonth, birthDay, birthHour, latitude, longitude)
        }

        // Helper to locate exact return or aspect crossing years
        fun findAspectCrossings(
            planetName: String,
            targetAspect: Double,
            searchRanges: List<IntRange>,
            onFound: (age: Int, calYear: Int, cycleIndex: Int) -> LifeMilestone
        ) {
            val natalLon = natalPlanets[planetName]?.longitude ?: return
            searchRanges.forEachIndexed { cycleIdx, range ->
                val bestAge = range.minByOrNull { age ->
                    val transitLon = annualTransits.getOrNull(age)?.get(planetName)?.longitude ?: 0.0
                    kotlin.math.abs(aspectDistance(transitLon, natalLon) - targetAspect)
                }
                if (bestAge != null && bestAge in 1..100) {
                    list.add(onFound(bestAge, birthYear + bestAge, cycleIdx + 1))
                }
            }
        }

        // Saturn Returns (Conjunction, 0°) near ages 28-31, 57-61, 86-90
        findAspectCrossings("Saturn", 0.0, listOf(28..31, 57..61, 86..90)) { age, yr, idx ->
            val title = when (idx) {
                1 -> "⚡ 1ST SATURN RETURN"
                2 -> "⚡ 2ND SATURN RETURN"
                else -> "⚡ 3RD SATURN RETURN"
            }
            val desc = when (idx) {
                1 -> "Major adulthood threshold. Restructuring career, shedding illusions, and committing to lifelong paths."
                2 -> "Gateway to Elder Wisdom. Retirement of worldly vanity and step into master craftsmanship."
                else -> "Sage milestone: complete transcendence of worldly boundaries and spiritual peace."
            }
            LifeMilestone(age, yr, title, desc, "SATURN")
        }

        // Saturn Oppositions (180°) near ages 14-16, 42-45, 72-75
        findAspectCrossings("Saturn", 180.0, listOf(14..16, 42..45, 72..75)) { age, yr, idx ->
            val title = "Saturn Opposition #$idx"
            val desc = when (idx) {
                1 -> "Adolescent individuation and tension with authority figures."
                2 -> "Re-evaluating achievements, commitments, and long-term stamina."
                else -> "Deep acceptance and stewardship of life's harvest."
            }
            LifeMilestone(age, yr, title, desc, "SATURN")
        }

        // Saturn Squares (90°) near ages 6-8, 20-22, 35-38, 50-53
        findAspectCrossings("Saturn", 90.0, listOf(6..8, 20..22, 35..38, 50..53)) { age, yr, idx ->
            val title = when (idx) {
                1 -> "Saturn Opening Square"
                2 -> "Saturn Closing Square"
                3 -> "Saturn Waxing Square"
                else -> "Saturn Waning Square"
            }
            val desc = when (idx) {
                1 -> "First encounters with social discipline and structural boundaries."
                2 -> "Transition to legal adulthood, financial self-reliance, and independent choices."
                3 -> "Mid-course review of post-Saturn-Return responsibilities."
                else -> "Pruning obsolete burdens to focus on highest-impact efforts."
            }
            LifeMilestone(age, yr, title, desc, "SATURN")
        }

        // Jupiter Returns (~11.86 year cycle: ages ~11-13, 23-25, 35-37, 47-49, 58-61, 70-73, 82-85, 94-97)
        val jupiterRanges = listOf(11..13, 23..25, 35..37, 47..49, 58..61, 70..73, 82..85, 94..97)
        findAspectCrossings("Jupiter", 0.0, jupiterRanges) { age, yr, idx ->
            val desc = when (idx) {
                1 -> "1st Jupiter Return: Adolescence horizon expansion"
                2 -> "2nd Jupiter Return: Young adult independence and fortune"
                3 -> "3rd Jupiter Return: Expanding mastery and community influence"
                4 -> "4th Jupiter Return: Social leadership and philosophical broadening"
                5 -> "5th Jupiter Return: Senior fruition and joyous synthesis"
                6 -> "6th Jupiter Return: Philosophical peace and legacy sharing"
                7 -> "7th Jupiter Return: Concurrence with Uranus Return; transcendent vision"
                else -> "8th Jupiter Return: Centennial sage blessing and completion"
            }
            LifeMilestone(age, yr, "Jupiter Return", desc, "JUPITER")
        }

        // Uranus Opposition (180°) near ages 40-44
        findAspectCrossings("Uranus", 180.0, listOf(40..44)) { age, yr, _ ->
            LifeMilestone(age, yr, "⚡ URANUS OPPOSITION", "The Midlife Awakening: authentic liberation, sudden breakthrough shifts.", "MIDLIFE")
        }

        // Uranus Return (0°) near ages 82-86
        findAspectCrossings("Uranus", 0.0, listOf(82..86)) { age, yr, _ ->
            LifeMilestone(age, yr, "⚡ URANUS RETURN", "Completion of full 84-year solar-orbital journey: revolutionary clarity and cosmic homecoming.", "URANUS")
        }

        // Chiron Return (0°) near ages 49-52
        findAspectCrossings("Chiron", 0.0, listOf(48..53)) { age, yr, _ ->
            LifeMilestone(age, yr, "Chiron Return", "Transmuting core vulnerability into compassionate healing mastery.", "CHIRON")
        }

        // Nodal Returns (18.6-year cycle: ~18-20, 36-38, 55-57, 73-76, 92-95)
        val nodalRanges = listOf(18..20, 36..38, 55..57, 73..76, 92..95)
        findAspectCrossings("North Node", 0.0, nodalRanges) { age, yr, idx ->
            LifeMilestone(age, yr, "Nodal Return #$idx", "Destiny alignment and activation of North Node evolutionary dharma.", "NODAL")
        }

        // Midlife Pluto & Neptune squares
        findAspectCrossings("Pluto", 90.0, listOf(36..45)) { age, yr, _ ->
            LifeMilestone(age, yr, "Pluto Square Pluto", "Deep psychic underworld journey; shedding disingenuous power dynamics.", "MIDLIFE")
        }
        findAspectCrossings("Neptune", 90.0, listOf(39..44)) { age, yr, _ ->
            LifeMilestone(age, yr, "Neptune Square Neptune", "Spiritual re-enchantment; seeing past worldly illusions.", "MIDLIFE")
        }

        // Progressed Moon Returns (~27.3 years)
        val progMoonAges = listOf(27, 54, 82)
        progMoonAges.forEachIndexed { i, a ->
            list.add(LifeMilestone(a, birthYear + a, "Progressed Moon Return #${i + 1}", "Emotional maturation, inner autonomy, and soul peace.", "PROGRESSED_MOON"))
        }

        return list.sortedBy { it.age }
    }
}
