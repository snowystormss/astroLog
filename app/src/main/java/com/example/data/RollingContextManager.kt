package com.example.data

/**
 * 16k Rolling Context Window Manager.
 *
 * Enforces a strict 16,384 token ceiling designed for 8GB RAM devices.
 * Retains an immutable astrological system anchor (natal placements & coordinates)
 * while sliding conversation turns using a FIFO rolling buffer.
 */
class RollingContextManager(
    private val maxTokens: Int = 16384
) {
    companion object {
        const val DEFAULT_MAX_TOKENS = 16384
        private const val HEADROOM_TOKENS = 1200 // Buffer reserved for model generation
    }

    private var systemAnchor: String = ""
    private val conversationTurns = mutableListOf<ContextTurn>()

    data class ContextTurn(
        val role: String, // "user" or "assistant"
        val content: String,
        val estimatedTokens: Int
    )

    fun setSystemAnchor(anchorText: String) {
        systemAnchor = anchorText
    }

    fun getSystemAnchor(): String = systemAnchor

    /**
     * Appends a user or model message and prunes older turns if exceeding 16k ceiling.
     */
    @Synchronized
    fun addMessage(role: String, content: String) {
        val tokens = estimateTokens(content)
        conversationTurns.add(ContextTurn(role, content, tokens))
        pruneIfNecessary()
    }

    /**
     * Rebuilds the conversation history from a list of stored messages.
     */
    @Synchronized
    fun setHistory(messages: List<Pair<String, String>>) {
        conversationTurns.clear()
        messages.forEach { (role, content) ->
            conversationTurns.add(ContextTurn(role, content, estimateTokens(content)))
        }
        pruneIfNecessary()
    }

    @Synchronized
    fun clearHistory() {
        conversationTurns.clear()
    }

    /**
     * Builds the final prompt/context payload with the immutable system anchor + rolling turns.
     */
    @Synchronized
    fun buildContextPayload(): String {
        val builder = StringBuilder()
        if (systemAnchor.isNotBlank()) {
            builder.append("### SYSTEM ASTROLOGICAL ANCHOR:\n")
            builder.append(systemAnchor.trim())
            builder.append("\n\n")
        }

        if (conversationTurns.isNotEmpty()) {
            builder.append("### CONVERSATION HISTORY (16k Rolling Window):\n")
            conversationTurns.forEach { turn ->
                val speaker = if (turn.role == "user") "Seeker" else "Cosmic Guide"
                builder.append("$speaker: ${turn.content}\n\n")
            }
        }

        return builder.toString()
    }

    @Synchronized
    fun getRecentTurns(): List<ContextTurn> = conversationTurns.toList()

    @Synchronized
    fun getContextState(): RollingContextState {
        val anchorTokens = estimateTokens(systemAnchor)
        val turnsTokens = conversationTurns.sumOf { it.estimatedTokens }
        val total = anchorTokens + turnsTokens
        val pct = (total.toFloat() / maxTokens.toFloat()).coerceIn(0f, 1f)
        return RollingContextState(
            maxTokens = maxTokens,
            estimatedActiveTokens = total,
            systemAnchorTokens = anchorTokens,
            retainedTurnsCount = conversationTurns.size,
            capacityPercent = pct
        )
    }

    private fun pruneIfNecessary() {
        val anchorTokens = estimateTokens(systemAnchor)
        val headroom = kotlin.math.min(HEADROOM_TOKENS, (maxTokens * 0.15).toInt())
        val targetCeiling = maxTokens - headroom

        while (conversationTurns.isNotEmpty()) {
            val totalTokens = anchorTokens + conversationTurns.sumOf { it.estimatedTokens }
            if (totalTokens <= targetCeiling) {
                break
            }
            // Discard oldest message turn to keep memory within budget
            conversationTurns.removeAt(0)
        }
    }

    /**
     * Fast subword token estimation heuristic (~3.8 to 4 chars per token in English).
     */
    fun estimateTokens(text: String): Int {
        if (text.isEmpty()) return 0
        val chars = text.length
        val words = text.split("\\s+".toRegex()).size
        return ((chars / 4.0) + (words * 0.25)).toInt().coerceAtLeast(1)
    }
}
