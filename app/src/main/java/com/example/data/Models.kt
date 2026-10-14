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
    val dailyReminderTime: String = "Morning" // "Morning" or "Night"
)
