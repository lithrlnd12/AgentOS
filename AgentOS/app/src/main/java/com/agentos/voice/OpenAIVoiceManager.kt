package com.agentos.voice

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import com.agentos.voice.realtime.*
import com.agentos.workflow.WorkflowEngine
import com.agentos.workflow.WorkflowState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Manages voice interaction using OpenAI Realtime API.
 * Handles bidirectional voice with Whisper transcription and natural TTS.
 */
class OpenAIVoiceManager(
    private val context: Context,
    private val apiKey: String,
    private val workflowEngine: WorkflowEngine
) {
    companion object {
        private const val TAG = "OpenAIVoiceManager"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var realtimeClient: RealtimeWebSocketClient? = null
    private var audioRecorder: AudioRecorder? = null
    private var audioPlayer: AudioPlayer? = null

    private var recordingJob: Job? = null
    private var eventJob: Job? = null
    private var workflowJob: Job? = null

    private val _state = MutableStateFlow<VoiceManagerState>(VoiceManagerState.Idle)
    val state: StateFlow<VoiceManagerState> = _state.asStateFlow()

    private val _transcripts = MutableSharedFlow<TranscriptUpdate>()
    val transcripts: SharedFlow<TranscriptUpdate> = _transcripts.asSharedFlow()

    private val _isEnabled = MutableStateFlow(false)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    // Conversation history for context
    private val conversationHistory = mutableListOf<ConversationTurn>()

    /**
     * Start voice mode with OpenAI Realtime API.
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start() {
        Log.d(TAG, "start() called, isEnabled=${_isEnabled.value}")
        if (_isEnabled.value) {
            Log.d(TAG, "Already enabled, returning")
            return
        }

        _isEnabled.value = true
        _state.value = VoiceManagerState.Connecting
        Log.d(TAG, "Initializing voice components...")

        // Initialize components
        audioRecorder = AudioRecorder()
        audioPlayer = AudioPlayer().apply {
            initialize()  // Explicitly initialize AudioPlayer for playback
        }
        Log.d(TAG, "Audio recorder and player initialized")

        Log.d(TAG, "Creating RealtimeWebSocketClient with apiKey length: ${apiKey.length}")
        realtimeClient = RealtimeWebSocketClient(apiKey).also { client ->
            // Observe events
            eventJob = scope.launch {
                client.events.collect { event ->
                    handleServerEvent(event)
                }
            }

            // Observe connection state
            scope.launch {
                client.connectionState.collect { connState ->
                    when (connState) {
                        ConnectionState.CONNECTED -> {
                            configureSession()
                        }
                        ConnectionState.ERROR -> {
                            _state.value = VoiceManagerState.Error("Connection failed")
                        }
                        ConnectionState.DISCONNECTED -> {
                            if (_isEnabled.value) {
                                _state.value = VoiceManagerState.Idle
                            }
                        }
                        else -> {}
                    }
                }
            }

            client.connect()
        }

        // Observe workflow state for voice narration
        workflowJob = scope.launch {
            workflowEngine.state.collect { state ->
                if (_isEnabled.value && _state.value is VoiceManagerState.Ready) {
                    narrateWorkflowState(state)
                }
            }
        }
    }

    /**
     * Stop voice mode.
     */
    fun stop() {
        _isEnabled.value = false
        _state.value = VoiceManagerState.Idle

        recordingJob?.cancel()
        eventJob?.cancel()
        workflowJob?.cancel()

        audioRecorder?.stopRecording()
        audioPlayer?.stop()

        realtimeClient?.close()

        audioRecorder = null
        audioPlayer = null
        realtimeClient = null
    }

    /**
     * Interrupt current speech (when user starts talking).
     */
    fun interrupt() {
        audioPlayer?.stop()
        realtimeClient?.cancelResponse()
        realtimeClient?.clearAudio()
    }

    /**
     * Send a text message (for typed input while in voice mode).
     */
    fun sendText(text: String) {
        conversationHistory.add(ConversationTurn(role = "user", content = text))
        realtimeClient?.sendText(text)
        realtimeClient?.requestResponse()
    }

    private fun configureSession() {
        val instructions = """
            You are AgentOS, a voice assistant that controls Android phones.

            STYLE:
            - Be very concise (1-2 sentences max)
            - Natural conversational tone
            - Don't repeat what the user said

            WHEN USER REQUESTS AN ACTION:
            - If they ask to open an app, change a setting, or do something on the phone, DO IT immediately
            - Say what you're doing briefly, then include the action tag
            - Format: "Opening settings now. [ACTION: Open Settings app]"
            - Format: "Turning on WiFi. [ACTION: Open Settings, go to WiFi, turn on WiFi]"

            EXAMPLES:
            - User: "Open settings" → "Opening settings. [ACTION: Open Settings app]"
            - User: "Turn on WiFi" → "Turning on WiFi. [ACTION: Open Settings, navigate to WiFi, enable WiFi]"
            - User: "Open Chrome" → "Opening Chrome. [ACTION: Open Chrome browser]"

            CONVERSATION:
            - For questions, just answer briefly
            - Don't ask for confirmation on simple actions
            - Only ask clarification if truly ambiguous
        """.trimIndent()

        realtimeClient?.configureSession(
            modalities = listOf("text", "audio"),
            instructions = instructions,
            voice = "alloy", // Options: alloy, echo, fable, onyx, nova, shimmer
            tools = emptyList() // We handle tools separately through WorkflowEngine
        )
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun startRecording() {
        recordingJob?.cancel()
        recordingJob = scope.launch {
            audioRecorder?.startRecording()?.collect { audioChunk ->
                realtimeClient?.sendAudio(audioChunk)
            }
        }
    }

    private fun stopRecording() {
        recordingJob?.cancel()
        audioRecorder?.stopRecording()
    }

    private suspend fun handleServerEvent(event: ServerEvent) {
        Log.d(TAG, "Handling event: ${event.javaClass.simpleName}")
        when (event) {
            is SessionCreatedEvent -> {
                Log.d(TAG, "Session created, starting recording...")
                _state.value = VoiceManagerState.Ready
                // Start recording once session is ready
                startRecording()
            }

            is SessionUpdatedEvent -> {
                _state.value = VoiceManagerState.Ready
            }

            is SpeechStartedEvent -> {
                // User started speaking - interrupt any assistant speech
                _state.value = VoiceManagerState.UserSpeaking
                audioPlayer?.stop()
            }

            is SpeechStoppedEvent -> {
                // User stopped speaking
                _state.value = VoiceManagerState.Processing
                Log.d(TAG, "Speech stopped, committing audio and requesting response")

                // Explicitly commit audio and request response as fallback
                // This ensures a response is triggered even if server_vad doesn't auto-respond
                realtimeClient?.commitAudio()
                realtimeClient?.requestResponse()
            }

            is TranscriptionCompletedEvent -> {
                // User's speech transcribed by Whisper
                val transcript = event.transcript
                conversationHistory.add(ConversationTurn(role = "user", content = transcript))
                _transcripts.emit(TranscriptUpdate(
                    text = transcript,
                    isUser = true,
                    isFinal = true
                ))
            }

            is ResponseCreatedEvent -> {
                _state.value = VoiceManagerState.Processing
            }

            is ResponseAudioDeltaEvent -> {
                // Assistant audio chunk - play it
                // Stop recording while assistant speaks to prevent echo feedback
                if (_state.value != VoiceManagerState.AssistantSpeaking) {
                    stopRecording()
                    Log.d(TAG, "Paused recording - assistant speaking")
                }
                _state.value = VoiceManagerState.AssistantSpeaking
                val audioBytes = android.util.Base64.decode(event.delta, android.util.Base64.DEFAULT)
                audioPlayer?.playChunk(audioBytes)
            }

            is ResponseAudioTranscriptDeltaEvent -> {
                // Assistant speech transcript (streaming)
                _transcripts.emit(TranscriptUpdate(
                    text = event.delta,
                    isUser = false,
                    isFinal = false
                ))

                // Check for action markers in audio transcript too
                checkForActionMarker(event.delta)
            }

            is ResponseTextDeltaEvent -> {
                // Text response (if no audio)
                _transcripts.emit(TranscriptUpdate(
                    text = event.delta,
                    isUser = false,
                    isFinal = false
                ))

                // Check for action markers
                checkForActionMarker(event.delta)
            }

            is ResponseDoneEvent -> {
                _state.value = VoiceManagerState.Ready

                // Get full response text (check both text and transcript for audio responses)
                val content = event.response?.output?.firstOrNull()?.content?.firstOrNull()
                val fullResponse = content?.text ?: content?.transcript
                if (fullResponse != null) {
                    conversationHistory.add(ConversationTurn(role = "assistant", content = fullResponse))
                    _transcripts.emit(TranscriptUpdate(
                        text = fullResponse,
                        isUser = false,
                        isFinal = true
                    ))

                    // Check for action markers in full response
                    checkForActionMarker(fullResponse)
                    Log.d(TAG, "Full response: $fullResponse")
                }

                // Resume recording after assistant finishes speaking
                // Clear any buffered audio first to prevent echo
                realtimeClient?.clearAudio()
                Log.d(TAG, "Response done - resuming recording")
                @Suppress("MissingPermission") // Permission already granted in start()
                startRecording()
            }

            is TranscriptionFailedEvent -> {
                Log.e(TAG, "Transcription failed: ${event.error.type} - ${event.error.code} - ${event.error.message}")
                // Transcription failed but we can still continue
            }

            is ErrorEvent -> {
                Log.e(TAG, "Error event: ${event.error.type} - ${event.error.code} - ${event.error.message}")
                _state.value = VoiceManagerState.Error(event.error.message)
                // Try to recover
                delay(1000)
                if (_isEnabled.value) {
                    _state.value = VoiceManagerState.Ready
                }
            }

            else -> {
                // Other events
            }
        }
    }

    private fun checkForActionMarker(text: String) {
        // Check if assistant wants to perform an action
        val actionPattern = Regex("\\[ACTION:\\s*(.+?)\\]", RegexOption.IGNORE_CASE)
        val match = actionPattern.find(text)

        if (match != null) {
            val actionDescription = match.groupValues[1]
            Log.d(TAG, "ACTION DETECTED: $actionDescription")
            scope.launch {
                // Execute the action through workflow engine
                Log.d(TAG, "Executing action via WorkflowEngine: $actionDescription")
                workflowEngine.execute(actionDescription)
            }
        }
    }

    private suspend fun narrateWorkflowState(state: WorkflowState) {
        val narration = when (state) {
            is WorkflowState.Running -> {
                // Short narration for steps
                shortenForVoice(state.description)
            }
            is WorkflowState.Completed -> {
                "Done. ${shortenForVoice(state.result)}"
            }
            is WorkflowState.Error -> {
                "I encountered an error. ${state.message}"
            }
            is WorkflowState.NeedsClarification -> {
                state.question
            }
            is WorkflowState.Idle -> null
        }

        if (narration != null && _state.value is VoiceManagerState.Ready) {
            // Send narration to be spoken
            realtimeClient?.sendText("[Narration: $narration]")
            realtimeClient?.requestResponse(listOf("audio")) // Audio only for narration
        }
    }

    private fun shortenForVoice(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("tap") -> "Tapping"
            lower.contains("swipe") || lower.contains("scroll") -> "Scrolling"
            lower.contains("type") -> "Typing"
            lower.contains("back") -> "Going back"
            lower.contains("home") -> "Going home"
            text.length > 30 -> text.take(27) + "..."
            else -> text
        }
    }
}

/**
 * States for the voice manager.
 */
sealed class VoiceManagerState {
    object Idle : VoiceManagerState()
    object Connecting : VoiceManagerState()
    object Ready : VoiceManagerState()
    object UserSpeaking : VoiceManagerState()
    object Processing : VoiceManagerState()
    object AssistantSpeaking : VoiceManagerState()
    data class Error(val message: String) : VoiceManagerState()
}

/**
 * Transcript update event.
 */
data class TranscriptUpdate(
    val text: String,
    val isUser: Boolean,
    val isFinal: Boolean
)

/**
 * Conversation turn for history.
 */
data class ConversationTurn(
    val role: String, // "user" or "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
