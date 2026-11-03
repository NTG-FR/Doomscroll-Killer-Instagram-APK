# Contributing to Doomscroll Killer

Thanks for helping! The most useful contributions are:

- **Rule updates** when Instagram changes its website: see [docs/UPDATING_RULES.md](docs/UPDATING_RULES.md).
- **More languages** in the rule labels (`hidePostTexts`, `followButtonTexts`, `closeLabels`).
- **Phone-brand tips** for devices that stop background apps.
- Bug reports with clear steps to reproduce.

## Ground rules

- **Privacy first.** The app must never send data anywhere, add analytics, or read content from other apps. Pull requests that do will not be merged.
- **No scraping, no automated requests to Instagram.** The app only displays instagram.com and hides parts of it locally.
- Keep the scope focused: distraction-free Instagram. One app, simple settings.

## Development setup

- Android Studio (latest stable), or JDK 17+ and the Android SDK.
- `./gradlew assembleDebug` builds the debug APK (`io.github.ntgfr.doomscrollkiller.debug`, installs next to a release build).
- `./gradlew test lintDebug` must pass before you open a pull request. CI runs the same commands.

## Code style

- Kotlin, Jetpack Compose, Material 3. No new dependency without a good reason.
- Follow the existing style: small files, comments explaining *why*, user-facing text in `res/values/strings.xml`.
- Pure logic (rules, routing) stays free of Android classes so it can be unit-tested.

## Pull requests

1. Fork, create a branch, make your change.
2. Add or update tests when possible.
3. Describe what you changed and how you tested it (device, Android version, Instagram language).

## Release signing

Maintainers sign releases with a private keystore. Locally, create `keystore.properties` at the project root (it is git-ignored):

```properties
storeFile=/absolute/path/to/release.jks
storePassword=…
keyAlias=…
keyPassword=…
```

In GitHub Actions, the release workflow reads the same values from repository secrets: `SIGNING_KEYSTORE_BASE64`, `SIGNING_KEYSTORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`.

## License

By contributing, you agree that your contributions are licensed under the [GPL-3.0](LICENSE).
