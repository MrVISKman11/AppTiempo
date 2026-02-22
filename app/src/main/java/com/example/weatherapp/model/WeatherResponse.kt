package com.example.weatherapp.model

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @SerializedName("calendarDayTemperatureMax") val maxTemps: List<Int?>,
    @SerializedName("calendarDayTemperatureMin") val minTemps: List<Int?>,
    @SerializedName("dayOfWeek") val daysOfWeek: List<String>,
    @SerializedName("narrative") val narratives: List<String>,
    @SerializedName("qpf") val qpf: List<Double?>,
    @SerializedName("sunriseTimeLocal") val sunriseTimeLocal: List<String?>,
    @SerializedName("sunsetTimeLocal") val sunsetTimeLocal: List<String?>,
    @SerializedName("moonPhase") val moonPhase: List<String?>
)
