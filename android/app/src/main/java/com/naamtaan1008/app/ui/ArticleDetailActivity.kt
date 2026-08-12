package com.naamtaan1008.app.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.naamtaan1008.app.R
import com.naamtaan1008.app.data.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ArticleDetailActivity : AppCompatActivity() {

    private lateinit var progress: ProgressBar
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_article_detail)

        val articleId = intent.getStringExtra(EXTRA_ID) ?: ""
        val articleTitle = intent.getStringExtra(EXTRA_TITLE) ?: ""

        findViewById<ImageButton>(R.id.backBtn).setOnClickListener { finish() }
        findViewById<TextView>(R.id.title).text = articleTitle

        progress = findViewById(R.id.progress)
        webView = findViewById(R.id.webView)
        setupWebView()

        if (articleId.isBlank()) {
            Toast.makeText(this, R.string.error_network, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch {
            try {
                val article = withContext(Dispatchers.IO) { Repository.fetchArticle(articleId) }
                findViewById<TextView>(R.id.title).text = article.title
                renderArticle(article.title, article.date, article.category, article.content)
            } catch (e: Exception) {
                Toast.makeText(this@ArticleDetailActivity, R.string.error_network, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.webViewClient = WebViewClient()
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = false
        settings.defaultTextEncodingName = "UTF-8"
    }

    /** Render the article body as styled HTML inside the zakka-themed page shell. */
    private fun renderArticle(title: String, date: String, category: String, content: String) {
        val html = buildString {
            append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">")
            append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
            append("<style>")
            append("body{margin:0;padding:20px 22px 40px;background:#FAF3E0;color:#3D3831;")
            append("font-family:-apple-system,'Noto Serif SC','PingFang SC','Microsoft YaHei',serif;line-height:1.9;font-size:16px;}")
            append("h1{font-size:22px;line-height:1.5;margin:0 0 6px;color:#3D3831;}")
            append(".meta{font-size:13px;color:#8A6D4B;margin-bottom:18px;}")
            append("h3,h4{margin:22px 0 8px;color:#3D3831;}")
            append("p{margin:10px 0;}")
            append("strong{color:#3D3831;}u{text-decoration-thickness:2px;}")
            append("img{max-width:100%;height:auto;border-radius:8px;}")
            append("a{color:#C77D5E;}")
            append("blockquote{border-left:3px solid #C9A227;margin:14px 0;padding:4px 14px;color:#8A6D4B;}")
            append("</style></head><body>")
            append("<h1>").append(htmlEscape(title)).append("</h1>")
            append("<div class=\"meta\">").append(htmlEscape(date))
            if (category.isNotBlank()) append(" · ").append(htmlEscape(category))
            append("</div>")
            append(content)
            append("</body></html>")
        }
        progress.visibility = View.GONE
        webView.visibility = View.VISIBLE
        webView.loadDataWithBaseURL(
            "https://naamtaan1008.com/", html, "text/html", "UTF-8", null
        )
    }

    private fun htmlEscape(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    companion object {
        private const val EXTRA_ID = "article_id"
        private const val EXTRA_TITLE = "article_title"

        fun start(context: Context, id: String, title: String) {
            context.startActivity(
                Intent(context, ArticleDetailActivity::class.java)
                    .putExtra(EXTRA_ID, id)
                    .putExtra(EXTRA_TITLE, title)
            )
        }
    }
}
