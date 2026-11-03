package io.github.ntgfr.doomscrollkiller.ui.screens

import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.ntgfr.doomscrollkiller.R
import io.github.ntgfr.doomscrollkiller.system.SetupStatus
import io.github.ntgfr.doomscrollkiller.ui.components.ready
import io.github.ntgfr.doomscrollkiller.ui.theme.LocalStatusColors
import io.github.ntgfr.doomscrollkiller.web.InstagramWebController

// instagram.com's own page backgrounds, so the status bar blends in with the page.
private val InstagramDark = Color(0xFF0C1014)
private val InstagramLight = Color.White

/**
 * The built-in Instagram feed, full screen: no app bar, so it feels like Instagram minus the doomscroll.
 * A slim banner shows up only when protection needs attention.
 */
@Composable
fun FeedScreen(web: InstagramWebController, status: SetupStatus, onOpenSettings: () -> Unit) {
    val pageBackground = if (isSystemInDarkTheme()) InstagramDark else InstagramLight
    Box(Modifier.fillMaxSize().background(pageBackground)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            AnimatedVisibility(!status.ready()) { AttentionBanner(onOpenSettings) }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = {
                        web.webView.also {
                            // The WebView outlives this screen: detach it from any previous parent first.
                            (it.parent as? ViewGroup)?.removeView(it)
                            // MATCH_PARENT is essential: with Compose's default WRAP_CONTENT, the WebView
                            // reports a 0px-high viewport, so Instagram's full-height windows (cookie consent)
                            // collapse to nothing and leave their dark backdrop over the page.
                            it.layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                        }
                    },
                )
                if (web.progress < 100) {
                    LinearProgressIndicator(
                        progress = { web.progress / 100f },
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        trackColor = Color.Transparent,
                    )
                }
                if (web.hasError) ErrorState(pageBackground, onRetry = web::reload)
            }
        }
        // Full-screen video covers everything.
        web.fullscreenView?.let { video ->
            AndroidView(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                factory = { video.also { (it.parent as? ViewGroup)?.removeView(it) } },
            )
        }
    }
}

@Composable
private fun AttentionBanner(onClick: () -> Unit) {
    val warning = LocalStatusColors.current.warning
    Row(
        Modifier
            .fillMaxWidth()
            .background(warning.copy(alpha = 0.18f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_shield_alert), contentDescription = null, tint = warning, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            stringResource(R.string.feed_banner_attention),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface, // not inside a Surface: set it explicitly
        )
    }
}

@Composable
private fun ErrorState(background: Color, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(background).padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painterResource(R.drawable.ic_cloud_off), contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp),
        )
        Text(
            stringResource(R.string.feed_error_title), style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            stringResource(R.string.feed_error_body), textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Button(onClick = onRetry) { Text(stringResource(R.string.feed_retry)) }
    }
}
