package com.zynelabs.streamcheck;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

// ⚙ Settings + About (Created by ZyneLabs + contacts).
public class SettingsActivity extends Activity {

    SharedPreferences sp;
    EditText timeoutInput, threadInput;
    Button style3dBtn, styleCalmBtn;
    String picked = "3d";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        sp = getSharedPreferences("sc", MODE_PRIVATE);
        picked = sp.getString("ui_style", "3d");

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(Theme.bgGradient());
        int pad = dp(16);
        root.setPadding(pad, pad, pad, pad);

        ScrollView sv = new ScrollView(this);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        sv.addView(body);
        root.addView(sv, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));

        TextView title = new TextView(this);
        title.setText("⚙ Settings");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        title.setTextColor(Theme.INK);
        title.setPadding(0, 0, 0, dp(12));
        body.addView(title);

        // timeout
        body.addView(card("CHECK TIMEOUT (SECONDS)"));
        timeoutInput = numInput(String.valueOf(sp.getInt("timeout_sec", 8)));
        body.addView(timeoutInput);

        // threads
        body.addView(card("PARALLEL CHECKS (1–20)"));
        threadInput = numInput(String.valueOf(sp.getInt("threads", 10)));
        body.addView(threadInput);

        // style
        body.addView(card("UI STYLE"));
        LinearLayout sRow = new LinearLayout(this);
        sRow.setOrientation(LinearLayout.HORIZONTAL);
        style3dBtn = mkBtn("3D Glossy", v -> { picked = "3d"; markStyle(); });
        styleCalmBtn = mkBtn("Calm", v -> { picked = "calm"; markStyle(); });
        for (Button bb : new Button[]{style3dBtn, styleCalmBtn}) {
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            blp.rightMargin = dp(8);
            sRow.addView(bb, blp);
        }
        body.addView(sRow);
        markStyle();

        // about
        body.addView(card("ABOUT"));
        LinearLayout about = new LinearLayout(this);
        about.setOrientation(LinearLayout.VERTICAL);
        about.setBackground(Theme.cardBg(this));
        about.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        alp.bottomMargin = dp(8);
        about.setLayoutParams(alp);

        TextView appName = new TextView(this);
        appName.setText("📺 StreamCheck " + appVer());
        appName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        appName.setTextColor(Theme.INK);
        about.addView(appName);

        TextView by = new TextView(this);
        by.setText("Created by ZyneLabs");
        by.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        by.setTextColor(Theme.CYAN);
        by.setPadding(0, dp(4), 0, dp(10));
        about.addView(by);

        TextView desc = new TextView(this);
        desc.setText("Stream health checker for your own M3U playlists.");
        desc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        desc.setTextColor(Theme.MUTED);
        desc.setPadding(0, 0, 0, dp(10));
        about.addView(desc);

        about.addView(linkBtn("Website", "https://dzinlabs-site.zynelabs.workers.dev/"));
        about.addView(linkBtn("Telegram", "https://t.me/Dominic_aiBot"));
        about.addView(linkBtn("GitHub", "https://github.com/zinmyo19"));
        body.addView(about);

        Button save = mkBtn("SAVE", v -> save());
        save.setBackground(Theme.glossy3d(this, Theme.COPPER_TOP, Theme.COPPER_BOT, 18));
        save.setTextColor(Color.WHITE);
        save.setShadowLayer(2, 0, 1, 0x80000000);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        slp.topMargin = dp(12);
        body.addView(save, slp);

        setContentView(root);
    }

    int dp(int v) {
        return Theme.dp(this, v);
    }

    TextView card(String label) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        t.setTextColor(Theme.MUTED);
        t.setPadding(dp(4), dp(14), dp(4), dp(4));
        return t;
    }

    EditText numInput(String val) {
        EditText e = new EditText(this);
        e.setText(val);
        e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setTextColor(Theme.INK);
        e.setBackground(Theme.inputBg(this));
        e.setPadding(dp(14), dp(12), dp(14), dp(12));
        return e;
    }

    Button mkBtn(String t, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(t);
        b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        b.setPadding(dp(8), dp(12), dp(8), dp(12));
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setOnClickListener(l);
        return b;
    }

    void markStyle() {
        if ("3d".equals(picked)) {
            style3dBtn.setBackground(
                    Theme.glossy3d(this, Theme.COPPER_TOP, Theme.COPPER_BOT, 16));
            style3dBtn.setTextColor(Color.WHITE);
            styleCalmBtn.setBackground(
                    Theme.glossy3d(this, Theme.PILL_OFF_TOP, Theme.PILL_OFF_BOT, 16));
            styleCalmBtn.setTextColor(0xFFDCDCDC);
        } else {
            styleCalmBtn.setBackground(
                    Theme.glossy3d(this, Theme.COPPER_TOP, Theme.COPPER_BOT, 16));
            styleCalmBtn.setTextColor(Color.WHITE);
            style3dBtn.setBackground(
                    Theme.glossy3d(this, Theme.PILL_OFF_TOP, Theme.PILL_OFF_BOT, 16));
            style3dBtn.setTextColor(0xFFDCDCDC);
        }
    }

    Button linkBtn(String label, final String url) {
        Button b = mkBtn(label, v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (Exception e) {
                Toast.makeText(this, "Cannot open link", Toast.LENGTH_SHORT).show();
            }
        });
        b.setBackground(Theme.glossy3d(this, Theme.SILVER_TOP, Theme.SILVER_BOT, 14));
        b.setTextColor(0xFF3A3A3A);
        b.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(8);
        b.setLayoutParams(lp);
        return b;
    }

    String appVer() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "";
        }
    }

    int parseNum(EditText e, int def) {
        try {
            return Integer.parseInt(e.getText().toString().trim());
        } catch (Exception ex) {
            return def;
        }
    }

    void save() {
        int to = Math.max(3, Math.min(30, parseNum(timeoutInput, 8)));
        int th = Math.max(1, Math.min(20, parseNum(threadInput, 10)));
        sp.edit()
                .putInt("timeout_sec", to)
                .putInt("threads", th)
                .putString("ui_style", picked)
                .apply();
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
        finish();
    }
}
