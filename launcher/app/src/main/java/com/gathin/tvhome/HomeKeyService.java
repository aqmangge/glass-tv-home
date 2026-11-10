package com.gathin.tvhome;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;

public class HomeKeyService extends AccessibilityService {

    private static final String TAG = "TvHome.HomeKey";
    private static HomeKeyService instance;
    private final Handler homeHandler = new Handler(Looper.getMainLooper());

    private String targetForceStopPackage = null;
    private final Queue<String> forceStopQueue = new LinkedList<>();
    private boolean isProcessingForceStop = false;
    private boolean isWaitingForConfirmDialog = false;
    private long lastHomePressTime = 0;

    private void openRecentAppsGlobal() {
        Intent recents = new Intent(this, RecentAppsActivity.class);
        recents.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        try {
            startActivity(recents);
        } catch (Exception ignored) {}
    }

    /** Scancodes of remote buttons that should open Settings (config.xml, settings_key_scancodes). */
    private int[] settingsScancodes = new int[0];

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        settingsScancodes = getResources().getIntArray(R.array.settings_key_scancodes);
        Log.i(TAG, "HomeKeyService connected");
    }

    @Override
    public void onDestroy() {
        if (instance == this) instance = null;
        super.onDestroy();
    }

    public static boolean sleepScreen() {
        if (instance != null) {
            Log.i(TAG, "Executing GLOBAL_ACTION_LOCK_SCREEN to turn off TV display");
            return instance.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
        }
        return false;
    }

    public static boolean restartTv(Context context) {
        if (instance != null) {
            Log.i(TAG, "Attempting TV restart via accessibility service");
            // Android 11+ (API 30+) provides GLOBAL_ACTION_REBOOT = 12
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                boolean done = instance.performGlobalAction(12);
                if (done) return true;
            }
            // Fallback: System power dialog which presents Restart
            boolean dialog = instance.performGlobalAction(GLOBAL_ACTION_POWER_DIALOG);
            if (dialog) return true;
        }
        // Fallback: system broadcast or shell reboot
        try {
            Intent intent = new Intent(Intent.ACTION_REBOOT);
            intent.putExtra("nowait", 1);
            intent.putExtra("interval", 1);
            intent.putExtra("window", 0);
            context.sendBroadcast(intent);
            return true;
        } catch (Exception e) {
            try {
                Runtime.getRuntime().exec("reboot");
                return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    public static boolean turnOffTv(Context context) {
        if (instance != null) {
            Log.i(TAG, "Attempting TV power off / sleep via accessibility service");
            boolean done = instance.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
            if (done) return true;
            boolean dialog = instance.performGlobalAction(GLOBAL_ACTION_POWER_DIALOG);
            if (dialog) return true;
        }
        try {
            Intent intent = new Intent("android.intent.action.ACTION_REQUEST_SHUTDOWN");
            intent.putExtra("android.intent.extra.KEY_CONFIRM", false);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Exception ignored) {}
        return false;
    }

    public static void forceStopApp(Context context, String pkg) {
        if (pkg == null || pkg.isEmpty() || pkg.equals(context.getPackageName())) return;
        
        // 1. Send MEDIA_STOP to release audio/video playback locks
        try {
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (am != null) {
                am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_STOP));
                am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_STOP));
            }
        } catch (Exception ignored) {}

        // 2. Kill background processes
        try {
            ActivityManager actMgr = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (actMgr != null) {
                actMgr.killBackgroundProcesses(pkg);
            }
        } catch (Exception ignored) {}

        // 3. Automated force-stop via accessibility if available
        if (instance != null) {
            instance.queueForceStop(pkg);
        }
    }

    public static void forceStopAll(Context context, List<String> packages) {
        if (packages == null) return;
        // Stop media and kill background for all
        try {
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (am != null) {
                am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_STOP));
                am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_STOP));
            }
            ActivityManager actMgr = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (actMgr != null) {
                for (String pkg : packages) {
                    actMgr.killBackgroundProcesses(pkg);
                }
            }
        } catch (Exception ignored) {}

        if (instance != null) {
            for (String pkg : packages) {
                if (!pkg.equals(context.getPackageName()) && !pkg.contains("com.android.tv.settings")) {
                    instance.queueForceStop(pkg);
                }
            }
        }
    }

    private synchronized void queueForceStop(String pkg) {
        forceStopQueue.offer(pkg);
        if (!isProcessingForceStop) {
            processNextForceStop();
        }
    }

    private synchronized void processNextForceStop() {
        if (forceStopQueue.isEmpty()) {
            isProcessingForceStop = false;
            isWaitingForConfirmDialog = false;
            targetForceStopPackage = null;
            return;
        }
        isProcessingForceStop = true;
        isWaitingForConfirmDialog = false;
        targetForceStopPackage = forceStopQueue.poll();
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + targetForceStopPackage));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);

            // Safety timeout: if window doesn't respond or already stopped within 400ms, proceed to next
            homeHandler.removeCallbacksAndMessages(null);
            homeHandler.postDelayed(() -> {
                if (isProcessingForceStop) {
                    isProcessingForceStop = false;
                    isWaitingForConfirmDialog = false;
                    targetForceStopPackage = null;
                    performGlobalAction(GLOBAL_ACTION_BACK);
                    homeHandler.postDelayed(this::processNextForceStop, 80);
                }
            }, 400);
        } catch (Exception e) {
            processNextForceStop();
        }
    }

    private boolean clickNodeOrAncestor(AccessibilityNodeInfo node) {
        if (node == null) return false;
        AccessibilityNodeInfo cur = node;
        for (int i = 0; i < 4; i++) {
            if (cur.isClickable() && cur.isEnabled()) {
                boolean res = cur.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                Log.d(TAG, "Clicked ancestor node: " + cur.getClassName() + " -> " + res);
                return res;
            }
            AccessibilityNodeInfo parent = cur.getParent();
            if (parent == null) break;
            cur = parent;
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!isProcessingForceStop || targetForceStopPackage == null) return;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        try {
            // Step 1: Check for "OK" confirmation button (if dialog is visible)
            List<AccessibilityNodeInfo> okNodes = root.findAccessibilityNodeInfosByText("OK");
            if (okNodes.isEmpty()) {
                okNodes = root.findAccessibilityNodeInfosByViewId("com.android.tv.settings:id/guidedactions_item_title");
            }
            if (okNodes.isEmpty()) {
                okNodes = root.findAccessibilityNodeInfosByViewId("android:id/button1");
            }

            for (AccessibilityNodeInfo ok : okNodes) {
                CharSequence text = ok.getText();
                if (text != null && text.toString().equalsIgnoreCase("OK")) {
                    Log.i(TAG, "Confirming Force Stop OK for: " + targetForceStopPackage);
                    clickNodeOrAncestor(ok);
                    isProcessingForceStop = false;
                    isWaitingForConfirmDialog = false;
                    targetForceStopPackage = null;
                    homeHandler.removeCallbacksAndMessages(null);
                    performGlobalAction(GLOBAL_ACTION_BACK);
                    homeHandler.postDelayed(this::processNextForceStop, 80);
                    return;
                }
            }

            if (isWaitingForConfirmDialog) return;

            // Step 2: Check for "Force stop" / "Paksa henti" button
            List<AccessibilityNodeInfo> forceStopButtons = root.findAccessibilityNodeInfosByText("Force stop");
            if (forceStopButtons.isEmpty()) {
                forceStopButtons = root.findAccessibilityNodeInfosByText("Paksa henti");
            }
            if (forceStopButtons.isEmpty()) {
                forceStopButtons = root.findAccessibilityNodeInfosByViewId("com.android.tv.settings:id/force_stop");
            }
            if (forceStopButtons.isEmpty()) {
                forceStopButtons = root.findAccessibilityNodeInfosByViewId("com.android.settings:id/force_stop_button");
            }

            for (AccessibilityNodeInfo btn : forceStopButtons) {
                CharSequence text = btn.getText();
                String t = text != null ? text.toString().toLowerCase(Locale.ROOT) : "";
                if (t.contains("force stop") || t.contains("paksa henti")) {
                    if (btn.isEnabled()) {
                        Log.i(TAG, "Clicking Force Stop button for: " + targetForceStopPackage);
                        isWaitingForConfirmDialog = true;
                        clickNodeOrAncestor(btn);
                        return;
                    } else {
                        // Already force stopped
                        Log.i(TAG, "Force Stop button already disabled for: " + targetForceStopPackage);
                        isProcessingForceStop = false;
                        isWaitingForConfirmDialog = false;
                        targetForceStopPackage = null;
                        homeHandler.removeCallbacksAndMessages(null);
                        performGlobalAction(GLOBAL_ACTION_BACK);
                        homeHandler.postDelayed(this::processNextForceStop, 80);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error during accessibility force stop", e);
        }
    }

    private boolean isHomeLongPressed = false;
    private final Runnable longPressHomeRunnable = new Runnable() {
        @Override
        public void run() {
            isHomeLongPressed = true;
            Log.i(TAG, "Hold Home button detected -> Opening Settings Popup globally");
            openSettingsPopupGlobal();
        }
    };

    public static void openSettingsPopupGlobal() {
        if (instance == null) return;
        if (MainActivity.isForeground()) {
            MainActivity.openSettingsFromGlobal();
        } else {
            Intent intent = new Intent(instance, MainActivity.class)
                    .putExtra("open_settings", true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            try {
                instance.startActivity(intent);
            } catch (Exception e) {
                Log.w(TAG, "Failed to launch MainActivity for settings", e);
            }
        }
    }

    private boolean isSettingsKey(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_SETTINGS) return true;
        for (int code : settingsScancodes) if (event.getScanCode() == code) return true;
        return false;
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (isSettingsKey(event)) {
            if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) {
                startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
            return true;
        }

        int keyCode = event.getKeyCode();
        if (keyCode == KeyEvent.KEYCODE_APP_SWITCH) {
            if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) {
                openRecentAppsGlobal();
            }
            return true;
        }

        if (keyCode != KeyEvent.KEYCODE_ALL_APPS && keyCode != KeyEvent.KEYCODE_HOME) return false;

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (event.isLongPress() || (event.getFlags() & KeyEvent.FLAG_LONG_PRESS) != 0 || event.getRepeatCount() > 0) {
                if (!isHomeLongPressed) {
                    isHomeLongPressed = true;
                    homeHandler.removeCallbacks(longPressHomeRunnable);
                    openSettingsPopupGlobal();
                }
                return true;
            }
            if (event.getRepeatCount() == 0) {
                isHomeLongPressed = false;
                homeHandler.removeCallbacks(longPressHomeRunnable);
                homeHandler.postDelayed(longPressHomeRunnable, 450);
            }
            return true;
        }

        if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) {
            homeHandler.removeCallbacks(longPressHomeRunnable);
            if (isHomeLongPressed) {
                isHomeLongPressed = false;
                return true;
            }

            long now = SystemClock.uptimeMillis();
            if (now - lastHomePressTime < 380) {
                // Double tap Home: open Task Manager / Recent Apps (Skyworth, Xiaomi, Hisense)
                lastHomePressTime = 0;
                openRecentAppsGlobal();
                return true;
            }
            lastHomePressTime = now;

            // Single Home press: return to Home
            if (RecentAppsActivity.isForeground()) {
                RecentAppsActivity.dismissFromGlobal();
            } else {
                Intent home = new Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_HOME)
                        .setPackage(getPackageName())
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                try {
                    startActivity(home);
                } catch (Exception ignored) {}
            }
            return true;
        }
        return true;
    }

    @Override
    public void onInterrupt() {
    }
}
