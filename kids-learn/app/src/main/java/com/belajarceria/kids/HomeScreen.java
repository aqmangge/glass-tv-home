package com.belajarceria.kids;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

/** The first screen: the app's name and one big glossy card per subject. */
final class HomeScreen extends Screen {
    private final ArrayList<View> cards = new ArrayList<>();
    private final ArrayList<View> icons = new ArrayList<>();
    private final ArrayList<Animator> bobs = new ArrayList<>();
    private boolean entered;

    HomeScreen(MainActivity a) {
        super(a);
        int cardH = (int) Math.min(Math.max(Ui.shortSide(a) * 0.5f, Ui.dp(170)), Ui.dp(300));
        // All five cards fit across the screen when it is wide enough, so nothing hides off the edge.
        int screenW = getResources().getDisplayMetrics().widthPixels;
        int fitW = (screenW - Ui.dp(48)) / (Content.SUBJECTS.length + 2) - Ui.dp(20);
        int cardW = (int) Math.max(Ui.dp(120), Math.min(cardH * 0.8f, fitW));
        cardH = (int) Math.min(cardH, cardW * 1.3f);

        LinearLayout col = new LinearLayout(a);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        Ui.noClip(col);

        // The publisher's mark, small and spaced out above the app's name.
        TextView brand = Ui.text(a, "KHAIZURAN ACADEMY™", 13, 0xFF5B4BB7, Ui.BOLD);
        brand.setLetterSpacing(0.2f);
        brand.setBackground(Ui.round(Ui.GLASS, Ui.dp(16)));
        brand.setPadding(Ui.dp(16), Ui.dp(6), Ui.dp(16), Ui.dp(6));
        Ui.lift(brand, 3, 0xFF8A5CFF);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        bp.bottomMargin = Ui.dp(2);
        col.addView(brand, bp);

        TextView title = Ui.text(a, "Belajar Ceria", 40, Ui.INK, Ui.BLACK);
        Ui.gradientText(title, Theme.current.title);
        col.addView(title, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        TextView sub = Ui.text(a, "Let's learn and play!  " + Theme.current.icon, 18, Ui.MUTED, Ui.BOLD);
        col.addView(sub, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        HorizontalScrollView sv = new HorizontalScrollView(a);
        sv.setFillViewport(true);
        sv.setHorizontalScrollBarEnabled(false);
        Ui.noClip(sv);
        LinearLayout row = new LinearLayout(a);
        row.setGravity(Gravity.CENTER);
        row.setPadding(Ui.dp(24), Ui.dp(22), Ui.dp(24), Ui.dp(30));
        Ui.noClip(row);
        for (Content.Subject s : Content.SUBJECTS) {
            View c = card(s, cardW, cardH);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(cardW, cardH);
            lp.leftMargin = lp.rightMargin = Ui.dp(10);
            row.addView(c, lp);
            cards.add(c);
        }
        View zh = card(new Content.Subject("华语", "Cakap Mandarin", "中文", ZhKit.RED_GOLD, Content.Kind.WORDS,
                new Content.Item[0]), cardW, cardH);
        zh.setContentDescription("Mandarin");
        Ui.press(zh, () -> act.push(new ChineseScreen(act)));
        LinearLayout.LayoutParams zp = new LinearLayout.LayoutParams(cardW, cardH);
        zp.leftMargin = zp.rightMargin = Ui.dp(10);
        row.addView(zh, zp);
        cards.add(zh);
        View exam = examCard(cardW, cardH);
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(cardW, cardH);
        ep.leftMargin = ep.rightMargin = Ui.dp(10);
        row.addView(exam, ep);
        cards.add(exam);
        sv.addView(row, new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        col.addView(sv, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        addView(col, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        addView(themePicker(), new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.END));
        addView(musicButton(), new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.START));
    }

    /** Top left: the music corner, a colourful round button that dances a little to invite a tap. */
    private View musicButton() {
        LinearLayout box = new LinearLayout(getContext());
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setPadding(Ui.dp(20), Ui.dp(14), Ui.dp(12), Ui.dp(8));
        Ui.noClip(box);
        TextView b = Ui.text(getContext(), "🎵", 26, Ui.WHITE, null);
        b.setBackground(Ui.oval(new int[]{0xFFFF6FB5, 0xFF8338EC}));
        b.setForeground(Ui.shine(Ui.dp(30)));
        Ui.lift(b, 8, 0xFF8338EC);
        b.setContentDescription("music");
        Ui.press(b, () -> act.push(new MusicScreen(act, 0)));
        box.addView(b, new LinearLayout.LayoutParams(Ui.dp(56), Ui.dp(56)));
        TextView t = Ui.text(getContext(), "Muzik", 15, 0xFF8338EC, Ui.BLACK);
        t.setBackground(Ui.round(Ui.GLASS, Ui.dp(14)));
        t.setPadding(Ui.dp(12), Ui.dp(4), Ui.dp(12), Ui.dp(4));
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        tp.leftMargin = Ui.dp(8);
        box.addView(t, tp);
        android.animation.ObjectAnimator wiggle = android.animation.ObjectAnimator.ofFloat(b, ROTATION, -10f, 10f);
        wiggle.setDuration(700);
        wiggle.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        wiggle.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        wiggle.start();
        return box;
    }

    /** Three round buttons, one per theme; the chosen one is bigger and ringed in its colours. */
    private View themePicker() {
        LinearLayout row = new LinearLayout(getContext());
        row.setPadding(Ui.dp(12), Ui.dp(14), Ui.dp(20), Ui.dp(8));
        Ui.noClip(row);
        for (Theme t : Theme.ALL) {
            boolean on = t == Theme.current;
            TextView b = Ui.text(getContext(), t.icon, 24, Ui.INK, null);
            android.graphics.drawable.GradientDrawable bg = Ui.oval(new int[]{0xF7FFFFFF, 0xF7FFFFFF});
            if (on) bg.setStroke(Ui.dp(3), t.title[0]);
            b.setBackground(bg);
            Ui.lift(b, on ? 8 : 3, t.title[0]);
            b.setContentDescription("theme:" + t.id);
            b.setScaleX(on ? 1.12f : 0.92f);
            b.setScaleY(on ? 1.12f : 0.92f);
            Ui.press(b, () -> act.switchTheme(t));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(48), Ui.dp(48));
            lp.leftMargin = Ui.dp(8);
            row.addView(b, lp);
        }
        return row;
    }

    private View card(Content.Subject s, int w, int h) {
        FrameLayout card = new FrameLayout(getContext());
        float r = w * 0.18f;
        card.setBackground(Ui.gradient(s.colors, r));
        card.setForeground(Ui.shine(r));
        Ui.lift(card, 12, s.colors[0]);
        card.setContentDescription(s.title);

        TextView icon = Ui.fit(getContext(), s.icon, 64, Ui.WHITE, Ui.BLACK);
        icon.setMaxLines(1);
        icon.setShadowLayer(Ui.dp(8), 0, Ui.dp(3), 0x33000000);
        FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, (int) (h * 0.46f), Gravity.TOP);
        ip.topMargin = (int) (h * 0.1f);
        ip.leftMargin = ip.rightMargin = Ui.dp(14);
        card.addView(icon, ip);
        icons.add(icon);

        LinearLayout labels = new LinearLayout(getContext());
        labels.setOrientation(LinearLayout.VERTICAL);
        TextView t = Ui.fit(getContext(), s.title, 22, Ui.WHITE, Ui.BLACK);
        t.setMaxLines(1);
        labels.addView(t, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(30)));
        TextView st = Ui.fit(getContext(), s.subtitle, 14, 0xEEFFFFFF, Ui.BOLD);
        st.setMaxLines(1);
        labels.addView(st, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(20)));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        lp.bottomMargin = (int) (h * 0.09f);
        lp.leftMargin = lp.rightMargin = Ui.dp(10);
        card.addView(labels, lp);

        // In a themed look, the subject's mascot peeks over the card's corner.
        HeroArt.Hero hero = Theme.current.hero(s);
        int baby = Theme.current.baby(s);
        String m = Theme.current.mascot(s);
        if (baby >= 0) {
            View peek = new BabyView(getContext(), baby);
            int size = Ui.dp(68);
            FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(size, size, Gravity.TOP | Gravity.END);
            pp.topMargin = -size / 2;
            pp.rightMargin = -size / 5;
            card.addView(peek, pp);
            Ui.noClip(card);
        } else if (hero != null) {
            View peek = new HeroView(getContext(), hero, HeroArt.STAND);
            int size = Ui.dp(70);
            FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(size, size, Gravity.TOP | Gravity.END);
            pp.topMargin = -size / 2;
            pp.rightMargin = -size / 4;
            card.addView(peek, pp);
            Ui.noClip(card);
        } else if (m != null) {
            TextView peek = Ui.text(getContext(), m, 26, Ui.INK, null);
            peek.setBackground(Ui.oval(new int[]{0xFFFFFFFF, 0xFFFFFFFF}));
            Ui.lift(peek, 6, s.colors[0]);
            peek.setRotation(12f);
            int size = Ui.dp(50);
            FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(size, size, Gravity.TOP | Gravity.END);
            pp.topMargin = -size / 3;
            pp.rightMargin = -size / 4;
            card.addView(peek, pp);
            Ui.noClip(card);
        }

        Ui.press(card, () -> act.push(new SubjectScreen(act, s)));
        return card;
    }

    /** The test: a card like the subjects', in the theme's title colours, with a trophy. */
    private View examCard(int w, int h) {
        Content.Subject look = new Content.Subject("Ujian", "Tahun 1 · Markah", "📝",
                new int[]{Theme.current.title[0], Theme.current.title[2]}, Content.Kind.WORDS, new Content.Item[0]);
        View card = card(look, w, h);
        card.setContentDescription("Ujian");
        Ui.press(card, () -> act.push(new ExamScreen(act)));
        return card;
    }

    @Override
    int[] palette() {
        return Theme.current.home;
    }

    @Override
    void onShown() {
        if (!entered) {
            entered = true;
            Ui.enter(cards, 150);
        }
        // The pictures on the cards bob gently, each at its own pace, so the screen feels alive.
        for (int i = 0; i < icons.size(); i++) {
            ObjectAnimator a = ObjectAnimator.ofFloat(icons.get(i), View.TRANSLATION_Y, -Ui.dp(5), Ui.dp(5));
            a.setDuration(1300 + i * 170L);
            a.setRepeatMode(ValueAnimator.REVERSE);
            a.setRepeatCount(ValueAnimator.INFINITE);
            a.setInterpolator(new AccelerateDecelerateInterpolator());
            a.start();
            bobs.add(a);
        }
    }

    @Override
    void onHidden() {
        for (Animator a : bobs) a.cancel();
        bobs.clear();
    }
}
