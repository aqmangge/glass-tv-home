package com.belajarceria.kids;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Level 4: giving a speech. A stage with velvet curtains, a spotlight and a podium; an audience in the seats.
 * A teleprompter shows one line at a time (lighting up character by character as the model voice reads it);
 * then it's the child's turn to say it. After the last line the audience stands up and claps.
 */
final class StageScreen extends Screen {
    private final Chinese.Speech speech;
    private final int number;
    private final StageView stage;
    private final TextView lineView, pinyinView, meaningView, counter, starsView;
    private final LinearLayout prompter;
    private final Speech listener;
    private int line, starsTotal, karaoke = -1;

    StageScreen(MainActivity a, int number) {
        super(a);
        this.number = number;
        speech = Chinese.SPEECHES[number];
        listener = new Speech(a);
        stage = new StageView(a);
        addView(stage, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        LinearLayout col = new LinearLayout(a);
        col.setOrientation(LinearLayout.VERTICAL);
        Ui.noClip(col);
        LinearLayout bar = header(speech.emoji + "  " + speech.title, ZhKit.RED);
        addSpacer(bar);
        counter = Ui.text(a, "", 16, Ui.INK, Ui.BOLD);
        counter.setBackground(Ui.round(Ui.GLASS, Ui.dp(26)));
        counter.setPadding(Ui.dp(18), 0, Ui.dp(18), 0);
        bar.addView(counter, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, Ui.dp(52)));
        col.addView(bar, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        LinearLayout body = new LinearLayout(a);
        body.setPadding(Ui.dp(24), Ui.dp(4), Ui.dp(20), 0);
        Ui.noClip(body);
        col.addView(body, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));

        // the teleprompter, high on the stage above the podium
        prompter = new LinearLayout(a);
        prompter.setOrientation(LinearLayout.VERTICAL);
        prompter.setGravity(Gravity.CENTER_HORIZONTAL);
        prompter.setPadding(Ui.dp(18), Ui.dp(8), Ui.dp(18), Ui.dp(8));
        prompter.setBackground(Ui.round(0xE6201418, Ui.dp(22)));
        Ui.lift(prompter, 10, 0xFF000000);
        lineView = Ui.fit(a, "", 40, 0xFFFFFFFF, Typeface.DEFAULT_BOLD);
        lineView.setMaxLines(2);
        prompter.addView(lineView, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        pinyinView = Ui.fit(a, "", 18, 0xFFFFE082, Ui.BOLD);
        pinyinView.setMaxLines(1);
        prompter.addView(pinyinView, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(26)));
        meaningView = Ui.fit(a, "", 14, 0xCCFFFFFF, Ui.BOLD);
        meaningView.setMaxLines(1);
        prompter.addView(meaningView, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(20)));
        LinearLayout left = new LinearLayout(a);
        left.setOrientation(LinearLayout.VERTICAL);
        left.addView(prompter, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 0.62f));
        left.addView(new View(a), new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 0.38f));
        body.addView(left, new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));

        // the controls, at the side of the stage
        LinearLayout tools = new LinearLayout(a);
        tools.setOrientation(LinearLayout.VERTICAL);
        tools.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        Ui.noClip(tools);
        int tw = Ui.dp(180);
        tools.addView(ZhKit.button(a, "🔊  Dengar contoh", new int[]{0xFF3A86FF, 0xFF5E6BFF}, this::model), tool(tw, 0));
        ZhKit.Mic mic = new ZhKit.Mic(a, listener);
        this.mic = mic;
        tools.addView(mic.view, tool(tw, 10));
        TextView next = ZhKit.button(a, "Seterusnya  ▶", new int[]{0xFF8D6E63, 0xFFBCAAA4}, () -> go(1));
        next.setContentDescription("next");
        tools.addView(next, tool(tw, 10));
        starsView = Ui.fit(a, "", 28, 0xFFFFD54F, null);
        starsView.setMaxLines(1);
        tools.addView(starsView, tool(tw, 6));
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(tw, LayoutParams.MATCH_PARENT);
        tl.leftMargin = Ui.dp(14);
        body.addView(tools, tl);
        addView(col, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        show();
    }

    private final ZhKit.Mic mic;

    private static LinearLayout.LayoutParams tool(int w, int top) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w, Ui.dp(50));
        lp.topMargin = Ui.dp(top);
        return lp;
    }

    private void show() {
        Chinese.Phrase p = speech.lines[line];
        lineView.setText(p.hanzi);
        pinyinView.setText(p.pinyin);
        meaningView.setText(p.malay + "  ·  " + p.english);
        counter.setText("Ayat " + (line + 1) + " / " + speech.lines.length);
        starsView.setText("");
        mic.onTap(p.hanzi, (stars, heard) -> {
            starsTotal += stars;
            starsView.setText(ZhKit.stars(stars));
            Ui.jelly(starsView, 3f);
            if (stars >= 2) {
                stage.nod(); // the audience nods along
                Sfx.ding();
                cheer();
            }
            MainActivity.log("zh stage said " + line + " " + stars);
            postDelayed(() -> go(1), 1300);
        });
        MainActivity.log("zh stage line " + line);
    }

    /** The model voice reads the line while the characters light up one by one, like a karaoke screen. */
    private void model() {
        Chinese.Phrase p = speech.lines[line];
        act.voice.sayChinese(p.hanzi, true);
        final int my = ++karaoke;
        final long start = SystemClock.uptimeMillis();
        final int n = p.hanzi.length();
        Runnable step = new Runnable() {
            @Override
            public void run() {
                if (my != karaoke || !p.hanzi.contentEquals(lineView.getText().toString())) return;
                int lit = (int) Math.min(n, (SystemClock.uptimeMillis() - start) / 330 + 1);
                SpannableString s = new SpannableString(p.hanzi);
                s.setSpan(new ForegroundColorSpan(0xFFFFD54F), 0, lit, 0);
                lineView.setText(s);
                if (lit < n) postDelayed(this, 80);
                else postDelayed(() -> { if (my == karaoke) lineView.setText(p.hanzi); }, 900);
            }
        };
        post(step);
    }

    private void go(int dir) {
        listener.stop();
        karaoke++;
        if (line + dir >= speech.lines.length) {
            finale();
            return;
        }
        line = Math.max(0, line + dir);
        prompter.setAlpha(0f);
        prompter.setTranslationY(Ui.dp(20));
        show();
        Spring.of(prompter, View.ALPHA).config(300f, 1f).to(1f);
        Spring.of(prompter, View.TRANSLATION_Y).config(300f, 0.75f).to(0f);
        postDelayed(this::model, 400);
    }

    /** The end of the speech: the audience stands and claps, and the speech can be given again. */
    private void finale() {
        int stars = Math.max(1, Math.round(starsTotal / (float) speech.lines.length));
        ZhKit.saveStars(getContext(), "zh_3_" + number, stars);
        stage.applaud();
        Sfx.applause();
        lineView.setText("👏 掌声！");
        pinyinView.setText("Zhǎngshēng!");
        meaningView.setText("Tepukan gemuruh! Awak berucap di depan orang ramai!");
        counter.setText(ZhKit.stars(stars));
        line = 0;
        MainActivity.log("zh stage applause " + number);
        postDelayed(() -> act.celebrate(null), 400);
        postDelayed(this::show, 4500);
    }

    @Override
    boolean holdToGoBack() {
        return true;
    }

    @Override
    int[] palette() {
        return new int[]{0xFF2B1B24, 0xFF140C12, 0xFF7B1F2E, 0xFF3B1E4A, 0xFF5A2A1A, 0xFF2A1A3A};
    }

    @Override
    void onShown() {
        postDelayed(this::model, 700);
    }

    @Override
    void onHidden() {
        listener.stop();
        karaoke++;
    }

    /** The theatre: curtains, valance, spotlight, wooden stage, podium with a microphone, and the audience. */
    static final class StageView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final long start = SystemClock.uptimeMillis();
        private long applauseAt = -100000, nodAt = -100000;

        StageView(Context c) {
            super(c);
        }

        void applaud() {
            applauseAt = SystemClock.uptimeMillis();
        }

        void nod() {
            nodAt = SystemClock.uptimeMillis();
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), t = (SystemClock.uptimeMillis() - start) / 1000f;
            long now = SystemClock.uptimeMillis();
            float clap = Math.max(0f, 1f - (now - applauseAt) / 4500f);
            float nod = Math.max(0f, 1f - (now - nodAt) / 900f);
            // back wall and stage floor
            paint.setColor(0xFFFFFFFF); // a shader is still scaled by the paint's alpha
            paint.setShader(new LinearGradient(0, 0, 0, h, 0xFF3A1F2B, 0xFF1A1016, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, paint);
            float floor = h * 0.66f;
            paint.setColor(0xFFFFFFFF); // a shader is still scaled by the paint's alpha
            paint.setShader(new LinearGradient(0, floor, 0, h, 0xFFB5793F, 0xFF6E4423, Shader.TileMode.CLAMP));
            c.drawRect(0, floor, w, h, paint);
            paint.setShader(null);
            paint.setColor(0x22000000);
            for (float x = 0; x < w; x += Ui.dp(46)) c.drawLine(x, floor, x, h, paint);
            // spotlight on the podium, gently breathing
            float sx = w * 0.42f, sr = h * (0.55f + 0.02f * (float) Math.sin(t * 2));
            paint.setColor(0xFFFFFFFF); // a shader is still scaled by the paint's alpha
            paint.setShader(new RadialGradient(sx, floor, sr, new int[]{0x66FFF3C4, 0x22FFF3C4, 0x00FFF3C4}, null,
                    Shader.TileMode.CLAMP));
            c.drawCircle(sx, floor, sr, paint);
            paint.setShader(null);
            // the podium, with a microphone on a stand
            float px = sx, pw = Math.min(w * 0.13f, Ui.dp(120)), ptop = floor - h * 0.12f;
            paint.setColor(0xFF7B4A2A);
            path.reset();
            path.moveTo(px - pw / 2, ptop);
            path.lineTo(px + pw / 2, ptop);
            path.lineTo(px + pw * 0.4f, floor + h * 0.08f);
            path.lineTo(px - pw * 0.4f, floor + h * 0.08f);
            path.close();
            c.drawPath(path, paint);
            paint.setColor(0xFFFFB300);
            c.drawCircle(px, ptop + h * 0.07f, Ui.dp(9), paint);
            paint.setColor(0xFF424242);
            c.drawRect(px - Ui.dp(1.5f), ptop - h * 0.07f, px + Ui.dp(1.5f), ptop, paint);
            c.drawCircle(px, ptop - h * 0.075f, Ui.dp(6), paint);
            // velvet curtains with folds, and the scalloped valance across the top
            curtain(c, 0, w * 0.1f, h);
            curtain(c, w * 0.9f, w, h);
            paint.setColor(0xFF8E1B2B);
            float vh = h * 0.07f;
            c.drawRect(0, 0, w, vh, paint);
            for (float x = 0; x < w; x += Ui.dp(60)) c.drawCircle(x + Ui.dp(30), vh, Ui.dp(30), paint);
            paint.setColor(0xFFFFC107);
            c.drawRect(0, vh - Ui.dp(4), w, vh, paint);
            // the audience: rows of heads seen from behind, bobbing when they nod or clap
            int[] tones = {0xFF3E2723, 0xFF5D4037, 0xFF1A1A1A, 0xFF795548, 0xFF4E342E};
            int[] shirts = {0xFFE57373, 0xFF64B5F6, 0xFF81C784, 0xFFFFD54F, 0xFFBA68C8, 0xFF4DD0E1, 0xFFFF8A65};
            for (int row = 0; row < 2; row++) {
                float y = h - Ui.dp(row == 0 ? 54 : 18), r = Ui.dp(row == 0 ? 20 : 26), stepX = r * 2.6f;
                for (int i = 0; i * stepX < w + stepX; i++) {
                    float x = i * stepX + (row == 0 ? stepX / 2 : 0);
                    float phase = i * 1.7f + row;
                    float bob = clap > 0 ? (float) Math.abs(Math.sin(t * 14 + phase)) * Ui.dp(8) * clap
                            : nod * (float) Math.sin(t * 10 + phase) * Ui.dp(4);
                    float stand = clap > 0.4f ? Ui.dp(10) : 0;
                    paint.setColor(shirts[(i + row * 3) % shirts.length]);
                    c.drawOval(x - r * 1.25f, y - r * 0.2f - bob - stand, x + r * 1.25f, y + r * 2f, paint);
                    paint.setColor(tones[(i * 3 + row) % tones.length]);
                    c.drawCircle(x, y - r * 0.6f - bob - stand, r, paint);
                    if (clap > 0.05f) {
                        // hands up, clapping above their heads
                        float hx = (float) Math.sin(t * 16 + phase) * r * 0.35f;
                        paint.setColor(0xFFFFCCBC);
                        c.drawCircle(x - r * 0.4f + hx, y - r * 2f - bob - stand, r * 0.28f, paint);
                        c.drawCircle(x + r * 0.4f - hx, y - r * 2f - bob - stand, r * 0.28f, paint);
                    }
                }
            }
            postInvalidateOnAnimation();
        }

        private void curtain(Canvas c, float x0, float x1, float h) {
            float fold = (x1 - x0) / 3f;
            for (int i = 0; i < 3; i++) {
                float a = x0 + i * fold;
                paint.setColor(0xFFFFFFFF); // a shader is still scaled by the paint's alpha
                paint.setShader(new LinearGradient(a, 0, a + fold, 0, new int[]{0xFF6D0F1E, 0xFFB71C2E, 0xFF6D0F1E}, null,
                        Shader.TileMode.CLAMP));
                c.drawRect(a, 0, a + fold, h, paint);
            }
            paint.setShader(null);
        }
    }
}
