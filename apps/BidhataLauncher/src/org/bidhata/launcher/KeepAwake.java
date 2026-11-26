package org.bidhata.launcher;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Keeps the screen on (no dimming, no sleep) while a book, a video or the recorder is in front.
 * Checks the foreground app every few seconds, only while the screen is on.
 */
final class KeepAwake {

    private static final String TAG = "BidhataKeepAwake";
    private static final long POLL_MS = 4000;

    static final Set<String> APPS = new HashSet<>(Arrays.asList(
            "org.coolreader", "org.koreader.launcher.fdroid", "org.koreader.launcher",  // books
            "org.videolan.vlc", "com.mxtech.videoplayer.pro",               // video
            "com.mxtech.videoplayer.ad", "com.android.gallery3d",
            "org.bidhata.recorder"));                                        // recording

    private final Context ctx;
    private final Handler handler = new Handler();
    private final PowerManager.WakeLock lock;
    private boolean screenOn = true;

    @SuppressWarnings("deprecation")
    private KeepAwake(Context ctx) {
        this.ctx = ctx;
        PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        // SCREEN_BRIGHT is deprecated for apps, but it is exactly "keep this screen as it is"
        lock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, TAG);
        lock.setReferenceCounted(false);
    }

    static void start(Context ctx) {
        final KeepAwake k = new KeepAwake(ctx);
        IntentFilter f = new IntentFilter(Intent.ACTION_SCREEN_ON);
        f.addAction(Intent.ACTION_SCREEN_OFF);
        ctx.registerReceiver(new BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent i) {
                k.screenOn = Intent.ACTION_SCREEN_ON.equals(i.getAction());
                k.handler.removeCallbacks(k.poll);
                if (k.screenOn) k.handler.post(k.poll); else k.release();
            }
        }, f);
        k.handler.post(k.poll);
    }

    private final Runnable poll = new Runnable() {
        @Override public void run() {
            if (!screenOn) return;
            String top = foreground();
            if (top != null && APPS.contains(top)) {
                if (!lock.isHeld()) {
                    Log.i(TAG, "keeping the screen on for " + top);
                    lock.acquire();
                }
            } else {
                release();
            }
            handler.postDelayed(this, POLL_MS);
        }
    };

    private void release() {
        if (lock.isHeld()) lock.release();
    }

    @SuppressWarnings("deprecation")
    private String foreground() {
        // REAL_GET_TASKS (we are a privileged system app) makes this return other apps' tasks too
        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> t = am.getRunningTasks(1);
        return t != null && !t.isEmpty() && t.get(0).topActivity != null
                ? t.get(0).topActivity.getPackageName() : null;
    }
}
