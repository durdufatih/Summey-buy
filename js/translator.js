// Türkçe → İngilizce çeviri. Anahtar gerektirmeyen ücretsiz servisler kullanılır:
// önce Google'ın herkese açık ucu denenir, başarısız olursa MyMemory'ye düşülür.
const Translator = (() => {
  const cache = new Map();

  async function viaGoogle(text) {
    const url =
      "https://translate.googleapis.com/translate_a/single?client=gtx&sl=tr&tl=en&dt=t&q=" +
      encodeURIComponent(text);
    const res = await fetch(url);
    if (!res.ok) throw new Error("google " + res.status);
    const data = await res.json();
    const translated = (data[0] || []).map((part) => part[0]).join("");
    if (!translated) throw new Error("google empty");
    return translated;
  }

  async function viaMyMemory(text) {
    const url =
      "https://api.mymemory.translated.net/get?langpair=tr|en&q=" +
      encodeURIComponent(text);
    const res = await fetch(url);
    if (!res.ok) throw new Error("mymemory " + res.status);
    const data = await res.json();
    const translated = data?.responseData?.translatedText;
    if (!translated || data.responseStatus !== 200) throw new Error("mymemory empty");
    return translated;
  }

  async function translate(text) {
    const trimmed = (text || "").trim();
    if (!trimmed) return "";
    if (cache.has(trimmed)) return cache.get(trimmed);

    let result;
    try {
      result = await viaGoogle(trimmed);
    } catch {
      result = await viaMyMemory(trimmed);
    }
    cache.set(trimmed, result);
    return result;
  }

  return { translate };
})();
