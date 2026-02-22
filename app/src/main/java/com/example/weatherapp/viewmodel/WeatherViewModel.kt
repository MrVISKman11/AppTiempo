package com.example.weatherapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.weatherapp.R
import com.example.weatherapp.api.WeatherService
import com.example.weatherapp.model.PwsHistoryObservation
import com.example.weatherapp.model.DailyForecast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val _weatherForecast = MutableLiveData<List<DailyForecast>>()
    val weatherForecast: LiveData<List<DailyForecast>> = _weatherForecast

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

    var lastTempUnit = ""
    var lastSpeedUnit = ""
    var lastPrecipUnit = ""
    var lastLanguage = ""

    private val service: WeatherService

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.weather.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        service = retrofit.create(WeatherService::class.java)
    }

    fun fetchWeather(stationId: String, tempPref: String, speedPref: String, precipPref: String) {
        lastTempUnit = tempPref
        lastSpeedUnit = speedPref
        lastPrecipUnit = precipPref
        lastLanguage = Locale.getDefault().language
        
        _isLoading.value = true
        _errorMessage.value = null
        
        // Clear previous data or keep it? Checking existing behavior: existing clears it.
        // We will clear it to show loading state effectively.
        _weatherForecast.value = emptyList()
        _currentConditions.value = getAppString(R.string.loading)
        _chartHistory.value = emptyList()

        // We always fetch 'm' (metric) from the API as our baseline, then convert locally.
        val unitCode = "m"
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

                    // Format Current Conditions (Base is Metric)
                    val data = observation.metric
                    val tempUnit = if (tempPref == "C") "°C" else "°F"
                    val speedUnit = if (speedPref == "kmh") "km/h" else "mph"
                    val precipUnitStr = if (precipPref == "mm") "mm/hr" else "in"
                    
                    if (data != null) {
                        val windDir = getWindDirection(observation.winddir)

                        // Temperature Logic
                        var tempVal = data.temp ?: 0.0
                        var heatIndexVal = data.heatIndex
                        var windChillVal = data.windChill
                        
                        var feelsLike = when {
                            heatIndexVal != null && heatIndexVal > tempVal -> heatIndexVal
                            windChillVal != null && windChillVal < tempVal -> windChillVal
                            else -> tempVal
                        }

                        if (tempPref == "F") {
                            tempVal = (tempVal * 9/5) + 32
                            feelsLike = (feelsLike * 9/5) + 32
                        }

                        // Speed Logic
                        var windSpeed = data.windSpeed ?: 0.0
                        var windGust = data.windGust ?: 0.0
                        
                        if (speedPref == "mph") {
                            windSpeed /= 1.60934
                            windGust /= 1.60934
                        }

                        // Precip Logic
                        var precipTotal = data.precipTotal ?: 0.0
                        if (precipPref == "in") {
                            precipTotal /= 25.4
                        }

                        val tempFormatted = String.format(Locale.getDefault(), "%.1f", tempVal)
                        val feelsLikeFormatted = String.format(Locale.getDefault(), "%.1f", feelsLike)
                        val windSpeedFormatted = String.format(Locale.getDefault(), "%.2f", windSpeed)
                        val windGustFormatted = String.format(Locale.getDefault(), "%.2f", windGust)
                        val precipFormatted = String.format(Locale.getDefault(), "%.2f", precipTotal)

                        val conditionText = "Temp: ${tempFormatted}$tempUnit\n" +
                                            "${getAppString(R.string.graph_feels_like)}: ${feelsLikeFormatted}$tempUnit\n" +
                                            "${getAppString(R.string.graph_wind)}: ${windSpeedFormatted} $speedUnit $windDir (${getAppString(R.string.label_gust)}: ${windGustFormatted} $speedUnit)\n" +
                                            "${getAppString(R.string.graph_precip)}: ${precipFormatted} $precipUnitStr"
                        _currentConditions.postValue(conditionText)
                    }

                    // 2. Get Forecast using Lat/Lon from PWS
                    val geocode = "$lat,$lon"
                    val langCode = if (Locale.getDefault().language == "en") "en-US" else "es-ES"
                    val forecastResponse = service.get5DayForecast(geocode = geocode, units = unitCode, language = langCode, apiKey = apiKey)

                    if (forecastResponse.isSuccessful) {
                        val weather = forecastResponse.body()
                        if (weather != null && weather.daysOfWeek.isNotEmpty()) {
                            val forecastList = mutableListOf<DailyForecast>()
                            val count = weather.daysOfWeek.size
                            for (i in 0 until count) {
                                forecastList.add(
                                    DailyForecast(
                                        dayOfWeek = weather.daysOfWeek.getOrNull(i) ?: "Día",
                                        maxTemp = weather.maxTemps.getOrNull(i),
                                        minTemp = weather.minTemps.getOrNull(i),
                                        narrative = weather.narratives.getOrNull(i) ?: "",
                                        qpf = weather.qpf?.getOrNull(i),
                                        sunrise = weather.sunriseTimeLocal?.getOrNull(i),
                                        sunset = weather.sunsetTimeLocal?.getOrNull(i),
                                        moonPhase = weather.moonPhase?.getOrNull(i)
                                    )
                                )
                            }
                            _weatherForecast.postValue(forecastList)
                        } else {
                            _errorMessage.postValue(getAppString(R.string.station_not_found))
                            _weatherForecast.postValue(emptyList())
                        }
                    } else {
                        _errorMessage.postValue("${getAppString(R.string.error_prefix)} Forecast ${forecastResponse.code()}")
                        _weatherForecast.postValue(emptyList())
                    }

                    // 3. Get History for Graph
                    try {
                        val historyResponse = service.getPwsHistory(stationId = stationId, units = unitCode, apiKey = apiKey)
                        if (historyResponse.isSuccessful) {
                            val history = historyResponse.body()?.observations
                            if (!history.isNullOrEmpty()) {
                                _chartHistory.postValue(history!!)
                                
                                // Calculate 24h Precip Total (Base Metric)
                                var precipSum = history.sumOf { 
                                    it.metric?.precipTotal ?: 0.0 
                                }
                                val precipUnitCur = if (precipPref == "mm") "mm/hr" else "in"
                                
                                if (precipPref == "in") {
                                    precipSum /= 25.4
                                }

                                val currentBase = if (data != null) {
                                    val windDir = getWindDirection(observation.winddir)

                                    var tempVal = data.temp ?: 0.0
                                    var heatIndexVal = data.heatIndex
                                    var windChillVal = data.windChill
                                    
                                    var feelsLike = when {
                                        heatIndexVal != null && heatIndexVal > tempVal -> heatIndexVal
                                        windChillVal != null && windChillVal < tempVal -> windChillVal
                                        else -> tempVal
                                    }

                                    if (tempPref == "F") {
                                        tempVal = (tempVal * 9/5) + 32
                                        feelsLike = (feelsLike * 9/5) + 32
                                    }

                                    var windSpeed = data.windSpeed ?: 0.0
                                    var windGust = data.windGust ?: 0.0
                                    
                                    if (speedPref == "mph") {
                                        windSpeed /= 1.60934
                                        windGust /= 1.60934
                                    }

                                    var precipTotal = data.precipTotal ?: 0.0
                                    if (precipPref == "in") {
                                        precipTotal /= 25.4
                                    }

                                    val tempFormatted = String.format(Locale.getDefault(), "%.1f", tempVal)
                                    val feelsLikeFormatted = String.format(Locale.getDefault(), "%.1f", feelsLike)
                                    val windSpeedFormatted = String.format(Locale.getDefault(), "%.2f", windSpeed)
                                    val windGustFormatted = String.format(Locale.getDefault(), "%.2f", windGust)
                                    val precipFormatted = String.format(Locale.getDefault(), "%.2f", precipTotal)

                                    "Temp: ${tempFormatted}$tempUnit\n" +
                                    "${getAppString(R.string.graph_feels_like)}: ${feelsLikeFormatted}$tempUnit\n" +
                                    "${getAppString(R.string.graph_wind)}: ${windSpeedFormatted} $speedUnit $windDir (${getAppString(R.string.label_gust)}: ${windGustFormatted} $speedUnit)\n" +
                                    "${getAppString(R.string.graph_precip)}: ${precipFormatted} $precipUnitCur"
                                } else ""

                                val humidityVal = observation.humidity ?: 0.0
                                val humidityStr = String.format(Locale.getDefault(), "%.1f", humidityVal)
                                val precip24Unit = if (precipPref == "mm") "mm" else "in"
                                val fullConditionText = "$currentBase\n${getAppString(R.string.graph_precip)} (24h): ${String.format(Locale.getDefault(), "%.2f", precipSum)} $precip24Unit\n${getAppString(R.string.label_humidity)}: $humidityStr%"
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
                    _errorMessage.postValue("${getAppString(R.string.error_prefix)} PWS ${pwsResponse.code()}")
                    _weatherForecast.postValue(emptyList())
                }
            } catch (e: Exception) {
                _errorMessage.postValue("${getAppString(R.string.error_prefix)} ${e.message}")
                _weatherForecast.postValue(emptyList())
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
