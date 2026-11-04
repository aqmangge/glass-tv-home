package com.belajarceria.kids;

import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Cakap Mandarin: the four steps from single words to a speech in front of an audience, shown as a path of four
 * cards, each with the stars earned so far.
 */
final class ChineseScreen extends Screen {
    private final ArrayList<View> cards = new ArrayList<>();
    private final LinearLayout row;
    private boolean entered;

    ChineseScreen(MainActivity a) {
        super(a);
        LinearLayout col = new LinearLayout(a);
        col.setOrientation(LinearLayout.VERTICAL);
        Ui.noClip(col);
        col.addView(header("华语 · Cakap Mandarin", ZhKit.RED),
                new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        TextView hint = Ui.text(a, "Dari perkataan, ke ayat, ke perbualan, hingga berucap di depan orang ramai!",
                16, Ui.MUTED, Ui.BOLD);
        col.addView(hint, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        HorizontalScrollView sv = new HorizontalScrollView(a);
        sv.setFillViewport(true);
        sv.setHorizontalScrollBarEnabled(false);
        Ui.noClip(sv);
        row = new LinearLayout(a);
        row.setGravity(Gravity.CENTER);
        row.setPadding(Ui.dp(20), Ui.dp(12), Ui.dp(20), Ui.dp(18));
        Ui.noClip(row);
        sv.addView(row, new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL));
        col.addView(sv, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        addView(col, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        build();
    }

    private void build() {
        row.removeAllViews();
        cards.clear();
        int h = (int) Math.min(Math.max(Ui.shortSide(getContext()) * 0.5f, Ui.dp(160)), Ui.dp(280));
        int w = (int) (h * 0.8f);
        int[][] colors = {{0xFFFF7043, 0xFFFFB300}, {0xFFE53935, 0xFFFF7043}, {0xFFD81B60, 0xFFFF5252}, {0xFFB71C1C, 0xFFFFA000}};
        for (int i = 0; i < 4; i++) {
            if (i > 0) {
                TextView arrow = Ui.text(getContext(), "➜", 26, 0x99D32F2F, Ui.BLACK);
                row.addView(arrow, new LinearLayout.LayoutParams(Ui.dp(30), LayoutParams.WRAP_CONTENT));
            }
            View c = card(i, colors[i], w, h);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w, h);
            lp.leftMargin = lp.rightMargin = Ui.dp(6);
            row.addView(c, lp);
            cards.add(c);
        }
    }

    private View card(int level, int[] colors, int w, int h) {
        String[] info = Chinese.LEVELS[level];
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(Ui.dp(10), Ui.dp(10), Ui.dp(10), Ui.dp(12));
        card.setBackground(Ui.gradient(colors, w * 0.16f));
        card.setForeground(Ui.shine(w * 0.16f));
        Ui.lift(card, 10, colors[0]);
        card.setContentDescription("zh level " + level);
        TextView step = Ui.text(getContext(), "Tahap " + info[0], 13, colors[0], Ui.BLACK);
        step.setBackground(Ui.round(0xF2FFFFFF, Ui.dp(12)));
        step.setPadding(Ui.dp(10), Ui.dp(2), Ui.dp(10), Ui.dp(2));
        card.addView(step, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        TextView emoji = Ui.fit(getContext(), info[4], 40, Ui.INK, null);
        card.addView(emoji, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 0.9f));
        TextView hanzi = Ui.fit(getContext(), info[1], 34, Ui.WHITE, Typeface.DEFAULT_BOLD);
        hanzi.setMaxLines(1);
        card.addView(hanzi, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 0.8f));
        TextView malay = Ui.fit(getContext(), info[2], 20, Ui.WHITE, Ui.BLACK);
        malay.setMaxLines(1);
        card.addView(malay, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(28)));
        TextView english = Ui.fit(getContext(), info[3], 13, 0xEEFFFFFF, Ui.BOLD);
        english.setMaxLines(1);
        card.addView(english, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(18)));
        TextView progress = Ui.fit(getContext(), progress(level), 14, 0xFFFFF59D, Ui.BLACK);
        progress.setMaxLines(1);
        card.addView(progress, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(22)));
        Ui.press(card, () -> act.push(level(level)));
        return card;
    }

    /** "★ 14 / 72": stars earned out of all there are at this level. */
    private String progress(int level) {
        int got = 0, max;
        if (level < 2) {
            Chinese.Phrase[] items = level == 0 ? Chinese.WORDS : Chinese.SENTENCES;
            for (int i = 0; i < items.length; i++) got += ZhKit.stars(getContext(), "zh_" + level + "_" + i);
            max = items.length * 3;
        } else {
            int n = level == 2 ? Chinese.TALKS.length : Chinese.SPEECHES.length;
            for (int i = 0; i < n; i++) got += ZhKit.stars(getContext(), "zh_" + level + "_" + i);
            max = n * 3;
        }
        return "★ " + got + " / " + max;
    }

    /** The screen for one level: word or sentence cards, or a choice of conversations or speeches. */
    Screen level(int level) {
        switch (level) {
            case 0: return new PhraseScreen(act, "Tahap 1 · 词语 Perkataan", Chinese.WORDS, "zh_0_");
            case 1: return new PhraseScreen(act, "Tahap 2 · 短句 Ayat Mudah", Chinese.SENTENCES, "zh_1_");
            default: return new ZhPickScreen(act, level);
        }
    }

    @Override
    int[] palette() {
        return ZhKit.PALETTE;
    }

    @Override
    void onShown() {
        if (!entered) {
            entered = true;
            Ui.enter(cards, 80);
        } else {
            build(); // fresh stars after a lesson
        }
        act.voice.sayChinese("我们一起说华语吧！", false);
    }
}
