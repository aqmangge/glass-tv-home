package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages weekly Friday sermon (Khutbah Minbar) content based on user location
 * (Malaysia, Singapore, and Indonesia) from official sources:
 * - Malaysia & Singapore: minbar.gosolat.com (JAWI, JAIS, MUIS, etc.)
 * - Indonesia: Kemenag RI / NU Online Friday Khutbah
 */
public final class MinbarManager {
    private static final String TAG = "TvHome.Minbar";
    private static final String PREFS_NAME = "minbar_cache";
    private static final String KEY_DISPLAY_MODE = "display_mode"; // 0 = DOA, 1 = MINBAR
    private static final String KEY_LAST_SYNC = "last_sync_time";

    public static final int MODE_DOA = 0;
    public static final int MODE_MINBAR = 1;

    public static class KhutbahItem {
        public final String title;
        public final String subtitle;
        public final String excerpt;
        public final String authority;
        public final String date;
        public final String region;

        public KhutbahItem(String title, String subtitle, String excerpt, String authority, String date, String region) {
            this.title = title;
            this.subtitle = subtitle;
            this.excerpt = excerpt;
            this.authority = authority;
            this.date = date;
            this.region = region;
        }
    }

    private MinbarManager() {}

    public static int getDisplayMode(Context ctx) {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(KEY_DISPLAY_MODE, MODE_DOA);
    }

    public static void setDisplayMode(Context ctx, int mode) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putInt(KEY_DISPLAY_MODE, mode).commit();
    }

    public static String getRegionKey(Context ctx) {
        String zone = PrayerTimes.getActiveZone(ctx);
        if (zone == null) zone = "WLY01";
        String loc = AppLocationManager.getLocationName(ctx).toLowerCase(Locale.ROOT);
        String stationId = AppLocationManager.getStationId(ctx).toUpperCase(Locale.ROOT);

        if (zone.startsWith("SGP") || loc.contains("singapore") || loc.contains("singapura") || stationId.startsWith("SG")) {
            return "MUIS";
        }
        if (zone.startsWith("ID_") || loc.contains("indonesia") || stationId.startsWith("ID")) {
            return "ID";
        }
        if (zone.startsWith("SGR")) return "JAIS";
        if (zone.startsWith("JHR")) return "JAINJ";
        if (zone.startsWith("PNG")) return "JAIPP";
        if (zone.startsWith("PRK")) return "Perak";
        if (zone.startsWith("PLS")) return "Perlis";
        if (zone.startsWith("KDH")) return "Kedah";
        if (zone.startsWith("NGS")) return "N. Sembilan";
        if (zone.startsWith("TRG")) return "Terengganu";
        if (zone.startsWith("KTN")) return "MAIK";
        if (zone.startsWith("SBH")) return "Sabah";
        if (zone.startsWith("SWK")) return "Sarawak";

        return "JAWI"; // Default W.P. Kuala Lumpur / Putrajaya
    }

    public static String getAuthorityName(String regionKey) {
        switch (regionKey) {
            case "MUIS": return "MUIS Singapura";
            case "ID": return "Kemenag RI (Indonesia)";
            case "JAIS": return "JAIS Selangor";
            case "JAINJ": return "JAINJ Johor";
            case "JAIPP": return "JAIPP Pulau Pinang";
            case "Perak": return "JAIPk Perak";
            case "Perlis": return "JAIPs Perlis";
            case "Kedah": return "JHEIK Kedah";
            case "N. Sembilan": return "JHEAINS N. Sembilan";
            case "Terengganu": return "JHEAT Terengganu";
            case "MAIK": return "MAIK Kelantan";
            case "Sabah": return "JHEAINS Sabah";
            case "Sarawak": return "JAIS Sarawak";
            case "JAWI":
            default:
                return "JAWI W.P. Putrajaya & KL";
        }
    }

    public static KhutbahItem getKhutbah(Context ctx) {
        String regionKey = getRegionKey(ctx);
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        String titleKey = "khutbah_" + regionKey + "_title";
        if (prefs.contains(titleKey)) {
            String title = prefs.getString(titleKey, "");
            if (!title.isEmpty()) {
                return new KhutbahItem(
                        title,
                        prefs.getString("khutbah_" + regionKey + "_sub", ""),
                        prefs.getString("khutbah_" + regionKey + "_exc", ""),
                        prefs.getString("khutbah_" + regionKey + "_auth", getAuthorityName(regionKey)),
                        prefs.getString("khutbah_" + regionKey + "_date", ""),
                        regionKey
                );
            }
        }

        return getOfflineFallback(regionKey);
    }

    public static void refreshIfDue(Context ctx, Runnable onUpdated) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastSync = prefs.getLong(KEY_LAST_SYNC, 0);
        long now = System.currentTimeMillis();
        // Refresh every 3 days (khutbah is weekly)
        if (now - lastSync > 3 * 86_400_000L) {
            refresh(ctx, onUpdated);
        }
    }

    public static void refresh(Context ctx, Runnable onUpdated) {
        new Thread(() -> {
            try {
                Context app = ctx.getApplicationContext();
                String regKey = getRegionKey(app);

                if ("ID".equals(regKey)) {
                    fetchIndonesiaKhutbah(app);
                } else {
                    fetchMinbarGoSolat(app);
                }

                app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .edit()
                        .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
                        .apply();

                if (onUpdated != null) {
                    new Handler(Looper.getMainLooper()).post(onUpdated);
                }
            } catch (Exception e) {
                Log.w(TAG, "Error refreshing khutbah: " + e.getMessage());
            }
        }, "minbar-fetch").start();
    }

    private static void fetchMinbarGoSolat(Context ctx) {
        try {
            URL url = new URL("https://minbar.gosolat.com/ms/khutbah/");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(12000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (GlassTV; Android TV)");

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                String html = sb.toString();
                Pattern cardPat = Pattern.compile("<article[^>]*>(.*?)</article>", Pattern.DOTALL);
                Matcher cardMat = cardPat.matcher(html);

                SharedPreferences.Editor editor = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                Map<String, Boolean> savedRegions = new HashMap<>();

                Pattern titlePat = Pattern.compile("<h2[^>]*>(.*?)</h2>", Pattern.DOTALL);
                Pattern subPat = Pattern.compile("<p class=[\"']mt-1 text-sm italic[^>]*>(.*?)</p>", Pattern.DOTALL);
                Pattern excPat = Pattern.compile("<p class=[\"']mt-3 line-clamp-2[^>]*>(.*?)</p>", Pattern.DOTALL);
                Pattern datePat = Pattern.compile("<span>(\\d+\\s+[A-Za-z]+\\s+\\d{4})</span>");
                Pattern regPat = Pattern.compile("rounded-full[^>]*>([^<]+).*?([A-Z]{2})</span>", Pattern.DOTALL);

                while (cardMat.find()) {
                    String card = cardMat.group(1);
                    Matcher rMat = regPat.matcher(card);
                    String reg = rMat.find() ? rMat.group(1).trim() : "";
                    if (reg.isEmpty() || savedRegions.containsKey(reg)) continue;

                    Matcher tMat = titlePat.matcher(card);
                    String title = tMat.find() ? cleanHtml(tMat.group(1)) : "";
                    if (title.isEmpty()) continue;

                    Matcher sMat = subPat.matcher(card);
                    String sub = sMat.find() ? cleanHtml(sMat.group(1)) : "";

                    Matcher eMat = excPat.matcher(card);
                    String exc = eMat.find() ? cleanHtml(eMat.group(1)) : "";

                    Matcher dMat = datePat.matcher(card);
                    String date = dMat.find() ? dMat.group(1).trim() : "";

                    editor.putString("khutbah_" + reg + "_title", title);
                    editor.putString("khutbah_" + reg + "_sub", sub);
                    editor.putString("khutbah_" + reg + "_exc", exc);
                    editor.putString("khutbah_" + reg + "_auth", getAuthorityName(reg));
                    editor.putString("khutbah_" + reg + "_date", date);

                    savedRegions.put(reg, true);
                }
                editor.apply();
                Log.i(TAG, "Synced minbar.gosolat.com for " + savedRegions.size() + " regions");
            }
        } catch (Exception e) {
            Log.w(TAG, "Minbar sync failed: " + e.getMessage());
        }
    }

    private static void fetchIndonesiaKhutbah(Context ctx) {
        try {
            URL url = new URL("https://islam.nu.or.id/khutbah");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(12000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) GlassTV/1.0");

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                String html = sb.toString();
                Pattern titlePat = Pattern.compile("<h2[^>]*class=[\"'][^\"']*font-bold[^\"']*[\"'][^>]*>(.*?)</h2>", Pattern.DOTALL);
                Matcher m = titlePat.matcher(html);
                if (m.find()) {
                    String fullTitle = cleanHtml(m.group(1));
                    String cleanTitle = fullTitle.replaceFirst("(?i)^Khutbah\\s+Jumat:\\s*", "").trim();

                    SharedPreferences.Editor editor = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                    editor.putString("khutbah_ID_title", cleanTitle);
                    editor.putString("khutbah_ID_sub", "Naskah Khutbah Jumat Kemenag RI & NU Online");
                    editor.putString("khutbah_ID_exc", "Khutbah mengajak umat memperkuat ketakwaan, kepedulian sesama, dan menjaga kerukunan umat dalam bingkai akhlakul karimah.");
                    editor.putString("khutbah_ID_auth", "Kemenag RI (Indonesia)");
                    editor.putString("khutbah_ID_date", "Jumat Pekan Ini");
                    editor.apply();
                    Log.i(TAG, "Synced Indonesian khutbah: " + cleanTitle);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Indonesia khutbah sync failed: " + e.getMessage());
        }
    }

    private static String cleanHtml(String text) {
        if (text == null) return "";
        return text.replaceAll("<[^>]+>", " ")
                .replace("&quot;", "\"")
                .replace("&#x27;", "'")
                .replace("&#39;", "'")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static KhutbahItem getOfflineFallback(String regionKey) {
        switch (regionKey) {
            case "MUIS":
                return new KhutbahItem(
                        "Al-Quran Menyinari Hati, Memperindah Peribadi",
                        "Al-Quran sebagai nur, huda, syifa dan pedoman hidup",
                        "Khutbah menegaskan bahawa Al-Quran bukan sekadar bacaan lisan, tetapi panduan membentuk akhlak mulia dan ketenangan jiwa dalam kehidupan seharian.",
                        "MUIS Singapura",
                        "30 Okt 2026",
                        "MUIS"
                );
            case "ID":
                return new KhutbahItem(
                        "Membangun Kesalehan Sosial dan Kepedulian Sesama",
                        "Memperkokoh ukhuwah dan membantu sesama di saat kesulitan",
                        "Khutbah Jumat Kemenag RI mengajak umat mengamalkan ajaran Islam secara nyata dengan tolong-menolong, memperkuat persaudaraan, dan menjaga integritas.",
                        "Kemenag RI (Indonesia)",
                        "Jumat Pekan Ini",
                        "ID"
                );
            case "JAIS":
                return new KhutbahItem(
                        "Buli: Dosa dan Kezaliman",
                        "Mencegah kezaliman dan memelihara maruah insan",
                        "Khutbah menegaskan bahawa perbuatan buli merosakkan jiwa, maruah dan dilarang keras dalam Islam yang menjunjung tinggi keadilan dan kasih sayang.",
                        "JAIS Selangor",
                        "30 Okt 2026",
                        "JAIS"
                );
            case "JAINJ":
                return new KhutbahItem(
                        "Kunci Rahsia Kesejahteraan Ummah",
                        "Perpaduan dan ketaqwaan asas kekuatan masyarakat",
                        "Kesejahteraan ummah terbina di atas landasan iman, kepatuhan kepada syariat, dan saling menghormati antara satu sama lain.",
                        "JAINJ Johor",
                        "30 Okt 2026",
                        "JAINJ"
                );
            case "JAWI":
            default:
                return new KhutbahItem(
                        "Bersama Allah, Indahnya Hidup Ini",
                        "Hati yang berpaut kepada Allah menemui kebahagiaan sejati",
                        "Kebahagiaan sejati terletak pada hubungan hati yang ikhlas kepada Allah SWT, sentiasa berzikir dan bersyukur dalam setiap nikmat dan dugaan.",
                        "JAWI W.P. Putrajaya & KL",
                        "30 Okt 2026",
                        "JAWI"
                );
        }
    }
}
