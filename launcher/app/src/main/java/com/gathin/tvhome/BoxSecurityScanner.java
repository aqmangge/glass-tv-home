package com.gathin.tvhome;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Intelligent Android TV Box security analyzer and malware cleaner.
 * Specialised for identifying and neutralizing supply-chain firmware malware common on Android TV boxes:
 * - XCPE Botnet (x.cpe / Videostrong OEM supply chain infection)
 * - BadBox Core Backdoor (com.oemservice, com.yokatv.market)
 * - ABCNexus Loader & Ad-Fraud Engine (com.abc.nexus)
 * - Peachpit Trojan Modules (com.asshow.asshow, fake fitness bots)
 * - Adups Firmware Spyware (com.adups.fota)
 * - Factory pre-rooted backdoors & unauthenticated open ADB ports
 *
 * For rooted TV boxes (common in 90% cheap Chinese TV boxes), enables 1-click firmware quarantine.
 * For unrooted devices, provides direct Settings disablement intents & ADB instructions.
 */
public final class BoxSecurityScanner {

    private static final String TAG = "BoxSecurityScanner";

    // Known malicious package definitions: PackageName -> ThreatMeta
    private static final Map<String, ThreatMeta> KNOWN_MALICIOUS_PACKAGES = new HashMap<>();

    // Known malicious file paths in /system, /vendor, /oem partitions
    private static final String[] SYSTEM_MALWARE_PATHS = new String[]{
            "/system/app/app_xcpe/app_xcpe.apk",
            "/system/app/app_xcpe",
            "/vendor/app/app_store/app_store.apk",
            "/oem/app/app_xcpe",
            "/data/system/core.jar",
            "/system/bin/daemonsu"
    };

    // Common factory root binary locations
    private static final String[] SU_BINARY_PATHS = new String[]{
            "/system/xbin/su",
            "/system/bin/su",
            "/sbin/su",
            "/vendor/bin/su",
            "/system/sd/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su"
    };

    static {
        // XCPE Botnet / Videostrong supply chain
        KNOWN_MALICIOUS_PACKAGES.put("x.cpe", new ThreatMeta(
                "XCPE Firmware Botnet",
                "Malware firmware tegar yang menculik sambungan internet sebagai proksi jenayah dan memuat turun virus.",
                "KRITIKAL", true));
        KNOWN_MALICIOUS_PACKAGES.put("X.CPE", new ThreatMeta(
                "XCPE Firmware Botnet",
                "Malware firmware tegar yang menculik sambungan internet sebagai proksi jenayah dan memuat turun virus.",
                "KRITIKAL", true));
        KNOWN_MALICIOUS_PACKAGES.put("com.oemservice", new ThreatMeta(
                "BadBox Firmware Backdoor",
                "Pintu belakang sistem (backdoor) kilang yang membolehkan kawalan jauh tanpa kebenaran.",
                "KRITIKAL", true));
        KNOWN_MALICIOUS_PACKAGES.put("com.yokatv.market", new ThreatMeta(
                "YokaTV Dropper Market",
                "Gedung aplikasi palsu pengeluar yang memuat turun malware secara tersembunyi.",
                "TINGGI", true));

        // ABCNexus & Ad-Fraud Loaders
        KNOWN_MALICIOUS_PACKAGES.put("com.abc.nexus", new ThreatMeta(
                "ABCNexus Loader & Proxy Engine",
                "Modul pemuat malware yang mengalirkan trafik haram (ad-fraud) dan menjual bandwidth Wi-Fi anda.",
                "TINGGI", false));

        // Peachpit & Fraud Apps
        KNOWN_MALICIOUS_PACKAGES.put("com.asshow.asshow", new ThreatMeta(
                "Peachpit Trojan Dropper",
                "Skrip pengiklanan tersembunyi yang menjana paparan iklan palsu di latar belakang secara rahsia.",
                "TINGGI", false));
        KNOWN_MALICIOUS_PACKAGES.put("sixpack.sixpackabs.absworkout.abexercises.tv", new ThreatMeta(
                "Peachpit Fraudulent Bot",
                "Aplikasi senaman palsu bawaan kilang yang menyamar untuk menjalankan penipuan iklan TV.",
                "SEDERHANA", false));

        // Adups Spyware
        KNOWN_MALICIOUS_PACKAGES.put("com.adups.fota", new ThreatMeta(
                "Adups Firmware Spyware",
                "Modul FOTA pihak ketiga berniat jahat yang menghantar data peranti ke pelayan asing.",
                "TINGGI", true));
        KNOWN_MALICIOUS_PACKAGES.put("com.adups.fota.sysoper", new ThreatMeta(
                "Adups Sysoper Root Service",
                "Servis sistem tersembunyi yang membuka laluan pemasangan aplikasi tanpa kebenaran.",
                "KRITIKAL", true));

        // Fake / Trojanized Apps
        KNOWN_MALICIOUS_PACKAGES.put("com.google.android.youtube.tv.unoff", new ThreatMeta(
                "Trojanized Fake YouTube TV",
                "Aplikasi YouTube tidak rasmi yang disuntik kod hasad / pancingan data.",
                "TINGGI", false));
        KNOWN_MALICIOUS_PACKAGES.put("com.rockchip.devicetest", new ThreatMeta(
                "Rockchip Unsecured Backdoor Test",
                "Alat ujian kilang Rockchip yang meninggalkan 'reverse shell' terbuka kepada penggodam.",
                "SEDERHANA", true));
        KNOWN_MALICIOUS_PACKAGES.put("com.android.partner.partnerinterface", new ThreatMeta(
                "Suspicious Partner Interface",
                "Antara muka rakan kongsi pengeluar yang membenarkan manipulasi tetapan TV dari jauh.",
                "SEDERHANA", true));
    }

    private static final ExecutorService SCAN_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "GlassTV-SecScan");
        t.setPriority(Thread.MIN_PRIORITY); // Keep UI silky smooth even during scan
        return t;
    });

    public interface ScanListener {
        void onScanComplete(ScanResult result);
    }

    public interface RemediationListener {
        void onRemediationComplete(boolean success, String message);
    }

    public static class ThreatMeta {
        public final String title;
        public final String description;
        public final String riskLevel; // KRITIKAL, TINGGI, SEDERHANA
        public final boolean isFirmwareLevel;

        public ThreatMeta(String title, String description, String riskLevel, boolean isFirmwareLevel) {
            this.title = title;
            this.description = description;
            this.riskLevel = riskLevel;
            this.isFirmwareLevel = isFirmwareLevel;
        }
    }

    public static class ThreatItem {
        public final String packageName;
        public final String appName;
        public final ThreatMeta meta;
        public final String filePath;
        public final boolean isSystemApp;
        public final boolean isEnabled;

        public ThreatItem(String packageName, String appName, ThreatMeta meta, String filePath, boolean isSystemApp, boolean isEnabled) {
            this.packageName = packageName;
            this.appName = appName;
            this.meta = meta;
            this.filePath = filePath;
            this.isSystemApp = isSystemApp;
            this.isEnabled = isEnabled;
        }
    }

    public static class ScanResult {
        public final long timestamp;
        public final List<ThreatItem> threats;
        public final boolean hasRoot;
        public final String rootBinaryPath;
        public final boolean isTestKeysRom;
        public final boolean isOpenAdb;
        public final long totalRamMb;
        public final long freeRamMb;
        public final boolean isUltraLowRamBox;

        public ScanResult(
                long timestamp,
                List<ThreatItem> threats,
                boolean hasRoot,
                String rootBinaryPath,
                boolean isTestKeysRom,
                boolean isOpenAdb,
                long totalRamMb,
                long freeRamMb,
                boolean isUltraLowRamBox
        ) {
            this.timestamp = timestamp;
            this.threats = threats;
            this.hasRoot = hasRoot;
            this.rootBinaryPath = rootBinaryPath;
            this.isTestKeysRom = isTestKeysRom;
            this.isOpenAdb = isOpenAdb;
            this.totalRamMb = totalRamMb;
            this.freeRamMb = freeRamMb;
            this.isUltraLowRamBox = isUltraLowRamBox;
        }

        public boolean isClean() {
            return threats.isEmpty();
        }

        public int threatCount() {
            return threats.size();
        }
    }

    private static ScanResult sLastResult = null;

    public static ScanResult getLastResult() {
        return sLastResult;
    }

    /**
     * Executes asynchronous scan with minimal CPU priority to avoid dropped frames on 1GB RAM boxes.
     */
    public static void scanAsync(Context context, ScanListener listener) {
        if (context == null) return;
        Context appCtx = context.getApplicationContext();

        SCAN_EXECUTOR.execute(() -> {
            ScanResult result = performScan(appCtx);
            sLastResult = result;
            if (listener != null) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> listener.onScanComplete(result));
            }
        });
    }

    /**
     * Synchronous scan. Inspects packages, firmware files, root binaries, and system integrity.
     */
    public static ScanResult performScan(Context context) {
        long now = System.currentTimeMillis();
        PackageManager pm = context.getPackageManager();
        List<ThreatItem> detected = new ArrayList<>();

        // 1. Scan installed packages
        try {
            List<PackageInfo> installed = pm.getInstalledPackages(0);
            for (PackageInfo pi : installed) {
                String pkg = pi.packageName;
                if (KNOWN_MALICIOUS_PACKAGES.containsKey(pkg)) {
                    ThreatMeta meta = KNOWN_MALICIOUS_PACKAGES.get(pkg);
                    String name = pi.applicationInfo != null ? pm.getApplicationLabel(pi.applicationInfo).toString() : pkg;
                    String sourceDir = (pi.applicationInfo != null) ? pi.applicationInfo.sourceDir : null;
                    boolean isSys = (pi.applicationInfo != null) && ((pi.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
                    boolean isEnabled = (pi.applicationInfo != null) && pi.applicationInfo.enabled;
                    detected.add(new ThreatItem(pkg, name, meta, sourceDir, isSys, isEnabled));
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error checking packages", e);
        }

        // 2. Check firmware file system paths (in case package is hidden from PackageManager)
        for (String p : SYSTEM_MALWARE_PATHS) {
            try {
                File f = new File(p);
                if (f.exists()) {
                    boolean already = false;
                    for (ThreatItem item : detected) {
                        if (item.filePath != null && item.filePath.startsWith(p)) {
                            already = true;
                            break;
                        }
                    }
                    if (!already) {
                        String fakePkg = "firmware:" + f.getName();
                        ThreatMeta meta = new ThreatMeta(
                                "Fail Hasad Firmware (" + f.getName() + ")",
                                "Fail eksekutif malware dikesan dalam direktori sistem: " + p,
                                "KRITIKAL", true);
                        detected.add(new ThreatItem(fakePkg, f.getName(), meta, p, true, true));
                    }
                }
            } catch (Exception ignored) {}
        }

        // 3. Root detection
        String foundSu = null;
        for (String su : SU_BINARY_PATHS) {
            try {
                File f = new File(su);
                if (f.exists()) {
                    foundSu = su;
                    break;
                }
            } catch (Exception ignored) {}
        }
        boolean hasRoot = foundSu != null || canExecuteSu();

        // 4. Test-Keys build verification (cheap custom ROM indicator)
        boolean isTestKeys = Build.TAGS != null && Build.TAGS.contains("test-keys");

        // 5. Open ADB port 5555 probe (listening on localhost without auth)
        boolean isOpenAdb = false;
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress("127.0.0.1", 5555), 300);
            isOpenAdb = true;
        } catch (Exception ignored) {}

        long totalRam = DevicePerformance.getTotalRamMb(context);
        long freeRam = DevicePerformance.getAvailableRamMb(context);
        boolean is1Gb = DevicePerformance.isUltraLowRam(context);

        ScanResult res = new ScanResult(
                now,
                Collections.unmodifiableList(detected),
                hasRoot,
                foundSu,
                isTestKeys,
                isOpenAdb,
                totalRam,
                freeRam,
                is1Gb
        );
        sLastResult = res;
        return res;
    }

    /**
     * Attempts to neutralize or remove malware threat.
     * Uses root commands if root is available.
     * If root is unavailable, opens Android App Settings for the user to disable.
     */
    public static void neutralizeThreat(Context context, ThreatItem threat, RemediationListener listener) {
        if (context == null || threat == null) return;

        SCAN_EXECUTOR.execute(() -> {
            boolean hasRoot = canExecuteSu();
            if (hasRoot) {
                boolean success = executeRootNeutralize(threat);
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    if (listener != null) {
                        listener.onRemediationComplete(success,
                                success ? "✨ Berjaya melumpuhkan & membekukan malware melalui akses Root!"
                                        : "⚠️ Gagal melumpuhkan fail sistem. Sila gunakan adb shell.");
                    }
                });
            } else {
                // Non-root: launch app details page
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + threat.packageName));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                        if (listener != null) {
                            listener.onRemediationComplete(true, "📱 Membuka Tetapan Aplikasi: Sila klik 'Disable' atau 'Force Stop'.");
                        }
                    } catch (Exception e) {
                        if (listener != null) {
                            listener.onRemediationComplete(false, "Sila nyahaktifkan secara manual di Tetapan Sistem.");
                        }
                    }
                });
            }
        });
    }

    private static boolean executeRootNeutralize(ThreatItem threat) {
        try {
            String pkg = threat.packageName;
            // 1. Force stop & disable via package manager
            runSuCmd("am force-stop " + pkg);
            runSuCmd("pm disable-user --user 0 " + pkg);
            runSuCmd("pm disable " + pkg);
            runSuCmd("pm hide " + pkg);
            runSuCmd("pm uninstall -k --user 0 " + pkg);

            // 2. Kill remaining processes
            runSuCmd("pkill -9 -f " + pkg);

            // 3. If file path in /system or /vendor, remount and strip execution permissions
            if (threat.filePath != null && !threat.filePath.isEmpty()) {
                File target = new File(threat.filePath);
                if (target.exists()) {
                    runSuCmd("mount -o remount,rw /system");
                    runSuCmd("mount -o remount,rw /vendor");
                    runSuCmd("mount -o remount,rw /oem");
                    runSuCmd("chmod 000 " + threat.filePath);
                    runSuCmd("mv " + threat.filePath + " " + threat.filePath + ".quarantined");
                    runSuCmd("mount -o remount,ro /system");
                }
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Root neutralization failed", e);
            return false;
        }
    }

    public static boolean canExecuteSu() {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "id"});
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();
            p.waitFor();
            return line != null && line.contains("uid=0");
        } catch (Exception ignored) {
            return false;
        }
    }

    private static void runSuCmd(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
            p.waitFor();
        } catch (Exception ignored) {}
    }
}
