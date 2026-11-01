package com.tenis.koc.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.tenis.koc.data.SessionStore
import com.tenis.koc.data.SettingsStore
import com.tenis.koc.plan.DailyPlan
import com.tenis.koc.plan.DailyPlanner
import com.tenis.koc.plan.Summarizer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    store: SessionStore,
    settings: SettingsStore,
    onStartLive: () -> Unit,
    onPickVideo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = remember { LocalDate.now() }
    val all = store.sessions
    val past = remember(all) { all.filter { it.date() < today } }
    val todays = remember(all) { all.filter { it.date() == today } }
    val plan = remember(past) { DailyPlanner.build(past, today) }
    val summary = remember(todays, past) {
        if (todays.isEmpty()) null else Summarizer.summarize(todays, past.filter { it.date() >= today.minusDays(14) }, "Bugün")
    }
    val dateText = remember { today.format(DateTimeFormatter.ofPattern("d MMMM EEEE", Locale.forLanguageTag("tr"))) }

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Text("🎾 Tenis Koçu", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(dateText, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStartLive, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Kamerayla antrenmanı başlat", style = MaterialTheme.typography.titleSmall)
                }
                OutlinedButton(onClick = onPickVideo, modifier = Modifier.fillMaxWidth()) {
                    Text("Kayıtlı videodan analiz et")
                }
            }
        }
        item { PlanCard(plan, today, settings) }
        item {
            SectionCard("Günün özeti") {
                if (summary == null) {
                    Text(
                        "Bugün henüz analiz edilmiş antrenman yok. Kamerayı başlat; her vuruşu izleyip hatalarını ve güçlü yönlerini burada özetleyeceğim.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    SummaryContent(summary)
                }
            }
        }
        item {
            SectionCard("📱 Kamera nasıl yerleştirilmeli?") {
                Text(
                    "• Telefonu 4–6 m uzağa, bel hizasında tripoda (veya fileye/çantaya) dik yerleştir.\n" +
                        "• Baştan ayağa tüm vücudun kadrajda olsun; iskelet çizgileri yeşil görünmeli.\n" +
                        "• En doğru sonuç için kameranı tam önüne ya da arkana koy (omuz dönüşü böyle ölçülür).\n" +
                        "• Ayarlardan solak/sağlak seçimini yapmayı unutma.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun PlanCard(plan: DailyPlan, today: LocalDate, settings: SettingsStore) {
    val keyOf = { id: String -> "$today:$id" }
    val done = plan.items.count { settings.isChecked(keyOf(it.id)) }
    SectionCard("Günün planı · ${plan.totalMinutes} dk") {
        Text(plan.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text("🎯 ${plan.goal}", style = MaterialTheme.typography.bodyMedium)
        if (!plan.personalized) {
            Text(
                "Henüz yeterli veri yok; bu genel bir plan. Birkaç antrenmandan sonra plan senin hatalarına göre hazırlanır.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = { if (plan.items.isEmpty()) 0f else done.toFloat() / plan.items.size },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
        )
        plan.items.forEach { item ->
            val checked = settings.isChecked(keyOf(item.id))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { settings.toggle(keyOf(item.id)) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Checkbox(checked = checked, onCheckedChange = { settings.toggle(keyOf(item.id)) })
                Column(Modifier.weight(1f).padding(top = 10.dp)) {
                    Row {
                        Text(
                            item.title,
                            Modifier.weight(1f),
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = if (checked) TextDecoration.LineThrough else null,
                        )
                        Text("${item.minutes} dk", style = MaterialTheme.typography.labelMedium)
                    }
                    Text(item.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (plan.cues.isNotEmpty()) {
            Text("Odak noktaları", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            plan.cues.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
        }
    }
}
