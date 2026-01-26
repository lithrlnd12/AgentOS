package com.agentos.ui.chat

import com.agentos.accessibility.AgentAccessibilityService
import com.agentos.ai.ConversationEngine
import com.agentos.ai.IntentClassifier
import com.agentos.ai.UserIntent
import com.agentos.claude.Message
import com.agentos.data.ConversationRepository
import com.agentos.voice.OpenAIVoiceManager
import com.agentos.voice.VoiceManagerState
import com.agentos.voice.VoicePipeline
import com.agentos.voice.VoiceState
import com.agentos.workflow.WorkflowEngine
import com.agentos.workflow.WorkflowState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ChatViewModel as a singleton class (not an Android ViewModel).
 * This allows it to be shared between the main Activity and the floating overlay Service,
 * ensuring state synchronization across both UI contexts.
 */
@Singleton
class ChatViewModel @Inject constructor(
    private val workflowEngine: WorkflowEngine,
    private val voicePipeline: VoicePipeline,
    private val openAIVoiceManager: OpenAIVoiceManager,
    private val intentClassifier: IntentClassifier,
    private val conversationEngine: ConversationEngine,
    private val conversationRepository: ConversationRepository
) {
    // Custom coroutine scope (replaces viewModelScope since this is no longer a ViewModel)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Flag to use OpenAI Realtime API for voice (vs system STT/TTS)
    private var useOpenAIVoice = true

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _mode = MutableStateFlow(InteractionMode.AUTO)
    val mode: StateFlow<InteractionMode> = _mode.asStateFlow()

    init {
        // Observe workflow state
        scope.launch {
            workflowEngine.state.collect { workflowState ->
                _uiState.update { state ->
                    state.copy(
                        isProcessing = workflowState is WorkflowState.Running,
                        currentAction = when (workflowState) {
                            is WorkflowState.Running -> workflowState.description
                            else -> null
                        },
                        workflowState = workflowState
                    )
                }

                // Handle workflow completion
                when (workflowState) {
                    is WorkflowState.Completed -> {
                        addMessage(ChatMessage.assistant(workflowState.result))
                    }
                    is WorkflowState.Error -> {
                        addMessage(ChatMessage.assistant("Error: ${workflowState.message}"))
                    }
                    else -> {}
                }
            }
        }

        // Observe accessibility service status
        scope.launch {
            AgentAccessibilityService.instance.collect { service ->
                _uiState.update { it.copy(isAccessibilityEnabled = service != null) }
            }
        }

        // Observe OpenAI voice state
        scope.launch {
            openAIVoiceManager.state.collect { voiceState ->
                _uiState.update { state ->
                    state.copy(
                        isVoiceActive = voiceState is VoiceManagerState.UserSpeaking ||
                                       voiceState is VoiceManagerState.Ready,
                        voiceStatus = when (voiceState) {
                            is VoiceManagerState.Idle -> null
                            is VoiceManagerState.Connecting -> "Connecting..."
                            is VoiceManagerState.Ready -> "Listening... speak naturally"
                            is VoiceManagerState.UserSpeaking -> "Listening..."
                            is VoiceManagerState.Processing -> "Processing..."
                            is VoiceManagerState.AssistantSpeaking -> "Speaking..."
                            is VoiceManagerState.Error -> "Error: ${voiceState.message}"
                        }
                    )
                }
            }
        }

        // Observe OpenAI voice transcripts
        scope.launch {
            openAIVoiceManager.transcripts.collect { transcript ->
                if (transcript.isFinal) {
                    if (transcript.isUser) {
                        addMessage(ChatMessage.user("[Voice] ${transcript.text}"))
                    } else {
                        addMessage(ChatMessage.assistant(transcript.text))
                    }
                }
            }
        }

        scope.launch {
            openAIVoiceManager.isEnabled.collect { enabled ->
                _uiState.update { it.copy(isVoiceEnabled = enabled) }
            }
        }

        // Legacy voice pipeline state (fallback)
        scope.launch {
            voicePipeline.state.collect { voiceState ->
                if (!useOpenAIVoice) {
                    _uiState.update { state ->
                        state.copy(
                            isVoiceActive = voiceState is VoiceState.WaitingForWakeWord ||
                                           voiceState is VoiceState.Listening,
                            voiceStatus = when (voiceState) {
                                is VoiceState.Idle -> null
                                is VoiceState.WaitingForWakeWord -> "Say \"AgentOS\" to activate"
                                is VoiceState.Listening -> "Listening: ${voiceState.partialText}"
                                is VoiceState.Processing -> "Processing: ${voiceState.command}"
                                is VoiceState.Error -> "Error: ${voiceState.message}"
                            }
                        )
                    }

                    if (voiceState is VoiceState.Processing) {
                        addMessage(ChatMessage.user("[Voice] ${voiceState.command}"))
                    }
                }
            }
        }

        scope.launch {
            if (!useOpenAIVoice) {
                voicePipeline.isEnabled.collect { enabled ->
                    _uiState.update { it.copy(isVoiceEnabled = enabled) }
                }
            }
        }

        // Load recent conversation history
        scope.launch {
            val history = conversationRepository.getRecentMessages(20)
            // History is loaded but not displayed - used for context
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return

        // Add user message
        val userMessage = ChatMessage.user(content)
        addMessage(userMessage)

        // Persist message
        scope.launch {
            conversationRepository.saveMessage(Message.user(content))
        }

        // Clear input
        _uiState.update { it.copy(inputText = "") }

        // Get conversation context for intent classification
        val recentMessages = _uiState.value.messages
            .takeLast(10)
            .filter { !it.isFromUser }
            .map { it.content }

        // Classify intent
        val intentResult = intentClassifier.classify(content, recentMessages)

        when (_mode.value) {
            InteractionMode.CONVERSATION -> {
                // Always use conversation mode
                handleConversation(content)
            }
            InteractionMode.ACTION -> {
                // Always use action mode (if accessibility enabled)
                handleAction(content)
            }
            InteractionMode.AUTO -> {
                // Use intent classification to decide
                when (intentResult.intent) {
                    UserIntent.CONVERSATION -> handleConversation(content)
                    UserIntent.ACTION -> handleAction(content)
                    UserIntent.ACTION_CONFIRMATION -> {
                        // User is confirming a previous suggestion
                        handleActionWithContext(content)
                    }
                    UserIntent.AMBIGUOUS -> {
                        // Let Claude decide based on context
                        handleAmbiguous(content)
                    }
                }
            }
        }
    }

    private fun handleConversation(content: String) {
        scope.launch {
            _uiState.update { it.copy(isProcessing = true, currentAction = "Thinking...") }

            val response = conversationEngine.chat(content)

            _uiState.update { it.copy(isProcessing = false, currentAction = null) }

            addMessage(ChatMessage.assistant(response.text))

            // Persist assistant response
            conversationRepository.saveMessage(Message.assistant(response.text))

            // Update mode indicator if action was suggested
            if (response.suggestsAction) {
                _uiState.update { it.copy(
                    modeIndicator = ModeIndicator.SUGGESTS_ACTION
                )}
            }
        }
    }

    private fun handleAction(content: String) {
        // Check if accessibility is enabled
        if (!_uiState.value.isAccessibilityEnabled) {
            addMessage(ChatMessage.assistant(
                "I need accessibility permissions to control your phone. " +
                "Please enable AgentOS in Settings > Accessibility."
            ))
            return
        }

        // Start workflow
        scope.launch {
            workflowEngine.execute(content)
        }
    }

    private fun handleActionWithContext(content: String) {
        // Get conversation history and pass to workflow engine
        scope.launch {
            if (!_uiState.value.isAccessibilityEnabled) {
                addMessage(ChatMessage.assistant(
                    "I need accessibility permissions to perform this action. " +
                    "Please enable AgentOS in Settings > Accessibility."
                ))
                return@launch
            }

            val history = conversationRepository.getRecentMessages(20)
            workflowEngine.executeWithContext(content, history)
        }
    }

    private fun handleAmbiguous(content: String) {
        // For ambiguous intents, try conversation first
        // Claude will suggest action if appropriate
        handleConversation(content)
    }

    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun cancelTask() {
        workflowEngine.cancel()
        addMessage(ChatMessage.assistant("Task cancelled."))
    }

    fun toggleVoice() {
        if (useOpenAIVoice) {
            // Use OpenAI Realtime API for natural voice conversation
            if (openAIVoiceManager.isEnabled.value) {
                openAIVoiceManager.stop()
                addMessage(ChatMessage.assistant("Voice mode disabled."))
            } else {
                try {
                    openAIVoiceManager.start()
                    addMessage(ChatMessage.assistant(
                        "Voice mode enabled! Just speak naturally - I'm listening.\n\n" +
                        "I can:\n" +
                        "- Answer questions and have conversations\n" +
                        "- Suggest and perform actions on your phone\n" +
                        "- Understand interruptions - just start talking\n\n" +
                        "Say \"stop\" to pause, or tap the mic button to disable."
                    ))
                } catch (e: SecurityException) {
                    addMessage(ChatMessage.assistant("Microphone permission required for voice mode."))
                } catch (e: Exception) {
                    // Handle API failures - fall back to legacy voice
                    android.util.Log.e("ChatViewModel", "OpenAI voice failed", e)
                    addMessage(ChatMessage.assistant(
                        "Real-time voice unavailable. Switching to basic voice mode.\n" +
                        "Say \"AgentOS\" followed by your command."
                    ))
                    useOpenAIVoice = false
                    try {
                        voicePipeline.enableVoiceActivation()
                    } catch (e2: SecurityException) {
                        addMessage(ChatMessage.assistant("Microphone permission required."))
                    }
                }
            }
        } else {
            // Legacy voice with wake word
            if (voicePipeline.isEnabled.value) {
                voicePipeline.disableVoiceActivation()
                addMessage(ChatMessage.assistant("Voice activation disabled."))
            } else {
                try {
                    voicePipeline.enableVoiceActivation()
                    addMessage(ChatMessage.assistant(
                        "Voice activation enabled! Say \"AgentOS\" followed by your command.\n\n" +
                        "Examples:\n" +
                        "- \"AgentOS, open settings\"\n" +
                        "- \"Hey AgentOS, find coffee nearby\""
                    ))
                } catch (e: SecurityException) {
                    addMessage(ChatMessage.assistant("Microphone permission required for voice activation."))
                }
            }
        }
    }

    fun setMode(mode: InteractionMode) {
        _mode.value = mode
        val modeText = when (mode) {
            InteractionMode.CONVERSATION -> "Conversation mode - I'll explain and answer questions."
            InteractionMode.ACTION -> "Action mode - I'll perform tasks on your phone."
            InteractionMode.AUTO -> "Auto mode - I'll decide based on your request."
        }
        addMessage(ChatMessage.assistant(modeText))
    }

    fun clearConversation() {
        scope.launch {
            conversationRepository.clearConversation()
            conversationEngine.clearHistory()
            _uiState.update { state ->
                state.copy(
                    messages = listOf(
                        ChatMessage.assistant(
                            "Conversation cleared. How can I help you?"
                        )
                    )
                )
            }
        }
    }

    /**
     * Clean up resources when the ViewModel is no longer needed.
     * Call this when the app is being destroyed.
     */
    fun onCleared() {
        scope.cancel()
    }

    private fun addMessage(message: ChatMessage) {
        _uiState.update { state ->
            state.copy(messages = state.messages + message)
        }
    }
}

/**
 * Interaction modes for the chat.
 */
enum class InteractionMode {
    CONVERSATION,  // Chat only, no automation
    ACTION,        // Execute automation
    AUTO           // Detect intent automatically
}

/**
 * Mode indicator for UI display.
 */
enum class ModeIndicator {
    NONE,
    CONVERSATION,
    ACTION,
    SUGGESTS_ACTION
}

data class ChatUiState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage.assistant(
            "Hello! I'm AgentOS, your AI assistant. I can help you automate tasks on your phone.\n\n" +
            "Try saying things like:\n" +
            "- \"Open Settings and enable dark mode\"\n" +
            "- \"Search for coffee shops nearby\"\n" +
            "- \"Send a text to Mom saying I'll be late\"\n\n" +
            "You can also enable voice activation and say \"AgentOS\" followed by your command!"
        )
    ),
    val inputText: String = "",
    val isProcessing: Boolean = false,
    val currentAction: String? = null,
    val isAccessibilityEnabled: Boolean = false,
    val isOverlayEnabled: Boolean = false,
    val isVoiceEnabled: Boolean = false,
    val isVoiceActive: Boolean = false,
    val voiceStatus: String? = null,
    val workflowState: WorkflowState = WorkflowState.Idle,
    val modeIndicator: ModeIndicator = ModeIndicator.NONE
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, Any> = emptyMap()
) {
    companion object {
        fun user(content: String) = ChatMessage(content = content, isFromUser = true)
        fun assistant(content: String) = ChatMessage(content = content, isFromUser = false)
    }
}
