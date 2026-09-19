# Garage - Guidelines for AI Agents & Developers

## CRITICAL ARCHITECTURAL CONSTRAINTS

### 1. STRICT 100% FOSS POLICY (F-DROID COMPLIANCE)
* **Zero Proprietary SDKs:** Garage is strictly Free and Open Source Software (FOSS), targeted for distribution on F-Droid and other open-source Android repositories.
* **FORBIDDEN DEPENDENCIES:**
  * **NO** Google Play Services (`com.google.android.gms:*`)
  * **NO** Google Mobile Ads / AdMob (`com.google.android.gms:play-services-ads`)
  * **NO** Google Play Billing (`com.android.billingclient:*`)
  * **NO** ML Kit or closed-source vision/speech SDKs (`com.google.mlkit:*`)
  * **NO** Firebase SDKs that pull in non-free binaries or Play Services requirements
  * **NO** Analytics, tracking, or proprietary telemetry libraries
* **ALLOWED DEPENDENCIES:**
  * Android Jetpack libraries (`androidx.*`)
  * Kotlin standard/coroutines/serialization libraries
  * Open-source third-party dependencies with permissive/copyleft FOSS licenses (Apache 2.0, MIT, BSD, GPL, MPL)
* **NO PRODUCT FLAVORS FOR PROPRIETARY PLUGINS:** Do not add product flavor dimensions or build variants that rely on non-free or Google Play-specific dependencies.

### 2. PRIVACY & OFFLINE FIRST
* The application must remain fully functional offline.
* All user data (vehicle records, maintenance logs, fuel data, reminders) must stay local on device or sync via user-configured open protocols (e.g., WebDAV).

## RELEASE & PUBLISHING WORKFLOW (F-DROID)

### AUTOMATED F-DROID UPDATES
The app is published on F-Droid with Reproducible Builds enabled and auto-update tracking via Git tags. When helping the user create a new release, agents **MUST** follow this exact workflow to ensure F-Droid successfully picks up the update:

1. **Bump Versions:** Update `versionCode` and `versionName` in `app/build.gradle.kts`.
2. **Fastlane Changelog:** Create a new text file at `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` containing the release notes.
3. **Standard Changelog:** Update the main `CHANGELOG.md` and `README.md` files in the project root if necessary.
4. **Commit & Tag:** Instruct the user to commit these changes, create a git tag strictly following the `vX.Y.Z` format (e.g., `v1.1.9`), and push both the commit and the tag to GitHub.
5. **Reproducible Build Binary:** F-Droid requires the compiled APK to verify reproducible builds. Instruct the user to:
   * Build the signed release APK (`app-release.apk`) using their secure `Garage-Release.jks` keystore.
   * Go to GitHub Releases, draft a new release for the newly pushed `vX.Y.Z` tag.
   * Upload the `app-release.apk` file. (F-Droid is hardcoded to look for this exact filename at `https://github.com/fearmikey/Garage/releases/download/v%v/app-release.apk`).