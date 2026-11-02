package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieCompositionFactory;
import com.airbnb.lottie.LottieDrawable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Manages the Lottie Profile Avatar theme, background color, character stroke color,
 * and continuous playback on the Home screen.
 */
public class AvatarThemeManager {

    private static final String PREFS_NAME = "app_avatar_prefs";
    private static final String KEY_BG_COLOR = "avatar_bg_color";
    private static final String KEY_CHAR_COLOR = "avatar_char_color";

    // Default teal background & crisp white character from original Lottie design
    public static final int DEFAULT_BG_COLOR = 0xFF00DDB3;
    public static final int DEFAULT_CHAR_COLOR = 0xFFFFFFFF;

    // Background color palette swatches
    public static final int[] BG_PALETTE = new int[]{
            0xFF00DDB3, // Mint Teal (Default)
            0xFF0EA5E9, // Cyber Sky Blue
            0xFF8B5CF6, // Electric Violet
            0xFFF59E0B, // Sunset Amber
            0xFFEF4444, // Crimson Red
            0xFF10B981, // Emerald Green
            0xFFEC4899, // Neon Rose
            0xFF1E293B  // Midnight Slate
    };

    public static final String[] BG_PALETTE_NAMES = new String[]{
            "Mint Teal", "Cyber Blue", "Electric Violet", "Sunset Amber",
            "Crimson", "Emerald", "Neon Rose", "Midnight"
    };

    // Character color palette swatches
    public static final int[] CHAR_PALETTE = new int[]{
            0xFFFFFFFF, // Pure White (Default)
            0xFF38BDF8, // Electric Cyan
            0xFFFDE047, // Golden Glow
            0xFF4ADE80, // Neon Mint
            0xFFFDA4AF, // Pastel Rose
            0xFFE9D5FF, // Pale Lilac
            0xFF111827  // Obsidian
    };

    public static final String[] CHAR_PALETTE_NAMES = new String[]{
            "Putih Bersih", "Cyan", "Kuning Suria", "Hijau Neon",
            "Rose", "Lilac", "Hitam Elegan"
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
     * Builds customized Lottie JSON by dynamically coloring the background circle
     * and character strokes according to user preferences.
     */
    public static String buildThemedJson(Context context, int bgColor, int charColor) {
        String raw = getRawJson(context);
        if (raw == null || raw.isEmpty()) return "";

        try {
            JSONObject root = new JSONObject(raw);
            JSONArray layers = root.optJSONArray("layers");
            if (layers != null) {
                double bgR = Color.red(bgColor) / 255.0;
                double bgG = Color.green(bgColor) / 255.0;
                double bgB = Color.blue(bgColor) / 255.0;

                double chR = Color.red(charColor) / 255.0;
                double chG = Color.green(charColor) / 255.0;
                double chB = Color.blue(charColor) / 255.0;

                // Secondary darker accent for depth
                double darkR = bgR * 0.35;
                double darkG = bgG * 0.35;
                double darkB = bgB * 0.35;

                for (int i = 0; i < layers.length(); i++) {
                    JSONObject layer = layers.getJSONObject(i);
                    String nm = layer.optString("nm", "");
                    JSONArray shapes = layer.optJSONArray("shapes");
                    if (shapes == null) continue;

                    if ("Shape Layer 1".equals(nm) || "Shape Layer 2".equals(nm)) {
                        // Background circle fill
                        recolorShapeFills(shapes, bgR, bgG, bgB);
                    } else if (nm.startsWith("Bezier")) {
                        // Character features and outlines
                        recolorShapeStrokes(shapes, chR, chG, chB, bgR, bgG, bgB, darkR, darkG, darkB);
                    }
                }
            }
            return root.toString();
        } catch (Exception e) {
            return raw;
        }
    }

    private static void recolorShapeFills(JSONArray shapes, double r, double g, double b) {
        try {
            for (int i = 0; i < shapes.length(); i++) {
                JSONObject s = shapes.getJSONObject(i);
                JSONArray items = s.optJSONArray("it");
                if (items != null) {
                    for (int j = 0; j < items.length(); j++) {
                        JSONObject it = items.getJSONObject(j);
                        if ("fl".equals(it.optString("ty"))) {
                            JSONObject c = it.optJSONObject("c");
                            if (c != null) {
                                JSONArray k = new JSONArray();
                                k.put(r).put(g).put(b).put(1.0);
                                c.put("k", k);
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private static void recolorShapeStrokes(JSONArray shapes, double chR, double chG, double chB,
                                            double bgR, double bgG, double bgB,
                                            double darkR, double darkG, double darkB) {
        try {
            for (int i = 0; i < shapes.length(); i++) {
                JSONObject s = shapes.getJSONObject(i);
                JSONArray items = s.optJSONArray("it");
                if (items != null) {
                    for (int j = 0; j < items.length(); j++) {
                        JSONObject it = items.getJSONObject(j);
                        if ("st".equals(it.optString("ty"))) {
                            JSONObject c = it.optJSONObject("c");
                            if (c != null) {
                                JSONArray oldK = c.optJSONArray("k");
                                if (oldK != null && oldK.length() >= 3) {
                                    double o0 = oldK.optDouble(0, 0);
                                    double o1 = oldK.optDouble(1, 0);
                                    double o2 = oldK.optDouble(2, 0);

                                    JSONArray newK = new JSONArray();
                                    // If originally white [1, 1, 1, 1], apply character color
                                    if (o0 > 0.9 && o1 > 0.9 && o2 > 0.9) {
                                        newK.put(chR).put(chG).put(chB).put(1.0);
                                    } else if (o0 == 0 && o1 > 0.8) {
                                        // Teal highlight stroke -> match background color
                                        newK.put(bgR).put(bgG).put(bgB).put(1.0);
                                    } else {
                                        // Dark stroke outline -> contrast shade
                                        newK.put(darkR).put(darkG).put(darkB).put(1.0);
                                    }
                                    c.put("k", newK);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
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

        String cacheKey = String.format(Locale.ROOT, "avatar_%08x_%08x", bgColor, charColor);
        String themedJson = buildThemedJson(context, bgColor, charColor);

        LottieCompositionFactory.fromJsonString(themedJson, cacheKey).addListener(composition -> {
            view.setComposition(composition);
            view.setRepeatCount(LottieDrawable.INFINITE);
            view.setRepeatMode(LottieDrawable.RESTART);
            view.playAnimation();
        });
    }
}
