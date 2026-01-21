package com.agentos.workflow

import com.agentos.accessibility.AgentAccessibilityService
import com.agentos.claude.ClaudeApiClient
import com.agentos.claude.ContentBlock
import com.agentos.claude.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/**
 * Orchestrates multi-step workflows using Claude for decision making
 * and AccessibilityService for action execution.
 */
class WorkflowEngine(
    private val claudeClient: ClaudeApiClient
) {
    private val _state = MutableStateFlow<WorkflowState>(WorkflowState.Idle)
    val state: StateFlow<WorkflowState> = _state.asStateFlow()

    private val conversationHistory = mutableListOf<Message>()

    /**
     * Execute a user request as a multi-step workflow.
     */
    suspend fun execute(userRequest: String, maxIterations: Int = 10) {
        _state.value = WorkflowState.Running(step = 0, description = "Starting...")
        conversationHistory.clear()
        conversationHistory.add(Message.user(userRequest))

        val accessibilityService = AgentAccessibilityService.instance.value
        if (accessibilityService == null) {
            _state.value = WorkflowState.Error("Accessibility service not available")
            return
        }

        repeat(maxIterations) { iteration ->
            _state.value = WorkflowState.Running(
                step = iteration + 1,
                description = "Analyzing screen and planning action..."
            )

            try {
                // TODO: Add screen context (accessibility tree + screenshot)
                val response = claudeClient.createMessage(
                    messages = conversationHistory,
                    systemPrompt = SYSTEM_PROMPT,
                    tools = getAvailableTools()
                )

                // Add assistant response to history
                val textContent = response.content
                    .filterIsInstance<ContentBlock.Text>()
                    .joinToString("") { it.text }

                if (textContent.isNotBlank()) {
                    conversationHistory.add(Message.assistant(textContent))
                }

                // Check for tool use
                val toolUses = response.content.filterIsInstance<ContentBlock.ToolUse>()

                if (toolUses.isEmpty()) {
                    // No more actions needed - task complete
                    _state.value = WorkflowState.Completed(textContent)
                    return
                }

                // Execute each tool
                toolUses.forEach { toolUse ->
                    _state.value = WorkflowState.Running(
                        step = iteration + 1,
                        description = "Executing: ${toolUse.name}"
                    )

                    val result = executeAction(accessibilityService, toolUse)

                    // Add tool result to conversation
                    conversationHistory.add(Message.user(
                        """Tool result for ${toolUse.id}:
                        |$result
                        """.trimMargin()
                    ))
                }

            } catch (e: Exception) {
                _state.value = WorkflowState.Error(e.message ?: "Unknown error")
                return
            }
        }

        _state.value = WorkflowState.Error("Max iterations reached")
    }

    private fun executeAction(
        service: AgentAccessibilityService,
        toolUse: ContentBlock.ToolUse
    ): String {
        return when (toolUse.name) {
            "tap" -> {
                val x = toolUse.input.getDouble("x").toFloat()
                val y = toolUse.input.getDouble("y").toFloat()
                service.tap(x, y)
                "Tapped at ($x, $y)"
            }
            "swipe" -> {
                val startX = toolUse.input.getDouble("start_x").toFloat()
                val startY = toolUse.input.getDouble("start_y").toFloat()
                val endX = toolUse.input.getDouble("end_x").toFloat()
                val endY = toolUse.input.getDouble("end_y").toFloat()
                service.swipe(startX, startY, endX, endY)
                "Swiped from ($startX, $startY) to ($endX, $endY)"
            }
            "type_text" -> {
                val text = toolUse.input.getString("text")
                // TODO: Find focused field and type
                "Typed: $text"
            }
            "press_back" -> {
                service.pressBack()
                "Pressed back button"
            }
            "press_home" -> {
                service.pressHome()
                "Pressed home button"
            }
            else -> "Unknown action: ${toolUse.name}"
        }
    }

    private fun getAvailableTools() = listOf(
        com.agentos.claude.Tool(
            name = "tap",
            description = "Tap at screen coordinates",
            inputSchema = JSONObject("""
                {
                    "type": "object",
                    "properties": {
                        "x": {"type": "number", "description": "X coordinate"},
                        "y": {"type": "number", "description": "Y coordinate"}
                    },
                    "required": ["x", "y"]
                }
            """.trimIndent())
        ),
        com.agentos.claude.Tool(
            name = "swipe",
            description = "Swipe from one point to another",
            inputSchema = JSONObject("""
                {
                    "type": "object",
                    "properties": {
                        "start_x": {"type": "number"},
                        "start_y": {"type": "number"},
                        "end_x": {"type": "number"},
                        "end_y": {"type": "number"}
                    },
                    "required": ["start_x", "start_y", "end_x", "end_y"]
                }
            """.trimIndent())
        ),
        com.agentos.claude.Tool(
            name = "type_text",
            description = "Type text into the focused field",
            inputSchema = JSONObject("""
                {
                    "type": "object",
                    "properties": {
                        "text": {"type": "string", "description": "Text to type"}
                    },
                    "required": ["text"]
                }
            """.trimIndent())
        ),
        com.agentos.claude.Tool(
            name = "press_back",
            description = "Press the back button",
            inputSchema = JSONObject("""{"type": "object", "properties": {}}""")
        ),
        com.agentos.claude.Tool(
            name = "press_home",
            description = "Press the home button",
            inputSchema = JSONObject("""{"type": "object", "properties": {}}""")
        )
    )

    fun cancel() {
        _state.value = WorkflowState.Idle
        conversationHistory.clear()
    }

    companion object {
        private val SYSTEM_PROMPT = """
            You are AgentOS, an AI assistant that controls an Android phone.
            You can see the screen content and execute actions to complete user requests.

            When given a task:
            1. Analyze the current screen state
            2. Determine the next action needed
            3. Execute the action using the available tools
            4. Verify the result and continue until the task is complete

            Always explain what you're doing and why.
            If you encounter an error, try to recover or ask for help.
            For sensitive actions (purchases, sending messages), always confirm with the user first.
        """.trimIndent()
    }
}

sealed class WorkflowState {
    object Idle : WorkflowState()
    data class Running(val step: Int, val description: String) : WorkflowState()
    data class Completed(val result: String) : WorkflowState()
    data class Error(val message: String) : WorkflowState()
}
