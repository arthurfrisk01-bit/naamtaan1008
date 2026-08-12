package com.naamtaan1008.app.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.naamtaan1008.app.R
import com.naamtaan1008.app.data.Repository
import com.naamtaan1008.app.data.model.SceneItem

class SceneDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scene_detail)

        val type = intent.getStringExtra(EXTRA_TYPE) ?: "bands"
        val item = IntentExtra.from(intent) ?: SceneItem()
        val titleLbl = findViewById<TextView>(R.id.title)

        findViewById<ImageButton>(R.id.backBtn).setOnClickListener { finish() }
        titleLbl.text = item.name.ifBlank { Repository.sceneTypeLabel(type) }

        val body = findViewById<LinearLayout>(R.id.body)
        renderHeader(body, type, item)
        renderFields(body, type, item)
    }

    private fun renderHeader(body: LinearLayout, type: String, item: SceneItem) {
        // Type badge + name
        body.addView(labelView(Repository.sceneTypeLabel(type)))
        body.addView(textView(item.name, 20f, R.color.zakka_ink, true))
        if (item.city.isNotBlank()) {
            val city = textView(item.city, 13f, R.color.zakka_brown, false)
            city.setPadding(dp(2), dp(2), dp(2), dp(10))
            body.addView(city)
        }
        if (item.images.isNotEmpty() || item.image.isNotBlank()) {
            val img = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(200)
                ).apply { bottomMargin = dp(10) }
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            img.load(item.images.firstOrNull() ?: item.image) {
                placeholder(R.color.zakka_cream)
                error(R.color.zakka_cream)
            }
            body.addView(img)
        }
    }

    private fun renderFields(body: LinearLayout, type: String, item: SceneItem) {
        val fields = fieldList(type, item)
        fields.forEach { (label, value) ->
            if (value.isNotBlank()) {
                body.addView(labelView(label))
                body.addView(textView(value, 14f, R.color.zakka_ink, false))
                body.addView(spacer())
            }
        }
        if (item.styles.isNotEmpty()) {
            body.addView(labelView("风格"))
            body.addView(textView(item.styles.joinToString(" / "), 14f, R.color.zakka_ink, false))
            body.addView(spacer())
        }
    }

    /** Type → ordered display fields. Render by type, never by field existence. */
    private fun fieldList(type: String, item: SceneItem): List<Pair<String, String>> = when (type) {
        "bands" -> listOf(
            "简介" to item.intro,
            "成员" to item.members.joinToString("\n"),
            "联系" to item.contact
        )
        "venues" -> listOf(
            "地址" to item.address,
            "容量" to item.capacity,
            "设备" to item.equipment,
            "预订" to item.booking,
            "简介" to item.intro
        )
        "rehearsals" -> listOf(
            "地址" to item.address,
            "价格" to item.price,
            "设备" to item.equipment,
            "营业时间" to item.hours,
            "联系" to item.contact
        )
        "shops" -> listOf(
            "地址" to item.address,
            "营业时间" to item.hours,
            "联系" to item.contact,
            "简介" to item.intro,
            "设备" to item.equipment
        )
        "studios" -> listOf(
            "地址" to item.address,
            "价格" to item.price,
            "营业时间" to item.hours,
            "联系" to item.contact,
            "简介" to item.intro,
            "设备" to item.equipment
        )
        "homestays" -> listOf(
            "地址" to item.address,
            "价格" to item.price,
            "预订" to item.booking,
            "联系" to item.contact,
            "设备" to item.equipment
        )
        else -> emptyList()
    }

    private fun labelView(text: String): TextView =
        TextView(this).apply {
            setTextColor(resources.getColor(R.color.zakka_clay, null))
            textSize = 13f
            textStyle = android.graphics.Typeface.BOLD
            setPadding(dp(2), dp(8), dp(2), dp(2))
            this.text = text
        }

    private fun textView(text: String, size: Float, colorId: Int, bold: Boolean): TextView =
        TextView(this).apply {
            setTextColor(resources.getColor(colorId, null))
            textSize = size
            if (bold) typeface = android.graphics.Typeface.DEFAULT_BOLD
            lineSpacingExtra = dp(3).toFloat()
            setPadding(dp(2), 0, dp(2), dp(4))
            this.text = text
        }

    private fun spacer(): View =
        View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(4)
            )
        }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    companion object {
        private const val EXTRA_TYPE = "type"
        private const val EXTRA_ITEM = "item"

        fun start(context: Context, type: String, item: SceneItem) {
            context.startActivity(
                Intent(context, SceneDetailActivity::class.java)
                    .putExtra(EXTRA_TYPE, type)
                    .putExtra(EXTRA_ITEM, item)
            )
        }
    }

    private object IntentExtra {
        fun from(intent: Intent): SceneItem? =
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra(EXTRA_ITEM, SceneItem::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(EXTRA_ITEM)
            }
    }
}
