package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Clean Waktu Solat for Putrajaya (JAKIM Zone WLY01).
 * Fetches official JAKIM e-solat timetable, caches locally, and includes
 * an accurate solar-calculation fallback so it never fails offline.
 */
final class PrayerTimes {
    private static final String TAG = "TvHome.Prayer";
    private static final String PREFS_NAME = "prayer_times_cache";
    private static final String KEY_JSON = "takwim_wly01";
    private static final String KEY_MONTH = "takwim_month";

    // Coordinates for Putrajaya
    private static final double LATITUDE = 2.9264;
    private static final double LONGITUDE = 101.6964;
    private static final double TIMEZONE = 8.0;

    static final String[] NAMES = {"Subuh", "Syuruk", "Zohor", "Asar", "Maghrib", "Isyak"};
    private static final String[] PRAYER_NAMES = NAMES;
    private static final String[] PRAYER_KEYS = {"fajr", "syuruk", "dhuhr", "asr", "maghrib", "isha"};

    static final class Day {
        final int[] minutes;
        final String hijri;

        Day(int[] minutes, String hijri) {
            this.minutes = minutes;
            this.hijri = hijri;
        }
    }

    static Day day(Context ctx, String zone, Calendar date) {
        if (zone == null || zone.isEmpty()) zone = getJakimZone(ctx);
        long[] times = getTimesForToday(ctx, date.get(Calendar.DAY_OF_MONTH));
        if (times == null || times.length < 6) {
            times = calculateFallback(ctx, date);
        }
        if (times == null || times.length < 6) return null;
        int[] minutes = new int[6];
        for (int i = 0; i < 6; i++) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(times[i]);
            minutes[i] = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
        }
        return new Day(minutes, getHijriDate());
    }

    static boolean due(Context ctx, String zone) {
        return day(ctx, zone, Calendar.getInstance()) == null;
    }

    static boolean refresh(Context ctx, String zone) {
        sync(ctx, null);
        return true;
    }

    private PrayerTimes() {
    }

    /** Forces an immediate background sync for the active location/zone. */
    static void sync(Context context, Runnable onUpdated) {
        syncInternal(context, onUpdated, true);
    }

    /** Refresh timetable in background if needed (once a month / day). */
    static void syncIfDue(Context context, Runnable onUpdated) {
        syncInternal(context, onUpdated, false);
    }

    private static void syncInternal(Context context, Runnable onUpdated, boolean force) {
        new Thread(() -> {
            try {
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                Calendar now = Calendar.getInstance();
                int currentMonth = now.get(Calendar.YEAR) * 100 + (now.get(Calendar.MONTH) + 1);
                String zone = getJakimZone(context);
                String cachedZone = prefs.getString("cached_zone", "");

                if (!force && zone != null && zone.equals(cachedZone) && prefs.getInt(KEY_MONTH, 0) == currentMonth && prefs.contains(KEY_JSON)) {
                    return; // already have this month's takwim
                }

                int year = now.get(Calendar.YEAR);
                int month = now.get(Calendar.MONTH) + 1;

                if (zone != null && zone.startsWith("SGP")) {
                    // Majlis Ugama Islam Singapura (MUIS) official timetable via Aladhan Method 11
                    String urlStr = "https://api.aladhan.com/v1/calendarByCity/" + year + "/" + month + "?city=Singapore&country=Singapore&method=11";
                    URL url = new URL(urlStr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 GlassTV/1.0");

                    if (conn.getResponseCode() == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) sb.append(line);
                        reader.close();

                        JSONObject converted = convertAladhanToTakwim(sb.toString(), zone, year, month, TimeZone.getTimeZone("Asia/Singapore"));
                        if (converted != null) {
                            prefs.edit()
                                    .putString(KEY_JSON, converted.toString())
                                    .putInt(KEY_MONTH, currentMonth)
                                    .putString("cached_zone", zone)
                                    .apply();

                            if (onUpdated != null) {
                                new Handler(Looper.getMainLooper()).post(onUpdated);
                            }
                            Log.i(TAG, "Synced MUIS Singapore prayer times successfully");
                            return;
                        }
                    }
                } else if (zone != null && zone.startsWith("ID_")) {
                    // Indonesia (Kemenag) via Aladhan Method 20
                    double lat = prefs.contains("manual_lat")
                            ? Double.parseDouble(prefs.getString("manual_lat", String.valueOf(AppLocationManager.getLatitude(context))))
                            : AppLocationManager.getLatitude(context);
                    double lon = prefs.contains("manual_lon")
                            ? Double.parseDouble(prefs.getString("manual_lon", String.valueOf(AppLocationManager.getLongitude(context))))
                            : AppLocationManager.getLongitude(context);
                    String urlStr = "https://api.aladhan.com/v1/calendar/" + year + "/" + month + "?latitude=" + lat + "&longitude=" + lon + "&method=20";
                    URL url = new URL(urlStr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 GlassTV/1.0");

                    if (conn.getResponseCode() == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) sb.append(line);
                        reader.close();

                        TimeZone idTz = lon < 114.0 ? TimeZone.getTimeZone("Asia/Jakarta")
                                : (lon < 125.0 ? TimeZone.getTimeZone("Asia/Makassar") : TimeZone.getTimeZone("Asia/Jayapura"));
                        JSONObject converted = convertAladhanToTakwim(sb.toString(), zone, year, month, idTz);
                        if (converted != null) {
                            prefs.edit()
                                    .putString(KEY_JSON, converted.toString())
                                    .putInt(KEY_MONTH, currentMonth)
                                    .putString("cached_zone", zone)
                                    .apply();

                            if (onUpdated != null) {
                                new Handler(Looper.getMainLooper()).post(onUpdated);
                            }
                            Log.i(TAG, "Synced Kemenag Indonesia prayer times successfully");
                            return;
                        }
                    }
                } else {
                    String apiZone = (zone != null && zone.contains("_")) ? zone.split("_")[0] : zone;
                    URL url = new URL("https://api.waktusolat.app/v2/solat/" + apiZone);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);
                    conn.setRequestMethod("GET");

                    if (conn.getResponseCode() == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) sb.append(line);
                        reader.close();

                        prefs.edit()
                                .putString(KEY_JSON, sb.toString())
                                .putInt(KEY_MONTH, currentMonth)
                                .putString("cached_zone", zone)
                                .apply();

                        if (onUpdated != null) {
                            new Handler(Looper.getMainLooper()).post(onUpdated);
                        }
                        Log.i(TAG, "Synced prayer times for zone " + zone + " successfully");
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Prayer sync offline/failed: " + e.getMessage());
            }
        }, "prayer-sync").start();
    }

    private static JSONObject convertAladhanToTakwim(String rawJson, String zone, int year, int month, TimeZone tz) {
        try {
            JSONObject root = new JSONObject(rawJson);
            JSONArray data = root.optJSONArray("data");
            if (data == null || data.length() == 0) return null;

            JSONArray prayersArray = new JSONArray();
            Calendar dayCal = Calendar.getInstance(tz);

            for (int i = 0; i < data.length(); i++) {
                JSONObject dayObj = data.getJSONObject(i);
                JSONObject timings = dayObj.getJSONObject("timings");
                JSONObject dateObj = dayObj.getJSONObject("date");
                JSONObject greg = dateObj.getJSONObject("gregorian");
                int dayNum = Integer.parseInt(greg.optString("day", String.valueOf(i + 1)).trim());

                dayCal.set(Calendar.YEAR, year);
                dayCal.set(Calendar.MONTH, month - 1);
                dayCal.set(Calendar.DAY_OF_MONTH, dayNum);
                dayCal.set(Calendar.SECOND, 0);
                dayCal.set(Calendar.MILLISECOND, 0);

                JSONObject pEntry = new JSONObject();
                pEntry.put("day", dayNum);
                if (dateObj.has("hijri")) {
                    pEntry.put("hijri", dateObj.getJSONObject("hijri").optString("date", ""));
                }

                pEntry.put("fajr", parseEpochSeconds(dayCal, timings.getString("Fajr")));
                pEntry.put("syuruk", parseEpochSeconds(dayCal, timings.getString("Sunrise")));
                pEntry.put("dhuhr", parseEpochSeconds(dayCal, timings.getString("Dhuhr")));
                pEntry.put("asr", parseEpochSeconds(dayCal, timings.getString("Asr")));
                pEntry.put("maghrib", parseEpochSeconds(dayCal, timings.getString("Maghrib")));
                pEntry.put("isha", parseEpochSeconds(dayCal, timings.getString("Isha")));

                prayersArray.put(pEntry);
            }

            JSONObject converted = new JSONObject();
            converted.put("zone", zone);
            converted.put("year", year);
            converted.put("month", month);
            converted.put("prayers", prayersArray);
            return converted;
        } catch (Exception e) {
            Log.e(TAG, "Error converting Aladhan JSON: " + e.getMessage());
            return null;
        }
    }

    private static long parseEpochSeconds(Calendar dayCal, String timeWithTz) {
        try {
            String timeStr = timeWithTz.trim().split(" ")[0];
            String[] parts = timeStr.split(":");
            int hour = Integer.parseInt(parts[0].trim());
            int minute = Integer.parseInt(parts[1].trim());
            Calendar c = (Calendar) dayCal.clone();
            c.set(Calendar.HOUR_OF_DAY, hour);
            c.set(Calendar.MINUTE, minute);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            return c.getTimeInMillis() / 1000L;
        } catch (Exception e) {
            return 0L;
        }
    }

    public static class PrayerZone {
        public final String code;
        public final String state;
        public final String name;
        public final double lat;
        public final double lon;

        public PrayerZone(String code, String state, String name, double lat, double lon) {
            this.code = code;
            this.state = state;
            this.name = name;
            this.lat = lat;
            this.lon = lon;
        }

        public String getDisplayName() {
            return name + " (" + state + " • " + code + ")";
        }
    }

    private static final List<PrayerZone> PRESET_ZONES = new ArrayList<>();

    static {
        // W.P. Kuala Lumpur & Putrajaya (Separated)
        PRESET_ZONES.add(new PrayerZone("WLY01", "W.P. Kuala Lumpur", "Kuala Lumpur", 3.1412, 101.6865));
        PRESET_ZONES.add(new PrayerZone("WLY01_PJ", "W.P. Putrajaya", "Putrajaya", 2.9158, 101.6909));
        PRESET_ZONES.add(new PrayerZone("WLY02", "W.P.", "Labuan", 5.3347, 115.2289));

        // Selangor
        PRESET_ZONES.add(new PrayerZone("SGR01", "Selangor", "Shah Alam, Petaling, Gombak, Sepang", 3.0683, 101.5058));
        PRESET_ZONES.add(new PrayerZone("SGR02", "Selangor", "Kuala Selangor, Sabak Bernam", 3.3500, 101.2500));
        PRESET_ZONES.add(new PrayerZone("SGR03", "Selangor", "Klang, Kuala Langat, Banting", 3.0108, 101.4081));

        // Melaka
        PRESET_ZONES.add(new PrayerZone("MLK01", "Melaka", "Bandaraya Melaka, Alor Gajah, Jasin", 2.1909, 102.2571));

        // Negeri Sembilan
        PRESET_ZONES.add(new PrayerZone("NGS02", "Negeri Sembilan", "Seremban, Port Dickson, Nilai", 2.6953, 101.9056));
        PRESET_ZONES.add(new PrayerZone("NGS01", "Negeri Sembilan", "Tampin, Jempol", 2.4700, 102.2300));

        // Johor
        PRESET_ZONES.add(new PrayerZone("JHR02", "Johor", "Johor Bahru, Pasir Gudang, Kulai", 1.4947, 103.7419));
        PRESET_ZONES.add(new PrayerZone("JHR03", "Johor", "Batu Pahat, Muar, Tangkak, Segamat", 1.9191, 102.8658));
        PRESET_ZONES.add(new PrayerZone("JHR04", "Johor", "Kluang, Mersing", 2.0306, 103.3214));

        // Pulau Pinang
        PRESET_ZONES.add(new PrayerZone("PNG01", "Pulau Pinang", "Seluruh Negeri Pulau Pinang", 5.3942, 100.3983));

        // Perak
        PRESET_ZONES.add(new PrayerZone("PRK02", "Perak", "Ipoh, Batu Gajah, Kampar", 4.6397, 101.1083));
        PRESET_ZONES.add(new PrayerZone("PRK03", "Perak", "Taiping, Bagan Serai, Parit Buntar", 4.8569, 100.7417));
        PRESET_ZONES.add(new PrayerZone("PRK04", "Perak", "Lumut, Manjung, Pangkor, Sitiawan", 4.1953, 100.6728));
        PRESET_ZONES.add(new PrayerZone("PRK01", "Perak", "Tapah, Slim River, Tanjung Malim", 3.6800, 101.5200));

        // Kedah & Perlis
        PRESET_ZONES.add(new PrayerZone("KDH01", "Kedah", "Kota Setar, Pokok Sena, Kubang Pasu", 6.1371, 100.3469));
        PRESET_ZONES.add(new PrayerZone("KDH02", "Kedah", "Kuala Muda, Yan, Pendang, Sungai Petani", 5.6297, 100.4677));
        PRESET_ZONES.add(new PrayerZone("KDH07", "Kedah", "Pulau Langkawi", 6.3316, 99.8585));
        PRESET_ZONES.add(new PrayerZone("PLS01", "Perlis", "Kangar, Padang Besar, Arau", 6.4299, 100.2109));

        // Pahang
        PRESET_ZONES.add(new PrayerZone("PHG02", "Pahang", "Kuantan, Pekan, Muadzam Shah", 3.8192, 103.2966));
        PRESET_ZONES.add(new PrayerZone("PHG03", "Pahang", "Jerantut, Temerloh, Maran, Bera", 3.4478, 102.4178));
        PRESET_ZONES.add(new PrayerZone("PHG04", "Pahang", "Bentong, Raub, Lipis", 3.5200, 101.9000));
        PRESET_ZONES.add(new PrayerZone("PHG06", "Pahang", "Cameron Highlands", 4.4700, 101.3800));

        // Terengganu & Kelantan
        PRESET_ZONES.add(new PrayerZone("TRG01", "Terengganu", "Kuala Terengganu, Marang, Kuala Nerus", 5.3117, 103.1325));
        PRESET_ZONES.add(new PrayerZone("TRG04", "Terengganu", "Kemaman, Dungun", 4.2622, 103.4258));
        PRESET_ZONES.add(new PrayerZone("KTN01", "Kelantan", "Kota Bharu, Bachok, Pasir Puteh, Tumpat", 6.1256, 102.2386));
        PRESET_ZONES.add(new PrayerZone("KTN02", "Kelantan", "Machang, Pasir Mas, Tanah Merah", 5.7600, 102.2100));

        // Sabah
        PRESET_ZONES.add(new PrayerZone("SBH07", "Sabah", "Kota Kinabalu, Penampang, Tuaran, Papar", 6.0971, 116.1665));
        PRESET_ZONES.add(new PrayerZone("SBH03", "Sabah", "Tawau, Balung, Merotai, Kalabakan", 4.2564, 117.8931));
        PRESET_ZONES.add(new PrayerZone("SBH05", "Sabah", "Kudat, Kota Marudu, Pitas, Sandakan", 5.8456, 118.0678));

        // Sarawak
        PRESET_ZONES.add(new PrayerZone("SWK08", "Sarawak", "Kuching, Bau, Lundu, Samarahan, Serian", 1.5622, 110.3889));
        PRESET_ZONES.add(new PrayerZone("SWK04", "Sarawak", "Bintulu, Tatau, Sebauh", 3.1772, 113.0412));
        PRESET_ZONES.add(new PrayerZone("SWK02", "Sarawak", "Miri, Niah, Bekenu, Sibuti", 4.4947, 114.0434));
        PRESET_ZONES.add(new PrayerZone("SWK05", "Sarawak", "Sibu, Mukah, Kanowit, Song", 2.2981, 111.8344));

        // Singapura (MUIS)
        PRESET_ZONES.add(new PrayerZone("SGP01", "Singapura (Singapore)", "Singapura", 1.3521, 103.8198));

        // Indonesia (KEMENAG)
        PRESET_ZONES.add(new PrayerZone("ID_JKT", "Indonesia: DKI Jakarta", "Jakarta", -6.2088, 106.8456));
        PRESET_ZONES.add(new PrayerZone("ID_BDG", "Indonesia: Jawa Barat", "Bandung", -6.9175, 107.6191));
        PRESET_ZONES.add(new PrayerZone("ID_BKR", "Indonesia: Jawa Barat", "Bekasi & Bogor", -6.2383, 106.9756));
        PRESET_ZONES.add(new PrayerZone("ID_SBY", "Indonesia: Jawa Timur", "Surabaya", -7.2575, 112.7521));
        PRESET_ZONES.add(new PrayerZone("ID_MLG", "Indonesia: Jawa Timur", "Malang", -7.9797, 112.6304));
        PRESET_ZONES.add(new PrayerZone("ID_SMG", "Indonesia: Jawa Tengah", "Semarang", -6.9667, 110.4167));
        PRESET_ZONES.add(new PrayerZone("ID_SOC", "Indonesia: Jawa Tengah", "Surakarta (Solo)", -7.5755, 110.8243));
        PRESET_ZONES.add(new PrayerZone("ID_JOG", "Indonesia: D.I. Yogyakarta", "Yogyakarta", -7.7956, 110.3695));
        PRESET_ZONES.add(new PrayerZone("ID_MDN", "Indonesia: Sumatera Utara", "Medan", 3.5952, 98.6722));
        PRESET_ZONES.add(new PrayerZone("ID_BTJ", "Indonesia: Aceh", "Banda Aceh", 5.5483, 95.3238));
        PRESET_ZONES.add(new PrayerZone("ID_PDG", "Indonesia: Sumatera Barat", "Padang", -0.9471, 100.4172));
        PRESET_ZONES.add(new PrayerZone("ID_PKU", "Indonesia: Riau", "Pekanbaru", 0.5071, 101.4478));
        PRESET_ZONES.add(new PrayerZone("ID_BTH", "Indonesia: Kepulauan Riau", "Batam", 1.1301, 104.0529));
        PRESET_ZONES.add(new PrayerZone("ID_PLM", "Indonesia: Sumatera Selatan", "Palembang", -2.9761, 104.7754));
        PRESET_ZONES.add(new PrayerZone("ID_BDJ", "Indonesia: Kalimantan Selatan", "Banjarmasin", -3.3194, 114.5908));
        PRESET_ZONES.add(new PrayerZone("ID_BPN", "Indonesia: Kalimantan Timur", "Balikpapan & IKN", -1.2379, 116.8529));
        PRESET_ZONES.add(new PrayerZone("ID_SMD", "Indonesia: Kalimantan Timur", "Samarinda", -0.5022, 117.1536));
        PRESET_ZONES.add(new PrayerZone("ID_PNK", "Indonesia: Kalimantan Barat", "Pontianak", -0.0263, 109.3425));
        PRESET_ZONES.add(new PrayerZone("ID_DPS", "Indonesia: Bali", "Denpasar", -8.6705, 115.2126));
        PRESET_ZONES.add(new PrayerZone("ID_MTR", "Indonesia: Nusa Tenggara Barat", "Mataram", -8.5833, 116.1167));
        PRESET_ZONES.add(new PrayerZone("ID_UPG", "Indonesia: Sulawesi Selatan", "Makassar", -5.1477, 119.4327));
        PRESET_ZONES.add(new PrayerZone("ID_MDC", "Indonesia: Sulawesi Utara", "Manado", 1.4748, 124.8421));
    }

    public static List<PrayerZone> getPresetZones() {
        return Collections.unmodifiableList(PRESET_ZONES);
    }

    public static String getActiveZone(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.contains("manual_prayer_zone")) {
            return prefs.getString("manual_prayer_zone", "WLY01");
        }
        return getJakimZone(context);
    }

    public static String getActiveZoneName(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.contains("manual_prayer_zone_name")) {
            return prefs.getString("manual_prayer_zone_name", "Putrajaya & Kuala Lumpur");
        }
        String zone = getActiveZone(context);
        for (PrayerZone pz : PRESET_ZONES) {
            if (pz.code.equalsIgnoreCase(zone)) return pz.name;
        }
        return AppLocationManager.getLocationName(context);
    }

    public static String getShortZoneName(Context context) {
        String name = getActiveZoneName(context);
        if (name == null || name.trim().isEmpty()) return "Putrajaya";
        int cut = name.indexOf(',');
        if (cut > 0) name = name.substring(0, cut);
        cut = name.indexOf('&');
        if (cut > 0) name = name.substring(0, cut);
        cut = name.indexOf('(');
        if (cut > 0) name = name.substring(0, cut);
        name = name.trim();
        return name.isEmpty() ? "Putrajaya" : name;
    }

    public static void clearManualZone(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .remove("manual_prayer_zone")
                .remove("manual_prayer_zone_name")
                .remove("manual_lat")
                .remove("manual_lon")
                .apply();
    }

    public static void setZone(Context context, String zoneCode, String name, double lat, double lon) {
        if (zoneCode == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString("manual_prayer_zone", zoneCode)
                .putString("manual_prayer_zone_name", name != null ? name : zoneCode)
                .putString("manual_lat", String.valueOf(lat))
                .putString("manual_lon", String.valueOf(lon))
                .remove(KEY_JSON)
                .remove("cached_zone")
                .apply();
        sync(context, null);
    }

    public static void setZone(Context context, String zoneCode) {
        if (zoneCode == null) return;
        Location.Zone z = Location.byCode(zoneCode);
        if (z != null) {
            setZone(context, z.code, z.town, z.lat, z.lon);
        } else {
            setZone(context, zoneCode, zoneCode, 0, 0);
        }
    }

    public static void setPrayerZone(Context context, PrayerZone zone) {
        if (zone == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString("manual_prayer_zone", zone.code)
                .putString("manual_prayer_zone_name", zone.name)
                .putString("manual_lat", String.valueOf(zone.lat))
                .putString("manual_lon", String.valueOf(zone.lon))
                .apply();
        sync(context, null);
    }

    private static String getJakimZone(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.contains("manual_prayer_zone")) {
            return prefs.getString("manual_prayer_zone", "WLY01");
        }
        String loc = AppLocationManager.getLocationName(context).toLowerCase(Locale.ROOT);
        String stationId = AppLocationManager.getStationId(context).toUpperCase(Locale.ROOT);

        if (loc.contains("singapore") || loc.contains("singapura") || stationId.startsWith("SG")) return "SGP01";
        if (loc.contains("jakarta") || stationId.equals("ID01")) return "ID_JKT";
        if (loc.contains("bandung") || stationId.equals("ID02")) return "ID_BDG";
        if (loc.contains("surabaya") || stationId.equals("ID03")) return "ID_SBY";
        if (loc.contains("medan") || stationId.equals("ID04")) return "ID_MDN";
        if (loc.contains("batam") || stationId.equals("ID05")) return "ID_BTH";
        if (loc.contains("bali") || loc.contains("denpasar") || stationId.equals("ID06")) return "ID_DPS";

        if (loc.contains("putrajaya") || stationId.equals("CA17W")) return "WLY01_PJ";
        if (loc.contains("kuala lumpur") || loc.contains("cheras") || loc.contains("batu muda")) return "WLY01";
        if (loc.contains("labuan")) return "WLY02";
        if (loc.contains("melaka") || loc.contains("alor gajah") || loc.contains("rambai")) return "MLK01";
        if (loc.contains("petaling") || loc.contains("shah alam")) return "SGR01";
        if (loc.contains("klang") || loc.contains("banting")) return "SGR03";
        if (loc.contains("seremban") || loc.contains("nilai") || loc.contains("port dickson")) return "NGS02";
        if (loc.contains("johor") || loc.contains("larkin") || loc.contains("pasir gudang")) return "JHR02";
        if (loc.contains("batu pahat")) return "JHR03";
        if (loc.contains("kluang")) return "JHR04";
        if (loc.contains("tangkak")) return "JHR01";
        if (loc.contains("pinang") || loc.contains("seberang") || loc.contains("minden")) return "PNG01";
        if (loc.contains("ipoh") || loc.contains("tasek") || loc.contains("taiping")) return "PRK02";
        if (loc.contains("manjung")) return "PRK01";
        if (loc.contains("alor setar")) return "KDH01";
        if (loc.contains("sungai petani")) return "KDH02";
        if (loc.contains("langkawi")) return "KDH07";
        if (loc.contains("perlis") || loc.contains("kangar")) return "PLS01";
        if (loc.contains("kuantan")) return "PHG02";
        if (loc.contains("temerloh")) return "PHG04";
        if (loc.contains("terengganu") || loc.contains("kemaman")) return "TRG01";
        if (loc.contains("kota bharu") || loc.contains("kelantan")) return "KTN01";
        if (loc.contains("kota kinabalu")) return "SBH07";
        if (loc.contains("sandakan")) return "SBH05";
        if (loc.contains("tawau")) return "SBH03";
        if (loc.contains("kuching")) return "SWK08";
        if (loc.contains("miri")) return "SWK04";
        if (loc.contains("bintulu")) return "SWK06";
        if (loc.contains("sibu")) return "SWK05";

        return "WLY01"; // Default Putrajaya
    }

    static class PrayerItem {
        final String name;
        final String timeStr;
        final boolean isNext;
        final boolean isPast;

        PrayerItem(String name, String timeStr, boolean isNext, boolean isPast) {
            this.name = name;
            this.timeStr = timeStr;
            this.isNext = isNext;
            this.isPast = isPast;
        }
    }

    /** Returns all 6 prayers for today with formatted times and status. */
    static PrayerItem[] getPrayerItems(Context context) {
        Calendar now = Calendar.getInstance();
        int day = now.get(Calendar.DAY_OF_MONTH);
        long[] times = getTimesForToday(context, day);
        if (times == null || times.length < 6) {
            times = calculateFallback(context, now);
        }
        long nowMs = now.getTimeInMillis();
        int nextIdx = -1;
        for (int i = 0; i < times.length; i++) {
            if (i == 1) continue;
            if (times[i] > nowMs) {
                nextIdx = i;
                break;
            }
        }
        if (nextIdx == -1) nextIdx = 0;

        SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String[] prayerNames = AppLanguage.getPrayerNames(context);
        PrayerItem[] items = new PrayerItem[6];
        for (int i = 0; i < 6; i++) {
            items[i] = new PrayerItem(
                    prayerNames[i],
                    timeFmt.format(new Date(times[i])),
                    i == nextIdx,
                    times[i] < nowMs
            );
        }
        return items;
    }

    /** Returns formatted clean UI CharSequence with highlighted next prayer, bold and legible. */
    static CharSequence getFormatted(Context context) {
        Calendar now = Calendar.getInstance();
        int day = now.get(Calendar.DAY_OF_MONTH);
        long[] times = getTimesForToday(context, day);

        if (times == null || times.length < 6) {
            times = calculateFallback(context, now);
        }

        long nowMs = now.getTimeInMillis();
        int nextIdx = -1;

        // Find next prayer (skip Syuruk idx 1)
        for (int i = 0; i < times.length; i++) {
            if (i == 1) continue;
            if (times[i] > nowMs) {
                nextIdx = i;
                break;
            }
        }
        if (nextIdx == -1) nextIdx = 0; // After Isyak -> Subuh next morning

        SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String[] prayerNames = AppLanguage.getPrayerNames(context);
        String nextWord = AppLanguage.getNextPrayerPrefix(context);

        SpannableStringBuilder sb = new SpannableStringBuilder();

        // LINE 1: LOCATION  •  NEXT / SETERUSNYA: ASAR 16:18
        String nextName = prayerNames[nextIdx].toUpperCase(Locale.ROOT);
        long displayTimeMs = times[nextIdx];
        if (nextIdx == 0 && times[0] < nowMs) {
            Calendar tomorrow = (Calendar) now.clone();
            tomorrow.add(Calendar.DAY_OF_MONTH, 1);
            long[] tomTimes = getTimesForToday(context, tomorrow.get(Calendar.DAY_OF_MONTH));
            if (tomTimes == null || tomTimes.length < 6) tomTimes = calculateFallback(context, tomorrow);
            if (tomTimes != null && tomTimes.length > 0) displayTimeMs = tomTimes[0];
        }
        String nextTime = timeFmt.format(new Date(displayTimeMs));

        int l1Start = sb.length();
        String zoneHeader = getShortZoneName(context).toUpperCase(Locale.ROOT);
        sb.append(zoneHeader).append("  •  ").append(nextWord).append(": ").append(nextName).append(" ").append(nextTime);
        int l1End = sb.length();
        sb.setSpan(new ForegroundColorSpan(0xFFFFFFFF), l1Start, l1End, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new StyleSpan(Typeface.BOLD), l1Start, l1End, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        sb.append("\n");

        // LINE 2: SUBUH 05:49  •  SYURUK 06:55  •  ZOHOR 13:01  •  ASAR 16:18  •  MAGHRIB 19:00  •  ISYAK 20:10
        int[] displayIndices = {0, 1, 2, 3, 4, 5}; // Subuh, Syuruk, Zohor, Asar, Maghrib, Isyak
        for (int i = 0; i < displayIndices.length; i++) {
            int idx = displayIndices[i];
            boolean isNext = (idx == nextIdx);
            int start = sb.length();

            String name = prayerNames[idx].toUpperCase(Locale.ROOT);
            String timeStr = timeFmt.format(new Date(times[idx]));

            sb.append(name).append(" ").append(timeStr);
            if (isNext) {
                sb.setSpan(new ForegroundColorSpan(0xFF4ADE80), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new StyleSpan(Typeface.BOLD), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                sb.setSpan(new ForegroundColorSpan(0xE6FFFFFF), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new StyleSpan(Typeface.BOLD), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            if (i < displayIndices.length - 1) {
                int dotStart = sb.length();
                sb.append("  •  ");
                sb.setSpan(new ForegroundColorSpan(0x80FFFFFF), dotStart, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        return sb;
    }

    private static long[] getTimesForToday(Context context, int day) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String jsonStr = prefs.getString(KEY_JSON, null);
            if (jsonStr == null) return null;

            JSONObject obj = new JSONObject(jsonStr);
            JSONArray prayers = obj.getJSONArray("prayers");
            for (int i = 0; i < prayers.length(); i++) {
                JSONObject p = prayers.getJSONObject(i);
                if (p.optInt("day") == day) {
                    long[] res = new long[6];
                    for (int k = 0; k < 6; k++) {
                        res[k] = p.getLong(PRAYER_KEYS[k]) * 1000L;
                    }
                    return res;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /** Astronomical calculation fallback based on user's active Malaysian, Singapore, or Indonesian location. */
    private static long[] calculateFallback(Context context, Calendar cal) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        double latitude = prefs.contains("manual_lat")
                ? Double.parseDouble(prefs.getString("manual_lat", String.valueOf(AppLocationManager.getLatitude(context))))
                : AppLocationManager.getLatitude(context);
        double longitude = prefs.contains("manual_lon")
                ? Double.parseDouble(prefs.getString("manual_lon", String.valueOf(AppLocationManager.getLongitude(context))))
                : AppLocationManager.getLongitude(context);

        String zone = getActiveZone(context);
        String loc = AppLocationManager.getLocationName(context).toLowerCase(Locale.ROOT);
        String stationId = AppLocationManager.getStationId(context).toUpperCase(Locale.ROOT);
        boolean isSingapore = (zone != null && (zone.startsWith("SGP") || zone.startsWith("SG0")))
                || stationId.startsWith("SG")
                || loc.contains("singapore")
                || loc.contains("singapura");
        boolean isIndonesia = (zone != null && zone.startsWith("ID_"))
                || stationId.startsWith("ID")
                || loc.contains("indonesia");

        double tz = 8.0;
        if (isIndonesia) {
            // Indonesia timezones: WIB (UTC+7), WITA (UTC+8), WIT (UTC+9)
            if (longitude < 114.0) {
                tz = 7.0; // WIB
            } else if (longitude < 125.0) {
                tz = 8.0; // WITA
            } else {
                tz = 9.0; // WIT
            }
        } else {
            tz = 8.0; // Malaysia & Singapore (UTC+8)
        }

        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);

        double d = 367.0 * year - Math.floor((7.0 * (year + Math.floor((month + 9.0) / 12.0))) / 4.0)
                + Math.floor((275.0 * month) / 9.0) + day - 730531.5;

        double L = (280.461 + 0.9856474 * d) % 360.0;
        if (L < 0) L += 360.0;
        double M = (357.528 + 0.9856003 * d) % 360.0;
        if (M < 0) M += 360.0;

        double lambda = L + 1.915 * Math.sin(Math.toRadians(M)) + 0.020 * Math.sin(Math.toRadians(2 * M));
        double epsilon = 23.439 - 0.0000004 * d;
        double alpha = Math.toDegrees(Math.atan2(Math.cos(Math.toRadians(epsilon)) * Math.sin(Math.toRadians(lambda)), Math.cos(Math.toRadians(lambda)))) / 15.0;
        double delta = Math.toDegrees(Math.asin(Math.sin(Math.toRadians(epsilon)) * Math.sin(Math.toRadians(lambda))));

        double noon = 12.0 + tz - (longitude / 15.0) - (L / 15.0 - alpha);
        if (noon < 0) noon += 24.0;
        if (noon >= 24) noon -= 24.0;

        double fajrHour = noon - sunAngleHour(-20.0, latitude, delta);
        double sunriseHour = noon - sunAngleHour(-0.833, latitude, delta);
        double asrHour = noon + asrHour(1, latitude, delta);
        double sunsetHour = noon + sunAngleHour(-0.833, latitude, delta);
        double ishaHour = noon + sunAngleHour(-18.0, latitude, delta);

        double[] hours;
        if (isSingapore) {
            // Singapore (MUIS standard: Fajr 20°, Isha 18°, 0-minute ihtiyat buffer, exact solar transit)
            hours = new double[]{fajrHour, sunriseHour, noon, asrHour + 0.012, sunsetHour, ishaHour};
        } else if (isIndonesia) {
            // Indonesia (Kemenag standard: +2 min ihtiyat)
            hours = new double[]{fajrHour + 0.033, sunriseHour, noon + 0.033, asrHour + 0.033, sunsetHour + 0.033, ishaHour + 0.033};
        } else {
            // Malaysia (JAKIM standard: +2 to +3 min ihtiyat)
            hours = new double[]{fajrHour + 0.05, sunriseHour, noon + 0.04, asrHour + 0.04, sunsetHour + 0.05, ishaHour + 0.05};
        }

        long[] res = new long[6];
        Calendar base = (Calendar) cal.clone();
        base.set(Calendar.SECOND, 0);
        base.set(Calendar.MILLISECOND, 0);

        for (int i = 0; i < 6; i++) {
            int h, m;
            if (isSingapore) {
                // MUIS uses standard rounding to nearest minute
                long totalM = Math.round(hours[i] * 60.0);
                h = (int) (totalM / 60);
                m = (int) (totalM % 60);
            } else {
                h = (int) hours[i];
                m = (int) ((hours[i] - h) * 60.0);
            }
            base.set(Calendar.HOUR_OF_DAY, h % 24);
            base.set(Calendar.MINUTE, m);
            res[i] = base.getTimeInMillis();
        }
        return res;
    }

    private static double sunAngleHour(double angle, double lat, double delta) {
        double cosH = (Math.sin(Math.toRadians(angle)) - Math.sin(Math.toRadians(lat)) * Math.sin(Math.toRadians(delta)))
                / (Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(delta)));
        cosH = Math.max(-1.0, Math.min(1.0, cosH));
        return Math.toDegrees(Math.acos(cosH)) / 15.0;
    }

    private static double asrHour(int shadowFactor, double lat, double delta) {
        double phi = Math.abs(lat - delta);
        double cotA = shadowFactor + Math.tan(Math.toRadians(phi));
        double angle = Math.toDegrees(Math.atan(1.0 / cotA));
        return sunAngleHour(angle, lat, delta);
    }

    static class ScheduleItem {
        final String name;
        final String timeFormatted;
        final boolean isNext;
        final boolean isPast;

        ScheduleItem(String name, String timeFormatted, boolean isNext, boolean isPast) {
            this.name = name;
            this.timeFormatted = timeFormatted;
            this.isNext = isNext;
            this.isPast = isPast;
        }
    }

    /** Returns full day prayer list for popup display. */
    static java.util.List<ScheduleItem> getFullSchedule(Context context) {
        java.util.List<ScheduleItem> list = new java.util.ArrayList<>();
        Calendar now = Calendar.getInstance();
        long[] times = getTimesForToday(context, now.get(Calendar.DAY_OF_MONTH));
        if (times == null || times.length < 6) times = calculateFallback(context, now);

        long nowMs = now.getTimeInMillis();
        int nextIdx = -1;
        for (int i = 0; i < times.length; i++) {
            if (i == 1) continue; // Skip Syuruk (sunrise)
            if (times[i] > nowMs) {
                nextIdx = i;
                break;
            }
        }
        if (nextIdx == -1) nextIdx = 0;

        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
        String[] prayerNames = AppLanguage.getPrayerNames(context);
        for (int i = 0; i < times.length; i++) {
            boolean isNext = (i == nextIdx);
            boolean isPast = (times[i] < nowMs);
            list.add(new ScheduleItem(prayerNames[i], sdf.format(new Date(times[i])), isNext, isPast));
        }
        return list;
    }

    /** Returns short summary of next prayer (e.g. for screensaver). */
    static String getNextPrayerSummary(Context context) {
        Calendar now = Calendar.getInstance();
        long[] times = getTimesForToday(context, now.get(Calendar.DAY_OF_MONTH));
        if (times == null || times.length < 6) times = calculateFallback(context, now);

        long nowMs = now.getTimeInMillis();
        int nextIdx = -1;
        for (int i = 0; i < times.length; i++) {
            if (i == 1) continue; // Skip Syuruk (sunrise)
            if (times[i] > nowMs) {
                nextIdx = i;
                break;
            }
        }

        String[] prayerNames = AppLanguage.getPrayerNames(context);
        long nextTimeMs;
        String prayerName;
        if (nextIdx != -1) {
            prayerName = prayerNames[nextIdx];
            nextTimeMs = times[nextIdx];
        } else {
            // All prayers today have passed (after Isyak) -> Next is tomorrow's Subuh
            prayerName = prayerNames[0];
            Calendar tomorrow = (Calendar) now.clone();
            tomorrow.add(Calendar.DAY_OF_MONTH, 1);
            long[] tomTimes = getTimesForToday(context, tomorrow.get(Calendar.DAY_OF_MONTH));
            if (tomTimes == null || tomTimes.length < 6) {
                tomTimes = calculateFallback(context, tomorrow);
            }
            nextTimeMs = (tomTimes != null && tomTimes.length > 0) ? tomTimes[0] : (times[0] + 86400000L);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
        String zone = getShortZoneName(context);
        return prayerName + " • " + sdf.format(new Date(nextTimeMs)) + " (" + zone + ")";
    }

    /** Returns Islamic Hijrah date localized according to active language (e.g. "11 Jamadilawal 1448H"). */
    static String getHijriDate(Context context) {
        try {
            java.time.chrono.HijrahDate h = java.time.chrono.HijrahDate.now();
            int day = h.get(java.time.temporal.ChronoField.DAY_OF_MONTH);
            int month = h.get(java.time.temporal.ChronoField.MONTH_OF_YEAR);
            int year = h.get(java.time.temporal.ChronoField.YEAR);
            String lang = context != null ? AppLanguage.getLanguage(context) : AppLanguage.LANG_MS;
            String[] months;
            switch (lang) {
                case AppLanguage.LANG_EN:
                    months = new String[]{
                        "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
                        "Jumada al-Awwal", "Jumada al-Thani", "Rajab", "Sha'ban",
                        "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah"
                    };
                    break;
                case AppLanguage.LANG_ID:
                    months = new String[]{
                        "Muharram", "Safar", "Rabiul Awal", "Rabiul Akhir",
                        "Jumadil Awal", "Jumadil Akhir", "Rajab", "Sya'ban",
                        "Ramadhan", "Syawal", "Zulkaidah", "Zulhijjah"
                    };
                    break;
                case AppLanguage.LANG_MS:
                default:
                    months = new String[]{
                        "Muharram", "Safar", "Rabiulawal", "Rabiulakhir",
                        "Jamadilawal", "Jamadilakhir", "Rejab", "Syaaban",
                        "Ramadan", "Syawal", "Zulkaedah", "Zulhijjah"
                    };
                    break;
            }
            String monthName = (month >= 1 && month <= 12) ? months[month - 1] : "Hijrah";
            return day + " " + monthName + " " + year + "H";
        } catch (Exception e) {
            return "";
        }
    }

    static String getHijriDate() {
        return getHijriDate(null);
    }
}
