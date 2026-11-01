package com.tenis.koc;

import java.util.List;

public class StrokeResult {
    public final long timeMs;
    public final StrokeType type;
    public final int score;
    public final List<Fault> faults;
    public final float speed;

    public StrokeResult(long timeMs, StrokeType type, int score, List<Fault> faults, float speed) {
        this.timeMs = timeMs; this.type = type; this.score = score; this.faults = faults; this.speed = speed;
    }
}
