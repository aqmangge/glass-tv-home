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
 * single character focus, expressions, character body recoloring, background recoloring,
 * and continuous loop playback on TV.
 */
public class AvatarThemeManager {

    private static final String PREFS_NAME = "app_avatar_prefs";
    private static final String KEY_BG_COLOR = "avatar_bg_color";
    private static final String KEY_CHAR_COLOR = "avatar_char_color";
    private static final String KEY_FACE_INDEX = "avatar_face_index";

    // Original default colors from the Lottie animation
    public static final int DEFAULT_CHAR_COLOR = 0xFF00A0FF; // Electric Blue character body
    public static final int DEFAULT_BG_COLOR = 0xFF141720;   // Sleek Dark Glass background
    public static final int DEFAULT_FACE_INDEX = 0;          // 0 = Loop All Expressions, 1..8 = Specific Expression

    // Character expression definitions (each runs 576 frames at 60fps)
    public static final String[] FACE_NAMES = new String[]{
            "Semua (Kitaran)", // 0: Frames 0-4608
            "1: Senyum",        // 1: Frames 0-576
            "2: Kenyit",        // 2: Frames 576-1152
            "3: Bersiul",       // 3: Frames 1152-1728
            "4: Mengantuk",     // 4: Frames 1728-2304
            "5: Ketawa",        // 5: Frames 2304-2880
            "6: Ceria",         // 6: Frames 2880-3456
            "7: Ingin Tahu",    // 7: Frames 3456-4032
            "8: Terkejut"       // 8: Frames 4032-4608
    };

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

    public static synchronized void clearCache() {
        cachedRawJson = null;
    }

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

    public static int getFaceIndex(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return p.getInt(KEY_FACE_INDEX, DEFAULT_FACE_INDEX);
    }

    public static void setFaceIndex(Context context, int index) {
        SharedPreferences p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        p.edit().putInt(KEY_FACE_INDEX, index).apply();
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
        int faceIndex = getFaceIndex(context);
        applyAvatar(view, bgColor, charColor, faceIndex, context);
    }

    public static void applyAvatar(LottieAnimationView view, int bgColor, int charColor, Context context) {
        applyAvatar(view, bgColor, charColor, getFaceIndex(context), context);
    }

    public static void applyAvatar(LottieAnimationView view, int bgColor, int charColor, int faceIndex, Context context) {
        if (view == null || context == null) return;

        String cacheKey = String.format(Locale.ROOT, "char_avatar_%08x_%08x_f%d", bgColor, charColor, faceIndex);
        String themedJson = buildThemedJson(context, bgColor, charColor);

        boolean animEnabled = !context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
                .getBoolean("reduced_motion", false);

        LottieCompositionFactory.fromJsonString(themedJson, cacheKey).addListener(composition -> {
            view.setComposition(composition);
            if (faceIndex >= 1 && faceIndex <= 8) {
                int startFrame = (faceIndex - 1) * 576;
                int endFrame = faceIndex * 576;
                view.setMinAndMaxFrame(startFrame, endFrame);
            } else {
                view.setMinAndMaxFrame(0, 4608);
            }
            if (animEnabled) {
                view.setRepeatCount(LottieDrawable.INFINITE);
                view.setRepeatMode(LottieDrawable.RESTART);
                view.playAnimation();
            } else {
                view.setFrame(faceIndex >= 1 && faceIndex <= 8 ? (faceIndex - 1) * 576 : 0);
                view.pauseAnimation();
            }
        });
    }
}
