package com.agentos.workflow

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.agentos.accessibility.AgentAccessibilityService
import com.agentos.claude.ClaudeApiClient
import com.agentos.claude.ContentBlock
import com.agentos.claude.Message
import com.agentos.core.apps.AppManager
import com.agentos.core.apps.RecommendationType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Orchestrates multi-step workflows using Claude for decision making
 * and AccessibilityService for action execution.
 */
class WorkflowEngine(
    private val claudeClient: ClaudeApiClient,
    private val appManager: AppManager
) {
    private val _state = MutableStateFlow<WorkflowState>(WorkflowState.Idle)
    val state: StateFlow<WorkflowState> = _state.asStateFlow()

    private val conversationHistory = mutableListOf<Message>()
    private var retryCount = 0
    private val maxRetries = 3

    /**
     * Execute a user request as a multi-step workflow.
     */
    suspend fun execute(userRequest: String, maxIterations: Int = 10) {
        executeInternal(userRequest, emptyList(), maxIterations)
    }

    /**
     * Execute with prior conversation context for seamless transitions.
     */
    suspend fun executeWithContext(
        userRequest: String,
        priorContext: List<Message>,
        maxIterations: Int = 10
    ) {
        executeInternal(userRequest, priorContext, maxIterations)
    }

    private suspend fun executeInternal(
        userRequest: String,
        priorContext: List<Message>,
        maxIterations: Int
    ) {
        _state.value = WorkflowState.Running(step = 0, description = "Starting...")
        conversationHistory.clear()
        retryCount = 0

        // Add prior context if available
        if (priorContext.isNotEmpty()) {
            conversationHistory.addAll(priorContext)
        }

        conversationHistory.add(Message.user(userRequest))

        val accessibilityService = AgentAccessibilityService.instance.value
        if (accessibilityService == null) {
            _state.value = WorkflowState.Error("Accessibility service not available")
            return
        }

        // Add installed apps context at the start of execution
        val appsContext = appManager.getAppsContextForAI()
        conversationHistory.add(Message.user(
            """Device app information:
            |$appsContext
            """.trimMargin()
        ))

        repeat(maxIterations) { iteration ->
            _state.value = WorkflowState.Running(
                step = iteration + 1,
                description = "Analyzing screen and planning action..."
            )

            try {
                // Get screen context from accessibility service
                val screenContext = buildScreenContext(accessibilityService)

                // Add screen context to the conversation
                if (screenContext.isNotEmpty()) {
                    conversationHistory.add(Message.user(
                        """Current screen state:
                        |$screenContext
                        """.trimMargin()
                    ))
                }

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

                // Check for clarification needed
                if (needsClarification(textContent, response.content)) {
                    _state.value = WorkflowState.NeedsClarification(
                        step = iteration + 1,
                        question = extractClarificationQuestion(textContent),
                        context = textContent
                    )
                    return
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
                        description = describeAction(toolUse)
                    )

                    val result = executeAction(accessibilityService, toolUse)

                    // Check for stuck state
                    if (isStuck(result, toolUse)) {
                        retryCount++
                        if (retryCount >= maxRetries) {
                            _state.value = WorkflowState.NeedsClarification(
                                step = iteration + 1,
                                question = "I'm having trouble with this action. Could you help?",
                                context = result
                            )
                            return
                        }
                    } else {
                        retryCount = 0
                    }

                    // Brief delay to let screen update after action
                    delay(500)

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

    /**
     * Provide clarification and resume workflow.
     */
    suspend fun provideClarification(input: String) {
        val currentState = _state.value
        if (currentState !is WorkflowState.NeedsClarification) return

        conversationHistory.add(Message.user(input))
        retryCount = 0

        // Resume execution
        executeInternal("", emptyList(), 10)
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
            "wait" -> {
                val duration = toolUse.input.optLong("duration_ms", 1000)
                Thread.sleep(duration)
                "Waited for ${duration}ms"
            }
            "launch_app" -> {
                val packageName = toolUse.input.getString("package_name")
                val success = appManager.launchApp(packageName)
                if (success) {
                    "Launched app: $packageName"
                } else {
                    "Failed to launch app: $packageName - app may not be installed"
                }
            }
            "search_apps" -> {
                val query = toolUse.input.getString("query")
                val results = appManager.searchApps(query)
                if (results.isEmpty()) {
                    "No apps found matching '$query'"
                } else {
                    val appList = results.take(5).joinToString("\n") {
                        "- ${it.appName} (${it.packageName})"
                    }
                    "Found ${results.size} apps matching '$query':\n$appList"
                }
            }
            "open_url" -> {
                val url = toolUse.input.getString("url")
                val success = appManager.launchUrl(url)
                if (success) {
                    "Opened URL in browser: $url"
                } else {
                    "Failed to open URL: $url"
                }
            }
            "get_app_recommendation" -> {
                val action = toolUse.input.getString("action")
                val recommendation = appManager.getBestAppForAction(action)
                when (recommendation.type) {
                    RecommendationType.USE_APP -> {
                        "Recommendation: Use ${recommendation.app?.appName} (${recommendation.app?.packageName}). ${recommendation.reason}"
                    }
                    RecommendationType.USE_BROWSER -> {
                        "Recommendation: Use browser. ${recommendation.reason}. Browser: ${recommendation.app?.appName ?: "default browser"}"
                    }
                    RecommendationType.NOT_AVAILABLE -> {
                        "No app available for this action. ${recommendation.reason}"
                    }
                }
            }
            else -> "Unknown action: ${toolUse.name}"
        }
    }

    private fun describeAction(toolUse: ContentBlock.ToolUse): String {
        return when (toolUse.name) {
            "tap" -> {
                val x = toolUse.input.optDouble("x", 0.0)
                val y = toolUse.input.optDouble("y", 0.0)
                "Tapping at (${"%.0f".format(x)}, ${"%.0f".format(y)})"
            }
            "swipe" -> "Swiping on screen"
            "type_text" -> {
                val text = toolUse.input.optString("text", "")
                "Typing: ${text.take(20)}${if (text.length > 20) "..." else ""}"
            }
            "press_back" -> "Pressing back"
            "press_home" -> "Going home"
            "wait" -> "Waiting..."
            "launch_app" -> {
                val pkg = toolUse.input.optString("package_name", "")
                "Launching app: $pkg"
            }
            "search_apps" -> {
                val query = toolUse.input.optString("query", "")
                "Searching for apps: $query"
            }
            "open_url" -> "Opening URL in browser"
            "get_app_recommendation" -> "Checking for best app to use"
            else -> "Executing: ${toolUse.name}"
        }
    }

    private fun needsClarification(text: String, content: List<ContentBlock>): Boolean {
        val lowerText = text.lowercase()

        // Check for explicit clarification signals
        val clarificationPhrases = listOf(
            "could you clarify",
            "i need more information",
            "please specify",
            "which one",
            "password",
            "login required",
            "sign in",
            "authentication needed",
            "permission denied",
            "i'm not sure",
            "unclear",
            "ambiguous"
        )

        return clarificationPhrases.any { lowerText.contains(it) }
    }

    private fun extractClarificationQuestion(text: String): String {
        // Try to extract the actual question
        val questionMatch = Regex("[^.!]*\\?").find(text)
        return questionMatch?.value?.trim() ?: "Could you provide more information?"
    }

    private fun isStuck(result: String, toolUse: ContentBlock.ToolUse): Boolean {
        val lowerResult = result.lowercase()

        // Check for failure indicators
        val failureIndicators = listOf(
            "not found",
            "element not found",
            "failed",
            "error",
            "timeout",
            "unable to"
        )

        return failureIndicators.any { lowerResult.contains(it) }
    }

    /**
     * Build a text description of the current screen from the accessibility tree.
     */
    private fun buildScreenContext(service: AgentAccessibilityService): String {
        val root = service.getRootNode() ?: return "Unable to read screen content."

        val elements = mutableListOf<ScreenElement>()
        extractElements(root, elements, 0)

        if (elements.isEmpty()) {
            return "Screen appears empty or inaccessible."
        }

        val sb = StringBuilder()
        sb.appendLine("Screen elements (id, type, text, bounds, clickable):")

        elements.forEachIndexed { index, element ->
            sb.appendLine("[$index] ${element.className}: \"${element.text}\" at (${element.centerX}, ${element.centerY}) - ${if (element.clickable) "clickable" else "not clickable"}")
        }

        // Store elements for reference during action execution
        lastScreenElements = elements

        return sb.toString()
    }

    private var lastScreenElements: List<ScreenElement> = emptyList()

    /**
     * Recursively extract UI elements from the accessibility tree.
     */
    private fun extractElements(
        node: AccessibilityNodeInfo,
        elements: MutableList<ScreenElement>,
        depth: Int
    ) {
        if (depth > 15) return // Prevent infinite recursion

        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        // Only include elements that have text or are interactive
        val text = node.text?.toString() ?: node.contentDescription?.toString() ?: ""
        val className = node.className?.toString()?.substringAfterLast('.') ?: "View"
        val isClickable = node.isClickable
        val isEditable = node.isEditable
        val isVisible = bounds.width() > 0 && bounds.height() > 0

        if (isVisible && (text.isNotEmpty() || isClickable || isEditable)) {
            elements.add(ScreenElement(
                className = className,
                text = text.take(50), // Truncate long text
                bounds = bounds,
                centerX = bounds.centerX(),
                centerY = bounds.centerY(),
                clickable = isClickable,
                editable = isEditable,
                resourceId = node.viewIdResourceName
            ))
        }

        // Recursively process children
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                extractElements(child, elements, depth + 1)
                child.recycle()
            }
        }
    }

    /**
     * Data class representing a UI element on screen.
     */
    private data class ScreenElement(
        val className: String,
        val text: String,
        val bounds: Rect,
        val centerX: Int,
        val centerY: Int,
        val clickable: Boolean,
        val editable: Boolean,
        val resourceId: String?
    )

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
        ),
        com.agentos.claude.Tool(
            name = "wait",
            description = "Wait for a specified duration",
            inputSchema = JSONObject("""
                {
                    "type": "object",
                    "properties": {
                        "duration_ms": {"type": "integer", "description": "Duration to wait in milliseconds", "default": 1000}
                    }
                }
            """.trimIndent())
        ),
        com.agentos.claude.Tool(
            name = "launch_app",
            description = "Launch an installed app directly by its package name. Use this instead of navigating through the home screen when you know the app is installed.",
            inputSchema = JSONObject("""
                {
                    "type": "object",
                    "properties": {
                        "package_name": {"type": "string", "description": "The package name of the app to launch (e.g., com.spotify.music, com.instagram.android)"}
                    },
                    "required": ["package_name"]
                }
            """.trimIndent())
        ),
        com.agentos.claude.Tool(
            name = "search_apps",
            description = "Search for installed apps by name or keyword. Use this to find the correct package name before launching.",
            inputSchema = JSONObject("""
                {
                    "type": "object",
                    "properties": {
                        "query": {"type": "string", "description": "Search query (app name or keyword)"}
                    },
                    "required": ["query"]
                }
            """.trimIndent())
        ),
        com.agentos.claude.Tool(
            name = "open_url",
            description = "Open a URL in the default browser. Use this when the user wants to visit a website or when a native app is not installed.",
            inputSchema = JSONObject("""
                {
                    "type": "object",
                    "properties": {
                        "url": {"type": "string", "description": "The full URL to open (must include https://)"}
                    },
                    "required": ["url"]
                }
            """.trimIndent())
        ),
        com.agentos.claude.Tool(
            name = "get_app_recommendation",
            description = "Get a recommendation for the best app to use for a given action. Returns whether to use a native app or browser.",
            inputSchema = JSONObject("""
                {
                    "type": "object",
                    "properties": {
                        "action": {"type": "string", "description": "The action or task the user wants to perform (e.g., 'order food', 'check email', 'watch videos')"}
                    },
                    "required": ["action"]
                }
            """.trimIndent())
        )
    )

    fun cancel() {
        _state.value = WorkflowState.Idle
        conversationHistory.clear()
        retryCount = 0
    }

    companion object {
        private val SYSTEM_PROMPT = """
            You are AgentOS, an AI assistant that controls an Android phone.
            You can see the screen content and execute actions to complete user requests.

            APP AWARENESS:
            - You have access to a list of installed apps on this device
            - ALWAYS prefer using launch_app to open apps directly instead of searching the home screen
            - If the user asks to open an app (e.g., "open Spotify"), use search_apps first to find the package name, then launch_app
            - If a native app is NOT installed, use open_url to open the website version in browser
            - Use get_app_recommendation when unsure whether to use an app or browser

            APP NAVIGATION STRATEGY:
            1. User says "open Twitter" → search_apps("twitter") → if found, launch_app(package_name)
            2. User says "check my email" → You know Gmail is common, try launch_app("com.google.android.gm")
            3. User says "order pizza from Dominos" → search_apps("dominos") → if not found, open_url("https://dominos.com")
            4. User says "play music" → get_app_recommendation("play music") → use recommended app or browser

            ANDROID UI UNDERSTANDING:
            - You are viewing an Android device screen through accessibility services
            - Screen elements are listed with their type, text, coordinates (x,y), and if they're clickable
            - Common Android UI patterns:
              * Settings app: Has categories like "Network & internet", "Connected devices", "Apps", etc.
              * Toggle switches are usually on the right side of list items
              * Back navigation is typically at top-left or use press_back action
              * Scrollable lists may need swipe actions to reveal more content
              * Buttons, TextViews, ImageViews, Switches are common element types

            COORDINATE SYSTEM:
            - Coordinates are in pixels from top-left (0,0)
            - Tap the CENTER coordinates provided for each element
            - Phone screens are typically 1080-1440px wide and 2000-3000px tall

            WORKFLOW RULES:
            1. For app-related requests, first check if the app is installed using search_apps
            2. Use launch_app to open apps directly - it's faster than tapping through the UI
            3. Analyze the screen elements provided - look for relevant text/buttons
            4. Find the element that matches what you need to tap
            5. Use the CENTER coordinates (centerX, centerY) from the element list
            6. Execute ONE action at a time, then wait for the next screen state
            7. If you don't see the expected result after 2-3 attempts, ask for help

            COMPLETION:
            - When the task is DONE, respond with a summary WITHOUT using any tools
            - Don't keep tapping if the goal is already achieved
            - Example: If asked to "turn on WiFi" and WiFi toggle shows ON, you're done

            IMPORTANT:
            - If you see a login/password screen, STOP and ask the user
            - For sensitive actions (purchases, messages), confirm first
            - If stuck or unsure, ask for clarification
            - Don't guess - if you can't find an element, say so
            - NEVER search the home screen for an app if you can launch it directly!
        """.trimIndent()
    }
}

sealed class WorkflowState {
    object Idle : WorkflowState()
    data class Running(val step: Int, val description: String) : WorkflowState()
    data class NeedsClarification(
        val step: Int,
        val question: String,
        val context: String
    ) : WorkflowState()
    data class Completed(val result: String) : WorkflowState()
    data class Error(val message: String) : WorkflowState()
}
