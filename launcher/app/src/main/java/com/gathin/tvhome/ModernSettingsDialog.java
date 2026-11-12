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
import java.util.HashSet;
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
    private static java.lang.ref.WeakReference<AlertDialog> sActiveDialog;

    public static void dismissCurrent() {
        if (sActiveDialog != null) {
            AlertDialog d = sActiveDialog.get();
            if (d != null && d.isShowing()) {
                try {
                    d.dismiss();
                } catch (Exception ignored) {}
            }
            sActiveDialog = null;
        }
    }

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

        // Deep Navy Glassmorphism modal container over existing system wallpaper
        GradientDrawable dialogBg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xD60D1524, 0xDE080D17}
        );
        dialogBg.setCornerRadius(24 * density);
        dialogBg.setStroke((int) (1.5f * density), 0x4D38BDF8); // Thin cyan-blue luminous outline
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

        // Subtle dark navy translucent background for left sidebar
        GradientDrawable leftBg = new GradientDrawable();
        leftBg.setColor(0x280B1322);
        leftCol.setBackground(leftBg);

        final LinearLayout profileCard = new LinearLayout(activity);
        final LinearLayout rightContent = new LinearLayout(activity);

        // 1. Search Bar (Optimized Dark Navy Glass pill container)
        LinearLayout searchBox = new LinearLayout(activity);
        searchBox.setOrientation(LinearLayout.HORIZONTAL);
        searchBox.setGravity(Gravity.CENTER_VERTICAL);
        searchBox.setPadding((int) (14 * density), 0, (int) (14 * density), 0);
        searchBox.setFocusable(true);
        searchBox.setClickable(true);

        GradientDrawable searchBg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x241E293B, 0x1A0F172A}
        );
        searchBg.setCornerRadius(12 * density);
        searchBg.setStroke((int) (1 * density), 0x2638BDF8);
        searchBox.setBackground(searchBg);

        ImageView searchIcon = new ImageView(activity);
        searchIcon.setImageResource(R.drawable.ic_search);
        searchIcon.setColorFilter(0xFF94A3B8);
        LinearLayout.LayoutParams sIconLp = new LinearLayout.LayoutParams((int) (15 * density), (int) (15 * density));
        sIconLp.setMarginEnd((int) (8 * density));
        searchBox.addView(searchIcon, sIconLp);

        TextView searchPrompt = new TextView(activity);
        searchPrompt.setText(AppLanguage.strSettingsSearch(activity));
        searchPrompt.setTextSize(12.5f);
        searchPrompt.setTextColor(0xFF94A3B8);
        searchPrompt.setTypeface(outfit, Typeface.NORMAL);
        searchBox.addView(searchPrompt);

        searchBox.setOnFocusChangeListener((v, hasFocus) -> {
            GradientDrawable g = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    hasFocus ? new int[]{0x4D1E3A5F, 0x33172E4C} : new int[]{0x241E293B, 0x1A0F172A}
            );
            g.setCornerRadius(12 * density);
            g.setStroke((int) ((hasFocus ? 2f : 1f) * density), hasFocus ? 0xFF38BDF8 : 0x2638BDF8);
            v.setBackground(g);
            searchPrompt.setTextColor(hasFocus ? 0xFFF8FAFC : 0xFF94A3B8);
            searchIcon.setColorFilter(hasFocus ? 0xFF38BDF8 : 0xFF94A3B8);
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

        FrameLayout profileAvatarView = new FrameLayout(activity);
        ProfileAvatarHelper.bindAvatar(profileAvatarView, ProfileManager.getActiveProfile(activity), activity);
        LinearLayout.LayoutParams pavLp = new LinearLayout.LayoutParams((int) (38 * density), (int) (38 * density));
        pavLp.setMarginEnd((int) (10 * density));
        profileCard.addView(profileAvatarView, pavLp);

        LinearLayout profileTextCol = new LinearLayout(activity);
        profileTextCol.setOrientation(LinearLayout.VERTICAL);

        TextView tvProfileName = new TextView(activity);
        tvProfileName.setText(ProfileManager.getProfileName(activity));
        tvProfileName.setTextSize(14f);
        tvProfileName.setTextColor(0xFFF8FAFC);
        tvProfileName.setTypeface(outfit, Typeface.BOLD);
        tvProfileName.setSingleLine(true);
        profileTextCol.addView(tvProfileName);

        TextView tvProfileSub = new TextView(activity);
        tvProfileSub.setText(AppLanguage.strProfileSubtitle(activity));
        tvProfileSub.setTextSize(11f);
        tvProfileSub.setTextColor(0xFF94A3B8);
        tvProfileSub.setTypeface(outfit, Typeface.NORMAL);
        profileTextCol.addView(tvProfileSub);

        profileCard.addView(profileTextCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout.LayoutParams pcLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (52 * density));
        pcLp.bottomMargin = (int) (8 * density);
        leftCol.addView(profileCard, pcLp);

        // Vertical divider between Left and Right
        View verticalDivider = new View(activity);
        verticalDivider.setBackgroundColor(0x1F38BDF8);
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
            boolean pFocus = profileCard.hasFocus();
            GradientDrawable pcBg = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    isProfile ? new int[]{0x380284C7, 0x220369A1} : new int[]{0x221E293B, 0x160F172A}
            );
            pcBg.setCornerRadius(14 * density);
            pcBg.setStroke(
                    (int) ((pFocus ? 2f : 1.5f) * density),
                    pFocus ? 0xFF7DD3FC : (isProfile ? 0xFF38BDF8 : 0x1F38BDF8)
            );
            profileCard.setBackground(pcBg);

            ProfileAvatarHelper.bindAvatar(profileAvatarView, ProfileManager.getActiveProfile(activity), activity);
            tvProfileName.setText(ProfileManager.getProfileName(activity));

            for (NavItem item : navItems) {
                if (item.rowView == null) continue;
                boolean isSelected = (activeTab[0] == item.tabId);
                boolean iFocus = item.rowView.hasFocus();
                GradientDrawable rbg = new GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        isSelected ? new int[]{0x330284C7, 0x1A0369A1} : (iFocus ? new int[]{0x261E3A5F, 0x1A1E3A5F} : new int[]{Color.TRANSPARENT, Color.TRANSPARENT})
                );
                rbg.setCornerRadius(12 * density);
                if (iFocus) {
                    rbg.setStroke((int) (2f * density), 0xFF7DD3FC);
                } else if (isSelected) {
                    rbg.setStroke((int) (1.5f * density), 0xFF38BDF8);
                }
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
            }
            updateSidebarSelection.run();
            if (hasFocus && renderRightPane[0] != null) renderRightPane[0].run();
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
            lbl.setTextColor(0xFFF1F5F9);
            lbl.setTypeface(outfit, Typeface.NORMAL);
            row.addView(lbl);

            row.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    activeSidebarView[0] = row;
                    activeTab[0] = item.tabId;
                }
                updateSidebarSelection.run();
                if (hasFocus && renderRightPane[0] != null) renderRightPane[0].run();
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
                tvTitle.setTextColor(0xFFF8FAFC);
                tvTitle.setTypeface(outfit, Typeface.BOLD);
                rightContent.addView(tvTitle);

                ProfileManager.UserProfile activeP = ProfileManager.getActiveProfile(activity);

                // Section 1: Active Profile Avatars
                TextView tvAvatars = new TextView(activity);
                tvAvatars.setText(AppLanguage.strActiveProfileAvatarHeader(activity, activeP.name));
                tvAvatars.setTextSize(11f);
                tvAvatars.setTextColor(0xFF94A3B8);
                tvAvatars.setTypeface(outfit, Typeface.BOLD);
                tvAvatars.setPadding(0, (int) (16 * density), 0, (int) (8 * density));
                rightContent.addView(tvAvatars);

                // Modern 3 Avatar Options Card
                LinearLayout avatarSettingsCard = buildCardRow(
                        activity,
                        density,
                        outfit,
                        AppLanguage.strAvatarCustomizeTitle(activity),
                        AppLanguage.strAvatarCustomizeSub(activity),
                        () -> {
                            SoundEffects.playSelect();
                            ProfileSettingsDialog.show(activity, activeP, () -> {
                                updateSidebarSelection.run();
                                if (renderRightPane[0] != null) renderRightPane[0].run();
                                activity.updateTopBarUserIcon();
                            });
                        }
                );
                rightContent.addView(avatarSettingsCard);

                // Edit Active Profile Name Card
                LinearLayout editNameCard = new LinearLayout(activity);
                editNameCard.setOrientation(LinearLayout.HORIZONTAL);
                editNameCard.setGravity(Gravity.CENTER_VERTICAL);
                editNameCard.setPadding((int) (16 * density), (int) (11 * density), (int) (16 * density), (int) (11 * density));
                editNameCard.setFocusable(true);
                editNameCard.setClickable(true);

                applySoapBubbleCardBg(editNameCard, density, false);

                // Pencil icon squircle
                FrameLayout pencilSquircle = new FrameLayout(activity);
                GradientDrawable psq = new GradientDrawable();
                psq.setColor(0x330284C7);
                psq.setCornerRadius(8 * density);
                pencilSquircle.setBackground(psq);

                ImageView ivPencil = new ImageView(activity);
                ivPencil.setImageResource(R.drawable.ic_pencil);
                ivPencil.setColorFilter(0xFF38BDF8);
                FrameLayout.LayoutParams ivpLp = new FrameLayout.LayoutParams((int) (16 * density), (int) (16 * density), Gravity.CENTER);
                pencilSquircle.addView(ivPencil, ivpLp);

                LinearLayout.LayoutParams psqLp = new LinearLayout.LayoutParams((int) (32 * density), (int) (32 * density));
                psqLp.setMarginEnd((int) (12 * density));
                editNameCard.addView(pencilSquircle, psqLp);

                TextView tvEditLabel = new TextView(activity);
                tvEditLabel.setText(AppLanguage.strEditName(activity));
                tvEditLabel.setTextSize(14f);
                tvEditLabel.setTextColor(0xFFF8FAFC);
                tvEditLabel.setTypeface(outfit, Typeface.BOLD);
                editNameCard.addView(tvEditLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                TextView tvCurName = new TextView(activity);
                tvCurName.setText(activeP.name);
                tvCurName.setTextSize(13.5f);
                tvCurName.setTextColor(0xFF94A3B8);
                tvCurName.setTypeface(outfit, Typeface.NORMAL);
                editNameCard.addView(tvCurName);

                editNameCard.setOnFocusChangeListener((v, hasFocus) -> {
                    applySoapBubbleCardBg(v, density, hasFocus);
                    tvCurName.setTextColor(hasFocus ? 0xFFE0F2FE : 0xFF94A3B8);
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
                encLp.bottomMargin = (int) (12 * density);
                rightContent.addView(editNameCard, encLp);

                // Section 2: Manage All Users (Add & Delete User)
                List<ProfileManager.UserProfile> allProfiles = ProfileManager.getAllProfiles(activity);
                TextView tvUserList = new TextView(activity);
                tvUserList.setText(AppLanguage.strUserListHeader(activity, allProfiles.size()));
                tvUserList.setTextSize(11f);
                tvUserList.setTextColor(0xFF94A3B8);
                tvUserList.setTypeface(outfit, Typeface.BOLD);
                tvUserList.setPadding(0, (int) (4 * density), 0, (int) (8 * density));
                rightContent.addView(tvUserList);

                for (ProfileManager.UserProfile p : allProfiles) {
                    boolean isCur = p.id.equals(activeP.id);

                    LinearLayout pRow = new LinearLayout(activity);
                    pRow.setOrientation(LinearLayout.HORIZONTAL);
                    pRow.setGravity(Gravity.CENTER_VERTICAL);
                    pRow.setPadding((int) (16 * density), (int) (10 * density), (int) (16 * density), (int) (10 * density));
                    pRow.setFocusable(true);
                    pRow.setClickable(true);

                    final Runnable updatePRowBg = () -> {
                        boolean hasFocus = pRow.hasFocus();
                        GradientDrawable pBg = new GradientDrawable(
                                GradientDrawable.Orientation.TOP_BOTTOM,
                                isCur
                                        ? (hasFocus ? new int[]{0x550284C7, 0x380369A1} : new int[]{0x380284C7, 0x220369A1})
                                        : (hasFocus ? new int[]{0x551E3A5F, 0x38172942} : new int[]{0x2E1E293B, 0x1F111827})
                        );
                        pBg.setCornerRadius(14 * density);
                        if (hasFocus) {
                            pBg.setStroke((int) (2f * density), isCur ? 0xFFFFFFFF : 0xFF38BDF8);
                        } else if (isCur) {
                            pBg.setStroke((int) (1.5f * density), 0xFF38BDF8);
                        } else {
                            pBg.setStroke((int) (1f * density), 0x2638BDF8);
                        }
                        pRow.setBackground(pBg);
                    };
                    updatePRowBg.run();

                    FrameLayout ivP = new FrameLayout(activity);
                    ProfileAvatarHelper.bindAvatar(ivP, p, activity);
                    LinearLayout.LayoutParams ivpLp2 = new LinearLayout.LayoutParams((int) (34 * density), (int) (34 * density));
                    ivpLp2.setMarginEnd((int) (12 * density));
                    pRow.addView(ivP, ivpLp2);

                    TextView tvPName = new TextView(activity);
                    String childSuffix = p.isChild ? " (" + AppLanguage.strProfileRoleChild(activity) + ")" : "";
                    tvPName.setText(p.name + childSuffix);
                    tvPName.setTextSize(14f);
                    tvPName.setTextColor(0xFFF8FAFC);
                    tvPName.setTypeface(outfit, isCur ? Typeface.BOLD : Typeface.NORMAL);
                    pRow.addView(tvPName, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                    if (isCur) {
                        TextView tvTag = new TextView(activity);
                        tvTag.setText(AppLanguage.strActiveStatus(activity));
                        tvTag.setTextSize(11f);
                        tvTag.setTextColor(0xFF38BDF8);
                        tvTag.setTypeface(outfit, Typeface.BOLD);
                        tvTag.setPadding((int) (10 * density), (int) (4 * density), (int) (10 * density), (int) (4 * density));

                        GradientDrawable tagBg = new GradientDrawable();
                        tagBg.setCornerRadius(10 * density);
                        tagBg.setColor(0x2638BDF8);
                        tagBg.setStroke((int) (1 * density), 0xFF38BDF8);
                        tvTag.setBackground(tagBg);
                        pRow.addView(tvTag);
                    } else if (allProfiles.size() > 1) {
                        TextView tvDel = new TextView(activity);
                        tvDel.setText(AppLanguage.strDeleteUser(activity));
                        tvDel.setTextSize(11.5f);
                        tvDel.setTextColor(0xFFF87171);
                        tvDel.setTypeface(outfit, Typeface.NORMAL);
                        pRow.addView(tvDel);
                    }

                    pRow.setOnFocusChangeListener((v, hasFocus) -> {
                        updatePRowBg.run();
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
                            ProfileSettingsDialog.show(activity, p, () -> {
                                updateSidebarSelection.run();
                                if (renderRightPane[0] != null) renderRightPane[0].run();
                                activity.updateTopBarUserIcon();
                            });
                        } else {
                            // Show options: Switch, Manage/Avatar, or Delete
                            AlertDialog.Builder ob = new AlertDialog.Builder(activity, R.style.Theme_TvHome_GlassDialog);
                            ob.setTitle("Pengguna: " + p.name);
                            String[] opts = allProfiles.size() > 1 
                                    ? new String[]{"✨ Tukar ke Profil Ini", "⚙️ Urus Profil & Avatar", "🗑️ Padam Pengguna"} 
                                    : new String[]{"✨ Tukar ke Profil Ini", "⚙️ Urus Profil & Avatar"};
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
                                    ProfileSettingsDialog.show(activity, p, () -> {
                                        updateSidebarSelection.run();
                                        if (renderRightPane[0] != null) renderRightPane[0].run();
                                        activity.updateTopBarUserIcon();
                                    });
                                } else if (which == 2) {
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
                LinearLayout addUserBtn = buildCardRow(activity, density, outfit, AppLanguage.strAddNewUser(activity), AppLanguage.strAddNewUserDesc(activity), () -> {
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
                tvPhotos.setTextSize(11f);
                tvPhotos.setTextColor(0xFF94A3B8);
                tvPhotos.setTypeface(outfit, Typeface.BOLD);
                tvPhotos.setPadding(0, (int) (14 * density), 0, (int) (8 * density));
                rightContent.addView(tvPhotos);

                LinearLayout photoUploadRow = buildCardRow(activity, density, outfit, AppLanguage.strManagePhotosTitle(activity), AppLanguage.strManagePhotosSub(activity), () -> {
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
                        "📖 Paparan Teks Di Atas Jadual Solat", "Papar Doa Harian atau Teks Minbar di atas jadual solat",
                        ayahShown, isChecked -> {
                            WeeklyAyah.setShown(activity, isChecked);
                            activity.updateIslamicShelf();
                        }));

                int displayMode = MinbarManager.getDisplayMode(activity);
                String selectedCapsule = displayMode == MinbarManager.MODE_MINBAR ? "✓ Teks Minbar" : "✓ Ayat Al-Quran";
                rightContent.addView(buildCardRowWithBadge(activity, density, outfit,
                        "🕌 Pilihan Kandungan Paparan",
                        "Pilih kandungan teks rohani antara Ayat Al-Quran atau Teks Minbar di atas jadual solat",
                        selectedCapsule,
                        () -> activity.showDisplayModeChoiceDialog(() -> {
                            if (renderRightPane[0] != null) renderRightPane[0].run();
                        })));

                rightContent.addView(buildSwitchRow(activity, density, outfit,
                        "Papan Pemuka Cuaca & Kualiti Udara (AQI)", "Papar suhu live, cuaca dan status jerebu JAS",
                        activity.isWeatherWidgetEnabled(), isChecked -> {
                            activity.setWeatherWidgetEnabled(isChecked);
                        }));

                rightContent.addView(buildSwitchRow(activity, density, outfit,
                        "⚠️ Amaran Cuaca Buruk (Severe Weather Alert)",
                        "Papar banner amaran ribut petir & hujan lebat sebelah widget cuaca",
                        activity.isWeatherWarningEnabled(), isChecked -> {
                            activity.setWeatherWarningEnabled(isChecked);
                        }));

                int curCols = activity.getDockColumns();
                String dockSub = "Kapasiti ikon pada barisan dok utama (Semasa: " + curCols + " Aplikasi • Pilihan 4, 5, atau 6)";
                rightContent.addView(buildCardRow(activity, density, outfit,
                        "📱 Bilangan Aplikasi Dok (Maksimum 6)", dockSub,
                        () -> activity.showDockColumnsDialog(() -> {
                            if (renderRightPane[0] != null) renderRightPane[0].run();
                        })));

                rightContent.addView(buildSwitchRow(activity, density, outfit,
                        "⚡ Animasi Antaramuka (UI Animations)",
                        "Kesan 'spring glider' meluncur, zum ikon melantun & animasi peralihan skrin",
                        activity.isAnimationsEnabled(), isChecked -> {
                            activity.setAnimationsEnabled(isChecked);
                        }));

                boolean isLowSpecActive = DevicePerformance.isLowSpec(activity);
                String lowSpecSub = isLowSpecActive
                        ? "Aktif: Optimum untuk TV 1GB-2GB RAM • Ringan & lancar seperti Versi 3"
                        : "Nyahaktif: Mod animasi penuh untuk peranti TV berprestasi tinggi";
                rightContent.addView(buildSwitchRow(activity, density, outfit,
                        "🚀 Mod Kelancaran TV Rendah (Smooth Mode v3)",
                        lowSpecSub,
                        isLowSpecActive, isChecked -> {
                            DevicePerformance.setOptimizationOverride(activity, isChecked);
                            activity.setAnimationsEnabled(!isChecked);
                            if (renderRightPane[0] != null) renderRightPane[0].run();
                        }));

                rightContent.addView(buildSwitchRow(activity, density, outfit,
                        "📱 Papar Aplikasi Mobile (Show Sideloaded Apps)",
                        "Papar aplikasi telefon pintar & tablet yang dipasang melalui APK di skrin utama TV",
                        activity.isMobileAppsEnabled(), isChecked -> {
                            activity.setMobileAppsEnabled(isChecked);
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

                rightContent.addView(buildSwitchRow(activity, density, outfit,
                        "⚡ Animasi Antaramuka (UI Animations)",
                        "Kesan 'spring glider' meluncur, zum ikon melantun & animasi peralihan skrin",
                        activity.isAnimationsEnabled(), isChecked -> {
                            activity.setAnimationsEnabled(isChecked);
                        }));

                boolean isLowSpecActive = DevicePerformance.isLowSpec(activity);
                String lowSpecSub = isLowSpecActive
                        ? "Aktif: Optimum untuk TV 1GB-2GB RAM • Ringan & lancar seperti Versi 3"
                        : "Nyahaktif: Mod animasi penuh untuk peranti TV berprestasi tinggi";
                rightContent.addView(buildSwitchRow(activity, density, outfit,
                        "🚀 Mod Kelancaran TV Rendah (Smooth Mode v3)",
                        lowSpecSub,
                        isLowSpecActive, isChecked -> {
                            DevicePerformance.setOptimizationOverride(activity, isChecked);
                            activity.setAnimationsEnabled(!isChecked);
                            if (renderRightPane[0] != null) renderRightPane[0].run();
                        }));

                rightContent.addView(buildSwitchRow(activity, density, outfit,
                        "📱 Papar Aplikasi Mobile (Show Sideloaded Apps)",
                        "Papar aplikasi telefon pintar & tablet yang dipasang melalui APK di skrin utama TV",
                        activity.isMobileAppsEnabled(), isChecked -> {
                            activity.setMobileAppsEnabled(isChecked);
                        }));

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

                rightContent.addView(buildSwitchRow(activity, density, outfit,
                        "⚠️ Amaran Cuaca Buruk (Severe Weather Alert)",
                        "Papar amaran ribut petir & hujan lebat MET Malaysia / ramalan sebelah cuaca",
                        activity.isWeatherWarningEnabled(), isChecked -> {
                            activity.setWeatherWarningEnabled(isChecked);
                        }));

                boolean isTestWarning = activity.isWeatherWarningTestMode();
                rightContent.addView(buildSwitchRow(activity, density, outfit,
                        "🧪 Uji Pratonton Amaran Cuaca (Test Warning)",
                        "Paksa paparan amaran ribut petir contoh pada skrin utama untuk semakan visual",
                        isTestWarning, isChecked -> {
                            activity.setWeatherWarningTestMode(isChecked);
                            Toast.makeText(activity, isChecked ? "🧪 Pratonton Amaran Diaktifkan" : "Pratonton Dimatikan", Toast.LENGTH_SHORT).show();
                        }));

            } else if (tab == TAB_STANDBY) {
                // === TAB 4: STANDBY MODE ===
                addHeader(rightContent, outfit, "StandBy Mode");

                boolean redMode = activity.isNightstandRedModeEnabled();
                rightContent.addView(buildSwitchRow(activity, density, outfit,
                        "🔴 Mod Malam Merah (Versi Asal)",
                        "Papar teks jam warna merah monokrom pada waktu malam (10:00 PM – 6:00 AM) ala Apple StandBy / OLED. Matikan untuk jam sentiasa putih bersih.",
                        redMode, isChecked -> {
                            activity.setNightstandRedModeEnabled(isChecked);
                            Toast.makeText(activity, isChecked ? "🔴 Mod Malam Merah Diaktifkan (10 Malam - 6 Pagi)" : "⚪ Jam Sentiasa Putih Bersih", Toast.LENGTH_SHORT).show();
                        }));

                rightContent.addView(buildCardRow(activity, density, outfit,
                        "🌙 Uji StandBy Screensaver Sekarang", "Aktifkan jam ambien StandBy skrin penuh",
                        () -> {
                            if (dialogHolder[0] != null) {
                                try {
                                    dialogHolder[0].dismiss();
                                } catch (Exception ignored) {}
                            }
                            activity.showNightstandScreensaver();
                        }));

            } else if (tab == TAB_REMOTE) {
                // === TAB 5: REMOTE & ACCESSIBILITY ===
                addHeader(rightContent, outfit, "Remote & Accessibility");

                Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                homeIntent.addCategory(Intent.CATEGORY_HOME);
                ResolveInfo resolveInfo = activity.getPackageManager().resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY);
                boolean isDefault = resolveInfo != null && activity.getPackageName().equals(resolveInfo.activityInfo.packageName);
                String activePkg = resolveInfo != null ? resolveInfo.activityInfo.packageName : "Tiada";

                String homeSub = isDefault ? "✅ Aktif sebagai pelancar utama default TV" : "⚠️ Butang Home masih dikawal oleh: " + activePkg;
                rightContent.addView(buildCardRow(activity, density, outfit,
                        "🏠 Tetapkan Glass TV Sebagai Default Home",
                        homeSub + " — Klik untuk panduan kunci butang Home",
                        () -> showDefaultLauncherDialog(activity, density, outfit, isDefault, activePkg)));

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

                BoxSecurityScanner.ScanResult secResult = BoxSecurityScanner.getLastResult();
                String secBadge;
                if (secResult == null) {
                    secBadge = "🔍 Imbas";
                } else if (secResult.isClean()) {
                    secBadge = "✅ Selamat";
                } else {
                    secBadge = "⚠️ " + secResult.threatCount() + " Ancaman!";
                }

                rightContent.addView(buildCardRowWithBadge(activity, density, outfit,
                        "🛡️ Keselamatan & Pengimbas Malware Box",
                        "Periksa malware XCPE, BadBox, botnet firmware kilang & pelindung TV 1GB RAM",
                        secBadge,
                        () -> BoxSecurityDialog.show(activity)));

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

                boolean is1GbBox = DevicePerformance.isUltraLowRam(activity);
                String ramProfile = is1GbBox
                        ? "⚡ Mod 1GB RAM Aktif (Kelancaran UI 60fps Rendah Memori)"
                        : "⚡ Standard TV Mode (" + DevicePerformance.getTotalRamMb(activity) + "MB RAM)";
                rightContent.addView(buildInfoCard(activity, density, outfit,
                        "🚀 Profil Prestasi Peranti", ramProfile));

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
        sActiveDialog = new java.lang.ref.WeakReference<>(dialog);
        dialog.setOnDismissListener(d -> {
            sDpadLeftTarget = null;
            if (sActiveDialog != null && sActiveDialog.get() == d) {
                sActiveDialog = null;
            }
        });
        dialog.show();

        Window win = dialog.getWindow();
        if (win != null) {
            win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            win.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            win.setDimAmount(0.55f);
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
        tv.setTextColor(0xFFF8FAFC);
        tv.setTypeface(font, Typeface.BOLD);
        tv.setPadding(0, 0, 0, (int) (14 * root.getContext().getResources().getDisplayMetrics().density));
        root.addView(tv);
    }

    private static void applySoapBubbleCardBg(View v, float density, boolean hasFocus) {
        GradientDrawable g = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                hasFocus ? new int[]{0x551E3A5F, 0x38172942} : new int[]{0x2E1E293B, 0x1F111827}
        );
        g.setCornerRadius(14 * density);
        if (hasFocus) {
            g.setStroke((int) (2f * density), 0xFF38BDF8); // Electric cyan luminous outline
        } else {
            g.setStroke((int) (1f * density), 0x2638BDF8); // Fine low-contrast cyan-blue border
        }
        v.setBackground(g);
    }

    private static LinearLayout buildCardRow(Context ctx, float density, Typeface font, String title, String subtitle, Runnable action) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding((int) (16 * density), (int) (11 * density), (int) (16 * density), (int) (11 * density));
        card.setFocusable(true);
        card.setClickable(true);

        applySoapBubbleCardBg(card, density, false);

        TextView tvTitle = new TextView(ctx);
        tvTitle.setText(title);
        tvTitle.setTextSize(14f);
        tvTitle.setTextColor(0xFFF8FAFC);
        tvTitle.setTypeface(font, Typeface.BOLD);
        card.addView(tvTitle);

        if (subtitle != null && !subtitle.isEmpty()) {
            TextView tvSub = new TextView(ctx);
            tvSub.setText(subtitle);
            tvSub.setTextSize(12);
            tvSub.setTextColor(0xFF94A3B8);
            tvSub.setTypeface(font, Typeface.NORMAL);
            tvSub.setPadding(0, (int) (3 * density), 0, 0);
            card.addView(tvSub);
        }

        final boolean isLowSpec = DevicePerformance.isLowSpec(ctx);
        card.setOnFocusChangeListener((v, hasFocus) -> {
            applySoapBubbleCardBg(v, density, hasFocus);
            if (!isLowSpec) {
                v.animate()
                        .scaleX(hasFocus ? 1.015f : 1.0f)
                        .scaleY(hasFocus ? 1.015f : 1.0f)
                        .setInterpolator(hasFocus ? new OvershootInterpolator(1.4f) : new DecelerateInterpolator())
                        .setDuration(hasFocus ? 160 : 100)
                        .start();
            }
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

    private static LinearLayout buildCardRowWithBadge(Context ctx, float density, Typeface font, String title, String subtitle, String badgeText, Runnable action) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding((int) (16 * density), (int) (11 * density), (int) (16 * density), (int) (11 * density));
        card.setFocusable(true);
        card.setClickable(true);

        applySoapBubbleCardBg(card, density, false);

        LinearLayout textCol = new LinearLayout(ctx);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        textCol.setLayoutParams(textLp);

        TextView tvTitle = new TextView(ctx);
        tvTitle.setText(title);
        tvTitle.setTextSize(14f);
        tvTitle.setTextColor(0xFFF8FAFC);
        tvTitle.setTypeface(font, Typeface.BOLD);
        textCol.addView(tvTitle);

        if (subtitle != null && !subtitle.isEmpty()) {
            TextView tvSub = new TextView(ctx);
            tvSub.setText(subtitle);
            tvSub.setTextSize(12);
            tvSub.setTextColor(0xFF94A3B8);
            tvSub.setTypeface(font, Typeface.NORMAL);
            tvSub.setPadding(0, (int) (3 * density), 0, 0);
            textCol.addView(tvSub);
        }
        card.addView(textCol);

        if (badgeText != null && !badgeText.isEmpty()) {
            TextView badge = new TextView(ctx);
            badge.setText(badgeText);
            badge.setTextSize(11f);
            badge.setTextColor(0xFF38BDF8);
            badge.setTypeface(font, Typeface.BOLD);
            badge.setGravity(Gravity.CENTER);
            badge.setPadding((int) (12 * density), (int) (4 * density), (int) (12 * density), (int) (4 * density));

            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setShape(GradientDrawable.RECTANGLE);
            badgeBg.setCornerRadius(12 * density);
            badgeBg.setColor(0x2638BDF8);
            badgeBg.setStroke((int) (1 * density), 0xFF38BDF8);
            badge.setBackground(badgeBg);

            LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            badgeLp.setMarginStart((int) (12 * density));
            card.addView(badge, badgeLp);
        }

        final boolean isLowSpec = DevicePerformance.isLowSpec(ctx);
        card.setOnFocusChangeListener((v, hasFocus) -> {
            applySoapBubbleCardBg(v, density, hasFocus);
            if (!isLowSpec) {
                v.animate()
                        .scaleX(hasFocus ? 1.015f : 1.0f)
                        .scaleY(hasFocus ? 1.015f : 1.0f)
                        .setInterpolator(hasFocus ? new OvershootInterpolator(1.4f) : new DecelerateInterpolator())
                        .setDuration(hasFocus ? 160 : 100)
                        .start();
            }
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
        card.setPadding((int) (16 * density), (int) (11 * density), (int) (16 * density), (int) (11 * density));
        card.setFocusable(true);
        card.setClickable(true);

        applySoapBubbleCardBg(card, density, false);

        LinearLayout textCol = new LinearLayout(ctx);
        textCol.setOrientation(LinearLayout.VERTICAL);

        TextView tvTitle = new TextView(ctx);
        tvTitle.setText(title);
        tvTitle.setTextSize(14f);
        tvTitle.setTextColor(0xFFF8FAFC);
        tvTitle.setTypeface(font, Typeface.BOLD);
        textCol.addView(tvTitle);

        if (subtitle != null && !subtitle.isEmpty()) {
            TextView tvSub = new TextView(ctx);
            tvSub.setText(subtitle);
            tvSub.setTextSize(12);
            tvSub.setTextColor(0xFF94A3B8);
            tvSub.setTypeface(font, Typeface.NORMAL);
            tvSub.setPadding(0, (int) (3 * density), 0, 0);
            textCol.addView(tvSub);
        }

        card.addView(textCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Switch sw = new Switch(ctx);
        sw.setChecked(isChecked);
        sw.setFocusable(false);
        sw.setClickable(false);
        card.addView(sw);

        final boolean isLowSpec = DevicePerformance.isLowSpec(ctx);
        card.setOnFocusChangeListener((v, hasFocus) -> {
            applySoapBubbleCardBg(v, density, hasFocus);
            if (!isLowSpec) {
                v.animate()
                        .scaleX(hasFocus ? 1.015f : 1.0f)
                        .scaleY(hasFocus ? 1.015f : 1.0f)
                        .setInterpolator(hasFocus ? new OvershootInterpolator(1.4f) : new DecelerateInterpolator())
                        .setDuration(hasFocus ? 160 : 100)
                        .start();
            }
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
        List<ResolveInfo> apps = new ArrayList<>(pm.queryIntentActivities(mainIntent, 0));
        if (activity.isMobileAppsEnabled()) {
            Intent mobileIntent = new Intent(Intent.ACTION_MAIN, null);
            mobileIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> mob = pm.queryIntentActivities(mobileIntent, 0);
            if (mob != null) apps.addAll(mob);
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

        Set<String> seenPkgs = new HashSet<>();
        List<AppEntry> entryList = new ArrayList<>();
        if (apps != null) {
            for (ResolveInfo ri : apps) {
                if (ri.activityInfo == null) continue;
                String p = ri.activityInfo.packageName;
                if (p.equals(selfPkg) || seenPkgs.contains(p)) continue;
                seenPkgs.add(p);
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
        TextView btnDone = new TextView(activity);
        btnDone.setText("SELESAI (DONE)");
        btnDone.setTextSize(13.5f);
        btnDone.setTextColor(0xFFFFFFFF);
        btnDone.setTypeface(font, Typeface.BOLD);
        btnDone.setGravity(Gravity.CENTER);
        btnDone.setIncludeFontPadding(false);
        btnDone.setFocusable(true);
        btnDone.setClickable(true);

        int doneRadius = Math.round(12 * density);
        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(0xFF10B981);
        btnBg.setCornerRadius(doneRadius);
        btnDone.setBackground(btnBg);

        btnDone.setOnFocusChangeListener((v, hasFocus) -> {
            GradientDrawable g = new GradientDrawable();
            g.setColor(hasFocus ? 0xFF059669 : 0xFF10B981);
            g.setCornerRadius(doneRadius);
            if (hasFocus) g.setStroke((int) (2 * density), 0xFFFFFFFF);
            v.setBackground(g);
        });

        btnDone.setOnClickListener(v -> {
            SoundEffects.playSelect();
            if (pickerHolder[0] != null) pickerHolder[0].dismiss();
            activity.reloadTiles();
        });

        pickerLayout.addView(btnDone, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (40 * density)));

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

        AlertDialog[] dialogHolder = new AlertDialog[1];

        // Root container with clipChildren disabled
        FrameLayout dialogWrap = new FrameLayout(activity);
        dialogWrap.setClipChildren(false);
        dialogWrap.setClipToPadding(false);

        // Elegant Dark Slate Glass Container
        LinearLayout dialogBox = new LinearLayout(activity);
        dialogBox.setOrientation(LinearLayout.VERTICAL);
        dialogBox.setClipChildren(false);
        dialogBox.setClipToPadding(false);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xF4181B24); // Deep translucent glass
        bg.setCornerRadius(20 * density);
        bg.setStroke(Math.round(1.5f * density), 0x3367E8F9); // Subtle cyan rim
        dialogBox.setBackground(bg);
        dialogBox.setPadding(Math.round(24 * density), Math.round(20 * density), Math.round(24 * density), Math.round(20 * density));

        // Dialog Title
        TextView tvTitle = new TextView(activity);
        tvTitle.setText("🏠 Tetapkan Glass TV Sebagai Default Home");
        tvTitle.setTextSize(18f);
        tvTitle.setTextColor(0xFFFFFFFF);
        tvTitle.setTypeface(font, Typeface.BOLD);
        tvTitle.setIncludeFontPadding(false);
        tvTitle.setPadding(0, 0, 0, Math.round(8 * density));
        dialogBox.addView(tvTitle);

        // Subtitle / Status description
        TextView tvDesc = new TextView(activity);
        String desc = isDefault
                ? "✅ Tahniah! Glass TV Home kini merupakan pelancar lalai utama peranti anda. Setiap kali butang Home ditekan pada alat kawalan jauh, anda akan terus kembali ke Glass TV."
                : "⚠️ Pelancar aktif semasa: " + activePkg + "\nSila pilih salah satu kaedah di bawah untuk mengunci Glass TV sebagai pelancar utama:";
        tvDesc.setText(desc);
        tvDesc.setTextSize(12.5f);
        tvDesc.setTextColor(isDefault ? 0xFF34D399 : 0xFF94A3B8);
        tvDesc.setTypeface(font, Typeface.NORMAL);
        tvDesc.setIncludeFontPadding(false);
        tvDesc.setPadding(0, 0, 0, Math.round(14 * density));
        dialogBox.addView(tvDesc);

        // Scrollable container with plenty of breathing room
        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);

        LinearLayout optionsCol = new LinearLayout(activity);
        optionsCol.setOrientation(LinearLayout.VERTICAL);
        optionsCol.setClipChildren(false);
        optionsCol.setClipToPadding(false);
        optionsCol.setPadding(Math.round(2 * density), Math.round(4 * density), Math.round(2 * density), Math.round(4 * density));

        // Selection Option 1: Open TV Settings
        View optSettings = buildSelectionOption(
                activity, density, font,
                "⚙️ Buka Tetapan Pemilih Pelancar TV",
                "Cuba buka menu Android untuk memilih Glass TV sebagai Default Home (jika disokong oleh TV)",
                "Buka Tetapan ⚙️",
                0xFF38BDF8,
                () -> {
                    SoundEffects.playSelect();
                    try {
                        Intent intent = new Intent(Settings.ACTION_HOME_SETTINGS);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        activity.startActivity(intent);
                        if (dialogHolder[0] != null) dialogHolder[0].dismiss();
                    } catch (Exception e1) {
                        try {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            activity.startActivity(intent);
                            if (dialogHolder[0] != null) dialogHolder[0].dismiss();
                        } catch (Exception e2) {
                            Toast.makeText(activity, "Menu tetapan pelancar tidak disokong pada firmware ini.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
        optionsCol.addView(optSettings);

        // Selection Option 2: ADB Command
        View optAdb = buildSelectionOption(
                activity, density, font,
                "💻 Kunci Kekal Melalui Arahan ADB (Sangat Disyorkan)",
                "Jalankan arahan melalui PC:\nadb shell cmd package set-home-activity com.gathin.tvhome/.MainActivity" +
                (isDefault ? "" : "\nNyahdayakan pelancar lama: adb shell pm disable-user --user 0 " + activePkg),
                "Salin ADB 📋",
                0xFF34D399,
                () -> {
                    SoundEffects.playSelect();
                    try {
                        android.content.ClipboardManager cm = (android.content.ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                        if (cm != null) {
                            cm.setPrimaryClip(android.content.ClipData.newPlainText("ADB Command", "adb shell cmd package set-home-activity com.gathin.tvhome/.MainActivity"));
                        }
                        Toast.makeText(activity, "📋 Arahan ADB disalin ke papan keratan", Toast.LENGTH_LONG).show();
                    } catch (Exception ignored) {
                        Toast.makeText(activity, "Arahan: adb shell cmd package set-home-activity com.gathin.tvhome/.MainActivity", Toast.LENGTH_LONG).show();
                    }
                }
        );
        optionsCol.addView(optAdb);

        // Selection Option 3: Phone App / Installer
        View optPhone = buildSelectionOption(
                activity, density, font,
                "📱 Tanpa PC: Gunakan Aplikasi Telefon (Glass TV Installer)",
                "Pasang release/glass-tv-installer.apk pada telefon di Wi-Fi yang sama untuk aktifkan Home dalam 1-klik.",
                "Buka Panduan 📱",
                0xFFA78BFA,
                () -> {
                    SoundEffects.playSelect();
                    Toast.makeText(activity, "Buka glass-tv-installer.apk di telefon anda pada Wi-Fi sama.", Toast.LENGTH_SHORT).show();
                }
        );
        optionsCol.addView(optPhone);

        scroll.addView(optionsCol, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        scrollLp.bottomMargin = Math.round(14 * density);
        dialogBox.addView(scroll, scrollLp);

        // Bottom Action: Tutup Button (styled as uncropped focusable pill TextView)
        TextView btnClose = new TextView(activity);
        btnClose.setText("Tutup");
        btnClose.setTextColor(0xFFFFFFFF);
        btnClose.setTypeface(font, Typeface.BOLD);
        btnClose.setTextSize(14f);
        btnClose.setGravity(Gravity.CENTER);
        btnClose.setIncludeFontPadding(false);
        btnClose.setFocusable(true);
        btnClose.setClickable(true);

        int closeHeight = Math.round(38 * density);
        int closeRadius = Math.round(19 * density);

        GradientDrawable closeIdle = new GradientDrawable();
        closeIdle.setShape(GradientDrawable.RECTANGLE);
        closeIdle.setColor(0xFF2E3440);
        closeIdle.setCornerRadius(closeRadius);
        closeIdle.setStroke(Math.round(1f * density), 0x33FFFFFF);
        btnClose.setBackground(closeIdle);
        btnClose.setPadding(Math.round(24 * density), 0, Math.round(24 * density), 0);

        btnClose.setOnFocusChangeListener((v, hasFocus) -> {
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.RECTANGLE);
            g.setCornerRadius(closeRadius);
            if (hasFocus) {
                g.setColor(0xFF434C5E);
                g.setStroke(Math.round(2f * density), 0xFF67E8F9); // Glowing cyan rim
            } else {
                g.setColor(0xFF2E3440);
                g.setStroke(Math.round(1f * density), 0x33FFFFFF);
            }
            v.setBackground(g);
        });

        btnClose.setOnClickListener(v -> {
            SoundEffects.playSelect();
            if (dialogHolder[0] != null) dialogHolder[0].dismiss();
        });

        LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, closeHeight);
        dialogBox.addView(btnClose, closeLp);

        dialogWrap.addView(dialogBox, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        AlertDialog.Builder pb = new AlertDialog.Builder(activity, R.style.Theme_TvHome_GlassDialog);
        pb.setView(dialogWrap);
        AlertDialog d = pb.create();
        dialogHolder[0] = d;
        sActiveDialog = new java.lang.ref.WeakReference<>(d);
        d.setOnDismissListener(dlg -> {
            if (sActiveDialog != null && sActiveDialog.get() == dlg) {
                sActiveDialog = null;
            }
        });
        d.show();

        Window win = d.getWindow();
        if (win != null) {
            win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int dw = Math.min(Math.round(650 * density), Math.round(activity.getResources().getDisplayMetrics().widthPixels * 0.92f));
            int dh = Math.min(Math.round(520 * density), Math.round(activity.getResources().getDisplayMetrics().heightPixels * 0.88f));
            win.setLayout(dw, dh);
            win.setGravity(Gravity.CENTER);
        }

        optSettings.requestFocus();
    }

    private static View buildSelectionOption(
            Context ctx,
            float density,
            Typeface font,
            String title,
            String subtitle,
            String actionLabel,
            int actionColor,
            Runnable action
    ) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setFocusable(true);
        row.setClickable(true);
        row.setClipChildren(false);
        row.setClipToPadding(false);
        row.setPadding(
                Math.round(16 * density),
                Math.round(12 * density),
                Math.round(16 * density),
                Math.round(12 * density)
        );

        int radius = Math.round(14 * density);

        // Idle background
        GradientDrawable idleBg = new GradientDrawable();
        idleBg.setColor(0x2E1E2838);
        idleBg.setCornerRadius(radius);
        idleBg.setStroke(Math.round(1f * density), 0x2E67E8F9);
        row.setBackground(idleBg);

        // Text Column (Left)
        LinearLayout textCol = new LinearLayout(ctx);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvTitle = new TextView(ctx);
        tvTitle.setText(title);
        tvTitle.setTextSize(14f);
        tvTitle.setTextColor(0xFFFFFFFF);
        tvTitle.setTypeface(font, Typeface.BOLD);
        tvTitle.setIncludeFontPadding(false);
        textCol.addView(tvTitle);

        if (subtitle != null && !subtitle.isEmpty()) {
            TextView tvSub = new TextView(ctx);
            tvSub.setText(subtitle);
            tvSub.setTextSize(11.5f);
            tvSub.setTextColor(0xFF94A3B8);
            tvSub.setTypeface(font, Typeface.NORMAL);
            tvSub.setIncludeFontPadding(false);
            tvSub.setPadding(0, Math.round(4 * density), Math.round(10 * density), 0);
            textCol.addView(tvSub);
        }
        row.addView(textCol);

        // Action Pill Button (Right) - Uncropped focusable indicator pill!
        TextView btnAction = new TextView(ctx);
        btnAction.setText(actionLabel);
        btnAction.setTextSize(12f);
        btnAction.setTextColor(0xFFFFFFFF);
        btnAction.setTypeface(font, Typeface.BOLD);
        btnAction.setIncludeFontPadding(false);
        btnAction.setGravity(Gravity.CENTER);

        int pillHeight = Math.round(32 * density);
        int pillRadius = Math.round(16 * density);

        GradientDrawable pillBg = new GradientDrawable();
        pillBg.setShape(GradientDrawable.RECTANGLE);
        pillBg.setCornerRadius(pillRadius);
        pillBg.setColor((actionColor & 0x00FFFFFF) | 0x33000000);
        pillBg.setStroke(Math.round(1f * density), actionColor);
        btnAction.setBackground(pillBg);
        btnAction.setPadding(Math.round(14 * density), 0, Math.round(14 * density), 0);

        LinearLayout.LayoutParams pillLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, pillHeight);
        pillLp.gravity = Gravity.CENTER_VERTICAL;
        row.addView(btnAction, pillLp);

        // Focus listener on row
        row.setOnFocusChangeListener((v, hasFocus) -> {
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(radius);
            if (hasFocus) {
                g.setColor(0x5533445C);
                g.setStroke(Math.round(2f * density), 0xFF67E8F9); // Luminous cyan glint
                btnAction.setTextColor(0xFFFFFFFF);
                GradientDrawable pGlow = new GradientDrawable();
                pGlow.setShape(GradientDrawable.RECTANGLE);
                pGlow.setCornerRadius(pillRadius);
                pGlow.setColor(actionColor);
                pGlow.setStroke(Math.round(1.5f * density), 0xFFFFFFFF);
                btnAction.setBackground(pGlow);
            } else {
                g.setColor(0x2E1E2838);
                g.setStroke(Math.round(1f * density), 0x2E67E8F9);
                btnAction.setTextColor(0xFFE2E8F0);
                btnAction.setBackground(pillBg);
            }
            v.setBackground(g);
        });

        row.setOnClickListener(v -> {
            if (action != null) action.run();
        });

        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rowLp.bottomMargin = Math.round(8 * density);
        row.setLayoutParams(rowLp);

        return row;
    }

    interface EditNameCallback {
        void onSave(String name);
    }

    interface SwitchCallback {
        void onChanged(boolean isChecked);
    }
}
