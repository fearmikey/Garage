package com.fearmikey.garage.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.fearmikey.garage.MainActivity
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.data.repository.MaintenanceRepository
import com.fearmikey.garage.data.repository.PreferencesRepository
import com.fearmikey.garage.data.repository.ReminderRepository
import com.fearmikey.garage.data.repository.ReminderStatus
import com.fearmikey.garage.data.repository.VehicleRepository
import com.fearmikey.garage.ui.util.UnitConverter
import com.fearmikey.garage.ui.util.toDisplayDate
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

/**
 * [GlanceAppWidget] can't be constructed by Hilt directly (it's instantiated by
 * [GarageWidgetReceiver], not by the DI graph), so repositories are pulled out
 * of the Application's Hilt container via this entry point instead.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface GarageWidgetEntryPoint {
    fun vehicleRepository(): VehicleRepository
    fun maintenanceRepository(): MaintenanceRepository
    fun reminderRepository(): ReminderRepository
    fun preferencesRepository(): PreferencesRepository
}

private data class WidgetReminderRow(
    val vehicleLabel: String,
    val taskName: String,
    val status: ReminderStatus,
    val dueInfo: String,
)

private const val MAX_ROWS = 5

class GarageWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication<GarageWidgetEntryPoint>(context)
        val vehicleRepository = entryPoint.vehicleRepository()
        val maintenanceRepository = entryPoint.maintenanceRepository()
        val reminderRepository = entryPoint.reminderRepository()
        val preferencesRepository = entryPoint.preferencesRepository()

        val vehicles = vehicleRepository.getAllVehicles().first()
        val incompleteReminders = reminderRepository.getIncompleteReminders()
        val unitSystem = preferencesRepository.unitSystem.first()

        val rows = incompleteReminders
            .mapNotNull { reminder ->
                val vehicle = vehicles.find { it.id == reminder.vehicleId } ?: return@mapNotNull null
                val latestMileage = maintenanceRepository.getLatestMileageForVehicle(reminder.vehicleId).first()
                val status = ReminderRepository.computeStatus(reminder, latestMileage)
                if (status != ReminderStatus.OVERDUE && status != ReminderStatus.UPCOMING) return@mapNotNull null

                val dueInfo = listOfNotNull(
                    reminder.dueDate?.toDisplayDate(),
                    reminder.dueMileage?.let { UnitConverter.formatDistance(it, unitSystem) },
                ).joinToString(" · ")

                WidgetReminderRow(
                    vehicleLabel = vehicle.widgetLabel(),
                    taskName = reminder.taskName,
                    status = status,
                    dueInfo = dueInfo,
                )
            }
            .sortedBy { if (it.status == ReminderStatus.OVERDUE) 0 else 1 }
            .take(MAX_ROWS)

        val defaultVehicleId = preferencesRepository.defaultVehicleId.first()
        val targetVehicle = vehicles.find { it.id == defaultVehicleId } ?: vehicles.firstOrNull()
        val targetVehicleId = targetVehicle?.id

        provideContent {
            GlanceTheme {
                GarageWidgetContent(context = context, rows = rows, targetVehicleId = targetVehicleId)
            }
        }
    }
}

private fun Vehicle.widgetLabel(): String =
    listOfNotNull(year?.toString(), make, model).joinToString(" ").ifBlank { vin }

@Composable
private fun GarageWidgetContent(
    context: Context,
    rows: List<WidgetReminderRow>,
    targetVehicleId: Long?,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .appWidgetBackground()
            .padding(12.dp),
    ) {
        Text(
            text = "Garage",
            style = TextStyle(fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onBackground),
        )
        Spacer(modifier = GlanceModifier.height(8.dp))

        if (rows.isEmpty()) {
            Text(
                text = "All caught up!",
                style = TextStyle(color = GlanceTheme.colors.onBackground),
            )
        } else {
            rows.forEach { row ->
                WidgetReminderRowContent(row)
                Spacer(modifier = GlanceModifier.height(6.dp))
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Button(
                text = "Log Service",
                onClick = actionStartActivity(
                    logServiceIntent(context, MainActivity.ACTION_LOG_SERVICE, targetVehicleId),
                ),
                modifier = GlanceModifier.defaultWeight(),
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Button(
                text = "Log Fuel",
                onClick = actionStartActivity(
                    logServiceIntent(context, MainActivity.ACTION_LOG_FUEL, targetVehicleId),
                ),
                modifier = GlanceModifier.defaultWeight(),
            )
        }
    }
}

@Composable
private fun WidgetReminderRowContent(row: WidgetReminderRow) {
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Text(
            text = "${row.vehicleLabel} · ${row.taskName}",
            style = TextStyle(fontWeight = FontWeight.Medium, color = GlanceTheme.colors.onBackground),
        )
        val statusPrefix = if (row.status == ReminderStatus.OVERDUE) "Overdue" else "Due"
        Text(
            text = "$statusPrefix · ${row.dueInfo}",
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
        )
    }
}

private fun logServiceIntent(context: Context, action: String, vehicleId: Long?): Intent =
    Intent(context, MainActivity::class.java).apply {
        setAction(action)
        vehicleId?.let { putExtra(MainActivity.EXTRA_VEHICLE_ID, it) }
    }
