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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import io.github.ntgfr.doomscrollkiller.data.SettingsRepository
import io.github.ntgfr.doomscrollkiller.debug.DebugStore
import io.github.ntgfr.doomscrollkiller.debug.PageCapture
import io.github.ntgfr.doomscrollkiller.ui.components.rememberSetupStatus
import io.github.ntgfr.doomscrollkiller.ui.screens.CapturesScreen
import io.github.ntgfr.doomscrollkiller.ui.screens.FeedScreen
import io.github.ntgfr.doomscrollkiller.ui.screens.OnboardingScreen
import io.github.ntgfr.doomscrollkiller.ui.screens.SettingsScreen
import io.github.ntgfr.doomscrollkiller.ui.theme.DoomscrollTheme
import io.github.ntgfr.doomscrollkiller.web.InstagramWebController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/** Screens of the app. The feed always stays alive underneath the others. */
enum class Screen { FEED, SETTINGS, CAPTURES }

/** The single activity: onboarding, the built-in Instagram feed, and settings. */
class MainActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_URL = "url"
        private const val STATE_WEB = "web"
        private const val DEBUG_CAPTURE_MS = 4_000L

        /** Opens the app on the feed, optionally at a given instagram.com page. */
        fun feedIntent(context: Context, url: String? = null): Intent =
            Intent(context, MainActivity::class.java).apply {
                if (url != null) putExtra(EXTRA_URL, url)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
    }

    private lateinit var web: InstagramWebController
    private val repo by lazy { SettingsRepository(this) }
    private var screen by mutableStateOf(Screen.FEED)

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
        setContent { DoomscrollTheme { App() } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Opened from a notification or from the redirect: show the feed, at the right page if given.
        intent.getStringExtra(EXTRA_URL)?.let(web::load)
        screen = Screen.FEED
    }

    override fun onResume() {
        super.onResume()
        web.onResume()
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
    private fun App() {
        val settings = repo.settings.collectAsStateWithLifecycle(initialValue = null).value
        val scope = rememberCoroutineScope()
        // Blank background while settings load (a few milliseconds): no flash of the wrong screen.
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (settings == null) return@Box
            if (!settings.onboardingDone) {
                OnboardingScreen(onFinish = { scope.launch { repo.setOnboardingDone(true) } })
                return@Box
            }

            val status = rememberSetupStatus()
            FeedScreen(web, status, onOpenSettings = { screen = Screen.SETTINGS })
            AnimatedContent(
                targetState = screen,
                transitionSpec = {
                    (slideInHorizontally { it / 4 } + fadeIn()) togetherWith (slideOutHorizontally { it / 4 } + fadeOut())
                },
                label = "screen",
            ) { target ->
                when (target) {
                    Screen.FEED -> Unit // the feed is drawn underneath
                    Screen.SETTINGS -> SettingsScreen(
                        settings = settings,
                        status = status,
                        repo = repo,
                        onBack = { screen = Screen.FEED },
                        onReloadFeed = {
                            web.reload()
                            screen = Screen.FEED
                        },
                        onLogOut = {
                            web.logOut()
                            screen = Screen.FEED
                        },
                        onOpenCaptures = { screen = Screen.CAPTURES },
                        onReplayIntro = { scope.launch { repo.setOnboardingDone(false) } },
                    )
                    Screen.CAPTURES -> CapturesScreen(onBack = { screen = Screen.SETTINGS })
                }
            }

            BackHandler {
                when (screen) {
                    Screen.CAPTURES -> screen = Screen.SETTINGS
                    Screen.SETTINGS -> screen = Screen.FEED
                    // Nothing left to go back to: keep the app (and the login) alive in the background.
                    Screen.FEED -> if (!web.goBack()) moveTaskToBack(true)
                }
            }

            if (settings.debugEnabled) DebugCaptureLoop()
        }
    }

    /** Developer mode: captures the current page every few seconds while the app is visible. */
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
