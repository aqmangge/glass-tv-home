package com.belajarceria.kids;

import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * The music corner, a break from learning: piano, drums, guitar and xylophone to play freely. The tabs at the top
 * switch instrument; the sound engine runs only while this screen is open.
 */
final class MusicScreen extends Screen {
    static final String[][] TABS = {{"🎹", "Piano"}, {"🥁", "Drum"}, {"🎸", "Gitar"}, {"🎶", "Xylophone"}};
    private static final int[][] TAB_COLORS = {{0xFF5E6BFF, 0xFF3FB6FF}, {0xFFFF4F5E, 0xFFFF9F1C},
            {0xFFFF9F1C, 0xFFFFC300}, {0xFF2EC4B6, 0xFF8338EC}};

    private final FrameLayout stage;
    private final ArrayList<TextView> tabs = new ArrayList<>();
    private int current = -1;

    MusicScreen(MainActivity a, int start) {
        super(a);
        LinearLayout col = new LinearLayout(a);
        col.setOrientation(LinearLayout.VERTICAL);
        Ui.noClip(col);
        LinearLayout bar = header("Muzik", 0xFF8338EC);
        addSpacer(bar);
        for (int i = 0; i < TABS.length; i++) {
            final int which = i;
            TextView t = Ui.text(a, TABS[i][0] + "  " + TABS[i][1], 16, Ui.INK, Ui.BLACK);
            t.setPadding(Ui.dp(16), 0, Ui.dp(16), 0);
            t.setContentDescription("instrument:" + i);
            Ui.press(t, () -> show(which));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, Ui.dp(50));
            lp.leftMargin = Ui.dp(8);
            bar.addView(t, lp);
            tabs.add(t);
        }
        col.addView(bar, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        stage = new FrameLayout(a);
        stage.setPadding(Ui.dp(20), Ui.dp(10), Ui.dp(20), Ui.dp(16));
        Ui.noClip(stage);
        col.addView(stage, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        addView(col, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        show(Math.max(0, Math.min(TABS.length - 1, start)));
    }

    void show(int which) {
        if (which == current) return;
        current = which;
        for (int i = 0; i < tabs.size(); i++) {
            TextView t = tabs.get(i);
            boolean on = i == which;
            t.setBackground(on ? Ui.gradient(TAB_COLORS[i], Ui.dp(25)) : Ui.round(Ui.GLASS, Ui.dp(25)));
            t.setTextColor(on ? Ui.WHITE : Ui.INK);
            Ui.lift(t, on ? 8 : 3, TAB_COLORS[i][0]);
            if (on) Ui.jelly(t, 1.4f);
        }
        stage.removeAllViews();
        InstrumentView v;
        switch (which) {
            case 0: v = new PianoView(getContext()); break;
            case 1: v = new DrumView(getContext()); break;
            case 2: v = new GuitarView(getContext()); break;
            default: v = new XyloView(getContext()); break;
        }
        v.prewarm();
        stage.addView(v, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        v.setAlpha(0f);
        v.setScaleX(0.92f);
        v.setScaleY(0.92f);
        Spring.of(v, View.ALPHA).config(300f, 1f).to(1f);
        Spring.scale(v, 1f, 260f, 0.7f);
        MainActivity.log("music show " + TABS[which][1]);
        act.logViewsSoon();
    }

    @Override
    boolean holdToGoBack() {
        return true;
    }

    @Override
    int[] palette() {
        return new int[]{0xFFFFF4FB, 0xFFEFF4FF, 0xFFFF9BD2, 0xFF9CC9FF, 0xFFFFE08A, 0xFFB5E8C8};
    }

    @Override
    void onShown() {
        Synth.get().start();
    }

    @Override
    void onHidden() {
        Synth.get().stop();
    }
}
