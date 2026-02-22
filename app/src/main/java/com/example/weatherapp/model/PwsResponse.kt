package com.example.weatherapp.model

import com.google.gson.annotations.SerializedName

data class PwsResponse(
    @SerializedName("observations") val observations: List<PwsObservation>?
)

data class PwsObservation(
    @SerializedName("stationID") val stationID: String,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lon") val lon: Double,
    @SerializedName("neighborhood") val neighborhood: String?,
    @SerializedName("obsTimeLocal") val obsTimeLocal: String?,
    @SerializedName("winddir") val winddir: Int?,
    @SerializedName("imperial") val imperial: PwsUnits?, // Used when units=e
    @SerializedName("metric") val metric: PwsUnits?,     // Used when units=m
    @SerializedName("humidity") val humidity: Double?
)

data class PwsUnits(
    @SerializedName("temp") val temp: Double?,
    @SerializedName("heatIndex") val heatIndex: Double?,
    @SerializedName("dewpt") val dewpt: Double?,
    @SerializedName("windChill") val windChill: Double?,
    @SerializedName("windSpeed") val windSpeed: Double?,
    @SerializedName("windGust") val windGust: Double?,
    @SerializedName("pressure") val pressure: Double?,
    @SerializedName("precipRate") val precipRate: Double?,
    @SerializedName("precipTotal") val precipTotal: Double?,
    @SerializedName("elev") val elev: Double?
)
