package com.gathin.tvhome;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * Modern 16:9 Glass Dialog for TV Box Security, Malware (XCPE/Badbox) Inspection & 1GB RAM Optimization.
 * Follows the ReactDrops Soap Bubble thin-film aesthetic matching Glass TV Home.
 */
public final class BoxSecurityDialog {

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public static void show(MainActivity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        float density = activity.getResources().getDisplayMetrics().density;
        Typeface loadedBold = null;
        try {
            loadedBold = activity.getResources().getFont(R.font.inter);
        } catch (Exception ignored) {}
        final Typeface fontBold = loadedBold;

        final AlertDialog[] dialogHolder = new AlertDialog[1];

        // Root container (16:9 Glass Card)
        FrameLayout rootFrame = new FrameLayout(activity);
        rootFrame.setFocusable(false);

        int modalW = Math.round(660 * density);
        int modalH = Math.round(380 * density);

        FrameLayout.LayoutParams modalLp = new FrameLayout.LayoutParams(modalW, modalH);
        modalLp.gravity = Gravity.CENTER;

        LinearLayout modalCard = new LinearLayout(activity);
        modalCard.setOrientation(LinearLayout.VERTICAL);
        modalCard.setLayoutParams(modalLp);
        modalCard.setPadding(Math.round(24 * density), Math.round(20 * density), Math.round(24 * density), Math.round(18 * density));

        // ReactDrops Soap Bubble glass styling
        GradientDrawable modalBg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xF6111827, 0xF60B0F19}
        );
        modalBg.setCornerRadius(24 * density);
        modalBg.setStroke(Math.round(1.5f * density), 0x4D67E8F9); // Iridescent glass rim
        modalCard.setBackground(modalBg);
        modalCard.setClipToOutline(true);

        // --- 1. HEADER ROW ---
        LinearLayout headerRow = new LinearLayout(activity);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Shield Badge Icon
        FrameLayout shieldBadge = new FrameLayout(activity);
        int badgeSize = Math.round(36 * density);
        LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(badgeSize, badgeSize);
        shieldBadge.setLayoutParams(badgeLp);

        GradientDrawable badgeGd = new GradientDrawable();
        badgeGd.setShape(GradientDrawable.OVAL);
        badgeGd.setColor(0x2E67E8F9);
        badgeGd.setStroke(Math.round(1.5f * density), 0x8067E8F9);
        shieldBadge.setBackground(badgeGd);

        TextView tvShieldIcon = new TextView(activity);
        tvShieldIcon.setText("🛡️");
        tvShieldIcon.setTextSize(16);
        tvShieldIcon.setGravity(Gravity.CENTER);
        shieldBadge.addView(tvShieldIcon, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        headerRow.addView(shieldBadge);

        // Titles
        LinearLayout titleCol = new LinearLayout(activity);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tColLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tColLp.setMarginStart(Math.round(12 * density));
        titleCol.setLayoutParams(tColLp);

        TextView tvTitle = new TextView(activity);
        tvTitle.setText("PENGIMBAS KESELAMATAN & MALWARE BOX");
        tvTitle.setTextSize(14.5f);
        tvTitle.setTextColor(0xFFFFFFFF);
        if (fontBold != null) tvTitle.setTypeface(fontBold, Typeface.BOLD);
        tvTitle.setLetterSpacing(0.04f);
        titleCol.addView(tvTitle);

        TextView tvSub = new TextView(activity);
        tvSub.setText("Semak & Bersihkan Malware Firmware (XCPE, BadBox, Root Backdoor) & Optimumkan 1GB RAM");
        tvSub.setTextSize(11);
        tvSub.setTextColor(0xFF94A3B8);
        tvSub.setPadding(0, Math.round(2 * density), 0, 0);
        titleCol.addView(tvSub);

        headerRow.addView(titleCol);

        // Close Button
        TextView btnClose = new TextView(activity);
        btnClose.setText("✕");
        btnClose.setTextSize(15);
        btnClose.setTextColor(0xFF94A3B8);
        btnClose.setGravity(Gravity.CENTER);
        btnClose.setFocusable(true);
        btnClose.setClickable(true);
        int closeDim = Math.round(32 * density);
        LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(closeDim, closeDim);
        btnClose.setLayoutParams(closeLp);

        GradientDrawable closeBg = new GradientDrawable();
        closeBg.setShape(GradientDrawable.OVAL);
        closeBg.setColor(0x22FFFFFF);
        btnClose.setBackground(closeBg);

        btnClose.setOnFocusChangeListener((v, hasFocus) -> {
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(hasFocus ? 0x44EF4444 : 0x22FFFFFF);
            g.setStroke(hasFocus ? Math.round(2f * density) : 0, hasFocus ? 0xFFEF4444 : 0);
            v.setBackground(g);
            btnClose.setTextColor(hasFocus ? 0xFFFFFFFF : 0xFF94A3B8);
            if (hasFocus) SoundEffects.playNav();
        });
        btnClose.setOnClickListener(v -> {
            SoundEffects.playSelect();
            if (dialogHolder[0] != null) dialogHolder[0].dismiss();
        });
        headerRow.addView(btnClose);

        modalCard.addView(headerRow);

        // --- 2. SUMMARY METRICS ROW (3 Glass Pills) ---
        LinearLayout metricsRow = new LinearLayout(activity);
        metricsRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams mRowLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mRowLp.topMargin = Math.round(14 * density);
        mRowLp.bottomMargin = Math.round(12 * density);
        metricsRow.setLayoutParams(mRowLp);

        final LinearLayout cardStatus = buildSummaryCard(activity, density, fontBold, "Status Keselamatan", "Memeriksa...", 0xFF38BDF8);
        final LinearLayout cardRoot = buildSummaryCard(activity, density, fontBold, "Integriti & Root", "Memeriksa...", 0xFF94A3B8);
        final LinearLayout cardRam = buildSummaryCard(activity, density, fontBold, "Profil Kelancaran TV", "Memeriksa...", 0xFF34D399);

        metricsRow.addView(cardStatus);
        metricsRow.addView(cardRoot);
        metricsRow.addView(cardRam);
        modalCard.addView(metricsRow);

        // --- 3. DYNAMIC CONTENT SCROLLER ---
        ScrollView contentScroll = new ScrollView(activity);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        contentScroll.setLayoutParams(scrollLp);
        contentScroll.setVerticalScrollBarEnabled(false);

        LinearLayout contentCol = new LinearLayout(activity);
        contentCol.setOrientation(LinearLayout.VERTICAL);
        contentScroll.addView(contentCol, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        modalCard.addView(contentScroll);

        // --- 4. ACTION FOOTER ROW ---
        LinearLayout footerRow = new LinearLayout(activity);
        footerRow.setOrientation(LinearLayout.HORIZONTAL);
        footerRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams footLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        footLp.topMargin = Math.round(10 * density);
        footerRow.setLayoutParams(footLp);

        TextView btnRescan = buildFooterButton(activity, density, fontBold, "🔍 Imbas Semula (Deep Scan)", 0xFF0284C7);
        TextView btnOptimizeRam = buildFooterButton(activity, density, fontBold, "🚀 Optimumkan 1GB RAM", 0xFF059669);
        TextView btnDismiss = buildFooterButton(activity, density, fontBold, "Tutup", 0xFF374151);

        footerRow.addView(btnRescan);
        footerRow.addView(btnOptimizeRam);
        footerRow.addView(btnDismiss);
        modalCard.addView(footerRow);

        rootFrame.addView(modalCard);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.Theme_TvHome_GlassDialog);
        builder.setView(rootFrame);
        AlertDialog dialog = builder.create();
        dialogHolder[0] = dialog;

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams wlp = new WindowManager.LayoutParams();
            wlp.copyFrom(window.getAttributes());
            wlp.width = WindowManager.LayoutParams.MATCH_PARENT;
            wlp.height = WindowManager.LayoutParams.MATCH_PARENT;
            wlp.gravity = Gravity.CENTER;
            window.setAttributes(wlp);
        }

        // Runner to execute scan and render UI
        Runnable runScan = () -> {
            updateCardValue(cardStatus, "⏳ Sedang Mengimbas...", 0xFF38BDF8);
            contentCol.removeAllViews();

            TextView tvScanning = new TextView(activity);
            tvScanning.setText("⏳ Sedang mengimbas pakej firmware, fail sistem /system/app, port proksi dan akses root...");
            tvScanning.setTextColor(0xFF94A3B8);
            tvScanning.setTextSize(12);
            tvScanning.setGravity(Gravity.CENTER);
            tvScanning.setPadding(0, Math.round(24 * density), 0, Math.round(24 * density));
            contentCol.addView(tvScanning);

            BoxSecurityScanner.scanAsync(activity, result -> {
                // Update Summary Cards
                if (result.isClean()) {
                    updateCardValue(cardStatus, "✅ BERSIH & SELAMAT", 0xFF4ADE80);
                } else {
                    updateCardValue(cardStatus, "⚠️ " + result.threatCount() + " ANCAMAN DIKESAN!", 0xFFF87171);
                }

                String rootText;
                int rootColor;
                if (result.hasRoot) {
                    rootText = "🔑 Root Kilang: Aktif (su)";
                    rootColor = 0xFFFBBF24; // Amber
                } else {
                    rootText = "🔒 Tiada Akses Root";
                    rootColor = 0xFF94A3B8;
                }
                if (result.isTestKeysRom) {
                    rootText += " · Test-Keys";
                }
                updateCardValue(cardRoot, rootText, rootColor);

                String ramText;
                if (result.isUltraLowRamBox) {
                    ramText = "⚡ 1GB RAM · Mod Ultra-Lancar (" + result.freeRamMb + "MB Bebas)";
                } else {
                    ramText = "⚡ " + (result.totalRamMb > 0 ? (result.totalRamMb / 1024 + 1) + "GB" : "Standard") + " RAM (" + result.freeRamMb + "MB Bebas)";
                }
                updateCardValue(cardRam, ramText, 0xFF34D399);

                // Populate Content
                contentCol.removeAllViews();
                if (result.isClean()) {
                    renderCleanReport(activity, contentCol, density, fontBold);
                } else {
                    renderThreatsList(activity, contentCol, density, fontBold, result.threats, result.hasRoot, () -> {
                        // Re-run scan on remediation
                        MAIN_HANDLER.post(() -> btnRescan.performClick());
                    });
                }
            });
        };

        btnRescan.setOnClickListener(v -> {
            SoundEffects.playSelect();
            runScan.run();
        });

        btnOptimizeRam.setOnClickListener(v -> {
            SoundEffects.playSelect();
            DevicePerformance.applyAggressiveMemoryTrims(activity);
            CacheCleaner.cleanNow(activity, null);
            Toast.makeText(activity, "✨ Memori 1GB RAM dioptimumkan! Cache dibebaskan untuk navigasi TV 60fps.", Toast.LENGTH_SHORT).show();
            // Re-read memory status
            long freeMb = DevicePerformance.getAvailableRamMb(activity);
            updateCardValue(cardRam, "⚡ " + freeMb + "MB Bebas · Memori Dioptimumkan", 0xFF34D399);
        });

        btnDismiss.setOnClickListener(v -> {
            SoundEffects.playSelect();
            dialog.dismiss();
        });

        dialog.show();

        // Initial scan
        runScan.run();
        btnRescan.requestFocus();
    }

    private static LinearLayout buildSummaryCard(Context context, float density, Typeface fontBold, String title, String value, int valColor) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMarginEnd(Math.round(8 * density));
        card.setLayoutParams(lp);
        card.setPadding(Math.round(10 * density), Math.round(8 * density), Math.round(10 * density), Math.round(8 * density));

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(0x281F2937);
        gd.setCornerRadius(12 * density);
        gd.setStroke(Math.round(1 * density), 0x2E67E8F9);
        card.setBackground(gd);

        TextView tvTitle = new TextView(context);
        tvTitle.setText(title);
        tvTitle.setTextSize(9.5f);
        tvTitle.setTextColor(0xFF94A3B8);
        tvTitle.setAllCaps(true);
        tvTitle.setLetterSpacing(0.06f);
        card.addView(tvTitle);

        TextView tvVal = new TextView(context);
        tvVal.setTag("val");
        tvVal.setText(value);
        tvVal.setTextSize(11.5f);
        tvVal.setTextColor(valColor);
        if (fontBold != null) tvVal.setTypeface(fontBold, Typeface.BOLD);
        tvVal.setPadding(0, Math.round(2 * density), 0, 0);
        tvVal.setSingleLine(true);
        tvVal.setEllipsize(android.text.TextUtils.TruncateAt.END);
        card.addView(tvVal);

        return card;
    }

    private static void updateCardValue(LinearLayout card, String value, int valColor) {
        if (card == null) return;
        TextView tv = card.findViewWithTag("val");
        if (tv != null) {
            tv.setText(value);
            tv.setTextColor(valColor);
        }
    }

    private static TextView buildFooterButton(Context context, float density, Typeface fontBold, String text, int baseColor) {
        TextView btn = new TextView(context);
        btn.setText(text);
        btn.setTextSize(11.5f);
        btn.setTextColor(0xFFFFFFFF);
        if (fontBold != null) btn.setTypeface(fontBold, Typeface.BOLD);
        btn.setGravity(Gravity.CENTER);
        btn.setFocusable(true);
        btn.setClickable(true);
        btn.setPadding(Math.round(14 * density), Math.round(8 * density), Math.round(14 * density), Math.round(8 * density));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(Math.round(8 * density));
        btn.setLayoutParams(lp);

        GradientDrawable defaultBg = new GradientDrawable();
        defaultBg.setColor(baseColor);
        defaultBg.setCornerRadius(10 * density);
        btn.setBackground(defaultBg);

        btn.setOnFocusChangeListener((v, hasFocus) -> {
            GradientDrawable g = new GradientDrawable();
            g.setColor(hasFocus ? 0xFF0284C7 : baseColor);
            g.setCornerRadius(10 * density);
            g.setStroke(hasFocus ? Math.round(2f * density) : 0, 0xFF67E8F9);
            v.setBackground(g);
            v.animate()
                    .scaleX(hasFocus ? 1.05f : 1.0f)
                    .scaleY(hasFocus ? 1.05f : 1.0f)
                    .setDuration(120)
                    .start();
            if (hasFocus) SoundEffects.playNav();
        });

        return btn;
    }

    private static void renderCleanReport(Context context, LinearLayout container, float density, Typeface fontBold) {
        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Math.round(16 * density), Math.round(14 * density), Math.round(16 * density), Math.round(14 * density));

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(0x1A059669); // Subtle emerald glass
        gd.setCornerRadius(14 * density);
        gd.setStroke(Math.round(1 * density), 0x4D10B981);
        box.setBackground(gd);

        TextView tvHeading = new TextView(context);
        tvHeading.setText("🛡️ KESELAMATAN KOTAK ANDROID TV: CEMERLANG");
        tvHeading.setTextColor(0xFF34D399);
        tvHeading.setTextSize(12.5f);
        if (fontBold != null) tvHeading.setTypeface(fontBold, Typeface.BOLD);
        box.addView(tvHeading);

        String[] checks = new String[]{
                "✓ Bebas dari Botnet XCPE (x.cpe / Videostrong supply chain)",
                "✓ Bebas dari Pintu Belakang BadBox & YokaTV Market (com.oemservice)",
                "✓ Bebas dari Pengiklanan Palsu & Pemindahan Proksi Harap (com.abc.nexus / asshow)",
                "✓ Direktori Sistem /system/app & /vendor/app dalam keadaan bersih",
                "✓ Pengoptimuman RAM 1GB disesuaikan untuk navigasi lancar 60 FPS"
        };

        for (String c : checks) {
            TextView tv = new TextView(context);
            tv.setText(c);
            tv.setTextColor(0xFFE2E8F0);
            tv.setTextSize(11);
            tv.setPadding(0, Math.round(4 * density), 0, 0);
            box.addView(tv);
        }

        container.addView(box);
    }

    private static void renderThreatsList(
            Context context,
            LinearLayout container,
            float density,
            Typeface fontBold,
            List<BoxSecurityScanner.ThreatItem> threats,
            boolean hasRoot,
            Runnable onRemediated
    ) {
        TextView tvWarn = new TextView(context);
        tvWarn.setText("⚠️ SISTEM MENGESAN ANCAMAN KESELAMATAN BERIKUT:");
        tvWarn.setTextColor(0xFFF87171);
        tvWarn.setTextSize(12);
        if (fontBold != null) tvWarn.setTypeface(fontBold, Typeface.BOLD);
        tvWarn.setPadding(0, 0, 0, Math.round(6 * density));
        container.addView(tvWarn);

        for (BoxSecurityScanner.ThreatItem threat : threats) {
            LinearLayout itemCard = new LinearLayout(context);
            itemCard.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = Math.round(10 * density);
            itemCard.setLayoutParams(lp);
            itemCard.setPadding(Math.round(12 * density), Math.round(10 * density), Math.round(12 * density), Math.round(10 * density));

            GradientDrawable cardGd = new GradientDrawable();
            cardGd.setColor(0x28DC2626); // Translucent crimson glass
            cardGd.setCornerRadius(12 * density);
            cardGd.setStroke(Math.round(1 * density), 0x80EF4444);
            itemCard.setBackground(cardGd);

            // Title row
            LinearLayout tRow = new LinearLayout(context);
            tRow.setOrientation(LinearLayout.HORIZONTAL);
            tRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView tvBadge = new TextView(context);
            tvBadge.setText(" " + threat.meta.riskLevel + " ");
            tvBadge.setTextColor(0xFFFFFFFF);
            tvBadge.setTextSize(9);
            if (fontBold != null) tvBadge.setTypeface(fontBold, Typeface.BOLD);
            GradientDrawable bGd = new GradientDrawable();
            bGd.setColor(0xFFDC2626);
            bGd.setCornerRadius(6 * density);
            tvBadge.setBackground(bGd);
            tRow.addView(tvBadge);

            TextView tvName = new TextView(context);
            tvName.setText("  " + threat.meta.title + " (" + threat.packageName + ")");
            tvName.setTextColor(0xFFFFFFFF);
            tvName.setTextSize(11.5f);
            if (fontBold != null) tvName.setTypeface(fontBold, Typeface.BOLD);
            tRow.addView(tvName);

            itemCard.addView(tRow);

            // Description
            TextView tvDesc = new TextView(context);
            tvDesc.setText(threat.meta.description + (threat.filePath != null ? "\nLokasi fail: " + threat.filePath : ""));
            tvDesc.setTextColor(0xFFCBD5E1);
            tvDesc.setTextSize(10.5f);
            tvDesc.setPadding(0, Math.round(4 * density), 0, Math.round(8 * density));
            itemCard.addView(tvDesc);

            // Action row
            LinearLayout actRow = new LinearLayout(context);
            actRow.setOrientation(LinearLayout.HORIZONTAL);

            if (hasRoot) {
                TextView btnRootFix = new TextView(context);
                btnRootFix.setText("⚡ Lumpuhkan / Padam (Root 1-Klik)");
                btnRootFix.setTextColor(0xFFFFFFFF);
                btnRootFix.setTextSize(11);
                if (fontBold != null) btnRootFix.setTypeface(fontBold, Typeface.BOLD);
                btnRootFix.setFocusable(true);
                btnRootFix.setClickable(true);
                btnRootFix.setPadding(Math.round(12 * density), Math.round(6 * density), Math.round(12 * density), Math.round(6 * density));

                GradientDrawable fixGd = new GradientDrawable();
                fixGd.setColor(0xFFDC2626);
                fixGd.setCornerRadius(8 * density);
                btnRootFix.setBackground(fixGd);

                btnRootFix.setOnFocusChangeListener((v, hasFocus) -> {
                    GradientDrawable g = new GradientDrawable();
                    g.setColor(hasFocus ? 0xFFEF4444 : 0xFFDC2626);
                    g.setCornerRadius(8 * density);
                    g.setStroke(hasFocus ? Math.round(2f * density) : 0, 0xFFFFFFFF);
                    v.setBackground(g);
                    if (hasFocus) SoundEffects.playNav();
                });

                btnRootFix.setOnClickListener(v -> {
                    SoundEffects.playSelect();
                    Toast.makeText(context, "⏳ Sedang melumpuhkan & membekukan malware dengan akses Root...", Toast.LENGTH_SHORT).show();
                    BoxSecurityScanner.neutralizeThreat(context, threat, (success, msg) -> {
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                        if (onRemediated != null) onRemediated.run();
                    });
                });

                actRow.addView(btnRootFix);
            }

            TextView btnAppSet = new TextView(context);
            btnAppSet.setText(hasRoot ? "⚙️ Buka Tetapan" : "⚙️ Buka Tetapan Aplikasi (Nyahaktifkan Manual)");
            btnAppSet.setTextColor(0xFF94A3B8);
            btnAppSet.setTextSize(11);
            if (fontBold != null) btnAppSet.setTypeface(fontBold, Typeface.BOLD);
            btnAppSet.setFocusable(true);
            btnAppSet.setClickable(true);
            btnAppSet.setPadding(Math.round(12 * density), Math.round(6 * density), Math.round(12 * density), Math.round(6 * density));

            GradientDrawable setGd = new GradientDrawable();
            setGd.setColor(0x28FFFFFF);
            setGd.setCornerRadius(8 * density);
            btnAppSet.setBackground(setGd);

            btnAppSet.setOnFocusChangeListener((v, hasFocus) -> {
                GradientDrawable g = new GradientDrawable();
                g.setColor(hasFocus ? 0x44FFFFFF : 0x28FFFFFF);
                g.setCornerRadius(8 * density);
                g.setStroke(hasFocus ? Math.round(1.5f * density) : 0, 0xFF67E8F9);
                v.setBackground(g);
                btnAppSet.setTextColor(hasFocus ? 0xFFFFFFFF : 0xFF94A3B8);
                if (hasFocus) SoundEffects.playNav();
            });

            btnAppSet.setOnClickListener(v -> {
                SoundEffects.playSelect();
                BoxSecurityScanner.neutralizeThreat(context, threat, (success, msg) -> {
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                });
            });

            LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            bLp.setMarginStart(hasRoot ? Math.round(8 * density) : 0);
            btnAppSet.setLayoutParams(bLp);
            actRow.addView(btnAppSet);

            itemCard.addView(actRow);
            container.addView(itemCard);
        }
    }
}
