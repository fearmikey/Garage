package com.fearmikey.garage.data.schedule

import com.fearmikey.garage.data.repository.ReminderStatus

/**
 * A [MaintenanceRule] evaluated against a specific vehicle's service history
 * and current mileage/date.
 *
 * [nextDueMileage] is only non-null when the rule specifies
 * [MaintenanceRule.intervalMiles], and likewise [nextDueDate] is only
 * non-null when the rule specifies [MaintenanceRule.intervalMonths] -- a
 * rule (most commonly a user-defined custom one) may set only one of the two.
 * The UI should show whichever of the two is present/more relevant -- see
 * [status], which already reflects "whichever comes first" between the two.
 */
data class MaintenanceSuggestion(
    val rule: MaintenanceRule,
    val lastServiceMileage: Int?,
    val lastServiceDate: Long?,
    val nextDueMileage: Int?,
    val nextDueDate: Long?,
    /**
     * Reuses [ReminderStatus] so the UI can share [com.fearmikey.garage.ui.components.StatusChip].
     * Can be [ReminderStatus.OVERDUE], [ReminderStatus.UPCOMING], or [ReminderStatus.OK] based on
     * vehicle mileage and time intervals evaluated against recorded service history.
     */
    val status: ReminderStatus,
)
