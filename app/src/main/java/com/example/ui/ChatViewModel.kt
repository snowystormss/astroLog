package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private val _rollingContextState = MutableStateFlow(hostManager.rollingContext.getContextState())
    val rollingContextState: StateFlow<RollingContextState> = _rollingContextState.asStateFlow()

    private var currentContext: String = ""

    fun initContext(profile: UserProfile, insights: List<AstrologyInsight>) {
        val contextBuilder = StringBuilder()
        contextBuilder.append("User's Name: ${profile.name}\n")
        contextBuilder.append("User's Birth Info: ${profile.birthDate} ${profile.birthTime} at ${profile.birthLocation}\n\n")

        val sun = insights.find { it.type == "SUN" }?.content
        val moon = insights.find { it.type == "MOON" }?.content
        val rising = insights.find { it.type == "RISING" }?.content
        val venus = insights.find { it.type == "VENUS" }?.content
        val mars = insights.find { it.type == "MARS" }?.content

        if (sun != null) contextBuilder.append("Sun Sign: $sun\n")
        if (moon != null) contextBuilder.append("Moon Sign: $moon\n")
        if (rising != null) contextBuilder.append("Rising Sign: $rising\n")
        if (venus != null) contextBuilder.append("Venus Sign: $venus\n")
        if (mars != null) contextBuilder.append("Mars Sign: $mars\n")

        contextBuilder.append("\n--- PERSONAL INSIGHTS ---\n")
        insights.forEach { insight ->
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

            val introMsg = ChatMessage(
                "Hello! I am your 100% on-device Cosmic Guide. How can I help you navigate your chart today?",
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
                hostManager.rollingContext.clearHistory()
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
        if (text.isBlank()) return
        if (_isLoading.value) return
        val sessionId = _currentSessionId.value ?: return

        // Check if model is mounted
        if (!isModelMounted.value) {
            val errorMsg = ChatMessage(
                "A 4B or smaller on-device model (.gguf or .bin) must be mounted to generate astrological interpretations. Please tap 'Upload / Mount Model' above or in Settings to load your model from storage or Google Drive.",
                isUser = false,
                isError = true
            )
            _messages.value = _messages.value + errorMsg
            return
        }

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
            try {
                // Initialize assistant placeholder message for streaming
                var streamedText = ""
                val streamingMessageIndex = _messages.value.size
                _messages.value = _messages.value + ChatMessage("", isUser = false)

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
                val errorReply = "On-Device Inference Error: ${e.message}"
                _messages.value = _messages.value + ChatMessage(errorReply, isUser = false, isError = true)
                repository.saveChatMessage(sessionId, errorReply, isUser = false)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
