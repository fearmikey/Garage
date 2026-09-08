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
     * Records with no explicit task name (e.g. an "Other / custom" entry like "Daytime running lights")
     * fall back to matching by [category] AND checking if the record's description matches the rule's
     * task name or keywords. This prevents custom/other records (such as replacing lights or wipers) from
     * incorrectly fulfilling unrelated rules in the same category (like "Spark plug replacement").
     */
    private fun MaintenanceRecord.satisfies(rule: MaintenanceRule): Boolean {
        if (taskName != null) {
            if (taskName.equals(rule.taskName, ignoreCase = true)) {
                return true
            }
            val ruleNameLower = rule.taskName.lowercase()
            val recordTaskLower = taskName.lowercase()

            // Any tire rotation service (Rotation, Rotation & Balance, etc.) satisfies a tire rotation rule
            return ruleNameLower.contains("rotat") && recordTaskLower.contains("rotat")
        }
        if (category != rule.category) {
            return false
        }
        return descriptionMatchesTask(description, rule.taskName)
    }

    private fun descriptionMatchesTask(description: String, taskName: String): Boolean {
        val cleanDesc = description.trim().lowercase()
        val cleanTask = taskName.trim().lowercase()
        if (cleanDesc.isEmpty()) return false

        if (cleanDesc.contains(cleanTask) || cleanTask.contains(cleanDesc)) {
            return true
        }

        val requiredKeywords = getRequiredKeywords(cleanTask)
        if (requiredKeywords.isNotEmpty()) {
            return requiredKeywords.all { cleanDesc.contains(it) }
        }

        val taskKeywords = extractKeywords(cleanTask)
        if (taskKeywords.isEmpty()) return false

        return taskKeywords.any { keyword -> cleanDesc.contains(keyword) }
    }

    private fun getRequiredKeywords(taskNameLower: String): List<String> {
        return when {
            taskNameLower.contains("spark plug") -> listOf("spark")
            taskNameLower.contains("cabin") -> listOf("cabin")
            taskNameLower.contains("engine air") -> listOf("engine")
            taskNameLower.contains("front diff") || taskNameLower.contains("front differential") -> listOf("front")
            taskNameLower.contains("rear diff") || taskNameLower.contains("rear differential") -> listOf("rear")
            taskNameLower.contains("power steering") -> listOf("steering")
            taskNameLower.contains("transfer case") -> listOf("transfer")
            taskNameLower.contains("brake fluid") -> listOf("brake")
            taskNameLower.contains("coolant") || taskNameLower.contains("antifreeze") -> listOf("coolant")
            taskNameLower.contains("transmission") || taskNameLower.contains("atf") -> listOf("transmission")
            taskNameLower.contains("rotation and balance") -> listOf("rotat", "balanc")
            taskNameLower.contains("rotation") -> listOf("rotat")
            taskNameLower.contains("tire replacement") -> listOf("tire")
            else -> emptyList()
        }
    }

    private fun extractKeywords(taskNameLower: String): List<String> {
        val keywords = mutableListOf<String>()

        when {
            taskNameLower.contains("oil") -> keywords.add("oil")
            taskNameLower.contains("rotation") || taskNameLower.contains("rotate") -> keywords.add("rotat")
            taskNameLower.contains("balance") -> keywords.add("balanc")
            taskNameLower.contains("battery") -> keywords.add("battery")
        }

        val words = taskNameLower.split(Regex("[\\s/\\-,_]+"))
            .filter { it.length >= 3 && it !in STOP_WORDS }

        for (word in words) {
            if (word !in keywords) {
                keywords.add(word)
            }
        }

        return keywords
    }

    private val STOP_WORDS = setOf(
        "replacement", "replace", "replaced", "replacing",
        "change", "changed", "changing",
        "flush", "flushed", "flushing",
        "service", "serviced", "servicing",
        "test", "testing", "check", "checking", "checked",
        "load", "fluid", "and", "for", "with", "other", "custom", "filter", "air",
    )
}
