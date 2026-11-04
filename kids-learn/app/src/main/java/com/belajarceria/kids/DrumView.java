package com.belajarceria.kids;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

/**
 * A drum kit seen from the drummer's seat: a big bass drum, snare, three toms, a hi-hat (tap the top to close it,
 * the rim to open it) and a crash cymbal. Each is a big, colourful target that squashes and sends out rings when hit.
 */
final class DrumView extends InstrumentView {
    // which sound, x, y (fractions of the view), radius (fraction of height), colour, is a cymbal
    private static final float[][] PADS = {
            {0, 0.50f, 0.70f, 0.29f}, // kick
            {1, 0.27f, 0.70f, 0.19f}, // snare
            {2, 0.09f, 0.30f, 0.14f}, // hi-hat
            {3, 0.09f, 0.72f, 0.12f}, // open hat
            {4, 0.38f, 0.27f, 0.15f}, // tom 1
            {5, 0.62f, 0.27f, 0.15f}, // tom 2
            {6, 0.76f, 0.72f, 0.20f}, // floor tom
            {7, 0.91f, 0.27f, 0.17f}, // crash
    };
    private static final int[] COLORS = {0xFFFF4F5E, 0xFF3A86FF, 0xFFFFC300, 0xFFFFC300, 0xFF2EC4B6, 0xFF8338EC,
            0xFFFF9F1C, 0xFFFFC300};
    private static final String[] LABELS = {"BOOM", "Snare", "Hi-hat", "Open hat", "Tom", "Tom", "Floor", "Crash"};

    private final float[] hit = new float[PADS.length];
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();

    DrumView(Context c) {
        super(c);
        ring.setStyle(Paint.Style.STROKE);
        label.setTextAlign(Paint.Align.CENTER);
        label.setFakeBoldText(true);
        setContentDescription("drums");
    }

    private boolean cymbal(int i) {
        int s = (int) PADS[i][0];
        return s == 2 || s == 3 || s == 7;
    }

    @Override
    void down(int pointer, float x, float y) {
        float h = getHeight(), w = getWidth();
        // the nearest pad whose circle the finger is in, smallest first so small drums aren't hidden by big ones
        int best = -1;
        float bestD = Float.MAX_VALUE;
        for (int i = 0; i < PADS.length; i++) {
            float cx = PADS[i][1] * w, cy = PADS[i][2] * h, rad = PADS[i][3] * h * (cymbal(i) ? 1.25f : 1f);
            float d = (float) Math.hypot(x - cx, (y - cy) * (cymbal(i) ? 2.2f : 1.4f));
            if (d < rad * 1.1f && d / rad < bestD) {
                bestD = d / rad;
                best = i;
            }
        }
        if (best < 0) return;
        int sound = (int) PADS[best][0];
        synth.play(synth.sound("drum" + sound, () -> SoundBank.drum(sound)), 1f);
        hit[best] = 1f;
        sparkle(x, y, COLORS[best]);
        MainActivity.log("music drum " + SoundBank.DRUMS[sound]);
    }

    @Override
    void prewarm() {
        String[] keys = new String[SoundBank.DRUMS.length];
        Synth.Render[] renders = new Synth.Render[keys.length];
        for (int i = 0; i < keys.length; i++) {
            int d = i;
            keys[i] = "drum" + d;
            renders[i] = () -> SoundBank.drum(d);
        }
        synth.prewarm(keys, renders);
    }

    @Override
    void drawInstrument(Canvas c, float t) {
        float w = getWidth(), h = getHeight();
        for (int i = 0; i < PADS.length; i++) {
            float cx = PADS[i][1] * w, cy = PADS[i][2] * h, rad = PADS[i][3] * h;
            float k = hit[i];
            hit[i] = Math.max(0f, k - 0.06f);
            float squash = 1f - 0.08f * k;
            int col = COLORS[i];
            if (cymbal(i)) {
                // a brass cymbal on a stand, wobbling when struck
                float tilt = (float) Math.sin(t * 30) * 8f * k;
                paint.setShader(null);
                paint.setColor(0xFF9E9E9E);
                c.drawRect(cx - Ui.dp(2), cy, cx + Ui.dp(2), Math.min(h, cy + rad * 2.2f), paint);
                c.save();
                c.rotate(tilt, cx, cy);
                r.set(cx - rad * 1.25f, cy - rad * 0.42f * squash, cx + rad * 1.25f, cy + rad * 0.42f * squash);
                paint.setColor(0xFFFFFFFF); // a shader is still scaled by the paint's alpha
                paint.setShader(new RadialGradient(cx - rad * 0.3f, cy - rad * 0.2f, rad * 1.4f,
                        new int[]{0xFFFFF3B0, 0xFFFFC300, 0xFFC98A00}, null, Shader.TileMode.CLAMP));
                c.drawOval(r, paint);
                paint.setShader(null);
                paint.setColor(0x55FFFFFF);
                for (int g = 1; g <= 3; g++) {
                    ring.setColor(0x33A06A00);
                    ring.setStrokeWidth(Ui.dp(1));
                    c.drawOval(cx - rad * 1.25f * g / 4, cy - rad * 0.42f * g / 4, cx + rad * 1.25f * g / 4, cy + rad * 0.42f * g / 4, ring);
                }
                paint.setColor(0xFFB07A00);
                c.drawCircle(cx, cy, Ui.dp(5), paint);
                c.restore();
            } else {
                // a drum: coloured shell, white head, chrome rim; it squashes and glows when hit
                paint.setShader(null);
                paint.setColor(0x33000000);
                c.drawCircle(cx + Ui.dp(3), cy + Ui.dp(6), rad, paint);
                paint.setColor(Content.mix(col, 0xFF000000, 0.25f));
                c.drawCircle(cx, cy + rad * 0.12f, rad, paint);
                paint.setColor(col);
                c.drawCircle(cx, cy, rad, paint);
                paint.setColor(0xFFE0E0E8);
                c.drawCircle(cx, cy, rad * 0.86f * squash, paint);
                paint.setColor(0xFFFFFFFF); // a shader is still scaled by the paint's alpha
                paint.setShader(new RadialGradient(cx - rad * 0.25f, cy - rad * 0.3f, rad,
                        new int[]{0xFFFFFFFF, k > 0 ? Content.mix(0xFFF6F3EA, col, 0.4f * k) : 0xFFF6F3EA}, null,
                        Shader.TileMode.CLAMP));
                c.drawCircle(cx, cy, rad * 0.8f * squash, paint);
                paint.setShader(null);
                // tension lugs around the rim
                paint.setColor(0xFFCFD3DC);
                for (int l = 0; l < 8; l++) {
                    double a = Math.PI / 4 * l;
                    c.drawCircle(cx + (float) Math.cos(a) * rad * 0.93f, cy + (float) Math.sin(a) * rad * 0.93f, Ui.dp(3), paint);
                }
            }
            if (k > 0) {
                // rings spreading out from the hit
                ring.setColor((col & 0x00FFFFFF) | ((int) (160 * k) << 24));
                ring.setStrokeWidth(Ui.dp(4) * k);
                c.drawCircle(cx, cy, rad * (1.05f + (1 - k) * 0.5f), ring);
            }
            label.setColor(cymbal(i) ? 0xFF7A5200 : Content.mix(col, 0xFF000000, 0.3f));
            label.setTextSize(Math.min(rad * 0.3f, Ui.dp(16)));
            c.drawText(LABELS[i], cx, cy + label.getTextSize() * 0.35f + (cymbal(i) ? rad * 0.75f : 0), label);
        }
    }
}
