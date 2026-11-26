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
        registerReceiver(new BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent i) { LockActivity.show(c); }
        }, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        // we start with the system at boot: cover the stock keyguard from the first frame
        LockActivity.show(this);
        AppList.preload(this);
        WiredBluetooth.register(this);
        KeepAwake.start(this);
        PowerSchedule.apply(this);
    }
}
