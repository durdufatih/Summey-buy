package com.tenis.koc.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tenis.koc.plan.Summary
import kotlin.math.roundToInt

private val LightColors = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E6C9),
    onPrimaryContainer = Color(0xFF0B3D0F),
    secondary = Color(0xFF6B7F00),
    secondaryContainer = Color(0xFFEEF7B5),
    onSecondaryContainer = Color(0xFF2B3300),
    tertiary = Color(0xFFE65100),
    background = Color(0xFFF7FAF5),
    surface = Color(0xFFF7FAF5),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF00390A),
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = Color(0xFFC8E6C9),
    secondary = Color(0xFFD4E157),
    secondaryContainer = Color(0xFF3B4500),
    onSecondaryContainer = Color(0xFFEEF7B5),
    tertiary = Color(0xFFFFB74D),
)

@Composable
fun TenisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}

fun scoreColor(score: Int): Color = when {
    score >= 80 -> Color(0xFF43A047)
    score >= 60 -> Color(0xFFFFA000)
    else -> Color(0xFFE53935)
}

@Composable
fun SectionCard(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
fun ScoreBadge(score: Int, size: Int = 48) {
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(scoreColor(score)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "$score",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size / 2.6).sp,
        )
    }
}

@Composable
fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

/** Günün / antrenmanın özet içeriği. */
@Composable
fun SummaryContent(s: Summary) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBox("Vuruş", "${s.strokeCount}", Modifier.weight(1f))
            StatBox("Ort. puan", "${s.avgScore}", Modifier.weight(1f))
            val d = s.scoreDelta
            StatBox(
                if (d != null) "Değişim" else "Süre",
                if (d != null) (if (d > 0) "+$d" else "$d") else "${s.minutes} dk",
                Modifier.weight(1f),
            )
        }

        Text(s.text, style = MaterialTheme.typography.bodyMedium)

        if (s.types.isNotEmpty()) {
            Text("Vuruş türleri", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            s.types.forEach { t ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t.type.label, Modifier.weight(1f))
                    Text("${t.count} vuruş", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.width(10.dp))
                    ScoreBadge(t.avgScore, 34)
                }
            }
        }

        if (s.faults.isNotEmpty()) {
            Text("Dikkat etmen gerekenler", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            s.faults.take(5).forEach { fs ->
                Column {
                    Row {
                        Text(fs.fault.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text("%${(fs.rate * 100).roundToInt()}", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { fs.rate.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }

        if (s.strengths.isNotEmpty()) {
            Text("Güçlü yönlerin", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            s.strengths.forEach { Text("✓ $it", color = MaterialTheme.colorScheme.primary) }
        }

        s.nextFocus?.let { f ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(14.dp),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Sıradaki odak: ${f.title}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(f.tip, color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Çalışma: ${f.drill} — ${f.drillDetail}",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

fun formatClock(ms: Long): String {
    val s = ms / 1000
    return "%02d:%02d".format(s / 60, s % 60)
}
