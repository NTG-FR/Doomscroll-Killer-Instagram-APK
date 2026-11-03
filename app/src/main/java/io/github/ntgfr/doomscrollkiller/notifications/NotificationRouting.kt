package io.github.ntgfr.doomscrollkiller.notifications

import io.github.ntgfr.doomscrollkiller.web.InstagramWebRules

/**
 * Pure logic (no Android): decides how a relayed Instagram notification is shown and where it leads.
 * Tested in NotificationRoutingTest.
 */
object NotificationRouting {

    /** Instagram's own channel for direct messages (seen on real notifications). */
    private const val DIRECT_CHANNEL = "ig_direct"
    /** Android's Notification.CATEGORY_MESSAGE, duplicated here so this file stays Android-free. */
    private const val CATEGORY_MESSAGE = "msg"

    /** Direct message, or other activity (likes, comments, follows…)? */
    fun isDirectMessage(channelId: String?, category: String?): Boolean {
        val channel = channelId.orEmpty().lowercase()
        return category == CATEGORY_MESSAGE || channel == DIRECT_CHANNEL || "direct" in channel
    }

    /**
     * Page to open when the notification is tapped: the conversation itself when Instagram tells us which
     * one ("thread_id:<number>" in the notification group), otherwise the inbox or the activity page.
     */
    fun targetUrl(isDirectMessage: Boolean, notificationGroup: String?): String = when {
        isDirectMessage -> threadUrl(notificationGroup) ?: InstagramWebRules.DIRECT_URL
        else -> InstagramWebRules.ACTIVITY_URL
    }

    fun threadUrl(notificationGroup: String?): String? {
        val id = notificationGroup?.let { Regex("thread_id:(\\d+)").find(it)?.groupValues?.get(1) } ?: return null
        return "https://www.instagram.com/direct/t/$id/"
    }
}
