/*
 * ============================================================================
 * GARAGE - STRICT 100% FOSS & F-DROID COMPLIANCE POLICY
 * ============================================================================
 * This application is strictly Free and Open Source Software (FOSS) built for F-Droid.
 *
 * DO NOT ADD:
 *  - Google Play Services (com.google.android.gms:*)
 *  - Google Mobile Ads / AdMob
 *  - ML Kit or proprietary Google SDKs
 *  - Firebase or non-free binary dependencies
 *  - Closed-source analytics, tracking, or ads
 * ============================================================================
 */

plugins {
    // AGP 9's built-in Kotlin support means we do NOT apply
    // org.jetbrains.kotlin.android here; Kotlin compilation is handled by AGP
    // itself. We still need the Compose compiler plugin (decoupled from AGP
    // since Kotlin 2.0), KSP (Room/Hilt annotation processing) and Hilt.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.fearmikey.garage"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.fearmikey.garage"
        minSdk = 34
        targetSdk = 37
        versionCode = 8
        versionName = "1.1.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
            isShrinkResources = true
        }
        // A release-like build type -- R8 optimization/shrinking enabled,
        // non-debuggable -- but signed with the debug keystore so it can be
        // installed straight from Studio/adb without a production signing
        // config. Use this (`installBenchmark`/`assembleBenchmark`) instead of
        // `debug` when evaluating real-world performance: `debuggable=true`
        // (the default on `debug`) disables several ART runtime
        // optimizations independent of R8, so a merely R8-minified debug
        // build still won't feel like what a real user experiences.
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.core.splashscreen)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Retrofit / networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.gson)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Image loading (internal file storage)
    implementation(libs.coil.compose)

    // DataStore
    implementation(libs.androidx.datastore)

    // DocumentFile
    implementation(libs.androidx.documentfile)

    implementation(libs.coil.network.okhttp)

    // Glance app widget
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // Cloud backup (WebDAV)
    implementation(libs.okhttp)
    implementation(libs.androidx.security.crypto)

    // EXIF orientation handling for vehicle photo downsampling
    implementation(libs.androidx.exifinterface)
}
