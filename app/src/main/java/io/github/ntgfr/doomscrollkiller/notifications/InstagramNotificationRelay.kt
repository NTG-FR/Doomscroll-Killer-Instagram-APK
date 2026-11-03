package io.github.ntgfr.doomscrollkiller.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import io.github.ntgfr.doomscrollkiller.R
import io.github.ntgfr.doomscrollkiller.data.AppSettings
import io.github.ntgfr.doomscrollkiller.data.SettingsRepository
import io.github.ntgfr.doomscrollkiller.service.InstagramRedirectService
import io.github.ntgfr.doomscrollkiller.ui.FeedActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Notification relay: the official Instagram app (kept installed, hidden) still receives push
 * notifications. We re-post them as Doomscroll Killer notifications and remove the original, so tapping
 * one opens the built-in feed instead of the official app.
 *
 * Requires "Notification access", granted by the user in Android settings.
 */
class InstagramNotificationRelay : NotificationListenerService() {

    companion object {
        const val CHANNEL_MESSAGES = "instagram_messages"
        const val CHANNEL_ACTIVITY = "instagram_activity"
        private const val GROUP = "instagram"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Volatile
    private var settings = AppSettings()

    override fun onListenerConnected() {
        super.onListenerConnected()
        scope.launch { SettingsRepository(this@InstagramNotificationRelay).settings.collect { settings = it } }
        createChannels()
        // Notifications posted before the relay started.
        activeNotifications?.forEach(::relay)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) = relay(sbn)

    private fun relay(sbn: StatusBarNotification) {
        if (sbn.packageName != InstagramRedirectService.INSTAGRAM_PACKAGE) return
        if (!settings.notificationRelay) return
        val n = sbn.notification
        if (settings.debugEnabled) logForDebug(sbn)

        // Upload progress and other ongoing notifications: leave them alone.
        if (n.flags and Notification.FLAG_ONGOING_EVENT != 0) return
        // Group summaries are useless once re-posted.
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) {
            cancelNotification(sbn.key)
            return
        }

        val extras = n.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT))
            ?.toString()
        if (title.isNullOrBlank() && text.isNullOrBlank()) return

        val isMessage = NotificationRouting.isDirectMessage(n.channelId, n.category)
        val channelId = if (isMessage) CHANNEL_MESSAGES else CHANNEL_ACTIVITY
        // If our copy cannot be shown (permission denied, channel turned off), keep Instagram's original:
        // an "Instagram" notification is better than no notification at all.
        if (!canNotify(channelId)) return

        val id = sbn.key.hashCode()
        val url = NotificationRouting.targetUrl(isMessage, n.group)
        val open = PendingIntent.getActivity(
            this, id, FeedActivity.intent(this, url),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val copy = Notification.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setWhen(n.`when`)
            .setShowWhen(true)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setGroup(GROUP)
            .setCategory(if (isMessage) Notification.CATEGORY_MESSAGE else Notification.CATEGORY_SOCIAL)
            .build()
        getSystemService(NotificationManager::class.java).notify(id, copy)
        cancelNotification(sbn.key)
    }

    private fun canNotify(channelId: String): Boolean {
        val nm = getSystemService(NotificationManager::class.java)
        if (!nm.areNotificationsEnabled()) return false
        val channel = nm.getNotificationChannel(channelId) ?: return false
        return channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    private fun createChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_MESSAGES, getString(R.string.channel_messages), NotificationManager.IMPORTANCE_HIGH)
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ACTIVITY, getString(R.string.channel_activity), NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    /** Debug mode: records the notification's structure (never its text) to help tune the routing. */
    private fun logForDebug(sbn: StatusBarNotification) {
        val n = sbn.notification
        val line = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date()) +
            " channel=${n.channelId} category=${n.category} flags=${n.flags} group=${n.group} " +
            "extras=${n.extras.keySet().sorted()}\n"
        scope.launch(Dispatchers.IO) {
            val dir = File(filesDir, "debug").apply { mkdirs() }
            File(dir, "notifications.log").appendText(line)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
