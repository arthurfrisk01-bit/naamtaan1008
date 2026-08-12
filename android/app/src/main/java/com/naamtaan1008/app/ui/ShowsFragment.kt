package com.naamtaan1008.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.naamtaan1008.app.R
import com.naamtaan1008.app.data.Repository
import com.naamtaan1008.app.data.model.Show
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShowsFragment : Fragment(), ShowAdapterClickListener {

    private lateinit var recycler: androidx.recyclerview.widget.RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private val adapter: ShowAdapter = ShowAdapter(this::onShowClick)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_shows, container, false)
        recycler = v.findViewById(R.id.showsRecycler)
        swipeRefresh = v.findViewById(R.id.showsSwipeRefresh)
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
                val shows = withContext(Dispatchers.IO) { Repository.fetchShows() }
                adapter.submit(shows)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), R.string.error_network, Toast.LENGTH_SHORT).show()
            } finally {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    override fun onShowClick(show: Show) {
        ShowDetailActivity.start(requireContext(), show)
    }
}
