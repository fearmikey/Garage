package com.fearmikey.garage.config

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

/**
 * Configuration utilities for the Garage application.
 *
 * NOTE: Garage is strictly a 100% FOSS application. Do not add proprietary Google
 * Play Services integrations or non-free dependencies to this project.
 */
object FlavorConfig {
    const val isVinScannerSupported = true

    fun launchOssLicenses(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, "https://github.com/fearmikey/Garage/blob/master/LICENSE".toUri())
        context.startActivity(intent)
    }
}
