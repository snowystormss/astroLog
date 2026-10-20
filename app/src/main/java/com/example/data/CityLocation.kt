package com.example.data

data class CityLocation(
    val name: String,
    val region: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val stateCode: String = "",
    val priority: Int = 5
) {
    val formatted: String
        get() = if (stateCode.isNotBlank() && (country == "United States" || country == "USA")) {
            "$name, $region, United States"
        } else if (region.isNotBlank()) {
            "$name, $region, $country"
        } else {
            "$name, $country"
        }

    val displaySubtitle: String
        get() = if (stateCode.isNotBlank() && (country == "United States" || country == "USA")) {
            "$region ($stateCode), USA"
        } else if (region.isNotBlank()) {
            "$region, $country"
        } else {
            country
        }

    val coordinatesDisplay: String
        get() {
            val latDir = if (latitude >= 0) "N" else "S"
            val lonDir = if (longitude >= 0) "E" else "W"
            return "${"%.2f".format(kotlin.math.abs(latitude))}°$latDir, ${"%.2f".format(kotlin.math.abs(longitude))}°$lonDir"
        }
}

object CitySearchService {
    private val CITIES: List<CityLocation> by lazy {
        UsCitiesData.ALL_CITIES
    }

    /**
     * Fast token-based search supporting:
     * - "Austin, TX" / "Austin TX" / "Austin, Texas"
     * - "Dallas" (returns Dallas, TX before smaller Dallases)
     * - "Springfield, IL" vs "Springfield, MO"
     * - Full state name and postal abbreviation two-way resolution
     */
    fun search(query: String, limit: Int = 6): List<CityLocation> {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return emptyList()

        val parsed = UsStateHelper.parseQuery(cleanQuery)
        val cityQueryLower = parsed.cityQuery.lowercase().trim()
        val stateFilterCode = parsed.stateFilterCode
        val tokens = cleanQuery.lowercase().split(Regex("[,\\s]+")).filter { it.isNotEmpty() }

        if (cityQueryLower.isEmpty() && stateFilterCode == null) return emptyList()

        return CITIES
            .mapNotNull { city ->
                val nameLower = city.name.lowercase()
                val regionLower = city.region.lowercase()
                val stateCodeUpper = city.stateCode.uppercase()
                val countryLower = city.country.lowercase()

                var score = 0

                // If explicit state filter was detected (e.g. "FL", "Texas", "CA")
                if (stateFilterCode != null) {
                    val stateMatches = stateCodeUpper == stateFilterCode || 
                                       regionLower == parsed.stateFilterName?.lowercase()

                    if (stateMatches) {
                        score += 80
                    } else if (city.country == "United States") {
                        // User specified a different state, so exclude non-matching states
                        return@mapNotNull null
                    }

                    if (cityQueryLower.isNotEmpty()) {
                        if (nameLower == cityQueryLower) {
                            score += 200
                        } else if (nameLower.startsWith(cityQueryLower)) {
                            score += 120
                        } else if (nameLower.contains(cityQueryLower)) {
                            score += 60
                        } else {
                            // City name doesn't match
                            return@mapNotNull null
                        }
                    }
                } else {
                    // General multi-token search
                    val primaryToken = tokens.first()

                    if (nameLower == primaryToken) {
                        score += 150
                    } else if (nameLower.startsWith(primaryToken)) {
                        score += 100
                    } else if (nameLower.contains(primaryToken)) {
                        score += 50
                    }

                    if (tokens.size > 1) {
                        val secondaryTokens = tokens.drop(1)
                        var secondaryMatched = 0
                        for (token in secondaryTokens) {
                            val upperToken = token.uppercase()
                            if (stateCodeUpper == upperToken) {
                                score += 90
                                secondaryMatched++
                            } else if (regionLower.startsWith(token) || regionLower.contains(token)) {
                                score += 60
                                secondaryMatched++
                            } else if (countryLower.startsWith(token)) {
                                score += 30
                                secondaryMatched++
                            } else if (nameLower.contains(token)) {
                                score += 30
                                secondaryMatched++
                            }
                        }
                        if (secondaryMatched == 0 && score < 100) {
                            score -= 30
                        }
                    } else {
                        // If single token matches a state name/code
                        val upperPrimary = primaryToken.uppercase()
                        if (stateCodeUpper == upperPrimary) score += 40
                        if (regionLower.startsWith(primaryToken)) score += 30
                    }
                }

                // Add population priority weighting (1-10) to break ties nicely
                if (score > 0) {
                    score += city.priority * 5
                    city to score
                } else {
                    null
                }
            }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    /**
     * Resolves the best exact or highest-confidence match for a query string.
     * Useful for instant offline resolution during form submission.
     */
    fun findBestMatch(query: String): CityLocation? {
        val matches = search(query, limit = 1)
        return matches.firstOrNull()
    }
}
