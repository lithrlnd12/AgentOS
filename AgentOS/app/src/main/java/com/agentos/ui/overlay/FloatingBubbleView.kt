package com.agentos.ui.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

/**
 * Floating bubble view that provides quick access to AgentOS.
 * Features subtle pulse animation and drag-to-move behavior.
 */
class FloatingBubbleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val bubbleSize = 56.dpToPx()
    private val padding = 4.dpToPx()

    private val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 24.dpToPx().toFloat()
        textAlign = Paint.Align.CENTER
    }

    // Animation
    private var pulseScale = 1f
    private val pulseAnimator = ValueAnimator.ofFloat(1f, 1.08f, 1f).apply {
        duration = 2000
        repeatCount = ValueAnimator.INFINITE
        interpolator = AccelerateDecelerateInterpolator()
        addUpdateListener {
            pulseScale = it.animatedValue as Float
            invalidate()
        }
    }

    // Colors
    private val cyanPrimary = Color.parseColor("#00D9FF")
    private val cyanDark = Color.parseColor("#00A8CC")
    private val navyDark = Color.parseColor("#0D1B2A")

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null) // Required for blur effect
        pulseAnimator.start()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = bubbleSize + padding * 2
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val baseRadius = bubbleSize / 2f
        val scaledRadius = baseRadius * pulseScale

        // Draw glow
        glowPaint.shader = RadialGradient(
            centerX, centerY, scaledRadius * 1.3f,
            intArrayOf(
                Color.argb(60, 0, 217, 255),
                Color.argb(20, 0, 217, 255),
                Color.TRANSPARENT
            ),
            floatArrayOf(0.5f, 0.8f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(centerX, centerY, scaledRadius * 1.3f, glowPaint)

        // Draw outer circle with gradient
        outerPaint.shader = RadialGradient(
            centerX, centerY - baseRadius * 0.3f, scaledRadius,
            intArrayOf(cyanPrimary, cyanDark),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(centerX, centerY, scaledRadius, outerPaint)

        // Draw inner dark circle
        innerPaint.color = navyDark
        canvas.drawCircle(centerX, centerY, scaledRadius * 0.75f, innerPaint)

        // Draw "A" logo (simplified AgentOS icon)
        val textY = centerY + iconPaint.textSize / 3
        canvas.drawText("A", centerX, textY, iconPaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pulseAnimator.cancel()
    }

    private fun Int.dpToPx(): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }
}
