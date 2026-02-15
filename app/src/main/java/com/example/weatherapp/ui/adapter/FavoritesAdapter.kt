package com.example.weatherapp.ui.adapter

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.R
import com.example.weatherapp.model.FavoriteStation
import java.util.Collections

class FavoritesAdapter(
    private val favorites: MutableList<FavoriteStation>,
    private val onStationClick: (FavoriteStation) -> Unit,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit
) : RecyclerView.Adapter<FavoritesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvStationName)
        val tvId: TextView = view.findViewById(R.id.tvStationId)
        val ivDragHandle: ImageView = view.findViewById(R.id.ivDragHandle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val station = favorites[position]
        holder.tvName.text = station.name
        holder.tvId.text = "ID: ${station.id}"

        holder.itemView.setOnClickListener {
            onStationClick(station)
        }

        holder.ivDragHandle.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                onStartDrag(holder)
            }
            false
        }
    }

    override fun getItemCount() = favorites.size

    fun onItemMove(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(favorites, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(favorites, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
    }
    
    fun getItems(): List<FavoriteStation> = favorites
}
