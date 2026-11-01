package com.tenis.koc.plan

import com.tenis.koc.model.Fault
import com.tenis.koc.model.Session
import com.tenis.koc.model.StrokeResult
import com.tenis.koc.model.StrokeType
import java.time.LocalDate
import kotlin.math.roundToInt

data class TypeStat(val type: StrokeType, val count: Int, val avgScore: Int)
data class FaultStat(val fault: Fault, val count: Int, val rate: Float)

data class Summary(
    val sessionCount: Int,
    val strokeCount: Int,
    val minutes: Int,
    val avgScore: Int,
    val scoreDelta: Int?,
    val types: List<TypeStat>,
    val faults: List<FaultStat>,
    val strengths: List<String>,
    val text: String,
    val nextFocus: Fault?,
)

data class PlanItem(val id: String, val title: String, val detail: String, val minutes: Int)

data class DailyPlan(
    val title: String,
    val goal: String,
    val items: List<PlanItem>,
    val cues: List<String>,
    val personalized: Boolean,
) {
    val totalMinutes: Int get() = items.sumOf { it.minutes }
}

object Summarizer {

    /** Hata oranları, ilgili vuruş tipine göre hesaplanır (servis hataları yalnız servislerde). */
    fun faultStats(strokes: List<StrokeResult>): List<FaultStat> {
        val serves = strokes.count { it.type == StrokeType.SERVE }
        val ground = strokes.size - serves
        return Fault.entries.mapNotNull { f ->
            val base = if (f.forServe) serves else ground
            if (base == 0) return@mapNotNull null
            val c = strokes.count { f in it.faults }
            if (c == 0) null else FaultStat(f, c, c.toFloat() / base)
        }.sortedByDescending { it.rate * it.fault.penalty }
    }

    fun summarize(sessions: List<Session>, previous: List<Session>, label: String): Summary {
        val strokes = sessions.flatMap { it.strokes }
        val avg = if (strokes.isEmpty()) 0 else strokes.map { it.score }.average().roundToInt()
        val prevStrokes = previous.flatMap { it.strokes }
        val delta = if (prevStrokes.size >= 5 && strokes.isNotEmpty())
            avg - prevStrokes.map { it.score }.average().roundToInt() else null

        val types = StrokeType.entries.mapNotNull { t ->
            val list = strokes.filter { it.type == t }
            if (list.isEmpty()) null
            else TypeStat(t, list.size, list.map { it.score }.average().roundToInt())
        }
        val faults = faultStats(strokes)
        val minutes = (sessions.sumOf { it.durationMs } / 60_000L).toInt()

        val serves = strokes.count { it.type == StrokeType.SERVE }
        val ground = strokes.size - serves
        val faultRate = faults.associate { it.fault to it.rate }
        val strengths = Fault.entries.filter { f ->
            val enough = if (f.forServe) serves >= 3 else ground >= 5
            enough && (faultRate[f] ?: 0f) < 0.15f
        }.map { it.good }.take(3)

        val sb = StringBuilder()
        val prefix = when {
            sessions.size > 1 -> "$label ${sessions.size} antrenmanda"
            label == "Bugün" -> "Bugünkü antrenmanda"
            else -> "$label antrenmanda"
        }
        sb.append("$prefix toplam ${strokes.size} vuruş analiz edildi")
        if (minutes > 0) sb.append(" ($minutes dk)")
        sb.append(". Ortalama teknik puanın $avg/100")
        if (delta != null) {
            when {
                delta > 0 -> sb.append(", önceki antrenmanlara göre +$delta puan ilerleme var")
                delta < 0 -> sb.append(", önceki antrenmanlara göre $delta puan geride")
                else -> sb.append(", önceki antrenmanlarla aynı seviyede")
            }
        }
        sb.append(".")
        val best = types.filter { it.count >= 3 }.maxByOrNull { it.avgScore }
        val worst = types.filter { it.count >= 3 }.minByOrNull { it.avgScore }
        if (best != null && worst != null && best != worst) {
            sb.append(" En iyi vuruşun ${best.type.label} (${best.avgScore}), en çok gelişmesi gereken ${worst.type.label} (${worst.avgScore}).")
        }
        val top = faults.firstOrNull()
        if (top != null) {
            sb.append(" En sık görülen hata: “${top.fault.title}” (ilgili vuruşların %${(top.rate * 100).roundToInt()}'i).")
        } else if (strokes.isNotEmpty()) {
            sb.append(" Belirgin bir teknik hata tespit edilmedi, harika!")
        }

        return Summary(
            sessionCount = sessions.size,
            strokeCount = strokes.size,
            minutes = minutes,
            avgScore = avg,
            scoreDelta = delta,
            types = types,
            faults = faults,
            strengths = strengths,
            text = sb.toString(),
            nextFocus = top?.fault,
        )
    }
}

object DailyPlanner {

    private val warmup = PlanItem(
        "warmup", "Isınma + mini tenis",
        "5 dk hafif koşu, kol/omuz/kalça dinamik esneme. Ardından servis çizgileri arasında 5 dk kontrollü mini tenis.",
        10,
    )
    private val cooldown = PlanItem(
        "cooldown", "Soğuma ve esneme",
        "Omuz, ön kol, kalça ve baldır için statik esneme. Ardından uygulamada günün özetine göz at.",
        5,
    )

    fun build(history: List<Session>, today: LocalDate): DailyPlan {
        val recent = history.filter { it.date() >= today.minusDays(14) && it.date() < today }
        val strokes = recent.flatMap { it.strokes }
        if (strokes.size < 5) return defaultPlan(today)

        val stats = Summarizer.faultStats(strokes)
        val top = stats.filter { it.rate >= 0.15f }.take(3)
        val items = mutableListOf(warmup)
        top.forEachIndexed { i, fs ->
            items += PlanItem(
                "drill_${fs.fault.name}", fs.fault.drill, fs.fault.drillDetail,
                if (i == 0) 15 else 10,
            )
        }

        val fh = strokes.count { it.type == StrokeType.FOREHAND }
        val bh = strokes.count { it.type == StrokeType.BACKHAND }
        val sv = strokes.count { it.type == StrokeType.SERVE }
        if (fh + bh >= 10 && bh < (fh + bh) * 0.3f) {
            items += PlanItem(
                "bh_balance", "Backhand çapraz ralli",
                "Backhand'i daha az kullanıyorsun. 10 dk sadece backhand çapraz ralli, her vuruşta omuz dönüşüne dikkat.",
                10,
            )
        }
        if (sv < strokes.size * 0.1f) {
            items += PlanItem(
                "serve_routine", "Servis rutini",
                "Her iki karede 10'ar servis: önce %70 güçle isabet, sonra hız. Her servisten önce aynı ritüeli uygula.",
                10,
            )
        }
        items += PlanItem(
            "play", "Puan oyunu",
            "Serbest puan oyna; her puanda günün odak noktalarından birini bilinçli uygula.",
            15,
        )
        items += cooldown

        val avg = strokes.map { it.score }.average().roundToInt()
        val main = top.firstOrNull()
        val goal = if (main != null) {
            val target = ((main.rate * 100).roundToInt() - 15).coerceAtLeast(5)
            "Ortalama puanını ${(avg + 5).coerceAtMost(95)}+ yap ve “${main.fault.title}” oranını %$target altına indir."
        } else {
            "Ortalama puanını ${(avg + 3).coerceAtMost(98)}+ seviyesinde koru, vuruş sayını artır."
        }
        val title = if (main != null) "Odak: ${main.fault.title}" else "Odak: Ritim ve tutarlılık"
        return DailyPlan(
            title = title,
            goal = goal,
            items = items,
            cues = top.map { "${it.fault.cue} — ${it.fault.tip}" },
            personalized = true,
        )
    }

    private fun defaultPlan(today: LocalDate): DailyPlan {
        val theme = when (today.dayOfYear % 3) {
            0 -> Triple("Forehand temeli", listOf(Fault.NO_UNIT_TURN, Fault.NO_FOLLOW_THROUGH), "forehand")
            1 -> Triple("Backhand ve ayak çalışması", listOf(Fault.KNEES_STRAIGHT, Fault.NARROW_STANCE), "backhand")
            else -> Triple("Servis günü", listOf(Fault.LOW_SERVE_CONTACT, Fault.SERVE_NO_LEGS), "servis")
        }
        val items = mutableListOf(warmup)
        theme.second.forEach { f ->
            items += PlanItem("drill_${f.name}", f.drill, f.drillDetail, 12)
        }
        items += PlanItem(
            "record", "Kamerayla analiz turu",
            "Telefonu tripoda koy ve 10 dk ${theme.third} ağırlıklı ralli yap; uygulama hatalarını kaydetsin. Yarın plan sana göre kişiselleşir.",
            10,
        )
        items += PlanItem("play", "Puan oyunu", "Serbest puan oyna, günün odak noktalarını uygula.", 15)
        items += cooldown
        return DailyPlan(
            title = "Odak: ${theme.first}",
            goal = "En az 40 vuruşu kamerayla analiz ettir; kişisel planın oluşsun.",
            items = items,
            cues = theme.second.map { "${it.cue} — ${it.tip}" },
            personalized = false,
        )
    }
}
