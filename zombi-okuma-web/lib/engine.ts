import type { WordItem } from "./words";

/**
 * Oyun sahnesi (canvas): üstte hece/kelime panosu, altta kahramana doğru yürüyen zombi.
 * Zombi, çocuk dinlenirken durur (amaç hız değil, doğru okuma).
 */

type ZState = "gone" | "enter" | "walk" | "knock" | "dying";

interface Particle {
  glyph: string;
  x: number; y: number; vx: number; vy: number;
  life: number; maxLife: number; size: number; rot: number; vrot: number;
}

export interface EngineEvents {
  onZombieReachedHero: () => void;
  onZombieVanished: () => void;
}

const HERO_X = 0.11;
const HIT_X = 0.25;
const START_X = 0.92;
const CHIP_COLORS = ["#CDE8FB", "#F6C6E0", "#FFEFA8", "#D9F5D0", "#E6DAFB", "#FDD9B5"];
const INK = "#26335A";
const FONT = 'ui-rounded, "SF Pro Rounded", "Nunito", "Segoe UI", system-ui, sans-serif';
const EMOJI_FONT = '"Apple Color Emoji", "Segoe UI Emoji", "Noto Color Emoji", sans-serif';

const clamp01 = (v: number) => (v < 0 ? 0 : v > 1 ? 1 : v);
const easeInOut = (t: number) => t * t * (3 - 2 * t);

export class GameEngine {
  private ctx: CanvasRenderingContext2D;
  private w = 0;
  private h = 0;
  private raf = 0;
  private last = 0;
  private stars: { x: number; y: number }[] = [];
  private particles: Particle[] = [];

  private word: WordItem | null = null;
  private speed = 0.03;
  private speedBoost = 1;
  private angryTime = 99;
  private zState: ZState = "gone";
  private zombieX = 1.2;
  private zStateTime = 0;

  private listening = false;
  private paused = false;
  private rms = 0;
  private time = 0;
  private boardTime = 0;
  private heroShake = 0;
  private board = { x: 0, y: 0, w: 0, h: 0 };

  private message: string | null = null;
  private messageColor = "#fff";
  private messageTime = 99;

  constructor(private canvas: HTMLCanvasElement, private events: EngineEvents) {
    const ctx = canvas.getContext("2d");
    if (!ctx) throw new Error("Canvas desteklenmiyor");
    this.ctx = ctx;
    for (let i = 0; i < 40; i++) this.stars.push({ x: Math.random(), y: Math.random() * 0.6 });
    this.resize();
    this.raf = requestAnimationFrame(this.frame);
  }

  destroy() {
    cancelAnimationFrame(this.raf);
  }

  resize() {
    const dpr = Math.min(window.devicePixelRatio || 1, 2.5);
    const rect = this.canvas.getBoundingClientRect();
    this.w = Math.max(1, rect.width);
    this.h = Math.max(1, rect.height);
    this.canvas.width = Math.round(this.w * dpr);
    this.canvas.height = Math.round(this.h * dpr);
    this.ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  }

  // ------------------------------------------------------------ kontrol

  setWord(word: WordItem, speed: number) {
    this.word = word;
    this.speed = speed;
    this.speedBoost = 1;
    this.angryTime = 99;
    this.zombieX = 1.15;
    this.zState = "enter";
    this.zStateTime = 0;
    this.boardTime = 0;
  }

  setListening(l: boolean) {
    this.listening = l;
    if (!l) this.rms = 0;
  }

  /** 0..1 arası ses seviyesi */
  setLevel(v: number) {
    this.rms = clamp01(v);
  }

  setPaused(p: boolean) {
    this.paused = p;
  }

  replayBoard() {
    this.boardTime = 0;
  }

  isZombieActive() {
    return this.zState === "walk" || this.zState === "enter" || this.zState === "knock";
  }

  killZombie() {
    if (this.zState === "gone" || this.zState === "dying") return;
    this.zState = "dying";
    this.zStateTime = 0;
    const cx = this.zombieX * this.w;
    const cy = this.groundY() - this.zombieSize() * 0.4;
    const glyphs = ["⭐", "✨", "🌟", "💥", "⭐"];
    for (let i = 0; i < 26; i++) {
      this.spawn(glyphs[Math.floor(Math.random() * glyphs.length)], cx, cy, this.zombieSize() * 0.22, 1.1);
    }
  }

  /** Yanlış okuma: zombi öne atılır ve hızlanır. Yeni çarpanı döndürür. */
  angerZombie(): number {
    if (this.zState !== "walk" && this.zState !== "enter") return this.speedBoost;
    this.speedBoost = Math.min(4, this.speedBoost * 1.6);
    this.zombieX = Math.max(HIT_X + 0.02, this.zombieX - 0.07);
    this.angryTime = 0;
    const cx = this.zombieX * this.w;
    for (let i = 0; i < 6; i++) {
      this.spawn("💢", cx, this.groundY() - this.zombieSize() * 0.8, this.zombieSize() * 0.18, 0.6);
    }
    return this.speedBoost;
  }

  showMessage(text: string, color = "#fff") {
    this.message = text;
    this.messageColor = color;
    this.messageTime = 0;
  }

  /** Panoya dokunuldu mu? (canvas CSS koordinatları) */
  hitBoard(x: number, y: number) {
    const b = this.board;
    return x >= b.x && x <= b.x + b.w && y >= b.y && y <= b.y + b.h;
  }

  // ------------------------------------------------------------ döngü

  /** Ölçek birimi: dar/dikey ekranlarda da orantılı kalsın. */
  private get u() {
    return Math.min(this.h, this.w * 0.8);
  }

  private groundY() {
    return this.h * 0.84;
  }

  private zombieSize() {
    return this.u * 0.26;
  }

  private frame = (now: number) => {
    const dt = this.last ? Math.min(0.05, (now - this.last) / 1000) : 0;
    this.last = now;
    if (!this.paused) this.update(dt);
    this.draw();
    this.raf = requestAnimationFrame(this.frame);
  };

  private update(dt: number) {
    this.time += dt;
    this.boardTime += dt;
    this.messageTime += dt;
    this.angryTime += dt;
    this.heroShake = Math.max(0, this.heroShake - dt);

    switch (this.zState) {
      case "enter":
        this.zombieX -= 0.5 * dt;
        if (this.zombieX <= START_X) {
          this.zombieX = START_X;
          this.zState = "walk";
        }
        break;
      case "walk":
        if (!this.listening) this.zombieX -= this.speed * this.speedBoost * dt;
        if (this.zombieX <= HIT_X) {
          this.zState = "knock";
          this.zStateTime = 0;
          this.heroShake = 0.6;
          for (let i = 0; i < 10; i++) {
            this.spawn("💥", HIT_X * this.w, this.groundY() - this.zombieSize() * 0.5, this.zombieSize() * 0.2, 0.7);
          }
          setTimeout(() => this.events.onZombieReachedHero(), 0);
        }
        break;
      case "knock":
        this.zStateTime += dt;
        this.zombieX += 1.4 * dt;
        if (this.zombieX >= START_X) {
          this.zombieX = START_X;
          this.zState = "walk";
        }
        break;
      case "dying":
        this.zStateTime += dt;
        if (this.zStateTime > 0.8) {
          this.zState = "gone";
          setTimeout(() => this.events.onZombieVanished(), 0);
        }
        break;
    }

    this.particles = this.particles.filter((p) => {
      p.life += dt;
      if (p.life >= p.maxLife) return false;
      p.vy += this.u * 1.2 * dt;
      p.x += p.vx * dt;
      p.y += p.vy * dt;
      p.rot += p.vrot * dt;
      return true;
    });
  }

  private spawn(glyph: string, x: number, y: number, size: number, life: number) {
    const a = Math.random() * Math.PI * 2;
    const v = this.u * (0.5 + Math.random() * 0.9);
    this.particles.push({
      glyph, x, y,
      vx: Math.cos(a) * v,
      vy: Math.sin(a) * v - this.u * 0.6,
      size: size * (0.6 + Math.random() * 0.8),
      maxLife: life * (0.7 + Math.random() * 0.6),
      life: 0,
      rot: 0,
      vrot: (Math.random() - 0.5) * 720,
    });
  }

  // ------------------------------------------------------------ çizim

  private emoji(glyph: string, x: number, y: number, size: number, alpha = 1) {
    const c = this.ctx;
    c.globalAlpha = alpha;
    c.font = `${size}px ${EMOJI_FONT}`;
    c.textAlign = "center";
    c.textBaseline = "alphabetic";
    c.fillText(glyph, x, y);
    c.globalAlpha = 1;
  }

  private draw() {
    const c = this.ctx;
    c.clearRect(0, 0, this.w, this.h);
    this.drawBackground();
    this.drawHero();
    this.drawZombie();
    for (const p of this.particles) {
      c.save();
      c.translate(p.x, p.y);
      c.rotate((p.rot * Math.PI) / 180);
      this.emoji(p.glyph, 0, 0, p.size, 1 - p.life / p.maxLife);
      c.restore();
    }
    this.drawBoard();
    this.drawMessage();
  }

  private drawBackground() {
    const { ctx: c, w, h, u } = this;
    const g = c.createLinearGradient(0, 0, 0, h);
    g.addColorStop(0, "#1B1446");
    g.addColorStop(0.5, "#3D2A7A");
    g.addColorStop(1, "#6A4BB0");
    c.fillStyle = g;
    c.fillRect(0, 0, w, h);

    c.fillStyle = "#fff";
    this.stars.forEach((s, i) => {
      const tw = 0.5 + 0.5 * Math.sin(this.time * 2 + i);
      c.globalAlpha = (80 + 150 * tw) / 255;
      c.beginPath();
      c.arc(s.x * w, s.y * h, u * 0.004 + tw * u * 0.002, 0, Math.PI * 2);
      c.fill();
    });
    c.globalAlpha = 1;

    this.emoji("🌕", w * 0.07, h * 0.3, u * 0.13);

    const gy = this.groundY();
    c.fillStyle = "#2F7D3A";
    c.fillRect(0, gy, w, h - gy);
    c.fillStyle = "#49A85A";
    c.fillRect(0, gy, w, h * 0.025);

    this.emoji("🪦", w * 0.42, gy + h * 0.01, u * 0.09, 0.67);
    this.emoji("🪦", w * 0.67, gy + h * 0.01, u * 0.09, 0.67);
    this.emoji("🌳", w * 0.86, gy + h * 0.02, u * 0.16, 0.67);
  }

  private drawHero() {
    const { ctx: c, u } = this;
    const size = u * 0.24;
    let x = HERO_X * this.w;
    if (this.heroShake > 0) x += Math.sin(this.heroShake * 60) * u * 0.015;
    const y = this.groundY() + size * 0.05 + Math.sin(this.time * 3) * u * 0.005;

    if (this.listening) {
      c.strokeStyle = "#FFE066";
      c.lineWidth = u * 0.008;
      for (let i = 0; i < 3; i++) {
        const ph = (this.time * 1.5 + i / 3) % 1;
        const r = size * (0.45 + ph * 0.6 + this.rms * 0.25);
        c.globalAlpha = 0.86 * (1 - ph);
        c.beginPath();
        c.arc(x, y - size * 0.4, r, 0, Math.PI * 2);
        c.stroke();
      }
      c.globalAlpha = 1;
    }
    this.emoji("🦸", x, y, size);
    this.emoji(this.listening ? "🎤" : "🛡️", x + size * 0.45, y - size * 0.25, size * 0.45);
  }

  private drawZombie() {
    if (this.zState === "gone" || !this.word) return;
    const { ctx: c, u } = this;
    const size = this.zombieSize();
    const x = this.zombieX * this.w;
    const moving = (this.zState === "walk" && !this.listening) || this.zState === "enter";
    const step = 5 * this.speedBoost;
    const bob = moving ? Math.abs(Math.sin(this.time * step)) * u * 0.015 : 0;
    const y = this.groundY() + size * 0.06 - bob;

    c.save();
    let alpha = 1;
    const pivotY = y - size * 0.4;
    if (this.zState === "dying") {
      const t = Math.min(1, this.zStateTime / 0.8);
      alpha = 1 - t;
      c.translate(x, pivotY);
      c.rotate((t * 540 * Math.PI) / 180);
      c.scale(1 - t, 1 - t);
      c.translate(-x, -pivotY);
    } else {
      let tilt = moving ? Math.sin(this.time * step) * (6 + 3 * (this.speedBoost - 1)) : 0;
      if (this.listening) tilt = Math.sin(this.time * 2) * 3;
      c.translate(x, y);
      c.rotate((tilt * Math.PI) / 180);
      c.translate(-x, -y);
    }
    this.emoji("🧟", x, y, size, alpha);

    if (this.speedBoost > 1.01 && this.zState === "walk") {
      const pulse = this.angryTime < 0.6 ? 1.4 - this.angryTime * 0.66 : 1;
      this.emoji("💢", x - size * 0.35, y - size * 0.8, size * 0.3 * pulse);
      c.font = `bold ${size * 0.16}px ${FONT}`;
      c.fillStyle = "#FFB3B3";
      c.textAlign = "center";
      c.fillText(`×${this.speedBoost.toFixed(1)}`, x, y - size * 1.05);
    }
    if (this.listening && this.zState === "walk") {
      this.emoji("❄️", x + size * 0.35, y - size * 0.75, size * 0.35);
    }
    c.restore();
  }

  private measureBoard(textSize: number, split: number) {
    const c = this.ctx;
    const word = this.word!;
    c.font = `bold ${textSize}px ${FONT}`;
    const pad = textSize * 0.16;
    const gap = textSize * 0.45 * split;
    const wordGap = textSize * 0.45;
    let total = 0;
    word.syllables.forEach((syl, wi) => {
      syl.forEach((s, si) => {
        total += c.measureText(s).width + pad * 2;
        if (si < syl.length - 1) total += gap;
      });
      if (wi < word.syllables.length - 1) total += wordGap;
    });
    return total;
  }

  private roundRect(x: number, y: number, w: number, h: number, r: number) {
    const c = this.ctx;
    c.beginPath();
    c.roundRect(x, y, w, h, r);
  }

  private drawBoard() {
    const word = this.word;
    if (!word) return;
    const { ctx: c, w, h } = this;

    const top = Math.max(h * 0.17, 56);
    const maxWidth = w * 0.86;

    let textSize = Math.min(h * 0.15, w * 0.12);
    while (this.measureBoard(textSize, 1) > maxWidth && textSize > 12) textSize *= 0.92;

    // Yükseklik içeriğe göre: hece kartları + altta ipucu satırı
    const hint = "panoya dokun: heceleri tekrar göster";
    const hintSize = Math.max(10, Math.min(textSize * 0.22, 16));
    const chipArea = textSize * 1.3 + textSize * 0.6; // zıplama payı dahil
    const boardH = chipArea + hintSize * 2.2;
    c.font = `${hintSize}px ${FONT}`;
    const hintW = c.measureText(hint).width + hintSize * 2;

    const hopEach = 0.45;
    const single = word.syllableCount === 1;
    const mergeStart = word.syllableCount * hopEach + 0.25;
    const merge = single ? 1 : easeInOut(clamp01((this.boardTime - mergeStart) / 0.6));

    const contentW = this.measureBoard(textSize, 1 - merge);
    const boardW = Math.min(w - 16, Math.max(contentW + textSize * 0.9, w * 0.3, hintW));
    const left = (w - boardW) / 2;
    this.board = { x: left, y: top, w: boardW, h: boardH };

    const r = Math.min(h, w) * 0.04;
    c.fillStyle = "rgba(0,0,0,0.33)";
    this.roundRect(left, top + h * 0.012, boardW, boardH, r);
    c.fill();
    c.fillStyle = "#fff";
    this.roundRect(left, top, boardW, boardH, r);
    c.fill();

    const pad = textSize * 0.16;
    const chipH = textSize * 1.3;
    const gap = textSize * 0.45 * (1 - merge);
    const wordGap = textSize * 0.45;
    const cy = top + textSize * 0.3 + chipArea / 2;

    c.font = `bold ${textSize}px ${FONT}`;
    c.textAlign = "center";
    c.textBaseline = "middle";

    let x = (w - contentW) / 2;
    let k = 0;
    word.syllables.forEach((syl) => {
      const wordStart = x;
      syl.forEach((s, si) => {
        const sw = c.measureText(s).width + pad * 2;
        const hopT = clamp01((this.boardTime - k * hopEach) / 0.4);
        const hop = hopT > 0 && hopT < 1 ? -Math.sin(Math.PI * hopT) * textSize * 0.3 : 0;
        const visible = single || this.boardTime >= k * hopEach;
        const appear = single ? 1 : clamp01((this.boardTime - k * hopEach) / 0.2);

        if (visible) {
          c.globalAlpha = appear;
          c.fillStyle = CHIP_COLORS[k % CHIP_COLORS.length];
          this.roundRect(x, cy - chipH / 2 + hop, sw, chipH, textSize * 0.22);
          c.fill();
          c.fillStyle = INK;
          c.fillText(s, x + sw / 2, cy + hop + textSize * 0.04);
          c.globalAlpha = 1;
        }
        x += sw;
        if (si < syl.length - 1) {
          if (gap > 2 && visible) {
            c.globalAlpha = 1 - merge;
            c.fillStyle = "#B0B6C8";
            c.fillText("-", x + gap / 2, cy);
            c.globalAlpha = 1;
          }
          x += gap;
        }
        k++;
      });
      if (merge > 0.05 && syl.length > 1) {
        c.globalAlpha = 0.8 * merge;
        c.strokeStyle = INK;
        c.lineWidth = Math.max(2, textSize * 0.05);
        this.roundRect(wordStart - 3, cy - chipH / 2 - 3, x - wordStart + 6, chipH + 6, textSize * 0.25);
        c.stroke();
        c.globalAlpha = 1;
      }
      x += wordGap;
    });

    c.font = `${hintSize}px ${FONT}`;
    c.fillStyle = "#8A90A8";
    c.textBaseline = "alphabetic";
    c.fillText(hint, w / 2, top + boardH - hintSize * 0.8);
  }

  private drawMessage() {
    if (!this.message || this.messageTime > 2.2) return;
    const { ctx: c, w, h } = this;
    const alpha = this.messageTime < 1.7 ? 1 : 1 - (this.messageTime - 1.7) / 0.5;
    const pop = this.messageTime < 0.2 ? 0.6 + 2 * this.messageTime : 1;
    let size = Math.min(h * 0.075, w * 0.06) * pop;
    c.font = `bold ${size}px ${FONT}`;
    const tw = c.measureText(this.message).width;
    if (tw > w * 0.94) size *= (w * 0.94) / tw;
    c.font = `bold ${size}px ${FONT}`;
    c.textAlign = "center";
    c.textBaseline = "alphabetic";
    c.globalAlpha = Math.max(0, alpha);
    c.lineJoin = "round";
    c.lineWidth = size * 0.16;
    c.strokeStyle = "#1B1446";
    c.strokeText(this.message, w / 2, h * 0.55);
    c.fillStyle = this.messageColor;
    c.fillText(this.message, w / 2, h * 0.55);
    c.globalAlpha = 1;
  }
}
