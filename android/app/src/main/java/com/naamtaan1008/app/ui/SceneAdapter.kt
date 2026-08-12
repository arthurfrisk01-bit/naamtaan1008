package com.naamtaan1008.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.naamtaan1008.app.R
import com.naamtaan1008.app.data.model.SceneItem

interface SceneAdapterClickListener {
    fun onSceneClick(item: SceneItem)
}

class SceneAdapter(
    private val onClick: (SceneItem) -> Unit
) : RecyclerView.Adapter<SceneAdapter.VH>() {

    private val items = mutableListOf<SceneItem>()

    fun submit(list: List<SceneItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_scene, parent, false)
        return VH(v, onClick)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    class VH(
        v: View,
        onClick: (SceneItem) -> Unit
    ) : RecyclerView.ViewHolder(v) {
        private val name: TextView = v.findViewById(R.id.name)
        private val cityTag: TextView = v.findViewById(R.id.cityTag)
        private val subtitle: TextView = v.findViewById(R.id.subtitle)

        init { v.setOnClickListener { onClick.invoke(current) } }

        private var current: SceneItem = SceneItem()

        fun bind(item: SceneItem) {
            current = item
            name.text = item.name
            cityTag.text = item.city
            cityTag.visibility = if (item.city.isBlank()) View.GONE else View.VISIBLE
            subtitle.text = buildString {
                if (item.styles.isNotEmpty()) {
                    append(item.styles.joinToString(" / "))
                    if (item.intro.isNotBlank()) append("\n")
                }
                append(item.intro)
            }
        }
    }
}
