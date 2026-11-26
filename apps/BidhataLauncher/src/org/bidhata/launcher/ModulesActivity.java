package org.bidhata.launcher;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

/** More -> Power & updates -> Background features: one switch per module. */
public class ModulesActivity extends Activity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_modules);
        LinearLayout list = (LinearLayout) findViewById(R.id.module_list);
        float dp = getResources().getDisplayMetrics().density;
        for (final Module m : Modules.ALL) {
            Switch sw = new Switch(this);
            sw.setText(m.title());
            sw.setTextColor(getResources().getColor(R.color.text));
            sw.setTextSize(15);
            sw.setMinHeight((int) (44 * dp));
            sw.setChecked(Modules.isEnabled(this, m.id()));
            sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override public void onCheckedChanged(CompoundButton v, boolean on) {
                    Modules.setEnabled(ModulesActivity.this, m.id(), on);
                }
            });
            list.addView(sw, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView hint = new TextView(this);
            hint.setText(m.summary());
            hint.setTextColor(getResources().getColor(R.color.text_dim));
            hint.setTextSize(12);
            hint.setPadding(0, 0, 0, (int) (10 * dp));
            list.addView(hint);
        }
    }
}
