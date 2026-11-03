package io.github.ntgfr.doomscrollkiller.system

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import io.github.ntgfr.doomscrollkiller.notifications.InstagramNotificationRelay
import io.github.ntgfr.doomscrollkiller.service.InstagramRedirectService

/** State of the accessibility service. */
enum class ServiceState {
    /** Enabled in system settings AND running. */
    RUNNING,
    /** Enabled in system settings but stopped (some phones kill apps swiped away from Recents). */
    ENABLED_NOT_RUNNING,
    DISABLED,
}

/** Snapshot of everything the app needs from the system. */
data class SetupStatus(
    val instagramInstalled: Boolean,
    val service: ServiceState,
    val notificationAccess: Boolean,
    val canPostNotifications: Boolean,
    val batteryUnrestricted: Boolean,
) {
    /** Redirect + notifications fully working. */
    val allGood: Boolean
        get() = service == ServiceState.RUNNING && notificationAccess && canPostNotifications && batteryUnrestricted
}

/** Reads the system state and builds the intents that open the right settings screens. */
object SetupChecks {

    fun status(context: Context) = SetupStatus(
        instagramInstalled = isInstagramInstalled(context),
        service = serviceState(context),
        notificationAccess = isNotificationAccessGranted(context),
        canPostNotifications = context.getSystemService(NotificationManager::class.java).areNotificationsEnabled(),
        batteryUnrestricted = context.getSystemService(PowerManager::class.java)
            .isIgnoringBatteryOptimizations(context.packageName),
    )

    fun serviceState(context: Context): ServiceState = when {
        !isListedInSecureSetting(context, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, InstagramRedirectService::class.java) ->
            ServiceState.DISABLED
        InstagramRedirectService.isRunning -> ServiceState.RUNNING
        else -> ServiceState.ENABLED_NOT_RUNNING
    }

    fun isNotificationAccessGranted(context: Context): Boolean =
        isListedInSecureSetting(context, "enabled_notification_listeners", InstagramNotificationRelay::class.java)

    fun isInstagramInstalled(context: Context): Boolean = try {
        context.packageManager.getPackageInfo(InstagramRedirectService.INSTAGRAM_PACKAGE, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    /**
     * True when Android 13+ may grey out the accessibility switch ("Restricted setting"): this happens for
     * apps installed from a downloaded file rather than from a store or adb.
     */
    fun mayNeedRestrictedSettings(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        val source = try {
            context.packageManager.getInstallSourceInfo(context.packageName).packageSource
        } catch (_: PackageManager.NameNotFoundException) {
            return false
        }
        // Only files opened from a browser or a file manager are restricted (not stores, not adb).
        return source == PackageInstaller.PACKAGE_SOURCE_DOWNLOADED_FILE ||
            source == PackageInstaller.PACKAGE_SOURCE_LOCAL_FILE
    }

    private fun isListedInSecureSetting(context: Context, key: String, component: Class<*>): Boolean {
        val value = Settings.Secure.getString(context.contentResolver, key) ?: return false
        val me = ComponentName(context, component)
        return value.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    // ------------------------------------------------------------------ Intents

    fun accessibilitySettings() = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).newTask()

    /** Opens our own entry directly when the system allows it (Android 11+), else the full list. */
    fun notificationAccessSettings(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(
                    Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                    ComponentName(context, InstagramNotificationRelay::class.java).flattenToString(),
                ).newTask()
        } else {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).newTask()
        }

    fun appNotificationSettings(context: Context) = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName).newTask()

    /** System dialog "Let app always run in background?" (falls back to the list if unavailable). */
    @SuppressLint("BatteryLife") // Sideloaded app whose whole point is to keep running; not a Play Store app.
    fun batteryOptimizationRequest(context: Context) =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}")).newTask()

    fun batteryOptimizationList() = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).newTask()

    fun appDetails(context: Context, packageName: String = context.packageName) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")).newTask()

    private fun Intent.newTask(): Intent = addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
