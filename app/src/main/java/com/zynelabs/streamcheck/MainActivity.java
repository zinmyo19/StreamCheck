package com.zynelabs.streamcheck;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

// 📺 StreamCheck — dead-stream checker for the user's own M3U playlists.
// ZyneLabs "Calm Night" theme (shared with the IPTV app).
public class MainActivity extends Activity {

    static final int LIST_CAP = 500;

    EditText urlInput;
    TextView infoText, statusText;
    ProgressBar progress;
    Button loadBtn, fileBtn, checkBtn, stopBtn, exportBtn, clearBtn;
    Button fDead, fAlive, fAll;
    LinearLayout listBox;

    List<M3u.Channel> channels = new ArrayList<>();
    int filter = 0; // 0=dead 1=alive 2=all
    boolean use3d = true;

    ExecutorService pool;
    AtomicBoolean checking = new AtomicBoolean(false);
    AtomicInteger doneCount = new AtomicInteger(0);
    AtomicInteger okCount = new AtomicInteger(0);
    AtomicInteger badCount = new AtomicInteger(0);
    SharedPreferences sp;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        sp = getSharedPreferences("sc", MODE_PRIVATE);
        use3d = "3d".equals(sp.getString("ui_style", "3d"));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(use3d ? Theme.bgGradient()
                : new android.graphics.drawable.ColorDrawable(Theme.PAPER));
        int pad = dp(16);
        root.setPadding(pad, pad, pad, pad);

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this);
        title.setText("📺 StreamCheck");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        title.setTextColor(Theme.INK);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleRow.addView(title, tlp);
        Button gear = mkButton("SETTINGS", false,
                v -> startActivity(new Intent(this, SettingsActivity.class)));
        gear.setPadding(dp(14), dp(8), dp(14), dp(8));
        titleRow.addView(gear, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        titleRow.setPadding(0, 0, 0, dp(12));
        root.addView(titleRow);

        // URL row
        LinearLayout urlRow = new LinearLayout(this);
        urlRow.setOrientation(LinearLayout.HORIZONTAL);
        urlRow.setGravity(Gravity.CENTER_VERTICAL);
        urlInput = new EditText(this);
        urlInput.setHint("M3U URL…");
        urlInput.setHintTextColor(Theme.MUTED);
        urlInput.setTextColor(Theme.INK);
        urlInput.setBackground(Theme.inputBg(this));
        urlInput.setPadding(dp(14), dp(12), dp(14), dp(12));
        urlInput.setText(sp.getString("last_url", ""));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.rightMargin = dp(8);
        urlRow.addView(urlInput, lp);
        loadBtn = mkButton("LOAD", true, v -> loadFromUrl());
        urlRow.addView(loadBtn, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(urlRow);

        // action row 1
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setPadding(0, dp(12), 0, 0);
        fileBtn = mkButton("FILE", false, v -> pickFile());
        checkBtn = mkButton("CHECK", true, v -> startCheck());
        stopBtn = mkButton("STOP", false, v -> stopCheck());
        addRowButtons(row1, fileBtn, checkBtn, stopBtn);
        root.addView(row1);

        // action row 2
        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setPadding(0, dp(8), 0, 0);
        exportBtn = mkButton("EXPORT", false, v -> exportAlive());
        clearBtn = mkButton("CLEAR", false, v -> clearResults());
        addRowButtons(row2, exportBtn, clearBtn);
        root.addView(row2);

        infoText = new TextView(this);
        infoText.setTextColor(Theme.MUTED);
        infoText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        infoText.setPadding(0, dp(12), 0, 0);
        infoText.setText("No playlist yet — Load URL or pick a file");
        root.addView(infoText);

        statusText = new TextView(this);
        statusText.setTextColor(Theme.INK);
        statusText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        statusText.setPadding(0, dp(4), 0, 0);
        root.addView(statusText);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setPadding(0, dp(8), 0, dp(4));
        progress.getProgressDrawable().setColorFilter(Theme.COPPER_BOT, PorterDuff.Mode.SRC_IN);
        root.addView(progress);

        // filter pills
        LinearLayout fRow = new LinearLayout(this);
        fRow.setOrientation(LinearLayout.HORIZONTAL);
        fDead = mkButton("DEAD (0)", false, v -> { filter = 0; renderList(); markFilter(); });
        fAlive = mkButton("ALIVE (0)", false, v -> { filter = 1; renderList(); markFilter(); });
        fAll = mkButton("ALL (0)", false, v -> { filter = 2; renderList(); markFilter(); });
        addRowButtons(fRow, fDead, fAlive, fAll);
        root.addView(fRow);

        ScrollView sv = new ScrollView(this);
        LinearLayout.LayoutParams svlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        svlp.topMargin = dp(8);
        listBox = new LinearLayout(this);
        listBox.setOrientation(LinearLayout.VERTICAL);
        sv.addView(listBox);
        root.addView(sv, svlp);

        setContentView(root);
        markFilter();
        updateButtons();
    }

    int dp(int v) {
        return Theme.dp(this, v);
    }

    void addRowButtons(LinearLayout row, Button... btns) {
        for (Button b : btns) {
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            blp.rightMargin = dp(8);
            row.addView(b, blp);
        }
    }

    Button mkButton(String t, boolean primary, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(t);
        b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        b.setPadding(dp(8), dp(12), dp(8), dp(12));
        b.setTag(primary ? "p" : "s");
        b.setOnClickListener(l);
        styleBtn(b, primary, true);
        // remove default min sizes so rows stay compact
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        return b;
    }

    void styleBtn(Button b, boolean primary, boolean enabled) {
        b.setEnabled(enabled);
        boolean isPrimary = "p".equals(b.getTag()) && primary;
        if (use3d) {
            b.setShadowLayer(2, 0, 1, 0x80000000);
            if (!enabled) {
                b.setBackground(Theme.glossy3d(this, 0xFF707070, 0xFF484848, 18));
                b.setTextColor(0xFFCFCFCF);
            } else if (isPrimary) {
                b.setBackground(Theme.glossy3d(this, Theme.COPPER_TOP, Theme.COPPER_BOT, 18));
                b.setTextColor(Color.WHITE);
            } else {
                b.setBackground(Theme.glossy3d(this, Theme.SILVER_TOP, Theme.SILVER_BOT, 18));
                b.setTextColor(0xFF3A3A3A);
            }
        } else {
            b.setShadowLayer(0, 0, 0, 0);
            if (!enabled) {
                b.setBackground(Theme.disabledBtn(this));
                b.setTextColor(Theme.MUTED);
            } else if (isPrimary) {
                b.setBackground(Theme.primaryBtn(this));
                b.setTextColor(Color.WHITE);
            } else {
                b.setBackground(Theme.outlineBtn(this));
                b.setTextColor(Theme.INK);
            }
        }
    }

    void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    // ---------- loading ----------

    void loadFromUrl() {
        final String u = urlInput.getText().toString().trim();
        if (u.isEmpty()) {
            toast("Enter a URL");
            return;
        }
        sp.edit().putString("last_url", u).apply();
        setBusy(true);
        statusText.setText("Loading playlist…");
        new Thread(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
                c.setConnectTimeout(15000);
                c.setReadTimeout(60000);
                c.setRequestProperty("User-Agent", "Mozilla/5.0");
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(c.getInputStream(), "UTF-8"));
                final List<M3u.Channel> list = M3u.parse(br);
                br.close();
                runOnUiThread(() -> {
                    setBusy(false);
                    onPlaylist(list);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setBusy(false);
                    statusText.setText("");
                    toast("Load failed: " + e.getMessage());
                });
            }
        }).start();
    }

    void pickFile() {
        Intent it = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        it.setType("*/*");
        it.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(it, 41);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 41 && res == RESULT_OK && data != null && data.getData() != null) {
            final Uri uri = data.getData();
            setBusy(true);
            statusText.setText("Reading file…");
            new Thread(() -> {
                try (InputStream in = getContentResolver().openInputStream(uri);
                     BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"))) {
                    final List<M3u.Channel> list = M3u.parse(br);
                    runOnUiThread(() -> {
                        setBusy(false);
                        onPlaylist(list);
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        setBusy(false);
                        statusText.setText("");
                        toast("Read failed: " + e.getMessage());
                    });
                }
            }).start();
        }
    }

    void onPlaylist(List<M3u.Channel> list) {
        stopCheck();
        channels = list;
        statusText.setText("");
        progress.setProgress(0);
        progress.setMax(Math.max(1, list.size()));
        infoText.setText("Channels: " + list.size());
        renderList();
        markFilter();
        updateButtons();
        toast(list.size() + " channels loaded — tap CHECK");
    }

    void setBusy(boolean busy) {
        loadBtn.setEnabled(!busy);
        fileBtn.setEnabled(!busy);
        urlInput.setEnabled(!busy);
        styleBtn(loadBtn, true, !busy);
        styleBtn(fileBtn, false, !busy);
    }

    // ---------- checking ----------

    void startCheck() {
        if (channels.isEmpty()) {
            toast("Load a playlist first");
            return;
        }
        if (checking.get()) return;
        checking.set(true);
        for (M3u.Channel ch : channels) ch.checked = false;
        final int n = channels.size();
        doneCount.set(0);
        okCount.set(0);
        badCount.set(0);
        progress.setMax(n);
        progress.setProgress(0);
        updateButtons();
        statusText.setText("Checking… 0/" + n);
        final AtomicLong lastUi = new AtomicLong(0);
        int threads = Math.max(1, Math.min(20, sp.getInt("threads", 10)));
        pool = Executors.newFixedThreadPool(threads);
        for (M3u.Channel ch : channels) {
            pool.execute(() -> {
                if (!checking.get()) return;
                long t = SystemClock.uptimeMillis();
                boolean alive = probe(ch.url);
                ch.ms = (int) (SystemClock.uptimeMillis() - t);
                ch.alive = alive;
                ch.checked = true;
                if (alive) okCount.incrementAndGet();
                else badCount.incrementAndGet();
                int d = doneCount.incrementAndGet();
                long now = SystemClock.uptimeMillis();
                boolean isLast = d >= n;
                // THROTTLE: at most one UI post per 300ms — this was the ANR cause.
                if (isLast || now - lastUi.get() > 300) {
                    lastUi.set(now);
                    int dd = d, ok = okCount.get(), bad = badCount.get();
                    runOnUiThread(() -> {
                        if (!checking.get() && !isLast) return;
                        progress.setProgress(dd);
                        statusText.setText("Checking… " + dd + "/" + n
                                + "   ✅ " + ok + "   ❌ " + bad);
                        if (isLast) finishCheck();
                    });
                }
            });
        }
        pool.shutdown();
    }

    void finishCheck() {
        checking.set(false);
        updateButtons();
        renderList();
        markFilter();
        statusText.setText("Done ✅ " + okCount.get() + "   ❌ " + badCount.get());
        toast("Done — EXPORT saves the alive-only list");
    }

    void stopCheck() {
        boolean was = checking.getAndSet(false);
        if (pool != null) pool.shutdownNow();
        updateButtons();
        if (was) {
            statusText.setText("Stopped — ✅ " + okCount.get()
                    + "   ❌ " + badCount.get()
                    + "   (" + doneCount.get() + "/" + channels.size() + ")");
            renderList();
            markFilter();
        }
    }

    boolean probe(String url) {
        int to = Math.max(3, Math.min(30, sp.getInt("timeout_sec", 8))) * 1000;
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(to);
            c.setReadTimeout(to);
            c.setInstanceFollowRedirects(true);
            c.setRequestProperty("User-Agent", "Mozilla/5.0");
            c.setRequestProperty("Range", "bytes=0-0");
            int code = c.getResponseCode();
            return code == 200 || code == 206;
        } catch (Exception e) {
            return false;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    void updateButtons() {
        boolean run = checking.get();
        boolean has = !channels.isEmpty();
        styleBtn(checkBtn, true, !run && has);
        styleBtn(stopBtn, false, run);
        styleBtn(exportBtn, false, !run && has);
        styleBtn(clearBtn, false, !run && has);
    }

    // ---------- clear results ----------

    void clearResults() {
        if (channels.isEmpty()) {
            toast("Nothing to clear");
            return;
        }
        stopCheck();
        for (M3u.Channel ch : channels) {
            ch.checked = false;
            ch.ms = 0;
        }
        doneCount.set(0);
        okCount.set(0);
        badCount.set(0);
        statusText.setText("");
        progress.setProgress(0);
        renderList();
        markFilter();
        toast("Results cleared");
    }

    // ---------- list ----------

    void markFilter() {
        int dead = 0, alive = 0;
        for (M3u.Channel ch : channels) {
            if (!ch.checked) continue;
            if (ch.alive) alive++;
            else dead++;
        }
        fDead.setText("DEAD (" + dead + ")");
        fAlive.setText("ALIVE (" + alive + ")");
        fAll.setText("ALL (" + channels.size() + ")");
        stylePill(fDead, filter == 0, Theme.PILL_RED_TOP, Theme.PILL_RED_BOT);
        stylePill(fAlive, filter == 1, Theme.PILL_GREEN_TOP, Theme.PILL_GREEN_BOT);
        stylePill(fAll, filter == 2, Theme.PILL_GRAY_TOP, Theme.PILL_GRAY_BOT);
    }

    void stylePill(Button b, boolean selected, int top, int bot) {
        b.setEnabled(true);
        if (use3d) {
            b.setShadowLayer(2, 0, 1, 0x80000000);
            if (selected) {
                b.setBackground(Theme.glossy3d(this, top, bot, 16));
                b.setTextColor(Color.WHITE);
            } else {
                b.setBackground(Theme.glossy3d(this, Theme.PILL_OFF_TOP, Theme.PILL_OFF_BOT, 16));
                b.setTextColor(0xFFDCDCDC);
            }
        } else {
            b.setShadowLayer(0, 0, 0, 0);
            if (selected) {
                b.setBackground(Theme.primaryBtn(this));
                b.setTextColor(Color.WHITE);
            } else {
                b.setBackground(Theme.outlineBtn(this));
                b.setTextColor(Theme.INK);
            }
        }
    }

    void renderList() {
        listBox.removeAllViews();
        int shown = 0, matched = 0;
        for (M3u.Channel ch : channels) {
            boolean show;
            if (filter == 0) show = ch.checked && !ch.alive;
            else if (filter == 1) show = ch.checked && ch.alive;
            else show = true;
            if (!show) continue;
            matched++;
            if (shown >= LIST_CAP) continue;
            shown++;
            listBox.addView(row(ch));
        }
        if (matched > shown) {
            TextView t = new TextView(this);
            t.setTextColor(Theme.MUTED);
            t.setGravity(Gravity.CENTER);
            t.setPadding(dp(4), dp(12), dp(4), dp(12));
            t.setText("… +" + (matched - shown) + " more (use filters)");
            listBox.addView(t);
        }
        if (matched == 0) {
            TextView t = new TextView(this);
            t.setTextColor(Theme.MUTED);
            t.setPadding(dp(4), dp(16), dp(4), dp(16));
            t.setGravity(Gravity.CENTER);
            t.setText(channels.isEmpty() ? "—" : "Nothing here");
            listBox.addView(t);
        }
    }

    View row(M3u.Channel ch) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setBackground(Theme.cardBg(this));
        r.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(6);
        r.setLayoutParams(lp);

        TextView dot = new TextView(this);
        if (!ch.checked) {
            dot.setText("•");
            dot.setTextColor(Theme.MUTED);
        } else if (ch.alive) {
            dot.setText("✅");
        } else {
            dot.setText("❌");
        }
        dot.setPadding(0, 0, dp(10), 0);
        r.addView(dot);

        TextView name = new TextView(this);
        name.setText(ch.name);
        name.setTextColor(ch.checked && !ch.alive ? Theme.MUTED : Theme.INK);
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        r.addView(name, nlp);

        if (!ch.group.isEmpty()) {
            TextView grp = new TextView(this);
            grp.setText(ch.group);
            grp.setTextColor(Theme.MUTED);
            grp.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            grp.setPadding(dp(8), 0, dp(8), 0);
            grp.setMaxWidth(dp(120));
            grp.setSingleLine(true);
            r.addView(grp);
        }

        TextView ms = new TextView(this);
        ms.setText(ch.checked ? ch.ms + "ms" : "");
        ms.setTextColor(Theme.MUTED);
        ms.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        r.addView(ms);

        r.setOnClickListener(v -> toast(ch.url));
        return r;
    }

    // ---------- export ----------

    void exportAlive() {
        List<M3u.Channel> alive = new ArrayList<>();
        for (M3u.Channel ch : channels) {
            if (ch.checked && ch.alive) alive.add(ch);
        }
        if (alive.isEmpty()) {
            toast("No alive streams (run CHECK first)");
            return;
        }
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(new Date());
        final String fname = "StreamCheck-cleaned-" + stamp + ".m3u";
        final String data = M3u.buildClean(alive);
        new Thread(() -> {
            try {
                ContentValues v = new ContentValues();
                v.put(MediaStore.Downloads.DISPLAY_NAME, fname);
                v.put(MediaStore.Downloads.MIME_TYPE, "audio/x-mpegurl");
                v.put(MediaStore.Downloads.RELATIVE_PATH, "Download/");
                v.put(MediaStore.Downloads.IS_PENDING, 1);
                Uri uri = getContentResolver().insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                    os.write(data.getBytes("UTF-8"));
                }
                v.clear();
                v.put(MediaStore.Downloads.IS_PENDING, 0);
                getContentResolver().update(uri, v, null, null);
                final int n = alive.size();
                runOnUiThread(() -> toast("💾 Downloads/" + fname + " (" + n + ")"));
            } catch (Exception e) {
                runOnUiThread(() -> toast("Export failed: " + e.getMessage()));
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // pick up a changed UI style from Settings
        boolean want3d = "3d".equals(sp.getString("ui_style", "3d"));
        if (want3d != use3d) recreate();
    }

    @Override
    protected void onDestroy() {
        stopCheck();
        super.onDestroy();
    }
}
