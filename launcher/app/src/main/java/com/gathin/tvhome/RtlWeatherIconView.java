package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

/**
 * Weather Hero View powered by Bas Milius Meteocons (https://github.com/basmilius/meteocons).
 * Displays rich, modern weather vector art with subtle 60fps breathing and floating animation:
 * - Floating ambient elevation (±3dp sine wave)
 * - Gentle atmospheric breathing scale (0.98x - 1.02x)
 * - Full condition support (Sunny, Moon, Partly Cloudy Day/Night, Cloud, Fog, Drizzle, Rain, Thunderstorm, Snow, Hail)
 */
public class RtlWeatherIconView extends View {

    public enum Type {
        SUNNY,
        PARTLY_CLOUDY,
        CLOUDY,
        DRIZZLE,
        RAIN,
        THUNDERSTORM,
        NIGHT,
        SNOW,
        FOG,
        HAIL
    }

    private Type type = Type.PARTLY_CLOUDY;
    private boolean isNight = false;
    private final float density;
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
    }

    public void setType(Type type) {
        if (this.type != type) {
            this.type = type;
            invalidate();
        }
    }

    public void setCondition(String condition, String icon, boolean isNight) {
        this.isNight = isNight;
        if (isNight) {
            if (condition != null && (condition.contains("rain") || condition.contains("hujan"))) {
                setType(Type.RAIN);
            } else if (condition != null && (condition.contains("storm") || condition.contains("ribut") || condition.contains("petir"))) {
                setType(Type.THUNDERSTORM);
            } else if (condition != null && (condition.contains("cloud") || condition.contains("berawan") || condition.contains("mendung"))) {
                setType(Type.PARTLY_CLOUDY);
            } else if (condition != null && (condition.contains("snow") || condition.contains("salji"))) {
                setType(Type.SNOW);
            } else if (condition != null && (condition.contains("fog") || condition.contains("kabus") || condition.contains("haze"))) {
                setType(Type.FOG);
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
        } else if (c.contains("snow") || c.contains("salji")) {
            setType(Type.SNOW);
        } else if (c.contains("hail") || c.contains("hujan batu")) {
            setType(Type.HAIL);
        } else if (c.contains("fog") || c.contains("kabus") || c.contains("haze")) {
            setType(Type.FOG);
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

    private int getDrawableResForType(Type t) {
        switch (t) {
            case SUNNY: return R.drawable.wx_sun;
            case NIGHT: return R.drawable.wx_moon;
            case PARTLY_CLOUDY: return isNight ? R.drawable.wx_partly_night : R.drawable.wx_partly;
            case CLOUDY: return R.drawable.wx_cloud;
            case FOG: return R.drawable.wx_fog;
            case DRIZZLE: return R.drawable.wx_drizzle;
            case RAIN: return R.drawable.wx_rain;
            case THUNDERSTORM: return R.drawable.wx_storm;
            case SNOW: return R.drawable.wx_snow;
            case HAIL: return R.drawable.wx_hail;
            default: return R.drawable.wx_partly;
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

        Drawable d = ContextCompat.getDrawable(getContext(), getDrawableResForType(type));
        if (d == null) return;

        boolean anim = isAnimationsEnabled();
        float timeSec = anim ? ((SystemClock.uptimeMillis() - startTime) / 1000f) : 0f;

        // Smooth animations: gentle float (±3dp) and breathing pulse (±2%)
        float floatY = anim ? (float) (Math.sin(timeSec * 1.8) * (3f * density)) : 0f;
        float scale = anim ? (1.0f + 0.02f * (float) Math.sin(timeSec * 2.2)) : 1.0f;

        int size = Math.min(w, h);
        int left = (w - size) / 2;
        int top = (h - size) / 2;

        canvas.save();
        canvas.translate(0, floatY);
        canvas.scale(scale, scale, w / 2f, h / 2f);
        d.setBounds(left, top, left + size, top + size);
        d.draw(canvas);
        canvas.restore();

        if (isAnimating && getVisibility() == VISIBLE && anim) {
            postInvalidateOnAnimation();
        }
    }
}
