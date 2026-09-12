package com.fearmikey.garage.config

import android.content.Context
import android.content.Intent
import android.net.Uri

object FlavorConfig {
    const val isVinScannerSupported = false

    fun launchOssLicenses(context: Context) {
        // Fallback for FOSS variant since Play Services OSS plugin is removed
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/fearmikey/Garage/blob/main/LICENSE"))
        context.startActivity(intent)
    }
}
