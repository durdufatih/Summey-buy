package com.tenis.koc;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Günün / antrenmanın özeti ve kişisel günlük plan. */
public class Coaching {

    public static class TypeStat {
        public StrokeType type;
        public int count, avgScore;
    }

    public static class FaultStat {
        public Fault fault;
        public int count;
        public float rate;
    }

    public static class Summary {
        public int sessionCount, strokeCount, minutes, avgScore;
        public Integer scoreDelta;
        public List<TypeStat> types = new ArrayList<TypeStat>();
        public List<FaultStat> faults = new ArrayList<FaultStat>();
        public List<String> strengths = new ArrayList<String>();
        public String text;
        public Fault nextFocus;
    }

    public static class PlanItem {
        public final String id, title, detail;
        public final int minutes;

        PlanItem(String id, String title, String detail, int minutes) {
            this.id = id; this.title = title; this.detail = detail; this.minutes = minutes;
        }
    }

    public static class DailyPlan {
        public String title, goal;
        public List<PlanItem> items = new ArrayList<PlanItem>();
        public List<String> cues = new ArrayList<String>();
        public boolean personalized;

        public int totalMinutes() {
            int m = 0;
            for (PlanItem i : items) m += i.minutes;
            return m;
        }
    }

    private static List<StrokeResult> strokesOf(List<Session> sessions) {
        List<StrokeResult> out = new ArrayList<StrokeResult>();
        for (Session s : sessions) out.addAll(s.strokes);
        return out;
    }

    private static int avg(List<StrokeResult> strokes) {
        if (strokes.isEmpty()) return 0;
        long s = 0;
        for (StrokeResult r : strokes) s += r.score;
        return Math.round((float) s / strokes.size());
    }

    private static int count(List<StrokeResult> strokes, StrokeType t) {
        int c = 0;
        for (StrokeResult r : strokes) if (r.type == t) c++;
        return c;
    }

    /** Hata oranları ilgili vuruş tipine göre hesaplanır (servis hataları yalnız servislerde). */
    public static List<FaultStat> faultStats(List<StrokeResult> strokes) {
        int serves = count(strokes, StrokeType.SERVE);
        int ground = strokes.size() - serves;
        List<FaultStat> out = new ArrayList<FaultStat>();
        for (Fault f : Fault.values()) {
            int base = f.forServe ? serves : ground;
            if (base == 0) continue;
            int c = 0;
            for (StrokeResult r : strokes) if (r.faults.contains(f)) c++;
            if (c == 0) continue;
            FaultStat fs = new FaultStat();
            fs.fault = f;
            fs.count = c;
            fs.rate = (float) c / base;
            out.add(fs);
        }
        Collections.sort(out, new Comparator<FaultStat>() {
            @Override
            public int compare(FaultStat a, FaultStat b) {
                return Float.compare(b.rate * b.fault.penalty, a.rate * a.fault.penalty);
            }
        });
        return out;
    }

    private static float rateOf(List<FaultStat> stats, Fault f) {
        for (FaultStat s : stats) if (s.fault == f) return s.rate;
        return 0f;
    }

    public static Summary summarize(List<Session> sessions, List<Session> previous, String label) {
        Summary sm = new Summary();
        List<StrokeResult> strokes = strokesOf(sessions);
        List<StrokeResult> prev = strokesOf(previous);
        sm.sessionCount = sessions.size();
        sm.strokeCount = strokes.size();
        sm.avgScore = avg(strokes);
        if (prev.size() >= 5 && !strokes.isEmpty()) sm.scoreDelta = sm.avgScore - avg(prev);
        long dur = 0;
        for (Session s : sessions) dur += s.durationMs;
        sm.minutes = (int) (dur / 60000L);

        for (StrokeType t : StrokeType.values()) {
            List<StrokeResult> l = new ArrayList<StrokeResult>();
            for (StrokeResult r : strokes) if (r.type == t) l.add(r);
            if (l.isEmpty()) continue;
            TypeStat ts = new TypeStat();
            ts.type = t;
            ts.count = l.size();
            ts.avgScore = avg(l);
            sm.types.add(ts);
        }
        sm.faults = faultStats(strokes);

        int serves = count(strokes, StrokeType.SERVE);
        int ground = strokes.size() - serves;
        for (Fault f : Fault.values()) {
            boolean enough = f.forServe ? serves >= 3 : ground >= 5;
            if (enough && rateOf(sm.faults, f) < 0.15f && sm.strengths.size() < 3) sm.strengths.add(f.good);
        }

        StringBuilder sb = new StringBuilder();
        String prefix;
        if (sessions.size() > 1) prefix = label + " " + sessions.size() + " antrenmanda";
        else if (label.equals("Bugün")) prefix = "Bugünkü antrenmanda";
        else prefix = label + " antrenmanda";
        sb.append(prefix).append(" toplam ").append(strokes.size()).append(" vuruş analiz edildi");
        if (sm.minutes > 0) sb.append(" (").append(sm.minutes).append(" dk)");
        sb.append(". Ortalama teknik puanın ").append(sm.avgScore).append("/100");
        if (sm.scoreDelta != null) {
            int d = sm.scoreDelta;
            if (d > 0) sb.append(", önceki antrenmanlara göre +").append(d).append(" puan ilerleme var");
            else if (d < 0) sb.append(", önceki antrenmanlara göre ").append(d).append(" puan geride");
            else sb.append(", önceki antrenmanlarla aynı seviyede");
        }
        sb.append(".");
        TypeStat best = null, worst = null;
        for (TypeStat t : sm.types) {
            if (t.count < 3) continue;
            if (best == null || t.avgScore > best.avgScore) best = t;
            if (worst == null || t.avgScore < worst.avgScore) worst = t;
        }
        if (best != null && worst != null && best != worst) {
            sb.append(" En iyi vuruşun ").append(best.type.label).append(" (").append(best.avgScore)
                .append("), en çok gelişmesi gereken ").append(worst.type.label).append(" (")
                .append(worst.avgScore).append(").");
        }
        if (!sm.faults.isEmpty()) {
            FaultStat top = sm.faults.get(0);
            sb.append(" En sık görülen hata: “").append(top.fault.title).append("” (ilgili vuruşların %")
                .append(Math.round(top.rate * 100)).append("'i).");
            sm.nextFocus = top.fault;
        } else if (!strokes.isEmpty()) {
            sb.append(" Belirgin bir teknik hata tespit edilmedi, harika!");
        }
        sm.text = sb.toString();
        return sm;
    }

    // ---------------- Günlük plan ----------------

    private static final PlanItem WARMUP = new PlanItem("warmup", "Isınma + mini tenis",
        "5 dk hafif koşu, kol/omuz/kalça dinamik esneme. Ardından servis çizgileri arasında 5 dk kontrollü mini tenis.", 10);
    private static final PlanItem COOLDOWN = new PlanItem("cooldown", "Soğuma ve esneme",
        "Omuz, ön kol, kalça ve baldır için statik esneme. Ardından uygulamada günün özetine göz at.", 5);

    /** history: bugünden önceki antrenmanlar (son 14 gün). */
    public static DailyPlan plan(List<Session> history) {
        List<StrokeResult> strokes = strokesOf(history);
        if (strokes.size() < 5) return defaultPlan();

        List<FaultStat> stats = faultStats(strokes);
        List<FaultStat> top = new ArrayList<FaultStat>();
        for (FaultStat s : stats) if (s.rate >= 0.15f && top.size() < 3) top.add(s);

        DailyPlan p = new DailyPlan();
        p.personalized = true;
        p.items.add(WARMUP);
        for (int i = 0; i < top.size(); i++) {
            Fault f = top.get(i).fault;
            p.items.add(new PlanItem("drill_" + f.name(), f.drill, f.drillDetail, i == 0 ? 15 : 10));
            p.cues.add(f.cue + " — " + f.tip);
        }
        int fh = count(strokes, StrokeType.FOREHAND), bh = count(strokes, StrokeType.BACKHAND);
        int sv = count(strokes, StrokeType.SERVE);
        if (fh + bh >= 10 && bh < (fh + bh) * 0.3f) {
            p.items.add(new PlanItem("bh_balance", "Backhand çapraz ralli",
                "Backhand'i daha az kullanıyorsun. 10 dk sadece backhand çapraz ralli, her vuruşta omuz dönüşüne dikkat.", 10));
        }
        if (sv < strokes.size() * 0.1f) {
            p.items.add(new PlanItem("serve_routine", "Servis rutini",
                "Her iki karede 10'ar servis: önce %70 güçle isabet, sonra hız. Her servisten önce aynı ritüeli uygula.", 10));
        }
        p.items.add(new PlanItem("play", "Puan oyunu",
            "Serbest puan oyna; her puanda günün odak noktalarından birini bilinçli uygula.", 15));
        p.items.add(COOLDOWN);

        int a = avg(strokes);
        if (!top.isEmpty()) {
            FaultStat m = top.get(0);
            int target = Math.max(5, Math.round(m.rate * 100) - 15);
            p.title = "Odak: " + m.fault.title;
            p.goal = "Ortalama puanını " + Math.min(95, a + 5) + "+ yap ve “" + m.fault.title + "” oranını %"
                + target + " altına indir.";
        } else {
            p.title = "Odak: Ritim ve tutarlılık";
            p.goal = "Ortalama puanını " + Math.min(98, a + 3) + "+ seviyesinde koru, vuruş sayını artır.";
        }
        return p;
    }

    private static DailyPlan defaultPlan() {
        int day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % 3;
        String theme, kind;
        Fault[] fs;
        if (day == 0) {
            theme = "Forehand temeli"; kind = "forehand";
            fs = new Fault[]{Fault.NO_UNIT_TURN, Fault.NO_FOLLOW_THROUGH};
        } else if (day == 1) {
            theme = "Backhand ve ayak çalışması"; kind = "backhand";
            fs = new Fault[]{Fault.KNEES_STRAIGHT, Fault.NARROW_STANCE};
        } else {
            theme = "Servis günü"; kind = "servis";
            fs = new Fault[]{Fault.LOW_SERVE_CONTACT, Fault.SERVE_NO_LEGS};
        }
        DailyPlan p = new DailyPlan();
        p.personalized = false;
        p.title = "Odak: " + theme;
        p.goal = "En az 40 vuruşu kamerayla analiz ettir; kişisel planın oluşsun.";
        p.items.add(WARMUP);
        for (Fault f : fs) {
            p.items.add(new PlanItem("drill_" + f.name(), f.drill, f.drillDetail, 12));
            p.cues.add(f.cue + " — " + f.tip);
        }
        p.items.add(new PlanItem("record", "Kamerayla analiz turu",
            "Telefonu tripoda koy ve 10 dk " + kind + " ağırlıklı ralli yap; uygulama hatalarını kaydetsin. Sonraki gün plan sana göre kişiselleşir.", 10));
        p.items.add(new PlanItem("play", "Puan oyunu", "Serbest puan oyna, günün odak noktalarını uygula.", 15));
        p.items.add(COOLDOWN);
        return p;
    }
}
