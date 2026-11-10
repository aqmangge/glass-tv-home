package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.StatFs;
import android.util.Base64;
import android.util.Log;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Empties the TV's app caches every few weeks, from the TV itself. The Play Store leaves gigabytes of
 * download fragments behind; on a TV with 11 GB that filled storage until YouTube opened to a blank
 * screen. Android lets no installed app clear other apps' caches, so this asks the TV's own debugging
 * service, the one the setup used over Wi-Fi, to run {@code pm trim-caches}. The first time, the TV
 * shows "Allow USB debugging?" once.
 *
 * Built so it cannot hurt the TV: it only ever sends that one command, which can reach nothing but
 * cache folders (no app data, sign-ins or settings); it runs only while Home is idle on screen; and
 * when debugging is off or the prompt is declined it gives up quietly, tries at most once a day, and
 * stops asking after a few declines.
 */
final class CacheCleaner {
    private static final String TAG = "TvHome.Clean";
    static final int DEFAULT_DAYS = 15;
    /** How long Home must sit on screen before it cleans, so it never runs while someone is busy. */
    static final long IDLE_MS = 20_000;
    /** The one command this ever sends. */
    private static final String COMMAND = "shell:pm trim-caches 999999999999";
    private static final long DAY_MS = 24L * 60 * 60 * 1000;
    /** Storage this full cleans early: apps start failing well before Android itself steps in. */
    private static final int FULL_PERCENT = 80;
    private static final long FULL_MIN_GAP_MS = 3 * DAY_MS;
    private static final int MAX_DECLINES = 3;
    /**
     * The TV forgets an "Always allow" key that has not connected for 7 days, and would ask again at
     * every clean. Connecting this often, running nothing, keeps the key remembered.
     */
    private static final long CHECK_IN_MS = 3 * DAY_MS;
    private static final int PORT = 5555;
    private static volatile boolean running;

    public interface CleanCallback {
        void onCleanFinished(long bytesFreed, int percentBefore, int percentAfter, boolean systemCachesTrimmed);
    }

    private CacheCleaner() {
    }

    public static long getFreeStorageBytes() {
        try {
            StatFs s = new StatFs(Environment.getDataDirectory().getPath());
            return s.getAvailableBytes();
        } catch (Exception e) {
            return 0;
        }
    }

    public static long getTotalStorageBytes() {
        try {
            StatFs s = new StatFs(Environment.getDataDirectory().getPath());
            return s.getTotalBytes();
        } catch (Exception e) {
            return 0;
        }
    }

    public static long deleteDirContents(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return 0;
        long bytes = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    bytes += deleteDirContents(f);
                    f.delete();
                } else {
                    bytes += f.length();
                    f.delete();
                }
            }
        }
        return bytes;
    }

    /** Manually triggers an immediate deep cache clean from Settings. */
    public static void cleanNow(Context ctx, CleanCallback callback) {
        if (running) {
            if (callback != null) {
                callback.onCleanFinished(0, usedPercent(), usedPercent(), false);
            }
            return;
        }
        running = true;
        final Context app = ctx.getApplicationContext();
        new Thread(() -> {
            long bytesFreed = 0;
            int beforePercent = usedPercent();
            long freeBefore = getFreeStorageBytes();
            boolean systemTrimmed = false;
            try {
                // 1. Delete app internal and external cache
                bytesFreed += deleteDirContents(app.getCacheDir());
                bytesFreed += deleteDirContents(app.getExternalCacheDir());

                // 2. Clear any lingering .tmp downloads
                File onlineDir = OnlineWallpapers.dir(app);
                if (onlineDir != null && onlineDir.exists()) {
                    File[] tmps = onlineDir.listFiles((d, name) -> name.endsWith(".tmp"));
                    if (tmps != null) {
                        for (File f : tmps) {
                            bytesFreed += f.length();
                            f.delete();
                        }
                    }
                }

                // 3. Run system trim-caches via local ADB
                try {
                    systemTrimmed = shell(app, false, COMMAND);
                    if (!systemTrimmed) systemTrimmed = shell(app, true, COMMAND);
                    if (systemTrimmed) {
                        long now = System.currentTimeMillis();
                        prefs(app).edit().putLong("cleaned", now).putLong("seen", now).putInt("declined", 0).apply();
                        Log.i(TAG, "system trim-caches succeeded via adb shell");
                    }
                } catch (Exception e) {
                    Log.i(TAG, "system trim-caches skipped/failed: " + e);
                }

                // 4. Force JVM garbage collection
                System.gc();

                long freeAfter = getFreeStorageBytes();
                int afterPercent = usedPercent();
                if (freeAfter > freeBefore) {
                    bytesFreed = Math.max(bytesFreed, freeAfter - freeBefore);
                }

                if (callback != null) {
                    long finalFreed = bytesFreed;
                    boolean finalTrimmed = systemTrimmed;
                    android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                    mainHandler.post(() -> callback.onCleanFinished(finalFreed, beforePercent, afterPercent, finalTrimmed));
                }
            } finally {
                running = false;
            }
        }, "cache-clean-now").start();
    }

    /** Cleans in the background when it is due; {@code days} 0 means never. */
    static void maybeRun(Context ctx, int days) {
        if (running) return;
        SharedPreferences p = prefs(ctx);
        long now = System.currentTimeMillis();
        int used = usedPercent();
        // A "clean-now" file pushed next to the theme asks for a clean at once, and asks again even
        // after a declined prompt: the setup uses it while someone is at the TV to answer.
        File trigger = Config.dir(ctx) != null ? new File(Config.dir(ctx), "clean-now") : null;
        if (trigger != null && trigger.delete()) {
            p.edit().putInt("declined", 0).apply();
            // "new-key" beside it makes a fresh key, so the TV asks again: for testing the prompt.
            if (new File(Config.dir(ctx), "new-key").delete()) {
                new File(ctx.getFilesDir(), "adbkey").delete();
                new File(ctx.getFilesDir(), "adbkey.pub").delete();
            }
        } else {
            if (days <= 0 || p.getInt("declined", 0) >= MAX_DECLINES) return;
            if (now - p.getLong("tried", 0) < DAY_MS) return;
            long since = now - p.getLong("cleaned", 0);
            if (since < days * DAY_MS && !(used >= FULL_PERCENT && since >= FULL_MIN_GAP_MS)) {
                if (p.getLong("cleaned", 0) > 0 && now - p.getLong("seen", 0) >= CHECK_IN_MS) checkIn(ctx);
                return;
            }
        }
        running = true;
        p.edit().putLong("tried", now).apply();
        final Context app = ctx.getApplicationContext();
        new Thread(() -> {
            try {
                clean(app, used);
            } finally {
                running = false;
            }
        }, "cache-clean").start();
    }

    /** Connects and disconnects, running nothing, so the TV keeps this app's key. Never asks. */
    private static void checkIn(Context ctx) {
        running = true;
        prefs(ctx).edit().putLong("tried", System.currentTimeMillis()).apply();
        final Context app = ctx.getApplicationContext();
        new Thread(() -> {
            try {
                if (shell(app, false, null)) prefs(app).edit().putLong("seen", System.currentTimeMillis()).apply();
            } catch (Exception e) {
                Log.i(TAG, "check-in failed: " + e);
            } finally {
                running = false;
            }
        }, "cache-clean").start();
    }

    /**
     * Restart ({@code reboot}) or standby of the box, asked for from the Settings menu. Uses the same
     * debugging connection as the cleaning, with one of two fixed commands. Runs off the main thread;
     * {@code onFail} runs on the main thread when the box could not be asked (debugging off, declined).
     */
    static void power(Context ctx, boolean reboot, Runnable onFail) {
        final Context app = ctx.getApplicationContext();
        final String cmd = reboot ? "shell:svc power reboot" : "shell:input keyevent KEYCODE_SLEEP";
        final android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
        new Thread(() -> {
            boolean ok = false;
            try {
                ok = shell(app, true, cmd) || shell(app, false, cmd);
            } catch (IOException e) {
                // A reboot drops the connection as it starts: that is success, not failure.
                ok = reboot && e.getMessage() != null && !e.getMessage().contains("refused");
                Log.i(TAG, "power " + (reboot ? "reboot" : "sleep") + ": " + e);
            } catch (Exception e) {
                Log.w(TAG, "power command failed: " + e);
            }
            Log.i(TAG, "power " + (reboot ? "reboot" : "sleep") + (ok ? " sent" : " not possible"));
            if (!ok && onFail != null) main.post(onFail);
        }, "tv-power").start();
    }

    private static void clean(Context ctx, int before) {
        SharedPreferences p = prefs(ctx);
        try {
            // Once the prompt is answered the TV saves the key, but the connection that asked is not
            // always let through: a fresh one, with no second prompt, finds the key allowed.
            if (shell(ctx, true, COMMAND) || shell(ctx, false, COMMAND)) {
                long now = System.currentTimeMillis();
                p.edit().putLong("cleaned", now).putLong("seen", now).putInt("declined", 0).apply();
                Log.i(TAG, "cleared cached files, storage " + before + "% -> " + usedPercent() + "% used");
            } else {
                p.edit().putInt("declined", p.getInt("declined", 0) + 1).apply();
                Log.i(TAG, "not allowed to clean: the debugging prompt was declined or not answered");
            }
        } catch (IOException e) {
            // Debugging is off or the TV is busy: nothing happens, and it tries again tomorrow.
            Log.i(TAG, "cannot reach the TV's debugging service: " + e);
        } catch (Exception e) {
            Log.w(TAG, "cache clean failed: " + e);
        }
    }

    static int usedPercent() {
        try {
            StatFs s = new StatFs(Environment.getDataDirectory().getPath());
            long total = s.getTotalBytes();
            return total > 0 ? (int) ((total - s.getAvailableBytes()) * 100 / total) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences("cache_clean", Context.MODE_PRIVATE);
    }

    // ---- the TV's debugging service, spoken to just enough to run one command ------------------

    private static final int CNXN = 0x4e584e43, AUTH = 0x48545541, OPEN = 0x4e45504f,
            OKAY = 0x59414b4f, CLSE = 0x45534c43, WRTE = 0x45545257;
    private static final int AUTH_TOKEN = 1, AUTH_SIGNATURE = 2, AUTH_PUBLIC_KEY = 3;
    /** PKCS#1 DigestInfo for SHA-1: the service signs its 20-byte token as if it were a SHA-1 digest. */
    private static final byte[] SHA1_PREFIX = {0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2b, 0x0e, 0x03, 0x02,
            0x1a, 0x05, 0x00, 0x04, 0x14};

    /**
     * Runs {@code command} ({@link #COMMAND}, or null to only connect). False when the TV did not let
     * this app in; IOException when unreachable. {@code mayAsk} false never shows the prompt.
     */
    private static boolean shell(Context ctx, boolean mayAsk, String command) throws Exception {
        KeyPair keys = keys(ctx);
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", PORT), 3000);
            socket.setSoTimeout(10_000);
            DataInputStream in = new DataInputStream(socket.getInputStream());
            OutputStream out = socket.getOutputStream();
            send(out, CNXN, 0x01000000, 4096, "host::\0".getBytes(StandardCharsets.US_ASCII));
            Message m = read(in);
            if (m.cmd == AUTH && m.arg0 == AUTH_TOKEN) {
                send(out, AUTH, AUTH_SIGNATURE, 0, sign(keys.getPrivate(), m.data));
                m = read(in);
                if (m.cmd == AUTH && m.arg0 == AUTH_TOKEN) {
                    if (!mayAsk) return false;
                    // Not trusted yet: the TV now asks "Allow USB debugging?". Wait for the answer.
                    send(out, AUTH, AUTH_PUBLIC_KEY, 0, publicKey((RSAPublicKey) keys.getPublic()));
                    socket.setSoTimeout(60_000);
                    try {
                        m = read(in);
                    } catch (SocketTimeoutException | EOFException e) {
                        return false;
                    }
                }
            }
            if (m.cmd != CNXN) return false;
            if (command == null) return true;

            // Trimming can take a while on a slow TV.
            socket.setSoTimeout(120_000);
            send(out, OPEN, 1, 0, (command + "\0").getBytes(StandardCharsets.US_ASCII));
            while (true) {
                m = read(in);
                if (m.cmd == WRTE) {
                    send(out, OKAY, 1, m.arg0, null);
                } else if (m.cmd == CLSE) {
                    send(out, CLSE, 1, m.arg0, null);
                    return true;
                }
            }
        }
    }

    private static final class Message {
        int cmd, arg0;
        byte[] data;
    }

    private static void send(OutputStream out, int cmd, int arg0, int arg1, byte[] data) throws IOException {
        int len = data == null ? 0 : data.length, sum = 0;
        for (int i = 0; i < len; i++) sum += data[i] & 0xff;
        ByteBuffer h = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN);
        h.putInt(cmd).putInt(arg0).putInt(arg1).putInt(len).putInt(sum).putInt(~cmd);
        out.write(h.array());
        if (len > 0) out.write(data);
        out.flush();
    }

    private static Message read(DataInputStream in) throws IOException {
        byte[] h = new byte[24];
        in.readFully(h);
        ByteBuffer b = ByteBuffer.wrap(h).order(ByteOrder.LITTLE_ENDIAN);
        Message m = new Message();
        m.cmd = b.getInt();
        m.arg0 = b.getInt();
        b.getInt();
        int len = b.getInt();
        if (len < 0 || len > 1 << 20) throw new IOException("bad message length " + len);
        m.data = new byte[len];
        in.readFully(m.data);
        return m;
    }

    private static byte[] sign(PrivateKey key, byte[] token) throws Exception {
        Signature s = Signature.getInstance("NONEwithRSA");
        s.initSign(key);
        s.update(SHA1_PREFIX);
        s.update(token);
        return s.sign();
    }

    /** The public key in the service's own format: a 2048-bit RSA key as little-endian words, base64. */
    private static byte[] publicKey(RSAPublicKey pub) {
        BigInteger n = pub.getModulus();
        BigInteger r32 = BigInteger.ONE.shiftLeft(32);
        BigInteger n0inv = r32.subtract(n.mod(r32).modInverse(r32));
        BigInteger rr = BigInteger.ONE.shiftLeft(4096).mod(n);
        ByteBuffer b = ByteBuffer.allocate(4 + 4 + 256 + 256 + 4).order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(64).putInt(n0inv.intValue());
        for (int i = 0; i < 64; i++) b.putInt(n.shiftRight(32 * i).intValue());
        for (int i = 0; i < 64; i++) b.putInt(rr.shiftRight(32 * i).intValue());
        b.putInt(pub.getPublicExponent().intValue());
        String text = Base64.encodeToString(b.array(), Base64.NO_WRAP) + " Glass TV\0";
        return text.getBytes(StandardCharsets.US_ASCII);
    }

    /** This app's own key, made once and kept in its private files. */
    private static KeyPair keys(Context ctx) throws Exception {
        File priv = new File(ctx.getFilesDir(), "adbkey"), pub = new File(ctx.getFilesDir(), "adbkey.pub");
        KeyFactory f = KeyFactory.getInstance("RSA");
        if (priv.isFile() && pub.isFile()) {
            try {
                return new KeyPair(f.generatePublic(new X509EncodedKeySpec(readAll(pub))),
                        f.generatePrivate(new PKCS8EncodedKeySpec(readAll(priv))));
            } catch (Exception e) {
                Log.w(TAG, "stored key unreadable, making a new one: " + e);
            }
        }
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(new RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4));
        KeyPair k = g.generateKeyPair();
        writeAll(priv, k.getPrivate().getEncoded());
        writeAll(pub, k.getPublic().getEncoded());
        return k;
    }

    private static byte[] readAll(File file) throws IOException {
        byte[] buf = new byte[(int) file.length()];
        try (FileInputStream in = new FileInputStream(file)) {
            int n = 0, r;
            while (n < buf.length && (r = in.read(buf, n, buf.length - n)) > 0) n += r;
        }
        return buf;
    }

    private static void writeAll(File file, byte[] data) throws IOException {
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(data);
        }
    }
}
