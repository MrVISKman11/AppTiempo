package com.example.weatherapp

import android.content.Context
import android.os.Bundle
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

class SettingsActivity : AppCompatActivity() {

    private lateinit var rgUnits: RadioGroup
    private lateinit var rgTheme: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar?.title = getString(R.string.menu_settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        rgUnits = findViewById(R.id.rgSettingsUnits)
        rgTheme = findViewById(R.id.rgSettingsTheme)

        loadSettings()

        rgUnits.setOnCheckedChangeListener { _, checkedId ->
            val isMetric = checkedId == R.id.rbSettingsMetric
            saveUnitPreference(isMetric)
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
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun loadSettings() {
        val sharedPref = getSharedPreferences("WeatherAppPrefs", Context.MODE_PRIVATE)
        
        // Units
        val isMetric = sharedPref.getBoolean("isMetric", true) // Default Metric
        if (isMetric) {
            findViewById<RadioButton>(R.id.rbSettingsMetric).isChecked = true
        } else {
            findViewById<RadioButton>(R.id.rbSettingsImperial).isChecked = true
        }

        // Theme
        val themeMode = sharedPref.getInt("themeMode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        when (themeMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> findViewById<RadioButton>(R.id.rbThemeLight).isChecked = true
            AppCompatDelegate.MODE_NIGHT_YES -> findViewById<RadioButton>(R.id.rbThemeDark).isChecked = true
            else -> findViewById<RadioButton>(R.id.rbThemeSystem).isChecked = true
        }
    }

    private fun saveUnitPreference(isMetric: Boolean) {
        val sharedPref = getSharedPreferences("WeatherAppPrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("isMetric", isMetric)
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
