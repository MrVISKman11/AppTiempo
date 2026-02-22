package com.example.weatherapp.ui

import android.content.Context
import android.widget.TextView
import com.example.weatherapp.R
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.text.DecimalFormat

class CustomMarkerView(context: Context, layoutResource: Int) : MarkerView(context, layoutResource) {

    private val tvContent: TextView = findViewById(R.id.tvContent)
    private val format = DecimalFormat("###.0")

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e != null) {
            var text = format.format(e.y.toDouble())
            
            // Try to extract the unit from the dataset label if it exists in parenthesis (e.g. "°C" from "Temperatura (°C)")
            if (highlight != null && chartView != null) {
                val dataSet = chartView.data?.getDataSetByIndex(highlight.dataSetIndex)
                if (dataSet != null && dataSet.label != null) {
                    val label = dataSet.label
                    if (label.contains("(") && label.contains(")")) {
                        val unit = label.substringAfter("(").substringBefore(")")
                        text = "$text $unit"
                    }
                }
            }
            tvContent.text = text
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        // Center the marker horizontally and position it slightly above the point
        return MPPointF(-(width / 2f), -height.toFloat() - 10f)
    }
}
