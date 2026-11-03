package io.github.ntgfr.doomscrollkiller.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import io.github.ntgfr.doomscrollkiller.R
import io.github.ntgfr.doomscrollkiller.system.DeviceBrand
import io.github.ntgfr.doomscrollkiller.system.ServiceState
import io.github.ntgfr.doomscrollkiller.system.SetupChecks
import io.github.ntgfr.doomscrollkiller.system.SetupStatus
import io.github.ntgfr.doomscrollkiller.ui.theme.LocalStatusColors
import kotlinx.coroutines.delay

/**
 * Live system status: refreshed every second while the screen is visible, so a permission granted in
 * system settings shows up as soon as the user comes back.
 */
@Composable
fun rememberSetupStatus(): SetupStatus {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var status by remember { mutableStateOf(SetupChecks.status(context)) }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                status = SetupChecks.status(context)
                delay(1_000)
            }
        }
    }
    return status
}

/** True when every step that matters on this phone is done. */
fun SetupStatus.ready(): Boolean = if (instagramInstalled) allGood else batteryUnrestricted

/** The list of setup steps, each with its state and a button to fix it. */
@Composable
fun SetupChecklist(status: SetupStatus) {
    val context = LocalContext.current
    var showDisclosure by rememberSaveable { mutableStateOf(false) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // Android stops showing the dialog after two refusals: send the user to the settings instead.
        if (!granted) context.startActivity(SetupChecks.appNotificationSettings(context))
    }

    Group {
        if (!status.instagramInstalled) {
            ListRow(
                icon = R.drawable.ic_notifications,
                title = stringResource(R.string.setup_no_instagram_title),
                description = stringResource(R.string.setup_no_instagram_desc),
                iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            GroupDivider()
        } else {
            val serviceOk = status.service == ServiceState.RUNNING
            SetupItem(
                icon = R.drawable.ic_accessibility,
                title = stringResource(R.string.setup_redirect_title),
                description = stringResource(
                    if (status.service == ServiceState.ENABLED_NOT_RUNNING) R.string.setup_redirect_stopped
                    else R.string.setup_redirect_desc
                ),
                done = serviceOk,
                onFix = { showDisclosure = true },
            ) {
                if (!serviceOk && SetupChecks.mayNeedRestrictedSettings(context)) {
                    Text(
                        stringResource(R.string.setup_restricted_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = { context.startActivity(SetupChecks.appDetails(context)) }) {
                        Text(stringResource(R.string.setup_restricted_button))
                    }
                }
            }
            GroupDivider()
            SetupItem(
                icon = R.drawable.ic_notifications,
                title = stringResource(R.string.setup_notif_access_title),
                description = stringResource(R.string.setup_notif_access_desc),
                done = status.notificationAccess,
                onFix = { context.startActivity(SetupChecks.notificationAccessSettings(context)) },
            )
            GroupDivider()
            SetupItem(
                icon = R.drawable.ic_notifications,
                title = stringResource(R.string.setup_post_notif_title),
                description = stringResource(R.string.setup_post_notif_desc),
                done = status.canPostNotifications,
                onFix = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        context.startActivity(SetupChecks.appNotificationSettings(context))
                    }
                },
            )
            GroupDivider()
        }
        SetupItem(
            icon = R.drawable.ic_battery,
            title = stringResource(R.string.setup_battery_title),
            description = stringResource(R.string.setup_battery_desc),
            done = status.batteryUnrestricted,
            onFix = { context.startSafely(SetupChecks.batteryOptimizationRequest(context), SetupChecks.batteryOptimizationList()) },
        )
    }

    if (showDisclosure) {
        // Prominent disclosure, shown before sending the user to the accessibility settings.
        AlertDialog(
            onDismissRequest = { showDisclosure = false },
            icon = { Icon(painterResource(R.drawable.ic_accessibility), contentDescription = null) },
            title = { Text(stringResource(R.string.disclosure_title)) },
            text = { Text(stringResource(R.string.disclosure_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showDisclosure = false
                    context.startActivity(SetupChecks.accessibilitySettings())
                }) { Text(stringResource(R.string.action_open_settings)) }
            },
            dismissButton = {
                TextButton(onClick = { showDisclosure = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun SetupItem(
    @DrawableRes icon: Int,
    title: String,
    description: String,
    done: Boolean,
    onFix: () -> Unit,
    extra: @Composable () -> Unit = {},
) {
    val status = LocalStatusColors.current
    Column {
        ListRow(
            icon = icon,
            title = title,
            description = description,
            iconTint = if (done) status.good else status.warning,
            onClick = if (done) null else onFix,
        ) {
            if (done) {
                Icon(
                    painterResource(R.drawable.ic_check_circle),
                    contentDescription = stringResource(R.string.status_done),
                    tint = status.good,
                    modifier = Modifier.size(24.dp),
                )
            } else {
                FilledTonalButton(onClick = onFix) { Text(stringResource(R.string.action_fix)) }
            }
        }
        if (!done) Column(Modifier.padding(start = 66.dp, end = 16.dp, bottom = 8.dp)) { extra() }
    }
}

/** Brand-specific advice (background limits, hiding Instagram's icon), with a link to the full guide. */
@Composable
fun DeviceTipsCard(brand: DeviceBrand, manufacturerName: String) {
    val context = LocalContext.current
    val tips = when (brand) {
        DeviceBrand.XIAOMI -> R.string.tips_xiaomi
        DeviceBrand.SAMSUNG -> R.string.tips_samsung
        DeviceBrand.HUAWEI -> R.string.tips_huawei
        DeviceBrand.ONEPLUS -> R.string.tips_oneplus
        DeviceBrand.OPPO -> R.string.tips_oppo
        DeviceBrand.VIVO -> R.string.tips_vivo
        DeviceBrand.OTHER -> R.string.tips_other
    }
    Group {
        ListRow(
            icon = R.drawable.ic_visibility_off,
            title = if (brand == DeviceBrand.OTHER) stringResource(R.string.tips_title_generic)
                else stringResource(R.string.tips_title, manufacturerName),
            description = stringResource(tips),
        )
        if (brand != DeviceBrand.OTHER) {
            GroupDivider()
            LinkRow(R.drawable.ic_open_in_new, stringResource(R.string.tips_full_guide), external = true) {
                context.openUrl(brand.guideUrl)
            }
        }
    }
}

fun Context.openUrl(url: String) = startSafely(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

/** Starts [intent], or [fallback] if no app handles it (some phones remove settings screens). */
fun Context.startSafely(intent: Intent, fallback: Intent? = null) {
    try {
        startActivity(intent)
    } catch (_: android.content.ActivityNotFoundException) {
        fallback?.let { runCatching { startActivity(it) } }
    }
}
