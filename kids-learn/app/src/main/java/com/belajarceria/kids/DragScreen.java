package com.belajarceria.kids;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Random;

/**
 * Drag & drop: three picture cards with an empty slot each, and three tiles below. Drag each tile into the slot of
 * its picture: the apple takes "A", three ducklings take "3", the shadow of ب takes ب, the mosque takes مسجد.
 */
final class DragScreen extends Screen {
    private static final int COUNT = 3;

    private static final class Slot {
        Content.Item item;
        View card, hole;
        boolean filled;
    }

    private final Content.Subject s;
    /** The Chinese words' English meaning, shown on request. */
    private final EnglishHint hint = new EnglishHint();
    private final FrameLayout board;
    private final Random rnd = new Random();
    private final ArrayList<Slot> slots = new ArrayList<>();
    private int placed;

    DragScreen(MainActivity a, Content.Subject s) {
        super(a);
        this.s = s;
        LinearLayout col = new LinearLayout(a);
        col.setOrientation(LinearLayout.VERTICAL);
        Ui.noClip(col);
        LinearLayout bar = header(s.title + "  ·  Drag & drop", s);
        if (s.kind == Content.Kind.CHINESE) hint.addButton(this, bar);
        col.addView(bar,
                new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        board = new FrameLayout(a) {
            @Override
            protected void onSizeChanged(int w, int h, int ow, int oh) {
                super.onSizeChanged(w, h, ow, oh);
                if (w > 0 && h > 0) post(DragScreen.this::newRound);
            }
        };
        Ui.noClip(board);
        col.addView(board, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        addView(col, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    private void newRound() {
        int w = board.getWidth(), h = board.getHeight();
        if (w == 0 || h == 0) return;
        board.removeAllViews();
        hint.clear();
        slots.clear();
        placed = 0;
        Content.Item[] items = Content.pick(s.items, COUNT, rnd);
        int n = items.length;
        Content.Item[] tilesOrder = Content.shuffled(items, rnd);

        boolean wide = s.wordTiles();
        int gap = Ui.dp(18), pad = Ui.dp(10);
        float colW = (w - gap * (n + 1)) / (float) n;
        // t: tile height. A card is a picture (0.8 t) over a slot (t); the tiles sit in a row under the cards.
        int t = (int) Math.min(Math.min((h - Ui.dp(48) - 3 * pad) / 2.8f, wide ? colW / 1.7f : colW / 1.25f), Ui.dp(170));
        int tileW = wide ? (int) Math.min(colW - 2 * pad, t * 2.2f) : t;
        int cardW = tileW + 2 * pad, picH = (int) (t * 0.8f), cardH = picH + t + 3 * pad;
        float r = Math.min(t * 0.24f, Ui.dp(28));

        ArrayList<View> cards = new ArrayList<>(), tiles = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Slot slot = new Slot();
            slot.item = items[i];
            FrameLayout card = new FrameLayout(getContext());
            card.setBackground(Ui.round(Ui.GLASS, r + pad));
            Ui.lift(card, 8, s.colors[0]);
            Ui.noClip(card);
            FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, picH, Gravity.TOP);
            pp.setMargins(pad, pad, pad, 0);
            card.addView(picture(items[i]), pp);
            View hole = new View(getContext());
            GradientDrawable hd = Ui.round(Content.mix(Ui.WHITE, s.colors[0], 0.1f), r);
            hd.setStroke(Ui.dp(3), (s.colors[0] & 0x00FFFFFF) | 0x99000000, Ui.dp(10), Ui.dp(7));
            hole.setBackground(hd);
            FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(tileW, t, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            hp.bottomMargin = pad;
            card.addView(hole, hp);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(cardW, cardH);
            lp.leftMargin = (int) (gap + i * (colW + gap) + (colW - cardW) / 2);
            lp.topMargin = Ui.dp(6);
            board.addView(card, lp);
            card.setContentDescription("slot:" + items[i].word);
            slot.card = card;
            slot.hole = hole;
            slots.add(slot);
            cards.add(card);
        }
        for (int i = 0; i < n; i++) {
            View tile = tile(tilesOrder[i], tileW, t, r);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(tileW, t);
            lp.leftMargin = (int) (gap + i * (colW + gap) + (colW - tileW) / 2);
            lp.topMargin = h - t - Ui.dp(22);
            board.addView(tile, lp);
            tile.setOnTouchListener(new Dragger(tilesOrder[i]));
            tiles.add(tile);
        }
        Ui.enter(cards, 60);
        Ui.enter(tiles, 260);
        act.logViewsSoon();
    }

    /** What the card shows: the picture, the dots to count, or for Hijaiyah the letter's shadow. */
    private View picture(Content.Item it) {
        Context c = getContext();
        if (s.kind == Content.Kind.HIJAIYAH) return Ui.fit(c, it.big, 140, 0x40243B53, Typeface.DEFAULT_BOLD);
        TextView t = Ui.fit(c, "", 120, Ui.INK, null);
        Ui.picture(t, it.pic);
        return t;
    }

    private View tile(Content.Item it, int w, int h, float r) {
        Context c = getContext();
        FrameLayout tile = new FrameLayout(c);
        tile.setBackground(Ui.gradient(s.colors, r));
        tile.setForeground(Ui.shine(r));
        Ui.lift(tile, 8, s.colors[0]);
        tile.setContentDescription("tile:" + it.word);
        if (s.wordTiles()) {
            LinearLayout v = new LinearLayout(c);
            v.setOrientation(LinearLayout.VERTICAL);
            v.setPadding(Ui.dp(8), Ui.dp(4), Ui.dp(8), Ui.dp(4));
            TextView big = Ui.fit(c, it.big, 44, Ui.WHITE, Typeface.DEFAULT_BOLD);
            big.setMaxLines(1);
            v.addView(big, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
            TextView small = Ui.fit(c, it.word, 14, 0xEEFFFFFF, Ui.BOLD);
            small.setMaxLines(1);
            hint.track(small, it);
            v.addView(small, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, (int) (h * 0.26f)));
            tile.addView(v, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        } else {
            TextView big = Ui.fit(c, it.big, 120, Ui.WHITE, s.arabic() ? Typeface.DEFAULT_BOLD : Ui.BLACK);
            big.setMaxLines(1);
            big.setShadowLayer(Ui.dp(6), 0, Ui.dp(2), 0x33000000);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            lp.setMargins(Ui.dp(6), Ui.dp(4), Ui.dp(6), Ui.dp(4));
            tile.addView(big, lp);
        }
        return tile;
    }

    private final class Dragger implements View.OnTouchListener {
        private final Content.Item item;
        private float grabX, grabY;
        private boolean locked;

        Dragger(Content.Item item) {
            this.item = item;
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent e) {
            if (locked) return false;
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    Spring.stop(v);
                    v.setAlpha(1f); // grabbed while still floating in
                    grabX = e.getRawX() - v.getTranslationX();
                    grabY = e.getRawY() - v.getTranslationY();
                    v.bringToFront();
                    v.setTranslationZ(Ui.dp(14)); // lifted off the table: a deeper shadow
                    Spring.scale(v, 1.14f, 500f, 0.6f);
                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    Sfx.tick();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    v.setTranslationX(e.getRawX() - grabX);
                    v.setTranslationY(e.getRawY() - grabY);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    drop(v);
                    return true;
                default:
                    return true;
            }
        }

        private void drop(View v) {
            float cx = v.getLeft() + v.getTranslationX() + v.getWidth() / 2f;
            float cy = v.getTop() + v.getTranslationY() + v.getHeight() / 2f;
            Slot hit = null;
            int slack = Ui.dp(28);
            for (Slot sl : slots) {
                View c = sl.card;
                if (!sl.filled && cx > c.getLeft() - slack && cx < c.getRight() + slack
                        && cy > c.getTop() - slack && cy < c.getBottom() + slack) {
                    hit = sl;
                    break;
                }
            }
            v.setTranslationZ(0f);
            if (hit != null && hit.item == item) {
                hit.filled = true;
                locked = true;
                placed++;
                MainActivity.log("drag right " + item.word);
                float tx = hit.card.getLeft() + hit.hole.getLeft() + hit.hole.getWidth() / 2f - (v.getLeft() + v.getWidth() / 2f);
                float ty = hit.card.getTop() + hit.hole.getTop() + hit.hole.getHeight() / 2f - (v.getTop() + v.getHeight() / 2f);
                Spring.move(v, tx, ty, 450f, 0.62f);
                Spring.scale(v, 1f, 450f, 0.45f);
                Ui.jelly(hit.card, 1.4f);
                Sfx.ding();
                act.reward(hit.card, s);
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                act.voice.item(s, item);
                if (placed == slots.size()) postDelayed(() -> act.celebrate(DragScreen.this::newRound), 1000);
            } else {
                if (hit != null) {
                    MainActivity.log("drag wrong " + item.word);
                    Sfx.oops();
                    Ui.shake(hit.card);
                }
                Spring.move(v, 0f, 0f, 320f, 0.6f);
                Spring.scale(v, 1f, 420f, 0.6f);
            }
        }
    }

    @Override
    int[] palette() {
        return s.palette();
    }
}
