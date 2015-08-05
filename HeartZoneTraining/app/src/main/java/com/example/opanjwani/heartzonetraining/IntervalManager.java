package com.example.opanjwani.heartzonetraining;

import android.app.Activity;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

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
    private int nagDuringTime;
    private int nagBetwenTime;
    private int cycle;
    private int currentRepTime;

    private List<Listener> listeners = new ArrayList<>();
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

    public void init(int prepTime, int workTime, int restTime, int numReps, int nagDuringTime, int nagBetwenTime) {
        this.prepTime = prepTime;
        this.workTime = workTime;
        this.restTime = restTime;
        this.numReps = numReps;
        this.nagDuringTime = nagDuringTime;
        this.nagBetwenTime = nagBetwenTime;
        NUM_REPS = numReps;
        cycle = workTime + restTime;
        previousState = null;

    }

    public void addListener(Listener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        this.listeners.remove(listener);
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
            default:
                break;
        }

        if (numReps > 0 && activeTime >= (restTime + workTime + currentRepTime)) {
            numReps--;
            if (numReps == 0) {
                for (Listener listener: listeners) {
                    listener.onIntervalFinished();
                }
            }
        }

        if (previousState != currentState) {
            previousState = currentState;
            for (Listener listener: listeners) {
                listener.onStateChanged(currentState.toString());
            }
        }
    }

    public int getPrepTime() {
        return prepTime;
    }

    public int getWorkTime() {
        return workTime;
    }

    public int getRestTime() {
        return restTime;
    }

    public int getNumReps() {
        return NUM_REPS;
    }

    public int getNagDuringTime() {
        return nagDuringTime;
    }

    public int getNagBetwenTime() {
        return nagBetwenTime;
    }

    public State getState(double activeTime) {
        if (prepTime >= activeTime) {
            return State.PREP;
        } else if ((workTime + currentRepTime) >= activeTime && activeTime >= (currentRepTime)) {
            return State.WORK;
        } else if ((restTime + workTime + currentRepTime) >= activeTime && activeTime >= (workTime + currentRepTime)) {
            return State.REST;
        } else {
            return currentState;
        }
    }

    public interface Listener {
        void onIntervalFinished();
        void onStateChanged(String state);
    }

}
