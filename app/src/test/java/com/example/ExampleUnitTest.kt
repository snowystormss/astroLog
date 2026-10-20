package com.example

import com.example.data.DignityType
import com.example.ui.PlanetCalculator
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testPlanetaryCalculationsAndSigns() {
        val planets = PlanetCalculator.calculatePlanets(
            year = 1990, month = 7, day = 15, hour = 14.5,
            latitude = 40.7128, longitude = -74.0060
        )
        assertNotNull(planets["Sun"])
        assertNotNull(planets["Moon"])
        assertNotNull(planets["Ascendant"])
        assertNotNull(planets["Midheaven"])
        assertNotNull(planets["North Node"])
        assertNotNull(planets["South Node"])
        assertNotNull(planets["Chiron"])

        // In July, the Sun is in Cancer
        assertEquals("Cancer", planets["Sun"]?.sign)

        // North and South Nodes are diametrically opposed (180° apart)
        val nnLon = planets["North Node"]?.longitude ?: 0.0
        val snLon = planets["South Node"]?.longitude ?: 0.0
        var diff = kotlin.math.abs(nnLon - snLon)
        if (diff > 180.0) diff = 360.0 - diff
        assertEquals(180.0, diff, 0.01)
    }

    @Test
    fun testChartAnglesCalculation() {
        // Test standard coordinates
        val angles = PlanetCalculator.calculateAngles(
            year = 2000, month = 1, day = 1, hour = 12.0,
            latitude = 51.5074, longitude = 0.1278
        )
        assertNotNull(angles.ascendant)
        assertNotNull(angles.midheaven)
        assertNotNull(angles.descendant)
        assertNotNull(angles.imumCoeli)

        // Ascendant and Descendant must be 180° apart
        var ascDcDiff = kotlin.math.abs(angles.ascendant.longitude - angles.descendant.longitude)
        if (ascDcDiff > 180.0) ascDcDiff = 360.0 - ascDcDiff
        assertEquals(180.0, ascDcDiff, 0.01)

        // MC and IC must be 180° apart
        var mcIcDiff = kotlin.math.abs(angles.midheaven.longitude - angles.imumCoeli.longitude)
        if (mcIcDiff > 180.0) mcIcDiff = 360.0 - mcIcDiff
        assertEquals(180.0, mcIcDiff, 0.01)
    }

    @Test
    fun testWholeSignAndPlacidusHouses() {
        val angles = PlanetCalculator.calculateAngles(
            year = 1995, month = 10, day = 24, hour = 8.5,
            latitude = 34.0522, longitude = -118.2437
        )

        // 1. Whole Sign Cusps
        val wshCusps = PlanetCalculator.calculateHouseCusps(
            system = "WHOLE_SIGN",
            ascLon = angles.ascendant.longitude,
            mcLon = angles.midheaven.longitude,
            latitude = 34.0522,
            longitude = -118.2437
        )
        assertEquals(12, wshCusps.size)
        // House 1 starts at 0° of the rising sign
        assertEquals(0, wshCusps[0].degree)
        assertEquals(angles.ascendant.sign, wshCusps[0].sign)

        // 2. Placidus Cusps
        val placidusCusps = PlanetCalculator.calculateHouseCusps(
            system = "PLACIDUS",
            ascLon = angles.ascendant.longitude,
            mcLon = angles.midheaven.longitude,
            latitude = 34.0522,
            longitude = -118.2437
        )
        assertEquals(12, placidusCusps.size)
        // House 1 cusp must match Ascendant longitude
        assertEquals(angles.ascendant.longitude, placidusCusps[0].longitude, 0.01)
        // House 10 cusp must match Midheaven longitude
        assertEquals(angles.midheaven.longitude, placidusCusps[9].longitude, 0.01)
    }

    @Test
    fun testEssentialDignities() {
        // Sun rules Leo, exalted in Aries, detriment in Aquarius, fall in Libra
        assertEquals(DignityType.DOMICILE, PlanetCalculator.calculateDignity("Sun", "Leo"))
        assertEquals(DignityType.EXALTATION, PlanetCalculator.calculateDignity("Sun", "Aries"))
        assertEquals(DignityType.DETRIMENT, PlanetCalculator.calculateDignity("Sun", "Aquarius"))
        assertEquals(DignityType.FALL, PlanetCalculator.calculateDignity("Sun", "Libra"))
        assertEquals(DignityType.PEREGRINE, PlanetCalculator.calculateDignity("Sun", "Gemini"))

        // Moon rules Cancer, exalted in Taurus, detriment in Capricorn, fall in Scorpio
        assertEquals(DignityType.DOMICILE, PlanetCalculator.calculateDignity("Moon", "Cancer"))
        assertEquals(DignityType.EXALTATION, PlanetCalculator.calculateDignity("Moon", "Taurus"))
        assertEquals(DignityType.DETRIMENT, PlanetCalculator.calculateDignity("Moon", "Capricorn"))
        assertEquals(DignityType.FALL, PlanetCalculator.calculateDignity("Moon", "Scorpio"))

        // Mars rules Aries & Scorpio, exalted in Capricorn, detriment in Libra & Taurus, fall in Cancer
        assertEquals(DignityType.DOMICILE, PlanetCalculator.calculateDignity("Mars", "Aries"))
        assertEquals(DignityType.DOMICILE, PlanetCalculator.calculateDignity("Mars", "Scorpio"))
        assertEquals(DignityType.EXALTATION, PlanetCalculator.calculateDignity("Mars", "Capricorn"))
        assertEquals(DignityType.DETRIMENT, PlanetCalculator.calculateDignity("Mars", "Libra"))
        assertEquals(DignityType.FALL, PlanetCalculator.calculateDignity("Mars", "Cancer"))
    }

    @Test
    fun testElementalAndModalityScoring() {
        val planets = PlanetCalculator.calculatePlanets(
            year = 1988, month = 3, day = 21, hour = 6.0,
            latitude = 41.8781, longitude = -87.6298
        )
        val balance = PlanetCalculator.calculateElementalBalance(planets)

        // Triplicities sum must be approximately 100%
        val triplicitySum = balance.fire + balance.earth + balance.air + balance.water
        assertTrue(triplicitySum in 98..102)

        // Quadruplicities sum must be approximately 100%
        val modalitySum = balance.cardinal + balance.fixed + balance.mutable
        assertTrue(modalitySum in 98..102)

        assertNotNull(balance.summary)
        assertTrue(balance.summary.isNotBlank())
    }

    @Test
    fun testDispositorsAndAspects() {
        val planets = PlanetCalculator.calculatePlanets(
            year = 2005, month = 5, day = 12, hour = 18.0,
            latitude = 37.7749, longitude = -122.4194
        )

        val dispositorResult = PlanetCalculator.calculateDispositors(planets)
        assertNotNull(dispositorResult.dispositors)
        assertEquals(10, dispositorResult.dispositors.size) // 10 traditional planets

        val aspects = PlanetCalculator.calculateAspectsDetailed(planets)
        assertTrue(aspects.isNotEmpty())
        for (aspect in aspects) {
            assertTrue(aspect.orb >= 0.0)
            assertTrue(aspect.aspectName in listOf("Conjunction", "Sextile", "Square", "Trine", "Opposition", "Inconjunct", "Semisextile"))
        }
    }

    @Test
    fun testCitySearchService() {
        val resultsLos = com.example.data.CitySearchService.search("Los")
        assertTrue(resultsLos.isNotEmpty())
        assertEquals("Los Angeles", resultsLos.first().name)
        assertEquals("California", resultsLos.first().region)
        assertTrue(resultsLos.first().latitude > 34.0)

        val resultsParis = com.example.data.CitySearchService.search("Paris")
        assertTrue(resultsParis.isNotEmpty())
        assertEquals("Paris", resultsParis.first().name)
        assertEquals("France", resultsParis.first().country)

        val resultsTokyo = com.example.data.CitySearchService.search("Tokyo")
        assertTrue(resultsTokyo.isNotEmpty())
        assertEquals("Tokyo", resultsTokyo.first().name)
        assertTrue(resultsTokyo.first().latitude > 30.0)
        assertTrue(resultsTokyo.first().longitude > 130.0)

        val resultsMultiWord = com.example.data.CitySearchService.search("Miami Florida")
        assertTrue(resultsMultiWord.isNotEmpty())
        assertEquals("Miami", resultsMultiWord.first().name)

        val resultsEmpty = com.example.data.CitySearchService.search("")
        assertTrue(resultsEmpty.isEmpty())
        val resultsBlank = com.example.data.CitySearchService.search("   ")
        assertTrue(resultsBlank.isEmpty())

        // Coordinates display string
        val ny = resultsLos.first()
        assertTrue(ny.coordinatesDisplay.contains("N") || ny.coordinatesDisplay.contains("S"))
        assertTrue(ny.coordinatesDisplay.contains("W") || ny.coordinatesDisplay.contains("E"))
    }

    @Test
    fun testUsStateHelperAndAbbreviationSearch() {
        // Test state abbreviation lookup
        val resolvedTx = com.example.data.UsStateHelper.resolveState("TX")
        assertNotNull(resolvedTx)
        assertEquals("Texas", resolvedTx?.second)

        val resolvedCal = com.example.data.UsStateHelper.resolveState("california")
        assertNotNull(resolvedCal)
        assertEquals("CA", resolvedCal?.first)

        // Test query parser
        val parsed1 = com.example.data.UsStateHelper.parseQuery("Austin, TX")
        assertEquals("Austin", parsed1.cityQuery)
        assertEquals("TX", parsed1.stateFilterCode)

        val parsed2 = com.example.data.UsStateHelper.parseQuery("Seattle Washington")
        assertEquals("Seattle", parsed2.cityQuery)
        assertEquals("WA", parsed2.stateFilterCode)

        // Test abbreviation searches across the country
        val resultsAustinTx = com.example.data.CitySearchService.search("Austin, TX")
        assertTrue(resultsAustinTx.isNotEmpty())
        assertEquals("Austin", resultsAustinTx.first().name)
        assertEquals("TX", resultsAustinTx.first().stateCode)

        val resultsMiamiFl = com.example.data.CitySearchService.search("Miami, FL")
        assertTrue(resultsMiamiFl.isNotEmpty())
        assertEquals("Miami", resultsMiamiFl.first().name)
        assertEquals("FL", resultsMiamiFl.first().stateCode)

        val resultsDenverCo = com.example.data.CitySearchService.search("Denver, CO")
        assertTrue(resultsDenverCo.isNotEmpty())
        assertEquals("Denver", resultsDenverCo.first().name)
        assertEquals("CO", resultsDenverCo.first().stateCode)

        val resultsSeattleWa = com.example.data.CitySearchService.search("Seattle, WA")
        assertTrue(resultsSeattleWa.isNotEmpty())
        assertEquals("Seattle", resultsSeattleWa.first().name)
        assertEquals("WA", resultsSeattleWa.first().stateCode)

        // Disambiguation between cities with same name in different states
        val springfieldIl = com.example.data.CitySearchService.search("Springfield, IL")
        assertTrue(springfieldIl.isNotEmpty())
        assertEquals("Springfield", springfieldIl.first().name)
        assertEquals("IL", springfieldIl.first().stateCode)

        val springfieldMo = com.example.data.CitySearchService.search("Springfield, MO")
        assertTrue(springfieldMo.isNotEmpty())
        assertEquals("Springfield", springfieldMo.first().name)
        assertEquals("MO", springfieldMo.first().stateCode)

        // Full state name search
        val chicagoFull = com.example.data.CitySearchService.search("Chicago, Illinois")
        assertTrue(chicagoFull.isNotEmpty())
        assertEquals("Chicago", chicagoFull.first().name)
        assertEquals("Illinois", chicagoFull.first().region)

        // Test coverage of diverse US states (e.g. Alaska, Hawaii, Wyoming, Vermont, Maine)
        val anchorage = com.example.data.CitySearchService.search("Anchorage")
        assertTrue(anchorage.isNotEmpty())
        assertEquals("AK", anchorage.first().stateCode)

        val honolulu = com.example.data.CitySearchService.search("Honolulu")
        assertTrue(honolulu.isNotEmpty())
        assertEquals("HI", honolulu.first().stateCode)

        val cheyenne = com.example.data.CitySearchService.search("Cheyenne")
        assertTrue(cheyenne.isNotEmpty())
        assertEquals("WY", cheyenne.first().stateCode)

        val burlington = com.example.data.CitySearchService.search("Burlington, VT")
        assertTrue(burlington.isNotEmpty())
        assertEquals("VT", burlington.first().stateCode)

        // Instant findBestMatch resolution test
        val bestMatch = com.example.data.CitySearchService.findBestMatch("Houston, TX")
        assertNotNull(bestMatch)
        assertEquals("Houston", bestMatch?.name)
        assertEquals("Texas", bestMatch?.region)
        assertEquals("TX", bestMatch?.stateCode)
        assertTrue(bestMatch!!.latitude > 29.0)
    }
}
