package com.belajarceria.kids;

/**
 * Cakap Mandarin: Mandarin for speaking in front of people, in four steps. Words first, then short sentences, then
 * conversations with a friend, then short speeches on a stage. Simplified characters with Hanyu Pinyin, as taught
 * in Malaysian schools, each with its meaning in Malay and English.
 */
final class Chinese {
    static final class Phrase {
        final String hanzi, pinyin, malay, english;

        Phrase(String hanzi, String pinyin, String malay, String english) {
            this.hanzi = hanzi;
            this.pinyin = pinyin;
            this.malay = malay;
            this.english = english;
        }
    }

    /** One turn in a conversation: the friend speaks, the child picks the right reply (index 0) and says it. */
    static final class Turn {
        final Phrase friend, reply, wrong;

        Turn(Phrase friend, Phrase reply, Phrase wrong) {
            this.friend = friend;
            this.reply = reply;
            this.wrong = wrong;
        }
    }

    static final class Talk {
        final String title, emoji;
        final Turn[] turns;

        Talk(String title, String emoji, Turn... turns) {
            this.title = title;
            this.emoji = emoji;
            this.turns = turns;
        }
    }

    static final class Speech {
        final String title, emoji;
        final Phrase[] lines;

        Speech(String title, String emoji, Phrase... lines) {
            this.title = title;
            this.emoji = emoji;
            this.lines = lines;
        }
    }

    static final String[][] LEVELS = {
            {"1", "词语", "Perkataan", "Words", "🔤"},
            {"2", "短句", "Ayat Mudah", "Simple sentences", "💬"},
            {"3", "对话", "Perbualan", "Conversations", "🗣️"},
            {"4", "演讲", "Ucapan Awam", "Public speaking", "🎤"},
    };

    private static Phrase p(String hanzi, String pinyin, String malay, String english) {
        return new Phrase(hanzi, pinyin, malay, english);
    }

    // ---------------------------------------------------------------- level 1: words

    static final Phrase[] WORDS = {
            p("你好", "nǐ hǎo", "Helo", "Hello"),
            p("早上好", "zǎoshang hǎo", "Selamat pagi", "Good morning"),
            p("谢谢", "xièxie", "Terima kasih", "Thank you"),
            p("再见", "zàijiàn", "Selamat tinggal", "Goodbye"),
            p("大家", "dàjiā", "Semua orang", "Everyone"),
            p("欢迎", "huānyíng", "Selamat datang", "Welcome"),
            p("请", "qǐng", "Sila", "Please"),
            p("我", "wǒ", "Saya", "I, me"),
            p("你", "nǐ", "Awak", "You"),
            p("是", "shì", "Ialah", "Am, is, are"),
            p("叫", "jiào", "Bernama", "To be called"),
            p("名字", "míngzi", "Nama", "Name"),
            p("岁", "suì", "Tahun (umur)", "Years old"),
            p("学生", "xuésheng", "Murid", "Student"),
            p("老师", "lǎoshī", "Cikgu", "Teacher"),
            p("朋友", "péngyou", "Kawan", "Friend"),
            p("爸爸", "bàba", "Ayah", "Father"),
            p("妈妈", "māma", "Ibu", "Mother"),
            p("家", "jiā", "Rumah, keluarga", "Home, family"),
            p("学校", "xuéxiào", "Sekolah", "School"),
            p("喜欢", "xǐhuan", "Suka", "To like"),
            p("爱", "ài", "Sayang, cinta", "To love"),
            p("高兴", "gāoxìng", "Gembira", "Happy"),
            p("今天", "jīntiān", "Hari ini", "Today"),
    };

    // ---------------------------------------------------------------- games: things with a picture

    /** Hanzi, pinyin, Malay, English and a picture: for Drag & drop, Match and the test. */
    static final String[][] PICTURES = {
            {"猫", "māo", "Kucing", "Cat", "🐱"},
            {"兔子", "tùzi", "Arnab", "Rabbit", "🐰"},
            {"鱼", "yú", "Ikan", "Fish", "🐟"},
            {"鸟", "niǎo", "Burung", "Bird", "🐦"},
            {"熊猫", "xióngmāo", "Panda", "Panda", "🐼"},
            {"大象", "dàxiàng", "Gajah", "Elephant", "🐘"},
            {"苹果", "píngguǒ", "Epal", "Apple", "🍎"},
            {"香蕉", "xiāngjiāo", "Pisang", "Banana", "🍌"},
            {"西瓜", "xīguā", "Tembikai", "Watermelon", "🍉"},
            {"牛奶", "niúnǎi", "Susu", "Milk", "🥛"},
            {"水", "shuǐ", "Air", "Water", "💧"},
            {"书", "shū", "Buku", "Book", "📚"},
            {"笔", "bǐ", "Pensel", "Pencil", "✏️"},
            {"球", "qiú", "Bola", "Ball", "⚽"},
            {"车", "chē", "Kereta", "Car", "🚗"},
            {"飞机", "fēijī", "Kapal terbang", "Aeroplane", "✈️"},
            {"房子", "fángzi", "Rumah", "House", "🏠"},
            {"学校", "xuéxiào", "Sekolah", "School", "🏫"},
            {"太阳", "tàiyáng", "Matahari", "Sun", "☀️"},
            {"月亮", "yuèliang", "Bulan", "Moon", "🌙"},
            {"星星", "xīngxing", "Bintang", "Star", "⭐"},
            {"花", "huā", "Bunga", "Flower", "🌸"},
            {"树", "shù", "Pokok", "Tree", "🌳"},
            {"手", "shǒu", "Tangan", "Hand", "✋"},
    };

    /** 一 to 十, with pinyin. */
    static final String[][] NUMBERS = {
            {"一", "yī"}, {"二", "èr"}, {"三", "sān"}, {"四", "sì"}, {"五", "wǔ"},
            {"六", "liù"}, {"七", "qī"}, {"八", "bā"}, {"九", "jiǔ"}, {"十", "shí"},
    };

    /** The picture words as a subject, so the Drag & drop and Match games can play them. */
    static Content.Subject gameSubject() {
        Content.Item[] items = new Content.Item[PICTURES.length];
        for (int i = 0; i < items.length; i++) {
            String[] w = PICTURES[i];
            items[i] = new Content.Item(w[0], w[1], w[2], w[4]);
        }
        return new Content.Subject("华语", "Mandarin", "中文", ZhKit.RED_GOLD, Content.Kind.CHINESE, items);
    }

    // ---------------------------------------------------------------- level 2: simple sentences

    static final Phrase[] SENTENCES = {
            p("大家好！", "Dàjiā hǎo!", "Helo semua!", "Hello, everyone!"),
            p("早上好，老师！", "Zǎoshang hǎo, lǎoshī!", "Selamat pagi, cikgu!", "Good morning, teacher!"),
            p("我叫阿里。", "Wǒ jiào Ālǐ.", "Nama saya Ali.", "My name is Ali."),
            p("我今年六岁。", "Wǒ jīnnián liù suì.", "Saya berumur enam tahun.", "I am six years old."),
            p("我是学生。", "Wǒ shì xuésheng.", "Saya seorang murid.", "I am a student."),
            p("我是马来西亚人。", "Wǒ shì Mǎláixīyà rén.", "Saya orang Malaysia.", "I am Malaysian."),
            p("我喜欢唱歌。", "Wǒ xǐhuan chàng gē.", "Saya suka menyanyi.", "I like singing."),
            p("我喜欢画画。", "Wǒ xǐhuan huà huà.", "Saya suka melukis.", "I like drawing."),
            p("这是我的朋友。", "Zhè shì wǒ de péngyou.", "Ini kawan saya.", "This is my friend."),
            p("我爱我的家。", "Wǒ ài wǒ de jiā.", "Saya sayang keluarga saya.", "I love my family."),
            p("很高兴认识你。", "Hěn gāoxìng rènshi nǐ.", "Gembira berkenalan dengan awak.", "Nice to meet you."),
            p("今天天气很好。", "Jīntiān tiānqì hěn hǎo.", "Cuaca hari ini sangat baik.", "The weather is lovely today."),
            p("请坐。", "Qǐng zuò.", "Sila duduk.", "Please sit down."),
            p("欢迎大家！", "Huānyíng dàjiā!", "Selamat datang semua!", "Welcome, everyone!"),
            p("谢谢大家！", "Xièxie dàjiā!", "Terima kasih semua!", "Thank you, everyone!"),
            p("再见，朋友们！", "Zàijiàn, péngyoumen!", "Selamat tinggal, kawan-kawan!", "Goodbye, friends!"),
    };

    // ---------------------------------------------------------------- level 3: conversations

    static final Talk[] TALKS = {
            new Talk("Berkenalan", "🤝",
                    new Turn(p("你好！", "Nǐ hǎo!", "Helo!", "Hello!"),
                            p("你好！", "Nǐ hǎo!", "Helo!", "Hello!"),
                            p("再见！", "Zàijiàn!", "Selamat tinggal!", "Goodbye!")),
                    new Turn(p("你叫什么名字？", "Nǐ jiào shénme míngzi?", "Siapa nama awak?", "What is your name?"),
                            p("我叫阿里。", "Wǒ jiào Ālǐ.", "Nama saya Ali.", "My name is Ali."),
                            p("我六岁。", "Wǒ liù suì.", "Saya enam tahun.", "I am six.")),
                    new Turn(p("你几岁？", "Nǐ jǐ suì?", "Berapa umur awak?", "How old are you?"),
                            p("我六岁。", "Wǒ liù suì.", "Saya enam tahun.", "I am six."),
                            p("我是老师。", "Wǒ shì lǎoshī.", "Saya cikgu.", "I am a teacher.")),
                    new Turn(p("很高兴认识你！", "Hěn gāoxìng rènshi nǐ!", "Gembira berkenalan dengan awak!", "Nice to meet you!"),
                            p("我也很高兴认识你！", "Wǒ yě hěn gāoxìng rènshi nǐ!", "Saya pun gembira berkenalan dengan awak!", "Nice to meet you too!"),
                            p("我喜欢猫。", "Wǒ xǐhuan māo.", "Saya suka kucing.", "I like cats."))),
            new Talk("Di Sekolah", "🏫",
                    new Turn(p("早上好！", "Zǎoshang hǎo!", "Selamat pagi!", "Good morning!"),
                            p("早上好，老师！", "Zǎoshang hǎo, lǎoshī!", "Selamat pagi, cikgu!", "Good morning, teacher!"),
                            p("晚安！", "Wǎn'ān!", "Selamat malam!", "Good night!")),
                    new Turn(p("你喜欢什么？", "Nǐ xǐhuan shénme?", "Awak suka apa?", "What do you like?"),
                            p("我喜欢画画。", "Wǒ xǐhuan huà huà.", "Saya suka melukis.", "I like drawing."),
                            p("我叫阿里。", "Wǒ jiào Ālǐ.", "Nama saya Ali.", "My name is Ali.")),
                    new Turn(p("你最好的朋友是谁？", "Nǐ zuì hǎo de péngyou shì shéi?", "Siapa kawan baik awak?", "Who is your best friend?"),
                            p("我最好的朋友是小美。", "Wǒ zuì hǎo de péngyou shì Xiǎoměi.", "Kawan baik saya ialah Xiao Mei.", "My best friend is Xiao Mei."),
                            p("我爱我的家。", "Wǒ ài wǒ de jiā.", "Saya sayang keluarga saya.", "I love my family.")),
                    new Turn(p("真棒！", "Zhēn bàng!", "Hebatnya!", "Great!"),
                            p("谢谢老师！", "Xièxie lǎoshī!", "Terima kasih, cikgu!", "Thank you, teacher!"),
                            p("请坐。", "Qǐng zuò.", "Sila duduk.", "Please sit down."))),
            new Talk("Keluarga", "👨‍👩‍👧",
                    new Turn(p("你家有几个人？", "Nǐ jiā yǒu jǐ gè rén?", "Berapa orang dalam keluarga awak?", "How many people are in your family?"),
                            p("我家有四个人。", "Wǒ jiā yǒu sì gè rén.", "Keluarga saya ada empat orang.", "There are four people in my family."),
                            p("我四岁。", "Wǒ sì suì.", "Saya empat tahun.", "I am four.")),
                    new Turn(p("他们是谁？", "Tāmen shì shéi?", "Siapa mereka?", "Who are they?"),
                            p("爸爸、妈妈、姐姐和我。", "Bàba, māma, jiějie hé wǒ.", "Ayah, ibu, kakak dan saya.", "Dad, Mum, my big sister and me."),
                            p("我喜欢唱歌。", "Wǒ xǐhuan chàng gē.", "Saya suka menyanyi.", "I like singing.")),
                    new Turn(p("你爱你的家吗？", "Nǐ ài nǐ de jiā ma?", "Awak sayang keluarga awak?", "Do you love your family?"),
                            p("我很爱我的家！", "Wǒ hěn ài wǒ de jiā!", "Saya sangat sayang keluarga saya!", "I love my family very much!"),
                            p("今天天气很好。", "Jīntiān tiānqì hěn hǎo.", "Cuaca hari ini sangat baik.", "The weather is lovely today."))),
            new Talk("Di Majlis", "🎉",
                    new Turn(p("欢迎你来！", "Huānyíng nǐ lái!", "Selamat datang!", "Welcome!"),
                            p("谢谢！", "Xièxie!", "Terima kasih!", "Thank you!"),
                            p("再见！", "Zàijiàn!", "Selamat tinggal!", "Goodbye!")),
                    new Turn(p("你可以向大家介绍自己吗？", "Nǐ kěyǐ xiàng dàjiā jièshào zìjǐ ma?", "Boleh awak perkenalkan diri kepada semua?", "Can you introduce yourself to everyone?"),
                            p("可以！大家好，我叫阿里。", "Kěyǐ! Dàjiā hǎo, wǒ jiào Ālǐ.", "Boleh! Helo semua, nama saya Ali.", "Sure! Hello everyone, my name is Ali."),
                            p("我不知道。", "Wǒ bù zhīdào.", "Saya tak tahu.", "I don't know.")),
                    new Turn(p("你是哪里人？", "Nǐ shì nǎlǐ rén?", "Awak orang mana?", "Where are you from?"),
                            p("我是马来西亚人。", "Wǒ shì Mǎláixīyà rén.", "Saya orang Malaysia.", "I am Malaysian."),
                            p("我喜欢画画。", "Wǒ xǐhuan huà huà.", "Saya suka melukis.", "I like drawing.")),
                    new Turn(p("说得真好！", "Shuō de zhēn hǎo!", "Pandainya awak bercakap!", "You spoke so well!"),
                            p("谢谢大家！", "Xièxie dàjiā!", "Terima kasih semua!", "Thank you, everyone!"),
                            p("早上好！", "Zǎoshang hǎo!", "Selamat pagi!", "Good morning!"))),
    };

    // ---------------------------------------------------------------- level 4: speeches

    static final Speech[] SPEECHES = {
            new Speech("Perkenalan Diri · 自我介绍", "🙋",
                    p("大家好！", "Dàjiā hǎo!", "Helo semua!", "Hello, everyone!"),
                    p("我叫阿里。", "Wǒ jiào Ālǐ.", "Nama saya Ali.", "My name is Ali."),
                    p("我今年六岁。", "Wǒ jīnnián liù suì.", "Saya berumur enam tahun.", "I am six years old."),
                    p("我是一年级的学生。", "Wǒ shì yī niánjí de xuésheng.", "Saya murid tahun satu.", "I am a Year One pupil."),
                    p("我喜欢画画和唱歌。", "Wǒ xǐhuan huà huà hé chàng gē.", "Saya suka melukis dan menyanyi.", "I like drawing and singing."),
                    p("很高兴认识大家。", "Hěn gāoxìng rènshi dàjiā.", "Gembira berkenalan dengan semua.", "I am happy to meet you all."),
                    p("谢谢大家！", "Xièxie dàjiā!", "Terima kasih semua!", "Thank you, everyone!")),
            new Speech("Keluarga Saya · 我的家", "🏠",
                    p("各位老师，各位同学，大家好！", "Gèwèi lǎoshī, gèwèi tóngxué, dàjiā hǎo!", "Para guru dan kawan-kawan, helo semua!", "Teachers and classmates, hello everyone!"),
                    p("今天我要介绍我的家。", "Jīntiān wǒ yào jièshào wǒ de jiā.", "Hari ini saya mahu memperkenalkan keluarga saya.", "Today I will tell you about my family."),
                    p("我家有四个人。", "Wǒ jiā yǒu sì gè rén.", "Keluarga saya ada empat orang.", "There are four people in my family."),
                    p("爸爸、妈妈、姐姐和我。", "Bàba, māma, jiějie hé wǒ.", "Ayah, ibu, kakak dan saya.", "Dad, Mum, my big sister and me."),
                    p("我的爸爸很高。", "Wǒ de bàba hěn gāo.", "Ayah saya sangat tinggi.", "My dad is very tall."),
                    p("我的妈妈很漂亮。", "Wǒ de māma hěn piàoliang.", "Ibu saya sangat cantik.", "My mum is very pretty."),
                    p("我爱我的家。", "Wǒ ài wǒ de jiā.", "Saya sayang keluarga saya.", "I love my family."),
                    p("谢谢大家！", "Xièxie dàjiā!", "Terima kasih semua!", "Thank you, everyone!")),
            new Speech("Sekolah Saya · 我的学校", "🏫",
                    p("大家好！", "Dàjiā hǎo!", "Helo semua!", "Hello, everyone!"),
                    p("我的学校很大，也很漂亮。", "Wǒ de xuéxiào hěn dà, yě hěn piàoliang.", "Sekolah saya besar dan cantik.", "My school is big and beautiful."),
                    p("我的老师很好。", "Wǒ de lǎoshī hěn hǎo.", "Cikgu saya sangat baik.", "My teacher is very kind."),
                    p("我有很多好朋友。", "Wǒ yǒu hěn duō hǎo péngyou.", "Saya ada ramai kawan baik.", "I have many good friends."),
                    p("我每天都很开心。", "Wǒ měitiān dōu hěn kāixīn.", "Saya gembira setiap hari.", "I am happy every day."),
                    p("我爱我的学校！", "Wǒ ài wǒ de xuéxiào!", "Saya sayang sekolah saya!", "I love my school!"),
                    p("谢谢！", "Xièxie!", "Terima kasih!", "Thank you!")),
            new Speech("Cita-cita Saya · 我的梦想", "🌟",
                    p("各位老师，各位同学，大家好！", "Gèwèi lǎoshī, gèwèi tóngxué, dàjiā hǎo!", "Para guru dan kawan-kawan, helo semua!", "Teachers and classmates, hello everyone!"),
                    p("我的梦想是当一名医生。", "Wǒ de mèngxiǎng shì dāng yì míng yīshēng.", "Cita-cita saya ialah menjadi doktor.", "My dream is to be a doctor."),
                    p("因为我想帮助别人。", "Yīnwèi wǒ xiǎng bāngzhù biérén.", "Kerana saya mahu membantu orang lain.", "Because I want to help other people."),
                    p("我会努力学习。", "Wǒ huì nǔlì xuéxí.", "Saya akan belajar bersungguh-sungguh.", "I will study hard."),
                    p("我相信我可以做到！", "Wǒ xiāngxìn wǒ kěyǐ zuò dào!", "Saya yakin saya boleh!", "I believe I can do it!"),
                    p("谢谢大家！", "Xièxie dàjiā!", "Terima kasih semua!", "Thank you, everyone!")),
    };

    private Chinese() {}
}
