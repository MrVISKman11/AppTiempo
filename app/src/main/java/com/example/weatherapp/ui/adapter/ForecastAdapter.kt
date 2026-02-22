package com.example.weatherapp.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.R
import com.example.weatherapp.model.DailyForecast
import java.util.Locale

class ForecastAdapter : RecyclerView.Adapter<ForecastAdapter.ForecastViewHolder>() {

    private val forecastList = mutableListOf<DailyForecast>()
    private var tempUnitPref = "C"

    fun setForecasts(forecasts: List<DailyForecast>, tempUnitPref: String) {
        this.tempUnitPref = tempUnitPref
        forecastList.clear()
        forecastList.addAll(forecasts)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForecastViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_forecast_day, parent, false)
        return ForecastViewHolder(view)
    }

    override fun onBindViewHolder(holder: ForecastViewHolder, position: Int) {
        val forecast = forecastList[position]
        holder.bind(forecast, tempUnitPref) {
            // Toggle expanded state
            forecast.isExpanded = !forecast.isExpanded
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = forecastList.size

    class ForecastViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val llHeader: LinearLayout = itemView.findViewById(R.id.llHeader)
        private val tvDayOfWeek: TextView = itemView.findViewById(R.id.tvDayOfWeek)
        private val tvMaxMinTemp: TextView = itemView.findViewById(R.id.tvMaxMinTemp)
        private val ivExpandIcon: ImageView = itemView.findViewById(R.id.ivExpandIcon)
        private val llDetails: LinearLayout = itemView.findViewById(R.id.llDetails)
        
        private val tvNarrative: TextView = itemView.findViewById(R.id.tvNarrative)
        private val tvRain: TextView = itemView.findViewById(R.id.tvRain)
        private val tvMoon: TextView = itemView.findViewById(R.id.tvMoon)
        private val tvSunrise: TextView = itemView.findViewById(R.id.tvSunrise)
        private val tvSunset: TextView = itemView.findViewById(R.id.tvSunset)

        fun bind(forecast: DailyForecast, tempUnitPref: String, onHeaderClick: () -> Unit) {
            tvDayOfWeek.text = forecast.dayOfWeek
            
            val tempUnit = if (tempUnitPref == "C") "°C" else "°F"
            
            var maxT = forecast.maxTemp
            var minT = forecast.minTemp
            if (tempUnitPref == "F") {
                if (maxT != null) maxT = (maxT * 9/5) + 32
                if (minT != null) minT = (minT * 9/5) + 32
            }

            tvMaxMinTemp.text = "${maxT ?: "--"}$tempUnit / ${minT ?: "--"}$tempUnit"

            tvNarrative.text = forecast.narrative.replace(Regex("(?<=\\d)\\s*C\\b"), "ºC").replace(Regex("(?<=\\d)\\s*F\\b"), "ºF")

            val context = itemView.context
            // We default to mm if not specified otherwise in adapter (User can't customize this explicitly in forecast view without further changes, but we'll leave as mm or standard)
            tvRain.text = "${context.getString(R.string.label_rain)}: ${forecast.qpf ?: "0.0"} mm"
            tvMoon.text = "${context.getString(R.string.label_moon_phase)}: ${forecast.moonPhase ?: "--"}"
            
            // Format Sunrise and Sunset time slightly if it includes timezone offset e.g. "2026-02-22T07:58:55+0100"
            // We can just try to extract the HH:mm portion, or show as is if unable.
            tvSunrise.text = "${context.getString(R.string.label_sunrise)}: ${extractTime(forecast.sunrise)}"
            tvSunset.text = "${context.getString(R.string.label_sunset)}: ${extractTime(forecast.sunset)}"

            if (forecast.isExpanded) {
                llDetails.visibility = View.VISIBLE
                ivExpandIcon.rotation = 180f
            } else {
                llDetails.visibility = View.GONE
                ivExpandIcon.rotation = 0f
            }

            llHeader.setOnClickListener { onHeaderClick() }
        }
        
        private fun extractTime(timeStr: String?): String {
            if (timeStr == null) return "--"
            // Example format: 2026-02-22T07:58:55+0100 -> we want 07:58
            try {
                val tIndex = timeStr.indexOf("T")
                if (tIndex != -1 && timeStr.length >= tIndex + 6) {
                    return timeStr.substring(tIndex + 1, tIndex + 6)
                }
            } catch (e: Exception) {
               // Ignore
            }
            return timeStr
        }
    }
}
