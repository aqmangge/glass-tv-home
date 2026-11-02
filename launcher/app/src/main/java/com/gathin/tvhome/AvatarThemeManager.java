package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieCompositionFactory;
import com.airbnb.lottie.LottieDrawable;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Manages the Lottie Character Profile Avatar (Eyes 2 Animated / Cute expressive faces),
 * character body recoloring, background recoloring, and continuous loop playback on TV.
 */
public class AvatarThemeManager {

    private static final String PREFS_NAME = "app_avatar_prefs";
    private static final String KEY_BG_COLOR = "avatar_bg_color";
    private static final String KEY_CHAR_COLOR = "avatar_char_color";

    // Original default colors from the Lottie animation
    public static final int DEFAULT_CHAR_COLOR = 0xFF00A0FF; // Electric Blue character body
    public static final int DEFAULT_BG_COLOR = 0xFF141720;   // Sleek Dark Glass background

    // Character body color palette swatches
    public static final int[] CHAR_PALETTE = new int[]{
            0xFF00A0FF, // Electric Blue (Original default)
            0xFF10B981, // Emerald Green
            0xFFF59E0B, // Sunny Amber
            0xFFFB7185, // Coral Pink
            0xFF8B5CF6, // Royal Violet
            0xFFEF4444, // Crimson Red
            0xFF06B6D4, // Cyber Cyan
            0xFFC084FC  // Lavender
    };

    public static final String[] CHAR_PALETTE_NAMES = new String[]{
            "Biru Elektrik", "Hijau Zamrud", "Kuning Amber", "Merah Jambu",
            "Ungu Diraja", "Merah Karang", "Siber Sian", "Lavender"
    };

    // Background color palette swatches
    public static final int[] BG_PALETTE = new int[]{
            0xFF141720, // Kaca Gelap (Dark Glass default)
            0xFFFAFAFA, // Putih Krim (Light mode default)
            0xFF1E293B, // Fajar Kelabu (Deep Slate)
            0xFF0F172A, // Biru Gelap (Royal Navy)
            0xFF2E1065, // Ungu Malam (Night Violet)
            0xFF064E3B, // Hijau Gelap (Forest Dark)
            0xFF111827, // Hitam Obsidian (Obsidian)
            0xFF7C2D12  // Senja Jingga (Sunset Bronze)
    };

    public static final String[] BG_PALETTE_NAMES = new String[]{
            "Kaca Gelap", "Putih Bersih", "Kelabu Slate", "Biru Gelap",
            "Ungu Malam", "Hijau Hutan", "Hitam Obsidian", "Senja Jingga"
    };

    private static String cachedRawJson = null;

    public static int getBgColor(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return p.getInt(KEY_BG_COLOR, DEFAULT_BG_COLOR);
    }

    public static void setBgColor(Context context, int color) {
        SharedPreferences p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        p.edit().putInt(KEY_BG_COLOR, color).apply();
    }

    public static int getCharColor(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return p.getInt(KEY_CHAR_COLOR, DEFAULT_CHAR_COLOR);
    }

    public static void setCharColor(Context context, int color) {
        SharedPreferences p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        p.edit().putInt(KEY_CHAR_COLOR, color).apply();
    }

    private static synchronized String getRawJson(Context context) {
        if (cachedRawJson != null) return cachedRawJson;
        try (InputStream is = context.getAssets().open("avatar_character.json")) {
            byte[] buf = new byte[is.available()];
            int read = is.read(buf);
            cachedRawJson = new String(buf, 0, read, StandardCharsets.UTF_8);
        } catch (Exception e) {
            cachedRawJson = "";
        }
        return cachedRawJson;
    }

    /**
     * Builds customized Lottie JSON by dynamically recoloring character bodies
     * and canvas background. Fast O(N) string replace in ~1.5ms.
     */
    public static String buildThemedJson(Context context, int bgColor, int charColor) {
        String raw = getRawJson(context);
        if (raw == null || raw.isEmpty()) return "";

        String charHex = String.format(Locale.ROOT, "#%06x", (0xFFFFFF & charColor));
        String bgHex = String.format(Locale.ROOT, "#%06x", (0xFFFFFF & bgColor));

        return raw
                .replace("\"#00a0ff\"", "\"" + charHex + "\"")
                .replace("\"#00A0FF\"", "\"" + charHex + "\"")
                .replace("\"#fafafa\"", "\"" + bgHex + "\"")
                .replace("\"#FAFAFA\"", "\"" + bgHex + "\"");
    }

    /**
     * Applies the user's customized avatar to a LottieAnimationView and loops continuously.
     */
    public static void applyAvatar(LottieAnimationView view, Context context) {
        int bgColor = getBgColor(context);
        int charColor = getCharColor(context);
        applyAvatar(view, bgColor, charColor, context);
    }

    public static void applyAvatar(LottieAnimationView view, int bgColor, int charColor, Context context) {
        if (view == null || context == null) return;

        String cacheKey = String.format(Locale.ROOT, "char_avatar_%08x_%08x", bgColor, charColor);
        String themedJson = buildThemedJson(context, bgColor, charColor);

        LottieCompositionFactory.fromJsonString(themedJson, cacheKey).addListener(composition -> {
            view.setComposition(composition);
            view.setRepeatCount(LottieDrawable.INFINITE);
            view.setRepeatMode(LottieDrawable.RESTART);
            view.playAnimation();
        });
    }
}
