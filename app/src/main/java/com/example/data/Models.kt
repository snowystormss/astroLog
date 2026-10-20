package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey
    val id: Int = 1, // Only one profile
    val name: String,
    val birthDate: String,
    val birthTime: String,
    val birthLocation: String,
    val sunSign: String = "",
    val moonSign: String = "",
    val risingSign: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val avatarUri: String? = null
)

@Entity(tableName = "astrology_insights")
data class AstrologyInsight(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val type: String, // e.g., "CHART_SUMMARY", "CHILDHOOD", "YOUNG_ADULTHOOD", "FUTURE"
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false
)

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = ChatSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index("sessionId")]
)
data class ChatMessageData(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val sessionId: Int,
    val content: String,
    val isUser: Boolean,
    val isError: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
@Entity(tableName = "saved_profiles")
data class SavedProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val birthDate: String,
    val birthTime: String,
    val birthLocation: String,
    val relationshipType: String, // "Friendship" or "Romantic"
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

@Entity(tableName = "astrology_event_notes")
data class AstrologyEventNote(
    @PrimaryKey val dateStr: String, // format yyyy-MM-dd
    val note: String
)

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey val id: Int = 1,
    val notifyRetrogrades: Boolean = true,
    val notifyMajorAspects: Boolean = true,
    val notifyLunarPhases: Boolean = true,
    val notifyPersonalTransits: Boolean = true,
    val enableAudioFeedback: Boolean = false,
    val dailyReminderEnabled: Boolean = false,
    val dailyReminderTime: String = "Morning", // "Morning" or "Night"
    val houseSystem: String = "WHOLE_SIGN" // "WHOLE_SIGN" or "PLACIDUS"
)

data class HouseCusp(
    val houseNumber: Int,
    val sign: String,
    val degree: Int,
    val minute: Int,
    val longitude: Double
)

data class ElementalBalance(
    val fire: Int,
    val earth: Int,
    val air: Int,
    val water: Int,
    val cardinal: Int,
    val fixed: Int,
    val mutable: Int,
    val masculine: Int,
    val feminine: Int,
    val summary: String
)

enum class DignityType {
    DOMICILE,
    EXALTATION,
    DETRIMENT,
    FALL,
    PEREGRINE
}

data class DetailedPlanetInfo(
    val name: String,
    val sign: String,
    val degree: Int,
    val minute: Int,
    val longitude: Double,
    val house: Int,
    val dignity: DignityType,
    val isRetrograde: Boolean = false
)

data class AspectDetail(
    val planet1: String,
    val planet2: String,
    val aspectName: String,
    val orb: Double,
    val isApplying: Boolean
)

data class DispositorResult(
    val dispositors: Map<String, String>,
    val mutualReceptions: List<Pair<String, String>>,
    val finalDispositor: String?
)

data class AnglesResult(
    val ascendant: com.example.ui.PlanetCalculator.PlanetPosition,
    val midheaven: com.example.ui.PlanetCalculator.PlanetPosition,
    val descendant: com.example.ui.PlanetCalculator.PlanetPosition,
    val imumCoeli: com.example.ui.PlanetCalculator.PlanetPosition
)

