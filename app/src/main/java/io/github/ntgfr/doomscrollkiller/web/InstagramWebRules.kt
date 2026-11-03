package io.github.ntgfr.doomscrollkiller.web

/*
 * ============================================================================
 *  FILTERING RULES FOR INSTAGRAM.COM
 *
 *  This is THE file to edit when instagram.com changes. Turn on developer mode,
 *  browse the built-in feed, then read the page captures (docs/UPDATING_RULES.md).
 *
 *  Prefer what does not depend on the language: link addresses (href) and page
 *  structure. When a text is unavoidable, list every language you know of.
 *
 *  Last verified on a real device: November 2026 (mobile instagram.com, French and English).
 * ============================================================================
 */
object InstagramWebRules {

    /** Start page: the "Following" feed (accounts you follow only, newest first). */
    const val HOME_URL = "https://www.instagram.com/?variant=following"
    const val DIRECT_URL = "https://www.instagram.com/direct/inbox/"
    const val ACTIVITY_URL = "https://www.instagram.com/notifications/"

    /** Sites that stay INSIDE the app (subdomains included). Everything else opens in the browser. */
    private val allowedHosts = listOf("instagram.com", "facebook.com")

    /** Path segments that are never shown: "/reels/…" (Reels tab), "/reel/…" and "/<account>/reel/…". */
    private val blockedSegments = listOf("reels", "reel")

    /** Labels marking a feed post as an ad or a suggestion, in every language we know of. */
    val hidePostTexts = listOf(
        "Sponsored", "Suggested for you", "Suggested posts", // English
        "Sponsorisé", "Sponsorisée", "Suggestions pour vous", "Suggestion pour vous", // French
    )

    /** "Follow" button: a feed post showing it comes from an account you don't follow. */
    val followButtonTexts = listOf("Follow", "Suivre")

    /** aria-label of the button closing a full-screen viewer (used to recognise the viewer). */
    val closeLabels = listOf("Close", "Fermer")

    /**
     * CSS injected into every page. The script tags <html> with the current page
     * (data-dk-page="explore" on Explore), so some rules only apply to one page.
     * On Explore, the grid thumbnails (links to /p/… and /reel/…) are hidden but search keeps working:
     * search results are links to profiles (/account_name/).
     */
    val css = """
        a[href="/reels/"], a[href^="/reels/"] { display: none !important; }
        html[data-dk-page="explore"] main a[href^="/p/"],
        html[data-dk-page="explore"] main a[href*="/reel/"] { display: none !important; }
    """.trimIndent()

    fun isAllowedHost(host: String?): Boolean =
        host != null && allowedHosts.any { host == it || host.endsWith(".$it") }

    fun isBlockedPath(path: String?): Boolean {
        val segments = path?.split('/')?.filter { it.isNotEmpty() } ?: return false
        // "/reels/…" or "/reel/…" first, or "/<account>/reel/…" second.
        return segments.take(2).withIndex().any { (i, seg) -> seg in blockedSegments && (i == 0 || seg == "reel") }
    }

    /** True for the algorithmic "For you" home ("/" without ?variant=following): it is replaced by the Following feed. */
    fun isForYouHome(path: String?, query: String?): Boolean =
        (path.isNullOrEmpty() || path == "/") && query?.contains("variant=following") != true

    /** Kotlin string -> quoted JavaScript string. */
    private fun js(s: String) = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""

    private fun jsArray(items: List<String>) = items.joinToString(",", "[", "]") { js(it) }

    /**
     * Script injected into every page. It adds the CSS, then watches the page (MutationObserver) and hides
     * unwanted elements as they arrive. The browser hides them before drawing them: nothing jumps on screen.
     */
    fun injectionScript(): String = """
        (function () {
          if (window.__doomscrollKiller) { window.__doomscrollKiller.clean(); return; }
          var CSS = ${js(css)};
          var HIDE_TEXTS = ${jsArray(hidePostTexts)};
          var FOLLOW_TEXTS = ${jsArray(followButtonTexts)};
          var CLOSE_LABELS = ${jsArray(closeLabels)};

          function addStyle() {
            if (document.getElementById('dk-style')) return;
            var s = document.createElement('style');
            s.id = 'dk-style';
            s.textContent = CSS;
            (document.head || document.documentElement).appendChild(s);
          }
          function hasText(root, selector, texts) {
            var els = root.querySelectorAll(selector);
            for (var i = 0; i < els.length; i++) {
              if (texts.indexOf((els[i].textContent || '').trim()) >= 0) return true;
            }
            return false;
          }
          function hide(el) {
            el.style.setProperty('display', 'none', 'important');
            el.dataset.dk = 'hidden';
          }

          // Home feed: ads, suggestions and posts from accounts you don't follow.
          function cleanFeed() {
            var posts = document.querySelectorAll('article');
            for (var i = 0; i < posts.length; i++) {
              var p = posts[i];
              if (p.dataset.dk === 'hidden') continue;
              if (hasText(p, 'span, a', HIDE_TEXTS) || hasText(p, 'button, div[role="button"]', FOLLOW_TEXTS)) hide(p);
            }
          }

          // Reel opened from a direct message: Instagram shows it full screen, as the FIRST page of a vertical
          // stack, and appends more Reels below it so you keep scrolling. Keep the first page, hide the others.
          // A stack is: at least 2 siblings, each at least half the screen tall, same height, stacked vertically,
          // inside a full-screen viewer that has a "Close" button.
          function cleanDmViewer() {
            var selector = CLOSE_LABELS.map(function (l) { return 'svg[aria-label="' + l + '"]'; }).join(',');
            var closes = document.querySelectorAll(selector);
            for (var c = 0; c < closes.length; c++) {
              var viewer = closes[c].parentElement;
              while (viewer && viewer.getBoundingClientRect().height < window.innerHeight * 0.9) viewer = viewer.parentElement;
              if (viewer) cleanStacks(viewer);
            }
          }
          function cleanStacks(viewer) {
            var all = viewer.querySelectorAll('div');
            for (var i = 0; i < all.length; i++) {
              var kids = all[i].children;
              if (kids.length < 2 || !isVerticalStack(kids)) continue;
              for (var k = 1; k < kids.length; k++) if (kids[k].dataset.dk !== 'hidden') hide(kids[k]);
            }
          }
          function isVerticalStack(kids) {
            var first = kids[0].getBoundingClientRect();
            if (first.height < window.innerHeight * 0.5) return false;
            for (var k = 1; k < kids.length; k++) {
              if (kids[k].dataset.dk === 'hidden') continue;
              var r = kids[k].getBoundingClientRect();
              if (Math.abs(r.height - first.height) > 2 || Math.abs(r.left - first.left) > 2 || r.top <= first.top) return false;
            }
            return true;
          }

          function clean() {
            addStyle();
            var page = location.pathname.indexOf('/explore') === 0 ? 'explore' : 'other';
            if (document.documentElement.getAttribute('data-dk-page') !== page) {
              document.documentElement.setAttribute('data-dk-page', page);
            }
            // "Home" links go straight to the Following feed, never through "For you".
            var homes = document.querySelectorAll('a[href="/"]');
            for (var h = 0; h < homes.length; h++) homes[h].setAttribute('href', '/?variant=following');
            // Feed filtering only on the home page: on the profile of an account you don't follow,
            // its posts must stay visible.
            if (location.pathname === '/') cleanFeed();
            if (location.pathname.indexOf('/direct/') === 0) cleanDmViewer();
          }

          var pending = false;
          new MutationObserver(function () {
            if (pending) return;
            pending = true;
            requestAnimationFrame(function () { pending = false; clean(); });
          }).observe(document.documentElement, { childList: true, subtree: true });
          window.__doomscrollKiller = { clean: clean };
          clean();
        })();
    """.trimIndent()

    /**
     * Developer mode: describes the page as a text tree (tag, role, aria-label, href, text, position).
     * Instagram's CSS class names are unreadable and change constantly, so they are left out.
     */
    val domDumpScript = """
        (function () {
          var out = [], n = 0;
          function walk(el, d) {
            if (n > 4000) return;
            var r = el.getBoundingClientRect();
            if ((r.width === 0 || r.height === 0) && d > 2) return;
            n++;
            var line = new Array(d + 1).join('  ') + el.tagName.toLowerCase();
            ['role', 'aria-label', 'href', 'id', 'type', 'data-dk'].forEach(function (a) {
              var v = el.getAttribute(a);
              if (v) line += ' ' + a + '="' + v.slice(0, 80) + '"';
            });
            var t = '';
            el.childNodes.forEach(function (c) { if (c.nodeType === 3) t += c.textContent; });
            t = t.trim();
            if (t) line += ' text="' + t.slice(0, 80) + '"';
            line += ' (' + Math.round(r.left) + ',' + Math.round(r.top) + ' ' + Math.round(r.width) + 'x' + Math.round(r.height) + ')';
            out.push(line);
            if (el.tagName.toLowerCase() === 'svg') return;
            for (var i = 0; i < el.children.length; i++) walk(el.children[i], d + 1);
          }
          walk(document.body, 0);
          return location.href + '\n' + out.join('\n');
        })();
    """.trimIndent()
}
