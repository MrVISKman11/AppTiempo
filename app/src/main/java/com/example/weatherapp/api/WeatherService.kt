package com.example.weatherapp.api

import com.example.weatherapp.model.PwsResponse
import com.example.weatherapp.model.WeatherResponse
import com.example.weatherapp.model.PwsHistoryResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherService {
    @GET("v3/wx/forecast/daily/5day")
    suspend fun get5DayForecast(
        @Query("geocode") geocode: String,
        @Query("format") format: String = "json",
        @Query("units") units: String,
        @Query("language") language: String = "es-ES",
        @Query("apiKey") apiKey: String
    ): Response<WeatherResponse>

    @GET("v2/pws/observations/all/1day")
    suspend fun getPwsHistory(
        @Query("stationId") stationId: String,
        @Query("format") format: String = "json",
        @Query("units") units: String, // e = imperial, m = metric
        @Query("apiKey") apiKey: String
    ): Response<PwsHistoryResponse>

    @GET("v2/pws/observations/current")
    suspend fun getPwsObservations(
        @Query("stationId") stationId: String,
        @Query("format") format: String = "json",
        @Query("units") units: String = "e",
        @Query("apiKey") apiKey: String
    ): Response<PwsResponse>
}
