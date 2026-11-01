package com.tenis.koc.ui

import android.net.Uri
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.tenis.koc.analysis.Frame
import com.tenis.koc.analysis.StrokeAnalyzer
import com.tenis.koc.analysis.VideoAnalyzer
import com.tenis.koc.camera.PoseCameraView
import com.tenis.koc.camera.SkeletonOverlay
import com.tenis.koc.camera.VoiceCoach
import com.tenis.koc.data.SettingsStore
import com.tenis.koc.model.Session
import com.tenis.koc.model.StrokeResult
import com.tenis.koc.model.StrokeType
import com.tenis.koc.plan.Summarizer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Canlı kamera antrenmanı: her vuruşu yakalar, anında geri bildirim verir. */
@Composable
fun LiveSessionScreen(settings: SettingsStore, onFinish: (Session?) -> Unit) {
    val context = LocalContext.current
    val startWall = remember { System.currentTimeMillis() }
    val startClock = remember { SystemClock.elapsedRealtime() }
    val strokes = remember { mutableStateListOf<StrokeResult>() }
    var last by remember { mutableStateOf<StrokeResult?>(null) }
    var frame by remember { mutableStateOf<Frame?>(null) }
    var imgSize by remember { mutableStateOf(IntSize(1, 1)) }
    var useFront by remember { mutableStateOf(false) }
    var voiceOn by remember { mutableStateOf(settings.voice) }
    var bodyVisible by remember { mutableStateOf(false) }
    var elapsed by remember { mutableLongStateOf(0L) }

    val coach = remember { VoiceCoach(context) }
    DisposableEffect(Unit) { onDispose { coach.shutdown() } }
    val voiceState = rememberUpdatedState(voiceOn)

    val analyzer = remember {
        StrokeAnalyzer(settings.leftHanded, settings.sensitivity) { r ->
            strokes.add(r)
            last = r
            if (voiceState.value) coach.react(r)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            elapsed = SystemClock.elapsedRealtime() - startClock
            delay(500)
        }
    }
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        key(useFront) {
            PoseCameraView(useFront, Modifier.fillMaxSize()) { pose, w, h ->
                val f = Frame.from(pose, SystemClock.elapsedRealtime() - startClock)
                frame = f
                imgSize = IntSize(w, h)
                analyzer.process(f)
                bodyVisible = analyzer.lastBodyVisible
            }
        }
        SkeletonOverlay(frame, imgSize, mirror = useFront, modifier = Modifier.fillMaxSize())

        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(12.dp)) {
            // Üst bilgi çubuğu
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill("⏱ ${formatClock(elapsed)}")
                Spacer(Modifier.width(8.dp))
                Pill("🎾 ${strokes.size} vuruş")
                Spacer(Modifier.weight(1f))
                Pill(if (voiceOn) "🔊" else "🔇", onClick = { voiceOn = !voiceOn })
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { useFront = !useFront; frame = null }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Kamerayı değiştir", tint = Color.White)
                }
            }
            if (!bodyVisible) {
                Spacer(Modifier.height(8.dp))
                Surface(color = Color(0xCCE65100), shape = RoundedCornerShape(12.dp)) {
                    Text(
                        "Tüm vücudun (baştan ayak bileğine) görünecek şekilde geri çekil",
                        color = Color.White,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }
            Spacer(Modifier.weight(1f))

            last?.let { LastStrokeCard(it) }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val fh = strokes.count { it.type == StrokeType.FOREHAND }
                val bh = strokes.count { it.type == StrokeType.BACKHAND }
                val sv = strokes.count { it.type == StrokeType.SERVE }
                Pill("FH $fh"); Pill("BH $bh"); Pill("Servis $sv")
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    if (strokes.isEmpty()) onFinish(null)
                    else onFinish(Session(startWall, startWall, elapsed, "Canlı kamera", strokes.toList()))
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
            ) { Text("Antrenmanı bitir ve özeti gör", color = Color.White) }
        }
    }
}

@Composable
private fun Pill(text: String, onClick: (() -> Unit)? = null) {
    if (onClick != null) {
        Surface(onClick = onClick, color = Color(0x99000000), shape = RoundedCornerShape(50)) {
            Text(text, color = Color.White, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
        }
    } else {
        Surface(color = Color(0x99000000), shape = RoundedCornerShape(50)) {
            Text(text, color = Color.White, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
        }
    }
}

@Composable
private fun LastStrokeCard(r: StrokeResult) {
    Surface(color = Color(0xDD000000), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ScoreBadge(r.score, 52)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Son vuruş: ${r.type.label}", color = Color.White, fontWeight = FontWeight.Bold)
                if (r.faults.isEmpty()) {
                    Text("Teknik olarak temiz, böyle devam!", color = Color(0xFFC6FF00))
                } else {
                    r.faults.take(2).forEach { f ->
                        Text("• ${f.title}", color = Color(0xFFFFCC80))
                    }
                    Text(r.faults.first().tip, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/** Seçilen videoyu analiz eder. */
@Composable
fun VideoAnalysisScreen(uri: Uri, settings: SettingsStore, onDone: (Session?) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    var progress by remember { mutableFloatStateOf(0f) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uri) {
        try {
            val s = VideoAnalyzer.analyze(context, uri, settings.leftHanded, settings.sensitivity) { p -> progress = p }
            onDone(s)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "Video okunamadı"
        }
    }

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (error == null) {
            Text("Video analiz ediliyor…", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Text("%${(progress * 100).toInt()}")
            Spacer(Modifier.height(16.dp))
            Text(
                "Her kare tek tek inceleniyor; uzun videolar birkaç dakika sürebilir.",
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            Text("Analiz başarısız: $error", color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onCancel) { Text(if (error == null) "İptal" else "Geri dön") }
    }
}

private val dateTimeFmt = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale.forLanguageTag("tr"))

fun Session.title(): String =
    Instant.ofEpochMilli(startedAt).atZone(ZoneId.systemDefault()).format(dateTimeFmt)

@Composable
fun HistoryScreen(sessions: List<Session>, onOpen: (Session) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Geçmiş antrenmanlar", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        if (sessions.isEmpty()) {
            item { Text("Henüz kayıtlı antrenman yok.") }
        }
        items(sessions, key = { it.id }) { s ->
            Card(
                onClick = { onOpen(s) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    ScoreBadge(s.avgScore)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.title(), fontWeight = FontWeight.SemiBold)
                        Text(
                            "${s.source} · ${s.strokes.size} vuruş · ${formatClock(s.durationMs)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        val top = Summarizer.faultStats(s.strokes).firstOrNull()
                        if (top != null) {
                            Text("Ana hata: ${top.fault.title}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SessionDetailScreen(session: Session, previous: List<Session>, onBack: () -> Unit, onDelete: () -> Unit) {
    val summary = remember(session, previous) { Summarizer.summarize(listOf(session), previous, "Bu") }
    LazyColumn(
        Modifier.fillMaxSize().safeDrawingPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri") }
                Column(Modifier.weight(1f)) {
                    Text("Antrenman özeti", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${session.title()} · ${session.source}", style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Sil") }
            }
        }
        item { SectionCard("Özet") { SummaryContent(summary) } }
        item {
            Text("Vuruş vuruş", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(session.strokes) { st ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ScoreBadge(st.score, 38)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("${formatClock(st.timeMs)} · ${st.type.label}", fontWeight = FontWeight.SemiBold)
                    Text(
                        if (st.faults.isEmpty()) "Temiz vuruş" else st.faults.joinToString(", ") { it.title },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        item {
            FilledTonalButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Ana sayfaya dön") }
        }
    }
}
