package org.bidhata.launcher;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**
 * How other apps add themselves to the launcher without any of their code running inside it.
 *
 * An app that wants a row on the More page declares an activity with this intent filter:
 *
 *   <intent-filter>
 *       <action android:name="org.bidhata.launcher.MORE_ROW" />
 *       <category android:name="android.intent.category.DEFAULT" />
 *   </intent-filter>
 *
 * The activity's label is the row text; a tap opens the activity. (For a home tile nothing is
 * needed: any installed app can be put on home through home.json.)
 */
final class Extensions {

    static final String ACTION_MORE_ROW = "org.bidhata.launcher.MORE_ROW";
    /** The More page is short; beyond this, rows would squeeze out the app grid. */
    private static final int MAX_ROWS = 3;

    private Extensions() { }

    static void addMoreRows(final Activity a, LinearLayout into) {
        into.removeAllViews();
        PackageManager pm = a.getPackageManager();
        List<ResolveInfo> found;
        try {
            found = pm.queryIntentActivities(new Intent(ACTION_MORE_ROW), 0);
        } catch (RuntimeException e) {
            Log.w("BidhataExt", "could not look up extension rows", e);
            return;
        }
        float dp = a.getResources().getDisplayMetrics().density;
        int n = 0;
        for (ResolveInfo ri : found) {
            if (n++ >= MAX_ROWS) break;
            final ComponentName target = new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name);
            TextView row = new TextView(a);
            row.setText(ri.loadLabel(pm));
            row.setTextColor(a.getResources().getColor(R.color.text));
            row.setTextSize(15);
            row.setSingleLine(true);
            row.setBackgroundResource(R.drawable.tile_bg);
            row.setPadding((int) (14 * dp), (int) (12 * dp), (int) (14 * dp), (int) (12 * dp));
            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    try {
                        a.startActivity(new Intent(ACTION_MORE_ROW).setComponent(target));
                    } catch (RuntimeException e) {
                        Log.w("BidhataExt", "extension row did not open: " + target, e);
                    }
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = (int) (8 * dp);
            into.addView(row, lp);
        }
    }
}
