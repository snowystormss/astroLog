package com.example.ui

import com.example.data.*
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.*

object PlanetCalculator {

    fun getJulianDaysSinceJ2000(year: Int, month: Int, day: Int, hour: Double): Double {
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

    fun normalize(angle: Double): Double {
        var a = angle % 360.0
        if (a < 0) a += 360.0
        return a
    }

    data class PlanetPosition(
        val sign: String,
        val degree: Int,
        val longitude: Double,
        val minute: Int = 0
    )

    val ZODIAC_SIGNS = listOf(
        "Aries", "Taurus", "Gemini", "Cancer",
        "Leo", "Virgo", "Libra", "Scorpio",
        "Sagittarius", "Capricorn", "Aquarius", "Pisces"
    )

    fun getSignAndDegree(longitude: Double): PlanetPosition {
        val normLon = normalize(longitude)
        val signIndex = (normLon / 30.0).toInt().coerceIn(0, 11)
        val degInSign = normLon % 30.0
        val degree = degInSign.toInt()
        val minute = ((degInSign - degree) * 60.0).toInt().coerceIn(0, 59)
        return PlanetPosition(ZODIAC_SIGNS[signIndex], degree, normLon, minute)
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
        val yv = a * (sqrt(max(0.0, 1.0 - e * e)) * sin(E))

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

    fun calculateAngles(
        year: Int, month: Int, day: Int, hour: Double = 12.0,
        latitude: Double = 40.0, longitude: Double = 0.0
    ): AnglesResult {
        val tzOffset = round(longitude / 15.0)
        val utcHour = hour - tzOffset
        val d = getJulianDaysSinceJ2000(year, month, day, utcHour)

        val gmstDeg = 280.46061837 + 360.98564736629 * d
        val ramc = normalize(gmstDeg + longitude)
        val ramc_rad = Math.toRadians(ramc)
        val eps_rad = Math.toRadians(23.4392911 - 3.56E-7 * d)
        val lat_rad = Math.toRadians(latitude)

        // Midheaven (MC)
        val mc_lon = normalize(Math.toDegrees(atan2(sin(ramc_rad), cos(ramc_rad) * cos(eps_rad))))
        val mcPosition = getSignAndDegree(mc_lon)

        // Ascendant (ASC)
        val asc_y = cos(ramc_rad)
        val asc_x = -(sin(ramc_rad) * cos(eps_rad) + tan(lat_rad) * sin(eps_rad))
        val asc_lon = normalize(Math.toDegrees(atan2(asc_y, asc_x)))
        val ascPosition = getSignAndDegree(asc_lon)

        // Descendant (DC) = ASC + 180°
        val dc_lon = normalize(asc_lon + 180.0)
        val dcPosition = getSignAndDegree(dc_lon)

        // Imum Coeli (IC) = MC + 180°
        val ic_lon = normalize(mc_lon + 180.0)
        val icPosition = getSignAndDegree(ic_lon)

        return AnglesResult(ascPosition, mcPosition, dcPosition, icPosition)
    }

    fun calculateHouseCusps(
        system: String,
        ascLon: Double,
        mcLon: Double,
        latitude: Double = 40.0,
        longitude: Double = 0.0,
        year: Int = 2000,
        month: Int = 1,
        day: Int = 1,
        hour: Double = 12.0
    ): List<HouseCusp> {
        val tzOffset = round(longitude / 15.0)
        val utcHour = hour - tzOffset
        val d = getJulianDaysSinceJ2000(year, month, day, utcHour)
        val gmstDeg = 280.46061837 + 360.98564736629 * d
        val ramc = normalize(gmstDeg + longitude)

        return if (system.uppercase() == "PLACIDUS" && abs(latitude) < 66.5) {
            calculatePlacidusCuspsInternal(ramc, latitude, ascLon, mcLon)
        } else {
            calculateWholeSignCuspsInternal(ascLon)
        }
    }

    private fun calculateWholeSignCuspsInternal(ascLon: Double): List<HouseCusp> {
        val ascSignIndex = (normalize(ascLon) / 30.0).toInt().coerceIn(0, 11)
        val cusps = mutableListOf<HouseCusp>()
        for (h in 1..12) {
            val signIdx = (ascSignIndex + (h - 1)) % 12
            val signName = ZODIAC_SIGNS[signIdx]
            val cuspLon = signIdx * 30.0
            cusps.add(HouseCusp(h, signName, 0, 0, cuspLon))
        }
        return cusps
    }

    private fun calculatePlacidusCuspsInternal(
        ramcDeg: Double,
        latitude: Double,
        ascLon: Double,
        mcLon: Double
    ): List<HouseCusp> {
        val epsRad = Math.toRadians(23.4392911)
        val phiRad = Math.toRadians(latitude)

        fun placidusIteration(offsetDeg: Double, f: Double, isDiurnal: Boolean): Double {
            var ra = normalize(ramcDeg + offsetDeg)
            for (iter in 0..6) {
                val raRad = Math.toRadians(ra)
                val tanDecl = sin(raRad) * tan(epsRad)
                val valAd = (tan(phiRad) * tanDecl).coerceIn(-1.0, 1.0)
                val ad = Math.toDegrees(asin(valAd))
                val targetRa = if (isDiurnal) {
                    normalize(ramcDeg + offsetDeg + ad * (1.0 - f))
                } else {
                    normalize(ramcDeg + offsetDeg - ad * (1.0 - f))
                }
                ra = targetRa
            }
            val raRad = Math.toRadians(ra)
            return normalize(Math.toDegrees(atan2(sin(raRad), cos(raRad) * cos(epsRad))))
        }

        val c10 = mcLon
        val c11 = placidusIteration(30.0, 1.0 / 3.0, true)
        val c12 = placidusIteration(60.0, 2.0 / 3.0, true)
        val c1 = ascLon
        val c2 = placidusIteration(120.0, 2.0 / 3.0, false)
        val c3 = placidusIteration(150.0, 1.0 / 3.0, false)

        val c4 = normalize(c10 + 180.0)
        val c5 = normalize(c11 + 180.0)
        val c6 = normalize(c12 + 180.0)
        val c7 = normalize(c1 + 180.0)
        val c8 = normalize(c2 + 180.0)
        val c9 = normalize(c3 + 180.0)

        val rawLongitudes = listOf(c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11, c12)
        return rawLongitudes.mapIndexed { idx, lon ->
            val pos = getSignAndDegree(lon)
            HouseCusp(idx + 1, pos.sign, pos.degree, pos.minute, lon)
        }
    }

    fun getPlanetHouse(planetLon: Double, cusps: List<HouseCusp>, system: String = "WHOLE_SIGN"): Int {
        if (cusps.size < 12) return 1
        val norm = normalize(planetLon)
        if (system.uppercase() == "WHOLE_SIGN") {
            val planetSign = (norm / 30.0).toInt().coerceIn(0, 11)
            val ascSign = (normalize(cusps[0].longitude) / 30.0).toInt().coerceIn(0, 11)
            return ((planetSign - ascSign + 12) % 12) + 1
        }

        for (i in 0 until 12) {
            val start = normalize(cusps[i].longitude)
            val end = normalize(cusps[(i + 1) % 12].longitude)
            if (start < end) {
                if (norm in start..end) return i + 1
            } else {
                // House spans 0° Aries
                if (norm >= start || norm < end) return i + 1
            }
        }
        return 1
    }

    fun calculatePlanets(
        year: Int, month: Int, day: Int, hour: Double = 12.0,
        latitude: Double = 40.0, longitude: Double = 0.0
    ): Map<String, PlanetPosition> {
        val tzOffset = round(longitude / 15.0)
        val utcHour = hour - tzOffset
        val d = getJulianDaysSinceJ2000(year, month, day, utcHour)

        val w_sun = 282.9404 + 4.70935E-5 * d
        val e_sun = 0.016709 - 1.151E-9 * d
        val M_sun = normalize(356.0470 + 0.9856002585 * d)
        val M_sun_rad = Math.toRadians(M_sun)

        val E_sun = M_sun_rad + e_sun * sin(M_sun_rad) * (1.0 + e_sun * cos(M_sun_rad))
        val xv_sun = cos(E_sun) - e_sun
        val yv_sun = sqrt(max(0.0, 1.0 - e_sun * e_sun)) * sin(E_sun)
        val v_sun = Math.toDegrees(atan2(yv_sun, xv_sun))
        val r_sun = sqrt(xv_sun * xv_sun + yv_sun * yv_sun)

        val ecliptic_longitude_sun = normalize(v_sun + w_sun)

        // Moon
        val L_moon = normalize(218.316 + 13.176396 * d)
        val M_moon = normalize(134.963 + 13.064993 * d)
        val ecliptic_longitude_moon = normalize(L_moon + 6.289 * sin(Math.toRadians(M_moon)))

        // Angles
        val angles = calculateAngles(year, month, day, hour, latitude, longitude)

        // Mean Lunar Nodes (North & South Node) - Sakoian & Acker Ch. 2, Riske Appendix I
        val T = d / 36525.0
        val omega_node = normalize(125.04452 - 1934.136261 * T + 0.0020708 * T * T)
        val northNode = getSignAndDegree(omega_node)
        val southNode = getSignAndDegree(normalize(omega_node + 180.0))

        // Black Moon Lilith (Mean Lunar Apogee)
        val ecliptic_longitude_lilith = normalize(83.353243 + 0.111403539 * d)

        // Chiron ("The Wounded Healer") - Riske Appendix I
        val chironM = normalize(359.88 + 7.14256 * T)
        val chironElem = OrbitalElements(209.3, 0.0, 6.94, 0.0, 130.3, 0.0, 13.682, 0.380, 0.0, chironM, 0.0)
        val geocentricChiron = getGeocentricLongitude(d, chironElem, ecliptic_longitude_sun, r_sun, false)

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

        // Part of Fortune: Day vs Night formula
        val ascLon = angles.ascendant.longitude
        val sunDiffFromAsc = normalize(ecliptic_longitude_sun - ascLon)
        val isDayBirth = sunDiffFromAsc in 180.0..360.0 // Sun above horizon
        val partOfFortuneLon = if (isDayBirth) {
            normalize(ascLon + ecliptic_longitude_moon - ecliptic_longitude_sun)
        } else {
            normalize(ascLon + ecliptic_longitude_sun - ecliptic_longitude_moon)
        }

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
            "North Node" to northNode,
            "South Node" to southNode,
            "Chiron" to getSignAndDegree(geocentricChiron),
            "Lilith" to getSignAndDegree(ecliptic_longitude_lilith),
            "Ascendant" to angles.ascendant,
            "Midheaven" to angles.midheaven,
            "Descendant" to angles.descendant,
            "Imum Coeli" to angles.imumCoeli,
            "Part of Fortune" to getSignAndDegree(partOfFortuneLon)
        )
    }

    // Essential Dignities - Sakoian & Acker Ch. 8, Riske Chart 8
    fun calculateDignity(planetName: String, sign: String): DignityType {
        return when (planetName) {
            "Sun" -> when (sign) {
                "Leo" -> DignityType.DOMICILE
                "Aries" -> DignityType.EXALTATION
                "Aquarius" -> DignityType.DETRIMENT
                "Libra" -> DignityType.FALL
                else -> DignityType.PEREGRINE
            }
            "Moon" -> when (sign) {
                "Cancer" -> DignityType.DOMICILE
                "Taurus" -> DignityType.EXALTATION
                "Capricorn" -> DignityType.DETRIMENT
                "Scorpio" -> DignityType.FALL
                else -> DignityType.PEREGRINE
            }
            "Mercury" -> when (sign) {
                "Gemini", "Virgo" -> DignityType.DOMICILE
                "Virgo", "Aquarius" -> DignityType.EXALTATION
                "Sagittarius", "Pisces" -> DignityType.DETRIMENT
                "Pisces" -> DignityType.FALL
                else -> DignityType.PEREGRINE
            }
            "Venus" -> when (sign) {
                "Taurus", "Libra" -> DignityType.DOMICILE
                "Pisces" -> DignityType.EXALTATION
                "Scorpio", "Aries" -> DignityType.DETRIMENT
                "Virgo" -> DignityType.FALL
                else -> DignityType.PEREGRINE
            }
            "Mars" -> when (sign) {
                "Aries", "Scorpio" -> DignityType.DOMICILE
                "Capricorn" -> DignityType.EXALTATION
                "Libra", "Taurus" -> DignityType.DETRIMENT
                "Cancer" -> DignityType.FALL
                else -> DignityType.PEREGRINE
            }
            "Jupiter" -> when (sign) {
                "Sagittarius", "Pisces" -> DignityType.DOMICILE
                "Cancer" -> DignityType.EXALTATION
                "Gemini", "Virgo" -> DignityType.DETRIMENT
                "Capricorn" -> DignityType.FALL
                else -> DignityType.PEREGRINE
            }
            "Saturn" -> when (sign) {
                "Capricorn", "Aquarius" -> DignityType.DOMICILE
                "Libra" -> DignityType.EXALTATION
                "Cancer", "Leo" -> DignityType.DETRIMENT
                "Aries" -> DignityType.FALL
                else -> DignityType.PEREGRINE
            }
            "Uranus" -> when (sign) {
                "Aquarius" -> DignityType.DOMICILE
                "Scorpio" -> DignityType.EXALTATION
                "Leo" -> DignityType.DETRIMENT
                "Taurus" -> DignityType.FALL
                else -> DignityType.PEREGRINE
            }
            "Neptune" -> when (sign) {
                "Pisces" -> DignityType.DOMICILE
                "Cancer", "Leo" -> DignityType.EXALTATION
                "Virgo" -> DignityType.DETRIMENT
                "Capricorn" -> DignityType.FALL
                else -> DignityType.PEREGRINE
            }
            "Pluto" -> when (sign) {
                "Scorpio" -> DignityType.DOMICILE
                "Leo", "Aries" -> DignityType.EXALTATION
                "Taurus" -> DignityType.DETRIMENT
                "Aquarius" -> DignityType.FALL
                else -> DignityType.PEREGRINE
            }
            else -> DignityType.PEREGRINE
        }
    }

    // Dispositors & Mutual Reception - Sakoian & Acker Ch. 7, Riske Ch. 5
    val SIGN_RULERS = mapOf(
        "Aries" to "Mars",
        "Taurus" to "Venus",
        "Gemini" to "Mercury",
        "Cancer" to "Moon",
        "Leo" to "Sun",
        "Virgo" to "Mercury",
        "Libra" to "Venus",
        "Scorpio" to "Pluto",
        "Sagittarius" to "Jupiter",
        "Capricorn" to "Saturn",
        "Aquarius" to "Uranus",
        "Pisces" to "Neptune"
    )

    fun calculateDispositors(planets: Map<String, PlanetPosition>): DispositorResult {
        val traditionalPlanets = listOf("Sun", "Moon", "Mercury", "Venus", "Mars", "Jupiter", "Saturn", "Uranus", "Neptune", "Pluto")
        val dispositors = mutableMapOf<String, String>()
        for (p in traditionalPlanets) {
            val sign = planets[p]?.sign ?: continue
            val ruler = SIGN_RULERS[sign] ?: continue
            dispositors[p] = ruler
        }

        // Detect mutual receptions: A rules B's sign and B rules A's sign
        val mutualReceptions = mutableListOf<Pair<String, String>>()
        for (i in 0 until traditionalPlanets.size) {
            for (j in i + 1 until traditionalPlanets.size) {
                val p1 = traditionalPlanets[i]
                val p2 = traditionalPlanets[j]
                if (dispositors[p1] == p2 && dispositors[p2] == p1) {
                    mutualReceptions.add(Pair(p1, p2))
                }
            }
        }

        // Final dispositor: A planet in its own sign that disposes of others
        val inOwnSign = traditionalPlanets.filter { dispositors[it] == it }
        val finalDispositor = if (inOwnSign.size == 1) inOwnSign.first() else null

        return DispositorResult(dispositors, mutualReceptions, finalDispositor)
    }

    // Elemental, Modality & Polarity Engine - Sakoian & Acker Ch. 1, Riske Ch. 2
    fun calculateElementalBalance(planets: Map<String, PlanetPosition>): ElementalBalance {
        val weights = mapOf(
            "Sun" to 3,
            "Moon" to 3,
            "Ascendant" to 3,
            "Midheaven" to 2,
            "Mercury" to 2,
            "Venus" to 2,
            "Mars" to 2,
            "Jupiter" to 1,
            "Saturn" to 1,
            "Uranus" to 1,
            "Neptune" to 1,
            "Pluto" to 1
        )

        val fireSigns = setOf("Aries", "Leo", "Sagittarius")
        val earthSigns = setOf("Taurus", "Virgo", "Capricorn")
        val airSigns = setOf("Gemini", "Libra", "Aquarius")
        val waterSigns = setOf("Cancer", "Scorpio", "Pisces")

        val cardinalSigns = setOf("Aries", "Cancer", "Libra", "Capricorn")
        val fixedSigns = setOf("Taurus", "Leo", "Scorpio", "Aquarius")
        val mutableSigns = setOf("Gemini", "Virgo", "Sagittarius", "Pisces")

        var firePts = 0
        var earthPts = 0
        var airPts = 0
        var waterPts = 0

        var cardinalPts = 0
        var fixedPts = 0
        var mutablePts = 0

        var totalPts = 0

        for ((body, w) in weights) {
            val sign = planets[body]?.sign ?: continue
            totalPts += w
            if (sign in fireSigns) firePts += w
            if (sign in earthSigns) earthPts += w
            if (sign in airSigns) airPts += w
            if (sign in waterSigns) waterPts += w

            if (sign in cardinalSigns) cardinalPts += w
            if (sign in fixedSigns) fixedPts += w
            if (sign in mutableSigns) mutablePts += w
        }

        val total = max(1, totalPts)
        val firePct = round((firePts * 100.0) / total).toInt()
        val earthPct = round((earthPts * 100.0) / total).toInt()
        val airPct = round((airPts * 100.0) / total).toInt()
        val waterPct = round((waterPts * 100.0) / total).toInt()

        val cardPct = round((cardinalPts * 100.0) / total).toInt()
        val fixPct = round((fixedPts * 100.0) / total).toInt()
        val mutPct = round((mutablePts * 100.0) / total).toInt()

        val masculinePct = firePct + airPct
        val femininePct = earthPct + waterPct

        val dominantElement = listOf(
            "Fire" to firePct,
            "Earth" to earthPct,
            "Air" to airPct,
            "Water" to waterPct
        ).maxByOrNull { it.second }?.first ?: "Balanced"

        val dominantModality = listOf(
            "Cardinal" to cardPct,
            "Fixed" to fixPct,
            "Mutable" to mutPct
        ).maxByOrNull { it.second }?.first ?: "Balanced"

        val summary = "Dominant $dominantElement & $dominantModality: " + when (dominantElement) {
            "Fire" -> "Driven by passion, leadership, and spontaneous action."
            "Earth" -> "Grounded in practical achievement, patience, and tangible results."
            "Air" -> "Intellectually curious, socially agile, and idea-oriented."
            "Water" -> "Deeply intuitive, emotionally perceptive, and creatively empathetic."
            else -> "Harmoniously distributed personality energies."
        }

        return ElementalBalance(
            fire = firePct,
            earth = earthPct,
            air = airPct,
            water = waterPct,
            cardinal = cardPct,
            fixed = fixPct,
            mutable = mutPct,
            masculine = masculinePct,
            feminine = femininePct,
            summary = summary
        )
    }

    // Aspects with Textbook Orbs and Applying/Separating Detection
    private val MEAN_DAILY_SPEEDS = mapOf(
        "Moon" to 13.176,
        "Mercury" to 1.38,
        "Venus" to 1.20,
        "Sun" to 0.9856,
        "Mars" to 0.524,
        "Jupiter" to 0.083,
        "Saturn" to 0.033,
        "Uranus" to 0.011,
        "Neptune" to 0.006,
        "Pluto" to 0.004,
        "Chiron" to 0.020,
        "North Node" to -0.053,
        "Ascendant" to 360.0,
        "Midheaven" to 360.0
    )

    fun calculateAspectsDetailed(planets: Map<String, PlanetPosition>): List<AspectDetail> {
        val aspects = mutableListOf<AspectDetail>()
        val planetNames = planets.keys.filter { it !in listOf("Descendant", "Imum Coeli", "South Node", "Part of Fortune") }.toList()

        for (i in 0 until planetNames.size) {
            for (j in i + 1 until planetNames.size) {
                val p1 = planetNames[i]
                val p2 = planetNames[j]
                val pos1 = planets[p1]?.longitude ?: continue
                val pos2 = planets[p2]?.longitude ?: continue

                var diff = abs(pos1 - pos2)
                if (diff > 180.0) diff = 360.0 - diff

                val hasLuminary = (p1 == "Sun" || p1 == "Moon" || p2 == "Sun" || p2 == "Moon")
                val orbMax = if (hasLuminary) 10.0 else 8.0

                val v1 = MEAN_DAILY_SPEEDS[p1] ?: 0.5
                val v2 = MEAN_DAILY_SPEEDS[p2] ?: 0.5
                val (faster, slower, fastLon, slowLon) = if (v1 >= v2) {
                    Quadruple(p1, p2, pos1, pos2)
                } else {
                    Quadruple(p2, p1, pos2, pos1)
                }

                fun checkAspect(targetAngle: Double, allowableOrb: Double, name: String) {
                    val orb = abs(diff - targetAngle)
                    if (orb <= allowableOrb) {
                        var fastToSlow = normalize(slowLon - fastLon)
                        val isApplying = if (targetAngle == 0.0) {
                            fastToSlow > 180.0 // faster is catching up from behind
                        } else {
                            val currentDelta = if (fastToSlow <= 180.0) fastToSlow else 360.0 - fastToSlow
                            currentDelta < targetAngle
                        }
                        aspects.add(AspectDetail(faster, slower, name, orb, isApplying))
                    }
                }

                checkAspect(0.0, orbMax, "Conjunction")
                checkAspect(60.0, 6.0, "Sextile")
                checkAspect(90.0, orbMax, "Square")
                checkAspect(120.0, orbMax, "Trine")
                checkAspect(180.0, orbMax, "Opposition")
                checkAspect(150.0, 3.0, "Inconjunct")
                checkAspect(30.0, 2.0, "Semisextile")
            }
        }
        return aspects.sortedBy { it.orb }
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    data class Aspect(val planet1: String, val planet2: String, val aspectName: String, val orb: Double)

    fun calculateAspects(planets: Map<String, PlanetPosition>): List<Aspect> {
        return calculateAspectsDetailed(planets).map {
            Aspect(it.planet1, it.planet2, it.aspectName, it.orb)
        }
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
            val nodeLon = dailyPlanets["North Node"]?.longitude ?: 0.0

            var diff = moonLon - sunLon
            if (diff < 0) diff += 360.0

            if (i > 0) {
                // True Astronomical Node Proximity for Eclipses (within 18.5° of node)
                var distFromNode = abs(sunLon - nodeLon)
                if (distFromNode > 180.0) distFromNode = 360.0 - distFromNode
                val isNearNode = distFromNode <= 18.5 || abs(distFromNode - 180.0) <= 18.5

                if (prevPhase < 180 && diff >= 180) {
                    val moonSign = dailyPlanets["Moon"]?.sign ?: ""
                    if (isNearNode) {
                        events.add(AstroEvent("Total Lunar Eclipse in $moonSign", cal.timeInMillis, "A profound emotional reset, turning point, and karmic release under nodal alignment.", "ECLIPSE", "MAJOR"))
                    } else {
                        events.add(AstroEvent("Full Moon in $moonSign", cal.timeInMillis, "A powerful time for release, illumination, and culmination.", "LUNAR", "MINOR"))
                    }
                } else if (prevPhase in 250.0..359.0 && diff in 0.0..60.0) {
                    val sunSign = dailyPlanets["Sun"]?.sign ?: ""
                    if (isNearNode) {
                        events.add(AstroEvent("Solar Eclipse in $sunSign", cal.timeInMillis, "Fateful new beginnings and major cosmic doorways opening.", "ECLIPSE", "MAJOR"))
                    } else {
                        events.add(AstroEvent("New Moon in $sunSign", cal.timeInMillis, "A sacred moment for setting intentions and launching fresh cycles.", "LUNAR", "MINOR"))
                    }
                }

                // Retrograde / Direct stations
                for (p in trackerPlanets) {
                    val currLon = dailyPlanets[p]!!.longitude
                    val prevLon = prevLons[p] ?: currLon
                    var mDiff = currLon - prevLon
                    val normalizedDiff = if (mDiff < -180.0) mDiff + 360.0 else if (mDiff > 180.0) mDiff - 360.0 else mDiff

                    if (i > 1) {
                        val prevDiff = prevDiffs[p] ?: 1.0
                        if (prevDiff > 0 && normalizedDiff < 0) {
                            events.add(AstroEvent("$p turns Retrograde", cal.timeInMillis, "A cycle of review, reassessment, and internal realignment begins.", "PLANETARY", "MAJOR"))
                        } else if (prevDiff < 0 && normalizedDiff > 0) {
                            events.add(AstroEvent("$p turns Direct", cal.timeInMillis, "Forward momentum, clarity, and outward progress resume.", "PLANETARY", "MINOR"))
                        }
                    }
                    prevDiffs[p] = normalizedDiff
                }

                // Personal Transits & Milestone Returns
                if (natalPlanets != null) {
                    fun isOrb(curr: Double, nat: Double, maxOrb: Double = 1.0): Boolean {
                        var dO = abs(curr - nat)
                        if (dO > 180.0) dO = 360.0 - dO
                        return dO <= maxOrb
                    }

                    fun isAspect(curr: Double, nat: Double, targetAngle: Double, maxOrb: Double = 1.0): Boolean {
                        var dO = abs(curr - nat)
                        if (dO > 180.0) dO = 360.0 - dO
                        return abs(dO - targetAngle) <= maxOrb
                    }

                    val currentSun = dailyPlanets["Sun"]?.longitude ?: 0.0
                    val currentVenus = dailyPlanets["Venus"]?.longitude ?: 0.0
                    val currentMars = dailyPlanets["Mars"]?.longitude ?: 0.0
                    val currentJupiter = dailyPlanets["Jupiter"]?.longitude ?: 0.0
                    val currentSaturn = dailyPlanets["Saturn"]?.longitude ?: 0.0
                    val currentUranus = dailyPlanets["Uranus"]?.longitude ?: 0.0

                    val natalSun = natalPlanets["Sun"]?.longitude ?: 0.0
                    val natalMoon = natalPlanets["Moon"]?.longitude ?: 0.0
                    val natalVenus = natalPlanets["Venus"]?.longitude ?: 0.0
                    val natalJupiter = natalPlanets["Jupiter"]?.longitude ?: 0.0
                    val natalSaturn = natalPlanets["Saturn"]?.longitude ?: 0.0
                    val natalUranus = natalPlanets["Uranus"]?.longitude ?: 0.0

                    // Saturn Return (coming of age at 28-30 & maturity at 58-60)
                    if (isOrb(currentSaturn, natalSaturn, 1.2)) {
                        events.add(AstroEvent("Saturn Return (Exact Peak)", cal.timeInMillis, "A monumental milestone defining your life purpose, maturity, career foundation, and long-term destiny.", "PERSONAL", "MAJOR"))
                    }

                    // Jupiter Return (every ~12 years)
                    if (isOrb(currentJupiter, natalJupiter, 1.2)) {
                        events.add(AstroEvent("Jupiter Return", cal.timeInMillis, "A glorious new 12-year cycle of abundance, personal expansion, wisdom, and auspicious opportunities begins.", "PERSONAL", "MAJOR"))
                    }

                    // Uranus Opposition (~40-42 years)
                    if (isAspect(currentUranus, natalUranus, 180.0, 1.5)) {
                        events.add(AstroEvent("Uranus Opposition", cal.timeInMillis, "The profound midlife awakening of independence, breaking free from routine, and authentic self-reinvention.", "PERSONAL", "MAJOR"))
                    }

                    // Solar Return (Birthday!)
                    if (isOrb(currentSun, natalSun, 0.8)) {
                        events.add(AstroEvent("Solar Return (Birthday!)", cal.timeInMillis, "The Sun completes its orbit to your exact birth degree. Your personal cosmic New Year begins today.", "PERSONAL", "MAJOR"))
                    }

                    // Venus Return
                    if (isOrb(currentVenus, natalVenus, 1.0)) {
                        events.add(AstroEvent("Venus Return", cal.timeInMillis, "Harmonious renewal of personal charm, romantic attraction, artistic inspiration, and financial magnetism.", "PERSONAL", "MINOR"))
                    }

                    if (isOrb(currentMars, natalVenus, 1.0)) {
                        events.add(AstroEvent("Mars conjunct Natal Venus", cal.timeInMillis, "Passionate chemistry, creative ignition, and magnetic attraction are stimulated today.", "PERSONAL", "MINOR"))
                    }
                    if (isOrb(currentJupiter, natalSun, 1.0)) {
                        events.add(AstroEvent("Jupiter conjunct Natal Sun", cal.timeInMillis, "A cycle of expansive self-confidence, vital vitality, and good fortune.", "PERSONAL", "MAJOR"))
                    }
                    if (isOrb(currentJupiter, natalMoon, 1.0)) {
                        events.add(AstroEvent("Jupiter conjunct Natal Moon", cal.timeInMillis, "Deep domestic contentment, emotional peace, and heartwarming blessings.", "PERSONAL", "MAJOR"))
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
