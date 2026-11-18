package com.fearmikey.garage.ui.settings

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.fearmikey.garage.R
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.ui.theme.AppThemePalette
import com.fearmikey.garage.ui.util.label
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
                    Text(text = stringResource(R.string.settings_default_vehicle_first))
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
        "zh-CN" to stringResource(R.string.language_chinese_simplified),
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
                    // selectedLangCode is a bare language ("zh"); options may carry a region ("zh-CN").
                    val isSelected = code.substringBefore('-') == selectedLangCode
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

/** Localized name for a stored theme value ("light" / "dark" / "system"). */
@Composable
fun themeLabel(theme: String): String = when (theme) {
    "light" -> stringResource(R.string.onboarding_theme_light)
    "dark" -> stringResource(R.string.onboarding_theme_dark)
    else -> stringResource(R.string.onboarding_theme_system)
}

/** Localized name for a stored unit system value ("metric" / "imperial" / "uk_mixed"). */
@Composable
fun unitsLabel(units: String): String = when (units) {
    "imperial" -> stringResource(R.string.units_imperial)
    "uk_mixed" -> stringResource(R.string.units_uk_mixed)
    else -> stringResource(R.string.units_metric)
}

@Composable
fun SingleChoiceDialog(
    title: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    onDismissRequest: () -> Unit,
    optionLabel: @Composable (String) -> String = { it },
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
                        Text(text = optionLabel(option))
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
            title = { Text(stringResource(R.string.settings_insecure_title)) },
            text = {
                Text(stringResource(R.string.settings_insecure_webdav_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showHttpWarning = false
                        doSave()
                    }
                ) {
                    Text(stringResource(R.string.common_continue))
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
        title = { Text(stringResource(R.string.settings_webdav_config_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.common_server_url)) },
                    placeholder = { Text("https://cloud.example.com/remote.php/dav/files/user/") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.padding(top = 8.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.common_username)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.padding(top = 8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = {
                        Text(
                            stringResource(
                                if (uiState.webdavPasswordSet) R.string.settings_password_unchanged else R.string.common_password
                            )
                        )
                    },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.padding(top = 8.dp))
                TextButton(
                    onClick = { onTestConnection(url, username, password) },
                    enabled = url.isNotBlank() && username.isNotBlank(),
                ) {
                    Text(stringResource(R.string.settings_test_connection))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = ::handleSaveClick,
                enabled = url.isNotBlank() && username.isNotBlank(),
            ) {
                Text(stringResource(R.string.common_save))
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
        title = { Text(stringResource(R.string.settings_local_backup_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.settings_local_backup_description),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.settings_backup_folder_label),
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
                    Text(
                        stringResource(
                            if (uiState.localBackupFolderUri.isBlank()) R.string.settings_select_folder else R.string.settings_change_folder
                        )
                    )
                }
                val status = uiState.lastLocalBackupError?.let { stringResource(R.string.settings_status_error, it) }
                    ?: uiState.lastLocalBackupTimestamp?.let {
                        val relative = DateUtils.getRelativeTimeSpanString(
                            it,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS
                        )
                        stringResource(R.string.settings_last_saved, relative)
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
                Text(stringResource(R.string.common_close))
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
    val options = if (unitSystem == UnitSystem.IMPERIAL || unitSystem == UnitSystem.UK_MIXED) {
        listOf(
            250 to "250 mi",
            500 to "500 mi",
            1000 to "1,000 mi",
        )
    } else {
        listOf(
            UnitConverter.kmToMiles(250) to "250 km",
            UnitConverter.kmToMiles(500) to "500 km",
            UnitConverter.kmToMiles(800) to "800 km (~500 mi)",
            UnitConverter.kmToMiles(1000) to "1,000 km",
            UnitConverter.kmToMiles(1600) to "1,600 km (~1,000 mi)",
        )
    }

    val selectedOptionMiles = options.minByOrNull { abs(it.first - currentMilesWindow) }?.first ?: 500

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.settings_maint_mileage_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.settings_notify_due_within),
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
        title = { Text(stringResource(R.string.settings_maint_time_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.settings_notify_due_within),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(16.dp))
                val currentDaysInt = selectedDays.roundToInt()
                val daysLabel = pluralStringResource(R.plurals.common_days, currentDaysInt, currentDaysInt)
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
                Text(stringResource(R.string.common_save))
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
        title = { Text(stringResource(R.string.settings_doc_window_title)) },
        text = {
            Column {
                options.forEach { days ->
                    val label = pluralStringResource(R.plurals.settings_days_before, days, days)
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
        title = { Text(stringResource(R.string.settings_select_accent)) },
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
                            text = palette.label(),
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

/** "just now" for the last minute, otherwise the platform's localized relative time ("5 minutes ago"). */
@Composable
fun relativeTimeText(at: Long, now: Long = System.currentTimeMillis()): String =
    if (now - at < DateUtils.MINUTE_IN_MILLIS) {
        stringResource(R.string.common_just_now)
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
        !configured -> stringResource(R.string.sync_not_configured) to MaterialTheme.colorScheme.onSurfaceVariant
        !syncEnabled -> stringResource(R.string.sync_disabled_configured) to MaterialTheme.colorScheme.onSurfaceVariant
        uiState.isSyncing -> stringResource(R.string.sync_syncing) to MaterialTheme.colorScheme.primary
        uiState.lastSyncError != null -> stringResource(
            R.string.sync_failed_at,
            uiState.lastSyncTimestamp?.let { relativeTimeText(it, now) } ?: stringResource(R.string.sync_recently),
        ) to MaterialTheme.colorScheme.error
        uiState.lastSyncTimestamp != null ->
            stringResource(R.string.sync_connected_last, relativeTimeText(uiState.lastSyncTimestamp, now)) to successColor
        else -> stringResource(R.string.sync_connected_never) to successColor
    }

    ListItem(
        headlineContent = { Text(stringResource(R.string.webdav_title)) },
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
                !configured || !syncEnabled -> Icon(Icons.Filled.CloudOff, contentDescription = stringResource(R.string.webdav_disabled_cd), tint = iconColor)
                uiState.isSyncing -> {
                    val syncingCd = stringResource(R.string.sync_syncing)
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp).semantics { contentDescription = syncingCd },
                        strokeWidth = 2.dp,
                    )
                }
                uiState.lastSyncError != null -> Icon(Icons.Filled.Error, contentDescription = stringResource(R.string.sync_failed), tint = iconColor)
                else -> Icon(Icons.Filled.CheckCircle, contentDescription = stringResource(R.string.sync_connected), tint = iconColor)
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
        !configured -> stringResource(R.string.local_not_configured) to MaterialTheme.colorScheme.onSurfaceVariant
        !syncEnabled -> stringResource(R.string.local_disabled_folder, folderName) to MaterialTheme.colorScheme.onSurfaceVariant
        uiState.lastLocalBackupError != null -> stringResource(R.string.local_failed) to MaterialTheme.colorScheme.error
        uiState.lastLocalBackupTimestamp != null ->
            stringResource(R.string.local_active_last, relativeTimeText(uiState.lastLocalBackupTimestamp, now)) to successColor
        else -> stringResource(R.string.local_active_never) to successColor
    }
    val folderLine = stringResource(R.string.local_folder, folderName)

    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_local_backup_title)) },
        supportingContent = {
            Column {
                Text(statusText, color = if (configured && syncEnabled) iconColor else MaterialTheme.colorScheme.onSurfaceVariant)
                if (configured && syncEnabled && uiState.lastLocalBackupError != null) {
                    Text(folderLine, style = MaterialTheme.typography.bodySmall)
                    Text(uiState.lastLocalBackupError, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                } else if (configured && syncEnabled && uiState.lastLocalBackupTimestamp != null) {
                    Text(folderLine, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        leadingContent = {
            when {
                !configured || !syncEnabled -> Icon(Icons.Filled.FolderZip, contentDescription = stringResource(R.string.local_disabled_cd), tint = iconColor)
                uiState.lastLocalBackupError != null -> Icon(Icons.Filled.Error, contentDescription = stringResource(R.string.local_backup_failed_cd), tint = iconColor)
                else -> Icon(Icons.Filled.CheckCircle, contentDescription = stringResource(R.string.local_active), tint = iconColor)
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
}

fun formatFolderUriForDisplay(context: Context, uriString: String): String {
    if (uriString.isBlank()) return context.getString(R.string.folder_none)
    return try {
        val uri = uriString.toUri()
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
                context.getString(R.string.folder_selected)
            }
        }
    } catch (_: Exception) {
        context.getString(R.string.folder_selected)
    }
}
