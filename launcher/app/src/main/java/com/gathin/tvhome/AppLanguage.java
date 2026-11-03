package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

/**
 * Multi-language support for Glass TV Home:
 * - Bahasa Melayu (ms) [Default]
 * - English (en)
 * - Bahasa Indonesia (id)
 */
final class AppLanguage {
    public static final String LANG_MS = "ms"; // Bahasa Melayu
    public static final String LANG_EN = "en"; // English
    public static final String LANG_ID = "id"; // Bahasa Indonesia

    private static final String PREFS_NAME = "language_prefs";
    private static final String KEY_LANG = "selected_lang";

    private AppLanguage() {}

    public static String getLanguage(Context ctx) {
        if (ctx == null) return LANG_MS;
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LANG, LANG_MS);
    }

    public static void setLanguage(Context ctx, String lang) {
        if (ctx == null || lang == null) return;
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LANG, lang)
                .apply();
    }

    public static String getLanguageDisplayName(Context ctx) {
        String lang = getLanguage(ctx);
        switch (lang) {
            case LANG_EN:
                return "English";
            case LANG_ID:
                return "Bahasa Indonesia";
            case LANG_MS:
            default:
                return "Bahasa Melayu";
        }
    }

    public static String getLanguageBadge(Context ctx) {
        String lang = getLanguage(ctx);
        switch (lang) {
            case LANG_EN:
                return "🇬🇧 EN";
            case LANG_ID:
                return "🇮🇩 ID";
            case LANG_MS:
            default:
                return "🇲🇾 BM";
        }
    }

    public static String[] getPrayerNames(Context ctx) {
        String lang = getLanguage(ctx);
        switch (lang) {
            case LANG_EN:
                return new String[]{"Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha"};
            case LANG_ID:
                return new String[]{"Subuh", "Terbit", "Dzuhur", "Ashar", "Maghrib", "Isya"};
            case LANG_MS:
            default:
                return new String[]{"Subuh", "Syuruk", "Zohor", "Asar", "Maghrib", "Isyak"};
        }
    }

    public static String getPrayerHeaderPrefix(Context ctx) {
        String lang = getLanguage(ctx);
        switch (lang) {
            case LANG_EN:
                return "PRAYER TIMES";
            case LANG_ID:
                return "WAKTU SHOLAT";
            case LANG_MS:
            default:
                return "WAKTU SOLAT";
        }
    }

    public static String getAyahLabel(Context ctx, String reference) {
        String lang = getLanguage(ctx);
        String prefix;
        switch (lang) {
            case LANG_EN:
                prefix = "DAILY PRAYER";
                break;
            case LANG_ID:
            case LANG_MS:
            default:
                prefix = "DOA HARIAN";
                break;
        }
        if (reference != null && !reference.isEmpty()) {
            return prefix + "  •  " + reference.toUpperCase(Locale.ROOT);
        }
        switch (lang) {
            case LANG_EN:
                return prefix + "  •  VERSE OF THE DAY";
            case LANG_ID:
            case LANG_MS:
            default:
                return prefix + "  •  AYAT PILIHAN";
        }
    }

    public static String getNextPrayerPrefix(Context ctx) {
        String lang = getLanguage(ctx);
        switch (lang) {
            case LANG_EN:
                return "NEXT";
            case LANG_ID:
                return "SELANJUTNYA";
            case LANG_MS:
            default:
                return "SETERUSNYA";
        }
    }

    public static String getPeriodString(Context ctx, int hour) {
        String lang = getLanguage(ctx);
        switch (lang) {
            case LANG_EN:
                return hour < 12 ? "AM" : "PM";
            case LANG_ID:
                if (hour >= 1 && hour < 11) return "PAGI";
                else if (hour >= 11 && hour < 15) return "SIANG";
                else if (hour >= 15 && hour < 18) return "SORE";
                else return "MLM";
            case LANG_MS:
            default:
                if (hour >= 1 && hour < 12) return "PG";
                else if (hour >= 12 && hour < 19) return "PTG";
                else return "MLM";
        }
    }

    // --- UI Labels for Popups & Settings ---

    public static String strRestart(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Restart TV";
            case LANG_ID: return "Mulai Ulang TV";
            case LANG_MS: default: return "Mula Semula TV";
        }
    }

    public static String strTurnOff(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Turn Off TV";
            case LANG_ID: return "Matikan TV";
            case LANG_MS: default: return "Matikan TV";
        }
    }

    public static String strCleanCache(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Clean TV Cache";
            case LANG_ID: return "Bersihkan Cache TV";
            case LANG_MS: default: return "Bersihkan Cache TV";
        }
    }

    public static String strWallpaper(Context ctx) {
        return "Wallpaper";
    }

    public static String strLanguage(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Language Selection";
            case LANG_ID: return "Pilihan Bahasa";
            case LANG_MS: default: return "Pilihan Bahasa";
        }
    }

    public static String strAllSettings(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "All Android Settings";
            case LANG_ID: return "Semua Pengaturan Android";
            case LANG_MS: default: return "Semua Tetapan Android";
        }
    }

    public static String strClose(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Close";
            case LANG_ID: return "Tutup";
            case LANG_MS: default: return "Tutup";
        }
    }

    // --- Modern Settings UI Strings ---
    public static String strSettingsSearch(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Search";
            case LANG_ID: return "Cari";
            case LANG_MS: default: return "Cari";
        }
    }

    public static String strProfileTitle(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Profile";
            case LANG_ID: return "Profil";
            case LANG_MS: default: return "Profil";
        }
    }

    public static String strProfileSubtitle(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "User Profiles";
            case LANG_ID: return "Profil Pengguna";
            case LANG_MS: default: return "Profil Pengguna";
        }
    }

    public static String strAvatars(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Avatars";
            case LANG_ID: return "Avatar";
            case LANG_MS: default: return "Avatar";
        }
    }

    public static String strYourPhotos(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Your Photos";
            case LANG_ID: return "Foto Anda";
            case LANG_MS: default: return "Gambar Anda";
        }
    }

    public static String strYourPhotosDesc(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Add photos to the TV's Pictures or Download folder.";
            case LANG_ID: return "Tambahkan foto ke folder Gambar atau Unduhan TV.";
            case LANG_MS: default: return "Tambah gambar ke folder Pictures atau Download di TV.";
        }
    }

    public static String strEditName(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Edit Name";
            case LANG_ID: return "Ubah Nama";
            case LANG_MS: default: return "Tukar Nama";
        }
    }

    public static String strHomeScreen(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Home Screen";
            case LANG_ID: return "Layar Utama";
            case LANG_MS: default: return "Skrin Utama";
        }
    }

    public static String strAppearance(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Appearance";
            case LANG_ID: return "Tampilan";
            case LANG_MS: default: return "Penampilan";
        }
    }

    public static String strStatusBarWeather(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Status Bar & Weather";
            case LANG_ID: return "Bilah Status & Cuaca";
            case LANG_MS: default: return "Bar Status & Cuaca";
        }
    }

    public static String strStandByMode(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "StandBy Mode";
            case LANG_ID: return "Mode StandBy";
            case LANG_MS: default: return "Mod StandBy";
        }
    }

    public static String strRemoteAccessibility(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Remote & Accessibility";
            case LANG_ID: return "Remote & Aksesibilitas";
            case LANG_MS: default: return "Alat Kawalan & Aksesibiliti";
        }
    }

    public static String strChildMode(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Child Mode";
            case LANG_ID: return "Mode Anak";
            case LANG_MS: default: return "Mod Kanak-kanak";
        }
    }

    public static String strGeneral(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "General";
            case LANG_ID: return "Umum";
            case LANG_MS: default: return "Umum";
        }
    }

    public static String strTv(Context ctx) {
        return "TV";
    }

    public static String strBack(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Back";
            case LANG_ID: return "Kembali";
            case LANG_MS: default: return "Kembali";
        }
    }

    public static String strCancel(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Cancel";
            case LANG_ID: return "Batal";
            case LANG_MS: default: return "Batal";
        }
    }

    public static String strLocation(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Location";
            case LANG_ID: return "Lokasi";
            case LANG_MS: default: return "Lokasi";
        }
    }

    public static String strLocationRow(Context ctx, String zoneName) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "📍 Location (Weather & Prayer): " + zoneName;
            case LANG_ID: return "📍 Lokasi (Cuaca & Sholat): " + zoneName;
            case LANG_MS: default: return "📍 Lokasi (Cuaca & Solat): " + zoneName;
        }
    }

    public static String strDynamicWeather(Context ctx, boolean on) {
        String state = on ? "ON" : "OFF";
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Dynamic Weather: " + state;
            case LANG_ID: return "Cuaca Dinamis: " + state;
            case LANG_MS: default: return "Cuaca Dinamik: " + state;
        }
    }

    public static String strAzanRow(Context ctx, boolean on) {
        String state = on ? "ON" : "OFF";
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Adhan Settings: " + state;
            case LANG_ID: return "Pengaturan Adzan: " + state;
            case LANG_MS: default: return "Tetapan Azan: " + state;
        }
    }

    public static String strDailyDoaRow(Context ctx, boolean on) {
        String state = on ? "ON" : "OFF";
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Daily Prayer: " + state;
            case LANG_ID: return "Doa Harian: " + state;
            case LANG_MS: default: return "Doa Harian: " + state;
        }
    }

    public static String strSoundRow(Context ctx, String themeName) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Sound: " + themeName;
            case LANG_ID: return "Efek Suara: " + themeName;
            case LANG_MS: default: return "Kesan Bunyi: " + themeName;
        }
    }

    public static String strAutoUpdateRow(Context ctx, boolean on, String version) {
        String state = on ? "ON" : "OFF";
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Auto-Update: " + state + " (v" + version + ")";
            case LANG_ID: return "Pembaruan Otomatis: " + state + " (v" + version + ")";
            case LANG_MS: default: return "Kemas Kini Automatik: " + state + " (v" + version + ")";
        }
    }

    public static String strScreensaver(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Nightstand Screensaver";
            case LANG_ID: return "Screensaver Meja Tidur";
            case LANG_MS: default: return "Screensaver Meja Tidur";
        }
    }

    public static String strAzanSettings(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Adhan Settings";
            case LANG_ID: return "Pengaturan Adzan";
            case LANG_MS: default: return "Tetapan Azan";
        }
    }

    public static String strSoundEffects(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Sound Effects";
            case LANG_ID: return "Efek Suara";
            case LANG_MS: default: return "Kesan Bunyi";
        }
    }

    public static String strAutoUpdate(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Auto-Update";
            case LANG_ID: return "Pembaruan Otomatis";
            case LANG_MS: default: return "Kemas Kini Automatik";
        }
    }

    public static String strLanguageChangedToast(Context ctx, String lang) {
        switch (lang) {
            case LANG_EN:
                return "Language switched to English 🇬🇧";
            case LANG_ID:
                return "Bahasa diubah ke Bahasa Indonesia 🇮🇩";
            case LANG_MS:
            default:
                return "Bahasa ditukar ke Bahasa Melayu 🇲🇾";
        }
    }

    public static String strNowBadge(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Now";
            case LANG_ID: return "Aktif";
            case LANG_MS: default: return "Kini";
        }
    }

    // --- Restart Dialog ---
    public static String strRestartTitle(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "🔄 Restart TV";
            case LANG_ID: return "🔄 Mulai Ulang TV";
            case LANG_MS: default: return "🔄 Mulakan Semula TV (Restart)";
        }
    }

    public static String strRestartMsg(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Are you sure you want to restart this TV?";
            case LANG_ID: return "Apakah Anda yakin ingin memulai ulang (restart) TV ini?";
            case LANG_MS: default: return "Adakah anda pasti ingin memulakan semula (restart) TV ini?";
        }
    }

    public static String strRestartConfirm(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Yes, Restart";
            case LANG_ID: return "Ya, Restart";
            case LANG_MS: default: return "Ya, Restart";
        }
    }

    public static String strRestartToast(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Restarting TV...";
            case LANG_ID: return "Memulai ulang TV...";
            case LANG_MS: default: return "Memulakan semula TV...";
        }
    }

    // --- Turn Off Dialog ---
    public static String strTurnOffTitle(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "🔌 Turn Off TV";
            case LANG_ID: return "🔌 Matikan TV";
            case LANG_MS: default: return "🔌 Padamkan TV (Turn Off)";
        }
    }

    public static String strTurnOffMsg(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Would you like to turn off or put the TV into sleep mode?";
            case LANG_ID: return "Apakah Anda ingin mematikan atau menidurkan TV?";
            case LANG_MS: default: return "Adakah anda ingin memadamkan / meletakkan TV dalam mod rehat?";
        }
    }

    public static String strTurnOffConfirm(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Turn Off";
            case LANG_ID: return "Matikan TV";
            case LANG_MS: default: return "Padam TV";
        }
    }

    // --- Clean Cache Dialog ---
    public static String strCleanCacheTitle(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "🧹 Clean TV Cache";
            case LANG_ID: return "🧹 Bersihkan Cache TV";
            case LANG_MS: default: return "🧹 Bersihkan Cache TV";
        }
    }

    public static String strCleanCacheMsg(Context ctx, String freeStr, String totalStr, int freePct, int usedPct) {
        switch (getLanguage(ctx)) {
            case LANG_EN:
                return "Clear app cache files, temporary download fragments, and refresh TV memory.\n\n"
                        + "📊 TV Storage Status:\n"
                        + "• Free Space: " + freeStr + " of " + totalStr + " (" + freePct + "% free)\n"
                        + "• Usage: " + usedPct + "%";
            case LANG_ID:
                return "Bersihkan file cache aplikasi, file unduhan sementara, dan segarkan memori TV.\n\n"
                        + "📊 Status Penyimpanan TV:\n"
                        + "• Ruang Bebas: " + freeStr + " dari " + totalStr + " (" + freePct + "% bebas)\n"
                        + "• Penggunaan: " + usedPct + "%";
            case LANG_MS:
            default:
                return "Kosongkan fail cache aplikasi, serpihan muat turun sementara, dan segarkan memori TV.\n\n"
                        + "📊 Status Storan TV:\n"
                        + "• Ruang Bebas: " + freeStr + " daripada " + totalStr + " (" + freePct + "% bebas)\n"
                        + "• Penggunaan: " + usedPct + "%";
        }
    }

    public static String strCleanCacheStart(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "🧹 Start Cleaning";
            case LANG_ID: return "🧹 Mulai Bersihkan";
            case LANG_MS: default: return "🧹 Mula Bersihkan";
        }
    }

    public static String strCleanCacheToast(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Cleaning TV cache...";
            case LANG_ID: return "Sedang membersihkan cache TV...";
            case LANG_MS: default: return "Sedang membersihkan cache TV...";
        }
    }

    public static String strCleanCacheDoneTitle(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "✨ Cleaning Complete";
            case LANG_ID: return "✨ Pembersihan Selesai";
            case LANG_MS: default: return "✨ Pembersihan Selesai";
        }
    }

    public static String strCleanCacheDoneMsg(Context ctx, String freedStr, boolean hasFreed) {
        switch (getLanguage(ctx)) {
            case LANG_EN:
                return hasFreed
                        ? "✅ Successfully cleaned " + freedStr + " of cache files!\nTV storage is now lighter and smoother."
                        : "✅ TV cache is already completely clean.\nTV storage and memory are at optimal levels.";
            case LANG_ID:
                return hasFreed
                        ? "✅ Berhasil membersihkan " + freedStr + " file cache!\nPenyimpanan TV kini lebih lega dan lancar."
                        : "✅ Cache TV sudah bersih sepenuhnya.\nPenyimpanan dan memori TV berada pada tingkat optimal.";
            case LANG_MS:
            default:
                return hasFreed
                        ? "✅ Berjaya membersihkan " + freedStr + " fail cache!\nStoran TV kini lebih ringan dan lancar."
                        : "✅ Cache TV telah dibersihkan sepenuhnya.\nStoran dan memori TV berada pada tahap optimum.";
        }
    }

    public static String strCleanCacheUsageLine(Context ctx, int before, int after) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "\n\n📊 Storage Usage: " + before + "% → " + after + "%";
            case LANG_ID: return "\n\n📊 Penggunaan Penyimpanan: " + before + "% → " + after + "%";
            case LANG_MS: default: return "\n\n📊 Penggunaan Storan: " + before + "% → " + after + "%";
        }
    }

    public static String strDone(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Done";
            case LANG_ID: return "Selesai";
            case LANG_MS: default: return "Selesai";
        }
    }

    // --- Azan Settings Dialog ---
    public static String strAzanSettingsHeaderTitle(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "🕌 Adhan Settings & Options";
            case LANG_ID: return "🕌 Pengaturan & Pilihan Adzan";
            case LANG_MS: default: return "🕌 Tetapan & Pilihan Azan";
        }
    }

    public static String strAzanSettingsHeaderSub(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Floating glass popup card over all apps & adhan audio settings";
            case LANG_ID: return "Notifikasi popup kartu kaca melayang di atas semua aplikasi & pengaturan audio";
            case LANG_MS: default: return "Notifikasi popup kad kaca terapung di atas semua aplikasi & tetapan audio azan";
        }
    }

    public static String strAzanSectionMaster(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "A. ADHAN POPUP MASTER SWITCH";
            case LANG_ID: return "A. STATUS UTAMA POPUP ADZAN";
            case LANG_MS: default: return "A. STATUS UTAMA POPUP AZAN";
        }
    }

    public static String strAzanMasterTitle(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "🔔 TV Adhan Popup Notification";
            case LANG_ID: return "🔔 Notifikasi Popup Adzan TV";
            case LANG_MS: default: return "🔔 Notifikasi Popup Azan TV";
        }
    }

    public static String strAzanMasterDesc(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Show floating prayer card over Netflix, YouTube & all apps";
            case LANG_ID: return "Tampilkan kartu waktu sholat melayang di atas Netflix, YouTube & semua app";
            case LANG_MS: default: return "Papar kad waktu solat terapung di atas Netflix, YouTube & semua app";
        }
    }

    public static String strAzanSectionPrayers(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "B. PRAYER TIME SELECTION (ON / OFF)";
            case LANG_ID: return "B. PILIHAN WAKTU SHOLAT (ON / OFF)";
            case LANG_MS: default: return "B. PILIHAN WAKTU SOLAT (ON / OFF)";
        }
    }

    public static String strAzanSectionAudio(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "C. ADHAN AUDIO SELECTION";
            case LANG_ID: return "C. PILIHAN SUARA ADZAN";
            case LANG_MS: default: return "C. PILIHAN BUNYI AZAN";
        }
    }

    public static String strAzanAudioCurrent(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "🔊 Current Adhan Audio";
            case LANG_ID: return "🔊 Suara Adzan Saat Ini";
            case LANG_MS: default: return "🔊 Bunyi Azan Semasa";
        }
    }

    public static String strAzanAudioChangeBtn(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "CHANGE ❯";
            case LANG_ID: return "UBAH ❯";
            case LANG_MS: default: return "TUKAR ❯";
        }
    }

    public static String strAzanSectionTest(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "D. TEST ADHAN POPUP";
            case LANG_ID: return "D. UJI POPUP ADZAN";
            case LANG_MS: default: return "D. UJI POPUP AZAN";
        }
    }

    public static String strAzanTestTitle(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "▶️ Test Adhan Popup Now";
            case LANG_ID: return "▶️ Uji Popup Adzan Sekarang";
            case LANG_MS: default: return "▶️ Uji Popup Azan Sekarang";
        }
    }

    public static String strAzanTestDesc(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Trigger floating card & play selected audio";
            case LANG_ID: return "Tampilkan kartu melayang & putar audio pilihan";
            case LANG_MS: default: return "Mencetuskan kad terapung & main audio pilihan";
        }
    }

    public static String strAzanTestBtn(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "TEST ▶";
            case LANG_ID: return "UJI ▶";
            case LANG_MS: default: return "UJI ▶";
        }
    }

    public static String strAzanInfoText(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "💡 Prayer times are automatically synchronized based on official schedules. The adhan card appears over Netflix, YouTube & streaming apps.";
            case LANG_ID: return "💡 Waktu sholat disinkronkan secara otomatis sesuai jadwal resmi. Kartu adzan tetap muncul di atas Netflix, YouTube & aplikasi streaming.";
            case LANG_MS: default: return "💡 Waktu solat diselaraskan secara automatik mengikut zon JAKIM. Kad azan tetap muncul di atas Netflix, YouTube & Perfect TV.";
        }
    }

    public static String strAzanCloseBtn(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "✕ Close Settings";
            case LANG_ID: return "✕ Tutup Pengaturan";
            case LANG_MS: default: return "✕ Tutup Tetapan";
        }
    }

    public static String strAzanChooseAudioTitle(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "🔊 Choose Adhan Audio";
            case LANG_ID: return "🔊 Pilih Suara Adzan";
            case LANG_MS: default: return "🔊 Pilih Audio Azan";
        }
    }

    public static String[] strAzanSoundOptions(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN:
                return new String[]{
                        "🕋 Masjid al-Haram Makkah Adhan (Full)",
                        "🕌 Al-Masjid an-Nabawi Madinah Adhan (Full)",
                        "🇲🇾 KLCC As-Syakirin Mosque Adhan",
                        "🇸🇬 Singapore Sultan Mosque / Regional Adhan",
                        "🇮🇩 Indonesia National / Istiqlal Mosque Adzan",
                        "🔔 Soft Chime Tone (Brief ~5s)",
                        "🔕 Silent Mode (Visual Only, No Audio)"
                };
            case LANG_ID:
                return new String[]{
                        "🕋 Adzan Masjidil Haram Makkah (Penuh)",
                        "🕌 Adzan Masjid Nabawi Madinah (Penuh)",
                        "🇲🇾 Adzan Masjid As-Syakirin KLCC",
                        "🇸🇬 Adzan Singapura (Masjid Sultan)",
                        "🇮🇩 Adzan Indonesia (Masjid Istiqlal / Nasional)",
                        "🔔 Nada Chime Lembut (Singkat ~5 detik)",
                        "🔕 Mode Hening (Hanya Visual Tanpa Suara)"
                };
            case LANG_MS:
            default:
                return new String[]{
                        "🕋 Azan Masjidil Haram Makkah (Penuh)",
                        "🕌 Azan Masjid Nabawi Madinah (Penuh)",
                        "🇲🇾 Azan Masjid As-Syakirin KLCC",
                        "🇸🇬 Azan Singapura (Masjid Sultan / Jiharkah)",
                        "🇮🇩 Adzan Indonesia (Masjid Istiqlal / Nasional)",
                        "🔔 Nada Chime Lembut (Ringkas ~5 saat)",
                        "🔕 Mod Senyap (Visual Sahaja Tanpa Audio)"
                };
        }
    }

    // --- Wallpaper Dialog ---
    public static String strWallpaperDialogTitle(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "🖼️ Select 4K Wallpaper Collection";
            case LANG_ID: return "🖼️ Pilih Koleksi Wallpaper 4K";
            case LANG_MS: default: return "🖼️ Pilih Koleksi Wallpaper 4K";
        }
    }

    public static String strWallpaperDialogSub(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Pexels & Unsplash CDN • Ultra HD 3840×2160 Resolution";
            case LANG_ID: return "Pexels & Unsplash CDN • Resolusi Ultra HD 3840×2160";
            case LANG_MS: default: return "Pexels & Unsplash CDN • Resolusi Ultra HD 3840×2160";
        }
    }

    public static String strWallpaperIntervalTitle(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "⏱️ Wallpaper Shuffle Interval";
            case LANG_ID: return "⏱️ Interval Shuffle Wallpaper";
            case LANG_MS: default: return "⏱️ Selang Masa Shuffle Wallpaper";
        }
    }

    public static String strWallpaperIntervalSub(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Select how often wallpaper changes automatically";
            case LANG_ID: return "Pilih frekuensi wallpaper berganti otomatis";
            case LANG_MS: default: return "Pilih kekerapan wallpaper bertukar secara automatik";
        }
    }

    public static String strWallpaperIntervalText(Context ctx, int curInterval) {
        String lang = getLanguage(ctx);
        if (curInterval == 1) {
            return LANG_EN.equals(lang) ? "Every 1 Minute" : (LANG_ID.equals(lang) ? "Setiap 1 Menit" : "Setiap 1 Minit");
        } else if (curInterval == 5) {
            return LANG_EN.equals(lang) ? "Every 5 Minutes (Recommended)" : (LANG_ID.equals(lang) ? "Setiap 5 Menit (Disarankan)" : "Setiap 5 Minit (Disyorkan)");
        } else if (curInterval == 10) {
            return LANG_EN.equals(lang) ? "Every 10 Minutes (Recommended)" : (LANG_ID.equals(lang) ? "Setiap 10 Menit (Disarankan)" : "Setiap 10 Minit (Disyorkan)");
        } else if (curInterval == 15) {
            return LANG_EN.equals(lang) ? "Every 15 Minutes" : (LANG_ID.equals(lang) ? "Setiap 15 Menit" : "Setiap 15 Minit");
        } else if (curInterval == 30) {
            return LANG_EN.equals(lang) ? "Every 30 Minutes" : (LANG_ID.equals(lang) ? "Setiap 30 Menit" : "Setiap 30 Minit");
        } else if (curInterval == 60) {
            return LANG_EN.equals(lang) ? "Every 1 Hour" : (LANG_ID.equals(lang) ? "Setiap 1 Jam" : "Setiap 1 Jam");
        } else if (curInterval == 0) {
            return LANG_EN.equals(lang) ? "Manual Only (Off)" : (LANG_ID.equals(lang) ? "Manual Saja (Mati)" : "Manual Sahaja (Mati)");
        } else {
            return LANG_EN.equals(lang) ? "Every " + curInterval + " Minutes" : (LANG_ID.equals(lang) ? "Setiap " + curInterval + " Menit" : "Setiap " + curInterval + " Minit");
        }
    }

    // --- Azan Popup Card Strings ---
    public static String strAzanPopupTitle(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "ADHAN";
            case LANG_ID: return "ADZAN";
            case LANG_MS: default: return "AZAN";
        }
    }

    public static String strAzanPopupHint(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Press Back / OK to dismiss";
            case LANG_ID: return "Tekan Kembali / OK untuk menutup";
            case LANG_MS: default: return "Tekan Back / OK untuk tutup";
        }
    }

    public static String strAzanPopupSilentHint(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Silent Mode • Press Back to dismiss";
            case LANG_ID: return "Mode Hening • Tekan Kembali untuk menutup";
            case LANG_MS: default: return "Mod Senyap • Tekan Back untuk tutup";
        }
    }

    public static String strAzanPopupChimeHint(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Short Chime Tone • Press Back to dismiss";
            case LANG_ID: return "Nada Dering Singkat • Tekan Kembali untuk menutup";
            case LANG_MS: default: return "Nada Peringatan Ringkas • Tekan Back untuk tutup";
        }
    }

    public static String strAzanPopupDownloadingHint(Context ctx, String soundLabel) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Downloading audio " + soundLabel + "...";
            case LANG_ID: return "Mengunduh audio " + soundLabel + "...";
            case LANG_MS: default: return "Memuat turun audio " + soundLabel + "...";
        }
    }

    // --- Screensaver Hint ---
    public static String strScreensaverHint(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Press any button to return";
            case LANG_ID: return "Tekan tombol apa saja untuk kembali";
            case LANG_MS: default: return "Tekan mana-mana butang untuk kembali";
        }
    }

    // --- Who's Watching Profile Selection Screen ---
    public static String strWhosWatchingTitle(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Who's watching?";
            case LANG_ID: return "Siapa yang menonton?";
            case LANG_MS: default: return "Siapa yang menonton?";
        }
    }

    public static String strWhosWatchingSub(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Select a profile to continue";
            case LANG_ID: return "Pilih profil untuk melanjutkan";
            case LANG_MS: default: return "Pilih profil untuk teruskan";
        }
    }

    public static String strProfileRolePrimary(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "PRIMARY";
            case LANG_ID: return "UTAMA";
            case LANG_MS: default: return "UTAMA";
        }
    }

    public static String strProfileRoleChild(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "KIDS";
            case LANG_ID: return "ANAK-ANAK";
            case LANG_MS: default: return "KANAK-KANAK";
        }
    }

    public static String strAddProfile(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Add Profile";
            case LANG_ID: return "Tambah Profil";
            case LANG_MS: default: return "Tambah Profil";
        }
    }

    public static String strWhosWatchingHint(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Press & hold to manage profile";
            case LANG_ID: return "Tekan & tahan untuk kelola profil";
            case LANG_MS: default: return "Tekan & tahan untuk urus profil";
        }
    }

    // --- Profile Settings & Management Tab ---
    public static String strActiveProfileAvatarHeader(Context ctx, String name) {
        String n = (name != null) ? name.toUpperCase(Locale.ROOT) : "";
        switch (getLanguage(ctx)) {
            case LANG_EN: return "CURRENT PROFILE AVATAR (" + n + ")";
            case LANG_ID: return "AVATAR PROFIL SAAT INI (" + n + ")";
            case LANG_MS: default: return "AVATAR PROFIL SEMASA (" + n + ")";
        }
    }

    public static String strAvatarCustomizeTitle(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "🎨 Avatar Customization (3 Options)";
            case LANG_ID: return "🎨 Kustomisasi Avatar (3 Pilihan)";
            case LANG_MS: default: return "🎨 Kustomisasi Avatar (3 Pilihan)";
        }
    }

    public static String strAvatarCustomizeSub(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Choose Single Character, Search GIF (Giphy), or Upload from Phone";
            case LANG_ID: return "Pilih Karakter Tunggal, Cari GIF (Giphy), atau Unggah dari Ponsel";
            case LANG_MS: default: return "Pilih Watak Tunggal, Cari GIF (Giphy), atau Muat Naik dari Telefon";
        }
    }

    public static String strUserListHeader(Context ctx, int count) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "USER LIST (" + count + ")";
            case LANG_ID: return "DAFTAR PENGGUNA (" + count + ")";
            case LANG_MS: default: return "SENARAI PENGGUNA (" + count + ")";
        }
    }

    public static String strActiveStatus(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Active ✓";
            case LANG_ID: return "Aktif ✓";
            case LANG_MS: default: return "Aktif ✓";
        }
    }

    public static String strDeleteUser(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "🗑️ Delete";
            case LANG_ID: return "🗑️ Hapus";
            case LANG_MS: default: return "🗑️ Padam";
        }
    }

    public static String strAddNewUser(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "➕ Add New User";
            case LANG_ID: return "➕ Tambah Pengguna Baru";
            case LANG_MS: default: return "➕ Tambah Pengguna Baharu";
        }
    }

    public static String strAddNewUserDesc(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Create a new profile for family members or kids";
            case LANG_ID: return "Buat profil baru untuk anggota keluarga atau anak";
            case LANG_MS: default: return "Cipta profil baharu untuk ahli keluarga atau anak";
        }
    }

    public static String strManagePhotosTitle(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "📸 Manage Photos & Phone Upload (QR)";
            case LANG_ID: return "📸 Kelola Foto & Unggah Ponsel (QR)";
            case LANG_MS: default: return "📸 Urus Gambar & Muat Naik Telefon (QR)";
        }
    }

    public static String strManagePhotosSub(Context ctx) {
        switch (getLanguage(ctx)) {
            case LANG_EN: return "Scan QR code to send photos directly from phone to TV";
            case LANG_ID: return "Pindai kode QR untuk kirim foto dari ponsel langsung ke TV";
            case LANG_MS: default: return "Imbas kod QR untuk hantar gambar dari telefon terus ke TV";
        }
    }
}
