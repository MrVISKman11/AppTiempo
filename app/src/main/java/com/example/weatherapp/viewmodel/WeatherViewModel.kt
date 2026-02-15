package com.example.weatherapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.weatherapp.R
import com.example.weatherapp.api.WeatherService
import com.example.weatherapp.model.PwsHistoryObservation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val _weatherForecast = MutableLiveData<String>()
    val weatherForecast: LiveData<String> = _weatherForecast

    private val _currentConditions = MutableLiveData<String>()
    val currentConditions: LiveData<String> = _currentConditions

    private val _locationName = MutableLiveData<String>()
    val locationName: LiveData<String> = _locationName

    private val _chartHistory = MutableLiveData<List<PwsHistoryObservation>>()
    val chartHistory: LiveData<List<PwsHistoryObservation>> = _chartHistory

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val service: WeatherService

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.weather.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        service = retrofit.create(WeatherService::class.java)
    }

    fun fetchWeather(stationId: String, isMetric: Boolean) {
        _isLoading.value = true
        _errorMessage.value = null
        
        // Clear previous data or keep it? Checking existing behavior: existing clears it.
        // We will clear it to show loading state effectively.
        _weatherForecast.value = getAppString(R.string.loading)
        _currentConditions.value = getAppString(R.string.loading)
        _chartHistory.value = emptyList()

        val unitCode = if (isMetric) "m" else "e"
        val apiKey = "d183be15278740c583be15278740c504"

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Get PWS Observations
                val pwsResponse = service.getPwsObservations(stationId = stationId, units = unitCode, apiKey = apiKey)

                if (pwsResponse.isSuccessful && pwsResponse.body()?.observations?.isNotEmpty() == true) {
                    val observation = pwsResponse.body()!!.observations!![0]
                    val lat = observation.lat
                    val lon = observation.lon
                    val neighborhood = observation.neighborhood ?: stationId

                    // Update Location
                    _locationName.postValue(neighborhood)

                    // Format Current Conditions
                    val data = if (isMetric) observation.metric else observation.imperial
                    val tempUnit = if (isMetric) "°C" else "°F"
                    val speedUnit = if (isMetric) "km/h" else "mph"
                    
                    if (data != null) {
                        val precipUnit = if (isMetric) "mm/hr" else "in"
                        val windDir = getWindDirection(observation.winddir)

                        // Calculate Feels Like
                        val tempVal = data.temp ?: 0.0
                        val heatIndexVal = data.heatIndex
                        val windChillVal = data.windChill
                        
                        val feelsLike = when {
                            heatIndexVal != null && heatIndexVal > tempVal -> heatIndexVal
                            windChillVal != null && windChillVal < tempVal -> windChillVal
                            else -> tempVal
                        }

                        val tempFormatted = String.format(Locale.getDefault(), "%.1f", tempVal)
                        val feelsLikeFormatted = String.format(Locale.getDefault(), "%.1f", feelsLike)
                        val windSpeedFormatted = String.format(Locale.getDefault(), "%.2f", data.windSpeed ?: 0.0)
                        val windGustFormatted = String.format(Locale.getDefault(), "%.2f", data.windGust ?: 0.0)
                        val precipFormatted = String.format(Locale.getDefault(), "%.2f", data.precipTotal ?: 0.0)

                        val conditionText = "Temp: ${tempFormatted}$tempUnit\n" +
                                            "Sensación: ${feelsLikeFormatted}$tempUnit\n" +
                                            "Viento: ${windSpeedFormatted} $speedUnit $windDir (Ráfaga: ${windGustFormatted})\n" +
                                            "Precip: ${precipFormatted} $precipUnit"
                        _currentConditions.postValue(conditionText)
                    }

                    // 2. Get Forecast using Lat/Lon from PWS
                    val geocode = "$lat,$lon"
                    val forecastResponse = service.get5DayForecast(geocode = geocode, units = unitCode, language = "es-ES", apiKey = apiKey)

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
                            _weatherForecast.postValue(sb.toString())
                        } else {
                            _weatherForecast.postValue(getAppString(R.string.station_not_found))
                        }
                    } else {
                        _weatherForecast.postValue("${getAppString(R.string.error_prefix)} Forecast ${forecastResponse.code()}")
                    }

                    // 3. Get History for Graph
                    try {
                        val historyResponse = service.getPwsHistory(stationId = stationId, units = unitCode, apiKey = apiKey)
                        if (historyResponse.isSuccessful) {
                            val history = historyResponse.body()?.observations
                            if (!history.isNullOrEmpty()) {
                                _chartHistory.postValue(history!!)
                                
                                // Calculate 24h Precip Total
                                val precipSum = history.sumOf { 
                                    (if (isMetric) it.metric?.precipTotal else it.imperial?.precipTotal) ?: 0.0 
                                }
                                val precipUnit = if (isMetric) "mm" else "in"
                                
                                // Append to current conditions
                                // We need to be careful with concurrency here. We should construct the full string or update it.
                                // Given simple nature, we'll just post a new value based on what we calculated.
                                // But _currentConditions might not be updated yet on the main thread if we just posted it.
                                // So we reconstruct the string or use a synchronized approach.
                                // Let's simplify: We format the base string above. We append here.
                                
                                val currentBase = if (data != null) {
                                    val precipUnitCur = if (isMetric) "mm/hr" else "in"
                                    val windDir = getWindDirection(observation.winddir)

                                    // Calculate Feels Like
                                    val tempVal = data.temp ?: 0.0
                                    val heatIndexVal = data.heatIndex
                                    val windChillVal = data.windChill
                                    
                                    val feelsLike = when {
                                        heatIndexVal != null && heatIndexVal > tempVal -> heatIndexVal
                                        windChillVal != null && windChillVal < tempVal -> windChillVal
                                        else -> tempVal
                                    }

                                    val tempFormatted = String.format(Locale.getDefault(), "%.1f", tempVal)
                                    val feelsLikeFormatted = String.format(Locale.getDefault(), "%.1f", feelsLike)
                                    val windSpeedFormatted = String.format(Locale.getDefault(), "%.2f", data.windSpeed ?: 0.0)
                                    val windGustFormatted = String.format(Locale.getDefault(), "%.2f", data.windGust ?: 0.0)
                                    val precipFormatted = String.format(Locale.getDefault(), "%.2f", data.precipTotal ?: 0.0)

                                    "Temp: ${tempFormatted}$tempUnit\n" +
                                    "Sensación: ${feelsLikeFormatted}$tempUnit\n" +
                                    "Viento: ${windSpeedFormatted} $speedUnit $windDir (Ráfaga: ${windGustFormatted})\n" +
                                    "Precip: ${precipFormatted} $precipUnitCur"
                                } else ""

                                val fullConditionText = "$currentBase\nPrecip (24h): ${String.format(Locale.getDefault(), "%.2f", precipSum)} $precipUnit"
                                _currentConditions.postValue(fullConditionText)
                            } else {
                                _chartHistory.postValue(emptyList())
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                } else {
                    _currentConditions.postValue(getAppString(R.string.station_not_found))
                    _weatherForecast.postValue("${getAppString(R.string.error_prefix)} PWS ${pwsResponse.code()}")
                }
            } catch (e: Exception) {
                _weatherForecast.postValue("${getAppString(R.string.error_prefix)} ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    private fun getAppString(resId: Int): String {
        return getApplication<Application>().getString(resId)
    }

    private fun getWindDirection(degrees: Int?): String {
        if (degrees == null) return ""
        val directions = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSO", "SO", "OSO", "O", "ONO", "NO", "NNO")
        val index = ((degrees / 22.5) + 0.5).toInt() % 16
        return directions[index]
    }
}
