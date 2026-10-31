/** Tarayıcı konuşma tanıma (Web Speech API) ve seslendirme sarmalayıcıları. */

/* eslint-disable @typescript-eslint/no-explicit-any */
type RecognitionCtor = new () => any;

export function getRecognitionCtor(): RecognitionCtor | null {
  if (typeof window === "undefined") return null;
  const w = window as any;
  return w.SpeechRecognition || w.webkitSpeechRecognition || null;
}

export type ListenError = "no-speech" | "not-allowed" | "network" | "unsupported" | "other";

export interface ListenHandlers {
  /** Ara sonuçlar; true dönerse dinleme hemen bitirilir. */
  onPartial: (alternatives: string[]) => boolean;
  /** Son sonuçlar (boş olabilir). */
  onFinal: (alternatives: string[]) => void;
  onError: (e: ListenError) => void;
  onStart: () => void;
}

export class Listener {
  private rec: any = null;
  private active = false;
  private session = 0;

  get isActive() {
    return this.active;
  }

  start(h: ListenHandlers) {
    const Ctor = getRecognitionCtor();
    if (!Ctor) {
      h.onError("unsupported");
      return;
    }
    this.abort();
    const rec = new Ctor();
    this.rec = rec;
    const id = ++this.session;
    let done = false;
    let heard: string[] = [];
    rec.lang = "tr-TR";
    rec.continuous = false;
    rec.interimResults = true;
    rec.maxAlternatives = 5;

    // Eski bir oturumun geç gelen olayları yok sayılır
    const finish = (fn: () => void) => {
      if (done || id !== this.session) return;
      done = true;
      this.active = false;
      fn();
    };

    rec.onstart = () => h.onStart();
    rec.onresult = (ev: any) => {
      const alts: string[] = [];
      let final = false;
      for (let i = 0; i < ev.results.length; i++) {
        const r = ev.results[i];
        if (r.isFinal) final = true;
        for (let j = 0; j < r.length; j++) alts.push(r[j].transcript);
      }
      // birden çok parça geldiyse birleşik metni de dene
      if (ev.results.length > 1) {
        let joined = "";
        for (let i = 0; i < ev.results.length; i++) joined += " " + ev.results[i][0].transcript;
        alts.unshift(joined.trim());
      }
      heard = alts;
      if (h.onPartial(alts)) {
        finish(() => {});
        try { rec.abort(); } catch { /* yoksay */ }
        return;
      }
      if (final) {
        finish(() => h.onFinal(alts));
        try { rec.stop(); } catch { /* yoksay */ }
      }
    };
    rec.onerror = (ev: any) => {
      const code: string = ev.error;
      if (code === "aborted") return;
      finish(() =>
        h.onError(
          code === "no-speech" ? "no-speech"
            : code === "not-allowed" || code === "service-not-allowed" ? "not-allowed"
            : code === "network" ? "network"
            : "other",
        ),
      );
    };
    rec.onend = () => finish(() => h.onFinal(heard));

    try {
      rec.start();
      this.active = true;
    } catch {
      this.active = false;
      h.onError("other");
    }
  }

  /** Konuşmayı bitir ve sonucu değerlendir. */
  stop() {
    try { this.rec?.stop(); } catch { /* yoksay */ }
  }

  /** Sonucu yok sayarak iptal et. */
  abort() {
    this.session++;
    this.active = false;
    try { this.rec?.abort(); } catch { /* yoksay */ }
    this.rec = null;
  }
}

// ------------------------------------------------------------ seslendirme

let turkishVoice: SpeechSynthesisVoice | null = null;

function pickVoice() {
  if (typeof window === "undefined" || !window.speechSynthesis) return;
  const voices = window.speechSynthesis.getVoices();
  turkishVoice =
    voices.find((v) => v.lang.toLowerCase() === "tr-tr" && /google/i.test(v.name)) ||
    voices.find((v) => v.lang.toLowerCase().startsWith("tr")) ||
    null;
}

export function initSpeech() {
  if (typeof window === "undefined" || !window.speechSynthesis) return;
  pickVoice();
  window.speechSynthesis.addEventListener?.("voiceschanged", pickVoice);
}

export function canSpeak() {
  return typeof window !== "undefined" && !!window.speechSynthesis;
}

export function hasTurkishVoice() {
  if (!turkishVoice) pickVoice();
  return !!turkishVoice;
}

/** Metni Türkçe okur. Başarısızsa onError çağrılır. */
export function speak(text: string, onError?: () => void) {
  if (!canSpeak()) {
    onError?.();
    return;
  }
  const synth = window.speechSynthesis;
  if (!turkishVoice) pickVoice();
  synth.cancel(); // Chrome'da takılı kalan kuyruğu temizle
  const u = new SpeechSynthesisUtterance(text);
  u.lang = "tr-TR";
  if (turkishVoice) u.voice = turkishVoice;
  u.rate = 0.8;
  u.volume = 1;
  u.onerror = (e) => {
    if (e.error !== "interrupted" && e.error !== "canceled") onError?.();
  };
  synth.speak(u);
  synth.resume();
}

export function stopSpeaking() {
  if (canSpeak()) window.speechSynthesis.cancel();
}
