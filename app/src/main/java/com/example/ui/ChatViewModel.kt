package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatMessage(val content: String, val isUser: Boolean, val isError: Boolean = false)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("chat_prefs", android.content.Context.MODE_PRIVATE)
    private val hostManager = LocalModelHostManager.getInstance(application)

    val isModelMounted: StateFlow<Boolean> = hostManager.isModelMounted
    val hostedModel: StateFlow<HostedModelInfo?> = hostManager.hostedModel

    private val _retentionPeriodMillis = MutableStateFlow(prefs.getLong("retention_millis", 7L * 24 * 60 * 60 * 1000))
    val retentionPeriodMillis: StateFlow<Long> = _retentionPeriodMillis.asStateFlow()

    fun setRetentionPeriod(millis: Long) {
        _retentionPeriodMillis.value = millis
        prefs.edit().putLong("retention_millis", millis).apply()
        viewModelScope.launch {
            repository.cleanUpOldSessions(millis)
        }
    }

    private val repository = AstrologyRepository(DatabaseProvider.database.astrologyDao())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val sessions: StateFlow<List<ChatSession>> = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            repository.getAllChatSessions()
        } else {
            repository.searchChatSessions(query)
        }
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentSessionId = MutableStateFlow<Int?>(null)
    val currentSessionId: StateFlow<Int?> = _currentSessionId.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _rollingContextState = MutableStateFlow(hostManager.rollingContext.getContextState())
    val rollingContextState: StateFlow<RollingContextState> = _rollingContextState.asStateFlow()

    private var currentContext: String = ""

    fun initContext(profile: UserProfile, insights: List<AstrologyInsight>) {
        val contextBuilder = StringBuilder()
        contextBuilder.append("User's Name: ${profile.name.ifBlank { "Seeker" }}\n")
        contextBuilder.append("User's Birth Info: ${profile.birthDate} ${profile.birthTime} at ${profile.birthLocation}\n")

        // fix finding 3: pull signs directly from user profile instead of missing insight rows
        val sun = profile.sunSign.ifBlank { "Sun" }
        val moon = profile.moonSign.ifBlank { "Moon" }
        val rising = profile.risingSign.ifBlank { "Ascendant" }

        contextBuilder.append("Sun Sign: $sun\n")
        contextBuilder.append("Moon Sign: $moon\n")
        contextBuilder.append("Rising Sign: $rising\n")

        val venus = insights.find { it.type == "VENUS" }?.content?.lines()?.firstOrNull()?.replace("TITLE:", "")?.trim()
        val mars = insights.find { it.type == "MARS" }?.content?.lines()?.firstOrNull()?.replace("TITLE:", "")?.trim()
        if (!venus.isNullOrBlank()) contextBuilder.append("Venus Sign: $venus\n")
        if (!mars.isNullOrBlank()) contextBuilder.append("Mars Sign: $mars\n")

        contextBuilder.append("\n--- PERSONAL INSIGHTS ---\n")
        insights.filter { it.type !in listOf("SUN", "MOON", "RISING") }.forEach { insight ->
            contextBuilder.append("${insight.type}:\n${insight.content}\n\n")
        }

        currentContext = contextBuilder.toString()
        hostManager.rollingContext.setSystemAnchor(currentContext)
        _rollingContextState.value = hostManager.rollingContext.getContextState()

        viewModelScope.launch {
            repository.cleanUpOldSessions(_retentionPeriodMillis.value)
        }

        if (_currentSessionId.value == null) {
            viewModelScope.launch {
                val dbSessions = repository.getAllChatSessions().firstOrNull() ?: emptyList()
                if (dbSessions.isNotEmpty()) {
                    loadSession(dbSessions.first().id)
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

            val introMsg = ChatMessage(
                "Hello! I am your on-device Cosmic Guide. How can I help you navigate your chart today?",
                isUser = false
            )
            _messages.value = listOf(introMsg)
            repository.saveChatMessage(newId, introMsg.content, isUser = false)

            hostManager.rollingContext.clearHistory()
            hostManager.rollingContext.addMessage("assistant", introMsg.content)
            _rollingContextState.value = hostManager.rollingContext.getContextState()
        }
    }

    fun loadSession(sessionId: Int) {
        viewModelScope.launch {
            _currentSessionId.value = sessionId
            val dbMessages = repository.getChatMessages(sessionId).firstOrNull() ?: emptyList()

            _messages.value = dbMessages.map { ChatMessage(it.content, it.isUser, it.isError) }

            val turns = dbMessages.filter { !it.isError }.map {
                val role = if (it.isUser) "user" else "assistant"
                role to it.content
            }
            hostManager.rollingContext.setHistory(turns)
            _rollingContextState.value = hostManager.rollingContext.getContextState()
        }
    }

    fun renameSession(sessionId: Int, newTitle: String) {
        viewModelScope.launch {
            repository.renameChatSession(sessionId, newTitle)
        }
    }

    fun togglePinSession(session: ChatSession) {
        viewModelScope.launch {
            repository.togglePinSession(session)
        }
    }

    fun deleteSession(sessionId: Int) {
        viewModelScope.launch {
            repository.deleteChatSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                _currentSessionId.value = null
                _messages.value = emptyList()
                hostManager.rollingContext.clearHistory()
                startNewSession()
            }
        }
    }

    suspend fun getFormattedChatHistoryForSession(sessionId: Int): String {
        val msgs = repository.getChatMessages(sessionId).firstOrNull() ?: emptyList()
        val buildStr = StringBuilder()
        msgs.forEach { m ->
            val speaker = if (m.isUser) "You" else "Cosmic Guide"
            buildStr.append("$speaker: ${m.content}\n\n")
        }
        return buildStr.toString().trim()
    }

    fun mountModelFromUri(uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = hostManager.mountModelFromUri(uri)
            result.onSuccess { model ->
                onResult(true, "Mounted ${model.fileName}")
            }.onFailure { err ->
                onResult(false, err.message ?: "Failed to mount model")
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _isLoading.value) return
        val sessionId = _currentSessionId.value ?: return

        val userMsg = ChatMessage(text, isUser = true)
        _messages.value = _messages.value + userMsg

        viewModelScope.launch {
            repository.saveChatMessage(sessionId, text, isUser = true)
        }

        val recentTurns = _messages.value.filter { !it.isError }.map {
            (if (it.isUser) "user" else "assistant") to it.content
        }

        _isLoading.value = true

        viewModelScope.launch {
            var streamingMessageIndex = -1
            try {
                streamingMessageIndex = _messages.value.size
                _messages.value = _messages.value + ChatMessage("", isUser = false)

                var streamedText = ""
                hostManager.executeInferenceStream(
                    prompt = text,
                    systemAnchor = currentContext,
                    recentTurns = recentTurns
                ).collect { chunk ->
                    streamedText += chunk
                    val currentList = _messages.value.toMutableList()
                    if (streamingMessageIndex in currentList.indices) {
                        currentList[streamingMessageIndex] = ChatMessage(streamedText, isUser = false)
                        _messages.value = currentList
                    }
                }

                repository.saveChatMessage(sessionId, streamedText, isUser = false)
                _rollingContextState.value = hostManager.rollingContext.getContextState()
            } catch (e: Exception) {
                // fix finding 7: overwrite the placeholder slot instead of appending an extra empty message
                val currentList = _messages.value.toMutableList()
                val errorReply = "Astrological Synthesis Notice: ${e.message ?: "Inference error occurred."}"
                if (streamingMessageIndex in currentList.indices) {
                    currentList[streamingMessageIndex] = ChatMessage(errorReply, isUser = false, isError = true)
                    _messages.value = currentList
                } else {
                    _messages.value = _messages.value + ChatMessage(errorReply, isUser = false, isError = true)
                }
                repository.saveChatMessage(sessionId, errorReply, isUser = false, isError = true)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
