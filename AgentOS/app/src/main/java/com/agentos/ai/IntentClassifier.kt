package com.agentos.ai

/**
 * Quick local intent classification before sending to Claude API.
 * Determines whether user input is conversational or action-oriented.
 */
class IntentClassifier {

    /**
     * Classify user input to determine interaction mode.
     */
    fun classify(input: String, conversationContext: List<String> = emptyList()): IntentResult {
        val lowerInput = input.lowercase().trim()

        // Check for explicit action confirmation
        if (isActionConfirmation(lowerInput, conversationContext)) {
            return IntentResult(
                intent = UserIntent.ACTION_CONFIRMATION,
                confidence = 0.95f,
                extractedAction = extractImpliedAction(conversationContext)
            )
        }

        // Check for explicit conversation indicators
        if (isConversational(lowerInput)) {
            return IntentResult(
                intent = UserIntent.CONVERSATION,
                confidence = calculateConversationConfidence(lowerInput),
                extractedAction = null
            )
        }

        // Check for explicit action indicators
        if (isAction(lowerInput)) {
            return IntentResult(
                intent = UserIntent.ACTION,
                confidence = calculateActionConfidence(lowerInput),
                extractedAction = lowerInput
            )
        }

        // Ambiguous - let Claude decide
        return IntentResult(
            intent = UserIntent.AMBIGUOUS,
            confidence = 0.5f,
            extractedAction = null
        )
    }

    private fun isActionConfirmation(input: String, context: List<String>): Boolean {
        val confirmationPhrases = listOf(
            "do it",
            "go ahead",
            "yes do it",
            "make it happen",
            "proceed",
            "yes please",
            "sure",
            "ok do it",
            "okay",
            "yep",
            "yeah",
            "yes",
            "execute",
            "run it",
            "start",
            "begin"
        )

        // Only consider as confirmation if there's prior context suggesting an action
        val hasPriorActionContext = context.any { msg ->
            msg.lowercase().let { m ->
                m.contains("would you like me to") ||
                m.contains("shall i") ||
                m.contains("i can") ||
                m.contains("do you want me to")
            }
        }

        return hasPriorActionContext && confirmationPhrases.any { input.startsWith(it) || input == it }
    }

    private fun isConversational(input: String): Boolean {
        val conversationalStarters = listOf(
            "what is",
            "what's",
            "what are",
            "how do i",
            "how does",
            "how can i",
            "how to",
            "why is",
            "why does",
            "why do",
            "when is",
            "when does",
            "where is",
            "where can",
            "who is",
            "who are",
            "can you explain",
            "explain",
            "tell me about",
            "tell me how",
            "describe",
            "what does",
            "should i",
            "is it possible",
            "can i",
            "help me understand"
        )

        return conversationalStarters.any { input.startsWith(it) }
    }

    private fun isAction(input: String): Boolean {
        val actionVerbs = listOf(
            "open",
            "launch",
            "start",
            "turn on",
            "turn off",
            "enable",
            "disable",
            "click",
            "tap",
            "press",
            "swipe",
            "scroll",
            "send",
            "share",
            "post",
            "set",
            "change",
            "switch",
            "go to",
            "navigate to",
            "find",
            "search for",
            "search",
            "play",
            "stop",
            "pause",
            "call",
            "text",
            "message",
            "email",
            "download",
            "install",
            "uninstall",
            "delete",
            "remove",
            "add",
            "create",
            "make",
            "take a",
            "show me",
            "close",
            "increase",
            "decrease",
            "lower",
            "raise",
            "mute",
            "unmute"
        )

        return actionVerbs.any { input.startsWith(it) }
    }

    private fun calculateConversationConfidence(input: String): Float {
        // Higher confidence for clear question patterns
        if (input.endsWith("?")) return 0.9f
        if (input.startsWith("what ") || input.startsWith("how ") ||
            input.startsWith("why ") || input.startsWith("explain")) return 0.85f
        return 0.7f
    }

    private fun calculateActionConfidence(input: String): Float {
        // Higher confidence for imperative commands
        val imperativeVerbs = listOf("open", "turn", "send", "click", "tap", "press")
        if (imperativeVerbs.any { input.startsWith(it) }) return 0.9f
        return 0.75f
    }

    private fun extractImpliedAction(context: List<String>): String? {
        // Look for the last assistant message that suggested an action
        return context.lastOrNull { msg ->
            msg.lowercase().let { m ->
                m.contains("would you like me to") ||
                m.contains("shall i") ||
                m.contains("i can help you")
            }
        }
    }
}

/**
 * Result of intent classification.
 */
data class IntentResult(
    val intent: UserIntent,
    val confidence: Float,
    val extractedAction: String?
)

/**
 * Types of user intents.
 */
enum class UserIntent {
    CONVERSATION,        // User wants to chat/ask questions
    ACTION,              // User wants to perform an action
    ACTION_CONFIRMATION, // User is confirming a previously suggested action
    AMBIGUOUS            // Intent is unclear, needs Claude to decide
}
