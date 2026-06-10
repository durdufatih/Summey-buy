// Web Speech API sarmalayıcısı: Türkçe konuşma tanıma + İngilizce/Türkçe seslendirme.
const Speech = (() => {
  const SR = window.SpeechRecognition || window.webkitSpeechRecognition;

  function isRecognitionSupported() {
    return Boolean(SR);
  }

  // onInterim(text): henüz kesinleşmemiş ara sonuç
  // onFinal(text): kesinleşen cümle/parça
  // onStateChange(listening): dinleme durumu değişti
  function createRecognizer({ onInterim, onFinal, onStateChange, onError }) {
    if (!SR) return null;

    const rec = new SR();
    rec.lang = "tr-TR";
    rec.continuous = true;
    rec.interimResults = true;

    let wantListening = false;

    rec.onresult = (event) => {
      let interim = "";
      for (let i = event.resultIndex; i < event.results.length; i++) {
        const result = event.results[i];
        const transcript = result[0].transcript;
        if (result.isFinal) {
          if (transcript.trim()) onFinal(transcript.trim());
        } else {
          interim += transcript;
        }
      }
      onInterim(interim);
    };

    rec.onerror = (event) => {
      if (event.error === "no-speech" || event.error === "aborted") return;
      wantListening = false;
      onError?.(event.error);
      onStateChange(false);
    };

    // Tarayıcı tanımayı kendiliğinden durdurursa (sessizlik vb.) yeniden başlat.
    rec.onend = () => {
      if (wantListening) {
        try { rec.start(); } catch { /* zaten başlamış */ }
      } else {
        onStateChange(false);
      }
    };

    return {
      start() {
        wantListening = true;
        try { rec.start(); } catch { /* zaten dinliyor */ }
        onStateChange(true);
      },
      stop() {
        wantListening = false;
        rec.stop();
      },
      get listening() {
        return wantListening;
      }
    };
  }

  function speak(text, lang = "en-US") {
    if (!("speechSynthesis" in window) || !text) return;
    const utter = new SpeechSynthesisUtterance(text);
    utter.lang = lang;
    const voice = speechSynthesis
      .getVoices()
      .find((v) => v.lang.toLowerCase().startsWith(lang.slice(0, 2).toLowerCase()));
    if (voice) utter.voice = voice;
    speechSynthesis.cancel();
    speechSynthesis.speak(utter);
  }

  return { isRecognitionSupported, createRecognizer, speak };
})();
