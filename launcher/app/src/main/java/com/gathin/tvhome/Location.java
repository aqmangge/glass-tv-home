package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * Where the TV is: one of JAKIM's 60 e-solat zones, chosen on the TV (Location screen, from the Wallpaper
 * menu, or by itself on a first start with no zone in the theme). It sets the Waktu Solat zone, and the
 * zone's main town gives the coordinates for the weather line and the Weather app. A choice made on the
 * TV wins over the theme's prayer_zone, weather_lat and weather_lon.
 *
 * Zones and their districts are JAKIM's (as listed by api.waktusolat.app); each zone's main town and its
 * coordinates are from Open-Meteo's geocoding, checked by hand.
 */
final class Location {
    static final class Zone {
        final String code, state, town, districts;
        final double lat, lon;

        Zone(String[] r) {
            code = r[0];
            state = r[1];
            town = r[2];
            districts = r[3];
            lat = Double.parseDouble(r[4]);
            lon = Double.parseDouble(r[5]);
        }
    }

    /** Code, state, main town, districts, latitude, longitude. */
    private static final String[][] ZONES = {
        {"JHR01", "Johor", "Pulau Aur", "Pulau Aur dan Pulau Pemanggil", "2.4497", "104.5211"},
        {"JHR02", "Johor", "Johor Bahru", "Johor Bahru, Kota Tinggi, Mersing, Kulai", "1.4655", "103.7578"},
        {"JHR03", "Johor", "Kluang", "Kluang, Pontian", "2.0305", "103.3169"},
        {"JHR04", "Johor", "Batu Pahat", "Batu Pahat, Muar, Segamat, Gemas Johor, Tangkak", "1.8548", "102.9325"},
        {"KDH01", "Kedah", "Alor Setar", "Kota Setar, Kubang Pasu, Pokok Sena (Daerah Kecil)", "6.121", "100.3601"},
        {"KDH02", "Kedah", "Sungai Petani", "Kuala Muda, Yan, Pendang", "5.647", "100.4877"},
        {"KDH03", "Kedah", "Kuala Nerang", "Padang Terap, Sik", "6.2584", "100.6126"},
        {"KDH04", "Kedah", "Baling", "Baling", "5.6781", "100.9177"},
        {"KDH05", "Kedah", "Kulim", "Bandar Baharu, Kulim", "5.365", "100.5618"},
        {"KDH06", "Kedah", "Langkawi", "Langkawi", "6.3265", "99.8432"},
        {"KDH07", "Kedah", "Gunung Jerai", "Puncak Gunung Jerai", "5.8172", "100.4738"},
        {"KTN01", "Kelantan", "Kota Bharu", "Bachok, Kota Bharu, Machang, Pasir Mas, Pasir Puteh, Tanah Merah, Tumpat, Kuala Krai, Mukim Chiku", "6.1236", "102.2433"},
        {"KTN02", "Kelantan", "Gua Musang", "Gua Musang (Daerah Galas Dan Bertam), Jeli, Jajahan Kecil Lojing", "4.8844", "101.9686"},
        {"MLK01", "Melaka", "Melaka", "Seluruh negeri", "2.1896", "102.2501"},
        {"NGS01", "Negeri Sembilan", "Tampin", "Tampin, Jempol", "2.4701", "102.2302"},
        {"NGS02", "Negeri Sembilan", "Kuala Pilah", "Jelebu, Kuala Pilah, Rembau", "2.7389", "102.2487"},
        {"NGS03", "Negeri Sembilan", "Seremban", "Port Dickson, Seremban", "2.7297", "101.9381"},
        {"PHG01", "Pahang", "Pulau Tioman", "Pulau Tioman", "2.7972", "104.166"},
        {"PHG02", "Pahang", "Kuantan", "Kuantan, Pekan, Muadzam Shah", "3.8077", "103.326"},
        {"PHG03", "Pahang", "Temerloh", "Jerantut, Temerloh, Maran, Bera, Chenor, Jengka", "3.4506", "102.4176"},
        {"PHG04", "Pahang", "Raub", "Bentong, Lipis, Raub", "3.7899", "101.857"},
        {"PHG05", "Pahang", "Genting Sempah", "Genting Sempah, Janda Baik, Bukit Tinggi", "3.35", "101.7833"},
        {"PHG06", "Pahang", "Cameron Highlands", "Cameron Highlands, Genting Highlands, Bukit Fraser", "4.4636", "101.3763"},
        {"PHG07", "Pahang", "Rompin", "Zon Khas Daerah Rompin, (Mukim Rompin, Mukim Endau, Mukim Pontian)", "2.8167", "103.4833"},
        {"PRK01", "Perak", "Tapah", "Tapah, Slim River, Tanjung Malim", "4.1833", "101.2667"},
        {"PRK02", "Perak", "Ipoh", "Kuala Kangsar, Sg. Siput, Ipoh, Batu Gajah, Kampar", "4.5841", "101.0829"},
        {"PRK03", "Perak", "Gerik", "Lenggong, Pengkalan Hulu, Grik", "5.4167", "101.1333"},
        {"PRK04", "Perak", "Temengor", "Temengor, Belum", "5.3333", "101.3667"},
        {"PRK05", "Perak", "Teluk Intan", "Kg Gajah, Teluk Intan, Bagan Datuk, Seri Iskandar, Beruas, Parit, Lumut, Sitiawan, Pulau Pangkor", "4.0222", "101.0208"},
        {"PRK06", "Perak", "Taiping", "Selama, Taiping, Bagan Serai, Parit Buntar", "4.85", "100.7333"},
        {"PRK07", "Perak", "Bukit Larut", "Bukit Larut", "4.8625", "100.7926"},
        {"PLS01", "Perlis", "Perlis", "Seluruh negeri", "6.4414", "100.1986"},
        {"PNG01", "Pulau Pinang", "Pulau Pinang", "Seluruh negeri", "5.4112", "100.3354"},
        {"SBH01", "Sabah", "Sandakan", "Bahagian Sandakan (Timur), Bukit Garam, Semawang, Temanggong, Tambisan, Bandar Sandakan, Sukau", "5.8402", "118.1179"},
        {"SBH02", "Sabah", "Beluran", "Beluran, Telupid, Pinangah, Terusan, Kuamut, Bahagian Sandakan (Barat)", "5.8956", "117.5557"},
        {"SBH03", "Sabah", "Lahad Datu", "Lahad Datu, Silabukan, Kunak, Sahabat, Semporna, Tungku, Bahagian Tawau (Timur)", "5.023", "118.329"},
        {"SBH04", "Sabah", "Tawau", "Bandar Tawau, Balong, Merotai, Kalabakan, Bahagian Tawau (Barat)", "4.2448", "117.8911"},
        {"SBH05", "Sabah", "Kudat", "Kudat, Kota Marudu, Pitas, Pulau Banggi, Bahagian Kudat", "6.8873", "116.8236"},
        {"SBH06", "Sabah", "Gunung Kinabalu", "Gunung Kinabalu", "6.1345", "116.6375"},
        {"SBH07", "Sabah", "Kota Kinabalu", "Kota Kinabalu, Ranau, Kota Belud, Tuaran, Penampang, Papar, Putatan, Bahagian Pantai Barat", "5.9749", "116.0724"},
        {"SBH08", "Sabah", "Keningau", "Pensiangan, Keningau, Tambunan, Nabawan, Bahagian Pendalaman (Atas)", "5.3378", "116.1602"},
        {"SBH09", "Sabah", "Beaufort", "Beaufort, Kuala Penyu, Sipitang, Tenom, Long Pasia, Membakut, Weston, Bahagian Pendalaman (Bawah)", "5.3473", "115.7455"},
        {"SWK01", "Sarawak", "Limbang", "Limbang, Lawas, Sundar, Trusan", "4.7548", "115.0101"},
        {"SWK02", "Sarawak", "Miri", "Miri, Niah, Bekenu, Sibuti, Marudi", "4.3993", "113.9916"},
        {"SWK03", "Sarawak", "Bintulu", "Pandan, Belaga, Suai, Tatau, Sebauh, Bintulu", "3.1667", "113.0333"},
        {"SWK04", "Sarawak", "Sibu", "Sibu, Mukah, Dalat, Song, Igan, Oya, Balingian, Kanowit, Kapit", "2.3", "111.8167"},
        {"SWK05", "Sarawak", "Sarikei", "Sarikei, Matu, Julau, Rajang, Daro, Bintangor, Belawai", "2.1167", "111.5167"},
        {"SWK06", "Sarawak", "Sri Aman", "Lubok Antu, Sri Aman, Roban, Debak, Kabong, Lingga, Engkelili, Betong, Spaoh, Pusa, Saratok", "1.2472", "111.4528"},
        {"SWK07", "Sarawak", "Serian", "Serian, Simunjan, Samarahan, Sebuyau, Meludam", "1.1667", "110.5667"},
        {"SWK08", "Sarawak", "Kuching", "Kuching, Bau, Lundu, Sematan", "1.55", "110.3333"},
        {"SWK09", "Sarawak", "Kampung Patarikan", "Zon Khas (Kampung Patarikan)", "4.8604", "115.409"},
        {"SGR01", "Selangor", "Shah Alam", "Gombak, Petaling, Sepang, Hulu Langat, Hulu Selangor, Shah Alam", "3.0851", "101.5328"},
        {"SGR01", "Selangor", "Cyberjaya", "Cyberjaya, Dengkil, Salak Tinggi, Sepang", "2.9213", "101.6559"},
        {"SGR02", "Selangor", "Kuala Selangor", "Kuala Selangor, Sabak Bernam", "3.35", "101.25"},
        {"SGR03", "Selangor", "Klang", "Klang, Kuala Langat", "3.0367", "101.4433"},
        {"TRG01", "Terengganu", "Kuala Terengganu", "Kuala Terengganu, Marang, Kuala Nerus", "5.3302", "103.1408"},
        {"TRG02", "Terengganu", "Besut", "Besut, Setiu", "5.7336", "102.4897"},
        {"TRG03", "Terengganu", "Hulu Terengganu", "Hulu Terengganu", "5.0734", "103.0114"},
        {"TRG04", "Terengganu", "Dungun", "Dungun, Kemaman", "4.775", "103.4162"},
        {"WLY01", "Wilayah Persekutuan", "Kuala Lumpur", "Pusat Bandaraya Kuala Lumpur", "3.1412", "101.6865"},
        {"WLY01_PJ", "Wilayah Persekutuan", "Putrajaya", "Pusat Pentadbiran Kerajaan Persekutuan", "2.9158", "101.6909"},
        {"WLY02", "Wilayah Persekutuan", "Labuan", "Labuan", "5.2803", "115.2475"},

        // --- 🇸🇬 SINGAPURA (MUIS) ---
        {"SGP01", "Singapura (Singapore)", "Singapura", "Seluruh Negara Singapura (MUIS)", "1.3521", "103.8198"},

        // --- 🇮🇩 INDONESIA (KEMENAG) ---
        {"ID_JKT", "Indonesia: DKI Jakarta", "Jakarta", "Jakarta Pusat, Selatan, Barat, Timur, Utara", "-6.2088", "106.8456"},
        {"ID_BDG", "Indonesia: Jawa Barat", "Bandung", "Kota Bandung, Cimahi, Kab. Bandung", "-6.9175", "107.6191"},
        {"ID_BKR", "Indonesia: Jawa Barat", "Bekasi & Bogor", "Kota Bekasi, Kab. Bekasi, Kota Bogor, Depok", "-6.2383", "106.9756"},
        {"ID_SBY", "Indonesia: Jawa Timur", "Surabaya", "Kota Surabaya, Sidoarjo, Gresik", "-7.2575", "112.7521"},
        {"ID_MLG", "Indonesia: Jawa Timur", "Malang", "Kota Malang, Kota Batu, Kab. Malang", "-7.9797", "112.6304"},
        {"ID_SMG", "Indonesia: Jawa Tengah", "Semarang", "Kota Semarang, Salatiga, Kendal", "-6.9667", "110.4167"},
        {"ID_SOC", "Indonesia: Jawa Tengah", "Surakarta (Solo)", "Kota Surakarta, Sukoharjo, Klaten, Boyolali", "-7.5755", "110.8243"},
        {"ID_JOG", "Indonesia: D.I. Yogyakarta", "Yogyakarta", "Kota Yogyakarta, Sleman, Bantul, Gunungkidul", "-7.7956", "110.3695"},
        {"ID_MDN", "Indonesia: Sumatera Utara", "Medan", "Kota Medan, Binjai, Deli Serdang", "3.5952", "98.6722"},
        {"ID_BTJ", "Indonesia: Aceh", "Banda Aceh", "Kota Banda Aceh, Aceh Besar, Sabang", "5.5483", "95.3238"},
        {"ID_PDG", "Indonesia: Sumatera Barat", "Padang", "Kota Padang, Pariaman, Bukittinggi", "-0.9471", "100.4172"},
        {"ID_PKU", "Indonesia: Riau", "Pekanbaru", "Kota Pekanbaru, Kampar, Siak, Dumai", "0.5071", "101.4478"},
        {"ID_BTH", "Indonesia: Kepulauan Riau", "Batam", "Kota Batam, Tanjung Pinang, Bintan, Karimun", "1.1301", "104.0529"},
        {"ID_PLM", "Indonesia: Sumatera Selatan", "Palembang", "Kota Palembang, Banyuasin, Ogan Ilir", "-2.9761", "104.7754"},
        {"ID_BDJ", "Indonesia: Kalimantan Selatan", "Banjarmasin", "Kota Banjarmasin, Banjarbaru, Kab. Banjar", "-3.3194", "114.5908"},
        {"ID_BPN", "Indonesia: Kalimantan Timur", "Balikpapan & IKN", "Kota Balikpapan, Penajam Paser Utara (IKN)", "-1.2379", "116.8529"},
        {"ID_SMD", "Indonesia: Kalimantan Timur", "Samarinda", "Kota Samarinda, Kutai Kartanegara", "-0.5022", "117.1536"},
        {"ID_PNK", "Indonesia: Kalimantan Barat", "Pontianak", "Kota Pontianak, Kubu Raya", "-0.0263", "109.3425"},
        {"ID_DPS", "Indonesia: Bali", "Denpasar", "Kota Denpasar, Badung, Gianyar, Tabanan", "-8.6705", "115.2126"},
        {"ID_MTR", "Indonesia: Nusa Tenggara Barat", "Mataram", "Kota Mataram, Lombok Barat, Lombok Tengah", "-8.5833", "116.1167"},
        {"ID_UPG", "Indonesia: Sulawesi Selatan", "Makassar", "Kota Makassar, Gowa, Maros", "-5.1477", "119.4327"},
        {"ID_MDC", "Indonesia: Sulawesi Utara", "Manado", "Kota Manado, Minahasa, Bitung", "1.4748", "124.8421"},
    };

    private static List<Zone> all;

    private Location() {
    }

    static synchronized List<Zone> all() {
        if (all == null) {
            all = new ArrayList<>();
            for (String[] r : ZONES) all.add(new Zone(r));
        }
        return all;
    }

    /** The states, in the list's order. */
    static List<String> states() {
        List<String> s = new ArrayList<>();
        for (Zone z : all()) if (!s.contains(z.state)) s.add(z.state);
        return s;
    }

    static List<Zone> inState(String state) {
        List<Zone> s = new ArrayList<>();
        for (Zone z : all()) if (z.state.equals(state)) s.add(z);
        return s;
    }

    static Zone byCode(String code) {
        if (code == null) return null;
        for (Zone z : all()) if (z.code.equalsIgnoreCase(code)) return z;
        return null;
    }

    /** The zone chosen on the TV, or null when none has been chosen there. */
    static Zone chosen(Context ctx) {
        SharedPreferences p = prefs(ctx);
        return byCodeAndTown(p.getString("zone", null), p.getString("zone_town", null));
    }

    static void choose(Context ctx, Zone z) {
        prefs(ctx).edit().putString("zone", z.code).putString("zone_town", z.town).apply();
    }

    /**
     * Several towns can share one JAKIM zone (Shah Alam and Cyberjaya are both SGR01): the town picks the
     * row for the weather and the name shown, the code still decides Waktu Solat. Unknown town → first row.
     */
    static Zone byCodeAndTown(String code, String town) {
        if (code == null) return null;
        if (town != null) {
            for (Zone z : all()) if (z.code.equalsIgnoreCase(code) && z.town.equalsIgnoreCase(town)) return z;
        }
        return byCode(code);
    }

    /** A zone row whose town is {@code town} (case-insensitive), e.g. "Cyberjaya"; null when none. */
    static Zone byTown(String town) {
        if (town == null) return null;
        String t = town.trim();
        int comma = t.indexOf(',');
        if (comma > 0) t = t.substring(0, comma).trim();   // "Cyberjaya, Selangor" -> "Cyberjaya"
        for (Zone z : all()) if (z.town.equalsIgnoreCase(t)) return z;
        return null;
    }

    /** Whether the Location screen has opened by itself once already (first start with no zone). */
    static boolean asked(Context ctx) {
        return prefs(ctx).getBoolean("asked", false);
    }

    static void setAsked(Context ctx) {
        prefs(ctx).edit().putBoolean("asked", true).apply();
    }

    /** What Home uses now, kept for the Weather app to read (LocationProvider). */
    static void publish(Context ctx, String code, String town, String state, double lat, double lon) {
        prefs(ctx).edit().putString("now_code", code).putString("now_town", town).putString("now_state", state)
                .putString("now_lat", String.valueOf(lat)).putString("now_lon", String.valueOf(lon)).apply();
    }

    static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences("location", Context.MODE_PRIVATE);
    }
}
