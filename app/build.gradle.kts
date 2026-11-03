import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

/**
 * Release signing, read from (in order):
 *  1. keystore.properties at the project root (local builds, never committed), or
 *  2. environment variables (GitHub Actions secrets).
 * Without either, release builds are left unsigned.
 */
val signing: Map<String, String?> = run {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) {
        val p = Properties().apply { file.inputStream().use(::load) }
        mapOf(
            "storeFile" to p.getProperty("storeFile"),
            "storePassword" to p.getProperty("storePassword"),
            "keyAlias" to p.getProperty("keyAlias"),
            "keyPassword" to p.getProperty("keyPassword"),
        )
    } else {
        mapOf(
            "storeFile" to System.getenv("SIGNING_KEYSTORE_PATH"),
            "storePassword" to System.getenv("SIGNING_KEYSTORE_PASSWORD"),
            "keyAlias" to System.getenv("SIGNING_KEY_ALIAS"),
            "keyPassword" to System.getenv("SIGNING_KEY_PASSWORD"),
        )
    }
}
val hasSigning = signing.values.all { !it.isNullOrBlank() }

android {
    namespace = "io.github.ntgfr.doomscrollkiller"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.ntgfr.doomscrollkiller"
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        versionName = "1.1.0"
    }

    signingConfigs {
        if (hasSigning) {
            create("release") {
                storeFile = rootProject.file(signing.getValue("storeFile")!!)
                storePassword = signing["storePassword"]
                keyAlias = signing["keyAlias"]
                keyPassword = signing["keyPassword"]
            }
        }
    }

    buildTypes {
        release {
            // R8 shrinks the APK from ~12 MB to a few MB.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasSigning) signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    lint {
        // Sideloaded app by design: battery-optimisation request and accessibility use are intentional.
        disable += setOf("BatteryLife")
        // Suggests androidx.core KTX helpers; not worth a dependency for three Uri.parse calls.
        disable += setOf("UseKtx")
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.datastore:datastore-preferences:1.3.0")

    testImplementation("junit:junit:4.13.2")
}
