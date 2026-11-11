package com.fearmikey.garage.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService

/**
 * Opens [intent] from a Quick Settings tile and collapses the shade.
 *
 * Android 14+ only accepts a [PendingIntent] (the Intent overload throws there); Android 13
 * only has the Intent overload.
 */
internal fun TileService.launchAndCollapse(intent: Intent) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        startActivityAndCollapse(pendingIntent)
    } else {
        startActivityAndCollapseLegacy(intent)
    }
}

/** Android 13 path; only reached below API 34, where the Intent overload is the only one. */
@SuppressLint("StartActivityAndCollapseDeprecated")
@Suppress("DEPRECATION")
private fun TileService.startActivityAndCollapseLegacy(intent: Intent) {
    startActivityAndCollapse(intent)
}
