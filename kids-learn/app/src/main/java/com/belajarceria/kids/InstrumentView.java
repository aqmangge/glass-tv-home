package com.belajarceria.kids;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Random;

/**
 * Shared by the four instruments: every finger is tracked separately (chords, drum rolls, glissandi), and every note
 * sends a colourful ♪ or ♫ floating up from where it was played.
 */
abstract class InstrumentView extends View {
    static final int[] RAINBOW = {0xFFFF4F5E, 0xFFFF9F1C, 0xFFFFD23F, 0xFF2EC4B6, 0xFF3A86FF, 0xFF8338EC, 0xFFFF6FB5};

    private static final class Note {
        float x, y, vx, vy, life, rot, size;
        int color;
        String glyph;
    }

    private final ArrayList<Note> notes = new ArrayList<>();
    private final Paint notePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    protected final Random rnd = new Random();
    protected final Synth synth = Synth.get();
    private long last = SystemClock.uptimeMillis();

    InstrumentView(Context c) {
        super(c);
        notePaint.setTextAlign(Paint.Align.CENTER);
        notePaint.setFakeBoldText(true);
    }

    /** A finger touched down (or slid in) at x, y. */
    abstract void down(int pointer, float x, float y);

    /** A finger moved. */
    void move(int pointer, float x, float y) {}

    /** A finger lifted. */
    void up(int pointer) {}

    /** Draws the instrument itself; t is seconds since start, for gentle idle animation. */
    abstract void drawInstrument(Canvas c, float t);

    /** Prepares this instrument's sounds in the background. */
    abstract void prewarm();

    void sparkle(float x, float y, int color) {
        Note n = new Note();
        n.x = x;
        n.y = y;
        n.vx = (rnd.nextFloat() - 0.5f) * Ui.dp(60);
        n.vy = -Ui.dp(90) - rnd.nextFloat() * Ui.dp(70);
        n.life = 1f;
        n.rot = (rnd.nextFloat() - 0.5f) * 40f;
        n.size = Ui.dp(22) + rnd.nextFloat() * Ui.dp(12);
        n.color = color;
        n.glyph = rnd.nextBoolean() ? "♪" : "♫";
        notes.add(n);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int a = e.getActionMasked();
        switch (a) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                int i = e.getActionIndex();
                getParent().requestDisallowInterceptTouchEvent(true);
                down(e.getPointerId(i), e.getX(i), e.getY(i));
                break;
            }
            case MotionEvent.ACTION_MOVE:
                // A fast finger arrives as several positions in one event: play every one, or a quick strum or
                // glissando would skip strings and bars.
                for (int i = 0; i < e.getPointerCount(); i++) {
                    int id = e.getPointerId(i);
                    for (int h = 0; h < e.getHistorySize(); h++) move(id, e.getHistoricalX(i, h), e.getHistoricalY(i, h));
                    move(id, e.getX(i), e.getY(i));
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP: {
                int i = e.getActionIndex();
                move(e.getPointerId(i), e.getX(i), e.getY(i)); // the last stretch before the finger lifted
                up(e.getPointerId(i));
                break;
            }
            case MotionEvent.ACTION_CANCEL:
                for (int i = 0; i < e.getPointerCount(); i++) up(e.getPointerId(i));
                break;
            default:
                break;
        }
        invalidate();
        return true;
    }

    private final long start = SystemClock.uptimeMillis();

    @Override
    protected void onDraw(Canvas c) {
        long now = SystemClock.uptimeMillis();
        float dt = Math.min(0.05f, (now - last) / 1000f);
        last = now;
        drawInstrument(c, (now - start) / 1000f);
        for (int i = notes.size() - 1; i >= 0; i--) {
            Note n = notes.get(i);
            n.life -= dt * 0.9f;
            if (n.life <= 0) {
                notes.remove(i);
                continue;
            }
            n.x += n.vx * dt + (float) Math.sin(n.life * 9) * Ui.dp(0.6f);
            n.y += n.vy * dt;
            notePaint.setTextSize(n.size);
            notePaint.setColor((n.color & 0x00FFFFFF) | ((int) (255 * Math.min(1f, n.life * 1.6f)) << 24));
            c.save();
            c.rotate(n.rot, n.x, n.y);
            c.drawText(n.glyph, n.x, n.y, notePaint);
            c.restore();
        }
        postInvalidateOnAnimation();
    }

    static String noteName(int midi) {
        String[] names = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        return names[midi % 12];
    }
}
