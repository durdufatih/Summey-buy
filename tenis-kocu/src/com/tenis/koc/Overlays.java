package com.tenis.koc;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.FrameLayout;

import java.util.Locale;
import java.util.Random;

/** Kamera önizlemesi için yardımcı görünümler ve sesli koç. */
public class Overlays {

    /** Çocuklarını verilen en-boy oranında, ebeveyne sığacak şekilde ortalar. */
    public static class AspectFrame extends FrameLayout {
        private float aspect = 3f / 4f; // genişlik / yükseklik

        public AspectFrame(Context c) {
            super(c);
        }

        public void setAspect(float a) {
            if (Math.abs(a - aspect) > 1e-3) {
                aspect = a;
                requestLayout();
            }
        }

        @Override
        protected void onMeasure(int wSpec, int hSpec) {
            int w = MeasureSpec.getSize(wSpec), h = MeasureSpec.getSize(hSpec);
            if (w <= 0 || h <= 0) {
                super.onMeasure(wSpec, hSpec);
                return;
            }
            int tw = w, th = Math.round(w / aspect);
            if (th > h) {
                th = h;
                tw = Math.round(h * aspect);
            }
            super.onMeasure(MeasureSpec.makeMeasureSpec(tw, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(th, MeasureSpec.EXACTLY));
        }
    }

    /** Vücut iskeletini çizer. Koordinatlar dik görüntü pikseli; görünüm boyutuna ölçeklenir. */
    public static class SkeletonView extends View {
        private final Paint bone = new Paint(Paint.ANTI_ALIAS_FLAG), joint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Frame frame;
        private int iw = 1, ih = 1;
        private boolean mirror;

        public SkeletonView(Context c) {
            super(c);
            bone.setColor(0xFFC6FF00);
            bone.setStrokeWidth(Ui.dp(c, 3.5f));
            bone.setStrokeCap(Paint.Cap.ROUND);
            joint.setColor(0xFFFFFFFF);
        }

        public void set(Frame f, int w, int h, boolean mirror) {
            frame = f;
            iw = Math.max(1, w);
            ih = Math.max(1, h);
            this.mirror = mirror;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas c) {
            Frame f = frame;
            if (f == null) return;
            float sx = getWidth() / (float) iw, sy = getHeight() / (float) ih;
            for (int[] b : Frame.BONES) {
                if (!f.ok[b[0]] || !f.ok[b[1]]) continue;
                c.drawLine(x(f, b[0], sx), f.ys[b[0]] * sy, x(f, b[1], sx), f.ys[b[1]] * sy, bone);
            }
            float r = Ui.dp(getContext(), 4);
            for (int j : Frame.JOINTS) if (f.ok[j]) c.drawCircle(x(f, j, sx), f.ys[j] * sy, r, joint);
        }

        private float x(Frame f, int i, float sx) {
            float x = f.xs[i] * sx;
            return mirror ? getWidth() - x : x;
        }
    }

    /** Her vuruştan sonra kısa Türkçe sesli geri bildirim. */
    public static class VoiceCoach implements TextToSpeech.OnInitListener {
        private final TextToSpeech tts;
        private boolean ready;
        private int goodStreak;
        private final Random rnd = new Random();
        private static final String[] PRAISE = {"Harika vuruş", "Çok iyi", "Böyle devam", "Mükemmel"};

        public VoiceCoach(Context c) {
            tts = new TextToSpeech(c.getApplicationContext(), this);
        }

        @Override
        public void onInit(int status) {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("tr", "TR"));
                ready = true;
            }
        }

        public void react(StrokeResult r) {
            if (!r.faults.isEmpty()) {
                goodStreak = 0;
                say(r.faults.get(0).cue);
            } else {
                goodStreak++;
                if (goodStreak % 3 == 1) say(PRAISE[rnd.nextInt(PRAISE.length)]);
            }
        }

        public void say(String s) {
            if (ready) tts.speak(s, TextToSpeech.QUEUE_FLUSH, null, "coach");
        }

        public void shutdown() {
            tts.stop();
            tts.shutdown();
        }
    }
}
