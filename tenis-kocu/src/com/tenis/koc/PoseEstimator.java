package com.tenis.koc;

import android.content.Context;
import android.content.res.AssetFileDescriptor;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

/**
 * Cihaz üzerinde 33 noktalı vücut iskeleti tespiti (MediaPipe BlazePose modelleri, TensorFlow Lite).
 * 1) Kişi bulunamadıysa tüm karede kişi dedektörü çalışır ve bir ilgi bölgesi (ROI) çıkarır.
 * 2) ROI döndürülüp 256x256 kırpılır, iskelet modeli 39 nokta verir.
 * 3) Sonraki karede ROI önceki iskeletten hesaplanır (takip); kişi kaybolursa dedektöre dönülür.
 */
public class PoseEstimator {
    private static final int DET = 224, LMK = 256, ANCHORS = 2254;

    private final Interpreter det, lmk;
    private final float[][] anchors = new float[ANCHORS][2];
    private final ByteBuffer detIn, lmkIn;
    private final float[][][] detBoxes = new float[1][ANCHORS][12];
    private final float[][][] detScores = new float[1][ANCHORS][1];
    private final float[][] lmkOut = new float[1][195];
    private final float[][] lmkFlag = new float[1][1];
    private final int lmkOutIdx, lmkFlagIdx, detBoxIdx, detScoreIdx;
    private float[] roi = null; // cx, cy, size, theta

    public PoseEstimator(Context ctx) throws IOException {
        Interpreter.Options o = new Interpreter.Options();
        o.setNumThreads(4);
        det = new Interpreter(load(ctx, "pose_detection.tflite"), o);
        Interpreter.Options o2 = new Interpreter.Options();
        o2.setNumThreads(4);
        lmk = new Interpreter(load(ctx, "pose_landmark_full.tflite"), o2);

        detIn = ByteBuffer.allocateDirect(DET * DET * 3 * 4).order(ByteOrder.nativeOrder());
        lmkIn = ByteBuffer.allocateDirect(LMK * LMK * 3 * 4).order(ByteOrder.nativeOrder());

        int a = -1, b = -1;
        for (int i = 0; i < det.getOutputTensorCount(); i++) {
            int[] s = det.getOutputTensor(i).shape();
            if (s.length == 3 && s[2] == 12) a = i;
            else if (s.length == 3 && s[2] == 1) b = i;
        }
        detBoxIdx = a; detScoreIdx = b;
        a = -1; b = -1;
        for (int i = 0; i < lmk.getOutputTensorCount(); i++) {
            int[] s = lmk.getOutputTensor(i).shape();
            if (s.length == 2 && s[1] == 195) a = i;
            else if (s.length == 2 && s[1] == 1) b = i;
        }
        lmkOutIdx = a; lmkFlagIdx = b;
        buildAnchors();
    }

    private static MappedByteBuffer load(Context ctx, String name) throws IOException {
        AssetFileDescriptor fd = ctx.getAssets().openFd(name);
        FileInputStream in = new FileInputStream(fd.getFileDescriptor());
        FileChannel ch = in.getChannel();
        return ch.map(FileChannel.MapMode.READ_ONLY, fd.getStartOffset(), fd.getDeclaredLength());
    }

    private void buildAnchors() {
        int[] strides = {8, 16, 32, 32, 32};
        int k = 0, i = 0;
        while (i < strides.length) {
            int n = 0, j = i;
            while (j < strides.length && strides[j] == strides[i]) { n += 2; j++; }
            int fm = (int) Math.ceil(DET / (double) strides[i]);
            for (int y = 0; y < fm; y++)
                for (int x = 0; x < fm; x++)
                    for (int q = 0; q < n; q++) {
                        anchors[k][0] = (x + 0.5f) / fm;
                        anchors[k][1] = (y + 0.5f) / fm;
                        k++;
                    }
            i = j;
        }
    }

    public void reset() {
        roi = null;
    }

    public void close() {
        det.close();
        lmk.close();
    }

    /** Kareyi işler; kişi yoksa null döner. */
    public Frame estimate(PixelSource src, long t) {
        if (roi == null) roi = detect(src);
        if (roi == null) return null;
        float cx = roi[0], cy = roi[1], size = roi[2], th = roi[3];
        fill(lmkIn, src, cx, cy, size, th, LMK, 0f, 1f);
        Map<Integer, Object> outs = new HashMap<Integer, Object>();
        outs.put(lmkOutIdx, lmkOut);
        outs.put(lmkFlagIdx, lmkFlag);
        lmk.runForMultipleInputsOutputs(new Object[]{lmkIn}, outs);
        if (lmkFlag[0][0] < 0.5f) {
            roi = null;
            return null;
        }
        float c = (float) Math.cos(th), s = (float) Math.sin(th);
        float[] px = new float[35], py = new float[35], vis = new float[35];
        for (int q = 0; q < 35; q++) {
            float a = (lmkOut[0][q * 5] / LMK - 0.5f) * size;
            float b = (lmkOut[0][q * 5 + 1] / LMK - 0.5f) * size;
            px[q] = cx + a * c - b * s;
            py[q] = cy + a * s + b * c;
            vis[q] = sigmoid(lmkOut[0][q * 5 + 3]);
        }
        roi = roiFrom(px[33], py[33], px[34], py[34]);

        float[] xs = new float[Frame.COUNT], ys = new float[Frame.COUNT];
        boolean[] ok = new boolean[Frame.COUNT];
        int w = src.width(), h = src.height();
        for (int q = 0; q < Frame.COUNT; q++) {
            xs[q] = px[q];
            ys[q] = py[q];
            ok[q] = vis[q] >= 0.5f && px[q] >= -0.05f * w && px[q] <= 1.05f * w && py[q] >= -0.05f * h && py[q] <= 1.05f * h;
        }
        return new Frame(t, xs, ys, ok);
    }

    private float[] detect(PixelSource src) {
        int w = src.width(), h = src.height();
        float side = Math.max(w, h);
        fill(detIn, src, w / 2f, h / 2f, side, 0f, DET, -1f, 1f);
        Map<Integer, Object> outs = new HashMap<Integer, Object>();
        outs.put(detBoxIdx, detBoxes);
        outs.put(detScoreIdx, detScores);
        det.runForMultipleInputsOutputs(new Object[]{detIn}, outs);
        int best = -1;
        float bs = 0;
        for (int i = 0; i < ANCHORS; i++) {
            float v = detScores[0][i][0];
            if (best < 0 || v > bs) { bs = v; best = i; }
        }
        if (best < 0 || sigmoid(Math.max(-100f, Math.min(100f, bs))) < 0.5f) return null;
        float[] r = detBoxes[0][best];
        float ax = anchors[best][0], ay = anchors[best][1];
        float x0 = (r[4] / DET + ax - 0.5f) * side + w / 2f;
        float y0 = (r[5] / DET + ay - 0.5f) * side + h / 2f;
        float x1 = (r[6] / DET + ax - 0.5f) * side + w / 2f;
        float y1 = (r[7] / DET + ay - 0.5f) * side + h / 2f;
        return roiFrom(x0, y0, x1, y1);
    }

    private static float[] roiFrom(float x0, float y0, float x1, float y1) {
        float size = 2f * (float) Math.hypot(x1 - x0, y1 - y0) * 1.25f;
        double th = Math.PI / 2 - Math.atan2(-(y1 - y0), x1 - x0);
        th = th - 2 * Math.PI * Math.floor((th + Math.PI) / (2 * Math.PI));
        if (size < 8f) return null;
        return new float[]{x0, y0, size, (float) th};
    }

    /** ROI'yi döndürerek n x n float tensörüne örnekler; değerler [lo, hi] aralığında. */
    private static void fill(ByteBuffer buf, PixelSource src, float cx, float cy, float size, float th, int n,
                             float lo, float hi) {
        buf.rewind();
        float c = (float) Math.cos(th), s = (float) Math.sin(th);
        float k = (hi - lo) / 255f;
        for (int j = 0; j < n; j++) {
            float b = ((j + 0.5f) / n - 0.5f) * size;
            for (int i = 0; i < n; i++) {
                float a = ((i + 0.5f) / n - 0.5f) * size;
                int x = (int) Math.floor(cx + a * c - b * s);
                int y = (int) Math.floor(cy + a * s + b * c);
                int p = src.rgb(x, y);
                if (p < 0) {
                    buf.putFloat(0f); buf.putFloat(0f); buf.putFloat(0f);
                } else {
                    buf.putFloat(((p >> 16) & 0xff) * k + lo);
                    buf.putFloat(((p >> 8) & 0xff) * k + lo);
                    buf.putFloat((p & 0xff) * k + lo);
                }
            }
        }
        buf.rewind();
    }

    private static float sigmoid(float v) {
        return (float) (1.0 / (1.0 + Math.exp(-v)));
    }
}
