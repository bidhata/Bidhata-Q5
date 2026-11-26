package org.bidhata.launcher;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * The launcher process is persistent (system app), so this lives as long as the phone is on.
 * When the screen goes off we put the lockscreen up straight away, so waking the player shows it
 * without a flash of whatever was open before.
 */
public class LockWatcher extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        if (!"user".equals(android.os.Build.TYPE)) watchMainThread();
        registerReceiver(new BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent i) { LockActivity.show(c); }
        }, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        // we start with the system at boot: cover the stock keyguard from the first frame
        LockActivity.show(this);
        AppList.preload(this);
        Modules.startAll(this);
        // have the home tiles and small settings files in memory before any screen asks for them
        new Thread("bidhata-warm") {
            @Override public void run() {
                HomeConfig.refresh();
                getSharedPreferences("bootname", MODE_PRIVATE).getAll();
                PowerSchedule.prefs(LockWatcher.this).getAll();
            }
        }.start();
    }

    /**
     * Debug builds only: log disk or network work on the main thread (tag StrictMode), the usual
     * cause of a home screen that stutters. Android forgets a policy set while the app starts,
     * so it is set again as each screen opens.
     */
    private void watchMainThread() {
        final android.os.StrictMode.ThreadPolicy policy = new android.os.StrictMode.ThreadPolicy.Builder()
                .detectDiskReads().detectDiskWrites().detectNetwork().penaltyLog().build();
        android.os.StrictMode.setThreadPolicy(policy);
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override public void onActivityCreated(android.app.Activity a, android.os.Bundle b) {
                android.os.StrictMode.setThreadPolicy(policy);
            }
            @Override public void onActivityStarted(android.app.Activity a) { }
            @Override public void onActivityResumed(android.app.Activity a) { }
            @Override public void onActivityPaused(android.app.Activity a) { }
            @Override public void onActivityStopped(android.app.Activity a) { }
            @Override public void onActivitySaveInstanceState(android.app.Activity a, android.os.Bundle b) { }
            @Override public void onActivityDestroyed(android.app.Activity a) { }
        });
    }
}
