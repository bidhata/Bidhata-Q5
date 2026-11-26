package org.bidhata.launcher;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

/**
 * Minimal HTTP file server for the Q5's internal storage: browse, download, upload.
 * Every request must carry the session key (?k=...), so only someone who sees the link on the
 * Q5's screen can get in. Runs only while "Wi-Fi Share" is switched on.
 */
final class ShareServer implements Runnable {
    private static final String TAG = "BidhataShare";
    static final int PORT = 8080;

    private final File root = Environment.getExternalStorageDirectory();
    private final String key;
    private ServerSocket server;
    private Thread thread;

    ShareServer(String key) { this.key = key; }

    void start() throws IOException {
        server = new ServerSocket(PORT, 8, (InetAddress) null);
        thread = new Thread(this, "share-http");
        thread.start();
    }

    void stop() {
        try { if (server != null) server.close(); } catch (IOException ignored) { }
        server = null;
    }

    @Override
    public void run() {
        while (server != null && !server.isClosed()) {
            try {
                final Socket s = server.accept();
                new Thread(new Runnable() {
                    @Override public void run() { handle(s); }
                }, "share-conn").start();
            } catch (IOException e) {
                break;
            }
        }
    }

    private void handle(Socket s) {
        try {
            s.setSoTimeout(30000);
            InputStream in = s.getInputStream();
            OutputStream out = new BufferedOutputStream(s.getOutputStream(), 65536);
            String head = readHead(in);
            if (head == null) { s.close(); return; }
            String[] lines = head.split("\r\n");
            String[] req = lines[0].split(" ");
            if (req.length < 2) { s.close(); return; }
            String method = req[0], target = req[1];
            String path = target, query = "";
            int q = target.indexOf('?');
            if (q >= 0) { path = target.substring(0, q); query = target.substring(q + 1); }
            path = URLDecoder.decode(path, "UTF-8");

            if (!query.contains("k=" + key)) { send(out, 403, "text/plain", "Forbidden\n".getBytes()); s.close(); return; }

            File f = new File(root, path).getCanonicalFile();
            if (!f.getPath().startsWith(root.getCanonicalPath())) { send(out, 403, "text/plain", "Forbidden\n".getBytes()); s.close(); return; }

            if ("POST".equals(method) && f.isDirectory()) {
                long len = 0;
                String boundary = null;
                for (String l : lines) {
                    String lower = l.toLowerCase(Locale.US);
                    if (lower.startsWith("content-length:")) len = Long.parseLong(l.substring(15).trim());
                    if (lower.startsWith("content-type:") && lower.contains("boundary=")) boundary = l.substring(l.indexOf("boundary=") + 9).trim();
                }
                String msg = boundary != null ? receiveUpload(in, len, boundary, f) : "Bad upload";
                redirect(out, path, msg);
            } else if (f.isDirectory()) {
                send(out, 200, "text/html; charset=utf-8", listing(f, path).getBytes("UTF-8"));
            } else if (f.isFile()) {
                sendFile(out, f);
            } else {
                send(out, 404, "text/plain", "Not found\n".getBytes());
            }
            out.flush();
            s.close();
        } catch (Exception e) {
            Log.w(TAG, "request failed: " + e);
            try { s.close(); } catch (IOException ignored) { }
        }
    }

    private static String readHead(InputStream in) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        int c, state = 0;
        while ((c = in.read()) != -1) {
            b.write(c);
            if ((state == 0 || state == 2) && c == '\r') state++;
            else if ((state == 1 || state == 3) && c == '\n') state++;
            else state = 0;
            if (state == 4) return b.toString("UTF-8");
            if (b.size() > 16384) return null;
        }
        return null;
    }

    /** Streams one multipart file part to disk without holding it in memory. */
    private String receiveUpload(InputStream in, long len, String boundary, File dir) throws IOException {
        byte[] delim = ("\r\n--" + boundary).getBytes("UTF-8");
        String partHead = readHead(in);
        if (partHead == null) return "Bad upload";
        String name = "upload.bin";
        int i = partHead.indexOf("filename=\"");
        if (i >= 0) {
            int e = partHead.indexOf('"', i + 10);
            if (e > i + 10) name = new File(partHead.substring(i + 10, e)).getName();
        }
        if (name.isEmpty()) return "No file chosen";
        File target = new File(dir, name);
        OutputStream fo = new BufferedOutputStream(new FileOutputStream(target), 65536);
        // simple delimiter search over a sliding window
        byte[] win = new byte[delim.length];
        int filled = 0;
        long total = 0;
        int c;
        while ((c = in.read()) != -1) {
            if (filled < win.length) {
                win[filled++] = (byte) c;
            } else {
                fo.write(win[0]);
                total++;
                System.arraycopy(win, 1, win, 0, win.length - 1);
                win[win.length - 1] = (byte) c;
            }
            if (filled == win.length && Arrays.equals(win, delim)) break;
        }
        fo.close();
        return "Uploaded " + name + " (" + (total / 1024) + " KB)";
    }

    private String listing(File dir, String path) {
        File[] files = dir.listFiles();
        if (files == null) files = new File[0];
        Arrays.sort(files, new Comparator<File>() {
            @Override public int compare(File a, File b) {
                if (a.isDirectory() != b.isDirectory()) return a.isDirectory() ? -1 : 1;
                return a.getName().compareToIgnoreCase(b.getName());
            }
        });
        StringBuilder h = new StringBuilder();
        h.append("<!doctype html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'>")
         .append("<title>Q5 ").append(esc(path)).append("</title><style>")
         .append("body{background:#121212;color:#ececec;font-family:sans-serif;margin:0;padding:16px}")
         .append("a{color:#7fd4ff;text-decoration:none}h1{font-weight:300;font-size:20px}")
         .append("table{width:100%;border-collapse:collapse}td{padding:8px 4px;border-bottom:1px solid #26262a}")
         .append(".d{color:#8e8e93;font-size:13px;text-align:right}form{background:#1c1c1e;padding:12px;border-radius:10px;margin:12px 0}")
         .append("</style></head><body><h1>Bidhata Q5 &mdash; ").append(esc(path)).append("</h1>")
         .append("<form method='post' enctype='multipart/form-data' action='").append(href(path)).append("'>")
         .append("<input type='file' name='f'> <input type='submit' value='Upload here'></form><table>");
        if (!"/".equals(path) && !path.isEmpty()) {
            String up = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
            up = up.substring(0, Math.max(1, up.lastIndexOf('/')));
            h.append("<tr><td><a href='").append(href(up)).append("'>&larr; up</a></td><td></td></tr>");
        }
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        for (File f : files) {
            String p = (path.endsWith("/") ? path : path + "/") + f.getName();
            h.append("<tr><td><a href='").append(href(p)).append("'>").append(esc(f.getName())).append(f.isDirectory() ? "/" : "")
             .append("</a></td><td class='d'>").append(f.isDirectory() ? "" : human(f.length()) + " &middot; ")
             .append(df.format(new Date(f.lastModified()))).append("</td></tr>");
        }
        return h.append("</table></body></html>").toString();
    }

    private String href(String p) {
        try {
            StringBuilder b = new StringBuilder();
            for (String seg : p.split("/")) {
                if (seg.isEmpty()) continue;
                b.append('/').append(URLEncoder.encode(seg, "UTF-8").replace("+", "%20"));
            }
            return (b.length() == 0 ? "/" : b.toString()) + "?k=" + key;
        } catch (IOException e) {
            return "/?k=" + key;
        }
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("'", "&#39;");
    }

    private static String human(long n) {
        if (n < 1024) return n + " B";
        if (n < 1024 * 1024) return (n / 1024) + " KB";
        return String.format(Locale.US, "%.1f MB", n / 1048576.0);
    }

    private static void send(OutputStream out, int code, String type, byte[] body) throws IOException {
        out.write(("HTTP/1.1 " + code + " OK\r\nContent-Type: " + type + "\r\nContent-Length: " + body.length
                + "\r\nConnection: close\r\n\r\n").getBytes("UTF-8"));
        out.write(body);
    }

    private void redirect(OutputStream out, String path, String msg) throws IOException {
        out.write(("HTTP/1.1 303 See Other\r\nLocation: " + href(path) + "\r\nContent-Length: 0\r\nConnection: close\r\n\r\n").getBytes("UTF-8"));
        Log.i(TAG, msg);
    }

    private static void sendFile(OutputStream out, File f) throws IOException {
        out.write(("HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Length: " + f.length()
                + "\r\nContent-Disposition: attachment; filename=\"" + f.getName().replace("\"", "") + "\"\r\nConnection: close\r\n\r\n").getBytes("UTF-8"));
        InputStream in = new FileInputStream(f);
        byte[] buf = new byte[65536];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
    }
}
