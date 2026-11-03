package io.github.ntgfr.doomscrollkiller.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import io.github.ntgfr.doomscrollkiller.data.SettingsRepository
import io.github.ntgfr.doomscrollkiller.debug.DebugStore
import io.github.ntgfr.doomscrollkiller.debug.PageCapture
import io.github.ntgfr.doomscrollkiller.ui.components.rememberSetupStatus
import io.github.ntgfr.doomscrollkiller.ui.screens.FeedScreen
import io.github.ntgfr.doomscrollkiller.ui.theme.DoomscrollTheme
import io.github.ntgfr.doomscrollkiller.web.InstagramWebController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * "Insta": the built-in Instagram feed, full screen, with its own home screen icon.
 * Setup and settings live in [MainActivity] ("Doomscroll Killer").
 */
class FeedActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_URL = "url"
        private const val STATE_WEB = "web"
        private const val DEBUG_CAPTURE_MS = 4_000L

        /** Opens the feed, optionally at a given instagram.com page. */
        fun intent(context: Context, url: String? = null): Intent =
            Intent(context, FeedActivity::class.java).apply {
                if (url != null) putExtra(EXTRA_URL, url)
                // Bring the existing feed to the front (above Instagram) instead of starting a new one.
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
    }

    private lateinit var web: InstagramWebController
    private val repo by lazy { SettingsRepository(this) }

    // Picking a file for a post or a story.
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private val fileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        fileCallback?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data))
        fileCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        web = InstagramWebController(this, ::openFileChooser)
        web.start(savedInstanceState?.getBundle(STATE_WEB), intent.getStringExtra(EXTRA_URL))
        setContent { DoomscrollTheme { Feed() } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Opened from a notification: go straight to the right page.
        intent.getStringExtra(EXTRA_URL)?.let(web::load)
    }

    override fun onResume() {
        super.onResume()
        web.onResume()
        web.reloadIfLoggedOut()
    }

    override fun onPause() {
        web.onPause()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBundle(STATE_WEB, Bundle().also(web::saveState))
    }

    override fun onDestroy() {
        web.destroy()
        super.onDestroy()
    }

    private fun openFileChooser(intent: Intent, callback: ValueCallback<Array<Uri>>): Boolean {
        fileCallback?.onReceiveValue(null)
        fileCallback = callback
        return try {
            fileLauncher.launch(intent)
            true
        } catch (_: ActivityNotFoundException) {
            fileCallback = null
            false
        }
    }

    @Composable
    private fun Feed() {
        val settings = repo.settings.collectAsStateWithLifecycle(initialValue = null).value ?: return
        if (!settings.onboardingDone) {
            // First launch from the "Insta" icon: run the setup in Doomscroll Killer first.
            LaunchedEffect(Unit) {
                startActivity(Intent(this@FeedActivity, MainActivity::class.java))
                finish()
            }
            return
        }
        FeedScreen(web, rememberSetupStatus(), onOpenSettings = {
            startActivity(Intent(this, MainActivity::class.java))
        })
        // Nothing left to go back to: keep the feed (and the login) alive in the background.
        BackHandler { if (!web.goBack()) moveTaskToBack(true) }
        if (settings.debugEnabled) DebugCaptureLoop()
    }

    /** Developer mode: captures the current page every few seconds while the feed is visible. */
    @Composable
    private fun DebugCaptureLoop() {
        val dir = remember { File(filesDir, "debug") }
        LaunchedEffect(Unit) {
            while (true) {
                web.captureDom { content ->
                    val capture = PageCapture(System.currentTimeMillis(), web.webView.url.orEmpty(), content)
                    if (DebugStore.add(capture)) {
                        lifecycleScope.launch(Dispatchers.IO) { DebugStore.saveToFile(dir, capture) }
                    }
                }
                delay(DEBUG_CAPTURE_MS)
            }
        }
    }
}
