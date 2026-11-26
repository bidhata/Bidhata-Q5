package org.bidhata.launcher;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;

/** More -> Scheduled power: turn the player off and on at set times every day. */
public class PowerActivity extends Activity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_power);
        row("off", R.id.pw_off_switch, R.id.pw_off_time, 23, 0);
        row("on", R.id.pw_on_switch, R.id.pw_on_time, 7, 0);
        findViewById(R.id.pw_recovery).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { askRecovery(); }
        });
    }

    /** Updates are flashed from TWRP; this saves people a PC and an adb command. */
    private void askRecovery() {
        new android.app.AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setMessage(R.string.power_recovery_ask)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface d, int w) {
                        ((android.os.PowerManager) getSystemService(POWER_SERVICE)).reboot("recovery");
                    }
                }).show();
    }

    private void row(final String key, int switchId, int timeId, final int defH, final int defM) {
        final SharedPreferences p = PowerSchedule.prefs(this);
        final Switch sw = (Switch) findViewById(switchId);
        final TextView time = (TextView) findViewById(timeId);
        sw.setChecked(p.getBoolean(key + "_on", false));
        showTime(time, p.getInt(key + "_h", defH), p.getInt(key + "_m", defM));
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton v, boolean on) {
                p.edit().putBoolean(key + "_on", on).apply();
                PowerSchedule.apply(PowerActivity.this);
            }
        });
        time.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                new TimePickerDialog(PowerActivity.this, android.R.style.Theme_Material_Dialog_Alert,
                        new TimePickerDialog.OnTimeSetListener() {
                    @Override public void onTimeSet(TimePicker tp, int h, int m) {
                        p.edit().putInt(key + "_h", h).putInt(key + "_m", m).putBoolean(key + "_on", true).apply();
                        showTime(time, h, m);
                        sw.setChecked(true);
                        PowerSchedule.apply(PowerActivity.this);
                    }
                }, p.getInt(key + "_h", defH), p.getInt(key + "_m", defM),
                        android.text.format.DateFormat.is24HourFormat(PowerActivity.this)).show();
            }
        });
    }

    private void showTime(TextView v, int h, int m) {
        v.setText(String.format("%02d:%02d", h, m));
    }
}
