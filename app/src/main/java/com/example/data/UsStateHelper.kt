package com.example.data

object UsStateHelper {
    val STATE_CODE_TO_NAME: Map<String, String> = mapOf(
        "AL" to "Alabama",
        "AK" to "Alaska",
        "AZ" to "Arizona",
        "AR" to "Arkansas",
        "CA" to "California",
        "CO" to "Colorado",
        "CT" to "Connecticut",
        "DE" to "Delaware",
        "FL" to "Florida",
        "GA" to "Georgia",
        "HI" to "Hawaii",
        "ID" to "Idaho",
        "IL" to "Illinois",
        "IN" to "Indiana",
        "IA" to "Iowa",
        "KS" to "Kansas",
        "KY" to "Kentucky",
        "LA" to "Louisiana",
        "ME" to "Maine",
        "MD" to "Maryland",
        "MA" to "Massachusetts",
        "MI" to "Michigan",
        "MN" to "Minnesota",
        "MS" to "Mississippi",
        "MO" to "Missouri",
        "MT" to "Montana",
        "NE" to "Nebraska",
        "NV" to "Nevada",
        "NH" to "New Hampshire",
        "NJ" to "New Jersey",
        "NM" to "New Mexico",
        "NY" to "New York",
        "NC" to "North Carolina",
        "ND" to "North Dakota",
        "OH" to "Ohio",
        "OK" to "Oklahoma",
        "OR" to "Oregon",
        "PA" to "Pennsylvania",
        "RI" to "Rhode Island",
        "SC" to "South Carolina",
        "SD" to "South Dakota",
        "TN" to "Tennessee",
        "TX" to "Texas",
        "UT" to "Utah",
        "VT" to "Vermont",
        "VA" to "Virginia",
        "WA" to "Washington",
        "WV" to "West Virginia",
        "WI" to "Wisconsin",
        "WY" to "Wyoming",
        "DC" to "District of Columbia",
        "PR" to "Puerto Rico",
        "GU" to "Guam",
        "VI" to "Virgin Islands"
    )

    val STATE_NAME_TO_CODE: Map<String, String> = STATE_CODE_TO_NAME.entries.associate { (code, name) ->
        name.lowercase() to code
    }

    /**
     * Resolves a state input (e.g. "tx", "TX", "texas", "Texas") to a Pair of (StateCode, StateName)
     */
    fun resolveState(input: String): Pair<String, String>? {
        val clean = input.trim().uppercase()
        if (STATE_CODE_TO_NAME.containsKey(clean)) {
            return clean to STATE_CODE_TO_NAME[clean]!!
        }
        val cleanLower = input.trim().lowercase()
        val codeFromName = STATE_NAME_TO_CODE[cleanLower]
        if (codeFromName != null) {
            return codeFromName to STATE_CODE_TO_NAME[codeFromName]!!
        }
        return null
    }

    data class ParsedLocationQuery(
        val cityQuery: String,
        val stateFilterCode: String? = null,
        val stateFilterName: String? = null
    )

    /**
     * Parses a query string like "Miami, FL" or "Seattle, Washington" or "Austin TX"
     * into city token and state filter.
     */
    fun parseQuery(query: String): ParsedLocationQuery {
        val clean = query.trim()
        if (clean.contains(",")) {
            val parts = clean.split(",").map { it.trim() }
            val city = parts.firstOrNull().orEmpty()
            val stateOrCountry = parts.getOrNull(1).orEmpty()
            val resolvedState = resolveState(stateOrCountry)
            if (resolvedState != null) {
                return ParsedLocationQuery(
                    cityQuery = city,
                    stateFilterCode = resolvedState.first,
                    stateFilterName = resolvedState.second
                )
            }
            return ParsedLocationQuery(cityQuery = city)
        }

        // Space separated check: e.g. "Dallas TX"
        val tokens = clean.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (tokens.size >= 2) {
            val lastToken = tokens.last()
            val resolvedLast = resolveState(lastToken)
            if (resolvedLast != null) {
                val cityPart = tokens.dropLast(1).joinToString(" ")
                return ParsedLocationQuery(
                    cityQuery = cityPart,
                    stateFilterCode = resolvedLast.first,
                    stateFilterName = resolvedLast.second
                )
            }
        }

        return ParsedLocationQuery(cityQuery = clean)
    }
}
