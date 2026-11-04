package com.belajarceria.kids;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.SparseArray;

/**
 * A two-octave piano, C4 to C6, with rainbow-coloured note names under the white keys. Several fingers play chords;
 * sliding along the keys plays each one in turn; a note keeps ringing while held and fades when let go.
 */
final class PianoView extends InstrumentView {
    private static final int LOW = 60, HIGH = 84; // C4 .. C6
    private static final int[] WHITE_STEPS = {0, 2, 4, 5, 7, 9, 11};

    private final int[] whites, blacks;
    private final SparseArray<Integer> fingerKey = new SparseArray<>();
    private final SparseArray<Synth.Voice> fingerVoice = new SparseArray<>();
    private final boolean[] down = new boolean[128];
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();

    PianoView(Context c) {
        super(c);
        int nw = 0, nb = 0;
        for (int m = LOW; m <= HIGH; m++) if (isWhite(m)) nw++; else nb++;
        whites = new int[nw];
        blacks = new int[nb];
        nw = 0;
        nb = 0;
        for (int m = LOW; m <= HIGH; m++) if (isWhite(m)) whites[nw++] = m; else blacks[nb++] = m;
        label.setTextAlign(Paint.Align.CENTER);
        label.setFakeBoldText(true);
        setContentDescription("piano");
    }

    private static boolean isWhite(int m) {
        int s = m % 12;
        for (int w : WHITE_STEPS) if (w == s) return true;
        return false;
    }

    private float keyW() {
        return getWidth() / (float) whites.length;
    }

    private int whiteIndex(int midi) {
        for (int i = 0; i < whites.length; i++) if (whites[i] == midi) return i;
        return -1;
    }

    /** The black key over (x, y) if any, else the white key under x. */
    private int keyAt(float x, float y) {
        float kw = keyW(), bh = getHeight() * 0.6f;
        if (y < bh) {
            for (int b : blacks) {
                float cx = (whiteIndex(b - 1) + 1) * kw;
                if (Math.abs(x - cx) < kw * 0.32f) return b;
            }
        }
        int i = (int) (x / kw);
        return whites[Math.max(0, Math.min(whites.length - 1, i))];
    }

    private void press(int pointer, int midi, float x, float y) {
        Integer old = fingerKey.get(pointer);
        if (old != null && old == midi) return;
        lift(pointer);
        fingerKey.put(pointer, midi);
        down[midi] = true;
        short[] s = synth.sound("piano" + midi, () -> SoundBank.piano(midi));
        fingerVoice.put(pointer, synth.play(s, 0.9f));
        int color = RAINBOW[noteColorIndex(midi)];
        sparkle(x, y - Ui.dp(10), color);
        MainActivity.log("music piano " + midi);
    }

    private static int noteColorIndex(int midi) {
        int s = midi % 12;
        for (int i = 0; i < WHITE_STEPS.length; i++) if (WHITE_STEPS[i] >= s) return i;
        return 6;
    }

    private void lift(int pointer) {
        Integer k = fingerKey.get(pointer);
        if (k == null) return;
        down[k] = false;
        synth.release(fingerVoice.get(pointer), 0.35f); // the damper settles on the string
        fingerKey.remove(pointer);
        fingerVoice.remove(pointer);
    }

    @Override
    void down(int pointer, float x, float y) {
        press(pointer, keyAt(x, y), x, y);
    }

    @Override
    void move(int pointer, float x, float y) {
        if (fingerKey.get(pointer) != null && x >= 0 && x < getWidth() && y >= 0 && y < getHeight()) {
            press(pointer, keyAt(x, y), x, y);
        }
    }

    @Override
    void up(int pointer) {
        lift(pointer);
    }

    @Override
    void prewarm() {
        int n = HIGH - LOW + 1;
        String[] keys = new String[n];
        Synth.Render[] renders = new Synth.Render[n];
        for (int i = 0; i < n; i++) {
            int m = LOW + i;
            keys[i] = "piano" + m;
            renders[i] = () -> SoundBank.piano(m);
        }
        synth.prewarm(keys, renders);
    }

    @Override
    void drawInstrument(Canvas c, float t) {
        float kw = keyW(), h = getHeight(), gap = Ui.dp(3), rad = Ui.dp(10);
        // white keys: ivory, rounded at the bottom, a rainbow label; pressed ones sink and glow in their colour
        for (int i = 0; i < whites.length; i++) {
            int m = whites[i];
            int color = RAINBOW[noteColorIndex(m)];
            float sink = down[m] ? Ui.dp(4) : 0;
            r.set(i * kw + gap / 2, sink, (i + 1) * kw - gap / 2, h - Ui.dp(4) + sink);
            paint.setShader(null);
            paint.setColor(0x33000000);
            c.drawRoundRect(r.left, r.top + Ui.dp(4), r.right, r.bottom + Ui.dp(4) - sink, rad, rad, paint);
            paint.setColor(0xFFFFFFFF); // a shader is still scaled by the paint's alpha
            paint.setShader(new LinearGradient(0, r.top, 0, r.bottom,
                    down[m] ? Content.mix(0xFFFFFFFF, color, 0.35f) : 0xFFFFFFFF,
                    down[m] ? Content.mix(0xFFFFFFFF, color, 0.6f) : 0xFFF1F1F6, Shader.TileMode.CLAMP));
            c.drawRoundRect(r, rad, rad, paint);
            paint.setShader(null);
            // the coloured name pill at the bottom of the key
            float ph = Math.min(kw * 0.62f, Ui.dp(34));
            paint.setColor(color);
            c.drawRoundRect(r.left + kw * 0.16f, r.bottom - ph - Ui.dp(10), r.right - kw * 0.16f, r.bottom - Ui.dp(10),
                    ph / 2, ph / 2, paint);
            label.setColor(0xFFFFFFFF);
            label.setTextSize(Math.min(kw * 0.36f, Ui.dp(17)));
            c.drawText(noteName(m), r.centerX(), r.bottom - Ui.dp(10) - ph / 2 + label.getTextSize() * 0.36f, label);
        }
        // black keys: glossy ebony
        float bh = h * 0.6f;
        for (int b : blacks) {
            float cx = (whiteIndex(b - 1) + 1) * kw;
            float sink = down[b] ? Ui.dp(3) : 0;
            r.set(cx - kw * 0.3f, sink - rad, cx + kw * 0.3f, bh + sink);
            paint.setColor(0x44000000);
            c.drawRoundRect(r.left + Ui.dp(2), r.top + Ui.dp(4), r.right + Ui.dp(2), r.bottom + Ui.dp(4), rad * 0.6f, rad * 0.6f, paint);
            int top = down[b] ? Content.mix(0xFF2B2D42, RAINBOW[noteColorIndex(b)], 0.45f) : 0xFF2B2D42;
            paint.setColor(0xFFFFFFFF); // a shader is still scaled by the paint's alpha
            paint.setShader(new LinearGradient(0, r.top, 0, r.bottom, top, 0xFF0E0F18, Shader.TileMode.CLAMP));
            c.drawRoundRect(r, rad * 0.6f, rad * 0.6f, paint);
            paint.setShader(null);
            paint.setColor(0x33FFFFFF);
            c.drawRoundRect(r.left + Ui.dp(3), r.top + rad, r.left + Ui.dp(7), r.bottom - Ui.dp(8), Ui.dp(2), Ui.dp(2), paint);
        }
    }
}
