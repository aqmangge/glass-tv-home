package com.belajarceria.kids;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

/** Draw: trace the letter, number or word with a finger, in a crayon colour the child picks. */
final class TraceScreen extends Screen {
    private static final int[] CRAYONS = {0xFFFF4F79, 0xFFFF9F1C, 0xFFFFC300, 0xFF2EC4B6, 0xFF3A86FF, 0xFF8338EC};

    private final Content.Subject s;
    private final TraceView trace;
    private final TextView label;
    private final View clear;
    private final ArrayList<View> dots = new ArrayList<>();
    private int index;

    TraceScreen(MainActivity a, Content.Subject s) {
        super(a);
        this.s = s;
        LinearLayout col = new LinearLayout(a);
        col.setOrientation(LinearLayout.VERTICAL);
        Ui.noClip(col);
        col.addView(header(s.title + "  ·  Draw", s),
                new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        LinearLayout body = new LinearLayout(a);
        body.setPadding(Ui.dp(20), Ui.dp(10), Ui.dp(20), Ui.dp(18));
        Ui.noClip(body);

        FrameLayout board = new FrameLayout(a);
        board.setBackground(Ui.round(Ui.WHITE, Ui.dp(36)));
        Ui.lift(board, 12, s.colors[0]);
        trace = new TraceView(a);
        trace.setContentDescription("board");
        // The right strip of the board holds the crayon box, so the letter is centred in the rest.
        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        tp.rightMargin = Ui.dp(118);
        board.addView(trace, tp);

        label = Ui.text(a, "", 20, Ui.INK, Ui.BLACK);
        label.setBackground(Ui.round(Content.mix(Ui.WHITE, s.colors[0], 0.12f), Ui.dp(22)));
        label.setPadding(Ui.dp(18), 0, Ui.dp(18), 0);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, Ui.dp(44), Gravity.TOP | Gravity.START);
        lp.setMargins(Ui.dp(14), Ui.dp(14), 0, 0);
        board.addView(label, lp);
        Ui.press(label, () -> act.voice.item(s, s.items[index]));

        int nav = Ui.dp(52);
        FrameLayout.LayoutParams pl = new FrameLayout.LayoutParams(nav, nav, Gravity.BOTTOM | Gravity.START);
        pl.setMargins(Ui.dp(14), 0, 0, Ui.dp(14));
        board.addView(Ui.chevronButton(a, true, s.colors[0], () -> go(-1)), pl);
        FrameLayout.LayoutParams nl = new FrameLayout.LayoutParams(nav, nav, Gravity.BOTTOM | Gravity.END);
        nl.setMargins(0, 0, Ui.dp(130), Ui.dp(14));
        board.addView(Ui.chevronButton(a, false, s.colors[0], () -> go(1)), nl);
        body.addView(board, new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));

        // The crayon box: two columns of colour dots, and a sponge to wipe the board.
        LinearLayout tools = new LinearLayout(a);
        tools.setOrientation(LinearLayout.VERTICAL);
        tools.setGravity(Gravity.CENTER);
        Ui.noClip(tools);
        int dot = Ui.dp(36);
        for (int r = 0; r < 3; r++) {
            LinearLayout row = new LinearLayout(a);
            Ui.noClip(row);
            for (int c = 0; c < 2; c++) {
                final int which = r * 2 + c;
                View d = new View(a);
                GradientDrawable g = Ui.oval(new int[]{CRAYONS[which], Content.mix(CRAYONS[which], Ui.WHITE, 0.25f)});
                g.setStroke(Ui.dp(3), Ui.WHITE);
                d.setBackground(g);
                Ui.lift(d, 4, CRAYONS[which]);
                Ui.press(d, () -> pickCrayon(which));
                LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(dot, dot);
                dl.setMargins(Ui.dp(5), Ui.dp(5), Ui.dp(5), Ui.dp(5));
                row.addView(d, dl);
                dots.add(d);
            }
            tools.addView(row);
        }
        clear = Ui.emojiButton(a, "🧽", s.colors[0], trace::clear);
        clear.setContentDescription("Clear");
        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(Ui.dp(56), Ui.dp(56));
        cl.topMargin = Ui.dp(14);
        tools.addView(clear, cl);
        FrameLayout.LayoutParams tl = new FrameLayout.LayoutParams(Ui.dp(110), LayoutParams.MATCH_PARENT, Gravity.END);
        tl.rightMargin = Ui.dp(8);
        board.addView(tools, tl);

        col.addView(body, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        addView(col, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        trace.required = s.kind == Content.Kind.LETTERS || s.kind == Content.Kind.NUMBERS ? 0.9f : 0.7f;
        trace.listener = new TraceView.Listener() {
            @Override
            public void onTraced() {
                Sfx.ding();
                act.reward(trace, s);
                act.voice.item(s, s.items[index]);
                postDelayed(() -> act.celebrate(() -> go(1)), 900);
            }

            @Override
            public void onMessy() {
                Ui.jelly(clear, 3f); // hint: wipe and try again
            }
        };
        pickCrayon(0);
        show();
    }

    private void pickCrayon(int which) {
        trace.setBrush(CRAYONS[which]);
        for (int i = 0; i < dots.size(); i++) Spring.scale(dots.get(i), i == which ? 1.25f : 1f, 500f, 0.5f);
    }

    private void show() {
        Content.Item it = s.items[index];
        String name = s.kind == Content.Kind.WORDS ? it.word : it.big + "  ·  " + it.word;
        label.setText(name);
        Typeface tf = s.arabic() ? Typeface.DEFAULT_BOLD : Ui.BLACK;
        trace.setGlyph(it.big, s.colors, tf);
    }

    private void go(int dir) {
        int n = s.items.length;
        index = (index + dir + n) % n;
        trace.setAlpha(0f);
        trace.setTranslationX(dir * Ui.dp(60));
        show();
        Spring.of(trace, View.ALPHA).config(300f, 1f).to(1f);
        Spring.of(trace, View.TRANSLATION_X).config(300f, 0.75f).to(0f);
        act.voice.item(s, s.items[index]);
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
        postDelayed(() -> act.voice.item(s, s.items[index]), 450);
    }
}
