package com.agentos.ui.overlay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agentos.ui.chat.ChatMessage
import com.agentos.ui.chat.ChatViewModel
import com.agentos.ui.components.AgentSphere
import com.agentos.ui.components.AgentSphereState
import com.agentos.ui.components.StepIndicatorBar
import com.agentos.ui.theme.AgentOSColors
import com.agentos.ui.theme.AgentOSTheme
import com.agentos.workflow.WorkflowState

/**
 * Full-screen floating overlay for AgentOS interaction.
 * Features animated sphere, chat area, and step indicator.
 */
@Composable
fun FloatingAgentOverlay(
    viewModel: ChatViewModel,
    onMinimize: () -> Unit,
    onClose: () -> Unit,
    startInVoiceMode: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Derive sphere state from workflow state
    val sphereState = remember(uiState.workflowState, uiState.isVoiceActive, uiState.isProcessing) {
        when {
            uiState.isVoiceActive -> AgentSphereState.Listening
            uiState.workflowState is WorkflowState.Running -> AgentSphereState.Action
            uiState.workflowState is WorkflowState.NeedsClarification -> AgentSphereState.NeedsHelp
            uiState.workflowState is WorkflowState.Error -> AgentSphereState.Error
            uiState.workflowState is WorkflowState.Completed -> AgentSphereState.Complete
            uiState.isProcessing -> AgentSphereState.Thinking
            else -> AgentSphereState.Idle
        }
    }

    // Auto-scroll when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // Start voice mode if requested
    LaunchedEffect(startInVoiceMode) {
        if (startInVoiceMode && !uiState.isVoiceEnabled) {
            viewModel.toggleVoice()
        }
    }

    AgentOSTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            AgentOSColors.NavyDark.copy(alpha = 0.95f),
                            AgentOSColors.NavyPrimary.copy(alpha = 0.98f)
                        )
                    )
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { /* Consume taps to prevent pass-through */ }
                    )
                }
        ) {
            // Screen outline overlay for workflow state indication
            ScreenOutlineOverlay(
                workflowState = uiState.workflowState,
                modifier = Modifier.fillMaxSize()
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                // Top bar with controls
                OverlayTopBar(
                    onMinimize = onMinimize,
                    onClose = onClose,
                    isVoiceEnabled = uiState.isVoiceEnabled,
                    onToggleVoice = viewModel::toggleVoice
                )

                // Animated sphere
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AgentSphere(
                        state = sphereState,
                        modifier = Modifier.size(120.dp)
                    )
                }

                // Chat messages area
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(
                        items = uiState.messages,
                        key = { it.id }
                    ) { message ->
                        OverlayChatBubble(message = message)
                    }
                }

                // Step indicator (shows current action)
                AnimatedVisibility(
                    visible = uiState.isProcessing && uiState.currentAction != null,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    StepIndicatorBar(
                        stepText = uiState.currentAction ?: "",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                // Input bar
                OverlayInputBar(
                    inputText = uiState.inputText,
                    onInputChange = viewModel::updateInputText,
                    onSend = { viewModel.sendMessage(uiState.inputText) },
                    isProcessing = uiState.isProcessing,
                    onCancel = viewModel::cancelTask,
                    isVoiceEnabled = uiState.isVoiceEnabled,
                    isVoiceActive = uiState.isVoiceActive,
                    voiceStatus = uiState.voiceStatus
                )
            }
        }
    }
}

@Composable
private fun OverlayTopBar(
    onMinimize: () -> Unit,
    onClose: () -> Unit,
    isVoiceEnabled: Boolean,
    onToggleVoice: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Minimize button
        IconButton(onClick = onMinimize) {
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Minimize",
                tint = AgentOSColors.Silver
            )
        }

        // Title
        Text(
            text = "AgentOS",
            style = MaterialTheme.typography.titleMedium,
            color = AgentOSColors.CyanPrimary
        )

        Row {
            // Voice toggle
            IconButton(
                onClick = onToggleVoice,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = if (isVoiceEnabled) AgentOSColors.CyanPrimary
                                   else AgentOSColors.SilverDark
                )
            ) {
                Icon(
                    if (isVoiceEnabled) Icons.Filled.Mic else Icons.Filled.MicOff,
                    contentDescription = if (isVoiceEnabled) "Disable voice" else "Enable voice"
                )
            }

            // Close button
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = AgentOSColors.Silver
                )
            }
        }
    }
}

@Composable
private fun OverlayChatBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val isUser = message.isFromUser

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser)
                AgentOSColors.CyanDark.copy(alpha = 0.3f)
            else
                AgentOSColors.NavySurface.copy(alpha = 0.7f),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUser) AgentOSColors.CyanLight else AgentOSColors.SilverLight
            )
        }
    }
}

@Composable
private fun OverlayInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    isProcessing: Boolean,
    onCancel: () -> Unit,
    isVoiceEnabled: Boolean,
    isVoiceActive: Boolean,
    voiceStatus: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AgentOSColors.NavyPrimary.copy(alpha = 0.9f))
    ) {
        // Voice status indicator
        AnimatedVisibility(
            visible = isVoiceEnabled && voiceStatus != null,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isVoiceActive) AgentOSColors.CyanDark.copy(alpha = 0.2f)
                        else AgentOSColors.NavySurface.copy(alpha = 0.5f)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isVoiceActive) Icons.Filled.Mic else Icons.Filled.MicOff,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isVoiceActive) AgentOSColors.CyanPrimary else AgentOSColors.SilverDark
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = voiceStatus ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isVoiceActive) AgentOSColors.CyanLight else AgentOSColors.Silver,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Input row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        if (isVoiceEnabled) "Say \"AgentOS\" or type..."
                        else "Ask me anything...",
                        color = AgentOSColors.SilverDark
                    )
                },
                shape = RoundedCornerShape(24.dp),
                enabled = !isProcessing,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AgentOSColors.CyanPrimary,
                    unfocusedBorderColor = AgentOSColors.NavyLight,
                    focusedTextColor = AgentOSColors.SilverLight,
                    unfocusedTextColor = AgentOSColors.Silver,
                    cursorColor = AgentOSColors.CyanPrimary
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            if (isProcessing) {
                FilledIconButton(
                    onClick = onCancel,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = AgentOSColors.Error.copy(alpha = 0.8f)
                    )
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Cancel",
                        tint = Color.White
                    )
                }
            } else {
                FilledIconButton(
                    onClick = onSend,
                    enabled = inputText.isNotBlank(),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = AgentOSColors.CyanPrimary,
                        disabledContainerColor = AgentOSColors.NavyLight
                    )
                ) {
                    Icon(
                        Icons.Filled.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank()) AgentOSColors.NavyDark
                               else AgentOSColors.SilverDark
                    )
                }
            }
        }
    }
}
