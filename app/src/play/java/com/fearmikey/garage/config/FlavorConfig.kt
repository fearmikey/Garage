package com.fearmikey.garage.config

import android.content.Context
import android.content.Intent
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity

object FlavorConfig {
    const val isVinScannerSupported = true

    fun launchOssLicenses(context: Context) {
        val intent = Intent(context, OssLicensesMenuActivity::class.java)
        context.startActivity(intent)
    }
}
