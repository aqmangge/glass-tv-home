package com.gathin.tvhome;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * Device capability detector and performance profiler.
 * Automatically identifies low-spec TV boxes (<= 2GB RAM) and ultra low-spec TV boxes (<= 1GB RAM, low memory class, weak 4-core SoC)
 * and provides optimization flags to ensure 60fps smooth UI navigation matching Version 3.
 */
public final class DevicePerformance {

    private static final String PREF_NAME = "theme_prefs";
    private static final String KEY_PERF_MODE = "pref_low_spec_optimization";

    private static Boolean sAutoDetectedLowSpec = null;
    private static Boolean sAutoDetectedUltraLowRam = null;

    private DevicePerformance() {}

    /**
     * Determines whether low-spec optimizations should be active.
     * Honors explicit user preference if set; otherwise falls back to hardware auto-detection.
     */
    public static boolean isLowSpec(Context context) {
        if (context == null) return false;
        try {
            SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            if (sp.contains(KEY_PERF_MODE)) {
                return sp.getBoolean(KEY_PERF_MODE, false);
            }
        } catch (Exception ignored) {}

        return isHardwareLowSpec(context);
    }

    /**
     * Specifically identifies ultra-low RAM devices (<= 1.2GB total RAM, such as 1GB TV boxes / sticks:
     * Tanix, X96 Mini, MXQ Pro 4K, H96, Allwinner H313/H616, Rockchip RK3328).
     */
    public static boolean isUltraLowRam(Context context) {
        if (sAutoDetectedUltraLowRam != null) {
            return sAutoDetectedUltraLowRam;
        }
        if (context == null) return false;

        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                am.getMemoryInfo(mi);
                // Devices with <= 1.2GB RAM (1GB nominal RAM often shows ~950MB to 1050MB total)
                if (mi.totalMem <= 1288490188L) {
                    sAutoDetectedUltraLowRam = true;
                    return true;
                }
                if (am.getMemoryClass() <= 128) {
                    sAutoDetectedUltraLowRam = true;
                    return true;
                }
            }
        } catch (Exception ignored) {}

        sAutoDetectedUltraLowRam = false;
        return false;
    }

    /**
     * Hardware auto-detection based on RAM size, low RAM flags, and memory class.
     */
    public static boolean isHardwareLowSpec(Context context) {
        if (sAutoDetectedLowSpec != null) {
            return sAutoDetectedLowSpec;
        }
        if (context == null) return false;

        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                if (am.isLowRamDevice()) {
                    sAutoDetectedLowSpec = true;
                    return true;
                }
                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                am.getMemoryInfo(mi);
                // TV boxes with <= 2.2GB RAM (covers 1GB and 2GB TV sticks/boxes like Mi Box, Firestick, Chromecast HD)
                if (mi.totalMem <= 2362232012L) {
                    sAutoDetectedLowSpec = true;
                    return true;
                }
                // Low memory class per app process
                if (am.getMemoryClass() <= 192) {
                    sAutoDetectedLowSpec = true;
                    return true;
                }
            }

            int cores = Runtime.getRuntime().availableProcessors();
            if (cores <= 4 && am != null && am.getMemoryClass() <= 256) {
                sAutoDetectedLowSpec = true;
                return true;
            }
        } catch (Exception ignored) {}

        sAutoDetectedLowSpec = false;
        return false;
    }

    public static long getTotalRamMb(Context context) {
        if (context == null) return 0;
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                am.getMemoryInfo(mi);
                return mi.totalMem / (1024 * 1024);
            }
        } catch (Exception ignored) {}
        return 0;
    }

    public static long getAvailableRamMb(Context context) {
        if (context == null) return 0;
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                am.getMemoryInfo(mi);
                return mi.availMem / (1024 * 1024);
            }
        } catch (Exception ignored) {}
        return 0;
    }

    public static int getRamUsagePercent(Context context) {
        long total = getTotalRamMb(context);
        long avail = getAvailableRamMb(context);
        if (total <= 0) return 0;
        long used = total - avail;
        return (int) Math.max(0, Math.min(100, (used * 100) / total));
    }

    /**
     * Proactively releases caches and invokes lightweight memory trim for 1GB RAM devices.
     */
    public static void applyAggressiveMemoryTrims(Context context) {
        if (context == null) return;
        try {
            // Suggest GC when free memory is below 150MB
            long freeMb = getAvailableRamMb(context);
            if (freeMb > 0 && freeMb < 150) {
                System.gc();
            }
        } catch (Exception ignored) {}
    }

    public static void setOptimizationOverride(Context context, boolean enabled) {
        if (context == null) return;
        try {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
                    .putBoolean(KEY_PERF_MODE, enabled)
                    .apply();
        } catch (Exception ignored) {}
    }

    public static boolean isUserOverrideSet(Context context) {
        if (context == null) return false;
        try {
            return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).contains(KEY_PERF_MODE);
        } catch (Exception ignored) {
            return false;
        }
    }
}
