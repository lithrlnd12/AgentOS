package com.agentos.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agentos.accessibility.AgentAccessibilityService
import com.agentos.voice.VoicePipeline
import com.agentos.voice.VoiceState
import com.agentos.workflow.WorkflowEngine
import com.agentos.workflow.WorkflowState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(
    private val workflowEngine: WorkflowEngine,
    private val voicePipeline: VoicePipeline? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        // Observe workflow state
        viewModelScope.launch {
            workflowEngine.state.collect { workflowState ->
                _uiState.update { state ->
                    state.copy(
                        isProcessing = workflowState is WorkflowState.Running,
                        currentAction = when (workflowState) {
                            is WorkflowState.Running -> workflowState.description
                            else -> null
                        }
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
        viewModelScope.launch {
            AgentAccessibilityService.instance.collect { service ->
                _uiState.update { it.copy(isAccessibilityEnabled = service != null) }
            }
        }

        // Observe voice state
        voicePipeline?.let { pipeline ->
            viewModelScope.launch {
                pipeline.state.collect { voiceState ->
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

                    // Add message when command is detected via voice
                    if (voiceState is VoiceState.Processing) {
                        addMessage(ChatMessage.user("[Voice] ${voiceState.command}"))
                    }
                }
            }

            viewModelScope.launch {
                pipeline.isEnabled.collect { enabled ->
                    _uiState.update { it.copy(isVoiceEnabled = enabled) }
                }
            }
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return

        // Add user message
        addMessage(ChatMessage.user(content))

        // Clear input
        _uiState.update { it.copy(inputText = "") }

        // Check if accessibility is enabled
        if (!_uiState.value.isAccessibilityEnabled) {
            addMessage(ChatMessage.assistant(
                "I need accessibility permissions to control your phone. " +
                "Please enable AgentOS in Settings > Accessibility."
            ))
            return
        }

        // Start workflow
        viewModelScope.launch {
            workflowEngine.execute(content)
        }
    }

    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun cancelTask() {
        workflowEngine.cancel()
        addMessage(ChatMessage.assistant("Task cancelled."))
    }

    fun toggleVoice() {
        voicePipeline?.let { pipeline ->
            if (pipeline.isEnabled.value) {
                pipeline.disableVoiceActivation()
                addMessage(ChatMessage.assistant("Voice activation disabled."))
            } else {
                // Note: Caller must ensure RECORD_AUDIO permission is granted
                try {
                    pipeline.enableVoiceActivation()
                    addMessage(ChatMessage.assistant(
                        "Voice activation enabled! Say \"AgentOS\" followed by your command.\n\n" +
                        "Examples:\n" +
                        "• \"AgentOS, open settings\"\n" +
                        "• \"Hey AgentOS, find coffee nearby\""
                    ))
                } catch (e: SecurityException) {
                    addMessage(ChatMessage.assistant("Microphone permission required for voice activation."))
                }
            }
        } ?: run {
            addMessage(ChatMessage.assistant("Voice features not available."))
        }
    }

    private fun addMessage(message: ChatMessage) {
        _uiState.update { state ->
            state.copy(messages = state.messages + message)
        }
    }
}

data class ChatUiState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage.assistant(
            "Hello! I'm AgentOS, your AI assistant. I can help you automate tasks on your phone.\n\n" +
            "Try saying things like:\n" +
            "• \"Open Settings and enable dark mode\"\n" +
            "• \"Search for coffee shops nearby\"\n" +
            "• \"Send a text to Mom saying I'll be late\"\n\n" +
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
    val voiceStatus: String? = null
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun user(content: String) = ChatMessage(content = content, isFromUser = true)
        fun assistant(content: String) = ChatMessage(content = content, isFromUser = false)
    }
}
