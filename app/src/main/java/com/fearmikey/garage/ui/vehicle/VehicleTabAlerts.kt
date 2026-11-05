package com.fearmikey.garage.ui.vehicle

import com.fearmikey.garage.data.local.entity.CustomDocument
import com.fearmikey.garage.data.local.entity.VehicleRegistrationInsurance
import com.fearmikey.garage.data.repository.ReminderStatus
import com.fearmikey.garage.data.schedule.DocumentExpirationEngine
import com.fearmikey.garage.data.schedule.MaintenanceSuggestion

/**
 * Computes the attention indicator shown on each Vehicle Detail tab.
 * Only tabs needing attention appear in the result, mapped to their most urgent
 * status ([ReminderStatus.OVERDUE] beats [ReminderStatus.UPCOMING]).
 */
object VehicleTabAlerts {

    fun compute(
        suggestions: List<MaintenanceSuggestion>,
        registration: VehicleRegistrationInsurance?,
        documentDaysWindow: Int,
        now: Long = System.currentTimeMillis(),
        customDocuments: List<CustomDocument> = emptyList(),
    ): Map<VehicleTab, ReminderStatus> = buildMap {
        mostUrgent(suggestions.map { it.status })?.let { put(VehicleTab.SCHEDULE, it) }

        val docStatuses = DocumentExpirationEngine
            .checkAll(registration, customDocuments, upcomingWindowDays = documentDaysWindow, now = now)
            .map { it.status }
        mostUrgent(docStatuses)?.let { put(VehicleTab.DOCUMENTS, it) }
    }

    private fun mostUrgent(statuses: List<ReminderStatus>): ReminderStatus? = when {
        ReminderStatus.OVERDUE in statuses -> ReminderStatus.OVERDUE
        ReminderStatus.UPCOMING in statuses -> ReminderStatus.UPCOMING
        else -> null
    }
}
