package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

import android.app.Application
import androidx.lifecycle.AndroidViewModel

data class ChatMessage(val content: String, val isUser: Boolean, val isError: Boolean = false)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("chat_prefs", android.content.Context.MODE_PRIVATE)

    private val _retentionPeriodMillis = MutableStateFlow(prefs.getLong("retention_millis", 7L * 24 * 60 * 60 * 1000))
    val retentionPeriodMillis: StateFlow<Long> = _retentionPeriodMillis.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setRetentionPeriod(millis: Long) {
        prefs.edit().putLong("retention_millis", millis).apply()
        _retentionPeriodMillis.value = millis
        viewModelScope.launch {
            repository.cleanUpOldSessions(millis)
        }
    }

    private val repository = AstrologyRepository(DatabaseProvider.database.astrologyDao())

    @OptIn(ExperimentalCoroutinesApi::class)
    val sessions: StateFlow<List<ChatSession>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) repository.getAllChatSessions() else repository.searchChatSessions(query)
        }
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentSessionId = MutableStateFlow<Int?>(null)
    val currentSessionId: StateFlow<Int?> = _currentSessionId.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentContext: String = ""
    private var chatHistory = mutableListOf<Content>()

    fun initContext(profile: UserProfile, insights: List<AstrologyInsight>) {
        val contextBuilder = StringBuilder()
        contextBuilder.append("You are an expert astrological guide within a personalized astrology app. You have access to the user's full astrological profile and their connections' details.\n\n")
        contextBuilder.append("User's Name: ${profile.name}\n")
        contextBuilder.append("User's Birth Info: ${profile.birthDate} ${profile.birthTime} at ${profile.birthLocation}\n\n")
        
        contextBuilder.append("--- PERSONAL INSIGHTS ---\n")
        insights.forEach { insight ->
            contextBuilder.append("${insight.type}:\n${insight.content}\n\n")
        }

        contextBuilder.append("Be friendly, empathetic, and speak in an open, natural, conversational manner. Offer true astrological guidance and explore possibilities with the user based on their chart data. Keep your responses reasonably concise. Do not output markdown lists detailing rules or internal thoughts. Just converse directly with the user.")

        currentContext = contextBuilder.toString()
        
        viewModelScope.launch {
            repository.cleanUpOldSessions(_retentionPeriodMillis.value)
        }
        
        if (_currentSessionId.value == null) {
            viewModelScope.launch {
                val dbSessions = repository.getAllChatSessions().firstOrNull() ?: emptyList()
                val latest = dbSessions.firstOrNull()
                if (latest != null) {
                    loadSession(latest.id)
                } else {
                    startNewSession()
                }
            }
        }
    }

    fun startNewSession() {
        viewModelScope.launch {
            val defaultTitle = "Cosmic Session"
            val newId = repository.createChatSession(defaultTitle).toInt()
            _currentSessionId.value = newId
            
            chatHistory.clear()
            val introMsg = ChatMessage("Hello! I am your personal cosmic guide. I have reviewed all of your astrological data and connections. How can I help you navigate your journey today?", isUser = false)
            _messages.value = listOf(introMsg)
            
            repository.saveChatMessage(newId, introMsg.content, isUser = false)
        }
    }

    fun loadSession(sessionId: Int) {
        viewModelScope.launch {
            _currentSessionId.value = sessionId
            val dbMessages = repository.getChatMessages(sessionId).firstOrNull() ?: emptyList()
            
            _messages.value = dbMessages.map { ChatMessage(it.content, it.isUser, it.isError) }
            
            chatHistory.clear()
            dbMessages.filter { !it.isError }.forEach { dbMsg ->
                val role = if (dbMsg.isUser) "user" else "model"
                chatHistory.add(Content(role = role, parts = listOf(Part(dbMsg.content))))
            }
        }
    }

    fun togglePinSession(session: ChatSession) {
        viewModelScope.launch {
            repository.togglePinSession(session)
        }
    }

    fun renameSession(sessionId: Int, newTitle: String) {
        viewModelScope.launch {
            repository.renameChatSession(sessionId, newTitle)
        }
    }

    fun deleteSession(sessionId: Int) {
        viewModelScope.launch {
            repository.deleteChatSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                _currentSessionId.value = null
                _messages.value = emptyList()
                chatHistory.clear()
                startNewSession()
            }
        }
    }

    suspend fun getFormattedChatHistoryForSession(sessionId: Int): String {
        val dbMessages = repository.getChatMessages(sessionId).firstOrNull() ?: emptyList()
        val buildStr = StringBuilder()
        dbMessages.forEach { msg ->
            if (msg.isUser) {
                buildStr.append("User:\n${msg.content}\n\n")
            } else {
                buildStr.append("AI:\n${msg.content}\n\n")
            }
        }
        return buildStr.toString().trim()
    }

    private fun generateLocalReply(userMessage: String): String {
        val query = userMessage.lowercase()
        val nameRegex = "User's Name: ([\\w\\s\\-]+)".toRegex()
        val name = nameRegex.find(currentContext)?.groupValues?.get(1) ?: "Cosmic Traveler"
        
        val sunRegex = "Sun Sign: ([\\w]+)".toRegex()
        val moonRegex = "Moon Sign: ([\\w]+)".toRegex()
        val venusRegex = "Venus Sign: ([\\w]+)".toRegex()
        val marsRegex = "Mars Sign: ([\\w]+)".toRegex()

        val sun = sunRegex.find(currentContext)?.groupValues?.get(1) ?: "the Sun"
        val moon = moonRegex.find(currentContext)?.groupValues?.get(1) ?: "the Moon"
        val venus = venusRegex.find(currentContext)?.groupValues?.get(1) ?: "Venus"
        val mars = marsRegex.find(currentContext)?.groupValues?.get(1) ?: "Mars"

        return when {
            query.contains("sun") || query.contains("identity") || query.contains("personality") -> {
                "Hello, $name. Your core self is aligned with the brilliant energy of the Sun in $sun. This fuels your vitality, primary life purpose, and authentic self-expression. In your journey, embracing this energy will bring solar clarity and help you shine as you are destined to."
            }
            query.contains("moon") || query.contains("feel") || query.contains("emotion") || query.contains("intuition") -> {
                "Dear $name, your emotional landscape is guided by your Moon in $moon. This placement shapes how you handle intuitive insights, seek safety, and nurture relationships. Trust your lunar cycles and treat your inner feelings with care."
            }
            query.contains("venus") || query.contains("love") || query.contains("relationship") || query.contains("romance") || query.contains("attract") -> {
                "For relationships and beauty, $name, your Venus is in $venus. This influences your romantic ideals, aesthetic pleasures, and what you naturally attract. You flourish in connections that respect this standard of harmony."
            }
            query.contains("mars") || query.contains("energy") || query.contains("drive") || query.contains("work") || query.contains("career") -> {
                "Your passion, courage, and physical drive are channeled through Mars in $mars. This tells us how you take action, pursue material ambitions, and face life's obstacles. Direct this powerful fire methodically to achieve your desires!"
            }
            query.contains("future") || query.contains("timing") || query.contains("tomorrow") || query.contains("happen") -> {
                "As the planets move across your natal coordinates, you are entering an important period of inward integration. Use this time not for forced progress, but for strengthening your roots and fine-tuning your goals with cosmic alignment."
            }
            query.contains("hello") || query.contains("hi") || query.contains("hey") -> {
                "Hello, $name! I am always here to assist you. Ask me anything about your Sun in $sun, Moon in $moon, Venus in $venus, or Mars in $mars placements."
            }
            else -> {
                "That is a profound astrological curiosity, $name. In reflection of your $sun Sun and $moon Moon, the planets suggest that this phase of your life demands mindful balancing. Always remain true to your internal light and let the cosmic forces guide you."
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        if (_isLoading.value) return
        val sessionId = _currentSessionId.value ?: return
        
        val userMsg = ChatMessage(text, isUser = true)
        _messages.value = _messages.value + userMsg
        
        viewModelScope.launch {
            repository.saveChatMessage(sessionId, text, isUser = true)
        }
        
        chatHistory.add(Content(role = "user", parts = listOf(Part(text))))

        _isLoading.value = true
        
        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "YOUR_GEMINI_API_KEY") {
                    throw IllegalStateException("API Key is missing")
                }
                val request = GenerateContentRequest(
                    contents = chatHistory.toList(),
                    generationConfig = GenerationConfig(temperature = 0.7f),
                    systemInstruction = Content(parts = listOf(Part(text = currentContext))),
                    tools = listOf(ToolItem(googleSearch = emptyMap()))
                )
                
                var response: com.example.data.GenerateContentResponse? = null
                try {
                    response = RetrofitClient.generateContentCached("gemini-3.1-flash-lite", apiKey, request)
                } catch (e: Exception) {
                    response = RetrofitClient.generateContentCached("gemma-4-31B-it", apiKey, request)
                }

                val reply = response?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                
                if (reply != null) {
                    _messages.value = _messages.value + ChatMessage(reply, isUser = false)
                    repository.saveChatMessage(sessionId, reply, isUser = false)
                    chatHistory.add(Content(role = "model", parts = listOf(Part(reply))))
                } else {
                    throw IllegalStateException("No candidate response")
                }
            } catch (e: Exception) {
                // Resilient local fallback so offline/non-activated users always receive an astrological reply
                val reply = generateLocalReply(text)
                _messages.value = _messages.value + ChatMessage(reply, isUser = false)
                repository.saveChatMessage(sessionId, reply, isUser = false)
                chatHistory.add(Content(role = "model", parts = listOf(Part(reply))))
            } finally {
                _isLoading.value = false
            }
        }
    }
}
