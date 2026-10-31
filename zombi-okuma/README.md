# 🧟 Zombi Okuma Kahramanı (Android)

"Okuma Kahramanı – A-N-E-T-İ-L" kitapçığındaki hece, kelime ve cümlelerle
hazırlanmış sesli okuma oyunu.

**Nasıl oynanır?** Üstteki panoda heceler tek tek zıplar, sonra birleşerek
kelimeyi oluşturur. Çocuk **🎤 SESLİ OKU** düğmesine basıp okur. Okuma doğruysa
zombi yıldızlara dönüşüp yok olur ve yeni kelime gelir. Zombi kahramana
ulaşırsa bir can gider (3 can).

- Çocuk okurken zombi **durur** – amaç hız değil, doğru okuma.
- **🔊 Dinle**: kelimeyi seslendirir (ipucu, puan 10 yerine 5).
- **👍 (basılı tut)**: ses tanıma çocuğu duyamazsa yetişkin onayı.
- Panoya dokununca heceler tekrar ayrılıp birleşir.

## Görevler (kitapçıktan)

| Görev | İçerik |
|---|---|
| 🚂 1. Hece Avcısı | al, la, at, na, an, ne, nen, ni, nil, el, le, en, ta, te, et, tan, tel, ten, net, il, li, lan |
| 🚀 2. Kelime Roketi | A-li, E-la, Nil, La-le, na-ne, at, el, tel, a-na, i-le |
| 🔎 3. Cümle Dedektifi | Ela → Ela ile → Ela nane → Ela nane al. · Ali al. → Ali el al. → Ali Ela ile el al. · At al. → Lale at al. → Ali Lale at al. · Nil nane al. · Nil el al. · Lale ile Nil nane al. |

Kelime eklemek için `app/src/main/java/com/okumakahramani/zombi/WordBank.java`
dosyasını düzenleyin (heceler tire, kelimeler boşlukla ayrılır).

## APK'yı alma

Bu klasörde değişiklik push edildiğinde GitHub Actions APK'yı otomatik derler:

- **Releases → "Zombi Okuma (son sürüm)"** altından `zombi-okuma.apk` indirilebilir, veya
- **Actions → "Zombi Okuma - Android APK"** çalıştırmasının *Artifacts* bölümünden.

Kendiniz derlemek için (Android Studio veya Android SDK kurulu bilgisayarda):

```bash
cd zombi-okuma
./gradlew assembleDebug
# app/build/outputs/apk/debug/app-debug.apk
```

## Gereksinimler

- Android 7.0+ (API 24)
- Telefonda Google ses tanıma (Google uygulaması) ve Türkçe dil desteği.
  Çoğu cihazda ses tanıma için internet gerekir.
