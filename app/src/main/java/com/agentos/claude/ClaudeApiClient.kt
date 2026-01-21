package com.agentos.claude

import com.agentos.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Client for interacting with the Claude API.
 * Supports both standard and streaming responses.
 */
class ClaudeApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val apiKey: String = BuildConfig.CLAUDE_API_KEY
    private val baseUrl = "https://api.anthropic.com/v1"

    /**
     * Send a message to Claude and get a response.
     */
    suspend fun createMessage(
        messages: List<Message>,
        systemPrompt: String? = null,
        tools: List<Tool>? = null,
        maxTokens: Int = 4096
    ): ClaudeResponse {
        val requestBody = buildRequestBody(messages, systemPrompt, tools, maxTokens, stream = false)

        val request = Request.Builder()
            .url("$baseUrl/messages")
            .header("Content-Type", "application/json")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return kotlinx.coroutines.withContext(Dispatchers.IO) {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: throw Exception("Empty response")
            parseResponse(JSONObject(body))
        }
    }

    /**
     * Stream a response from Claude.
     */
    fun streamMessage(
        messages: List<Message>,
        systemPrompt: String? = null,
        tools: List<Tool>? = null,
        maxTokens: Int = 4096
    ): Flow<StreamEvent> = flow {
        val requestBody = buildRequestBody(messages, systemPrompt, tools, maxTokens, stream = true)

        val request = Request.Builder()
            .url("$baseUrl/messages")
            .header("Content-Type", "application/json")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        // TODO: Implement SSE streaming
        // For now, fall back to non-streaming
        val response = createMessage(messages, systemPrompt, tools, maxTokens)
        val textContent = response.content
            .filterIsInstance<ContentBlock.Text>()
            .firstOrNull()?.text ?: ""
        emit(StreamEvent.ContentDelta(textContent))
        emit(StreamEvent.MessageComplete(response))
    }.flowOn(Dispatchers.IO)

    private fun buildRequestBody(
        messages: List<Message>,
        systemPrompt: String?,
        tools: List<Tool>?,
        maxTokens: Int,
        stream: Boolean
    ): JSONObject {
        return JSONObject().apply {
            put("model", "claude-sonnet-4-20250514")
            put("max_tokens", maxTokens)
            put("stream", stream)

            systemPrompt?.let { put("system", it) }

            put("messages", JSONArray().apply {
                messages.forEach { message ->
                    put(JSONObject().apply {
                        put("role", message.role)
                        put("content", message.content)
                    })
                }
            })

            tools?.let { toolList ->
                put("tools", JSONArray().apply {
                    toolList.forEach { tool ->
                        put(tool.toJson())
                    }
                })
            }
        }
    }

    private fun parseResponse(json: JSONObject): ClaudeResponse {
        val content = mutableListOf<ContentBlock>()
        val contentArray = json.getJSONArray("content")

        for (i in 0 until contentArray.length()) {
            val block = contentArray.getJSONObject(i)
            when (block.getString("type")) {
                "text" -> content.add(ContentBlock.Text(block.getString("text")))
                "tool_use" -> content.add(
                    ContentBlock.ToolUse(
                        id = block.getString("id"),
                        name = block.getString("name"),
                        input = block.getJSONObject("input")
                    )
                )
            }
        }

        return ClaudeResponse(
            id = json.getString("id"),
            content = content,
            stopReason = json.optString("stop_reason")
        )
    }
}

// Data classes
data class Message(
    val role: String,
    val content: String
) {
    companion object {
        fun user(content: String) = Message("user", content)
        fun assistant(content: String) = Message("assistant", content)
    }
}

data class ClaudeResponse(
    val id: String,
    val content: List<ContentBlock>,
    val stopReason: String
)

sealed class ContentBlock {
    data class Text(val text: String) : ContentBlock()
    data class ToolUse(
        val id: String,
        val name: String,
        val input: JSONObject
    ) : ContentBlock()
}

sealed class StreamEvent {
    data class ContentDelta(val text: String) : StreamEvent()
    data class MessageComplete(val response: ClaudeResponse) : StreamEvent()
}

data class Tool(
    val name: String,
    val description: String,
    val inputSchema: JSONObject
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        put("description", description)
        put("input_schema", inputSchema)
    }
}
