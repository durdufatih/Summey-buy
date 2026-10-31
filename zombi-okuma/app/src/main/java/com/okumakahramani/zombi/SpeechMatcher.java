package com.okumakahramani.zombi;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Ses tanıma sonuçlarını hedef metinle karşılaştırır.
 * Çocuk okuması ve kısa heceler için biraz hoşgörülüdür.
 */
public final class SpeechMatcher {

    private static final Locale TR = new Locale("tr", "TR");

    private SpeechMatcher() {}

    public static String normalize(String s) {
        if (s == null) return "";
        String t = s.toLowerCase(TR)
                .replace("̇", "") // "i̇" → "i"
                .replace('â', 'a').replace('î', 'i').replace('û', 'u');
        StringBuilder sb = new StringBuilder(t.length());
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            sb.append(Character.isLetter(c) ? c : ' ');
        }
        return sb.toString().trim().replaceAll("\\s+", " ");
    }

    private static String[] tokens(String normalized) {
        return normalized.isEmpty() ? new String[0] : normalized.split(" ");
    }

    /** Kısa kelimelerde birebir, 4+ harfte tek harf farkına izin verir. */
    static boolean tokenMatches(String heard, String target) {
        if (heard.equals(target)) return true;
        return target.length() >= 4 && levenshtein(heard, target) <= 1;
    }

    /** Tek bir tanıma sonucu hedefle eşleşiyor mu? */
    public static boolean matchesOne(String target, String candidate) {
        String nt = normalize(target);
        String nc = normalize(candidate);
        if (nt.isEmpty() || nc.isEmpty()) return false;

        // "na ne" → "nane" gibi bölünmüş/birleşik okumalar
        String jt = nt.replace(" ", "");
        String jc = nc.replace(" ", "");
        if (jc.equals(jt) || (jt.length() >= 5 && levenshtein(jc, jt) <= 1)) return true;

        String[] tt = tokens(nt);
        String[] ct = tokens(nc);

        if (tt.length == 1) {
            for (String c : ct) if (tokenMatches(c, tt[0])) return true;
            // Hece tekrarlanarak okunmuşsa ("ne ne", "nene") kabul et
            if (jt.length() <= 3 && jc.length() % jt.length() == 0 && !jc.isEmpty()) {
                boolean allSame = true;
                for (int i = 0; i < jc.length(); i += jt.length()) {
                    if (!jc.startsWith(jt, i)) { allSame = false; break; }
                }
                if (allSame) return true;
            }
            return false;
        }

        // Cümle: hedef kelimeler sırayla duyulmalı (araya giren kelimeler olabilir)
        int ti = 0;
        for (int ci = 0; ci < ct.length && ti < tt.length; ci++) {
            if (tokenMatches(ct[ci], tt[ti])) ti++;
        }
        return ti == tt.length;
    }

    /** Eşleşen sonucu döndürür; yoksa null. */
    public static String findMatch(String target, List<String> candidates) {
        if (candidates == null) return null;
        for (String c : candidates) {
            if (matchesOne(target, c)) return c;
        }
        return null;
    }

    static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev; prev = cur; cur = tmp;
        }
        return prev[b.length()];
    }

    /** Basit kendi kendine test (bilgisayarda çalıştırmak için). */
    public static void main(String[] args) {
        List<String[]> cases = new ArrayList<>();
        cases.add(new String[]{"Ali", "ali", "true"});
        cases.add(new String[]{"Ela", "Ela", "true"});
        cases.add(new String[]{"nane", "na ne", "true"});
        cases.add(new String[]{"nane", "nene", "true"});
        cases.add(new String[]{"at", "ad", "false"});
        cases.add(new String[]{"at", "at at", "true"});
        cases.add(new String[]{"ne", "ne ne", "true"});
        cases.add(new String[]{"il", "İL", "true"});
        cases.add(new String[]{"Lale", "lale", "true"});
        cases.add(new String[]{"Ela nane al.", "Ela nane al", "true"});
        cases.add(new String[]{"Ela nane al.", "ela nane", "false"});
        cases.add(new String[]{"Ali Ela ile el al.", "Ali Ela ile el al", "true"});
        cases.add(new String[]{"Lale ile Nil nane al.", "lale ile nil nane al", "true"});
        cases.add(new String[]{"Nil el al.", "Nil el aldı", "false"});
        int fail = 0;
        for (String[] c : cases) {
            boolean got = matchesOne(c[0], c[1]);
            boolean want = Boolean.parseBoolean(c[2]);
            if (got != want) {
                fail++;
                System.out.println("FAIL: " + c[0] + " <- " + c[1] + " got " + got);
            }
        }
        System.out.println(fail == 0 ? "ALL OK (" + cases.size() + ")" : fail + " failures");
        if (fail != 0) System.exit(1);
    }
}
