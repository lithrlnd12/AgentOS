package com.agentos.voice.realtime

import org.json.JSONArray
import org.json.JSONObject

/**
 * Message types for OpenAI Realtime API WebSocket communication.
 */

// ============ Client → Server Messages ============

/**
 * Base class for client-to-server messages.
 */
sealed class ClientMessage {
    abstract fun toJson(): JSONObject
}

/**
 * Configure the realtime session.
 */
data class SessionUpdateMessage(
    val modalities: List<String> = listOf("text", "audio"),
    val instructions: String? = null,
    val voice: String = "alloy",
    val inputAudioFormat: String = "pcm16",
    val outputAudioFormat: String = "pcm16",
    val inputAudioTranscription: AudioTranscriptionConfig? = null, // Disabled - requires separate API access
    val turnDetection: TurnDetectionConfig? = TurnDetectionConfig(),
    val tools: List<RealtimeTool> = emptyList()
) : ClientMessage() {
    override fun toJson(): JSONObject = JSONObject().apply {
        put("type", "session.update")
        put("session", JSONObject().apply {
            put("modalities", JSONArray(modalities))
            instructions?.let { put("instructions", it) }
            put("voice", voice)
            put("input_audio_format", inputAudioFormat)
            put("output_audio_format", outputAudioFormat)
            // Only include transcription if explicitly enabled
            inputAudioTranscription?.let {
                put("input_audio_transcription", JSONObject().apply {
                    put("model", it.model)
                })
            }
            turnDetection?.let {
                put("turn_detection", JSONObject().apply {
                    put("type", it.type)
                    put("threshold", it.threshold)
                    put("prefix_padding_ms", it.prefixPaddingMs)
                    put("silence_duration_ms", it.silenceDurationMs)
                    put("create_response", it.createResponse)
                })
            }
            if (tools.isNotEmpty()) {
                put("tools", JSONArray().apply {
                    tools.forEach { tool ->
                        put(tool.toJson())
                    }
                })
            }
        })
    }
}

data class AudioTranscriptionConfig(
    val model: String = "whisper-1"
)

data class TurnDetectionConfig(
    val type: String = "server_vad",
    val threshold: Float = 0.85f,  // Higher threshold to avoid noise/interrupts (0.0-1.0)
    val prefixPaddingMs: Int = 400,  // Padding before speech
    val silenceDurationMs: Int = 1200,  // Wait 1.2s of silence before ending turn (prevents interrupts)
    val createResponse: Boolean = true // Automatically create response after speech ends
)

data class RealtimeTool(
    val type: String = "function",
    val name: String,
    val description: String,
    val parameters: JSONObject
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("type", type)
        put("name", name)
        put("description", description)
        put("parameters", parameters)
    }
}

/**
 * Append audio data to the input buffer.
 */
data class InputAudioBufferAppendMessage(
    val audio: String // Base64 encoded PCM audio
) : ClientMessage() {
    override fun toJson(): JSONObject = JSONObject().apply {
        put("type", "input_audio_buffer.append")
        put("audio", audio)
    }
}

/**
 * Commit the audio buffer (end of speech).
 */
class InputAudioBufferCommitMessage : ClientMessage() {
    override fun toJson(): JSONObject = JSONObject().apply {
        put("type", "input_audio_buffer.commit")
    }
}

/**
 * Clear the audio buffer.
 */
class InputAudioBufferClearMessage : ClientMessage() {
    override fun toJson(): JSONObject = JSONObject().apply {
        put("type", "input_audio_buffer.clear")
    }
}

/**
 * Create a conversation item (text input).
 */
data class ConversationItemCreateMessage(
    val role: String = "user",
    val content: List<ContentPart>
) : ClientMessage() {
    override fun toJson(): JSONObject = JSONObject().apply {
        put("type", "conversation.item.create")
        put("item", JSONObject().apply {
            put("type", "message")
            put("role", role)
            put("content", JSONArray().apply {
                content.forEach { part ->
                    put(part.toJson())
                }
            })
        })
    }
}

data class ContentPart(
    val type: String, // "input_text" or "input_audio"
    val text: String? = null,
    val audio: String? = null // Base64
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("type", type)
        text?.let { put("text", it) }
        audio?.let { put("audio", it) }
    }
}

/**
 * Request a response from the model.
 */
data class ResponseCreateMessage(
    val modalities: List<String> = listOf("text", "audio"),
    val instructions: String? = null
) : ClientMessage() {
    override fun toJson(): JSONObject = JSONObject().apply {
        put("type", "response.create")
        put("response", JSONObject().apply {
            put("modalities", JSONArray(modalities))
            instructions?.let { put("instructions", it) }
        })
    }
}

/**
 * Cancel an in-progress response.
 */
class ResponseCancelMessage : ClientMessage() {
    override fun toJson(): JSONObject = JSONObject().apply {
        put("type", "response.cancel")
    }
}


// ============ Server → Client Events ============

/**
 * Base class for server-to-client events.
 */
sealed class ServerEvent {
    abstract val eventId: String?

    companion object {
        fun fromJson(json: JSONObject): ServerEvent {
            val type = json.getString("type")
            val eventId: String? = if (json.has("event_id")) json.getString("event_id") else null

            return when (type) {
                "session.created" -> SessionCreatedEvent(
                    eventId = eventId,
                    session = parseSession(json.getJSONObject("session"))
                )
                "session.updated" -> SessionUpdatedEvent(
                    eventId = eventId,
                    session = parseSession(json.getJSONObject("session"))
                )
                "input_audio_buffer.speech_started" -> SpeechStartedEvent(
                    eventId = eventId,
                    audioStartMs = json.optInt("audio_start_ms", 0)
                )
                "input_audio_buffer.speech_stopped" -> SpeechStoppedEvent(
                    eventId = eventId,
                    audioEndMs = json.optInt("audio_end_ms", 0)
                )
                "input_audio_buffer.committed" -> AudioBufferCommittedEvent(
                    eventId = eventId,
                    itemId = json.optString("item_id", "")
                )
                "conversation.item.created" -> ConversationItemCreatedEvent(
                    eventId = eventId,
                    item = parseConversationItem(json.getJSONObject("item"))
                )
                "response.created" -> ResponseCreatedEvent(
                    eventId = eventId,
                    responseId = json.getJSONObject("response").getString("id")
                )
                "response.output_item.added" -> ResponseOutputItemAddedEvent(
                    eventId = eventId,
                    responseId = json.optString("response_id", ""),
                    item = parseConversationItem(json.getJSONObject("item"))
                )
                "response.audio.delta" -> ResponseAudioDeltaEvent(
                    eventId = eventId,
                    responseId = json.optString("response_id", ""),
                    itemId = json.optString("item_id", ""),
                    delta = json.getString("delta") // Base64 audio
                )
                "response.audio_transcript.delta" -> ResponseAudioTranscriptDeltaEvent(
                    eventId = eventId,
                    responseId = json.optString("response_id", ""),
                    itemId = json.optString("item_id", ""),
                    delta = json.getString("delta")
                )
                "response.text.delta" -> ResponseTextDeltaEvent(
                    eventId = eventId,
                    responseId = json.optString("response_id", ""),
                    itemId = json.optString("item_id", ""),
                    delta = json.getString("delta")
                )
                "response.function_call_arguments.delta" -> FunctionCallArgumentsDeltaEvent(
                    eventId = eventId,
                    responseId = json.optString("response_id", ""),
                    itemId = json.optString("item_id", ""),
                    callId = json.optString("call_id", ""),
                    delta = json.getString("delta")
                )
                "response.function_call_arguments.done" -> FunctionCallArgumentsDoneEvent(
                    eventId = eventId,
                    responseId = json.optString("response_id", ""),
                    itemId = json.optString("item_id", ""),
                    callId = json.optString("call_id", ""),
                    arguments = json.getString("arguments")
                )
                "response.done" -> ResponseDoneEvent(
                    eventId = eventId,
                    response = parseResponseWithOutput(json.getJSONObject("response"))
                )
                "conversation.item.input_audio_transcription.completed" -> TranscriptionCompletedEvent(
                    eventId = eventId,
                    itemId = json.optString("item_id", ""),
                    transcript = json.optString("transcript", "")
                )
                "conversation.item.input_audio_transcription.failed" -> {
                    val errorObj = json.optJSONObject("error")
                    TranscriptionFailedEvent(
                        eventId = eventId,
                        itemId = json.optString("item_id", ""),
                        error = ErrorInfo(
                            type = errorObj?.optString("type", "") ?: "",
                            code = errorObj?.optString("code", "") ?: "",
                            message = errorObj?.optString("message", "Transcription failed") ?: "Transcription failed"
                        )
                    )
                }
                "error" -> ErrorEvent(
                    eventId = eventId,
                    error = parseError(json.getJSONObject("error"))
                )
                else -> UnknownEvent(eventId = eventId, type = type, raw = json.toString())
            }
        }

        private fun parseSession(json: JSONObject) = SessionInfo(
            id = json.optString("id", ""),
            model = json.optString("model", ""),
            modalities = json.optJSONArray("modalities")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            } ?: emptyList()
        )

        private fun parseConversationItem(json: JSONObject) = ConversationItem(
            id = json.optString("id", ""),
            type = json.optString("type", ""),
            role = json.optString("role", ""),
            status = json.optString("status", "")
        )

        private fun parseResponse(json: JSONObject) = ResponseInfo(
            id = json.optString("id", ""),
            status = json.optString("status", ""),
            output = null
        )

        private fun parseResponseWithOutput(json: JSONObject): ResponseInfo {
            val outputArray = json.optJSONArray("output")
            val output = outputArray?.let { arr ->
                (0 until arr.length()).mapNotNull { i ->
                    val item = arr.getJSONObject(i)
                    val contentArray = item.optJSONArray("content")
                    val content = contentArray?.let { cArr ->
                        (0 until cArr.length()).mapNotNull { j ->
                            val c = cArr.getJSONObject(j)
                            ResponseContent(
                                type = c.optString("type", ""),
                                text = c.optString("text", null),
                                transcript = c.optString("transcript", null)
                            )
                        }
                    }
                    ResponseOutputItem(
                        id = item.optString("id", ""),
                        type = item.optString("type", ""),
                        role = item.optString("role", ""),
                        content = content
                    )
                }
            }
            return ResponseInfo(
                id = json.optString("id", ""),
                status = json.optString("status", ""),
                output = output
            )
        }

        private fun parseError(json: JSONObject) = ErrorInfo(
            type = json.optString("type", ""),
            code = json.optString("code", ""),
            message = json.optString("message", "")
        )
    }
}

// Session events
data class SessionCreatedEvent(
    override val eventId: String?,
    val session: SessionInfo
) : ServerEvent()

data class SessionUpdatedEvent(
    override val eventId: String?,
    val session: SessionInfo
) : ServerEvent()

// Audio buffer events
data class SpeechStartedEvent(
    override val eventId: String?,
    val audioStartMs: Int
) : ServerEvent()

data class SpeechStoppedEvent(
    override val eventId: String?,
    val audioEndMs: Int
) : ServerEvent()

data class AudioBufferCommittedEvent(
    override val eventId: String?,
    val itemId: String
) : ServerEvent()

// Conversation events
data class ConversationItemCreatedEvent(
    override val eventId: String?,
    val item: ConversationItem
) : ServerEvent()

// Response events
data class ResponseCreatedEvent(
    override val eventId: String?,
    val responseId: String
) : ServerEvent()

data class ResponseOutputItemAddedEvent(
    override val eventId: String?,
    val responseId: String,
    val item: ConversationItem
) : ServerEvent()

data class ResponseAudioDeltaEvent(
    override val eventId: String?,
    val responseId: String,
    val itemId: String,
    val delta: String // Base64 encoded audio
) : ServerEvent()

data class ResponseAudioTranscriptDeltaEvent(
    override val eventId: String?,
    val responseId: String,
    val itemId: String,
    val delta: String
) : ServerEvent()

data class ResponseTextDeltaEvent(
    override val eventId: String?,
    val responseId: String,
    val itemId: String,
    val delta: String
) : ServerEvent()

data class FunctionCallArgumentsDeltaEvent(
    override val eventId: String?,
    val responseId: String,
    val itemId: String,
    val callId: String,
    val delta: String
) : ServerEvent()

data class FunctionCallArgumentsDoneEvent(
    override val eventId: String?,
    val responseId: String,
    val itemId: String,
    val callId: String,
    val arguments: String
) : ServerEvent()

data class ResponseDoneEvent(
    override val eventId: String?,
    val response: ResponseInfo?
) : ServerEvent()

// Transcription completed event (Whisper)
data class TranscriptionCompletedEvent(
    override val eventId: String?,
    val itemId: String,
    val transcript: String
) : ServerEvent()

// Transcription failed event
data class TranscriptionFailedEvent(
    override val eventId: String?,
    val itemId: String,
    val error: ErrorInfo
) : ServerEvent()

// Error event
data class ErrorEvent(
    override val eventId: String?,
    val error: ErrorInfo
) : ServerEvent()

// Unknown event fallback
data class UnknownEvent(
    override val eventId: String?,
    val type: String,
    val raw: String
) : ServerEvent()

// Data classes for parsed info
data class SessionInfo(
    val id: String,
    val model: String,
    val modalities: List<String>
)

data class ConversationItem(
    val id: String,
    val type: String,
    val role: String,
    val status: String
)

data class ResponseInfo(
    val id: String,
    val status: String,
    val output: List<ResponseOutputItem>?
)

data class ResponseOutputItem(
    val id: String,
    val type: String,
    val role: String,
    val content: List<ResponseContent>?
)

data class ResponseContent(
    val type: String,
    val text: String?,
    val transcript: String?
)

data class ErrorInfo(
    val type: String,
    val code: String,
    val message: String
)
