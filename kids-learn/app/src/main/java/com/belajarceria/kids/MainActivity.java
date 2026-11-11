package com.belajarceria.kids;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Random;

/**
 * The only activity. Screens are plain views in a stack: opening one zooms it in from the card that was tapped
 * (like opening an app on an iPhone) while the one behind fades and grows slightly; going back reverses it.
 */
public final class MainActivity extends Activity {
    private final ArrayList<Screen> stack = new ArrayList<>();
    private final Random random = new Random();
    private FrameLayout host;
    private BlobBackground bg;
    private Confetti confetti;
    private HeroLayer heroes;
    private BabyLayer babies;
    private TextView badge;
    private long lastNav;
    Voice voice;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Ui.init(this);
        Theme.load(this);
        String theme = getIntent().getStringExtra("theme"); // for screenshots: --es theme hero
        if (theme != null) Theme.save(this, Theme.byId(theme));
        Sfx.init();
        voice = new Voice(this);

        FrameLayout root = new FrameLayout(this);
        bg = new BlobBackground(this);
        root.addView(bg, match());
        host = new FrameLayout(this);
        Ui.noClip(host);
        root.addView(host, match());
        heroes = new HeroLayer(this);
        root.addView(heroes, match());
        babies = new BabyLayer(this);
        root.addView(babies, match());
        confetti = new Confetti(this);
        root.addView(confetti, match());

        badge = Ui.text(this, "", 30, Ui.INK, Ui.BLACK);
        badge.setBackground(Ui.round(0xF7FFFFFF, Ui.dp(40)));
        badge.setPadding(Ui.dp(44), Ui.dp(20), Ui.dp(44), Ui.dp(26));
        Ui.lift(badge, 24, 0xFFFFB300);
        badge.setVisibility(View.GONE);
        root.addView(badge, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        setContentView(root);
        immersive();
        push(new HomeScreen(this));
        String open = getIntent().getStringExtra("open");
        if (open != null) open(open);
        if ("demo".equals(getIntent().getStringExtra("heroes"))) heroes.demo(); // for screenshots
        if ("demo".equals(getIntent().getStringExtra("babies"))) babies.demo();
    }

    /**
     * Opens a screen straight away, for screenshots and testing:
     * {@code adb shell am start -n com.belajarceria.kids/.MainActivity --es open 2,3} opens Alif Ba Ta, Match.
     */
    private void open(String path) {
        try {
            if (path.startsWith("zh")) {
                // "zh" opens Cakap Mandarin; "zh,3" goes straight to level 4 (public speaking)
                String[] p = path.split(",");
                lastNav = 0;
                ChineseScreen zh = new ChineseScreen(this);
                push(zh);
                if (p.length > 1) {
                    int level = Integer.parseInt(p[1].trim());
                    lastNav = 0;
                    push(zh.level(level));
                    // a third number opens one conversation (level 3) or speech (level 4) directly
                    if (p.length > 2 && (level == 2 || level == 3)) {
                        int item = Integer.parseInt(p[2].trim());
                        lastNav = 0;
                        push(level == 2 ? new ChatScreen(this, item) : new StageScreen(this, item));
                    }
                }
                return;
            }
            if (path.startsWith("music")) {
                // "music,2" opens the music corner on the guitar
                String[] p = path.split(",");
                lastNav = 0;
                push(new MusicScreen(this, p.length > 1 ? Integer.parseInt(p[1].trim()) : 0));
                return;
            }
            if (path.startsWith("exam")) {
                // "exam" opens the test; "exam,2" starts its Matematik part straight away.
                lastNav = 0;
                ExamScreen exam = new ExamScreen(this);
                push(exam);
                String[] p = path.split(",");
                if (p.length > 1) exam.start(new int[]{Integer.parseInt(p[1].trim())});
                return;
            }
            String[] p = path.split(",");
            Content.Subject s = Content.SUBJECTS[Integer.parseInt(p[0].trim())];
            lastNav = 0;
            push(new SubjectScreen(this, s));
            if (p.length > 1) {
                lastNav = 0;
                Screen game = SubjectScreen.activity(this, s, Integer.parseInt(p[1].trim()));
                // A third number opens Learn at that item: "0,0,23" is the X card.
                if (p.length > 2 && game instanceof LearnScreen) ((LearnScreen) game).jumpTo(Integer.parseInt(p[2].trim()));
                push(game);
            }
        } catch (RuntimeException ignored) {
            // a mistyped path just leaves the home screen
        }
    }

    private static FrameLayout.LayoutParams match() {
        return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    void push(Screen s) {
        long now = SystemClock.uptimeMillis();
        if (!stack.isEmpty() && now - lastNav < 400) return; // a double tap opens one screen, not two
        lastNav = now;
        final Screen prev = stack.isEmpty() ? null : stack.get(stack.size() - 1);
        stack.add(s);
        host.addView(s, match());
        s.setAlpha(0f);
        s.setScaleX(0.86f);
        s.setScaleY(0.86f);
        s.setTranslationY(Ui.dp(30));
        Spring.of(s, View.ALPHA).config(260f, 1f).to(1f);
        Spring.scale(s, 1f, 240f, 0.74f);
        Spring.of(s, View.TRANSLATION_Y).config(240f, 0.8f).to(0f);
        if (prev != null) {
            prev.onHidden();
            Spring.of(prev, View.ALPHA).config(400f, 1f).then(() -> prev.setVisibility(View.INVISIBLE)).to(0f);
            Spring.scale(prev, 1.08f, 300f, 1f);
        }
        bg.setPalette(s.palette());
        s.onShown();
        logViewsSoon();
    }

    /** Switches the whole app to another theme: the home screen is rebuilt in its colours and fades in. */
    void switchTheme(Theme t) {
        if (t == Theme.current || stack.size() != 1) return;
        Theme.save(this, t);
        final Screen old = stack.remove(0);
        old.onHidden();
        Spring.of(old, View.ALPHA).config(400f, 1f).then(() -> host.removeView(old)).to(0f);
        lastNav = 0;
        push(new HomeScreen(this));
        confetti.burst(confetti.getWidth() / 2f, confetti.getHeight() * 0.3f, 50);
        voice.say(t.label + "!");
    }

    void back() {
        if (stack.size() <= 1) {
            finish();
            return;
        }
        long now = SystemClock.uptimeMillis();
        if (now - lastNav < 250) return;
        lastNav = now;
        voice.stop();
        final Screen top = stack.remove(stack.size() - 1);
        top.onHidden();
        Spring.stop(top);
        // Let touches reach the screen behind right away, while this one shrinks away.
        top.setEnabled(false);
        Spring.of(top, View.ALPHA).config(380f, 1f).then(() -> host.removeView(top)).to(0f);
        Spring.scale(top, 0.86f, 320f, 1f);
        Screen prev = stack.get(stack.size() - 1);
        prev.setVisibility(View.VISIBLE);
        prev.bringToFront();
        Spring.of(prev, View.ALPHA).config(260f, 1f).to(1f);
        Spring.scale(prev, 1f, 260f, 0.8f);
        bg.setPalette(prev.palette());
        prev.onShown();
        logViewsSoon();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        // The phone's own back gesture is locked too: only holding the ⬅ button for five seconds goes back.
        backHint();
    }

    private android.widget.Toast hintToast;

    /** "Hold for five seconds": shown when back is tapped instead of held. */
    void backHint() {
        log("nav back tap");
        if (hintToast != null) hintToast.cancel();
        hintToast = android.widget.Toast.makeText(this, stack.size() <= 1
                ? "Tekan butang Home telefon untuk keluar 🏠"
                : "Tekan & tahan ⬅ selama 5 saat untuk kembali ⏳", android.widget.Toast.LENGTH_SHORT);
        hintToast.show();
    }

    /** Confetti, a star, a fanfare and a cheer; then {@code after} runs (usually the next round). */
    void celebrate(Runnable after) {
        Theme th = Theme.current;
        String[] p = th.praise[random.nextInt(th.praise.length)];
        if (!stack.isEmpty()) stack.get(stack.size() - 1).cheer();
        log("celebrate " + p[0]);
        SpannableString s = new SpannableString(th.badge + "\n" + p[0]);
        s.setSpan(new RelativeSizeSpan(2.2f), 0, th.badge.length(), 0);
        badge.setText(s);
        badge.setVisibility(View.VISIBLE);
        badge.setAlpha(0f);
        badge.setScaleX(0.3f);
        badge.setScaleY(0.3f);
        badge.setRotation(-18f);
        Spring.of(badge, View.ALPHA).config(300f, 1f).to(1f);
        Spring.scale(badge, 1f, 260f, 0.48f);
        Spring.of(badge, View.ROTATION).config(200f, 0.42f).to(0f);
        if (Theme.current == Theme.HERO) heroes.squad();
        if (Theme.current == Theme.BABY) babies.wave();
        confetti.rain(70);
        confetti.burst(confetti.getWidth() / 2f, confetti.getHeight() * 0.62f, 60);
        Sfx.fanfare();
        voice.say(p[1]);
        badge.postDelayed(() -> {
            Spring.of(badge, View.ALPHA).config(300f, 1f).then(() -> {
                badge.setVisibility(View.GONE);
                if (after != null) after.run();
            }).to(0f);
            Spring.scale(badge, 0.8f, 300f, 1f);
        }, 1700);
    }

    private Runnable micGranted;

    /** Runs the action once the microphone may be used, asking the parent the first time. */
    void withMic(Runnable action) {
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            action.run();
            return;
        }
        micGranted = action;
        requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, 7);
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        if (code == 7 && results.length > 0 && results[0] == android.content.pm.PackageManager.PERMISSION_GRANTED
                && micGranted != null) {
            micGranted.run();
        }
        micGranted = null;
    }

    /** For automated tests: once the screen settles, log where each labelled piece is on screen. */
    void logViewsSoon() {
        host.removeCallbacks(logViews);
        host.postDelayed(logViews, 1800);
    }

    private final Runnable logViews = () -> {
        if (stack.isEmpty()) return;
        log("layout");
        logTree(stack.get(stack.size() - 1));
    };

    private void logTree(View v) {
        CharSequence d = v.getContentDescription();
        if (d != null && v.isShown()) {
            int[] p = restingPlace(v);
            log("view " + d + " " + p[0] + " " + p[1] + " " + (p[0] + v.getWidth()) + " " + (p[1] + v.getHeight()));
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) logTree(g.getChildAt(i));
        }
    }

    /** Where the view sits on screen once every animation has settled: layout positions, no movement or scale. */
    private int[] restingPlace(View v) {
        int x = 0, y = 0;
        View c = v;
        while (c != host && c.getParent() instanceof View) {
            View parent = (View) c.getParent();
            x += c.getLeft() - parent.getScrollX();
            y += c.getTop() - parent.getScrollY();
            c = parent;
        }
        int[] h = new int[2];
        host.getLocationOnScreen(h);
        return new int[]{h[0] + x, h[1] + y};
    }

    /** One line per game event in logcat (tag Ceria), so automated tests can check what happened. */
    static void log(String event) {
        Log.i("Ceria", event);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) immersive();
    }

    @SuppressWarnings("deprecation")
    private void immersive() {
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.systemBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }

    /**
     * A right answer, in a themed look: a hero flies in and zaps it with a star-laser, or a baby pops up and sends
     * hearts at it. Never called from the test, so it gives nothing away there.
     */
    void reward(View target, Content.Subject s) {
        if (target == null) return;
        int[] t = new int[2], me = new int[2];
        target.getLocationOnScreen(t);
        heroes.getLocationOnScreen(me);
        if (Theme.current == Theme.BABY) {
            int b = Theme.current.baby(s);
            babies.pop(b < 0 ? random.nextInt(BabyArt.COUNT) : b,
                    t[0] - me[0] + target.getWidth() / 2f, t[1] - me[1] + target.getHeight() / 2f);
            MainActivity.log("baby pop " + b);
            return;
        }
        if (Theme.current != Theme.HERO) return;
        HeroArt.Hero h = HeroArt.SQUAD[random.nextInt(HeroArt.SQUAD.length)];
        heroes.zap(h, t[0] - me[0] + target.getWidth() / 2f, t[1] - me[1] + target.getHeight() / 2f);
        MainActivity.log("hero zap " + h.name);
    }

    /** Every so often in the Hero Squad theme, a hero runs along the bottom or flies across the sky. */
    private final Runnable ambient = new Runnable() {
        @Override
        public void run() {
            if (Theme.current == Theme.BABY && !babies.busy()) babies.play();
            if (Theme.current == Theme.HERO && !heroes.busy()) {
                HeroArt.Hero h = HeroArt.SQUAD[random.nextInt(HeroArt.SQUAD.length)];
                if (random.nextBoolean()) heroes.runAcross(h);
                else heroes.flyAcross(h, 0, 0.12f + random.nextFloat() * 0.25f);
            }
            heroes.postDelayed(this, 9000 + random.nextInt(9000));
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (!stack.isEmpty() && stack.get(stack.size() - 1) instanceof MusicScreen) Synth.get().start();
        heroes.removeCallbacks(ambient);
        heroes.postDelayed(ambient, 4000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Synth.get().stop();
        heroes.removeCallbacks(ambient);
        voice.stop();
    }

    @Override
    protected void onDestroy() {
        voice.shutdown();
        Sfx.release();
        super.onDestroy();
    }
}
