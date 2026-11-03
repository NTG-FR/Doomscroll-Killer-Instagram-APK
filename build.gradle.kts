// Root build file: only declares plugin versions. They are applied in app/build.gradle.kts.
plugins {
    id("com.android.application") version "9.5.0" apply false
    // The Compose compiler plugin version must match the Kotlin version.
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
}
