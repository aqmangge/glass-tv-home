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
 * Google Weather style smooth temperature curve graph for Android TV.
 * Connects 8 hourly intervals with natural cubic Bezier curves,
 * soft translucent gradient fills, temperature dots & labels, and time axis.
 */
public class HourlyWeatherGraphView extends View {

    private final List<WeatherAirQuality.HourlyItem> items = new ArrayList<>();
    private int accentColor = 0xFFFBBF24; // Amber Sun default

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointHaloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
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
        pointHaloPaint.setStyle(Paint.Style.STROKE);
        pointHaloPaint.setStrokeWidth(2.0f * density);
        pointHaloPaint.setColor(0x40000000);

        tempTextPaint.setColor(0xFFFFFFFF);
        tempTextPaint.setTextSize(13.5f * density);
        tempTextPaint.setTextAlign(Paint.Align.CENTER);
        tempTextPaint.setTypeface(interTypeface != null ? Typeface.create(interTypeface, Typeface.BOLD) : Typeface.DEFAULT_BOLD);
        tempTextPaint.setShadowLayer(3.0f * density, 0, 1.0f * density, 0x80000000);

        timeTextPaint.setColor(0xFF94A3B8); // Slate 400
        timeTextPaint.setTextSize(11.5f * density);
        timeTextPaint.setTextAlign(Paint.Align.CENTER);
        timeTextPaint.setTypeface(interTypeface != null ? interTypeface : Typeface.DEFAULT);

        baselinePaint.setColor(0x1AFFFFFF); // Subtle dotted/soft guide
        baselinePaint.setStrokeWidth(1.0f * density);
        baselinePaint.setStyle(Paint.Style.STROKE);
    }

    public void setData(List<WeatherAirQuality.HourlyItem> newItems, int color) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        this.accentColor = color;

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

        // Find min & max temperature
        int minTemp = Integer.MAX_VALUE;
        int maxTemp = Integer.MIN_VALUE;
        for (WeatherAirQuality.HourlyItem item : items) {
            if (item.temp < minTemp) minTemp = item.temp;
            if (item.temp > maxTemp) maxTemp = item.temp;
        }

        if (minTemp == maxTemp) {
            minTemp -= 2;
            maxTemp += 2;
        }
        float tempRange = Math.max(4.0f, (float) (maxTemp - minTemp + 2));
        float tempBase = (float) (minTemp - 1);

        float[] xs = new float[n];
        float[] ys = new float[n];

        for (int i = 0; i < n; i++) {
            xs[i] = paddingH + (i * (usableW / (float) (n - 1)));
            float norm = (items.get(i).temp - tempBase) / tempRange;
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

        // Shading: translucent top to transparent baseline
        int topColor = Color.argb((int) (60 * animProgress), Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor));
        int bottomColor = Color.argb(0, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor));
        fillPaint.setShader(new LinearGradient(0, paddingTop, 0, baselineY, topColor, bottomColor, Shader.TileMode.CLAMP));
        canvas.drawPath(fillPath, fillPaint);

        // Draw Golden/Accent Curve Line
        linePaint.setColor(accentColor);
        linePaint.setAlpha((int) (255 * animProgress));
        canvas.drawPath(curvePath, linePaint);

        // Draw Points & Labels
        pointPaint.setColor(accentColor);
        tempTextPaint.setAlpha((int) (255 * animProgress));
        timeTextPaint.setAlpha(220);

        for (int i = 0; i < n; i++) {
            WeatherAirQuality.HourlyItem item = items.get(i);

            // Halo & Center Point Dot
            canvas.drawCircle(xs[i], ys[i], 5.0f * density, pointHaloPaint);
            canvas.drawCircle(xs[i], ys[i], 3.2f * density, pointPaint);

            // Temperature text above point
            canvas.drawText(item.temp + "°", xs[i], ys[i] - (8.0f * density), tempTextPaint);

            // Time label below baseline
            String label = item.time != null ? item.time.toLowerCase(Locale.ROOT) : "";
            canvas.drawText(label, xs[i], baselineY + (17.0f * density), timeTextPaint);
        }
    }
}
