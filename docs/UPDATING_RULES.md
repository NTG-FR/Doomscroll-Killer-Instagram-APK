# Updating the filtering rules

Instagram changes its website regularly. When something slips through (a Reel, the Explore grid, an ad…), the fix is almost always a small change in **one file**:

[`app/src/main/java/io/github/ntgfr/doomscrollkiller/web/InstagramWebRules.kt`](../app/src/main/java/io/github/ntgfr/doomscrollkiller/web/InstagramWebRules.kt)

## What the rules can do

| Rule | Where | Example |
|---|---|---|
| Block an address | `blockedSegments`, `isBlockedPath()` | `/reels/…`, `/<account>/reel/…` |
| Hide with CSS | `css` | `a[href="/reels/"]`, the Explore grid |
| Hide a feed post by label | `hidePostTexts`, `followButtonTexts` | "Sponsored", "Suggested for you", "Follow" |
| Clean the DM Reel viewer | `cleanDmViewer()` in `injectionScript()` | keeps the Reel you were sent, hides the ones stacked below |

**Prefer rules that don't depend on the language**: link addresses (`href`), `role`, page structure. Instagram's CSS class names are generated and change with every release: never rely on them. When a text is unavoidable, add it in every language you can.

## 1. Capture the page

1. Build and install a **debug** build (see the README).
2. In the app: **Settings → Developer → Developer mode** on.
3. Go to the page that misbehaves and wait a few seconds: a capture is saved every 4 seconds.
4. **Settings → Developer → Page captures** shows them in the app, with a **Copy** button.

From a computer, with the phone connected over USB:

```bash
adb shell run-as io.github.ntgfr.doomscrollkiller.debug ls files/debug
adb shell run-as io.github.ntgfr.doomscrollkiller.debug cat files/debug/143205_page.txt
```

> Captures contain what was on screen (usernames, captions). Don't paste them publicly without removing personal information.

## 2. Read a capture

Each line is an element of the page, indented by depth:

```
a role="link" href="/explore/" (0,739 77x48)
  svg role="img" aria-label="Explore" (26,751 24x24)
article (0,124 412x655)
  span text="Sponsored" (60,140 70x16)
```

- `href`, `role`, `aria-label`: stable hooks for rules.
- `text`: visible text (language-dependent).
- `data-dk="hidden"`: already hidden by a rule.
- `(x,y widthxheight)`: position and size in CSS pixels.

Compare a capture of a page that **should** be filtered with one that **must not** be (your feed, a conversation, a profile).

## 3. Change the rule and test it

1. Edit `InstagramWebRules.kt`.
2. Add or update a test in [`InstagramWebRulesTest.kt`](../app/src/test/java/io/github/ntgfr/doomscrollkiller/web/InstagramWebRulesTest.kt) when the rule is testable without a browser (addresses, labels).
3. Run the checks and install:

```bash
./gradlew test lintDebug assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

4. Check the page again, **and** check that the feed, messages, a profile and search still work.

## 4. Share the fix

Open a pull request with a short description of what Instagram changed. Mention your Instagram language and the date you tested.
