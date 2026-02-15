package com.example.weatherapp.data

import android.content.Context
import android.content.SharedPreferences
import com.example.weatherapp.model.FavoriteStation
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class FavoritesRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val key = "favorites_list"

    fun getFavorites(): MutableList<FavoriteStation> {
        val json = prefs.getString(key, null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<FavoriteStation>>() {}.type
        return gson.fromJson(json, type)
    }

    fun addFavorite(station: FavoriteStation) {
        val list = getFavorites()
        // Remove if exists to update/avoid duplicates by ID
        list.removeAll { it.id == station.id }
        list.add(station)
        saveList(list)
    }

    fun removeFavorite(stationId: String) {
        val list = getFavorites()
        list.removeAll { it.id == stationId }
        saveList(list)
    }

    private fun saveList(list: List<FavoriteStation>) {
        val json = gson.toJson(list)
        prefs.edit().putString(key, json).apply()
    }
    
    fun isFavorite(stationId: String): Boolean {
        return getFavorites().any { it.id == stationId }
    }
}
