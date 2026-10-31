# 🧟 Zombi Okuma Kahramanı — Web (Next.js)

"Okuma Kahramanı – A-N-E-T-İ-L" kitapçığındaki hece, kelime ve cümlelerle sesli
okuma oyunu. Android uygulamasının (`../zombi-okuma`) tarayıcı sürümü.

- Heceler panoda sırayla zıplar, sonra birleşip kelimeyi oluşturur.
- **🎤 SESLİ OKU**: çocuk okur; doğruysa zombi yıldızlara dönüşür, yeni kelime gelir.
- Çocuk okurken zombi durur. **Yanlış okursa** zombi öne atılır ve hızlanır
  (her yanlışta ×1,6, en fazla ×4; yeni kelimede sıfırlanır). Ses duyulmazsa ceza yok.
- **🔊 Dinle**: kelimeyi Türkçe seslendirir (ipucu: puan 10 yerine 5).
- **👍 basılı tut**: ses tanıma duyamazsa yetişkin onayı.
- Yıldızlar tarayıcıda (localStorage) saklanır.

## Tarayıcı desteği

Ses tanıma için **Web Speech API** kullanılır: **Chrome** (Android ve bilgisayar)
ve Edge'de çalışır; Firefox desteklemez. Mikrofon için site **HTTPS** üzerinden
açılmalıdır (aşağıdaki tüm dağıtım seçenekleri HTTPS verir). Chrome'da ses
tanıma internet bağlantısı ister.

## Geliştirme

```bash
npm install
npm run dev      # http://localhost:3000
npm test         # kelime eşleştirme testleri
npm run build
```

## Dağıtım (deploy)

**Vercel (önerilen):** vercel.com → *Add New Project* → bu repoyu seçin →
**Root Directory: `zombi-okuma-web`** → Deploy. Başka ayar gerekmez.

**Netlify / Cloudflare Pages / herhangi bir statik barındırma:**

```bash
STATIC_EXPORT=1 npm run build   # çıktı: out/
```

`out/` klasörünü yükleyin. Alt dizinde yayınlanacaksa (ör. GitHub Pages
`kullanici.github.io/repo/`) `BASE_PATH=/repo` ekleyin.

**Kendi sunucunuz (Node):** `npm run build && npm start`

## Kelime eklemek

`lib/words.ts` → ilgili seviyenin `specs` dizisi (heceler tire, kelimeler boşlukla:
`"La-le i-le Nil na-ne al."`).
