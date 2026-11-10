package com.belajarceria.kids;

import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Levels 1 and 2: one word or sentence at a time. The phone says it (normal or slow), then the child says it into
 * the microphone and earns up to three stars. The best stars for each phrase are kept.
 */
final class PhraseScreen extends Screen {
    private final Chinese.Phrase[] items;
    private final String key;
    private final TextView[] lines;
    private final TextView counter, starsView, heardView;
    private final LinearLayout card;
    private final Speech speech;
    private final ZhKit.Mic mic;
    private View hint;
    private int index;

    PhraseScreen(MainActivity a, String title, Chinese.Phrase[] items, String key) {
        super(a);
        this.items = items;
        this.key = key;
        speech = new Speech(a);
        LinearLayout col = new LinearLayout(a);
        col.setOrientation(LinearLayout.VERTICAL);
        Ui.noClip(col);
        LinearLayout bar = header(title, ZhKit.RED);
        addSpacer(bar);
        counter = Ui.text(a, "", 18, Ui.MUTED, Ui.BOLD);
        counter.setBackground(Ui.round(Ui.GLASS, Ui.dp(26)));
        counter.setPadding(Ui.dp(20), 0, Ui.dp(20), 0);
        bar.addView(counter, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, Ui.dp(52)));
        col.addView(bar, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        LinearLayout body = new LinearLayout(a);
        body.setPadding(Ui.dp(20), Ui.dp(8), Ui.dp(20), Ui.dp(16));
        Ui.noClip(body);
        col.addView(body, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));

        // the card: characters, pinyin, meanings; prev / next on its sides
        FrameLayout cardBox = new FrameLayout(a);
        Ui.noClip(cardBox);
        card = new LinearLayout(a);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(Ui.dp(70), Ui.dp(10), Ui.dp(70), Ui.dp(10));
        card.setBackground(Ui.round(Ui.WHITE, Ui.dp(32)));
        Ui.lift(card, 12, ZhKit.RED);
        lines = ZhKit.phraseViews(a, card, 1f, 110);
        Ui.press(card, () -> say(false));
        cardBox.addView(card, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        int nb = Ui.dp(52);
        FrameLayout.LayoutParams pl = new FrameLayout.LayoutParams(nb, nb, Gravity.CENTER_VERTICAL | Gravity.START);
        pl.leftMargin = Ui.dp(10);
        // the arrows sit on the card, so they must be lifted above it: the card's own shadow is higher than a
        // button's, and the higher view is the one drawn on top and the one a finger touches
        View prev = Ui.chevronButton(a, true, ZhKit.RED, () -> go(-1));
        prev.setTranslationZ(Ui.dp(10));
        cardBox.addView(prev, pl);
        FrameLayout.LayoutParams nl = new FrameLayout.LayoutParams(nb, nb, Gravity.CENTER_VERTICAL | Gravity.END);
        nl.rightMargin = Ui.dp(10);
        View next = Ui.chevronButton(a, false, ZhKit.RED, () -> go(1));
        next.setContentDescription("next");
        next.setTranslationZ(Ui.dp(10));
        cardBox.addView(next, nl);
        body.addView(cardBox, new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));

        // the controls: listen, listen slowly, your turn, stars
        LinearLayout tools = new LinearLayout(a);
        tools.setOrientation(LinearLayout.VERTICAL);
        tools.setGravity(Gravity.CENTER);
        Ui.noClip(tools);
        int tw = Ui.dp(190);
        tools.addView(ZhKit.button(a, "🔊  Dengar", new int[]{0xFF3A86FF, 0xFF5E6BFF}, () -> say(false)), tool(tw, 0));
        tools.addView(ZhKit.button(a, "🐢  Perlahan", new int[]{0xFF8338EC, 0xFFB14FD6}, () -> say(true)), tool(tw, 10));
        mic = new ZhKit.Mic(a, speech);
        tools.addView(mic.view, tool(tw, 10));
        starsView = Ui.fit(a, "☆☆☆", 34, 0xFFFFB300, null);
        starsView.setMaxLines(1);
        tools.addView(starsView, tool(tw, 6));
        heardView = Ui.fit(a, "", 14, Ui.MUTED, Ui.BOLD);
        heardView.setMaxLines(2);
        tools.addView(heardView, new LinearLayout.LayoutParams(tw, Ui.dp(38)));
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(tw, LayoutParams.MATCH_PARENT);
        tl.leftMargin = Ui.dp(16);
        body.addView(tools, tl);

        addView(col, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        show();
    }

    private static LinearLayout.LayoutParams tool(int w, int top) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w, Ui.dp(50));
        lp.topMargin = Ui.dp(top);
        return lp;
    }

    private void show() {
        Chinese.Phrase p = items[index];
        ZhKit.show(lines, p);
        counter.setText((index + 1) + " / " + items.length);
        int best = ZhKit.stars(getContext(), key + index);
        starsView.setText(ZhKit.stars(best));
        heardView.setText(best > 0 ? "Terbaik setakat ini" : "Dengar 🔊, kemudian tekan 🎤 dan sebut!");
        mic.onTap(p.hanzi, (stars, heard) -> {
            starsView.setText(ZhKit.stars(stars));
            Ui.jelly(starsView, 3f);
            ZhKit.saveStars(getContext(), key + index, stars);
            if (heard == null) {
                heardView.setText(stars > 0 ? "Bagus! Dengar suara awak 🔁" : "Cuba sebut lebih kuat 🙂");
            } else {
                heardView.setText(heard.isEmpty() ? "Tak dengar. Cuba lagi 🙂" : "Saya dengar: " + heard);
            }
            if (stars >= 2) {
                Sfx.ding();
                act.reward(card, null);
                cheer();
            } else {
                Sfx.oops();
            }
            if (stars == 3) postDelayed(() -> act.celebrate(null), 600);
        });
        MainActivity.log("zh phrase " + index + " " + p.hanzi);
    }

    private void say(boolean slow) {
        boolean ok = act.voice.sayChinese(items[index].hanzi, slow);
        if (!ok && hint == null) {
            hint = ZhKit.noVoiceHint(getContext());
            LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
            lp.setMargins(Ui.dp(40), 0, Ui.dp(240), Ui.dp(20));
            addView(hint, lp);
        }
        Ui.jelly(card, 0.8f);
    }

    private void go(int dir) {
        speech.stop();
        index = (index + dir + items.length) % items.length;
        float off = dir * Ui.dp(80);
        card.setTranslationX(off);
        card.setAlpha(0f);
        show();
        Spring.of(card, View.TRANSLATION_X).config(300f, 0.75f).to(0f);
        Spring.of(card, View.ALPHA).config(300f, 1f).to(1f);
        say(false);
    }

    @Override
    int[] palette() {
        return ZhKit.PALETTE;
    }

    @Override
    void onShown() {
        postDelayed(() -> say(false), 500);
    }

    @Override
    void onHidden() {
        speech.stop();
    }
}
