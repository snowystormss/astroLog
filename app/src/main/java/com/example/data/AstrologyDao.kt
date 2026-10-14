package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AstrologyDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getUserProfileSync(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfile)

    @Query("SELECT * FROM astrology_insights ORDER BY createdAt ASC")
    fun getAllInsights(): Flow<List<AstrologyInsight>>

    @Query("SELECT * FROM astrology_insights WHERE type = :type ORDER BY createdAt DESC LIMIT 1")
    fun getLatestInsightByType(type: String): Flow<AstrologyInsight?>

    @Query("SELECT * FROM astrology_insights WHERE type = :type ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestInsightSync(type: String): AstrologyInsight?

    @Query("SELECT * FROM astrology_insights WHERE type IN (:types) ORDER BY createdAt ASC")
    fun getInsightsByTypes(types: List<String>): Flow<List<AstrologyInsight>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsight(insight: AstrologyInsight)

    @Query("DELETE FROM astrology_insights")
    suspend fun clearInsights()

    @Query("DELETE FROM astrology_insights WHERE type = :type")
    suspend fun clearInsightsByType(type: String)

    @Query("SELECT * FROM saved_profiles")
    fun getAllSavedProfiles(): Flow<List<SavedProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedProfile(profile: SavedProfile): Long

    @Query("DELETE FROM saved_profiles WHERE id = :id")
    suspend fun deleteSavedProfile(id: Int)

    // Chat queries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatSession(session: ChatSession): Long

    @Query("UPDATE chat_sessions SET title = :newTitle, updatedAt = :time WHERE id = :sessionId")
    suspend fun updateChatSessionTitle(sessionId: Int, newTitle: String, time: Long)

    @Update
    suspend fun updateChatSession(session: ChatSession)

    @Query("UPDATE chat_sessions SET updatedAt = :time WHERE id = :sessionId")
    suspend fun updateChatSessionLastInteraction(sessionId: Int, time: Long)

    @Query("SELECT * FROM chat_sessions ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllChatSessions(): Flow<List<ChatSession>>

    @Query("""
        SELECT DISTINCT cs.* FROM chat_sessions cs
        LEFT JOIN chat_messages cm ON cs.id = cm.sessionId
        WHERE cs.title LIKE '%' || :query || '%' OR cm.content LIKE '%' || :query || '%'
        ORDER BY cs.isPinned DESC, cs.updatedAt DESC
    """)
    fun searchChatSessions(query: String): Flow<List<ChatSession>>

    @Query("SELECT * FROM chat_sessions")
    suspend fun getAllChatSessionsList(): List<ChatSession>

    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteChatSession(sessionId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageData)

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    fun getChatMessagesForSession(sessionId: Int): Flow<List<ChatMessageData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEventNote(note: AstrologyEventNote)

    @Query("SELECT * FROM astrology_event_notes WHERE dateStr = :dateStr")
    fun getEventNote(dateStr: String): Flow<AstrologyEventNote?>

    @Query("SELECT * FROM astrology_event_notes")
    fun getAllEventNotes(): Flow<List<AstrologyEventNote>>

    @Query("SELECT * FROM user_settings WHERE id = 1")
    suspend fun getUserSettingsSync(): UserSettings?

    @Query("SELECT * FROM user_settings WHERE id = 1")
    fun getUserSettings(): Flow<UserSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserSettings(settings: UserSettings)
}
