package com.belajarceria.kids;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.SparseIntArray;

/**
 * A rainbow xylophone: fifteen bars from C5 to C7 (the white notes), long and low on the left, short and high on the
 * right. Tap a bar, or slide a finger across them for a glissando; each struck bar bounces and glows.
 */
final class XyloView extends InstrumentView {
    private static final int[] BARS = {72, 74, 76, 77, 79, 81, 83, 84, 86, 88, 89, 91, 93, 95, 96};
    private static final int[] COLORS = {0xFFFF4F5E, 0xFFFF7A3D, 0xFFFF9F1C, 0xFFFFC300, 0xFFB5D334, 0xFF5CC86B,
            0xFF2EC4B6, 0xFF2BA7E0, 0xFF3A86FF, 0xFF5E6BFF, 0xFF8338EC, 0xFFB14FD6, 0xFFE05CB8, 0xFFFF6FB5, 0xFFFF4F79};

    private final float[] hit = new float[BARS.length];
    private final SparseIntArray fingerBar = new SparseIntArray();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();

    XyloView(Context c) {
        super(c);
        label.setTextAlign(Paint.Align.CENTER);
        label.setFakeBoldText(true);
        setContentDescription("xylophone");
    }

    private float barW() {
        return getWidth() / (float) BARS.length;
    }

    /** Bar i's top and bottom: the low bars are long, the high ones short, centred between the rails. */
    private float[] barSpan(int i) {
        float h = getHeight(), len = h * (0.92f - 0.42f * i / (BARS.length - 1f));
        return new float[]{h / 2 - len / 2, h / 2 + len / 2};
    }

    private int barAt(float x, float y) {
        int i = (int) (x / barW());
        if (i < 0 || i >= BARS.length) return -1;
        float[] s = barSpan(i);
        return y > s[0] - Ui.dp(16) && y < s[1] + Ui.dp(16) ? i : -1;
    }

    private void strike(int i, float x, float y) {
        int midi = BARS[i];
        synth.play(synth.sound("xylo" + midi, () -> SoundBank.xylophone(midi)), 0.9f);
        hit[i] = 1f;
        sparkle(x, y, COLORS[i]);
        MainActivity.log("music xylophone " + midi);
    }

    @Override
    void down(int pointer, float x, float y) {
        int i = barAt(x, y);
        fingerBar.put(pointer, i);
        if (i >= 0) strike(i, x, y);
    }

    @Override
    void move(int pointer, float x, float y) {
        int i = barAt(x, y);
        if (i >= 0 && i != fingerBar.get(pointer, -1)) strike(i, x, y); // a glissando
        fingerBar.put(pointer, i);
    }

    @Override
    void up(int pointer) {
        fingerBar.delete(pointer);
    }

    @Override
    void prewarm() {
        String[] keys = new String[BARS.length];
        Synth.Render[] renders = new Synth.Render[BARS.length];
        for (int i = 0; i < BARS.length; i++) {
            int m = BARS[i];
            keys[i] = "xylo" + m;
            renders[i] = () -> SoundBank.xylophone(m);
        }
        synth.prewarm(keys, renders);
    }

    @Override
    void drawInstrument(Canvas c, float t) {
        float w = getWidth(), h = getHeight(), bw = barW();
        // two wooden rails under the bars, closer together towards the short, high end
        paint.setShader(null);
        paint.setColor(0xFF8D5A3B);
        float[] first = barSpan(0), last = barSpan(BARS.length - 1);
        float inset = Ui.dp(26);
        c.drawLine(0, first[0] + inset, w, last[0] + inset, rail());
        c.drawLine(0, first[1] - inset, w, last[1] - inset, rail());
        for (int i = 0; i < BARS.length; i++) {
            float k = hit[i];
            hit[i] = Math.max(0f, k - 0.05f);
            float[] s = barSpan(i);
            float bounce = (float) Math.sin(k * Math.PI * 3) * Ui.dp(6) * k;
            r.set(i * bw + Ui.dp(4), s[0] + bounce, (i + 1) * bw - Ui.dp(4), s[1] + bounce);
            int col = COLORS[i];
            paint.setShader(null);
            paint.setColor(0x33000000);
            c.drawRoundRect(r.left + Ui.dp(2), r.top + Ui.dp(5), r.right + Ui.dp(2), r.bottom + Ui.dp(5), Ui.dp(10), Ui.dp(10), paint);
            int light = Content.mix(col, 0xFFFFFFFF, 0.25f + 0.4f * k), dark = Content.mix(col, 0xFF000000, 0.12f);
            paint.setColor(0xFFFFFFFF); // a shader is still scaled by the paint's alpha
            paint.setShader(new LinearGradient(r.left, 0, r.right, 0, light, dark, Shader.TileMode.CLAMP));
            c.drawRoundRect(r, Ui.dp(10), Ui.dp(10), paint);
            paint.setShader(null);
            paint.setColor(0x55FFFFFF);
            c.drawRoundRect(r.left + Ui.dp(4), r.top + Ui.dp(8), r.left + Ui.dp(9), r.bottom - Ui.dp(8), Ui.dp(3), Ui.dp(3), paint);
            // the two pins that hold the bar on the rails
            paint.setColor(0xFFEDEDED);
            c.drawCircle(r.centerX(), r.top + inset, Ui.dp(3.5f), paint);
            c.drawCircle(r.centerX(), r.bottom - inset, Ui.dp(3.5f), paint);
            label.setColor(0xFFFFFFFF);
            label.setTextSize(Math.min(bw * 0.38f, Ui.dp(18)));
            c.drawText(noteName(BARS[i]), r.centerX(), r.centerY() + label.getTextSize() * 0.36f, label);
        }
    }

    private final Paint railPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Paint rail() {
        railPaint.setColor(0xFF8D5A3B);
        railPaint.setStrokeWidth(Ui.dp(10));
        railPaint.setStrokeCap(Paint.Cap.ROUND);
        return railPaint;
    }
}
