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
