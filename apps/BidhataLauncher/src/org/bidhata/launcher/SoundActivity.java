package org.bidhata.launcher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Sound: find your headphones in the AutoEQ database and apply the correction to JamesDSP for
 * wired, Bluetooth or the speaker. The actual write into JamesDSP is done by the q5_autoeq
 * service (root), see rootdir/bin/q5_autoeq.sh.
 */
public class SoundActivity extends Activity {

    private EditText query;
    private ListView list;
    private TextView status, current;
    private RadioGroup output;
    private List<AutoEq.Entry> all;
    private final List<AutoEq.Entry> shown = new ArrayList<>();
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sound);
        prefs = getSharedPreferences("autoeq", MODE_PRIVATE);

        query = (EditText) findViewById(R.id.ae_query);
        list = (ListView) findViewById(R.id.ae_list);
        status = (TextView) findViewById(R.id.ae_status);
        current = (TextView) findViewById(R.id.ae_current);
        output = (RadioGroup) findViewById(R.id.ae_output);

        // start on whatever the sound is going to right now
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        output.check(am.isBluetoothA2dpOn() ? R.id.ae_bt : am.isWiredHeadsetOn() ? R.id.ae_wired : R.id.ae_wired);
        output.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(RadioGroup g, int id) { showCurrent(); }
        });

        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> p, View v, int pos, long id) { apply(shown.get(pos)); }
        });
        query.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { filter(); }
        });
        findViewById(R.id.ae_off).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { turnOff(); }
        });
        findViewById(R.id.ae_advanced).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent i = getPackageManager().getLaunchIntentForPackage("james.dsp");
                if (i != null) startActivity(i);
            }
        });

        android.widget.Switch wiredBt = (android.widget.Switch) findViewById(R.id.wired_bt);
        wiredBt.setChecked(Modules.isEnabled(this, WiredBluetooth.ID));
        wiredBt.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(android.widget.CompoundButton b, boolean on) {
                Modules.setEnabled(SoundActivity.this, WiredBluetooth.ID, on);
            }
        });

        setupCodec(R.id.bt_codec, R.array.bt_codec_names, "persist.bt.a2dp.codec",
                new String[] { "auto", "ldac", "aac", "sbc" });
        setupCodec(R.id.ldac_quality, R.array.ldac_quality_names, "persist.bt.ldac.quality",
                new String[] { "abr", "hq", "sq", "mq" });

        showCurrent();
        loadIndex(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String[] perms = { android.Manifest.permission.WRITE_EXTERNAL_STORAGE };
        if (checkSelfPermission(perms[0]) != PackageManager.PERMISSION_GRANTED) requestPermissions(perms, 3);
    }

    /** A spinner bound to one of the Bluetooth stack's persist.bt.* settings (written by root). */
    private void setupCodec(int id, int names, final String prop, final String[] values) {
        android.widget.Spinner sp = (android.widget.Spinner) findViewById(id);
        android.widget.ArrayAdapter<CharSequence> ad = android.widget.ArrayAdapter.createFromResource(
                this, names, android.R.layout.simple_spinner_item);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(ad);
        String cur = sysprop(prop);
        int sel = 0;
        for (int i = 0; i < values.length; i++) if (values[i].equals(cur)) sel = i;
        sp.setSelection(sel, false);
        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long rowId) {
                if (values[pos].equals(sysprop(prop))) return;
                Root.request("btprop:" + prop + "=" + values[pos]);
                status.setText(R.string.bt_reconnect);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private static String sysprop(String key) {
        try {
            Class<?> sp = Class.forName("android.os.SystemProperties");
            Object v = sp.getMethod("get", String.class, String.class).invoke(null, key, "");
            return v != null ? v.toString() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private String outputKey() {
        int id = output.getCheckedRadioButtonId();
        return id == R.id.ae_bt ? "bluetooth" : id == R.id.ae_speaker ? "speaker" : "headset";
    }

    private void showCurrent() {
        String name = prefs.getString("active." + outputKey(), null);
        current.setText(name != null ? getString(R.string.ae_active, name) : getString(R.string.ae_none));
    }

    private void loadIndex(final boolean force) {
        loading = true;
        lastTry = System.currentTimeMillis();
        status.setText(R.string.ae_loading);
        new AsyncTask<Void, Void, List<AutoEq.Entry>>() {
            String error;
            @Override protected List<AutoEq.Entry> doInBackground(Void... v) {
                try { return AutoEq.index(getCacheDir(), force); }
                catch (Exception e) { error = e.getMessage(); return null; }
            }
            @Override protected void onPostExecute(List<AutoEq.Entry> r) {
                loading = false;
                if (r == null) { status.setText(getString(R.string.ae_offline, error)); return; }
                all = r;
                status.setText(getString(R.string.ae_count, r.size()));
                filter();
            }
        }.execute();
    }

    private boolean loading;
    private long lastTry;

    private void filter() {
        // opened before Wi-Fi was up: try again as soon as the user starts typing
        if (all == null && !loading && System.currentTimeMillis() - lastTry > 5000) loadIndex(false);
        shown.clear();
        String q = query.getText().toString();
        if (all != null && q.trim().length() >= 2) shown.addAll(AutoEq.search(all, q, 60));
        adapter.notifyDataSetChanged();
    }

    private void apply(final AutoEq.Entry e) {
        final String out = outputKey();
        status.setText(getString(R.string.ae_fetching, e.name));
        new AsyncTask<Void, Void, String>() {
            String error;
            @Override protected String doInBackground(Void... v) {
                try {
                    String geq = AutoEq.fetchGraphicEq(e);
                    File dir = new File(Environment.getExternalStorageDirectory(), "Bidhata/AutoEQ");
                    dir.mkdirs();
                    // keep a copy per headphone (works offline later), plus the one to apply
                    write(new File(dir, e.name.replace('/', '-') + ".txt"), geq);
                    write(new File(dir, "current.txt"), geq);
                    return geq;
                } catch (Exception ex) {
                    error = ex.getMessage();
                    return null;
                }
            }
            @Override protected void onPostExecute(String geq) {
                if (geq == null) { status.setText(getString(R.string.ae_failed, error)); return; }
                request(out);
                prefs.edit().putString("active." + out, e.name).apply();
                showCurrent();
                status.setText(getString(R.string.ae_applied, e.name));
            }
        }.execute();
    }

    private void turnOff() {
        String out = outputKey();
        request(out + ":off");
        prefs.edit().remove("active." + out).apply();
        showCurrent();
        status.setText(R.string.ae_turned_off);
    }

    private static void request(String target) {
        Root.request(target);
    }

    private static void write(File f, String s) throws java.io.IOException {
        FileOutputStream o = new FileOutputStream(f);
        try { o.write((s + "\n").getBytes("UTF-8")); } finally { o.close(); }
    }

    private final BaseAdapter adapter = new BaseAdapter() {
        @Override public int getCount() { return shown.size(); }
        @Override public Object getItem(int i) { return shown.get(i); }
        @Override public long getItemId(int i) { return i; }
        @Override public View getView(int i, View v, ViewGroup parent) {
            if (v == null) v = getLayoutInflater().inflate(R.layout.ae_row, parent, false);
            AutoEq.Entry e = shown.get(i);
            ((TextView) v.findViewById(R.id.ae_name)).setText(e.name);
            ((TextView) v.findViewById(R.id.ae_source)).setText(e.source);
            return v;
        }
    };
}
