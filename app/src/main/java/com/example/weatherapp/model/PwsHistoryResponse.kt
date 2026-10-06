package com.example.weatherapp.model

import com.google.gson.annotations.SerializedName

data class PwsHistoryResponse(
    @SerializedName("observations") val observations: List<PwsHistoryObservation>?
)

data class PwsHistoryObservation(
    @SerializedName("epoch") val epoch: Long,
    @SerializedName("metric") val metric: PwsHistoryUnits?, // For Celsius
    @SerializedName("imperial") val imperial: PwsHistoryUnits?, // For Fahrenheit
    @SerializedName("solarRadiationHigh") val solarRadiationHigh: Double?,
    @SerializedName("uvHigh") val uvHigh: Double?
)

data class PwsHistoryUnits(
    @SerializedName("tempAvg") val tempAvg: Double?,
    @SerializedName("windspeedAvg") val windspeedAvg: Double?,
    @SerializedName("precipRate") val precipRate: Double?,
    @SerializedName("precipTotal") val precipTotal: Double?,
    @SerializedName("pressureMax") val pressureMax: Double?,
    @SerializedName("windchillAvg") val windchillAvg: Double?,
    @SerializedName("heatindexAvg") val heatindexAvg: Double?
)

