package com.agentos.voice.realtime

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * WebSocket client for OpenAI Realtime API.
 * Provides bidirectional audio streaming with low latency.
 */
class RealtimeWebSocketClient(
    private val apiKey: String,
    private val okHttpClient: OkHttpClient = createDefaultClient()
) {
    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _events = Channel<ServerEvent>(Channel.BUFFERED)
    val events: Flow<ServerEvent> = _events.receiveAsFlow()

    private var sessionId: String? = null

    /**
     * Connect to the OpenAI Realtime API.
     * Model history:
     * - gpt-4o-realtime-preview-2024-10-01: deprecated Sept 2025
     * - gpt-4o-realtime-preview-2024-12-17: stable preview
     * - gpt-4o-mini-realtime-preview-2024-12-17: mini version
     */
    fun connect(model: String = "gpt-4o-mini-realtime-preview-2024-12-17") {
        if (_connectionState.value == ConnectionState.CONNECTED ||
            _connectionState.value == ConnectionState.CONNECTING) {
            Log.d(TAG, "Already connected or connecting, skipping")
            return
        }

        _connectionState.value = ConnectionState.CONNECTING
        Log.d(TAG, "Connecting to OpenAI Realtime API...")

        val url = "wss://api.openai.com/v1/realtime?model=$model"
        Log.d(TAG, "URL: $url")

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("OpenAI-Beta", "realtime=v1")
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected!")
                _connectionState.value = ConnectionState.CONNECTED
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                scope.launch {
                    try {
                        val json = JSONObject(text)
                        val eventType = json.optString("type", "unknown")
                        Log.d(TAG, "Received event: $eventType")

                        // Log full response for debugging
                        if (eventType == "response.done" || eventType == "error") {
                            Log.d(TAG, "Full event: $text")
                        }

                        val event = ServerEvent.fromJson(json)

                        // Track session ID
                        if (event is SessionCreatedEvent) {
                            sessionId = event.session.id
                            Log.d(TAG, "Session created: ${event.session.id}")
                        }

                        _events.send(event)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing event: ${e.message}", e)
                        _events.send(ErrorEvent(
                            eventId = null,
                            error = ErrorInfo(
                                type = "parse_error",
                                code = "invalid_json",
                                message = "Failed to parse event: ${e.message}"
                            )
                        ))
                    }
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code - $reason")
                _connectionState.value = ConnectionState.DISCONNECTING
                webSocket.close(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code - $reason")
                _connectionState.value = ConnectionState.DISCONNECTED
                sessionId = null
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure: ${t.message}", t)
                _connectionState.value = ConnectionState.ERROR
                scope.launch {
                    _events.send(ErrorEvent(
                        eventId = null,
                        error = ErrorInfo(
                            type = "connection_error",
                            code = "websocket_failure",
                            message = t.message ?: "Connection failed"
                        )
                    ))
                }
            }
        })
    }

    /**
     * Disconnect from the API.
     */
    fun disconnect() {
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    /**
     * Configure the session with modalities, voice, and tools.
     */
    fun configureSession(
        modalities: List<String> = listOf("text", "audio"),
        instructions: String? = null,
        voice: String = "alloy",
        tools: List<RealtimeTool> = emptyList()
    ) {
        val message = SessionUpdateMessage(
            modalities = modalities,
            instructions = instructions,
            voice = voice,
            tools = tools
        )
        send(message)
    }

    /**
     * Send audio data to the input buffer.
     * Audio should be PCM16 format, 24kHz, mono.
     */
    fun sendAudio(audioData: ByteArray) {
        val base64Audio = Base64.encodeToString(audioData, Base64.NO_WRAP)
        send(InputAudioBufferAppendMessage(base64Audio))
    }

    /**
     * Commit the audio buffer (signal end of speech).
     */
    fun commitAudio() {
        send(InputAudioBufferCommitMessage())
    }

    /**
     * Clear the audio buffer.
     */
    fun clearAudio() {
        send(InputAudioBufferClearMessage())
    }

    /**
     * Send a text message.
     */
    fun sendText(text: String) {
        send(ConversationItemCreateMessage(
            role = "user",
            content = listOf(ContentPart(type = "input_text", text = text))
        ))
    }

    /**
     * Request a response from the model.
     */
    fun requestResponse(modalities: List<String> = listOf("text", "audio")) {
        send(ResponseCreateMessage(modalities = modalities))
    }

    /**
     * Cancel an in-progress response.
     */
    fun cancelResponse() {
        send(ResponseCancelMessage())
    }

    /**
     * Send a client message to the WebSocket.
     */
    private fun send(message: ClientMessage) {
        val json = message.toJson().toString()
        val type = message.javaClass.simpleName
        Log.d(TAG, "Sending message: $type")
        webSocket?.send(json)
    }

    /**
     * Clean up resources.
     */
    fun close() {
        disconnect()
        scope.cancel()
    }

    companion object {
        private const val TAG = "RealtimeWebSocket"

        private fun createDefaultClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS) // No timeout for WebSocket
                .writeTimeout(30, TimeUnit.SECONDS)
                .pingInterval(30, TimeUnit.SECONDS)
                .build()
        }
    }
}

/**
 * Connection states for the WebSocket.
 */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    ERROR
}
