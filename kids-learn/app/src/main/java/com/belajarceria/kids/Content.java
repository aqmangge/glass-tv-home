package com.belajarceria.kids;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Everything the child learns: five subjects, each a list of items. */
final class Content {
    enum Kind { LETTERS, NUMBERS, HIJAIYAH, WORDS, CHINESE }

    static final class Item {
        /** What is shown big, traced and dragged: "A", "3", "ب", "مسجد". */
        final String big;
        /** Its name: "Apple", "Three", "Ba", "Masjid". */
        final String word;
        /** A small extra line: the Malay number, or the meaning of an Arabic word. May be null. */
        final String sub;
        /** The picture (emoji, or a row of them to count). Null for Hijaiyah letters. */
        final String pic;

        /** The English meaning, for the Chinese games' hint button. May be null. */
        final String en;

        Item(String big, String word, String sub, String pic) {
            this(big, word, sub, pic, null);
        }

        Item(String big, String word, String sub, String pic, String en) {
            this.big = big;
            this.word = word;
            this.sub = sub;
            this.pic = pic;
            this.en = en;
        }
    }

    static final class Subject {
        final String title, subtitle, icon;
        /** The subject's gradient pair; set by the current {@link Theme}. */
        int[] colors;
        final Kind kind;
        final Item[] items;

        Subject(String title, String subtitle, String icon, int[] colors, Kind kind, Item[] items) {
            this.title = title;
            this.subtitle = subtitle;
            this.icon = icon;
            this.colors = colors;
            this.kind = kind;
            this.items = items;
        }

        /** Words shown as a word over its reading (Arabic and Chinese words), rather than one big letter. */
        boolean wordTiles() {
            return kind == Kind.WORDS || kind == Kind.CHINESE;
        }

        boolean arabic() {
            return kind == Kind.HIJAIYAH || kind == Kind.WORDS;
        }

        int[] palette() {
            return Content.palette(colors[0], colors[1]);
        }
    }

    static final Subject[] SUBJECTS = {
            new Subject("ABC", "Letters A to Z", "ABC",
                    new int[]{0xFFFF4F86, 0xFFFFA45B}, Kind.LETTERS, letters()),
            new Subject("123", "Numbers 1 to 10", "123",
                    new int[]{0xFF4F7BFF, 0xFF3FD0FF}, Kind.NUMBERS, numbers()),
            new Subject("Alif Ba Ta", "Huruf Hijaiyah", "ا ب ت",
                    new int[]{0xFF0FB57F, 0xFF7BE495}, Kind.HIJAIYAH, hijaiyah()),
            new Subject("Islamic Words", "Kalimah Islam", "🕌",
                    new int[]{0xFF8A5CFF, 0xFFD16BFF}, Kind.WORDS, islamic()),
            new Subject("Arabic Words", "Kosa Kata Arab", "🐫",
                    new int[]{0xFFFF8A1C, 0xFFFFC93F}, Kind.WORDS, arabicWords()),
    };

    /** Stands in for an emoji where none exists: shown as a {@link XylophoneDrawable} (see {@link Ui#picture}). */
    static final String XYLOPHONE = "[xylophone]";

    private Content() {}

    static int[] palette(int a, int b) {
        int[] x = Theme.current.extras;
        return new int[]{mix(0xFFFFFFFF, a, 0.14f), mix(0xFFFFFFFF, b, 0.22f), a, b, x[0], x[1]};
    }

    static int mix(int x, int y, float t) {
        int a = Math.round(((x >>> 24) & 255) * (1 - t) + ((y >>> 24) & 255) * t);
        int r = Math.round(((x >> 16) & 255) * (1 - t) + ((y >> 16) & 255) * t);
        int g = Math.round(((x >> 8) & 255) * (1 - t) + ((y >> 8) & 255) * t);
        int b = Math.round((x & 255) * (1 - t) + (y & 255) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** n different items in random order, no two with the same name or picture (so every answer is fair). */
    static Item[] pick(Item[] all, int n, Random rnd) {
        List<Item> list = new ArrayList<>();
        Collections.addAll(list, all);
        Collections.shuffle(list, rnd);
        List<Item> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Item it : list) {
            if (out.size() == n) break;
            if (!seen.add("w" + it.word) || (it.pic != null && !seen.add("p" + it.pic))) continue;
            out.add(it);
        }
        return out.toArray(new Item[0]);
    }

    static Item[] shuffled(Item[] items, Random rnd) {
        List<Item> list = new ArrayList<>();
        Collections.addAll(list, items);
        Collections.shuffle(list, rnd);
        return list.toArray(new Item[0]);
    }

    private static Item[] letters() {
        String[][] d = {
                {"A", "Apple", "🍎"}, {"B", "Ball", "⚽"}, {"C", "Cat", "🐱"}, {"D", "Duck", "🦆"},
                {"E", "Egg", "🥚"}, {"F", "Fish", "🐟"}, {"G", "Grapes", "🍇"}, {"H", "House", "🏠"},
                {"I", "Ice cream", "🍦"}, {"J", "Jeans", "👖"}, {"K", "Key", "🔑"}, {"L", "Lion", "🦁"},
                {"M", "Moon", "🌙"}, {"N", "Nose", "👃"}, {"O", "Orange", "🍊"}, {"P", "Penguin", "🐧"},
                {"Q", "Queen", "👸"}, {"R", "Rainbow", "🌈"}, {"S", "Sun", "☀️"}, {"T", "Tree", "🌳"},
                {"U", "Umbrella", "☂️"}, {"V", "Van", "🚐"}, {"W", "Whale", "🐳"}, {"X", "Xylophone", XYLOPHONE},
                {"Y", "Yo-yo", "🪀"}, {"Z", "Zebra", "🦓"},
        };
        Item[] out = new Item[d.length];
        for (int i = 0; i < d.length; i++) {
            out[i] = new Item(d[i][0], d[i][1], null, d[i][2]);
        }
        return out;
    }

    private static Item[] numbers() {
        String[] words = {"One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten"};
        String[] malay = {"Satu", "Dua", "Tiga", "Empat", "Lima", "Enam", "Tujuh", "Lapan", "Sembilan", "Sepuluh"};
        String[] things = {"🍎", "🐥", "🍓", "🐟", "⭐", "🎈", "🐞", "🍪", "🌸", "🦋"};
        Item[] out = new Item[10];
        for (int i = 0; i < 10; i++) {
            out[i] = new Item(String.valueOf(i + 1), words[i], malay[i], count(things[i], i + 1));
        }
        return out;
    }

    /** "🍎🍎🍎🍎🍎\n🍎🍎": rows of five, the way small children count on their fingers. */
    private static String count(String e, int n) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0 && i % 5 == 0) b.append('\n');
            b.append(e);
        }
        return b.toString();
    }

    private static Item[] hijaiyah() {
        // Names as taught in Malaysian Iqra' classes. ح and ه are both "Ha": the dot tells them apart.
        String[][] d = {
                {"ا", "Alif"}, {"ب", "Ba"}, {"ت", "Ta"}, {"ث", "Tsa"}, {"ج", "Jim"}, {"ح", "Ḥa"},
                {"خ", "Kho"}, {"د", "Dal"}, {"ذ", "Dzal"}, {"ر", "Ro"}, {"ز", "Zai"}, {"س", "Sin"},
                {"ش", "Syin"}, {"ص", "Sod"}, {"ض", "Dhod"}, {"ط", "Tho"}, {"ظ", "Zho"}, {"ع", "'Ain"},
                {"غ", "Ghain"}, {"ف", "Fa"}, {"ق", "Qof"}, {"ك", "Kaf"}, {"ل", "Lam"}, {"م", "Mim"},
                {"ن", "Nun"}, {"و", "Wau"}, {"ه", "Ha"}, {"ء", "Hamzah"}, {"ي", "Ya"},
        };
        Item[] out = new Item[d.length];
        for (int i = 0; i < d.length; i++) out[i] = new Item(d[i][0], d[i][1], null, null);
        return out;
    }

    private static Item[] islamic() {
        return new Item[]{
                new Item("الله", "Allah", "The One God", "☝️"),
                new Item("بسم الله", "Bismillah", "In the name of Allah", "🤲"),
                new Item("الحمد لله", "Alhamdulillah", "All praise is for Allah", "😊"),
                new Item("سبحان الله", "Subhanallah", "Glory be to Allah", "🌸"),
                new Item("الله أكبر", "Allahu Akbar", "Allah is the Greatest", "✨"),
                new Item("السلام عليكم", "Assalamualaikum", "Peace be upon you", "👋"),
                new Item("إن شاء الله", "Insya-Allah", "If Allah wills", "🌤️"),
                new Item("ما شاء الله", "Masya-Allah", "What Allah has willed", "🌟"),
                new Item("جزاك الله خيرا", "Jazakallahu khairan", "May Allah reward you", "🎁"),
                new Item("مسجد", "Masjid", "Mosque", "🕌"),
                new Item("الكعبة", "Kaabah", "The Kaabah in Makkah", "🕋"),
                new Item("القرآن", "Al-Quran", "The holy book", "📖"),
                new Item("صلاة", "Solat", "Prayer", "🧎"),
                new Item("رمضان", "Ramadan", "The month of fasting", "🌙"),
        };
    }

    private static Item[] arabicWords() {
        return new Item[]{
                new Item("كتاب", "Kitab", "Book", "📚"),
                new Item("قلم", "Qalam", "Pen", "✏️"),
                new Item("بيت", "Bait", "House", "🏠"),
                new Item("شمس", "Syams", "Sun", "☀️"),
                new Item("قمر", "Qamar", "Moon", "🌙"),
                new Item("ماء", "Maa'", "Water", "💧"),
                new Item("قطة", "Qittah", "Cat", "🐱"),
                new Item("أسد", "Asad", "Lion", "🦁"),
                new Item("سمكة", "Samakah", "Fish", "🐟"),
                new Item("تفاحة", "Tuffahah", "Apple", "🍎"),
                new Item("موز", "Mauz", "Banana", "🍌"),
                new Item("شجرة", "Syajarah", "Tree", "🌳"),
                new Item("باب", "Bab", "Door", "🚪"),
                new Item("نجمة", "Najmah", "Star", "⭐"),
                new Item("وردة", "Wardah", "Flower", "🌹"),
                new Item("سيارة", "Sayyarah", "Car", "🚗"),
                new Item("طائر", "Ta'ir", "Bird", "🐦"),
                new Item("جمل", "Jamal", "Camel", "🐫"),
                new Item("حليب", "Halib", "Milk", "🥛"),
                new Item("عين", "'Ain", "Eye", "👁️"),
        };
    }
}
