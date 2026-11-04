package com.belajarceria.kids;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.SparseArray;

/**
 * A guitar laid across the screen: chord buttons on the left (C, G, D, Am, Em, F, or open strings), six strings in
 * standard tuning on a wooden body. Tap a string to pluck it, or swipe across the strings to strum the chord.
 * A plucked string shimmers as it rings.
 */
final class GuitarView extends InstrumentView {
    private static final int[] CHORD_COLORS = {0xFFFF4F5E, 0xFFFF9F1C, 0xFFFFC300, 0xFF2EC4B6, 0xFF3A86FF, 0xFF8338EC, 0xFF9E9E9E};

    private int chord = -1; // -1: open strings
    private final float[] ring = new float[6];
    private final SparseArray<Float> lastY = new SparseArray<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();

    GuitarView(Context c) {
        super(c);
        label.setTextAlign(Paint.Align.CENTER);
        label.setFakeBoldText(true);
        setContentDescription("guitar");
    }

    private float panelW() {
        return Math.min(getWidth() * 0.22f, Ui.dp(190));
    }

    /** y of string s (0 = low E at the top, as a guitarist sees it looking down). */
    private float stringY(int s) {
        float top = getHeight() * 0.14f, bottom = getHeight() * 0.86f;
        return top + (bottom - top) * s / 5f;
    }

    private int midiFor(int s) {
        if (chord < 0) return SoundBank.GUITAR_STRINGS[s];
        int fret = SoundBank.FRETS[chord][s];
        return fret < 0 ? -1 : SoundBank.GUITAR_STRINGS[s] + fret;
    }

    private void pluck(int s, float x) {
        int midi = midiFor(s);
        if (midi < 0) return; // a string this chord doesn't use
        synth.play(synth.sound("guitar" + midi, () -> SoundBank.guitar(midi)), 0.85f);
        ring[s] = 1f;
        sparkle(x, stringY(s), CHORD_COLORS[chord < 0 ? 6 : chord]);
        MainActivity.log("music guitar " + midi);
    }

    @Override
    void down(int pointer, float x, float y) {
        if (x < panelW()) {
            // the chord buttons: two columns of round buttons, the last one "open"
            int rows = 4, col = x < panelW() / 2 ? 0 : 1;
            int row = (int) (y / (getHeight() / (float) rows));
            int idx = row * 2 + col;
            chord = idx < SoundBank.CHORDS.length ? idx : -1;
            Sfx.tick();
            MainActivity.log("music chord " + (chord < 0 ? "open" : SoundBank.CHORDS[chord]));
            return;
        }
        // tap: pluck the nearest string
        int nearest = 0;
        for (int s = 1; s < 6; s++) if (Math.abs(y - stringY(s)) < Math.abs(y - stringY(nearest))) nearest = s;
        if (Math.abs(y - stringY(nearest)) < (stringY(1) - stringY(0)) * 0.55f) pluck(nearest, x);
        lastY.put(pointer, y);
    }

    @Override
    void move(int pointer, float x, float y) {
        Float from = lastY.get(pointer);
        if (from == null || x < panelW()) return;
        // strum: every string the finger crossed since the last move rings, in order
        for (int s = 0; s < 6; s++) {
            float sy = stringY(s);
            if ((from < sy && y >= sy) || (from > sy && y <= sy)) pluck(s, x);
        }
        lastY.put(pointer, y);
    }

    @Override
    void up(int pointer) {
        lastY.remove(pointer);
    }

    @Override
    void prewarm() {
        java.util.TreeSet<Integer> all = new java.util.TreeSet<>();
        for (int s = 0; s < 6; s++) {
            all.add(SoundBank.GUITAR_STRINGS[s]);
            for (int[] f : SoundBank.FRETS) if (f[s] >= 0) all.add(SoundBank.GUITAR_STRINGS[s] + f[s]);
        }
        String[] keys = new String[all.size()];
        Synth.Render[] renders = new Synth.Render[all.size()];
        int i = 0;
        for (int m : all) {
            keys[i] = "guitar" + m;
            renders[i++] = () -> SoundBank.guitar(m);
        }
        synth.prewarm(keys, renders);
    }

    @Override
    void drawInstrument(Canvas c, float t) {
        float w = getWidth(), h = getHeight(), pw = panelW();
        // the wooden body, with a sound hole and rosette
        r.set(pw + Ui.dp(12), 0, w, h);
        paint.setColor(0xFFFFFFFF); // a shader is still scaled by the paint's alpha
        paint.setShader(new LinearGradient(0, 0, 0, h, 0xFFE8A55A, 0xFFB8692E, Shader.TileMode.CLAMP));
        c.drawRoundRect(r, Ui.dp(28), Ui.dp(28), paint);
        paint.setShader(null);
        float hx = w * 0.78f, hy = h * 0.5f, hr = h * 0.27f;
        paint.setColor(0xFF7A4520);
        c.drawCircle(hx, hy, hr * 1.18f, paint);
        int[] rosette = {0xFFFF4F5E, 0xFFFFC300, 0xFF2EC4B6, 0xFF3A86FF};
        for (int k = 0; k < rosette.length; k++) {
            paint.setColor(rosette[k]);
            c.drawCircle(hx, hy, hr * (1.14f - k * 0.03f), paint);
        }
        paint.setColor(0xFFFFFFFF); // a shader is still scaled by the paint's alpha
        paint.setShader(new RadialGradient(hx, hy, hr, 0xFF2A160A, 0xFF3D2412, Shader.TileMode.CLAMP));
        c.drawCircle(hx, hy, hr, paint);
        paint.setShader(null);
        // the strings: thick and bronze at the bottom of the pitch range, thin and silver at the top
        for (int s = 0; s < 6; s++) {
            float y = stringY(s), k = ring[s];
            ring[s] = Math.max(0f, k - 0.012f);
            float thick = Ui.dp(5.5f - s * 0.75f);
            boolean used = midiFor(s) >= 0;
            paint.setColor(!used ? 0x66D7CCB8 : s < 3 ? 0xFFD9A441 : 0xFFE6E9F0);
            paint.setStrokeWidth(thick);
            if (k > 0.01f) {
                // a ringing string: a wobble that dies away
                float amp = Ui.dp(5) * k, step = Ui.dp(8);
                float px = pw + Ui.dp(12), py = y;
                for (float x = px + step; x <= w; x += step) {
                    float ny = y + (float) Math.sin(x * 0.05f + t * 60) * amp * (float) Math.sin(Math.PI * (x - px) / (w - px));
                    c.drawLine(px, py, x, ny, paint);
                    px = x;
                    py = ny;
                }
            } else {
                c.drawLine(pw + Ui.dp(12), y, w, y, paint);
            }
            if (!used) {
                label.setColor(0xCCFFFFFF);
                label.setTextSize(Ui.dp(14));
                c.drawText("✕", pw + Ui.dp(28), y + Ui.dp(5), label);
            }
        }
        // chord buttons
        int rows = 4;
        float bh = h / rows;
        for (int i = 0; i <= SoundBank.CHORDS.length; i++) {
            int col = i % 2, row = i / 2;
            float cx = pw / 4f + col * pw / 2f, cy = bh * (row + 0.5f), rad = Math.min(pw / 4.6f, bh * 0.4f);
            boolean on = (i == SoundBank.CHORDS.length && chord < 0) || i == chord;
            int color = CHORD_COLORS[i == SoundBank.CHORDS.length ? 6 : i];
            paint.setColor(0x33000000);
            c.drawCircle(cx + Ui.dp(2), cy + Ui.dp(4), rad, paint);
            paint.setColor(on ? color : 0xF2FFFFFF);
            c.drawCircle(cx, cy, on ? rad * 1.08f : rad, paint);
            label.setColor(on ? 0xFFFFFFFF : color);
            label.setTextSize(rad * (i == SoundBank.CHORDS.length ? 0.55f : 0.75f));
            c.drawText(i == SoundBank.CHORDS.length ? "Open" : SoundBank.CHORDS[i], cx, cy + label.getTextSize() * 0.36f, label);
        }
    }
}
