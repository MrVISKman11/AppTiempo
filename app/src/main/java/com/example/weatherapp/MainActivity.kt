package com.example.weatherapp

import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.weatherapp.api.WeatherService
import com.example.weatherapp.data.FavoritesRepository
import com.example.weatherapp.model.FavoriteStation
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var tvWeather: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvCurrentConditions: TextView
    private lateinit var btnConsult: Button
    private lateinit var etStationId: EditText
    private lateinit var rgUnits: RadioGroup
    private lateinit var btnAddToFavorites: ImageButton
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var repository: FavoritesRepository
    private lateinit var chartTemperature: LineChart
    private lateinit var chartWind: LineChart
    private lateinit var chartPrecip: BarChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repository = FavoritesRepository(this)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        tvWeather = findViewById(R.id.tvWeather)
        tvLocation = findViewById(R.id.tvLocation)
        tvCurrentConditions = findViewById(R.id.tvCurrentConditions)
        btnConsult = findViewById(R.id.btnConsult)
        etStationId = findViewById(R.id.etStationId)
        rgUnits = findViewById(R.id.rgUnits)
        btnAddToFavorites = findViewById(R.id.btnAddToFavorites)
        chartTemperature = findViewById(R.id.chartTemperature)
        chartWind = findViewById(R.id.chartWind)
        chartPrecip = findViewById(R.id.chartPrecip)
        
        setupCharts()
        setupCollapsibleSections()
        
        // Setup Drawer Toggle
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, R.string.menu_home, R.string.menu_favorites
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        navView.setNavigationItemSelectedListener(this)

        tvWeather.text = getString(R.string.loading)

        btnConsult.setOnClickListener {
            val stationId = etStationId.text.toString().trim()
            if (stationId.isNotEmpty()) {
                fetchWeather(stationId)
            } else {
                etStationId.error = getString(R.string.hint_pws_id)
            }
        }

        btnAddToFavorites.setOnClickListener {
            val stationId = etStationId.text.toString().trim()
            if (stationId.isNotEmpty()) {
                showAddFavoriteDialog(stationId)
            } else {
                Toast.makeText(this, getString(R.string.hint_pws_id), Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun setupCollapsibleSections() {
        val headerForecast: TextView = findViewById(R.id.headerForecast)
        val headerTemp: TextView = findViewById(R.id.headerTemp)
        val headerWind: TextView = findViewById(R.id.headerWind)
        val headerPrecip: TextView = findViewById(R.id.headerPrecip)

        toggleSection(headerForecast, tvWeather)
        toggleSection(headerTemp, chartTemperature)
        toggleSection(headerWind, chartWind)
        toggleSection(headerPrecip, chartPrecip)
    }

    private fun toggleSection(header: TextView, content: android.view.View) {
        header.setOnClickListener {
            val isVisible = content.visibility == android.view.View.VISIBLE
            content.visibility = if (isVisible) android.view.View.GONE else android.view.View.VISIBLE
            
            val iconRes = if (isVisible) R.drawable.ic_arrow_drop_down else R.drawable.ic_arrow_drop_up
            header.setCompoundDrawablesWithIntrinsicBounds(0, 0, iconRes, 0)
        }
    }
    
    private fun setupCharts() {
        // Temperature
        chartTemperature.description.isEnabled = false
        chartTemperature.setTouchEnabled(true)
        chartTemperature.isDragEnabled = true
        chartTemperature.setScaleEnabled(true)
        val xAxisTemp = chartTemperature.xAxis
        xAxisTemp.position = XAxis.XAxisPosition.BOTTOM
        xAxisTemp.setDrawGridLines(false)
        chartTemperature.axisRight.isEnabled = false

        // Wind
        chartWind.description.isEnabled = false
        chartWind.setTouchEnabled(true)
        chartWind.isDragEnabled = true
        chartWind.setScaleEnabled(true)
        val xAxisWind = chartWind.xAxis
        xAxisWind.position = XAxis.XAxisPosition.BOTTOM
        xAxisWind.setDrawGridLines(false)
        chartWind.axisRight.isEnabled = false

        // Precip
        chartPrecip.description.isEnabled = false
        chartPrecip.setTouchEnabled(true)
        chartPrecip.isDragEnabled = true
        chartPrecip.setScaleEnabled(true)
        val xAxisPrecip = chartPrecip.xAxis
        xAxisPrecip.position = XAxis.XAxisPosition.BOTTOM
        xAxisPrecip.setDrawGridLines(false)
        chartPrecip.axisRight.isEnabled = false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                // Already on home
            }
            R.id.nav_favorites -> {
                showFavoritesDialog()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun showAddFavoriteDialog(stationId: String) {
        val editText = EditText(this)
        editText.hint = getString(R.string.favorite_name_hint)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.add_favorite))
            .setMessage("ID: $stationId")
            .setView(editText)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val name = editText.text.toString().trim()
                val finalName = if (name.isNotEmpty()) name else stationId
                repository.addFavorite(FavoriteStation(stationId, finalName))
                Toast.makeText(this, "Guardado!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showFavoritesDialog() {
        val favorites = repository.getFavorites()
        if (favorites.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_favorites), Toast.LENGTH_SHORT).show()
            return
        }

        val names: Array<CharSequence> = favorites.map { "${it.name} (${it.id})" }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.menu_favorites))
            .setItems(names) { _, which ->
                val selected = favorites[which]
                etStationId.setText(selected.id)
                fetchWeather(selected.id)
            }
            .setNeutralButton(getString(R.string.cancel), null)
            .show()
    }

    private fun fetchWeather(stationId: String) {
        tvWeather.text = getString(R.string.loading)
        tvCurrentConditions.text = getString(R.string.loading)
        chartTemperature.clear()
        chartWind.clear()
        chartPrecip.clear()
        
        // Determine units
        val isMetric = rgUnits.checkedRadioButtonId == R.id.rbMetric
        val unitCode = if (isMetric) "m" else "e"
        
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.weather.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(WeatherService::class.java)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Get PWS Observations
                val apiKey = "d183be15278740c583be15278740c504"
                // Localized request
                val pwsResponse = service.getPwsObservations(stationId = stationId, units = unitCode, apiKey = apiKey)
                
                if (pwsResponse.isSuccessful && pwsResponse.body()?.observations?.isNotEmpty() == true) {
                    val observation = pwsResponse.body()!!.observations!![0]
                    val lat = observation.lat
                    val lon = observation.lon
                    val neighborhood = observation.neighborhood ?: stationId
                    
                    // Display Current Conditions
                    withContext(Dispatchers.Main) {
                        tvLocation.text = neighborhood
                        val data = if (isMetric) observation.metric else observation.imperial
                        val tempUnit = if (isMetric) "°C" else "°F"
                        val speedUnit = if (isMetric) "km/h" else "mph"
                        
                        if (data != null) {
                            val precipUnit = if (isMetric) "mm/hr" else "in"
                            val windDir = getWindDirection(observation.winddir)
                            tvCurrentConditions.text = "Temp: ${data.temp}$tempUnit\n" +
                                                       "Viento: ${data.windSpeed} $speedUnit $windDir (Ráfaga: ${data.windGust})\n" +
                                                       "Precip: ${data.precipTotal?.toString() ?: "0"} $precipUnit"
                        }
                    }

                    // 2. Get Forecast using Lat/Lon from PWS
                    val geocode = "$lat,$lon"
                    // Force Spanish language
                    val forecastResponse = service.get5DayForecast(geocode = geocode, units = unitCode, language = "es-ES", apiKey = apiKey)
                    
                    withContext(Dispatchers.Main) {
                        if (forecastResponse.isSuccessful) {
                            val weather = forecastResponse.body()
                            if (weather != null) {
                                val sb = StringBuilder()
                                val days = weather.daysOfWeek
                                val maxs = weather.maxTemps
                                val mins = weather.minTemps
                                val narratives = weather.narratives
                                
                                for (i in days.indices) {
                                    sb.append("${days.getOrNull(i) ?: "Día"}:\n")
                                    sb.append("Max: ${maxs.getOrNull(i)} | Min: ${mins.getOrNull(i)}\n")
                                    sb.append("${narratives.getOrNull(i)}\n\n")
                                }
                                tvWeather.text = sb.toString()
                            } else {
                                tvWeather.text = getString(R.string.station_not_found)
                            }
                        } else {
                            tvWeather.text = "${getString(R.string.error_prefix)} Forecast ${forecastResponse.code()}"
                        }
                    }
                    
                    // 3. Get History for Graph
                    try {
                        val historyResponse = service.getPwsHistory(stationId = stationId, units = unitCode, apiKey = apiKey)
                        if (historyResponse.isSuccessful) {
                            val history = historyResponse.body()?.observations
                            if (!history.isNullOrEmpty()) {
                                updateCharts(history, isMetric)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                } else {
                    withContext(Dispatchers.Main) {
                        tvCurrentConditions.text = getString(R.string.station_not_found)
                        tvWeather.text = "${getString(R.string.error_prefix)} PWS ${pwsResponse.code()}"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvWeather.text = "${getString(R.string.error_prefix)} ${e.message}"
                }
            }
        }
    }
    
    private suspend fun updateCharts(observations: List<com.example.weatherapp.model.PwsHistoryObservation>, isMetric: Boolean) {
        val tempEntries = ArrayList<Entry>()
        val windEntries = ArrayList<Entry>()
        val precipEntries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        
        val sortedObs = observations.sortedBy { it.epoch }
        
        for ((index, obs) in sortedObs.withIndex()) {
             val data = if (isMetric) obs.metric else obs.imperial
             val temp = data?.tempAvg?.toFloat() ?: 0f
             val wind = data?.windspeedAvg?.toFloat() ?: 0f
             val precip = data?.precipTotal?.toFloat() ?: 0f

             tempEntries.add(Entry(index.toFloat(), temp))
             windEntries.add(Entry(index.toFloat(), wind))
             precipEntries.add(BarEntry(index.toFloat(), precip))
             
             labels.add(sdf.format(Date(obs.epoch * 1000)))
        }
        
        withContext(Dispatchers.Main) {
            // Temp Chart
            val tempDataSet = LineDataSet(tempEntries, "Temperatura (${if (isMetric) "°C" else "°F"})")
            tempDataSet.color = Color.RED
            tempDataSet.setDrawCircles(false)
            tempDataSet.lineWidth = 2f
            chartTemperature.data = LineData(tempDataSet)
            chartTemperature.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            chartTemperature.invalidate()
            chartTemperature.animateX(1000)

            // Wind Chart
            val windDataSet = LineDataSet(windEntries, "Viento (${if (isMetric) "km/h" else "mph"})")
            windDataSet.color = Color.GREEN
            windDataSet.setDrawCircles(false)
            windDataSet.lineWidth = 2f
            chartWind.data = LineData(windDataSet)
            chartWind.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            chartWind.invalidate()
            chartWind.animateX(1000)

            // Precip Chart
            val precipDataSet = BarDataSet(precipEntries, "Precipitación (${if (isMetric) "mm" else "in"})")
            precipDataSet.color = Color.BLUE
            chartPrecip.data = BarData(precipDataSet)
            chartPrecip.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            chartPrecip.invalidate()
            chartPrecip.animateY(1000)
        }
    }

    private fun getWindDirection(degrees: Int?): String {
        if (degrees == null) return ""
        val directions = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSO", "SO", "OSO", "O", "ONO", "NO", "NNO")
        val index = ((degrees / 22.5) + 0.5).toInt() % 16
        return directions[index]
    }
}
