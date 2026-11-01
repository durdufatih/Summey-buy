package com.tenis.koc;

import java.util.List;

public class Session {
    public final long id, startedAt, durationMs;
    public final String source;
    public final List<StrokeResult> strokes;

    public Session(long id, long startedAt, long durationMs, String source, List<StrokeResult> strokes) {
        this.id = id; this.startedAt = startedAt; this.durationMs = durationMs; this.source = source; this.strokes = strokes;
    }

    public int avgScore() {
        if (strokes.isEmpty()) return 0;
        long s = 0;
        for (StrokeResult r : strokes) s += r.score;
        return Math.round((float) s / strokes.size());
    }
}
