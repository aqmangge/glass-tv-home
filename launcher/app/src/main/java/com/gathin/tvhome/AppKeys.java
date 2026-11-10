package com.gathin.tvhome;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.KeyEvent;

/**
 * The remote's app buttons (Netflix, YouTube, Prime Video on the Xiaomi Mi Box S 2nd gen). On Google TV the stock
 * launcher opens their apps; with it disabled the buttons did nothing. They are matched by Linux scan code, which
 * is what the remote sends whatever the box's key layout makes of it, and handled in {@link InputKeyService}.
 *
 * Defaults are the Mi Box S 2nd gen remote's codes. Change one without a rebuild (shell has DUMP):
 *   adb shell am broadcast -a com.gathin.tvhome.SET_APP_KEY -p com.gathin.tvhome --ei scan 416 --es pkg com.netflix.ninja
 * An empty pkg removes the mapping; --ez reset true goes back to the defaults.
 */
final class AppKeys {
    private static final String TAG = "TvHome.AppKeys";
    private static final String PREFS = "app_keys";
    static final String ACTION_SET = "com.gathin.tvhome.SET_APP_KEY";

    /** Scan code → package, for the Mi Box S 2nd gen remote. */
    private static final int[] DEFAULT_SCANS = {416, 425, 207};
    private static final String[] DEFAULT_PKGS = {
            "com.netflix.ninja",                  // KEY_VIDEOPHONE: Netflix button
            "com.google.android.youtube.tv",      // KEY_PRESENTATION: YouTube button
            "com.amazon.amazonvideo.livingroom"}; // KEY_PLAY: Prime Video button

    private AppKeys() {
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static String packageFor(Context ctx, int scan) {
        if (scan <= 0) return null;
        SharedPreferences p = prefs(ctx);
        String key = "scan_" + scan;
        if (p.contains(key)) {
            String pkg = p.getString(key, "");
            return pkg.isEmpty() ? null : pkg;
        }
        for (int i = 0; i < DEFAULT_SCANS.length; i++) if (DEFAULT_SCANS[i] == scan) return DEFAULT_PKGS[i];
        return null;
    }

    /** True when {@code event} is an app button: it is consumed, and on release its app opens. */
    static boolean handle(Context ctx, KeyEvent event) {
        String pkg = packageFor(ctx, event.getScanCode());
        if (pkg == null) return false;
        if (event.getAction() == KeyEvent.ACTION_UP && event.getRepeatCount() == 0) {
            Log.i(TAG, "scan " + event.getScanCode() + " key " + event.getKeyCode() + " -> " + pkg);
            launch(ctx, pkg);
        }
        return true;
    }

    private static void launch(Context ctx, String pkg) {
        PackageManager pm = ctx.getPackageManager();
        Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (i == null) i = pm.getLaunchIntentForPackage(pkg);
        if (i == null) {
            Log.w(TAG, pkg + " is not installed or is disabled");
            return;
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            ctx.startActivity(i);
        } catch (ActivityNotFoundException | SecurityException e) {
            Log.w(TAG, "cannot open " + pkg, e);
        }
    }

    /** SET_APP_KEY from adb; the manifest requires DUMP, which the shell has and ordinary apps do not. */
    public static final class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !ACTION_SET.equals(intent.getAction())) return;
            SharedPreferences.Editor e = prefs(context).edit();
            if (intent.getBooleanExtra("reset", false)) {
                e.clear().apply();
                Log.i(TAG, "reset to defaults");
                return;
            }
            int scan = intent.getIntExtra("scan", -1);
            if (scan <= 0) return;
            String pkg = intent.getStringExtra("pkg");
            e.putString("scan_" + scan, pkg == null ? "" : pkg.trim()).apply();
            Log.i(TAG, "scan " + scan + " -> " + (pkg == null || pkg.trim().isEmpty() ? "(none)" : pkg.trim()));
        }
    }
}
