package org.bidhata.launcher;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;

import java.util.List;

/** "More": Wi-Fi Share switch and every installed app. Long-press an app for its info page. */
public class AppsActivity extends Activity {

    private TextView shareTitle, shareUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apps);

        shareTitle = (TextView) findViewById(R.id.share_title);
        shareUrl = (TextView) findViewById(R.id.share_url);
        findViewById(R.id.share).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { toggleShare(); }
        });

        bootValue = (TextView) findViewById(R.id.bootname_value);
        bootValue.setText(getSharedPreferences("bootname", MODE_PRIVATE).getString("name", "BIDHATA"));
        findViewById(R.id.bootname).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { askBootName(); }
        });
        findViewById(R.id.power_row).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(AppsActivity.this, PowerActivity.class));
            }
        });
        Extensions.addMoreRows(this, (android.widget.LinearLayout) findViewById(R.id.ext_rows));
        grid = (GridView) findViewById(R.id.grid);
        AppList.get(this, appsReady);
    }

    private GridView grid;
    private TextView bootValue;

    /** Ask for a name; build the personal boot animation in the background. */
    private void askBootName() {
        final android.widget.EditText in = new android.widget.EditText(this);
        in.setSingleLine(true);
        in.setHint(R.string.bootname_hint);
        in.setFilters(new android.text.InputFilter[] { new android.text.InputFilter.LengthFilter(16),
                new android.text.InputFilter.AllCaps() });
        in.setText(bootValue.getText());
        new android.app.AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle(R.string.bootname_title)
                .setView(in)
                .setPositiveButton(R.string.bootname_save, new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface d, int w) {
                        String name = in.getText().toString().trim();
                        if (name.isEmpty() || name.equals("BIDHATA")) resetBootName(); else makeBootName(name);
                    }
                })
                .setNeutralButton(R.string.bootname_default, new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface d, int w) { resetBootName(); }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void makeBootName(final String name) {
        bootValue.setText(R.string.bootname_working);
        new android.os.AsyncTask<Void, Void, Boolean>() {
            @Override protected Boolean doInBackground(Void... v) {
                try {
                    java.io.File dir = new java.io.File(android.os.Environment.getExternalStorageDirectory(), "Bidhata");
                    dir.mkdirs();
                    BootName.build(name, new java.io.File(dir, "bootanimation.zip"));
                    return true;
                } catch (Exception e) {
                    android.util.Log.w("BidhataBootName", "build failed", e);
                    return false;
                }
            }
            @Override protected void onPostExecute(Boolean ok) {
                if (ok) {
                    Root.request("bootanim:apply");
                    getSharedPreferences("bootname", MODE_PRIVATE).edit().putString("name", name).apply();
                    bootValue.setText(name);
                    android.widget.Toast.makeText(AppsActivity.this, R.string.bootname_done, android.widget.Toast.LENGTH_SHORT).show();
                } else {
                    bootValue.setText(R.string.bootname_failed);
                }
            }
        }.execute();
    }

    private void resetBootName() {
        Root.request("bootanim:reset");
        getSharedPreferences("bootname", MODE_PRIVATE).edit().remove("name").apply();
        bootValue.setText("BIDHATA");
    }

    private final AppList.Listener appsReady = new AppList.Listener() {
        @Override public void onApps(final List<AppList.App> apps) {
            grid.setAdapter(new AppList.Adapter(AppsActivity.this, apps));
            grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override public void onItemClick(AdapterView<?> p, View v, int pos, long id) {
                    startActivity(apps.get(pos).launchIntent());
                }
            });
            grid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override public boolean onItemLongClick(AdapterView<?> p, View v, int pos, long id) {
                    startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", apps.get(pos).component.getPackageName(), null)));
                    return true;
                }
            });
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AppList.forget(appsReady);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateShare();
    }

    private void toggleShare() {
        Intent svc = new Intent(this, ShareService.class);
        if (ShareService.url != null) {
            stopService(svc);
            shareTitle.postDelayed(new Runnable() { @Override public void run() { updateShare(); } }, 300);
            return;
        }
        String[] perms = { android.Manifest.permission.READ_EXTERNAL_STORAGE,
                           android.Manifest.permission.WRITE_EXTERNAL_STORAGE };
        if (checkSelfPermission(perms[1]) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(perms, 2);
            return;
        }
        if (ShareService.currentIp(this) == null) {
            shareUrl.setText(R.string.share_no_wifi);
            return;
        }
        startService(svc);
        shareTitle.postDelayed(new Runnable() { @Override public void run() { updateShare(); } }, 500);
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        if (code == 2 && results.length > 1 && results[1] == PackageManager.PERMISSION_GRANTED) toggleShare();
    }

    private void updateShare() {
        if (ShareService.url != null) {
            shareTitle.setText(R.string.share_on);
            shareTitle.setTextColor(getColor(R.color.accent));
            shareUrl.setText(ShareService.url);
        } else {
            shareTitle.setText(R.string.share_off);
            shareTitle.setTextColor(getColor(R.color.text));
            shareUrl.setText(R.string.share_hint);
        }
    }
}
