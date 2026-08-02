plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.termuxai.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.termuxai.app"
        // 26+ so a real foreground service with a notification channel is
        // straightforward, and the wake word listener's foreground
        // microphone service type is well supported.
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.3.0-vosk"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    // Kotlin 1.9.24 -> Compose Compiler 1.5.14 is the version pinned by the
    // official JetBrains/Google compatibility map for this Kotlin version.
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-service:2.8.4")

    // Real Jetpack Compose UI (MainActivity + Dashboard screen)
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.0")

    // Real WebSocket transport
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Real JSON serialization for the wire protocol
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Real on-device wake word / speech engine (Vosk) -- Apache 2.0,
    // no account or API key required, unlike the Picovoice SDK this replaces.
    implementation("com.alphacephei:vosk-android:0.3.75")
    implementation("net.java.dev.jna:jna:5.18.1@aar")
}
