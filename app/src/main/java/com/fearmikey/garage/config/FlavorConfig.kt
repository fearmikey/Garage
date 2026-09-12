package com.fearmikey.garage.config

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Configuration utilities for the Garage application.
 *
 * NOTE: Garage is strictly a 100% FOSS application. Do not add proprietary Google
 * Play Services integrations or non-free dependencies to this project.
 */
object FlavorConfig {
    const val isVinScannerSupported = false

    fun launchOssLicenses(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/fearmikey/Garage/blob/main/LICENSE"))
        context.startActivity(intent)
    }
}
