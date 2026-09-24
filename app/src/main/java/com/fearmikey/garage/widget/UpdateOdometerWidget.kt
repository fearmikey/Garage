package com.fearmikey.garage.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.fearmikey.garage.MainActivity
import com.fearmikey.garage.ui.util.UnitConverter
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

class UpdateOdometerWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication<GarageWidgetEntryPoint>(context)
        val vehicleRepository = entryPoint.vehicleRepository()
        val maintenanceRepository = entryPoint.maintenanceRepository()
        val preferencesRepository = entryPoint.preferencesRepository()

        val vehicles = vehicleRepository.getAllVehicles().first()
        val unitSystem = preferencesRepository.unitSystem.first()
        val defaultVehicleId = preferencesRepository.defaultVehicleId.first()
        val targetVehicle = vehicles.find { it.id == defaultVehicleId } ?: vehicles.firstOrNull()

        val vehicleLabel = targetVehicle?.let {
            listOfNotNull(it.year?.toString(), it.make, it.model).joinToString(" ").ifBlank { it.vin }
        } ?: "Garage"

        val currentMileageText = if (targetVehicle != null) {
            val mileage = maintenanceRepository.getLatestMileageForVehicle(targetVehicle.id).first()
            mileage?.let { UnitConverter.formatDistance(it, unitSystem) } ?: "No mileage"
        } else {
            "No mileage"
        }

        provideContent {
            GlanceTheme {
                UpdateOdometerWidgetContent(
                    context = context,
                    vehicleLabel = vehicleLabel,
                    currentMileageText = currentMileageText,
                    targetVehicleId = targetVehicle?.id,
                )
            }
        }
    }
}

@Composable
private fun UpdateOdometerWidgetContent(
    context: Context,
    vehicleLabel: String,
    currentMileageText: String,
    targetVehicleId: Long?,
) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .appWidgetBackground()
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = vehicleLabel,
                style = TextStyle(fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onBackground),
            )
            Text(
                text = currentMileageText,
                style = TextStyle(fontSize = 11.sp, color = GlanceTheme.colors.onSurfaceVariant),
            )
        }
        Spacer(modifier = GlanceModifier.width(8.dp))
        Button(
            text = "Update",
            onClick = actionStartActivity(
                Intent(context, MainActivity::class.java).apply {
                    action = MainActivity.ACTION_UPDATE_ODOMETER
                    targetVehicleId?.let { putExtra(MainActivity.EXTRA_VEHICLE_ID, it) }
                },
            ),
        )
    }
}
