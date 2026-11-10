package com.gathin.tvhome;

import android.content.Context;
import android.util.DisplayMetrics;

/**
 * Responsive Display Profile Manager for Android TV.
 * Provides two independent display profiles:
 * - Profile A: 4K UHD (3840 × 2160 px, 16:9 widescreen)
 * - Profile B: Full HD (1920 × 1080 px, 16:9 widescreen)
 *
 * Automatically adjusts typography, component paddings, prayer table spacing,
 * and dock proportions without arbitrary stretching or visual clipping.
 */
public final class DisplayProfile {

    public enum Type {
        FULL_HD_1080P,
        UHD_4K
    }

    private DisplayProfile() {}

    /**
     * Determines whether the active display canvas is running in 4K UHD profile.
     */
    public static boolean is4K(Context context) {
        if (context == null) return false;
        try {
            DisplayMetrics dm = context.getResources().getDisplayMetrics();
            int maxDim = Math.max(dm.widthPixels, dm.heightPixels);
            int minDim = Math.min(dm.widthPixels, dm.heightPixels);
            return maxDim >= 3840 || minDim >= 2160;
        } catch (Throwable ignored) {}
        return false;
    }

    public static Type getProfile(Context context) {
        return is4K(context) ? Type.UHD_4K : Type.FULL_HD_1080P;
    }

    /**
     * Proportional scaling multiplier for large-screen 4K TV viewing.
     * 1.0f on Full HD (1080p), 2.0f on 4K UHD.
     */
    public static float getScaleFactor(Context context) {
        return is4K(context) ? 2.0f : 1.0f;
    }

    /**
     * Screen horizontal side padding: 56dp for Full HD, 74dp for 4K UHD.
     */
    public static int getHorizontalPaddingDp(Context context) {
        return is4K(context) ? 74 : 56;
    }

    /**
     * Screen top padding: 26dp for Full HD, 36dp for 4K UHD.
     */
    public static int getTopPaddingDp(Context context) {
        return is4K(context) ? 36 : 26;
    }

    /**
     * Width in pixels for the Daily Prayer and Prayer Times panel (min(30vw, 1152px)).
     */
    public static int getIslamicPanelWidthPx(Context context) {
        if (context == null) return 576;
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        return Math.round(Math.min(dm.widthPixels * 0.30f, 1152f));
    }
}
