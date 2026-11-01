package com.tenis.koc;

import android.app.Activity;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** Canlı kamera antrenmanı: her vuruşu yakalar, anında geri bildirim verir. */
@SuppressWarnings("deprecation")
public class LiveActivity extends Activity implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private Store store;
    private final List<StrokeResult> strokes = new ArrayList<StrokeResult>();
    private long startWall, startClock;

    private Overlays.AspectFrame frameBox;
    private SurfaceView surface;
    private Overlays.SkeletonView skeleton;
    private TextView timer, count, warn, types, lastTitle, lastBody, voiceBtn;
    private LinearLayout lastBox;
    private Overlays.VoiceCoach coach;
    private boolean voiceOn;

    private HandlerThread camThread, poseThread;
    private Handler camHandler, poseHandler;
    private final Handler ui = new Handler();
    private volatile Camera camera;
    private volatile boolean busy, surfaceReady, resumed;
    private int camId = -1, rotation, prevW, prevH;
    private boolean front;

    private PoseEstimator estimator;
    private StrokeAnalyzer analyzer;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        store = Store.get(this);
        voiceOn = store.voice();
        coach = new Overlays.VoiceCoach(this);
        startWall = System.currentTimeMillis();
        startClock = SystemClock.elapsedRealtime();
        buildUi();

        analyzer = new StrokeAnalyzer(store.leftHanded(), store.sensitivity(), new StrokeAnalyzer.Listener() {
            @Override
            public void onStroke(final StrokeResult r) {
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        onStrokeUi(r);
                    }
                });
            }
        });

        camThread = new HandlerThread("camera");
        camThread.start();
        camHandler = new Handler(camThread.getLooper());
        poseThread = new HandlerThread("pose");
        poseThread.start();
        poseHandler = new Handler(poseThread.getLooper());
        poseHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    estimator = new PoseEstimator(LiveActivity.this);
                } catch (final Throwable e) {
                    ui.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(LiveActivity.this, "Model yüklenemedi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
        ui.post(tick);
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        frameBox = new Overlays.AspectFrame(this);
        surface = new SurfaceView(this);
        surface.getHolder().addCallback(this);
        skeleton = new Overlays.SkeletonView(this);
        frameBox.addView(surface, new FrameLayout.LayoutParams(-1, -1));
        frameBox.addView(skeleton, new FrameLayout.LayoutParams(-1, -1));
        root.addView(frameBox, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));

        LinearLayout hud = Ui.vbox(this);
        int p = Ui.dp(this, 12);
        hud.setPadding(p, Ui.dp(this, 28), p, Ui.dp(this, 20));

        LinearLayout top = Ui.hbox(this);
        timer = pill("⏱ 00:00");
        count = pill("🎾 0 vuruş");
        top.addView(timer);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.leftMargin = Ui.dp(this, 8);
        top.addView(count, lp);
        View sp = new View(this);
        top.addView(sp, Ui.weight(1));
        voiceBtn = pill(voiceOn ? "🔊" : "🔇");
        voiceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                voiceOn = !voiceOn;
                voiceBtn.setText(voiceOn ? "🔊" : "🔇");
            }
        });
        top.addView(voiceBtn);
        TextView flip = pill("🔄 Kamera");
        flip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                front = !front;
                restartCamera();
            }
        });
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(-2, -2);
        lp2.leftMargin = Ui.dp(this, 8);
        top.addView(flip, lp2);
        hud.addView(top);

        warn = Ui.text(this, "Tüm vücudun (baştan ayak bileğine) görünecek şekilde geri çekil", 14, Color.WHITE, false);
        warn.setBackground(Ui.rounded(0xCCE65100, Ui.dp(this, 12)));
        warn.setPadding(p, p, p, p);
        hud.addView(warn, Ui.matchWrap(this, 8));

        View fill = new View(this);
        hud.addView(fill, new LinearLayout.LayoutParams(-1, 0, 1));

        lastBox = Ui.vbox(this);
        lastBox.setBackground(Ui.rounded(0xDD000000, Ui.dp(this, 16)));
        lastBox.setPadding(p, p, p, p);
        lastTitle = Ui.text(this, "", 16, Color.WHITE, true);
        lastBody = Ui.text(this, "", 14, 0xFFFFCC80, false);
        lastBox.addView(lastTitle);
        lastBox.addView(lastBody);
        lastBox.setVisibility(View.GONE);
        hud.addView(lastBox, Ui.matchWrap(this, 0));

        types = pill("FH 0 · BH 0 · Servis 0");
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(-2, -2);
        tl.topMargin = Ui.dp(this, 10);
        hud.addView(types, tl);

        Button stop = Ui.button(this, "Antrenmanı bitir ve özeti gör", 0xFFE53935, Color.WHITE);
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishSession();
            }
        });
        hud.addView(stop, Ui.matchWrap(this, 10));

        root.addView(hud, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
    }

    private TextView pill(String s) {
        TextView t = Ui.text(this, s, 14, Color.WHITE, false);
        t.setBackground(Ui.rounded(0x99000000, Ui.dp(this, 50)));
        t.setPadding(Ui.dp(this, 12), Ui.dp(this, 7), Ui.dp(this, 12), Ui.dp(this, 7));
        return t;
    }

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            timer.setText("⏱ " + Store.formatClock(SystemClock.elapsedRealtime() - startClock));
            ui.postDelayed(this, 500);
        }
    };

    private void onStrokeUi(StrokeResult r) {
        strokes.add(r);
        int fh = 0, bh = 0, sv = 0;
        for (StrokeResult s : strokes) {
            if (s.type == StrokeType.FOREHAND) fh++;
            else if (s.type == StrokeType.BACKHAND) bh++;
            else sv++;
        }
        count.setText("🎾 " + strokes.size() + " vuruş");
        types.setText("FH " + fh + " · BH " + bh + " · Servis " + sv);
        lastBox.setVisibility(View.VISIBLE);
        lastTitle.setText("Son vuruş: " + r.type.label + " · " + r.score + " puan");
        lastTitle.setTextColor(Ui.scoreColor(r.score));
        if (r.faults.isEmpty()) {
            lastBody.setText("Teknik olarak temiz, böyle devam!");
            lastBody.setTextColor(0xFFC6FF00);
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(2, r.faults.size()); i++) sb.append("• ").append(r.faults.get(i).title).append('\n');
            sb.append(r.faults.get(0).tip);
            lastBody.setText(sb.toString());
            lastBody.setTextColor(0xFFFFCC80);
        }
        if (voiceOn) coach.react(r);
    }

    private void finishSession() {
        if (strokes.isEmpty()) {
            Toast.makeText(this, "Hiç vuruş algılanmadı. Tüm vücudun kadrajda mı?", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        Session s = new Session(startWall, startWall, SystemClock.elapsedRealtime() - startClock, "Canlı kamera",
            new ArrayList<StrokeResult>(strokes));
        store.add(s);
        DetailActivity.open(this, s.id);
        finish();
    }

    // ---------------- Kamera ----------------

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        if (surfaceReady) startCamera();
    }

    @Override
    protected void onPause() {
        resumed = false;
        stopCamera();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        ui.removeCallbacks(tick);
        coach.shutdown();
        camThread.quitSafely();
        poseHandler.post(new Runnable() {
            @Override
            public void run() {
                if (estimator != null) estimator.close();
                estimator = null;
            }
        });
        poseThread.quitSafely();
        super.onDestroy();
    }

    @Override
    public void surfaceCreated(SurfaceHolder h) {
        surfaceReady = true;
        if (resumed) startCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder h, int f, int w, int hh) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder h) {
        surfaceReady = false;
        stopCamera();
    }

    private void restartCamera() {
        stopCamera();
        poseHandler.post(new Runnable() {
            @Override
            public void run() {
                if (estimator != null) estimator.reset();
            }
        });
        skeleton.set(null, 1, 1, front);
        if (surfaceReady && resumed) startCamera();
    }

    private void startCamera() {
        final SurfaceHolder holder = surface.getHolder();
        camHandler.post(new Runnable() {
            @Override
            public void run() {
                if (camera != null) return;
                try {
                    int n = Camera.getNumberOfCameras();
                    Camera.CameraInfo info = new Camera.CameraInfo();
                    int chosen = -1;
                    for (int i = 0; i < n; i++) {
                        Camera.getCameraInfo(i, info);
                        boolean isFront = info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT;
                        if (isFront == front) { chosen = i; break; }
                    }
                    if (chosen < 0) chosen = 0;
                    Camera.getCameraInfo(chosen, info);
                    camId = chosen;
                    final boolean isFront = info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT;
                    Camera c = Camera.open(chosen);
                    Camera.Parameters p = c.getParameters();
                    Camera.Size best = null;
                    for (Camera.Size s : p.getSupportedPreviewSizes()) {
                        int score = Math.abs(s.width - 960) + Math.abs(s.height - 720);
                        if (best == null || score < Math.abs(best.width - 960) + Math.abs(best.height - 720)) best = s;
                    }
                    if (best != null) p.setPreviewSize(best.width, best.height);
                    List<String> fm = p.getSupportedFocusModes();
                    if (fm != null && fm.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                        p.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                    }
                    c.setParameters(p);
                    Camera.Size ps = c.getParameters().getPreviewSize();
                    prevW = ps.width;
                    prevH = ps.height;
                    rotation = info.orientation;
                    int display = isFront ? (360 - info.orientation % 360) % 360 : info.orientation;
                    c.setDisplayOrientation(display);
                    c.setPreviewDisplay(holder);
                    int size = prevW * prevH * 3 / 2;
                    c.addCallbackBuffer(new byte[size]);
                    c.addCallbackBuffer(new byte[size]);
                    c.setPreviewCallbackWithBuffer(LiveActivity.this);
                    c.startPreview();
                    camera = c;
                    final float aspect = rotation % 180 == 0 ? prevW / (float) prevH : prevH / (float) prevW;
                    ui.post(new Runnable() {
                        @Override
                        public void run() {
                            frameBox.setAspect(aspect);
                        }
                    });
                } catch (final Exception e) {
                    ui.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(LiveActivity.this, "Kamera açılamadı: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }

    private void stopCamera() {
        camHandler.post(new Runnable() {
            @Override
            public void run() {
                Camera c = camera;
                camera = null;
                if (c != null) {
                    try {
                        c.setPreviewCallbackWithBuffer(null);
                        c.stopPreview();
                    } catch (Exception ignored) {
                    }
                    c.release();
                }
            }
        });
    }

    @Override
    public void onPreviewFrame(final byte[] data, final Camera cam) {
        if (busy || estimator == null) {
            cam.addCallbackBuffer(data);
            return;
        }
        busy = true;
        final int w = prevW, h = prevH, rot = rotation;
        final boolean mirror = front;
        poseHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    PoseEstimator est = estimator;
                    if (est == null) return;
                    PixelSource src = new PixelSource.Nv21(data, w, h, rot);
                    final Frame f = est.estimate(src, SystemClock.elapsedRealtime() - startClock);
                    analyzer.process(f);
                    final boolean visible = analyzer.isBodyVisible();
                    final int uw = src.width(), uh = src.height();
                    ui.post(new Runnable() {
                        @Override
                        public void run() {
                            skeleton.set(f, uw, uh, mirror);
                            warn.setVisibility(visible ? View.GONE : View.VISIBLE);
                        }
                    });
                } catch (Throwable ignored) {
                } finally {
                    busy = false;
                    camHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (camera == cam) cam.addCallbackBuffer(data);
                        }
                    });
                }
            }
        });
    }
}
