import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Only apply google-services plugin when the config file is present, so the
// project builds without Firebase keys during local dev / CI smoke tests.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

// Load signing credentials from keystore.properties (gitignored) or env vars.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) load(FileInputStream(f))
}
fun secret(key: String): String? =
    keystoreProps.getProperty(key) ?: System.getenv(key)

android {
    namespace = "com.tripwave.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tripwave.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "BASE_URL", "\"https://www.tripwave.co\"")
        // Shared mobile-client secret. Server-side check is in
        // src/lib/bot-protection.ts (isTrustedMobileClient). When this
        // header value matches MOBILE_CLIENT_SECRET on the server, the
        // /api/auth/send-otp Cloudflare Turnstile gate is skipped.
        // Sourced from keystore.properties / env var TRIPWAVE_MOBILE_SECRET
        // — falls back to empty so unconfigured local builds compile,
        // sign-in just hits the Turnstile gate (which native can't satisfy
        // → login fails with 'Verification required'). Configure in
        // production before release.
        buildConfigField(
            "String",
            "MOBILE_CLIENT_SECRET",
            "\"${secret("TRIPWAVE_MOBILE_SECRET").orEmpty()}\""
        )
        // Supabase Realtime credentials for the typing channel. Empty string
        // means realtime typing is disabled — the SupabaseBackend init returns
        // null and ChatScreen falls back to the REST typing poll. Configure
        // these in keystore.properties / env vars TRIPWAVE_SUPABASE_URL +
        // TRIPWAVE_SUPABASE_ANON_KEY to enable.
        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"${secret("TRIPWAVE_SUPABASE_URL").orEmpty()}\""
        )
        buildConfigField(
            "String",
            "SUPABASE_ANON_KEY",
            "\"${secret("TRIPWAVE_SUPABASE_ANON_KEY").orEmpty()}\""
        )
    }

    signingConfigs {
        create("release") {
            val storePath = secret("TRIPWAVE_STORE_FILE")
            if (!storePath.isNullOrBlank()) {
                storeFile = file(storePath)
                storePassword = secret("TRIPWAVE_STORE_PASSWORD")
                keyAlias = secret("TRIPWAVE_KEY_ALIAS")
                keyPassword = secret("TRIPWAVE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Only attach the signing config if a keystore was provided;
            // this lets `assembleRelease` still run (unsigned) on CI without secrets.
            if (!secret("TRIPWAVE_STORE_FILE").isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        // We're a pure-Compose / ComponentActivity app — no Fragment
        // usage anywhere. The lint check fires on any
        // registerForActivityResult call inside a ComponentActivity
        // because older FragmentActivity versions had a bug, but the
        // base ComponentActivity path is fine.
        disable.add("InvalidFragmentVersionForActivityResult")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.okhttp.sse)
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.realtime)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coroutines.android)
    implementation(libs.datastore.preferences)
    implementation(libs.coil.compose)
    implementation(libs.gson)
    // Glance — Jetpack Compose-based home-screen widgets. Used by
    // com.tripwave.app.widget for the "next trip" countdown widget.
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    // Firebase (requires google-services.json in app/ directory - get from Firebase Console)
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
