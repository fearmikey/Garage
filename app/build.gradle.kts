plugins {
    // AGP 9's built-in Kotlin support means we do NOT apply
    // org.jetbrains.kotlin.android here; Kotlin compilation is handled by AGP
    // itself. We still need the Compose compiler plugin (decoupled from AGP
    // since Kotlin 2.0), KSP (Room/Hilt annotation processing) and Hilt.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.oss.licenses.plugin)
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
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
            isShrinkResources = true
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

    // OSS Licenses
    //
    // play-services-oss-licenses transitively pulls in an alpha build of
    // androidx.compose.material3:material3 (for its newer, unused "v2" Compose
    // licenses UI). That alpha version is binary-incompatible with the stable
    // androidx.compose.foundation version pinned by our Compose BOM (it was
    // compiled against an older, pre-stabilization shape of Foundation's
    // Styles API), which crashes ANY OutlinedTextField in the app with an
    // AbstractMethodError. We only use the legacy, plain-View
    // OssLicensesMenuActivity (which never touches Compose), so it's safe to
    // drop this transitive dependency and let our Compose BOM's stable
    // material3 version win instead.
    implementation(libs.oss.licenses) {
        exclude(group = "androidx.compose.material3", module = "material3")
    }
    implementation(libs.coil.network.okhttp)

    // CameraX (VIN scanning)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // ML Kit (VIN barcode/text scanning)
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.mlkit.text.recognition)

    // Glance app widget
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // Cloud backup (WebDAV)
    implementation(libs.okhttp)
    implementation(libs.androidx.security.crypto)
}
