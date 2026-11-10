package com.belajarceria.kids;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The Year 1 diagnostic test (Ujian Diagnostik Tahun 1): four parts of 25 marks, each made of five kinds of
 * question with five questions each, following the school's paper. Questions are drawn fresh from banks every
 * time, so a child can take it again without learning the answers by heart.
 */
final class Exam {
    static final int BM = 0, EN = 1, MT = 2, JW = 3, ZH = 4;
    /** The Year 1 paper's four parts; Mandarin (ZH) is a test of its own, in Cakap Mandarin. */
    static final int[] YEAR_ONE = {BM, EN, MT, JW};
    static final String[] NAMES = {"Bahasa Melayu", "Bahasa Inggeris", "Matematik", "Jawi", "Bahasa Cina"};
    static final String[] ICONS = {"📖", "🔤", "🧮", "🕌", "🏮"};
    static final int[][] COLORS = {{0xFFE53935, 0xFFFF7A59}, {0xFF1E6FE0, 0xFF3FB6FF}, {0xFF14A05A, 0xFF5BD68F},
            {0xFF7B3FD6, 0xFFB57BFF}, {0xFFC62828, 0xFFFFA000}};
    static final int PER_SECTION = 25;

    enum Stem { TEXT, PICTURE, SWATCH, SHAPE, JAWI }

    static final class Q {
        int section;
        /** The instruction in Malay and in English, as on the paper; shown and read aloud in the chosen one. */
        String promptMs, promptEn;
        Stem kind;
        /** Mandarin read aloud after the instruction (the word or the friend's line), or null. */
        String zh;
        /** Text, emoji, or Jawi shown big; for SWATCH and SHAPE see color and shape. */
        String stem = "";
        /** A line under the stem, such as "_ _ ju" or "2 + 1 = ?". May be empty. */
        String caption = "";
        int color, shape;
        String[] options;
        int answer;
        boolean jawiOptions;
        String chosen;

        boolean right() {
            return options[answer].equals(chosen);
        }
    }

    private final Random rnd;

    Exam(Random rnd) {
        this.rnd = rnd;
    }

    /** The 25 questions of one part, in the paper's order: five of each kind. */
    List<Q> section(int s) {
        List<Q> out = new ArrayList<>();
        switch (s) {
            case BM: bm(out); break;
            case EN: en(out); break;
            case MT: mt(out); break;
            case ZH: zh(out); break;
            default: jw(out); break;
        }
        for (Q q : out) q.section = s;
        return out;
    }

    // ---------------------------------------------------------------- Bahasa Melayu

    private static final String[][] BM_WORDS = {
            {"kucing", "🐱"}, {"ikan", "🐟"}, {"bola", "⚽"}, {"rumah", "🏠"}, {"buku", "📚"}, {"ayam", "🐔"},
            {"epal", "🍎"}, {"kereta", "🚗"}, {"bunga", "🌸"}, {"bulan", "🌙"}, {"kasut", "👟"}, {"pisang", "🍌"},
            {"matahari", "☀️"}, {"pokok", "🌳"}, {"itik", "🦆"}, {"susu", "🥛"},
    };
    /** Picture, word, the hidden syllable, and where it is (true: at the start). */
    private static final Object[][] BM_SYLLABLES = {
            {"👕", "baju", "ba", true}, {"👁️", "mata", "ma", true}, {"📚", "buku", "ku", false},
            {"🦶", "kaki", "ki", false}, {"🧢", "topi", "pi", false}, {"⚽", "bola", "bo", true},
            {"🥛", "susu", "su", false}, {"🍞", "roti", "ti", false}, {"🐴", "kuda", "ku", true},
            {"🚚", "lori", "ri", false}, {"🦌", "rusa", "ru", true}, {"🍵", "teko", "te", true},
    };
    private static final String[] SYLLABLES = {"ba", "ma", "ku", "ki", "pi", "bo", "su", "ti", "ri", "ru", "te",
            "sa", "na", "lo", "pa"};
    private static final String[][] BM_INI = {
            {"🍎", "epal", "ayam"}, {"🐔", "ayam", "bola"}, {"🚗", "kereta", "kerusi"}, {"🌙", "bulan", "bunga"},
            {"🌸", "bunga", "bulan"}, {"🐟", "ikan", "itik"}, {"⚽", "bola", "buku"}, {"🏠", "rumah", "rusa"},
            {"🐱", "kucing", "kuda"}, {"🍌", "pisang", "pinggan"},
    };

    private void bm(List<Q> out) {
        // 1. Vowels: pick the vowel among consonants.
        List<String> vowels = new ArrayList<>(Arrays.asList("a", "e", "i", "o", "u"));
        Collections.shuffle(vowels, rnd);
        // No "l": in this font a small L looks just like a capital I.
        List<String> consonants = Arrays.asList("b", "c", "d", "g", "k", "m", "n", "p", "r", "s", "t");
        for (String v : vowels) {
            Q q = q("Pilih huruf vokal.", "Choose the vowel.", Stem.TEXT, "🔤");
            choices(q, v, consonants, 3);
            out.add(q);
        }
        // 2. Match the picture to its word.
        for (String[] w : pick(BM_WORDS, 5)) {
            Q q = q("Pilih perkataan bagi gambar ini.", "Choose the word for this picture.", Stem.PICTURE, w[1]);
            choices(q, w[0], column(BM_WORDS, 0), 3);
            out.add(q);
        }
        // 3. Complete the word with the right syllable.
        for (Object[] w : pick(BM_SYLLABLES, 5)) {
            String word = (String) w[1], syl = (String) w[2];
            Q q = q("Lengkapkan perkataan dengan suku kata yang betul.", "Complete the word with the right syllable.", Stem.PICTURE, (String) w[0]);
            q.caption = (Boolean) w[3] ? "_ _ " + word.substring(2) : word.substring(0, 2) + " _ _";
            choices(q, syl, Arrays.asList(SYLLABLES), 3);
            out.add(q);
        }
        // 4. The capital letter for a small letter.
        for (String l : letters(5)) {
            Q q = q("Pilih huruf besar bagi huruf ini.", "Choose the capital letter for this letter.", Stem.TEXT, l);
            choices(q, l.toUpperCase(), upperAlphabet(), 3);
            out.add(q);
        }
        // 5. "Ini ____." — tick the right word.
        for (String[] w : pick(BM_INI, 5)) {
            Q q = q("Ini apa? Pilih perkataan yang betul.", "What is this? Choose the right word.", Stem.PICTURE, w[0]);
            q.caption = "Ini ______ .";
            pair(q, w[1], w[2]);
            out.add(q);
        }
    }

    // ---------------------------------------------------------------- English

    private static final String[][] EN_START = {
            {"🍎", "a"}, {"🐝", "b"}, {"🐱", "c"}, {"🐶", "d"}, {"🥚", "e"}, {"🐟", "f"}, {"🍇", "g"}, {"🏠", "h"},
            {"🔑", "k"}, {"👃", "n"}, {"🌙", "m"}, {"🐧", "p"}, {"🌈", "r"}, {"☀️", "s"}, {"🌳", "t"}, {"🐳", "w"},
            {"🦓", "z"},
    };
    private static final String[][] EN_CVC = {
            {"🐱", "c_t", "a"}, {"🐶", "d_g", "o"}, {"☀️", "s_n", "u"}, {"🖊️", "p_n", "e"}, {"🎒", "b_g", "a"},
            {"🎩", "h_t", "a"}, {"🚌", "b_s", "u"}, {"🐷", "p_g", "i"}, {"🦊", "f_x", "o"}, {"🛏️", "b_d", "e"},
            {"🥤", "c_p", "u"}, {"🐔", "h_n", "e"},
    };
    private static final Object[][] COLOURS = {
            {"red", 0xFFE53935}, {"yellow", 0xFFFDD835}, {"blue", 0xFF1E88E5}, {"green", 0xFF43A047},
            {"orange", 0xFFFB8C00}, {"purple", 0xFF8E24AA}, {"pink", 0xFFF06292}, {"brown", 0xFF8D6E63},
            {"black", 0xFF263238},
    };
    private static final String[][] EN_THIS = {
            {"🐟", "fish", "ball"}, {"📚", "book", "bag"}, {"🚌", "bus", "box"}, {"🍌", "banana", "apple"},
            {"🎩", "hat", "hen"}, {"🐱", "cat", "car"}, {"🐶", "dog", "doll"}, {"☀️", "sun", "sock"},
            {"🍎", "apple", "ant"}, {"🐔", "hen", "hat"}, {"🚗", "car", "cat"},
    };

    private void en(List<Q> out) {
        // 1. Capital letter to small letter.
        for (String l : letters(5)) {
            Q q = q("Pilih huruf kecil.", "Choose the small letter.", Stem.TEXT, l.toUpperCase());
            choices(q, l, lowerAlphabet(), 3);
            out.add(q);
        }
        // 2. The beginning sound of a picture.
        for (String[] w : pick(EN_START, 5)) {
            Q q = q("Pilih bunyi huruf awal.", "Choose the beginning sound.", Stem.PICTURE, w[0]);
            choices(q, w[1], lowerAlphabet(), 3);
            out.add(q);
        }
        // 3. The missing vowel in a three-letter word.
        for (String[] w : pick(EN_CVC, 5)) {
            Q q = q("Isi huruf yang tertinggal.", "Fill in the missing letter.", Stem.PICTURE, w[0]);
            q.caption = w[1].replace("_", " _ ");
            choices(q, w[2], Arrays.asList("a", "e", "i", "o", "u"), 4);
            out.add(q);
        }
        // 4. A colour to its word.
        for (Object[] c : pick(COLOURS, 5)) {
            Q q = q("Apakah warna ini?", "What colour is this?", Stem.SWATCH, "");
            q.color = (Integer) c[1];
            List<String> names = new ArrayList<>();
            for (Object[] o : COLOURS) names.add((String) o[0]);
            choices(q, (String) c[0], names, 3);
            out.add(q);
        }
        // 5. "This is a ____."
        for (String[] w : pick(EN_THIS, 5)) {
            Q q = q("Lihat dan pilih perkataan yang betul.", "Look and choose the correct word.", Stem.PICTURE, w[0]);
            q.caption = "This is a ______ .";
            pair(q, w[1], w[2]);
            out.add(q);
        }
    }

    // ---------------------------------------------------------------- Matematik

    private static final String[] THINGS = {"🍎", "⭐", "🐟", "🎈", "🍭", "🍓", "🐥", "🍪", "🌼", "🚗", "🦋", "🐸"};

    private void mt(List<Q> out) {
        // 1. Count the pictures (1 to 10).
        for (int n : distinct(1, 10, 5)) {
            Q q = q("Kira gambar. Berapa banyak?", "Count the pictures. How many?", Stem.PICTURE, rows(thing(), n));
            numberChoices(q, n, 1, 10);
            out.add(q);
        }
        // 2. The missing number in a sequence (1 to 20).
        for (int i = 0; i < 5; i++) {
            int start = 1 + rnd.nextInt(16), gap = 1 + rnd.nextInt(3);
            StringBuilder b = new StringBuilder();
            for (int k = 0; k < 5; k++) {
                if (k > 0) b.append("   ");
                b.append(k == gap ? "?" : String.valueOf(start + k));
            }
            Q q = q("Nombor apakah yang tertinggal?", "Which number is missing?", Stem.TEXT, b.toString());
            numberChoices(q, start + gap, 1, 20);
            out.add(q);
        }
        // 3. Add with pictures (sums up to 10).
        for (int i = 0; i < 5; i++) {
            int a = 1 + rnd.nextInt(5), b = 1 + rnd.nextInt(Math.min(5, 10 - a));
            String t = thing();
            Q q = q("Tambah. Kira semua gambar.", "Add. Count all the pictures.", Stem.PICTURE, repeat(t, a) + "  ➕  " + repeat(t, b));
            q.caption = a + " + " + b + " = ?";
            numberChoices(q, a + b, 0, 10);
            out.add(q);
        }
        // 4. Take away with pictures.
        for (int i = 0; i < 5; i++) {
            int a = 3 + rnd.nextInt(6), b = 1 + rnd.nextInt(a - 1);
            String t = thing();
            Q q = q("Tolak. Gambar yang dipangkah telah diambil.", "Take away. The crossed pictures are gone.", Stem.PICTURE,
                    repeat(t, a - b) + repeat("❌", b));
            q.caption = a + " − " + b + " = ?";
            numberChoices(q, a - b, 0, 10);
            out.add(q);
        }
        // 5. Name the shape.
        String[] names = {"segi tiga", "bulatan", "segi empat tepat", "segi empat sama", "segi tiga"};
        Integer[] order = {0, 1, 2, 3, 4};
        Collections.shuffle(Arrays.asList(order), rnd);
        for (int s : order) {
            Q q = q("Namakan bentuk ini.", "Name this shape.", Stem.SHAPE, "");
            q.shape = s;
            q.options = new String[]{"bulatan", "segi tiga", "segi empat sama", "segi empat tepat"};
            q.answer = Arrays.asList(q.options).indexOf(names[s]);
            out.add(q);
        }
    }

    // ---------------------------------------------------------------- Jawi

    /** The Jawi alphabet in its taught order (without the rarely taught ۏ and ݢ). */
    static final String[] JAWI = {"ا", "ب", "ت", "ث", "ج", "چ", "ح", "خ", "د", "ذ", "ر", "ز", "س", "ش", "ص",
            "ض", "ط", "ظ", "ع", "غ", "ڠ", "ف", "ڤ", "ق", "ک", "ل", "م", "ن", "و", "ه", "ء", "ي", "ڽ"};
    private static final String[][] JAWI_NAMES = {
            {"ا", "alif"}, {"ب", "ba"}, {"ت", "ta"}, {"ج", "jim"}, {"چ", "ca"}, {"د", "dal"}, {"ر", "ra"},
            {"س", "sin"}, {"ش", "syin"}, {"ف", "fa"}, {"ڤ", "pa"}, {"ک", "kaf"}, {"ل", "lam"}, {"م", "mim"},
            {"ن", "nun"}, {"و", "wau"}, {"ي", "ya"}, {"ڠ", "nga"},
    };
    /** A letter and three that look like it. */
    private static final String[][] LOOKALIKES = {
            {"ب", "ت", "ن", "ث"}, {"ج", "ح", "خ", "چ"}, {"س", "ش", "ص", "ض"}, {"ر", "ز", "و", "د"},
            {"م", "ه", "ن", "ف"}, {"ف", "ق", "ڤ", "غ"}, {"ع", "غ", "ح", "ه"}, {"ن", "ب", "ت", "ي"},
            {"د", "ذ", "ر", "ل"},
    };
    private static final String[][] JAWI_WORDS = {
            {"📚", "buku", "بوکو"}, {"🐟", "ikan", "ايکن"}, {"⚽", "bola", "بولا"}, {"🏠", "rumah", "رومه"},
            {"👁️", "mata", "ماتا"}, {"🐱", "kucing", "کوچيڠ"}, {"🐔", "ayam", "ايم"}, {"🌙", "bulan", "بولن"},
            {"🦶", "kaki", "کاکي"}, {"👕", "baju", "باجو"}, {"🍎", "epal", "ايڤل"}, {"🌸", "bunga", "بوڠا"},
            {"🧢", "topi", "توڤي"}, {"🥛", "susu", "سوسو"},
    };
    /** A word, its Jawi spelling, and a plausible wrong spelling. */
    private static final String[][] JAWI_SPELLING = {
            {"👕", "baju", "باجو", "بيجو"}, {"🦶", "kaki", "کاکي", "کوکي"}, {"🐔", "ayam", "ايم", "اوم"},
            {"🌙", "bulan", "بولن", "بيلن"}, {"📚", "buku", "بوکو", "بيکو"}, {"⚽", "bola", "بولا", "بيلا"},
            {"👁️", "mata", "ماتا", "ميتا"}, {"🏠", "rumah", "رومه", "ريمه"}, {"🐟", "ikan", "ايکن", "اوکن"},
            {"🥛", "susu", "سوسو", "سيسي"},
    };

    private void jw(List<Q> out) {
        // 1. The name of a Jawi letter.
        for (String[] l : pick(JAWI_NAMES, 5)) {
            Q q = q("Apakah nama huruf Jawi ini?", "What is the name of this Jawi letter?", Stem.JAWI, l[0]);
            choices(q, l[1], column(JAWI_NAMES, 1), 3);
            out.add(q);
        }
        // 2. Find the same letter among look-alikes.
        for (String[] set : pick(LOOKALIKES, 5)) {
            Q q = q("Pilih huruf yang sama.", "Choose the same letter.", Stem.JAWI, set[0]);
            List<String> opts = new ArrayList<>(Arrays.asList(set));
            Collections.shuffle(opts, rnd);
            q.options = opts.toArray(new String[0]);
            q.answer = opts.indexOf(set[0]);
            q.jawiOptions = true;
            out.add(q);
        }
        // 3. The missing letter in alphabet order, read right to left.
        for (int start : distinct(0, JAWI.length - 5, 5)) {
            int gap = 1 + rnd.nextInt(3);
            StringBuilder b = new StringBuilder();
            for (int k = 0; k < 5; k++) {
                if (k > 0) b.append("  ");
                b.append(k == gap ? "؟" : JAWI[start + k]);
            }
            Q q = q("Huruf apakah yang tertinggal? Baca dari kanan ke kiri.", "Which letter is missing? Read from right to left.", Stem.JAWI, b.toString());
            choices(q, JAWI[start + gap], Arrays.asList(JAWI), 3);
            q.jawiOptions = true;
            out.add(q);
        }
        // 4. A picture to its Jawi word.
        for (String[] w : pick(JAWI_WORDS, 5)) {
            Q q = q("Pilih perkataan Jawi bagi gambar ini.", "Choose the Jawi word for this picture.", Stem.PICTURE, w[0]);
            choices(q, w[2], column(JAWI_WORDS, 2), 3);
            q.jawiOptions = true;
            out.add(q);
        }
        // 5. The right Jawi spelling of a word.
        for (String[] w : pick(JAWI_SPELLING, 5)) {
            Q q = q("Pilih ejaan Jawi yang betul.", "Choose the correct Jawi spelling.", Stem.PICTURE, w[0]);
            q.caption = w[1];
            pair(q, w[2], w[3]);
            q.jawiOptions = true;
            out.add(q);
        }
    }

    // ---------------------------------------------------------------- Bahasa Cina (Cakap Mandarin)

    private void zh(List<Q> out) {
        String[][] pics = Chinese.PICTURES;
        // 1. A picture to its Chinese word.
        List<String> picWords = new ArrayList<>();
        for (String[] w : pics) picWords.add(w[0] + "  " + w[1]);
        for (String[] w : pick(pics, 5)) {
            Q q = q("Pilih perkataan Cina bagi gambar ini.", "Choose the Chinese word for this picture.", Stem.PICTURE, w[4]);
            choices(q, w[0] + "  " + w[1], picWords, 3);
            out.add(q);
        }
        // 2. What a word means (in Malay).
        List<String> meanings = new ArrayList<>();
        for (Chinese.Phrase p : Chinese.WORDS) meanings.add(p.malay);
        for (Chinese.Phrase p : pick(Chinese.WORDS, 5)) {
            Q q = q("Apakah maksud perkataan ini?", "What does this word mean?", Stem.TEXT, p.hanzi);
            q.caption = p.pinyin;
            q.zh = p.hanzi;
            choices(q, p.malay, meanings, 3);
            out.add(q);
        }
        // 3. Count, and choose the Chinese number.
        List<String> numerals = new ArrayList<>();
        for (String[] n : Chinese.NUMBERS) numerals.add(n[0] + "  " + n[1]);
        for (int n : distinct(1, 10, 5)) {
            Q q = q("Kira gambar. Pilih nombor dalam bahasa Cina.", "Count the pictures. Choose the Chinese number.",
                    Stem.PICTURE, rows(thing(), n));
            List<String> near = new ArrayList<>();
            for (int d = -2; d <= 2; d++) if (d != 0 && n + d >= 1 && n + d <= 10) near.add(numerals.get(n + d - 1));
            choices(q, numerals.get(n - 1), near, 3);
            out.add(q);
        }
        // 4. Read the pinyin, choose the characters.
        List<String> hanzi = new ArrayList<>();
        for (Chinese.Phrase p : Chinese.WORDS) hanzi.add(p.hanzi);
        for (Chinese.Phrase p : pick(Chinese.WORDS, 5)) {
            Q q = q("Baca pinyin ini. Pilih perkataan Cina yang betul.", "Read the pinyin. Choose the right Chinese word.",
                    Stem.TEXT, p.pinyin);
            q.zh = p.hanzi;
            choices(q, p.hanzi, hanzi, 3);
            out.add(q);
        }
        // 5. A friend speaks: choose the right reply, as in a real conversation.
        List<Chinese.Turn> turns = new ArrayList<>();
        for (Chinese.Talk t : Chinese.TALKS) turns.addAll(Arrays.asList(t.turns));
        Collections.shuffle(turns, rnd);
        for (Chinese.Turn t : turns.subList(0, 5)) {
            Q q = q("Kawan berkata begini. Apakah jawapan yang betul?", "Your friend says this. What is the right reply?",
                    Stem.TEXT, "🐼 " + t.friend.hanzi);
            q.caption = t.friend.pinyin + "  ·  " + t.friend.malay;
            q.zh = t.friend.hanzi;
            pair(q, t.reply.hanzi, t.wrong.hanzi);
            out.add(q);
        }
    }

    // ---------------------------------------------------------------- helpers

    private static Q q(String promptMs, String promptEn, Stem kind, String stem) {
        Q q = new Q();
        q.promptMs = promptMs;
        q.promptEn = promptEn;
        q.kind = kind;
        q.stem = stem;
        return q;
    }

    /** The answer and n - 1 different wrong answers from the pool, in random order. */
    private void choices(Q q, String answer, List<String> pool, int n) {
        List<String> others = new ArrayList<>();
        for (String p : pool) if (!p.equals(answer) && !others.contains(p)) others.add(p);
        Collections.shuffle(others, rnd);
        List<String> opts = new ArrayList<>(others.subList(0, Math.min(n - 1, others.size())));
        opts.add(answer);
        Collections.shuffle(opts, rnd);
        q.options = opts.toArray(new String[0]);
        q.answer = opts.indexOf(answer);
    }

    private void pair(Q q, String right, String wrong) {
        boolean first = rnd.nextBoolean();
        q.options = first ? new String[]{right, wrong} : new String[]{wrong, right};
        q.answer = first ? 0 : 1;
    }

    /** The right number and two near it, so the child has to count rather than guess. */
    private void numberChoices(Q q, int n, int min, int max) {
        List<String> near = new ArrayList<>();
        for (int d = -2; d <= 2; d++) if (d != 0 && n + d >= min && n + d <= max) near.add(String.valueOf(n + d));
        choices(q, String.valueOf(n), near, 3);
    }

    private <T> List<T> pick(T[] all, int n) {
        List<T> list = new ArrayList<>(Arrays.asList(all));
        Collections.shuffle(list, rnd);
        return list.subList(0, n);
    }

    private static List<String> column(String[][] rows, int col) {
        List<String> out = new ArrayList<>();
        for (String[] r : rows) out.add(r[col]);
        return out;
    }

    private List<Integer> distinct(int from, int to, int n) {
        List<Integer> all = new ArrayList<>();
        for (int i = from; i <= to; i++) all.add(i);
        Collections.shuffle(all, rnd);
        return all.subList(0, n);
    }

    private List<String> letters(int n) {
        List<String> all = lowerAlphabet();
        Collections.shuffle(all, rnd);
        return all.subList(0, n);
    }

    /** a to z without l, which looks like a capital I in the app's font (and so does I like l). */
    private static List<String> lowerAlphabet() {
        List<String> out = new ArrayList<>();
        for (char c = 'a'; c <= 'z'; c++) if (c != 'l') out.add(String.valueOf(c));
        return out;
    }

    private static List<String> upperAlphabet() {
        List<String> out = new ArrayList<>();
        for (char c = 'A'; c <= 'Z'; c++) if (c != 'I') out.add(String.valueOf(c));
        return out;
    }

    private String thing() {
        return THINGS[rnd.nextInt(THINGS.length)];
    }

    private static String repeat(String s, int n) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < n; i++) b.append(s);
        return b.toString();
    }

    /** Rows of five, the way the paper lays out pictures to count. */
    private static String rows(String s, int n) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0 && i % 5 == 0) b.append('\n');
            b.append(s);
        }
        return b.toString();
    }

    /** The paper's own guide for one part's mark out of 25. */
    static String band(int score) {
        if (score >= 21) return "Cemerlang";
        if (score >= 15) return "Baik";
        if (score >= 10) return "Perlu bimbingan";
        return "Perlu intervensi segera";
    }
}
