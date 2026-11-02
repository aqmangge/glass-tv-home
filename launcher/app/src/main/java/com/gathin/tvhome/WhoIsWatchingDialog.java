package com.gathin.tvhome;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**
 * Fullscreen cinematic "Who's watching?" profile selection screen for Glass TV,
 * styled faithfully after the Nuvio / Netflix reference image.
 * Triggered exclusively when clicking the top bar user button on the homescreen.
 */
public final class WhoIsWatchingDialog {

    public static void show(MainActivity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        SoundEffects.playSelect();

        float density = activity.getResources().getDisplayMetrics().density;
        DisplayMetrics dm = activity.getResources().getDisplayMetrics();

        AlertDialog[] dialogHolder = new AlertDialog[1];

        // Root fullscreen layout
        FrameLayout rootFrame = new FrameLayout(activity);
        rootFrame.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // Dark cinematic glass vignette background - opaque deep dark teal to pitch black
        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF09201C, 0xFF051210, 0xFF020706}
        );
        rootFrame.setBackground(bg);

        LinearLayout contentCol = new LinearLayout(activity);
        contentCol.setOrientation(LinearLayout.VERTICAL);
        contentCol.setGravity(Gravity.CENTER_HORIZONTAL);
        contentCol.setPadding(
                (int) (40 * density),
                (int) (68 * density),
                (int) (40 * density),
                (int) (36 * density)
        );

        FrameLayout.LayoutParams colLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        rootFrame.addView(contentCol, colLp);

        // Main Title
        TextView tvTitle = new TextView(activity);
        tvTitle.setText("Who's watching?");
        tvTitle.setTextSize(38);
        tvTitle.setTextColor(0xFFFFFFFF);
        tvTitle.setTypeface(activity.getResources().getFont(R.font.inter), android.graphics.Typeface.BOLD);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(0, 0, 0, (int) (6 * density));
        contentCol.addView(tvTitle);

        // Subtitle
        TextView tvSub = new TextView(activity);
        tvSub.setText("Select a profile to continue");
        tvSub.setTextSize(15.5f);
        tvSub.setTextColor(0xFF94A3B8);
        tvSub.setGravity(Gravity.CENTER);
        tvSub.setPadding(0, 0, 0, (int) (40 * density));
        contentCol.addView(tvSub);

        // Profiles Horizontal Row
        HorizontalScrollView scroll = new HorizontalScrollView(activity);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setFillViewport(true);

        LinearLayout profilesRow = new LinearLayout(activity);
        profilesRow.setOrientation(LinearLayout.HORIZONTAL);
        profilesRow.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        profilesRow.setClipChildren(false);
        profilesRow.setClipToPadding(false);
        profilesRow.setPadding((int) (20 * density), (int) (14 * density), (int) (20 * density), (int) (20 * density));
        scroll.addView(profilesRow, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        scrollLp.gravity = Gravity.CENTER_HORIZONTAL;
        contentCol.addView(scroll, scrollLp);

        // Spacer pushing bottom hint to screen footer
        View footerSpacer = new View(activity);
        contentCol.addView(footerSpacer, new LinearLayout.LayoutParams(0, 0, 1f));

        // Bottom Hint
        TextView tvHint = new TextView(activity);
        tvHint.setText("Hold to manage profile • Tekan lama untuk urus profil");
        tvHint.setTextSize(13);
        tvHint.setTextColor(0xFF64748B);
        tvHint.setGravity(Gravity.CENTER);
        tvHint.setPadding(0, (int) (16 * density), 0, 0);
        contentCol.addView(tvHint);

        // Populate profiles into row
        populateProfiles(
                activity,
                profilesRow,
                dialogHolder,
                density
        );

        AlertDialog.Builder builder = new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen);
        builder.setView(rootFrame);
        AlertDialog dialog = builder.create();
        dialogHolder[0] = dialog;

        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(0xFF020706));
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        dialog.show();

        // Focus active profile
        ProfileManager.UserProfile active = ProfileManager.getActiveProfile(activity);
        List<ProfileManager.UserProfile> list = ProfileManager.getAllProfiles(activity);
        int focusIdx = 0;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(active.id)) {
                focusIdx = i;
                break;
            }
        }
        final int targetIdx = focusIdx;
        profilesRow.post(() -> {
            if (profilesRow.getChildCount() > targetIdx) {
                profilesRow.getChildAt(targetIdx).requestFocus();
            }
        });
    }

    private static void populateProfiles(
            MainActivity activity,
            LinearLayout row,
            AlertDialog[] dialogHolder,
            float density
    ) {
        row.removeAllViews();
        List<ProfileManager.UserProfile> list = ProfileManager.getAllProfiles(activity);
        ProfileManager.UserProfile active = ProfileManager.getActiveProfile(activity);

        int avatarSize = Math.round(136 * density);
        int ringSize = Math.round(144 * density);
        int cardWidth = Math.round(160 * density);
        int cardMargin = Math.round(20 * density);
        int nameHeight = Math.round(28 * density);
        int roleHeight = Math.round(20 * density);

        for (int i = 0; i < list.size(); i++) {
            final ProfileManager.UserProfile prof = list.get(i);
            boolean isActive = prof.id.equals(active.id);

            LinearLayout card = new LinearLayout(activity);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            card.setFocusable(true);
            card.setClickable(true);
            card.setClipChildren(false);
            card.setClipToPadding(false);

            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    cardWidth,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            cardLp.setMarginEnd(cardMargin);
            card.setLayoutParams(cardLp);

            // Avatar Container with Focus Ring Frame
            FrameLayout ringFrame = new FrameLayout(activity);
            LinearLayout.LayoutParams ringLp = new LinearLayout.LayoutParams(ringSize, ringSize);
            ringLp.gravity = Gravity.CENTER_HORIZONTAL;
            ringFrame.setLayoutParams(ringLp);
            ringFrame.setClipChildren(false);
            ringFrame.setClipToPadding(false);

            // Circular Focus Ring Background
            GradientDrawable ringGd = new GradientDrawable();
            ringGd.setShape(GradientDrawable.OVAL);
            ringGd.setColor(Color.TRANSPARENT);
            ringGd.setStroke(Math.round(2f * density), 0x26FFFFFF); // subtle translucent rim
            ringFrame.setBackground(ringGd);

            // Inner Circular Avatar
            FrameLayout innerAvatar = new FrameLayout(activity);
            FrameLayout.LayoutParams innerLp = new FrameLayout.LayoutParams(avatarSize, avatarSize);
            innerLp.gravity = Gravity.CENTER;
            innerAvatar.setLayoutParams(innerLp);

            ProfileAvatarHelper.bindAvatar(innerAvatar, prof, activity);
            ringFrame.addView(innerAvatar);

            // Primary Star Badge on Bottom-Right (like Sara in reference)
            if (prof.isPrimary) {
                FrameLayout starBadge = new FrameLayout(activity);
                int starSize = Math.round(28 * density);
                FrameLayout.LayoutParams starLp = new FrameLayout.LayoutParams(starSize, starSize);
                starLp.gravity = Gravity.BOTTOM | Gravity.END;
                starLp.setMargins(0, 0, Math.round(4 * density), Math.round(4 * density));
                starBadge.setLayoutParams(starLp);

                GradientDrawable starBg = new GradientDrawable();
                starBg.setShape(GradientDrawable.OVAL);
                starBg.setColor(0xFFF59E0B); // Amber gold
                starBg.setStroke(Math.round(2f * density), 0xFF0A0F1D);
                starBadge.setBackground(starBg);

                TextView tvStar = new TextView(activity);
                tvStar.setText("★");
                tvStar.setTextColor(0xFF000000);
                tvStar.setTextSize(14);
                tvStar.setGravity(Gravity.CENTER);
                tvStar.setIncludeFontPadding(false);
                starBadge.addView(tvStar, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                ));

                ringFrame.addView(starBadge);
            }

            card.addView(ringFrame);

            // Profile Name Label - fixed height, perfectly centered
            TextView tvName = new TextView(activity);
            LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    nameHeight
            );
            nameLp.topMargin = Math.round(14 * density);
            tvName.setLayoutParams(nameLp);
            tvName.setText(prof.name);
            tvName.setTextSize(17);
            tvName.setTextColor(0xFF94A3B8);
            tvName.setTypeface(activity.getResources().getFont(R.font.inter), android.graphics.Typeface.BOLD);
            tvName.setGravity(Gravity.CENTER);
            tvName.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            tvName.setSingleLine(true);
            tvName.setEllipsize(TextUtils.TruncateAt.END);
            card.addView(tvName);

            // Role / Subtitle Label (PRIMARY / KANAK-KANAK) - fixed height and slot for all profiles
            TextView tvRole = new TextView(activity);
            LinearLayout.LayoutParams roleLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    roleHeight
            );
            roleLp.topMargin = Math.round(3 * density);
            tvRole.setLayoutParams(roleLp);
            tvRole.setGravity(Gravity.CENTER);
            tvRole.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            tvRole.setTextSize(11);
            tvRole.setLetterSpacing(0.12f);
            tvRole.setTypeface(activity.getResources().getFont(R.font.inter), android.graphics.Typeface.BOLD);

            if (prof.isPrimary) {
                tvRole.setText("PRIMARY");
                tvRole.setTextColor(0xFFF59E0B); // Amber gold
                tvRole.setVisibility(View.VISIBLE);
            } else if (prof.isChild) {
                tvRole.setText("KANAK-KANAK");
                tvRole.setTextColor(0xFF38BDF8); // Sky blue
                tvRole.setVisibility(View.VISIBLE);
            } else {
                tvRole.setText(" ");
                tvRole.setVisibility(View.INVISIBLE); // Maintains exact same vertical height slot
            }
            card.addView(tvRole);

            // Focus & Scale Animation - only scale ringFrame so text labels stay strictly aligned!
            card.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    ringGd.setStroke(Math.round(3.5f * density), 0xFFFFFFFF); // Crisp glowing white rim
                    ringGd.setColor(0x18FFFFFF);
                    ringFrame.animate().scaleX(1.10f).scaleY(1.10f).setDuration(160).start();
                    tvName.setTextColor(0xFFFFFFFF);
                } else {
                    ringGd.setStroke(Math.round(2f * density), 0x26FFFFFF);
                    ringGd.setColor(Color.TRANSPARENT);
                    ringFrame.animate().scaleX(1.0f).scaleY(1.0f).setDuration(160).start();
                    tvName.setTextColor(0xFF94A3B8);
                }
            });

            // Click -> Switch active profile immediately!
            card.setOnClickListener(v -> {
                SoundEffects.playSelect();
                ProfileManager.setActiveProfile(activity, prof.id);
                activity.updateTopBarUserIcon();
                if (dialogHolder[0] != null) dialogHolder[0].dismiss();
            });

            // Long Click -> Open Profile Settings (Tetapan Profil)!
            card.setOnLongClickListener(v -> {
                SoundEffects.playSelect();
                ProfileSettingsDialog.show(activity, prof, () -> {
                    populateProfiles(activity, row, dialogHolder, density);
                    activity.updateTopBarUserIcon();
                });
                return true;
            });

            row.addView(card);
        }

        // Add Profile Card (+) at end of row
        LinearLayout addCard = new LinearLayout(activity);
        addCard.setOrientation(LinearLayout.VERTICAL);
        addCard.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        addCard.setFocusable(true);
        addCard.setClickable(true);
        addCard.setClipChildren(false);
        addCard.setClipToPadding(false);

        LinearLayout.LayoutParams addCardLp = new LinearLayout.LayoutParams(
                cardWidth,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        addCard.setLayoutParams(addCardLp);

        FrameLayout plusRing = new FrameLayout(activity);
        LinearLayout.LayoutParams plusLp = new LinearLayout.LayoutParams(ringSize, ringSize);
        plusLp.gravity = Gravity.CENTER_HORIZONTAL;
        plusRing.setLayoutParams(plusLp);

        GradientDrawable plusGd = new GradientDrawable();
        plusGd.setShape(GradientDrawable.OVAL);
        plusGd.setColor(0x14FFFFFF); // Translucent frosted glass circle
        plusGd.setStroke(Math.round(2f * density), 0x26FFFFFF);
        plusRing.setBackground(plusGd);

        TextView tvPlus = new TextView(activity);
        tvPlus.setText("+");
        tvPlus.setTextColor(0xFF94A3B8);
        tvPlus.setTextSize(36);
        tvPlus.setTypeface(activity.getResources().getFont(R.font.inter), android.graphics.Typeface.NORMAL);
        tvPlus.setGravity(Gravity.CENTER);
        tvPlus.setIncludeFontPadding(false);
        plusRing.addView(tvPlus, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        addCard.addView(plusRing);

        TextView tvAddLabel = new TextView(activity);
        LinearLayout.LayoutParams addLabelLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                nameHeight
        );
        addLabelLp.topMargin = Math.round(14 * density);
        tvAddLabel.setLayoutParams(addLabelLp);
        tvAddLabel.setText("Add Profile");
        tvAddLabel.setTextSize(17);
        tvAddLabel.setTextColor(0xFF94A3B8);
        tvAddLabel.setTypeface(activity.getResources().getFont(R.font.inter), android.graphics.Typeface.BOLD);
        tvAddLabel.setGravity(Gravity.CENTER);
        tvAddLabel.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        tvAddLabel.setSingleLine(true);
        addCard.addView(tvAddLabel);

        // Invisible Spacer for Add Profile matching tvRole height
        TextView tvAddSpacer = new TextView(activity);
        LinearLayout.LayoutParams addSpacerLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                roleHeight
        );
        addSpacerLp.topMargin = Math.round(3 * density);
        tvAddSpacer.setLayoutParams(addSpacerLp);
        tvAddSpacer.setText(" ");
        tvAddSpacer.setVisibility(View.INVISIBLE);
        addCard.addView(tvAddSpacer);

        addCard.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                plusGd.setStroke(Math.round(3.5f * density), 0xFFFFFFFF);
                plusGd.setColor(0x28FFFFFF);
                tvPlus.setTextColor(0xFFFFFFFF);
                tvAddLabel.setTextColor(0xFFFFFFFF);
                plusRing.animate().scaleX(1.10f).scaleY(1.10f).setDuration(160).start();
            } else {
                plusGd.setStroke(Math.round(2f * density), 0x26FFFFFF);
                plusGd.setColor(0x14FFFFFF);
                tvPlus.setTextColor(0xFF94A3B8);
                tvAddLabel.setTextColor(0xFF94A3B8);
                plusRing.animate().scaleX(1.0f).scaleY(1.0f).setDuration(160).start();
            }
        });

        addCard.setOnClickListener(v -> {
            SoundEffects.playSelect();
            AddProfileDialog.show(activity, newProfile -> {
                populateProfiles(activity, row, dialogHolder, density);
                activity.updateTopBarUserIcon();
            });
        });

        row.addView(addCard);
    }
}
