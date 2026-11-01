package com.tenis.koc;

/** Vücut iskeletinden tespit edilebilen teknik hatalar, ipuçları ve düzeltme çalışmaları. */
public enum Fault {
    KNEES_STRAIGHT("Dizler yeterince bükülmüyor", "Dizlerini bük",
        "Topa vurmadan önce dizlerini bükerek ağırlık merkezini alçalt. Güç bacaklardan başlar, kalçadan gövdeye ve kola aktarılır.",
        "Diz bükmen ve alçalman iyi", "Alçak top squat drili",
        "Her vuruştan önce yarım squat pozisyonuna in. Partnerin diz hizasında alçak toplar atsın. 3 set × 12 vuruş.", 15, false),
    NO_UNIT_TURN("Omuz dönüşü yetersiz", "Omuzlarını çevir",
        "Top gelirken ilk hareketin omuzları yana çevirmek olsun (unit turn). Ön omzun topu göstermeli; güç bu dönüşten gelir.",
        "Omuz dönüşün (unit turn) iyi", "Gölge unit turn",
        "Önce topsuz: 'dön – vur' komutuyla 20 gölge vuruş. Sonra partner top atarken top filenin üstündeyken dönüşü bitir. 3 × 10.", 15, false),
    SHORT_BACKSWING("Raket geriye kısa alınıyor", "Raketi erken geri al",
        "Hazırlığını top senin sahana sekmeden bitir. Kısa ve geç backswing ritmi bozar, vuruşu kola yükler.",
        "Hazırlığın (backswing) yeterli", "Erken hazırlık drili",
        "Partner topu attığı anda 'hazır' de ve raketi geri götür; top sekmeden backswing tamamlanmış olsun. 3 × 10.", 10, false),
    ELBOW_CRAMPED("Kol çok bükük, top vücuda yakın", "Topu önde karşıla",
        "Temas noktasını vücudunun önüne ve yanına al. Topla arana kolunun rahatça uzanabileceği kadar mesafe bırak.",
        "Temas mesafen iyi", "Temas noktası konisi",
        "Ön ayağının bir adım önüne ve yanına bir koni koy. Topa koni hizasında vurmaya çalış. 3 × 12.", 12, false),
    NO_FOLLOW_THROUGH("Vuruş sonu (follow-through) eksik", "Vuruşu bitir",
        "Raket vuruştan sonra karşı omzun üzerinde bitmeli. Yarım kalan vuruş kontrolü ve topspin'i azaltır.",
        "Vuruşu tam bitiriyorsun", "Omuz üstü bitiriş",
        "Her vuruştan sonra raketi karşı omzunun üstünde 1 saniye tut (poz ver). 3 × 10, sonra serbest ralli.", 12, false),
    NARROW_STANCE("Ayaklar çok kapalı", "Ayaklarını aç",
        "Vuruş anında ayaklarının arası en az omuz genişliğinde olsun. Geniş taban denge ve güç sağlar.",
        "Duruş genişliğin dengeli", "Split-step + geniş duruş",
        "Her toptan önce split-step yap, vuruşta ayaklarını omuz genişliğinden fazla aç. 3 × 10.", 8, false),
    WIDE_STANCE("Ayaklar aşırı açık", "Adımını kısalt",
        "Aşırı geniş duruş gövde dönüşünü ve toparlanmayı zorlaştırır. Topa küçük ayar adımlarıyla yaklaş.",
        "Adım uzunluğun iyi", "Küçük adım drili",
        "5 dk ip/merdiven ile ayak çabukluğu, ardından küçük ayar adımlarıyla topa yaklaşarak 3 × 10 vuruş.", 6, false),
    HEAD_MOVING("Baş ve gövde çok hareket ediyor", "Başını sabit tut",
        "Vuruş boyunca başını sabit tut ve temas noktasına bak. Gözlerin top raketle buluşurken orada olmalı.",
        "Vuruşta başın sabit", "Temas noktasına bak",
        "Vuruştan sonra yarım saniye başını temas noktasında tut. Duvara karşı 5 dk kontrollü vuruş.", 10, false),
    LOW_SERVE_CONTACT("Serviste temas noktası düşük", "Yukarıda vur",
        "Topa, kolunun tam uzandığı en yüksek noktada vur. Top atışını biraz daha yükseğe ve öne yap.",
        "Servis temas noktan yüksek", "Top atış drili",
        "Raketsiz 20 top atışı: top ön ayağının biraz önüne düşsün. Ardından 15 servis, en yüksek noktada temas.", 15, true),
    SERVE_ARM_BENT("Serviste kol tam uzanmıyor", "Kolunu tam uzat",
        "Temas anında vuruş kolu dirsekle birlikte tam uzanmalı. Trofi pozisyonundan yukarı doğru patla.",
        "Serviste kolunu tam uzatıyorsun", "Diz üstü servis",
        "Bir dizin yerdeyken servis at; bu yukarı uzanmayı ve üst vücut dönüşünü zorlar. 3 × 8.", 12, true),
    SERVE_NO_LEGS("Serviste bacak itişi yok", "Bacaklarınla it",
        "Top atışı sırasında dizlerini bük, vuruşta yukarı doğru it. Servis gücünün büyük kısmı bacaklardan gelir.",
        "Serviste bacaklarını kullanıyorsun", "Trofi pozisyonu drili",
        "Trofi pozisyonunda dizler bükülü 2 sn bekle, sonra yukarı doğru patlayarak servis at. 3 × 8.", 12, true);

    public final String title, cue, tip, good, drill, drillDetail;
    public final int penalty;
    public final boolean forServe;

    Fault(String title, String cue, String tip, String good, String drill, String drillDetail, int penalty, boolean forServe) {
        this.title = title; this.cue = cue; this.tip = tip; this.good = good;
        this.drill = drill; this.drillDetail = drillDetail; this.penalty = penalty; this.forServe = forServe;
    }
}
