package com.gathin.tvhome;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Google Weather style smooth curve graph for Android TV.
 * Connects hourly intervals with natural cubic Bezier curves,
 * soft translucent gradient fills, highlight dots & labels, and time axis.
 * Supports 3 interactive modes:
 * - TEMPERATURE: Temperature curve in °C (warm amber/yellow)
 * - PRECIPITATION: Rain probability in % (sky blue)
 * - WIND: Wind speed in km/h (mint teal)
 */
public class HourlyWeatherGraphView extends View {

    public enum Mode {
        TEMPERATURE,
        PRECIPITATION,
        WIND
    }

    private Mode mode = Mode.TEMPERATURE;
    private final List<WeatherAirQuality.HourlyItem> items = new ArrayList<>();
    private int accentColor = 0xFFFBBF24; // Amber Sun default

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointHaloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint whiteCenterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tempTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint timeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint baselinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path curvePath = new Path();
    private final Path fillPath = new Path();

    private float animProgress = 1.0f;
    private ValueAnimator animator;
    private Typeface interTypeface;

    public HourlyWeatherGraphView(Context context) {
        super(context);
        init(context);
    }

    public HourlyWeatherGraphView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public HourlyWeatherGraphView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        try {
            interTypeface = ResourcesCompat.getFont(context, R.font.inter);
        } catch (Exception ignored) {
            interTypeface = Typeface.DEFAULT;
        }

        float density = getResources().getDisplayMetrics().density;

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(3.0f * density);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        fillPaint.setStyle(Paint.Style.FILL);

        pointPaint.setStyle(Paint.Style.FILL);
        pointHaloPaint.setStyle(Paint.Style.FILL);

        whiteCenterPaint.setStyle(Paint.Style.FILL);
        whiteCenterPaint.setColor(0xFFFFFFFF);

        tempTextPaint.setColor(0xFFF5F7FC);
        tempTextPaint.setTextSize(13.5f * density);
        tempTextPaint.setTextAlign(Paint.Align.CENTER);
        tempTextPaint.setTypeface(interTypeface != null ? Typeface.create(interTypeface, Typeface.BOLD) : Typeface.DEFAULT_BOLD);
        tempTextPaint.setShadowLayer(3.0f * density, 0, 1.0f * density, 0x80000000);

        timeTextPaint.setColor(0xFFA8B5CC); // Slate A8B5CC
        timeTextPaint.setTextSize(11.5f * density);
        timeTextPaint.setTextAlign(Paint.Align.CENTER);
        timeTextPaint.setTypeface(interTypeface != null ? interTypeface : Typeface.DEFAULT);

        baselinePaint.setColor(0x1AFFFFFF); // Subtle dotted/soft guide
        baselinePaint.setStrokeWidth(1.0f * density);
        baselinePaint.setStyle(Paint.Style.STROKE);
    }

    public void setMode(Mode newMode) {
        if (this.mode != newMode) {
            this.mode = newMode;
            startRevealAnimation();
        }
    }

    public Mode getMode() {
        return mode;
    }

    public void setData(List<WeatherAirQuality.HourlyItem> newItems, int color) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        this.accentColor = color;
        startRevealAnimation();
    }

    private void startRevealAnimation() {
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }

        animProgress = 0f;
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(350);
        animator.setInterpolator(new DecelerateInterpolator(1.5f));
        animator.addUpdateListener(animation -> {
            animProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    private int getActiveColor() {
        switch (mode) {
            case PRECIPITATION:
                return 0xFF38BDF8; // Electric Blue Accent
            case WIND:
                return 0xFF38BDF8; // Electric Blue Accent
            case TEMPERATURE:
            default:
                return 0xFF38BDF8; // Electric Blue Accent (#38BDF8)
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (items.isEmpty()) return;

        int n = items.size();
        float width = getWidth();
        float height = getHeight();
        float density = getResources().getDisplayMetrics().density;

        float paddingH = 32.0f * density;
        float paddingTop = 24.0f * density;
        float paddingBottom = 26.0f * density;

        float usableW = width - (paddingH * 2.0f);
        float baselineY = height - paddingBottom;
        float usableH = baselineY - paddingTop;

        if (usableW <= 0 || usableH <= 0) return;

        float[] values = new float[n];
        float minVal = Float.MAX_VALUE;
        float maxVal = Float.MIN_VALUE;

        for (int i = 0; i < n; i++) {
            WeatherAirQuality.HourlyItem item = items.get(i);
            float v;
            if (mode == Mode.PRECIPITATION) {
                v = item.rainChance;
            } else if (mode == Mode.WIND) {
                v = item.windSpeed;
            } else {
                v = item.temp;
            }
            values[i] = v;
            if (v < minVal) minVal = v;
            if (v > maxVal) maxVal = v;
        }

        float valBase;
        float valRange;

        if (mode == Mode.PRECIPITATION) {
            // Precipitation is 0% to 100%
            valBase = 0f;
            valRange = Math.max(50.0f, maxVal > 80 ? 100.0f : Math.max(40.0f, maxVal + 10.0f));
        } else if (mode == Mode.WIND) {
            // Wind speed in km/h
            valBase = Math.max(0f, minVal - 2f);
            valRange = Math.max(12.0f, maxVal - valBase + 2.0f);
        } else {
            // Temperature in °C
            if (minVal == maxVal) {
                minVal -= 2;
                maxVal += 2;
            }
            valRange = Math.max(4.0f, maxVal - minVal + 2.0f);
            valBase = minVal - 1.0f;
        }

        float[] xs = new float[n];
        float[] ys = new float[n];

        for (int i = 0; i < n; i++) {
            xs[i] = paddingH + (i * (usableW / (float) (n - 1)));
            float norm = Math.max(0f, Math.min(1f, (values[i] - valBase) / valRange));
            float targetY = baselineY - (norm * usableH);
            // Smooth reveal animation from baseline up to targetY
            ys[i] = baselineY - ((baselineY - targetY) * animProgress);
        }

        // Draw horizontal baseline guide
        canvas.drawLine(paddingH - (8 * density), baselineY, width - paddingH + (8 * density), baselineY, baselinePaint);

        // Build smooth Cubic Spline
        curvePath.reset();
        curvePath.moveTo(xs[0], ys[0]);
        for (int i = 0; i < n - 1; i++) {
            float midX = (xs[i] + xs[i + 1]) / 2.0f;
            curvePath.cubicTo(midX, ys[i], midX, ys[i + 1], xs[i + 1], ys[i + 1]);
        }

        // Build Gradient Fill Path
        fillPath.reset();
        fillPath.set(curvePath);
        fillPath.lineTo(xs[n - 1], baselineY);
        fillPath.lineTo(xs[0], baselineY);
        fillPath.close();

        int activeColor = getActiveColor();

        // Shading: translucent top to transparent baseline
        int topColor = Color.argb((int) (60 * animProgress), Color.red(activeColor), Color.green(activeColor), Color.blue(activeColor));
        int bottomColor = Color.argb(0, Color.red(activeColor), Color.green(activeColor), Color.blue(activeColor));
        fillPaint.setShader(new LinearGradient(0, paddingTop, 0, baselineY, topColor, bottomColor, Shader.TileMode.CLAMP));
        canvas.drawPath(fillPath, fillPaint);

        // Draw Active Curve Line
        linePaint.setColor(activeColor);
        linePaint.setAlpha((int) (255 * animProgress));
        canvas.drawPath(curvePath, linePaint);

        // Draw Points & Labels
        pointPaint.setColor(activeColor);
        tempTextPaint.setAlpha((int) (255 * animProgress));
        timeTextPaint.setAlpha(220);

        for (int i = 0; i < n; i++) {
            WeatherAirQuality.HourlyItem item = items.get(i);

            // Electric blue outer dot with crisp white center (matching minimal dashboard mockup)
            pointHaloPaint.setColor(activeColor);
            canvas.drawCircle(xs[i], ys[i], 4.2f * density, pointHaloPaint);
            canvas.drawCircle(xs[i], ys[i], 2.2f * density, whiteCenterPaint);

            // Value text above point according to mode
            String valStr;
            if (mode == Mode.PRECIPITATION) {
                valStr = item.rainChance + "%";
            } else if (mode == Mode.WIND) {
                valStr = item.windSpeed + " km/h";
            } else {
                valStr = item.temp + "°";
            }
            canvas.drawText(valStr, xs[i], ys[i] - (8.0f * density), tempTextPaint);

            // Time label below baseline
            String label = item.time != null ? item.time.toLowerCase(Locale.ROOT) : "";
            canvas.drawText(label, xs[i], baselineY + (17.0f * density), timeTextPaint);
        }
    }
}
