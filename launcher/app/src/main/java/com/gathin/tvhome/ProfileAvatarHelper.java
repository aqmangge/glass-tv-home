package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.Outline;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.airbnb.lottie.LottieAnimationView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Universal helper that binds a UserProfile's avatar (Watak Tunggal Lottie, Animated GIF, or Custom Image)
 * inside any ViewGroup with smooth circular clipping and hardware acceleration.
 */
public final class ProfileAvatarHelper {

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private ProfileAvatarHelper() {}

    public static void bindAvatar(ViewGroup container, ProfileManager.UserProfile profile, Context context) {
        if (container == null || context == null) return;
        container.removeAllViews();

        container.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), view.getWidth() / 2f);
            }
        });
        container.setClipToOutline(true);

        if (profile == null || ProfileManager.AVATAR_TYPE_LOTTIE.equals(profile.avatarType)) {
            // 1. Watak Tunggal (Lottie Animation)
            LottieAnimationView lottie = new LottieAnimationView(context);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER
            );
            lottie.setLayoutParams(lp);
            container.addView(lottie);

            int bg = (profile != null && profile.lottieBgColor != 0) ? profile.lottieBgColor : AvatarThemeManager.DEFAULT_BG_COLOR;
            int chr = (profile != null && profile.lottieCharColor != 0) ? profile.lottieCharColor : AvatarThemeManager.DEFAULT_CHAR_COLOR;
            int face = (profile != null) ? profile.lottieFaceIndex : AvatarThemeManager.DEFAULT_FACE_INDEX;

            AvatarThemeManager.applyAvatar(lottie, bg, chr, face, context);

        } else {
            // 2 & 3. Animated GIF or Custom Image
            ImageView iv = new ImageView(context);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER
            );
            iv.setLayoutParams(lp);
            container.addView(iv);

            loadGifOrImage(iv, profile.avatarPath, context);
        }
    }

    public static void loadGifOrImage(ImageView iv, String path, Context context) {
        if (iv == null || context == null || path == null || path.isEmpty()) return;

        File localFile = new File(path);
        if (localFile.exists()) {
            displayLocalFile(iv, localFile);
            return;
        }

        // Check if cached in avatars dir
        File avatarsDir = new File(context.getFilesDir(), "avatars");
        if (!avatarsDir.exists()) avatarsDir.mkdirs();
        String safeName = "gif_" + Math.abs(path.hashCode()) + ".gif";
        File cached = new File(avatarsDir, safeName);
        if (cached.exists()) {
            displayLocalFile(iv, cached);
            return;
        }

        // If remote URL, fetch in background thread
        if (path.startsWith("http://") || path.startsWith("https://")) {
            new Thread(() -> {
                try {
                    URL u = new URL(path);
                    HttpURLConnection conn = (HttpURLConnection) u.openConnection();
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(12000);
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (GlassTV; Android)");
                    conn.connect();

                    if (conn.getResponseCode() == 200) {
                        try (InputStream in = conn.getInputStream();
                             FileOutputStream out = new FileOutputStream(cached)) {
                            byte[] buf = new byte[8192];
                            int r;
                            while ((r = in.read(buf)) != -1) {
                                out.write(buf, 0, r);
                            }
                        }
                        MAIN_HANDLER.post(() -> displayLocalFile(iv, cached));
                    }
                } catch (Exception ignored) {}
            }).start();
        }
    }

    private static void displayLocalFile(ImageView iv, File file) {
        if (iv == null || file == null || !file.exists()) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                ImageDecoder.Source src = ImageDecoder.createSource(file);
                Drawable d = ImageDecoder.decodeDrawable(src);
                iv.setImageDrawable(d);
                if (d instanceof AnimatedImageDrawable) {
                    ((AnimatedImageDrawable) d).setRepeatCount(AnimatedImageDrawable.REPEAT_INFINITE);
                    ((AnimatedImageDrawable) d).start();
                }
                return;
            } catch (Exception ignored) {}
        }

        // Fallback static bitmap
        try {
            Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
            if (bmp != null) iv.setImageBitmap(bmp);
        } catch (Exception ignored) {}
    }
}
