package com.belajarceria.kids;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Random;

/**
 * Match the word: pictures on the left, words (or letters, numbers, names) on the right. Draw a line from one to its
 * partner with a finger, or tap one and then the other. A right pair stays joined by a colourful curve.
 */
final class MatchScreen extends Screen {
    private static final int[] LINE_COLORS = {0xFFFF4F79, 0xFF3A86FF, 0xFF0FB57F, 0xFFFF9F1C, 0xFF8338EC};

    private static final class Card {
        Content.Item item;
        View view;
        boolean left, done;
    }

    private static final class Link {
        Card a, b;
        int color;
    }

    private final Content.Subject s;
    /** The Chinese words' English meaning, shown on request. */
    private final EnglishHint hint = new EnglishHint();
    private final FrameLayout board;
    private final Lines lines;
    private final Random rnd = new Random();
    private final ArrayList<Card> cards = new ArrayList<>();
    private final ArrayList<Link> links = new ArrayList<>();
    private int pairs;

    MatchScreen(MainActivity a, Content.Subject s) {
        super(a);
        this.s = s;
        LinearLayout col = new LinearLayout(a);
        col.setOrientation(LinearLayout.VERTICAL);
        Ui.noClip(col);
        LinearLayout bar = header(s.title + "  ·  Match", s);
        if (s.kind == Content.Kind.CHINESE) hint.addButton(this, bar);
        col.addView(bar,
                new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        FrameLayout area = new FrameLayout(a) {
            @Override
            protected void onSizeChanged(int w, int h, int ow, int oh) {
                super.onSizeChanged(w, h, ow, oh);
                if (w > 0 && h > 0) post(MatchScreen.this::newRound);
            }
        };
        Ui.noClip(area);
        board = new FrameLayout(a);
        Ui.noClip(board);
        area.addView(board, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        lines = new Lines(a);
        lines.setTranslationZ(Ui.dp(20)); // above the cards' shadows
        area.addView(lines, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        col.addView(area, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        addView(col, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    private void newRound() {
        int w = board.getWidth(), h = board.getHeight();
        if (w == 0 || h == 0) return;
        board.removeAllViews();
        hint.clear();
        cards.clear();
        links.clear();
        lines.reset();

        int gap = Ui.dp(14);
        int n = h >= Ui.dp(330) ? 4 : 3;
        Content.Item[] items = Content.pick(s.items, n, rnd);
        n = items.length;
        pairs = n;
        Content.Item[] right = Content.shuffled(items, rnd);
        int ch = Math.min((h - gap * (n + 1)) / n, Ui.dp(130));
        int cw = (int) Math.min(w * 0.3f, Math.max(ch * 1.7f, Ui.dp(140)));
        int top = (h - (n * ch + (n - 1) * gap)) / 2;
        int leftX = (int) (w * 0.1f), rightX = (int) (w * 0.9f) - cw;

        ArrayList<View> views = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            views.add(add(items[i], true, leftX, top + i * (ch + gap), cw, ch));
            views.add(add(right[i], false, rightX, top + i * (ch + gap), cw, ch));
        }
        Ui.enter(views, 60);
        lines.animateFor(1400);
        act.logViewsSoon();
    }

    private View add(Content.Item it, boolean left, int x, int y, int w, int h) {
        Context c = getContext();
        FrameLayout v = new FrameLayout(c);
        v.setBackground(Ui.round(Ui.GLASS, Ui.dp(24)));
        Ui.lift(v, 6, s.colors[0]);
        v.setContentDescription((left ? "left:" : "right:") + it.word);
        View content = left ? leftContent(it) : rightContent(it);
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        cp.setMargins(Ui.dp(10), Ui.dp(6), Ui.dp(10), Ui.dp(6));
        v.addView(content, cp);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(w, h);
        lp.leftMargin = x;
        lp.topMargin = y;
        board.addView(v, lp);
        Card card = new Card();
        card.item = it;
        card.view = v;
        card.left = left;
        cards.add(card);
        return v;
    }

    /** Left column: the picture (or for Hijaiyah, the letter itself). */
    private View leftContent(Content.Item it) {
        Context c = getContext();
        if (s.kind == Content.Kind.HIJAIYAH) {
            TextView t = Ui.fit(c, it.big, 90, Ui.INK, Typeface.DEFAULT_BOLD);
            Ui.gradientText(t, s.colors);
            return t;
        }
        TextView t = Ui.fit(c, "", 90, Ui.INK, null);
        Ui.picture(t, it.pic);
        return t;
    }

    /** Right column: the letter, the number, the letter's name, or the Arabic word with its reading. */
    private View rightContent(Content.Item it) {
        Context c = getContext();
        switch (s.kind) {
            case LETTERS:
            case NUMBERS: {
                TextView t = Ui.fit(c, it.big, 90, Ui.INK, Ui.BLACK);
                t.setMaxLines(1);
                Ui.gradientText(t, s.colors);
                return t;
            }
            case HIJAIYAH: {
                TextView t = Ui.fit(c, it.word, 40, Ui.INK, Ui.BLACK);
                t.setMaxLines(1);
                return t;
            }
            default: {
                LinearLayout v = new LinearLayout(c);
                v.setOrientation(LinearLayout.VERTICAL);
                TextView big = Ui.fit(c, it.big, 44, Ui.INK, Typeface.DEFAULT_BOLD);
                big.setMaxLines(1);
                Ui.gradientText(big, s.colors);
                v.addView(big, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
                TextView small = Ui.fit(c, it.word, 15, Ui.MUTED, Ui.BOLD);
                small.setMaxLines(1);
                hint.track(small, it);
                v.addView(small, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(20)));
                return v;
            }
        }
    }

    private void tryPair(Card a, Card b) {
        Spring.scale(a.view, 1f, 420f, 0.5f);
        if (a.item == b.item) {
            a.done = b.done = true;
            Link l = new Link();
            l.a = a.left ? a : b;
            l.b = a.left ? b : a;
            l.color = LINE_COLORS[links.size() % LINE_COLORS.length];
            links.add(l);
            MainActivity.log("match right " + a.item.word);
            for (Card c : new Card[]{a, b}) {
                GradientDrawable g = Ui.round(Content.mix(Ui.WHITE, l.color, 0.16f), Ui.dp(24));
                g.setStroke(Ui.dp(3), l.color);
                c.view.setBackground(g);
                Ui.jelly(c.view, 1.4f);
            }
            Sfx.ding();
            act.reward(b.view, s);
            lines.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            act.voice.item(s, a.item);
            lines.animateFor(800);
            if (links.size() == pairs) postDelayed(() -> act.celebrate(this::newRound), 1000);
        } else {
            MainActivity.log("match wrong " + a.item.word + " / " + b.item.word);
            Sfx.oops();
            Ui.shake(b.view);
            lines.animateFor(800);
        }
    }

    private Card hit(float x, float y) {
        int slack = Ui.dp(10);
        for (Card c : cards) {
            View v = c.view;
            float l = v.getLeft() + v.getTranslationX(), t = v.getTop() + v.getTranslationY();
            if (x > l - slack && x < l + v.getWidth() + slack && y > t - slack && y < t + v.getHeight() + slack) return c;
        }
        return null;
    }

    /** Where a card's line starts: the middle of its inner edge. */
    private static float anchorX(Card c) {
        View v = c.view;
        return v.getLeft() + v.getTranslationX() + (c.left ? v.getWidth() : 0);
    }

    private static float anchorY(Card c) {
        View v = c.view;
        return v.getTop() + v.getTranslationY() + v.getHeight() / 2f;
    }

    /** The layer above the cards: the joining curves, the little handles, and the finger's line. */
    private final class Lines extends View {
        private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private Card from, selected;
        private float fx, fy;
        private boolean dragging;
        private ValueAnimator retract;
        private long animateUntil;

        Lines(Context c) {
            super(c);
            line.setStyle(Paint.Style.STROKE);
            line.setStrokeCap(Paint.Cap.ROUND);
            line.setStrokeWidth(Ui.dp(7));
        }

        void reset() {
            from = selected = null;
            dragging = false;
            if (retract != null) retract.cancel();
            invalidate();
        }

        /** Keeps redrawing while cards move, so lines and handles follow them. */
        void animateFor(long ms) {
            animateUntil = SystemClock.uptimeMillis() + ms;
            postInvalidateOnAnimation();
        }

        @Override
        protected void onDraw(Canvas c) {
            for (Link l : links) {
                curve(c, anchorX(l.a), anchorY(l.a), anchorX(l.b), anchorY(l.b), 1f, l.color);
            }
            for (Card card : cards) {
                if (card.done) continue;
                float x = anchorX(card), y = anchorY(card);
                dot.setColor(Ui.WHITE);
                c.drawCircle(x, y, Ui.dp(11), dot);
                dot.setColor(s.colors[0]);
                c.drawCircle(x, y, Ui.dp(6), dot);
            }
            if (from != null && (dragging || retract != null)) {
                curve(c, anchorX(from), anchorY(from), fx, fy, from.left ? 1f : -1f, s.colors[0]);
                dot.setColor(s.colors[0]);
                c.drawCircle(fx, fy, Ui.dp(10), dot);
            }
            if (SystemClock.uptimeMillis() < animateUntil) postInvalidateOnAnimation();
        }

        private void curve(Canvas c, float x1, float y1, float x2, float y2, float dir, int color) {
            float d = Math.abs(x2 - x1) * 0.4f;
            path.reset();
            path.moveTo(x1, y1);
            path.cubicTo(x1 + dir * d, y1, x2 - dir * d, y2, x2, y2);
            line.setColor(color);
            c.drawPath(path, line);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent e) {
            float x = e.getX(), y = e.getY();
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: {
                    Card c = hit(x, y);
                    if (c == null || c.done) return false;
                    if (retract != null) retract.cancel();
                    if (selected != null && selected.left != c.left) {
                        // second tap of tap-tap matching
                        Card first = selected;
                        selected = null;
                        from = null;
                        tryPair(first, c);
                        return false;
                    }
                    if (selected != null) Spring.scale(selected.view, 1f, 420f, 0.5f);
                    selected = null;
                    from = c;
                    fx = x;
                    fy = y;
                    dragging = true;
                    Spring.scale(c.view, 1.06f, 500f, 0.6f);
                    Sfx.tick();
                    invalidate();
                    return true;
                }
                case MotionEvent.ACTION_MOVE:
                    if (!dragging) return false;
                    fx = x;
                    fy = y;
                    invalidate();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    if (!dragging) return false;
                    dragging = false;
                    Card to = e.getActionMasked() == MotionEvent.ACTION_UP ? hit(x, y) : null;
                    if (to == from) {
                        // a tap: keep it chosen until the partner is tapped
                        selected = from;
                        from = null;
                        act.voice.item(s, selected.item);
                    } else if (to != null && !to.done && to.left != from.left) {
                        Card a = from;
                        from = null;
                        tryPair(a, to);
                    } else {
                        Spring.scale(from.view, 1f, 420f, 0.5f);
                        if (to != null && to != from) {
                            Sfx.oops();
                            Ui.shake(to.view);
                        }
                        pullBack();
                    }
                    invalidate();
                    return true;
                }
                default:
                    return true;
            }
        }

        /** The line snaps back into its card like a rubber band. */
        private void pullBack() {
            final Card c = from;
            final float sx = fx, sy = fy;
            retract = ValueAnimator.ofFloat(0f, 1f);
            retract.setDuration(220);
            retract.addUpdateListener(a -> {
                float t = (float) a.getAnimatedValue();
                fx = sx + (anchorX(c) - sx) * t;
                fy = sy + (anchorY(c) - sy) * t;
                invalidate();
            });
            retract.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    retract = null;
                    if (from == c) from = null;
                    invalidate();
                }
            });
            retract.start();
        }
    }

    @Override
    boolean holdToGoBack() {
        return true;
    }

    @Override
    int[] palette() {
        return s.palette();
    }
}
