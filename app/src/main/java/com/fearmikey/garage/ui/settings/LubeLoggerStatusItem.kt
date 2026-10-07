package com.fearmikey.garage.ui.settings

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fearmikey.garage.data.remote.lubelogger.LubeLoggerSyncStatus
import com.fearmikey.garage.ui.theme.GarageTheme

/**
 * The "LubeLogger Sync" row in Settings. Shows whether Garage could actually connect at the last
 * sync, not just whether a server URL is saved.
 */
@Composable
fun LubeLoggerStatusItem(
    configured: Boolean,
    serverUrl: String,
    status: LubeLoggerSyncStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    now: Long = System.currentTimeMillis(),
) {
    val successColor = Color(0xFF2E7D32)
    val (statusText, iconColor) = when {
        !configured -> "Not configured" to MaterialTheme.colorScheme.onSurfaceVariant
        status is LubeLoggerSyncStatus.Syncing -> "Syncing\u2026" to MaterialTheme.colorScheme.primary
        status is LubeLoggerSyncStatus.Success -> "Connected \u00b7 last synced ${relative(status.at, now)}" to successColor
        status is LubeLoggerSyncStatus.Failed -> "Connection failed \u00b7 ${relative(status.at, now)}" to MaterialTheme.colorScheme.error
        else -> "Not connected yet \u00b7 tap Sync now to test" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    ListItem(
        headlineContent = { Text("LubeLogger Sync") },
        supportingContent = {
            Column {
                Text(statusText, color = if (configured) iconColor else MaterialTheme.colorScheme.onSurfaceVariant)
                if (configured && serverUrl.isNotBlank()) {
                    Text(serverUrl, style = MaterialTheme.typography.bodySmall)
                }
                if (configured && status is LubeLoggerSyncStatus.Failed) {
                    Text(status.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    status.lastSuccessAt?.let {
                        Text("Last successful sync ${relative(it, now)}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        leadingContent = {
            when {
                !configured -> Icon(Icons.Filled.CloudOff, contentDescription = "LubeLogger not configured", tint = iconColor)
                status is LubeLoggerSyncStatus.Syncing ->
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp).semantics { contentDescription = "Syncing" },
                        strokeWidth = 2.dp,
                    )
                status is LubeLoggerSyncStatus.Success -> Icon(Icons.Filled.CheckCircle, contentDescription = "Connected", tint = iconColor)
                status is LubeLoggerSyncStatus.Failed -> Icon(Icons.Filled.Error, contentDescription = "Connection failed", tint = iconColor)
                else -> Icon(Icons.Filled.CloudSync, contentDescription = null, tint = iconColor)
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
}

private fun relative(at: Long, now: Long): String =
    if (now - at < DateUtils.MINUTE_IN_MILLIS) {
        "just now"
    } else {
        DateUtils.getRelativeTimeSpanString(at, now, DateUtils.MINUTE_IN_MILLIS).toString().lowercase()
    }

@Preview(showBackground = true)
@Composable
private fun LubeLoggerStatusItemPreview() {
    val now = 1_750_000_000_000L
    GarageTheme {
        Column {
            LubeLoggerStatusItem(false, "", LubeLoggerSyncStatus.NeverSynced, {}, now = now)
            LubeLoggerStatusItem(true, "https://ll.example.com", LubeLoggerSyncStatus.NeverSynced, {}, now = now)
            LubeLoggerStatusItem(true, "https://ll.example.com", LubeLoggerSyncStatus.Syncing(null), {}, now = now)
            LubeLoggerStatusItem(true, "https://ll.example.com", LubeLoggerSyncStatus.Success(now - 5 * 60_000), {}, now = now)
            LubeLoggerStatusItem(
                true,
                "https://ll.example.com",
                LubeLoggerSyncStatus.Failed(now - 60_000, "LubeLogger rejected the username/password or API key (HTTP 401).", now - 86_400_000),
                {},
                now = now,
            )
        }
    }
}
