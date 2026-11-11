package com.belajarceria.kids;

import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Level 3: a conversation, laid out like a chat. A friendly panda asks something (and says it aloud); the child
 * picks the right reply out of two, then says it into the microphone. Step by step the chat grows into a real
 * exchange: greeting, name, age, family, being asked to introduce yourself at an event.
 */
final class ChatScreen extends Screen {
    private final Chinese.Talk talk;
    private final int number;
    private final LinearLayout chat, panel;
    private final ScrollView scroller;
    private final Speech speech;
    private int turn, starsTotal;

    ChatScreen(MainActivity a, int number) {
        super(a);
        this.number = number;
        talk = Chinese.TALKS[number];
        speech = new Speech(a);
        LinearLayout col = new LinearLayout(a);
        col.setOrientation(LinearLayout.VERTICAL);
        Ui.noClip(col);
        col.addView(header(talk.emoji + "  " + talk.title, ZhKit.RED),
                new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        LinearLayout body = new LinearLayout(a);
        body.setPadding(Ui.dp(20), Ui.dp(6), Ui.dp(20), Ui.dp(14));
        Ui.noClip(body);
        col.addView(body, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));

        // the chat, in a soft rounded window
        scroller = new ScrollView(a);
        scroller.setBackground(Ui.round(0xB3FFFFFF, Ui.dp(28)));
        scroller.setPadding(Ui.dp(14), Ui.dp(10), Ui.dp(14), Ui.dp(10));
        scroller.setClipToPadding(false);
        chat = new LinearLayout(a);
        chat.setOrientation(LinearLayout.VERTICAL);
        scroller.addView(chat);
        body.addView(scroller, new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1.25f));

        // the answer panel: the two choices, then the microphone
        panel = new LinearLayout(a);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER);
        Ui.noClip(panel);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
        pp.leftMargin = Ui.dp(16);
        body.addView(panel, pp);
        addView(col, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    /** One chat bubble: the panda's on the left (with its face), the child's on the right. */
    private void bubble(Chinese.Phrase p, boolean friend) {
        LinearLayout line = new LinearLayout(getContext());
        line.setGravity(friend ? Gravity.START | Gravity.BOTTOM : Gravity.END | Gravity.BOTTOM);
        if (friend) {
            TextView face = Ui.text(getContext(), "🐼", 30, Ui.INK, null);
            line.addView(face, new LinearLayout.LayoutParams(Ui.dp(44), Ui.dp(44)));
        }
        LinearLayout b = new LinearLayout(getContext());
        b.setOrientation(LinearLayout.VERTICAL);
        b.setPadding(Ui.dp(16), Ui.dp(8), Ui.dp(16), Ui.dp(10));
        b.setBackground(friend ? Ui.round(0xFFFFFFFF, Ui.dp(20)) : Ui.gradient(ZhKit.RED_GOLD, Ui.dp(20)));
        Ui.lift(b, 3, ZhKit.RED);
        TextView hanzi = Ui.text(getContext(), p.hanzi, 24, friend ? ZhKit.RED : Ui.WHITE, Typeface.DEFAULT_BOLD);
        hanzi.setGravity(Gravity.START);
        b.addView(hanzi);
        TextView pinyin = Ui.text(getContext(), p.pinyin, 14, friend ? 0xFF6D4C41 : 0xF2FFFFFF, Ui.BOLD);
        pinyin.setGravity(Gravity.START);
        b.addView(pinyin);
        TextView meaning = Ui.text(getContext(), p.malay, 13, friend ? Ui.MUTED : 0xE6FFFFFF, Ui.BOLD);
        meaning.setGravity(Gravity.START);
        b.addView(meaning);
        Ui.press(b, () -> act.voice.sayChinese(p.hanzi, false));
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        bl.leftMargin = Ui.dp(6);
        line.addView(b, bl);
        LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        ll.topMargin = Ui.dp(8);
        chat.addView(line, ll);
        b.setScaleX(0.6f);
        b.setScaleY(0.6f);
        b.setAlpha(0f);
        Spring.scale(b, 1f, 380f, 0.6f);
        Spring.of(b, View.ALPHA).config(300f, 1f).to(1f);
        scroller.post(() -> scroller.smoothScrollTo(0, chat.getHeight()));
    }

    /** The panda asks; a moment later the two replies appear. */
    private void friendTurn() {
        if (turn >= talk.turns.length) {
            finish();
            return;
        }
        Chinese.Turn t = talk.turns[turn];
        panel.removeAllViews();
        TextView typing = Ui.text(getContext(), "🐼 sedang menaip…", 15, Ui.MUTED, Ui.BOLD);
        panel.addView(typing);
        postDelayed(() -> {
            bubble(t.friend, true);
            act.voice.sayChinese(t.friend.hanzi, true);
            postDelayed(() -> choices(t), 900);
        }, 700);
    }

    private void choices(Chinese.Turn t) {
        panel.removeAllViews();
        TextView ask = Ui.fit(getContext(), "Apa jawapan awak?", 18, Ui.INK, Ui.BLACK);
        panel.addView(ask, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(32)));
        boolean rightFirst = Math.random() < 0.5;
        for (int k = 0; k < 2; k++) {
            boolean right = (k == 0) == rightFirst;
            Chinese.Phrase p = right ? t.reply : t.wrong;
            LinearLayout opt = new LinearLayout(getContext());
            opt.setOrientation(LinearLayout.VERTICAL);
            opt.setGravity(Gravity.CENTER);
            opt.setPadding(Ui.dp(12), Ui.dp(6), Ui.dp(12), Ui.dp(6));
            opt.setBackground(Ui.round(Ui.GLASS, Ui.dp(22)));
            Ui.lift(opt, 6, ZhKit.RED);
            opt.setContentDescription(right ? "reply:right" : "reply:wrong");
            TextView h = Ui.fit(getContext(), p.hanzi, 26, ZhKit.RED, Typeface.DEFAULT_BOLD);
            h.setMaxLines(1);
            opt.addView(h, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
            TextView py = Ui.fit(getContext(), p.pinyin, 14, 0xFF6D4C41, Ui.BOLD);
            py.setMaxLines(1);
            opt.addView(py, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(20)));
            TextView m = Ui.fit(getContext(), p.malay, 13, Ui.MUTED, Ui.BOLD);
            m.setMaxLines(1);
            opt.addView(m, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(18)));
            Ui.press(opt, () -> {
                if (right) {
                    MainActivity.log("zh chat right " + turn);
                    Sfx.ding();
                    bubble(p, false);
                    sayIt(p);
                } else {
                    MainActivity.log("zh chat wrong " + turn);
                    Sfx.oops();
                    Ui.shake(opt);
                    act.voice.sayChinese("再想想？", false); // "think again?"
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f);
            lp.topMargin = Ui.dp(10);
            panel.addView(opt, lp);
        }
        act.logViewsSoon();
    }

    /** "Now say it": the microphone, or skip ahead. */
    private void sayIt(Chinese.Phrase p) {
        panel.removeAllViews();
        TextView say = Ui.fit(getContext(), "Sekarang sebut: " + p.hanzi, 18, Ui.INK, Ui.BLACK);
        say.setMaxLines(2);
        panel.addView(say, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(52)));
        panel.addView(ZhKit.button(getContext(), "🔊  Dengar dulu", new int[]{0xFF3A86FF, 0xFF5E6BFF},
                () -> act.voice.sayChinese(p.hanzi, true)), row(10));
        ZhKit.Mic mic = new ZhKit.Mic(act, speech);
        mic.onTap(p.hanzi, (stars, heard) -> {
            starsTotal += stars;
            TextView s = Ui.fit(getContext(), ZhKit.stars(stars) + (heard != null && !heard.isEmpty() ? "  " + heard : ""), 22, 0xFFFFB300, Ui.BOLD);
            panel.addView(s, row(8));
            Ui.jelly(s, 3f);
            if (stars >= 2) cheer();
            postDelayed(this::nextTurn, 1400);
        });
        panel.addView(mic.view, row(10));
        TextView skip = ZhKit.button(getContext(), "Seterusnya  ▶", new int[]{0xFF9E9E9E, 0xFFBDBDBD}, this::nextTurn);
        skip.setContentDescription("skip");
        panel.addView(skip, row(10));
        act.logViewsSoon();
    }

    private static LinearLayout.LayoutParams row(int top) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(50));
        lp.topMargin = Ui.dp(top);
        return lp;
    }

    private void nextTurn() {
        speech.stop();
        turn++;
        friendTurn();
    }

    private void finish() {
        panel.removeAllViews();
        int stars = Math.max(1, Math.round(starsTotal / (float) talk.turns.length));
        ZhKit.saveStars(getContext(), "zh_2_" + number, stars);
        TextView done = Ui.fit(getContext(), "🎉 Hebat! Awak berbual dalam Bahasa Cina!", 22, Ui.INK, Ui.BLACK);
        done.setMaxLines(3);
        panel.addView(done, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(100)));
        panel.addView(ZhKit.button(getContext(), "🔁  Sekali lagi", ZhKit.RED_GOLD, () -> {
            chat.removeAllViews();
            turn = 0;
            starsTotal = 0;
            friendTurn();
        }), row(10));
        MainActivity.log("zh chat done " + number);
        act.celebrate(null);
        act.logViewsSoon();
    }

    @Override
    boolean holdToGoBack() {
        return true;
    }

    @Override
    int[] palette() {
        return ZhKit.PALETTE;
    }

    @Override
    void onShown() {
        if (turn == 0 && chat.getChildCount() == 0) friendTurn();
    }

    @Override
    void onHidden() {
        speech.stop();
    }
}
