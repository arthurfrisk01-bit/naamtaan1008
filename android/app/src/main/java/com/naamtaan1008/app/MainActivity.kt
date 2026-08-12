package com.naamtaan1008.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.naamtaan1008.app.ui.AboutFragment
import com.naamtaan1008.app.ui.ArticlesFragment
import com.naamtaan1008.app.ui.CommunityFragment
import com.naamtaan1008.app.ui.HomeFragment
import com.naamtaan1008.app.ui.SceneFragment
import com.naamtaan1008.app.ui.ShowsFragment

class MainActivity : AppCompatActivity() {

    private val homeFragment by lazy(LazyThreadSafetyMode.NONE) { HomeFragment() }
    private val showsFragment by lazy(LazyThreadSafetyMode.NONE) { ShowsFragment() }
    private val articlesFragment by lazy(LazyThreadSafetyMode.NONE) { ArticlesFragment() }
    private val sceneFragment by lazy(LazyThreadSafetyMode.NONE) { SceneFragment() }
    private val communityFragment by lazy(LazyThreadSafetyMode.NONE) { CommunityFragment() }
    private val aboutFragment by lazy(LazyThreadSafetyMode.NONE) { AboutFragment() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化社区登录态存储
        com.naamtaan1008.app.data.TokenStorage.init(applicationContext)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, homeFragment, TAG_HOME)
                .commit()
        }

        findViewById<BottomNavigationView>(R.id.bottomNav).setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> switchTo(TAG_HOME, homeFragment)
                R.id.nav_shows -> switchTo(TAG_SHOWS, showsFragment)
                R.id.nav_articles -> switchTo(TAG_ARTICLES, articlesFragment)
                R.id.nav_scene -> switchTo(TAG_SCENE, sceneFragment)
                R.id.nav_community -> switchTo(TAG_COMMUNITY, communityFragment)
                R.id.nav_about -> switchTo(TAG_ABOUT, aboutFragment)
                else -> false
            }
        }
    }

    private fun switchTo(tag: String, target: Fragment): Boolean {
        val fm = supportFragmentManager
        val currentTag = fm.fragments.lastOrNull()?.tag
        if (currentTag == tag) return true
        fm.beginTransaction().apply {
            fm.fragments.forEach { hide(it) }
            val existing = fm.findFragmentByTag(tag)
            if (existing == null) add(R.id.fragmentContainer, target, tag)
            else show(existing)
        }.commit()
        return true
    }

    companion object {
        private const val TAG_HOME = "home"
        private const val TAG_SHOWS = "shows"
        private const val TAG_ARTICLES = "articles"
        private const val TAG_SCENE = "scene"
        private const val TAG_COMMUNITY = "community"
        private const val TAG_ABOUT = "about"
    }
}
