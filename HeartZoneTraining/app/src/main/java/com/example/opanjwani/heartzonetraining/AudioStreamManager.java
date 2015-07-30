package com.example.opanjwani.heartzonetraining;

import android.content.Context;
import android.media.AudioManager;

public class AudioStreamManager {

    public static final int MAX_LEVEL = 10;

    private static AudioStreamManager instance;

    private AudioManager audioManager;

    private int acquireCount;
    private int targetStream;
    private int targetStreamDurationHint;
    private int targetStreamOriginalVolume;

    private AudioStreamManager(Context context) {
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        acquireCount = 0;
    }

    public static synchronized AudioStreamManager getInstance(Context context) {
        if (instance == null) {
            instance = new AudioStreamManager(context);
        }
        return instance;
    }


    public synchronized int acquireStream(int volume, boolean shouldDuckAudio) {

        if (audioManager != null) {

            int durationHint = calculateDurationHint(shouldDuckAudio);

            if (acquireCount != 0 && durationHint == targetStreamDurationHint) {
                setRelativeVolume(volume);
            } else if (acquireCount != 0 && durationHint != targetStreamDurationHint) {
                releaseStreamFocus();
                targetStreamDurationHint = durationHint;
                requestStreamFocus();
            } else {
                targetStream = getDesiredStream();
                targetStreamOriginalVolume = audioManager.getStreamVolume(targetStream);

                targetStreamDurationHint = durationHint;
                requestStreamFocus();
                setRelativeVolume(volume);
            }
            acquireCount++;

            return targetStream;
        } else {
            return -1;
        }
    }

    public synchronized void releaseStream() {

        if (audioManager != null) {

            if (acquireCount == 0) {
                return;
            }

            acquireCount--;

            if (acquireCount == 0) {
                audioManager.setStreamVolume(targetStream, targetStreamOriginalVolume, 0);
                releaseStreamFocus();
            }
        }
    }

    public synchronized boolean shouldChangeStream() {
        return targetStream != getDesiredStream();
    }

    public synchronized int getDesiredStream() {
        // NOTE: Bug in Android will only use actual volume from voice call
        // any other stream will be quiet regardless of user setting. Unfortunately,
        // the only place for a user to set this volume is while in a call.

        // Some phones will route this through the ear-piece so only use
        // IF the headset is wired in.
        if (audioManager.isWiredHeadsetOn() || audioManager.isBluetoothA2dpOn()) {
            return AudioManager.STREAM_MUSIC;
        }
        return AudioManager.STREAM_NOTIFICATION;
    }

    private void setRelativeVolume(int volume) {
        int max = audioManager.getStreamMaxVolume(targetStream);
        int playbackVolume = (volume * max) / (MAX_LEVEL - 1);
        if (playbackVolume <= 0) {
            playbackVolume = 1; // Ensure it plays back at some level
        }
        audioManager.setStreamVolume(targetStream, playbackVolume, 0);
    }

    private int calculateDurationHint(boolean shouldDuckAudio) {
        return shouldDuckAudio ? AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK : AudioManager.AUDIOFOCUS_GAIN_TRANSIENT;
    }

    private void requestStreamFocus() {
        audioManager.requestAudioFocus(null, targetStream, targetStreamDurationHint);
    }

    private void releaseStreamFocus() {
        audioManager.abandonAudioFocus(null);
    }

}
