package com.gathin.tvhome;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

/**
 * Where is this TV? States on the left, the state's JAKIM zones on the right (each with its main town and
 * districts). OK on a zone keeps it: Waktu Solat, the weather line and the Weather app follow it. Back
 * leaves everything as it was. Opened from the Wallpaper menu, and once by itself on a first start when
 * the theme names no zone.
 */
public class LocationActivity extends Activity {
    private float density;
    private Typeface outfit;
    private LinearLayout states, zones;
    private ScrollView zoneScroll;
    private TextView hint;
    private String current;
    private String currentTown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        density = getResources().getDisplayMetrics().density;
        outfit = getResources().getFont(R.font.inter);
        Location.Zone now = Location.chosen(this);
        if (now == null) now = Location.byCode(getIntent().getStringExtra("zone"));
        current = now == null ? null : now.code;
        currentTown = now == null ? null : now.town;

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(64), dp(40), dp(64), dp(28));
        page.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xFF1E2A3A, 0xFF0A0E14}));
        String lang = AppLanguage.getLanguage(this);
        String title = AppLanguage.strLocation(this);
        String subtitle;
        String hintText;
        switch (lang) {
            case AppLanguage.LANG_EN:
                subtitle = "Where is this TV? Prayer times follow your zone, and the weather follows its main town.";
                hintText = "OK: choose  ·  Back: leave it as it is";
                break;
            case AppLanguage.LANG_ID:
                subtitle = "Di mana posisi TV ini? Waktu sholat mengikuti zona, dan cuaca mengikuti kota utama.";
                hintText = "OK: pilih  ·  Kembali: batalkan";
                break;
            case AppLanguage.LANG_MS:
            default:
                subtitle = "Di manakah lokasi TV ini? Waktu Solat mengikut zon, dan cuaca mengikut bandar utama.";
                hintText = "OK: pilih  ·  Kembali: kekalkan tetapan";
                break;
        }

        page.addView(text(title, 34, 0xFFFFFFFF, 500));
        TextView sub = text(subtitle, 15, 0xA6FFFFFF, 400);
        sub.setPadding(0, dp(4), 0, dp(20));
        page.addView(sub);

        LinearLayout columns = new LinearLayout(this);
        columns.setOrientation(LinearLayout.HORIZONTAL);
        ScrollView stateScroll = new ScrollView(this);
        stateScroll.setVerticalScrollBarEnabled(false);
        states = new LinearLayout(this);
        states.setOrientation(LinearLayout.VERTICAL);
        stateScroll.addView(states);
        columns.addView(stateScroll, new LinearLayout.LayoutParams(dp(250), ViewGroup.LayoutParams.MATCH_PARENT));
        zoneScroll = new ScrollView(this);
        zoneScroll.setVerticalScrollBarEnabled(false);
        zones = new LinearLayout(this);
        zones.setOrientation(LinearLayout.VERTICAL);
        zoneScroll.addView(zones);
        LinearLayout.LayoutParams zlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        zlp.leftMargin = dp(24);
        columns.addView(zoneScroll, zlp);
        page.addView(columns, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        hint = text(hintText, 13, 0x80FFFFFF, 400);
        hint.setPadding(0, dp(14), 0, 0);
        page.addView(hint);
        setContentView(page);

        View focus = null;
        String nowState = now == null ? null : now.state;
        for (String s : Location.states()) {
            TextView row = text(s, 18, 0xFFFFFFFF, 500);
            row.setPadding(dp(18), dp(10), dp(18), dp(10));
            row.setFocusable(true);
            row.setClickable(true);
            row.setId(View.generateViewId());
            row.setTextColor(new ColorStateList(new int[][]{{android.R.attr.state_focused}, {android.R.attr.state_selected}, {}},
                    new int[]{0xFF000000, 0xFFFFFFFF, 0xB3FFFFFF}));
            row.setBackground(pill());
            row.setOnFocusChangeListener((v, has) -> {
                if (!has) return;
                for (int i = 0; i < states.getChildCount(); i++) states.getChildAt(i).setSelected(states.getChildAt(i) == v);
                showZones(s, v);
            });
            row.setOnClickListener(v -> {
                if (zones.getChildCount() > 0) zones.getChildAt(0).requestFocus();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(4);
            states.addView(row, lp);
            if (focus == null || s.equals(nowState)) focus = row;
        }
        if (focus != null) focus.requestFocus();
    }

    /** Fills the right column with {@code state}'s zones; Right from the state lands on the current zone, else the first. */
    private void showZones(String state, View stateRow) {
        zones.removeAllViews();
        zoneScroll.scrollTo(0, 0);
        List<Location.Zone> list = Location.inState(state);
        View land = null;
        for (Location.Zone z : list) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dp(18), dp(10), dp(18), dp(10));
            row.setFocusable(true);
            row.setClickable(true);
            row.setId(View.generateViewId());
            row.setBackground(card());
            LinearLayout top = new LinearLayout(this);
            top.setOrientation(LinearLayout.HORIZONTAL);
            top.setGravity(Gravity.CENTER_VERTICAL);
            TextView town = text(z.town, 18, 0xFFFFFFFF, 500);
            top.addView(town, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            boolean isNow = z.code.equals(current) && (currentTown == null || z.town.equals(currentTown));
            String nowPrefix = AppLanguage.strNowBadge(this);
            TextView code = text(isNow ? nowPrefix + "  ·  " + z.code : z.code, 13, isNow ? 0xFF30D158 : 0x99FFFFFF, 600);
            top.addView(code);
            row.addView(top);
            TextView districts = text(z.districts, 13, 0x99FFFFFF, 400);
            districts.setSingleLine(true);
            districts.setEllipsize(TextUtils.TruncateAt.END);
            districts.setPadding(0, dp(3), 0, 0);
            row.addView(districts);
            row.setNextFocusLeftId(stateRow.getId());
            row.setOnClickListener(v -> {
                Location.choose(this, z);
                Location.publish(this, z.code, z.town, z.state, z.lat, z.lon);
                PrayerTimes.setZone(this, z.code, z.town, z.lat, z.lon);
                AppLocationManager.Station matched = AppLocationManager.findNearestStation(z.lat, z.lon);
                if (matched != null) {
                    AppLocationManager.setStation(this, matched);
                } else {
                    AppLocationManager.setCustomLocation(this, "CUSTOM", z.town, z.state, z.town, z.lat, z.lon);
                }
                HomeWeather.forget(this);
                Azan.schedule(this, z.code);
                setResult(RESULT_OK);
                finish();
            });
            row.setOnKeyListener((v, keyCode, e) -> e.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_RIGHT);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(6);
            zones.addView(row, lp);
            if (land == null || isNow) land = row;
        }
        if (land != null) stateRow.setNextFocusRightId(land.getId());
    }

    /** A state: white when focused, a faint pill when it is the one shown on the right. */
    private StateListDrawable pill() {
        GradientDrawable on = new GradientDrawable();
        on.setColor(0xFFFFFFFF);
        on.setCornerRadius(dp(22));
        GradientDrawable sel = new GradientDrawable();
        sel.setColor(0x24FFFFFF);
        sel.setCornerRadius(dp(22));
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_focused}, on);
        s.addState(new int[]{android.R.attr.state_selected}, sel);
        s.addState(new int[]{}, new GradientDrawable());
        return s;
    }

    /** A zone: glass, with a bright ring when focused. */
    private StateListDrawable card() {
        GradientDrawable on = new GradientDrawable();
        on.setColor(0x33FFFFFF);
        on.setCornerRadius(dp(14));
        on.setStroke(dp(2), 0xFF64D2FF);
        GradientDrawable off = new GradientDrawable();
        off.setColor(0x14FFFFFF);
        off.setCornerRadius(dp(14));
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_focused}, on);
        s.addState(new int[]{}, off);
        return s;
    }

    private TextView text(String s, float sp, int color, int weight) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(outfit);
        t.setFontVariationSettings("'wght' " + weight);
        return t;
    }

    private int dp(float v) {
        return Math.round(v * density);
    }
}
