# 🎾 Tenis Koçu — Android

Telefon kamerasıyla seni izleyip **her vuruşunu analiz eden**, hatalarını ve dikkat etmen
gereken noktaları gösteren, **günün planını** ve **günün özetini** çıkaran Android uygulaması.

## Özellikler

- **Canlı kamera analizi** — Vücudundaki 33 nokta (omuz, dirsek, bilek, kalça, diz, ayak bileği…)
  cihaz üzerinde TensorFlow Lite + BlazePose ile tespit edilir, iskelet görüntünün üstüne çizilir. İnternet gerekmez,
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

1. Hazır APK: [`apk/TenisKocu.apk`](apk/TenisKocu.apk) — GitHub'da dosyayı açıp **Download raw file** ile
   telefonuna indir (Actions çalışıyorsa Releases → “Tenis Koçu (son sürüm)” sayfasında da bulunur).
2. Dosyayı aç; istenirse “Bilinmeyen kaynaklardan uygulama yükleme” iznini ver.
3. Android 8.0 (API 26) ve üzeri, ARM işlemcili telefon gerekir (neredeyse tüm telefonlar).

## Kullanım ipuçları

- Telefonu 4–6 m uzağa, bel hizasında tripoda dik yerleştir; baştan ayağa tüm vücudun görünsün.
- Kamerayı tam önüne veya arkana koymak omuz dönüşü ölçümünü en doğru yapar.
- Ayarlardan sağ/sol el seçimini yap.

> Not: Top ve raket takip edilmez; değerlendirilen şey vücut tekniğidir. Eşikler sezgiseldir,
> sonuçlar eğilim gösterir ve bir antrenörün yerini tutmaz.

## Derleme

Gradle ve Android Studio gerekmez; Ubuntu/Debian paketleriyle derlenir:

```bash
sudo apt-get install android-sdk-platform-23 aapt dalvik-exchange apksigner zipalign default-jdk
./build.sh          # çıktı: apk/TenisKocu.apk
```

`build.sh` TensorFlow Lite'ı Maven Central'dan, BlazePose modellerini PyPI'deki `mediapipe`
paketinden indirir. `.github/workflows/android-apk.yml` aynı betikle her push'ta APK üretir.

## Kod yapısı

```
src/com/tenis/koc/
  MainActivity.java     Bugün (plan + özet) / Geçmiş / Ayarlar sekmeleri
  LiveActivity.java     Kamera, canlı iskelet çizimi, anlık geri bildirim
  VideoActivity.java    Kayıtlı videoyu kare kare analiz
  DetailActivity.java   Antrenman özeti ve vuruş vuruş liste
  PoseEstimator.java    BlazePose (TFLite): kişi tespiti + 33 nokta, kareler arası takip
  PixelSource.java      Kamera (NV21) ve bitmap piksellerini döndürerek okuma
  StrokeAnalyzer.java   Vuruş yakalama, tür belirleme ve hata kuralları
  Coaching.java         Günün özeti ve kişisel günlük plan
  Fault.java            Hata kataloğu (ipuçları + drill'ler)
  Store.java            Antrenmanlar ve ayarların cihazda saklanması
  Ui.java, Overlays.java  Arayüz yardımcıları, iskelet görünümü, sesli koç
```

Poz modelleri: MediaPipe BlazePose (Apache 2.0). TensorFlow Lite (Apache 2.0).
