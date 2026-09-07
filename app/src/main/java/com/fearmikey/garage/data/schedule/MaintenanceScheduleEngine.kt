package com.fearmikey.garage.data.schedule

import com.fearmikey.garage.data.local.entity.MaintenanceCategory
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.data.repository.ReminderRepository
import com.fearmikey.garage.data.repository.ReminderStatus
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Turns [MaintenanceScheduleRules] (plus any user-defined
 * [com.fearmikey.garage.data.local.entity.CustomMaintenanceRule]s) into
 * concrete, mileage- and/or time-based suggestions for a specific vehicle,
 * given its service history and latest known mileage.
 */
object MaintenanceScheduleEngine {

    fun suggestionsFor(
        vehicle: Vehicle,
        latestMileage: Int?,
        records: List<MaintenanceRecord>,
        customRules: List<MaintenanceRule> = emptyList(),
        now: Long = System.currentTimeMillis(),
    ): List<MaintenanceSuggestion> {
        val builtInMatching = MaintenanceScheduleRules.rules.filter { it.matches(vehicle) }
        val applicableRules = (builtInMatching + customRules).mostSpecificPerTask()

        return applicableRules
            .asSequence()
            .map { rule -> toSuggestion(rule, latestMileage, records, now) }
            .sortedBy { it.nextDueMileage ?: Int.MAX_VALUE }
            .toList()
    }

    /**
     * If two rules share the same task name (e.g. a generic 4WD "transfer
     * case fluid" rule and a Tacoma-specific one, or a built-in rule the user
     * has overridden with a [MaintenanceRule.isCustom] one of their own),
     * only the most-preferred one should be shown so the user doesn't see the
     * same task twice. Custom rules always beat built-in ones; among
     * built-in rules, the more specific (make/model/drivetrain-matched) one wins.
     */
    private fun List<MaintenanceRule>.mostSpecificPerTask(): List<MaintenanceRule> =
        groupBy { it.taskName.lowercase() }
            .values
            .map { rulesForTask -> rulesForTask.maxWith(compareBy({ it.isCustom }, { it.specificity })) }

    private fun toSuggestion(
        rule: MaintenanceRule,
        latestMileage: Int?,
        records: List<MaintenanceRecord>,
        now: Long,
    ): MaintenanceSuggestion {
        // Both the mileage- and date-based baselines come from the same "last
        // service" record so the two dimensions always agree on which real
        // event they're projecting forward from.
        val lastService = records.filter { it.satisfies(rule) }.maxByOrNull { it.mileage }
        val lastServiceMileage = lastService?.mileage
        val lastServiceDate = lastService?.date

        val nextDueMileage = rule.intervalMiles?.let { interval ->
            var next = (lastServiceMileage ?: 0) + interval
            // Handles being overdue by more than one interval (e.g. a
            // never-serviced task on a high-mileage vehicle).
            while ((latestMileage != null) && (next <= latestMileage)) {
                next += interval
            }
            next
        }

        val nextDueDate = rule.intervalMonths?.let { months ->
            // When never serviced, project forward from today rather than
            // the epoch -- there's no reliable "vehicle age" baseline to use
            // instead, and this avoids claiming an untracked task is already
            // wildly overdue purely because it's never been logged.
            var next = addMonths(lastServiceDate ?: now, months)
            while (next <= now) {
                next = addMonths(next, months)
            }
            next
        }

        // The loops above guarantee both projections land strictly after
        // "now" (mileage- and date-wise) once settled on, so there's no
        // separate "overdue" case here -- being overdue just means a larger
        // interval multiple was due. Whichever dimension is closer/more
        // urgent (mileage or time) wins, matching ReminderRepository's
        // "whichever comes first" semantics for user-set Reminders.
        val mileageStatus = if (nextDueMileage == null || latestMileage == null) {
            null
        } else if ((nextDueMileage - latestMileage) <= ReminderRepository.UPCOMING_WINDOW_MILES) {
            ReminderStatus.UPCOMING
        } else {
            ReminderStatus.OK
        }

        val dateStatus = nextDueDate?.let {
            val upcomingWindowMillis = TimeUnit.DAYS.toMillis(ReminderRepository.UPCOMING_WINDOW_DAYS)
            if ((it - now) <= upcomingWindowMillis) ReminderStatus.UPCOMING else ReminderStatus.OK
        }

        val status = listOfNotNull(mileageStatus, dateStatus)
            .minByOrNull { if (it == ReminderStatus.UPCOMING) 0 else 1 }
            ?: ReminderStatus.OK

        return MaintenanceSuggestion(
            rule = rule,
            lastServiceMileage = lastServiceMileage,
            lastServiceDate = lastServiceDate,
            nextDueMileage = nextDueMileage,
            nextDueDate = nextDueDate,
            status = status,
        )
    }

    private fun addMonths(epochMillis: Long, months: Int): Long =
        Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .plusMonths(months.toLong())
            .toInstant()
            .toEpochMilli()

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
