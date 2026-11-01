package com.tenis.koc;

public enum StrokeType {
    FOREHAND("Forehand"), BACKHAND("Backhand"), SERVE("Servis / Smaç");
    public final String label;
    StrokeType(String label) { this.label = label; }
}
