# 🎾 Tenis Koçu — Android

Telefon kamerasıyla seni izleyip **her vuruşunu analiz eden**, hatalarını ve dikkat etmen
gereken noktaları gösteren, **günün planını** ve **günün özetini** çıkaran Android uygulaması.

## Özellikler

- **Canlı kamera analizi** — Vücudundaki 33 nokta (omuz, dirsek, bilek, kalça, diz, ayak bileği…)
  cihaz üzerinde ML Kit ile tespit edilir, iskelet görüntünün üstüne çizilir. İnternet gerekmez,
  görüntü hiçbir yere gönderilmez.
- **Vuruş tespiti ve türü** — Bilek hızından vuruş anı yakalanır; forehand, backhand ve
  servis/smaç ayrılır.
- **Teknik hatalar** (her vuruşa 0–100 puan):
  - Dizler yeterince bükülmüyor
  - Omuz dönüşü (unit turn) yetersiz
  - Raket geriye kısa alınıyor (backswing)
  - Kol çok bükük, top vücuda yakın
  - Vuruş sonu (follow-through) eksik
  - Ayaklar çok kapalı / aşırı açık
  - Baş ve gövde vuruşta çok hareket ediyor
  - Serviste temas noktası düşük, kol tam uzanmıyor, bacak itişi yok
- **Sesli koç** — Her vuruştan sonra Türkçe kısa komut: “Dizlerini bük”, “Vuruşu bitir”…
- **Günün planı** — Son 14 günün en sık hatalarına göre ısınma, hedefli drill'ler, denge
  çalışmaları ve puan oyunundan oluşan, işaretlenebilir günlük program ve günlük hedef.
- **Günün özeti** — Toplam vuruş, ortalama puan, önceki günlere göre değişim, vuruş türlerine
  göre puanlar, hata oranları, güçlü yönler ve sıradaki odak noktası.
- **Videodan analiz** — Telefonda kayıtlı antrenman videosunu da analiz edebilir.
- **Geçmiş** — Tüm antrenmanlar vuruş vuruş saklanır.

## Kurulum (APK)

1. GitHub'da **Releases → “Tenis Koçu (son sürüm)”** sayfasından `TenisKocu.apk` dosyasını
   telefonuna indir (veya Actions sekmesindeki son çalışmadan `TenisKocu-apk` artifact'ını).
2. Dosyayı aç; istenirse “Bilinmeyen kaynaklardan uygulama yükleme” iznini ver.
3. Android 8.0 (API 26) ve üzeri gerekir.

## Kullanım ipuçları

- Telefonu 4–6 m uzağa, bel hizasında tripoda dik yerleştir; baştan ayağa tüm vücudun görünsün.
- Kamerayı tam önüne veya arkana koymak omuz dönüşü ölçümünü en doğru yapar.
- Ayarlardan sağ/sol el seçimini yap.

> Not: Top ve raket takip edilmez; değerlendirilen şey vücut tekniğidir. Eşikler sezgiseldir,
> sonuçlar eğilim gösterir ve bir antrenörün yerini tutmaz.

## Derleme

```bash
cd tenis-kocu
./gradlew assembleDebug
# çıktı: app/build/outputs/apk/debug/app-debug.apk
```

Her push'ta `.github/workflows/android-apk.yml` APK'yı otomatik derleyip Release'e yükler.

## Kod yapısı

```
app/src/main/java/com/tenis/koc/
  MainActivity.kt            Gezinme (Bugün / Geçmiş / Ayarlar, canlı ve video ekranları)
  analysis/Frame.kt          Bir karedeki vücut noktaları ve açı hesapları
  analysis/StrokeAnalyzer.kt Vuruş yakalama, tür belirleme ve hata kuralları
  analysis/VideoAnalyzer.kt  Kayıtlı videoyu kare kare analiz
  camera/PoseCamera.kt       CameraX + ML Kit, iskelet çizimi, sesli koç
  plan/Coaching.kt           Günün özeti ve kişisel günlük plan
  data/Stores.kt             Antrenmanların ve ayarların cihazda saklanması
  model/Models.kt            Vuruş, hata kataloğu (ipuçları + drill'ler), antrenman
  ui/                        Compose ekranları
```
