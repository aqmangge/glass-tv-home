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
    private HourlyWeatherGraphView hourlyGraphView;

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
        hourlyGraphView = findViewById(R.id.hourlyGraphView);

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

        int tempColor = getConditionTempColor(conditionType, w.temp);
        weatherMainTemp.setText(w.temp + "°C");
        weatherMainTemp.setTextColor(tempColor);

        String initialCond = getConditionString(conditionType, w.condition, isMalay, isPortuguese);
        if (weatherMainCondition != null) {
            weatherMainCondition.setText(initialCond);
        }

        if (rtlWeatherIcon != null) {
            boolean isNight = (conditionType == WeatherBackgroundView.ConditionType.NIGHT);
            rtlWeatherIcon.setCondition(initialCond, w.icon, isNight);
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
            feelsLikeVal.setTextColor(getConditionTempColor(conditionType, w.apparentTemp));
        }

        if (humidityLabel != null) {
            humidityLabel.setText(isMalay ? "KELEMBAPAN" : (isPortuguese ? "UMIDADE" : "HUMIDITY"));
        }
        if (humidityValText != null) {
            humidityValText.setText(w.humidity + "%");
            humidityValText.setTextColor(0xFF38BDF8); // Water Cyan
        }

        if (aqiLabel != null) {
            aqiLabel.setText(isMalay ? "IPU UDARA" : (isPortuguese ? "PRESSÃO" : "PRESSURE"));
        }
        if (aqiValue != null) {
            if (isMalay) {
                aqiValue.setText(w.aqi + " " + w.aqiStatus);
                aqiValue.setTextColor(w.aqiColor); // Official JAS APIMS status color
            } else {
                aqiValue.setText((w.pressure > 0 ? w.pressure : 1010) + "hPa");
                aqiValue.setTextColor(tempColor);
            }
        }

        // 3.5. Middle Section: Google Weather Style Hourly Temperature Curve
        if (hourlyGraphView != null) {
            List<WeatherAirQuality.HourlyItem> hList = (!w.daily.isEmpty() && w.daily.get(0).hourly != null && !w.daily.get(0).hourly.isEmpty())
                    ? w.daily.get(0).hourly : w.hourly;
            hourlyGraphView.setData(hList, tempColor);
        }

        // 4. Horizontal Daily Forecast Cards (5 Cards matching São Paulo TV UI design)
        if (dailyContainer != null) {
            dailyContainer.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(this);
            int dayCount = Math.min(5, w.daily.size());
            for (int i = 0; i < dayCount; i++) {
                final int dayIdx = i;
                final WeatherAirQuality.DailyItem item = w.daily.get(i);
                View v = inflater.inflate(R.layout.item_weather_forecast_card, dailyContainer, false);
                TextView dayView = v.findViewById(R.id.dailyDay);
                ImageView iconView = v.findViewById(R.id.dailyIcon);
                TextView minTempView = v.findViewById(R.id.dailyMinTemp);
                TextView maxTempView = v.findViewById(R.id.dailyMaxTemp);

                if (dayView != null) dayView.setText(item.day);
                if (iconView != null) iconView.setImageResource(getIconDrawable(item.icon, false));
                if (minTempView != null) minTempView.setText(item.minTemp + "°C");
                if (maxTempView != null) maxTempView.setText(item.maxTemp + "°C");

                if (i == dayCount - 1) {
                    LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) v.getLayoutParams();
                    if (lp != null) {
                        lp.setMarginEnd(0);
                        v.setLayoutParams(lp);
                    }
                }

                setupForecastCard(v, w, item, dayIdx, isMalay, isPortuguese);
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

        // Cloudy / Mendung (code 1, 2, 3)
        if (cond.contains("cloud") || cond.contains("mendung") || cond.contains("berawan") || cond.contains("overcast") || icon.contains("☁") || icon.contains("⛅")) {
            return WeatherBackgroundView.ConditionType.CLOUDY;
        }

        // Daytime: clear or mainly clear
        return WeatherBackgroundView.ConditionType.SUNNY;
    }

    private String getConditionString(WeatherBackgroundView.ConditionType type, String rawCond, boolean isMalay, boolean isPortuguese) {
        if (type == WeatherBackgroundView.ConditionType.THUNDERSTORM) {
            return isMalay ? "Ribut Petir" : (isPortuguese ? "Tempestade" : "Thunderstorm");
        } else if (type == WeatherBackgroundView.ConditionType.RAIN) {
            return isMalay ? "Hujan" : (isPortuguese ? "Chuva" : "Rain");
        } else if (type == WeatherBackgroundView.ConditionType.DRIZZLE) {
            return isMalay ? "Gerimis" : (isPortuguese ? "Garoa" : "Drizzle");
        } else if (type == WeatherBackgroundView.ConditionType.NIGHT) {
            return isMalay ? "Malam Tenang" : (isPortuguese ? "Céu Limpo" : "Clear Night");
        } else if (type == WeatherBackgroundView.ConditionType.CLOUDY) {
            return isMalay ? "Sebahagiannya mendung" : (isPortuguese ? "Alguma nebulosidade" : "Mostly cloudy");
        } else {
            return isMalay ? "Cerah" : (isPortuguese ? "Céu Limpo" : "Sunny");
        }
    }

    private void setupForecastCard(View v, WeatherAirQuality.FullWeather w, WeatherAirQuality.DailyItem item, int dayIndex, boolean isMalay, boolean isPortuguese) {
        v.setOnFocusChangeListener((view, hasFocus) -> {
            float s = hasFocus ? 1.04f : 1.0f;
            float elev = hasFocus ? 8f : 0f;
            view.animate().scaleX(s).scaleY(s).translationZ(elev).setDuration(160).start();
            if (hasFocus) {
                updateHeroForDay(w, item, dayIndex, isMalay, isPortuguese);
            }
        });
        v.setOnClickListener(view -> updateHeroForDay(w, item, dayIndex, isMalay, isPortuguese));
    }

    private void updateHeroForDay(WeatherAirQuality.FullWeather w, WeatherAirQuality.DailyItem item, int dayIndex, boolean isMalay, boolean isPortuguese) {
        if (item == null) return;

        int temp = (dayIndex == 0) ? w.temp : item.maxTemp;
        int apparent = (dayIndex == 0) ? w.apparentTemp : (item.maxTemp + 3);
        int high = (dayIndex == 0) ? w.highTemp : item.maxTemp;
        int low = (dayIndex == 0) ? w.lowTemp : item.minTemp;

        WeatherBackgroundView.ConditionType cType;
        String conditionText;

        if (dayIndex == 0) {
            cType = determineCondition(w);
            conditionText = getConditionString(cType, w.condition, isMalay, isPortuguese);
            if (weatherSubtitle != null) {
                weatherSubtitle.setText(isMalay ? "Sekarang di" : (isPortuguese ? "Agora em" : "Now in"));
            }
        } else {
            cType = mapCodeToConditionType(item.code);
            conditionText = getConditionString(cType, null, isMalay, isPortuguese);
            if (weatherSubtitle != null) {
                weatherSubtitle.setText(item.day + (isPortuguese ? " em" : (isMalay ? " di" : " in")));
            }
        }

        int tempColor = getConditionTempColor(cType, temp);
        if (weatherMainTemp != null) {
            weatherMainTemp.setText(temp + "°C");
            weatherMainTemp.setTextColor(tempColor);
        }

        if (weatherMainCondition != null) {
            weatherMainCondition.setText(conditionText);
        }

        if (rtlWeatherIcon != null) {
            boolean isNight = (dayIndex == 0 && cType == WeatherBackgroundView.ConditionType.NIGHT);
            rtlWeatherIcon.setCondition(conditionText, item.icon, isNight);
        }

        if (weatherMainSub != null) {
            weatherMainSub.setText(String.format(Locale.ROOT, "Max: %d°C  ·  Min: %d°C", high, low));
        }

        if (feelsLikeVal != null) {
            feelsLikeVal.setText(apparent + "°C");
            feelsLikeVal.setTextColor(getConditionTempColor(cType, apparent));
        }

        if (humidityLabel != null) {
            if (dayIndex == 0) {
                humidityLabel.setText(isMalay ? "KELEMBAPAN" : (isPortuguese ? "UMIDADE" : "HUMIDITY"));
            } else {
                humidityLabel.setText(isMalay ? "KEBARANGKALIAN HUJAN" : (isPortuguese ? "CHANCE DE CHUVA" : "RAIN CHANCE"));
            }
        }

        if (humidityValText != null) {
            if (dayIndex == 0) {
                humidityValText.setText(w.humidity + "%");
            } else {
                humidityValText.setText(item.rainChance + "%");
            }
            humidityValText.setTextColor(0xFF38BDF8);
        }

        if (hourlyGraphView != null) {
            List<WeatherAirQuality.HourlyItem> hList = (item.hourly != null && !item.hourly.isEmpty()) ? item.hourly : w.hourly;
            hourlyGraphView.setData(hList, tempColor);
        }

        if (weatherBgView != null) {
            weatherBgView.setCondition(cType);
        }
    }

    private WeatherBackgroundView.ConditionType mapCodeToConditionType(int code) {
        if (code >= 95) return WeatherBackgroundView.ConditionType.THUNDERSTORM;
        if ((code >= 61 && code <= 67) || (code >= 80 && code <= 82)) return WeatherBackgroundView.ConditionType.RAIN;
        if (code >= 51 && code <= 57) return WeatherBackgroundView.ConditionType.DRIZZLE;
        if (code == 1 || code == 2 || code == 3) return WeatherBackgroundView.ConditionType.CLOUDY;
        return WeatherBackgroundView.ConditionType.SUNNY;
    }

    private int getConditionTempColor(WeatherBackgroundView.ConditionType type, int temp) {
        if (type == WeatherBackgroundView.ConditionType.THUNDERSTORM) {
            return 0xFFFDE047; // Electric Yellow (#FDE047)
        } else if (type == WeatherBackgroundView.ConditionType.RAIN || type == WeatherBackgroundView.ConditionType.DRIZZLE) {
            return 0xFF38BDF8; // Refreshing Rain Sky Blue (#38BDF8)
        } else if (type == WeatherBackgroundView.ConditionType.NIGHT) {
            return 0xFFBAE6FD; // Ice Blue Moonlight (#BAE6FD)
        } else if (type == WeatherBackgroundView.ConditionType.CLOUDY) {
            return (temp >= 28) ? 0xFFFBBF24 : 0xFFE2E8F0; // Amber Gold or Slate
        } else {
            // SUNNY / CERAH
            if (temp >= 33) {
                return 0xFFFF4545; // Coral Flame Red (#FF4545) for heatwave
            } else if (temp >= 28) {
                return 0xFFFBBF24; // Solar Amber Gold (#FBBF24) for warm sunshine
            } else {
                return 0xFFFACC15; // Bright Sunny Yellow (#FACC15)
            }
        }
    }

    private int getIconDrawable(String icon, boolean isNight) {
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

    private int getIconDrawable(String icon, String timeText) {
        boolean isNight = false;
        if (timeText != null) {
            String lower = timeText.trim().toLowerCase(Locale.ROOT);
            if ("now".equals(lower)) {
                int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                isNight = (h < 7 || h >= 19);
            } else if (lower.contains(":")) {
                try {
                    String[] parts = lower.split(":");
                    int hourNum = Integer.parseInt(parts[0].replaceAll("[^0-9]", ""));
                    if (lower.contains("pm") && hourNum < 12) hourNum += 12;
                    else if (lower.contains("am") && hourNum == 12) hourNum = 0;
                    isNight = (hourNum < 7 || hourNum >= 19);
                } catch (Exception ignored) {
                }
            }
        }
        return getIconDrawable(icon, isNight);
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
