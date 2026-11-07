package com.gathin.tvhome;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

/**
 * Manages Child Mode, PIN security, App Lock, and Hidden Apps for Glass TV Home.
 */
public final class ChildModeManager {
    private static final String PREFS_NAME = "child_mode_prefs";
    private static final String KEY_ENABLED = "child_mode_enabled";
    private static final String KEY_PIN = "security_pin";
    private static final String KEY_LOCKED_APPS = "locked_apps";
    private static final String KEY_HIDDEN_APPS = "hidden_apps";

    public interface PinCallback {
        void onSuccess();
        void onCancelled();
    }

    private ChildModeManager() {}

    public static boolean isChildModeEnabled(Context ctx) {
        if (ctx == null) return false;
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_ENABLED, false);
    }

    public static void setChildModeEnabled(Context ctx, boolean enabled) {
        if (ctx == null) return;
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_ENABLED, enabled)
                .apply();
    }

    public static String getPin(Context ctx) {
        if (ctx == null) return "0000";
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_PIN, "0000");
    }

    public static void setPin(Context ctx, String pin) {
        if (ctx == null || pin == null || pin.length() != 4) return;
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_PIN, pin)
                .apply();
    }

    public static boolean verifyPin(Context ctx, String entered) {
        if (ctx == null || entered == null) return false;
        return entered.equals(getPin(ctx));
    }

    public static boolean isAppLocked(Context ctx, String pkg) {
        if (ctx == null || pkg == null) return false;
        if (!isChildModeEnabled(ctx)) return false;
        Set<String> set = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getStringSet(KEY_LOCKED_APPS, null);
        return set != null && set.contains(pkg);
    }

    public static void setAppLocked(Context ctx, String pkg, boolean locked) {
        if (ctx == null || pkg == null) return;
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> set = new HashSet<>(sp.getStringSet(KEY_LOCKED_APPS, new HashSet<>()));
        if (locked) set.add(pkg);
        else set.remove(pkg);
        sp.edit().putStringSet(KEY_LOCKED_APPS, set).apply();
    }

    public static Set<String> getLockedApps(Context ctx) {
        if (ctx == null) return new HashSet<>();
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getStringSet(KEY_LOCKED_APPS, new HashSet<>());
    }

    public static boolean isAppHidden(Context ctx, String pkg) {
        if (ctx == null || pkg == null) return false;
        Set<String> set = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getStringSet(KEY_HIDDEN_APPS, null);
        return set != null && set.contains(pkg);
    }

    public static void setAppHidden(Context ctx, String pkg, boolean hidden) {
        if (ctx == null || pkg == null) return;
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> set = new HashSet<>(sp.getStringSet(KEY_HIDDEN_APPS, new HashSet<>()));
        if (hidden) set.add(pkg);
        else set.remove(pkg);
        sp.edit().putStringSet(KEY_HIDDEN_APPS, set).apply();
    }

    public static Set<String> getHiddenApps(Context ctx) {
        if (ctx == null) return new HashSet<>();
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getStringSet(KEY_HIDDEN_APPS, new HashSet<>());
    }

    /**
     * Shows a TV-friendly 4-digit PIN dialog with a numeric keypad operable by remote control.
     */
    public static void promptPin(Context ctx, String title, String subtitle, PinCallback callback) {
        float density = ctx.getResources().getDisplayMetrics().density;
        Typeface outfit = ctx.getResources().getFont(R.font.inter);

        AlertDialog[] dialogHolder = new AlertDialog[1];
        StringBuilder pinBuilder = new StringBuilder();

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding((int) (28 * density), (int) (24 * density), (int) (28 * density), (int) (24 * density));

        // Background
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xF0181A22);
        bg.setCornerRadius(24 * density);
        bg.setStroke((int) (1.5f * density), 0x44FFFFFF);
        root.setBackground(bg);

        // Icon
        ImageView iconView = new ImageView(ctx);
        iconView.setImageResource(R.drawable.ic_lock);
        iconView.setColorFilter(0xFFF59E0B);
        LinearLayout.LayoutParams icLp = new LinearLayout.LayoutParams((int) (38 * density), (int) (38 * density));
        icLp.bottomMargin = (int) (10 * density);
        root.addView(iconView, icLp);

        // Title
        TextView tvTitle = new TextView(ctx);
        tvTitle.setText(title != null ? title : "Keselamatan PIN");
        tvTitle.setTextSize(19);
        tvTitle.setTextColor(0xFFFFFFFF);
        tvTitle.setTypeface(outfit, Typeface.BOLD);
        tvTitle.setGravity(Gravity.CENTER);
        root.addView(tvTitle);

        // Subtitle
        TextView tvSub = new TextView(ctx);
        tvSub.setText(subtitle != null ? subtitle : "Masukkan PIN 4-digit untuk meneruskan");
        tvSub.setTextSize(13);
        tvSub.setTextColor(0xB3FFFFFF);
        tvSub.setTypeface(outfit, Typeface.NORMAL);
        tvSub.setGravity(Gravity.CENTER);
        tvSub.setPadding(0, (int) (4 * density), 0, (int) (16 * density));
        root.addView(tvSub);

        // PIN Dots Display
        LinearLayout dotsRow = new LinearLayout(ctx);
        dotsRow.setOrientation(LinearLayout.HORIZONTAL);
        dotsRow.setGravity(Gravity.CENTER);
        dotsRow.setPadding(0, 0, 0, (int) (20 * density));

        TextView[] dots = new TextView[4];
        for (int i = 0; i < 4; i++) {
            TextView dot = new TextView(ctx);
            dot.setText("○");
            dot.setTextSize(26);
            dot.setTextColor(0x88FFFFFF);
            dot.setPadding((int) (10 * density), 0, (int) (10 * density), 0);
            dots[i] = dot;
            dotsRow.addView(dot);
        }
        root.addView(dotsRow);

        Runnable updateDots = () -> {
            for (int i = 0; i < 4; i++) {
                if (i < pinBuilder.length()) {
                    dots[i].setText("●");
                    dots[i].setTextColor(0xFFF59E0B);
                } else {
                    dots[i].setText("○");
                    dots[i].setTextColor(0x88FFFFFF);
                }
            }
        };

        // Keypad Grid (3 columns: 1-9, then Del, 0, OK)
        GridLayout grid = new GridLayout(ctx);
        grid.setColumnCount(3);
        grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);

        String[] keys = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "⌫", "0", "OK"};
        View firstKey = null;

        for (String k : keys) {
            Button btn = new Button(ctx);
            btn.setText(k);
            btn.setTextSize(18);
            btn.setTypeface(outfit, Typeface.BOLD);
            btn.setTextColor(0xFFFFFFFF);
            btn.setFocusable(true);
            btn.setClickable(true);

            StateListDrawable sld = new StateListDrawable();
            GradientDrawable f = new GradientDrawable();
            f.setColor(k.equals("OK") ? 0xFF10B981 : 0xFF38BDF8);
            f.setCornerRadius(14 * density);
            GradientDrawable u = new GradientDrawable();
            u.setColor(0x22FFFFFF);
            u.setCornerRadius(14 * density);
            u.setStroke((int) (1 * density), 0x33FFFFFF);
            sld.addState(new int[]{android.R.attr.state_focused}, f);
            sld.addState(new int[]{}, u);
            btn.setBackground(sld);

            btn.setOnFocusChangeListener((v, hasFocus) -> {
                btn.setTextColor(hasFocus ? 0xFF000000 : 0xFFFFFFFF);
                v.animate().scaleX(hasFocus ? 1.08f : 1.0f).scaleY(hasFocus ? 1.08f : 1.0f).setDuration(120).start();
            });

            btn.setOnClickListener(v -> {
                SoundEffects.playSelect();
                if (k.equals("⌫")) {
                    if (pinBuilder.length() > 0) {
                        pinBuilder.deleteCharAt(pinBuilder.length() - 1);
                        updateDots.run();
                    }
                } else if (k.equals("OK")) {
                    if (pinBuilder.length() == 4) {
                        if (verifyPin(ctx, pinBuilder.toString())) {
                            if (dialogHolder[0] != null) dialogHolder[0].dismiss();
                            if (callback != null) callback.onSuccess();
                        } else {
                            Toast.makeText(ctx, "❌ PIN salah. Sila cuba lagi.", Toast.LENGTH_SHORT).show();
                            pinBuilder.setLength(0);
                            updateDots.run();
                        }
                    } else {
                        Toast.makeText(ctx, "Sila masukkan 4 digit PIN", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    if (pinBuilder.length() < 4) {
                        pinBuilder.append(k);
                        updateDots.run();
                        if (pinBuilder.length() == 4) {
                            // Auto-verify on 4th digit
                            if (verifyPin(ctx, pinBuilder.toString())) {
                                if (dialogHolder[0] != null) dialogHolder[0].dismiss();
                                if (callback != null) callback.onSuccess();
                            } else {
                                Toast.makeText(ctx, "❌ PIN salah. Sila cuba lagi.", Toast.LENGTH_SHORT).show();
                                pinBuilder.setLength(0);
                                updateDots.run();
                            }
                        }
                    }
                }
            });

            GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
            glp.width = (int) (68 * density);
            glp.height = (int) (52 * density);
            glp.setMargins((int) (6 * density), (int) (6 * density), (int) (6 * density), (int) (6 * density));
            grid.addView(btn, glp);

            if (firstKey == null) firstKey = btn;
        }

        root.addView(grid);

        // Cancel Button
        Button btnCancel = new Button(ctx);
        btnCancel.setText("Batal");
        btnCancel.setTextSize(13);
        btnCancel.setTypeface(outfit, Typeface.NORMAL);
        btnCancel.setTextColor(0xAAFFFFFF);
        btnCancel.setBackgroundColor(Color.TRANSPARENT);
        btnCancel.setFocusable(true);
        btnCancel.setPadding(0, (int) (8 * density), 0, 0);
        btnCancel.setOnFocusChangeListener((v, hasFocus) -> {
            btnCancel.setTextColor(hasFocus ? 0xFFFFFFFF : 0xAAFFFFFF);
        });
        btnCancel.setOnClickListener(v -> {
            if (dialogHolder[0] != null) dialogHolder[0].dismiss();
            if (callback != null) callback.onCancelled();
        });
        root.addView(btnCancel);

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx, R.style.Theme_TvHome_GlassDialog);
        builder.setView(root);
        AlertDialog dialog = builder.create();
        dialogHolder[0] = dialog;
        dialog.setOnCancelListener(d -> {
            if (callback != null) callback.onCancelled();
        });
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int w = (int) (340 * density);
            dialog.getWindow().setLayout(w, LinearLayout.LayoutParams.WRAP_CONTENT);
        }

        if (firstKey != null) firstKey.requestFocus();
    }
}
