package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * The small weather line under the clock (weather_lat / weather_lon): the temperature and sky from
 * Open-Meteo, and the official Air Pollutant Index of the nearest Jabatan Alam Sekitar station from
 * APIMS. The last line is kept, so Home shows it at once and only refreshes it behind.
 */
final class HomeWeather {
    private static final String TAG = "TvHome.Weather";
    static final long REFRESH_MS = 15 * 60_000L;
    private static final String DOE = "https://eqms.doe.gov.my/api3/publicportalapims/mapcaqm";
    /** Malaysian API/IPU bands, with DOE's colours: blue, green, yellow, orange, red. */
    private static final int[] API_TOP = {50, 100, 200, 300, 9999};
    static final int[] API_COLOR = {0xFF3D8BFF, 0xFF34C759, 0xFFFFD60A, 0xFFFF9F0A, 0xFFFF453A};
    private static final String[] API_NAME_EN = {"Good", "Moderate", "Unhealthy", "Very unhealthy", "Hazardous"};
    private static final String[] API_NAME_MS = {"Baik", "Sederhana", "Tidak Sihat", "Sangat Tidak Sihat", "Berbahaya"};

    /** What the line says: the weather part, the API part and its colour; either part may be empty. */
    static final class Line {
        final String weather, api;
        final int apiColor, icon;

        Line(String weather, String api, int apiColor, int icon) {
            this.weather = weather;
            this.api = api;
            this.apiColor = apiColor;
            this.icon = icon;
        }
    }

    private HomeWeather() {}

    /** Forgets the line, so a new location is fetched at once instead of the old place's weather showing. */
    static void forget(Context ctx) {
        prefs(ctx).edit().clear().apply();
    }

    static boolean due(Context ctx) {
        return Math.abs(System.currentTimeMillis() - prefs(ctx).getLong("at", 0)) >= REFRESH_MS;
    }

    /** The last line fetched, or null before the first. Reads storage only and re-localizes dynamically. */
    static Line last(Context ctx) {
        SharedPreferences p = prefs(ctx);
        String w = p.getString("weather", null), a = p.getString("api", "");
        if (w != null && !w.isEmpty() && !Character.isDigit(w.charAt(0))) {
            // An older line, with an emoji: dropped, and fetched again at once.
            w = "";
            p.edit().putLong("at", 0).putString("weather", "").apply();
        }
        int iconCode = p.getInt("icon_code", -1);
        if (w != null && !w.isEmpty() && iconCode >= 0) {
            int degIdx = w.indexOf('°');
            if (degIdx > 0) {
                try {
                    int temp = Integer.parseInt(w.substring(0, degIdx).trim());
                    w = temp + "°  " + describe(iconCode, ctx);
                } catch (Exception ignored) {}
            }
        }
        if (!a.isEmpty()) {
            boolean isMalay = isMalayLanguage(ctx);
            if (isMalay && a.startsWith("API ")) {
                a = "IPU " + a.substring(4);
                for (int i = 0; i < API_NAME_EN.length; i++) {
                    if (a.contains(API_NAME_EN[i])) {
                        a = a.replace(API_NAME_EN[i], API_NAME_MS[i]);
                    }
                }
            } else if (!isMalay && a.startsWith("IPU ")) {
                a = "API " + a.substring(4);
                for (int i = 0; i < API_NAME_MS.length; i++) {
                    if (a.contains(API_NAME_MS[i])) {
                        a = a.replace(API_NAME_MS[i], API_NAME_EN[i]);
                    }
                }
            }
        }
        return w == null && a.isEmpty() ? null : new Line(w == null ? "" : w, a, p.getInt("api_color", 0),
                iconCode < 0 ? 0 : icon(iconCode, p.getBoolean("icon_day", true)));
    }

    /** Fetches both parts. Network; never on the UI thread. Returns null when neither could be read. */
    static Line refresh(Context ctx, double lat, double lon) {
        String weather = "", api = "";
        int color = 0, code = -1;
        boolean day = true;
        try {
            JSONObject now = new JSONObject(get("https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon
                    + "&current=temperature_2m,weather_code,is_day,precipitation,rain&timezone=Asia%2FKuala_Lumpur")).getJSONObject("current");
            code = now.optInt("weather_code");
            day = now.optInt("is_day", 1) == 1;
            double precip = now.optDouble("precipitation", 0.0);
            double rain = now.optDouble("rain", 0.0);

            // Tropical Calibration: Open-Meteo European models often simulate 0.1-0.3mm trace moisture
            // as code 51-55 in Malaysia during hot sunny days. If precipitation < 0.5mm, it is partly sunny / clear.
            if (day && code >= 51 && code <= 55 && precip < 0.5 && rain < 0.4) {
                code = 2; // Partly sunny / Sebahagian Cerah
            }

            weather = Math.round(now.optDouble("temperature_2m")) + "°  " + describe(code, ctx);
        } catch (Exception e) {
            Log.w(TAG, "weather failed: " + e.getMessage());
        }
        try {
            JSONArray all = new JSONObject(get(DOE)).getJSONArray("data");
            JSONObject near = null;
            double best = Double.MAX_VALUE;
            for (int i = 0; i < all.length(); i++) {
                JSONObject st = all.getJSONObject(i);
                if (st.optInt("API", -1) < 0) continue;
                double dx = st.optDouble("LONGITUDE") - lon, dy = st.optDouble("LATITUDE") - lat, d = dx * dx + dy * dy;
                if (d < best) {
                    best = d;
                    near = st;
                }
            }
            // Only a station near home (about 80 km): the network covers Malaysia, and outside it the
            // nearest one would be far away and say nothing about the air here.
            if (near != null && best < 0.5) {
                int value = near.getInt("API"), band = 0;
                while (band < API_TOP.length - 1 && value > API_TOP[band]) band++;
                boolean isMalay = isMalayLanguage(ctx);
                String prefix = isMalay ? "IPU " : "API ";
                String[] apiNames = isMalay ? API_NAME_MS : API_NAME_EN;
                api = prefix + value + " " + apiNames[band];
                color = API_COLOR[band];
            }
        } catch (Exception e) {
            Log.w(TAG, "APIMS failed: " + e.getMessage());
        }
        // A failed try waits for the next round too, rather than asking again every minute.
        SharedPreferences.Editor ed = prefs(ctx).edit().putLong("at", System.currentTimeMillis());
        if (weather.isEmpty() && api.isEmpty()) {
            ed.apply();
            return null;
        }
        ed.putString("weather", weather).putString("api", api).putInt("api_color", color)
                .putInt("icon_code", code).putBoolean("icon_day", day).apply();
        return new Line(weather, api, color, code < 0 ? 0 : icon(code, day));
    }

    static boolean isMalayLanguage(Context ctx) {
        if (ctx == null) return true;
        String lang = AppLanguage.getLanguage(ctx);
        return AppLanguage.LANG_MS.equals(lang) || AppLanguage.LANG_ID.equals(lang)
                || java.util.Locale.getDefault().getLanguage().startsWith("ms")
                || java.util.Locale.getDefault().getLanguage().startsWith("in")
                || java.util.Locale.getDefault().getLanguage().startsWith("id");
    }

    private static String describe(int code, Context ctx) {
        boolean isMalay = isMalayLanguage(ctx);
        if (code == 0) return isMalay ? "Cerah" : "Clear";
        if (code == 1) return isMalay ? "Sebahagiannya cerah" : "Mainly clear";
        if (code == 2) return isMalay ? "Sebahagiannya mendung" : "Partly cloudy";
        if (code == 3) return isMalay ? "Mendung" : "Cloudy";
        if (code == 45 || code == 48) return isMalay ? "Kabus" : "Haze";
        if (code >= 51 && code <= 57) return isMalay ? "Gerimis" : "Drizzle";
        if (code >= 61 && code <= 67) return code >= 65 ? (isMalay ? "Hujan lebat" : "Heavy rain") : (isMalay ? "Hujan" : "Rain");
        if (code >= 80 && code <= 82) return isMalay ? "Hujan renyai" : "Showers";
        if (code >= 95) return isMalay ? "Ribut petir" : "Thunderstorm";
        return "";
    }

    /** The sky as a small vector symbol (not an emoji: the colour emoji font alone costs Home megabytes). */
    static int icon(int code, boolean day) {
        if (code == 0) return day ? R.drawable.wx_sun : R.drawable.wx_moon;
        if (code == 1 || code == 2) return day ? R.drawable.wx_partly : R.drawable.wx_partly_night;
        if (code == 3) return R.drawable.wx_cloud;
        if (code == 45 || code == 48) return R.drawable.wx_fog;
        if (code >= 51 && code <= 57) return R.drawable.wx_drizzle;
        if (code >= 61 && code <= 67) return R.drawable.wx_rain;
        if (code >= 80 && code <= 82) return R.drawable.wx_rain;
        if (code >= 95) return R.drawable.wx_storm;
        return R.drawable.wx_cloud;
    }

    private static String get(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(20000);
        c.setRequestProperty("User-Agent", "GlassTV/1.0 (Android TV home screen)");
        try {
            if (c.getResponseCode() != 200) throw new IllegalStateException("HTTP " + c.getResponseCode());
            try (InputStream in = c.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buf = new byte[16 * 1024];
                int r;
                while ((r = in.read(buf)) > 0) {
                    out.write(buf, 0, r);
                    if (out.size() > 512 * 1024) throw new IllegalStateException("response too large");
                }
                return new String(out.toByteArray(), StandardCharsets.UTF_8);
            }
        } finally {
            c.disconnect();
        }
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences("home_weather", Context.MODE_PRIVATE);
    }
}
