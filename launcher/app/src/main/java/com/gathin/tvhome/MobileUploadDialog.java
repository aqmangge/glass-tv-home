package com.gathin.tvhome;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

/**
 * TV Dialog guiding the user to upload a custom profile image or animated GIF
 * from their smartphone or local TV storage over Wi-Fi.
 */
public class MobileUploadDialog {

    private static final int PORT = 8765;

    public interface OnAvatarUploadedListener {
        void onAvatarUploaded(String localFilePath);
    }

    public static void show(Context context, String profileId, OnAvatarUploadedListener listener) {
        float density = context.getResources().getDisplayMetrics().density;
        AlertDialog.Builder builder = new AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundResource(R.drawable.bg_glass_dialog);
        root.setPadding((int) (28 * density), (int) (26 * density), (int) (28 * density), (int) (26 * density));

        // Header
        TextView tvTitle = new TextView(context);
        tvTitle.setText("📲 Muat Naik Avatar Dari Telefon");
        tvTitle.setTextSize(20);
        tvTitle.setTextColor(0xFFFFFFFF);
        tvTitle.setTypeface(context.getResources().getFont(R.font.inter), android.graphics.Typeface.BOLD);
        root.addView(tvTitle);

        TextView tvSub = new TextView(context);
        tvSub.setText("Sambungkan telefon pintar ke Wi-Fi yang sama untuk menghantar gambar atau animasi GIF");
        tvSub.setTextSize(13);
        tvSub.setTextColor(0xFF94A3B8);
        tvSub.setPadding(0, (int) (4 * density), 0, (int) (16 * density));
        root.addView(tvSub);

        // IP Address URL Display Box
        String ip = LocalUploadServer.getLocalIpAddress();
        String url = "http://" + ip + ":" + PORT;

        LinearLayout urlBox = new LinearLayout(context);
        urlBox.setOrientation(LinearLayout.VERTICAL);
        urlBox.setGravity(Gravity.CENTER);
        urlBox.setPadding((int) (20 * density), (int) (18 * density), (int) (20 * density), (int) (18 * density));

        GradientDrawable urlBg = new GradientDrawable();
        urlBg.setColor(0x280284C7); // Cyan glass glow
        urlBg.setCornerRadius(18 * density);
        urlBg.setStroke((int) (1.8f * density), 0xFF38BDF8);
        urlBox.setBackground(urlBg);

        TextView tvUrlLabel = new TextView(context);
        tvUrlLabel.setText("🌐 BUKA PAUTAN INI DI TELEFON ANDA:");
        tvUrlLabel.setTextSize(12);
        tvUrlLabel.setTextColor(0xFF67E8F9);
        tvUrlLabel.setTypeface(context.getResources().getFont(R.font.inter), android.graphics.Typeface.BOLD);
        urlBox.addView(tvUrlLabel);

        TextView tvUrl = new TextView(context);
        tvUrl.setText(url);
        tvUrl.setTextSize(24);
        tvUrl.setTextColor(0xFFFFFFFF);
        tvUrl.setTypeface(context.getResources().getFont(R.font.inter), android.graphics.Typeface.BOLD);
        tvUrl.setPadding(0, (int) (6 * density), 0, (int) (6 * density));
        urlBox.addView(tvUrl);

        TextView tvHelp = new TextView(context);
        tvHelp.setText("Ketik pautan di atas pada pelayar web (Safari / Chrome) di telefon anda.");
        tvHelp.setTextSize(12.5f);
        tvHelp.setTextColor(0xFFE2E8F0);
        urlBox.addView(tvHelp);

        root.addView(urlBox);

        // Server Status Pulse
        LinearLayout statusRow = new LinearLayout(context);
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        statusRow.setPadding(0, (int) (16 * density), 0, (int) (14 * density));

        TextView tvDot = new TextView(context);
        tvDot.setText("● ");
        tvDot.setTextSize(14);
        tvDot.setTextColor(0xFF10B981); // Emerald green
        statusRow.addView(tvDot);

        TextView tvStatus = new TextView(context);
        tvStatus.setText("Pelayan aktif. Menunggu anda memilih fail dari telefon...");
        tvStatus.setTextSize(13);
        tvStatus.setTextColor(0xFF10B981);
        statusRow.addView(tvStatus);
        root.addView(statusRow);

        // Action Buttons Row
        LinearLayout actionsRow = new LinearLayout(context);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        actionsRow.setGravity(Gravity.END);
        actionsRow.setPadding(0, (int) (8 * density), 0, 0);

        Button btnClose = new Button(context, null, android.R.attr.borderlessButtonStyle);
        btnClose.setText("Batal");
        btnClose.setTextColor(0xFF94A3B8);
        btnClose.setFocusable(true);
        actionsRow.addView(btnClose);

        root.addView(actionsRow);

        builder.setView(root);
        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(d -> LocalUploadServer.stop());
        btnClose.setOnClickListener(v -> dialog.dismiss());

        // Start Local Upload Server
        File saveDir = new File(context.getFilesDir(), "avatars");
        LocalUploadServer.start(PORT, saveDir, uploadedFile -> {
            Toast.makeText(context, "✨ Avatar berjaya diterima dari telefon!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            LocalUploadServer.stop();
            if (listener != null) {
                listener.onAvatarUploaded(uploadedFile.getAbsolutePath());
            }
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(Math.round(640 * density), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
