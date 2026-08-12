package com.naamtaan1008.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.tabs.TabLayout
import com.naamtaan1008.app.R
import com.naamtaan1008.app.data.Repository
import com.naamtaan1008.app.data.model.SceneItem
import com.naamtaan1008.app.data.model.SceneResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SceneFragment : Fragment(), SceneAdapterClickListener {

    private lateinit var tabs: TabLayout
    private lateinit var recycler: androidx.recyclerview.widget.RecyclerView
    private lateinit var progress: ProgressBar
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private val adapter: SceneAdapter = SceneAdapter(this::onSceneClick)

    private val TYPES = listOf("bands", "venues", "rehearsals", "shops", "studios", "homestays")
    private var currentType = "bands"
    private var loadedResponse: SceneResponse? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_scene, container, false)
        tabs = v.findViewById(R.id.sceneTabs)
        recycler = v.findViewById(R.id.sceneRecycler)
        progress = v.findViewById(R.id.sceneLoading)
        swipeRefresh = v.findViewById(R.id.sceneSwipeRefresh)

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter
        swipeRefresh.setColorSchemeResources(R.color.zakka_brown, R.color.zakka_clay)
        swipeRefresh.setOnRefreshListener { load() }

        TYPES.forEach { type ->
            tabs.addTab(tabs.newTab().setText(Repository.sceneTypeLabel(type)))
        }
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentType = TYPES[tab.position]
                render()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        load()
        return v
    }

    private fun load() {
        viewLifecycleOwner.lifecycleScope.launch {
            progress.isVisible = true
            swipeRefresh.isRefreshing = true
            try {
                val resp = withContext(Dispatchers.IO) { Repository.fetchScene() }
                loadedResponse = resp
                render()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), R.string.error_network, Toast.LENGTH_SHORT).show()
            } finally {
                progress.isVisible = false
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun render() {
        val resp = loadedResponse ?: return
        adapter.submit(resp.data.listFor(currentType))
    }

    override fun onSceneClick(item: SceneItem) {
        SceneDetailActivity.start(requireContext(), currentType, item)
    }
}
