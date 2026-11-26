package org.bidhata.launcher;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.util.Log;

/**
 * Wired headphones in -> Bluetooth off (saves power, nothing to connect to anyway).
 * Unplugged -> Bluetooth back on, but only if we were the ones who turned it off.
 * Leaves Bluetooth alone while a Bluetooth headphone is actually connected.
 */
final class WiredBluetooth extends BroadcastReceiver {

    private static final String TAG = "BidhataWiredBt";
    static final String PREFS = "wired_bt";
    static final String KEY_ENABLED = "enabled";
    private static final String KEY_WE_TURNED_OFF = "we_turned_off";

    static void register(Context ctx) {
        ctx.registerReceiver(new WiredBluetooth(), new IntentFilter(AudioManager.ACTION_HEADSET_PLUG));
    }

    static boolean isEnabled(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, true);
    }

    @Override
    public void onReceive(Context ctx, Intent i) {
        // the plug state is a sticky broadcast: ignore the copy we get just for registering
        if (isInitialStickyBroadcast()) return;
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!p.getBoolean(KEY_ENABLED, true)) return;
        BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
        if (bt == null) return;

        boolean plugged = i.getIntExtra("state", 0) == 1;
        if (plugged) {
            boolean btAudioConnected =
                    bt.getProfileConnectionState(BluetoothProfile.A2DP) == BluetoothProfile.STATE_CONNECTED;
            if (bt.isEnabled() && !btAudioConnected) {
                Log.i(TAG, "wired headphones in: Bluetooth off");
                p.edit().putBoolean(KEY_WE_TURNED_OFF, true).apply();
                bt.disable();
            }
        } else if (p.getBoolean(KEY_WE_TURNED_OFF, false)) {
            Log.i(TAG, "wired headphones out: Bluetooth back on");
            p.edit().putBoolean(KEY_WE_TURNED_OFF, false).apply();
            if (!bt.isEnabled()) bt.enable();
        }
    }
}
