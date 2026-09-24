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
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import com.fearmikey.garage.MainActivity
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

class QuickActionsWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication<GarageWidgetEntryPoint>(context)
        val vehicleRepository = entryPoint.vehicleRepository()
        val preferencesRepository = entryPoint.preferencesRepository()

        val vehicles = vehicleRepository.getAllVehicles().first()
        val defaultVehicleId = preferencesRepository.defaultVehicleId.first()
        val targetVehicleId = (vehicles.find { it.id == defaultVehicleId } ?: vehicles.firstOrNull())?.id

        provideContent {
            GlanceTheme {
                QuickActionsWidgetContent(
                    context = context,
                    targetVehicleId = targetVehicleId,
                )
            }
        }
    }
}

@Composable
private fun QuickActionsWidgetContent(
    context: Context,
    targetVehicleId: Long?,
) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .appWidgetBackground()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            text = "Log Fuel",
            onClick = actionStartActivity(
                actionIntent(context, MainActivity.ACTION_LOG_FUEL, targetVehicleId),
            ),
            modifier = GlanceModifier.defaultWeight(),
        )
        Spacer(modifier = GlanceModifier.width(4.dp))
        Button(
            text = "Odometer",
            onClick = actionStartActivity(
                actionIntent(context, MainActivity.ACTION_UPDATE_ODOMETER, targetVehicleId),
            ),
            modifier = GlanceModifier.defaultWeight(),
        )
        Spacer(modifier = GlanceModifier.width(4.dp))
        Button(
            text = "Log Service",
            onClick = actionStartActivity(
                actionIntent(context, MainActivity.ACTION_LOG_SERVICE, targetVehicleId),
            ),
            modifier = GlanceModifier.defaultWeight(),
        )
    }
}

private fun actionIntent(context: Context, action: String, vehicleId: Long?): Intent =
    Intent(context, MainActivity::class.java).apply {
        setAction(action)
        vehicleId?.let { putExtra(MainActivity.EXTRA_VEHICLE_ID, it) }
    }
