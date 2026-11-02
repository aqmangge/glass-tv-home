package com.gathin.tvhome;

import android.graphics.Typeface;

/**
 * Backward compatibility wrapper forwarding user profile selection to the modern
 * Netflix / Nuvio styled "Who's watching?" screen (WhoIsWatchingDialog).
 */
public final class UserSwitchDialog {

    private UserSwitchDialog() {}

    public static void show(MainActivity activity) {
        WhoIsWatchingDialog.show(activity);
    }

    public static void showAddProfileDialog(MainActivity activity, float density, Typeface font, Runnable onAdded) {
        AddProfileDialog.show(activity, p -> {
            if (onAdded != null) onAdded.run();
        });
    }
}
