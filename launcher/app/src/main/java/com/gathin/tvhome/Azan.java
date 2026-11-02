package com.gathin.tvhome;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;

/**
 * The azan at prayer time, while the TV is on. Each prayer can be switched on or off (Wallpaper menu →
 * Location, text &amp; ayat → Azan); Maghrib is on by default. One exact alarm is kept for the next prayer
 * that is switched on, worked out from the stored JAKIM timetable; Home sets it again each time it comes
 * to the front, and the alarm sets the one after. Nothing runs in between.
 *
 * The recording is the theme's azan.mp3 when there is one, else the call to prayer recorded at Masjid
 * KLCC (Internet Archive "AzanZhurMasjidKlcc", CC0), fetched once and kept.
 */
final class Azan {
    private static final String TAG = "TvHome.Azan";
    /** The prayers that can call the azan: indexes into PrayerTimes.NAMES (Syuruk is not a prayer). */
    static final int[] PRAYERS = {0, 2, 3, 4, 5};
    private static final int MAGHRIB = 4;

    static final String SOUND_MAKKAH = "makkah";
    static final String SOUND_MADINAH = "madinah";
    static final String SOUND_KLCC = "klcc";
    static final String SOUND_SINGAPORE = "singapore";
    static final String SOUND_INDONESIA = "indonesia";
    static final String SOUND_CHIME = "chime";
    static final String SOUND_SILENT = "silent";

    private static final String SOURCE_MAKKAH = "https://archive.org/download/full-madinah.mp3/full-makkah.mp3.mp3";
    private static final String SOURCE_MADINAH = "https://archive.org/download/full-madinah.mp3/full-madinah.mp3.mp3";
    private static final String SOURCE_KLCC = "https://archive.org/download/AzanZhurMasjidKlcc/CallToPrayer.mp3";
    private static final String SOURCE_SINGAPORE = "https://archive.org/download/AzanJiharkahUstazFahmi_201711/Azan%20Jiharkah%20Ustaz%20Fahmi%20.mp3";
    private static final String SOURCE_INDONESIA = "https://archive.org/download/ADZANMaghribDiTrans7/ADZAN%20Maghrib%20di%20Trans%207.mp3";

    /** An alarm delivered this late (the TV was asleep) is let go rather than played out of time. */
    static final long LATE_MS = 2 * 60 * 1000L;
    /** Where the azan itself begins in the KLCC recording (before it: birds and the park). */
    static final int KLCC_START_MS = 12_000;

    private Azan() {
    }

    static boolean isMasterEnabled(Context ctx) {
        return prefs(ctx).getBoolean("master_enabled", true);
    }

    static void setMasterEnabled(Context ctx, boolean enabled) {
        prefs(ctx).edit().putBoolean("master_enabled", enabled).apply();
    }

    static boolean on(Context ctx, int prayer) {
        return prefs(ctx).getBoolean("on_" + prayer, true);
    }

    static void set(Context ctx, int prayer, boolean on) {
        prefs(ctx).edit().putBoolean("on_" + prayer, on).apply();
    }

    static String getSoundChoice(Context ctx) {
        return prefs(ctx).getString("sound_choice", SOUND_MAKKAH);
    }

    static void setSoundChoice(Context ctx, String choice) {
        prefs(ctx).edit().putString("sound_choice", choice).apply();
    }

    static String soundLabel(String choice) {
        if (SOUND_MADINAH.equals(choice)) return "🕌 Azan Masjid Nabawi Madinah";
        if (SOUND_KLCC.equals(choice)) return "🇲🇾 Azan Masjid As-Syakirin KLCC";
        if (SOUND_SINGAPORE.equals(choice)) return "🇸🇬 Azan Singapura (Masjid Sultan)";
        if (SOUND_INDONESIA.equals(choice)) return "🇮🇩 Adzan Indonesia (Masjid Istiqlal / Nasional)";
        if (SOUND_CHIME.equals(choice)) return "🔔 Nada Chime Lembut (Ringkas)";
        if (SOUND_SILENT.equals(choice)) return "🔕 Mod Senyap (Visual Sahaja)";
        return "🕋 Azan Masjidil Haram Makkah";
    }

    /** Sets the alarm for the next prayer that is on, today or tomorrow; clears it when none is. */
    static void schedule(Context ctx, String zone) {
        AlarmManager am = ctx.getSystemService(AlarmManager.class);
        PendingIntent old = pending(ctx, -1, 0, PendingIntent.FLAG_NO_CREATE);
        if (old != null) am.cancel(old);
        if (!isMasterEnabled(ctx)) {
            note(ctx, "azan disabled (master off)");
            return;
        }
        if (zone == null || zone.isEmpty()) return;
        prefs(ctx).edit().putString("zone", zone).apply();
        long now = System.currentTimeMillis();
        // An "azan-test" file next to the theme sets a Maghrib azan one minute from now, to try it from the
        // computer while another app is in front: tools/tv.py 'touch …/files/azan-test', then press Home.
        File dir = Config.dir(ctx);
        if (dir != null && new File(dir, "azan-test").delete()) {
            long when = now + 60_000;
            try {
                am.setExact(AlarmManager.RTC, when, pending(ctx, MAGHRIB, when, PendingIntent.FLAG_UPDATE_CURRENT));
                Log.i(TAG, "test azan in one minute");
            } catch (SecurityException e) {
                Log.w(TAG, "exact alarms not allowed: " + e.getMessage());
            }
            return;
        }
        for (int d = 0; d < 2; d++) {
            Calendar day = Calendar.getInstance();
            day.add(Calendar.DAY_OF_MONTH, d);
            PrayerTimes.Day times = PrayerTimes.day(ctx, zone, day);
            if (times == null) continue;
            for (int p : PRAYERS) {
                if (!on(ctx, p)) continue;
                Calendar at = (Calendar) day.clone();
                at.set(Calendar.HOUR_OF_DAY, times.minutes[p] / 60);
                at.set(Calendar.MINUTE, times.minutes[p] % 60);
                at.set(Calendar.SECOND, 0);
                at.set(Calendar.MILLISECOND, 0);
                long when = at.getTimeInMillis();
                if (when <= now + 5000) continue;
                try {
                    // RTC, not RTC_WAKEUP: a TV in standby stays asleep; the receiver drops a late alarm.
                    am.setExact(AlarmManager.RTC, when, pending(ctx, p, when, PendingIntent.FLAG_UPDATE_CURRENT));
                    note(ctx, "next azan: " + PrayerTimes.NAMES[p] + " at " + String.format(java.util.Locale.ROOT, "%02d:%02d",
                            times.minutes[p] / 60, times.minutes[p] % 60) + (d == 1 ? " tomorrow" : ""));
                } catch (SecurityException e) {
                    note(ctx, "exact alarms not allowed: " + e.getMessage());
                }
                return;
            }
        }
    }

    private static PendingIntent pending(Context ctx, int prayer, long when, int flags) {
        Intent i = new Intent(ctx, AzanReceiver.class).putExtra("prayer", prayer).putExtra("at", when);
        return PendingIntent.getBroadcast(ctx, 0, i, flags | PendingIntent.FLAG_IMMUTABLE);
    }

    /** The recording to play for the chosen sound. */
    static File recording(Context ctx, String soundKey) {
        String targetName = "azan-makkah.mp3";
        if (SOUND_MADINAH.equals(soundKey)) targetName = "azan-madinah.mp3";
        else if (SOUND_KLCC.equals(soundKey)) targetName = "azan-klcc.mp3";
        else if (SOUND_SINGAPORE.equals(soundKey)) targetName = "azan-singapore.mp3";
        else if (SOUND_INDONESIA.equals(soundKey)) targetName = "azan-indonesia.mp3";

        File dir = Config.dir(ctx);
        if (dir != null) {
            File match = new File(dir, targetName);
            if (match.isFile() && match.length() > 50_000) return match;
            File generic = new File(dir, "azan.mp3");
            if (generic.isFile() && generic.length() > 50_000) return generic;
        }
        File kept = new File(ctx.getFilesDir(), targetName);
        if (kept.isFile() && kept.length() > 50_000) return kept;

        // Fallback to any existing azan file
        if (dir != null) {
            File[] files = dir.listFiles((d, n) -> n.startsWith("azan-") && n.endsWith(".mp3"));
            if (files != null) {
                for (File f : files) {
                    if (f.length() > 50_000) return f;
                }
            }
        }
        File klcc = new File(ctx.getFilesDir(), "azan-klcc.mp3");
        return (klcc.isFile() && klcc.length() > 50_000) ? klcc : null;
    }

    /** Legacy helper. */
    static File recording(Context ctx) {
        return recording(ctx, getSoundChoice(ctx));
    }

    /** Fetches the recording once in the background. */
    static void fetch(Context ctx, String soundKey) {
        if (SOUND_CHIME.equals(soundKey) || SOUND_SILENT.equals(soundKey)) return;
        final Context app = ctx.getApplicationContext();
        new Thread(() -> {
            String url = SOURCE_MAKKAH;
            String fileName = "azan-makkah.mp3";
            if (SOUND_MADINAH.equals(soundKey)) {
                url = SOURCE_MADINAH;
                fileName = "azan-madinah.mp3";
            } else if (SOUND_KLCC.equals(soundKey)) {
                url = SOURCE_KLCC;
                fileName = "azan-klcc.mp3";
            } else if (SOUND_SINGAPORE.equals(soundKey)) {
                url = SOURCE_SINGAPORE;
                fileName = "azan-singapore.mp3";
            } else if (SOUND_INDONESIA.equals(soundKey)) {
                url = SOURCE_INDONESIA;
                fileName = "azan-indonesia.mp3";
            }
            File dest = new File(app.getFilesDir(), fileName);
            if (dest.exists() && dest.length() > 100_000) return;
            File part = new File(app.getFilesDir(), fileName + ".part");
            HttpURLConnection c = null;
            try {
                c = (HttpURLConnection) new URL(url).openConnection();
                c.setConnectTimeout(15000);
                c.setReadTimeout(30000);
                c.setRequestProperty("User-Agent", "Mozilla/5.0 (GlassTV; Android)");
                try (InputStream in = c.getInputStream(); FileOutputStream out = new FileOutputStream(part)) {
                    byte[] buf = new byte[16 * 1024];
                    int r;
                    while ((r = in.read(buf)) > 0) out.write(buf, 0, r);
                }
                if (part.length() > 100_000 && part.renameTo(dest)) {
                    Log.i(TAG, "azan recording kept: " + fileName);
                }
            } catch (Exception e) {
                Log.w(TAG, "azan recording not fetched: " + e.getMessage());
            } finally {
                if (c != null) c.disconnect();
                part.delete();
            }
        }, "azan-fetch").start();
    }

    static void fetch(Context ctx) {
        fetch(ctx, getSoundChoice(ctx));
    }

    /** Displays FullScreenIntent notification so system guarantees overlay on all Android versions. */
    static void showFullScreenNotification(Context ctx, int prayer, Intent azanIntent) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) return;
        try {
            android.app.NotificationManager nm = ctx.getSystemService(android.app.NotificationManager.class);
            if (nm == null) return;
            String channelId = "azan_alarm_channel";
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                    channelId,
                    "Waktu Solat & Azan TV",
                    android.app.NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifikasi dan popup waktu solat / azan terapung");
            channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
            channel.setBypassDnd(true);
            nm.createNotificationChannel(channel);

            PendingIntent pi = PendingIntent.getActivity(
                    ctx,
                    101,
                    azanIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            String prayerName = (prayer >= 0 && prayer < PrayerTimes.NAMES.length) ? PrayerTimes.NAMES[prayer] : "Solat";
            android.app.Notification.Builder builder = new android.app.Notification.Builder(ctx, channelId)
                    .setSmallIcon(R.drawable.ic_azan)
                    .setContentTitle("🕌 Masuk Waktu " + prayerName)
                    .setContentText("Peringatan Waktu Solat")
                    .setCategory(android.app.Notification.CATEGORY_ALARM)
                    .setVisibility(android.app.Notification.VISIBILITY_PUBLIC)
                    .setFullScreenIntent(pi, true)
                    .setAutoCancel(true);
            nm.notify(9999, builder.build());
        } catch (Exception e) {
            Log.w(TAG, "Notification error: " + e.getMessage());
        }
    }

    /**
     * Notes what the azan did (set, played, skipped and why) in azan-log.txt, keeping the last 40
     * lines: logcat is gone after the TV sleeps, and "why was there no azan?" is asked the next day.
     */
    static synchronized void note(Context ctx, String line) {
        Log.i(TAG, line);
        // Next to the theme (Android/data/com.gathin.tvhome/files), where the computer can read it.
        File dir = Config.dir(ctx);
        File f = new File(dir != null ? dir : ctx.getFilesDir(), "azan-log.txt");
        java.util.List<String> lines = new java.util.ArrayList<>();
        try {
            if (f.isFile()) lines.addAll(java.nio.file.Files.readAllLines(f.toPath()));
        } catch (Exception ignored) {
        }
        lines.add(new java.text.SimpleDateFormat("MM-dd HH:mm:ss", java.util.Locale.ROOT).format(new java.util.Date()) + "  " + line);
        while (lines.size() > 40) lines.remove(0);
        try {
            java.nio.file.Files.write(f.toPath(), lines);
        } catch (Exception ignored) {
        }
    }

    static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences("azan", Context.MODE_PRIVATE);
    }
}
