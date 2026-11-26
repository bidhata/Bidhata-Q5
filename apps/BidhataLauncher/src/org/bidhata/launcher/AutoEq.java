package org.bidhata.launcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The AutoEQ database (github.com/jaakkopasanen/AutoEq): ~9000 measured headphones, each with
 * a "GraphicEQ" line that JamesDSP's arbitrary-response EQ takes as-is.
 */
final class AutoEq {

    private static final String BASE = "https://raw.githubusercontent.com/jaakkopasanen/AutoEq/master/results/";

    static final class Entry {
        final String name, source, path;   // path is URL-encoded, relative to results/

        Entry(String name, String source, String path) {
            this.name = name;
            this.source = source;
            this.path = path;
        }

        String graphicEqUrl() {
            return BASE + path + "/" + path.substring(path.lastIndexOf('/') + 1) + "%20GraphicEQ.txt";
        }
    }

    private AutoEq() {}

    /** Loads the index, from the cached copy when there is one (refresh with force). */
    static List<Entry> index(File cacheDir, boolean force) throws IOException {
        File cached = new File(cacheDir, "autoeq_index.md");
        if (force || !cached.exists() || cached.length() < 10000) {
            File tmp = new File(cacheDir, "autoeq_index.part");
            download(BASE + "INDEX.md", tmp);
            if (!tmp.renameTo(cached)) throw new IOException("cannot save index");
        }
        List<Entry> out = new ArrayList<>();
        BufferedReader r = new BufferedReader(new InputStreamReader(
                new java.io.FileInputStream(cached), "UTF-8"));
        try {
            String line;
            // - [Sennheiser HD 650](./oratory1990/over-ear/Sennheiser%20HD%20650) by oratory1990
            while ((line = r.readLine()) != null) {
                if (!line.startsWith("- [")) continue;
                int nameEnd = line.indexOf("](./");
                int pathEnd = line.indexOf(')', nameEnd);
                if (nameEnd < 0 || pathEnd < 0) continue;
                String name = line.substring(3, nameEnd);
                String path = line.substring(nameEnd + 4, pathEnd);
                String source = line.substring(pathEnd + 1).replace(" by ", "").trim();
                out.add(new Entry(name, source, path));
            }
        } finally {
            r.close();
        }
        return out;
    }

    /** Every typed word must appear in the name; spaces and dashes do not count ("hd650" = "HD 650"). */
    static List<Entry> search(List<Entry> all, String query, int max) {
        List<Entry> out = new ArrayList<>();
        String[] words = compact(query).length() == 0 ? new String[0]
                : query.toLowerCase(Locale.ROOT).trim().split("\\s+");
        for (int i = 0; i < words.length; i++) words[i] = compact(words[i]);
        for (Entry e : all) {
            String n = compact(e.name);
            boolean ok = words.length > 0;
            for (String w : words) if (!n.contains(w)) { ok = false; break; }
            if (ok) {
                out.add(e);
                if (out.size() >= max) break;
            }
        }
        return out;
    }

    private static String compact(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    static String fetchGraphicEq(Entry e) throws IOException {
        File tmp = File.createTempFile("geq", ".txt");
        try {
            download(e.graphicEqUrl(), tmp);
            BufferedReader r = new BufferedReader(new java.io.FileReader(tmp));
            try {
                String line = r.readLine();
                if (line == null || !line.startsWith("GraphicEQ:")) throw new IOException("not a GraphicEQ file");
                return line;
            } finally {
                r.close();
            }
        } finally {
            tmp.delete();
        }
    }

    static String decode(String s) {
        try { return URLDecoder.decode(s, "UTF-8"); } catch (Exception ex) { return s; }
    }

    private static void download(String url, File to) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        try {
            if (c.getResponseCode() != 200) throw new IOException("HTTP " + c.getResponseCode());
            InputStream in = c.getInputStream();
            OutputStream out = new FileOutputStream(to);
            try {
                byte[] buf = new byte[16384];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            } finally {
                out.close();
                in.close();
            }
        } finally {
            c.disconnect();
        }
    }
}
