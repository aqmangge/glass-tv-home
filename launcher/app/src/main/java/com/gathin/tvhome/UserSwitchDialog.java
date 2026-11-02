package com.gathin.tvhome;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * Fast floating TV popup for switching user profiles and managing Child Mode.
 * Triggered from top bar Candy/User button.
 */
public final class UserSwitchDialog {

    public static void show(MainActivity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        SoundEffects.playSelect();

        float density = activity.getResources().getDisplayMetrics().density;
        DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        Typeface outfit = activity.getResources().getFont(R.font.inter);

        AlertDialog[] dialogHolder = new AlertDialog[1];

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding((int) (22 * density), (int) (20 * density), (int) (22 * density), (int) (20 * density));
        root.setClipChildren(false);
        root.setClipToPadding(false);

        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xF4141B28, 0xF40C111A}
        );
        bg.setCornerRadius(26 * density);
        bg.setStroke((int) (1.5f * density), 0x4D67E8F9); // Soap bubble iridescent rim
        root.setBackground(bg);

        // Header Title
        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        ImageView ivIcon = new ImageView(activity);
        ivIcon.setImageResource(R.drawable.ic_shield_child);
        ivIcon.setColorFilter(0xFFF59E0B); // Amber
        header.addView(ivIcon, new LinearLayout.LayoutParams((int) (24 * density), (int) (24 * density)));

        LinearLayout headerTextCol = new LinearLayout(activity);
        headerTextCol.setOrientation(LinearLayout.VERTICAL);
        headerTextCol.setPadding((int) (10 * density), 0, 0, 0);

        TextView tvTitle = new TextView(activity);
        tvTitle.setText("Tukar Pengguna & Mod Kanak-kanak");
        tvTitle.setTextSize(18);
        tvTitle.setTextColor(0xFFFFFFFF);
        tvTitle.setTypeface(outfit, Typeface.BOLD);
        headerTextCol.addView(tvTitle);

        TextView tvSub = new TextView(activity);
        tvSub.setText("Pilih profil aktif atau kawal akses aplikasi anak");
        tvSub.setTextSize(12);
        tvSub.setTextColor(0xFF8E929E);
        tvSub.setTypeface(outfit, Typeface.NORMAL);
        headerTextCol.addView(tvSub);

        header.addView(headerTextCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(header);

        // Divider
        View div1 = new View(activity);
        div1.setBackgroundColor(0xFF282B35);
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (1 * density));
        divLp.topMargin = (int) (14 * density);
        divLp.bottomMargin = (int) (14 * density);
        root.addView(div1, divLp);

        // Section 1: User Profiles
        TextView tvProfSec = new TextView(activity);
        tvProfSec.setText("PILIH PENGGUNA (PROFILES)");
        tvProfSec.setTextSize(11.5f);
        tvProfSec.setTextColor(0xFF8E929E);
        tvProfSec.setTypeface(outfit, Typeface.BOLD);
        root.addView(tvProfSec);

        HorizontalScrollView profScroll = new HorizontalScrollView(activity);
        profScroll.setHorizontalScrollBarEnabled(false);
        profScroll.setClipChildren(false);
        profScroll.setClipToPadding(false);
        profScroll.setPadding(0, (int) (10 * density), 0, (int) (12 * density));

        LinearLayout profRow = new LinearLayout(activity);
        profRow.setOrientation(LinearLayout.HORIZONTAL);
        profRow.setGravity(Gravity.CENTER_VERTICAL);
        profRow.setClipChildren(false);
        profRow.setClipToPadding(false);

        List<ProfileManager.UserProfile> profiles = ProfileManager.getAllProfiles(activity);
        ProfileManager.UserProfile activeProfile = ProfileManager.getActiveProfile(activity);

        for (ProfileManager.UserProfile prof : profiles) {
            boolean isActive = prof.id.equals(activeProfile.id);

            LinearLayout itemCol = new LinearLayout(activity);
            itemCol.setOrientation(LinearLayout.VERTICAL);
            itemCol.setGravity(Gravity.CENTER_HORIZONTAL);
            itemCol.setFocusable(true);
            itemCol.setClickable(true);
            itemCol.setPadding((int) (8 * density), (int) (6 * density), (int) (8 * density), (int) (6 * density));

            GradientDrawable iBg = new GradientDrawable();
            iBg.setColor(isActive ? 0xFF2A2D38 : Color.TRANSPARENT);
            iBg.setCornerRadius(14 * density);
            if (isActive) iBg.setStroke((int) (1.5f * density), 0x88FFFFFF);
            itemCol.setBackground(iBg);

            // Avatar Frame with Ring
            FrameLayout aFrame = new FrameLayout(activity);
            int frameDim = (int) (60 * density);

            View ring = new View(activity);
            GradientDrawable ringBg = new GradientDrawable();
            ringBg.setShape(GradientDrawable.OVAL);
            ringBg.setColor(Color.TRANSPARENT);
            ringBg.setStroke((int) (3 * density), isActive ? 0xFFFFFFFF : Color.TRANSPARENT);
            ring.setBackground(ringBg);
            aFrame.addView(ring, new FrameLayout.LayoutParams(frameDim, frameDim));

            ImageView aImg = new ImageView(activity);
            aImg.setImageResource(ProfileManager.getAvatarResId(prof.avatarIndex));
            int imgDim = (int) (48 * density);
            aFrame.addView(aImg, new FrameLayout.LayoutParams(imgDim, imgDim, Gravity.CENTER));

            itemCol.addView(aFrame, new LinearLayout.LayoutParams(frameDim, frameDim));

            TextView tvName = new TextView(activity);
            tvName.setText(prof.name + (prof.isChild ? " 👶" : ""));
            tvName.setTextSize(12.5f);
            tvName.setTextColor(isActive ? 0xFFFFFFFF : 0xFFCBD5E1);
            tvName.setTypeface(outfit, isActive ? Typeface.BOLD : Typeface.NORMAL);
            tvName.setPadding(0, (int) (6 * density), 0, 0);
            tvName.setSingleLine(true);
            itemCol.addView(tvName);

            itemCol.setOnFocusChangeListener((v, hasFocus) -> {
                GradientDrawable g = new GradientDrawable();
                g.setColor(hasFocus ? 0xFF353946 : (isActive ? 0xFF2A2D38 : Color.TRANSPARENT));
                g.setCornerRadius(14 * density);
                if (hasFocus) {
                    g.setStroke((int) (2 * density), 0xFF38BDF8);
                } else if (isActive) {
                    g.setStroke((int) (1.5f * density), 0x88FFFFFF);
                }
                v.setBackground(g);
                v.animate().scaleX(hasFocus ? 1.08f : 1.0f).scaleY(hasFocus ? 1.08f : 1.0f).setDuration(120).start();
            });

            itemCol.setOnClickListener(v -> {
                SoundEffects.playSelect();
                if (isActive) {
                    Toast.makeText(activity, "Profil " + prof.name + " sudah aktif", Toast.LENGTH_SHORT).show();
                    return;
                }

                // If currently Child Mode is on and switching to adult profile, verify PIN
                boolean curChildOn = ChildModeManager.isChildModeEnabled(activity);
                if (curChildOn && !prof.isChild) {
                    ChildModeManager.promptPin(activity, "Sahkan PIN Keluar Mod Anak", "Masukkan PIN untuk bertukar ke profil " + prof.name, new ChildModeManager.PinCallback() {
                        @Override
                        public void onSuccess() {
                            switchProfile(activity, prof, false, dialogHolder[0]);
                        }
                        @Override
                        public void onCancelled() {}
                    });
                } else {
                    switchProfile(activity, prof, prof.isChild, dialogHolder[0]);
                }
            });

            LinearLayout.LayoutParams iLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            iLp.setMarginEnd((int) (12 * density));
            profRow.addView(itemCol, iLp);
        }

        // Add Profile Button in Row
        LinearLayout addCol = new LinearLayout(activity);
        addCol.setOrientation(LinearLayout.VERTICAL);
        addCol.setGravity(Gravity.CENTER_HORIZONTAL);
        addCol.setFocusable(true);
        addCol.setClickable(true);
        addCol.setPadding((int) (12 * density), (int) (8 * density), (int) (12 * density), (int) (8 * density));

        GradientDrawable addBg = new GradientDrawable();
        addBg.setColor(0xFF242731);
        addBg.setCornerRadius(14 * density);
        addCol.setBackground(addBg);

        FrameLayout plusFrame = new FrameLayout(activity);
        int pfDim = (int) (52 * density);
        GradientDrawable pfBg = new GradientDrawable();
        pfBg.setShape(GradientDrawable.OVAL);
        pfBg.setColor(0xFF2E323F);
        plusFrame.setBackground(pfBg);

        TextView tvPlus = new TextView(activity);
        tvPlus.setText("＋");
        tvPlus.setTextSize(22);
        tvPlus.setTextColor(0xFF38BDF8);
        tvPlus.setGravity(Gravity.CENTER);
        plusFrame.addView(tvPlus, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        addCol.addView(plusFrame, new LinearLayout.LayoutParams(pfDim, pfDim));

        TextView tvAddLbl = new TextView(activity);
        tvAddLbl.setText("Tambah");
        tvAddLbl.setTextSize(12);
        tvAddLbl.setTextColor(0xFF8E929E);
        tvAddLbl.setTypeface(outfit, Typeface.NORMAL);
        tvAddLbl.setPadding(0, (int) (6 * density), 0, 0);
        addCol.addView(tvAddLbl);

        addCol.setOnFocusChangeListener((v, hasFocus) -> {
            GradientDrawable g = new GradientDrawable();
            g.setColor(hasFocus ? 0xFF353946 : 0xFF242731);
            g.setCornerRadius(14 * density);
            if (hasFocus) g.setStroke((int) (2 * density), 0xFFFFFFFF);
            v.setBackground(g);
            v.animate().scaleX(hasFocus ? 1.08f : 1.0f).scaleY(hasFocus ? 1.08f : 1.0f).setDuration(120).start();
        });

        addCol.setOnClickListener(v -> {
            SoundEffects.playSelect();
            showAddProfileDialog(activity, density, outfit, () -> {
                if (dialogHolder[0] != null) dialogHolder[0].dismiss();
                show(activity);
            });
        });

        profRow.addView(addCol);
        profScroll.addView(profRow);
        root.addView(profScroll);

        // Divider
        View div2 = new View(activity);
        div2.setBackgroundColor(0xFF282B35);
        LinearLayout.LayoutParams div2Lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (1 * density));
        div2Lp.topMargin = (int) (6 * density);
        div2Lp.bottomMargin = (int) (14 * density);
        root.addView(div2, div2Lp);

        // Section 2: Child Mode Quick Actions
        TextView tvChildSec = new TextView(activity);
        tvChildSec.setText("KAWALAN MOD KANAK-KANAK (CHILD MODE)");
        tvChildSec.setTextSize(11.5f);
        tvChildSec.setTextColor(0xFF8E929E);
        tvChildSec.setTypeface(outfit, Typeface.BOLD);
        tvChildSec.setPadding(0, 0, 0, (int) (8 * density));
        root.addView(tvChildSec);

        boolean isChildOn = ChildModeManager.isChildModeEnabled(activity);

        // Switch Card: Status Mode Kanak-kanak
        LinearLayout swCard = new LinearLayout(activity);
        swCard.setOrientation(LinearLayout.HORIZONTAL);
        swCard.setGravity(Gravity.CENTER_VERTICAL);
        swCard.setPadding((int) (16 * density), (int) (12 * density), (int) (16 * density), (int) (12 * density));
        swCard.setFocusable(true);
        swCard.setClickable(true);

        GradientDrawable swBg = new GradientDrawable();
        swBg.setColor(0xFF242731);
        swBg.setCornerRadius(12 * density);
        swCard.setBackground(swBg);

        LinearLayout swTextCol = new LinearLayout(activity);
        swTextCol.setOrientation(LinearLayout.VERTICAL);

        TextView tvSwTitle = new TextView(activity);
        tvSwTitle.setText("🛡️ Status Mode Kanak-kanak");
        tvSwTitle.setTextSize(14.5f);
        tvSwTitle.setTextColor(0xFFFFFFFF);
        tvSwTitle.setTypeface(outfit, Typeface.BOLD);
        swTextCol.addView(tvSwTitle);

        TextView tvSwSub = new TextView(activity);
        tvSwSub.setText(isChildOn ? "Aktif: Aplikasi terpilih dikunci dengan PIN & disembunyikan" : "Tidak Aktif: Semua aplikasi dibuka tanpa sekatan");
        tvSwSub.setTextSize(12);
        tvSwSub.setTextColor(isChildOn ? 0xFFF59E0B : 0xFF8E929E);
        tvSwSub.setTypeface(outfit, Typeface.NORMAL);
        swTextCol.addView(tvSwSub);

        swCard.addView(swTextCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Switch sw = new Switch(activity);
        sw.setChecked(isChildOn);
        sw.setFocusable(false);
        sw.setClickable(false);
        swCard.addView(sw);

        swCard.setOnFocusChangeListener((v, hasFocus) -> {
            GradientDrawable g = new GradientDrawable();
            g.setColor(hasFocus ? 0xFF333744 : 0xFF242731);
            g.setCornerRadius(12 * density);
            if (hasFocus) g.setStroke((int) (2 * density), 0xFFFFFFFF);
            v.setBackground(g);
            v.animate().scaleX(hasFocus ? 1.02f : 1.0f).scaleY(hasFocus ? 1.02f : 1.0f).setDuration(120).start();
        });

        swCard.setOnClickListener(v -> {
            SoundEffects.playSelect();
            boolean currentlyOn = ChildModeManager.isChildModeEnabled(activity);
            if (currentlyOn) {
                // To turn OFF, prompt PIN
                ChildModeManager.promptPin(activity, "Sahkan PIN Ibu Bapa", "Masukkan PIN untuk mematikan Mode Kanak-kanak", new ChildModeManager.PinCallback() {
                    @Override
                    public void onSuccess() {
                        ChildModeManager.setChildModeEnabled(activity, false);
                        sw.setChecked(false);
                        tvSwSub.setText("Tidak Aktif: Semua aplikasi dibuka tanpa sekatan");
                        tvSwSub.setTextColor(0xFF8E929E);
                        activity.reloadTiles();
                        Toast.makeText(activity, "Mode Kanak-kanak dimatikan", Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onCancelled() {}
                });
            } else {
                // To turn ON, activate immediately
                ChildModeManager.setChildModeEnabled(activity, true);
                sw.setChecked(true);
                tvSwSub.setText("Aktif: Aplikasi terpilih dikunci dengan PIN & disembunyikan");
                tvSwSub.setTextColor(0xFFF59E0B);
                activity.reloadTiles();
                Toast.makeText(activity, "✅ Mode Kanak-kanak diaktifkan!", Toast.LENGTH_SHORT).show();
            }
        });

        LinearLayout.LayoutParams scLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        scLp.bottomMargin = (int) (8 * density);
        root.addView(swCard, scLp);

        // Action Card: Manage Locked Apps
        LinearLayout lockCard = new LinearLayout(activity);
        lockCard.setOrientation(LinearLayout.HORIZONTAL);
        lockCard.setGravity(Gravity.CENTER_VERTICAL);
        lockCard.setPadding((int) (16 * density), (int) (11 * density), (int) (16 * density), (int) (11 * density));
        lockCard.setFocusable(true);
        lockCard.setClickable(true);

        GradientDrawable lcBg = new GradientDrawable();
        lcBg.setColor(0xFF242731);
        lcBg.setCornerRadius(12 * density);
        lockCard.setBackground(lcBg);

        TextView tvLcTitle = new TextView(activity);
        tvLcTitle.setText("🔒 Urus Senarai Kunci & Sembunyi Aplikasi");
        tvLcTitle.setTextSize(14);
        tvLcTitle.setTextColor(0xFFFFFFFF);
        tvLcTitle.setTypeface(outfit, Typeface.BOLD);
        lockCard.addView(tvLcTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvLcArrow = new TextView(activity);
        tvLcArrow.setText("→");
        tvLcArrow.setTextSize(16);
        tvLcArrow.setTextColor(0xFF8E929E);
        lockCard.addView(tvLcArrow);

        lockCard.setOnFocusChangeListener((v, hasFocus) -> {
            GradientDrawable g = new GradientDrawable();
            g.setColor(hasFocus ? 0xFF333744 : 0xFF242731);
            g.setCornerRadius(12 * density);
            if (hasFocus) g.setStroke((int) (2 * density), 0xFFFFFFFF);
            v.setBackground(g);
            v.animate().scaleX(hasFocus ? 1.02f : 1.0f).scaleY(hasFocus ? 1.02f : 1.0f).setDuration(120).start();
        });

        lockCard.setOnClickListener(v -> {
            SoundEffects.playSelect();
            if (dialogHolder[0] != null) dialogHolder[0].dismiss();
            ModernSettingsDialog.show(activity);
        });

        LinearLayout.LayoutParams lcLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lcLp.bottomMargin = (int) (10 * density);
        root.addView(lockCard, lcLp);

        // Close / Batal Button
        Button btnClose = new Button(activity);
        btnClose.setText("Tutup");
        btnClose.setTextSize(14);
        btnClose.setTextColor(0xFFE2E8F0);
        btnClose.setTypeface(outfit, Typeface.BOLD);
        btnClose.setFocusable(true);
        btnClose.setClickable(true);

        GradientDrawable cBg = new GradientDrawable();
        cBg.setColor(0xFF2C303D);
        cBg.setCornerRadius(10 * density);
        btnClose.setBackground(cBg);

        btnClose.setOnFocusChangeListener((v, hasFocus) -> {
            GradientDrawable g = new GradientDrawable();
            g.setColor(hasFocus ? 0xFF3D4354 : 0xFF2C303D);
            g.setCornerRadius(10 * density);
            if (hasFocus) g.setStroke((int) (2 * density), 0xFFFFFFFF);
            v.setBackground(g);
        });

        btnClose.setOnClickListener(v -> {
            SoundEffects.playSelect();
            if (dialogHolder[0] != null) dialogHolder[0].dismiss();
        });

        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (42 * density));
        root.addView(btnClose, btnLp);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.Theme_TvHome_GlassDialog);
        builder.setView(root);
        AlertDialog dialog = builder.create();
        dialogHolder[0] = dialog;
        dialog.show();

        Window win = dialog.getWindow();
        if (win != null) {
            win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            win.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            win.setDimAmount(0.65f);
            int w = Math.min((int) (580 * density), (int) (dm.widthPixels * 0.85f));
            win.setLayout(w, ViewGroup.LayoutParams.WRAP_CONTENT);
            win.setGravity(Gravity.CENTER);
        }

        profRow.getChildAt(0).requestFocus();
    }

    private static void switchProfile(MainActivity activity, ProfileManager.UserProfile prof, boolean activateChild, AlertDialog dialog) {
        ProfileManager.setActiveProfile(activity, prof.id);
        if (activateChild) {
            ChildModeManager.setChildModeEnabled(activity, true);
        } else {
            ChildModeManager.setChildModeEnabled(activity, false);
        }
        activity.reloadTiles();
        activity.updateTopBarUserIcon();
        Toast.makeText(activity, "✨ Profil aktif: " + prof.name + (prof.isChild ? " (Mod Kanak-kanak)" : ""), Toast.LENGTH_SHORT).show();
        if (dialog != null) dialog.dismiss();
    }

    public static void showAddProfileDialog(MainActivity activity, float density, Typeface font, Runnable onAdded) {
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding((int) (22 * density), (int) (18 * density), (int) (22 * density), (int) (16 * density));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF1E2026);
        bg.setCornerRadius(20 * density);
        bg.setStroke((int) (1.5f * density), 0xFF2F323B);
        content.setBackground(bg);

        TextView tvTitle = new TextView(activity);
        tvTitle.setText("➕ Tambah Pengguna Baru");
        tvTitle.setTextSize(18);
        tvTitle.setTextColor(0xFFFFFFFF);
        tvTitle.setTypeface(font, Typeface.BOLD);
        content.addView(tvTitle);

        TextView tvSub = new TextView(activity);
        tvSub.setText("Masukkan nama dan pilih avatar untuk profil ini");
        tvSub.setTextSize(12.5f);
        tvSub.setTextColor(0xFF8E929E);
        tvSub.setTypeface(font, Typeface.NORMAL);
        tvSub.setPadding(0, (int) (2 * density), 0, (int) (12 * density));
        content.addView(tvSub);

        // Edit text for Name
        EditText et = new EditText(activity);
        et.setHint("Contoh: Ibu, Ayah, Adik");
        et.setHintTextColor(0xFF6B7280);
        et.setTextColor(0xFFFFFFFF);
        et.setTextSize(15);
        et.setTypeface(font, Typeface.NORMAL);
        et.setPadding((int) (14 * density), (int) (10 * density), (int) (14 * density), (int) (10 * density));

        GradientDrawable etBg = new GradientDrawable();
        etBg.setColor(0xFF262934);
        etBg.setCornerRadius(10 * density);
        etBg.setStroke((int) (1 * density), 0xFF3F4454);
        et.setBackground(etBg);
        content.addView(et);

        // Avatar Picker Header
        TextView tvAvHeader = new TextView(activity);
        tvAvHeader.setText("PILIH AVATAR");
        tvAvHeader.setTextSize(11.5f);
        tvAvHeader.setTextColor(0xFF8E929E);
        tvAvHeader.setTypeface(font, Typeface.BOLD);
        tvAvHeader.setPadding(0, (int) (12 * density), 0, (int) (6 * density));
        content.addView(tvAvHeader);

        // Horizontal Avatar Picker Row
        HorizontalScrollView avScroll = new HorizontalScrollView(activity);
        avScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout avRow = new LinearLayout(activity);
        avRow.setOrientation(LinearLayout.HORIZONTAL);
        avRow.setPadding(0, (int) (4 * density), 0, (int) (8 * density));

        int[] chosenAvatar = new int[]{0}; // default Fox
        View[] rings = new View[7];

        for (int i = 0; i < 7; i++) {
            final int idx = i;
            FrameLayout frame = new FrameLayout(activity);
            frame.setFocusable(true);
            frame.setClickable(true);

            int frameDim = (int) (52 * density);
            LinearLayout.LayoutParams fLp = new LinearLayout.LayoutParams(frameDim, frameDim);
            fLp.setMarginEnd((int) (8 * density));
            frame.setLayoutParams(fLp);

            View ring = new View(activity);
            GradientDrawable ringBg = new GradientDrawable();
            ringBg.setShape(GradientDrawable.OVAL);
            ringBg.setColor(Color.TRANSPARENT);
            ringBg.setStroke((int) (2.5f * density), 0xFFFFFFFF);
            ring.setBackground(ringBg);
            ring.setVisibility(idx == 0 ? View.VISIBLE : View.GONE);
            frame.addView(ring, new FrameLayout.LayoutParams(frameDim, frameDim));
            rings[idx] = ring;

            ImageView img = new ImageView(activity);
            img.setImageResource(ProfileManager.getAvatarResId(idx));
            int innerDim = (int) (42 * density);
            frame.addView(img, new FrameLayout.LayoutParams(innerDim, innerDim, Gravity.CENTER));

            frame.setOnFocusChangeListener((v, hasFocus) -> {
                v.animate().scaleX(hasFocus ? 1.15f : 1.0f).scaleY(hasFocus ? 1.15f : 1.0f).setDuration(120).start();
            });

            frame.setOnClickListener(v -> {
                SoundEffects.playSelect();
                chosenAvatar[0] = idx;
                for (int k = 0; k < 7; k++) {
                    if (rings[k] != null) rings[k].setVisibility(k == idx ? View.VISIBLE : View.GONE);
                }
            });

            avRow.addView(frame);
        }
        avScroll.addView(avRow);
        content.addView(avScroll);

        // Checkbox: Child Mode profile
        CheckBox cbChild = new CheckBox(activity);
        cbChild.setText("Tandakan sebagai Profil Kanak-kanak (Auto Child Mode)");
        cbChild.setTextColor(0xFFCBD5E1);
        cbChild.setTextSize(13);
        cbChild.setTypeface(font, Typeface.NORMAL);
        cbChild.setPadding((int) (6 * density), (int) (6 * density), 0, (int) (12 * density));
        content.addView(cbChild);

        AlertDialog[] dHolder = new AlertDialog[1];

        // Button Row: Cancel & Simpan
        LinearLayout btnRow = new LinearLayout(activity);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);

        Button btnCancel = new Button(activity);
        btnCancel.setText("Batal");
        btnCancel.setTextColor(0xFF8E929E);
        btnCancel.setTypeface(font, Typeface.NORMAL);
        btnCancel.setFocusable(true);
        btnCancel.setClickable(true);
        GradientDrawable cBg = new GradientDrawable();
        cBg.setColor(0xFF262934);
        cBg.setCornerRadius(8 * density);
        btnCancel.setBackground(cBg);
        btnCancel.setPadding((int) (16 * density), 0, (int) (16 * density), 0);
        btnCancel.setOnFocusChangeListener((v, hasFocus) -> {
            cBg.setStroke((int) (2 * density), hasFocus ? 0xFFFFFFFF : 0);
            btnCancel.setTextColor(hasFocus ? 0xFFFFFFFF : 0xFF8E929E);
            v.animate().scaleX(hasFocus ? 1.05f : 1.0f).scaleY(hasFocus ? 1.05f : 1.0f).setDuration(120).start();
        });
        btnCancel.setOnClickListener(v -> {
            if (dHolder[0] != null) dHolder[0].dismiss();
        });
        btnRow.addView(btnCancel, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (int) (40 * density)));

        Button btnSave = new Button(activity);
        btnSave.setText("Simpan Profil");
        btnSave.setTextColor(0xFFFFFFFF);
        btnSave.setTypeface(font, Typeface.BOLD);
        btnSave.setFocusable(true);
        btnSave.setClickable(true);
        GradientDrawable sBg = new GradientDrawable();
        sBg.setColor(0xFF10B981); // Emerald
        sBg.setCornerRadius(8 * density);
        btnSave.setBackground(sBg);
        btnSave.setPadding((int) (16 * density), 0, (int) (16 * density), 0);
        btnSave.setOnFocusChangeListener((v, hasFocus) -> {
            sBg.setStroke((int) (2 * density), hasFocus ? 0xFFFFFFFF : 0);
            v.animate().scaleX(hasFocus ? 1.05f : 1.0f).scaleY(hasFocus ? 1.05f : 1.0f).setDuration(120).start();
        });

        btnSave.setOnClickListener(v -> {
            String name = et.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(activity, "Sila masukkan nama pengguna", Toast.LENGTH_SHORT).show();
                return;
            }
            SoundEffects.playSelect();
            ProfileManager.addProfile(activity, name, chosenAvatar[0], cbChild.isChecked());
            Toast.makeText(activity, "✨ Profil " + name + " berjaya dicipta!", Toast.LENGTH_SHORT).show();
            if (dHolder[0] != null) dHolder[0].dismiss();
            if (onAdded != null) onAdded.run();
        });

        LinearLayout.LayoutParams svLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (int) (40 * density));
        svLp.setMarginStart((int) (10 * density));
        btnRow.addView(btnSave, svLp);
        content.addView(btnRow);

        AlertDialog.Builder b = new AlertDialog.Builder(activity, R.style.Theme_TvHome_GlassDialog);
        b.setView(content);
        AlertDialog d = b.create();
        dHolder[0] = d;
        d.show();

        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int dw = Math.min((int) (520 * density), (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.85f));
            w.setLayout(dw, ViewGroup.LayoutParams.WRAP_CONTENT);
            w.setGravity(Gravity.CENTER);
        }
    }
}
