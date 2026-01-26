package com.agentos.ui.overlay

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.agentos.workflow.WorkflowState
import kotlin.math.min

/**
 * Full-screen overlay that draws animated border around screen edges.
 * Indicates agent working state through color and animation.
 */
@Composable
fun ScreenOutlineOverlay(
    workflowState: WorkflowState,
    modifier: Modifier = Modifier
) {
    val outlineState = remember(workflowState) {
        when (workflowState) {
            is WorkflowState.Running -> OutlineState.Working
            is WorkflowState.NeedsClarification -> OutlineState.NeedsClarification
            is WorkflowState.Completed -> OutlineState.Complete
            is WorkflowState.Error -> OutlineState.Error
            else -> OutlineState.Hidden
        }
    }

    // Don't render if hidden
    if (outlineState == OutlineState.Hidden) return

    val targetColor = when (outlineState) {
        OutlineState.Working -> Color(0xFF2196F3) // Blue
        OutlineState.NeedsClarification -> Color(0xFFFFC107) // Amber
        OutlineState.Complete -> Color(0xFF4CAF50) // Green
        OutlineState.Error -> Color(0xFFF44336) // Red
        OutlineState.Hidden -> Color.Transparent
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(400),
        label = "outlineColor"
    )

    // Gradient sweep animation
    val infiniteTransition = rememberInfiniteTransition(label = "outline")

    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "sweep"
    )

    // Glow pulse
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    // Fade out for complete state
    var completeFade by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(outlineState) {
        if (outlineState == OutlineState.Complete) {
            kotlinx.coroutines.delay(1500)
            animate(1f, 0f, animationSpec = tween(500)) { value, _ ->
                completeFade = value
            }
        } else {
            completeFade = 1f
        }
    }

    val finalAlpha = if (outlineState == OutlineState.Complete) completeFade else 1f

    Canvas(modifier = modifier.fillMaxSize()) {
        if (finalAlpha <= 0f) return@Canvas

        val strokeWidth = 8.dp.toPx()
        val glowWidth = 16.dp.toPx()
        val cornerRadius = 24.dp.toPx()

        // Draw glow layer
        drawOutlineGlow(
            color = animatedColor.copy(alpha = glowAlpha * finalAlpha),
            strokeWidth = glowWidth,
            cornerRadius = cornerRadius
        )

        // Draw main outline with gradient sweep
        drawAnimatedOutline(
            color = animatedColor.copy(alpha = finalAlpha),
            strokeWidth = strokeWidth,
            cornerRadius = cornerRadius,
            sweepAngle = sweepAngle
        )
    }
}

private fun DrawScope.drawOutlineGlow(
    color: Color,
    strokeWidth: Float,
    cornerRadius: Float
) {
    val path = createRoundedRectPath(strokeWidth / 2, cornerRadius)

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawAnimatedOutline(
    color: Color,
    strokeWidth: Float,
    cornerRadius: Float,
    sweepAngle: Float
) {
    val path = createRoundedRectPath(strokeWidth / 2, cornerRadius)
    val pathMeasure = PathMeasure()
    pathMeasure.setPath(path, false)

    val totalLength = pathMeasure.length
    val startDistance = (sweepAngle / 360f) * totalLength
    val segmentLength = totalLength * 0.3f // 30% of path is bright

    // Create gradient effect along the path
    val brush = Brush.sweepGradient(
        0f to color.copy(alpha = 0.2f),
        0.3f to color,
        0.5f to color,
        0.7f to color.copy(alpha = 0.2f),
        1f to color.copy(alpha = 0.2f)
    )

    drawPath(
        path = path,
        brush = brush,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
}

private fun DrawScope.createRoundedRectPath(
    inset: Float,
    cornerRadius: Float
): Path {
    val adjustedRadius = min(cornerRadius, min(size.width, size.height) / 2 - inset)

    return Path().apply {
        // Start from top-left corner (after the curve)
        moveTo(inset + adjustedRadius, inset)

        // Top edge
        lineTo(size.width - inset - adjustedRadius, inset)

        // Top-right corner
        arcTo(
            rect = androidx.compose.ui.geometry.Rect(
                left = size.width - inset - adjustedRadius * 2,
                top = inset,
                right = size.width - inset,
                bottom = inset + adjustedRadius * 2
            ),
            startAngleDegrees = -90f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false
        )

        // Right edge
        lineTo(size.width - inset, size.height - inset - adjustedRadius)

        // Bottom-right corner
        arcTo(
            rect = androidx.compose.ui.geometry.Rect(
                left = size.width - inset - adjustedRadius * 2,
                top = size.height - inset - adjustedRadius * 2,
                right = size.width - inset,
                bottom = size.height - inset
            ),
            startAngleDegrees = 0f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false
        )

        // Bottom edge
        lineTo(inset + adjustedRadius, size.height - inset)

        // Bottom-left corner
        arcTo(
            rect = androidx.compose.ui.geometry.Rect(
                left = inset,
                top = size.height - inset - adjustedRadius * 2,
                right = inset + adjustedRadius * 2,
                bottom = size.height - inset
            ),
            startAngleDegrees = 90f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false
        )

        // Left edge
        lineTo(inset, inset + adjustedRadius)

        // Top-left corner
        arcTo(
            rect = androidx.compose.ui.geometry.Rect(
                left = inset,
                top = inset,
                right = inset + adjustedRadius * 2,
                bottom = inset + adjustedRadius * 2
            ),
            startAngleDegrees = 180f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false
        )

        close()
    }
}

/**
 * States for the screen outline overlay.
 */
enum class OutlineState {
    Hidden,
    Working,            // Blue - actively executing
    NeedsClarification, // Amber - waiting for user input
    Complete,           // Green - briefly flash then hide
    Error               // Red - error occurred
}

private val EaseInOutSine = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)
