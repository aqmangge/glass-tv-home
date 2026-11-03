package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Animated Weather Icon View inspired by Lucas Carrascosa (RTL Weather Icons).
 * Renders bold, geometric, clean modern weather vector illustrations with continuous 60fps animations:
 * - Breathing & pulsing golden sun disc with soft corona glow
 * - Sine-wave floating & bobbing puffy white cloud with soft 3D ambient depth
 * - Animated falling diagonal raindrops
 * - Electric yellow lightning flash
 * - Soft crescent moon with twinkling stars
 */
public class RtlWeatherIconView extends View {

    public enum Type {
        SUNNY,
        PARTLY_CLOUDY,
        CLOUDY,
        DRIZZLE,
        RAIN,
        THUNDERSTORM,
        NIGHT
    }

    private Type type = Type.PARTLY_CLOUDY;
    private final float density;

    // Paints
    private final Paint sunPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sunGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cloudPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cloudShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rainPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lightningPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint moonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Paths
    private final Path cloudPath = new Path();
    private final Path cloudShadowPath = new Path();
    private final Path lightningPath = new Path();
    private final Path moonPath = new Path();
    private final RectF tempRect = new RectF();

    private final long startTime = SystemClock.uptimeMillis();
    private boolean isAnimating = true;

    public RtlWeatherIconView(Context context) {
        this(context, null);
    }

    public RtlWeatherIconView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RtlWeatherIconView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        density = getResources().getDisplayMetrics().density;
        initPaints();
    }

    private void initPaints() {
        // Sun: Bold geometric golden disc matching RTL Weather Icons
        sunPaint.setStyle(Paint.Style.FILL);
        sunPaint.setColor(0xFFFBBF24); // Warm vibrant amber-yellow

        sunGlowPaint.setStyle(Paint.Style.FILL);

        // Puffy Cloud: Pure crisp white with soft ambient bevel
        cloudPaint.setStyle(Paint.Style.FILL);
        cloudPaint.setColor(0xFFFFFFFF);

        cloudShadowPaint.setStyle(Paint.Style.FILL);
        cloudShadowPaint.setColor(0x26000000); // 15% black soft ambient shadow

        // Rain
        rainPaint.setStyle(Paint.Style.STROKE);
        rainPaint.setStrokeCap(Paint.Cap.ROUND);
        rainPaint.setColor(0xFF67E8F9); // Cyan raindrop

        // Lightning
        lightningPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        lightningPaint.setColor(0xFFFDE047); // Electric yellow

        // Moon
        moonPaint.setStyle(Paint.Style.FILL);
        moonPaint.setColor(0xFFE2E8F0);

        // Stars
        starPaint.setStyle(Paint.Style.FILL);
        starPaint.setColor(0xFFFFFFFF);
    }

    public void setType(Type type) {
        if (this.type != type) {
            this.type = type;
            invalidate();
        }
    }

    public void setCondition(String condition, String icon, boolean isNight) {
        if (isNight) {
            if (condition != null && (condition.contains("rain") || condition.contains("hujan"))) {
                setType(Type.RAIN);
            } else if (condition != null && (condition.contains("storm") || condition.contains("ribut"))) {
                setType(Type.THUNDERSTORM);
            } else if (condition != null && (condition.contains("cloud") || condition.contains("berawan") || condition.contains("mendung"))) {
                setType(Type.PARTLY_CLOUDY);
            } else {
                setType(Type.NIGHT);
            }
            return;
        }

        if (condition == null) {
            setType(Type.PARTLY_CLOUDY);
            return;
        }

        String c = condition.toLowerCase();
        if (c.contains("thunder") || c.contains("storm") || c.contains("ribut") || c.contains("petir")) {
            setType(Type.THUNDERSTORM);
        } else if (c.contains("heavy rain") || c.contains("lebat") || (c.contains("hujan") && !c.contains("gerimis") && !c.contains("renyai"))) {
            setType(Type.RAIN);
        } else if (c.contains("drizzle") || c.contains("gerimis") || c.contains("renyai") || c.contains("light rain")) {
            setType(Type.DRIZZLE);
        } else if (c.contains("partly") || c.contains("sebahagian") || c.contains("alguma")) {
            setType(Type.PARTLY_CLOUDY);
        } else if (c.contains("cloud") || c.contains("mendung") || c.contains("overcast")) {
            setType(Type.CLOUDY);
        } else {
            setType(Type.SUNNY);
        }
    }

    private boolean isAnimationsEnabled() {
        try {
            return !getContext().getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
                    .getBoolean("reduced_motion", false);
        } catch (Exception e) {
            return true;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        isAnimating = isAnimationsEnabled();
        if (isAnimating) {
            postInvalidateOnAnimation();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        isAnimating = false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        boolean anim = isAnimationsEnabled();
        long elapsed = anim ? (SystemClock.uptimeMillis() - startTime) : 0;
        float timeSec = elapsed / 1000f;

        // Smooth animations
        float sunPulse = anim ? (float) Math.sin(timeSec * 2.2f) : 0f; // Gentle breathing scale
        float cloudBob = anim ? ((float) Math.sin(timeSec * 1.8f) * (6f * density)) : 0f; // Gentle vertical float

        switch (type) {
            case SUNNY:
                drawSunny(canvas, w, h, sunPulse, timeSec);
                break;
            case PARTLY_CLOUDY:
                // Signature RTL Sun + Puffy Cloud (matching user's reference photo)
                drawPartlyCloudy(canvas, w, h, sunPulse, cloudBob, timeSec);
                break;
            case CLOUDY:
                drawCloudy(canvas, w, h, cloudBob, timeSec);
                break;
            case DRIZZLE:
            case RAIN:
                drawRain(canvas, w, h, cloudBob, timeSec, type == Type.DRIZZLE);
                break;
            case THUNDERSTORM:
                drawThunderstorm(canvas, w, h, cloudBob, timeSec);
                break;
            case NIGHT:
                drawNight(canvas, w, h, cloudBob, timeSec);
                break;
        }

        if (isAnimating && getVisibility() == VISIBLE && anim) {
            postInvalidateOnAnimation();
        }
    }

    /**
     * Signature RTL Weather Icon: Golden Sun disc peaking at top-right behind pure white puffy cloud.
     */
    private void drawPartlyCloudy(Canvas canvas, int w, int h, float sunPulse, float cloudBob, float timeSec) {
        // 1. Draw Golden Sun Disc at top-right
        float sunCenterX = w * 0.70f;
        float sunCenterY = h * 0.38f;
        float baseSunR = Math.min(w, h) * 0.28f;
        float currentSunR = baseSunR * (1.0f + 0.035f * sunPulse);

        // Soft corona aura behind sun
        sunGlowPaint.setColor(0x4DFACC15);
        canvas.drawCircle(sunCenterX, sunCenterY, currentSunR * 1.25f, sunGlowPaint);

        // Solid golden disc
        sunPaint.setColor(0xFFFACC15);
        canvas.drawCircle(sunCenterX, sunCenterY, currentSunR, sunPaint);

        // 2. Draw Puffy White Cloud floating in front
        float cloudCenterX = w * 0.48f;
        float cloudCenterY = (h * 0.60f) + cloudBob;
        float cloudScale = Math.min(w, h) / (160f * density);

        buildPuffyCloudPath(cloudPath, cloudCenterX, cloudCenterY, cloudScale);

        // Ambient cloud drop shadow
        canvas.save();
        canvas.translate(0, 6f * density);
        cloudShadowPaint.setColor(0x2B000000);
        canvas.drawPath(cloudPath, cloudShadowPaint);
        canvas.restore();

        // Main white cloud body
        cloudPaint.setColor(0xFFFFFFFF);
        canvas.drawPath(cloudPath, cloudPaint);
    }

    private void drawSunny(Canvas canvas, int w, int h, float sunPulse, float timeSec) {
        float cx = w * 0.5f;
        float cy = h * 0.5f;
        float r = Math.min(w, h) * 0.34f * (1.0f + 0.04f * sunPulse);

        // Radiating corona glow
        sunGlowPaint.setColor(0x55FBBF24);
        canvas.drawCircle(cx, cy, r * 1.35f, sunGlowPaint);
        sunGlowPaint.setColor(0x26FBBF24);
        canvas.drawCircle(cx, cy, r * 1.65f, sunGlowPaint);

        // Solid golden sun disc
        sunPaint.setColor(0xFFFBBF24);
        canvas.drawCircle(cx, cy, r, sunPaint);
    }

    private void drawCloudy(Canvas canvas, int w, int h, float cloudBob, float timeSec) {
        float scale = Math.min(w, h) / (160f * density);

        // Back cloud (slate gray)
        float backCx = w * 0.60f;
        float backCy = (h * 0.46f) + (float) Math.sin(timeSec * 1.4f) * (4f * density);
        buildPuffyCloudPath(cloudPath, backCx, backCy, scale * 0.85f);
        cloudPaint.setColor(0xFF94A3B8); // Slate 400
        canvas.drawPath(cloudPath, cloudPaint);

        // Front cloud (crisp white)
        float frontCx = w * 0.45f;
        float frontCy = (h * 0.60f) + cloudBob;
        buildPuffyCloudPath(cloudPath, frontCx, frontCy, scale);

        canvas.save();
        canvas.translate(0, 6f * density);
        cloudShadowPaint.setColor(0x33000000);
        canvas.drawPath(cloudPath, cloudShadowPaint);
        canvas.restore();

        cloudPaint.setColor(0xFFFFFFFF);
        canvas.drawPath(cloudPath, cloudPaint);
    }

    private void drawRain(Canvas canvas, int w, int h, float cloudBob, float timeSec, boolean isDrizzle) {
        float scale = Math.min(w, h) / (160f * density);
        float cx = w * 0.50f;
        float cy = (h * 0.46f) + cloudBob;

        // Draw cloud
        buildPuffyCloudPath(cloudPath, cx, cy, scale);
        cloudPaint.setColor(isDrizzle ? 0xFFE2E8F0 : 0xFFCBD5E1);
        canvas.drawPath(cloudPath, cloudPaint);

        // Animated falling raindrops
        rainPaint.setStrokeWidth(isDrizzle ? 2.5f * density : 4f * density);
        rainPaint.setColor(0xFF38BDF8);

        float dropLen = isDrizzle ? 10f * density : 18f * density;
        int dropCount = isDrizzle ? 3 : 5;
        float startX = cx - (50f * density);
        float stepX = (100f * density) / Math.max(1, dropCount - 1);
        float baseY = cy + (35f * density);

        for (int i = 0; i < dropCount; i++) {
            float dropPhase = ((timeSec * 2.5f) + (i * 0.35f)) % 1.0f;
            float dx = startX + (i * stepX) - (dropPhase * 8f * density);
            float dy = baseY + (dropPhase * 45f * density);
            int alpha = (int) (255 * (1.0f - dropPhase));
            rainPaint.setAlpha(Math.max(0, Math.min(255, alpha)));
            canvas.drawLine(dx, dy, dx - (4f * density), dy + dropLen, rainPaint);
        }
    }

    private void drawThunderstorm(Canvas canvas, int w, int h, float cloudBob, float timeSec) {
        float scale = Math.min(w, h) / (160f * density);
        float cx = w * 0.50f;
        float cy = (h * 0.45f) + cloudBob;

        // Dark storm cloud
        buildPuffyCloudPath(cloudPath, cx, cy, scale);
        cloudPaint.setColor(0xFF475569); // Slate 600
        canvas.drawPath(cloudPath, cloudPaint);

        // Lightning flash animation
        float flashCycle = (timeSec * 1.2f) % 3.0f;
        boolean isFlashing = (flashCycle > 0.0f && flashCycle < 0.25f);

        if (isFlashing) {
            float boltX = cx - (6f * density);
            float boltY = cy + (25f * density);

            lightningPath.reset();
            lightningPath.moveTo(boltX, boltY);
            lightningPath.lineTo(boltX - (12f * density), boltY + (22f * density));
            lightningPath.lineTo(boltX - (2f * density), boltY + (22f * density));
            lightningPath.lineTo(boltX - (16f * density), boltY + (48f * density));
            lightningPath.lineTo(boltX + (6f * density), boltY + (20f * density));
            lightningPath.lineTo(boltX - (3f * density), boltY + (20f * density));
            lightningPath.close();

            lightningPaint.setColor(0xFFFDE047);
            canvas.drawPath(lightningPath, lightningPaint);
        }
    }

    private void drawNight(Canvas canvas, int w, int h, float cloudBob, float timeSec) {
        float cx = w * 0.50f;
        float cy = h * 0.48f + (float) Math.sin(timeSec * 1.5f) * (4f * density);
        float moonR = Math.min(w, h) * 0.28f;

        // Crescent moon
        moonPath.reset();
        tempRect.set(cx - moonR, cy - moonR, cx + moonR, cy + moonR);
        moonPath.addArc(tempRect, -90, 180);
        tempRect.set(cx - moonR * 0.4f, cy - moonR, cx + moonR * 0.8f, cy + moonR);
        moonPath.arcTo(tempRect, 90, -180, false);
        moonPath.close();

        moonPaint.setColor(0xFFE0F2FE); // Ice moon
        canvas.drawPath(moonPath, moonPaint);

        // Twinkling stars
        drawStar(canvas, cx + (55f * density), cy - (35f * density), 4f * density, timeSec, 0f);
        drawStar(canvas, cx - (45f * density), cy - (25f * density), 3f * density, timeSec, 1.2f);
        drawStar(canvas, cx + (35f * density), cy + (40f * density), 3.5f * density, timeSec, 2.4f);
    }

    private void drawStar(Canvas canvas, float x, float y, float r, float timeSec, float phase) {
        float pulse = (float) Math.abs(Math.sin((timeSec * 3f) + phase));
        int alpha = (int) (120 + 135 * pulse);
        starPaint.setAlpha(alpha);
        canvas.drawCircle(x, y, r * (0.8f + 0.4f * pulse), starPaint);
    }

    /**
     * Builds the iconic RTL puffy cloud shape formed by geometric overlapping circles and a base pill.
     */
    private void buildPuffyCloudPath(Path path, float cx, float cy, float scale) {
        path.reset();

        float baseWidth = 100f * density * scale;
        float baseHeight = 36f * density * scale;
        float baseLeft = cx - baseWidth / 2f;
        float baseTop = cy - baseHeight / 2f;

        // Bottom rounded base pill
        tempRect.set(baseLeft, baseTop, baseLeft + baseWidth, baseTop + baseHeight);
        path.addRoundRect(tempRect, baseHeight / 2f, baseHeight / 2f, Path.Direction.CW);

        // Center puff (largest circle)
        float centerR = 30f * density * scale;
        path.addCircle(cx - (4f * density * scale), cy - (12f * density * scale), centerR, Path.Direction.CW);

        // Left puff
        float leftR = 22f * density * scale;
        path.addCircle(cx - (34f * density * scale), cy - (2f * density * scale), leftR, Path.Direction.CW);

        // Right puff
        float rightR = 20f * density * scale;
        path.addCircle(cx + (28f * density * scale), cy - (2f * density * scale), rightR, Path.Direction.CW);
    }
}
