/** Kazanılan yıldızlar tarayıcıda saklanır (yalnızca bu cihazda). */

const key = (level: number) => `zombi-okuma:stars:${level}`;

export function loadStars(level: number): number {
  try {
    return Number(localStorage.getItem(key(level))) || 0;
  } catch {
    return 0;
  }
}

export function saveStars(level: number, stars: number) {
  try {
    if (stars > loadStars(level)) localStorage.setItem(key(level), String(stars));
  } catch {
    /* gizli sekme vb. */
  }
}

export function starText(n: number) {
  return Array.from({ length: 3 }, (_, i) => (i < n ? "⭐" : "☆")).join("");
}
