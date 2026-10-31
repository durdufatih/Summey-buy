"use client";

import { useEffect, useState } from "react";
import { LEVELS } from "@/lib/words";
import { loadStars, starText } from "@/lib/progress";
import { getRecognitionCtor } from "@/lib/speech";

export default function Menu({ onPlay }: { onPlay: (level: number) => void }) {
  const [stars, setStars] = useState<number[]>(LEVELS.map(() => 0));
  const [supported, setSupported] = useState(true);

  useEffect(() => {
    setStars(LEVELS.map((_, i) => loadStars(i)));
    setSupported(!!getRecognitionCtor());
  }, []);

  return (
    <main className="menu">
      <h1>🧟 Zombi Okuma Kahramanı 🦸</h1>
      <p className="lead">
        Panodaki heceyi, kelimeyi ya da cümleyi <b>SESLİ</b> oku. Doğru okursan zombi yok olur!
      </p>

      {!supported && (
        <p className="warn">
          Bu tarayıcı ses tanımayı desteklemiyor. Lütfen <b>Chrome</b> (Android / bilgisayar) ile açın.
        </p>
      )}

      <div className="levels">
        {LEVELS.map((lv, i) => (
          <button key={lv.title} className="level" style={{ background: lv.color }} onClick={() => onPlay(i)}>
            <span className="level-emoji">{lv.emoji}</span>
            <span className="level-text">
              <b>
                {i + 1}. {lv.title}
              </b>
              <small>{lv.subtitle}</small>
            </span>
            <span className="level-stars">{starText(stars[i])}</span>
          </button>
        ))}
      </div>

      <p className="note">
        <b>Yetişkin için:</b> Amaç hız değil, doğru seslendirme. Çocuk okurken zombi durur; yanlış okursa
        zombi hızlanır. Ses tanıma kelimeyi duyamazsa 👍 düğmesine <b>basılı tutarak</b> onaylayabilirsiniz.
        Kelimeler “Okuma Kahramanı – A-N-E-T-İ-L” kitapçığından alınmıştır.
      </p>
    </main>
  );
}
