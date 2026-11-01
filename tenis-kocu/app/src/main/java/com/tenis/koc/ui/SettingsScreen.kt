package com.tenis.koc.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tenis.koc.data.SessionStore
import com.tenis.koc.data.SettingsStore
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(settings: SettingsStore, store: SessionStore, modifier: Modifier = Modifier) {
    var confirm by remember { mutableStateOf(false) }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Ayarlar", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        SectionCard("Vuruş elin") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !settings.leftHanded, onClick = { settings.leftHanded = false }, label = { Text("Sağ el") })
                FilterChip(selected = settings.leftHanded, onClick = { settings.leftHanded = true }, label = { Text("Sol el") })
            }
            Text(
                "Forehand / backhand ayrımı ve kol açıları bu ele göre hesaplanır.",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        SectionCard("Sesli koç") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Her vuruştan sonra sesli ipucu ver", Modifier.weight(1f))
                Switch(checked = settings.voice, onCheckedChange = { settings.voice = it })
            }
            Text(
                "Telefon uzakta dururken ekrana bakmana gerek kalmaz: “Dizlerini bük”, “Vuruşu bitir” gibi kısa komutlar duyarsın.",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        SectionCard("Vuruş algılama hassasiyeti") {
            Slider(
                value = settings.sensitivity.toFloat(),
                onValueChange = { settings.sensitivity = it.roundToInt().coerceIn(1, 3) },
                valueRange = 1f..3f,
                steps = 1,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                when (settings.sensitivity) {
                    1 -> "Düşük: yalnız hızlı ve tam vuruşlar sayılır (maç temposu)."
                    3 -> "Yüksek: yavaş vuruşlar ve gölge çalışmaları da sayılır."
                    else -> "Orta: çoğu antrenman için önerilen ayar."
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }

        SectionCard("Nasıl çalışır?") {
            Text(
                "Uygulama kameradan vücudundaki 33 noktayı (omuz, dirsek, bilek, kalça, diz, ayak bileği…) " +
                    "cihaz üzerinde tespit eder; internet gerekmez, görüntü hiçbir yere gönderilmez. " +
                    "Bilek hızından vuruş anını yakalar, vuruş türünü (forehand, backhand, servis) belirler ve " +
                    "diz bükme, omuz dönüşü, hazırlık, temas mesafesi, vuruş sonu, duruş ve baş sabitliğini ölçer.\n\n" +
                    "Not: Top ve raket takip edilmez; topun dışarı/fileye gitmesi değil, tekniğin değerlendirilir. " +
                    "Sonuçlar tahminidir ve bir antrenörün yerini tutmaz.",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        OutlinedButton(onClick = { confirm = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Tüm antrenman verilerini sil")
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Emin misin?") },
            text = { Text("Tüm geçmiş antrenmanlar silinecek. Bu işlem geri alınamaz.") },
            confirmButton = { TextButton(onClick = { store.clear(); confirm = false }) { Text("Sil") } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Vazgeç") } },
        )
    }
}
