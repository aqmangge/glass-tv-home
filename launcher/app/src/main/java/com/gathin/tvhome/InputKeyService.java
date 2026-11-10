package com.gathin.tvhome;

import android.accessibilityservice.AccessibilityService;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/**
 * Maps the remote's Input button to the system input switcher, from any app. Remotes such as the Chromecast voice
 * remote send KEYCODE_TV for it; on Google TV the stock launcher handles that key, so with the stock launcher
 * disabled the button does nothing. Only KEYCODE_TV is consumed; every other key passes through untouched.
 * Opt-in: INPUT_KEY_SERVICE=1 build-and-install.sh (or -InputKeyService on Windows) enables it.
 */
public class InputKeyService extends AccessibilityService {

    private static final String ACTION_VIEW_INPUTS = "com.android.tv.action.VIEW_INPUTS";

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (AppKeys.handle(this, event)) return true;
        if (event.getKeyCode() != KeyEvent.KEYCODE_TV) return false;
        if (event.getAction() == KeyEvent.ACTION_UP) {
            try {
                startActivity(new Intent(ACTION_VIEW_INPUTS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (ActivityNotFoundException ignored) {
            }
        }
        return true;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Which app is in front, for the azan: it stays quiet over Live TV and the HDMI inputs.
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || event.getPackageName() == null) return;
        String pkg = event.getPackageName().toString();
        if (pkg.equals("com.android.systemui") || pkg.startsWith("com.google.android.inputmethod")) return;
        front = pkg;
    }

    /** The app last brought to the front (as this service saw it); null before the first change. */
    private static volatile String front;
    /** Apps that show Live TV or an HDMI input on Android TV sets. */
    private static final java.util.Set<String> LIVE_TV = new java.util.HashSet<>(java.util.Arrays.asList(
            "com.android.tv", "com.google.android.tv", "com.mediatek.wwtv.tvcenter", "com.sony.dtv.tvx",
            "com.sony.dtv.tvinput", "com.realtek.tv", "com.tcl.tv", "com.xiaomi.mitv.tvplayer"));

    static String front() {
        return front;
    }

    static boolean liveTvInFront() {
        String f = front;
        return f != null && LIVE_TV.contains(f);
    }

    @Override
    public void onInterrupt() {
    }
}
