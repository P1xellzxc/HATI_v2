package com.hativ2.web

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.RenderProcessGoneDetail
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.webkit.WebViewAssetLoader

/**
 * Hosts the HATI² web app (docs/app, bundled as assets/app) in a WebView.
 *
 * The page is served from https://appassets.androidplatform.net/assets/app/ through
 * WebViewAssetLoader, so it is a secure context (localStorage, clipboard work) and
 * never touches the network. Window.HatiAndroid (see [HatiBridge]) stands in for
 * navigator.share and file downloads, which Android WebView does not provide.
 */
class MainActivity : ComponentActivity() {

    internal lateinit var webView: WebView
        private set

    private val assetLoader by lazy {
        WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()
    }

    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            if (webView.canGoBack()) webView.goBack() else finish()
        }
    }

    private var pendingSave: String? = null
    private val createDocument =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val text = pendingSave
            pendingSave = null
            val uri = result.data?.data
            if (text == null || uri == null) return@registerForActivityResult
            Thread {
                val ok = runCatching {
                    contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray()) }
                }.isSuccess
                runOnUiThread { toast(if (ok) R.string.saved else R.string.save_failed) }
            }.start()
        }

    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private val openDocument =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            fileCallback?.onReceiveValue(
                WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
            )
            fileCallback = null
        }

    private lateinit var root: FrameLayout

    /** How many times the WebView renderer died and the page was rebuilt (read by tests). */
    @Volatile
    internal var renderRestarts = 0
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // The bars are drawn edge-to-edge (required at targetSdk 35); the WebView is
        // padded by the bar, cutout and keyboard insets because not every WebView
        // version reports env(safe-area-inset-*). The strips show the paper colour.
        root = FrameLayout(this)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val b = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                    WindowInsetsCompat.Type.displayCutout() or
                    WindowInsetsCompat.Type.ime()
            )
            v.setPadding(b.left, b.top, b.right, b.bottom)
            WindowInsetsCompat.CONSUMED
        }
        setContentView(root)
        onBackPressedDispatcher.addCallback(this, backCallback)

        attachWebView()
        if (savedInstanceState == null || webView.restoreState(savedInstanceState) == null) {
            webView.loadUrl(startUrl(intent?.data))
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun attachWebView() {
        webView = WebView(this)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
        }
        webView.webViewClient = Client()
        webView.webChromeClient = Chrome()
        webView.addJavascriptInterface(HatiBridge(this), HatiBridge.NAME)
        root.addView(webView, FrameLayout.LayoutParams(-1, -1))
    }

    /**
     * The renderer is a separate process the system may kill (low memory) or that may
     * crash. Unhandled, Android then kills the whole app. Instead, drop the dead
     * WebView and load the app again; its data is in localStorage, so nothing is lost.
     */
    private fun onRendererGone(dead: WebView) {
        if (dead !== webView) return
        renderRestarts++
        root.removeView(dead)
        dead.destroy()
        attachWebView()
        backCallback.isEnabled = false
        webView.loadUrl(INDEX)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.let { webView.loadUrl(startUrl(it)) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onDestroy() {
        fileCallback?.onReceiveValue(null)
        fileCallback = null
        webView.destroy()
        super.onDestroy()
    }

    /** True when the WebView is showing our own bundled page (checked before bridge actions). */
    internal fun isOwnPage(): Boolean = webView.url?.startsWith(ORIGIN + "/") == true

    internal fun share(text: String) {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        launch(Intent.createChooser(send, getString(R.string.share_title)))
    }

    internal fun saveFile(name: String, mime: String, text: String) {
        val create = Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(mime.ifBlank { "application/octet-stream" })
            .putExtra(Intent.EXTRA_TITLE, name)
        pendingSave = text
        try {
            createDocument.launch(create)
        } catch (e: ActivityNotFoundException) {
            pendingSave = null
            toast(R.string.no_app)
        }
    }

    private fun launch(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            toast(R.string.no_app)
        }
    }

    private fun toast(res: Int) = Toast.makeText(this, res, Toast.LENGTH_SHORT).show()

    private inner class Client : WebViewClient() {
        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
            assetLoader.shouldInterceptRequest(request.url)

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val url = request.url
            if (url.host == HOST) return false
            if (isAppLink(url)) {
                view.loadUrl(startUrl(url))
                return true
            }
            // Anything else (help links, mailto:, …) opens outside the app.
            launch(Intent(Intent.ACTION_VIEW, url).addCategory(Intent.CATEGORY_BROWSABLE))
            return true
        }

        override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
            onRendererGone(view)
            return true
        }

        override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
            backCallback.isEnabled = view.canGoBack()
        }
    }

    private inner class Chrome : WebChromeClient() {
        override fun onShowFileChooser(
            view: WebView,
            callback: ValueCallback<Array<Uri>>,
            params: FileChooserParams,
        ): Boolean {
            fileCallback?.onReceiveValue(null)
            fileCallback = callback
            return try {
                openDocument.launch(params.createIntent())
                true
            } catch (e: ActivityNotFoundException) {
                fileCallback = null
                false
            }
        }
    }

    companion object {
        const val HOST = WebViewAssetLoader.DEFAULT_DOMAIN
        const val ORIGIN = "https://$HOST"
        const val INDEX = "$ORIGIN/assets/app/index.html"

        /** https://p1xellzxc.github.io/HATI_v2/app/… (the published web app). */
        fun isAppLink(uri: Uri): Boolean =
            uri.scheme == "https" && uri.host == "p1xellzxc.github.io" &&
                (uri.path ?: "").startsWith("/HATI_v2/app")

        /**
         * Maps an incoming link to the bundled page, keeping only the #fragment
         * (where invites carry their data):
         *  - https://p1xellzxc.github.io/HATI_v2/app/#<data> → index.html#<data>
         *  - hati2://join#<data> → index.html#<data>
         * Anything else opens the start page.
         */
        fun startUrl(uri: Uri?): String {
            val ok = uri != null && (isAppLink(uri) || (uri.scheme == "hati2" && uri.host == "join"))
            val fragment = if (ok) uri!!.encodedFragment else null
            return if (fragment.isNullOrEmpty()) INDEX else "$INDEX#$fragment"
        }
    }
}
