package com.tenis.koc;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** Telefonda kayıtlı bir antrenman videosunu kare kare analiz eder. */
public class VideoActivity extends Activity {
    private static final long STEP_MS = 80; // ~12 kare/sn
    private static final int MAX_SIDE = 640;

    private final Handler ui = new Handler();
    private volatile boolean cancelled;
    private ProgressBar bar;
    private TextView pct, title;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = Ui.vbox(this);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.WHITE);
        int p = Ui.dp(this, 24);
        root.setPadding(p, p, p, p);
        title = Ui.text(this, "Video analiz ediliyor…", 20, Ui.TEXT, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title, Ui.matchWrap(this, 0));
        bar = Ui.bar(this, 0, Ui.GREEN);
        root.addView(bar, Ui.matchWrap(this, 16));
        pct = Ui.body(this, "%0");
        pct.setGravity(Gravity.CENTER);
        root.addView(pct, Ui.matchWrap(this, 8));
        TextView note = Ui.small(this, "Her kare tek tek inceleniyor; uzun videolar birkaç dakika sürebilir. Ekranı açık tut.");
        note.setGravity(Gravity.CENTER);
        root.addView(note, Ui.matchWrap(this, 12));
        Button cancel = Ui.button(this, "İptal", 0xFFE8F5E9, Ui.GREEN_DARK);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelled = true;
                finish();
            }
        });
        root.addView(cancel, Ui.matchWrap(this, 24));
        setContentView(root);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        final Uri uri = getIntent().getData();
        new Thread(new Runnable() {
            @Override
            public void run() {
                analyze(uri);
            }
        }).start();
    }

    @Override
    public void onBackPressed() {
        cancelled = true;
        super.onBackPressed();
    }

    private void analyze(Uri uri) {
        final List<StrokeResult> strokes = new ArrayList<StrokeResult>();
        MediaMetadataRetriever mr = new MediaMetadataRetriever();
        PoseEstimator est = null;
        long duration = 0;
        try {
            est = new PoseEstimator(this);
            mr.setDataSource(this, uri);
            String d = mr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            duration = d == null ? 0 : Long.parseLong(d);
            if (duration <= 0) throw new IllegalArgumentException("Video süresi okunamadı");
            StrokeAnalyzer an = new StrokeAnalyzer(Store.get(this).leftHanded(), Store.get(this).sensitivity(),
                new StrokeAnalyzer.Listener() {
                    @Override
                    public void onStroke(StrokeResult r) {
                        strokes.add(r);
                    }
                });
            int[] px = null;
            for (long t = 0; t <= duration && !cancelled; t += STEP_MS) {
                Bitmap bm = mr.getFrameAtTime(t * 1000, MediaMetadataRetriever.OPTION_CLOSEST);
                if (bm != null) {
                    int side = Math.max(bm.getWidth(), bm.getHeight());
                    Bitmap sc = bm;
                    if (side > MAX_SIDE) {
                        float k = MAX_SIDE / (float) side;
                        sc = Bitmap.createScaledBitmap(bm, Math.round(bm.getWidth() * k), Math.round(bm.getHeight() * k), true);
                    }
                    int w = sc.getWidth(), h = sc.getHeight();
                    if (px == null || px.length != w * h) px = new int[w * h];
                    sc.getPixels(px, 0, w, 0, 0, w, h);
                    if (sc != bm) sc.recycle();
                    bm.recycle();
                    an.process(est.estimate(new PixelSource.Pixels(px, w, h), t));
                }
                final float prog = t / (float) duration;
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        bar.setProgress(Math.round(prog * 1000));
                        pct.setText("%" + Math.round(prog * 100) + " · " + strokes.size() + " vuruş");
                    }
                });
            }
        } catch (final Throwable e) {
            if (!cancelled) {
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        title.setText("Analiz başarısız: " + e.getMessage());
                    }
                });
            }
            return;
        } finally {
            try {
                mr.release();
            } catch (Exception ignored) {
            }
            if (est != null) est.close();
        }
        if (cancelled) return;
        final long dur = duration;
        ui.post(new Runnable() {
            @Override
            public void run() {
                if (strokes.isEmpty()) {
                    Toast.makeText(VideoActivity.this, "Videoda vuruş algılanmadı. Tüm vücut kadrajda mı?", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                long now = System.currentTimeMillis();
                Session s = new Session(now, now, dur, "Video", strokes);
                Store.get(VideoActivity.this).add(s);
                DetailActivity.open(VideoActivity.this, s.id);
                finish();
            }
        });
    }
}
