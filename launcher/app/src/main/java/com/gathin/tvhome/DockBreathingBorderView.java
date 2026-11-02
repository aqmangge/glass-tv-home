package com.gathin.tvhome;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.Nullable;

/**
 * Organic Breathing Glow & Glass Rim Focus Effect for TV Dock Cards.
 * Gently and elegantly pulses border illumination, specular reflection,
 * and outer atmospheric bloom in a calm rhythmic breathing cycle.
 */
public class DockBreathingBorderView extends View {

    private final Paint rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint topSheenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF bounds = new RectF();
    private final RectF glowBounds = new RectF();
    private final RectF sheenBounds = new RectF();

    private float density = 1f;
    private float cornerRadius = 16f;

    private float breathProgress = 0f;
    private ValueAnimator breathAnimator;

    public DockBreathingBorderView(Context context) {
        super(context);
        init(context);
    }

    public DockBreathingBorderView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DockBreathingBorderView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        density = context.getResources().getDisplayMetrics().density;
        cornerRadius = 16f * density;

        rimPaint.setStyle(Paint.Style.STROKE);
        rimPaint.setStrokeCap(Paint.Cap.ROUND);
        rimPaint.setStrokeJoin(Paint.Join.ROUND);

        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeCap(Paint.Cap.ROUND);
        glowPaint.setStrokeJoin(Paint.Join.ROUND);

        topSheenPaint.setStyle(Paint.Style.STROKE);
        topSheenPaint.setStrokeCap(Paint.Cap.ROUND);

        setWillNotDraw(false);
    }

    public void setCornerRadiusDp(float radiusDp) {
        this.cornerRadius = radiusDp * density;
        updateBounds();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateBounds();
    }

    private void updateBounds() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        float stroke = 2.0f * density;
        float half = stroke / 2.0f;
        bounds.set(half, half, w - half, h - half);

        float glowInset = 1.0f * density;
        glowBounds.set(bounds.left - glowInset, bounds.top - glowInset,
                bounds.right + glowInset, bounds.bottom + glowInset);

        sheenBounds.set(bounds.left + 2 * density, bounds.top + density,
                bounds.right - 2 * density, bounds.top + 16 * density);
    }

    public void startAnimation() {
        if (breathAnimator != null && breathAnimator.isRunning()) return;

        // Smooth 1600ms inhale-exhale cycle
        breathAnimator = ValueAnimator.ofFloat(0f, 1f);
        breathAnimator.setDuration(1600);
        breathAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        breathAnimator.setRepeatMode(ValueAnimator.REVERSE);
        breathAnimator.setRepeatCount(ValueAnimator.INFINITE);
        breathAnimator.addUpdateListener(animation -> {
            breathProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        breathAnimator.start();
    }

    public void stopAnimation() {
        if (breathAnimator != null) {
            breathAnimator.cancel();
            breathAnimator = null;
        }
        breathProgress = 0f;
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (getVisibility() == VISIBLE) {
            startAnimation();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimation();
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility == VISIBLE) {
            startAnimation();
        } else {
            stopAnimation();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (getWidth() <= 0 || getHeight() <= 0) return;

        // Calculate smooth breathing factors
        // min alpha: 0.35, max alpha: 1.0
        float alphaFactor = 0.35f + (0.65f * breathProgress);
        // stroke width breaths between 1.6dp and 2.6dp
        float strokeW = (1.6f + (1.0f * breathProgress)) * density;
        rimPaint.setStrokeWidth(strokeW);

        // 1. Soft Outer Atmospheric Breathing Glow (Cyan-Sky bloom)
        int glowAlpha = (int) (65 * alphaFactor);
        glowPaint.setStrokeWidth((4.0f + (3.0f * breathProgress)) * density);
        glowPaint.setColor(Color.argb(glowAlpha, 56, 189, 248)); // Cyan #38BDF8
        canvas.drawRoundRect(glowBounds, cornerRadius + (2f * density), cornerRadius + (2f * density), glowPaint);

        // 2. Main Glass Rim Border with Vertical Specular Gradient
        // Top: pure white highlight -> Bottom: electric cyan
        int topAlpha = (int) (245 * alphaFactor);
        int bottomAlpha = (int) (180 * alphaFactor);
        int topColor = Color.argb(topAlpha, 255, 255, 255);
        int bottomColor = Color.argb(bottomAlpha, 56, 189, 248);

        LinearGradient rimShader = new LinearGradient(
                0, bounds.top, 0, bounds.bottom,
                new int[]{topColor, bottomColor},
                new float[]{0.0f, 1.0f},
                Shader.TileMode.CLAMP
        );
        rimPaint.setShader(rimShader);
        canvas.drawRoundRect(bounds, cornerRadius, cornerRadius, rimPaint);

        // 3. Delicate Soap-Bubble Top Edge Specular Glint
        int sheenAlpha = (int) (190 * (0.4f + (0.6f * breathProgress)));
        topSheenPaint.setStrokeWidth(1.2f * density);
        LinearGradient sheenShader = new LinearGradient(
                sheenBounds.left, sheenBounds.top, sheenBounds.right, sheenBounds.top,
                new int[]{0x00FFFFFF, Color.argb(sheenAlpha, 255, 255, 255), 0x00FFFFFF},
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP
        );
        topSheenPaint.setShader(sheenShader);
        canvas.drawLine(bounds.left + cornerRadius, bounds.top + (0.5f * density),
                bounds.right - cornerRadius, bounds.top + (0.5f * density), topSheenPaint);
    }
}
