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
import android.os.SystemClock;
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
    /** Which listen is current: a late answer from an older one (cancelled, or a new card) is ignored. */
    private int session;
    private Result pending;
    private Level meter;
    private boolean ready, spoke;
    private long startedAt;
    /** The current recording's own "keep going" flag, so stopping one never stops the next. */
    private boolean[] recordingFlag;
    private short[] last;

    Speech(Context c) {
        context = c.getApplicationContext();
    }

    boolean canRecognise() {
        return SpeechRecognizer.isRecognitionAvailable(context);
    }

    /**
     * Listens once for the target phrase and scores it; result gets exactly one answer, always. Falls back to
     * recording when the phone's recogniser doesn't work for Mandarin (no Chinese, no connection, busy...).
     */
    void listen(String target, Level meter, Result result) {
        stop();
        int me = ++session;
        pending = result;
        this.meter = meter;
        if (!canRecognise()) {
            record(me);
            return;
        }
        ready = false;
        spoke = false;
        startedAt = SystemClock.uptimeMillis();
        try {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context);
        } catch (RuntimeException e) {
            record(me);
            return;
        }
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN");
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "zh-CN");
        i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        i.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());
        // Children pause and start slowly: give them time before deciding they have finished.
        i.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L);
        i.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle b) {
                ready = true;
            }
            @Override public void onBeginningOfSpeech() {
                spoke = true;
            }
            @Override public void onRmsChanged(float db) {
                if (me == session && Speech.this.meter != null) {
                    Speech.this.meter.level(Math.max(0f, Math.min(1f, (db + 2f) / 12f)));
                }
            }
            @Override public void onBufferReceived(byte[] b) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onPartialResults(Bundle b) {}
            @Override public void onEvent(int t, Bundle b) {}

            @Override
            public void onError(int error) {
                if (me != session) return;
                destroyRecognizer();
                boolean listenedProperly = ready && (spoke || SystemClock.uptimeMillis() - startedAt > 2500);
                if (listenedProperly && (error == SpeechRecognizer.ERROR_NO_MATCH
                        || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)) {
                    finish(me, 0, ""); // it listened, but understood nothing
                } else {
                    // The recogniser gave up before really listening (no Mandarin on this phone, no connection,
                    // busy, refused): record and play back instead, so the tap always does something.
                    record(me);
                }
            }

            @Override
            public void onResults(Bundle b) {
                if (me != session) return;
                ArrayList<String> heard = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String best = "";
                float score = 0f;
                if (heard != null) {
                    for (String h : heard) {
                        float s = similarity(target, h);
                        if (s > score || best.isEmpty()) {
                            score = Math.max(score, s);
                            best = h;
                        }
                    }
                }
                finish(me, stars(score), best);
            }
        });
        try {
            recognizer.startListening(i);
        } catch (RuntimeException e) {
            destroyRecognizer();
            record(me);
            return;
        }
        // A recogniser that never gets ready is as good as none.
        main.postDelayed(() -> {
            if (me == session && recognizer != null && !ready) {
                destroyRecognizer();
                record(me);
            }
        }, 3000);
        // Never listen for ever: after ten seconds, judge what was said.
        main.postDelayed(() -> {
            if (me == session && recognizer != null) finishEarly();
        }, 10000);
    }

    /** "I've finished": the child tapped the microphone again. Judges what was said so far. */
    void finishEarly() {
        int me = session;
        if (recordingFlag != null) {
            recordingFlag[0] = false; // the recording ends and is judged
        } else if (recognizer != null) {
            try {
                recognizer.stopListening();
            } catch (RuntimeException ignored) {
                // already stopping
            }
            // Some recognisers never answer after a stop: don't leave the child waiting.
            main.postDelayed(() -> finish(me, 0, ""), 2500);
        }
    }

    /** Stops listening without judging (a new card, or leaving the screen). The result hears stars = -1. */
    void stop() {
        int me = session;
        destroyRecognizer();
        if (recordingFlag != null) recordingFlag[0] = false;
        recordingFlag = null;
        Result r = pending;
        pending = null;
        session++;
        if (r != null && me == session - 1) r.done(-1, null);
    }

    private void destroyRecognizer() {
        if (recognizer != null) {
            try {
                recognizer.destroy();
            } catch (RuntimeException ignored) {
                // already gone
            }
            recognizer = null;
        }
    }

    private void finish(int me, int stars, String heard) {
        if (me != session) return;
        destroyRecognizer();
        recordingFlag = null;
        Result r = pending;
        pending = null;
        session++;
        if (r != null) r.done(stars, heard);
    }

    // ---------------------------------------------------------------- record and play back

    /** Records up to six seconds (or until the child taps again); a clear voice of a second or more earns three stars. */
    private void record(int me) {
        destroyRecognizer();
        boolean[] go = {true};
        recordingFlag = go;
        Level meter = this.meter;
        new Thread(() -> {
            int rate = 16000;
            int min = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            AudioRecord rec;
            try {
                rec = new AudioRecord(MediaRecorder.AudioSource.MIC, rate, AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT, Math.max(min, rate));
                if (rec.getState() != AudioRecord.STATE_INITIALIZED) throw new IllegalStateException("no mic");
                rec.startRecording();
            } catch (RuntimeException e) { // includes SecurityException: no microphone permission
                main.post(() -> finish(me, 0, null));
                return;
            }
            short[] all = new short[rate * 6];
            short[] buf = new short[rate / 20];
            int n = 0, loud = 0, quietAfter = 0;
            while (go[0] && n + buf.length <= all.length) {
                int got = rec.read(buf, 0, buf.length);
                if (got <= 0) break;
                System.arraycopy(buf, 0, all, n, got);
                n += got;
                double sum = 0;
                for (int i = 0; i < got; i++) sum += buf[i] * (double) buf[i];
                float rms = (float) Math.sqrt(sum / got) / 3000f;
                if (meter != null) main.post(() -> {
                    if (me == session) meter.level(Math.min(1f, rms));
                });
                if (rms > 0.15f) {
                    loud++;
                    quietAfter = 0;
                } else if (loud > 0 && ++quietAfter > 30) {
                    break; // a second and a half of quiet after speaking: they're done
                }
            }
            try {
                rec.stop();
            } catch (RuntimeException ignored) {
                // stopped already
            }
            rec.release();
            short[] clip = new short[n];
            System.arraycopy(all, 0, clip, 0, n);
            boolean spokeHere = loud > 0;
            int stars = loud >= 20 ? 3 : loud >= 10 ? 2 : loud >= 4 ? 1 : 0;
            main.post(() -> {
                if (me != session) return; // cancelled meanwhile
                last = clip;
                finish(me, stars, null);
                if (spokeHere) playBack();
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
