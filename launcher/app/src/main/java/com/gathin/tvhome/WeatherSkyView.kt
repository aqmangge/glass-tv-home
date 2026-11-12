package com.gathin.tvhome

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.sin
import kotlin.random.Random

/**
 * Animated sky background (replaces the static .png).
 * blend = 0f -> Thunderstorm (dark clouds, rain, lightning)
 * blend = 1f -> Mostly Sunny (white clouds, sun glow)
 */
class WeatherSkyView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    enum class Condition { STORM, SUNNY }

    var currentCondition: Condition = Condition.STORM
        private set

    private var blend = 0f
    private var blendAnimator: ValueAnimator? = null
    private var time = 0f
    private var lastFrameNs = 0L
    private var flash = 0f

    // ---------- Colours ----------
    private val stormTop = 0xFF141A24.toInt()
    private val stormBottom = 0xFF3C4552.toInt()
    private val sunnyTop = 0xFF5F7FB8.toInt()
    private val sunnyBottom = 0xFFBFD0E8.toInt()
    private val stormCloud = 0xFF2B3039.toInt()
    private val sunnyCloud = 0xFFFFFFFF.toInt()

    // ---------- Paints ----------
    private val skyPaint = Paint()
    private val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val rainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 2f
        strokeCap = Paint.Cap.ROUND
    }
    private val flashPaint = Paint().apply { color = Color.WHITE }
    private val scrimPaint = Paint()

    // Soft round "puff" used to build every cloud (drawn once, reused)
    private val puff: Bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888).also { bmp ->
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = RadialGradient(
            128f, 128f, 128f,
            intArrayOf(0xFFFFFFFF.toInt(), 0xAAFFFFFF.toInt(), 0x00FFFFFF),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        c.drawCircle(128f, 128f, 128f, p)
    }

    // ---------- Clouds ----------
    private class Puff(val dx: Float, val dy: Float, val r: Float)
    private class Cloud(
        var x: Float,          // fraction of width
        val y: Float,          // fraction of height
        val size: Float,       // fraction of height
        val speed: Float,      // fraction of width per second
        val alpha: Float,
        val puffs: List<Puff>
    )

    private val clouds = mutableListOf<Cloud>()
    private val rect = RectF()

    // ---------- Rain ----------
    private class Drop(var x: Float, var y: Float, val len: Float, val speed: Float)
    private val drops = List(160) {
        Drop(Random.nextFloat(), Random.nextFloat(), 18f + Random.nextFloat() * 22f, 0.9f + Random.nextFloat() * 0.8f)
    }

    init {
        // Back layer: small, faint, slow
        repeat(6) { clouds += makeCloud(Random.nextFloat() * 1.4f - 0.2f, 0.08f + Random.nextFloat() * 0.3f, 0.25f, 0.006f, 0.45f) }
        // Hero cloud (centre-right, like the video)
        clouds += makeCloud(0.55f, 0.38f, 0.55f, 0.004f, 0.95f)
        // Front layer: bigger, faster, low
        repeat(4) { clouds += makeCloud(Random.nextFloat() * 1.4f - 0.2f, 0.65f + Random.nextFloat() * 0.25f, 0.45f, 0.012f, 0.6f) }
    }

    private fun makeCloud(x: Float, y: Float, size: Float, speed: Float, alpha: Float): Cloud {
        val puffs = List(8) {
            Puff(
                dx = (Random.nextFloat() - 0.5f) * 1.6f,
                dy = (Random.nextFloat() - 0.5f) * 0.5f,
                r = 0.35f + Random.nextFloat() * 0.35f
            )
        }
        return Cloud(x, y, size * (0.8f + Random.nextFloat() * 0.4f), speed * (0.7f + Random.nextFloat() * 0.6f), alpha, puffs)
    }

    /** Smoothly change the weather background. */
    fun setCondition(condition: Condition, animate: Boolean = true, durationMs: Long = 2500L) {
        currentCondition = condition
        val target = if (condition == Condition.SUNNY) 1f else 0f
        blendAnimator?.cancel()
        if (!animate) {
            blend = target
            invalidate()
            return
        }
        blendAnimator = ValueAnimator.ofFloat(blend, target).apply {
            duration = durationMs
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { blend = it.animatedValue as Float }
            start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        sunPaint.shader = RadialGradient(
            w * 0.62f, h * 0.30f, h * 0.55f,
            intArrayOf(0xFFFFF4C8.toInt(), 0x88FFE08A.toInt(), 0x00FFE08A),
            floatArrayOf(0f, 0.35f, 1f),
            Shader.TileMode.CLAMP
        )
        // Dark top & bottom so white text stays readable on the bright sky
        scrimPaint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(0x66000000, 0x00000000, 0x00000000, 0x80000000.toInt()),
            floatArrayOf(0f, 0.35f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f || h == 0f) return

        val now = System.nanoTime()
        val dt = if (lastFrameNs == 0L) 0f else ((now - lastFrameNs) / 1_000_000_000f).coerceAtMost(0.05f)
        lastFrameNs = now
        time += dt

        // 1) Sky gradient
        skyPaint.shader = LinearGradient(
            0f, 0f, 0f, h,
            lerpColor(stormTop, sunnyTop, blend),
            lerpColor(stormBottom, sunnyBottom, blend),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, h, skyPaint)

        // 2) Sun glow (pulses gently)
        if (blend > 0.01f) {
            sunPaint.alpha = (255 * blend * (0.85f + 0.15f * sin(time * 1.2f))).toInt().coerceIn(0, 255)
            canvas.drawRect(0f, 0f, w, h, sunPaint)
        }

        // 3) Clouds (drift + colour change)
        cloudPaint.colorFilter = PorterDuffColorFilter(lerpColor(stormCloud, sunnyCloud, blend), PorterDuff.Mode.SRC_IN)
        val density = 1f - 0.25f * blend // storm = thicker clouds
        for (c in clouds) {
            c.x += c.speed * dt * (1.6f - blend) // storm wind is faster
            if (c.x - c.size > 1.4f) c.x = -0.6f
            val cx = c.x * w
            val cy = c.y * h + sin(time * 0.3f + c.y * 10f) * h * 0.006f
            val s = c.size * h
            cloudPaint.alpha = (255 * c.alpha * density).toInt().coerceIn(0, 255)
            for (p in c.puffs) {
                val px = cx + p.dx * s
                val py = cy + p.dy * s
                val r = p.r * s
                rect.set(px - r, py - r, px + r, py + r)
                canvas.drawBitmap(puff, null, rect, cloudPaint)
            }
        }

        // 4) Rain
        val rainAlpha = (1f - blend)
        if (rainAlpha > 0.02f) {
            rainPaint.alpha = (110 * rainAlpha).toInt()
            for (d in drops) {
                d.y += d.speed * dt
                d.x += 0.12f * d.speed * dt
                if (d.y > 1.05f) { d.y = -0.05f; d.x = Random.nextFloat() }
                if (d.x > 1.05f) d.x -= 1.1f
                val x = d.x * w
                val y = d.y * h
                canvas.drawLine(x, y, x - d.len * 0.25f, y - d.len, rainPaint)
            }
        }

        // 5) Lightning flash (storm only)
        if (blend < 0.3f && Random.nextFloat() < dt * 0.18f) flash = 1f
        if (flash > 0f) {
            flashPaint.alpha = (90 * flash * (1f - blend)).toInt().coerceIn(0, 255)
            canvas.drawRect(0f, 0f, w, h, flashPaint)
            flash -= dt * 3.5f
        }

        // 6) Readability scrim
        canvas.drawRect(0f, 0f, w, h, scrimPaint)

        if (isAttachedToWindow && visibility == VISIBLE) postInvalidateOnAnimation()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lastFrameNs = 0L
        if (visibility == VISIBLE) {
            postInvalidateOnAnimation()
        }
    }

    override fun onDetachedFromWindow() {
        blendAnimator?.cancel()
        super.onDetachedFromWindow()
    }

    private fun lerpColor(a: Int, b: Int, t: Float): Int {
        val inv = 1f - t
        return Color.argb(
            (Color.alpha(a) * inv + Color.alpha(b) * t).toInt(),
            (Color.red(a) * inv + Color.red(b) * t).toInt(),
            (Color.green(a) * inv + Color.green(b) * t).toInt(),
            (Color.blue(a) * inv + Color.blue(b) * t).toInt()
        )
    }
}
