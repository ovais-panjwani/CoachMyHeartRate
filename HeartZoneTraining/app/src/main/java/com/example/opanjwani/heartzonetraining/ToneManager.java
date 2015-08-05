package com.example.opanjwani.heartzonetraining;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.AsyncTask;


public class ToneManager {

    private static final boolean ALLOW_DUCK_AUDIO = true;

    private Context applicationContext;
    private AudioStreamManager audioStreamManager;

    public ToneManager(Context context) {
        this.applicationContext = context;
        this.audioStreamManager = AudioStreamManager.getInstance(context);
    }

    public void playTone(Tone tone, int volume) {
        MyTonePlayerTask task = new MyTonePlayerTask(tone, volume);
        task.execute();
    }

    private void playTonesBackground(Tone tone, int volume) {

        int toneRes;
        switch (tone) {
            case END:
                toneRes = R.raw.coaching_chime_end;
                break;
            case SLOW_DOWN:
                toneRes = R.raw.coaching_chime_slowdown;
                break;
            case SPEED_UP:
                toneRes = R.raw.coaching_chime_speedup;
                break;
            case START:
                toneRes = R.raw.coaching_chime_start;
                break;
            case MAINTAIN:
                toneRes = R.raw.coaching_chime_maintain;
                break;

            default:
                throw new RuntimeException("unknown tone type");
        }

        MediaPlayer mediaPlayer = createMediaPlayer(toneRes);
        audioStreamManager.acquireStream(volume, ALLOW_DUCK_AUDIO);
        mediaPlayer.start();
    }

    private MediaPlayer createMediaPlayer(int resId) {
        try {
            AssetFileDescriptor afd = applicationContext.getResources().openRawResourceFd(resId);
            if (afd == null) {
                return null;
            }

            MediaPlayer mp = new MediaPlayer();
            mp.setAudioStreamType(audioStreamManager.getDesiredStream());
            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            mp.prepare();
            return mp;
        } catch (Exception e) {
            throw new RuntimeException("Unable to create mediaPlayer for tone.");
        }
    }

    public enum Tone {
        END,
        SLOW_DOWN,
        MAINTAIN,
        SPEED_UP,
        START;
    }

    private class MyTonePlayerTask extends AsyncTask<Void, Void, Void> {

        private Tone tone;
        private int volume;

        public MyTonePlayerTask(Tone tone, int volume) {
            this.tone = tone;
            this.volume = volume;
        }

        @Override
        protected Void doInBackground(Void... params) {
            playTonesBackground(tone, volume);
            return null;
        }
    }
}
