package org.bidhata.launcher;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Calendar;

/**
 * Scheduled power off / on (the stock firmware had this as an MTK app).
 * Off: an exact alarm here shuts the player down. On: the next on-time goes into
 * persist.bidhata.poweron; the framework's AlarmManagerService arms MediaTek's power-on RTC
 * alarm from it at every shutdown, so it also works after a manual power-off.
 */
public final class PowerSchedule extends BroadcastReceiver {

    private static final String TAG = "BidhataPower";
    static final String PREFS = "power_schedule";

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Call on boot and after any change: re-arms the off alarm and publishes the next on-time. */
    static void apply(Context c) {
        SharedPreferences p = prefs(c);
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent off = PendingIntent.getBroadcast(c, 0, new Intent(c, PowerSchedule.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
        am.cancel(off);
        if (p.getBoolean("off_on", false)) {
            long t = next(p.getInt("off_h", 23), p.getInt("off_m", 0));
            am.setExact(AlarmManager.RTC_WAKEUP, t, off);
            Log.i(TAG, "power off at " + t);
        }
        long on = p.getBoolean("on_on", false) ? next(p.getInt("on_h", 7), p.getInt("on_m", 0)) / 1000 : 0;
        Root.request("poweron:" + on);
    }

    /** Next wall-clock time at h:m, today if still ahead, otherwise tomorrow. */
    static long next(int h, int m) {
        Calendar now = Calendar.getInstance();
        Calendar t = (Calendar) now.clone();
        t.set(Calendar.HOUR_OF_DAY, h);
        t.set(Calendar.MINUTE, m);
        t.set(Calendar.SECOND, 0);
        t.set(Calendar.MILLISECOND, 0);
        if (!t.after(now)) t.add(Calendar.DAY_OF_MONTH, 1);
        return t.getTimeInMillis();
    }

    @Override
    public void onReceive(Context c, Intent i) {
        Log.i(TAG, "scheduled power off");
        Intent shut = new Intent("android.intent.action.ACTION_REQUEST_SHUTDOWN");
        shut.putExtra("android.intent.extra.KEY_CONFIRM", false);
        shut.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        c.startActivity(shut);
    }
}
