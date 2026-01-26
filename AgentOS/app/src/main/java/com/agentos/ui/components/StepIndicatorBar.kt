package com.agentos.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agentos.ui.theme.AgentOSColors

/**
 * Bottom pill indicator showing current workflow step.
 * Features crossfade animation between steps and auto-hide behavior.
 */
@Composable
fun StepIndicatorBar(
    stepText: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    // Derive icon from step text if not provided
    val derivedIcon = icon ?: remember(stepText) {
        deriveIconFromStep(stepText)
    }

    Box(
        modifier = modifier
            .fillMaxWidth(0.85f)
            .clip(RoundedCornerShape(24.dp))
            .background(AgentOSColors.NavySurface.copy(alpha = 0.9f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Animated icon with crossfade
            AnimatedContent(
                targetState = derivedIcon,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith
                            fadeOut(animationSpec = tween(200))
                },
                label = "icon"
            ) { targetIcon ->
                Icon(
                    imageVector = targetIcon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = AgentOSColors.CyanPrimary
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Animated text with crossfade
            AnimatedContent(
                targetState = stepText,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith
                            fadeOut(animationSpec = tween(200))
                },
                label = "text"
            ) { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AgentOSColors.SilverLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Progress dots
            StepProgressDots()
        }
    }
}

@Composable
private fun StepProgressDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")

    val dotAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(0)
        ),
        label = "dot1"
    )

    val dotAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(200)
        ),
        label = "dot2"
    )

    val dotAlpha3 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(400)
        ),
        label = "dot3"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(AgentOSColors.CyanPrimary.copy(alpha = dotAlpha1))
        )
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(AgentOSColors.CyanPrimary.copy(alpha = dotAlpha2))
        )
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(AgentOSColors.CyanPrimary.copy(alpha = dotAlpha3))
        )
    }
}

private fun deriveIconFromStep(stepText: String): ImageVector {
    val lowerText = stepText.lowercase()
    return when {
        lowerText.contains("tap") || lowerText.contains("click") -> Icons.Default.TouchApp
        lowerText.contains("swipe") || lowerText.contains("scroll") -> Icons.Default.SwipeVertical
        lowerText.contains("type") || lowerText.contains("enter") -> Icons.Default.Keyboard
        lowerText.contains("open") || lowerText.contains("launch") -> Icons.Default.OpenInNew
        lowerText.contains("back") -> Icons.Default.ArrowBack
        lowerText.contains("home") -> Icons.Default.Home
        lowerText.contains("settings") -> Icons.Default.Settings
        lowerText.contains("search") -> Icons.Default.Search
        lowerText.contains("send") -> Icons.Default.Send
        lowerText.contains("wait") || lowerText.contains("loading") -> Icons.Default.HourglassTop
        lowerText.contains("analyz") || lowerText.contains("think") -> Icons.Default.Psychology
        lowerText.contains("complet") || lowerText.contains("done") -> Icons.Default.CheckCircle
        lowerText.contains("error") || lowerText.contains("fail") -> Icons.Default.Error
        else -> Icons.Default.PlayArrow
    }
}

/**
 * Data class representing a workflow step for history tracking.
 */
data class WorkflowStep(
    val icon: ImageVector,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Extended step indicator with history support.
 * Shows current step and allows swiping to see history.
 */
@Composable
fun StepIndicatorWithHistory(
    currentStep: WorkflowStep?,
    stepHistory: List<WorkflowStep>,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    var showHistory by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = currentStep != null,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Current step
            currentStep?.let { step ->
                StepIndicatorBar(
                    stepText = step.description,
                    icon = step.icon,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // History expansion indicator
            if (stepHistory.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(AgentOSColors.SilverDark.copy(alpha = 0.5f))
                )
            }
        }
    }
}
