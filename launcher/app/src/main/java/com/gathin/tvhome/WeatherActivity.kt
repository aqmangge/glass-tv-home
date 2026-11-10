package com.gathin.tvhome

import android.animation.ValueAnimator
import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class WeatherActivity : Activity() {

    // ---------- Data ----------
    data class WeatherState(
        val condition: WeatherSkyView.Condition,
        val title: String,
        val icon: String,
        val temp: Int,
        val feelsLike: Int,
        val rainChance: Int,
        val pressure: Int,
        val max: Int,
        val min: Int,
        val hourly: IntArray
    )

    data class DayForecast(
        val name: String,
        val high: Int,
        val low: Int,
        val icon: String,
        val conditionTitle: String = "Thunderstorm",
        val weatherCode: Int = 95,
        val condition: WeatherSkyView.Condition = WeatherSkyView.Condition.STORM,
        val feelsLike: Int = 31,
        val rainChance: Int = 95,
        val pressure: Int = 1006,
        val hourlyTemps: IntArray = intArrayOf(25, 25, 25, 27, 29, 29, 28, 25),
        val hourlyPrecip: IntArray = intArrayOf(95, 80, 60, 40, 20, 15, 30, 70),
        val hourlyWinds: IntArray = intArrayOf(12, 14, 15, 18, 16, 15, 12, 10)
    )

    // Values taken from the current .png. Replace with live API data.
    private var stormState = WeatherState(
        WeatherSkyView.Condition.STORM, "Thunderstorm", "⛈️",
        temp = 26, feelsLike = 31, rainChance = 95, pressure = 1006, max = 30, min = 25,
        hourly = intArrayOf(25, 25, 25, 27, 29, 29, 28, 25)
    )

    // DEMO values only (for the transition effect). Replace with live API data.
    private var sunnyState = WeatherState(
        WeatherSkyView.Condition.SUNNY, "Mostly Sunny", "🌤️",
        temp = 29, feelsLike = 33, rainChance = 20, pressure = 1010, max = 32, min = 25,
        hourly = intArrayOf(25, 25, 26, 28, 31, 32, 30, 27)
    )

    private var days = listOf(
        DayForecast(
            "Today", 32, 24, "⛈️", "Thunderstorm", 95, WeatherSkyView.Condition.STORM,
            feelsLike = 33, rainChance = 95, pressure = 1007,
            hourlyTemps = intArrayOf(25, 25, 24, 27, 29, 32, 29, 26),
            hourlyPrecip = intArrayOf(95, 85, 70, 50, 30, 20, 40, 65),
            hourlyWinds = intArrayOf(12, 14, 15, 18, 16, 15, 12, 10)
        ),
        DayForecast(
            "Wednesday", 32, 24, "⛈️", "Thunderstorm", 95, WeatherSkyView.Condition.STORM,
            feelsLike = 34, rainChance = 85, pressure = 1008,
            hourlyTemps = intArrayOf(24, 25, 26, 28, 32, 31, 29, 26),
            hourlyPrecip = intArrayOf(85, 70, 50, 30, 15, 20, 45, 60),
            hourlyWinds = intArrayOf(10, 12, 14, 16, 18, 16, 13, 11)
        ),
        DayForecast(
            "Thursday", 30, 24, "🌧️", "Showers", 61, WeatherSkyView.Condition.STORM,
            feelsLike = 30, rainChance = 90, pressure = 1005,
            hourlyTemps = intArrayOf(24, 24, 25, 27, 30, 29, 27, 25),
            hourlyPrecip = intArrayOf(90, 85, 75, 60, 40, 35, 55, 70),
            hourlyWinds = intArrayOf(14, 15, 18, 20, 18, 16, 14, 12)
        ),
        DayForecast(
            "Friday", 30, 24, "⛈️", "Thunderstorm", 95, WeatherSkyView.Condition.STORM,
            feelsLike = 31, rainChance = 80, pressure = 1007,
            hourlyTemps = intArrayOf(24, 24, 25, 26, 29, 28, 26, 25),
            hourlyPrecip = intArrayOf(80, 70, 60, 45, 30, 25, 50, 65),
            hourlyWinds = intArrayOf(11, 13, 15, 17, 16, 14, 12, 10)
        ),
        DayForecast(
            "Saturday", 30, 24, "🌤️", "Mostly Sunny", 2, WeatherSkyView.Condition.SUNNY,
            feelsLike = 33, rainChance = 25, pressure = 1010,
            hourlyTemps = intArrayOf(25, 25, 26, 28, 30, 30, 28, 26),
            hourlyPrecip = intArrayOf(25, 20, 15, 10, 10, 15, 20, 25),
            hourlyWinds = intArrayOf(8, 10, 12, 14, 15, 14, 11, 9)
        )
    )

    private var selectedDayIndex = 0
    private var activeChip = 0 // 0 = Temperature, 1 = Precipitation, 2 = Wind

    // Set false to stop the automatic storm <-> sunny demo loop
    private val autoDemo = false
    private val demoIntervalMs = 8000L

    // ---------- Views ----------
    private lateinit var sky: WeatherSkyView
    private lateinit var chart: HourlyChartView
    private lateinit var tvIcon: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvTemp: TextView
    private lateinit var tvCondition: TextView
    private lateinit var tvMaxMin: TextView
    private lateinit var tvFeels: TextView
    private lateinit var tvRain: TextView
    private lateinit var tvPressure: TextView
    private lateinit var dayRow: LinearLayout
    private lateinit var headerBlock: View

    private var chipTemp: TextView? = null
    private var chipPrecip: TextView? = null
    private var chipWind: TextView? = null

    private var current = stormState
    private var shownTemp = 0
    private val handler = Handler(Looper.getMainLooper())
    private val demoRunnable = object : Runnable {
        override fun run() {
            toggleWeather()
            handler.postDelayed(this, demoIntervalMs)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather)

        sky = findViewById(R.id.skyView)
        chart = findViewById(R.id.hourlyChart)
        tvIcon = findViewById(R.id.tvIcon)
        tvLocation = findViewById(R.id.tvLocation)
        tvTemp = findViewById(R.id.tvTemp)
        tvCondition = findViewById(R.id.tvCondition)
        tvMaxMin = findViewById(R.id.tvMaxMin)
        tvFeels = findViewById(R.id.tvFeels)
        tvRain = findViewById(R.id.tvRain)
        tvPressure = findViewById(R.id.tvPressure)
        dayRow = findViewById(R.id.dayRow)
        headerBlock = findViewById(R.id.headerBlock)

        chipTemp = findViewById(R.id.chipTemp)
        chipPrecip = findViewById(R.id.chipPrecip)
        chipWind = findViewById(R.id.chipWind)

        // Setup Header Hero Card
        headerBlock.isFocusable = true
        headerBlock.isFocusableInTouchMode = true
        headerBlock.setOnFocusChangeListener { v, hasFocus ->
            val scale = if (hasFocus) 1.04f else 1.0f
            v.animate().scaleX(scale).scaleY(scale).setDuration(160L).setInterpolator(DecelerateInterpolator()).start()
        }
        headerBlock.setOnClickListener { toggleWeather() }

        setupChips()
        buildDayCards()

        // Initial launch setup
        sky.setCondition(current.condition, animate = false)
        applyState(current, fromTemp = current.temp - 4) // count up on launch
        playEntrance()

        // Focus chipTemp by default
        chipTemp?.post { chipTemp?.requestFocus() }

        // Fetch live weather data for Putrajaya
        fetchLiveWeatherPutrajaya()
    }

    override fun onResume() {
        super.onResume()
        if (autoDemo) handler.postDelayed(demoRunnable, demoIntervalMs)
    }

    override fun onPause() {
        handler.removeCallbacks(demoRunnable)
        super.onPause()
    }

    // Remote D-Pad handling: directs click to currently focused view or toggles weather
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN &&
            (event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER || event.keyCode == KeyEvent.KEYCODE_ENTER)) {
            val focused = currentFocus
            if (focused != null && focused.id != R.id.root) {
                focused.performClick()
                return true
            } else {
                toggleWeather()
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun toggleWeather() {
        val next = if (current.condition == WeatherSkyView.Condition.STORM) sunnyState else stormState
        android.util.Log.d("WeatherTV", "toggleWeather: ${current.condition} -> ${next.condition}")
        current = next
        sky.setCondition(current.condition, animate = true, durationMs = 2500L)
        applyState(current, fromTemp = shownTemp)
    }

    private fun applyState(s: WeatherState, fromTemp: Int) {
        // Count-up temperature
        ValueAnimator.ofInt(fromTemp, s.temp).apply {
            duration = 1500L
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                shownTemp = it.animatedValue as Int
                tvTemp.text = "$shownTemp°C"
            }
            start()
        }

        // Cross-fade text
        crossFade(tvCondition, s.title)
        crossFade(tvIcon, s.icon)
        crossFade(tvMaxMin, "Max: ${s.max}°C  ·  Min: ${s.min}°C")
        crossFade(tvFeels, "${s.feelsLike}°C")
        crossFade(tvRain, "${s.rainChance}%")
        crossFade(tvPressure, "${s.pressure}hPa")

        updateChartData()
    }

    // ---------- Subtabs: Temperature, Precipitation, Wind ----------
    private fun setupChips() {
        val chips = listOf(chipTemp, chipPrecip, chipWind)
        chips.forEachIndexed { idx, chip ->
            chip?.let { c ->
                c.isFocusable = true
                c.isFocusableInTouchMode = true
                c.setOnFocusChangeListener { v, hasFocus ->
                    val scale = if (hasFocus) 1.04f else 1.0f
                    v.animate().scaleX(scale).scaleY(scale).setDuration(140L).setInterpolator(DecelerateInterpolator()).start()
                    updateChipsAppearance()
                }
                c.setOnClickListener {
                    selectMetric(idx)
                }
            }
        }
        updateChipsAppearance()
    }

    private fun selectMetric(idx: Int) {
        activeChip = idx
        updateChipsAppearance()
        updateChartData()
    }

    private fun updateChipsAppearance() {
        val chips = listOf(chipTemp, chipPrecip, chipWind)
        chips.forEachIndexed { i, chip ->
            chip?.let { c ->
                val isSelected = (i == activeChip)
                val hasFocus = c.hasFocus()
                c.setBackgroundResource(
                    when {
                        isSelected -> R.drawable.chip_selected
                        hasFocus -> R.drawable.chip_focused
                        else -> R.drawable.chip
                    }
                )
                c.setTextColor(if (isSelected || hasFocus) 0xFFFFFFFF.toInt() else 0xB3FFFFFF.toInt())
            }
        }
    }

    private fun updateChartData() {
        val day = if (selectedDayIndex in days.indices) days[selectedDayIndex] else days[0]
        when (activeChip) {
            0 -> {
                chart.unit = "°"
                chart.temps = day.hourlyTemps
                chart.labels = arrayOf("12 am", "3 am", "6 am", "9 am", "12 pm", "3 pm", "6 pm", "9 pm")
            }
            1 -> {
                chart.unit = "%"
                chart.temps = day.hourlyPrecip
                chart.labels = arrayOf("12 am", "3 am", "6 am", "9 am", "12 pm", "3 pm", "6 pm", "9 pm")
            }
            2 -> {
                chart.unit = " km/h"
                chart.temps = day.hourlyWinds
                chart.labels = arrayOf("12 am", "3 am", "6 am", "9 am", "12 pm", "3 pm", "6 pm", "9 pm")
            }
        }
        chart.animateIn(delayMs = 120L)
    }

    // ---------- 5-Day Forecast Cards ----------
    private fun buildDayCards() {
        dayRow.removeAllViews()
        val inflater = LayoutInflater.from(this)
        days.forEachIndexed { i, d ->
            val card = inflater.inflate(R.layout.item_day, dayRow, false)
            card.id = View.generateViewId()
            card.findViewById<TextView>(R.id.tvDayName).text = d.name
            card.findViewById<TextView>(R.id.tvHigh).text = "↑ ${d.high}°C"
            card.findViewById<TextView>(R.id.tvLow).text = "↓ ${d.low}°C"
            card.findViewById<TextView>(R.id.tvDayIcon).text = d.icon

            updateCardAppearance(card, i == selectedDayIndex, card.hasFocus())

            card.isFocusable = true
            card.isFocusableInTouchMode = true
            card.setOnFocusChangeListener { v, hasFocus ->
                val scale = if (hasFocus) 1.03f else 1.0f
                v.animate().scaleX(scale).scaleY(scale).setDuration(140L).setInterpolator(DecelerateInterpolator()).start()
                updateCardAppearance(card, i == selectedDayIndex, hasFocus)
            }

            card.setOnClickListener {
                selectDay(i)
            }

            val lp = card.layoutParams as LinearLayout.LayoutParams
            lp.weight = 1f
            lp.width = 0
            lp.height = LinearLayout.LayoutParams.MATCH_PARENT
            if (i > 0) lp.marginStart = dp(14f).toInt()
            card.layoutParams = lp
            dayRow.addView(card)
        }
        wireDayRowFocus()
    }

    private fun wireDayRowFocus() {
        for (i in 0 until dayRow.childCount) {
            val child = dayRow.getChildAt(i)
            child.nextFocusUpId = when {
                i <= 1 -> R.id.chipTemp
                i == 2 -> R.id.chipPrecip
                else -> R.id.chipWind
            }
            if (i > 0) child.nextFocusLeftId = dayRow.getChildAt(i - 1).id
            if (i < dayRow.childCount - 1) child.nextFocusRightId = dayRow.getChildAt(i + 1).id
        }
    }

    private fun selectDay(index: Int) {
        if (index !in days.indices) return
        selectedDayIndex = index
        val day = days[index]

        // Update card borders
        for (i in 0 until dayRow.childCount) {
            val card = dayRow.getChildAt(i)
            updateCardAppearance(card, i == selectedDayIndex, card.hasFocus())
        }

        // Animate sky condition if day's weather condition differs
        if (sky.currentCondition != day.condition) {
            sky.setCondition(day.condition, animate = true, durationMs = 2000L)
        }

        // Count-up temp
        val targetTemp = (day.high + day.low) / 2
        ValueAnimator.ofInt(shownTemp, targetTemp).apply {
            duration = 1000L
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                shownTemp = it.animatedValue as Int
                tvTemp.text = "$shownTemp°C"
            }
            start()
        }

        // Cross-fade text
        crossFade(tvCondition, day.conditionTitle)
        crossFade(tvIcon, day.icon)
        crossFade(tvMaxMin, "Max: ${day.high}°C  ·  Min: ${day.low}°C")
        crossFade(tvFeels, "${day.feelsLike}°C")
        crossFade(tvRain, "${day.rainChance}%")
        crossFade(tvPressure, "${day.pressure}hPa")

        updateChartData()
    }

    private fun updateCardAppearance(card: View, isSelected: Boolean, hasFocus: Boolean) {
        val bg = when {
            isSelected -> R.drawable.glass_card_selected
            hasFocus -> R.drawable.glass_card_focused
            else -> R.drawable.glass_card
        }
        card.setBackgroundResource(bg)
    }

    private fun crossFade(tv: TextView, newText: String) {
        if (tv.text == newText) return
        tv.animate().alpha(0f).translationY(-dp(6f)).setDuration(250L).withEndAction {
            tv.text = newText
            tv.translationY = dp(6f)
            tv.animate().alpha(1f).translationY(0f).setDuration(400L).start()
        }.start()
    }

    /** Fade + slide up, staggered, like the video intro. */
    private fun playEntrance() {
        val views = mutableListOf<View>(
            findViewById(R.id.headerBlock),
            findViewById(R.id.statsBlock),
            findViewById(R.id.hourlyCard)
        )
        for (i in 0 until dayRow.childCount) views += dayRow.getChildAt(i)

        views.forEachIndexed { i, v ->
            v.alpha = 0f
            v.translationY = dp(30f)
            v.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(150L + i * 90L)
                .setDuration(700L)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun dp(v: Float) = v * resources.displayMetrics.density

    // ---------- Live OpenWeather / Open-Meteo API for Putrajaya ----------
    private fun fetchLiveWeatherPutrajaya() {
        // Read cached data first
        try {
            val cached = WeatherAirQuality.getFullWeather(this)
            if (cached != null && cached.temp > 0) {
                mapFullWeatherToState(cached)
            }
        } catch (_: Exception) {}

        // Live API background fetch
        Thread {
            try {
                // Putrajaya: lat 2.9264, lon 101.6964
                val urlStr = "https://api.open-meteo.com/v1/forecast?latitude=2.9264&longitude=101.6964" +
                        "&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,surface_pressure,wind_speed_10m" +
                        "&hourly=temperature_2m,precipitation_probability,wind_speed_10m" +
                        "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
                        "&timezone=Asia%2FKuala_Lumpur&forecast_days=7"
                val url = URL(urlStr)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(body)
                    val curr = json.optJSONObject("current")
                    val daily = json.optJSONObject("daily")
                    val hourlyObj = json.optJSONObject("hourly")

                    if (curr != null && daily != null) {
                        val temp = Math.round(curr.optDouble("temperature_2m", 26.0)).toInt()
                        val feels = Math.round(curr.optDouble("apparent_temperature", 31.0)).toInt()
                        val code = curr.optInt("weather_code", 95)
                        val pressure = Math.round(curr.optDouble("surface_pressure", 1006.0)).toInt()
                        val rainProb = curr.optDouble("precipitation", 0.0)

                        val maxArr = daily.optJSONArray("temperature_2m_max")
                        val minArr = daily.optJSONArray("temperature_2m_min")
                        val codeArr = daily.optJSONArray("weather_code")
                        val rainArr = daily.optJSONArray("precipitation_probability_max")

                        val hTemps = hourlyObj?.optJSONArray("temperature_2m")
                        val hRains = hourlyObj?.optJSONArray("precipitation_probability")
                        val hWinds = hourlyObj?.optJSONArray("wind_speed_10m")

                        val sdf = SimpleDateFormat("EEEE", Locale.ENGLISH)
                        val cal = Calendar.getInstance()

                        val newDays = mutableListOf<DayForecast>()
                        val count = Math.min(5, maxArr?.length() ?: 0)

                        for (d in 0 until count) {
                            val dName = if (d == 0) {
                                "Today"
                            } else {
                                cal.add(Calendar.DAY_OF_YEAR, 1)
                                sdf.format(cal.time)
                            }

                            val dMax = Math.round(maxArr?.optDouble(d, 30.0) ?: 30.0).toInt()
                            val dMin = Math.round(minArr?.optDouble(d, 25.0) ?: 25.0).toInt()
                            val dCode = codeArr?.optInt(d, 95) ?: 95
                            val dRain = Math.round(rainArr?.optDouble(d, 60.0) ?: 60.0).toInt()

                            val dCondition = mapCondition(dCode)
                            val dTitle = if (dCondition == WeatherSkyView.Condition.STORM) "Thunderstorm" else "Mostly Sunny"
                            val dIcon = if (dCondition == WeatherSkyView.Condition.STORM) "⛈️" else "🌤️"

                            // Sample 8 intervals across this day's 24 hours
                            val hT = IntArray(8) { i ->
                                Math.round(hTemps?.optDouble(d * 24 + i * 3, (dMax + dMin) / 2.0) ?: 26.0).toInt()
                            }
                            val hR = IntArray(8) { i ->
                                Math.round(hRains?.optDouble(d * 24 + i * 3, dRain.toDouble()) ?: 40.0).toInt()
                            }
                            val hW = IntArray(8) { i ->
                                Math.round(hWinds?.optDouble(d * 24 + i * 3, 12.0) ?: 12.0).toInt()
                            }

                            newDays.add(
                                DayForecast(
                                    name = dName,
                                    high = dMax,
                                    low = dMin,
                                    icon = dIcon,
                                    conditionTitle = dTitle,
                                    weatherCode = dCode,
                                    condition = dCondition,
                                    feelsLike = dMax + 2,
                                    rainChance = dRain,
                                    pressure = pressure,
                                    hourlyTemps = hT,
                                    hourlyPrecip = hR,
                                    hourlyWinds = hW
                                )
                            )
                        }

                        val isStorm = (code >= 95 || (code in 51..67) || (code in 80..82))
                        val todayState = if (newDays.isNotEmpty()) {
                            val td = newDays[0]
                            WeatherState(
                                condition = if (isStorm) WeatherSkyView.Condition.STORM else WeatherSkyView.Condition.SUNNY,
                                title = if (isStorm) "Thunderstorm" else "Mostly Sunny",
                                icon = if (isStorm) "⛈️" else "🌤️",
                                temp = temp,
                                feelsLike = feels,
                                rainChance = if (rainProb > 0.1) 95 else td.rainChance,
                                pressure = pressure,
                                max = td.high,
                                min = td.low,
                                hourly = td.hourlyTemps
                            )
                        } else null

                        runOnUiThread {
                            if (newDays.isNotEmpty()) {
                                days = newDays
                                buildDayCards()
                            }
                            if (todayState != null) {
                                if (isStorm) {
                                    stormState = todayState
                                } else {
                                    sunnyState = todayState
                                }
                                current = todayState
                                sky.setCondition(current.condition, animate = true)
                                applyState(current, fromTemp = shownTemp)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun mapFullWeatherToState(w: WeatherAirQuality.FullWeather) {
        val code = WeatherAirQuality.getCurrentCode(this)
        val isStorm = (code >= 95 || (code in 51..67) || (code in 80..82))

        val hArray = if (w.hourly != null && w.hourly.size >= 8) {
            val step = Math.max(1, w.hourly.size / 8)
            IntArray(8) { idx ->
                val itemIdx = Math.min(idx * step, w.hourly.size - 1)
                w.hourly[itemIdx].temp
            }
        } else {
            intArrayOf(25, 25, 25, 27, 29, 29, 28, 25)
        }

        val rChance = if (w.daily.isNotEmpty() && w.daily[0].rainChance > 0) w.daily[0].rainChance else 95
        val p = if (w.pressure > 0) w.pressure else 1006

        if (isStorm) {
            stormState = WeatherState(
                condition = WeatherSkyView.Condition.STORM,
                title = "Thunderstorm",
                icon = "⛈️",
                temp = w.temp,
                feelsLike = w.apparentTemp,
                rainChance = rChance,
                pressure = p,
                max = w.highTemp,
                min = w.lowTemp,
                hourly = hArray
            )
            current = stormState
        } else {
            sunnyState = WeatherState(
                condition = WeatherSkyView.Condition.SUNNY,
                title = "Mostly Sunny",
                icon = "🌤️",
                temp = w.temp,
                feelsLike = w.apparentTemp,
                rainChance = 20,
                pressure = p,
                max = w.highTemp,
                min = w.lowTemp,
                hourly = hArray
            )
            current = sunnyState
        }
    }

    companion object {
        /** Maps WMO / OpenWeather weather codes to WeatherSkyView.Condition */
        fun mapCondition(weatherCode: Int): WeatherSkyView.Condition {
            return if (weatherCode >= 95 || (weatherCode in 51..67) || (weatherCode in 80..82)) {
                WeatherSkyView.Condition.STORM
            } else {
                WeatherSkyView.Condition.SUNNY
            }
        }
    }
}
