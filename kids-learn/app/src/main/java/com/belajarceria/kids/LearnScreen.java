package com.belajarceria.kids;

import android.annotation.SuppressLint;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Flash cards: a big card with the picture, the letter or word and its name, read aloud. Swipe it away like a
 * photo on an iPhone (it tilts and follows the finger), tap it to hear it again.
 */
final class LearnScreen extends Screen {
    private final Content.Subject s;
    private final FrameLayout card;
    private final TextView pic, big, word, sub, counter;
    private int index;
    private VelocityTracker velocity;
    private float downX, startTx;

    LearnScreen(MainActivity a, Content.Subject s) {
        super(a);
        this.s = s;
        LinearLayout bar = header(s.title, s);
        // With a theme mascot the bar already ends in a gap and the mascot; the counter goes next to it.
        boolean hasMascot = bar.getChildCount() > 2;
        if (!hasMascot) addSpacer(bar);
        counter = Ui.text(a, "", 18, Ui.MUTED, Ui.BOLD);
        counter.setBackground(Ui.round(Ui.GLASS, Ui.dp(26)));
        counter.setPadding(Ui.dp(20), 0, Ui.dp(20), 0);
        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, Ui.dp(52));
        if (hasMascot) cl.leftMargin = Ui.dp(10);
        bar.addView(counter, cl);
        addView(bar, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.TOP));

        int ch = (int) (Ui.shortSide(a) * 0.68f);
        int cw = (int) (ch * 1.12f);
        card = new FrameLayout(a);
        card.setBackground(Ui.round(Ui.WHITE, Ui.dp(40)));
        Ui.lift(card, 16, s.colors[0]);

        LinearLayout inner = new LinearLayout(a);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setGravity(Gravity.CENTER_HORIZONTAL);
        inner.setPadding(Ui.dp(18), Ui.dp(14), Ui.dp(18), Ui.dp(14));
        pic = Ui.fit(a, "", 120, Ui.INK, null);
        big = Ui.fit(a, "", 170, Ui.INK, s.arabic() ? android.graphics.Typeface.DEFAULT_BOLD : Ui.BLACK);
        Ui.gradientText(big, s.colors);
        word = Ui.fit(a, "", 34, Ui.INK, Ui.BLACK);
        word.setMaxLines(1);
        sub = Ui.fit(a, "", 18, Ui.MUTED, Ui.BOLD);
        sub.setMaxLines(1);
        float picWeight = s.kind == Content.Kind.HIJAIYAH ? 0f : s.kind == Content.Kind.WORDS ? 1.1f : 1f;
        if (picWeight > 0) inner.addView(pic, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, picWeight));
        inner.addView(big, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        inner.addView(word, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(42)));
        inner.addView(sub, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(24)));
        card.addView(inner, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        LayoutParams cp = new LayoutParams(cw, ch, Gravity.CENTER);
        cp.topMargin = Ui.dp(44);
        addView(card, cp);
        card.setContentDescription("card");
        card.setOnTouchListener(this::onCardTouch);

        int b = Ui.dp(64);
        LayoutParams lp = new LayoutParams(b, b, Gravity.CENTER_VERTICAL | Gravity.START);
        lp.leftMargin = Ui.dp(28);
        lp.topMargin = Ui.dp(44);
        addView(Ui.chevronButton(a, true, s.colors[0], () -> go(-1)), lp);
        LayoutParams rp = new LayoutParams(b, b, Gravity.CENTER_VERTICAL | Gravity.END);
        rp.rightMargin = Ui.dp(28);
        rp.topMargin = Ui.dp(44);
        addView(Ui.chevronButton(a, false, s.colors[0], () -> go(1)), rp);

        show();
    }

    void jumpTo(int i) {
        index = Math.max(0, Math.min(s.items.length - 1, i));
        show();
    }

    private void show() {
        Content.Item it = s.items[index];
        Ui.picture(pic, it.pic);
        // Letters are shown in both cases, "A a", the way they are taught.
        big.setText(s.kind == Content.Kind.LETTERS ? it.big + " " + it.big.toLowerCase() : it.big);
        word.setText(it.word);
        sub.setText(it.sub == null ? "" : it.sub);
        counter.setText((index + 1) + " / " + s.items.length);
    }

    private void speak() {
        act.voice.item(s, s.items[index]);
    }

    /** Throws the card off one side and springs the next one in from the other. */
    private void go(int dir) {
        Sfx.tick();
        float off = Math.max(getWidth(), Ui.dp(400)) * 0.85f;
        Spring.of(card, View.ROTATION).config(500f, 1f).to(-dir * 14f);
        Spring.of(card, View.TRANSLATION_X).config(700f, 1f).then(() -> {
            int n = s.items.length;
            index = (index + dir + n) % n;
            show();
            MainActivity.log("card " + s.items[index].word);
            card.setTranslationX(dir * off);
            card.setRotation(dir * 14f);
            Spring.of(card, View.TRANSLATION_X).config(240f, 0.74f).to(0f);
            Spring.of(card, View.ROTATION).config(240f, 0.74f).to(0f);
            speak();
        }).to(-dir * off);
    }

    @SuppressLint("ClickableViewAccessibility")
    private boolean onCardTouch(View v, MotionEvent e) {
        MotionEvent raw = MotionEvent.obtain(e);
        raw.setLocation(e.getRawX(), e.getRawY());
        try {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    Spring.stop(card);
                    downX = e.getRawX();
                    startTx = card.getTranslationX();
                    if (velocity != null) velocity.recycle();
                    velocity = VelocityTracker.obtain();
                    velocity.addMovement(raw);
                    Spring.scale(card, 0.97f, 600f, 0.7f);
                    return true;
                case MotionEvent.ACTION_MOVE: {
                    if (velocity != null) velocity.addMovement(raw);
                    float dx = startTx + e.getRawX() - downX;
                    card.setTranslationX(dx);
                    card.setRotation(dx / Math.max(1, card.getWidth()) * 12f);
                    return true;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    float vx = 0f;
                    if (velocity != null) {
                        velocity.addMovement(raw);
                        velocity.computeCurrentVelocity(1000);
                        vx = velocity.getXVelocity();
                        velocity.recycle();
                        velocity = null;
                    }
                    Spring.scale(card, 1f, 420f, 0.5f);
                    float tx = card.getTranslationX();
                    boolean fling = Math.abs(vx) > Ui.dp(800);
                    if (Math.abs(tx) > card.getWidth() * 0.22f || fling) {
                        float d = fling ? vx : tx;
                        go(d < 0 ? 1 : -1);
                    } else {
                        Spring.of(card, View.TRANSLATION_X).config(420f, 0.6f).to(0f);
                        Spring.of(card, View.ROTATION).config(420f, 0.6f).to(0f);
                        if (e.getActionMasked() == MotionEvent.ACTION_UP && Math.abs(e.getRawX() - downX) < Ui.dp(12)) {
                            speak();
                            Ui.jelly(card, 1.2f);
                        }
                    }
                    return true;
                }
                default:
                    return true;
            }
        } finally {
            raw.recycle();
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

    @Override
    void onShown() {
        postDelayed(this::speak, 450);
    }
}
