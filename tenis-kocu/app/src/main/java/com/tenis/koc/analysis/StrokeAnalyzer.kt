package com.tenis.koc.analysis

import com.google.mlkit.vision.pose.PoseLandmark as L
import com.tenis.koc.model.Fault
import com.tenis.koc.model.StrokeResult
import com.tenis.koc.model.StrokeType
import kotlin.math.hypot
import kotlin.math.abs

/**
 * Kare kare gelen vücut iskeletinden vuruşları yakalar ve teknik hataları çıkarır.
 *
 * Yöntem:
 *  - Vuruş kolunun bileği omuz ortasına göre izlenir, gövde boyuna (omuz–kalça) göre normalize edilir.
 *    Böylece kameraya uzaklık ve oyuncunun koşması sonucu etkilemez.
 *  - Bilek hızı eşiği geçince "salınım" başlar; hız düşünce bitiş pozisyonu için kısa süre daha izlenir.
 *  - En yüksek hız anı temas anı kabul edilir; öncesi hazırlık, sonrası vuruş sonu olarak değerlendirilir.
 *
 * Eşikler sezgiseldir; sonuçlar bir koçun gözünün yerini tutmaz, eğilimleri gösterir.
 */
class StrokeAnalyzer(
    leftHanded: Boolean,
    sensitivity: Int,
    private val onStroke: (StrokeResult) -> Unit,
) {
    private val wristId = if (leftHanded) L.LEFT_WRIST else L.RIGHT_WRIST
    private val elbowId = if (leftHanded) L.LEFT_ELBOW else L.RIGHT_ELBOW
    private val shoulderId = if (leftHanded) L.LEFT_SHOULDER else L.RIGHT_SHOULDER
    private val otherShoulderId = if (leftHanded) L.RIGHT_SHOULDER else L.LEFT_SHOULDER

    /** Gövde boyu / saniye cinsinden salınım başlangıç eşiği. */
    private val startThr = when (sensitivity) {
        1 -> 8f
        3 -> 4f
        else -> 5.5f
    }

    private class Feat(
        val f: Frame,
        val torso: Float,
        /** Bilek konumu (omuz ortasına göre, gövde boyuyla normalize). y aşağı pozitif. */
        val wx: Float,
        val wy: Float,
        /** Bileğin vuruş kolu tarafına izdüşümü (+: vuruş kolu tarafı, -: karşı taraf). */
        val proj: Float,
        /** Görünen omuz genişliği / gövde boyu (omuz dönüşünü gösterir). */
        val sw: Float,
        /** Burnun omuz ortasına göre normalize yüksekliği (yoksa null). */
        val headY: Float?,
        var speed: Float = 0f,
    )

    private enum class State { IDLE, SWING, POST }

    private val buf = ArrayDeque<Feat>()
    private var state = State.IDLE
    private var swingStart = 0L
    private var postUntil = 0L
    private var lastEnd = -10_000L
    private var peak: Feat? = null
    private var smooth = 0f

    /** Son karede vücudun (ayak bilekleri dahil) görünüp görünmediği. */
    var lastBodyVisible = false
        private set

    fun process(frame: Frame?) {
        if (frame == null) {
            lastBodyVisible = false
            return
        }
        val core = frame.has(L.LEFT_SHOULDER, L.RIGHT_SHOULDER, L.LEFT_HIP, L.RIGHT_HIP)
        lastBodyVisible = core && frame.has(L.LEFT_ANKLE, L.RIGHT_ANKLE)
        if (!core || !frame.ok[wristId]) return

        val sx = (frame.xs[L.LEFT_SHOULDER] + frame.xs[L.RIGHT_SHOULDER]) / 2f
        val sy = (frame.ys[L.LEFT_SHOULDER] + frame.ys[L.RIGHT_SHOULDER]) / 2f
        val hx = (frame.xs[L.LEFT_HIP] + frame.xs[L.RIGHT_HIP]) / 2f
        val hy = (frame.ys[L.LEFT_HIP] + frame.ys[L.RIGHT_HIP]) / 2f
        val torso = hypot(sx - hx, sy - hy)
        if (torso < 10f) return

        val wx = (frame.xs[wristId] - sx) / torso
        val wy = (frame.ys[wristId] - sy) / torso
        val shW = frame.dist(shoulderId, otherShoulderId)
        val ux: Float
        val uy: Float
        if (shW > 1f) {
            ux = (frame.xs[shoulderId] - frame.xs[otherShoulderId]) / shW
            uy = (frame.ys[shoulderId] - frame.ys[otherShoulderId]) / shW
        } else {
            ux = 0f; uy = 0f
        }
        val headY = if (frame.ok[L.NOSE]) (frame.ys[L.NOSE] - sy) / torso else null
        val feat = Feat(frame, torso, wx, wy, wx * ux + wy * uy, shW / torso, headY)

        val prev = buf.lastOrNull()
        if (prev != null) {
            val dt = (frame.t - prev.f.t).coerceAtLeast(1L) / 1000f
            smooth = if (dt < 0.4f) {
                val raw = hypot(wx - prev.wx, wy - prev.wy) / dt
                0.6f * raw + 0.4f * smooth
            } else 0f
        }
        feat.speed = smooth
        buf.addLast(feat)
        while (buf.isNotEmpty() && buf.first().f.t < frame.t - 3000) buf.removeFirst()
        step(feat)
    }

    private fun step(ft: Feat) {
        val t = ft.f.t
        when (state) {
            State.IDLE -> if (ft.speed > startThr && t - lastEnd > 400) {
                state = State.SWING
                swingStart = t
                peak = ft
            }
            State.SWING -> {
                val p = peak
                if (p == null || ft.speed > p.speed) peak = ft
                if (ft.speed < startThr * 0.4f || t - swingStart > 1000) {
                    state = State.POST
                    postUntil = t + 300
                }
            }
            State.POST -> if (t >= postUntil) finish(t)
        }
    }

    private fun finish(t: Long) {
        state = State.IDLE
        lastEnd = t
        val pk = peak ?: return
        peak = null
        if (pk.speed < startThr * 1.6f) return

        val window = buf.filter { it.f.t >= swingStart - 800 }
        val pre = window.filter { it.f.t <= pk.f.t }
        val post = window.filter { it.f.t >= pk.f.t }
        if (pre.size < 2 || post.size < 2) return

        // Hazırlığın en uç noktası: temas anındaki bilekten en uzak nokta.
        val backFrame = pre.maxBy { hypot(it.wx - pk.wx, it.wy - pk.wy) }
        val arc = hypot(backFrame.wx - pk.wx, backFrame.wy - pk.wy)
        if (arc < 0.6f) return // küçük kol hareketi, vuruş değil

        onStroke(evaluate(pk, window, pre, post, backFrame, arc))
    }

    private fun evaluate(
        pk: Feat,
        window: List<Feat>,
        pre: List<Feat>,
        post: List<Feat>,
        backFrame: Feat,
        arc: Float,
    ): StrokeResult {
        val faults = mutableListOf<Fault>()

        // Bileğin burnun üstüne çıktığı en yüksek nokta (+ değer: başın üstünde)
        val reach = window
            .filter { abs(it.f.t - pk.f.t) <= 250 }
            .maxOf { (it.headY ?: -0.45f) - it.wy }
        val type = when {
            reach > 0.2f -> StrokeType.SERVE
            backFrame.proj - post.last().proj >= 0f -> StrokeType.FOREHAND
            else -> StrokeType.BACKHAND
        }

        val minKnee = window.filter { it.f.t <= pk.f.t + 100 }.flatMap { ft ->
            listOfNotNull(
                kneeAngle(ft.f, L.LEFT_HIP, L.LEFT_KNEE, L.LEFT_ANKLE),
                kneeAngle(ft.f, L.RIGHT_HIP, L.RIGHT_KNEE, L.RIGHT_ANKLE),
            )
        }.minOrNull()
        val elbow = if (pk.f.has(shoulderId, elbowId, wristId)) pk.f.angle(shoulderId, elbowId, wristId) else null

        if (type == StrokeType.SERVE) {
            if (minKnee != null && minKnee > 160f) faults += Fault.SERVE_NO_LEGS
            if (elbow != null && elbow < 145f) faults += Fault.SERVE_ARM_BENT
            if (reach < 0.55f) faults += Fault.LOW_SERVE_CONTACT
        } else {
            val side = if (type == StrokeType.FOREHAND) 1f else -1f

            if (minKnee != null && minKnee > 160f) faults += Fault.KNEES_STRAIGHT
            if (elbow != null && elbow < 90f) faults += Fault.ELBOW_CRAMPED

            // Omuz dönüşü: hazırlıkta görünen omuz genişliği belirgin şekilde daralmalı
            val widthRef = buf.filter { it.f.t >= swingStart - 1500 }.maxOf { it.sw }
            val minPre = pre.minOf { it.sw }
            val turnReliable = widthRef >= 0.4f
            if (turnReliable && minPre / widthRef > 0.8f) faults += Fault.NO_UNIT_TURN

            if (arc < 1.0f) faults += Fault.SHORT_BACKSWING

            // Vuruş sonu: bilek omuz hizasına çıkmalı ve vücudun karşı tarafına geçmeli
            val highest = post.minOf { it.wy }
            val high = highest < 0.1f
            val crossed = post.any { side * it.proj < -0.15f }
            if (!high || (turnReliable && !crossed)) faults += Fault.NO_FOLLOW_THROUGH

            if (pk.f.has(L.LEFT_ANKLE, L.RIGHT_ANKLE)) {
                val stance = pk.f.dist(L.LEFT_ANKLE, L.RIGHT_ANKLE) / pk.torso
                if (stance < 0.55f) faults += Fault.NARROW_STANCE
                else if (stance > 2.3f) faults += Fault.WIDE_STANCE
            }

            // Baş stabilitesi: burnun kalçaya göre konumu temas çevresinde fazla oynamamalı
            val heads = window.filter { it.f.t in (pk.f.t - 400)..(pk.f.t + 200) && it.f.ok[L.NOSE] }
                .map { ft ->
                    val hx = (ft.f.xs[L.LEFT_HIP] + ft.f.xs[L.RIGHT_HIP]) / 2f
                    val hy = (ft.f.ys[L.LEFT_HIP] + ft.f.ys[L.RIGHT_HIP]) / 2f
                    Pair((ft.f.xs[L.NOSE] - hx) / ft.torso, (ft.f.ys[L.NOSE] - hy) / ft.torso)
                }
            if (heads.size >= 3) {
                val mx = heads.map { it.first }.average().toFloat()
                val my = heads.map { it.second }.average().toFloat()
                val spread = heads.maxOf { hypot(it.first - mx, it.second - my) }
                if (spread > 0.4f) faults += Fault.HEAD_MOVING
            }
        }

        val score = (100 - faults.sumOf { it.penalty }).coerceIn(0, 100)
        return StrokeResult(
            timeMs = pk.f.t,
            type = type,
            score = score,
            faults = faults.sortedByDescending { it.penalty },
            speed = pk.speed,
        )
    }

    private fun kneeAngle(f: Frame, hip: Int, knee: Int, ankle: Int): Float? =
        if (f.has(hip, knee, ankle)) f.angle(hip, knee, ankle) else null
}
