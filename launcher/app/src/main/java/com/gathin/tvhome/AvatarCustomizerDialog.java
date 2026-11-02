package com.gathin.tvhome;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;

/**
 * Interactive TV Dialog allowing users to customize Avatar Character expressions,
 * Character body colors, and Background colors with live animated Lottie preview
 * and D-pad TV navigation.
 */
public class AvatarCustomizerDialog {

    public interface OnAvatarSavedListener {
        void onAvatarSaved();
    }

    public static void show(Context context, OnAvatarSavedListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar);
        LayoutInflater inflater = LayoutInflater.from(context);
        View root = inflater.inflate(R.layout.dialog_avatar_customizer, null);

        FrameLayout previewContainer = root.findViewById(R.id.previewAvatarContainer);
        LottieAnimationView previewLottie = (LottieAnimationView) root.findViewById(R.id.previewLottieAvatar);
        ViewGroup containerExpressions = root.findViewById(R.id.containerExpressions);
        ViewGroup row1 = root.findViewById(R.id.rowExpressions1);
        ViewGroup row2 = root.findViewById(R.id.rowExpressions2);
        LinearLayout containerChar = root.findViewById(R.id.containerCharColors);
        LinearLayout containerBg = root.findViewById(R.id.containerBgColors);
        Button btnCancel = root.findViewById(R.id.btnCancelAvatar);
        Button btnSave = root.findViewById(R.id.btnSaveAvatar);

        // Circular clipping for preview container
        if (previewContainer != null) {
            previewContainer.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), view.getWidth() / 2f);
                }
            });
            previewContainer.setClipToOutline(true);
        }

        int currentBg = AvatarThemeManager.getBgColor(context);
        int currentChar = AvatarThemeManager.getCharColor(context);
        int currentFace = AvatarThemeManager.getFaceIndex(context);

        final int[] state = new int[]{currentBg, currentChar, currentFace};

        // Initialize preview
        AvatarThemeManager.applyAvatar(previewLottie, state[0], state[1], state[2], context);

        float density = context.getResources().getDisplayMetrics().density;
        int swatchSize = Math.round(42 * density);
        int swatchMargin = Math.round(8 * density);

        // 1. Populate Expression Pills across 2 rows so none are cropped
        for (int i = 0; i < AvatarThemeManager.FACE_NAMES.length; i++) {
            final int faceIdx = i;
            String faceName = AvatarThemeManager.FACE_NAMES[i];
            TextView pill = createExpressionPill(context, faceName, density);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0,
                    Math.round(34 * density),
                    1f
            );
            lp.setMarginEnd(Math.round(6 * density));
            pill.setLayoutParams(lp);

            pill.setOnFocusChangeListener((v, hasFocus) -> {
                updatePillAppearance(pill, hasFocus, faceIdx == state[2], density);
                if (hasFocus) {
                    state[2] = faceIdx;
                    AvatarThemeManager.applyAvatar(previewLottie, state[0], state[1], state[2], context);
                }
            });

            pill.setOnClickListener(v -> {
                state[2] = faceIdx;
                AvatarThemeManager.applyAvatar(previewLottie, state[0], state[1], state[2], context);
                refreshExpressionPills(containerExpressions, state[2], density);
            });

            if (row1 != null && row2 != null) {
                if (i < 5) {
                    row1.addView(pill);
                } else {
                    row2.addView(pill);
                }
            } else if (containerExpressions != null) {
                containerExpressions.addView(pill);
            }
        }

        // 2. Populate Character Body Swatches
        for (int i = 0; i < AvatarThemeManager.CHAR_PALETTE.length; i++) {
            final int color = AvatarThemeManager.CHAR_PALETTE[i];
            View swatch = createSwatchView(context, color, swatchSize, density);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(swatchSize, swatchSize);
            lp.setMarginEnd(swatchMargin);
            swatch.setLayoutParams(lp);

            swatch.setOnFocusChangeListener((v, hasFocus) -> {
                updateSwatchAppearance(v, color, hasFocus, color == state[1], density);
                if (hasFocus) {
                    state[1] = color;
                    AvatarThemeManager.applyAvatar(previewLottie, state[0], state[1], state[2], context);
                }
            });

            swatch.setOnClickListener(v -> {
                state[1] = color;
                AvatarThemeManager.applyAvatar(previewLottie, state[0], state[1], state[2], context);
                refreshSwatches(containerChar, state[1], density);
            });

            containerChar.addView(swatch);
        }

        // 3. Populate Background Swatches
        for (int i = 0; i < AvatarThemeManager.BG_PALETTE.length; i++) {
            final int color = AvatarThemeManager.BG_PALETTE[i];
            View swatch = createSwatchView(context, color, swatchSize, density);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(swatchSize, swatchSize);
            lp.setMarginEnd(swatchMargin);
            swatch.setLayoutParams(lp);

            swatch.setOnFocusChangeListener((v, hasFocus) -> {
                updateSwatchAppearance(v, color, hasFocus, color == state[0], density);
                if (hasFocus) {
                    state[0] = color;
                    AvatarThemeManager.applyAvatar(previewLottie, state[0], state[1], state[2], context);
                }
            });

            swatch.setOnClickListener(v -> {
                state[0] = color;
                AvatarThemeManager.applyAvatar(previewLottie, state[0], state[1], state[2], context);
                refreshSwatches(containerBg, state[0], density);
            });

            containerBg.addView(swatch);
        }

        refreshExpressionPills(containerExpressions, state[2], density);
        refreshSwatches(containerChar, state[1], density);
        refreshSwatches(containerBg, state[0], density);

        builder.setView(root);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            AvatarThemeManager.setBgColor(context, state[0]);
            AvatarThemeManager.setCharColor(context, state[1]);
            AvatarThemeManager.setFaceIndex(context, state[2]);
            Toast.makeText(context, "✨ Avatar Watak berjaya disimpan!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            if (listener != null) {
                listener.onAvatarSaved();
            }
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(Math.round(660 * density), ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // Default focus on expression pills row
        if (containerExpressions.getChildCount() > 0) {
            int focusIdx = Math.max(0, Math.min(state[2], containerExpressions.getChildCount() - 1));
            containerExpressions.getChildAt(focusIdx).requestFocus();
        }
    }

    private static TextView createExpressionPill(Context context, String name, float density) {
        TextView tv = new TextView(context);
        tv.setText(name);
        tv.setTextSize(12.5f);
        tv.setTextColor(0xFFE2E8F0);
        tv.setGravity(Gravity.CENTER);
        tv.setFocusable(true);
        tv.setClickable(true);
        int padH = Math.round(12 * density);
        tv.setPadding(padH, 0, padH, 0);
        updatePillAppearance(tv, false, false, density);
        return tv;
    }

    private static void updatePillAppearance(TextView tv, boolean focused, boolean selected, float density) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setCornerRadius(17f * density);

        if (focused) {
            gd.setColor(0x3567E8F9); // Luminous cyan glass
            gd.setStroke(Math.round(2f * density), 0xFF67E8F9);
            tv.setTextColor(0xFFFFFFFF);
        } else if (selected) {
            gd.setColor(0x28FFFFFF); // Frosted white glass
            gd.setStroke(Math.round(1.8f * density), 0xFFFFFFFF);
            tv.setTextColor(0xFF38BDF8);
        } else {
            gd.setColor(0x14FFFFFF); // Subtle soap-bubble glass
            gd.setStroke(Math.round(1f * density), 0x22FFFFFF);
            tv.setTextColor(0xFF94A3B8);
        }
        tv.setBackground(gd);
    }

    private static void refreshExpressionPills(ViewGroup container, int selectedIdx, float density) {
        if (container == null) return;
        int idx = 0;
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child instanceof TextView) {
                updatePillAppearance((TextView) child, child.isFocused(), idx == selectedIdx, density);
                idx++;
            } else if (child instanceof ViewGroup) {
                ViewGroup subRow = (ViewGroup) child;
                for (int j = 0; j < subRow.getChildCount(); j++) {
                    View subChild = subRow.getChildAt(j);
                    if (subChild instanceof TextView) {
                        updatePillAppearance((TextView) subChild, subChild.isFocused(), idx == selectedIdx, density);
                        idx++;
                    }
                }
            }
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
