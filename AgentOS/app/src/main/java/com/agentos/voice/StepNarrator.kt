package com.agentos.voice

import com.agentos.voice.realtime.RealtimeSession
import com.agentos.voice.realtime.RealtimeSessionState
import com.agentos.workflow.WorkflowState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Narrates workflow steps via voice when in voice mode.
 * Maps workflow actions to natural speech.
 */
class StepNarrator(
    private val realtimeSession: RealtimeSession?
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var lastNarratedStep: String? = null
    private var isVoiceMode = false

    /**
     * Enable voice narration.
     */
    fun enableVoiceMode() {
        isVoiceMode = true
    }

    /**
     * Disable voice narration.
     */
    fun disableVoiceMode() {
        isVoiceMode = false
    }

    /**
     * Observe workflow state and narrate changes.
     */
    fun observeWorkflow(workflowState: StateFlow<WorkflowState>) {
        scope.launch {
            workflowState.collect { state ->
                handleStateChange(state)
            }
        }
    }

    private suspend fun handleStateChange(state: WorkflowState) {
        when (state) {
            is WorkflowState.Running -> {
                val narration = generateNarration(state.description)
                if (narration != lastNarratedStep) {
                    lastNarratedStep = narration
                    narrate(narration)
                }
            }
            is WorkflowState.NeedsClarification -> {
                narrate(state.question)
                lastNarratedStep = null
            }
            is WorkflowState.Completed -> {
                narrate("Done. ${summarizeResult(state.result)}")
                lastNarratedStep = null
            }
            is WorkflowState.Error -> {
                narrate("I encountered an error. ${state.message}")
                lastNarratedStep = null
            }
            is WorkflowState.Idle -> {
                lastNarratedStep = null
            }
        }
    }

    /**
     * Generate natural speech from workflow step description.
     */
    private fun generateNarration(description: String): String {
        val lower = description.lowercase()

        return when {
            // Tap actions
            lower.contains("tap") && lower.contains("settings") ->
                "Opening Settings"
            lower.contains("tap") && lower.contains("wifi") ->
                "Tapping on Wi-Fi"
            lower.contains("tap") && lower.contains("bluetooth") ->
                "Tapping on Bluetooth"
            lower.contains("tap") && lower.contains("toggle") ||
            lower.contains("tap") && lower.contains("switch") ->
                "Toggling the setting"
            lower.startsWith("executing: tap") ->
                "Tapping on the screen"

            // Swipe actions
            lower.contains("swipe") && lower.contains("down") ||
            lower.contains("scroll") && lower.contains("down") ->
                "Scrolling down"
            lower.contains("swipe") && lower.contains("up") ||
            lower.contains("scroll") && lower.contains("up") ->
                "Scrolling up"
            lower.startsWith("executing: swipe") ->
                "Swiping on the screen"

            // Type actions
            lower.contains("type") || lower.contains("enter") ||
            lower.contains("input") ->
                extractTypingNarration(description)

            // Navigation
            lower.contains("press_back") || lower.contains("back button") ->
                "Going back"
            lower.contains("press_home") || lower.contains("home button") ->
                "Going to home screen"
            lower.contains("open") ->
                "Opening ${extractAppName(description)}"

            // Analysis
            lower.contains("analyz") || lower.contains("screen") ->
                "Looking at the screen"
            lower.contains("planning") || lower.contains("thinking") ->
                "Thinking about the next step"

            // Default
            else -> description.removePrefix("Executing: ").trim()
        }
    }

    private fun extractTypingNarration(description: String): String {
        // Extract what's being typed
        val textPattern = Regex("type[d]?[:\\s]+[\"']?([^\"']+)[\"']?", RegexOption.IGNORE_CASE)
        val match = textPattern.find(description)

        return if (match != null) {
            val text = match.groupValues[1].take(20) // Limit to first 20 chars
            if (text.length < match.groupValues[1].length) {
                "Typing \"$text\"..."
            } else {
                "Typing \"$text\""
            }
        } else {
            "Typing text"
        }
    }

    private fun extractAppName(description: String): String {
        val openPattern = Regex("open[ing]?\\s+([\\w\\s]+)", RegexOption.IGNORE_CASE)
        val match = openPattern.find(description)
        return match?.groupValues?.getOrNull(1)?.trim() ?: "the app"
    }

    private fun summarizeResult(result: String): String {
        return when {
            result.length <= 50 -> result
            else -> result.take(47) + "..."
        }
    }

    /**
     * Speak the narration text.
     */
    private suspend fun narrate(text: String) {
        if (!isVoiceMode) return

        // Check if realtime session is ready for TTS
        val session = realtimeSession ?: return

        if (session.state.value is RealtimeSessionState.Ready ||
            session.state.value is RealtimeSessionState.AssistantSpeaking) {
            // Send text for TTS
            session.sendText(text)

            // Brief delay to prevent overwhelming
            delay(500)
        }
    }

    /**
     * Clean up resources.
     */
    fun close() {
        scope.cancel()
    }
}

/**
 * Data class for a narration event.
 */
data class NarrationEvent(
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
