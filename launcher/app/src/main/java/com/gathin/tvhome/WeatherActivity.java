package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class WeatherActivity extends Activity {

    private WeatherBackgroundView weatherBgView;
    private RtlWeatherIconView rtlWeatherIcon;
    private TextView weatherSubtitle;
    private TextView weatherLocation;
    private View btnChangeLocation;
    private TextView weatherMainTemp;
    private TextView weatherMainCondition;
    private TextView weatherMainSub;
    private TextView feelsLikeLabel;
    private TextView humidityLabel;
    private TextView aqiLabel;

    private LinearLayout hourlyContainer;
    private LinearLayout dailyContainer;

    // Custom Tiles
    private WindCompassView windCompassView;
    private MoonPhaseView moonPhaseView;
    private SunArcView sunArcView;
    private RadarMapView radarMapView;
    private PressureGaugeView pressureGaugeView;

    private TextView sunsetText;
    private TextView uvValText;
    private TextView uvDescText;
    private TextView feelsLikeVal;

    private TextView aqiValue;
    private TextView aqiStatus;
    private AqiSpectrumBarView aqiBar;
    private TextView aqiStation;

    private TextView humidityValText;
    private TextView weatherFooter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        weatherBgView = findViewById(R.id.weatherBgView);
        rtlWeatherIcon = findViewById(R.id.rtlWeatherIcon);
        weatherSubtitle = findViewById(R.id.weatherSubtitle);
        weatherLocation = findViewById(R.id.weatherLocation);
        btnChangeLocation = findViewById(R.id.btnChangeLocation);
        weatherMainTemp = findViewById(R.id.weatherMainTemp);
        weatherMainCondition = findViewById(R.id.weatherMainCondition);
        weatherMainSub = findViewById(R.id.weatherMainSub);
        feelsLikeLabel = findViewById(R.id.feelsLikeLabel);
        humidityLabel = findViewById(R.id.humidityLabel);
        aqiLabel = findViewById(R.id.aqiLabel);

        hourlyContainer = findViewById(R.id.hourlyContainer);
        dailyContainer = findViewById(R.id.dailyContainer);

        windCompassView = findViewById(R.id.windCompassView);
        moonPhaseView = findViewById(R.id.moonPhaseView);
        sunArcView = findViewById(R.id.sunArcView);
        radarMapView = findViewById(R.id.radarMapView);
        pressureGaugeView = findViewById(R.id.pressureGaugeView);

        sunsetText = findViewById(R.id.sunsetText);
        uvValText = findViewById(R.id.uvValText);
        uvDescText = findViewById(R.id.uvDescText);
        feelsLikeVal = findViewById(R.id.feelsLikeVal);

        aqiValue = findViewById(R.id.aqiValue);
        aqiStatus = findViewById(R.id.aqiStatus);
        aqiBar = findViewById(R.id.aqiBar);
        aqiStation = findViewById(R.id.aqiStation);

        humidityValText = findViewById(R.id.humidityValText);
        weatherFooter = findViewById(R.id.weatherFooter);

        if (btnChangeLocation != null) {
            btnChangeLocation.setVisibility(View.GONE);
        }

        populateUi();

        // Background sync to ensure freshest data
        WeatherAirQuality.syncIfDue(this, this::populateUi);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (weatherBgView != null) {
            weatherBgView.startAnimation();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (weatherBgView != null) {
            weatherBgView.stopAnimation();
        }
    }

    private void showLocationPickerDialog() {
        String activeRegion = AppLocationManager.getActiveRegion(this);
        CharSequence[] topOptions = new CharSequence[]{
                "📍 Pilih Bandar / Stesen (" + activeRegion + ")",
                "🌏 Tukar Negara / Region Asia (Default: Malaysia)",
                "✨ Tetapkan Suasana Latar Cuaca (Cerah, Hujan, Ribut, Malam)"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle("🌏 Lokasi & Suasana Cuaca");
        builder.setItems(topOptions, (dialog, which) -> {
            dialog.dismiss();
            if (which == 0) {
                showStationListDialog(activeRegion);
            } else if (which == 1) {
                showAsiaCountryDialog();
            } else {
                showWeatherAnimationPicker();
            }
        });
        builder.setNegativeButton("Tutup / Cancel", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void showWeatherAnimationPicker() {
        SharedPreferences p = getSharedPreferences("app_weather_prefs", MODE_PRIVATE);

        CharSequence[] conditions = new CharSequence[]{
                "☀️ Tetapkan Cerah / Sunny (Pantulan suria, lens flare & cahaya)",
                "🌧️ Tetapkan Hujan / Rain (Titisan hujan & percikan air)",
                "⛈️ Tetapkan Ribut Petir / Thunderstorm (Kilat menyambar & hujan)",
                "⛅ Tetapkan Mendung / Cloudy (Awan berarak di langit)",
                "🌙 Tetapkan Malam / Night (Bintang berkelip & sinaran bulan)",
                "🔄 Pulihkan kepada Cuaca Semasa Sebenar (Auto)"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle("✨ Pilihan Suasana Latar Belakang");
        builder.setItems(conditions, (dialog, which) -> {
            dialog.dismiss();
            SharedPreferences.Editor ed = p.edit();
            if (which == 0) {
                ed.putString("atmosphere_mode", "SUNNY").apply();
                Toast.makeText(this, "Suasana ditetapkan kepada: Cerah (Sunny)", Toast.LENGTH_SHORT).show();
            } else if (which == 1) {
                ed.putString("atmosphere_mode", "RAIN").apply();
                Toast.makeText(this, "Suasana ditetapkan kepada: Hujan (Rain)", Toast.LENGTH_SHORT).show();
            } else if (which == 2) {
                ed.putString("atmosphere_mode", "THUNDERSTORM").apply();
                Toast.makeText(this, "Suasana ditetapkan kepada: Ribut Petir", Toast.LENGTH_SHORT).show();
            } else if (which == 3) {
                ed.putString("atmosphere_mode", "CLOUDY").apply();
                Toast.makeText(this, "Suasana ditetapkan kepada: Mendung (Cloudy)", Toast.LENGTH_SHORT).show();
            } else if (which == 4) {
                ed.putString("atmosphere_mode", "NIGHT").apply();
                Toast.makeText(this, "Suasana ditetapkan kepada: Malam (Night)", Toast.LENGTH_SHORT).show();
            } else {
                ed.putString("atmosphere_mode", "AUTO").apply();
                Toast.makeText(this, "Pulih kepada data cuaca sebenar (Auto)", Toast.LENGTH_SHORT).show();
            }
            populateUi();
        });
        builder.setNegativeButton("Kembali", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void showAsiaCountryDialog() {
        final List<String> regions = AppLocationManager.getAvailableRegions();
        String activeRegion = AppLocationManager.getActiveRegion(this);
        int selectedIndex = 0;
        CharSequence[] items = new CharSequence[regions.size()];
        for (int i = 0; i < regions.size(); i++) {
            String r = regions.get(i);
            items[i] = r;
            if (r.equalsIgnoreCase(activeRegion)) {
                selectedIndex = i;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle("🌏 Pilih Negara / Region Asia");
        builder.setSingleChoiceItems(items, selectedIndex, (dialog, which) -> {
            dialog.dismiss();
            String chosenRegion = regions.get(which);
            AppLocationManager.setActiveRegion(WeatherActivity.this, chosenRegion);
            Toast.makeText(WeatherActivity.this, "Region dipilih: " + chosenRegion, Toast.LENGTH_SHORT).show();
            showStationListDialog(chosenRegion);
        });
        builder.setNegativeButton("Kembali", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void showStationListDialog(String region) {
        final List<AppLocationManager.Station> stations = AppLocationManager.getStationsForRegion(region);
        if (stations.isEmpty()) {
            Toast.makeText(this, "Tiada stesen dijumpai bagi " + region, Toast.LENGTH_SHORT).show();
            return;
        }

        String currentId = AppLocationManager.getStationId(this);
        int selectedIndex = 0;
        CharSequence[] items = new CharSequence[stations.size()];
        for (int i = 0; i < stations.size(); i++) {
            AppLocationManager.Station s = stations.get(i);
            items[i] = s.getDisplayName();
            if (s.id.equalsIgnoreCase(currentId)) {
                selectedIndex = i;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle("📍 Pilih Bandar / Stesen (" + region + ")");
        builder.setSingleChoiceItems(items, selectedIndex, (dialog, which) -> {
            dialog.dismiss();
            AppLocationManager.Station picked = stations.get(which);
            AppLocationManager.setStation(WeatherActivity.this, picked);
            Toast.makeText(WeatherActivity.this, "Menyelaras cuaca " + picked.name + "...", Toast.LENGTH_SHORT).show();
            WeatherAirQuality.sync(WeatherActivity.this, WeatherActivity.this::populateUi);
        });
        builder.setNegativeButton("Kembali", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void populateUi() {
        WeatherAirQuality.FullWeather w = WeatherAirQuality.getFullWeather(this);

        // 1. Determine Condition and update animated background
        WeatherBackgroundView.ConditionType conditionType = determineCondition(w);
        if (weatherBgView != null) {
            weatherBgView.setCondition(conditionType);
        }

        // 2. Header
        Location.Zone chosen = Location.chosen(this);
        String neatLoc = (chosen != null) ? chosen.town : w.location;
        if (neatLoc == null || neatLoc.isEmpty()) neatLoc = "Putrajaya";
        neatLoc = neatLoc.substring(0, 1).toUpperCase(Locale.ROOT) + neatLoc.substring(1);
        weatherLocation.setText(neatLoc);

        boolean isMalay = AppLanguage.LANG_MS.equals(AppLanguage.getLanguage(this))
                || AppLanguage.LANG_ID.equals(AppLanguage.getLanguage(this))
                || Locale.getDefault().getLanguage().startsWith("ms")
                || Locale.getDefault().getLanguage().startsWith("in")
                || Locale.getDefault().getLanguage().startsWith("id");
        boolean isPortuguese = Locale.getDefault().getLanguage().startsWith("pt");

        if (weatherSubtitle != null) {
            weatherSubtitle.setText(isMalay ? "Sekarang di" : (isPortuguese ? "Agora em" : "Now in"));
        }

        weatherMainTemp.setText(w.temp + "°C");

        if (weatherMainCondition != null) {
            if (conditionType == WeatherBackgroundView.ConditionType.SUNNY) {
                weatherMainCondition.setText(isMalay ? "Cerah" : (isPortuguese ? "Céu Limpo" : "Sunny"));
            } else if (conditionType == WeatherBackgroundView.ConditionType.NIGHT) {
                weatherMainCondition.setText(isMalay ? "Malam Tenang" : (isPortuguese ? "Céu Limpo" : "Clear Night"));
            } else if (conditionType == WeatherBackgroundView.ConditionType.THUNDERSTORM) {
                weatherMainCondition.setText(isMalay ? "Ribut Petir" : (isPortuguese ? "Tempestade" : "Thunderstorm"));
            } else if (conditionType == WeatherBackgroundView.ConditionType.RAIN) {
                weatherMainCondition.setText(isMalay ? "Hujan" : (isPortuguese ? "Chuva" : "Rain"));
            } else if (conditionType == WeatherBackgroundView.ConditionType.DRIZZLE) {
                weatherMainCondition.setText(isMalay ? "Gerimis" : (isPortuguese ? "Garoa" : "Drizzle"));
            } else {
                weatherMainCondition.setText(isMalay ? "Sebahagian Berawan" : (isPortuguese ? "Alguma nebulosidade" : "Mostly cloudy"));
            }
        }

        if (rtlWeatherIcon != null) {
            boolean isNight = (conditionType == WeatherBackgroundView.ConditionType.NIGHT);
            rtlWeatherIcon.setCondition(w.condition, w.icon, isNight);
        }

        if (weatherMainSub != null) {
            weatherMainSub.setText(String.format(Locale.ROOT, "Max: %d°C  ·  Min: %d°C", w.highTemp, w.lowTemp));
        }

        // 3. Core 3 Metrics Row (SENSAÇÃO / UMIDADE / PRESSÃO or IPU)
        if (feelsLikeLabel != null) {
            feelsLikeLabel.setText(isMalay ? "SUHU RASA" : (isPortuguese ? "SENSAÇÃO" : "FEELS LIKE"));
        }
        if (feelsLikeVal != null) {
            feelsLikeVal.setText(w.apparentTemp + "°C");
        }

        if (humidityLabel != null) {
            humidityLabel.setText(isMalay ? "KELEMBAPAN" : (isPortuguese ? "UMIDADE" : "HUMIDITY"));
        }
        if (humidityValText != null) {
            humidityValText.setText(w.humidity + "%");
        }

        if (aqiLabel != null) {
            aqiLabel.setText(isMalay ? "IPU UDARA" : (isPortuguese ? "PRESSÃO" : "PRESSURE"));
        }
        if (aqiValue != null) {
            if (isMalay) {
                aqiValue.setText(w.aqi + " " + w.aqiStatus);
            } else {
                aqiValue.setText((w.pressure > 0 ? w.pressure : 1010) + "hPa");
            }
        }

        // 4. Horizontal Daily Forecast Cards (RTL / São Paulo TV UI design)
        if (dailyContainer != null) {
            dailyContainer.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(this);
            for (int i = 0; i < w.daily.size(); i++) {
                WeatherAirQuality.DailyItem item = w.daily.get(i);
                View v = inflater.inflate(R.layout.item_weather_forecast_card, dailyContainer, false);
                TextView dayView = v.findViewById(R.id.dailyDay);
                ImageView iconView = v.findViewById(R.id.dailyIcon);
                TextView rainView = v.findViewById(R.id.dailyRain);
                TextView minTempView = v.findViewById(R.id.dailyMinTemp);
                TextView maxTempView = v.findViewById(R.id.dailyMaxTemp);

                if (dayView != null) dayView.setText(item.day);
                if (iconView != null) iconView.setImageResource(getIconDrawable(item.icon, "12:00"));
                if (minTempView != null) minTempView.setText(item.minTemp + "°C");
                if (maxTempView != null) maxTempView.setText(item.maxTemp + "°C");

                if (rainView != null) {
                    if (item.rainChance > 0) {
                        rainView.setVisibility(View.VISIBLE);
                        rainView.setText(item.rainChance + "%");
                    } else {
                        rainView.setVisibility(View.GONE);
                    }
                }

                setupFocusAnimation(v, 1.06f);
                dailyContainer.addView(v);

                if (i == 0) {
                    v.requestFocus();
                }
            }
        }

        // 5. Populate Modular Metric Tiles
        if (windCompassView != null) {
            windCompassView.setWind(w.windSpeed, w.windSpeed + 10, 180f);
        }

        if (sunArcView != null) {
            sunArcView.setTimes(w.sunrise, w.sunset);
        }
        if (sunsetText != null) {
            sunsetText.setText(w.sunset != null ? w.sunset : "6:57 PM");
        }

        if (uvValText != null) {
            uvValText.setText(String.valueOf(w.uvIndex));
        }
        if (uvDescText != null) {
            if (w.uvIndex <= 2) uvDescText.setText("Low");
            else if (w.uvIndex <= 5) uvDescText.setText("Moderate");
            else if (w.uvIndex <= 7) uvDescText.setText("High");
            else uvDescText.setText("Very High");
        }

        if (aqiStatus != null) aqiStatus.setText(w.aqiStatus);
        if (aqiBar != null) aqiBar.setAqi(w.aqi);
        if (aqiStation != null) {
            if (w.stationPlace != null && !w.stationPlace.isEmpty()) {
                aqiStation.setText(w.stationPlace);
            } else {
                aqiStation.setText("SK Putrajaya Presint 18");
            }
        }

        // Radar Map
        if (radarMapView != null) {
            radarMapView.setDetails(w.temp, neatLoc);
        }

        // Humidity & Pressure
        if (humidityValText != null) {
            humidityValText.setText(w.humidity + "%");
        }
        if (pressureGaugeView != null) {
            pressureGaugeView.setPressure(1014);
        }

        // 6. Footer
        if (weatherFooter != null) {
            weatherFooter.setText(String.format(Locale.ROOT,
                    "Updated %s  •  Jabatan Alam Sekitar (APIMS)  •  Open-Meteo Atmospheric Model",
                    w.updatedTime));
        }
    }

    private WeatherBackgroundView.ConditionType determineCondition(WeatherAirQuality.FullWeather w) {
        SharedPreferences p = getSharedPreferences("app_weather_prefs", MODE_PRIVATE);
        String mode = p.getString("atmosphere_mode", "AUTO");
        if ("SUNNY".equalsIgnoreCase(mode)) return WeatherBackgroundView.ConditionType.SUNNY;
        if ("RAIN".equalsIgnoreCase(mode)) return WeatherBackgroundView.ConditionType.RAIN;
        if ("THUNDERSTORM".equalsIgnoreCase(mode)) return WeatherBackgroundView.ConditionType.THUNDERSTORM;
        if ("CLOUDY".equalsIgnoreCase(mode)) return WeatherBackgroundView.ConditionType.CLOUDY;
        if ("NIGHT".equalsIgnoreCase(mode)) return WeatherBackgroundView.ConditionType.NIGHT;

        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);
        boolean isNight = (hour < 7 || hour >= 19);

        String cond = (w.condition != null) ? w.condition.toLowerCase(Locale.ROOT) : "";
        String icon = (w.icon != null) ? w.icon : "";

        // Thunderstorm codes: 95, 96, 99
        if (cond.contains("thunder") || cond.contains("storm") || cond.contains("ribut") || icon.contains("⛈")) {
            return WeatherBackgroundView.ConditionType.THUNDERSTORM;
        }

        // Heavy rain or normal rain (codes 61, 63, 65, 81, 82)
        if (cond.contains("heavy") || cond.contains("lebat") || 
            (cond.contains("hujan") && !cond.contains("renyai") && !cond.contains("gerimis")) || 
            (cond.contains("rain") && !cond.contains("drizzle") && !cond.contains("light"))) {
            return WeatherBackgroundView.ConditionType.RAIN;
        }

        // Drizzle / Gerimis / Renyai (codes 51-57, 80)
        if (cond.contains("drizzle") || cond.contains("gerimis") || cond.contains("renyai") || cond.contains("light rain") || icon.contains("🌦") || icon.contains("🌧")) {
            return WeatherBackgroundView.ConditionType.DRIZZLE;
        }

        // Night
        if (isNight) {
            return WeatherBackgroundView.ConditionType.NIGHT;
        }

        // Cloudy / Mendung (code 3)
        if (cond.contains("cloud") || cond.contains("mendung") || cond.contains("overcast") || icon.contains("☁")) {
            return WeatherBackgroundView.ConditionType.CLOUDY;
        }

        // Daytime: clear or mainly clear
        return WeatherBackgroundView.ConditionType.SUNNY;
    }

    private int getIconDrawable(String icon, String timeText) {
        boolean isNight = false;
        if (timeText != null) {
            String lower = timeText.trim().toLowerCase(Locale.ROOT);
            if ("now".equals(lower)) {
                int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                isNight = (h < 7 || h >= 19);
            } else {
                try {
                    boolean isPm = lower.contains("pm");
                    boolean isAm = lower.contains("am");
                    String digits = lower.replaceAll("[^0-9]", "");
                    if (!digits.isEmpty()) {
                        int hourNum = Integer.parseInt(digits);
                        int hour24;
                        if (isPm) {
                            hour24 = (hourNum == 12) ? 12 : hourNum + 12;
                        } else if (isAm) {
                            hour24 = (hourNum == 12) ? 0 : hourNum;
                        } else {
                            hour24 = hourNum;
                        }
                        isNight = (hour24 < 7 || hour24 >= 19);
                    }
                } catch (Exception ignored) {
                }
            }
        }

        if (icon == null) return isNight ? R.drawable.wx_rtl_night : R.drawable.wx_rtl_partly;
        if (icon.contains("☀")) return isNight ? R.drawable.wx_rtl_night : R.drawable.wx_rtl_sun;
        if (icon.contains("⛅")) return isNight ? R.drawable.wx_rtl_night : R.drawable.wx_rtl_partly;
        if (icon.contains("🌧")) return R.drawable.wx_rtl_rain;
        if (icon.contains("🌦")) return R.drawable.wx_rtl_drizzle;
        if (icon.contains("⛈")) return R.drawable.wx_rtl_storm;
        if (icon.contains("☁")) return R.drawable.wx_rtl_cloud;
        if (icon.contains("🌫")) return R.drawable.wx_fog;
        return isNight ? R.drawable.wx_rtl_night : R.drawable.wx_rtl_partly;
    }

    private void setupFocusAnimation(View v, float scale) {
        v.setOnFocusChangeListener((view, hasFocus) -> {
            float s = hasFocus ? scale : 1.0f;
            float elev = hasFocus ? 6f : 0f;
            view.animate().scaleX(s).scaleY(s).translationZ(elev).setDuration(140).start();
        });
    }

    private void setupCardFocus(View card) {
        if (card == null) return;
        card.setFocusable(true);
        card.setClickable(true);
        card.setOnFocusChangeListener((view, hasFocus) -> {
            float elev = hasFocus ? 6f : 0f;
            view.animate().scaleX(1.0f).scaleY(1.0f).translationZ(elev).setDuration(120).start();
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
