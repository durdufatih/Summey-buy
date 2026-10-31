/**
 * "Okuma Kahramanı – A-N-E-T-İ-L" kitapçığından alınan heceler, kelimeler ve cümleler.
 * Tanım biçimi: kelimeler boşlukla, heceler tire ile ayrılır ("E-la na-ne al.").
 */

export interface WordItem {
  /** Kelime → hece dizisi. */
  syllables: string[][];
  /** Düz metin ("Ela nane al."). */
  text: string;
  syllableCount: number;
}

export function parseWord(spec: string): WordItem {
  const words = spec.trim().split(/\s+/);
  const syllables = words.map((w) => w.split("-"));
  return {
    syllables,
    text: words.map((w) => w.replace(/-/g, "")).join(" "),
    syllableCount: syllables.reduce((n, s) => n + s.length, 0),
  };
}

export interface Level {
  emoji: string;
  title: string;
  subtitle: string;
  color: string;
  /** Zombi hızı: saniyede ekran genişliğinin kesri. */
  speed: number;
  shuffle: boolean;
  roundSize: number;
  specs: string[];
}

export const LEVELS: Level[] = [
  {
    // Görev 2 • Hece treni
    emoji: "🚂",
    title: "Hece Avcısı",
    subtitle: "Heceleri oku",
    color: "#FFEFA8",
    speed: 0.036,
    shuffle: true,
    roundSize: 10,
    specs: [
      "al", "la", "at", "na", "an", "ne", "nen", "ni", "nil", "el", "le",
      "en", "ta", "te", "et", "tan", "tel", "ten", "net", "il", "li", "lan",
    ],
  },
  {
    // Görev 3 • Kelime roketi
    emoji: "🚀",
    title: "Kelime Roketi",
    subtitle: "Heceleri birleştir, kelimeyi oku",
    color: "#F6C6E0",
    speed: 0.032,
    shuffle: true,
    roundSize: 10,
    specs: ["A-li", "E-la", "Nil", "La-le", "na-ne", "at", "el", "tel", "a-na", "i-le"],
  },
  {
    // Görev 5-7 • Cümle piramidi / tamamlama / okuduğunu anla
    emoji: "🔎",
    title: "Cümle Dedektifi",
    subtitle: "Cümleyi baştan sona oku",
    color: "#CDE8FB",
    speed: 0.023,
    shuffle: false,
    roundSize: 13,
    specs: [
      "E-la", "E-la i-le", "E-la na-ne", "E-la na-ne al.",
      "A-li al.", "A-li el al.", "A-li E-la i-le el al.",
      "At al.", "La-le at al.", "A-li La-le at al.",
      "Nil na-ne al.", "Nil el al.", "La-le i-le Nil na-ne al.",
    ],
  },
];

export function buildRound(level: Level): WordItem[] {
  const pool = [...level.specs];
  if (level.shuffle) {
    for (let i = pool.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [pool[i], pool[j]] = [pool[j], pool[i]];
    }
  }
  return pool.slice(0, level.roundSize).map(parseWord);
}
