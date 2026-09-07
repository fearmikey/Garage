// Top-level build file where you can add configuration options common to all sub-projects/modules.
// Note: org.jetbrains.kotlin.android is intentionally NOT applied since AGP 9
// provides built-in Kotlin support (see app/build.gradle.kts for details).
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.oss.licenses.plugin) apply false
}
