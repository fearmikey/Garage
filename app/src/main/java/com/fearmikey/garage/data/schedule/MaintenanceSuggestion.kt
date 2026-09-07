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
     * Only [ReminderStatus.UPCOMING] and [ReminderStatus.OK] are ever produced here: both
     * [nextDueMileage] and [nextDueDate] are always projected forward past the vehicle's current
     * mileage/today's date, so there's no fixed "overdue" target the way there is for a
     * user-set [com.fearmikey.garage.data.local.entity.Reminder].
     */
    val status: ReminderStatus,
)
