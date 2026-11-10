package com.gathin.tvhome

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.animation.DecelerateInterpolator

/** Smooth temperature line that "draws itself" from left to right. */
class HourlyChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var temps: IntArray = intArrayOf(25, 25, 25, 27, 29, 29, 28, 25)
        set(value) { field = value; invalidate() }

    var unit: String = "°"
        set(value) { field = value; invalidate() }

    var labels: Array<String> = arrayOf("12 am", "3 am", "6 am", "9 am", "12 pm", "3 pm", "6 pm", "9 pm")

    private var reveal = 0f
    private var animator: ValueAnimator? = null

    private val accent = 0xFF4FC3F7.toInt()

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = dp(3f); color = accent; strokeCap = Paint.Cap.ROUND
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = dp(10f); color = accent; alpha = 50; strokeCap = Paint.Cap.ROUND
    }
    private val dotOuter = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }
    private val dotInner = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val tempText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = sp(16f); textAlign = Paint.Align.CENTER; isFakeBoldText = true
    }
    private val hourText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xB3FFFFFF.toInt(); textSize = sp(13f); textAlign = Paint.Align.CENTER
    }
    private val basePaint = Paint().apply { color = 0x22FFFFFF; strokeWidth = dp(1f) }

    private val path = Path()
    private val segment = Path()

    fun animateIn(delayMs: Long = 0L) {
        animator?.cancel()
        reveal = 0f
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1400L
            startDelay = delayMs
            interpolator = DecelerateInterpolator()
            addUpdateListener { reveal = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val n = temps.size
        if (n < 2) return

        val padX = dp(30f)
        val top = dp(34f)
        val bottom = height - dp(34f)
        val left = padX
        val right = width - padX
        val minT = if (unit == "%") 0 else ((temps.minOrNull() ?: 0) - 1)
        val maxT = if (unit == "%") ((temps.maxOrNull() ?: 100).coerceAtLeast(30) + 5) else ((temps.maxOrNull() ?: 1) + 1)
        val range = (maxT - minT).coerceAtLeast(1)

        val xs = FloatArray(n) { left + it * (right - left) / (n - 1) }
        val ys = FloatArray(n) { bottom - (temps[it] - minT).toFloat() / range * (bottom - top) }

        path.reset()
        path.moveTo(xs[0], ys[0])
        for (i in 1 until n) {
            val midX = (xs[i - 1] + xs[i]) / 2f
            path.cubicTo(midX, ys[i - 1], midX, ys[i], xs[i], ys[i])
        }

        canvas.drawLine(left, bottom + dp(4f), right, bottom + dp(4f), basePaint)

        val pm = PathMeasure(path, false)
        segment.reset()
        pm.getSegment(0f, pm.length * reveal, segment, true)
        canvas.drawPath(segment, glowPaint)
        canvas.drawPath(segment, linePaint)

        val revealX = left + (right - left) * reveal
        for (i in 0 until n) {
            if (i < labels.size) canvas.drawText(labels[i], xs[i], height - dp(8f), hourText)
            if (xs[i] <= revealX + 1f) {
                canvas.drawCircle(xs[i], ys[i], dp(6f), dotOuter)
                canvas.drawCircle(xs[i], ys[i], dp(3f), dotInner)
                canvas.drawText("${temps[i]}$unit", xs[i], ys[i] - dp(14f), tempText)
            }
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }

    private fun dp(v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics)
    private fun sp(v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, resources.displayMetrics)
}
