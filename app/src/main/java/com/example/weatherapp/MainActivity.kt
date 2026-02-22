package com.example.weatherapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
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
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.DecimalFormat
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
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import com.example.weatherapp.ui.adapter.FavoritesAdapter
import com.example.weatherapp.viewmodel.WeatherViewModel
import com.example.weatherapp.ui.CustomMarkerView
import com.example.weatherapp.ui.adapter.ForecastAdapter

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var rvForecast: RecyclerView
    private lateinit var forecastAdapter: ForecastAdapter
    private lateinit var tvLocation: TextView
    private lateinit var tvCurrentConditions: TextView
    private lateinit var btnConsult: Button
    private lateinit var etStationId: EditText

    private lateinit var btnAddToFavorites: ImageButton
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var repository: FavoritesRepository
    private lateinit var chartTemperature: LineChart
    private lateinit var chartWind: LineChart
    private lateinit var chartPrecip: BarChart
    private lateinit var chartSolar: LineChart
    private lateinit var chartUV: LineChart
    private lateinit var chartPressure: LineChart
    private lateinit var llGraphsContent: android.widget.LinearLayout
    private lateinit var viewModel: WeatherViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyTheme()
        setContentView(R.layout.activity_main)

        repository = FavoritesRepository(this)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        rvForecast = findViewById(R.id.rvForecast)
        tvLocation = findViewById(R.id.tvLocation)
        tvCurrentConditions = findViewById(R.id.tvCurrentConditions)
        btnConsult = findViewById(R.id.btnConsult)
        etStationId = findViewById(R.id.etStationId)

        btnAddToFavorites = findViewById(R.id.btnAddToFavorites)
        chartTemperature = findViewById(R.id.chartTemperature)
        chartWind = findViewById(R.id.chartWind)
        chartPrecip = findViewById(R.id.chartPrecip)
        chartSolar = findViewById(R.id.chartSolar)
        chartUV = findViewById(R.id.chartUV)
        chartPressure = findViewById(R.id.chartPressure)
        llGraphsContent = findViewById(R.id.llGraphsContent)

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

        rvForecast.layoutManager = LinearLayoutManager(this)
        forecastAdapter = ForecastAdapter()
        rvForecast.adapter = forecastAdapter

        btnConsult.setOnClickListener {
            val stationId = etStationId.text.toString().trim().uppercase()
            if (stationId.isNotEmpty()) {
                val sharedPref = getSharedPreferences("WeatherAppPrefs", Context.MODE_PRIVATE)
                val tempUnit = sharedPref.getString("pref_temp_unit", "C") ?: "C"
                val speedUnit = sharedPref.getString("pref_speed_unit", "kmh") ?: "kmh"
                val precipUnit = sharedPref.getString("pref_precip_unit", "mm") ?: "mm"
                viewModel.fetchWeather(stationId, tempUnit, speedUnit, precipUnit)
            } else {
                etStationId.error = getString(R.string.hint_pws_id)
            }
        }

        etStationId.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateFavoriteIcon()
            }
        })

        btnAddToFavorites.setOnClickListener {
            val stationId = etStationId.text.toString().trim().uppercase()
            if (stationId.isNotEmpty()) {
                val isFavorite = repository.getFavorites().any { it.id.equals(stationId, ignoreCase = true) }
                if (isFavorite) {
                    repository.removeFavorite(stationId)
                    Toast.makeText(this, getString(R.string.remove), Toast.LENGTH_SHORT).show()
                    updateFavoriteIcon()
                } else {
                    showAddFavoriteDialog(stationId)
                }
            } else {
                Toast.makeText(this, getString(R.string.hint_pws_id), Toast.LENGTH_SHORT).show()
            }
        }
        
        setupViewModel()

        if (savedInstanceState == null) {
            val favorites = repository.getFavorites()
            if (favorites.isNotEmpty()) {
                val defaultStation = favorites[0]
                etStationId.setText(defaultStation.id)
                val sharedPref = getSharedPreferences("WeatherAppPrefs", Context.MODE_PRIVATE)
                val tempUnit = sharedPref.getString("pref_temp_unit", "C") ?: "C"
                val speedUnit = sharedPref.getString("pref_speed_unit", "kmh") ?: "kmh"
                val precipUnit = sharedPref.getString("pref_precip_unit", "mm") ?: "mm"
                viewModel.fetchWeather(defaultStation.id, tempUnit, speedUnit, precipUnit)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateFavoriteIcon()
        
        val sharedPref = getSharedPreferences("WeatherAppPrefs", Context.MODE_PRIVATE)
        val tempUnit = sharedPref.getString("pref_temp_unit", "C") ?: "C"
        val speedUnit = sharedPref.getString("pref_speed_unit", "kmh") ?: "kmh"
        val precipUnit = sharedPref.getString("pref_precip_unit", "mm") ?: "mm"
        val currentLang = java.util.Locale.getDefault().language
        val stationId = findViewById<EditText>(R.id.etStationId).text.toString().trim().uppercase()
        
        if (viewModel.lastLanguage != "" && (viewModel.lastLanguage != currentLang || viewModel.lastTempUnit != tempUnit || viewModel.lastSpeedUnit != speedUnit || viewModel.lastPrecipUnit != precipUnit)) {
            if (stationId.isNotEmpty()) {
                viewModel.fetchWeather(stationId, tempUnit, speedUnit, precipUnit)
            }
        }
    }

    private fun updateFavoriteIcon() {
        val etStationId: EditText = findViewById(R.id.etStationId)
        val btnAddToFavorites: ImageButton = findViewById(R.id.btnAddToFavorites)
        val stationId = etStationId.text.toString().trim().uppercase()
        val isFavorite = repository.getFavorites().any { it.id.equals(stationId, ignoreCase = true) }
        
        if (isFavorite) {
            btnAddToFavorites.setImageResource(R.drawable.ic_star)
            btnAddToFavorites.setColorFilter(Color.parseColor("#FFD700"))
        } else {
            btnAddToFavorites.setImageResource(R.drawable.ic_star_outline)
            btnAddToFavorites.setColorFilter(Color.parseColor("#FFD700"))
        }
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[WeatherViewModel::class.java]

        viewModel.weatherForecast.observe(this) { forecast ->
            if (forecast.isNotEmpty()) {
                val sharedPref = getSharedPreferences("WeatherAppPrefs", Context.MODE_PRIVATE)
                val tempUnit = sharedPref.getString("pref_temp_unit", "C") ?: "C"
                forecastAdapter.setForecasts(forecast, tempUnit)
                // We keep rvForecast visibility logic to the toggle header
            } else {
                rvForecast.visibility = android.view.View.GONE
            }
        }

        viewModel.currentConditions.observe(this) { conditions ->
            tvCurrentConditions.text = conditions
        }
        
        viewModel.locationName.observe(this) { location ->
            tvLocation.text = location
        }

        viewModel.chartHistory.observe(this) { history ->
            val sharedPref = getSharedPreferences("WeatherAppPrefs", Context.MODE_PRIVATE)
            val tempUnit = sharedPref.getString("pref_temp_unit", "C") ?: "C"
            val speedUnit = sharedPref.getString("pref_speed_unit", "kmh") ?: "kmh"
            val precipUnit = sharedPref.getString("pref_precip_unit", "mm") ?: "mm"
             CoroutineScope(Dispatchers.Default).launch {
                 updateCharts(history, tempUnit, speedUnit, precipUnit)
             }
        }
    }
    
    private fun setupCollapsibleSections() {
        val headerForecast: TextView = findViewById(R.id.headerForecast)
        val headerGraphs24h: TextView = findViewById(R.id.headerGraphs24h)
        val headerTemp: TextView = findViewById(R.id.headerTemp)
        val headerWind: TextView = findViewById(R.id.headerWind)
        val headerPrecip: TextView = findViewById(R.id.headerPrecip)
        val headerSolar: TextView = findViewById(R.id.headerSolar)
        val headerUV: TextView = findViewById(R.id.headerUV)
        val headerPressure: TextView = findViewById(R.id.headerPressure)

        toggleSection(headerForecast, rvForecast)
        toggleSection(headerGraphs24h, llGraphsContent)
        toggleSection(headerTemp, chartTemperature)
        toggleSection(headerWind, chartWind)
        toggleSection(headerPrecip, chartPrecip)
        toggleSection(headerSolar, chartSolar)
        toggleSection(headerUV, chartUV)
        toggleSection(headerPressure, chartPressure)
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
        val textColor = getThemeTextColor()

        // Temperature
        val tempMarker = CustomMarkerView(this, R.layout.custom_marker_view)
        tempMarker.chartView = chartTemperature
        chartTemperature.marker = tempMarker
        chartTemperature.description.isEnabled = false
        chartTemperature.setTouchEnabled(true)
        chartTemperature.isDragEnabled = true
        chartTemperature.setScaleEnabled(true)
        val xAxisTemp = chartTemperature.xAxis
        xAxisTemp.position = XAxis.XAxisPosition.BOTTOM
        xAxisTemp.setDrawGridLines(false)
        xAxisTemp.textColor = textColor
        chartTemperature.axisLeft.textColor = textColor
        chartTemperature.legend.textColor = textColor
        chartTemperature.axisRight.isEnabled = false

        // Wind
        val windMarker = CustomMarkerView(this, R.layout.custom_marker_view)
        windMarker.chartView = chartWind
        chartWind.marker = windMarker
        chartWind.description.isEnabled = false
        chartWind.setTouchEnabled(true)
        chartWind.isDragEnabled = true
        chartWind.setScaleEnabled(true)
        val xAxisWind = chartWind.xAxis
        xAxisWind.position = XAxis.XAxisPosition.BOTTOM
        xAxisWind.setDrawGridLines(false)
        xAxisWind.textColor = textColor
        chartWind.axisLeft.textColor = textColor
        chartWind.legend.textColor = textColor
        chartWind.axisRight.isEnabled = false

        // Precip
        val precipMarker = CustomMarkerView(this, R.layout.custom_marker_view)
        precipMarker.chartView = chartPrecip
        chartPrecip.marker = precipMarker
        chartPrecip.description.isEnabled = false
        chartPrecip.setTouchEnabled(true)
        chartPrecip.isDragEnabled = true
        chartPrecip.setScaleEnabled(true)
        val xAxisPrecip = chartPrecip.xAxis
        xAxisPrecip.position = XAxis.XAxisPosition.BOTTOM
        xAxisPrecip.setDrawGridLines(false)
        xAxisPrecip.textColor = textColor
        chartPrecip.axisLeft.textColor = textColor
        chartPrecip.axisLeft.axisMinimum = 0f // Start at 0
        chartPrecip.axisLeft.setDrawZeroLine(true) // Draw line at 0
        chartPrecip.legend.textColor = textColor
        chartPrecip.axisRight.isEnabled = false

        // Solar
        setupLineChart(chartSolar, textColor)

        // UV
        setupLineChart(chartUV, textColor)

        // Pressure
        setupLineChart(chartPressure, textColor)
    }

    private fun setupLineChart(chart: LineChart, textColor: Int) {
        val markerView = CustomMarkerView(this, R.layout.custom_marker_view)
        markerView.chartView = chart
        chart.marker = markerView
        chart.description.isEnabled = false
        chart.setTouchEnabled(true)
        chart.isDragEnabled = true
        chart.setScaleEnabled(true)
        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.textColor = textColor
        chart.axisLeft.textColor = textColor
        chart.legend.textColor = textColor
        chart.axisRight.isEnabled = false
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
            R.id.nav_wundermap -> {
                startActivity(Intent(this, WunderMapActivity::class.java))
            }
            R.id.nav_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
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
                updateFavoriteIcon()
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

        val dialogView = layoutInflater.inflate(R.layout.dialog_favorites, null)
        val rvFavorites = dialogView.findViewById<RecyclerView>(R.id.rvFavorites)
        val btnClose = dialogView.findViewById<Button>(R.id.btnClose)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val adapter = FavoritesAdapter(
            favorites,
            onStationClick = { selected ->
                etStationId.setText(selected.id)
                val sharedPref = getSharedPreferences("WeatherAppPrefs", Context.MODE_PRIVATE)
                val tempUnit = sharedPref.getString("pref_temp_unit", "C") ?: "C"
                val speedUnit = sharedPref.getString("pref_speed_unit", "kmh") ?: "kmh"
                val precipUnit = sharedPref.getString("pref_precip_unit", "mm") ?: "mm"
                viewModel.fetchWeather(selected.id, tempUnit, speedUnit, precipUnit)
                dialog.dismiss()
            },
            onStartDrag = { viewHolder ->
                // Drag started via handle, handled by ItemTouchHelper
            }
        )
        
        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.adapterPosition
                val toPos = target.adapterPosition
                adapter.onItemMove(fromPos, toPos)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // No swipe to dismiss
            }
            
            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                // Save new order when drag is finished
                repository.updateFavorites(adapter.getItems())
            }
        })
        
        touchHelper.attachToRecyclerView(rvFavorites)

        rvFavorites.layoutManager = LinearLayoutManager(this)
        rvFavorites.adapter = adapter

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }


    private suspend fun updateCharts(observations: List<com.example.weatherapp.model.PwsHistoryObservation>, tempUnitPref: String, speedUnitPref: String, precipUnitPref: String) {
        val tempEntries = ArrayList<Entry>()
        val feelsLikeEntries = ArrayList<Entry>()
        val windEntries = ArrayList<Entry>()
        val precipEntries = ArrayList<BarEntry>()
        val solarEntries = ArrayList<Entry>()
        val uvEntries = ArrayList<Entry>()
        val pressureEntries = ArrayList<Entry>()
        val labels = ArrayList<String>()
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val cal = java.util.Calendar.getInstance()
        
        val sortedObs = observations.sortedBy { it.epoch }
        
        for ((index, obs) in sortedObs.withIndex()) {
             val data = obs.metric
             var temp = data?.tempAvg?.toFloat() ?: 0f
             
             // Calculate Feels Like (Heat Index / Wind Chill)
             val heatIndexVal = data?.heatindexAvg?.toFloat()
             val windChillVal = data?.windchillAvg?.toFloat()
             var feelsLike = when {
                 heatIndexVal != null && heatIndexVal > temp -> heatIndexVal
                 windChillVal != null && windChillVal < temp -> windChillVal
                 else -> temp
             }

             if (tempUnitPref == "F") {
                 temp = (temp * 9f/5f) + 32f
                 feelsLike = (feelsLike * 9f/5f) + 32f
             }

             var wind = data?.windspeedAvg?.toFloat() ?: 0f
             if (speedUnitPref == "mph") {
                 wind /= 1.60934f
             }

             var precip = data?.precipTotal?.toFloat() ?: 0f
             if (precipUnitPref == "in") {
                 precip /= 25.4f
             }

             val solar = obs.solarRadiationHigh?.toFloat() ?: 0f
             val uv = obs.uvHigh?.toFloat() ?: 0f
             val pressure = data?.pressureMax?.toFloat() ?: 0f

             tempEntries.add(Entry(index.toFloat(), temp))
             feelsLikeEntries.add(Entry(index.toFloat(), feelsLike))
             windEntries.add(Entry(index.toFloat(), wind))
             precipEntries.add(BarEntry(index.toFloat(), precip))
             solarEntries.add(Entry(index.toFloat(), solar))
             uvEntries.add(Entry(index.toFloat(), uv))
             pressureEntries.add(Entry(index.toFloat(), pressure))
             
             // Round to nearest hour
             cal.timeInMillis = obs.epoch * 1000
             if (cal.get(java.util.Calendar.MINUTE) >= 30) {
                 cal.add(java.util.Calendar.HOUR_OF_DAY, 1)
             }
             cal.set(java.util.Calendar.MINUTE, 0)
             labels.add(sdf.format(cal.time))
        }
        
        withContext(Dispatchers.Main) {
            val textColor = getThemeTextColor()

            val sharedPrefColors = getSharedPreferences("WeatherAppPrefs", Context.MODE_PRIVATE)
            val colorTemp = sharedPrefColors.getInt("pref_color_temp", Color.RED)
            val colorFeels = sharedPrefColors.getInt("pref_color_feels_like", Color.parseColor("#FFA500"))
            val colorWind = sharedPrefColors.getInt("pref_color_wind", Color.GREEN)
            val colorPrecip = sharedPrefColors.getInt("pref_color_precip", Color.BLUE)
            val colorSolar = sharedPrefColors.getInt("pref_color_solar", Color.YELLOW)
            val colorUV = sharedPrefColors.getInt("pref_color_uv", Color.MAGENTA)
            val colorPressure = sharedPrefColors.getInt("pref_color_pressure", Color.CYAN)

            val decimalFormat = DecimalFormat("#.0")
            val defaultFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String = decimalFormat.format(value)
            }
            
            // Temp Chart
            val tempDataSet = LineDataSet(tempEntries, "${getString(R.string.graph_temp)} (${if (tempUnitPref == "C") "°C" else "°F"})")
            tempDataSet.color = colorTemp
            tempDataSet.setDrawCircles(false)
            tempDataSet.lineWidth = 3f
            tempDataSet.valueTextColor = textColor
            tempDataSet.valueFormatter = defaultFormatter
            
            val feelsLikeDataSet = LineDataSet(feelsLikeEntries, "${getString(R.string.graph_feels_like)} (${if (tempUnitPref == "C") "°C" else "°F"})")
            feelsLikeDataSet.color = colorFeels
            feelsLikeDataSet.setDrawCircles(false)
            feelsLikeDataSet.lineWidth = 1.5f
            feelsLikeDataSet.enableDashedLine(10f, 5f, 0f)
            feelsLikeDataSet.valueTextColor = textColor
            feelsLikeDataSet.valueFormatter = defaultFormatter

            chartTemperature.axisLeft.valueFormatter = defaultFormatter
            // Draw feelsLike first so tempDataSet draws on top when they overlap
            chartTemperature.data = LineData(feelsLikeDataSet, tempDataSet)
            chartTemperature.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            chartTemperature.invalidate()
            chartTemperature.animateX(1000)

            // Wind Chart
            val windDataSet = LineDataSet(windEntries, "${getString(R.string.graph_wind)} (${if (speedUnitPref == "kmh") "km/h" else "mph"})")
            windDataSet.color = colorWind
            windDataSet.setDrawCircles(false)
            windDataSet.lineWidth = 2f
            windDataSet.valueTextColor = textColor
            windDataSet.valueFormatter = defaultFormatter
            chartWind.axisLeft.valueFormatter = defaultFormatter
            chartWind.data = LineData(windDataSet)
            chartWind.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            chartWind.invalidate()
            chartWind.animateX(1000)

            // Precip Chart
            val precipDataSet = BarDataSet(precipEntries, "${getString(R.string.graph_precip)} (${if (precipUnitPref == "mm") "mm" else "in"})")
            precipDataSet.color = colorPrecip
            precipDataSet.valueTextColor = textColor
            precipDataSet.valueFormatter = defaultFormatter
            chartPrecip.axisLeft.valueFormatter = defaultFormatter
            chartPrecip.data = BarData(precipDataSet)
            chartPrecip.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            chartPrecip.invalidate()
            chartPrecip.animateY(1000)

            // Solar Chart
            val solarDataSet = LineDataSet(solarEntries, getString(R.string.graph_solar_unit))
            solarDataSet.color = colorSolar
            solarDataSet.setDrawCircles(false)
            solarDataSet.lineWidth = 2f
            solarDataSet.valueTextColor = textColor
            solarDataSet.valueFormatter = defaultFormatter
            chartSolar.axisLeft.valueFormatter = defaultFormatter
            chartSolar.data = LineData(solarDataSet)
            chartSolar.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            chartSolar.invalidate()
            chartSolar.animateX(1000)

            // UV Chart
            val uvDataSet = LineDataSet(uvEntries, getString(R.string.graph_uv))
            uvDataSet.color = colorUV
            uvDataSet.setDrawCircles(false)
            uvDataSet.lineWidth = 2f
            uvDataSet.valueTextColor = textColor
            uvDataSet.valueFormatter = defaultFormatter
            chartUV.axisLeft.valueFormatter = defaultFormatter
            chartUV.data = LineData(uvDataSet)
            chartUV.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            chartUV.invalidate()
            chartUV.animateX(1000)

            // Pressure Chart
            // We didn't add a pressure unit toggle yet, so we map it to precip unit roughly or temp unit for metricness
            val pressureDataSet = LineDataSet(pressureEntries, "${getString(R.string.graph_pressure)} (${if (tempUnitPref == "C") "hPa" else "inHg"})")
            pressureDataSet.color = colorPressure
            pressureDataSet.setDrawCircles(false)
            pressureDataSet.lineWidth = 2f
            pressureDataSet.valueTextColor = textColor
            pressureDataSet.valueFormatter = defaultFormatter
            chartPressure.axisLeft.valueFormatter = defaultFormatter
            chartPressure.data = LineData(pressureDataSet)
            chartPressure.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            chartPressure.invalidate()
            chartPressure.animateX(1000)
        }
    }

    private fun getThemeTextColor(): Int {
        val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return if (currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            Color.WHITE
        } else {
            Color.BLACK
        }
    }

    private fun getWindDirection(degrees: Int?): String {
        if (degrees == null) return ""
        val directions = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSO", "SO", "OSO", "O", "ONO", "NO", "NNO")
        val index = ((degrees / 22.5) + 0.5).toInt() % 16
        return directions[index]
    }
    private fun applyTheme() {
        val sharedPref = getSharedPreferences("WeatherAppPrefs", Context.MODE_PRIVATE)
        val themeMode = sharedPref.getInt("themeMode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(themeMode)
    }
}
