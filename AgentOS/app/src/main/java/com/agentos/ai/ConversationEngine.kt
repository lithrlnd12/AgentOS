package com.agentos.ai

import com.agentos.claude.ClaudeApiClient
import com.agentos.claude.ContentBlock
import com.agentos.claude.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Engine for handling conversational interactions.
 * Uses Claude API without tools for natural dialogue.
 */
class ConversationEngine(
    private val claudeClient: ClaudeApiClient
) {
    private val _state = MutableStateFlow<ConversationState>(ConversationState.Idle)
    val state: StateFlow<ConversationState> = _state.asStateFlow()

    private val conversationHistory = mutableListOf<Message>()

    /**
     * Send a conversational message and get a response.
     */
    suspend fun chat(
        userMessage: String,
        priorContext: List<Message> = emptyList()
    ): ConversationResponse {
        _state.value = ConversationState.Processing

        try {
            // Initialize with prior context if starting fresh
            if (conversationHistory.isEmpty() && priorContext.isNotEmpty()) {
                conversationHistory.addAll(priorContext)
            }

            // Add user message
            conversationHistory.add(Message.user(userMessage))

            // Send to Claude without tools (conversation only)
            val response = claudeClient.createMessage(
                messages = conversationHistory,
                systemPrompt = CONVERSATION_SYSTEM_PROMPT,
                tools = emptyList() // No tools for conversation
            )

            // Extract text response
            val textContent = response.content
                .filterIsInstance<ContentBlock.Text>()
                .joinToString("") { it.text }

            // Add assistant response to history
            conversationHistory.add(Message.assistant(textContent))

            // Analyze response for action suggestions
            val suggestedAction = extractActionSuggestion(textContent)

            _state.value = ConversationState.Idle

            return ConversationResponse(
                text = textContent,
                suggestsAction = suggestedAction != null,
                suggestedAction = suggestedAction
            )

        } catch (e: Exception) {
            _state.value = ConversationState.Error(e.message ?: "Unknown error")
            return ConversationResponse(
                text = "I'm sorry, I encountered an error: ${e.message}",
                suggestsAction = false,
                suggestedAction = null
            )
        }
    }

    /**
     * Get conversation history for handoff to action mode.
     */
    fun getConversationHistory(): List<Message> {
        return conversationHistory.toList()
    }

    /**
     * Clear conversation history.
     */
    fun clearHistory() {
        conversationHistory.clear()
    }

    /**
     * Check if the response suggests an action the user might want to perform.
     */
    private fun extractActionSuggestion(response: String): String? {
        val patterns = listOf(
            Regex("Would you like me to (.+?)\\?", RegexOption.IGNORE_CASE),
            Regex("I can (.+?) for you", RegexOption.IGNORE_CASE),
            Regex("Shall I (.+?)\\?", RegexOption.IGNORE_CASE),
            Regex("Do you want me to (.+?)\\?", RegexOption.IGNORE_CASE),
            Regex("I could (.+?) if you'd like", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val match = pattern.find(response)
            if (match != null) {
                return match.groupValues[1]
            }
        }

        return null
    }

    companion object {
        private val CONVERSATION_SYSTEM_PROMPT = """
            You are AgentOS, a helpful AI assistant running on an Android phone.

            CONTEXT:
            - You are on an Android device with full phone control capabilities
            - You can open apps, change settings, send messages, search, and more
            - The user is talking to you via voice or text

            CONVERSATION STYLE:
            - Be concise - keep responses under 3 sentences when possible
            - Be natural and conversational
            - Proactively offer to help when relevant

            ALWAYS OFFER TO HELP:
            When the user asks about doing something on the phone, ALWAYS:
            1. Briefly explain how (1-2 sentences max)
            2. Then offer: "Would you like me to do that for you?"

            Examples:
            - User: "How do I turn on WiFi?"
              You: "You can turn on WiFi in Settings under Network & internet. Would you like me to do that for you?"

            - User: "I need to check my battery"
              You: "I can open Settings to show your battery status. Would you like me to do that?"

            - User: "What's the weather like?"
              You: "I can search for the weather in your area. Would you like me to do that for you?"

            KEY BEHAVIORS:
            - Keep explanations SHORT - users prefer action over explanation
            - Always end phone-related questions with an offer to help
            - If user says "yes", "do it", "sure", "go ahead" - they want you to act
            - Be proactive: suggest related things ("I can also set up WiFi calling if you want")
        """.trimIndent()
    }
}

/**
 * Response from the conversation engine.
 */
data class ConversationResponse(
    val text: String,
    val suggestsAction: Boolean,
    val suggestedAction: String?
)

/**
 * State of the conversation engine.
 */
sealed class ConversationState {
    object Idle : ConversationState()
    object Processing : ConversationState()
    data class Error(val message: String) : ConversationState()
}
