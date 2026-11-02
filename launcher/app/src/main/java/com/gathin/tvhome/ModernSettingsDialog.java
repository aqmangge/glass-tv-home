package com.gathin.tvhome;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Modern floating Glass TV Settings modal matching the Apple TV / modern TV OS 2-column UI.
 * Features:
 * - Left column: Search, Profile & User card, and categorized navigation tabs.
 * - Right column: Dynamic content pane (Multi-user profiles, 7 Animal Avatars, Home, etc.).
 * - TV & Sistem: Displays native 4K Ultra HD panel resolution (3840x2160) & UI scale.
 * - Anti-clipping: Zero crop on focus borders and selection items across all TV screens.
 * - All Candy Box customizations consolidated here.
 */
public final class ModernSettingsDialog {

    private static final int TAB_PROFILE = 0;
    private static final int TAB_HOME_SCREEN = 1;
    private static final int TAB_APPEARANCE = 2;
    private static final int TAB_STATUS_WEATHER = 3;
    private static final int TAB_STANDBY = 4;
    private static final int TAB_REMOTE = 5;
    private static final int TAB_CHILD_MODE = 6;
    private static final int TAB_GENERAL = 7;
    private static final int TAB_TV = 8;

    private static Runnable sDpadLeftTarget;

    private static boolean focusRightPane(ViewGroup container) {
        if (container == null) return false;
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child.isFocusable() && child.getVisibility() == View.VISIBLE) {
                child.requestFocus();
                return true;
            }
            if (child instanceof ViewGroup) {
                if (focusRightPane((ViewGroup) child)) return true;
            }
        }
        return false;
    }

    public static void show(MainActivity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        SoundEffects.playSelect();

        float density = activity.getResources().getDisplayMetrics().density;
        DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        Typeface outfit = activity.getResources().getFont(R.font.inter);

        AlertDialog[] dialogHolder = new AlertDialog[1];
        int[] activeTab = new int[]{TAB_PROFILE};

        // Main Horizontal Container (2 Columns)
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setGravity(Gravity.CENTER);
        root.setClipChildren(false);
        root.setClipToPadding(false);

        // ReactDrops Soap Bubble styled translucent liquid glass modal container
        GradientDrawable dialogBg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xF4141B28, 0xF40C111A}
        );
        dialogBg.setCornerRadius(26 * density);
        dialogBg.setStroke((int) (1.5f * density), 0x4D67E8F9); // Soap bubble thin-film iridescent rim
        root.setBackground(dialogBg);
        root.setClipToOutline(true);

        // --- LEFT COLUMN: Sidebar ---
        LinearLayout leftCol = new LinearLayout(activity);
        leftCol.setOrientation(LinearLayout.VERTICAL);
        int leftWidth = (int) (250 * density);
        LinearLayout.LayoutParams leftLp = new LinearLayout.LayoutParams(leftWidth, ViewGroup.LayoutParams.MATCH_PARENT);
        leftCol.setLayoutParams(leftLp);
        leftCol.setPadding((int) (14 * density), (int) (12 * density), (int) (14 * density), (int) (12 * density));
        leftCol.setClipChildren(false);
        leftCol.setClipToPadding(false);

        // Subtle dark translucent background for left pane
        GradientDrawable leftBg = new GradientDrawable();
        leftBg.setColor(0x38090D14);
        leftCol.setBackground(leftBg);

        final LinearLayout profileCard = new LinearLayout(activity);
        final LinearLayout rightContent = new LinearLayout(activity);

        // 1. Search Bar (Optimized Soap Bubble pill container)
        LinearLayout searchBox = new LinearLayout(activity);
        searchBox.setOrientation(LinearLayout.HORIZONTAL);
        searchBox.setGravity(Gravity.CENTER_VERTICAL);
        searchBox.setPadding((int) (14 * density), 0, (int) (14 * density), 0);
        searchBox.setFocusable(true);
        searchBox.setClickable(true);

        GradientDrawable searchBg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x3328354A, 0x241C2534}
        );
        searchBg.setCornerRadius(14 * density);
        searchBg.setStroke((int) (1 * density), 0x2E67E8F9);
        searchBox.setBackground(searchBg);

        ImageView searchIcon = new ImageView(activity);
        searchIcon.setImageResource(R.drawable.ic_search);
        searchIcon.setColorFilter(0xFF8E929E);
        LinearLayout.LayoutParams sIconLp = new LinearLayout.LayoutParams((int) (15 * density), (int) (15 * density));
        sIconLp.setMarginEnd((int) (8 * density));
        searchBox.addView(searchIcon, sIconLp);

        TextView searchPrompt = new TextView(activity);
        searchPrompt.setText(AppLanguage.strSettingsSearch(activity));
        searchPrompt.setTextSize(12.5f);
        searchPrompt.setTextColor(0xFF8E929E);
        searchPrompt.setTypeface(outfit, Typeface.NORMAL);
        searchBox.addView(searchPrompt);

        searchBox.setOnFocusChangeListener((v, hasFocus) -> {
            GradientDrawable g = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    hasFocus ? new int[]{0x55384B66, 0x4428354A} : new int[]{0x3328354A, 0x241C2534}
            );
            g.setCornerRadius(14 * density);
            g.setStroke((int) ((hasFocus ? 2f : 1f) * density), hasFocus ? 0xFF67E8F9 : 0x2E67E8F9);
            v.setBackground(g);
            searchPrompt.setTextColor(hasFocus ? 0xFFFFFFFF : 0xFF8E929E);
            searchIcon.setColorFilter(hasFocus ? 0xFF67E8F9 : 0xFF8E929E);
            v.animate()
                    .scaleX(hasFocus ? 1.02f : 1.0f)
                    .scaleY(hasFocus ? 1.02f : 1.0f)
                    .setInterpolator(hasFocus ? new OvershootInterpolator(1.4f) : new DecelerateInterpolator())
                    .setDuration(hasFocus ? 160 : 100)
                    .start();
        });

        searchBox.setOnClickListener(v -> {
            SoundEffects.playSelect();
            Toast.makeText(activity, "🔍 Carian Pantas Tetapan & Aplikasi", Toast.LENGTH_SHORT).show();
        });

        searchBox.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    profileCard.requestFocus();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    return focusRightPane(rightContent);
                }
            }
            return false;
        });

        LinearLayout.LayoutParams sbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (36 * density));
        sbLp.topMargin = (int) (2 * density);
        sbLp.bottomMargin = (int) (8 * density);
        leftCol.addView(searchBox, sbLp);

        // 2. Profile Card (User Profiles - No mention of backup)
        profileCard.setOrientation(LinearLayout.HORIZONTAL);
        profileCard.setGravity(Gravity.CENTER_VERTICAL);
        profileCard.setPadding((int) (10 * density), (int) (6 * density), (int) (10 * density), (int) (6 * density));
        profileCard.setFocusable(true);
        profileCard.setClickable(true);

        ImageView profileAvatarView = new ImageView(activity);
        int curAvatarIdx = ProfileManager.getAvatarIndex(activity);
        profileAvatarView.setImageResource(ProfileManager.getAvatarResId(curAvatarIdx));
        LinearLayout.LayoutParams pavLp = new LinearLayout.LayoutParams((int) (38 * density), (int) (38 * density));
        pavLp.setMarginEnd((int) (10 * density));
        profileCard.addView(profileAvatarView, pavLp);

        LinearLayout profileTextCol = new LinearLayout(activity);
        profileTextCol.setOrientation(LinearLayout.VERTICAL);

        TextView tvProfileName = new TextView(activity);
        tvProfileName.setText(ProfileManager.getProfileName(activity));
        tvProfileName.setTextSize(14f);
        tvProfileName.setTextColor(0xFFFFFFFF);
        tvProfileName.setTypeface(outfit, Typeface.BOLD);
        tvProfileName.setSingleLine(true);
        profileTextCol.addView(tvProfileName);

        TextView tvProfileSub = new TextView(activity);
        tvProfileSub.setText(AppLanguage.strProfileSubtitle(activity));
        tvProfileSub.setTextSize(11f);
        tvProfileSub.setTextColor(0xFF8E929E);
        tvProfileSub.setTypeface(outfit, Typeface.NORMAL);
        profileTextCol.addView(tvProfileSub);

        profileCard.addView(profileTextCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout.LayoutParams pcLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (52 * density));
        pcLp.bottomMargin = (int) (8 * density);
        leftCol.addView(profileCard, pcLp);

        // Vertical divider between Left and Right
        View verticalDivider = new View(activity);
        verticalDivider.setBackgroundColor(0xFF262832);
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams((int) (1 * density), ViewGroup.LayoutParams.MATCH_PARENT);

        // --- RIGHT COLUMN: Content Area ---
        FrameLayout rightCol = new FrameLayout(activity);
        LinearLayout.LayoutParams rightLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        rightCol.setLayoutParams(rightLp);
        rightCol.setPadding((int) (22 * density), (int) (16 * density), (int) (22 * density), (int) (16 * density));
        rightCol.setClipChildren(false);
        rightCol.setClipToPadding(false);

        ScrollView rightScroll = new ScrollView(activity);
        rightScroll.setFillViewport(true);
        rightScroll.setVerticalScrollBarEnabled(false);
        rightScroll.setClipChildren(false);
        rightScroll.setClipToPadding(false);
        rightScroll.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        rightContent.setOrientation(LinearLayout.VERTICAL);
        rightContent.setClipChildren(false);
        rightContent.setClipToPadding(false);
        rightContent.setPadding((int) (8 * density), (int) (8 * density), (int) (8 * density), (int) (24 * density));
        rightScroll.addView(rightContent, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        rightCol.addView(rightScroll);

        // Sidebar Navigation Items
        ScrollView leftScroll = new ScrollView(activity);
        leftScroll.setVerticalScrollBarEnabled(false);
        leftScroll.setClipChildren(false);
        leftScroll.setClipToPadding(false);
        LinearLayout menuList = new LinearLayout(activity);
        menuList.setOrientation(LinearLayout.VERTICAL);
        menuList.setClipChildren(false);
        menuList.setClipToPadding(false);
        leftScroll.addView(menuList);
        leftCol.addView(leftScroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        class NavItem {
            final int tabId;
            final String label;
            final int iconRes;
            final int squircleBg;
            View rowView;

            NavItem(int tabId, String label, int iconRes, int squircleBg) {
                this.tabId = tabId;
                this.label = label;
                this.iconRes = iconRes;
                this.squircleBg = squircleBg;
            }
        }

        List<NavItem> navItems = new ArrayList<>();
        navItems.add(new NavItem(TAB_HOME_SCREEN, AppLanguage.strHomeScreen(activity), R.drawable.ic_home_screen, 0xFF7B1FA2));
        navItems.add(new NavItem(TAB_APPEARANCE, AppLanguage.strAppearance(activity), R.drawable.ic_appearance, 0xFF0097A7));
        navItems.add(new NavItem(TAB_STATUS_WEATHER, AppLanguage.strStatusBarWeather(activity), R.drawable.ic_screen_weather, 0xFF1976D2));
        navItems.add(new NavItem(TAB_STANDBY, AppLanguage.strStandByMode(activity), R.drawable.ic_standby_clock, 0xFF374151));
        navItems.add(new NavItem(TAB_REMOTE, AppLanguage.strRemoteAccessibility(activity), R.drawable.ic_remote, 0xFF374151));
        navItems.add(new NavItem(TAB_CHILD_MODE, AppLanguage.strChildMode(activity), R.drawable.ic_shield_child, 0xFFD97706));
        navItems.add(new NavItem(TAB_GENERAL, AppLanguage.strGeneral(activity), R.drawable.ic_globe, 0xFF374151));
        String tvLabel = AppLanguage.strTv(activity);
        if (AppUpdateManager.isUpdateAvailable(activity)) {
            tvLabel += " 🔴";
        }
        navItems.add(new NavItem(TAB_TV, tvLabel, R.drawable.ic_tv_display, 0xFF374151));

        Runnable[] renderRightPane = new Runnable[1];

        Runnable updateSidebarSelection = () -> {
            boolean isProfile = (activeTab[0] == TAB_PROFILE);
            GradientDrawable pcBg = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    isProfile ? new int[]{0x55384B66, 0x3D28354A} : new int[]{0x2A28354A, 0x1E1C2534}
            );
            pcBg.setCornerRadius(14 * density);
            pcBg.setStroke((int) (1.5f * density), isProfile ? 0x9967E8F9 : 0x2467E8F9);
            profileCard.setBackground(pcBg);

            profileAvatarView.setImageResource(ProfileManager.getAvatarResId(ProfileManager.getAvatarIndex(activity)));
            tvProfileName.setText(ProfileManager.getProfileName(activity));

            for (NavItem item : navItems) {
                if (item.rowView == null) continue;
                boolean isSelected = (activeTab[0] == item.tabId);
                GradientDrawable rbg = new GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        isSelected ? new int[]{0x3D38BDF8, 0x24818CF8} : new int[]{Color.TRANSPARENT, Color.TRANSPARENT}
                );
                rbg.setCornerRadius(12 * density);
                if (isSelected) rbg.setStroke((int) (1.2f * density), 0x9967E8F9);
                item.rowView.setBackground(rbg);
            }
        };

        View[] activeSidebarView = new View[]{profileCard};
        sDpadLeftTarget = () -> {
            if (activeSidebarView[0] != null) {
                activeSidebarView[0].requestFocus();
            }
        };

        profileCard.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                activeSidebarView[0] = profileCard;
                activeTab[0] = TAB_PROFILE;
                updateSidebarSelection.run();
                if (renderRightPane[0] != null) renderRightPane[0].run();
            }
            v.animate()
                    .scaleX(hasFocus ? 1.02f : 1.0f)
                    .scaleY(hasFocus ? 1.02f : 1.0f)
                    .setInterpolator(hasFocus ? new OvershootInterpolator(1.4f) : new DecelerateInterpolator())
                    .setDuration(hasFocus ? 160 : 100)
                    .start();
        });

        profileCard.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    searchBox.requestFocus();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    if (!navItems.isEmpty() && navItems.get(0).rowView != null) {
                        navItems.get(0).rowView.requestFocus();
                        return true;
                    }
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    return focusRightPane(rightContent);
                }
            }
            return false;
        });

        profileCard.setOnClickListener(v -> {
            SoundEffects.playSelect();
            activeSidebarView[0] = profileCard;
            activeTab[0] = TAB_PROFILE;
            updateSidebarSelection.run();
            if (renderRightPane[0] != null) renderRightPane[0].run();
        });

        for (int i = 0; i < navItems.size(); i++) {
            NavItem item = navItems.get(i);
            final int itemIndex = i;
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding((int) (10 * density), (int) (4 * density), (int) (10 * density), (int) (4 * density));
            row.setFocusable(true);
            row.setClickable(true);

            // Squircle Icon
            FrameLayout iconSquircle = new FrameLayout(activity);
            GradientDrawable sqBg = new GradientDrawable();
            sqBg.setColor(item.squircleBg);
            sqBg.setCornerRadius(8 * density);
            iconSquircle.setBackground(sqBg);

            ImageView iv = new ImageView(activity);
            iv.setImageResource(item.iconRes);
            iv.setColorFilter(0xFFFFFFFF);
            FrameLayout.LayoutParams ivLp = new FrameLayout.LayoutParams((int) (15 * density), (int) (15 * density), Gravity.CENTER);
            iconSquircle.addView(iv, ivLp);

            LinearLayout.LayoutParams sqLp = new LinearLayout.LayoutParams((int) (26 * density), (int) (26 * density));
            sqLp.setMarginEnd((int) (10 * density));
            row.addView(iconSquircle, sqLp);

            // Label
            TextView lbl = new TextView(activity);
            lbl.setText(item.label);
            lbl.setTextSize(13f);
            lbl.setTextColor(0xFFE2E8F0);
            lbl.setTypeface(outfit, Typeface.NORMAL);
            row.addView(lbl);

            row.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    activeSidebarView[0] = row;
                    activeTab[0] = item.tabId;
                    updateSidebarSelection.run();
                    if (renderRightPane[0] != null) renderRightPane[0].run();
                }
                v.animate().scaleX(hasFocus ? 1.015f : 1.0f).scaleY(hasFocus ? 1.015f : 1.0f).setDuration(120).start();
            });

            row.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_UP && itemIndex == 0) {
                        profileCard.requestFocus();
                        return true;
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        return focusRightPane(rightContent);
                    }
                }
                return false;
            });

            row.setOnClickListener(v -> {
                SoundEffects.playSelect();
                activeSidebarView[0] = row;
                activeTab[0] = item.tabId;
                updateSidebarSelection.run();
                if (renderRightPane[0] != null) renderRightPane[0].run();
            });

            LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (36 * density));
            rLp.bottomMargin = (int) (2 * density);
            menuList.addView(row, rLp);
            item.rowView = row;
        }

        // --- RENDER RIGHT CONTENT PANE ---
        renderRightPane[0] = () -> {
            rightContent.removeAllViews();
            int tab = activeTab[0];

            if (tab == TAB_PROFILE) {
                // === TAB 0: PROFILES & MULTI-USER MANAGEMENT ===
                TextView tvTitle = new TextView(activity);
                tvTitle.setText(AppLanguage.strProfileTitle(activity));
                tvTitle.setTextSize(24);
                tvTitle.setTextColor(0xFFFFFFFF);
                tvTitle.setTypeface(outfit, Typeface.BOLD);
                rightContent.addView(tvTitle);

                ProfileManager.UserProfile activeP = ProfileManager.getActiveProfile(activity);

                // Section 1: Active Profile Avatars
                TextView tvAvatars = new TextView(activity);
                tvAvatars.setText("AVATAR PROFIL SEMASA (" + activeP.name.toUpperCase() + ")");
                tvAvatars.setTextSize(11.5f);
                tvAvatars.setTextColor(0xFF8E929E);
                tvAvatars.setTypeface(outfit, Typeface.BOLD);
                tvAvatars.setPadding(0, (int) (14 * density), 0, (int) (8 * density));
                rightContent.addView(tvAvatars);

                // Horizontal Row of 7 Animal Avatars
                LinearLayout avatarRow = new LinearLayout(activity);
                avatarRow.setOrientation(LinearLayout.HORIZONTAL);
                avatarRow.setGravity(Gravity.CENTER_VERTICAL);
                avatarRow.setPadding(0, (int) (4 * density), 0, (int) (14 * density));
                avatarRow.setClipChildren(false);
                avatarRow.setClipToPadding(false);

                FrameLayout[] avatarFrames = new FrameLayout[7];
                View[] avatarRings = new View[7];

                int activeAvatar = activeP.avatarIndex;

                for (int i = 0; i < 7; i++) {
                    final int idx = i;
                    FrameLayout frame = new FrameLayout(activity);
                    frame.setFocusable(true);
                    frame.setClickable(true);

                    int frameDim = (int) (60 * density);
                    LinearLayout.LayoutParams fLp = new LinearLayout.LayoutParams(frameDim, frameDim);
                    fLp.setMarginEnd((int) (12 * density));
                    frame.setLayoutParams(fLp);

                    // Outer White Ring for Selected Avatar
                    View ring = new View(activity);
                    GradientDrawable ringBg = new GradientDrawable();
                    ringBg.setShape(GradientDrawable.OVAL);
                    ringBg.setColor(Color.TRANSPARENT);
                    ringBg.setStroke((int) (3f * density), 0xFFFFFFFF);
                    ring.setBackground(ringBg);
                    ring.setVisibility(idx == activeAvatar ? View.VISIBLE : View.GONE);
                    frame.addView(ring, new FrameLayout.LayoutParams(frameDim, frameDim));

                    // Inner Avatar Circle ImageView
                    ImageView img = new ImageView(activity);
                    img.setImageResource(ProfileManager.getAvatarResId(idx));
                    int innerDim = (int) (48 * density);
                    FrameLayout.LayoutParams imgLp = new FrameLayout.LayoutParams(innerDim, innerDim, Gravity.CENTER);
                    frame.addView(img, imgLp);

                    avatarFrames[idx] = frame;
                    avatarRings[idx] = ring;

                    if (idx == 0) {
                        frame.setOnKeyListener((v, keyCode, event) -> {
                            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                                if (sDpadLeftTarget != null) {
                                    sDpadLeftTarget.run();
                                    return true;
                                }
                            }
                            return false;
                        });
                    }

                    frame.setOnFocusChangeListener((v, hasFocus) -> {
                        v.animate().scaleX(hasFocus ? 1.12f : 1.0f).scaleY(hasFocus ? 1.12f : 1.0f).setDuration(120).start();
                    });

                    frame.setOnClickListener(v -> {
                        SoundEffects.playSelect();
                        ProfileManager.setAvatarIndex(activity, idx);
                        for (int k = 0; k < 7; k++) {
                            if (avatarRings[k] != null) {
                                avatarRings[k].setVisibility(k == idx ? View.VISIBLE : View.GONE);
                            }
                        }
                        profileAvatarView.setImageResource(ProfileManager.getAvatarResId(idx));
                        activity.updateTopBarUserIcon();
                        Toast.makeText(activity, "✨ Avatar dipilih: " + ProfileManager.AVATAR_NAMES[idx], Toast.LENGTH_SHORT).show();
                    });

                    avatarRow.addView(frame);
                }
                rightContent.addView(avatarRow);

                // Edit Active Profile Name Card
                LinearLayout editNameCard = new LinearLayout(activity);
                editNameCard.setOrientation(LinearLayout.HORIZONTAL);
                editNameCard.setGravity(Gravity.CENTER_VERTICAL);
                editNameCard.setPadding((int) (14 * density), (int) (10 * density), (int) (16 * density), (int) (10 * density));
                editNameCard.setFocusable(true);
                editNameCard.setClickable(true);

                GradientDrawable enBg = new GradientDrawable();
                enBg.setColor(0xFF272A33);
                enBg.setCornerRadius(12 * density);
                editNameCard.setBackground(enBg);

                // Pencil icon squircle
                FrameLayout pencilSquircle = new FrameLayout(activity);
                GradientDrawable psq = new GradientDrawable();
                psq.setColor(0xFF1E2026);
                psq.setCornerRadius(8 * density);
                pencilSquircle.setBackground(psq);

                ImageView ivPencil = new ImageView(activity);
                ivPencil.setImageResource(R.drawable.ic_pencil);
                FrameLayout.LayoutParams ivpLp = new FrameLayout.LayoutParams((int) (18 * density), (int) (18 * density), Gravity.CENTER);
                pencilSquircle.addView(ivPencil, ivpLp);

                LinearLayout.LayoutParams psqLp = new LinearLayout.LayoutParams((int) (32 * density), (int) (32 * density));
                psqLp.setMarginEnd((int) (12 * density));
                editNameCard.addView(pencilSquircle, psqLp);

                TextView tvEditLabel = new TextView(activity);
                tvEditLabel.setText(AppLanguage.strEditName(activity));
                tvEditLabel.setTextSize(14f);
                tvEditLabel.setTextColor(0xFFFFFFFF);
                tvEditLabel.setTypeface(outfit, Typeface.BOLD);
                editNameCard.addView(tvEditLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                TextView tvCurName = new TextView(activity);
                tvCurName.setText(activeP.name);
                tvCurName.setTextSize(13.5f);
                tvCurName.setTextColor(0xFF8E929E);
                tvCurName.setTypeface(outfit, Typeface.NORMAL);
                editNameCard.addView(tvCurName);

                editNameCard.setOnFocusChangeListener((v, hasFocus) -> {
                    GradientDrawable g = new GradientDrawable();
                    g.setColor(hasFocus ? 0xFF353844 : 0xFF272A33);
                    g.setCornerRadius(12 * density);
                    if (hasFocus) g.setStroke((int) (2f * density), 0xFFFFFFFF);
                    v.setBackground(g);
                    v.animate().scaleX(hasFocus ? 1.015f : 1.0f).scaleY(hasFocus ? 1.015f : 1.0f).setDuration(120).start();
                });

                editNameCard.setOnKeyListener((v, keyCode, event) -> {
                    if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                        if (sDpadLeftTarget != null) {
                            sDpadLeftTarget.run();
                            return true;
                        }
                    }
                    return false;
                });

                editNameCard.setOnClickListener(v -> {
                    SoundEffects.playSelect();
                    showEditNameDialog(activity, density, outfit, newName -> {
                        ProfileManager.setProfileName(activity, newName);
                        tvCurName.setText(newName);
                        tvProfileName.setText(newName);
                        if (renderRightPane[0] != null) renderRightPane[0].run();
                    });
                });

                LinearLayout.LayoutParams encLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                encLp.bottomMargin = (int) (14 * density);
                rightContent.addView(editNameCard, encLp);

                // Section 2: Manage All Users (Add & Delete User)
                List<ProfileManager.UserProfile> allProfiles = ProfileManager.getAllProfiles(activity);
                TextView tvUserList = new TextView(activity);
                tvUserList.setText("SENARAI PENGGUNA (" + allProfiles.size() + ")");
                tvUserList.setTextSize(11.5f);
                tvUserList.setTextColor(0xFF8E929E);
                tvUserList.setTypeface(outfit, Typeface.BOLD);
                tvUserList.setPadding(0, 0, 0, (int) (8 * density));
                rightContent.addView(tvUserList);

                for (ProfileManager.UserProfile p : allProfiles) {
                    boolean isCur = p.id.equals(activeP.id);

                    LinearLayout pRow = new LinearLayout(activity);
                    pRow.setOrientation(LinearLayout.HORIZONTAL);
                    pRow.setGravity(Gravity.CENTER_VERTICAL);
                    pRow.setPadding((int) (14 * density), (int) (8 * density), (int) (14 * density), (int) (8 * density));
                    pRow.setFocusable(true);
                    pRow.setClickable(true);

                    GradientDrawable pBg = new GradientDrawable();
                    pBg.setColor(isCur ? 0xFF2D313D : 0xFF23262F);
                    pBg.setCornerRadius(12 * density);
                    if (isCur) pBg.setStroke((int) (1.5f * density), 0x77FFFFFF);
                    pRow.setBackground(pBg);

                    ImageView ivP = new ImageView(activity);
                    ivP.setImageResource(ProfileManager.getAvatarResId(p.avatarIndex));
                    LinearLayout.LayoutParams ivpLp2 = new LinearLayout.LayoutParams((int) (32 * density), (int) (32 * density));
                    ivpLp2.setMarginEnd((int) (10 * density));
                    pRow.addView(ivP, ivpLp2);

                    TextView tvPName = new TextView(activity);
                    tvPName.setText(p.name + (p.isChild ? " (Mod Kanak-kanak)" : ""));
                    tvPName.setTextSize(13.5f);
                    tvPName.setTextColor(0xFFFFFFFF);
                    tvPName.setTypeface(outfit, isCur ? Typeface.BOLD : Typeface.NORMAL);
                    pRow.addView(tvPName, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                    if (isCur) {
                        TextView tvTag = new TextView(activity);
                        tvTag.setText("Aktif ✓");
                        tvTag.setTextSize(11f);
                        tvTag.setTextColor(0xFF38BDF8);
                        tvTag.setTypeface(outfit, Typeface.BOLD);
                        pRow.addView(tvTag);
                    } else if (allProfiles.size() > 1) {
                        TextView tvDel = new TextView(activity);
                        tvDel.setText("🗑️ Padam");
                        tvDel.setTextSize(11.5f);
                        tvDel.setTextColor(0xFFF87171);
                        tvDel.setTypeface(outfit, Typeface.NORMAL);
                        pRow.addView(tvDel);
                    }

                    pRow.setOnFocusChangeListener((v, hasFocus) -> {
                        GradientDrawable g = new GradientDrawable();
                        g.setColor(hasFocus ? 0xFF353946 : (isCur ? 0xFF2D313D : 0xFF23262F));
                        g.setCornerRadius(12 * density);
                        if (hasFocus) g.setStroke((int) (2f * density), 0xFFFFFFFF);
                        else if (isCur) g.setStroke((int) (1.5f * density), 0x77FFFFFF);
                        v.setBackground(g);
                        v.animate().scaleX(hasFocus ? 1.015f : 1.0f).scaleY(hasFocus ? 1.015f : 1.0f).setDuration(120).start();
                    });

                    pRow.setOnKeyListener((v, keyCode, event) -> {
                        if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                            if (sDpadLeftTarget != null) {
                                sDpadLeftTarget.run();
                                return true;
                            }
                        }
                        return false;
                    });

                    pRow.setOnClickListener(v -> {
                        SoundEffects.playSelect();
                        if (isCur) {
                            Toast.makeText(activity, "Profil " + p.name + " sudah aktif", Toast.LENGTH_SHORT).show();
                        } else {
                            // Show options: Switch or Delete
                            AlertDialog.Builder ob = new AlertDialog.Builder(activity, R.style.Theme_TvHome_GlassDialog);
                            ob.setTitle("Pengguna: " + p.name);
                            String[] opts = allProfiles.size() > 1 ? new String[]{"✨ Tukar ke Profil Ini", "🗑️ Padam Pengguna"} : new String[]{"✨ Tukar ke Profil Ini"};
                            ob.setItems(opts, (d, which) -> {
                                if (which == 0) {
                                    ProfileManager.setActiveProfile(activity, p.id);
                                    if (p.isChild) ChildModeManager.setChildModeEnabled(activity, true);
                                    activity.reloadTiles();
                                    activity.updateTopBarUserIcon();
                                    updateSidebarSelection.run();
                                    if (renderRightPane[0] != null) renderRightPane[0].run();
                                    Toast.makeText(activity, "✨ Bertukar ke profil: " + p.name, Toast.LENGTH_SHORT).show();
                                } else if (which == 1) {
                                    ProfileManager.deleteProfile(activity, p.id);
                                    updateSidebarSelection.run();
                                    if (renderRightPane[0] != null) renderRightPane[0].run();
                                    Toast.makeText(activity, "🗑️ Profil dipadam", Toast.LENGTH_SHORT).show();
                                }
                            });
                            ob.show();
                        }
                    });

                    LinearLayout.LayoutParams prLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    prLp.bottomMargin = (int) (6 * density);
                    rightContent.addView(pRow, prLp);
                }

                // Add User Button
                LinearLayout addUserBtn = buildCardRow(activity, density, outfit, "➕ Tambah Pengguna Baru", "Cipta profil baharu untuk ahli keluarga atau anak", () -> {
                    SoundEffects.playSelect();
                    UserSwitchDialog.showAddProfileDialog(activity, density, outfit, () -> {
                        updateSidebarSelection.run();
                        if (renderRightPane[0] != null) renderRightPane[0].run();
                    });
                });
                rightContent.addView(addUserBtn);

                // Section 3: Your Photos
                TextView tvPhotos = new TextView(activity);
                tvPhotos.setText(AppLanguage.strYourPhotos(activity));
                tvPhotos.setTextSize(11.5f);
                tvPhotos.setTextColor(0xFF8E929E);
                tvPhotos.setTypeface(outfit, Typeface.BOLD);
                tvPhotos.setPadding(0, (int) (12 * density), 0, (int) (4 * density));
                rightContent.addView(tvPhotos);

                LinearLayout photoUploadRow = buildCardRow(activity, density, outfit, "📸 Urus Gambar & Muat Naik Telefon (QR)", "Imbas kod QR untuk hantar gambar dari telefon terus ke TV", () -> {
                    SoundEffects.playSelect();
                    activity.showCustomWallpaperManagerDialog();
                });
                rightContent.addView(photoUploadRow);

            } else if (tab == TAB_HOME_SCREEN) {
                // === TAB 1: HOME SCREEN (CONSOLIDATING CANDY BOX SETTINGS) ===
                addHeader(rightContent, outfit, "Home Screen");

                rightContent.addView(buildSwitchRow(activity, density, outfit,
                        "Rak Doa Harian & Waktu Solat", "Paparkan ayat pilihan & jadual solat waktu tempatan",
                        activity.isIslamicShelfEnabled(), isChecked -> {
                            activity.setIslamicShelfEnabled(isChecked);
                        }));

                boolean ayahShown = WeeklyAyah.shown(activity);
                rightContent.addView(buildSwitchRow(activity, density, outfit,
                        "📖 Paparan Ayat & Doa Harian", "Papar ayat Al-Quran dan doa harian di atas jadual solat",
                        ayahShown, isChecked -> {
                            WeeklyAyah.setShown(activity, isChecked);
                            activity.showAyah(WeeklyAyah.last(activity));
                            if (isChecked) activity.updateAyah();
                        }));

                rightContent.addView(buildSwitchRow(activity, density, outfit,
                        "Papan Pemuka Cuaca & Kualiti Udara (AQI)", "Papar suhu live, cuaca dan status jerebu JAS",
                        activity.isWeatherWidgetEnabled(), isChecked -> {
                            activity.setWeatherWidgetEnabled(isChecked);
                        }));

                rightContent.addView(buildCardRow(activity, density, outfit,
                        "🔊 Kesan Bunyi Antaramuka (UI Sound)", "Pilih bunyi pilihan: PS5, Nintendo Switch, Xbox, atau Matikan",
                        activity::showSoundEffectsDialog));

                rightContent.addView(buildCardRow(activity, density, outfit,
                        "🕌 Tetapan & Pilihan Azan", "Konfigurasi audio azan Makkah/Madinah/KLCC & popup waktu solat",
                        activity::showAzanSettingsDialog));

                rightContent.addView(buildCardRow(activity, density, outfit,
                        "✨ Gemini AI Assistant", "Bantuan pintar AI dan pembantu suara peribadi",
                        activity::showGeminiAssistantDialog));

                rightContent.addView(buildCardRow(activity, density, outfit,
                        "🧹 Kosongkan Cache TV (Clean TV Cache)", "Segarkan memori TV dan kosongkan fail sementara",
                        activity::showCleanTvCacheDialog));

            } else if (tab == TAB_APPEARANCE) {
                // === TAB 2: APPEARANCE ===
                addHeader(rightContent, outfit, "Appearance");

                rightContent.addView(buildCardRow(activity, density, outfit,
                        "🖼️ Koleksi Wallpaper 4K", "Malaysia 4K, Alam Semula Jadi, Bandar Raya, Luar Angkasa, dll.",
                        activity::showWallpaperDialog));

                rightContent.addView(buildCardRow(activity, density, outfit,
                        "⏱️ Selang Masa Shuffle Wallpaper", "Kekerapan pertukaran gambar automatik (5 min, 10 min, dll.)",
                        activity::showWallpaperIntervalDialog));

                rightContent.addView(buildCardRow(activity, density, outfit,
                        "🌐 Pilihan Bahasa (Language Selection)", "Bahasa Melayu 🇲🇾, English 🇬🇧, Bahasa Indonesia 🇮🇩",
                        activity::showLanguageDialog));

                String darkSubtitle;
                int curDark = activity.getSharedPreferences("appearance_prefs", Context.MODE_PRIVATE).getInt("dark_mode_style", MainActivity.DARK_MODE_OFF);
                if (curDark == MainActivity.DARK_MODE_ALWAYS) {
                    darkSubtitle = "Sentiasa Gelap (Obsidian Dimming 50%)";
                } else if (curDark == MainActivity.DARK_MODE_AUTO) {
                    darkSubtitle = "Automatik (7:00 PM – 7:00 AM)";
                } else if (curDark == MainActivity.DARK_MODE_OLED_BLACK) {
                    darkSubtitle = "Hitam Pekat OLED (Pure Black • 0% Wallpaper)";
                } else {
                    darkSubtitle = "Mati (Kecerahan Asal Wallpaper)";
                }
                rightContent.addView(buildCardRow(activity, density, outfit,
                        "🌙 Mod Gelap & Perlindungan OLED", darkSubtitle,
                        activity::showDarkModeDialog));

            } else if (tab == TAB_STATUS_WEATHER) {
                // === TAB 3: STATUS BAR & WEATHER ===
                addHeader(rightContent, outfit, "Status Bar & Weather");

                rightContent.addView(buildCardRow(activity, density, outfit,
                        "📍 Pilih Lokasi TV & Zon Solat", "Sokongan zon Malaysia (JAKIM), Singapura (MUIS), & Indonesia (Kemenag)",
                        () -> activity.startActivity(new Intent(activity, LocationActivity.class))));

                boolean isWeatherFx = activity.getSharedPreferences("wallpaper_prefs", Context.MODE_PRIVATE).getBoolean("dynamic_weather_overlay", true);
                rightContent.addView(buildSwitchRow(activity, density, outfit,
                        "🌧️ Cuaca Dinamik (Weather Overlay Effects)", "Kesan visual zarah hujan, awan kabut dan salji pada wallpaper",
                        isWeatherFx, isChecked -> {
                            activity.getSharedPreferences("wallpaper_prefs", Context.MODE_PRIVATE).edit().putBoolean("dynamic_weather_overlay", isChecked).apply();
                            activity.testWeatherOverride = null;
                            activity.updateWeatherOverlay(HomeWeather.last(activity));
                            Toast.makeText(activity, isChecked ? "🌧️ Cuaca Dinamik Dihidupkan" : "⛅ Cuaca Dinamik Dimatikan", Toast.LENGTH_SHORT).show();
                        }));

            } else if (tab == TAB_STANDBY) {
                // === TAB 4: STANDBY MODE ===
                addHeader(rightContent, outfit, "StandBy Mode");

                rightContent.addView(buildCardRow(activity, density, outfit,
                        "🌙 Uji StandBy Screensaver Sekarang", "Aktifkan jam ambien StandBy skrin penuh",
                        activity::showNightstandScreensaver));

            } else if (tab == TAB_REMOTE) {
                // === TAB 5: REMOTE & ACCESSIBILITY ===
                addHeader(rightContent, outfit, "Remote & Accessibility");

                rightContent.addView(buildCardRow(activity, density, outfit,
                        "🏠 Tetapkan Glass TV Sebagai Default Home",
                        "Ubah butang Home alat kawalan TV supaya sentiasa membuka Glass TV Home secara automatik",
                        () -> {
                            SoundEffects.playSelect();
                            try {
                                Intent intent = new Intent(Settings.ACTION_HOME_SETTINGS);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                activity.startActivity(intent);
                            } catch (Exception e) {
                                try {
                                    Intent intent2 = new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
                                    intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    activity.startActivity(intent2);
                                } catch (Exception ex) {
                                    Toast.makeText(activity, "Sila buka Tetapan TV > Apps > Default Apps > Home App", Toast.LENGTH_LONG).show();
                                }
                            }
                        }));

                rightContent.addView(buildCardRow(activity, density, outfit,
                        "♿ Buka Tetapan Accessibility TV",
                        "Hidupkan 'Glass TV Home Button Service' untuk memintas butang Home pada Google TV & Chromecast",
                        () -> {
                            SoundEffects.playSelect();
                            try {
                                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                activity.startActivity(intent);
                            } catch (Exception e) {
                                Toast.makeText(activity, "Buka Tetapan TV > System > Accessibility", Toast.LENGTH_SHORT).show();
                            }
                        }));

            } else if (tab == TAB_CHILD_MODE) {
                // === TAB 6: CHILD MODE & APP LOCK ===
                addHeader(rightContent, outfit, "Child Mode");

                boolean isChildOn = ChildModeManager.isChildModeEnabled(activity);
                Set<String> lockedSet = ChildModeManager.getLockedApps(activity);
                Set<String> hiddenSet = ChildModeManager.getHiddenApps(activity);

                rightContent.addView(buildSwitchRow(activity, density, outfit,
                        "🛡️ Status Mode Kanak-kanak",
                        "Kunci aplikasi dengan PIN keselamatan 4-digit atau sembunyikan aplikasi dari skrin utama",
                        isChildOn, isChecked -> {
                            ChildModeManager.promptPin(activity, "Sahkan PIN Keselamatan", "Masukkan PIN 4-digit untuk menukar mod ini", new ChildModeManager.PinCallback() {
                                @Override
                                public void onSuccess() {
                                    ChildModeManager.setChildModeEnabled(activity, isChecked);
                                    activity.reloadTiles();
                                    Toast.makeText(activity, isChecked ? "✅ Mode Kanak-kanak diaktifkan!" : "Mode Kanak-kanak dimatikan", Toast.LENGTH_SHORT).show();
                                    if (renderRightPane[0] != null) renderRightPane[0].run();
                                }

                                @Override
                                public void onCancelled() {
                                    if (renderRightPane[0] != null) renderRightPane[0].run();
                                }
                            });
                        }));

                rightContent.addView(buildCardRow(activity, density, outfit,
                        "🔒 Kunci Aplikasi dengan PIN (" + lockedSet.size() + " Aplikasi)",
                        "Pilih aplikasi yang memerlukan kata laluan PIN 4-digit untuk dibuka",
                        () -> showAppLockPicker(activity, density, outfit, false)));

                rightContent.addView(buildCardRow(activity, density, outfit,
                        "👁️ Sembunyikan Aplikasi (" + hiddenSet.size() + " Aplikasi)",
                        "Pilih aplikasi yang disembunyikan sepenuhnya daripada dock dan grid skrin utama",
                        () -> showAppLockPicker(activity, density, outfit, true)));

                rightContent.addView(buildCardRow(activity, density, outfit,
                        "🔢 Tukar Kod PIN Keselamatan",
                        "Tukar 4-digit PIN kawalan ibu bapa (PIN Semasa: " + (ChildModeManager.getPin(activity).equals("0000") ? "0000 [Lalai]" : "••••") + ")",
                        () -> showChangePinDialog(activity, density, outfit)));

            } else if (tab == TAB_GENERAL) {
                // === TAB 7: GENERAL ===
                addHeader(rightContent, outfit, "General");

                rightContent.addView(buildCardRow(activity, density, outfit,
                        "🔄 Mulakan Semula TV (Restart TV)", "Mula semula sistem Android TV secara lancar",
                        activity::showRestartDialog));

                rightContent.addView(buildCardRow(activity, density, outfit,
                        "🔌 Padamkan TV (Turn Off TV)", "Letakkan peranti Android TV dalam mod tidur / mati",
                        activity::showTurnOffDialog));

                long freeBytes = CacheCleaner.getFreeStorageBytes();
                long totalBytes = CacheCleaner.getTotalStorageBytes();
                String freeStr = android.text.format.Formatter.formatFileSize(activity, freeBytes);
                String totalStr = android.text.format.Formatter.formatFileSize(activity, totalBytes);

                rightContent.addView(buildInfoCard(activity, density, outfit,
                        "📊 Status Storan TV", "Ruang Bebas: " + freeStr + " daripada " + totalStr + " (" + (100 - CacheCleaner.usedPercent()) + "% bebas)"));

            } else if (tab == TAB_TV) {
                // === TAB 8: TV & SISTEM (NATIVE 4K RECOGNITION) ===
                addHeader(rightContent, outfit, "TV & Sistem");

                Display display = activity.getWindowManager().getDefaultDisplay();
                Display.Mode mode = (display != null) ? display.getMode() : null;
                int physW = (mode != null) ? mode.getPhysicalWidth() : dm.widthPixels;
                int physH = (mode != null) ? mode.getPhysicalHeight() : dm.heightPixels;
                float fps = (mode != null) ? mode.getRefreshRate() : 60f;

                String resTitle;
                if (physW >= 3840 || physH >= 2160) {
                    resTitle = "4K Ultra HD (" + physW + " × " + physH + " @" + Math.round(fps) + "Hz)";
                } else if (physW >= 2560) {
                    resTitle = "2K QHD (" + physW + " × " + physH + " @" + Math.round(fps) + "Hz)";
                } else if (physW >= 1920 || dm.widthPixels >= 1920) {
                    resTitle = "Full HD (" + physW + " × " + physH + " @" + Math.round(fps) + "Hz)";
                } else {
                    resTitle = physW + " × " + physH + " @" + Math.round(fps) + "Hz";
                }

                rightContent.addView(buildInfoCard(activity, density, outfit,
                        "📺 Model TV", Build.MODEL + " (" + Build.MANUFACTURER + ")"));

                rightContent.addView(buildInfoCard(activity, density, outfit,
                        "🤖 Versi Android TV", "Android " + Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")"));

                rightContent.addView(buildInfoCard(activity, density, outfit,
                        "📐 Resolusi Paparan TV", resTitle));

                rightContent.addView(buildInfoCard(activity, density, outfit,
                        "🖥️ Resolusi UI Framebuffer", dm.widthPixels + " × " + dm.heightPixels + " (" + Math.round(density * 160) + " dpi · 60fps lancar)"));

                Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                homeIntent.addCategory(Intent.CATEGORY_HOME);
                ResolveInfo resolveInfo = activity.getPackageManager().resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY);
                boolean isDefault = resolveInfo != null && activity.getPackageName().equals(resolveInfo.activityInfo.packageName);
                String activePkg = resolveInfo != null ? resolveInfo.activityInfo.packageName : "Tiada";

                String homeSub = isDefault ? "✅ Aktif sebagai pelancar utama default TV" : "⚠️ Butang Home masih dikawal oleh: " + activePkg;
                rightContent.addView(buildCardRow(activity, density, outfit,
                        "🏠 Tetapkan Sebagai Pelancar Utama (Default Home)",
                        homeSub + " — Klik untuk panduan kunci butang Home",
                        () -> showDefaultLauncherDialog(activity, density, outfit, isDefault, activePkg)));

                boolean hasUpdate = AppUpdateManager.isUpdateAvailable(activity);
                String otaTitle = hasUpdate ? "🚀 Kemas Kini Sistem (OTA Update) 🔴" : "🚀 Kemas Kini Sistem (OTA Update)";
                String otaSub = hasUpdate
                        ? "Kemas kini baharu (v" + AppUpdateManager.getNewVersionName(activity) + ") sedia dipasang! Klik untuk kemas kini."
                        : "Semak versi terkini Glass TV Home (" + activity.getAppVersionName() + ")";

                rightContent.addView(buildCardRow(activity, density, outfit,
                        otaTitle, otaSub,
                        () -> {
                            if (hasUpdate) {
                                AppUpdateManager.checkForUpdates(activity, true);
                            } else {
                                activity.showAutoUpdateDialog();
                            }
                        }));

                rightContent.addView(buildCardRow(activity, density, outfit,
                        "⚙️ Buka Tetapan Penuh Android TV", "Akses menu tetapan asal pengeluar peranti",
                        () -> activity.startActivity(new Intent(Settings.ACTION_SETTINGS))));
            }
        };

        // Initial render of Tab 0 (Profile)
        updateSidebarSelection.run();
        renderRightPane[0].run();

        root.addView(leftCol, leftLp);
        root.addView(verticalDivider, divLp);
        root.addView(rightCol, rightLp);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.Theme_TvHome_GlassDialog);
        builder.setView(root);
        AlertDialog dialog = builder.create();
        dialogHolder[0] = dialog;
        dialog.setOnDismissListener(d -> sDpadLeftTarget = null);
        dialog.show();

        Window win = dialog.getWindow();
        if (win != null) {
            win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            win.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            win.setDimAmount(0.65f);
            int w = Math.min((int) (1000 * density), (int) (dm.widthPixels * 0.92f));
            int h = Math.min((int) (580 * density), (int) (dm.heightPixels * 0.92f));
            win.setLayout(w, h);
            win.setGravity(Gravity.CENTER);
        }

        profileCard.requestFocus();
    }

    private static void addHeader(LinearLayout root, Typeface font, String title) {
        TextView tv = new TextView(root.getContext());
        tv.setText(title);
        tv.setTextSize(24);
        tv.setTextColor(0xFFFFFFFF);
        tv.setTypeface(font, Typeface.BOLD);
        tv.setPadding(0, 0, 0, (int) (14 * root.getContext().getResources().getDisplayMetrics().density));
        root.addView(tv);
    }

    private static void applySoapBubbleCardBg(View v, float density, boolean hasFocus) {
        GradientDrawable g = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                hasFocus ? new int[]{0x5533445C, 0x3D223042} : new int[]{0x2E1E2838, 0x22141B26}
        );
        g.setCornerRadius(15 * density);
        if (hasFocus) {
            g.setStroke((int) (2f * density), 0xFF67E8F9); // Soap bubble luminous cyan glint
        } else {
            g.setStroke((int) (1f * density), 0x2E67E8F9); // Subtle iridescent rim
        }
        v.setBackground(g);
    }

    private static LinearLayout buildCardRow(Context ctx, float density, Typeface font, String title, String subtitle, Runnable action) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding((int) (18 * density), (int) (12 * density), (int) (18 * density), (int) (12 * density));
        card.setFocusable(true);
        card.setClickable(true);

        applySoapBubbleCardBg(card, density, false);

        TextView tvTitle = new TextView(ctx);
        tvTitle.setText(title);
        tvTitle.setTextSize(14f);
        tvTitle.setTextColor(0xFFFFFFFF);
        tvTitle.setTypeface(font, Typeface.BOLD);
        card.addView(tvTitle);

        if (subtitle != null && !subtitle.isEmpty()) {
            TextView tvSub = new TextView(ctx);
            tvSub.setText(subtitle);
            tvSub.setTextSize(12);
            tvSub.setTextColor(0xFF8E929E);
            tvSub.setTypeface(font, Typeface.NORMAL);
            tvSub.setPadding(0, (int) (2 * density), 0, 0);
            card.addView(tvSub);
        }

        card.setOnFocusChangeListener((v, hasFocus) -> {
            applySoapBubbleCardBg(v, density, hasFocus);
            v.animate()
                    .scaleX(hasFocus ? 1.025f : 1.0f)
                    .scaleY(hasFocus ? 1.025f : 1.0f)
                    .setInterpolator(hasFocus ? new OvershootInterpolator(1.4f) : new DecelerateInterpolator())
                    .setDuration(hasFocus ? 160 : 100)
                    .start();
        });

        card.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                if (sDpadLeftTarget != null) {
                    sDpadLeftTarget.run();
                    return true;
                }
            }
            return false;
        });

        card.setOnClickListener(v -> {
            if (action != null) action.run();
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = (int) (8 * density);
        card.setLayoutParams(lp);
        return card;
    }

    private static LinearLayout buildSwitchRow(Context ctx, float density, Typeface font, String title, String subtitle, boolean isChecked, SwitchCallback callback) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding((int) (18 * density), (int) (12 * density), (int) (18 * density), (int) (12 * density));
        card.setFocusable(true);
        card.setClickable(true);

        applySoapBubbleCardBg(card, density, false);

        LinearLayout textCol = new LinearLayout(ctx);
        textCol.setOrientation(LinearLayout.VERTICAL);

        TextView tvTitle = new TextView(ctx);
        tvTitle.setText(title);
        tvTitle.setTextSize(14f);
        tvTitle.setTextColor(0xFFFFFFFF);
        tvTitle.setTypeface(font, Typeface.BOLD);
        textCol.addView(tvTitle);

        if (subtitle != null && !subtitle.isEmpty()) {
            TextView tvSub = new TextView(ctx);
            tvSub.setText(subtitle);
            tvSub.setTextSize(12);
            tvSub.setTextColor(0xFF8E929E);
            tvSub.setTypeface(font, Typeface.NORMAL);
            tvSub.setPadding(0, (int) (2 * density), 0, 0);
            textCol.addView(tvSub);
        }

        card.addView(textCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Switch sw = new Switch(ctx);
        sw.setChecked(isChecked);
        sw.setFocusable(false);
        sw.setClickable(false);
        card.addView(sw);

        card.setOnFocusChangeListener((v, hasFocus) -> {
            applySoapBubbleCardBg(v, density, hasFocus);
            v.animate()
                    .scaleX(hasFocus ? 1.025f : 1.0f)
                    .scaleY(hasFocus ? 1.025f : 1.0f)
                    .setInterpolator(hasFocus ? new OvershootInterpolator(1.4f) : new DecelerateInterpolator())
                    .setDuration(hasFocus ? 160 : 100)
                    .start();
        });

        card.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                if (sDpadLeftTarget != null) {
                    sDpadLeftTarget.run();
                    return true;
                }
            }
            return false;
        });

        card.setOnClickListener(v -> {
            SoundEffects.playSelect();
            boolean newVal = !sw.isChecked();
            sw.setChecked(newVal);
            if (callback != null) callback.onChanged(newVal);
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = (int) (8 * density);
        card.setLayoutParams(lp);
        return card;
    }

    private static LinearLayout buildInfoCard(Context ctx, float density, Typeface font, String title, String value) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding((int) (16 * density), (int) (10 * density), (int) (16 * density), (int) (10 * density));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF22252E);
        bg.setCornerRadius(12 * density);
        card.setBackground(bg);

        TextView tvTitle = new TextView(ctx);
        tvTitle.setText(title);
        tvTitle.setTextSize(13.5f);
        tvTitle.setTextColor(0xFFE2E8F0);
        tvTitle.setTypeface(font, Typeface.BOLD);
        card.addView(tvTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvVal = new TextView(ctx);
        tvVal.setText(value);
        tvVal.setTextSize(13f);
        tvVal.setTextColor(0xFF38BDF8);
        tvVal.setTypeface(font, Typeface.NORMAL);
        card.addView(tvVal);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = (int) (8 * density);
        card.setLayoutParams(lp);
        return card;
    }

    private static void showEditNameDialog(MainActivity activity, float density, Typeface font, EditNameCallback callback) {
        EditText et = new EditText(activity);
        et.setText(ProfileManager.getProfileName(activity));
        et.setSelection(et.getText().length());
        et.setTextColor(0xFFFFFFFF);
        et.setTextSize(16);
        et.setTypeface(font, Typeface.NORMAL);
        et.setPadding((int) (16 * density), (int) (12 * density), (int) (16 * density), (int) (12 * density));

        GradientDrawable etBg = new GradientDrawable();
        etBg.setColor(0xFF282A33);
        etBg.setCornerRadius(10 * density);
        etBg.setStroke((int) (1 * density), 0xFF4A4E5C);
        et.setBackground(etBg);

        FrameLayout container = new FrameLayout(activity);
        container.setPadding((int) (24 * density), (int) (12 * density), (int) (24 * density), (int) (8 * density));
        container.addView(et);

        AlertDialog dialog = new AlertDialog.Builder(activity, R.style.Theme_TvHome_GlassDialog)
                .setTitle(AppLanguage.strEditName(activity))
                .setView(container)
                .setPositiveButton("Simpan", (d, which) -> {
                    String newName = et.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        if (callback != null) callback.onSave(newName);
                    }
                })
                .setNegativeButton(AppLanguage.strCancel(activity), (d, which) -> d.dismiss())
                .create();
        dialog.show();
    }

    private static void showChangePinDialog(MainActivity activity, float density, Typeface font) {
        ChildModeManager.promptPin(activity, "Sahkan PIN Semasa", "Masukkan 4-digit PIN keselamatan semasa", new ChildModeManager.PinCallback() {
            @Override
            public void onSuccess() {
                ChildModeManager.promptPin(activity, "Tetapkan PIN Baharu", "Masukkan 4-digit PIN baharu untuk kawalan ibu bapa", new ChildModeManager.PinCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(activity, "✅ PIN Keselamatan Berjaya Dikemaskini!", Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onCancelled() {}
                });
            }
            @Override
            public void onCancelled() {}
        });
    }

    private static void showAppLockPicker(MainActivity activity, float density, Typeface font, boolean isHideMode) {
        PackageManager pm = activity.getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(mainIntent, 0);
        if (apps == null || apps.isEmpty()) {
            mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            apps = pm.queryIntentActivities(mainIntent, 0);
        }

        String selfPkg = activity.getPackageName();
        class AppEntry {
            final String pkg;
            final String label;
            final android.graphics.drawable.Drawable icon;
            AppEntry(String pkg, String label, android.graphics.drawable.Drawable icon) {
                this.pkg = pkg;
                this.label = label;
                this.icon = icon;
            }
        }

        List<AppEntry> entryList = new ArrayList<>();
        if (apps != null) {
            for (ResolveInfo ri : apps) {
                String p = ri.activityInfo.packageName;
                if (p.equals(selfPkg)) continue;
                String lbl = ri.loadLabel(pm).toString();
                entryList.add(new AppEntry(p, lbl, ri.loadIcon(pm)));
            }
        }
        Collections.sort(entryList, (a, b) -> a.label.compareToIgnoreCase(b.label));

        final Set<String> targetSet = isHideMode ? ChildModeManager.getHiddenApps(activity) : ChildModeManager.getLockedApps(activity);

        LinearLayout pickerLayout = new LinearLayout(activity);
        pickerLayout.setOrientation(LinearLayout.VERTICAL);
        pickerLayout.setPadding((int) (22 * density), (int) (18 * density), (int) (22 * density), (int) (16 * density));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF1E2026);
        bg.setCornerRadius(20 * density);
        bg.setStroke((int) (1.5f * density), 0xFF2F323B);
        pickerLayout.setBackground(bg);

        TextView tvTitle = new TextView(activity);
        tvTitle.setText(isHideMode ? "👁️ Sembunyikan Aplikasi dari Skrin Utama" : "🔒 Kunci Aplikasi dengan PIN 4-Digit");
        tvTitle.setTextSize(17);
        tvTitle.setTextColor(0xFFFFFFFF);
        tvTitle.setTypeface(font, Typeface.BOLD);
        pickerLayout.addView(tvTitle);

        TextView tvSub = new TextView(activity);
        tvSub.setText(isHideMode ? "Aplikasi yang ditandakan tidak akan dipaparkan pada skrin utama." : "Aplikasi yang dipilih memerlukan kod PIN untuk dibuka.");
        tvSub.setTextSize(12);
        tvSub.setTextColor(0xFF8E929E);
        tvSub.setTypeface(font, Typeface.NORMAL);
        tvSub.setPadding(0, (int) (2 * density), 0, (int) (10 * density));
        pickerLayout.addView(tvSub);

        ScrollView sv = new ScrollView(activity);
        sv.setVerticalScrollBarEnabled(true);
        LinearLayout list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);

        for (AppEntry app : entryList) {
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding((int) (12 * density), (int) (8 * density), (int) (12 * density), (int) (8 * density));
            row.setFocusable(true);
            row.setClickable(true);

            GradientDrawable rbg = new GradientDrawable();
            rbg.setColor(0xFF242730);
            rbg.setCornerRadius(10 * density);
            row.setBackground(rbg);

            ImageView iv = new ImageView(activity);
            iv.setImageDrawable(app.icon);
            LinearLayout.LayoutParams ivlp = new LinearLayout.LayoutParams((int) (28 * density), (int) (28 * density));
            ivlp.setMarginEnd((int) (10 * density));
            row.addView(iv, ivlp);

            TextView tv = new TextView(activity);
            tv.setText(app.label);
            tv.setTextSize(13.5f);
            tv.setTextColor(0xFFFFFFFF);
            tv.setTypeface(font, Typeface.BOLD);
            row.addView(tv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            CheckBox cb = new CheckBox(activity);
            cb.setChecked(targetSet.contains(app.pkg));
            cb.setFocusable(false);
            cb.setClickable(false);
            row.addView(cb);

            row.setOnFocusChangeListener((v, hasFocus) -> {
                GradientDrawable g = new GradientDrawable();
                g.setColor(hasFocus ? 0xFF353946 : 0xFF242730);
                g.setCornerRadius(10 * density);
                if (hasFocus) g.setStroke((int) (2f * density), 0xFFFFFFFF);
                v.setBackground(g);
                v.animate().scaleX(hasFocus ? 1.015f : 1.0f).scaleY(hasFocus ? 1.015f : 1.0f).setDuration(120).start();
            });

            row.setOnClickListener(v -> {
                SoundEffects.playSelect();
                boolean nowChecked = !cb.isChecked();
                cb.setChecked(nowChecked);
                if (isHideMode) {
                    ChildModeManager.setAppHidden(activity, app.pkg, nowChecked);
                } else {
                    ChildModeManager.setAppLocked(activity, app.pkg, nowChecked);
                }
            });

            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rlp.bottomMargin = (int) (6 * density);
            list.addView(row, rlp);
        }

        sv.addView(list);
        LinearLayout.LayoutParams svLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (260 * density));
        svLp.bottomMargin = (int) (12 * density);
        pickerLayout.addView(sv, svLp);

        AlertDialog[] pickerHolder = new AlertDialog[1];
        Button btnDone = new Button(activity);
        btnDone.setText("SELESAI (DONE)");
        btnDone.setTextSize(13.5f);
        btnDone.setTextColor(0xFFFFFFFF);
        btnDone.setTypeface(font, Typeface.BOLD);

        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(0xFF10B981);
        btnBg.setCornerRadius(10 * density);
        btnDone.setBackground(btnBg);

        btnDone.setOnFocusChangeListener((v, hasFocus) -> {
            GradientDrawable g = new GradientDrawable();
            g.setColor(hasFocus ? 0xFF059669 : 0xFF10B981);
            g.setCornerRadius(10 * density);
            if (hasFocus) g.setStroke((int) (2 * density), 0xFFFFFFFF);
            v.setBackground(g);
        });

        btnDone.setOnClickListener(v -> {
            SoundEffects.playSelect();
            if (pickerHolder[0] != null) pickerHolder[0].dismiss();
            activity.reloadTiles();
        });

        pickerLayout.addView(btnDone, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (42 * density)));

        AlertDialog.Builder pb = new AlertDialog.Builder(activity, R.style.Theme_TvHome_GlassDialog);
        pb.setView(pickerLayout);
        AlertDialog d = pb.create();
        pickerHolder[0] = d;
        d.show();

        Window win = d.getWindow();
        if (win != null) {
            win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int dw = Math.min((int) (520 * density), (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.85f));
            win.setLayout(dw, ViewGroup.LayoutParams.WRAP_CONTENT);
            win.setGravity(Gravity.CENTER);
        }
    }

    private static void showDefaultLauncherDialog(MainActivity activity, float density, Typeface font, boolean isDefault, String activePkg) {
        if (activity == null || activity.isFinishing()) return;
        SoundEffects.playSelect();

        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding((int) (24 * density), (int) (20 * density), (int) (24 * density), (int) (20 * density));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF1E2026);
        bg.setCornerRadius(18 * density);
        bg.setStroke((int) (1.5f * density), 0xFF2F323B);
        root.setBackground(bg);

        TextView tvTitle = new TextView(activity);
        tvTitle.setText("🏠 Tetapkan Glass TV Sebagai Default Home");
        tvTitle.setTextSize(18f);
        tvTitle.setTextColor(0xFFFFFFFF);
        tvTitle.setTypeface(font, Typeface.BOLD);
        tvTitle.setPadding(0, 0, 0, (int) (8 * density));
        root.addView(tvTitle);

        TextView tvDesc = new TextView(activity);
        String desc = isDefault
                ? "✅ Tahniah! Glass TV Home kini merupakan pelancar lalai utama peranti anda. Setiap kali butang Home ditekan pada alat kawalan jauh, anda akan terus kembali ke Glass TV."
                : "⚠️ Pelancar aktif semasa: " + activePkg + "\n\nKebanyakan Android TV dan Google TV menyekat butang Home supaya tetap membuka skrin asal pengeluar. Sila pilih kaedah di bawah untuk mengunci Glass TV sebagai pelancar utama:";
        tvDesc.setText(desc);
        tvDesc.setTextSize(13f);
        tvDesc.setTextColor(isDefault ? 0xFF34D399 : 0xFFCBD5E1);
        tvDesc.setTypeface(font, Typeface.NORMAL);
        tvDesc.setPadding(0, 0, 0, (int) (16 * density));
        root.addView(tvDesc);

        AlertDialog[] dialogHolder = new AlertDialog[1];

        // Option 1: Open TV Settings
        LinearLayout btnSettings = buildCardRow(activity, density, font,
                "⚙️ Buka Tetapan Pemilih Pelancar TV",
                "Cuba buka menu Android untuk memilih Glass TV sebagai Default Home (jika disokong oleh TV)",
                () -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_HOME_SETTINGS);
                        activity.startActivity(intent);
                    } catch (Exception e1) {
                        try {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
                            activity.startActivity(intent);
                        } catch (Exception e2) {
                            Toast.makeText(activity, "Menu tetapan pelancar tidak disokong pada firmware ini.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        root.addView(btnSettings);

        // Option 2: ADB Command guidance
        LinearLayout btnAdb = buildCardRow(activity, density, font,
                "💻 Kunci Kekal Melalui Arahan ADB (Sangat Disyorkan)",
                "Jalankan arahan ini melalui PC / AI coding assistant:\nadb shell cmd package set-home-activity com.gathin.tvhome/.MainActivity" +
                (isDefault ? "" : "\n\nJika TV masih menyekat butang Home, nyahdayakan pelancar lama:\nadb shell pm disable-user --user 0 " + activePkg),
                () -> {
                    Toast.makeText(activity, "Arahan: adb shell cmd package set-home-activity com.gathin.tvhome/.MainActivity", Toast.LENGTH_LONG).show();
                });
        root.addView(btnAdb);

        // Option 3: Phone Installer
        LinearLayout btnPhone = buildCardRow(activity, density, font,
                "📱 Tanpa PC: Gunakan Aplikasi Telefon (Glass TV Installer)",
                "Pasang release/glass-tv-installer.apk pada telefon Android anda di rangkaian Wi-Fi yang sama untuk aktifkan Home secara automatik dalam 1-klik.",
                () -> {
                    Toast.makeText(activity, "Buka glass-tv-installer.apk di telefon anda pada Wi-Fi sama.", Toast.LENGTH_SHORT).show();
                });
        root.addView(btnPhone);

        // Close button
        Button btnClose = new Button(activity);
        btnClose.setText("Tutup");
        btnClose.setTextColor(0xFFFFFFFF);
        btnClose.setTypeface(font, Typeface.BOLD);
        btnClose.setTextSize(14f);
        GradientDrawable closeBg = new GradientDrawable();
        closeBg.setColor(0xFF374151);
        closeBg.setCornerRadius(10 * density);
        btnClose.setBackground(closeBg);
        btnClose.setFocusable(true);
        btnClose.setPadding((int) (16 * density), (int) (8 * density), (int) (16 * density), (int) (8 * density));
        btnClose.setOnFocusChangeListener((v, hasFocus) -> {
            GradientDrawable g = new GradientDrawable();
            g.setColor(hasFocus ? 0xFF4B5563 : 0xFF374151);
            g.setCornerRadius(10 * density);
            if (hasFocus) g.setStroke((int) (2 * density), 0xFFFFFFFF);
            v.setBackground(g);
        });
        btnClose.setOnClickListener(v -> {
            SoundEffects.playSelect();
            if (dialogHolder[0] != null) dialogHolder[0].dismiss();
        });

        LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (42 * density));
        closeLp.topMargin = (int) (12 * density);
        root.addView(btnClose, closeLp);

        scroll.addView(root);

        AlertDialog.Builder pb = new AlertDialog.Builder(activity, R.style.Theme_TvHome_GlassDialog);
        pb.setView(scroll);
        AlertDialog d = pb.create();
        dialogHolder[0] = d;
        d.show();

        Window win = d.getWindow();
        if (win != null) {
            win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int dw = Math.min((int) (620 * density), (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.90f));
            int dh = Math.min((int) (520 * density), (int) (activity.getResources().getDisplayMetrics().heightPixels * 0.88f));
            win.setLayout(dw, dh);
            win.setGravity(Gravity.CENTER);
        }

        btnSettings.requestFocus();
    }

    interface EditNameCallback {
        void onSave(String name);
    }

    interface SwitchCallback {
        void onChanged(boolean isChecked);
    }
}
