package io.github.ntgfr.doomscrollkiller.web

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstagramWebRulesTest {

    @Test
    fun allowedHosts_instagramAndSubdomainsOnly() {
        assertTrue(InstagramWebRules.isAllowedHost("www.instagram.com"))
        assertTrue(InstagramWebRules.isAllowedHost("instagram.com"))
        assertTrue(InstagramWebRules.isAllowedHost("accountscenter.instagram.com"))
        assertFalse(InstagramWebRules.isAllowedHost("example.com"))
        assertFalse(InstagramWebRules.isAllowedHost("fakeinstagram.com"))
        assertFalse(InstagramWebRules.isAllowedHost(null))
    }

    @Test
    fun reelPaths_areBlocked() {
        assertTrue(InstagramWebRules.isBlockedPath("/reels/"))
        assertTrue(InstagramWebRules.isBlockedPath("/reels"))
        assertTrue(InstagramWebRules.isBlockedPath("/reels/abc123/"))
        assertTrue(InstagramWebRules.isBlockedPath("/reel/abc123/"))
        // A Reel opened from the feed: /<account>/reel/<id>/ (seen on real captures).
        assertTrue(InstagramWebRules.isBlockedPath("/some.account/reel/Abc123xYz/"))
    }

    @Test
    fun otherPaths_areAllowed() {
        assertFalse(InstagramWebRules.isBlockedPath("/some.account/p/Abc123xYz/"))
        assertFalse(InstagramWebRules.isBlockedPath("/direct/inbox/"))
        assertFalse(InstagramWebRules.isBlockedPath("/p/abc123/"))
        assertFalse(InstagramWebRules.isBlockedPath("/explore/"))
        // An account whose name starts with "reels".
        assertFalse(InstagramWebRules.isBlockedPath("/reelsfan/"))
        assertFalse(InstagramWebRules.isBlockedPath(null))
    }

    @Test
    fun forYouHome_isDetected_followingFeedIsNot() {
        assertTrue(InstagramWebRules.isForYouHome("/", null))
        assertTrue(InstagramWebRules.isForYouHome("", null))
        assertTrue(InstagramWebRules.isForYouHome("/", "variant=home"))
        assertFalse(InstagramWebRules.isForYouHome("/", "variant=following"))
        assertFalse(InstagramWebRules.isForYouHome("/direct/inbox/", null))
    }

    @Test
    fun injectedScripts_embedEveryLanguage() {
        val script = InstagramWebRules.injectionScript()
        (InstagramWebRules.hidePostTexts + InstagramWebRules.followButtonTexts + InstagramWebRules.closeLabels).forEach {
            assertTrue("missing $it", script.contains("\"$it\""))
        }
        // No Kotlin template left unresolved.
        assertFalse(script.contains("\${"))
    }
}
