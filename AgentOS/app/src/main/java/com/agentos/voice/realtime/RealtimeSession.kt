package com.agentos.voice.realtime

import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.agentos.voice.AudioPlayer
import org.json.JSONObject

/**
 * High-level session manager for OpenAI Realtime API.
 * Coordinates between WebSocket client, audio capture, and playback.
 */
class RealtimeSession(
    private val apiKey: String,
    private val audioPlayer: AudioPlayer
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var client: RealtimeWebSocketClient? = null

    private val _state = MutableStateFlow<RealtimeSessionState>(RealtimeSessionState.Idle)
    val state: StateFlow<RealtimeSessionState> = _state.asStateFlow()

    private val _transcripts = MutableSharedFlow<TranscriptEvent>()
    val transcripts: SharedFlow<TranscriptEvent> = _transcripts.asSharedFlow()

    private val _functionCalls = MutableSharedFlow<FunctionCallEvent>()
    val functionCalls: SharedFlow<FunctionCallEvent> = _functionCalls.asSharedFlow()

    private var currentTranscript = StringBuilder()
    private var currentFunctionArgs = StringBuilder()
    private var currentCallId: String? = null
    private var currentFunctionName: String? = null

    /**
     * Start a realtime session.
     */
    fun start(
        instructions: String? = null,
        voice: String = "alloy",
        tools: List<RealtimeTool> = emptyList()
    ) {
        if (client != null) return

        client = RealtimeWebSocketClient(apiKey).also { ws ->
            // Observe events
            scope.launch {
                ws.events.collect { event ->
                    handleEvent(event)
                }
            }

            // Observe connection state
            scope.launch {
                ws.connectionState.collect { connState ->
                    when (connState) {
                        ConnectionState.CONNECTED -> {
                            ws.configureSession(
                                modalities = listOf("text", "audio"),
                                instructions = instructions,
                                voice = voice,
                                tools = tools
                            )
                        }
                        ConnectionState.ERROR -> {
                            _state.value = RealtimeSessionState.Error("Connection failed")
                        }
                        ConnectionState.DISCONNECTED -> {
                            if (_state.value !is RealtimeSessionState.Idle) {
                                _state.value = RealtimeSessionState.Idle
                            }
                        }
                        else -> {}
                    }
                }
            }

            ws.connect()
        }
    }

    /**
     * Stop the session.
     */
    fun stop() {
        audioPlayer.stop()
        client?.close()
        client = null
        _state.value = RealtimeSessionState.Idle
    }

    /**
     * Send audio data from microphone.
     */
    fun sendAudioChunk(audioData: ByteArray) {
        client?.sendAudio(audioData)
    }

    /**
     * Signal end of speech (for manual VAD).
     */
    fun endSpeech() {
        client?.commitAudio()
    }

    /**
     * Send a text message.
     */
    fun sendText(text: String) {
        client?.sendText(text)
        client?.requestResponse()
    }

    /**
     * Interrupt the current response (when user starts speaking).
     */
    fun interrupt() {
        audioPlayer.stop()
        client?.cancelResponse()
        client?.clearAudio()
    }

    /**
     * Provide a function call result.
     */
    fun provideFunctionResult(callId: String, result: String) {
        // Send function result as a conversation item
        // This would need additional message type implementation
    }

    private fun handleEvent(event: ServerEvent) {
        when (event) {
            is SessionCreatedEvent -> {
                _state.value = RealtimeSessionState.Ready
            }

            is SessionUpdatedEvent -> {
                _state.value = RealtimeSessionState.Ready
            }

            is SpeechStartedEvent -> {
                _state.value = RealtimeSessionState.UserSpeaking
                // Interrupt any ongoing playback
                audioPlayer.stop()
            }

            is SpeechStoppedEvent -> {
                _state.value = RealtimeSessionState.Processing
            }

            is ResponseCreatedEvent -> {
                _state.value = RealtimeSessionState.Processing
                currentTranscript.clear()
            }

            is ResponseAudioDeltaEvent -> {
                _state.value = RealtimeSessionState.AssistantSpeaking
                // Decode and play audio
                val audioBytes = Base64.decode(event.delta, Base64.DEFAULT)
                audioPlayer.playChunk(audioBytes)
            }

            is ResponseAudioTranscriptDeltaEvent -> {
                currentTranscript.append(event.delta)
                scope.launch {
                    _transcripts.emit(TranscriptEvent.Delta(
                        text = event.delta,
                        isAssistant = true
                    ))
                }
            }

            is ResponseTextDeltaEvent -> {
                currentTranscript.append(event.delta)
                scope.launch {
                    _transcripts.emit(TranscriptEvent.Delta(
                        text = event.delta,
                        isAssistant = true
                    ))
                }
            }

            is FunctionCallArgumentsDeltaEvent -> {
                currentFunctionArgs.append(event.delta)
                currentCallId = event.callId
            }

            is FunctionCallArgumentsDoneEvent -> {
                val name = currentFunctionName ?: "unknown"
                scope.launch {
                    _functionCalls.emit(FunctionCallEvent(
                        callId = event.callId,
                        name = name,
                        arguments = event.arguments
                    ))
                }
                currentFunctionArgs.clear()
                currentFunctionName = null
            }

            is ResponseDoneEvent -> {
                _state.value = RealtimeSessionState.Ready
                scope.launch {
                    _transcripts.emit(TranscriptEvent.Complete(
                        text = currentTranscript.toString(),
                        isAssistant = true
                    ))
                }
            }

            is ErrorEvent -> {
                _state.value = RealtimeSessionState.Error(event.error.message)
            }

            is ConversationItemCreatedEvent -> {
                // Track function name from item if present
                if (event.item.type == "function_call") {
                    // Would need to parse function name from item
                }
            }

            else -> {
                // Ignore other events
            }
        }
    }

    fun close() {
        stop()
        scope.cancel()
    }
}

/**
 * States for the realtime session.
 */
sealed class RealtimeSessionState {
    object Idle : RealtimeSessionState()
    object Connecting : RealtimeSessionState()
    object Ready : RealtimeSessionState()
    object UserSpeaking : RealtimeSessionState()
    object Processing : RealtimeSessionState()
    object AssistantSpeaking : RealtimeSessionState()
    data class Error(val message: String) : RealtimeSessionState()
}

/**
 * Events for transcript updates.
 */
sealed class TranscriptEvent {
    data class Delta(val text: String, val isAssistant: Boolean) : TranscriptEvent()
    data class Complete(val text: String, val isAssistant: Boolean) : TranscriptEvent()
}

/**
 * Event for function calls from the model.
 */
data class FunctionCallEvent(
    val callId: String,
    val name: String,
    val arguments: String // JSON string
)
