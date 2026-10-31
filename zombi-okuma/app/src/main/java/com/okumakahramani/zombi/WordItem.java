package com.okumakahramani.zombi;

/**
 * Ekranda gösterilen tek bir okuma hedefi (hece, kelime veya cümle).
 *
 * Tanım biçimi: kelimeler boşlukla, heceler tire ile ayrılır.
 * Örnek: "E-la na-ne al."  →  [[E, la], [na, ne], [al.]]
 */
public final class WordItem {

    /** Kelime → hece dizisi. */
    public final String[][] syllables;
    /** Ekranda/seslendirmede kullanılan düz metin ("Ela nane al."). */
    public final String text;
    /** Toplam hece sayısı (animasyon için). */
    public final int syllableCount;

    public WordItem(String spec) {
        String[] words = spec.trim().split("\\s+");
        syllables = new String[words.length][];
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = 0; i < words.length; i++) {
            syllables[i] = words[i].split("-");
            count += syllables[i].length;
            if (i > 0) sb.append(' ');
            sb.append(words[i].replace("-", ""));
        }
        text = sb.toString();
        syllableCount = count;
    }

    public boolean isSingleSyllable() {
        return syllableCount == 1;
    }
}
