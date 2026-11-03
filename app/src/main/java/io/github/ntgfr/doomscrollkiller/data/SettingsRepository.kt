package io.github.ntgfr.doomscrollkiller.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Every user setting, as one immutable object. */
data class AppSettings(
    /** Close the official Instagram app as soon as it opens and show the built-in feed instead. */
    val instagramRedirect: Boolean = true,
    /** Re-post Instagram notifications as Doomscroll Killer notifications that open the built-in feed. */
    val notificationRelay: Boolean = true,
    /** Developer mode: saves page captures to help update the filtering rules. */
    val debugEnabled: Boolean = false,
    /** True once the accessibility service has run (tells "never enabled" from "stopped by the system"). */
    val serviceWasEnabled: Boolean = false,
    val onboardingDone: Boolean = false,
)

// One single DataStore file for the whole process.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Reads and writes settings with DataStore (a local file: nothing leaves the device). */
class SettingsRepository(context: Context) {

    private val store = context.applicationContext.dataStore

    private object Keys {
        val INSTAGRAM_REDIRECT = booleanPreferencesKey("instagram_redirect")
        val NOTIFICATION_RELAY = booleanPreferencesKey("notification_relay")
        val DEBUG = booleanPreferencesKey("debug_enabled")
        val SERVICE_WAS_ENABLED = booleanPreferencesKey("service_was_enabled")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    }

    /** Emits the current settings, then a new value after every change. */
    val settings: Flow<AppSettings> = store.data.map { p ->
        val d = AppSettings()
        AppSettings(
            instagramRedirect = p[Keys.INSTAGRAM_REDIRECT] ?: d.instagramRedirect,
            notificationRelay = p[Keys.NOTIFICATION_RELAY] ?: d.notificationRelay,
            debugEnabled = p[Keys.DEBUG] ?: d.debugEnabled,
            serviceWasEnabled = p[Keys.SERVICE_WAS_ENABLED] ?: d.serviceWasEnabled,
            onboardingDone = p[Keys.ONBOARDING_DONE] ?: d.onboardingDone,
        )
    }

    suspend fun setInstagramRedirect(value: Boolean) = store.edit { it[Keys.INSTAGRAM_REDIRECT] = value }
    suspend fun setNotificationRelay(value: Boolean) = store.edit { it[Keys.NOTIFICATION_RELAY] = value }
    suspend fun setDebug(value: Boolean) = store.edit { it[Keys.DEBUG] = value }
    suspend fun setServiceWasEnabled(value: Boolean) = store.edit { it[Keys.SERVICE_WAS_ENABLED] = value }
    suspend fun setOnboardingDone(value: Boolean) = store.edit { it[Keys.ONBOARDING_DONE] = value }
}
