package org.bidhata.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Starts and stops the launcher's background features. Each one runs behind a try/catch: if a
 * module throws while starting, it is left off for this run and the home screen carries on.
 */
final class Modules {

    private static final String TAG = "BidhataModule";
    private static final String PREFS = "modules";

    static final List<Module> ALL = Arrays.asList(
            WiredBluetooth.MODULE, KeepAwake.MODULE, PowerSchedule.MODULE);

    private static final Set<String> running = new HashSet<>();

    private Modules() { }

    /** Reads the switches off the main thread, then starts the enabled modules on it. */
    static void startAll(final Context app) {
        final android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
        new Thread("bidhata-modules") {
            @Override public void run() {
                for (final Module m : ALL) {
                    if (!isEnabled(app, m.id())) continue;
                    main.post(new Runnable() {
                        @Override public void run() { Modules.start(app, m); }
                    });
                }
            }
        }.start();
    }

    static boolean isEnabled(Context c, String id) {
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!p.contains(id) && WiredBluetooth.ID.equals(id)) {
            // before modules existed this switch lived in its own file; keep the user's choice
            return c.getSharedPreferences(WiredBluetooth.PREFS, Context.MODE_PRIVATE)
                    .getBoolean(WiredBluetooth.KEY_ENABLED, true);
        }
        return p.getBoolean(id, true);
    }

    static void setEnabled(Context c, String id, boolean on) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(id, on).apply();
        Context app = c.getApplicationContext();
        for (Module m : ALL) {
            if (!m.id().equals(id)) continue;
            if (on) start(app, m); else stop(app, m);
        }
    }

    private static void start(Context app, Module m) {
        if (running.contains(m.id())) return;
        try {
            m.start(app);
            running.add(m.id());
        } catch (Throwable t) {
            Log.e(TAG, m.id() + " failed to start, left off", t);
            stop(app, m);
        }
    }

    private static void stop(Context app, Module m) {
        running.remove(m.id());
        try {
            m.stop(app);
        } catch (Throwable t) {
            Log.w(TAG, m.id() + " did not stop cleanly", t);
        }
    }
}
