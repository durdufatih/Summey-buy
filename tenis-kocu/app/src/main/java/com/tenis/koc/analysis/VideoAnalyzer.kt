package com.tenis.koc.analysis

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import com.tenis.koc.model.Session
import com.tenis.koc.model.StrokeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.math.max

/** Telefonda kayıtlı bir antrenman videosunu kare kare analiz eder. */
object VideoAnalyzer {
    private const val STEP_MS = 80L // ~12 kare/sn
    private const val MAX_SIDE = 640

    suspend fun analyze(
        context: Context,
        uri: Uri,
        leftHanded: Boolean,
        sensitivity: Int,
        onProgress: (Float) -> Unit,
    ): Session? = withContext(Dispatchers.Default) {
        val retriever = MediaMetadataRetriever()
        val detector = PoseDetection.getClient(
            PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                .build()
        )
        val strokes = mutableListOf<StrokeResult>()
        val analyzer = StrokeAnalyzer(leftHanded, sensitivity) { strokes += it }
        var durationMs = 0L
        try {
            retriever.setDataSource(context, uri)
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            if (durationMs <= 0L) throw IllegalArgumentException("Video süresi okunamadı")
            var t = 0L
            while (t <= durationMs) {
                ensureActive()
                val frame = retriever.getFrameAtTime(t * 1000, MediaMetadataRetriever.OPTION_CLOSEST)
                if (frame != null) {
                    val bmp = downscale(frame)
                    val pose = detector.process(InputImage.fromBitmap(bmp, 0)).await()
                    analyzer.process(Frame.from(pose, t))
                    if (bmp !== frame) bmp.recycle()
                    frame.recycle()
                }
                onProgress((t.toFloat() / durationMs).coerceIn(0f, 1f))
                t += STEP_MS
            }
        } finally {
            retriever.release()
            detector.close()
        }
        onProgress(1f)
        if (strokes.isEmpty()) null
        else {
            val now = System.currentTimeMillis()
            Session(now, now, durationMs, "Video", strokes.toList())
        }
    }

    private fun downscale(src: Bitmap): Bitmap {
        val side = max(src.width, src.height)
        if (side <= MAX_SIDE) return src
        val s = MAX_SIDE.toFloat() / side
        return Bitmap.createScaledBitmap(src, (src.width * s).toInt(), (src.height * s).toInt(), true)
    }
}
