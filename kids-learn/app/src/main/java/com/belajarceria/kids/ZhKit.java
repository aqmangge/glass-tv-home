package com.belajarceria.kids;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Shared pieces of Cakap Mandarin: colours, the phrase card, the microphone button, stars and saved progress. */
final class ZhKit {
    static final int[] RED_GOLD = {0xFFE53935, 0xFFFFB300};
    static final int RED = 0xFFD32F2F;
    static final int[] PALETTE = {0xFFFFF4EE, 0xFFFFF8E6, 0xFFFF8A80, 0xFFFFD180, 0xFFFFE082, 0xFFFFAB91};

    private ZhKit() {}

    /** The four lines of a phrase: characters, pinyin, Malay, English. Returned views: [hanzi, pinyin, malay, english]. */
    static TextView[] phraseViews(Context c, LinearLayout into, float hanziWeight, int hanziSp) {
        TextView hanzi = Ui.fit(c, "", hanziSp, RED, Typeface.DEFAULT_BOLD);
        hanzi.setMaxLines(2);
        Ui.gradientText(hanzi, RED_GOLD);
        into.addView(hanzi, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, hanziWeight));
        TextView pinyin = Ui.fit(c, "", 28, 0xFF6D4C41, Ui.BOLD);
        pinyin.setMaxLines(1);
        into.addView(pinyin, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(40)));
        TextView malay = Ui.fit(c, "", 20, Ui.INK, Ui.BLACK);
        malay.setMaxLines(1);
        into.addView(malay, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(30)));
        TextView english = Ui.fit(c, "", 15, Ui.MUTED, Ui.BOLD);
        english.setMaxLines(1);
        into.addView(english, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(22)));
        return new TextView[]{hanzi, pinyin, malay, english};
    }

    static void show(TextView[] v, Chinese.Phrase p) {
        v[0].setText(p.hanzi);
        v[1].setText(p.pinyin);
        v[2].setText(p.malay);
        v[3].setText(p.english);
    }

    /** A pill button with an emoji and a word. */
    static TextView button(Context c, String text, int[] colors, Runnable action) {
        TextView t = Ui.fit(c, text, 18, Ui.WHITE, Ui.BLACK);
        t.setMaxLines(1);
        t.setPadding(Ui.dp(14), 0, Ui.dp(14), 0);
        t.setBackground(Ui.gradient(colors, Ui.dp(26)));
        t.setForeground(Ui.shine(Ui.dp(26)));
        Ui.lift(t, 6, colors[0]);
        t.setContentDescription(text);
        Ui.press(t, action);
        return t;
    }

    static String stars(int n) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 3; i++) b.append(i < n ? "★" : "☆");
        return b.toString();
    }

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("ceria", Context.MODE_PRIVATE);
    }

    /** Keeps the best stars ever earned for one item. */
    static void saveStars(Context c, String key, int stars) {
        SharedPreferences p = prefs(c);
        if (stars > p.getInt(key, 0)) p.edit().putInt(key, stars).apply();
    }

    static int stars(Context c, String key) {
        return prefs(c).getInt(key, 0);
    }

    /** A note that this phone has no Chinese voice yet, and where to add one. */
    static View noVoiceHint(Context c) {
        TextView t = Ui.text(c, "🔇 Telefon ini belum ada suara Bahasa Cina. Tetapan → Sistem → Bahasa → "
                + "Teks-ke-pertuturan → pasang 中文 (Chinese).", 14, 0xFF8D3B00, Ui.BOLD);
        t.setGravity(Gravity.CENTER);
        t.setBackground(Ui.round(0xF2FFF3E0, Ui.dp(14)));
        t.setPadding(Ui.dp(14), Ui.dp(8), Ui.dp(14), Ui.dp(8));
        return t;
    }

    /** The microphone button: listens (or records), shows a live level, then shows the stars earned. */
    /**
     * The microphone button. Tap: it turns red, beeps and says "Cakap sekarang!" with a live sound meter, so the
     * child knows it is listening; tap again (or just stop talking) to finish. It always comes back to green.
     */
    static final class Mic {
        static final String IDLE = "🎤  Tekan & cakap";
        final TextView view;
        private final MainActivity act;
        private final Speech speech;
        private boolean busy;
        private Done done;
        private String target;

        interface Done {
            void done(int stars, String heard);
        }

        Mic(MainActivity a, Speech speech) {
            act = a;
            this.speech = speech;
            view = button(a, IDLE, new int[]{0xFF43A047, 0xFF00BFA5}, this::tapped);
            view.setContentDescription("mic");
        }

        /** What to listen for next, and what to do with the stars. */
        void onTap(String target, Done done) {
            this.target = target;
            this.done = done;
        }

        private void tapped() {
            if (busy) {
                view.setText("⏳  Sekejap…");
                speech.finishEarly(); // a second tap: "I've finished"
                return;
            }
            if (target == null) return;
            act.voice.stop();
            act.withMic(this::begin);
        }

        private void begin() {
            if (busy) return;
            busy = true;
            Sfx.tick();
            view.setText("🔴  Cakap sekarang!");
            view.setBackground(Ui.gradient(new int[]{0xFFE53935, 0xFFFF7043}, Ui.dp(26)));
            Ui.jelly(view, 1.2f);
            pulse();
            MainActivity.log("zh mic listening");
            Done d = done;
            speech.listen(target, v -> {
                if (!busy) return;
                int bars = Math.round(v * 4);
                StringBuilder b = new StringBuilder("🔴  Cakap sekarang! ");
                for (int i = 0; i < 4; i++) b.append(i < bars ? '▮' : '▯');
                view.setText(b);
            }, (stars, heard) -> {
                busy = false;
                view.removeCallbacks(pulser);
                view.setText(IDLE);
                view.setBackground(Ui.gradient(new int[]{0xFF43A047, 0xFF00BFA5}, Ui.dp(26)));
                view.setAlpha(1f);
                if (stars < 0) {
                    MainActivity.log("zh mic cancelled");
                    return; // stopped by a new card or leaving: nothing to judge
                }
                MainActivity.log("zh mic stars " + stars);
                if (d != null) d.done(stars, heard);
            });
        }

        private final Runnable pulser = new Runnable() {
            @Override
            public void run() {
                if (!busy) return;
                view.setAlpha(view.getAlpha() < 1f ? 1f : 0.82f);
                view.postDelayed(this, 450);
            }
        };

        private void pulse() {
            view.removeCallbacks(pulser);
            view.postDelayed(pulser, 450);
        }
    }
}
