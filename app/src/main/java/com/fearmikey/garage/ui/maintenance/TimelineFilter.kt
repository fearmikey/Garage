package com.fearmikey.garage.ui.maintenance

import com.fearmikey.garage.data.local.entity.MaintenanceCategory
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.remote.lubelogger.isOdometerCheckIn
import com.fearmikey.garage.obd.ObdScanSummary
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** Preset date windows offered by the Timeline filter bar. */
enum class TimelineDateRange(val displayName: String) {
    ALL("All time"),
    LAST_30_DAYS("Last 30 days"),
    LAST_12_MONTHS("Last 12 months"),
    THIS_YEAR("This year"),
    LAST_YEAR("Last year"),
    ;

    /**
     * Returns the inclusive `[start, end]` epoch-millis window for this range relative to [now],
     * or null for [ALL] (no date restriction).
     */
    fun bounds(now: Long): LongRange? = when (this) {
        ALL -> null
        LAST_30_DAYS -> (now - TimeUnit.DAYS.toMillis(30))..Long.MAX_VALUE
        LAST_12_MONTHS -> Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.YEAR, -1)
        }.timeInMillis..Long.MAX_VALUE
        THIS_YEAR -> startOfYear(now, yearOffset = 0)..Long.MAX_VALUE
        LAST_YEAR -> startOfYear(now, yearOffset = -1) until startOfYear(now, yearOffset = 0)
    }

    private fun startOfYear(now: Long, yearOffset: Int): Long = Calendar.getInstance().apply {
        timeInMillis = now
        add(Calendar.YEAR, yearOffset)
        set(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

/**
 * User-selected filters for the maintenance Timeline. Purely in-memory; applied on top of the
 * full record list so no database queries change.
 */
data class TimelineFilter(
    val query: String = "",
    /** Empty means "all categories". */
    val categories: Set<MaintenanceCategory> = emptySet(),
    val dateRange: TimelineDateRange = TimelineDateRange.ALL,
    val receiptsOnly: Boolean = false,
    /** Hides automatic/bookkeeping entries: odometer readings and OBD2 scan logs. */
    val hideAutoEntries: Boolean = false,
) {
    /** True when any filter other than the defaults is applied. */
    val isActive: Boolean
        get() = this != TimelineFilter()

    fun apply(records: List<MaintenanceRecord>, now: Long = System.currentTimeMillis()): List<MaintenanceRecord> {
        if (!isActive) return records
        val bounds = dateRange.bounds(now)
        val needle = query.trim()
        return records.filter { record ->
            (categories.isEmpty() || record.category in categories) &&
                (bounds == null || record.date in bounds) &&
                (!receiptsOnly || !record.receiptUri.isNullOrBlank()) &&
                (!hideAutoEntries || !record.isAutoEntry) &&
                (needle.isEmpty() || record.matchesQuery(needle))
        }
    }
}

private fun MaintenanceRecord.matchesQuery(needle: String): Boolean =
    description.contains(needle, ignoreCase = true) ||
        taskName?.contains(needle, ignoreCase = true) == true ||
        category.displayName.contains(needle, ignoreCase = true)

/** Description used by the Timeline's "Odometer Update" dialog (see AddOdometerUpdateDialog). */
internal const val ODOMETER_UPDATE_DESCRIPTION = "Odometer Update"

/**
 * Whether this record is an automatic/bookkeeping entry rather than real maintenance work:
 * odometer check-ins (widget/LubeLogger), Timeline odometer updates, and OBD2 scan/clear logs.
 */
val MaintenanceRecord.isAutoEntry: Boolean
    get() = isOdometerCheckIn ||
        taskName == ObdScanSummary.SCAN_TASK_NAME ||
        taskName == ObdScanSummary.CLEAR_TASK_NAME ||
        (taskName == null && cost == 0.0 && description == ODOMETER_UPDATE_DESCRIPTION)
