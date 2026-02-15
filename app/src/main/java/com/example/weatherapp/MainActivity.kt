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

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var tvWeather: TextView
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
    private lateinit var viewModel: WeatherViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyTheme()
        setContentView(R.layout.activity_main)

        repository = FavoritesRepository(this)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        tvWeather = findViewById(R.id.tvWeather)
        tvLocation = findViewById(R.id.tvLocation)
        tvCurrentConditions = findViewById(R.id.tvCurrentConditions)
        btnConsult = findViewById(R.id.btnConsult)
        etStationId = findViewById(R.id.etStationId)

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
                val sharedPref = getSharedPreferences("WeatherAppPrefs", Context.MODE_PRIVATE)
                val isMetric = sharedPref.getBoolean("isMetric", true)
                viewModel.fetchWeather(stationId, isMetric)
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
        
        setupViewModel()

        if (savedInstanceState == null) {
            val favorites = repository.getFavorites()
            if (favorites.isNotEmpty()) {
                val defaultStation = favorites[0]
                etStationId.setText(defaultStation.id)
                val sharedPref = getSharedPreferences("WeatherAppPrefs", Context.MODE_PRIVATE)
                val isMetric = sharedPref.getBoolean("isMetric", true)
                viewModel.fetchWeather(defaultStation.id, isMetric)
            }
        }
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[WeatherViewModel::class.java]

        viewModel.weatherForecast.observe(this) { forecast ->
            tvWeather.text = forecast
        }

        viewModel.currentConditions.observe(this) { conditions ->
            tvCurrentConditions.text = conditions
        }
        
        viewModel.locationName.observe(this) { location ->
            tvLocation.text = location
        }

        viewModel.chartHistory.observe(this) { history ->
            val sharedPref = getSharedPreferences("WeatherAppPrefs", Context.MODE_PRIVATE)
            val isMetric = sharedPref.getBoolean("isMetric", true)
            // Use coroutine or simple launch if updateCharts is suspend? 
            // updateCharts is suspend in original code. We should make it non-suspend or launch it.
            // Since we are on Main thread here, and updateCharts only does calculation and UI update...
            // the calculation part (creating entries) might be heavy?
            // Original code: private suspend fun updateCharts
            // logic: creates entries (fast), then withContext(Main) update UI.
            // We can make updateCharts non-suspend or launch a coroutine.
            // Let's change updateCharts to be non-suspend and use simple logic, or launch lifecycleScope.
             CoroutineScope(Dispatchers.Default).launch {
                 updateCharts(history, isMetric)
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
        val textColor = getThemeTextColor()

        // Temperature
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
                val isMetric = sharedPref.getBoolean("isMetric", true)
                viewModel.fetchWeather(selected.id, isMetric)
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


    
    private suspend fun updateCharts(observations: List<com.example.weatherapp.model.PwsHistoryObservation>, isMetric: Boolean) {
        val tempEntries = ArrayList<Entry>()
        val windEntries = ArrayList<Entry>()
        val precipEntries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val cal = java.util.Calendar.getInstance()
        
        val sortedObs = observations.sortedBy { it.epoch }
        
        for ((index, obs) in sortedObs.withIndex()) {
             val data = if (isMetric) obs.metric else obs.imperial
             val temp = data?.tempAvg?.toFloat() ?: 0f
             val wind = data?.windspeedAvg?.toFloat() ?: 0f
             val precip = data?.precipTotal?.toFloat() ?: 0f

             tempEntries.add(Entry(index.toFloat(), temp))
             windEntries.add(Entry(index.toFloat(), wind))
             precipEntries.add(BarEntry(index.toFloat(), precip))
             
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

            // Temp Chart
            val tempDataSet = LineDataSet(tempEntries, "Temperatura (${if (isMetric) "°C" else "°F"})")
            tempDataSet.color = Color.RED
            tempDataSet.setDrawCircles(false)
            tempDataSet.lineWidth = 2f
            tempDataSet.valueTextColor = textColor
            chartTemperature.data = LineData(tempDataSet)
            chartTemperature.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            chartTemperature.invalidate()
            chartTemperature.animateX(1000)

            // Wind Chart
            val windDataSet = LineDataSet(windEntries, "Viento (${if (isMetric) "km/h" else "mph"})")
            windDataSet.color = Color.GREEN
            windDataSet.setDrawCircles(false)
            windDataSet.lineWidth = 2f
            windDataSet.valueTextColor = textColor
            chartWind.data = LineData(windDataSet)
            chartWind.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            chartWind.invalidate()
            chartWind.animateX(1000)

            // Precip Chart
            val precipDataSet = BarDataSet(precipEntries, "Precipitación (${if (isMetric) "mm" else "in"})")
            precipDataSet.color = Color.BLUE
            precipDataSet.valueTextColor = textColor
            chartPrecip.data = BarData(precipDataSet)
            chartPrecip.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            chartPrecip.invalidate()
            chartPrecip.animateY(1000)
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
