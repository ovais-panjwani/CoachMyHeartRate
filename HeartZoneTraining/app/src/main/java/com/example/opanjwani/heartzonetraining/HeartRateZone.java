package com.example.opanjwani.heartzonetraining;

public class HeartRateZone {

    private int name;
    private int start;
    private int end;

    public HeartRateZone(int name, int start, int end) {
        this.name = name;
        this.start = start;
        this.end = end;
    }

    public int getName() {
        return name;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }
}
