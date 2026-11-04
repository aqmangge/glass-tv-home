package com.belajarceria.kids;

import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

/** Levels 3 and 4: choose a conversation to role-play, or a speech to give on stage. */
final class ZhPickScreen extends Screen {
    private final int level;
    private final ArrayList<View> cards = new ArrayList<>();
    private final LinearLayout row;
    private boolean entered;

    ZhPickScreen(MainActivity a, int level) {
        super(a);
        this.level = level;
        String[] info = Chinese.LEVELS[level];
        LinearLayout col = new LinearLayout(a);
        col.setOrientation(LinearLayout.VERTICAL);
        Ui.noClip(col);
        col.addView(header("Tahap " + info[0] + " · " + info[1] + " " + info[2], ZhKit.RED),
                new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        TextView hint = Ui.text(a, level == 2 ? "Berbual dengan kawan: pilih jawapan, kemudian sebutkannya!"
                : "Naik ke pentas dan berucap kepada penonton!", 16, Ui.MUTED, Ui.BOLD);
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
        int h = (int) Math.min(Math.max(Ui.shortSide(getContext()) * 0.5f, Ui.dp(160)), Ui.dp(270));
        int w = (int) (h * 0.85f);
        int n = level == 2 ? Chinese.TALKS.length : Chinese.SPEECHES.length;
        for (int i = 0; i < n; i++) {
            String title = level == 2 ? Chinese.TALKS[i].title : Chinese.SPEECHES[i].title;
            String emoji = level == 2 ? Chinese.TALKS[i].emoji : Chinese.SPEECHES[i].emoji;
            int lines = level == 2 ? Chinese.TALKS[i].turns.length : Chinese.SPEECHES[i].lines.length;
            View c = card(i, title, emoji, lines, w, h);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w, h);
            lp.leftMargin = lp.rightMargin = Ui.dp(9);
            row.addView(c, lp);
            cards.add(c);
        }
    }

    private View card(int i, String title, String emoji, int lines, int w, int h) {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(Ui.dp(10), Ui.dp(12), Ui.dp(10), Ui.dp(12));
        card.setBackground(Ui.round(Ui.GLASS, w * 0.14f));
        Ui.lift(card, 10, ZhKit.RED);
        card.setContentDescription("zh pick " + i);
        card.addView(Ui.fit(getContext(), emoji, 50, Ui.INK, null), new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        TextView t = Ui.fit(getContext(), title, 20, Ui.INK, Ui.BLACK);
        t.setMaxLines(2);
        card.addView(t, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(52)));
        TextView sub = Ui.fit(getContext(), (level == 2 ? lines + " soalan" : lines + " ayat"), 14, Ui.MUTED, Ui.BOLD);
        card.addView(sub, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(20)));
        TextView stars = Ui.fit(getContext(), ZhKit.stars(ZhKit.stars(getContext(), "zh_" + level + "_" + i)), 26, 0xFFFFB300, null);
        card.addView(stars, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(34)));
        Ui.press(card, () -> act.push(level == 2 ? new ChatScreen(act, i) : new StageScreen(act, i)));
        return card;
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
            build();
        }
    }
}
