package com.gathin.tvhome;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;

/**
 * Interactive TV Dialog allowing users to customize Avatar Character & Background colors
 * with live animated Lottie preview and D-pad TV navigation.
 */
public class AvatarCustomizerDialog {

    public interface OnAvatarSavedListener {
        void onAvatarSaved();
    }

    public static void show(Context context, OnAvatarSavedListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar);
        LayoutInflater inflater = LayoutInflater.from(context);
        View root = inflater.inflate(R.layout.dialog_avatar_customizer, null);

        LottieAnimationView previewLottie = (LottieAnimationView) root.findViewById(R.id.previewLottieAvatar);
        LinearLayout containerBg = root.findViewById(R.id.containerBgColors);
        LinearLayout containerChar = root.findViewById(R.id.containerCharColors);
        Button btnCancel = root.findViewById(R.id.btnCancelAvatar);
        Button btnSave = root.findViewById(R.id.btnSaveAvatar);

        int currentBg = AvatarThemeManager.getBgColor(context);
        int currentChar = AvatarThemeManager.getCharColor(context);

        final int[] selectedColors = new int[]{currentBg, currentChar};

        // Initialize preview
        AvatarThemeManager.applyAvatar(previewLottie, selectedColors[0], selectedColors[1], context);

        float density = context.getResources().getDisplayMetrics().density;
        int swatchSize = Math.round(44 * density);
        int swatchMargin = Math.round(8 * density);

        // Populate Background Swatches
        for (int i = 0; i < AvatarThemeManager.BG_PALETTE.length; i++) {
            final int color = AvatarThemeManager.BG_PALETTE[i];
            View swatch = createSwatchView(context, color, swatchSize, density);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(swatchSize, swatchSize);
            lp.setMarginEnd(swatchMargin);
            swatch.setLayoutParams(lp);

            swatch.setOnFocusChangeListener((v, hasFocus) -> {
                updateSwatchAppearance(v, color, hasFocus, color == selectedColors[0], density);
                if (hasFocus) {
                    selectedColors[0] = color;
                    AvatarThemeManager.applyAvatar(previewLottie, selectedColors[0], selectedColors[1], context);
                }
            });

            swatch.setOnClickListener(v -> {
                selectedColors[0] = color;
                AvatarThemeManager.applyAvatar(previewLottie, selectedColors[0], selectedColors[1], context);
                refreshSwatches(containerBg, selectedColors[0], density);
            });

            containerBg.addView(swatch);
        }

        // Populate Character Swatches
        for (int i = 0; i < AvatarThemeManager.CHAR_PALETTE.length; i++) {
            final int color = AvatarThemeManager.CHAR_PALETTE[i];
            View swatch = createSwatchView(context, color, swatchSize, density);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(swatchSize, swatchSize);
            lp.setMarginEnd(swatchMargin);
            swatch.setLayoutParams(lp);

            swatch.setOnFocusChangeListener((v, hasFocus) -> {
                updateSwatchAppearance(v, color, hasFocus, color == selectedColors[1], density);
                if (hasFocus) {
                    selectedColors[1] = color;
                    AvatarThemeManager.applyAvatar(previewLottie, selectedColors[0], selectedColors[1], context);
                }
            });

            swatch.setOnClickListener(v -> {
                selectedColors[1] = color;
                AvatarThemeManager.applyAvatar(previewLottie, selectedColors[0], selectedColors[1], context);
                refreshSwatches(containerChar, selectedColors[1], density);
            });

            containerChar.addView(swatch);
        }

        refreshSwatches(containerBg, selectedColors[0], density);
        refreshSwatches(containerChar, selectedColors[1], density);

        builder.setView(root);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            AvatarThemeManager.setBgColor(context, selectedColors[0]);
            AvatarThemeManager.setCharColor(context, selectedColors[1]);
            Toast.makeText(context, "✨ Warna Avatar berjaya disimpan!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            if (listener != null) {
                listener.onAvatarSaved();
            }
        });

        dialog.show();

        if (containerBg.getChildCount() > 0) {
            containerBg.getChildAt(0).requestFocus();
        }
    }

    private static View createSwatchView(Context context, int color, int size, float density) {
        FrameLayout frame = new FrameLayout(context);
        frame.setFocusable(true);
        frame.setClickable(true);
        updateSwatchAppearance(frame, color, false, false, density);
        return frame;
    }

    private static void updateSwatchAppearance(View v, int color, boolean focused, boolean selected, float density) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(color);

        if (focused) {
            gd.setStroke(Math.round(3.5f * density), 0xFF67E8F9); // Luminous cyan focus glow
            v.setScaleX(1.15f);
            v.setScaleY(1.15f);
            v.setElevation(8f * density);
        } else if (selected) {
            gd.setStroke(Math.round(2.5f * density), 0xFFFFFFFF); // White selection ring
            v.setScaleX(1.05f);
            v.setScaleY(1.05f);
            v.setElevation(4f * density);
        } else {
            gd.setStroke(Math.round(1.5f * density), 0x40FFFFFF); // Soft glass rim
            v.setScaleX(1.0f);
            v.setScaleY(1.0f);
            v.setElevation(0f);
        }
        v.setBackground(gd);
    }

    private static void refreshSwatches(LinearLayout container, int selectedColor, float density) {
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            int color = (container.getId() == R.id.containerBgColors)
                    ? AvatarThemeManager.BG_PALETTE[i] : AvatarThemeManager.CHAR_PALETTE[i];
            updateSwatchAppearance(child, color, child.isFocused(), color == selectedColor, density);
        }
    }
}
