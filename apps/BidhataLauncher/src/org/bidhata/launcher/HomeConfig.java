package org.bidhata.launcher;

import android.os.Environment;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What the eight home tiles are. Read from /sdcard/Bidhata/home.json when that file exists and
 * makes sense, otherwise the built-in default below. Long-press a tile on home to edit it.
 */
final class HomeConfig {

    private static final String TAG = "BidhataHome";
    static final int SLOTS = 8;

    static final class Tile {
        String label;
        String icon;        // a name from ICONS, or "app" for the first installed app's own icon
        String[] apps;      // packages to try in order
        String action;      // "sound" or "more" instead of apps
    }

    /** Icon names a config file may use. */
    static final Map<String, Integer> ICONS = new HashMap<>();
    static {
        ICONS.put("music", R.drawable.c_music);
        ICONS.put("radio", R.drawable.c_radio);
        ICONS.put("books", R.drawable.c_books);
        ICONS.put("video", R.drawable.c_video);
        ICONS.put("recorder", R.drawable.c_recorder);
        ICONS.put("sound", R.drawable.c_eq);
        ICONS.put("files", R.drawable.c_files);
        ICONS.put("more", R.drawable.c_more);
        ICONS.put("calculator", R.drawable.c_calculator);
        ICONS.put("gallery", R.drawable.c_gallery);
        ICONS.put("notes", R.drawable.c_notes);
        ICONS.put("settings", R.drawable.c_settings);
        ICONS.put("store", R.drawable.c_store);
        ICONS.put("web", R.drawable.c_web);
    }

    static final String DEFAULT = "{\n"
            + "  \"_help\": \"Up to 8 tiles, in order. icon: music radio books video recorder sound files more"
            + " calculator gallery notes settings store web, or app (the app's own icon)."
            + " apps: package names, the first one installed opens. action: sound or more.\",\n"
            + "  \"tiles\": [\n"
            + "    {\"label\": \"Music\", \"icon\": \"music\", \"apps\": [\"com.hiby.music\", \"org.videolan.vlc\"]},\n"
            + "    {\"label\": \"Radio\", \"icon\": \"radio\", \"apps\": [\"com.android.fmradio\"]},\n"
            + "    {\"label\": \"Books\", \"icon\": \"books\", \"apps\": [\"org.coolreader\", \"org.koreader.launcher.fdroid\", \"org.koreader.launcher\"]},\n"
            + "    {\"label\": \"Video\", \"icon\": \"video\", \"apps\": [\"com.mxtech.videoplayer.pro\", \"org.videolan.vlc\", \"com.mxtech.videoplayer.ad\", \"com.android.gallery3d\"]},\n"
            + "    {\"label\": \"Recorder\", \"icon\": \"recorder\", \"apps\": [\"org.bidhata.recorder\"]},\n"
            + "    {\"label\": \"Sound\", \"icon\": \"sound\", \"action\": \"sound\"},\n"
            + "    {\"label\": \"Files\", \"icon\": \"files\", \"apps\": [\"me.zhanghai.android.files\", \"com.amaze.filemanager\", \"com.cyanogenmod.filemanager\"]},\n"
            + "    {\"label\": \"More\", \"icon\": \"more\", \"action\": \"more\"}\n"
            + "  ]\n"
            + "}\n";

    /** Set when the user's file was there but unusable; home shows it once as a toast. */
    static String problem;

    private HomeConfig() { }

    static File file() {
        return new File(new File(Environment.getExternalStorageDirectory(), "Bidhata"), "home.json");
    }

    static List<Tile> load() {
        problem = null;
        File f = file();
        if (f.isFile()) {
            try {
                return parse(read(f));
            } catch (Exception e) {
                Log.w(TAG, "home.json not usable, using the default: " + e.getMessage());
                problem = e.getMessage();
            }
        }
        try {
            return parse(DEFAULT);
        } catch (Exception e) {
            throw new IllegalStateException("built-in home config is broken", e);
        }
    }

    static List<Tile> parse(String json) throws Exception {
        JSONArray a = new JSONObject(json).getJSONArray("tiles");
        if (a.length() == 0) throw new Exception("no tiles in home.json");
        List<Tile> out = new ArrayList<>();
        boolean hasMore = false;
        for (int i = 0; i < a.length() && out.size() < SLOTS; i++) {
            JSONObject o = a.getJSONObject(i);
            Tile t = new Tile();
            t.label = o.getString("label");
            t.icon = o.optString("icon", "app");
            t.action = o.optString("action", null);
            JSONArray apps = o.optJSONArray("apps");
            t.apps = new String[apps == null ? 0 : apps.length()];
            for (int k = 0; k < t.apps.length; k++) t.apps[k] = apps.getString(k);
            if (t.action == null && t.apps.length == 0) {
                throw new Exception("tile \"" + t.label + "\" needs apps or an action");
            }
            if (t.action != null && !"sound".equals(t.action) && !"more".equals(t.action)) {
                throw new Exception("tile \"" + t.label + "\": unknown action " + t.action);
            }
            if (!"app".equals(t.icon) && !ICONS.containsKey(t.icon)) {
                throw new Exception("tile \"" + t.label + "\": unknown icon " + t.icon);
            }
            hasMore |= "more".equals(t.action);
            out.add(t);
        }
        if (!hasMore) {
            // More is the only way to the app list and settings: never let a config lose it
            Tile more = new Tile();
            more.label = "More"; more.icon = "more"; more.action = "more"; more.apps = new String[0];
            if (out.size() == SLOTS) out.set(SLOTS - 1, more); else out.add(more);
        }
        return out;
    }

    /** Puts the default file in place if there is none, so there is something to edit. */
    static File ensureFile() throws Exception {
        File f = file();
        if (!f.isFile()) writeDefault();
        return f;
    }

    static void writeDefault() throws Exception {
        File f = file();
        f.getParentFile().mkdirs();
        FileOutputStream o = new FileOutputStream(f);
        try { o.write(DEFAULT.getBytes("UTF-8")); } finally { o.close(); }
    }

    private static String read(File f) throws Exception {
        if (f.length() > 64 * 1024) throw new Exception("home.json is too big");
        byte[] b = new byte[(int) f.length()];
        FileInputStream in = new FileInputStream(f);
        try {
            int n = 0;
            while (n < b.length) {
                int r = in.read(b, n, b.length - n);
                if (r < 0) break;
                n += r;
            }
        } finally { in.close(); }
        return new String(b, "UTF-8");
    }
}
