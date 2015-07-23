package com.example.opanjwani.heartzonetraining;

import android.app.Activity;
import android.util.Log;

public class IntervalManager {

    private static int NUM_REPS;
    private static IntervalManager instance;

    private enum State {
        PREP {
            public String toString() {
                return "PREP";
            }
        },
        WORK {
            public String toString() {
                return "WORK";
            }
        },
        REST {
            public String toString() {
                return "REST";
            }
        }

    }

    private int prepTime;
    private int workTime;
    private int restTime;
    private int numReps;
    private int cycle;
    private int currentRepTime;

    private Listener listener;
    private State previousState;
    private State currentState;

    private IntervalManager() {
    }

    public static IntervalManager getInstance() {
        if (instance == null) {
            instance = new IntervalManager();
        }
        return instance;
    }

    public void init(int prepTime, int workTime, int restTime, int numReps) {
        this.prepTime = prepTime;
        this.workTime = workTime;
        this.restTime = restTime;
        this.numReps = numReps;
        NUM_REPS = numReps;
        cycle = workTime + restTime;
        previousState = null;

    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void destroyListener() {
        this.listener = null;
    }

    public void onUpdate(double activeTime) {
        currentRepTime = (NUM_REPS - numReps) * cycle + prepTime;

        switch (getState(activeTime)) {
            case PREP:
                currentState = State.PREP;
                break;
            case WORK:
                currentState = State.WORK;
                break;
            case REST:
                currentState = State.REST;
                break;
        }

        if (numReps > 0 && activeTime >= (restTime + workTime + currentRepTime)) {
            numReps--;
            if (numReps == 0) {
                listener.onIntervalFinished();
            }
        }

        if (previousState != currentState) {
            previousState = currentState;
            listener.onStateChanged(currentState.toString());
        }
    }

    public State getState(double elapsedTime) {
        if (prepTime >= elapsedTime) {
            return State.PREP;
        } else if ((workTime + currentRepTime) >= elapsedTime && elapsedTime >= (currentRepTime)) {
            return State.WORK;
        } else if ((restTime + workTime + currentRepTime) >= elapsedTime && elapsedTime >= (workTime + currentRepTime)) {
            return State.REST;
        }
        return null;
    }

    public interface Listener {
        void onIntervalFinished();
        void onStateChanged(String state);
    }

}
