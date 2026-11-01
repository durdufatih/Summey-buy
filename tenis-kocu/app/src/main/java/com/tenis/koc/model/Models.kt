package com.tenis.koc.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

enum class StrokeType(val label: String) {
    FOREHAND("Forehand"),
    BACKHAND("Backhand"),
    SERVE("Servis / Smaç"),
}

/**
 * Vücut iskeletinden tespit edilebilen teknik hatalar.
 * cue: sesli koçun söylediği kısa komut, tip: açıklama, drill: düzeltme çalışması.
 */
enum class Fault(
    val title: String,
    val cue: String,
    val tip: String,
    val good: String,
    val drill: String,
    val drillDetail: String,
    val penalty: Int,
    val forServe: Boolean,
) {
    KNEES_STRAIGHT(
        title = "Dizler yeterince bükülmüyor",
        cue = "Dizlerini bük",
        tip = "Topa vurmadan önce dizlerini bükerek ağırlık merkezini alçalt. Güç bacaklardan başlar, kalçadan gövdeye ve kola aktarılır.",
        good = "Diz bükmen ve alçalman iyi",
        drill = "Alçak top squat drili",
        drillDetail = "Her vuruştan önce yarım squat pozisyonuna in. Partnerin diz hizasında alçak toplar atsın. 3 set × 12 vuruş.",
        penalty = 15, forServe = false,
    ),
    NO_UNIT_TURN(
        title = "Omuz dönüşü yetersiz",
        cue = "Omuzlarını çevir",
        tip = "Top gelirken ilk hareketin omuzları yana çevirmek olsun (unit turn). Ön omzun topu göstermeli; güç bu dönüşten gelir.",
        good = "Omuz dönüşün (unit turn) iyi",
        drill = "Gölge unit turn",
        drillDetail = "Önce topsuz: 'dön – vur' komutuyla 20 gölge vuruş. Sonra partner top atarken top filenin üstündeyken dönüşü bitir. 3 × 10.",
        penalty = 15, forServe = false,
    ),
    SHORT_BACKSWING(
        title = "Raket geriye kısa alınıyor",
        cue = "Raketi erken geri al",
        tip = "Hazırlığını top senin sahana sekmeden bitir. Kısa ve geç backswing ritmi bozar, vuruşu kola yükler.",
        good = "Hazırlığın (backswing) yeterli",
        drill = "Erken hazırlık drili",
        drillDetail = "Partner topu attığı anda 'hazır' de ve raketi geri götür; top sekmeden backswing tamamlanmış olsun. 3 × 10.",
        penalty = 10, forServe = false,
    ),
    ELBOW_CRAMPED(
        title = "Kol çok bükük, top vücuda yakın",
        cue = "Topu önde karşıla",
        tip = "Temas noktasını vücudunun önüne ve yanına al. Topla arana kolunun rahatça uzanabileceği kadar mesafe bırak.",
        good = "Temas mesafen iyi",
        drill = "Temas noktası konisi",
        drillDetail = "Ön ayağının bir adım önüne ve yanına bir koni koy. Topa koni hizasında vurmaya çalış. 3 × 12.",
        penalty = 12, forServe = false,
    ),
    NO_FOLLOW_THROUGH(
        title = "Vuruş sonu (follow-through) eksik",
        cue = "Vuruşu bitir",
        tip = "Raket vuruştan sonra karşı omzun üzerinde bitmeli. Yarım kalan vuruş kontrolü ve topspin'i azaltır.",
        good = "Vuruşu tam bitiriyorsun",
        drill = "Omuz üstü bitiriş",
        drillDetail = "Her vuruştan sonra raketi karşı omzunun üstünde 1 saniye tut (poz ver). 3 × 10, sonra serbest ralli.",
        penalty = 12, forServe = false,
    ),
    NARROW_STANCE(
        title = "Ayaklar çok kapalı",
        cue = "Ayaklarını aç",
        tip = "Vuruş anında ayaklarının arası en az omuz genişliğinde olsun. Geniş taban denge ve güç sağlar.",
        good = "Duruş genişliğin dengeli",
        drill = "Split-step + geniş duruş",
        drillDetail = "Her toptan önce split-step yap, vuruşta ayaklarını omuz genişliğinden fazla aç. 3 × 10.",
        penalty = 8, forServe = false,
    ),
    WIDE_STANCE(
        title = "Ayaklar aşırı açık",
        cue = "Adımını kısalt",
        tip = "Aşırı geniş duruş gövde dönüşünü ve toparlanmayı zorlaştırır. Topa küçük ayar adımlarıyla yaklaş.",
        good = "Adım uzunluğun iyi",
        drill = "Küçük adım drili",
        drillDetail = "5 dk ip/merdiven ile ayak çabukluğu, ardından küçük ayar adımlarıyla topa yaklaşarak 3 × 10 vuruş.",
        penalty = 6, forServe = false,
    ),
    HEAD_MOVING(
        title = "Baş ve gövde çok hareket ediyor",
        cue = "Başını sabit tut",
        tip = "Vuruş boyunca başını sabit tut ve temas noktasına bak. Gözlerin top raketle buluşurken orada olmalı.",
        good = "Vuruşta başın sabit",
        drill = "Temas noktasına bak",
        drillDetail = "Vuruştan sonra yarım saniye başını temas noktasında tut. Duvara karşı 5 dk kontrollü vuruş.",
        penalty = 10, forServe = false,
    ),
    LOW_SERVE_CONTACT(
        title = "Serviste temas noktası düşük",
        cue = "Yukarıda vur",
        tip = "Topa, kolunun tam uzandığı en yüksek noktada vur. Top atışını biraz daha yükseğe ve öne yap.",
        good = "Servis temas noktan yüksek",
        drill = "Top atış drili",
        drillDetail = "Raketsiz 20 top atışı: top ön ayağının biraz önüne düşsün. Ardından 15 servis, en yüksek noktada temas.",
        penalty = 15, forServe = true,
    ),
    SERVE_ARM_BENT(
        title = "Serviste kol tam uzanmıyor",
        cue = "Kolunu tam uzat",
        tip = "Temas anında vuruş kolu dirsekle birlikte tam uzanmalı. Trofi pozisyonundan yukarı doğru patla.",
        good = "Serviste kolunu tam uzatıyorsun",
        drill = "Diz üstü servis",
        drillDetail = "Bir dizin yerdeyken servis at; bu yukarı uzanmayı ve üst vücut dönüşünü zorlar. 3 × 8.",
        penalty = 12, forServe = true,
    ),
    SERVE_NO_LEGS(
        title = "Serviste bacak itişi yok",
        cue = "Bacaklarınla it",
        tip = "Top atışı sırasında dizlerini bük, vuruşta yukarı doğru it. Servis gücünün büyük kısmı bacaklardan gelir.",
        good = "Serviste bacaklarını kullanıyorsun",
        drill = "Trofi pozisyonu drili",
        drillDetail = "Trofi pozisyonunda dizler bükülü 2 sn bekle, sonra yukarı doğru patlayarak servis at. 3 × 8.",
        penalty = 12, forServe = true,
    ),
}

data class StrokeResult(
    val timeMs: Long,
    val type: StrokeType,
    val score: Int,
    val faults: List<Fault>,
    val speed: Float,
)

data class Session(
    val id: Long,
    val startedAt: Long,
    val durationMs: Long,
    val source: String,
    val strokes: List<StrokeResult>,
) {
    val avgScore: Int
        get() = if (strokes.isEmpty()) 0 else strokes.map { it.score }.average().roundToInt()

    fun date(): LocalDate =
        Instant.ofEpochMilli(startedAt).atZone(ZoneId.systemDefault()).toLocalDate()
}
