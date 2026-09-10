package com.fearmikey.garage.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager

/**
 * Small shared helper to force an immediate refresh of any placed [GarageWidget]
 * instances, e.g. after data changes that the widget's own refresh cadence
 * wouldn't otherwise pick up right away (a reminder check, or the user changing
 * their default vehicle in Settings).
 */
object WidgetRefresher {
    private const val TAG = "WidgetRefresher"

    suspend fun refresh(context: Context) {
        // Best-effort: a widget update failure (e.g. no widgets currently placed) should
        // never fail the caller's otherwise-successful operation.
        try {
            val manager = GlanceAppWidgetManager(context)
            val widget = GarageWidget()
            manager.getGlanceIds(GarageWidget::class.java).forEach { id -> widget.update(context, id) }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to refresh Garage widget", e)
        }
    }
}
