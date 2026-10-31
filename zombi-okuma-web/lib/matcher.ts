/**
 * Ses tanıma sonuçlarını hedef metinle karşılaştırır.
 * Çocuk okuması ve kısa heceler için biraz hoşgörülüdür.
 */

export function normalize(s: string | null | undefined): string {
  if (!s) return "";
  return s
    .toLocaleLowerCase("tr-TR")
    .replace(/̇/g, "") // "i̇" → "i"
    .replace(/â/g, "a")
    .replace(/î/g, "i")
    .replace(/û/g, "u")
    .replace(/[^\p{L}]+/gu, " ")
    .trim()
    .replace(/\s+/g, " ");
}

export function levenshtein(a: string, b: string): number {
  let prev = Array.from({ length: b.length + 1 }, (_, j) => j);
  let cur = new Array<number>(b.length + 1);
  for (let i = 1; i <= a.length; i++) {
    cur[0] = i;
    for (let j = 1; j <= b.length; j++) {
      const cost = a[i - 1] === b[j - 1] ? 0 : 1;
      cur[j] = Math.min(cur[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost);
    }
    [prev, cur] = [cur, prev];
  }
  return prev[b.length];
}

/** Kısa kelimelerde birebir, 4+ harfte tek harf farkına izin verir. */
function tokenMatches(heard: string, target: string): boolean {
  if (heard === target) return true;
  return target.length >= 4 && levenshtein(heard, target) <= 1;
}

/** Tek bir tanıma sonucu hedefle eşleşiyor mu? */
export function matchesOne(target: string, candidate: string): boolean {
  const nt = normalize(target);
  const nc = normalize(candidate);
  if (!nt || !nc) return false;

  // "na ne" → "nane" gibi bölünmüş/birleşik okumalar
  const jt = nt.replace(/ /g, "");
  const jc = nc.replace(/ /g, "");
  if (jc === jt || (jt.length >= 5 && levenshtein(jc, jt) <= 1)) return true;

  const tt = nt.split(" ");
  const ct = nc.split(" ");

  if (tt.length === 1) {
    if (ct.some((c) => tokenMatches(c, tt[0]))) return true;
    // Hece tekrarlanarak okunmuşsa ("ne ne", "nene") kabul et
    if (jt.length <= 3 && jc.length % jt.length === 0) {
      let allSame = true;
      for (let i = 0; i < jc.length; i += jt.length) {
        if (!jc.startsWith(jt, i)) {
          allSame = false;
          break;
        }
      }
      if (allSame) return true;
    }
    return false;
  }

  // Cümle: hedef kelimeler sırayla duyulmalı (araya giren kelimeler olabilir)
  let ti = 0;
  for (let ci = 0; ci < ct.length && ti < tt.length; ci++) {
    if (tokenMatches(ct[ci], tt[ti])) ti++;
  }
  return ti === tt.length;
}

/** Eşleşen sonucu döndürür; yoksa null. */
export function findMatch(target: string, candidates: string[]): string | null {
  for (const c of candidates) if (matchesOne(target, c)) return c;
  return null;
}
