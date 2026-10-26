package com.fearmikey.garage.ui.settings

import android.content.Context
import android.net.Uri
import android.text.format.DateUtils
import androidx.documentfile.provider.DocumentFile
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.fearmikey.garage.R
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.ui.theme.AppThemePalette
import com.fearmikey.garage.ui.util.UnitConverter
import com.fearmikey.garage.ui.util.UnitSystem
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun SettingsCategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun DefaultVehicleDialog(
    vehicles: List<Vehicle>,
    selectedVehicleId: Long?,
    onVehicleSelected: (Long?) -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.settings_default_vehicle)) },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onVehicleSelected(null) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selectedVehicleId == null,
                        onClick = { onVehicleSelected(null) },
                    )
                    Spacer(modifier = Modifier.padding(start = 8.dp))
                    Text(text = "First vehicle added (default)")
                }

                vehicles.forEach { vehicle ->
                    val vehicleName = listOfNotNull(vehicle.year?.toString(), vehicle.make, vehicle.model)
                        .joinToString(" ")
                        .ifBlank { vehicle.vin }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onVehicleSelected(vehicle.id) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = vehicle.id == selectedVehicleId,
                            onClick = { onVehicleSelected(vehicle.id) },
                        )
                        Spacer(modifier = Modifier.padding(start = 8.dp))
                        Text(text = vehicleName)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.obd_cancel))
            }
        },
    )
}

@Composable
fun LanguageSelectionDialog(
    selectedLangCode: String,
    onLanguageSelected: (String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val languageOptions = listOf(
        "" to stringResource(R.string.language_system_default),
        "en" to stringResource(R.string.language_english),
        "fr" to stringResource(R.string.language_french),
        "de" to stringResource(R.string.language_german),
        "pl" to stringResource(R.string.language_polish),
        "es" to stringResource(R.string.language_spanish),
        "it" to stringResource(R.string.language_italian),
        "pt" to stringResource(R.string.language_portuguese),
        "nl" to stringResource(R.string.language_dutch),
        "ru" to stringResource(R.string.language_russian),
        "tr" to stringResource(R.string.language_turkish),
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.language_selection)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                languageOptions.forEach { (code, name) ->
                    val isSelected = code == selectedLangCode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onLanguageSelected(code)
                                onDismissRequest()
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                onLanguageSelected(code)
                                onDismissRequest()
                            },
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.obd_cancel))
            }
        },
    )
}

@Composable
fun SingleChoiceDialog(
    title: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOptionSelected(option)
                                onDismissRequest()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = option == selectedOption,
                            onClick = {
                                onOptionSelected(option)
                                onDismissRequest()
                            }
                        )
                        Spacer(modifier = Modifier.padding(start = 8.dp))
                        Text(text = option.replaceFirstChar { it.uppercase() })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.obd_cancel))
            }
        }
    )
}

@Composable
fun CloudBackupConfigDialog(
    uiState: SettingsUiState,
    onSave: (url: String, username: String, password: String) -> Unit,
    onTestConnection: (url: String, username: String, password: String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var url by remember { mutableStateOf(uiState.webdavUrl) }
    var username by remember { mutableStateOf(uiState.webdavUsername) }
    var password by remember { mutableStateOf("") }
    var showHttpWarning by remember { mutableStateOf(false) }

    fun doSave() {
        onSave(url, username, password)
    }

    fun handleSaveClick() {
        val isHttp = url.isNotBlank() && !url.trim().startsWith("https://", ignoreCase = true)
        if (isHttp) {
            showHttpWarning = true
        } else {
            doSave()
        }
    }

    if (showHttpWarning) {
        AlertDialog(
            onDismissRequest = { showHttpWarning = false },
            title = { Text("Insecure Connection Warning") },
            text = {
                Text("You are connecting over an unencrypted HTTP connection. To keep your cloud backup credentials and data secure, ensure you are using a secure private VPN connection (such as Tailscale or WireGuard) to your server.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showHttpWarning = false
                        doSave()
                    }
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHttpWarning = false }) {
                    Text(stringResource(R.string.obd_cancel))
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Auto-Backup Configuration") },
        text = {
            Column {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Server URL") },
                    placeholder = { Text("https://cloud.example.com/remote.php/dav/files/user/") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.padding(top = 8.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.padding(top = 8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(if (uiState.webdavPasswordSet) "Password (unchanged)" else "Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.padding(top = 8.dp))
                TextButton(
                    onClick = { onTestConnection(url, username, password) },
                    enabled = url.isNotBlank() && username.isNotBlank(),
                ) {
                    Text("Test connection")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = ::handleSaveClick,
                enabled = url.isNotBlank() && username.isNotBlank(),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.obd_cancel))
            }
        },
    )
}

@Composable
fun LocalBackupConfigDialog(
    uiState: SettingsUiState,
    onSelectFolder: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Local Folder Auto-Backup") },
        text = {
            Column {
                Text(
                    text = "Automatically saves a backup zip (garage-backup.zip) to a folder on your phone whenever changes are made in Garage. You can sync this folder with custom or 3rd-party cloud storage apps.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Backup Folder:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatFolderUriForDisplay(context, uiState.localBackupFolderUri),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onSelectFolder,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (uiState.localBackupFolderUri.isBlank()) "Select Folder" else "Change Folder")
                }
                val status = uiState.lastLocalBackupError?.let { "Status: $it" }
                    ?: uiState.lastLocalBackupTimestamp?.let {
                        val relative = DateUtils.getRelativeTimeSpanString(
                            it,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS
                        )
                        "Last saved: $relative"
                    }
                if (status != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (uiState.lastLocalBackupError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Close")
            }
        },
    )
}

@Composable
fun MaintenanceMileageDialog(
    currentMilesWindow: Int,
    unitSystem: UnitSystem,
    onOptionSelected: (miles: Int) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val options = if (unitSystem == UnitSystem.IMPERIAL) {
        listOf(
            250 to "250 mi",
            500 to "500 mi",
            1000 to "1,000 mi",
        )
    } else {
        listOf(
            UnitConverter.kmToMiles(250) to "250 km",
            UnitConverter.kmToMiles(500) to "500 km",
            UnitConverter.kmToMiles(800) to "800 km (~500 mi equivalent)",
            UnitConverter.kmToMiles(1000) to "1,000 km",
            UnitConverter.kmToMiles(1600) to "1,600 km (~1,000 mi equivalent)",
        )
    }

    val selectedOptionMiles = options.minByOrNull { abs(it.first - currentMilesWindow) }?.first ?: 500

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Maintenance Mileage Notification") },
        text = {
            Column {
                Text(
                    text = "Notify when maintenance is due within:",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                options.forEach { (miles, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOptionSelected(miles)
                                onDismissRequest()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = miles == selectedOptionMiles,
                            onClick = {
                                onOptionSelected(miles)
                                onDismissRequest()
                            },
                        )
                        Spacer(modifier = Modifier.padding(start = 8.dp))
                        Text(text = label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.obd_cancel))
            }
        },
    )
}

@Composable
fun MaintenanceDaysDialog(
    currentDaysWindow: Int,
    onOptionSelected: (days: Int) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var selectedDays by remember { mutableFloatStateOf(currentDaysWindow.coerceIn(1, 30).toFloat()) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Maintenance Time Notification") },
        text = {
            Column {
                Text(
                    text = "Notify when maintenance is due within:",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(16.dp))
                val currentDaysInt = selectedDays.roundToInt()
                val daysLabel = if (currentDaysInt == 1) "1 day" else "$currentDaysInt days"
                Text(
                    text = daysLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = selectedDays,
                    onValueChange = { selectedDays = it },
                    valueRange = 1f..30f,
                    steps = 28,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onOptionSelected(selectedDays.roundToInt())
                    onDismissRequest()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.obd_cancel))
            }
        },
    )
}

@Composable
fun DocumentDaysDialog(
    currentDaysWindow: Int,
    onOptionSelected: (days: Int) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val options = listOf(7, 14, 30, 60, 90)
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Document Expiration Notice Window") },
        text = {
            Column {
                options.forEach { days ->
                    val label = if (days == 1) "1 day before" else "$days days before"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOptionSelected(days)
                                onDismissRequest()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = (days == currentDaysWindow),
                            onClick = {
                                onOptionSelected(days)
                                onDismissRequest()
                            },
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.obd_cancel))
            }
        },
    )
}

@Composable
fun PaletteSelectionDialog(
    selectedPaletteId: String,
    onPaletteSelected: (String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Select Accent Color") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AppThemePalette.entries.forEach { palette ->
                    val isSelected = palette.id == selectedPaletteId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onPaletteSelected(palette.id)
                                onDismissRequest()
                            }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                onPaletteSelected(palette.id)
                                onDismissRequest()
                            },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(palette.primaryColor, CircleShape),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = palette.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.obd_cancel))
            }
        },
    )
}

fun cloudBackupStatusText(uiState: SettingsUiState): String {
    val configured = uiState.webdavUrl.isNotBlank() && uiState.webdavUsername.isNotBlank()
    if (!configured) return "Not configured"
    if (!uiState.cloudSyncEnabled) return "Disabled (configured)"
    val error = uiState.lastSyncError
    if (error != null) return "Connected — sync failed: $error"
    val lastSync = uiState.lastSyncTimestamp
    return if (lastSync != null) {
        val relative = DateUtils.getRelativeTimeSpanString(lastSync, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
        "Connected — last synced $relative"
    } else {
        "Connected — not yet synced"
    }
}

fun localBackupStatusText(uiState: SettingsUiState, context: Context): String {
    if (uiState.localBackupFolderUri.isBlank()) return "Not configured — no folder selected"
    val folderName = formatFolderUriForDisplay(context, uiState.localBackupFolderUri)
    if (!uiState.localBackupEnabled) return "Disabled — Folder: $folderName"
    val error = uiState.lastLocalBackupError
    if (error != null) return "Folder: $folderName — Failed: $error"
    val lastBackup = uiState.lastLocalBackupTimestamp
    return if (lastBackup != null) {
        val relative = DateUtils.getRelativeTimeSpanString(
            lastBackup,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        )
        "Folder: $folderName — Last saved $relative"
    } else {
        "Folder: $folderName — Not yet backed up"
    }
}

fun relativeTimeText(at: Long, now: Long = System.currentTimeMillis()): String =
    if (now - at < DateUtils.MINUTE_IN_MILLIS) {
        "just now"
    } else {
        DateUtils.getRelativeTimeSpanString(at, now, DateUtils.MINUTE_IN_MILLIS).toString().lowercase()
    }

@Composable
fun WebDavStatusItem(
    uiState: SettingsUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    now: Long = System.currentTimeMillis(),
) {
    val configured = uiState.webdavUrl.isNotBlank() && uiState.webdavUsername.isNotBlank()
    val syncEnabled = uiState.cloudSyncEnabled
    val successColor = Color(0xFF2E7D32)

    val (statusText, iconColor) = when {
        !configured -> "Not configured" to MaterialTheme.colorScheme.onSurfaceVariant
        !syncEnabled -> "Disabled (configured)" to MaterialTheme.colorScheme.onSurfaceVariant
        uiState.isSyncing -> "Syncing\u2026" to MaterialTheme.colorScheme.primary
        uiState.lastSyncError != null -> "Sync failed \u00b7 ${uiState.lastSyncTimestamp?.let { relativeTimeText(it, now) } ?: "recently"}" to MaterialTheme.colorScheme.error
        uiState.lastSyncTimestamp != null -> "Connected \u00b7 last synced ${relativeTimeText(uiState.lastSyncTimestamp, now)}" to successColor
        else -> "Connected \u00b7 not yet synced" to successColor
    }

    ListItem(
        headlineContent = { Text("WebDAV Cloud Sync") },
        supportingContent = {
            Column {
                Text(statusText, color = if (configured && syncEnabled) iconColor else MaterialTheme.colorScheme.onSurfaceVariant)
                if (configured && uiState.webdavUrl.isNotBlank()) {
                    Text(uiState.webdavUrl, style = MaterialTheme.typography.bodySmall)
                }
                if (configured && syncEnabled && uiState.lastSyncError != null) {
                    Text(uiState.lastSyncError, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        leadingContent = {
            when {
                !configured || !syncEnabled -> Icon(Icons.Filled.CloudOff, contentDescription = "WebDAV disabled", tint = iconColor)
                uiState.isSyncing -> CircularProgressIndicator(
                    modifier = Modifier.size(24.dp).semantics { contentDescription = "Syncing" },
                    strokeWidth = 2.dp,
                )
                uiState.lastSyncError != null -> Icon(Icons.Filled.Error, contentDescription = "Sync failed", tint = iconColor)
                else -> Icon(Icons.Filled.CheckCircle, contentDescription = "Connected", tint = iconColor)
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
}

@Composable
fun LocalBackupStatusItem(
    uiState: SettingsUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    now: Long = System.currentTimeMillis(),
) {
    val context = LocalContext.current
    val configured = uiState.localBackupFolderUri.isNotBlank()
    val syncEnabled = uiState.localBackupEnabled
    val successColor = Color(0xFF2E7D32)
    val folderName = formatFolderUriForDisplay(context, uiState.localBackupFolderUri)

    val (statusText, iconColor) = when {
        !configured -> "Not configured — no folder selected" to MaterialTheme.colorScheme.onSurfaceVariant
        !syncEnabled -> "Disabled — Folder: $folderName" to MaterialTheme.colorScheme.onSurfaceVariant
        uiState.lastLocalBackupError != null -> "Auto-backup failed" to MaterialTheme.colorScheme.error
        uiState.lastLocalBackupTimestamp != null -> "Active \u00b7 last saved ${relativeTimeText(uiState.lastLocalBackupTimestamp, now)}" to successColor
        else -> "Active \u00b7 not yet saved" to successColor
    }

    ListItem(
        headlineContent = { Text("Local Folder Auto-Backup") },
        supportingContent = {
            Column {
                Text(statusText, color = if (configured && syncEnabled) iconColor else MaterialTheme.colorScheme.onSurfaceVariant)
                if (configured && syncEnabled && uiState.lastLocalBackupError != null) {
                    Text("Folder: $folderName", style = MaterialTheme.typography.bodySmall)
                    Text(uiState.lastLocalBackupError, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                } else if (configured && syncEnabled && uiState.lastLocalBackupTimestamp != null) {
                    Text("Folder: $folderName", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        leadingContent = {
            when {
                !configured || !syncEnabled -> Icon(Icons.Filled.FolderZip, contentDescription = "Local backup disabled", tint = iconColor)
                uiState.lastLocalBackupError != null -> Icon(Icons.Filled.Error, contentDescription = "Backup failed", tint = iconColor)
                else -> Icon(Icons.Filled.CheckCircle, contentDescription = "Active", tint = iconColor)
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
}

fun formatFolderUriForDisplay(context: Context, uriString: String): String {
    if (uriString.isBlank()) return "No folder selected"
    return try {
        val uri = Uri.parse(uriString)
        val docFile = DocumentFile.fromTreeUri(context, uri)
        val name = docFile?.name
        if (!name.isNullOrBlank()) {
            name
        } else {
            val decoded = Uri.decode(uriString)
            val treePart = decoded.substringAfter("/tree/").substringAfter(":")
            if (treePart.isNotBlank() && treePart != uriString) {
                treePart
            } else {
                "Selected Folder"
            }
        }
    } catch (_: Exception) {
        "Selected Folder"
    }
}
