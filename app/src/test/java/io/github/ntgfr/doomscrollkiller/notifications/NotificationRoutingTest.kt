package io.github.ntgfr.doomscrollkiller.notifications

import io.github.ntgfr.doomscrollkiller.web.InstagramWebRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationRoutingTest {

    // Same shape as a real Instagram DM notification group (IDs are made up).
    private val dmGroup = "1000000001-ig_direct-direct-1234567890;thread_id:340282366841710300000000000000000001"

    @Test
    fun directMessages_areRecognised() {
        assertTrue(NotificationRouting.isDirectMessage("ig_direct", "msg"))
        assertTrue(NotificationRouting.isDirectMessage("ig_direct", null))
        assertTrue(NotificationRouting.isDirectMessage("some_channel", "msg"))
        assertFalse(NotificationRouting.isDirectMessage("ig_heads_up_comments", "social"))
        assertFalse(NotificationRouting.isDirectMessage(null, null))
    }

    @Test
    fun message_opensItsConversation() {
        assertEquals(
            "https://www.instagram.com/direct/t/340282366841710300000000000000000001/",
            NotificationRouting.targetUrl(isDirectMessage = true, notificationGroup = dmGroup),
        )
    }

    @Test
    fun messageWithoutThread_opensInbox() {
        assertEquals(InstagramWebRules.DIRECT_URL, NotificationRouting.targetUrl(true, "ig_direct"))
        assertEquals(InstagramWebRules.DIRECT_URL, NotificationRouting.targetUrl(true, null))
    }

    @Test
    fun otherActivity_opensNotificationsPage() {
        assertEquals(InstagramWebRules.ACTIVITY_URL, NotificationRouting.targetUrl(false, dmGroup))
    }

    @Test
    fun threadUrl_needsANumericThreadId() {
        assertNull(NotificationRouting.threadUrl("thread_id:abc"))
        assertNull(NotificationRouting.threadUrl(null))
    }
}
