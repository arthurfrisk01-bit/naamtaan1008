package com.naamtaan1008.app.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.naamtaan1008.app.R
import com.naamtaan1008.app.data.model.Show

class ShowDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_detail)

        val show = readShow(intent)
        findViewById<ImageButton>(R.id.backBtn).setOnClickListener { finish() }
        findViewById<TextView>(R.id.title).text = show.title
        findViewById<TextView>(R.id.showTitle).text = show.title
        findViewById<ImageView>(R.id.poster).load(show.poster) {
            placeholder(R.color.zakka_cream)
            error(R.color.zakka_cream)
        }
        findViewById<TextView>(R.id.body).text = buildDetailBody(show)
    }

    private fun buildDetailBody(show: Show): String = buildString {
        append("时间  ").append(show.showTime).append("\n\n")
        if (show.city.isNotBlank() || show.venue.isNotBlank()) {
            append("地点  ")
            if (show.city.isNotBlank()) append(show.city).append(" · ")
            append(show.venue).append("\n\n")
        }
        if (show.price.isNotBlank()) append("票价  ").append(show.price).append("\n\n")
        if (show.performers.isNotBlank()) append("阵容  ").append(show.performers).append("\n\n")
        if (show.url.isNotBlank()) append("购票  ").append(show.url)
    }

    companion object {
        private const val EXTRA = "show"

        fun start(context: Context, show: Show) {
            context.startActivity(
                Intent(context, ShowDetailActivity::class.java)
                    .putExtra(EXTRA, show as java.io.Serializable)
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun readShow(intent: Intent): Show {
        val ser = intent.getSerializableExtra(EXTRA)
        return (ser as? Show) ?: Show()
    }
}
