package com.agentos.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/**
 * Animated sphere that represents AgentOS state.
 * Similar to Siri's orb but with unique AgentOS styling.
 */
@Composable
fun AgentSphere(
    state: AgentSphereState,
    modifier: Modifier = Modifier,
    audioLevel: Float = 0f // 0-1 for audio reactivity
) {
    // Color definitions
    val cyanPrimary = Color(0xFF00D9FF)
    val cyanLight = Color(0xFF67E8FF)
    val cyanDark = Color(0xFF00A8CC)
    val purple = Color(0xFF9C27B0)
    val magenta = Color(0xFFE91E63)
    val amber = Color(0xFFFFC107)
    val green = Color(0xFF4CAF50)
    val red = Color(0xFFF44336)
    val navy = Color(0xFF0D1B2A)

    // State-based colors
    val primaryColor = remember(state) {
        when (state) {
            AgentSphereState.Idle -> cyanPrimary
            AgentSphereState.Listening -> cyanLight
            AgentSphereState.Thinking -> purple
            AgentSphereState.Speaking -> magenta
            AgentSphereState.Action -> purple
            AgentSphereState.NeedsHelp -> amber
            AgentSphereState.Complete -> green
            AgentSphereState.Error -> red
        }
    }

    val secondaryColor = remember(state) {
        when (state) {
            AgentSphereState.Idle -> cyanDark
            AgentSphereState.Listening -> cyanPrimary
            AgentSphereState.Thinking -> magenta
            AgentSphereState.Speaking -> purple
            AgentSphereState.Action -> cyanPrimary
            AgentSphereState.NeedsHelp -> Color(0xFFFF9800)
            AgentSphereState.Complete -> Color(0xFF81C784)
            AgentSphereState.Error -> Color(0xFFEF5350)
        }
    }

    // Infinite transition for continuous animations
    val infiniteTransition = rememberInfiniteTransition(label = "sphere")

    // Idle breathing animation
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    // Rotation for thinking state
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    // Pulse for action state
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Ripple phase for speaking
    val ripplePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing)
        ),
        label = "ripple"
    )

    // Bounce for needs help
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    // Shake for error
    val shakeOffset by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shake"
    )

    // Animated color transition
    val animatedPrimaryColor by animateColorAsState(
        targetValue = primaryColor,
        animationSpec = tween(400),
        label = "primaryColor"
    )

    val animatedSecondaryColor by animateColorAsState(
        targetValue = secondaryColor,
        animationSpec = tween(400),
        label = "secondaryColor"
    )

    // Flash animation for complete state
    var showFlash by remember { mutableStateOf(false) }
    LaunchedEffect(state) {
        if (state == AgentSphereState.Complete) {
            showFlash = true
            kotlinx.coroutines.delay(300)
            showFlash = false
        }
    }

    Canvas(modifier = modifier.size(120.dp)) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val baseRadius = size.minDimension / 2 * 0.8f

        // Apply state-specific transformations
        val (finalScale, offsetX, offsetY, finalRotation) = when (state) {
            AgentSphereState.Idle -> SphereTransform(breatheScale, 0f, 0f, 0f)
            AgentSphereState.Listening -> SphereTransform(1f + audioLevel * 0.15f, 0f, 0f, 0f)
            AgentSphereState.Thinking -> SphereTransform(breatheScale, 0f, 0f, rotation)
            AgentSphereState.Speaking -> SphereTransform(breatheScale, 0f, 0f, 0f)
            AgentSphereState.Action -> SphereTransform(pulse, 0f, 0f, 0f)
            AgentSphereState.NeedsHelp -> SphereTransform(1f, 0f, -bounceOffset, 0f)
            AgentSphereState.Complete -> SphereTransform(if (showFlash) 1.3f else breatheScale, 0f, 0f, 0f)
            AgentSphereState.Error -> SphereTransform(1f, shakeOffset, 0f, 0f)
        }

        // Apply transformations
        scale(finalScale) {
            rotate(finalRotation) {
                // Draw outer glow
                drawGlow(
                    centerX + offsetX,
                    centerY + offsetY,
                    baseRadius,
                    animatedPrimaryColor,
                    state
                )

                // Draw ripples for speaking state
                if (state == AgentSphereState.Speaking) {
                    drawRipples(centerX, centerY, baseRadius, ripplePhase, animatedPrimaryColor)
                }

                // Draw main sphere
                drawSphere(
                    centerX + offsetX,
                    centerY + offsetY,
                    baseRadius,
                    animatedPrimaryColor,
                    animatedSecondaryColor,
                    navy
                )

                // Draw highlight
                drawHighlight(centerX + offsetX, centerY + offsetY - baseRadius * 0.3f, baseRadius * 0.4f)

                // Draw audio visualization for listening
                if (state == AgentSphereState.Listening && audioLevel > 0) {
                    drawAudioBars(centerX, centerY, baseRadius, audioLevel, animatedPrimaryColor)
                }

                // Draw flash for complete
                if (showFlash) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.6f),
                        radius = baseRadius * 1.5f,
                        center = Offset(centerX, centerY)
                    )
                }
            }
        }
    }
}

private data class SphereTransform(
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float,
    val rotation: Float
)

private fun DrawScope.drawGlow(
    centerX: Float,
    centerY: Float,
    radius: Float,
    color: Color,
    state: AgentSphereState
) {
    val glowAlpha = when (state) {
        AgentSphereState.Action -> 0.5f
        AgentSphereState.Speaking -> 0.4f
        AgentSphereState.Listening -> 0.35f
        else -> 0.25f
    }

    val glowRadius = when (state) {
        AgentSphereState.Action -> radius * 1.5f
        AgentSphereState.Speaking -> radius * 1.4f
        else -> radius * 1.3f
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = glowAlpha),
                color.copy(alpha = glowAlpha * 0.5f),
                Color.Transparent
            ),
            center = Offset(centerX, centerY),
            radius = glowRadius
        ),
        radius = glowRadius,
        center = Offset(centerX, centerY)
    )
}

private fun DrawScope.drawSphere(
    centerX: Float,
    centerY: Float,
    radius: Float,
    primaryColor: Color,
    secondaryColor: Color,
    coreColor: Color
) {
    // Outer gradient
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(primaryColor, secondaryColor),
            center = Offset(centerX, centerY - radius * 0.3f),
            radius = radius * 1.2f
        ),
        radius = radius,
        center = Offset(centerX, centerY)
    )

    // Inner dark core
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                coreColor.copy(alpha = 0.9f),
                coreColor.copy(alpha = 0.7f),
                Color.Transparent
            ),
            center = Offset(centerX, centerY),
            radius = radius * 0.7f
        ),
        radius = radius * 0.65f,
        center = Offset(centerX, centerY)
    )
}

private fun DrawScope.drawHighlight(
    centerX: Float,
    centerY: Float,
    radius: Float
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.4f),
                Color.White.copy(alpha = 0.1f),
                Color.Transparent
            ),
            center = Offset(centerX, centerY),
            radius = radius
        ),
        radius = radius,
        center = Offset(centerX, centerY)
    )
}

private fun DrawScope.drawRipples(
    centerX: Float,
    centerY: Float,
    baseRadius: Float,
    phase: Float,
    color: Color
) {
    val numRipples = 3
    for (i in 0 until numRipples) {
        val ripplePhase = (phase + i.toFloat() / numRipples) % 1f
        val rippleRadius = baseRadius * (1f + ripplePhase * 0.5f)
        val rippleAlpha = (1f - ripplePhase) * 0.3f

        drawCircle(
            color = color.copy(alpha = rippleAlpha),
            radius = rippleRadius,
            center = Offset(centerX, centerY),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
        )
    }
}

private fun DrawScope.drawAudioBars(
    centerX: Float,
    centerY: Float,
    radius: Float,
    audioLevel: Float,
    color: Color
) {
    val numBars = 5
    val barWidth = radius * 0.1f
    val maxBarHeight = radius * 0.5f
    val startX = centerX - (numBars - 1) * barWidth

    for (i in 0 until numBars) {
        val barHeight = maxBarHeight * audioLevel * (0.5f + sin(i * 1.5f + audioLevel * 10) * 0.5f)
        val x = startX + i * barWidth * 2

        drawLine(
            color = color.copy(alpha = 0.8f),
            start = Offset(x, centerY + barHeight / 2),
            end = Offset(x, centerY - barHeight / 2),
            strokeWidth = barWidth
        )
    }
}

/**
 * Visual states for the AgentSphere.
 */
enum class AgentSphereState {
    Idle,       // Blue gradient, subtle breathe
    Listening,  // Cyan glow, pulse reactive to audio
    Thinking,   // Purple shift, rotate + morph
    Speaking,   // Magenta accent, wave ripple outward
    Action,     // Purple core, energetic pulse
    NeedsHelp,  // Amber tint, gentle bounce
    Complete,   // Green flash, burst then fade
    Error       // Red tint, shake
}

private val EaseInOutSine = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)
