package com.fearmikey.garage.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import java.net.URI

object UrlLauncher {
    private const val AMAZON_SHOPPING_PACKAGE = "com.amazon.mShop.android.shopping"
    private const val AMAZON_TABLET_PACKAGE = "com.amazon.windowshop"

    /**
     * Checks if the given URL is an Amazon link.
     */
    fun isAmazonUrl(url: String): Boolean {
        val host = try {
            URI(url).host?.lowercase()
        } catch (_: Exception) {
            null
        } ?: try {
            Uri.parse(url).host?.lowercase()
        } catch (_: Exception) {
            null
        } ?: url.lowercase()

        return host.contains("amazon") || host.contains("amzn") || host.endsWith(".amazon")
    }

    /**
     * Opens a URL. If it is an Amazon link and the Amazon app is installed on the device,
     * it attempts to launch the link directly in the Amazon app.
     * Otherwise, it opens the link in the web browser or default URI handler.
     */
    fun openUrl(context: Context, url: String) {
        val uri = try {
            Uri.parse(url)
        } catch (_: Exception) {
            Toast.makeText(context, "Invalid URL", Toast.LENGTH_SHORT).show()
            return
        }

        if (isAmazonUrl(url)) {
            val amazonPackage = getInstalledAmazonPackage(context)
            if (amazonPackage != null) {
                try {
                    val amazonIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                        setPackage(amazonPackage)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(amazonIntent)
                    return
                } catch (_: ActivityNotFoundException) {
                    // Fallback to web browser if Amazon app fails to handle intent
                } catch (_: SecurityException) {
                    // Fallback to web browser if permission error
                }
            }
        }

        openInDefaultBrowser(context, uri)
    }

    private fun getInstalledAmazonPackage(context: Context): String? {
        val pm = context.packageManager
        for (pkg in listOf(AMAZON_SHOPPING_PACKAGE, AMAZON_TABLET_PACKAGE)) {
            try {
                pm.getPackageInfo(pkg, 0)
                return pkg
            } catch (_: PackageManager.NameNotFoundException) {
                // Package not installed
            }
        }
        return null
    }

    private fun openInDefaultBrowser(context: Context, uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
        }
    }
}
