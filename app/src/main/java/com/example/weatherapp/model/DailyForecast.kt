package com.example.weatherapp.model

data class DailyForecast(
    var isExpanded: Boolean = false,
    val dayOfWeek: String,
    val maxTemp: Int?,
    val minTemp: Int?,
    val narrative: String,
    val qpf: Double?,
    val sunrise: String?,
    val sunset: String?,
    val moonPhase: String?
)
