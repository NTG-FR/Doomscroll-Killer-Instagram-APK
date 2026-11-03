package io.github.ntgfr.doomscrollkiller.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ntgfr.doomscrollkiller.data.SettingsRepository
import io.github.ntgfr.doomscrollkiller.ui.components.rememberSetupStatus
import io.github.ntgfr.doomscrollkiller.ui.screens.CapturesScreen
import io.github.ntgfr.doomscrollkiller.ui.screens.OnboardingScreen
import io.github.ntgfr.doomscrollkiller.ui.screens.SettingsScreen
import io.github.ntgfr.doomscrollkiller.ui.theme.DoomscrollTheme
import io.github.ntgfr.doomscrollkiller.web.InstagramWebController
import kotlinx.coroutines.launch

/**
 * "Doomscroll Killer": first-launch setup, then settings and protection status.
 * The feed itself has its own icon and activity ("Insta", [FeedActivity]).
 */
class MainActivity : ComponentActivity() {

    private val repo by lazy { SettingsRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { DoomscrollTheme { App() } }
    }

    private fun openFeed() = startActivity(FeedActivity.intent(this))

    @Composable
    private fun App() {
        val settings = repo.settings.collectAsStateWithLifecycle(initialValue = null).value
        val scope = rememberCoroutineScope()
        var showCaptures by rememberSaveable { mutableStateOf(false) }
        // Blank background while settings load (a few milliseconds): no flash of the wrong screen.
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (settings == null) return@Box
            if (!settings.onboardingDone) {
                OnboardingScreen(onFinish = {
                    scope.launch {
                        repo.setOnboardingDone(true)
                        openFeed()
                    }
                })
                return@Box
            }

            AnimatedContent(
                targetState = showCaptures,
                transitionSpec = {
                    (slideInHorizontally { it / 4 } + fadeIn()) togetherWith (slideOutHorizontally { it / 4 } + fadeOut())
                },
                label = "screen",
            ) { captures ->
                if (captures) {
                    CapturesScreen(onBack = { showCaptures = false })
                } else {
                    SettingsScreen(
                        settings = settings,
                        status = rememberSetupStatus(),
                        repo = repo,
                        onOpenFeed = ::openFeed,
                        onLogOut = InstagramWebController::logOut,
                        onOpenCaptures = { showCaptures = true },
                        onReplayIntro = { scope.launch { repo.setOnboardingDone(false) } },
                    )
                }
            }
            if (showCaptures) BackHandler { showCaptures = false }
        }
    }
}
