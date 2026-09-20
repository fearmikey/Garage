package com.fearmikey.garage.data.schedule

import com.fearmikey.garage.data.local.entity.MaintenanceCategory
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.local.entity.Vehicle
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

    const val DEFAULT_UPCOMING_WINDOW_MILES = 500
    const val UPCOMING_WINDOW_DAYS = 30L

    fun suggestionsFor(
        vehicle: Vehicle,
        latestMileage: Int?,
        records: List<MaintenanceRecord>,
        customRules: List<MaintenanceRule> = emptyList(),
        now: Long = System.currentTimeMillis(),
        upcomingWindowMiles: Int = DEFAULT_UPCOMING_WINDOW_MILES,
    ): List<MaintenanceSuggestion> {
        val builtInMatching = MaintenanceScheduleRules.rules.filter { it.matches(vehicle) }
        val applicableRules = (builtInMatching + customRules).mostSpecificPerTask()

        return applicableRules
            .asSequence()
            .map { rule -> toSuggestion(rule, latestMileage, records, now, upcomingWindowMiles) }
            .sortedWith(
                compareBy(
                    { when (it.status) {
                        ReminderStatus.OVERDUE -> 0
                        ReminderStatus.UPCOMING -> 1
                        ReminderStatus.OK -> 2
                        ReminderStatus.COMPLETED -> 3
                    }},
                    { it.nextDueMileage ?: Int.MAX_VALUE },
                    { it.nextDueDate ?: Long.MAX_VALUE },
                ),
            )
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
        upcomingWindowMiles: Int,
    ): MaintenanceSuggestion {
        // Both the mileage- and date-based baselines come from the same "last
        // service" record so the two dimensions always agree on which real
        // event they're projecting forward from.
        val lastService = records.asSequence().filter { it.satisfies(rule) }.maxByOrNull { it.mileage }
        val lastServiceMileage = lastService?.mileage
        val lastServiceDate = lastService?.date

        val (nextDueMileage, mileageStatus) = calculateMileageDueAndStatus(
            rule = rule,
            lastServiceMileage = lastServiceMileage,
            latestMileage = latestMileage,
            upcomingWindowMiles = upcomingWindowMiles,
        )

        val (nextDueDate, dateStatus) = calculateDateDueAndStatus(
            rule = rule,
            lastServiceDate = lastServiceDate,
            now = now,
        )

        val status = when {
            (mileageStatus == ReminderStatus.OVERDUE) || (dateStatus == ReminderStatus.OVERDUE) -> ReminderStatus.OVERDUE
            (mileageStatus == ReminderStatus.UPCOMING) || (dateStatus == ReminderStatus.UPCOMING) -> ReminderStatus.UPCOMING
            else -> ReminderStatus.OK
        }

        return MaintenanceSuggestion(
            rule = rule,
            lastServiceMileage = lastServiceMileage,
            lastServiceDate = lastServiceDate,
            nextDueMileage = nextDueMileage,
            nextDueDate = nextDueDate,
            status = status,
        )
    }

    private fun calculateMileageDueAndStatus(
        rule: MaintenanceRule,
        lastServiceMileage: Int?,
        latestMileage: Int?,
        upcomingWindowMiles: Int,
    ): Pair<Int?, ReminderStatus?> {
        val interval = rule.intervalMiles ?: return Pair(null, null)

        val nextDueMileage = if (lastServiceMileage != null) {
            lastServiceMileage + interval
        } else if (latestMileage == null) {
            interval
        } else {
            var next = interval
            while (next < latestMileage) {
                next += interval
            }
            next
        }

        val status = if (latestMileage == null) {
            ReminderStatus.OK
        } else if (latestMileage >= nextDueMileage) {
            ReminderStatus.OVERDUE
        } else if ((nextDueMileage - latestMileage) <= upcomingWindowMiles) {
            ReminderStatus.UPCOMING
        } else {
            ReminderStatus.OK
        }

        return Pair(nextDueMileage, status)
    }

    private fun calculateDateDueAndStatus(
        rule: MaintenanceRule,
        lastServiceDate: Long?,
        now: Long,
    ): Pair<Long?, ReminderStatus?> {
        val months = rule.intervalMonths ?: return Pair(null, null)

        val nextDueDate = lastServiceDate?.let { addMonths(it, months) } ?: addMonths(now, months)

        val upcomingWindowMillis = TimeUnit.DAYS.toMillis(UPCOMING_WINDOW_DAYS)
        val status = if (now >= nextDueDate) {
            ReminderStatus.OVERDUE
        } else if ((nextDueDate - now) <= upcomingWindowMillis) {
            ReminderStatus.UPCOMING
        } else {
            ReminderStatus.OK
        }

        return Pair(nextDueDate, status)
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
            taskNameLower.contains("rust") -> listOf("rust")
            taskNameLower.contains("paint") -> listOf("paint")
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
            taskNameLower.contains("rust") -> keywords.add("rust")
            taskNameLower.contains("paint") -> keywords.add("paint")
        }

        val words = taskNameLower.split(Regex("[\\s/\\-,_]+"))
            .filter { (it.length >= 3) && (it !in STOP_WORDS) }

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
