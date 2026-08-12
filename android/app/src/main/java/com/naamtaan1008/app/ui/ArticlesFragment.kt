package com.naamtaan1008.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.naamtaan1008.app.R
import com.naamtaan1008.app.data.Repository
import com.naamtaan1008.app.data.model.ArticleSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ArticlesFragment : Fragment(), ArticleAdapterClickListener {

    private lateinit var recycler: androidx.recyclerview.widget.RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private val adapter: ArticleAdapter = ArticleAdapter(this::onArticleClick)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_articles, container, false)
        recycler = v.findViewById(R.id.articlesRecycler)
        swipeRefresh = v.findViewById(R.id.articlesSwipeRefresh)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter
        swipeRefresh.setColorSchemeResources(R.color.zakka_brown, R.color.zakka_clay)
        swipeRefresh.setOnRefreshListener { load() }
        load()
        return v
    }

    private fun load() {
        viewLifecycleOwner.lifecycleScope.launch {
            swipeRefresh.isRefreshing = true
            try {
                val articles = withContext(Dispatchers.IO) { Repository.fetchArticles() }
                adapter.submit(articles)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), R.string.error_network, Toast.LENGTH_SHORT).show()
            } finally {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    override fun onArticleClick(article: ArticleSummary) {
        ArticleDetailActivity.start(requireContext(), article.id, article.title)
    }
}
