package org.bidhata.launcher;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Launchable apps. Loading labels and icons is slow on this phone (a few hundred ms), so the
 * list is built once on a background thread, kept in memory, and rebuilt only when an app is
 * installed, removed or updated. Opening "More" then costs nothing.
 */
final class AppList {

    interface Listener { void onApps(List<App> apps); }

    static final class App {
        final String label;
        final Drawable icon;
        final ComponentName component;

        App(String label, Drawable icon, ComponentName component) {
            this.label = label;
            this.icon = icon;
            this.component = component;
        }

        Intent launchIntent() {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            i.setComponent(component);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            return i;
        }
    }

    /** Apps we know get a matching Adwaita glyph so the grid reads as one family. */
    private static final Map<String, Integer> GLYPHS = new HashMap<>();
    static {
        GLYPHS.put("com.android.settings", R.drawable.c_settings);
        GLYPHS.put("me.zhanghai.android.files", R.drawable.c_files);
        GLYPHS.put("com.amaze.filemanager", R.drawable.c_files);
        GLYPHS.put("com.android.documentsui", R.drawable.c_files);
        GLYPHS.put("org.fdroid.fdroid", R.drawable.c_store);
        GLYPHS.put("org.solovyev.android.calculator", R.drawable.c_calculator);
        GLYPHS.put("net.gsantner.markor", R.drawable.c_notes);
        GLYPHS.put("org.koreader.launcher.fdroid", R.drawable.c_books);
        GLYPHS.put("org.koreader.launcher", R.drawable.c_books);
        GLYPHS.put("org.coolreader", R.drawable.c_books);
        GLYPHS.put("james.dsp", R.drawable.c_eq);
        GLYPHS.put("com.android.fmradio", R.drawable.c_radio);
        GLYPHS.put("org.bidhata.recorder", R.drawable.c_recorder);
        GLYPHS.put("com.hiby.music", R.drawable.c_music);
        GLYPHS.put("com.cyanogenmod.eleven", R.drawable.c_music);
        GLYPHS.put("com.mxtech.videoplayer.pro", R.drawable.c_video);
        GLYPHS.put("com.mxtech.videoplayer.ad", R.drawable.c_video);
        GLYPHS.put("org.videolan.vlc", R.drawable.c_video);
        GLYPHS.put("com.android.gallery3d", R.drawable.c_gallery);
        GLYPHS.put("com.android.providers.downloads.ui", R.drawable.c_files);
        GLYPHS.put("org.cyanogenmod.theme.chooser2", R.drawable.c_settings);
    }

    private static final Handler main = new Handler(Looper.getMainLooper());
    private static List<App> cache;
    private static boolean loading, watching;
    private static final List<Listener> waiting = new ArrayList<>();

    private AppList() {}

    /** Calls back on the main thread, straight away if the list is already in memory. */
    static void get(Context ctx, Listener l) {
        if (cache != null) { l.onApps(cache); return; }
        waiting.add(l);
        load(ctx.getApplicationContext());
    }

    /** Warm the cache early (called from the home screen). */
    static void preload(Context ctx) {
        watch(ctx.getApplicationContext());
        if (cache == null) load(ctx.getApplicationContext());
    }

    static void forget(Listener l) { waiting.remove(l); }

    private static void load(final Context ctx) {
        if (loading) return;
        loading = true;
        new Thread("applist") {
            @Override public void run() {
                final List<App> apps = query(ctx);
                main.post(new Runnable() {
                    @Override public void run() {
                        cache = apps;
                        loading = false;
                        List<Listener> ls = new ArrayList<>(waiting);
                        waiting.clear();
                        for (Listener l : ls) l.onApps(apps);
                    }
                });
            }
        }.start();
    }

    private static void watch(final Context ctx) {
        if (watching) return;
        watching = true;
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_PACKAGE_ADDED);
        f.addAction(Intent.ACTION_PACKAGE_REMOVED);
        f.addAction(Intent.ACTION_PACKAGE_CHANGED);
        f.addAction(Intent.ACTION_PACKAGE_REPLACED);
        f.addDataScheme("package");
        ctx.registerReceiver(new BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent i) {
                cache = null;
                load(ctx);
            }
        }, f);
    }

    private static List<App> query(Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN);
        main.addCategory(Intent.CATEGORY_LAUNCHER);
        List<App> apps = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (ResolveInfo ri : pm.queryIntentActivities(main, 0)) {
            if (ctx.getPackageName().equals(ri.activityInfo.packageName)) continue;
            // some apps (Calculator++) list extra entry points: show each app once, by its main one
            String pkg = ri.activityInfo.packageName;
            Intent mainOf = pm.getLaunchIntentForPackage(pkg);
            if (mainOf != null && mainOf.getComponent() != null
                    && !mainOf.getComponent().getClassName().equals(ri.activityInfo.name)) continue;
            if (!seen.add(pkg)) continue;
            Integer glyph = GLYPHS.get(ri.activityInfo.packageName);
            Drawable icon = glyph != null ? ctx.getDrawable(glyph) : ri.loadIcon(pm);
            apps.add(new App(ri.loadLabel(pm).toString(), icon,
                    new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name)));
        }
        final Collator c = Collator.getInstance();
        Collections.sort(apps, new Comparator<App>() {
            @Override public int compare(App a, App b) { return c.compare(a.label, b.label); }
        });
        return apps;
    }

    /** Foreign icons sit inside the same circle, a bit smaller so they do not touch the rim. */
    private static Drawable shrink(Context ctx, Drawable d) {
        int inset = (int) (11 * ctx.getResources().getDisplayMetrics().density);
        return new android.graphics.drawable.InsetDrawable(d, inset);
    }

    static final class Adapter extends BaseAdapter {
        private final LayoutInflater inflater;
        private final List<App> apps;

        Adapter(Context ctx, List<App> apps) {
            this.inflater = LayoutInflater.from(ctx);
            this.apps = apps;
        }

        @Override public int getCount() { return apps.size(); }
        @Override public App getItem(int pos) { return apps.get(pos); }
        @Override public long getItemId(int pos) { return pos; }

        private static final class Holder {
            ImageView icon;
            TextView label;
        }

        @Override
        public View getView(int pos, View v, ViewGroup parent) {
            Holder h;
            if (v == null) {
                v = inflater.inflate(R.layout.app_item, parent, false);
                h = new Holder();
                h.icon = (ImageView) v.findViewById(R.id.icon);
                h.label = (TextView) v.findViewById(R.id.label);
                v.setTag(h);
            } else {
                h = (Holder) v.getTag();
            }
            App a = apps.get(pos);
            h.icon.setImageDrawable(a.icon);
            h.label.setText(a.label);
            return v;
        }
    }
}
