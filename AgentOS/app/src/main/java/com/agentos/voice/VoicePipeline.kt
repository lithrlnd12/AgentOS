package com.agentos.voice

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import com.agentos.claude.ClaudeApiClient
import com.agentos.workflow.WorkflowEngine
import com.agentos.workflow.WorkflowState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages the complete voice pipeline:
 * 1. Wake word detection ("AgentOS")
 * 2. Command extraction
 * 3. Workflow execution
 * 4. Voice feedback via TTS
 */
class VoicePipeline(
    private val context: Context,
    private val workflowEngine: WorkflowEngine,
    private val wakeWordDetector: WakeWordDetector,
    private val ttsEngine: TextToSpeechEngine
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val _state = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val state: StateFlow<VoiceState> = _state.asStateFlow()

    private val _isEnabled = MutableStateFlow(false)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private var listeningJob: Job? = null
    private var workflowJob: Job? = null

    init {
        // Observe workflow state changes for voice feedback
        workflowJob = scope.launch {
            workflowEngine.state.collect { state ->
                if (_isEnabled.value) {
                    provideWorkflowFeedback(state)
                }
            }
        }
    }

    private fun provideWorkflowFeedback(state: WorkflowState) {
        when (state) {
            is WorkflowState.Running -> {
                // Speak step descriptions
                val shortDescription = shortenDescription(state.description)
                ttsEngine.speak(shortDescription)
            }
            is WorkflowState.Completed -> {
                ttsEngine.speakImmediately("Done. ${shortenDescription(state.result)}")
            }
            is WorkflowState.Error -> {
                ttsEngine.speakImmediately("Error: ${state.message}")
            }
            is WorkflowState.NeedsClarification -> {
                ttsEngine.speakImmediately(state.question)
            }
            is WorkflowState.Idle -> {
                // Don't speak anything
            }
        }
    }

    private fun shortenDescription(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("tap") -> "Tapping"
            lower.contains("swipe") || lower.contains("scroll") -> "Scrolling"
            lower.contains("type") -> "Typing"
            lower.contains("back") -> "Going back"
            lower.contains("home") -> "Going home"
            lower.contains("wait") -> "Waiting"
            lower.contains("analyz") -> "Looking at screen"
            text.length > 50 -> text.take(47) + "..."
            else -> text
        }
    }

    /**
     * Enable voice activation with wake word detection.
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun enableVoiceActivation() {
        if (_isEnabled.value) return

        _isEnabled.value = true
        _state.value = VoiceState.WaitingForWakeWord

        ttsEngine.speak("Voice activated. Say AgentOS followed by your command.")

        listeningJob = scope.launch {
            wakeWordDetector.startListening().collect { result ->
                handleWakeWordResult(result)
            }
        }
    }

    /**
     * Disable voice activation.
     */
    fun disableVoiceActivation() {
        _isEnabled.value = false
        _state.value = VoiceState.Idle
        listeningJob?.cancel()
        listeningJob = null
        wakeWordDetector.stopListening()
        ttsEngine.speak("Voice deactivated.")
    }

    private suspend fun handleWakeWordResult(result: WakeWordResult) {
        when (result) {
            is WakeWordResult.Listening -> {
                _state.value = VoiceState.WaitingForWakeWord
            }

            is WakeWordResult.AudioLevel -> {
                // Could update UI with audio level visualization
            }

            is WakeWordResult.PartialResult -> {
                _state.value = VoiceState.Listening(result.text)
            }

            is WakeWordResult.CommandDetected -> {
                _state.value = VoiceState.Processing(result.command)

                // Confirm command reception
                ttsEngine.speak("Got it. ${result.command}")

                // Execute the command through the workflow engine
                scope.launch {
                    workflowEngine.execute(result.command)
                }

                _state.value = VoiceState.WaitingForWakeWord
            }

            is WakeWordResult.NoWakeWord -> {
                // User spoke but didn't say the wake word
                _state.value = VoiceState.WaitingForWakeWord
            }

            is WakeWordResult.Error -> {
                _state.value = VoiceState.Error(result.message)
                // Recover after a moment
                _state.value = VoiceState.WaitingForWakeWord
            }
        }
    }

    /**
     * Process a command directly (bypassing wake word detection).
     * Used when user types or uses push-to-talk.
     */
    suspend fun processCommand(command: String) {
        _state.value = VoiceState.Processing(command)
        workflowEngine.execute(command)
        _state.value = if (_isEnabled.value) VoiceState.WaitingForWakeWord else VoiceState.Idle
    }
}

sealed class VoiceState {
    object Idle : VoiceState()
    object WaitingForWakeWord : VoiceState()
    data class Listening(val partialText: String) : VoiceState()
    data class Processing(val command: String) : VoiceState()
    data class Error(val message: String) : VoiceState()
}
