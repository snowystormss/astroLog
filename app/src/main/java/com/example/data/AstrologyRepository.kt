package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class AstrologyRepository(private val dao: AstrologyDao) {

    val userProfile: Flow<UserProfile?> = dao.getUserProfile()
    val insights: Flow<List<AstrologyInsight>> = dao.getAllInsights()

    fun getLatestInsightByType(type: String) = dao.getLatestInsightByType(type)
    fun getInsightsByTypes(types: List<String>) = dao.getInsightsByTypes(types)

    val savedProfiles: Flow<List<SavedProfile>> = dao.getAllSavedProfiles()

    suspend fun saveSavedProfileLocally(profile: SavedProfile): Long = withContext(Dispatchers.IO) {
        dao.insertSavedProfile(profile)
    }

    suspend fun deleteSavedProfile(id: Int) = withContext(Dispatchers.IO) {
        dao.deleteSavedProfile(id)
    }

    suspend fun saveProfileLocally(profile: UserProfile) {
        dao.insertProfile(profile)
    }

    suspend fun clearInsights() {
        dao.clearInsights()
    }

    suspend fun clearInsightByType(type: String) {
        dao.clearInsightsByType(type)
    }

    suspend fun saveInsightLocally(type: String, text: String) = withContext(Dispatchers.IO) {
        clearInsightByType(type)
        dao.insertInsight(AstrologyInsight(type = type, content = text))
    }

    suspend fun generateShortReading(prompt: String): String = withContext(Dispatchers.Default) {
        "Cosmic insights indicate that current planetary transits encourage mindful introspection and intentional action. Channel your authentic focus toward steady personal growth."
    }

    suspend fun generateAndSaveInsight(type: String, prompt: String, replace: Boolean = false) = withContext(Dispatchers.Default) {
        val title = when (type) {
            "LOVE" -> "Celestial Harmony"
            "CAREER" -> "Strategic Drive"
            "GROWTH" -> "Inward Clarity"
            else -> "Cosmic Alignment"
        }
        val advice = when (type) {
            "LOVE" -> "Nurture sincere emotional presence with loved ones and express gratitude."
            "CAREER" -> "Focus ambition systematically on high-impact priorities without rushing."
            "GROWTH" -> "Honor intuitive pauses to ground yourself amidst shifting planetary currents."
            else -> "Maintain equilibrium between outward action and internal reflection."
        }
        val deepDive = "Current planetary positions reflect a period of strengthening foundational roots. Direct your authentic vitality toward sustainable long-term fulfillment."

        val formatted = "TITLE: $title\nADVICE: $advice\nDEEP_DIVE: $deepDive"
        if (replace) {
            clearInsightByType(type)
        }
        dao.insertInsight(AstrologyInsight(type = type, content = formatted))
    }

    suspend fun createChatSession(title: String): Long = withContext(Dispatchers.IO) {
        dao.insertChatSession(ChatSession(title = title))
    }

    fun getAllChatSessions(): Flow<List<ChatSession>> = dao.getAllChatSessions()

    fun searchChatSessions(query: String): Flow<List<ChatSession>> = dao.searchChatSessions(query)

    suspend fun deleteChatSession(sessionId: Int) = withContext(Dispatchers.IO) {
        dao.deleteChatSession(sessionId)
    }

    suspend fun saveChatMessage(sessionId: Int, content: String, isUser: Boolean, isError: Boolean = false) = withContext(Dispatchers.IO) {
        dao.insertChatMessage(ChatMessageData(sessionId = sessionId, content = content, isUser = isUser, isError = isError))
        dao.updateChatSessionLastInteraction(sessionId = sessionId, time = System.currentTimeMillis())
    }

    suspend fun renameChatSession(sessionId: Int, newTitle: String) = withContext(Dispatchers.IO) {
        dao.updateChatSessionTitle(sessionId, newTitle, System.currentTimeMillis())
    }

    suspend fun togglePinSession(session: ChatSession) = withContext(Dispatchers.IO) {
        dao.updateChatSession(session.copy(isPinned = !session.isPinned))
    }

    suspend fun cleanUpOldSessions(retentionTimeMillis: Long) = withContext(Dispatchers.IO) {
        if (retentionTimeMillis > 0) {
            val cutoff = System.currentTimeMillis() - retentionTimeMillis
            val allSessions = dao.getAllChatSessionsList()
            for (session in allSessions) {
                if (!session.isPinned && session.updatedAt < cutoff) {
                    dao.deleteChatSession(session.id)
                }
            }
        }
    }

    fun getChatMessages(sessionId: Int): Flow<List<ChatMessageData>> = dao.getChatMessagesForSession(sessionId)

    suspend fun saveEventNote(dateStr: String, note: String) = withContext(Dispatchers.IO) {
        dao.insertEventNote(AstrologyEventNote(dateStr = dateStr, note = note))
    }

    fun getEventNote(dateStr: String): Flow<AstrologyEventNote?> = dao.getEventNote(dateStr)

    val allEventNotes: Flow<List<AstrologyEventNote>> = dao.getAllEventNotes()

    val userSettings: Flow<UserSettings?> = dao.getUserSettings()

    suspend fun saveUserSettings(settings: UserSettings) = withContext(Dispatchers.IO) {
        dao.insertUserSettings(settings)
    }
}
