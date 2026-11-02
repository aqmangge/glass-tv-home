package com.gathin.tvhome;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * TV Dialog for managing a specific User Profile's settings.
 * Includes profile renaming, child mode switch, deletion, and avatar selection
 * between the 3 modern options: Watak Tunggal, Cari GIF, and Muat Naik Mudah Alih.
 */
public class ProfileSettingsDialog {

    public interface OnProfileSavedListener {
        void onProfileSaved();
    }

    public static void show(Context context, ProfileManager.UserProfile profile, OnProfileSavedListener listener) {
        if (context == null || profile == null) return;
        float density = context.getResources().getDisplayMetrics().density;
        AlertDialog.Builder builder = new AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar);

        ScrollView scroll = new ScrollView(context);
        scroll.setBackgroundResource(R.drawable.bg_glass_dialog);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        scroll.setPadding((int) (24 * density), (int) (18 * density), (int) (24 * density), (int) (18 * density));

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setClipChildren(false);
        root.setClipToPadding(false);
        scroll.addView(root);

        // Header
        TextView tvTitle = new TextView(context);
        tvTitle.setText("⚙️ Tetapan Profil: " + profile.name);
        tvTitle.setTextSize(20);
        tvTitle.setTextColor(0xFFFFFFFF);
        tvTitle.setTypeface(context.getResources().getFont(R.font.inter), android.graphics.Typeface.BOLD);
        root.addView(tvTitle);

        TextView tvSub = new TextView(context);
        tvSub.setText("Urus nama profil, kawalan kanak-kanak, dan pilihan avatar");
        tvSub.setTextSize(12.5f);
        tvSub.setTextColor(0xFF94A3B8);
        tvSub.setPadding(0, (int) (2 * density), 0, (int) (10 * density));
        root.addView(tvSub);

        // Top Row: Avatar Live Preview + Profile Name & Child Mode
        LinearLayout infoRow = new LinearLayout(context);
        infoRow.setOrientation(LinearLayout.HORIZONTAL);
        infoRow.setGravity(Gravity.CENTER_VERTICAL);
        infoRow.setPadding(0, 0, 0, (int) (12 * density));

        FrameLayout avatarPreview = new FrameLayout(context);
        int prevSize = Math.round(76 * density);
        LinearLayout.LayoutParams prevLp = new LinearLayout.LayoutParams(prevSize, prevSize);
        prevLp.setMarginEnd((int) (14 * density));
        avatarPreview.setLayoutParams(prevLp);
        infoRow.addView(avatarPreview);

        // Bind avatar preview
        ProfileAvatarHelper.bindAvatar(avatarPreview, profile, context);

        LinearLayout inputCol = new LinearLayout(context);
        inputCol.setOrientation(LinearLayout.VERTICAL);

        TextView tvNameLabel = new TextView(context);
        tvNameLabel.setText("NAMA PROFIL");
        tvNameLabel.setTextSize(11);
        tvNameLabel.setTextColor(0xFF94A3B8);
        tvNameLabel.setTypeface(context.getResources().getFont(R.font.inter), android.graphics.Typeface.BOLD);
        inputCol.addView(tvNameLabel);

        EditText etName = new EditText(context);
        etName.setText(profile.name);
        etName.setTextColor(0xFFFFFFFF);
        etName.setTextSize(14.5f);
        etName.setSingleLine(true);
        etName.setPadding((int) (12 * density), (int) (7 * density), (int) (12 * density), (int) (7 * density));

        GradientDrawable etBg = new GradientDrawable();
        etBg.setColor(0x22FFFFFF);
        etBg.setCornerRadius(10 * density);
        etBg.setStroke((int) (1.2f * density), 0x33FFFFFF);
        etName.setBackground(etBg);
        inputCol.addView(etName);

        CheckBox cbChild = new CheckBox(context);
        cbChild.setText("Aktifkan Mod Kanak-kanak (Sekat aplikasi dewasa)");
        cbChild.setTextColor(0xFFE2E8F0);
        cbChild.setTextSize(12.5f);
        cbChild.setChecked(profile.isChild);
        cbChild.setPadding((int) (6 * density), (int) (4 * density), 0, 0);
        inputCol.addView(cbChild);

        infoRow.addView(inputCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(infoRow);

        // Section Title: 3 Avatar Options
        TextView tvAvatarSection = new TextView(context);
        tvAvatarSection.setText("PILIH JENIS AVATAR (3 PILIHAN)");
        tvAvatarSection.setTextSize(11.5f);
        tvAvatarSection.setTextColor(0xFF38BDF8); // Cyan
        tvAvatarSection.setTypeface(context.getResources().getFont(R.font.inter), android.graphics.Typeface.BOLD);
        tvAvatarSection.setPadding(0, (int) (4 * density), 0, (int) (8 * density));
        root.addView(tvAvatarSection);

        // Option 1: Watak Tunggal (Lottie Animation)
        View opt1 = createOptionCard(
                context,
                "🎭 1. Watak Tunggal (Animasi Bergerak)",
                "Animasi karakter comel yang terapung dengan pelbagai mimik muka & warna tersendiri",
                "Kustomisasi Watak",
                density,
                v -> AvatarCustomizerDialog.show(context, () -> {
                    profile.avatarType = ProfileManager.AVATAR_TYPE_LOTTIE;
                    profile.lottieBgColor = AvatarThemeManager.getBgColor(context);
                    profile.lottieCharColor = AvatarThemeManager.getCharColor(context);
                    profile.lottieFaceIndex = AvatarThemeManager.getFaceIndex(context);
                    ProfileAvatarHelper.bindAvatar(avatarPreview, profile, context);
                })
        );
        root.addView(opt1);

        // Option 2: Cari GIF (Giphy Search)
        View opt2 = createOptionCard(
                context,
                "🔍 2. Cari Animasi GIF (Giphy)",
                "Cari ribuan animasi GIF menarik daripada pangkalan data Giphy secara terus",
                "Cari GIF",
                density,
                v -> GifSearchDialog.show(context, profile.id, path -> {
                    profile.avatarType = ProfileManager.AVATAR_TYPE_GIF;
                    profile.avatarPath = path;
                    ProfileAvatarHelper.bindAvatar(avatarPreview, profile, context);
                })
        );
        root.addView(opt2);

        // Option 3: Muat Naik Mudah Alih (Mobile Upload)
        View opt3 = createOptionCard(
                context,
                "📲 3. Muat Naik Mudah Alih (Telefon / Storan)",
                "Muat naik gambar profil atau GIF terus dari telefon pintar anda melalui Wi-Fi",
                "Muat Naik Dari Telefon",
                density,
                v -> MobileUploadDialog.show(context, profile.id, path -> {
                    profile.avatarType = ProfileManager.AVATAR_TYPE_CUSTOM;
                    profile.avatarPath = path;
                    ProfileAvatarHelper.bindAvatar(avatarPreview, profile, context);
                })
        );
        root.addView(opt3);

        // Action Buttons Row (Padam, Batal, Simpan)
        LinearLayout actionsRow = new LinearLayout(context);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        actionsRow.setGravity(Gravity.CENTER_VERTICAL);
        actionsRow.setPadding(0, (int) (14 * density), 0, 0);

        List<ProfileManager.UserProfile> allProfs = ProfileManager.getAllProfiles(context);
        if (allProfs.size() > 1) {
            TextView btnDelete = new TextView(context);
            btnDelete.setText("🗑️ Padam Profil");
            btnDelete.setTextColor(0xFFEF4444); // Red
            btnDelete.setTextSize(13);
            btnDelete.setTypeface(context.getResources().getFont(R.font.inter), android.graphics.Typeface.BOLD);
            btnDelete.setGravity(Gravity.CENTER);
            btnDelete.setPadding((int) (12 * density), (int) (8 * density), (int) (12 * density), (int) (8 * density));
            btnDelete.setFocusable(true);
            btnDelete.setClickable(true);
            btnDelete.setOnFocusChangeListener((v, hasFocus) -> {
                btnDelete.setTextColor(hasFocus ? 0xFFFCA5A5 : 0xFFEF4444);
            });
            actionsRow.addView(btnDelete);

            btnDelete.setOnClickListener(v -> {
                ProfileManager.deleteProfile(context, profile.id);
                Toast.makeText(context, "Profil berjaya dipadam.", Toast.LENGTH_SHORT).show();
                if (listener != null) listener.onProfileSaved();
            });
        }

        View spacer = new View(context);
        actionsRow.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));

        TextView btnCancel = new TextView(context);
        btnCancel.setText("Batal");
        btnCancel.setTextColor(0xFF94A3B8);
        btnCancel.setTextSize(13.5f);
        btnCancel.setTypeface(context.getResources().getFont(R.font.inter), android.graphics.Typeface.NORMAL);
        btnCancel.setGravity(Gravity.CENTER);
        btnCancel.setPadding((int) (16 * density), (int) (8 * density), (int) (16 * density), (int) (8 * density));
        btnCancel.setFocusable(true);
        btnCancel.setClickable(true);
        btnCancel.setOnFocusChangeListener((v, hasFocus) -> {
            btnCancel.setTextColor(hasFocus ? 0xFFFFFFFF : 0xFF94A3B8);
        });
        actionsRow.addView(btnCancel);

        TextView btnSave = new TextView(context);
        btnSave.setText("Simpan Perubahan");
        btnSave.setTextColor(0xFFFFFFFF);
        btnSave.setTextSize(13.5f);
        btnSave.setTypeface(context.getResources().getFont(R.font.inter), android.graphics.Typeface.BOLD);
        btnSave.setGravity(Gravity.CENTER);
        btnSave.setIncludeFontPadding(false);
        btnSave.setBackgroundResource(R.drawable.bg_dialog_btn);
        btnSave.setPadding((int) (22 * density), (int) (9 * density), (int) (22 * density), (int) (9 * density));
        btnSave.setFocusable(true);
        btnSave.setClickable(true);
        actionsRow.addView(btnSave, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(actionsRow);

        builder.setView(scroll);
        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String newName = etName.getText().toString().trim();
            if (!newName.isEmpty()) {
                profile.name = newName;
            }
            profile.isChild = cbChild.isChecked();
            ProfileManager.updateProfile(context, profile);
            Toast.makeText(context, "✨ Profil berjaya dikemaskini!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            if (listener != null) listener.onProfileSaved();
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(Math.round(680 * density), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private static View createOptionCard(
            Context context,
            String title,
            String subtitle,
            String btnText,
            float density,
            View.OnClickListener onClick
    ) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setFocusable(true);
        card.setClickable(true);
        card.setClipChildren(false);
        card.setClipToPadding(false);
        card.setPadding((int) (16 * density), (int) (11 * density), (int) (16 * density), (int) (11 * density));

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(0x18FFFFFF);
        gd.setCornerRadius(14 * density);
        gd.setStroke((int) (1.2f * density), 0x22FFFFFF);
        card.setBackground(gd);

        LinearLayout textCol = new LinearLayout(context);
        textCol.setOrientation(LinearLayout.VERTICAL);

        TextView tvT = new TextView(context);
        tvT.setText(title);
        tvT.setTextSize(14f);
        tvT.setTextColor(0xFFFFFFFF);
        tvT.setTypeface(context.getResources().getFont(R.font.inter), android.graphics.Typeface.BOLD);
        textCol.addView(tvT);

        TextView tvS = new TextView(context);
        tvS.setText(subtitle);
        tvS.setTextSize(11.5f);
        tvS.setTextColor(0xFF94A3B8);
        tvS.setPadding(0, (int) (2 * density), 0, 0);
        textCol.addView(tvS);

        card.addView(textCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView btn = new TextView(context);
        btn.setText(btnText);
        btn.setTextSize(12.5f);
        btn.setTextColor(0xFFFFFFFF);
        btn.setTypeface(context.getResources().getFont(R.font.inter), android.graphics.Typeface.BOLD);
        btn.setGravity(Gravity.CENTER);
        btn.setIncludeFontPadding(false);
        btn.setBackgroundResource(R.drawable.bg_dialog_btn);
        btn.setPadding((int) (16 * density), (int) (8 * density), (int) (16 * density), (int) (8 * density));

        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnLp.setMarginStart((int) (12 * density));
        card.addView(btn, btnLp);

        card.setOnFocusChangeListener((v, hasFocus) -> {
            gd.setColor(hasFocus ? 0x2867E8F9 : 0x18FFFFFF);
            gd.setStroke((int) (2.0f * density), hasFocus ? 0xFF67E8F9 : 0x22FFFFFF);
            btn.setTextColor(hasFocus ? 0xFFFFFFFF : 0xFFCBD5E1);
        });

        card.setOnClickListener(onClick);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = (int) (6 * density);
        card.setLayoutParams(lp);
        return card;
    }
}
