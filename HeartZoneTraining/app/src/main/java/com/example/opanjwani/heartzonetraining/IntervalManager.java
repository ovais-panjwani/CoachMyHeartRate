package com.example.opanjwani.heartzonetraining;

public class IntervalManager {

    private static IntervalManager instance;

    private enum State {
        PREP, WORK, REST
    }

    private int prepTime;
    private int workTime;
    private int restTime;
    private int numReps;
    private int cycle;
    private Listener listener;

    private IntervalManager() {  }

    public static IntervalManager getInstance() {
        if(instance == null){
            instance = new IntervalManager();
        }
        return instance;
    }

    public void init (int prepTime, int workTime, int restTime, int numReps) {
        this.prepTime = prepTime;
        this.workTime = workTime;
        this.restTime = restTime;
        this.numReps = numReps;
        cycle = prepTime + workTime + restTime;
    }

    public interface Listener{
        void onIntervalFinished();
        void onStateChanged(String state);
    }

}
