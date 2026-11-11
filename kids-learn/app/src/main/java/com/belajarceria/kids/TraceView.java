package com.belajarceria.kids;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Random;

/**
 * A big faint letter with a dashed outline to trace over with a thick crayon. When enough of the letter is
 * coloured, and most of the colour is on it, the letter fills with a gradient and the listener is told.
 * Small children are messy, so the check is generous.
 */
final class TraceView extends View {
    interface Listener {
        void onTraced();

        void onMessy();
    }

    private static final class Stroke {
        final Path path = new Path();
        final int color;

        Stroke(int color) {
            this.color = color;
        }
    }

    private static final class Spark {
        float x, y, vx, vy, life;
        int color;
    }

    /** The check runs on a copy of the drawing this many times smaller: fast, and precise enough. */
    private static final int SCALE = 4;

    private final ArrayList<Stroke> strokes = new ArrayList<>();
    private final ArrayList<Spark> sparks = new ArrayList<>();
    private final Paint guide = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mask = new Paint();
    private final Paint sparkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path glyphPath = new Path();
    private final Random rnd = new Random();
    private String glyph = "";
    private int[] colors = {0xFFFF4F86, 0xFFFFA45B};
    private int brush = 0xFFFF4F86;
    private float ox, oy, brushWidth, lastX, lastY, done;
    private Stroke current;
    private boolean traced;
    private Bitmap letterMask, nearMask, inkMask, wideMask;
    /** The letter thinned to a one-pixel centre line, in the check's small grid; made once per letter. */
    private boolean[] spine;
    private ValueAnimator doneAnim;
    Listener listener;

    /**
     * How much of the letter's centre line the crayon must run along: 90% for ABC and 123 (the whole letter, more
     * or less), 70% for Arabic, whose letters are smaller and harder for small hands.
     */
    float required = 0.7f;

    TraceView(Context c) {
        super(c);
        outline.setStyle(Paint.Style.STROKE);
        outline.setStrokeWidth(Ui.dp(3));
        outline.setPathEffect(new DashPathEffect(new float[]{Ui.dp(10), Ui.dp(8)}, 0));
        ink.setStyle(Paint.Style.STROKE);
        ink.setStrokeCap(Paint.Cap.ROUND);
        ink.setStrokeJoin(Paint.Join.ROUND);
        mask.setColor(Color.BLACK);
        mask.setStrokeCap(Paint.Cap.ROUND);
        mask.setStrokeJoin(Paint.Join.ROUND);
    }

    void setGlyph(String g, int[] colors, Typeface tf) {
        glyph = g;
        this.colors = colors;
        guide.setTypeface(tf);
        fill.setTypeface(tf);
        clear();
        layoutGlyph();
    }

    void setBrush(int color) {
        brush = color;
    }

    void clear() {
        strokes.clear();
        traced = false;
        done = 0f;
        if (doneAnim != null) doneAnim.cancel();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        if (w > 0 && h > 0) {
            letterMask = Bitmap.createBitmap(w / SCALE + 1, h / SCALE + 1, Bitmap.Config.ARGB_8888);
            nearMask = Bitmap.createBitmap(w / SCALE + 1, h / SCALE + 1, Bitmap.Config.ARGB_8888);
            inkMask = Bitmap.createBitmap(w / SCALE + 1, h / SCALE + 1, Bitmap.Config.ARGB_8888);
            wideMask = Bitmap.createBitmap(w / SCALE + 1, h / SCALE + 1, Bitmap.Config.ARGB_8888);
        }
        layoutGlyph();
    }

    /** Sizes the letter to fill about four fifths of the board and centres it on its actual ink. */
    private void layoutGlyph() {
        spine = null;
        int w = getWidth(), h = getHeight();
        if (w == 0 || glyph.isEmpty()) return;
        Rect b = new Rect();
        guide.setTextSize(100f);
        guide.getTextBounds(glyph, 0, glyph.length(), b);
        if (b.width() == 0 || b.height() == 0) return;
        float size = 100f * Math.min(w * 0.78f / b.width(), h * 0.74f / b.height());
        guide.setTextSize(size);
        fill.setTextSize(size);
        guide.getTextBounds(glyph, 0, glyph.length(), b);
        ox = (w - b.width()) / 2f - b.left;
        oy = (h - b.height()) / 2f - b.top;
        glyphPath.reset();
        Path raw = new Path();
        guide.getTextPath(glyph, 0, glyph.length(), ox, oy, raw);
        // Fonts build letters from overlapping strokes; merge them so the dashed outline is only the edge.
        if (!glyphPath.op(raw, Path.Op.UNION)) glyphPath.set(raw);
        brushWidth = Math.max(Ui.dp(16), Math.min(size * 0.11f, Ui.dp(46)));
        fill.setShader(new LinearGradient(0, oy + b.top, 0, oy + b.bottom, colors, null, Shader.TileMode.CLAMP));
        guide.setColor((colors[0] & 0x00FFFFFF) | 0x1F000000);
        outline.setColor((colors[0] & 0x00FFFFFF) | 0x99000000);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        if (glyph.isEmpty()) return;
        c.drawText(glyph, ox, oy, guide);
        if (done < 1f) c.drawPath(glyphPath, outline);
        ink.setStrokeWidth(brushWidth);
        for (Stroke s : strokes) {
            ink.setColor(s.color);
            c.drawPath(s.path, ink);
        }
        if (done > 0f) {
            // The finished letter rises over the crayon, solid and shiny.
            fill.setAlpha((int) (255 * done));
            c.save();
            float k = 0.9f + 0.1f * done;
            c.scale(k, k, getWidth() / 2f, getHeight() / 2f);
            c.drawText(glyph, ox, oy, fill);
            c.restore();
        }
        drawSparks(c);
    }

    private void drawSparks(Canvas c) {
        if (sparks.isEmpty()) return;
        for (int i = sparks.size() - 1; i >= 0; i--) {
            Spark p = sparks.get(i);
            p.life -= 0.035f;
            if (p.life <= 0) {
                sparks.remove(i);
                continue;
            }
            p.x += p.vx;
            p.y += p.vy;
            sparkPaint.setColor(p.color);
            sparkPaint.setAlpha((int) (255 * p.life));
            float r = Ui.dp(5) * p.life;
            // a little four-pointed twinkle
            c.drawCircle(p.x, p.y, r * 0.6f, sparkPaint);
            c.drawRect(p.x - r * 1.6f, p.y - r * 0.18f, p.x + r * 1.6f, p.y + r * 0.18f, sparkPaint);
            c.drawRect(p.x - r * 0.18f, p.y - r * 1.6f, p.x + r * 0.18f, p.y + r * 1.6f, sparkPaint);
        }
        postInvalidateOnAnimation();
    }

    private void sparkle(float x, float y) {
        if (rnd.nextInt(3) == 0) addSpark(x, y);
    }

    private void addSpark(float x, float y) {
        Spark p = new Spark();
        p.x = x + (rnd.nextFloat() - 0.5f) * brushWidth;
        p.y = y + (rnd.nextFloat() - 0.5f) * brushWidth;
        p.vx = (rnd.nextFloat() - 0.5f) * Ui.dp(3);
        p.vy = -rnd.nextFloat() * Ui.dp(3);
        p.life = 1f;
        p.color = rnd.nextBoolean() ? 0xFFFFD23F : 0xFFFFFFFF;
        sparks.add(p);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX(), y = e.getY();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                current = new Stroke(brush);
                current.path.moveTo(x, y);
                current.path.lineTo(x + 0.1f, y);
                strokes.add(current);
                lastX = x;
                lastY = y;
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (current == null) return true;
                for (int i = 0; i < e.getHistorySize(); i++) {
                    lineTo(e.getHistoricalX(i), e.getHistoricalY(i));
                }
                lineTo(x, y);
                sparkle(x, y);
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (current != null) current.path.lineTo(x, y);
                current = null;
                invalidate();
                if (!traced) check();
                return true;
            default:
                return true;
        }
    }

    private void lineTo(float x, float y) {
        // A smooth crayon line: curve through the midpoints of the finger's samples.
        current.path.quadTo(lastX, lastY, (x + lastX) / 2f, (y + lastY) / 2f);
        lastX = x;
        lastY = y;
    }

    /**
     * Following: how much of the letter's centre line the crayon has passed near (the child follows the shape, no need
     * to colour it all in). Precision: how much of the colour is on (or near) the letter, so scribbles don't count.
     */
    private void check() {
        if (letterMask == null || strokes.isEmpty()) return;
        float k = 1f / SCALE;
        letterMask.eraseColor(Color.TRANSPARENT);
        nearMask.eraseColor(Color.TRANSPARENT);
        inkMask.eraseColor(Color.TRANSPARENT);
        wideMask.eraseColor(Color.TRANSPARENT);

        Canvas lc = new Canvas(letterMask);
        lc.scale(k, k);
        mask.setStyle(Paint.Style.FILL);
        lc.drawPath(glyphPath, mask);

        Canvas nc = new Canvas(nearMask);
        nc.scale(k, k);
        nc.drawPath(glyphPath, mask);
        mask.setStyle(Paint.Style.STROKE);
        mask.setStrokeWidth(brushWidth * 1.3f);
        nc.drawPath(glyphPath, mask);

        Canvas ic = new Canvas(inkMask);
        ic.scale(k, k);
        mask.setStrokeWidth(brushWidth);
        for (Stroke s : strokes) ic.drawPath(s.path, mask);
        // The same lines, much wider: "near the centre line" is anywhere this covers.
        Canvas wc = new Canvas(wideMask);
        wc.scale(k, k);
        mask.setStrokeWidth(brushWidth * 2.2f);
        for (Stroke s : strokes) wc.drawPath(s.path, mask);

        int w = letterMask.getWidth(), h = letterMask.getHeight();
        int[] l = new int[w * h], n = new int[w * h], i = new int[w * h], wi = new int[w * h];
        letterMask.getPixels(l, 0, w, 0, 0, w, h);
        nearMask.getPixels(n, 0, w, 0, 0, w, h);
        inkMask.getPixels(i, 0, w, 0, 0, w, h);
        wideMask.getPixels(wi, 0, w, 0, 0, w, h);
        if (spine == null || spine.length != l.length) {
            boolean[] solid = new boolean[l.length];
            for (int p = 0; p < l.length; p++) solid[p] = (l[p] >>> 24) > 127;
            spine = thin(solid, w, h);
        }
        int letter = 0, followed = 0, inked = 0, onLetter = 0;
        for (int p = 0; p < l.length; p++) {
            if (spine[p]) {
                letter++;
                if ((wi[p] >>> 24) > 127) followed++;
            }
            if ((i[p] >>> 24) > 127) {
                inked++;
                if ((n[p] >>> 24) > 127) onLetter++;
            }
        }
        if (letter == 0 || inked == 0) return;
        float coverage = followed / (float) letter, precision = onLetter / (float) inked;
        MainActivity.log(String.format(java.util.Locale.US, "trace check coverage=%.2f precision=%.2f", coverage, precision));
        if (coverage >= required && precision >= 0.55f) {
            traced = true;
            doneAnim = ValueAnimator.ofFloat(0f, 1f);
            doneAnim.setDuration(550);
            doneAnim.addUpdateListener(a -> {
                done = (float) a.getAnimatedValue();
                invalidate();
            });
            doneAnim.start();
            for (int s = 0; s < 30; s++) {
                addSpark(getWidth() * (0.15f + 0.7f * rnd.nextFloat()), getHeight() * (0.1f + 0.8f * rnd.nextFloat()));
            }
            MainActivity.log("traced " + glyph);
            if (listener != null) listener.onTraced();
        } else if (precision < 0.4f && inked > letter * 8) {
            if (listener != null) listener.onMessy();
        }
    }

    /**
     * Zhang-Suen thinning: peels a shape's edges away, layer by layer, until only a one-pixel centre line is left,
     * keeping the shape connected. That line is the path a child's finger should follow.
     */
    static boolean[] thin(boolean[] shape, int w, int h) {
        boolean[] a = shape.clone();
        int[] gone = new int[a.length];
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int pass = 0; pass < 2; pass++) {
                int count = 0;
                for (int y = 1; y < h - 1; y++) {
                    for (int x = 1; x < w - 1; x++) {
                        int p = y * w + x;
                        if (!a[p]) continue;
                        boolean p2 = a[p - w], p3 = a[p - w + 1], p4 = a[p + 1], p5 = a[p + w + 1];
                        boolean p6 = a[p + w], p7 = a[p + w - 1], p8 = a[p - 1], p9 = a[p - w - 1];
                        boolean[] ring = {p2, p3, p4, p5, p6, p7, p8, p9, p2};
                        int neighbours = 0, rises = 0;
                        for (int k = 0; k < 8; k++) {
                            if (ring[k]) neighbours++;
                            if (!ring[k] && ring[k + 1]) rises++;
                        }
                        if (neighbours < 2 || neighbours > 6 || rises != 1) continue;
                        if (pass == 0 ? (p2 && p4 && p6) || (p4 && p6 && p8) : (p2 && p4 && p8) || (p2 && p6 && p8)) continue;
                        gone[count++] = p;
                    }
                }
                for (int k = 0; k < count; k++) a[gone[k]] = false;
                if (count > 0) changed = true;
            }
        }
        return a;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (doneAnim != null) doneAnim.cancel();
    }
}
