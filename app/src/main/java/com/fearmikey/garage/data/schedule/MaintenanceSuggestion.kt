package com.fearmikey.garage.data.schedule

import com.fearmikey.garage.data.repository.ReminderStatus

/**
 * A [MaintenanceRule] evaluated against a specific vehicle's service history
 * and current mileage.
 */
data class MaintenanceSuggestion(
    val rule: MaintenanceRule,
    val lastServiceMileage: Int?,
    val nextDueMileage: Int,
    /**
     * Reuses [ReminderStatus] so the UI can share [com.fearmikey.garage.ui.components.StatusChip].
     * Only [ReminderStatus.UPCOMING] and [ReminderStatus.OK] are ever produced here: [nextDueMileage]
     * is always projected forward past the vehicle's current mileage, so there's no fixed
     * "overdue" target the way there is for a user-set [com.fearmikey.garage.data.local.entity.Reminder].
     */
    val status: ReminderStatus,
)
