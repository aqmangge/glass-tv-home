package com.gathin.tvhome;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

/**
 * Interactive EA FC Play Button style animated focus border.
 * Features a luminous neon beam ("Stoke Mover") orbiting smoothly around
 * the rounded perimeter of the focused dock card, accompanied by a frosted glass rim.
 */
public class EafcFocusBorderView extends View {

    private final Paint glassRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint beamPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint headDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path roundRectPath = new Path();
    private final Path segmentPath = new Path();
    private final PathMeasure pathMeasure = new PathMeasure();
    private final RectF bounds = new RectF();

    private float totalLength = 0f;
    private float progress = 0f;
    private ValueAnimator orbitAnimator;

    private float density = 1f;
    private float strokeWidth = 2.5f;
    private float cornerRadius = 16f;

    // Head position coordinates for the leading neon comet head
    private final float[] headPos = new float[2];
    private final float[] headTan = new float[2];

    public EafcFocusBorderView(Context context) {
        super(context);
        init(context);
    }

    public EafcFocusBorderView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public EafcFocusBorderView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        density = context.getResources().getDisplayMetrics().density;
        strokeWidth = 2.5f * density;
        cornerRadius = 16f * density;

        // 1. Subtle static glass rim border
        glassRimPaint.setStyle(Paint.Style.STROKE);
        glassRimPaint.setStrokeWidth(1.2f * density);
        glassRimPaint.setColor(0x40FFFFFF); // Soft translucent glass rim

        // 2. Traveling luminous neon beam
        beamPaint.setStyle(Paint.Style.STROKE);
        beamPaint.setStrokeWidth(strokeWidth);
        beamPaint.setStrokeCap(Paint.Cap.ROUND);
        beamPaint.setStrokeJoin(Paint.Join.ROUND);

        // 3. Bloom glow behind beam
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(strokeWidth * 2.2f);
        glowPaint.setStrokeCap(Paint.Cap.ROUND);
        glowPaint.setStrokeJoin(Paint.Join.ROUND);

        // 4. White hot leading comet head
        headDotPaint.setStyle(Paint.Style.FILL);
        headDotPaint.setColor(0xFFFFFFFF);

        setWillNotDraw(false);
    }

    public void setCornerRadiusDp(float radiusDp) {
        this.cornerRadius = radiusDp * density;
        updatePath();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updatePath();
    }

    private void updatePath() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        float halfStroke = strokeWidth / 2.0f;
        bounds.set(halfStroke, halfStroke, w - halfStroke, h - halfStroke);

        roundRectPath.reset();
        roundRectPath.addRoundRect(bounds, cornerRadius, cornerRadius, Path.Direction.CW);

        pathMeasure.setPath(roundRectPath, true);
        totalLength = pathMeasure.getLength();

        // Multi-color EA FC Neon Shader across the card width:
        // Sky Blue (#38BDF8) -> Mint Green (#4ADE80) -> Violet (#A855F7) -> Pure White
        int[] neonColors = new int[]{
                0xFF38BDF8, // Cyan Sky
                0xFF4ADE80, // Electric Mint Green
                0xFFA855F7, // Royal Purple / Violet
                0xFF38BDF8  // Loop back to Cyan
        };
        float[] positions = new float[]{0.0f, 0.35f, 0.70f, 1.0f};

        LinearGradient shader = new LinearGradient(
                0, 0, w, h,
                neonColors, positions, Shader.TileMode.CLAMP
        );
        beamPaint.setShader(shader);

        int[] glowColors = new int[]{
                0x5038BDF8,
                0x504ADE80,
                0x50A855F7,
                0x5038BDF8
        };
        glowPaint.setShader(new LinearGradient(0, 0, w, h, glowColors, positions, Shader.TileMode.CLAMP));
    }

    public void startAnimation() {
        if (orbitAnimator != null && orbitAnimator.isRunning()) return;

        orbitAnimator = ValueAnimator.ofFloat(0f, 1f);
        orbitAnimator.setDuration(1800); // Smooth 1.8s orbit loop matching EA FC button
        orbitAnimator.setInterpolator(new LinearInterpolator());
        orbitAnimator.setRepeatCount(ValueAnimator.INFINITE);
        orbitAnimator.addUpdateListener(animation -> {
            progress = (float) animation.getAnimatedValue();
            invalidate();
        });
        orbitAnimator.start();
    }

    public void stopAnimation() {
        if (orbitAnimator != null) {
            orbitAnimator.cancel();
            orbitAnimator = null;
        }
        progress = 0f;
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

        if (totalLength <= 0) return;

        // 1. Draw static glass rim outline
        canvas.drawRoundRect(bounds, cornerRadius, cornerRadius, glassRimPaint);

        // If not running animation, only glass rim is drawn
        if (orbitAnimator == null || !orbitAnimator.isRunning()) return;

        // 2. Calculate EAFC traveling beam segment (~32% of perimeter)
        float beamLength = totalLength * 0.32f;
        float startD = progress * totalLength;
        float endD = startD + beamLength;

        segmentPath.reset();
        if (endD <= totalLength) {
            pathMeasure.getSegment(startD, endD, segmentPath, true);
        } else {
            // Wrap-around case: two parts
            pathMeasure.getSegment(startD, totalLength, segmentPath, true);
            pathMeasure.getSegment(0f, endD - totalLength, segmentPath, true);
        }

        // Draw soft bloom glow under the beam
        canvas.drawPath(segmentPath, glowPaint);

        // Draw crisp neon stroke
        canvas.drawPath(segmentPath, beamPaint);

        // 3. Draw luminous white comet head at the leading tip
        float leadingD = endD % totalLength;
        if (pathMeasure.getPosTan(leadingD, headPos, headTan)) {
            // Soft white halo
            headDotPaint.setColor(0x80FFFFFF);
            canvas.drawCircle(headPos[0], headPos[1], strokeWidth * 1.8f, headDotPaint);

            // Brilliant white core
            headDotPaint.setColor(0xFFFFFFFF);
            canvas.drawCircle(headPos[0], headPos[1], strokeWidth * 0.9f, headDotPaint);
        }
    }
}
