package com.fearmikey.garage.config

import android.content.Context
import com.fearmikey.garage.util.UrlLauncher

/**
 * Configuration utilities for the Garage application.
 *
 * NOTE: Garage is strictly a 100% FOSS application. Do not add proprietary Google
 * Play Services integrations or non-free dependencies to this project.
 */
object FlavorConfig {
    const val isVinScannerSupported = true

    fun launchOssLicenses(context: Context) {
        // UrlLauncher shows a toast instead of crashing when no browser is installed.
        UrlLauncher.openUrl(context, "https://github.com/fearmikey/Garage/blob/master/LICENSE")
    }
}
