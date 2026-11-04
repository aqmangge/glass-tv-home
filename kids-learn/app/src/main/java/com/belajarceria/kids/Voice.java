package com.belajarceria.kids;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

/**
 * Says letters, numbers and words aloud with the phone's own text-to-speech. Arabic is spoken with an Arabic
 * voice when the phone has one; otherwise the Malaysian transliteration is read in English, so it never goes silent.
 */
final class Voice implements TextToSpeech.OnInitListener {
    private static final Locale ARABIC = Locale.forLanguageTag("ar");
    private static final Locale ENGLISH = Locale.US;
    private static final Locale MALAY = Locale.forLanguageTag("ms-MY");
    /** Indonesian is spelt and said almost like Malay, and far more phones have a voice for it. */
    private static final Locale INDONESIAN = Locale.forLanguageTag("id-ID");
    private static final Locale CHINESE = Locale.SIMPLIFIED_CHINESE;

    private final TextToSpeech tts;
    private boolean ready, arabic;
    private Locale malay;
    private boolean muted, chinese;
    private int id;

    Voice(Context c) {
        tts = new TextToSpeech(c.getApplicationContext(), this);
    }

    @Override
    public void onInit(int status) {
        if (status != TextToSpeech.SUCCESS) return;
        ready = true;
        arabic = tts.isLanguageAvailable(ARABIC) >= TextToSpeech.LANG_AVAILABLE;
        if (tts.isLanguageAvailable(MALAY) >= TextToSpeech.LANG_AVAILABLE) malay = MALAY;
        else if (tts.isLanguageAvailable(INDONESIAN) >= TextToSpeech.LANG_AVAILABLE) malay = INDONESIAN;
        chinese = tts.isLanguageAvailable(CHINESE) >= TextToSpeech.LANG_AVAILABLE;
        tts.setSpeechRate(0.85f);
        tts.setPitch(1.1f);
    }

    void say(String text) {
        speak(text, ENGLISH, true);
    }

    /** Reads Malay with a Malay (or Indonesian) voice; stays quiet rather than mangle it with an English one. */
    void sayMalay(String text) {
        if (malay != null) speak(text, malay, true);
        else stop();
    }

    /** Reads one item the way a teacher would. */
    void item(Content.Subject s, Content.Item it) {
        switch (s.kind) {
            case LETTERS:
                say(it.sub != null ? it.word + ", " + it.sub : it.big + " is for " + it.word);
                break;
            case NUMBERS:
                say(it.word);
                break;
            case HIJAIYAH:
                arabicOr(it.big, it.word);
                break;
            default:
                arabicOr(it.big, it.word);
                if (it.sub != null) speak(it.sub, ENGLISH, false);
                break;
        }
    }

    private void arabicOr(String ar, String fallback) {
        if (arabic) speak(ar, ARABIC, true);
        else speak(fallback, ENGLISH, true);
    }

    /** Silences everything (the test's "Senyap" button) until unmuted. */
    void setMuted(boolean m) {
        muted = m;
        if (m) stop();
    }

    /**
     * Reads Mandarin with the phone's Chinese voice, slower for learners. Returns false when the phone has none
     * (the screen then says how to add one).
     */
    boolean sayChinese(String text, boolean slow) {
        if (!ready) return true;
        if (!chinese) return false;
        tts.setSpeechRate(slow ? 0.6f : 0.85f);
        speak(text, CHINESE, true);
        tts.setSpeechRate(0.85f);
        return true;
    }

    boolean hasMalay() {
        return malay != null;
    }

    void stop() {
        if (ready) tts.stop();
    }

    void shutdown() {
        tts.shutdown();
    }

    private void speak(String text, Locale locale, boolean flush) {
        if (!ready || muted || text == null || text.isEmpty()) return;
        tts.setLanguage(locale);
        tts.speak(text, flush ? TextToSpeech.QUEUE_FLUSH : TextToSpeech.QUEUE_ADD, null, "say" + (id++));
    }
}
