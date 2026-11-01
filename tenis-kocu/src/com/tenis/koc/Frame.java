package com.tenis.koc;

/** Tek bir karedeki 33 vücut noktası (görüntü pikseli cinsinden). t: oturum başından beri ms. */
public class Frame {
    public static final int COUNT = 33;
    public static final int NOSE = 0, L_SHOULDER = 11, R_SHOULDER = 12, L_ELBOW = 13, R_ELBOW = 14,
        L_WRIST = 15, R_WRIST = 16, L_HIP = 23, R_HIP = 24, L_KNEE = 25, R_KNEE = 26, L_ANKLE = 27, R_ANKLE = 28;

    public static final int[][] BONES = {
        {L_SHOULDER, R_SHOULDER}, {L_SHOULDER, L_ELBOW}, {L_ELBOW, L_WRIST}, {R_SHOULDER, R_ELBOW},
        {R_ELBOW, R_WRIST}, {L_SHOULDER, L_HIP}, {R_SHOULDER, R_HIP}, {L_HIP, R_HIP}, {L_HIP, L_KNEE},
        {L_KNEE, L_ANKLE}, {R_HIP, R_KNEE}, {R_KNEE, R_ANKLE},
    };
    public static final int[] JOINTS = {NOSE, L_SHOULDER, R_SHOULDER, L_ELBOW, R_ELBOW, L_WRIST, R_WRIST,
        L_HIP, R_HIP, L_KNEE, R_KNEE, L_ANKLE, R_ANKLE};

    public final long t;
    public final float[] xs, ys;
    public final boolean[] ok;

    public Frame(long t, float[] xs, float[] ys, boolean[] ok) {
        this.t = t; this.xs = xs; this.ys = ys; this.ok = ok;
    }

    public boolean has(int... ids) {
        for (int i : ids) if (!ok[i]) return false;
        return true;
    }

    public float dist(int a, int b) {
        return (float) Math.hypot(xs[a] - xs[b], ys[a] - ys[b]);
    }

    /** b noktasındaki açı (derece). */
    public float angle(int a, int b, int c) {
        float v1x = xs[a] - xs[b], v1y = ys[a] - ys[b], v2x = xs[c] - xs[b], v2y = ys[c] - ys[b];
        double m = Math.hypot(v1x, v1y) * Math.hypot(v2x, v2y);
        if (m < 1e-3) return 180f;
        double cos = Math.max(-1, Math.min(1, (v1x * v2x + v1y * v2y) / m));
        return (float) Math.toDegrees(Math.acos(cos));
    }
}
