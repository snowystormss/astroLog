package com.example.util

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.round

object TimezoneHelper {

    fun resolveZoneId(latitude: Double, longitude: Double, locationName: String = ""): ZoneId {
        val lower = locationName.lowercase()

        return when {
            // Eastern Time
            lower.contains(", ny") || lower.contains("new york") ||
            lower.contains(", fl") || lower.contains("florida") ||
            lower.contains(", ga") || lower.contains("georgia") ||
            lower.contains(", nc") || lower.contains("north carolina") ||
            lower.contains(", sc") || lower.contains("south carolina") ||
            lower.contains(", va") || lower.contains("virginia") ||
            lower.contains(", pa") || lower.contains("pennsylvania") ||
            lower.contains(", ma") || lower.contains("massachusetts") ||
            lower.contains(", nj") || lower.contains("new jersey") ||
            lower.contains(", oh") || lower.contains("ohio") ||
            lower.contains(", mi") || lower.contains("michigan") ||
            lower.contains(", dc") || lower.contains("washington dc") ||
            lower.contains(", md") || lower.contains("maryland") ||
            lower.contains(", ct") || lower.contains("connecticut") -> ZoneId.of("America/New_York")

            // Central Time
            lower.contains(", tx") || lower.contains("texas") ||
            lower.contains(", il") || lower.contains("illinois") ||
            lower.contains(", mo") || lower.contains("missouri") ||
            lower.contains(", mn") || lower.contains("minnesota") ||
            lower.contains(", wi") || lower.contains("wisconsin") ||
            lower.contains(", ia") || lower.contains("iowa") ||
            lower.contains(", la") || lower.contains("louisiana") ||
            lower.contains(", al") || lower.contains("alabama") ||
            lower.contains(", ms") || lower.contains("mississippi") ||
            lower.contains(", ar") || lower.contains("arkansas") ||
            lower.contains(", ok") || lower.contains("oklahoma") ||
            lower.contains(", ks") || lower.contains("kansas") ||
            lower.contains(", ne") || lower.contains("nebraska") ||
            lower.contains(", tn") || lower.contains("tennessee") -> ZoneId.of("America/Chicago")

            // Mountain Time
            lower.contains(", az") || lower.contains("arizona") -> ZoneId.of("America/Phoenix") // Arizona doesn't observe DST
            lower.contains(", co") || lower.contains("colorado") ||
            lower.contains(", ut") || lower.contains("utah") ||
            lower.contains(", nm") || lower.contains("new mexico") ||
            lower.contains(", id") || lower.contains("idaho") ||
            lower.contains(", mt") || lower.contains("montana") ||
            lower.contains(", wy") || lower.contains("wyoming") -> ZoneId.of("America/Denver")

            // Pacific Time
            lower.contains(", ca") || lower.contains("california") ||
            lower.contains(", wa") || lower.contains("washington") ||
            lower.contains(", or") || lower.contains("oregon") ||
            lower.contains(", nv") || lower.contains("nevada") -> ZoneId.of("America/Los_Angeles")

            // Alaska & Hawaii
            lower.contains(", ak") || lower.contains("alaska") -> ZoneId.of("America/Anchorage")
            lower.contains(", hi") || lower.contains("hawaii") -> ZoneId.of("Pacific/Honolulu")

            // Coordinates in United States roughly
            latitude in 24.0..50.0 && longitude in -125.0..-66.0 -> {
                when {
                    longitude >= -82.0 -> ZoneId.of("America/New_York")
                    longitude >= -104.0 -> ZoneId.of("America/Chicago")
                    longitude >= -115.0 -> {
                        if (latitude in 31.0..37.0 && longitude in -115.0..-109.0) ZoneId.of("America/Phoenix")
                        else ZoneId.of("America/Denver")
                    }
                    else -> ZoneId.of("America/Los_Angeles")
                }
            }

            // General world fallback based on standard longitude offset
            else -> {
                val offsetHours = round(longitude / 15.0).toInt().coerceIn(-18, 18)
                val sign = if (offsetHours >= 0) "+" else "-"
                val absH = kotlin.math.abs(offsetHours)
                try {
                    ZoneId.of("GMT$sign$absH")
                } catch (_: Exception) {
                    ZoneId.of("UTC")
                }
            }
        }
    }

    /**
     * Converts a local date and time into exact UTC hour (with decimal minutes)
     * properly accounting for historical Daylight Saving Time and standard offsets.
     */
    fun getUtcHour(
        year: Int,
        month: Int,
        day: Int,
        localHour: Double,
        latitude: Double,
        longitude: Double,
        locationName: String = ""
    ): Double {
        val h = localHour.toInt().coerceIn(0, 23)
        val m = ((localHour - h) * 60.0).toInt().coerceIn(0, 59)
        val d = day.coerceIn(1, 28)
        return try {
            val zoneId = resolveZoneId(latitude, longitude, locationName)
            val ldt = LocalDateTime.of(
                year.coerceIn(1900, 2150),
                month.coerceIn(1, 12),
                d,
                h,
                m
            )
            val zdt = ZonedDateTime.of(ldt, zoneId)
            val offsetHours = zdt.offset.totalSeconds / 3600.0
            var utc = localHour - offsetHours
            while (utc < 0.0) utc += 24.0
            while (utc >= 24.0) utc -= 24.0
            utc
        } catch (_: Exception) {
            val tzOffset = round(longitude / 15.0)
            var utc = localHour - tzOffset
            while (utc < 0.0) utc += 24.0
            while (utc >= 24.0) utc -= 24.0
            utc
        }
    }
}
