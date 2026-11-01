package com.tenis.koc.camera

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import com.tenis.koc.analysis.Frame
import com.tenis.koc.model.StrokeResult
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.max

/** Kamera önizlemesi + her karede ML Kit ile vücut iskeleti tespiti. */
@Composable
fun PoseCameraView(
    useFront: Boolean,
    modifier: Modifier = Modifier,
    onPose: (pose: Pose, width: Int, height: Int) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnPose by rememberUpdatedState(onPose)
    val detector = remember {
        PoseDetection.getClient(
            PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                .build()
        )
    }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
            detector.close()
        }
    }

    DisposableEffect(useFront, lifecycleOwner) {
        val future = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        future.addListener({
            val p = future.get()
            provider = p
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(executor) { proxy ->
                analyze(detector, proxy) { pose, w, h -> currentOnPose(pose, w, h) }
            }
            val selector = if (useFront) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
            try {
                p.unbindAll()
                p.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
            } catch (_: Exception) {
                // Seçilen kamera yoksa sessizce geç
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose { provider?.unbindAll() }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

@OptIn(ExperimentalGetImage::class)
private fun analyze(
    detector: PoseDetector,
    proxy: ImageProxy,
    onPose: (Pose, Int, Int) -> Unit,
) {
    val media = proxy.image
    if (media == null) {
        proxy.close()
        return
    }
    val rot = proxy.imageInfo.rotationDegrees
    val w = if (rot % 180 == 0) proxy.width else proxy.height
    val h = if (rot % 180 == 0) proxy.height else proxy.width
    detector.process(InputImage.fromMediaImage(media, rot))
        .addOnSuccessListener { pose -> onPose(pose, w, h) }
        .addOnCompleteListener { proxy.close() }
}

/** Kameranın üstüne vücut iskeletini çizer. */
@Composable
fun SkeletonOverlay(frame: Frame?, image: IntSize, mirror: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val f = frame ?: return@Canvas
        if (image.width <= 0 || image.height <= 0) return@Canvas
        val scale = max(size.width / image.width, size.height / image.height)
        val dx = (size.width - image.width * scale) / 2f
        val dy = (size.height - image.height * scale) / 2f
        fun pt(i: Int): Offset {
            var x = f.xs[i] * scale + dx
            if (mirror) x = size.width - x
            return Offset(x, f.ys[i] * scale + dy)
        }
        for ((a, b) in Frame.BONES) {
            if (f.ok[a] && f.ok[b]) {
                drawLine(Color(0xFFC6FF00), pt(a), pt(b), strokeWidth = 7f, cap = StrokeCap.Round)
            }
        }
        for (i in Frame.JOINTS) {
            if (f.ok[i]) drawCircle(Color.White, 8f, pt(i))
        }
    }
}

/** Her vuruştan sonra kısa sesli geri bildirim verir (Türkçe). */
class VoiceCoach(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    private var ready = false
    private var goodStreak = 0
    private val praise = listOf("Harika vuruş", "Çok iyi", "Böyle devam", "Mükemmel")

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.forLanguageTag("tr-TR")
            ready = true
        }
    }

    fun react(r: StrokeResult) {
        val top = r.faults.firstOrNull()
        if (top != null) {
            goodStreak = 0
            say(top.cue)
        } else {
            goodStreak++
            if (goodStreak % 3 == 1) say(praise.random())
        }
    }

    fun say(text: String) {
        if (ready) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "coach")
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
