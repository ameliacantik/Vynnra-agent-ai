package lol.vynnra.agent.platform

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.DownloadListener
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout

/**
 * AI-native browser surface. The agent can launch a controlled WebView without driving Chrome.
 * Navigation is restricted to HTTP(S) and uploads remain user-mediated until a picker bridge is added.
 */
class VynnraBrowserActivity : Activity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = true
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                    !isHttpUrl(request.url)
            }
            setDownloadListener(DownloadListener { url, _, _, _, _ ->
                BrowserController(this@VynnraBrowserActivity).download(url)
            })
        }
        setContentView(FrameLayout(this).apply { addView(webView) })
        val initialUrl = intent.getStringExtra(EXTRA_URL)?.let(::normalizeUrl) ?: "https://www.google.com"
        webView.loadUrl(initialUrl)
    }

    override fun onDestroy() {
        webView.stopLoading()
        webView.destroy()
        super.onDestroy()
    }

    private fun isHttpUrl(uri: Uri): Boolean =
        uri.scheme.equals("http", true) || uri.scheme.equals("https", true)

    private fun normalizeUrl(value: String): String? {
        val text = value.trim()
        if (text.isEmpty()) return null
        val candidate = if (text.startsWith("http://") || text.startsWith("https://")) text else "https://$text"
        return candidate.takeIf { isHttpUrl(Uri.parse(it)) }
    }

    companion object {
        private const val EXTRA_URL = "url"

        fun intent(context: Context, url: String? = null): Intent =
            Intent(context, VynnraBrowserActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                url?.let { putExtra(EXTRA_URL, it) }
            }
    }
}
