# 🌐 Anlık Çeviri — Türkçe → İngilizce

Türkçe konuşmayı **anlık olarak dinleyip İngilizceye çeviren**, sesli ve yazılı
çalışan, seviye seviye pratik paragrafları içeren bir web uygulaması.
Kurulum ve sunucu gerektirmez; tamamen tarayıcıda çalışır.

## Özellikler

- 🎤 **Sesli Çeviri:** Mikrofonla Türkçe dinler (Web Speech API, `tr-TR`),
  kesinleşen her cümleyi anında İngilizceye çevirir ve isterseniz sesli okur.
- ✍️ **Yazılı Çeviri:** Türkçe metni yazarken otomatik (veya butonla) çevirir,
  İngilizcesini dinletebilir ve panoya kopyalayabilirsiniz.
- 📚 **Seviyeli Pratik:** A1'den B2+'ya kadar seviyelendirilmiş paragraflar.
  Her paragraf cümle cümle çalışılır.
- 🛟 **Destek Modu:** Cümleler ve çevirileri gizli başlar; önce dinleyip kendiniz
  çevirmeyi denersiniz, takılınca **“Cümleyi Göster”** ve **“Çeviriyi Göster”**
  butonlarıyla destek alırsınız.

## Çalıştırma

Herhangi bir statik sunucuyla açmanız yeterli:

```bash
# Python ile
python3 -m http.server 8000
# sonra tarayıcıda: http://localhost:8000
```

> **Not:** Mikrofon erişimi için sayfanın `localhost` veya HTTPS üzerinden
> açılması gerekir. Konuşma tanıma en iyi Chrome / Edge tarayıcılarında çalışır.

## Teknik Detaylar

| Bileşen | Teknoloji |
|---|---|
| Konuşma tanıma | Web Speech API (`SpeechRecognition`, dil `tr-TR`) |
| Seslendirme | Web Speech API (`speechSynthesis`) |
| Çeviri | Google translate ücretsiz ucu, yedek olarak MyMemory API (anahtar gerekmez) |
| Pratik içeriği | `js/paragraphs-data.js` içinde gömülü (çevrimdışı çalışır) |

## Dosya Yapısı

```
index.html              Uygulama iskeleti (3 sekme)
css/style.css           Arayüz stilleri
js/app.js               Sekmeler ve tüm arayüz mantığı
js/speech.js            Konuşma tanıma + seslendirme sarmalayıcısı
js/translator.js        Çeviri servisi (önbellekli, yedekli)
js/paragraphs-data.js   Seviye seviye pratik paragrafları
```

## Yeni Paragraf Ekleme

`js/paragraphs-data.js` dosyasındaki ilgili seviyenin `paragraphs` dizisine
yeni bir nesne ekleyin; her cümle için `tr` ve `en` alanlarını doldurmanız
yeterlidir.
