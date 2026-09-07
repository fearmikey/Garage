package com.fearmikey.garage.data.schedule

import com.fearmikey.garage.data.local.entity.MaintenanceCategory
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.data.repository.ReminderRepository
import com.fearmikey.garage.data.repository.ReminderStatus

/**
 * Turns [MaintenanceScheduleRules] into concrete, mileage-based suggestions
 * for a specific vehicle, given its service history and latest known mileage.
 */
object MaintenanceScheduleEngine {

    fun suggestionsFor(
        vehicle: Vehicle,
        latestMileage: Int?,
        records: List<MaintenanceRecord>,
    ): List<MaintenanceSuggestion> {
        val applicableRules = MaintenanceScheduleRules.rules
            .asSequence()
            .filter { it.matches(vehicle) }
            .toList()
            .mostSpecificPerTask()

        return applicableRules
            .asSequence()
            .map { rule -> toSuggestion(rule, latestMileage, records) }
            .sortedBy { it.nextDueMileage }
            .toList()
    }

    /**
     * If two rules share the same task name (e.g. a generic 4WD "transfer
     * case fluid" rule and a Tacoma-specific one), only the more specific one
     * should be shown so the user doesn't see the same task twice.
     */
    private fun List<MaintenanceRule>.mostSpecificPerTask(): List<MaintenanceRule> =
        groupBy { it.taskName.lowercase() }
            .values
            .map { rulesForTask -> rulesForTask.maxBy { it.specificity } }

    private fun toSuggestion(
        rule: MaintenanceRule,
        latestMileage: Int?,
        records: List<MaintenanceRecord>,
    ): MaintenanceSuggestion {
        val lastServiceMileage = records
            .filter { it.satisfies(rule) }
            .maxByOrNull { it.mileage }
            ?.mileage

        var nextDueMileage = (lastServiceMileage ?: 0) + rule.intervalMiles
        // Handles being overdue by more than one interval (e.g. a
        // never-serviced task on a high-mileage vehicle).
        while ((latestMileage != null) && (nextDueMileage <= latestMileage)) {
            nextDueMileage += rule.intervalMiles
        }

        // The loop above guarantees nextDueMileage > latestMileage once a
        // number has been settled on, so there's no separate "overdue" case
        // here -- being overdue just means a larger interval multiple was due.
        val status = when {
            latestMileage == null -> ReminderStatus.OK
            (nextDueMileage - latestMileage) <= ReminderRepository.UPCOMING_WINDOW_MILES -> ReminderStatus.UPCOMING
            else -> ReminderStatus.OK
        }

        return MaintenanceSuggestion(
            rule = rule,
            lastServiceMileage = lastServiceMileage,
            nextDueMileage = nextDueMileage,
            status = status,
        )
    }

    /**
     * Whether [this] record should count as having fulfilled [rule].
     *
     * When the record has a [taskName] (set by the "Add maintenance"
     * UI whenever the user picks one of the built-in tasks), it must match the rule's
     * task name exactly -- this is what lets e.g. a "Brake fluid flush" record leave the
     * separate "Coolant flush" rule alone, even though both share [MaintenanceCategory.FLUIDS].
     *
     * Records with no task name (pre-migration history, or a free-form/custom entry) fall
     * back to matching by [category] alone, same as before this
     * distinction existed. This is intentionally imprecise for any category with more than
     * one rule -- there's no reliable way to guess which specific task an untagged record
     * refers to -- but it avoids silently dropping old service history from the schedule.
     */
    private fun MaintenanceRecord.satisfies(rule: MaintenanceRule): Boolean =
        if (taskName != null) {
            taskName.equals(rule.taskName, ignoreCase = true)
        } else {
            category == rule.category
        }
}
