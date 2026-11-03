package io.github.ntgfr.doomscrollkiller.ui.screens

import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.ntgfr.doomscrollkiller.R
import io.github.ntgfr.doomscrollkiller.system.DeviceBrand
import io.github.ntgfr.doomscrollkiller.ui.components.BrandMark
import io.github.ntgfr.doomscrollkiller.ui.components.DeviceTipsCard
import io.github.ntgfr.doomscrollkiller.ui.components.Group
import io.github.ntgfr.doomscrollkiller.ui.components.GroupDivider
import io.github.ntgfr.doomscrollkiller.ui.components.IconBadge
import io.github.ntgfr.doomscrollkiller.ui.components.ListRow
import io.github.ntgfr.doomscrollkiller.ui.components.SetupChecklist
import io.github.ntgfr.doomscrollkiller.ui.components.rememberSetupStatus
import kotlinx.coroutines.launch

private const val PAGES = 4

/** First launch: what the app does, how it protects privacy, the setup steps, then log in. */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pager = rememberPagerState { PAGES }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == PAGES - 1

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.End) {
                if (!last) TextButton(onClick = onFinish) { Text(stringResource(R.string.action_skip)) }
            }
            HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
                when (page) {
                    0 -> WelcomePage()
                    1 -> HowItWorksPage()
                    2 -> SetupPage()
                    else -> DonePage()
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PageDots(count = PAGES, current = pager.currentPage)
                Spacer(Modifier.weight(1f))
                if (pager.currentPage > 0) {
                    TextButton(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }) {
                        Text(stringResource(R.string.action_back))
                    }
                    Spacer(Modifier.width(8.dp))
                }
                Button(onClick = {
                    if (last) onFinish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                }) {
                    Text(stringResource(if (last) R.string.onb_done_button else R.string.action_continue))
                }
            }
        }
    }
}

@Composable
private fun PageDots(count: Int, current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { i ->
            Box(
                Modifier
                    .height(8.dp)
                    .width(if (i == current) 22.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (i == current) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline
                    ),
            )
        }
    }
}

/** Scrollable page with a big title and a short intro. */
@Composable
private fun Page(title: String, body: String, header: @Composable () -> Unit = {}, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            header()
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        content()
    }
}

@Composable
private fun WelcomePage() {
    Page(
        title = stringResource(R.string.onb_welcome_title),
        body = stringResource(R.string.onb_welcome_body),
        header = {
            Spacer(Modifier.height(24.dp))
            BrandMark(size = 72.dp)
            Spacer(Modifier.height(8.dp))
        },
    ) {
        Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Feature(R.drawable.ic_people, R.string.onb_feature_following, R.string.onb_feature_following_desc)
            Feature(R.drawable.ic_block, R.string.onb_feature_no_reels, R.string.onb_feature_no_reels_desc)
            Feature(R.drawable.ic_notifications, R.string.onb_feature_notifications, R.string.onb_feature_notifications_desc)
        }
    }
}

@Composable
private fun Feature(@DrawableRes icon: Int, title: Int, description: Int) {
    Row(verticalAlignment = Alignment.Top) {
        IconBadge(icon, size = 44.dp)
        Spacer(Modifier.width(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(description), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HowItWorksPage() {
    Page(title = stringResource(R.string.onb_how_title), body = stringResource(R.string.onb_how_web)) {
        Group {
            ListRow(R.drawable.ic_accessibility, stringResource(R.string.onb_how_app_title), stringResource(R.string.onb_how_app))
            GroupDivider()
            ListRow(R.drawable.ic_lock, stringResource(R.string.settings_privacy), stringResource(R.string.onb_how_privacy))
            GroupDivider()
            ListRow(R.drawable.ic_code, stringResource(R.string.settings_source), stringResource(R.string.onb_how_open_source))
        }
    }
}

@Composable
private fun SetupPage() {
    val status = rememberSetupStatus()
    Page(title = stringResource(R.string.onb_setup_title), body = stringResource(R.string.onb_setup_body)) {
        SetupChecklist(status)
    }
}

@Composable
private fun DonePage() {
    Page(
        title = stringResource(R.string.onb_done_title),
        body = stringResource(R.string.onb_done_body),
    ) {
        DeviceTipsCard(DeviceBrand.fromManufacturer(Build.MANUFACTURER), Build.MANUFACTURER.replaceFirstChar { it.uppercase() })
    }
}
