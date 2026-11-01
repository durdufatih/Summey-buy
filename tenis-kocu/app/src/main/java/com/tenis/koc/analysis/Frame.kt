package com.tenis.koc.analysis

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.acos
import kotlin.math.hypot

/** Tek bir karedeki vücut noktaları (görüntü pikseli cinsinden). t: oturum başından beri ms. */
class Frame(
    val t: Long,
    val xs: FloatArray,
    val ys: FloatArray,
    val ok: BooleanArray,
) {
    fun has(vararg ids: Int): Boolean = ids.all { ok[it] }

    fun dist(a: Int, b: Int): Float = hypot(xs[a] - xs[b], ys[a] - ys[b])

    /** b noktasındaki açı (derece). */
    fun angle(a: Int, b: Int, c: Int): Float {
        val v1x = xs[a] - xs[b]
        val v1y = ys[a] - ys[b]
        val v2x = xs[c] - xs[b]
        val v2y = ys[c] - ys[b]
        val m = hypot(v1x, v1y) * hypot(v2x, v2y)
        if (m < 1e-3f) return 180f
        val cos = ((v1x * v2x + v1y * v2y) / m).coerceIn(-1f, 1f)
        return Math.toDegrees(acos(cos).toDouble()).toFloat()
    }

    companion object {
        const val COUNT = 33

        fun from(pose: Pose, t: Long, minLikelihood: Float = 0.5f): Frame? {
            val lms = pose.allPoseLandmarks
            if (lms.isEmpty()) return null
            val xs = FloatArray(COUNT)
            val ys = FloatArray(COUNT)
            val ok = BooleanArray(COUNT)
            for (lm in lms) {
                val i = lm.landmarkType
                if (i in 0 until COUNT) {
                    xs[i] = lm.position.x
                    ys[i] = lm.position.y
                    ok[i] = lm.inFrameLikelihood >= minLikelihood
                }
            }
            return Frame(t, xs, ys, ok)
        }

        val BONES = listOf(
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.RIGHT_SHOULDER,
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_ELBOW,
            PoseLandmark.LEFT_ELBOW to PoseLandmark.LEFT_WRIST,
            PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_ELBOW,
            PoseLandmark.RIGHT_ELBOW to PoseLandmark.RIGHT_WRIST,
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_HIP,
            PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_HIP,
            PoseLandmark.LEFT_HIP to PoseLandmark.RIGHT_HIP,
            PoseLandmark.LEFT_HIP to PoseLandmark.LEFT_KNEE,
            PoseLandmark.LEFT_KNEE to PoseLandmark.LEFT_ANKLE,
            PoseLandmark.RIGHT_HIP to PoseLandmark.RIGHT_KNEE,
            PoseLandmark.RIGHT_KNEE to PoseLandmark.RIGHT_ANKLE,
        )

        val JOINTS = listOf(
            PoseLandmark.NOSE,
            PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER,
            PoseLandmark.LEFT_ELBOW, PoseLandmark.RIGHT_ELBOW,
            PoseLandmark.LEFT_WRIST, PoseLandmark.RIGHT_WRIST,
            PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP,
            PoseLandmark.LEFT_KNEE, PoseLandmark.RIGHT_KNEE,
            PoseLandmark.LEFT_ANKLE, PoseLandmark.RIGHT_ANKLE,
        )
    }
}
