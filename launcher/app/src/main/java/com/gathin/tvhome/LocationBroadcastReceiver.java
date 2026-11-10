package com.gathin.tvhome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

/**
 * Handles location configuration broadcasts (e.g. from AI assistant scripts or adb):
 * adb shell am broadcast -a com.gathin.tvhome.SET_LOCATION -p com.gathin.tvhome --es query "Putrajaya"
 * adb shell am broadcast -a com.gathin.tvhome.SET_LOCATION -p com.gathin.tvhome --es station_id "CA17W"
 * adb shell am broadcast -a com.gathin.tvhome.SET_LOCATION -p com.gathin.tvhome --ef lat 2.9158 --ef lon 101.6909
 */
public class LocationBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "TvHome.LocationReceiver";
    public static final String ACTION_SET_LOCATION = "com.gathin.tvhome.SET_LOCATION";
    public static final String ACTION_SET_WEATHER = "com.gathin.tvhome.SET_WEATHER";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();

        if (ACTION_SET_WEATHER.equals(action)) {
            String cond = intent.getStringExtra("condition");
            if (cond == null) cond = intent.getStringExtra("weather");
            Log.i(TAG, "Received SET_WEATHER: condition=" + cond);
            MainActivity main = MainActivity.getInstance();
            if (main != null) {
                final String finalCond = cond;
                main.runOnUiThread(() -> {
                    main.setTestWeatherOverride(finalCond);
                    Toast.makeText(context, "🌧️ Weather Input: " + (finalCond != null ? finalCond : "Auto"), Toast.LENGTH_SHORT).show();
                });
            }
            return;
        }

        if (!ACTION_SET_LOCATION.equals(action)) return;

        String query = intent.getStringExtra("query");
        if (query == null) query = intent.getStringExtra("location");
        if (query == null) query = intent.getStringExtra("station_name");

        String stationId = intent.getStringExtra("station_id");
        if (stationId == null) stationId = intent.getStringExtra("id");

        double lat = getDoubleExtra(intent, "lat", "latitude", 0.0);
        double lon = getDoubleExtra(intent, "lon", "longitude", 0.0);

        Log.i(TAG, "Received SET_LOCATION: query=" + query + ", stationId=" + stationId + ", lat=" + lat + ", lon=" + lon);

        AppLocationManager.Station matched = null;
        if (stationId != null && !stationId.trim().isEmpty()) {
            matched = AppLocationManager.findStationByQuery(stationId);
        }

        if (matched == null && query != null && !query.trim().isEmpty()) {
            matched = AppLocationManager.findStationByQuery(query);
        }

        // A town from the Waktu Solat list (e.g. "Cyberjaya" or "Cyberjaya, Selangor") that has no air-quality
        // station of its own: pick its JAKIM zone like the Location screen does, and the nearest station.
        Location.Zone townZone = (matched == null && lat == 0.0 && lon == 0.0) ? Location.byTown(query) : null;
        if (townZone != null) {
            AppLocationManager.setStation(context, AppLocationManager.findNearestStation(townZone.lat, townZone.lon));
            Location.choose(context, townZone);
            Location.publish(context, townZone.code, townZone.town, townZone.state, townZone.lat, townZone.lon);
            PrayerTimes.setZone(context, townZone.code, townZone.town, townZone.lat, townZone.lon);
            HomeWeather.forget(context);
            Azan.schedule(context, townZone.code);
            final String name = townZone.town + " (" + townZone.code + ")";
            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(context, "📍 Lokasi TV: " + name, Toast.LENGTH_LONG).show());
            MainActivity main = MainActivity.getInstance();
            if (main != null) main.runOnUiThread(main::refreshLocationDependentData);
            return;
        }

        if (matched == null && lat != 0.0 && lon != 0.0) {
            matched = AppLocationManager.findNearestStation(lat, lon);
        }

        if (matched != null) {
            PrayerTimes.clearManualZone(context);
            AppLocationManager.setStation(context, matched);
            final String name = matched.getDisplayName();
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(context, "📍 Lokasi TV: " + name, Toast.LENGTH_LONG).show();
            });
        } else if (lat != 0.0 && lon != 0.0) {
            PrayerTimes.clearManualZone(context);
            String customName = query != null ? query : "Lokasi Tersuai";
            AppLocationManager.setCustomLocation(context, "CUSTOM", customName, customName, lat, lon);
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(context, "📍 Lokasi TV: " + customName, Toast.LENGTH_LONG).show();
            });
        } else {
            Log.w(TAG, "No matching station found for: query=" + query + ", stationId=" + stationId);
            return;
        }

        MainActivity main = MainActivity.getInstance();
        if (main != null) {
            main.runOnUiThread(main::refreshLocationDependentData);
        }
    }

    private double getDoubleExtra(Intent intent, String key1, String key2, double def) {
        if (intent.hasExtra(key1)) {
            try {
                return Double.parseDouble(intent.getStringExtra(key1));
            } catch (Exception e) {
                return intent.getDoubleExtra(key1, def);
            }
        }
        if (intent.hasExtra(key2)) {
            try {
                return Double.parseDouble(intent.getStringExtra(key2));
            } catch (Exception e) {
                return intent.getDoubleExtra(key2, def);
            }
        }
        return def;
    }
}
