package com.example.ui

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.*

object PlanetCalculator {
    private fun getJulianDaysSinceJ2000(year: Int, month: Int, day: Int, hour: Double): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = y / 100
        val b = 2 - a + (a / 4)
        val jd = floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
        return (jd + hour / 24.0) - 2451545.0
    }

    private fun normalize(angle: Double): Double {
        var a = angle % 360.0
        if (a < 0) a += 360.0
        return a
    }

    data class PlanetPosition(val sign: String, val degree: Int, val longitude: Double)

    private fun getSignAndDegree(longitude: Double): PlanetPosition {
        val signs = listOf("Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo", "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces")
        var normLon = longitude % 360.0
        if (normLon < 0) normLon += 360.0
        val signIndex = (normLon / 30.0).toInt() % 12
        val degree = (normLon % 30.0).toInt()
        return PlanetPosition(signs[signIndex], degree, normLon)
    }

    data class OrbitalElements(
        val N: Double, val Nd: Double,
        val i: Double, val id: Double,
        val w: Double, val wd: Double,
        val a: Double,
        val e: Double, val ed: Double,
        val M: Double, val Md: Double
    )

    private fun getGeocentricLongitude(d: Double, elem: OrbitalElements, sunLon: Double, r_sun: Double, isInner: Boolean): Double {
        val N = normalize(elem.N + elem.Nd * d)
        val i = elem.i + elem.id * d
        val w = normalize(elem.w + elem.wd * d)
        val a = elem.a
        val e = elem.e + elem.ed * d
        val M = normalize(elem.M + elem.Md * d)

        val M_rad = Math.toRadians(M)
        var E = M_rad + e * sin(M_rad) * (1.0 + e * cos(M_rad))
        for (iter in 0..5) {
            E -= (E - e * sin(E) - M_rad) / (1 - e * cos(E))
        }

        val xv = a * (cos(E) - e)
        val yv = a * (sqrt(1.0 - e * e) * sin(E))

        val v = Math.toDegrees(atan2(yv, xv))
        val r = sqrt(xv * xv + yv * yv)

        val lxh = normalize(v + w)
        val lxh_rad = Math.toRadians(lxh)
        val N_rad = Math.toRadians(N)
        val i_rad = Math.toRadians(i)

        val xh = r * (cos(N_rad) * cos(lxh_rad) - sin(N_rad) * sin(lxh_rad) * cos(i_rad))
        val yh = r * (sin(N_rad) * cos(lxh_rad) + cos(N_rad) * sin(lxh_rad) * cos(i_rad))

        val sunLonRad = Math.toRadians(sunLon)
        val xg = xh + r_sun * cos(sunLonRad)
        val yg = yh + r_sun * sin(sunLonRad)

        return normalize(Math.toDegrees(atan2(yg, xg)))
    }

    fun calculatePlanets(year: Int, month: Int, day: Int, hour: Double = 12.0, latitude: Double = 40.0, longitude: Double = 0.0): Map<String, PlanetPosition> {
        val tzOffset = round(longitude / 15.0)
        val utcHour = hour - tzOffset
        val d = getJulianDaysSinceJ2000(year, month, day, utcHour)
        
        val w_sun = 282.9404 + 4.70935E-5 * d
        val e_sun = 0.016709 - 1.151E-9 * d
        val M_sun = normalize(356.0470 + 0.9856002585 * d)
        val M_sun_rad = Math.toRadians(M_sun)
        
        var E_sun = M_sun_rad + e_sun * sin(M_sun_rad) * (1.0 + e_sun * cos(M_sun_rad))
        val xv_sun = cos(E_sun) - e_sun
        val yv_sun = sqrt(1.0 - e_sun * e_sun) * sin(E_sun)
        val v_sun = Math.toDegrees(atan2(yv_sun, xv_sun))
        val r_sun = sqrt(xv_sun * xv_sun + yv_sun * yv_sun)
        
        val ecliptic_longitude_sun = normalize(v_sun + w_sun)
        
        // Exact trigonometric Ascendant calculation
        val gmstDeg = 280.46061837 + 360.98564736629 * d
        val LST_deg = normalize(gmstDeg + longitude)
        val LST_rad = Math.toRadians(LST_deg)
        val e_rad = Math.toRadians(23.4392911)
        val lat_rad = Math.toRadians(latitude)
        
        val asc_y = cos(LST_rad)
        val asc_x = -(sin(LST_rad) * cos(e_rad) + tan(lat_rad) * sin(e_rad))
        val ecliptic_longitude_ascendant = normalize(Math.toDegrees(atan2(asc_y, asc_x)))

        val L_moon = normalize(218.316 + 13.176396 * d)
        val M_moon = normalize(134.963 + 13.064993 * d)
        val ecliptic_longitude_moon = normalize(L_moon + 6.289 * sin(Math.toRadians(M_moon)))

        // Black Moon Lilith (Mean Lunar Apogee)
        val ecliptic_longitude_lilith = normalize(83.353243 + 0.111403539 * d)

        // Better Venus elements specifically adjusted for JPL ephemeris accuracy:
        // L = 181.979; w_bar = 131.56; N = 76.68; M = L - w_bar = 50.41 (adjusted to Schlyter's format where w is w_bar - N)
        val mercuryElem = OrbitalElements(48.3313, 3.24587E-5, 7.0047, 5.00E-8, 29.1241, 1.01444E-5, 0.387098, 0.205635, 5.59E-10, 168.6562, 4.0923344368)
        val venusElem = OrbitalElements(76.68069, 2.46590E-5, 3.39463, 2.75E-8, 54.89100, 1.38374E-5, 0.723330, 0.006773, -1.302E-9, 50.415, 1.6021302244)
        val marsElem = OrbitalElements(49.5574, 2.11081E-5, 1.8497, -1.78E-8, 286.5016, 2.92961E-5, 1.523688, 0.093405, 2.516E-9, 18.6021, 0.5240207766)
        val jupiterElem = OrbitalElements(100.4542, 2.76854E-5, 1.3030, -1.557E-7, 273.8777, 1.64505E-5, 5.20256, 0.048498, 4.469E-9, 19.8950, 0.0830853001)
        val saturnElem = OrbitalElements(113.6634, 2.38980E-5, 2.4886, -1.081E-7, 339.3939, 2.97661E-5, 9.55475, 0.055546, -9.499E-9, 316.9670, 0.0334442282)
        val uranusElem = OrbitalElements(74.0005, 1.3978E-5, 0.7733, 1.9E-8, 96.6612, 3.0565E-5, 19.18171, 0.047318, 7.45E-9, 142.5905, 0.011725806)
        val neptuneElem = OrbitalElements(131.7806, 3.0173E-5, 1.7700, -2.55E-7, 272.8461, -6.027E-6, 30.05826, 0.008606, 2.15E-9, 260.2471, 0.005995147)
        val plutoElem = OrbitalElements(110.3034, 0.0, 17.1417, 0.0, 113.7632, 0.0, 39.48168, 0.248827, 0.0, 14.882, 0.00396)

        val geocentricMercury = getGeocentricLongitude(d, mercuryElem, ecliptic_longitude_sun, r_sun, true)
        val geocentricVenus = getGeocentricLongitude(d, venusElem, ecliptic_longitude_sun, r_sun, true)
        val geocentricMars = getGeocentricLongitude(d, marsElem, ecliptic_longitude_sun, r_sun, false)
        val geocentricJupiter = getGeocentricLongitude(d, jupiterElem, ecliptic_longitude_sun, r_sun, false)
        val geocentricSaturn = getGeocentricLongitude(d, saturnElem, ecliptic_longitude_sun, r_sun, false)
        val geocentricUranus = getGeocentricLongitude(d, uranusElem, ecliptic_longitude_sun, r_sun, false)
        val geocentricNeptune = getGeocentricLongitude(d, neptuneElem, ecliptic_longitude_sun, r_sun, false)
        val geocentricPluto = getGeocentricLongitude(d, plutoElem, ecliptic_longitude_sun, r_sun, false)

        return mapOf(
            "Sun" to getSignAndDegree(ecliptic_longitude_sun),
            "Moon" to getSignAndDegree(ecliptic_longitude_moon),
            "Mercury" to getSignAndDegree(geocentricMercury),
            "Venus" to getSignAndDegree(geocentricVenus),
            "Mars" to getSignAndDegree(geocentricMars),
            "Jupiter" to getSignAndDegree(geocentricJupiter),
            "Saturn" to getSignAndDegree(geocentricSaturn),
            "Uranus" to getSignAndDegree(geocentricUranus),
            "Neptune" to getSignAndDegree(geocentricNeptune),
            "Pluto" to getSignAndDegree(geocentricPluto),
            "Lilith" to getSignAndDegree(ecliptic_longitude_lilith),
            "Ascendant" to getSignAndDegree(ecliptic_longitude_ascendant)
        )
    }

    data class Aspect(val planet1: String, val planet2: String, val aspectName: String, val orb: Double)

    fun calculateAspects(planets: Map<String, PlanetPosition>): List<Aspect> {
        val aspects = mutableListOf<Aspect>()
        val planetNames = planets.keys.toList()
        for (i in 0 until planetNames.size) {
            for (j in i + 1 until planetNames.size) {
                val p1 = planetNames[i]
                val p2 = planetNames[j]
                val pos1 = planets[p1]!!.longitude
                val pos2 = planets[p2]!!.longitude

                var diff = abs(pos1 - pos2)
                if (diff > 180.0) diff = 360.0 - diff

                when {
                    diff in 0.0..8.0 -> aspects.add(Aspect(p1, p2, "Conjunct", diff))
                    diff in 54.0..66.0 -> aspects.add(Aspect(p1, p2, "Sextile", abs(diff - 60.0)))
                    diff in 82.0..98.0 -> aspects.add(Aspect(p1, p2, "Square", abs(diff - 90.0)))
                    diff in 112.0..128.0 -> aspects.add(Aspect(p1, p2, "Trine", abs(diff - 120.0)))
                    diff in 172.0..180.0 -> aspects.add(Aspect(p1, p2, "Opposition", abs(diff - 180.0)))
                }
            }
        }
        return aspects.sortedBy { it.orb }
    }

    data class LunarPhaseInfo(val name: String, val description: String, val angle: Double, val moonSign: String = "Unknown")

    fun getLunarPhase(year: Int, month: Int, day: Int): LunarPhaseInfo {
        val planets = calculatePlanets(year, month, day)
        val sunLon = planets["Sun"]?.longitude ?: 0.0
        val moonLon = planets["Moon"]?.longitude ?: 0.0
        val moonSign = planets["Moon"]?.sign ?: "Unknown"
        var diff = moonLon - sunLon
        if (diff < 0) diff += 360.0
        val name: String
        val desc: String
        when {
            diff < 45.0 -> { name = "New Moon"; desc = "Planting seeds and setting intentions." }
            diff < 90.0 -> { name = "Waxing Crescent"; desc = "Gathering momentum and refining goals." }
            diff < 135.0 -> { name = "First Quarter"; desc = "Taking action and facing challenges." }
            diff < 180.0 -> { name = "Waxing Gibbous"; desc = "Adjusting and fine-tuning plans." }
            diff < 225.0 -> { name = "Full Moon"; desc = "Reaping results and illuminating truths." }
            diff < 270.0 -> { name = "Waning Gibbous"; desc = "Expressing gratitude and releasing." }
            diff < 315.0 -> { name = "Last Quarter"; desc = "Evaluating and forgiving." }
            else -> { name = "Waning Crescent"; desc = "Resting, reflecting, and surrendering." }
        }
        return LunarPhaseInfo(name, desc, diff, moonSign)
    }

    data class AstroEvent(val name: String, val dateMillis: Long, val description: String, val type: String = "GENERAL", val intensity: String = "MINOR")

    fun calculateUpcomingEvents(
        year: Int, month: Int, day: Int, 
        natalPlanets: Map<String, PlanetPosition>? = null,
        days: Int = 60
    ): List<AstroEvent> {
        val events = mutableListOf<AstroEvent>()
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, day, 12, 0, 0)
        
        var prevPhase = -1.0
        val prevLons = mutableMapOf<String, Double>()
        val prevDiffs = mutableMapOf<String, Double>()
        
        val trackerPlanets = listOf("Mercury", "Venus", "Mars", "Jupiter", "Saturn", "Uranus", "Neptune", "Pluto")
        
        for (i in 0..days) {
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH) + 1
            val d = cal.get(Calendar.DAY_OF_MONTH)
            
            val dailyPlanets = calculatePlanets(y, m, d)
            val sunLon = dailyPlanets["Sun"]!!.longitude
            val moonLon = dailyPlanets["Moon"]!!.longitude
            
            var diff = moonLon - sunLon
            if (diff < 0) diff += 360.0
            
            if (i > 0) {
                 if (prevPhase < 180 && diff >= 180) {
                     val desc = "A powerful time for release, illumination, and completion."
                     val isEclipseSeason = (m == 4 || m == 10) 
                     if (isEclipseSeason) {
                         events.add(AstroEvent("Lunar Eclipse in ${dailyPlanets["Moon"]?.sign}", cal.timeInMillis, "A profound emotional reset and turning point.", "ECLIPSE", "MAJOR"))
                     } else {
                         events.add(AstroEvent("Full Moon in ${dailyPlanets["Moon"]?.sign}", cal.timeInMillis, desc, "LUNAR", "MINOR"))
                     }
                 } else if (prevPhase in 250.0..359.0 && diff in 0.0..60.0) {
                     val isEclipseSeason = (m == 4 || m == 10) 
                     if (isEclipseSeason) {
                         events.add(AstroEvent("Solar Eclipse in ${dailyPlanets["Sun"]?.sign}", cal.timeInMillis, "Major new beginnings and fateful fresh starts.", "ECLIPSE", "MAJOR"))
                     } else {
                         events.add(AstroEvent("New Moon in ${dailyPlanets["Sun"]?.sign}", cal.timeInMillis, "A perfect moment for setting intentions and new beginnings.", "LUNAR", "MINOR"))
                     }
                 }
                 
                 for (p in trackerPlanets) {
                     val currLon = dailyPlanets[p]!!.longitude
                     val prevLon = prevLons[p] ?: currLon
                     var mDiff = currLon - prevLon
                     val normalizedDiff = if (mDiff < -180.0) mDiff + 360.0 else if (mDiff > 180.0) mDiff - 360.0 else mDiff
                     
                     if (i > 1) { // Need at least 2 diffs to see a change
                         val prevDiff = prevDiffs[p] ?: 1.0
                         if (prevDiff > 0 && normalizedDiff < 0) {
                             events.add(AstroEvent("$p turns Retrograde", cal.timeInMillis, "A cycle of review, reassessment, and internal shifts.", "PLANETARY", "MAJOR"))
                         } else if (prevDiff < 0 && normalizedDiff > 0) {
                             events.add(AstroEvent("$p turns Direct", cal.timeInMillis, "Forward momentum and clarity returns.", "PLANETARY", "MINOR"))
                         }
                     }
                     prevDiffs[p] = normalizedDiff
                 }
                 
                 // Personal Transits
                 if (natalPlanets != null) {
                     // Check combinations
                     val currentSun = dailyPlanets["Sun"]?.longitude ?: 0.0
                     val currentVenus = dailyPlanets["Venus"]?.longitude ?: 0.0
                     val currentMars = dailyPlanets["Mars"]?.longitude ?: 0.0
                     val currentJupiter = dailyPlanets["Jupiter"]?.longitude ?: 0.0
                     
                     val natalSun = natalPlanets["Sun"]?.longitude ?: 0.0
                     val natalMoon = natalPlanets["Moon"]?.longitude ?: 0.0
                     val natalVenus = natalPlanets["Venus"]?.longitude ?: 0.0

                     // Helper for exact conjunctions (within 1 degree)
                     fun isConjunct(curr: Double, nat: Double) = Math.abs(curr - nat) < 1.0 || Math.abs(curr - nat) > 359.0
                     
                     if (isConjunct(currentSun, natalSun)) {
                         events.add(AstroEvent("Solar Return (Birthday!)", cal.timeInMillis, "The Sun returns to its exact position at your birth. A new personal year begins.", "PERSONAL", "MAJOR"))
                     }
                     if (isConjunct(currentVenus, natalVenus)) {
                         events.add(AstroEvent("Venus Return", cal.timeInMillis, "A beautiful period for self-love, relationships, and attraction.", "PERSONAL", "MAJOR"))
                     }
                     if (isConjunct(currentMars, natalVenus)) {
                         events.add(AstroEvent("Mars conjunct Natal Venus", cal.timeInMillis, "Passions and romantic energy are highly stimulated today.", "PERSONAL", "MINOR"))
                     }
                     if (isConjunct(currentJupiter, natalSun)) {
                         events.add(AstroEvent("Jupiter conjunct Natal Sun", cal.timeInMillis, "A major cycle of growth, luck, and expansion begins.", "PERSONAL", "MAJOR"))
                     }
                     if (isConjunct(currentJupiter, natalMoon)) {
                         events.add(AstroEvent("Jupiter conjunct Natal Moon", cal.timeInMillis, "Emotional expansion, domestic happiness, and optimism.", "PERSONAL", "MAJOR"))
                     }                     
                 }
            }
            prevPhase = diff
            for (p in trackerPlanets) {
                prevLons[p] = dailyPlanets[p]!!.longitude
            }
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return events
    }
}
