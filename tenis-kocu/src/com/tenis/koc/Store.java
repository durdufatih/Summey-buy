package com.tenis.koc;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Antrenmanları (JSON dosyası) ve ayarları cihazda saklar. */
public class Store {
    public static final Locale TR = new Locale("tr", "TR");
    private static Store instance;

    private final File file;
    private final SharedPreferences prefs;
    private List<Session> sessions;

    public static synchronized Store get(Context ctx) {
        if (instance == null) instance = new Store(ctx.getApplicationContext());
        return instance;
    }

    private Store(Context ctx) {
        file = new File(ctx.getFilesDir(), "sessions.json");
        prefs = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE);
        sessions = load();
    }

    // ---- Tarih yardımcıları ----
    public static String dayKey(long millis) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(millis));
    }

    public static long startOfToday() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    public static String formatDateTime(long millis) {
        return new SimpleDateFormat("d MMMM yyyy, HH:mm", TR).format(new Date(millis));
    }

    public static String formatClock(long ms) {
        long s = ms / 1000;
        return String.format(Locale.US, "%02d:%02d", s / 60, s % 60);
    }

    // ---- Antrenmanlar ----
    public synchronized List<Session> sessions() {
        return new ArrayList<Session>(sessions);
    }

    public synchronized Session find(long id) {
        for (Session s : sessions) if (s.id == id) return s;
        return null;
    }

    /** [from, to) aralığında başlayan antrenmanlar. */
    public synchronized List<Session> between(long from, long to) {
        List<Session> out = new ArrayList<Session>();
        for (Session s : sessions) if (s.startedAt >= from && s.startedAt < to) out.add(s);
        return out;
    }

    public synchronized void add(Session s) {
        sessions.add(s);
        sort();
        save();
    }

    public synchronized void delete(long id) {
        for (int i = sessions.size() - 1; i >= 0; i--) if (sessions.get(i).id == id) sessions.remove(i);
        save();
    }

    public synchronized void clear() {
        sessions.clear();
        save();
    }

    private void sort() {
        Collections.sort(sessions, new Comparator<Session>() {
            @Override
            public int compare(Session a, Session b) {
                return Long.compare(b.startedAt, a.startedAt);
            }
        });
    }

    private List<Session> load() {
        List<Session> out = new ArrayList<Session>();
        if (!file.exists()) return out;
        try {
            FileInputStream in = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            int off = 0;
            while (off < data.length) {
                int n = in.read(data, off, data.length - off);
                if (n < 0) break;
                off += n;
            }
            in.close();
            JSONArray arr = new JSONArray(new String(data, 0, off, "UTF-8"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                JSONArray sa = o.getJSONArray("strokes");
                List<StrokeResult> strokes = new ArrayList<StrokeResult>();
                for (int j = 0; j < sa.length(); j++) {
                    JSONObject so = sa.getJSONObject(j);
                    StrokeType type;
                    try {
                        type = StrokeType.valueOf(so.getString("type"));
                    } catch (Exception e) {
                        continue;
                    }
                    List<Fault> faults = new ArrayList<Fault>();
                    JSONArray fa = so.optJSONArray("faults");
                    if (fa != null) for (int k = 0; k < fa.length(); k++) {
                        try {
                            faults.add(Fault.valueOf(fa.getString(k)));
                        } catch (Exception ignored) {
                        }
                    }
                    strokes.add(new StrokeResult(so.getLong("t"), type, so.getInt("score"), faults,
                        (float) so.optDouble("speed", 0)));
                }
                out.add(new Session(o.getLong("id"), o.getLong("startedAt"), o.getLong("durationMs"),
                    o.optString("source", "Kamera"), strokes));
            }
        } catch (Exception e) {
            // bozuk dosya: boş başla
        }
        sessions = out;
        sort();
        return out;
    }

    private void save() {
        try {
            JSONArray arr = new JSONArray();
            for (Session s : sessions) {
                JSONArray sa = new JSONArray();
                for (StrokeResult st : s.strokes) {
                    JSONArray fa = new JSONArray();
                    for (Fault f : st.faults) fa.put(f.name());
                    sa.put(new JSONObject().put("t", st.timeMs).put("type", st.type.name())
                        .put("score", st.score).put("speed", (double) st.speed).put("faults", fa));
                }
                arr.put(new JSONObject().put("id", s.id).put("startedAt", s.startedAt)
                    .put("durationMs", s.durationMs).put("source", s.source).put("strokes", sa));
            }
            FileOutputStream out = new FileOutputStream(file);
            out.write(arr.toString().getBytes("UTF-8"));
            out.close();
        } catch (Exception e) {
            // yazılamadı
        }
    }

    // ---- Ayarlar ----
    public boolean leftHanded() { return prefs.getBoolean("left", false); }
    public void setLeftHanded(boolean v) { prefs.edit().putBoolean("left", v).apply(); }

    public boolean voice() { return prefs.getBoolean("voice", true); }
    public void setVoice(boolean v) { prefs.edit().putBoolean("voice", v).apply(); }

    public int sensitivity() { return prefs.getInt("sensitivity", 2); }
    public void setSensitivity(int v) { prefs.edit().putInt("sensitivity", v).apply(); }

    public boolean isChecked(String key) {
        return prefs.getStringSet("checks", new HashSet<String>()).contains(key);
    }

    public void toggle(String key) {
        Set<String> cur = new HashSet<String>(prefs.getStringSet("checks", new HashSet<String>()));
        if (!cur.remove(key)) cur.add(key);
        prefs.edit().putStringSet("checks", cur).apply();
    }
}
