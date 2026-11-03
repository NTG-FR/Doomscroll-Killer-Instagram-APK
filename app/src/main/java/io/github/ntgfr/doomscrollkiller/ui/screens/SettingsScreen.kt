package io.github.ntgfr.doomscrollkiller.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.ntgfr.doomscrollkiller.Links
import io.github.ntgfr.doomscrollkiller.R
import io.github.ntgfr.doomscrollkiller.data.AppSettings
import io.github.ntgfr.doomscrollkiller.data.SettingsRepository
import io.github.ntgfr.doomscrollkiller.system.DeviceBrand
import io.github.ntgfr.doomscrollkiller.system.SetupStatus
import io.github.ntgfr.doomscrollkiller.ui.components.DeviceTipsCard
import io.github.ntgfr.doomscrollkiller.ui.components.Group
import io.github.ntgfr.doomscrollkiller.ui.components.GroupDivider
import io.github.ntgfr.doomscrollkiller.ui.components.LinkRow
import io.github.ntgfr.doomscrollkiller.ui.components.ListRow
import io.github.ntgfr.doomscrollkiller.ui.components.SectionTitle
import io.github.ntgfr.doomscrollkiller.ui.components.SetupChecklist
import io.github.ntgfr.doomscrollkiller.ui.components.SwitchRow
import io.github.ntgfr.doomscrollkiller.ui.components.openUrl
import io.github.ntgfr.doomscrollkiller.ui.components.ready
import io.github.ntgfr.doomscrollkiller.ui.theme.LocalStatusColors
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settings: AppSettings,
    status: SetupStatus,
    repo: SettingsRepository,
    onBack: () -> Unit,
    onReloadFeed: () -> Unit,
    onLogOut: () -> Unit,
    onOpenCaptures: () -> Unit,
    onReplayIntro: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmLogout by rememberSaveable { mutableStateOf(false) }
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().height(56.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.action_back))
                }
                Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge)
            }

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 24.dp)) {
                StatusHero(status.ready())

                SectionTitle(stringResource(R.string.settings_section_setup))
                SetupChecklist(status)

                SectionTitle(stringResource(R.string.settings_section_instagram))
                Group {
                    SwitchRow(
                        R.drawable.ic_block, stringResource(R.string.settings_redirect),
                        stringResource(R.string.settings_redirect_desc), settings.instagramRedirect,
                    ) { scope.launch { repo.setInstagramRedirect(it) } }
                    GroupDivider()
                    SwitchRow(
                        R.drawable.ic_notifications, stringResource(R.string.settings_relay),
                        stringResource(R.string.settings_relay_desc), settings.notificationRelay,
                    ) { scope.launch { repo.setNotificationRelay(it) } }
                    GroupDivider()
                    LinkRow(R.drawable.ic_refresh, stringResource(R.string.settings_reload), onClick = onReloadFeed)
                    GroupDivider()
                    LinkRow(R.drawable.ic_logout, stringResource(R.string.settings_logout)) { confirmLogout = true }
                }

                SectionTitle(stringResource(R.string.settings_section_phone))
                DeviceTipsCard(DeviceBrand.fromManufacturer(Build.MANUFACTURER), Build.MANUFACTURER.replaceFirstChar { it.uppercase() })

                SectionTitle(stringResource(R.string.settings_section_about))
                Group {
                    ListRow(R.drawable.ic_lock, stringResource(R.string.settings_privacy), stringResource(R.string.settings_privacy_desc))
                    GroupDivider()
                    LinkRow(R.drawable.ic_code, stringResource(R.string.settings_source), stringResource(R.string.settings_source_desc), external = true) {
                        context.openUrl(Links.REPOSITORY)
                    }
                    GroupDivider()
                    LinkRow(R.drawable.ic_bug, stringResource(R.string.settings_report), stringResource(R.string.settings_report_desc), external = true) {
                        context.openUrl(Links.NEW_ISSUE)
                    }
                    GroupDivider()
                    LinkRow(R.drawable.ic_people, stringResource(R.string.settings_onboarding), onClick = onReplayIntro)
                    GroupDivider()
                    ListRow(R.drawable.ic_shield_ok, stringResource(R.string.settings_version), version)
                }

                SectionTitle(stringResource(R.string.settings_section_developer))
                Group {
                    SwitchRow(
                        R.drawable.ic_bug, stringResource(R.string.settings_developer_mode),
                        stringResource(R.string.settings_developer_mode_desc), settings.debugEnabled,
                    ) { scope.launch { repo.setDebug(it) } }
                    if (settings.debugEnabled) {
                        GroupDivider()
                        LinkRow(R.drawable.ic_code, stringResource(R.string.settings_captures), onClick = onOpenCaptures)
                    }
                }
            }
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            text = { Text(stringResource(R.string.settings_logout_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    onLogOut()
                }) { Text(stringResource(R.string.settings_logout_action)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

/** Big status card at the top: protected, or what is left to do. */
@Composable
private fun StatusHero(ok: Boolean) {
    val colors = LocalStatusColors.current
    val tint = if (ok) colors.good else colors.warning
    Surface(
        color = tint.copy(alpha = 0.12f),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painterResource(if (ok) R.drawable.ic_shield_ok else R.drawable.ic_shield_alert),
                contentDescription = null, tint = tint, modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(if (ok) R.string.settings_status_ok_title else R.string.settings_status_attention_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    stringResource(if (ok) R.string.settings_status_ok_body else R.string.settings_status_attention_body),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
