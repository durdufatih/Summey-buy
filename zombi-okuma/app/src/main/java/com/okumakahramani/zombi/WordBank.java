package com.okumakahramani.zombi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * "Okuma Kahramanı – A-N-E-T-İ-L" kitapçığından alınan heceler, kelimeler ve cümleler.
 */
public final class WordBank {

    private WordBank() {}

    public static final class Level {
        public final String emoji;
        public final String title;
        public final String subtitle;
        public final int color;
        /** Zombinin hızı: saniyede ekran genişliğinin kesri. */
        public final float speed;
        public final String[] specs;
        public final boolean shuffle;
        public final int roundSize;

        Level(String emoji, String title, String subtitle, int color, float speed,
              boolean shuffle, int roundSize, String... specs) {
            this.emoji = emoji;
            this.title = title;
            this.subtitle = subtitle;
            this.color = color;
            this.speed = speed;
            this.shuffle = shuffle;
            this.roundSize = roundSize;
            this.specs = specs;
        }

        public List<WordItem> buildRound(Random random) {
            List<String> pool = new ArrayList<>();
            Collections.addAll(pool, specs);
            if (shuffle) Collections.shuffle(pool, random);
            int n = Math.min(roundSize, pool.size());
            List<WordItem> out = new ArrayList<>(n);
            for (int i = 0; i < n; i++) out.add(new WordItem(pool.get(i)));
            return out;
        }
    }

    public static final Level[] LEVELS = {
            // Görev 2 • Hece treni
            new Level("🚂", "Hece Avcısı", "Heceleri oku", 0xFFFFEFA8, 0.036f, true, 10,
                    "al", "la", "at", "na", "an", "ne", "nen", "ni", "nil", "el", "le",
                    "en", "ta", "te", "et", "tan", "tel", "ten", "net", "il", "li", "lan"),

            // Görev 3 • Kelime roketi (heceler birleşerek kelime olur)
            new Level("🚀", "Kelime Roketi", "Heceleri birleştir, kelimeyi oku", 0xFFF6C6E0, 0.032f, true, 10,
                    "A-li", "E-la", "Nil", "La-le", "na-ne", "at", "el", "tel", "a-na", "i-le"),

            // Görev 5-7 • Cümle piramidi / tamamlama / okuduğunu anla
            new Level("🔎", "Cümle Dedektifi", "Cümleyi baştan sona oku", 0xFFCDE8FB, 0.023f, false, 14,
                    "E-la", "E-la i-le", "E-la na-ne", "E-la na-ne al.",
                    "A-li al.", "A-li el al.", "A-li E-la i-le el al.",
                    "At al.", "La-le at al.", "A-li La-le at al.",
                    "Nil na-ne al.", "Nil el al.", "La-le i-le Nil na-ne al."),
    };
}
