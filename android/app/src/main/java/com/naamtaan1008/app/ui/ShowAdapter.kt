package com.naamtaan1008.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.naamtaan1008.app.R
import com.naamtaan1008.app.data.model.Show

interface ShowAdapterClickListener {
    fun onShowClick(show: Show)
}

class ShowAdapter(
    private val onClick: (Show) -> Unit
) : RecyclerView.Adapter<ShowAdapter.VH>() {

    private val items = mutableListOf<Show>()

    fun submit(shows: List<Show>) {
        items.clear()
        items.addAll(shows)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_show, parent, false)
        return VH(v, onClick)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    class VH(
        v: View,
        onClick: (Show) -> Unit
    ) : RecyclerView.ViewHolder(v) {
        private val poster: ImageView = v.findViewById(R.id.poster)
        private val statusTag: TextView = v.findViewById(R.id.statusTag)
        private val title: TextView = v.findViewById(R.id.title)
        private val time: TextView = v.findViewById(R.id.time)
        private val venue: TextView = v.findViewById(R.id.venue)
        private val price: TextView = v.findViewById(R.id.price)

        init { v.setOnClickListener { onClick.invoke(current) } }

        private var current: Show = Show()

        fun bind(show: Show) {
            current = show
            poster.load(show.poster) {
                placeholder(R.color.zakka_cream)
                error(R.color.zakka_cream)
            }
            statusTag.text = if (show.soldOut) "已售罄" else ""
            title.text = show.title
            time.text = show.showTime
            venue.text = buildString {
                if (show.city.isNotBlank()) { append(show.city); append(" · ") }
                append(show.venue)
            }
            price.text = show.price
        }
    }
}
