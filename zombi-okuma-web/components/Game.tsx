"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { GameEngine } from "@/lib/engine";
import { findMatch } from "@/lib/matcher";
import { Listener, getRecognitionCtor, hasTurkishVoice, initSpeech, speak, stopSpeaking } from "@/lib/speech";
import { LEVELS, buildRound, type WordItem } from "@/lib/words";
import { saveStars, starText } from "@/lib/progress";

const MAX_HEARTS = 3;
const PRAISE = ["Aferin!", "Harika!", "Süpersin!", "Bravo!", "Çok güzel okudun!"];

type End = null | { win: boolean; stars: number };

export default function Game({
  levelIndex,
  onExit,
  onNextLevel,
}: {
  levelIndex: number;
  onExit: () => void;
  onNextLevel: () => void;
}) {
  const level = LEVELS[levelIndex];
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const engineRef = useRef<GameEngine | null>(null);
  const listenerRef = useRef(new Listener());

  // Oyun durumu (geri çağrılarda güncel kalsın diye ref'te)
  const g = useRef({
    words: [] as WordItem[],
    index: 0,
    hearts: MAX_HEARTS,
    score: 0,
    answered: false,
    over: false,
    hintUsed: false,
  });

  const [hud, setHud] = useState({ hearts: MAX_HEARTS, score: 0, index: 0, total: 0 });
  const [listening, setListening] = useState(false);
  const [end, setEnd] = useState<End>(null);
  const [holdProgress, setHoldProgress] = useState(0);

  const syncHud = useCallback(() => {
    const s = g.current;
    setHud({ hearts: s.hearts, score: s.score, index: s.index, total: s.words.length });
  }, []);

  const msg = (text: string, color?: string) => engineRef.current?.showMessage(text, color);

  const showWord = useCallback(() => {
    const s = g.current;
    s.answered = false;
    s.hintUsed = false;
    engineRef.current?.setWord(s.words[s.index], level.speed);
    syncHud();
  }, [level.speed, syncHud]);

  const stopListening = useCallback(() => {
    listenerRef.current.abort();
    engineRef.current?.setListening(false);
    setListening(false);
  }, []);

  const startRound = useCallback(() => {
    stopListening();
    const s = g.current;
    s.words = buildRound(level);
    s.index = 0;
    s.hearts = MAX_HEARTS;
    s.score = 0;
    s.over = false;
    setEnd(null);
    engineRef.current?.setPaused(false);
    showWord();
    msg("Zombiler geliyor! Panodakini oku!", "#FFE066");
  }, [level, showWord, stopListening]);

  const onCorrect = useCallback(() => {
    const s = g.current;
    if (s.answered || s.over) return;
    s.answered = true;
    s.score += s.hintUsed ? 5 : 10;
    engineRef.current?.killZombie();
    const praise = PRAISE[Math.floor(Math.random() * PRAISE.length)];
    msg("🎉 " + praise, "#8CF0A0");
    speak(praise);
    syncHud();
  }, [syncHud]);

  const onWrong = useCallback((heard: string | null) => {
    if (!heard || !heard.trim()) {
      // Hiç ses duyulmadıysa ceza yok
      msg("Seni duyamadım 🙂 Tekrar dene!", "#FFC2C2");
      return;
    }
    const boost = engineRef.current?.angerZombie() ?? 1;
    msg(`"${heard}" değil! Zombi ${boost >= 3.9 ? "çok hızlı!" : "hızlandı!"} 💨`, "#FF9A9A");
  }, []);

  // ------------------------------------------------------------ motor

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    initSpeech();
    const engine = new GameEngine(canvas, {
      onZombieReachedHero: () => {
        const s = g.current;
        if (s.answered || s.over) return;
        s.hearts--;
        syncHud();
        if (s.hearts <= 0) {
          s.over = true;
          stopListening();
          engine.setPaused(true);
          setEnd({ win: false, stars: 0 });
        } else {
          engine.showMessage("Ah! Bir can gitti. Yavaş yavaş oku!", "#FFC2C2");
        }
      },
      onZombieVanished: () => {
        const s = g.current;
        if (s.over) return;
        s.index++;
        if (s.index >= s.words.length) {
          s.over = true;
          stopListening();
          const stars = s.hearts >= 3 ? 3 : s.hearts === 2 ? 2 : 1;
          saveStars(levelIndex, stars);
          setEnd({ win: true, stars });
          speak("Tebrikler! Okuma kahramanı oldun!");
        } else {
          showWord();
        }
      },
    });
    engineRef.current = engine;
    const ro = new ResizeObserver(() => engine.resize());
    ro.observe(canvas);
    startRound();

    const onVisibility = () => {
      if (document.hidden) {
        stopListening();
        stopSpeaking();
        engine.setPaused(true);
      } else if (!g.current.over) {
        engine.setPaused(false);
      }
    };
    document.addEventListener("visibilitychange", onVisibility);

    const listener = listenerRef.current;
    return () => {
      document.removeEventListener("visibilitychange", onVisibility);
      ro.disconnect();
      listener.abort();
      stopSpeaking();
      engine.destroy();
      engineRef.current = null;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [levelIndex]);

  // ------------------------------------------------------------ kontroller

  const onMic = () => {
    const s = g.current;
    if (s.over || s.answered) return;
    const listener = listenerRef.current;
    if (listener.isActive) {
      listener.stop();
      return;
    }
    if (!getRecognitionCtor()) {
      msg("Bu tarayıcıda ses tanıma yok. Chrome kullanın.", "#FFC2C2");
      return;
    }
    stopSpeaking();
    setListening(true);
    engineRef.current?.setListening(true);
    const target = () => s.words[s.index].text;
    const done = () => {
      setListening(false);
      engineRef.current?.setListening(false);
    };
    listener.start({
      onStart: () => msg("Şimdi oku! 📖", "#FFE066"),
      onPartial: (alts) => {
        if (s.answered || s.over) return true;
        if (findMatch(target(), alts)) {
          done();
          onCorrect();
          return true;
        }
        return false;
      },
      onFinal: (alts) => {
        done();
        if (s.answered || s.over) return;
        if (findMatch(target(), alts)) onCorrect();
        else onWrong(alts[0] ?? null);
      },
      onError: (e) => {
        done();
        if (s.answered || s.over) return;
        if (e === "no-speech") onWrong(null);
        else if (e === "not-allowed") msg("Mikrofon izni gerekli 🎤", "#FFC2C2");
        else if (e === "network") msg("İnternet bağlantısını kontrol et", "#FFC2C2");
        else if (e === "unsupported") msg("Bu tarayıcıda ses tanıma yok. Chrome kullanın.", "#FFC2C2");
        else msg("Tekrar dene 🙂", "#FFC2C2");
      },
    });
  };

  const onListen = () => {
    const s = g.current;
    if (s.over || !s.words.length) return;
    if (listenerRef.current.isActive) stopListening();
    s.hintUsed = true;
    engineRef.current?.replayBoard();
    if (!hasTurkishVoice()) msg("Türkçe ses bulunamadı, yine de deniyorum", "#FFE066");
    speak(s.words[s.index].text, () => msg("Seslendirme çalışmadı 😕", "#FFC2C2"));
  };

  // 👍 yetişkin onayı: 0,8 sn basılı tutulmalı
  const holdTimer = useRef<number | null>(null);
  const holdStart = () => {
    const t0 = performance.now();
    const tick = () => {
      const p = Math.min(1, (performance.now() - t0) / 800);
      setHoldProgress(p);
      if (p >= 1) {
        holdTimer.current = null;
        setHoldProgress(0);
        const s = g.current;
        if (!s.answered && !s.over && engineRef.current?.isZombieActive()) {
          stopListening();
          onCorrect();
        }
        return;
      }
      holdTimer.current = requestAnimationFrame(tick);
    };
    holdTimer.current = requestAnimationFrame(tick);
  };
  const holdEnd = () => {
    if (holdTimer.current != null) {
      cancelAnimationFrame(holdTimer.current);
      holdTimer.current = null;
      if (holdProgress < 1) msg("Yetişkin onayı için basılı tutun", "#FFE066");
    }
    setHoldProgress(0);
  };

  const onCanvasPointer = (e: React.PointerEvent<HTMLCanvasElement>) => {
    const r = e.currentTarget.getBoundingClientRect();
    if (engineRef.current?.hitBoard(e.clientX - r.left, e.clientY - r.top)) engineRef.current.replayBoard();
  };

  const hearts = Array.from({ length: MAX_HEARTS }, (_, i) => (i < hud.hearts ? "❤️" : "🤍")).join("");

  return (
    <div className="game">
      <div className="stage">
        <canvas ref={canvasRef} className="canvas" onPointerDown={onCanvasPointer} />
        <div className="hud">
          <button className="btn small" onClick={onExit} aria-label="Menüye dön">
            ⬅
          </button>
          <span className="hearts" aria-label={`${hud.hearts} can`}>
            {hearts}
          </span>
          <span className="hud-title">
            {level.emoji} {level.title}
          </span>
          <span className="progress">
            {Math.min(hud.index + 1, hud.total)} / {hud.total}
          </span>
          <span className="score">⭐ {hud.score}</span>
        </div>

        {end && (
          <div className="end-panel" role="dialog">
            {end.win ? (
              <>
                <h2>🏆 Tebrikler, Okuma Kahramanı!</h2>
                <p className="stars">{starText(end.stars)}</p>
                <p>Tüm zombileri yendin! Puanın: {hud.score}</p>
              </>
            ) : (
              <>
                <h2>🧟 Zombiler bu sefer kazandı</h2>
                <p>
                  Hata yapabilirim. Tekrar denerim.
                  <br />
                  Her gün biraz daha öğrenirim! (Puan: {hud.score})
                </p>
              </>
            )}
            <div className="end-buttons">
              <button className="btn" style={{ background: "#D9F5D0" }} onClick={startRound}>
                🔁 Tekrar Oyna
              </button>
              <button className="btn" style={{ background: "#CDE8FB" }} onClick={onExit}>
                🏠 Menü
              </button>
              {end.win && levelIndex + 1 < LEVELS.length && (
                <button className="btn" style={{ background: "#FFEFA8" }} onClick={onNextLevel}>
                  ➡ Sonraki Görev
                </button>
              )}
            </div>
          </div>
        )}
      </div>

      <div className="controls">
        <button className="btn" style={{ background: "#CDE8FB" }} onClick={onListen}>
          🔊 Dinle
        </button>
        <button className={`mic ${listening ? "on" : ""}`} onClick={onMic}>
          {listening ? "👂 Dinliyorum… oku!" : "🎤 SESLİ OKU"}
        </button>
        <button
          className="btn approve"
          style={{ background: "#D9F5D0" }}
          onPointerDown={holdStart}
          onPointerUp={holdEnd}
          onPointerLeave={holdEnd}
          onPointerCancel={holdEnd}
          onContextMenu={(e) => e.preventDefault()}
          aria-label="Yetişkin onayı (basılı tutun)"
        >
          <span className="hold" style={{ transform: `scaleX(${holdProgress})` }} />
          👍
        </button>
      </div>
    </div>
  );
}
