package com.example.weatherapp.model

data class PwsHistoryResponse(
    val observations: List<PwsHistoryObservation>?
)

data class PwsHistoryObservation(
    val epoch: Long,
    val metric: PwsHistoryUnits?, // For Celsius
    val imperial: PwsHistoryUnits?, // For Fahrenheit
    val solarRadiationHigh: Double?,
    val uvHigh: Double?
)

data class PwsHistoryUnits(
    val tempAvg: Double?,
    val windspeedAvg: Double?,
    val precipTotal: Double?,
    val pressureMax: Double?,
    val windchillAvg: Double?,
    val heatindexAvg: Double?
)
