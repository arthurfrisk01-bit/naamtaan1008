package com.naamtaan1008.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.naamtaan1008.app.R
import com.naamtaan1008.app.data.Repository
import com.naamtaan1008.app.data.model.Show
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private lateinit var container: LinearLayout
    private lateinit var swipeRefresh: androidx.swiperefreshlayout.widget.SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_home, container, false)
        this.container = v.findViewById(R.id.homeContainer)
        this.swipeRefresh = v.findViewById(R.id.homeSwipeRefresh)
        swipeRefresh.setColorSchemeResources(R.color.zakka_brown, R.color.zakka_clay)
        swipeRefresh.setOnRefreshListener { load() }
        load()
        return v
    }

    private fun load() {
        viewLifecycleOwner.lifecycleScope.launch {
            swipeRefresh.isRefreshing = true
            container.removeAllViews()
            val title = sectionTitle("平和日常")
            val introTv = bodyText("")
            val focusTitle = sectionTitle("焦点演出")
            val focusLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
            }
            container.addView(title)
            container.addView(introTv)
            try {
                withContext(Dispatchers.IO) {
                    Repository.fetchHomeIntro()
                }.let { introTv.text = it.ifBlank { "珠三角独立音乐场景平台" } }
            } catch (_: Exception) {
            }
            container.addView(emptyState("加载中…"))
            try {
                val focus = withContext(Dispatchers.IO) { Repository.fetchFocusShows() }
                container.removeViews(container.childCount - 1, 1)
                if (focus.isEmpty()) {
                    container.addView(emptyState("暂无焦点演出"))
                } else {
                    container.addView(focusTitle)
                    focus.forEach { show ->
                        focusLayout.addView(focusShowCard(show))
                    }
                    container.addView(focusLayout)
                }
            } catch (e: Exception) {
                container.removeViews(container.childCount - 1, 1)
                container.addView(emptyState(getString(R.string.error_network)))
            } finally {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun focusShowCard(show: Show): View {
        val ctx = requireContext()
        return MaterialCardView(ctx).apply {
            radius = resources.getDimension(R.dimen.card_radius)
            setCardBackgroundColor(resources.getColor(R.color.item_background, null))
            useCompatPadding = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
            addView(TextView(ctx).apply {
                setTextColor(resources.getColor(R.color.zakka_ink, null))
                textSize = 15f
                setPadding(dp(12), dp(10), dp(12), dp(4))
                text = show.title
            })
            addView(TextView(ctx).apply {
                setTextColor(resources.getColor(R.color.zakka_brown, null))
                textSize = 13f
                setPadding(dp(12), 0, dp(12), dp(4))
                text = "${show.showTime} · ${show.venue}"
            })
            addView(TextView(ctx).apply {
                setTextColor(resources.getColor(R.color.zakka_clay, null))
                textSize = 13f
                setPadding(dp(12), 0, dp(12), dp(12))
                text = show.price
            })
        }
    }

    private fun sectionTitle(text: String): TextView {
        return TextView(requireContext()).apply {
            setTextColor(resources.getColor(R.color.zakka_ink, null))
            textSize = 20f
            setPadding(0, dp(4), 0, dp(8))
            text = text
        }
    }

    private fun bodyText(text: String): TextView {
        return TextView(requireContext()).apply {
            setTextColor(resources.getColor(R.color.zakka_brown, null))
            textSize = 14f
            lineSpacingExtra = dp(3).toFloat()
            text = text
        }
    }

    private fun emptyState(text: String): TextView {
        return TextView(requireContext()).apply {
            setTextColor(resources.getColor(R.color.zakka_brown, null))
            textSize = 14f
            setPadding(dp(8), dp(24), dp(8), dp(24))
            gravity = android.view.Gravity.CENTER
            this.text = text
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
