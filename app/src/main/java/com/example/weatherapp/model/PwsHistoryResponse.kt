package com.example.weatherapp.model

data class PwsHistoryResponse(
    val observations: List<PwsHistoryObservation>?
)

data class PwsHistoryObservation(
    val epoch: Long,
    val metric: PwsHistoryUnits?, // For Celsius
    val imperial: PwsHistoryUnits? // For Fahrenheit
)

data class PwsHistoryUnits(
    val tempAvg: Double?,
    val windspeedAvg: Double?,
    val precipTotal: Double?
)
