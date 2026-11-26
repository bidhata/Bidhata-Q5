package org.bidhata.launcher;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.PowerManager;

import java.security.SecureRandom;
import java.util.Locale;

/** Keeps the Wi-Fi share server alive while the screen is off and shows the link in a notification. */
public class ShareService extends Service {
    static volatile String url;           // null when not running
    private ShareServer server;
    private WifiManager.WifiLock wifiLock;
    private PowerManager.WakeLock wake;

    static String currentIp(Context c) {
        WifiManager wm = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);
        int ip = wm.getConnectionInfo().getIpAddress();
        if (ip == 0) return null;
        return String.format(Locale.US, "%d.%d.%d.%d", ip & 0xff, (ip >> 8) & 0xff, (ip >> 16) & 0xff, (ip >> 24) & 0xff);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (server != null) return START_STICKY;
        String ip = currentIp(this);
        if (ip == null) { stopSelf(); return START_NOT_STICKY; }
        String key = String.format(Locale.US, "%04d", new SecureRandom().nextInt(10000));
        try {
            server = new ShareServer(key);
            server.start();
        } catch (Exception e) {
            server = null;
            stopSelf();
            return START_NOT_STICKY;
        }
        url = "http://" + ip + ":" + ShareServer.PORT + "/?k=" + key;

        WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "BidhataShare");
        wifiLock.acquire();
        wake = ((PowerManager) getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BidhataShare");
        wake.acquire();

        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, HomeActivity.class), 0);
        Notification n = new Notification.Builder(this)
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setContentTitle("Wi-Fi Share is on")
                .setContentText(url)
                .setContentIntent(open)
                .setOngoing(true)
                .build();
        startForeground(1, n);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (server != null) server.stop();
        server = null;
        url = null;
        if (wifiLock != null && wifiLock.isHeld()) wifiLock.release();
        if (wake != null && wake.isHeld()) wake.release();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent i) { return null; }
}
