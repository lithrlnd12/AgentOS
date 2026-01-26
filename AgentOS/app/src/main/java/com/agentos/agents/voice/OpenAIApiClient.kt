package com.agentos.agents.voice

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.takeWhile
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Placeholder OpenAI API client for voice processing
 * This will be replaced with the official OpenAI Java SDK when available
 */
class OpenAIApiClient(
    private val apiKey: String,
    private val baseUrl: String = "https://api.openai.com/v1"
) {
    
    companion object {
        private const val TAG = "OpenAIApiClient"
        private const val TIMEOUT_SECONDS = 30L
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()
    
    /**
     * Create a chat completion for intent extraction
     */
    suspend fun createChatCompletion(
        model: String,
        messages: List<ChatMessage>,
        temperature: Double = 0.3,
        maxTokens: Int = 500,
        responseFormat: String = "json"
    ): ChatCompletionResponse = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Creating chat completion with model: $model")
            
            val requestBody = JSONObject().apply {
                put("model", model)
                put("messages", messages.map { msg ->
                    JSONObject().apply {
                        put("role", msg.role)
                        put("content", msg.content)
                    }
                })
                put("temperature", temperature)
                put("max_tokens", maxTokens)
                put("response_format", JSONObject().apply {
                    put("type", responseFormat)
                })
            }
            
            val request = Request.Builder()
                .url("$baseUrl/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("API request failed: ${response.code} - ${response.body?.string()}")
                }
                
                val responseBody = response.body?.string() ?: throw IOException("Empty response body")
                val jsonResponse = JSONObject(responseBody)
                
                parseChatCompletionResponse(jsonResponse)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Chat completion failed", e)
            throw e
        }
    }
    
    /**
     * Test API connectivity
     */
    suspend fun testConnectivity(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Testing API connectivity")
            
            val request = Request.Builder()
                .url("$baseUrl/models")
                .addHeader("Authorization", "Bearer $apiKey")
                .get()
                .build()
            
            client.newCall(request).execute().use { response ->
                val success = response.isSuccessful
                Log.d(TAG, "API connectivity test: $success")
                success
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Connectivity test failed", e)
            false
        }
    }
    
    /**
     * Create a realtime session for voice processing
     */
    fun createRealtimeSession(
        model: String = "gpt-4o-realtime-preview",
        voice: String = "alloy"
    ): RealtimeSession {
        Log.d(TAG, "Creating realtime session with model: $model, voice: $voice")
        
        // Placeholder implementation - return mock session
        return MockRealtimeSession(model, voice, this)
    }
    
    /**
     * Parse chat completion response
     */
    private fun parseChatCompletionResponse(jsonResponse: JSONObject): ChatCompletionResponse {
        val choices = jsonResponse.getJSONArray("choices")
        val firstChoice = choices.getJSONObject(0)
        val message = firstChoice.getJSONObject("message")
        
        return ChatCompletionResponse(
            id = jsonResponse.getString("id"),
            model = jsonResponse.getString("model"),
            choices = listOf(
                ChatCompletionChoice(
                    message = ChatMessage(
                        role = message.getString("role"),
                        content = message.getString("content")
                    ),
                    finishReason = firstChoice.optString("finish_reason", "stop")
                )
            ),
            usage = parseUsage(jsonResponse.optJSONObject("usage"))
        )
    }
    
    private fun parseUsage(usageJson: JSONObject?): Usage {
        return Usage(
            promptTokens = usageJson?.optInt("prompt_tokens") ?: 0,
            completionTokens = usageJson?.optInt("completion_tokens") ?: 0,
            totalTokens = usageJson?.optInt("total_tokens") ?: 0
        )
    }
}

/**
 * Chat message for OpenAI API
 */
data class ChatMessage(
    val role: String,
    val content: String
)

/**
 * Chat completion response
 */
data class ChatCompletionResponse(
    val id: String,
    val model: String,
    val choices: List<ChatCompletionChoice>,
    val usage: Usage
)

/**
 * Chat completion choice
 */
data class ChatCompletionChoice(
    val message: ChatMessage,
    val finishReason: String
)

/**
 * Usage information
 */
data class Usage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)

/**
 * Mock realtime session for voice processing
 */
class MockRealtimeSession(
    private val model: String,
    private val voice: String,
    private val apiClient: OpenAIApiClient
) : RealtimeSession {
    
    private val isActive = AtomicBoolean(true)
    
    override fun send(message: RealtimeClientMessage) {
        Log.d("MockRealtimeSession", "Sending message: ${message.type()}")
        // Mock implementation - in production this would send to WebSocket
    }
    
    override fun responses(): Flow<RealtimeServerMessage> {
        return flow {
            // Mock responses for testing
            delay(100) // Simulate processing time
            
            emit(RealtimeServerMessage.ResponseTextDelta(
                delta = "Test recognized text from voice input"
            ))

            delay(50)

            emit(RealtimeServerMessage.ResponseTextDone())
        }.takeWhile { isActive.get() }
    }
    
    override fun close() {
        Log.d("MockRealtimeSession", "Closing session")
        isActive.set(false)
    }
}

/**
 * Mock realtime messages
 */
sealed class RealtimeClientMessage {
    abstract fun type(): String
    
    data class InputAudioBufferAppend(
        val audio: ByteArray
    ) : RealtimeClientMessage() {
        override fun type() = "input_audio_buffer.append"
    }
    
    data class InputAudioBufferCommit(
        val audio: ByteArray? = null
    ) : RealtimeClientMessage() {
        override fun type() = "input_audio_buffer.commit"
    }
    
    data class ResponseCreate(
        val modalities: List<String> = listOf("text", "audio")
    ) : RealtimeClientMessage() {
        override fun type() = "response.create"
    }
}

/**
 * Mock realtime server messages
 */
sealed class RealtimeServerMessage {
    data class ResponseTextDelta(
        val delta: String
    ) : RealtimeServerMessage()
    
    data class ResponseTextDone(
        val text: String? = null
    ) : RealtimeServerMessage()
    
    data class Error(
        val error: String
    ) : RealtimeServerMessage()
}

/**
 * Realtime session interface
 */
interface RealtimeSession {
    fun send(message: RealtimeClientMessage)
    fun responses(): Flow<RealtimeServerMessage>
    fun close()
}