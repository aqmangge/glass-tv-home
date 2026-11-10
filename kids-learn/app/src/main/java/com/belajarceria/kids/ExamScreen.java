package com.belajarceria.kids;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Ujian: the Year 1 diagnostic test as a game. The child picks one part or all four, answers one question at a time
 * by tapping a big button, then sees a mark for each part (/25), the total, the paper's band (Cemerlang, Baik...)
 * and, for the parent, every question that went wrong with its right answer. The last marks are kept.
 */
final class ExamScreen extends Screen {
    private final FrameLayout body;
    private final Random rnd = new Random();
    private List<Exam.Q> questions = new ArrayList<>();
    private int index;
    private boolean busy;
    private int[] taken = {};
    private View resultPage;
    /** The test's voice: "ms" (Melayu), "en" (English) or "off" (Senyap); remembered between tests. */
    private String mode;
    private TextView voiceButton, promptView;
    private Exam.Q current;
    /** The parts this test offers: the Year 1 paper's four, or Mandarin alone. */
    private final int[] parts;

    ExamScreen(MainActivity a) {
        this(a, Exam.YEAR_ONE, "Ujian Diagnostik Tahun 1");
    }

    ExamScreen(MainActivity a, int[] parts, String title) {
        super(a);
        this.parts = parts;
        LinearLayout col = new LinearLayout(a);
        col.setOrientation(LinearLayout.VERTICAL);
        Ui.noClip(col);
        mode = prefs().getString("exam_voice", "ms");
        LinearLayout bar = header(title, Theme.current.title[0]);
        addSpacer(bar);
        voiceButton = Ui.text(a, "", 17, Ui.INK, Ui.BLACK);
        voiceButton.setBackground(Ui.round(Ui.GLASS, Ui.dp(26)));
        voiceButton.setPadding(Ui.dp(20), 0, Ui.dp(20), 0);
        Ui.lift(voiceButton, 4, Theme.current.title[0]);
        voiceButton.setContentDescription("voice");
        Ui.press(voiceButton, this::nextVoice);
        bar.addView(voiceButton, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, Ui.dp(52)));
        showVoice();
        col.addView(bar, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        body = new FrameLayout(a);
        Ui.noClip(body);
        col.addView(body, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        addView(col, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        showIntro();
    }

    @Override
    int[] palette() {
        return Theme.current.home;
    }

    // ---------------------------------------------------------------- intro

    private void showIntro() {
        body.removeAllViews();
        Context c = getContext();
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        Ui.noClip(col);

        TextView hint = Ui.text(c, parts.length > 1 ? "Pilih satu bahagian, atau jawab semua · 25 markah setiap bahagian"
                : "25 soalan · 25 markah · Tekan untuk mula!", 16, Ui.MUTED, Ui.BOLD);
        col.addView(hint, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        int tileH = (int) Math.min(Math.max(Ui.shortSide(c) * 0.4f, Ui.dp(140)), Ui.dp(230));
        int tileW = (int) (tileH * 0.9f);
        HorizontalScrollView sv = new HorizontalScrollView(c);
        sv.setFillViewport(true);
        sv.setHorizontalScrollBarEnabled(false);
        Ui.noClip(sv);
        LinearLayout row = new LinearLayout(c);
        row.setGravity(Gravity.CENTER);
        row.setPadding(Ui.dp(20), Ui.dp(14), Ui.dp(20), Ui.dp(14));
        Ui.noClip(row);
        ArrayList<View> tiles = new ArrayList<>();
        for (int s : parts) {
            View t = sectionTile(s, tileW, tileH);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(tileW, tileH);
            lp.leftMargin = lp.rightMargin = Ui.dp(9);
            row.addView(t, lp);
            tiles.add(t);
        }
        sv.addView(row, new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        col.addView(sv, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        TextView all = Ui.text(c, "▶  Jawab semua  ·  100 markah", 20, Ui.WHITE, Ui.BLACK);
        all.setBackground(Ui.gradient(Theme.current.title, Ui.dp(30)));
        all.setForeground(Ui.shine(Ui.dp(30)));
        all.setPadding(Ui.dp(30), 0, Ui.dp(30), 0);
        Ui.lift(all, 8, Theme.current.title[0]);
        all.setContentDescription("start:all");
        Ui.press(all, () -> start(parts));
        if (parts.length > 1) {
            col.addView(all, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, Ui.dp(60)));
            tiles.add(all);
        }

        body.addView(col, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        Ui.enter(tiles, 60);
        talk("Pilih satu bahagian.", "Choose a part.");
        act.logViewsSoon();
    }

    private View sectionTile(int s, int w, int h) {
        Context c = getContext();
        LinearLayout tile = new LinearLayout(c);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setBackground(Ui.gradient(Exam.COLORS[s], w * 0.16f));
        tile.setForeground(Ui.shine(w * 0.16f));
        Ui.lift(tile, 10, Exam.COLORS[s][0]);
        tile.setContentDescription("start:" + s);
        TextView icon = Ui.fit(c, Exam.ICONS[s], 44, Ui.INK, null);
        tile.addView(icon, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, (int) (h * 0.36f)));
        TextView name = Ui.fit(c, Exam.NAMES[s], 20, Ui.WHITE, Ui.BLACK);
        name.setMaxLines(1);
        name.setPadding(Ui.dp(8), 0, Ui.dp(8), 0);
        tile.addView(name, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(30)));
        int last = prefs().getInt("exam_" + s, -1);
        String sub = last < 0 ? "25 markah" : "Lepas: " + last + "/25";
        TextView st = Ui.fit(c, sub, 14, 0xEEFFFFFF, Ui.BOLD);
        st.setMaxLines(1);
        tile.addView(st, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(22)));
        Ui.press(tile, () -> start(new int[]{s}));
        return tile;
    }

    // ---------------------------------------------------------------- questions

    void start(int[] sections) {
        taken = sections;
        Exam exam = new Exam(rnd);
        questions = new ArrayList<>();
        for (int s : sections) questions.addAll(exam.section(s));
        index = 0;
        showQuestion(true);
    }

    private void showQuestion(boolean first) {
        busy = false;
        Exam.Q q = questions.get(index);
        int[] col = Exam.COLORS[q.section];
        Context c = getContext();
        body.removeAllViews();

        LinearLayout page = new LinearLayout(c);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(Ui.dp(20), Ui.dp(2), Ui.dp(20), Ui.dp(14));
        Ui.noClip(page);

        // Which part, which question, and how far along.
        LinearLayout top = new LinearLayout(c);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView part = Ui.text(c, Exam.ICONS[q.section] + "  " + Exam.NAMES[q.section], 15, Ui.WHITE, Ui.BLACK);
        part.setBackground(Ui.gradient(col, Ui.dp(16)));
        part.setPadding(Ui.dp(14), 0, Ui.dp(14), 0);
        top.addView(part, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, Ui.dp(32)));
        int inPart = index % Exam.PER_SECTION;
        TextView count = Ui.text(c, "Soalan " + (inPart + 1) + " / " + Exam.PER_SECTION, 15, Ui.MUTED, Ui.BOLD);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, Ui.dp(32));
        cp.leftMargin = Ui.dp(12);
        top.addView(count, cp);
        FrameLayout bar = new FrameLayout(c);
        bar.setBackground(Ui.round(0x332B2D42, Ui.dp(5)));
        View fill = new View(c);
        fill.setBackground(Ui.gradient(col, Ui.dp(5)));
        bar.addView(fill, new FrameLayout.LayoutParams(0, LayoutParams.MATCH_PARENT));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0, Ui.dp(10), 1f);
        bp.leftMargin = Ui.dp(14);
        top.addView(bar, bp);
        bar.post(() -> {
            fill.getLayoutParams().width = bar.getWidth() * (index + 1) / questions.size();
            fill.requestLayout();
        });
        page.addView(top, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        LinearLayout main = new LinearLayout(c);
        Ui.noClip(main);
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f);
        mp.topMargin = Ui.dp(10);
        page.addView(main, mp);

        // The question card: instruction, then the picture or letters, then a line like "2 + 1 = ?".
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(Ui.dp(16), Ui.dp(10), Ui.dp(16), Ui.dp(10));
        card.setBackground(Ui.round(Ui.WHITE, Ui.dp(30)));
        Ui.lift(card, 12, col[0]);
        current = q;
        TextView prompt = Ui.fit(c, text(q), 20, Ui.INK, Ui.BLACK);
        promptView = prompt;
        prompt.setMaxLines(2);
        Ui.press(prompt, () -> sayPrompt(q));
        card.addView(prompt, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(50)));
        card.addView(stem(q, col), new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        if (!q.caption.isEmpty()) {
            TextView cap = Ui.fit(c, q.caption, 34, Ui.INK, Ui.BLACK);
            cap.setMaxLines(1);
            card.addView(cap, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(44)));
        }
        main.addView(card, new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1.15f));

        // The answers: big buttons, in one column, or two by two when there are four.
        LinearLayout answers = new LinearLayout(c);
        answers.setOrientation(LinearLayout.VERTICAL);
        Ui.noClip(answers);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
        ap.leftMargin = Ui.dp(18);
        main.addView(answers, ap);
        ArrayList<View> buttons = new ArrayList<>();
        int n = q.options.length, perRow = n == 4 ? 2 : 1;
        LinearLayout line = null;
        for (int i = 0; i < n; i++) {
            if (i % perRow == 0) {
                line = new LinearLayout(c);
                Ui.noClip(line);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f);
                if (i > 0) lp.topMargin = Ui.dp(12);
                answers.addView(line, lp);
            }
            View b = answer(q, i, col);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
            if (i % perRow > 0) lp.leftMargin = Ui.dp(12);
            line.addView(b, lp);
            buttons.add(b);
        }

        body.addView(page, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        if (first) {
            buttons.add(0, card);
            Ui.enter(buttons, 40);
        } else {
            // The next question slides in from the right, like turning a page.
            page.setTranslationX(Ui.dp(120));
            page.setAlpha(0f);
            Spring.of(page, View.TRANSLATION_X).config(320f, 0.8f).to(0f);
            Spring.of(page, View.ALPHA).config(320f, 1f).to(1f);
        }
        sayPrompt(q);
        MainActivity.log("exam q " + index + " " + q.answer);
        act.logViewsSoon();
    }

    private void sayPrompt(Exam.Q q) {
        talk(q.promptMs, q.promptEn);
        if (q.zh != null) act.voice.queueChinese(q.zh); // then the Mandarin itself
    }

    /** The instruction in the chosen language (Malay when muted, as on the paper). */
    private String text(Exam.Q q) {
        return "en".equals(mode) ? q.promptEn : q.promptMs;
    }

    private void talk(String ms, String en) {
        if ("en".equals(mode)) act.voice.say(en);
        else if ("ms".equals(mode)) act.voice.sayMalay(ms);
    }

    /** Melayu, then English, then Senyap, then Melayu again. */
    private void nextVoice() {
        mode = "ms".equals(mode) ? "en" : "en".equals(mode) ? "off" : "ms";
        prefs().edit().putString("exam_voice", mode).apply();
        showVoice();
        Ui.jelly(voiceButton, 1.5f);
        MainActivity.log("exam voice " + mode);
        if (promptView != null && current != null && promptView.isAttachedToWindow()) {
            promptView.setText(text(current));
            sayPrompt(current);
        }
        if ("ms".equals(mode) && !act.voice.hasMalay()) {
            // Say why it stays quiet: this phone has no Malay (or Indonesian) voice installed.
            voiceButton.setText("🔇 Tiada suara Melayu");
            voiceButton.postDelayed(this::showVoice, 2500);
        }
    }

    private void showVoice() {
        voiceButton.setText("en".equals(mode) ? "🔊 English" : "off".equals(mode) ? "🔇 Senyap" : "🔊 Melayu");
        act.voice.setMuted("off".equals(mode));
    }

    @Override
    void onShown() {
        act.voice.setMuted("off".equals(mode));
    }

    @Override
    void onHidden() {
        act.voice.setMuted(false);
    }

    private View stem(Exam.Q q, int[] col) {
        Context c = getContext();
        switch (q.kind) {
            case SWATCH: {
                // A paint splodge in the colour, in a frame so it reads as "this colour".
                FrameLayout f = new FrameLayout(c);
                View blob = new View(c);
                blob.setBackground(Ui.oval(new int[]{q.color, q.color}));
                Ui.lift(blob, 6, q.color);
                int s = Ui.dp(120);
                f.addView(blob, new FrameLayout.LayoutParams(s, (int) (s * 0.85f), Gravity.CENTER));
                return f;
            }
            case SHAPE: {
                View v = new View(c);
                v.setBackground(new ShapeDrawable2D(q.shape));
                return v;
            }
            case JAWI: {
                TextView t = Ui.fit(c, q.stem, 110, Ui.INK, Typeface.DEFAULT_BOLD);
                t.setMaxLines(1);
                Ui.gradientText(t, col);
                return t;
            }
            case TEXT: {
                TextView t = Ui.fit(c, q.stem, 110, Ui.INK, Ui.BLACK);
                t.setMaxLines(1);
                Ui.gradientText(t, col);
                return t;
            }
            default: {
                TextView t = Ui.fit(c, "", 90, Ui.INK, null);
                Ui.picture(t, q.stem);
                return t;
            }
        }
    }

    private View answer(Exam.Q q, int i, int[] col) {
        Context c = getContext();
        TextView b = Ui.fit(c, q.options[i], q.jawiOptions ? 48 : 40, Ui.INK,
                q.jawiOptions ? Typeface.DEFAULT_BOLD : Ui.BLACK);
        b.setMaxLines(1);
        b.setPadding(Ui.dp(10), Ui.dp(4), Ui.dp(10), Ui.dp(4));
        b.setBackground(Ui.round(Ui.GLASS, Ui.dp(24)));
        Ui.lift(b, 6, col[0]);
        b.setContentDescription("opt:" + i);
        Ui.press(b, () -> choose(q, i, b, col));
        return b;
    }

    private void choose(Exam.Q q, int i, TextView b, int[] col) {
        if (busy) return;
        busy = true;
        q.chosen = q.options[i];
        MainActivity.log("exam answer " + index + " " + (q.right() ? "right" : "wrong"));
        // No right-or-wrong here: it is a test. The choice lights up, and the next question comes.
        b.setBackground(Ui.gradient(col, Ui.dp(24)));
        b.setTextColor(Ui.WHITE);
        Ui.jelly(b, 1.5f);
        postDelayed(() -> {
            index++;
            if (index < questions.size()) showQuestion(false);
            else showResult();
        }, 450);
    }

    // ---------------------------------------------------------------- results

    private void showResult() {
        Context c = getContext();
        body.removeAllViews();
        int[] score = new int[Exam.NAMES.length];
        List<Exam.Q> wrong = new ArrayList<>();
        for (Exam.Q q : questions) {
            if (q.right()) score[q.section]++;
            else wrong.add(q);
        }
        int total = 0, max = taken.length * Exam.PER_SECTION;
        SharedPreferences.Editor e = prefs().edit();
        String day = new SimpleDateFormat("d MMM", Locale.forLanguageTag("ms")).format(new Date());
        for (int s : taken) {
            total += score[s];
            e.putInt("exam_" + s, score[s]).putString("exam_" + s + "_day", day);
            MainActivity.log("exam result " + s + " " + score[s] + "/25");
        }
        e.apply();
        MainActivity.log("exam total " + total + "/" + max);

        LinearLayout page = new LinearLayout(c);
        page.setPadding(Ui.dp(20), Ui.dp(4), Ui.dp(20), Ui.dp(14));
        Ui.noClip(page);

        // Left: the total, stars and a cheer.
        LinearLayout left = new LinearLayout(c);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setGravity(Gravity.CENTER);
        left.setBackground(Ui.round(Ui.WHITE, Ui.dp(30)));
        Ui.lift(left, 12, Theme.current.title[0]);
        float pct = total / (float) max;
        String stars = pct >= 0.84f ? "⭐⭐⭐" : pct >= 0.6f ? "⭐⭐☆" : pct >= 0.4f ? "⭐☆☆" : "☆☆☆";
        left.addView(Ui.fit(c, stars, 44, 0xFFFFB300, null),
                new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(60)));
        TextView big = Ui.fit(c, total + " / " + max, 64, Ui.INK, Ui.BLACK);
        big.setMaxLines(1);
        Ui.gradientText(big, Theme.current.title);
        big.setContentDescription("total:" + total);
        left.addView(big, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(84)));
        String cheer = pct >= 0.84f ? "Cemerlang! Tahniah!" : pct >= 0.6f ? "Bagus! Teruskan usaha!"
                : "Cuba lagi, kamu boleh!";
        String cheerEn = pct >= 0.84f ? "Excellent! Congratulations!" : pct >= 0.6f ? "Good! Keep it up!"
                : "Try again, you can do it!";
        left.addView(Ui.fit(c, cheer, 20, Ui.MUTED, Ui.BOLD),
                new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(32)));
        LinearLayout btns = new LinearLayout(c);
        btns.setGravity(Gravity.CENTER);
        Ui.noClip(btns);
        btns.addView(pill(wrong.isEmpty() ? "Semua betul!" : "Lihat jawapan", Theme.current.title,
                () -> { if (!wrong.isEmpty()) showReview(wrong); }));
        View again = pill("Cuba lagi", new int[]{0xFF6B7280, 0xFF9CA3AF}, this::showIntro);
        ((LinearLayout.LayoutParams) again.getLayoutParams()).leftMargin = Ui.dp(10);
        btns.addView(again);
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(56));
        bl.topMargin = Ui.dp(8);
        left.addView(btns, bl);
        page.addView(left, new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));

        // Right: each part's mark, a bar, and the paper's band.
        LinearLayout right = new LinearLayout(c);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1.1f);
        rp.leftMargin = Ui.dp(18);
        page.addView(right, rp);
        for (int s : taken) right.addView(scoreRow(s, score[s]), rowParams());

        body.addView(page, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        resultPage = page;
        page.setAlpha(0f);
        Spring.of(page, View.ALPHA).config(260f, 1f).to(1f);
        act.celebrate(null);
        talk("Kamu dapat " + total + " markah daripada " + max + ". " + cheer,
                "You got " + total + " marks out of " + max + ". " + cheerEn);
        act.logViewsSoon();
    }

    private LinearLayout.LayoutParams rowParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f);
        lp.topMargin = lp.bottomMargin = Ui.dp(4);
        return lp;
    }

    private View scoreRow(int s, int score) {
        Context c = getContext();
        LinearLayout row = new LinearLayout(c);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Ui.dp(14), 0, Ui.dp(14), 0);
        row.setBackground(Ui.round(Ui.GLASS, Ui.dp(20)));
        Ui.lift(row, 4, Exam.COLORS[s][0]);
        TextView name = Ui.fit(c, Exam.ICONS[s] + " " + Exam.NAMES[s], 17, Ui.INK, Ui.BLACK);
        name.setMaxLines(1);
        name.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        row.addView(name, new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1.5f));
        LinearLayout mid = new LinearLayout(c);
        mid.setOrientation(LinearLayout.VERTICAL);
        mid.setGravity(Gravity.CENTER);
        FrameLayout bar = new FrameLayout(c);
        bar.setBackground(Ui.round(0x222B2D42, Ui.dp(5)));
        View fill = new View(c);
        fill.setBackground(Ui.gradient(Exam.COLORS[s], Ui.dp(5)));
        bar.addView(fill, new FrameLayout.LayoutParams(0, LayoutParams.MATCH_PARENT));
        bar.post(() -> {
            fill.getLayoutParams().width = bar.getWidth() * score / Exam.PER_SECTION;
            fill.requestLayout();
        });
        mid.addView(bar, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(10)));
        TextView band = Ui.fit(c, Exam.band(score), 13, bandColor(score), Ui.BOLD);
        band.setMaxLines(1);
        mid.addView(band, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(20)));
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1.4f);
        mp.leftMargin = mp.rightMargin = Ui.dp(10);
        row.addView(mid, mp);
        TextView mark = Ui.fit(c, score + "/25", 22, Exam.COLORS[s][0], Ui.BLACK);
        mark.setMaxLines(1);
        mark.setContentDescription("score:" + s + ":" + score);
        row.addView(mark, new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 0.8f));
        return row;
    }

    private static int bandColor(int score) {
        if (score >= 21) return 0xFF14A05A;
        if (score >= 15) return 0xFF1E6FE0;
        if (score >= 10) return 0xFFF57C00;
        return 0xFFE53935;
    }

    private View pill(String text, int[] colors, Runnable action) {
        TextView t = Ui.text(getContext(), text, 17, Ui.WHITE, Ui.BLACK);
        t.setBackground(Ui.gradient(colors, Ui.dp(24)));
        t.setPadding(Ui.dp(20), 0, Ui.dp(20), 0);
        Ui.lift(t, 6, colors[0]);
        t.setContentDescription(text);
        Ui.press(t, action);
        t.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, Ui.dp(48)));
        return t;
    }

    /** For the parent: every question answered wrongly, what was chosen, and the right answer. */
    private void showReview(List<Exam.Q> wrong) {
        Context c = getContext();
        body.removeAllViews();
        LinearLayout page = new LinearLayout(c);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(Ui.dp(20), 0, Ui.dp(20), Ui.dp(10));
        LinearLayout top = new LinearLayout(c);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(Ui.text(c, "Jawapan yang salah (" + wrong.size() + ")", 18, Ui.INK, Ui.BLACK));
        Screen.addSpacer(top);
        top.addView(pill("Kembali", Theme.current.title, () -> {
            body.removeAllViews();
            body.addView(resultPage);
        }));
        page.addView(top, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(56)));
        ScrollView sv = new ScrollView(c);
        LinearLayout list = new LinearLayout(c);
        list.setOrientation(LinearLayout.VERTICAL);
        for (Exam.Q q : wrong) {
            LinearLayout row = new LinearLayout(c);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(Ui.dp(14), Ui.dp(8), Ui.dp(14), Ui.dp(8));
            row.setBackground(Ui.round(Ui.GLASS, Ui.dp(18)));
            String what = q.kind == Exam.Stem.SWATCH ? "🎨" : q.kind == Exam.Stem.SHAPE ? "🔷"
                    : Content.XYLOPHONE.equals(q.stem) ? "🎶" : q.stem.replace('\n', ' ');
            TextView qText = Ui.text(c, Exam.ICONS[q.section] + "  " + text(q) + "\n" + what
                    + (q.caption.isEmpty() ? "" : "   " + q.caption), 15, Ui.INK, Ui.BOLD);
            qText.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
            row.addView(qText, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
            // The child's answer in red, the right one in green.
            String wrongLine = "✗ " + q.chosen;
            android.text.SpannableString both = new android.text.SpannableString(wrongLine + "\n✓ " + q.options[q.answer]);
            both.setSpan(new android.text.style.ForegroundColorSpan(0xFFE53935), 0, wrongLine.length(), 0);
            TextView ans = Ui.text(c, both, 18, 0xFF14A05A, Ui.BLACK);
            ans.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
            row.addView(ans, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = Ui.dp(8);
            list.addView(row, lp);
        }
        sv.addView(list);
        page.addView(sv, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        body.addView(page, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    private SharedPreferences prefs() {
        return getContext().getSharedPreferences("ceria", Context.MODE_PRIVATE);
    }
}
