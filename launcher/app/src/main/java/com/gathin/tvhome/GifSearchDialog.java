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
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * TV Dialog for searching and selecting animated GIFs as profile avatars.
 * Queries Giphy GIF API with trending category chips, live preview, and D-pad navigation.
 */
public class GifSearchDialog {

    private static final String GIPHY_KEY = "sXpGFDGZs0Dv1mmNFvYaGUvYwKX0PWIh";
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public interface OnGifSelectedListener {
        void onGifSelected(String localGifPath);
    }

    public static void show(Context context, String profileId, OnGifSelectedListener listener) {
        float density = context.getResources().getDisplayMetrics().density;
        AlertDialog.Builder builder = new AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar);

        ScrollView scroll = new ScrollView(context);
        scroll.setBackgroundResource(R.drawable.bg_glass_dialog);
        scroll.setPadding((int) (26 * density), (int) (24 * density), (int) (26 * density), (int) (24 * density));

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(root);

        // Header
        TextView tvTitle = new TextView(context);
        tvTitle.setText("🔍 Cari Animasi GIF Avatar");
        tvTitle.setTextSize(20);
        tvTitle.setTextColor(0xFFFFFFFF);
        tvTitle.setTypeface(context.getResources().getFont(R.font.inter), android.graphics.Typeface.BOLD);
        root.addView(tvTitle);

        TextView tvSub = new TextView(context);
        tvSub.setText("Pilih atau cari sebarang animasi GIF untuk dijadikan avatar profil anda");
        tvSub.setTextSize(13);
        tvSub.setTextColor(0xFF94A3B8);
        root.addView(tvSub);

        // Search Input Row
        LinearLayout searchRow = new LinearLayout(context);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);
        searchRow.setPadding(0, (int) (14 * density), 0, (int) (8 * density));

        EditText etSearch = new EditText(context);
        etSearch.setHint("Taip carian GIF (cth: anime, cat, gaming)...");
        etSearch.setHintTextColor(0xFF64748B);
        etSearch.setTextColor(0xFFFFFFFF);
        etSearch.setTextSize(14);
        etSearch.setSingleLine(true);
        etSearch.setPadding((int) (14 * density), (int) (10 * density), (int) (14 * density), (int) (10 * density));

        GradientDrawable etBg = new GradientDrawable();
        etBg.setColor(0x22FFFFFF);
        etBg.setCornerRadius(12 * density);
        etBg.setStroke((int) (1.2f * density), 0x33FFFFFF);
        etSearch.setBackground(etBg);

        LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        etLp.setMarginEnd((int) (10 * density));
        searchRow.addView(etSearch, etLp);

        TextView btnSearch = new TextView(context);
        btnSearch.setText("Cari 🚀");
        btnSearch.setTextColor(0xFFFFFFFF);
        btnSearch.setTextSize(13.5f);
        btnSearch.setTypeface(context.getResources().getFont(R.font.inter), android.graphics.Typeface.BOLD);
        btnSearch.setGravity(Gravity.CENTER);
        btnSearch.setIncludeFontPadding(false);
        btnSearch.setBackgroundResource(R.drawable.bg_dialog_btn);
        btnSearch.setPadding((int) (20 * density), (int) (9 * density), (int) (20 * density), (int) (9 * density));
        btnSearch.setFocusable(true);
        btnSearch.setClickable(true);
        searchRow.addView(btnSearch, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(searchRow);

        // Category Tag Chips (Button Cadangan) - perfectly uncropped with clean vertical centering
        HorizontalScrollView chipScroll = new HorizontalScrollView(context);
        chipScroll.setClipChildren(false);
        chipScroll.setClipToPadding(false);
        chipScroll.setHorizontalScrollBarEnabled(false);

        LinearLayout chipContainer = new LinearLayout(context);
        chipContainer.setOrientation(LinearLayout.HORIZONTAL);
        chipContainer.setClipChildren(false);
        chipContainer.setClipToPadding(false);
        chipContainer.setPadding(0, (int) (6 * density), 0, (int) (14 * density));

        int chipHeight = Math.round(36 * density);
        int chipRadius = Math.round(18 * density);

        final Runnable[] doSearchRef = new Runnable[1];

        // Suggestion tags
        String[] tags = new String[]{"Anime", "Gaming", "Kucing", "Kartun", "Cool", "Retro", "Superhero", "Cute"};
        for (String tag : tags) {
            TextView chip = new TextView(context);
            chip.setText(tag);
            chip.setTextSize(12.5f);
            chip.setTextColor(0xFFE2E8F0);
            chip.setTypeface(context.getResources().getFont(R.font.inter), android.graphics.Typeface.BOLD);
            chip.setGravity(Gravity.CENTER);
            chip.setIncludeFontPadding(false);
            chip.setFocusable(true);
            chip.setClickable(true);
            chip.setPadding((int) (16 * density), 0, (int) (16 * density), 0);

            GradientDrawable chipBg = new GradientDrawable();
            chipBg.setColor(0x18FFFFFF);
            chipBg.setCornerRadius(chipRadius);
            chipBg.setStroke((int) (1.2f * density), 0x33FFFFFF);
            chip.setBackground(chipBg);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    chipHeight
            );
            lp.setMarginEnd((int) (8 * density));
            chip.setLayoutParams(lp);

            chip.setOnFocusChangeListener((v, hasFocus) -> {
                chipBg.setColor(hasFocus ? 0x4067E8F9 : 0x18FFFFFF);
                chipBg.setStroke((int) (2f * density), hasFocus ? 0xFF67E8F9 : 0x33FFFFFF);
                chip.setTextColor(hasFocus ? 0xFFFFFFFF : 0xFFE2E8F0);
            });

            chip.setOnClickListener(v -> {
                etSearch.setText(tag);
                if (doSearchRef[0] != null) doSearchRef[0].run();
            });
            chipContainer.addView(chip);
        }
        chipScroll.addView(chipContainer);
        root.addView(chipScroll);

        // Results Container
        ProgressBar loader = new ProgressBar(context);
        loader.setVisibility(View.GONE);
        LinearLayout.LayoutParams loadLp = new LinearLayout.LayoutParams((int) (40 * density), (int) (40 * density));
        loadLp.gravity = Gravity.CENTER_HORIZONTAL;
        loadLp.topMargin = (int) (16 * density);
        loadLp.bottomMargin = (int) (16 * density);
        root.addView(loader, loadLp);

        HorizontalScrollView resultsScroll = new HorizontalScrollView(context);
        resultsScroll.setClipChildren(false);
        resultsScroll.setClipToPadding(false);
        resultsScroll.setHorizontalScrollBarEnabled(false);

        LinearLayout resultsRow = new LinearLayout(context);
        resultsRow.setOrientation(LinearLayout.HORIZONTAL);
        resultsRow.setPadding(0, (int) (8 * density), 0, (int) (16 * density));
        resultsScroll.addView(resultsRow);
        root.addView(resultsScroll);

        // Close Button
        TextView btnClose = new TextView(context);
        btnClose.setText("Tutup");
        btnClose.setTextColor(0xFF94A3B8);
        btnClose.setTextSize(13.5f);
        btnClose.setTypeface(context.getResources().getFont(R.font.inter), android.graphics.Typeface.BOLD);
        btnClose.setGravity(Gravity.CENTER);
        btnClose.setFocusable(true);
        btnClose.setClickable(true);
        btnClose.setPadding((int) (18 * density), (int) (8 * density), (int) (18 * density), (int) (8 * density));
        btnClose.setOnFocusChangeListener((v, hasFocus) -> {
            btnClose.setTextColor(hasFocus ? 0xFFFFFFFF : 0xFF94A3B8);
        });

        LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        closeLp.gravity = Gravity.END;
        closeLp.topMargin = (int) (8 * density);
        root.addView(btnClose, closeLp);

        builder.setView(scroll);
        AlertDialog dialog = builder.create();
        btnClose.setOnClickListener(v -> dialog.dismiss());

        // Search Executor
        Runnable doSearch = () -> {
            String q = etSearch.getText().toString().trim();
            if (q.isEmpty()) q = "cute avatar";
            loader.setVisibility(View.VISIBLE);
            resultsRow.removeAllViews();

            final String finalQuery = q;
            new Thread(() -> {
                List<String> gifUrls = queryGiphy(finalQuery);
                MAIN_HANDLER.post(() -> {
                    loader.setVisibility(View.GONE);
                    if (gifUrls.isEmpty()) {
                        Toast.makeText(context, "Tiada GIF ditemui. Cuba carian lain.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    populateGifResults(context, resultsRow, gifUrls, profileId, path -> {
                        dialog.dismiss();
                        if (listener != null) listener.onGifSelected(path);
                    }, density);
                });
            }).start();
        };
        doSearchRef[0] = doSearch;

        btnSearch.setOnClickListener(v -> doSearch.run());
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            doSearch.run();
            return true;
        });

        // Search initial query on open
        etSearch.setText("anime avatar");
        doSearch.run();

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(Math.round(720 * density), ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        }
    }

    private static List<String> queryGiphy(String query) {
        List<String> results = new ArrayList<>();
        try {
            String encoded = URLEncoder.encode(query, "UTF-8");
            String urlStr = "https://api.giphy.com/v1/gifs/search?api_key=" + GIPHY_KEY + "&q=" + encoded + "&limit=14";
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (GlassTV; Android)");
            conn.connect();

            if (conn.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);

                JSONObject root = new JSONObject(sb.toString());
                JSONArray data = root.optJSONArray("data");
                if (data != null) {
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject item = data.getJSONObject(i);
                        JSONObject images = item.optJSONObject("images");
                        if (images != null) {
                            JSONObject fixed = images.optJSONObject("fixed_height");
                            if (fixed == null) fixed = images.optJSONObject("fixed_width_small");
                            if (fixed != null) {
                                String u = fixed.optString("url");
                                if (u != null && !u.isEmpty()) results.add(u);
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return results;
    }

    private static void populateGifResults(
            Context context,
            LinearLayout container,
            List<String> gifUrls,
            String profileId,
            OnGifSelectedListener listener,
            float density
    ) {
        container.removeAllViews();
        int cardSize = Math.round(110 * density);
        int margin = Math.round(10 * density);

        for (int i = 0; i < gifUrls.size(); i++) {
            final String gifUrl = gifUrls.get(i);

            FrameLayout frame = new FrameLayout(context);
            frame.setFocusable(true);
            frame.setClickable(true);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(cardSize, cardSize);
            lp.setMarginEnd(margin);
            frame.setLayoutParams(lp);

            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.OVAL);
            gd.setColor(0x22FFFFFF);
            gd.setStroke((int) (1.5f * density), 0x33FFFFFF);
            frame.setBackground(gd);

            ImageView iv = new ImageView(context);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            FrameLayout.LayoutParams ivLp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            iv.setLayoutParams(ivLp);
            frame.addView(iv);

            // Load GIF preview
            ProfileAvatarHelper.loadGifOrImage(iv, gifUrl, context);

            frame.setOnFocusChangeListener((v, hasFocus) -> {
                gd.setColor(hasFocus ? 0x3067E8F9 : 0x22FFFFFF);
                gd.setStroke((int) (3f * density), hasFocus ? 0xFF67E8F9 : 0x33FFFFFF);
                v.setScaleX(hasFocus ? 1.14f : 1.0f);
                v.setScaleY(hasFocus ? 1.14f : 1.0f);
                v.setElevation(hasFocus ? 8f * density : 0f);
            });

            frame.setOnClickListener(v -> {
                Toast.makeText(context, "⏳ Sedang memuat turun GIF avatar...", Toast.LENGTH_SHORT).show();
                new Thread(() -> {
                    String localPath = downloadAndSaveGif(context, gifUrl, profileId);
                    MAIN_HANDLER.post(() -> {
                        if (localPath != null) {
                            Toast.makeText(context, "✨ GIF berjaya dipilih sebagai avatar!", Toast.LENGTH_SHORT).show();
                            if (listener != null) listener.onGifSelected(localPath);
                        } else {
                            Toast.makeText(context, "Ralat memuat turun GIF. Sila cuba lagi.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }).start();
            });

            container.addView(frame);
        }
    }

    private static String downloadAndSaveGif(Context context, String gifUrl, String profileId) {
        try {
            File avatarsDir = new File(context.getFilesDir(), "avatars");
            if (!avatarsDir.exists()) avatarsDir.mkdirs();
            File target = new File(avatarsDir, "avatar_" + profileId + ".gif");

            URL u = new URL(gifUrl);
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(12000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (GlassTV; Android)");
            conn.connect();

            if (conn.getResponseCode() == 200) {
                try (InputStream in = conn.getInputStream();
                     FileOutputStream out = new FileOutputStream(target)) {
                    byte[] buf = new byte[8192];
                    int r;
                    while ((r = in.read(buf)) != -1) {
                        out.write(buf, 0, r);
                    }
                }
                return target.getAbsolutePath();
            }
        } catch (Exception ignored) {}
        return null;
    }
}
