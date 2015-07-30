package com.example.opanjwani.heartzonetraining;

import java.util.ArrayList;

public class HeartRateZoneManager {

    private static HeartRateZoneManager instance;

    private ArrayList<HeartRateZone> heartRateZones;
    private HeartRateZone lowIntensity;
    private HeartRateZone highIntensity;

    private HeartRateZoneManager() { }

    public static HeartRateZoneManager getInstance() {
        if (instance == null) {
            instance = new HeartRateZoneManager();
        }
        return instance;
    }

    public void init(ArrayList<HeartRateZone> heartRateZones, HeartRateZone lowIntensity, HeartRateZone highIntensity) {
        this.heartRateZones = heartRateZones;
        this.lowIntensity = lowIntensity;
        this.highIntensity = highIntensity;
    }

    public ArrayList<HeartRateZone> getHeartRateZones() {
        return heartRateZones;
    }

    public int getLowIntensityStart() {
        return lowIntensity.getStart();
    }

    public int getHighIntensityStart() {
        return highIntensity.getStart();
    }

    public int getLowIntensityEnd() {
        return lowIntensity.getEnd();
    }

    public int getHighIntensityEnd() {
        return highIntensity.getEnd();
    }
}
