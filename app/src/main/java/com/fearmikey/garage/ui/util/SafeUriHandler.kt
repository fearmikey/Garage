package com.fearmikey.garage.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.UriHandler
import com.fearmikey.garage.util.UrlLauncher

/**
 * A [UriHandler] that never crashes: Compose's default `LocalUriHandler` throws
 * `ActivityNotFoundException` when no app can open the link (e.g. a de-Googled device without a
 * browser). This one goes through [UrlLauncher], which shows a toast instead.
 */
@Composable
fun rememberSafeUriHandler(): UriHandler {
    val context = LocalContext.current
    return remember(context) {
        object : UriHandler {
            override fun openUri(uri: String) = UrlLauncher.openUrl(context, uri)
        }
    }
}
