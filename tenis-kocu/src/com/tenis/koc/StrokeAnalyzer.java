package com.tenis.koc;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Kare kare gelen vücut iskeletinden vuruşları yakalar ve teknik hataları çıkarır.
 *
 * - Vuruş kolunun bileği omuz ortasına göre izlenir ve gövde boyuna (omuz–kalça) göre normalize edilir;
 *   böylece kameraya uzaklık ve oyuncunun koşması sonucu etkilemez.
 * - Bilek hızı eşiği geçince "salınım" başlar; hız düşünce bitiş pozisyonu için kısa süre daha izlenir.
 * - En yüksek hız anı temas anı kabul edilir; öncesi hazırlık, sonrası vuruş sonu olarak değerlendirilir.
 *
 * Eşikler sezgiseldir; sonuçlar bir koçun gözünün yerini tutmaz, eğilimleri gösterir.
 */
public class StrokeAnalyzer {

    public interface Listener {
        void onStroke(StrokeResult r);
    }

    private static class Feat {
        Frame f;
        float torso, wx, wy, proj, sw, speed, raw;
        Float headY;
    }

    private static final int IDLE = 0, SWING = 1, POST = 2;

    private final int wristId, elbowId, shoulderId, otherShoulderId;
    private final float startThr;
    private final Listener listener;

    private final ArrayDeque<Feat> buf = new ArrayDeque<Feat>();
    private int state = IDLE;
    private long swingStart = 0, postUntil = 0, lastEnd = -10000;
    private Feat peak = null;
    private float smooth = 0;
    private boolean lastBodyVisible = false;

    public StrokeAnalyzer(boolean leftHanded, int sensitivity, Listener listener) {
        wristId = leftHanded ? Frame.L_WRIST : Frame.R_WRIST;
        elbowId = leftHanded ? Frame.L_ELBOW : Frame.R_ELBOW;
        shoulderId = leftHanded ? Frame.L_SHOULDER : Frame.R_SHOULDER;
        otherShoulderId = leftHanded ? Frame.R_SHOULDER : Frame.L_SHOULDER;
        startThr = sensitivity == 1 ? 8f : (sensitivity == 3 ? 4f : 5.5f);
        this.listener = listener;
    }

    /** Son karede vücudun (ayak bilekleri dahil) görünüp görünmediği. */
    public boolean isBodyVisible() {
        return lastBodyVisible;
    }

    public void process(Frame frame) {
        if (frame == null) {
            lastBodyVisible = false;
            return;
        }
        boolean core = frame.has(Frame.L_SHOULDER, Frame.R_SHOULDER, Frame.L_HIP, Frame.R_HIP);
        lastBodyVisible = core && frame.has(Frame.L_ANKLE, Frame.R_ANKLE);
        if (!core || !frame.ok[wristId]) return;

        float sx = (frame.xs[Frame.L_SHOULDER] + frame.xs[Frame.R_SHOULDER]) / 2f;
        float sy = (frame.ys[Frame.L_SHOULDER] + frame.ys[Frame.R_SHOULDER]) / 2f;
        float hx = (frame.xs[Frame.L_HIP] + frame.xs[Frame.R_HIP]) / 2f;
        float hy = (frame.ys[Frame.L_HIP] + frame.ys[Frame.R_HIP]) / 2f;
        float torso = (float) Math.hypot(sx - hx, sy - hy);
        if (torso < 10f) return;

        Feat ft = new Feat();
        ft.f = frame;
        ft.torso = torso;
        ft.wx = (frame.xs[wristId] - sx) / torso;
        ft.wy = (frame.ys[wristId] - sy) / torso;
        float shW = frame.dist(shoulderId, otherShoulderId);
        float ux = 0, uy = 0;
        if (shW > 1f) {
            ux = (frame.xs[shoulderId] - frame.xs[otherShoulderId]) / shW;
            uy = (frame.ys[shoulderId] - frame.ys[otherShoulderId]) / shW;
        }
        ft.proj = ft.wx * ux + ft.wy * uy;
        ft.sw = shW / torso;
        ft.headY = frame.ok[Frame.NOSE] ? (frame.ys[Frame.NOSE] - sy) / torso : null;

        Feat prev = buf.peekLast();
        if (prev != null) {
            float dt = Math.max(1L, frame.t - prev.f.t) / 1000f;
            if (dt < 0.4f) {
                ft.raw = (float) Math.hypot(ft.wx - prev.wx, ft.wy - prev.wy) / dt;
                smooth = 0.75f * ft.raw + 0.25f * smooth;
            } else {
                smooth = 0;
            }
        }
        ft.speed = smooth;
        buf.addLast(ft);
        while (!buf.isEmpty() && buf.peekFirst().f.t < frame.t - 3000) buf.removeFirst();
        step(ft);
    }

    private void step(Feat ft) {
        long t = ft.f.t;
        if (state == IDLE) {
            if (ft.speed > startThr && t - lastEnd > 400) {
                state = SWING;
                swingStart = t;
                peak = ft;
            }
        } else if (state == SWING) {
            if (peak == null || ft.raw >= peak.raw) peak = ft;
            if (ft.speed < startThr * 0.4f || t - swingStart > 1000) {
                state = POST;
                postUntil = t + 300;
            }
        } else if (t >= postUntil) {
            finish(t);
        }
    }

    private void finish(long t) {
        state = IDLE;
        lastEnd = t;
        Feat pk = peak;
        peak = null;
        if (pk == null || pk.raw < startThr * 1.4f) return;

        List<Feat> window = new ArrayList<Feat>(), pre = new ArrayList<Feat>(), post = new ArrayList<Feat>();
        for (Feat x : buf) {
            if (x.f.t < swingStart - 800) continue;
            window.add(x);
            if (x.f.t <= pk.f.t) pre.add(x);
            if (x.f.t >= pk.f.t) post.add(x);
        }
        if (pre.size() < 2 || post.size() < 2) return;

        // Hazırlığın en uç noktası: temas anındaki bilekten en uzak nokta.
        Feat back = pre.get(0);
        float arc = -1;
        for (Feat x : pre) {
            float d = (float) Math.hypot(x.wx - pk.wx, x.wy - pk.wy);
            if (d > arc) { arc = d; back = x; }
        }
        // Toplam salınım genliği: hazırlık ucundan bitişteki en uzak noktaya
        float amp = 0;
        for (Feat x : post) amp = Math.max(amp, (float) Math.hypot(x.wx - back.wx, x.wy - back.wy));
        if (amp < 0.8f) return; // küçük kol hareketi, vuruş değil

        StrokeResult r = evaluate(pk, window, pre, post, back, arc);
        if (listener != null) listener.onStroke(r);
    }

    private StrokeResult evaluate(Feat pk, List<Feat> window, List<Feat> pre, List<Feat> post, Feat back, float arc) {
        List<Fault> faults = new ArrayList<Fault>();

        // Bileğin burnun üstüne çıktığı en yüksek nokta (+: başın üstünde)
        float reach = -99;
        for (Feat x : window) {
            if (Math.abs(x.f.t - pk.f.t) > 250) continue;
            float head = x.headY != null ? x.headY : -0.45f;
            reach = Math.max(reach, head - x.wy);
        }
        StrokeType type;
        if (reach > 0.2f) type = StrokeType.SERVE;
        else if (back.proj - post.get(post.size() - 1).proj >= 0) type = StrokeType.FOREHAND;
        else type = StrokeType.BACKHAND;

        Float minKnee = null;
        for (Feat x : window) {
            if (x.f.t > pk.f.t + 100) continue;
            Float a = knee(x.f, Frame.L_HIP, Frame.L_KNEE, Frame.L_ANKLE);
            Float b = knee(x.f, Frame.R_HIP, Frame.R_KNEE, Frame.R_ANKLE);
            if (a != null && (minKnee == null || a < minKnee)) minKnee = a;
            if (b != null && (minKnee == null || b < minKnee)) minKnee = b;
        }
        Float elbow = pk.f.has(shoulderId, elbowId, wristId) ? pk.f.angle(shoulderId, elbowId, wristId) : null;

        if (type == StrokeType.SERVE) {
            if (minKnee != null && minKnee > 160f) faults.add(Fault.SERVE_NO_LEGS);
            if (elbow != null && elbow < 145f) faults.add(Fault.SERVE_ARM_BENT);
            if (reach < 0.55f) faults.add(Fault.LOW_SERVE_CONTACT);
        } else {
            float side = type == StrokeType.FOREHAND ? 1f : -1f;
            if (minKnee != null && minKnee > 160f) faults.add(Fault.KNEES_STRAIGHT);
            if (elbow != null && elbow < 90f) faults.add(Fault.ELBOW_CRAMPED);

            // Omuz dönüşü: hazırlıkta görünen omuz genişliği belirgin şekilde daralmalı
            float widthRef = 0;
            for (Feat x : buf) if (x.f.t >= swingStart - 1500) widthRef = Math.max(widthRef, x.sw);
            float minPre = 99;
            for (Feat x : pre) minPre = Math.min(minPre, x.sw);
            boolean turnReliable = widthRef >= 0.4f;
            if (turnReliable && minPre / widthRef > 0.8f) faults.add(Fault.NO_UNIT_TURN);

            // Hazırlık: önden/arkadan çekimde bilek vücudun vuruş tarafında yeterince geride olmalı;
            // yandan çekimde hazırlık ucunun temas noktasına uzaklığına bakılır.
            boolean shortBack = turnReliable ? side * back.proj < 1.1f : arc < 1.0f;
            if (shortBack) faults.add(Fault.SHORT_BACKSWING);

            // Vuruş sonu: bilek omuz hizasına çıkmalı ve vücudun karşı tarafına geçmeli
            float highest = 99;
            boolean crossed = false;
            for (Feat x : post) {
                highest = Math.min(highest, x.wy);
                if (side * x.proj < -0.15f) crossed = true;
            }
            boolean high = highest < 0.1f;
            if (!high || (turnReliable && !crossed)) faults.add(Fault.NO_FOLLOW_THROUGH);

            if (pk.f.has(Frame.L_ANKLE, Frame.R_ANKLE)) {
                float stance = pk.f.dist(Frame.L_ANKLE, Frame.R_ANKLE) / pk.torso;
                if (stance < 0.55f) faults.add(Fault.NARROW_STANCE);
                else if (stance > 2.3f) faults.add(Fault.WIDE_STANCE);
            }

            // Baş stabilitesi: burnun kalçaya göre konumu temas çevresinde fazla oynamamalı
            List<float[]> heads = new ArrayList<float[]>();
            for (Feat x : window) {
                if (x.f.t < pk.f.t - 400 || x.f.t > pk.f.t + 200 || !x.f.ok[Frame.NOSE]) continue;
                float hx = (x.f.xs[Frame.L_HIP] + x.f.xs[Frame.R_HIP]) / 2f;
                float hy = (x.f.ys[Frame.L_HIP] + x.f.ys[Frame.R_HIP]) / 2f;
                heads.add(new float[]{(x.f.xs[Frame.NOSE] - hx) / x.torso, (x.f.ys[Frame.NOSE] - hy) / x.torso});
            }
            if (heads.size() >= 3) {
                float mx = 0, my = 0;
                for (float[] h : heads) { mx += h[0]; my += h[1]; }
                mx /= heads.size();
                my /= heads.size();
                float spread = 0;
                for (float[] h : heads) spread = Math.max(spread, (float) Math.hypot(h[0] - mx, h[1] - my));
                if (spread > 0.4f) faults.add(Fault.HEAD_MOVING);
            }
        }

        int score = 100;
        for (Fault f : faults) score -= f.penalty;
        score = Math.max(0, Math.min(100, score));
        Collections.sort(faults, new Comparator<Fault>() {
            @Override
            public int compare(Fault a, Fault b) {
                return b.penalty - a.penalty;
            }
        });
        return new StrokeResult(pk.f.t, type, score, faults, pk.raw);
    }

    private static Float knee(Frame f, int hip, int knee, int ankle) {
        return f.has(hip, knee, ankle) ? f.angle(hip, knee, ankle) : null;
    }
}
