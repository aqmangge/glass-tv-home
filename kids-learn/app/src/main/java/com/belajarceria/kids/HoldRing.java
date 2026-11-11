package com.belajarceria.kids;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * Hold-to-press: a ring fills around the button while it is held, and only a full ring (a few seconds) runs the
 * action. Letting go early empties the ring and runs the hint instead. Used as a child lock on the back button.
 */
final class HoldRing extends Drawable {
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG), arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private float progress;

    private HoldRing(int color) {
        track.setStyle(Paint.Style.STROKE);
        track.setColor(0x22000000);
        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeCap(Paint.Cap.ROUND);
        arc.setColor(color);
    }

    @SuppressLint("ClickableViewAccessibility")
    static void attach(View v, long millis, int color, Runnable action, Runnable hint) {
        HoldRing ring = new HoldRing(color);
        v.setForeground(ring);
        final ValueAnimator[] run = new ValueAnimator[1];
        final boolean[] fired = new boolean[1];
        v.setOnTouchListener((view, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: {
                    fired[0] = false;
                    Spring.scale(view, 0.9f, 700f, 0.7f);
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    if (run[0] != null) run[0].cancel();
                    ValueAnimator a = ValueAnimator.ofFloat(ring.progress, 1f);
                    a.setDuration((long) (millis * (1f - ring.progress)));
                    a.setInterpolator(new LinearInterpolator());
                    a.addUpdateListener(x -> {
                        ring.progress = (float) x.getAnimatedValue();
                        ring.invalidateSelf();
                        if (ring.progress >= 1f && !fired[0]) {
                            fired[0] = true;
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                            Sfx.pop();
                            Spring.scale(view, 1f, 420f, 0.42f);
                            ring.progress = 0f;
                            MainActivity.log("nav back held");
                            action.run();
                        }
                    });
                    run[0] = a;
                    a.start();
                    return true;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    Spring.scale(view, 1f, 420f, 0.42f);
                    if (run[0] != null) run[0].cancel();
                    run[0] = null;
                    if (!fired[0]) {
                        // let go too soon: the ring drains away, and the child hears how to do it
                        ValueAnimator back = ValueAnimator.ofFloat(ring.progress, 0f);
                        back.setDuration(250);
                        back.addUpdateListener(x -> {
                            ring.progress = (float) x.getAnimatedValue();
                            ring.invalidateSelf();
                        });
                        back.start();
                        if (e.getActionMasked() == MotionEvent.ACTION_UP) hint.run();
                    }
                    return true;
                }
                default:
                    return true;
            }
        });
    }

    @Override
    public void draw(Canvas c) {
        if (progress <= 0f) return;
        float w = getBounds().width(), h = getBounds().height(), stroke = Math.min(w, h) * 0.1f;
        track.setStrokeWidth(stroke);
        arc.setStrokeWidth(stroke);
        oval.set(getBounds());
        oval.inset(stroke / 2f, stroke / 2f);
        c.drawOval(oval, track);
        c.drawArc(oval, -90f, 360f * progress, false, arc);
    }

    @Override public void setAlpha(int alpha) {}
    @Override public void setColorFilter(ColorFilter cf) {}
    @Override public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
