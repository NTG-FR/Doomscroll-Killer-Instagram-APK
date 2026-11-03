# Changelog

All notable changes to this project are documented here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow [Semantic Versioning](https://semver.org/).

## [1.1.0] - 2026-11-03

### Added
- Two home screen icons from one install: **Insta** opens the feed full screen, **Doomscroll Killer** holds
  setup and settings. The redirect and relayed notifications open Insta.

### Changed
- The feed has no app bar anymore: it looks like Instagram, minus the doomscroll. A slim banner shows up
  only when protection needs attention.

### Fixed
- The feed could stay greyed out and ignore taps: Instagram's cookie consent window collapsed to nothing,
  leaving only its dark backdrop. The built-in feed now always reports its real screen size to the page.
- Opening the official Instagram app several times in a row could get past the redirect.
  Every time Instagram shows up, the feed is now brought back on top, with no delay.

### Changed
- Setup: the Accessibility settings highlight Doomscroll Killer's entry when the phone supports it, and the
  explanation lists the exact steps for Xiaomi, Samsung and other phones.
- Settings shortcuts fall back gracefully on phones that removed them.

## [1.0.0] - 2026-11-03

First public release.

### Added
- Built-in Instagram feed (instagram.com) opening on the chronological *Following* feed.
- Reels tab, Reels in the feed and the Explore grid removed; search kept.
- Ads, suggested posts and posts from accounts you don't follow hidden from the home feed.
- Reels received in direct messages play, without scrolling to more Reels.
- Redirect from the official Instagram app to the built-in feed (accessibility service that cannot read the screen).
- Instagram notifications relayed as Doomscroll Killer notifications, opening the right conversation.
- Guided setup with live permission status and per-brand background tips (Xiaomi, Samsung, Huawei, OnePlus, Oppo, Vivo).
- Developer mode with page captures, to update the filtering rules when Instagram changes.

[1.1.0]: https://github.com/NTG-FR/Doomscroll-Killer-Instagram-APK/releases/tag/v1.1.0
[1.0.0]: https://github.com/NTG-FR/Doomscroll-Killer-Instagram-APK/releases/tag/v1.0.0
