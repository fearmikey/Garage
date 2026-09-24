package com.fearmikey.garage.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager

/**
 * Shared helper to force an immediate refresh of any placed Garage widget instances
 * ([GarageWidget], [LogFuelWidget], [QuickActionsWidget], [UpdateOdometerWidget]),
 * e.g. after data changes like fuel logs, odometer updates, or vehicle settings edits.
 */
object WidgetRefresher {
    private const val TAG = "WidgetRefresher"

    suspend fun refresh(context: Context) {
        // Best-effort: widget update failures (e.g. no widgets currently placed) should
        // never fail the caller's otherwise-successful operation.
        try {
            val manager = GlanceAppWidgetManager(context)

            val mainWidget = GarageWidget()
            manager.getGlanceIds(GarageWidget::class.java).forEach { id -> mainWidget.update(context, id) }

            val logFuelWidget = LogFuelWidget()
            manager.getGlanceIds(LogFuelWidget::class.java).forEach { id -> logFuelWidget.update(context, id) }

            val quickActionsWidget = QuickActionsWidget()
            manager.getGlanceIds(QuickActionsWidget::class.java).forEach { id -> quickActionsWidget.update(context, id) }

            val updateOdometerWidget = UpdateOdometerWidget()
            manager.getGlanceIds(UpdateOdometerWidget::class.java).forEach { id -> updateOdometerWidget.update(context, id) }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to refresh Garage widgets", e)
        }
    }
}
