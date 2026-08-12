package com.naamtaan1008.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.naamtaan1008.app.R
import com.naamtaan1008.app.data.model.ArticleSummary

interface ArticleAdapterClickListener {
    fun onArticleClick(article: ArticleSummary)
}

class ArticleAdapter(
    private val onClick: (ArticleSummary) -> Unit
) : RecyclerView.Adapter<ArticleAdapter.VH>() {

    private val items = mutableListOf<ArticleSummary>()

    fun submit(articles: List<ArticleSummary>) {
        items.clear()
        items.addAll(articles)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_article, parent, false)
        return VH(v, onClick)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    class VH(
        v: View,
        onClick: (ArticleSummary) -> Unit
    ) : RecyclerView.ViewHolder(v) {
        private val categoryTag: TextView = v.findViewById(R.id.categoryTag)
        private val date: TextView = v.findViewById(R.id.date)
        private val title: TextView = v.findViewById(R.id.title)
        private val summary: TextView = v.findViewById(R.id.summary)

        init { v.setOnClickListener { onClick.invoke(current) } }

        private var current: ArticleSummary = ArticleSummary()

        fun bind(article: ArticleSummary) {
            current = article
            categoryTag.text = article.category.ifBlank { "文章" }
            date.text = article.date
            title.text = article.title
            summary.text = article.summary
        }
    }
}
