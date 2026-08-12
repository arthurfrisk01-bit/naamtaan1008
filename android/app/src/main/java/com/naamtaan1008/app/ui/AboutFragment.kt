package com.naamtaan1008.app.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.naamtaan1008.app.R
import com.naamtaan1008.app.data.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AboutFragment : Fragment() {

    private lateinit var nameInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var messageInput: TextInputEditText
    private lateinit var sendBtn: MaterialButton
    private lateinit var feedback: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_about, container, false)
        nameInput = v.findViewById(R.id.nameInput)
        emailInput = v.findViewById(R.id.emailInput)
        messageInput = v.findViewById(R.id.messageInput)
        sendBtn = v.findViewById(R.id.sendBtn)
        feedback = v.findViewById(R.id.feedback)
        setupWebView(v.findViewById(R.id.aboutWeb))
        sendBtn.setOnClickListener { submit() }
        listOf(nameInput, emailInput, messageInput).forEach { input ->
            input.doAfterTextChanged { feedback.visibility = View.GONE }
        }
        loadAbout(v.findViewById(R.id.aboutWeb))
        return v
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(web: WebView) {
        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                // ScrollView 内的 WebView 需按内容高度自适应，否则高度塌陷为 0
                view?.post {
                    val w = view.measuredWidth
                    if (w > 0) {
                        val h = (w * view.contentHeight / view.scale).toInt()
                        if (h > 0 && view.layoutParams?.height != h) {
                            view.layoutParams = view.layoutParams?.apply { height = h }
                        }
                    }
                }
            }
        }
        val settings: WebSettings = web.settings
        settings.javaScriptEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = false
        settings.defaultTextEncodingName = "UTF-8"
    }

    private fun loadAbout(web: WebView) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val about = withContext(Dispatchers.IO) { Repository.fetchAbout() }
                renderAbout(web, about.title, about.content)
            } catch (e: Exception) {
                web.loadDataWithBaseURL(
                    null,
                    "<html><body style='background:#FAF3E0;color:#8A6D4B;padding:20px;'>内容加载失败，请下拉或稍后再试。</body></html>",
                    "text/html", "UTF-8", null
                )
            }
        }
    }

    private fun renderAbout(web: WebView, title: String, content: String) {
        val html = buildString {
            append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">")
            append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
            append("<style>")
            append("body{margin:0;padding:0;background:#FAF3E0;color:#3D3831;")
            append("font-family:-apple-system,'Noto Serif SC','PingFang SC','Microsoft YaHei',serif;line-height:1.9;font-size:15px;}")
            append("h3,h4{margin:20px 0 8px;color:#3D3831;}")
            append("p{margin:10px 0;}")
            append("strong{color:#3D3831;}")
            append("img{max-width:100%;height:auto;border-radius:8px;}")
            append("a{color:#C77D5E;}")
            append("blockquote{border-left:3px solid #C9A227;margin:14px 0;padding:4px 14px;color:#8A6D4B;}")
            append("</style></head><body>")
            append(content)
            append("</body></html>")
        }
        web.loadDataWithBaseURL("https://naamtaan1008.com/", html, "text/html", "UTF-8", null)
    }

    private fun submit() {
        val name = nameInput.text?.toString()?.trim().orEmpty()
        val email = emailInput.text?.toString()?.trim().orEmpty()
        val message = messageInput.text?.toString()?.trim().orEmpty()

        if (name.isEmpty() || email.isEmpty() || message.isEmpty()) {
            feedback.text = getString(R.string.contact_required)
            feedback.setTextColor(resources.getColor(R.color.zakka_clay, null))
            feedback.visibility = View.VISIBLE
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            feedback.text = getString(R.string.contact_email_invalid)
            feedback.setTextColor(resources.getColor(R.color.zakka_clay, null))
            feedback.visibility = View.VISIBLE
            return
        }

        sendBtn.isEnabled = false
        sendBtn.text = getString(R.string.contact_sending)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    Repository.submitContact(name, email, message)
                }
                if (result.success) {
                    feedback.text = getString(R.string.contact_success)
                    feedback.setTextColor(resources.getColor(R.color.zakka_mustard, null))
                    feedback.visibility = View.VISIBLE
                    nameInput.text?.clear()
                    emailInput.text?.clear()
                    messageInput.text?.clear()
                } else {
                    feedback.text = result.error?.takeIf { it.isNotBlank() }
                        ?: getString(R.string.contact_failed)
                    feedback.setTextColor(resources.getColor(R.color.zakka_clay, null))
                    feedback.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                feedback.text = getString(R.string.contact_failed)
                feedback.setTextColor(resources.getColor(R.color.zakka_clay, null))
                feedback.visibility = View.VISIBLE
            } finally {
                sendBtn.isEnabled = true
                sendBtn.text = getString(R.string.contact_send)
            }
        }
    }
}
