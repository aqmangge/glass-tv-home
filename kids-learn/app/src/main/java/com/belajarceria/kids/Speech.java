package com.belajarceria.kids;

import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;

/**
 * "Now you say it": listens to the child and tells how close they were to the Mandarin phrase. Uses the phone's own
 * speech recogniser (Mandarin, offline when the phone has it). A phone without one records the child instead and
 * plays it back, so they still hear themselves, which is half of learning to speak in public.
 */
final class Speech {
    interface Result {
        /** @param stars 0 to 3; heard is what the recogniser understood, or null when only recorded */
        void done(int stars, String heard);
    }

    interface Level {
        /** 0 to 1, for a little live meter while the child speaks. */
        void level(float v);
    }

    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private SpeechRecognizer recognizer;
    private volatile boolean recording;
    private short[] last;

    Speech(Context c) {
        context = c.getApplicationContext();
    }

    boolean canRecognise() {
        return SpeechRecognizer.isRecognitionAvailable(context);
    }

    /** Listens once for the target phrase and scores it. Falls back to recording when no recogniser works. */
    void listen(String target, Level meter, Result result) {
        if (!canRecognise()) {
            record(meter, result);
            return;
        }
        stop();
        recognizer = SpeechRecognizer.createSpeechRecognizer(context);
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN");
        i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        i.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle b) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float db) {
                if (meter != null) meter.level(Math.max(0f, Math.min(1f, (db + 2f) / 12f)));
            }
            @Override public void onBufferReceived(byte[] b) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onPartialResults(Bundle b) {}
            @Override public void onEvent(int t, Bundle b) {}

            @Override
            public void onError(int error) {
                stop();
                if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS || error == SpeechRecognizer.ERROR_CLIENT
                        || error == SpeechRecognizer.ERROR_SERVER || error == SpeechRecognizer.ERROR_NETWORK
                        || error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT) {
                    record(meter, result); // no working recogniser here: record and play back instead
                } else {
                    result.done(0, ""); // heard nothing it understood
                }
            }

            @Override
            public void onResults(Bundle b) {
                ArrayList<String> heard = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                stop();
                String best = "";
                float score = 0f;
                if (heard != null) {
                    for (String h : heard) {
                        float s = similarity(target, h);
                        if (s > score) {
                            score = s;
                            best = h;
                        }
                    }
                }
                result.done(stars(score), best);
            }
        });
        recognizer.startListening(i);
    }

    void stop() {
        recording = false;
        if (recognizer != null) {
            recognizer.destroy();
            recognizer = null;
        }
    }

    // ---------------------------------------------------------------- record and play back

    /** Records up to five seconds (or until {@link #stop}); a clear voice of a second or more earns three stars. */
    private void record(Level meter, Result result) {
        new Thread(() -> {
            int rate = 16000;
            int min = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            AudioRecord rec;
            try {
                rec = new AudioRecord(MediaRecorder.AudioSource.MIC, rate, AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT, Math.max(min, rate));
                rec.startRecording();
            } catch (RuntimeException e) { // includes SecurityException: no microphone permission
                main.post(() -> result.done(0, null));
                return;
            }
            recording = true;
            short[] all = new short[rate * 5];
            short[] buf = new short[rate / 20];
            int n = 0, loud = 0, quietAfter = 0;
            while (recording && n + buf.length <= all.length) {
                int got = rec.read(buf, 0, buf.length);
                if (got <= 0) break;
                System.arraycopy(buf, 0, all, n, got);
                n += got;
                double sum = 0;
                for (int i = 0; i < got; i++) sum += buf[i] * (double) buf[i];
                float rms = (float) Math.sqrt(sum / got) / 3000f;
                if (meter != null) main.post(() -> meter.level(Math.min(1f, rms)));
                if (rms > 0.15f) {
                    loud++;
                    quietAfter = 0;
                } else if (loud > 0 && ++quietAfter > 30) {
                    break; // a second and a half of quiet after speaking: they're done
                }
            }
            rec.stop();
            rec.release();
            recording = false;
            short[] clip = new short[n];
            System.arraycopy(all, 0, clip, 0, n);
            last = clip;
            boolean spoke = loud > 0;
            int stars = loud >= 20 ? 3 : loud >= 10 ? 2 : loud >= 4 ? 1 : 0;
            main.post(() -> {
                result.done(stars, null);
                if (spoke) playBack();
            });
        }, "record").start();
    }

    /** Plays the last recording back, louder and a little slower to start, so the child hears themselves. */
    void playBack() {
        short[] clip = last;
        if (clip == null || clip.length == 0) return;
        try {
            AudioTrack t = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(16000)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build())
                    .setBufferSizeInBytes(clip.length * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build();
            t.write(clip, 0, clip.length);
            t.play();
            main.postDelayed(t::release, clip.length / 16 + 500);
        } catch (RuntimeException ignored) {
            // no audio out: nothing to play
        }
    }

    boolean hasRecording() {
        return last != null && last.length > 0;
    }

    // ---------------------------------------------------------------- scoring

    /** 0 to 1: how much of the target's characters the child said, in order (longest common subsequence). */
    static float similarity(String target, String heard) {
        String a = clean(target), b = clean(heard);
        if (a.isEmpty() || b.isEmpty()) return 0f;
        int[][] d = new int[a.length() + 1][b.length() + 1];
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                d[i][j] = a.charAt(i - 1) == b.charAt(j - 1) ? d[i - 1][j - 1] + 1 : Math.max(d[i - 1][j], d[i][j - 1]);
            }
        }
        return d[a.length()][b.length()] / (float) a.length();
    }

    /**
     * Keeps only Chinese characters, letters and digits: punctuation and spaces don't count. Digits become Chinese
     * numerals, since a recogniser may write 六 as 6.
     */
    static String clean(String s) {
        String numerals = "零一二三四五六七八九";
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') b.append(numerals.charAt(c - '0'));
            else if (Character.isLetterOrDigit(c)) b.append(Character.toLowerCase(c));
        }
        return b.toString();
    }

    static int stars(float score) {
        return score >= 0.8f ? 3 : score >= 0.55f ? 2 : score >= 0.3f ? 1 : 0;
    }
}
