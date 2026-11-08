package dev.cadence.app.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator

class FluidEqualizerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val barPaints = mutableListOf<Paint>()
    private val barRect = RectF()
    private val barCount = 5
    private val barFractions = FloatArray(barCount) { 0.2f }
    private val barTargets = FloatArray(barCount) { 0.2f }

    private var animator: ValueAnimator? = null
    private var isAnimating = false

    private val colorStart = Color.parseColor("#00E5FF")
    private val colorEnd = Color.parseColor("#818CF8")

    init {
        for (i in 0 until barCount) {
            barPaints.add(Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
            })
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val shader = LinearGradient(
            0f, h.toFloat(), 0f, 0f,
            colorStart, colorEnd,
            Shader.TileMode.CLAMP
        )
        barPaints.forEach { it.shader = shader }
    }

    fun setPlaying(playing: Boolean) {
        if (playing == isAnimating) return
        isAnimating = playing

        if (playing) {
            startAnimationLoop()
        } else {
            stopAnimationLoop()
        }
    }

    private fun startAnimationLoop() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 320
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = LinearInterpolator()
            addUpdateListener {
                val fraction = it.animatedFraction
                barFractions[0] = 0.25f + 0.65f * Math.abs(Math.sin((fraction * Math.PI) + 0.2).toFloat())
                barFractions[1] = 0.35f + 0.60f * Math.abs(Math.sin((fraction * Math.PI) + 1.1).toFloat())
                barFractions[2] = 0.20f + 0.80f * Math.abs(Math.sin((fraction * Math.PI) + 2.0).toFloat())
                barFractions[3] = 0.30f + 0.65f * Math.abs(Math.sin((fraction * Math.PI) + 2.9).toFloat())
                barFractions[4] = 0.25f + 0.70f * Math.abs(Math.sin((fraction * Math.PI) + 3.8).toFloat())
                invalidate()
            }
            start()
        }
    }

    private fun stopAnimationLoop() {
        animator?.cancel()
        animator = null
        for (i in 0 until barCount) {
            barFractions[i] = 0.08f
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val totalBarWidth = w * 0.75f
        val barWidth = totalBarWidth / barCount
        val spacing = (w - totalBarWidth) / (barCount + 1)
        val cornerRadius = barWidth / 2f

        for (i in 0 until barCount) {
            val left = spacing + i * (barWidth + spacing)
            val right = left + barWidth
            val barHeight = h * barFractions[i].coerceIn(0.15f, 1.0f)
            val top = h - barHeight
            val bottom = h

            barRect.set(left, top, right, bottom)
            canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, barPaints[i])
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }
}
