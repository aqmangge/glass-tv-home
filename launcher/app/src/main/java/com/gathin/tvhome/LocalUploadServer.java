package com.gathin.tvhome;

import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * Lightweight embedded local HTTP server running on Android TV.
 * Allows phones/tablets on the same Wi-Fi network to upload custom profile images or animated GIFs
 * directly to the TV via an intuitive mobile web page.
 */
public class LocalUploadServer {

    public interface OnFileUploadedListener {
        void onFileUploaded(File uploadedFile);
    }

    private static ServerSocket serverSocket;
    private static Thread serverThread;
    private static boolean isRunning = false;
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public static synchronized void start(int port, File saveDir, OnFileUploadedListener listener) {
        stop();
        if (!saveDir.exists()) saveDir.mkdirs();

        isRunning = true;
        serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                while (isRunning && !serverSocket.isClosed()) {
                    Socket client = serverSocket.accept();
                    new Thread(() -> handleClient(client, saveDir, listener)).start();
                }
            } catch (Exception ignored) {
            } finally {
                isRunning = false;
            }
        });
        serverThread.setName("GlassTV-UploadServer");
        serverThread.start();
    }

    public static synchronized void stop() {
        isRunning = false;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (Exception ignored) {}
            serverSocket = null;
        }
        if (serverThread != null) {
            serverThread.interrupt();
            serverThread = null;
        }
    }

    public static boolean isRunning() {
        return isRunning && serverSocket != null && !serverSocket.isClosed();
    }

    public static String getLocalIpAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {}
        return "192.168.1.100";
    }

    private static void handleClient(Socket socket, File saveDir, OnFileUploadedListener listener) {
        try (InputStream in = socket.getInputStream();
             OutputStream out = socket.getOutputStream()) {

            // Read HTTP request line and headers
            String line;
            String requestLine = null;
            long contentLength = 0;
            String contentType = "";

            // Use a byte buffer to parse headers safely
            StringBuilder headerBuf = new StringBuilder();
            int b;
            int crlfCount = 0;
            while ((b = in.read()) != -1) {
                headerBuf.append((char) b);
                if (b == '\n') {
                    String h = headerBuf.toString().trim();
                    if (requestLine == null) {
                        requestLine = h;
                    } else if (h.toLowerCase().startsWith("content-length:")) {
                        try {
                            contentLength = Long.parseLong(h.substring(15).trim());
                        } catch (Exception ignored) {}
                    } else if (h.toLowerCase().startsWith("content-type:")) {
                        contentType = h.substring(13).trim();
                    }
                    if (headerBuf.toString().endsWith("\r\n\r\n") || headerBuf.toString().endsWith("\n\n")) {
                        break;
                    }
                    headerBuf.setLength(0);
                }
            }

            if (requestLine == null) {
                socket.close();
                return;
            }

            if (requestLine.startsWith("GET")) {
                // Serve Mobile Upload Web Page
                String html = getUploadPageHtml();
                byte[] htmlBytes = html.getBytes(StandardCharsets.UTF_8);
                String response = "HTTP/1.1 200 OK\r\n"
                        + "Content-Type: text/html; charset=UTF-8\r\n"
                        + "Content-Length: " + htmlBytes.length + "\r\n"
                        + "Connection: close\r\n\r\n";
                out.write(response.getBytes(StandardCharsets.UTF_8));
                out.write(htmlBytes);
                out.flush();

            } else if (requestLine.startsWith("POST")) {
                // Read uploaded file body
                String fileName = "avatar_" + System.currentTimeMillis() + ".gif";
                File targetFile = new File(saveDir, fileName);

                try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                    byte[] buf = new byte[8192];
                    long totalRead = 0;
                    int r;
                    while (totalRead < contentLength && (r = in.read(buf, 0, (int) Math.min(buf.length, contentLength - totalRead))) != -1) {
                        fos.write(buf, 0, r);
                        totalRead += r;
                    }
                }

                String successJson = "{\"status\":\"ok\",\"file\":\"" + targetFile.getName() + "\"}";
                byte[] respBytes = successJson.getBytes(StandardCharsets.UTF_8);
                String response = "HTTP/1.1 200 OK\r\n"
                        + "Content-Type: application/json\r\n"
                        + "Content-Length: " + respBytes.length + "\r\n"
                        + "Connection: close\r\n\r\n";
                out.write(response.getBytes(StandardCharsets.UTF_8));
                out.write(respBytes);
                out.flush();

                if (listener != null && targetFile.length() > 0) {
                    MAIN_HANDLER.post(() -> listener.onFileUploaded(targetFile));
                }
            }

        } catch (Exception ignored) {
        } finally {
            try {
                socket.close();
            } catch (Exception ignored) {}
        }
    }

    private static String getUploadPageHtml() {
        return "<!DOCTYPE html>\n"
                + "<html lang='ms'>\n"
                + "<head>\n"
                + "<meta charset='UTF-8'>\n"
                + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>\n"
                + "<title>Glass TV - Muat Naik Avatar</title>\n"
                + "<style>\n"
                + "  * { box-sizing: border-box; }\n"
                + "  body { background: #0A0F1D; color: #F8FAFC; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; display: flex; justify-content: center; align-items: center; min-height: 100vh; margin: 0; padding: 20px; }\n"
                + "  .card { background: rgba(30, 41, 59, 0.7); backdrop-filter: blur(20px); -webkit-backdrop-filter: blur(20px); border: 1.5px solid rgba(56, 189, 248, 0.35); border-radius: 28px; padding: 36px 28px; width: 100%; max-width: 400px; text-align: center; box-shadow: 0 25px 50px -12px rgba(0,0,0,0.6); }\n"
                + "  .badge { display: inline-block; background: rgba(56, 189, 248, 0.15); color: #38BDF8; font-size: 12px; font-weight: 700; padding: 6px 14px; border-radius: 999px; margin-bottom: 16px; border: 1px solid rgba(56, 189, 248, 0.3); letter-spacing: 0.5px; }\n"
                + "  h2 { font-size: 24px; margin: 0 0 8px 0; color: #FFFFFF; font-weight: 700; }\n"
                + "  p { color: #94A3B8; font-size: 14.5px; margin-bottom: 24px; line-height: 1.6; }\n"
                + "  .dropzone { border: 2px dashed rgba(56, 189, 248, 0.45); border-radius: 20px; padding: 30px 20px; margin-bottom: 24px; background: rgba(15, 23, 42, 0.55); cursor: pointer; transition: 0.2s; }\n"
                + "  .dropzone:active { border-color: #38BDF8; background: rgba(56, 189, 248, 0.08); }\n"
                + "  .icon { font-size: 40px; margin-bottom: 12px; }\n"
                + "  input[type=file] { display: none; }\n"
                + "  .file-label { font-size: 14px; color: #E2E8F0; font-weight: 600; word-break: break-all; }\n"
                + "  button { background: linear-gradient(135deg, #0284C7 0%, #0369A1 100%); color: white; border: none; padding: 15px 28px; border-radius: 14px; font-size: 16px; font-weight: 700; width: 100%; cursor: pointer; transition: all 0.2s; box-shadow: 0 10px 20px rgba(2, 132, 199, 0.3); }\n"
                + "  button:active { transform: scale(0.97); }\n"
                + "  #status { margin-top: 18px; font-size: 14px; font-weight: 600; min-height: 20px; }\n"
                + "</style>\n"
                + "</head>\n"
                + "<body>\n"
                + "  <div class='card'>\n"
                + "    <div class='badge'>📺 GLASS TV WIRELESS SYNC</div>\n"
                + "    <h2>Pilih Avatar Anda</h2>\n"
                + "    <p>Muat naik gambar profil atau fail animasi GIF terus ke Android TV anda secara langsung.</p>\n"
                + "    <label class='dropzone' for='fileInput'>\n"
                + "      <div class='icon'>🖼️</div>\n"
                + "      <div class='file-label' id='fileName'>Ketik di sini untuk pilih fail (GIF, PNG, JPG)</div>\n"
                + "      <input type='file' id='fileInput' accept='image/*,.gif,.png,.jpg,.jpeg,.webp'>\n"
                + "    </label>\n"
                + "    <button onclick='uploadFile()'>Hantar Avatar ke TV 🚀</button>\n"
                + "    <div id='status'></div>\n"
                + "  </div>\n"
                + "  <script>\n"
                + "    const fi = document.getElementById('fileInput');\n"
                + "    const fn = document.getElementById('fileName');\n"
                + "    fi.onchange = function() {\n"
                + "      if (fi.files.length > 0) fn.innerText = '📁 ' + fi.files[0].name;\n"
                + "    };\n"
                + "    function uploadFile() {\n"
                + "      const f = fi.files[0];\n"
                + "      if (!f) return alert('Sila pilih fail terlebih dahulu!');\n"
                + "      const st = document.getElementById('status');\n"
                + "      st.innerText = '⏳ Sedang memuat naik ke TV...';\n"
                + "      st.style.color = '#38BDF8';\n"
                + "      fetch('/upload', {\n"
                + "        method: 'POST',\n"
                + "        body: f\n"
                + "      }).then(r => r.json()).then(d => {\n"
                + "        st.innerText = '✨ Berjaya! Avatar telah dipasang pada TV.';\n"
                + "        st.style.color = '#10B981';\n"
                + "      }).catch(e => {\n"
                + "        st.innerText = '❌ Gagal: ' + e;\n"
                + "        st.style.color = '#EF4444';\n"
                + "      });\n"
                + "    }\n"
                + "  </script>\n"
                + "</body>\n"
                + "</html>";
    }
}
