package com.belajarceria.kids;

import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** One full screen of the app. {@link MainActivity} keeps them in a stack and animates between them. */
abstract class Screen extends FrameLayout {
    final MainActivity act;

    Screen(MainActivity a) {
        super(a);
        act = a;
        Ui.noClip(this);
    }

    /** The background colours while this screen is in front. */
    abstract int[] palette();

    /**
     * Games and the music corner say true: their back button must be held for two seconds (a child lock, so a
     * stray tap never ends a game), and the phone's back gesture is ignored there. Menus go back with a tap.
     */
    boolean holdToGoBack() {
        return false;
    }

    void onShown() {}

    void onHidden() {}

    /** The theme's mascot in this screen's top bar, if the theme has one. */
    private View mascot;

    /** The mascot jumps for joy: called on every win. */
    void cheer() {
        if (mascot == null) return;
        Ui.jelly(mascot, 4f);
        Spring.of(mascot, ROTATION).config(260f, 0.25f).kick(500f).to(0f);
    }

    /** The top bar for a subject's screen: back button, title pill, and the theme's mascot on the right. */
    LinearLayout header(String title, Content.Subject s) {
        LinearLayout bar = header(title, s.colors[0]);
        HeroArt.Hero hero = Theme.current.hero(s);
        int baby = Theme.current.baby(s);
        String m = Theme.current.mascot(s);
        if (baby >= 0) {
            // The subject's baby sits at the right of the bar, rocking; it bounces on every win.
            addSpacer(bar);
            mascot = new BabyView(getContext(), baby);
            bar.addView(mascot, new LinearLayout.LayoutParams(Ui.dp(60), Ui.dp(72)));
        } else if (hero != null) {
            // The subject's hero stands at the right of the bar, waving; it jumps on every win.
            addSpacer(bar);
            mascot = new HeroView(getContext(), hero, HeroArt.STAND);
            bar.addView(mascot, new LinearLayout.LayoutParams(Ui.dp(64), Ui.dp(72)));
        } else if (m != null) {
            addSpacer(bar);
            mascot = Ui.text(getContext(), m, 30, Ui.INK, null);
            mascot.setBackground(Ui.oval(new int[]{0xF2FFFFFF, Content.mix(Ui.WHITE, s.colors[1], 0.25f)}));
            Ui.lift(mascot, 6, s.colors[0]);
            bar.addView(mascot, new LinearLayout.LayoutParams(Ui.dp(56), Ui.dp(56)));
            // A gentle sway while the screen is open.
            android.animation.ObjectAnimator sway = android.animation.ObjectAnimator.ofFloat(mascot, ROTATION, -7f, 7f);
            sway.setDuration(1300);
            sway.setRepeatMode(android.animation.ValueAnimator.REVERSE);
            sway.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            mascot.addOnAttachStateChangeListener(new OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(android.view.View v) {
                    sway.start();
                }

                @Override
                public void onViewDetachedFromWindow(android.view.View v) {
                    sway.cancel();
                }
            });
        }
        return bar;
    }

    /** The top bar: a round back button and a title pill. More can be added to its right. */
    LinearLayout header(String title, int accent) {
        LinearLayout bar = new LinearLayout(getContext());
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(Ui.dp(20), Ui.dp(12), Ui.dp(20), Ui.dp(4));
        Ui.noClip(bar);
        View back = Ui.chevronButton(getContext(), true, accent, act::back);
        back.setContentDescription("nav back");
        // A child lock in games: going back needs the button held for two seconds.
        if (holdToGoBack()) HoldRing.attach(back, 2000, accent, act::back, () -> act.backHint());
        bar.addView(back, new LinearLayout.LayoutParams(Ui.dp(52), Ui.dp(52)));
        TextView t = Ui.text(getContext(), title, 22, Ui.INK, Ui.BLACK);
        t.setBackground(Ui.round(Ui.GLASS, Ui.dp(26)));
        t.setPadding(Ui.dp(24), 0, Ui.dp(24), 0);
        Ui.lift(t, 4, accent);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, Ui.dp(52));
        lp.leftMargin = Ui.dp(12);
        bar.addView(t, lp);
        return bar;
    }

    /** Adds v at the right of a bar: just before the mascot when there is one. */
    void addRight(LinearLayout bar, View v, LinearLayout.LayoutParams lp) {
        if (mascot != null && mascot.getParent() == bar) {
            lp.rightMargin = Ui.dp(12);
            bar.addView(v, bar.indexOfChild(mascot), lp);
        } else {
            addSpacer(bar);
            bar.addView(v, lp);
        }
    }

    /** A flexible gap that pushes what follows to the right end of a bar. */
    static void addSpacer(LinearLayout bar) {
        bar.addView(new android.view.View(bar.getContext()), new LinearLayout.LayoutParams(0, 1, 1f));
    }
}
