package com.belajarceria.kids;

import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * The Chinese games' 💡 Hint button: tapped, every word tile shows its English meaning under the characters
 * (tùzi · Rabbit); tapped again, only the pinyin. Remembered from round to round while the game is open.
 */
final class EnglishHint {
    private boolean on;
    private final ArrayList<TextView> views = new ArrayList<>();
    private final ArrayList<Content.Item> items = new ArrayList<>();
    private TextView button;

    /** Adds the button to the screen's top bar. */
    void addButton(Screen screen, LinearLayout bar) {
        button = ZhKit.button(screen.getContext(), "💡  Hint", new int[]{0xFFFFB300, 0xFFFF8F00}, this::toggle);
        button.setContentDescription("hint");
        screen.addRight(bar, button, new LinearLayout.LayoutParams(Ui.dp(170), Ui.dp(50)));
    }

    /** A new round: forget the old tiles. */
    void clear() {
        views.clear();
        items.clear();
    }

    /** Shows the item's reading on t, now and whenever the hint is switched. */
    void track(TextView t, Content.Item it) {
        views.add(t);
        items.add(it);
        t.setText(text(it));
    }

    private String text(Content.Item it) {
        return on && it.en != null ? it.word + "  ·  " + it.en : it.word;
    }

    private void toggle() {
        on = !on;
        MainActivity.log("zh hint " + (on ? "on" : "off"));
        button.setText(on ? "💡  English ✓" : "💡  Hint");
        Ui.jelly(button, 1.4f);
        for (int i = 0; i < views.size(); i++) {
            views.get(i).setText(text(items.get(i)));
            if (on) Ui.jelly(views.get(i), 1f);
        }
    }
}
