package io.github.ntgfr.doomscrollkiller.web

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.ntgfr.doomscrollkiller.R
import org.json.JSONArray

/**
 * Owns the WebView showing instagram.com and applies [InstagramWebRules] to it.
 * Lives as long as the activity, so switching screens never reloads the feed.
 *
 * @param openFileChooser called when a page wants a file (posting a photo or a story).
 */
class InstagramWebController(
    private val context: Context,
    private val openFileChooser: (Intent, ValueCallback<Array<Uri>>) -> Boolean,
) {
    val webView: WebView = WebView(context)

    // Compose state, read by the feed screen.
    var progress by mutableIntStateOf(0)
        private set
    var hasError by mutableStateOf(false)
        private set
    /** Full-screen video view (null when not in full screen). */
    var fullscreenView by mutableStateOf<View?>(null)
        private set
    private var fullscreenCallback: WebChromeClient.CustomViewCallback? = null

    /** Anti-loop for the "For you" -> "Following" redirect. */
    private var lastHomeRedirectAt = 0L

    init {
        configure()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configure() {
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        webView.settings.apply {
            javaScriptEnabled = true // instagram.com does not work without JavaScript.
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            setSupportMultipleWindows(false)
        }
        webView.webViewClient = Client()
        webView.webChromeClient = ChromeClient()
    }

    /** Loads the given page, or restores the previous session, or opens the Following feed. */
    fun start(savedState: Bundle?, url: String?) {
        when {
            url != null -> load(url)
            savedState != null && webView.restoreState(savedState) != null -> Unit
            else -> load(InstagramWebRules.HOME_URL)
        }
    }

    fun load(url: String) {
        hasError = false
        webView.loadUrl(url)
    }

    fun reload() {
        hasError = false
        webView.reload()
    }

    fun saveState(out: Bundle) {
        webView.saveState(out)
    }

    /** Handles the back button. Returns false when there is nothing to go back to. */
    fun goBack(): Boolean = when {
        fullscreenView != null -> {
            fullscreenCallback?.onCustomViewHidden()
            true
        }
        webView.canGoBack() -> {
            webView.goBack()
            true
        }
        else -> false
    }

    /** Keeps the login on disk. */
    fun persistSession() = CookieManager.getInstance().flush()

    /** Logs out of Instagram: clears cookies and site storage, then shows the login page. */
    fun logOut() {
        CookieManager.getInstance().removeAllCookies { CookieManager.getInstance().flush() }
        WebStorage.getInstance().deleteAllData()
        webView.clearCache(true)
        webView.clearHistory()
        load(InstagramWebRules.HOME_URL)
    }

    /** Developer mode: text description of the current page, delivered to [onResult]. */
    fun captureDom(onResult: (String) -> Unit) {
        webView.evaluateJavascript(InstagramWebRules.domDumpScript) { result ->
            // evaluateJavascript returns a JSON-encoded string (quotes and \n escaped).
            runCatching { JSONArray("[$result]").getString(0) }.getOrNull()?.let(onResult)
        }
    }

    fun onResume() = webView.onResume()
    fun onPause() {
        webView.onPause()
        persistSession()
    }

    fun destroy() {
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.destroy()
    }

    /** Applies the rules on every address change, including Instagram's in-page navigation (history.pushState). */
    private fun guard(url: String?) {
        val uri = url?.let(Uri::parse) ?: return
        if (!InstagramWebRules.isAllowedHost(uri.host)) return
        when {
            InstagramWebRules.isBlockedPath(uri.path) -> {
                Toast.makeText(context, R.string.feed_reel_blocked, Toast.LENGTH_SHORT).show()
                if (webView.canGoBack()) webView.goBack() else webView.loadUrl(InstagramWebRules.HOME_URL)
            }
            InstagramWebRules.isForYouHome(uri.path, uri.query) -> {
                // At most one redirect every 3 s, in case Instagram ever ignores ?variant=following.
                val now = System.currentTimeMillis()
                if (now - lastHomeRedirectAt > 3_000) {
                    lastHomeRedirectAt = now
                    webView.loadUrl(InstagramWebRules.HOME_URL)
                }
            }
        }
    }

    private fun inject() = webView.evaluateJavascript(InstagramWebRules.injectionScript(), null)

    private inner class Client : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val url = request.url
            if (InstagramWebRules.isAllowedHost(url.host)) {
                if (InstagramWebRules.isBlockedPath(url.path)) {
                    Toast.makeText(context, R.string.feed_reel_blocked, Toast.LENGTH_SHORT).show()
                    return true
                }
                return false // stays in the app
            }
            // External link: open it in the browser.
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, url).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: ActivityNotFoundException) {
            }
            return true
        }

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) = inject()
        override fun onPageCommitVisible(view: WebView, url: String?) = inject()
        override fun onPageFinished(view: WebView, url: String?) = inject()

        // Also called when Instagram changes page without reloading (history.pushState).
        override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
            guard(url)
            inject()
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            // Only the page itself matters; a missing image must not show the error screen.
            if (request.isForMainFrame) hasError = true
        }
    }

    private inner class ChromeClient : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            progress = newProgress
        }

        override fun onShowFileChooser(
            webView: WebView,
            filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams,
        ): Boolean = openFileChooser(fileChooserParams.createIntent(), filePathCallback)

        override fun onShowCustomView(view: View, callback: CustomViewCallback) {
            fullscreenView = view
            fullscreenCallback = callback
        }

        override fun onHideCustomView() {
            fullscreenView = null
            fullscreenCallback = null
        }
    }
}
