package io.github.ntgfr.doomscrollkiller.ui.screens

import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import io.github.ntgfr.doomscrollkiller.ui.components.BrandMark
import io.github.ntgfr.doomscrollkiller.ui.components.ready
import io.github.ntgfr.doomscrollkiller.ui.theme.LocalStatusColors
import io.github.ntgfr.doomscrollkiller.web.InstagramWebController

/** The built-in Instagram feed, under a slim bar showing whether protection is on. */
@Composable
fun FeedScreen(web: InstagramWebController, status: SetupStatus, onOpenSettings: () -> Unit) {
    val ok = status.ready()
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            TopBar(ok, onOpenSettings)
            AnimatedVisibility(!ok) { AttentionBanner(onOpenSettings) }
            Box(Modifier.weight(1f).fillMaxWidth().navigationBarsPadding().imePadding()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    // The WebView outlives this screen: detach it from any previous parent first.
                    factory = { web.webView.also { (it.parent as? ViewGroup)?.removeView(it) } },
                )
                if (web.progress < 100) {
                    LinearProgressIndicator(
                        progress = { web.progress / 100f },
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        trackColor = Color.Transparent,
                    )
                }
                if (web.hasError) ErrorState(onRetry = web::reload)
            }
        }
        // Full-screen video covers everything, bars included.
        web.fullscreenView?.let { video ->
            AndroidView(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                factory = { video.also { (it.parent as? ViewGroup)?.removeView(it) } },
            )
        }
    }
}

@Composable
private fun TopBar(ok: Boolean, onOpenSettings: () -> Unit) {
    val status = LocalStatusColors.current
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().height(52.dp).padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandMark(size = 28.dp)
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        IconButton(onClick = onOpenSettings) {
            Icon(
                painterResource(if (ok) R.drawable.ic_shield_ok else R.drawable.ic_shield_alert),
                contentDescription = stringResource(if (ok) R.string.feed_status_ok else R.string.feed_status_attention),
                tint = if (ok) status.good else status.warning,
            )
        }
        IconButton(onClick = onOpenSettings) {
            Icon(painterResource(R.drawable.ic_settings), contentDescription = stringResource(R.string.settings_title))
        }
    }
}

@Composable
private fun AttentionBanner(onClick: () -> Unit) {
    val warning = LocalStatusColors.current.warning
    Row(
        Modifier
            .fillMaxWidth()
            .background(warning.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_shield_alert), contentDescription = null, tint = warning, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.feed_banner_attention), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ErrorState(onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painterResource(R.drawable.ic_cloud_off), contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp),
        )
        Text(stringResource(R.string.feed_error_title), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.feed_error_body), textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Button(onClick = onRetry) { Text(stringResource(R.string.feed_retry)) }
    }
}
