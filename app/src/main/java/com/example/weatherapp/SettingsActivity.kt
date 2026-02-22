package com.example.weatherapp

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

class SettingsActivity : AppCompatActivity() {

    private lateinit var rgTemp: RadioGroup
    private lateinit var rgSpeed: RadioGroup
    private lateinit var rgPrecip: RadioGroup
    private lateinit var rgTheme: RadioGroup
    private lateinit var rgLanguage: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar?.title = getString(R.string.menu_settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        rgTemp = findViewById(R.id.rgSettingsTemp)
        rgSpeed = findViewById(R.id.rgSettingsSpeed)
        rgPrecip = findViewById(R.id.rgSettingsPrecip)
        rgTheme = findViewById(R.id.rgSettingsTheme)
        rgLanguage = findViewById(R.id.rgSettingsLanguage)

        loadSettings()

        rgTemp.setOnCheckedChangeListener { _, checkedId ->
            val unit = if (checkedId == R.id.rbTempC) "C" else "F"
            saveStringPreference("pref_temp_unit", unit)
        }

        rgSpeed.setOnCheckedChangeListener { _, checkedId ->
            val unit = if (checkedId == R.id.rbSpeedKmh) "kmh" else "mph"
            saveStringPreference("pref_speed_unit", unit)
        }

        rgPrecip.setOnCheckedChangeListener { _, checkedId ->
            val unit = if (checkedId == R.id.rbPrecipMm) "mm" else "in"
            saveStringPreference("pref_precip_unit", unit)
        }

        rgTheme.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.rbThemeLight -> AppCompatDelegate.MODE_NIGHT_NO
                R.id.rbThemeDark -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            saveThemePreference(mode)
            AppCompatDelegate.setDefaultNightMode(mode)
        }

        rgLanguage.setOnCheckedChangeListener { _, checkedId ->
            val langTag = if (checkedId == R.id.rbLangEnglish) "en" else "es"
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(langTag))
        }

        setupCollapsibleSections()
        setupColorPickers()
    }
    
    private fun setupCollapsibleSections() {
        val headerUnits = findViewById<TextView>(R.id.headerSettingsUnits)
        val contentUnits = findViewById<android.view.View>(R.id.llSettingsUnits)
        toggleSection(headerUnits, contentUnits)

        val headerTemp = findViewById<TextView>(R.id.headerSettingsTemp)
        val contentTemp = findViewById<android.view.View>(R.id.rgSettingsTemp)
        toggleSection(headerTemp, contentTemp)

        val headerSpeed = findViewById<TextView>(R.id.headerSettingsSpeed)
        val contentSpeed = findViewById<android.view.View>(R.id.rgSettingsSpeed)
        toggleSection(headerSpeed, contentSpeed)

        val headerPrecip = findViewById<TextView>(R.id.headerSettingsPrecip)
        val contentPrecip = findViewById<android.view.View>(R.id.rgSettingsPrecip)
        toggleSection(headerPrecip, contentPrecip)

        val headerTheme = findViewById<TextView>(R.id.headerSettingsTheme)
        val contentTheme = findViewById<android.view.View>(R.id.rgSettingsTheme)
        toggleSection(headerTheme, contentTheme)

        val headerLang = findViewById<TextView>(R.id.headerSettingsLanguage)
        val contentLang = findViewById<android.view.View>(R.id.rgSettingsLanguage)
        toggleSection(headerLang, contentLang)

        val headerColors = findViewById<TextView>(R.id.headerSettingsColors)
        val contentColors = findViewById<android.view.View>(R.id.llSettingsColors)
        toggleSection(headerColors, contentColors)
    }

    private fun toggleSection(header: TextView, content: android.view.View) {
        header.setOnClickListener {
            val isVisible = content.visibility == android.view.View.VISIBLE
            content.visibility = if (isVisible) android.view.View.GONE else android.view.View.VISIBLE
            
            val iconRes = if (isVisible) R.drawable.ic_arrow_drop_down else R.drawable.ic_arrow_drop_up
            header.setCompoundDrawablesWithIntrinsicBounds(0, 0, iconRes, 0)
        }
    }

    private fun setupColorPickers() {
        findViewById<TextView>(R.id.btnColorTemp).setOnClickListener { showColorPickerDialog("pref_color_temp", "Temperatura") }
        findViewById<TextView>(R.id.btnColorFeels).setOnClickListener { showColorPickerDialog("pref_color_feels_like", "Sensación Térmica") }
        findViewById<TextView>(R.id.btnColorWind).setOnClickListener { showColorPickerDialog("pref_color_wind", "Viento") }
        findViewById<TextView>(R.id.btnColorPrecip).setOnClickListener { showColorPickerDialog("pref_color_precip", "Precipitación") }
        findViewById<TextView>(R.id.btnColorSolar).setOnClickListener { showColorPickerDialog("pref_color_solar", "Radiación Solar") }
        findViewById<TextView>(R.id.btnColorUV).setOnClickListener { showColorPickerDialog("pref_color_uv", "Índice UV") }
        findViewById<TextView>(R.id.btnColorPressure).setOnClickListener { showColorPickerDialog("pref_color_pressure", "Presión") }

        findViewById<Button>(R.id.btnResetColors).setOnClickListener {
            val sharedPref = getSharedPreferences("WeatherAppPrefs", Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                remove("pref_color_temp")
                remove("pref_color_feels_like")
                remove("pref_color_wind")
                remove("pref_color_precip")
                remove("pref_color_solar")
                remove("pref_color_uv")
                remove("pref_color_pressure")
                apply()
            }
            AlertDialog.Builder(this)
                .setTitle("Restablecidos")
                .setMessage("Los colores de los gráficos han vuelto a sus valores por defecto.")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun showColorPickerDialog(prefKey: String, title: String) {
        val colorNames = arrayOf("Rojo", "Naranja", "Amarillo", "Verde", "Azul", "Cian", "Morado", "Blanco")
        val colorValues = intArrayOf(
            Color.RED, 
            Color.parseColor("#FFA500"), 
            Color.YELLOW, 
            Color.GREEN, 
            Color.BLUE, 
            Color.CYAN, 
            Color.parseColor("#800080"), 
            Color.WHITE
        )

        AlertDialog.Builder(this)
            .setTitle("Color de $title")
            .setItems(colorNames) { _, which ->
                val selectedColor = colorValues[which]
                val sharedPref = getSharedPreferences("WeatherAppPrefs", Context.MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putInt(prefKey, selectedColor)
                    apply()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun loadSettings() {
        val sharedPref = getSharedPreferences("WeatherAppPrefs", Context.MODE_PRIVATE)
        
        // Units
        val tempUnit = sharedPref.getString("pref_temp_unit", "C")
        if (tempUnit == "C") findViewById<RadioButton>(R.id.rbTempC).isChecked = true
        else findViewById<RadioButton>(R.id.rbTempF).isChecked = true

        val speedUnit = sharedPref.getString("pref_speed_unit", "kmh")
        if (speedUnit == "kmh") findViewById<RadioButton>(R.id.rbSpeedKmh).isChecked = true
        else findViewById<RadioButton>(R.id.rbSpeedMph).isChecked = true

        val precipUnit = sharedPref.getString("pref_precip_unit", "mm")
        if (precipUnit == "mm") findViewById<RadioButton>(R.id.rbPrecipMm).isChecked = true
        else findViewById<RadioButton>(R.id.rbPrecipIn).isChecked = true

        // Theme
        val themeMode = sharedPref.getInt("themeMode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        when (themeMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> findViewById<RadioButton>(R.id.rbThemeLight).isChecked = true
            AppCompatDelegate.MODE_NIGHT_YES -> findViewById<RadioButton>(R.id.rbThemeDark).isChecked = true
            else -> findViewById<RadioButton>(R.id.rbThemeSystem).isChecked = true
        }

        // Language
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        if (!currentLocales.isEmpty) {
            val currentLang = currentLocales.get(0)?.language
            if (currentLang == "en") {
                findViewById<RadioButton>(R.id.rbLangEnglish).isChecked = true
            } else {
                findViewById<RadioButton>(R.id.rbLangSpanish).isChecked = true
            }
        } else {
            findViewById<RadioButton>(R.id.rbLangSpanish).isChecked = true // Default to Spanish
        }
    }

    private fun saveStringPreference(key: String, value: String) {
        val sharedPref = getSharedPreferences("WeatherAppPrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString(key, value)
            apply()
        }
    }

    private fun saveThemePreference(mode: Int) {
        val sharedPref = getSharedPreferences("WeatherAppPrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("themeMode", mode)
            apply()
        }
    }
}
