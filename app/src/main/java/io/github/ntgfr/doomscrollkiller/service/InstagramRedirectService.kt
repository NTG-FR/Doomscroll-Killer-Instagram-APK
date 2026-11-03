package io.github.ntgfr.doomscrollkiller.service

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import io.github.ntgfr.doomscrollkiller.data.AppSettings
import io.github.ntgfr.doomscrollkiller.data.SettingsRepository
import io.github.ntgfr.doomscrollkiller.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Accessibility service that sends you to the built-in feed whenever the official Instagram app opens.
 *
 * It only listens to "window changed" events from Instagram (see accessibility_service_config.xml)
 * and is NOT allowed to read the content of the screen (canRetrieveWindowContent="false").
 */
class InstagramRedirectService : AccessibilityService() {

    companion object {
        private const val TAG = "DoomscrollKiller"
        const val INSTAGRAM_PACKAGE = "com.instagram.android"
        /** Ignore the burst of events Instagram sends while it is being closed. */
        private const val COOLDOWN_MS = 1_500L

        /**
         * True while the service is actually running in this process. A service can be ticked in the
         * system settings yet stopped (some phones kill apps swiped away from Recents): this tells them apart.
         */
        @Volatile
        var isRunning = false
            private set
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Volatile
    private var settings = AppSettings()
    private var lastRedirectAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        val repo = SettingsRepository(this)
        scope.launch { repo.settings.collect { settings = it } }
        scope.launch { repo.setServiceWasEnabled(true) }
        Log.i(TAG, "Redirect service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (event.packageName?.toString() != INSTAGRAM_PACKAGE) return
        if (!settings.instagramRedirect) return

        val now = SystemClock.uptimeMillis()
        if (now - lastRedirectAt < COOLDOWN_MS) return
        lastRedirectAt = now

        // Leave Instagram, then open the built-in feed on top.
        performGlobalAction(GLOBAL_ACTION_HOME)
        startActivity(MainActivity.feedIntent(this))
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        isRunning = false
        scope.cancel()
        super.onDestroy()
    }
}
