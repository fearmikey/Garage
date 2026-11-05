package com.fearmikey.garage.ui.maintenance

import com.fearmikey.garage.data.local.entity.MaintenanceCategory
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.remote.lubelogger.ODOMETER_CHECK_IN_TASK
import com.fearmikey.garage.obd.ObdScanSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.concurrent.TimeUnit

class TimelineFilterTest {

    /** 2024-06-15 12:00 local time. */
    private val now = Calendar.getInstance().apply {
        set(2024, Calendar.JUNE, 15, 12, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun record(
        id: Long,
        description: String = "Service $id",
        category: MaintenanceCategory = MaintenanceCategory.OTHER,
        daysAgo: Long = 0,
        cost: Double = 50.0,
        taskName: String? = null,
        receiptUri: String? = null,
    ) = MaintenanceRecord(
        id = id,
        vehicleId = 1L,
        date = now - TimeUnit.DAYS.toMillis(daysAgo),
        mileage = 1000,
        description = description,
        cost = cost,
        category = category,
        taskName = taskName,
        receiptUri = receiptUri,
    )

    @Test
    fun `default filter is inactive and returns all records`() {
        val records = listOf(record(1), record(2))
        val filter = TimelineFilter()
        assertFalse(filter.isActive)
        assertEquals(records, filter.apply(records, now))
    }

    @Test
    fun `category filter keeps only selected categories`() {
        val records = listOf(
            record(1, category = MaintenanceCategory.FLUIDS),
            record(2, category = MaintenanceCategory.BRAKES),
            record(3, category = MaintenanceCategory.TIRES),
        )
        val result = TimelineFilter(
            categories = setOf(MaintenanceCategory.FLUIDS, MaintenanceCategory.TIRES),
        ).apply(records, now)
        assertEquals(listOf(1L, 3L), result.map { it.id })
    }

    @Test
    fun `query matches description, task name and category case-insensitively`() {
        val records = listOf(
            record(1, description = "Oil change"),
            record(2, description = "Misc", taskName = "Engine OIL and filter"),
            record(3, description = "Pads", category = MaintenanceCategory.BRAKES),
            record(4, description = "Wipers"),
        )
        assertEquals(listOf(1L, 2L), TimelineFilter(query = " oil ").apply(records, now).map { it.id })
        assertEquals(listOf(3L), TimelineFilter(query = "brakes").apply(records, now).map { it.id })
    }

    @Test
    fun `receipts only excludes records without an attachment`() {
        val records = listOf(record(1, receiptUri = "a.jpg"), record(2), record(3, receiptUri = ""))
        assertEquals(listOf(1L), TimelineFilter(receiptsOnly = true).apply(records, now).map { it.id })
    }

    @Test
    fun `hide auto entries removes odometer readings and OBD scans`() {
        val records = listOf(
            record(1, description = "Oil change", category = MaintenanceCategory.FLUIDS),
            record(2, description = ODOMETER_UPDATE_DESCRIPTION, cost = 0.0),
            record(3, category = MaintenanceCategory.INSPECTION, taskName = ODOMETER_CHECK_IN_TASK, cost = 0.0),
            record(4, category = MaintenanceCategory.INSPECTION, taskName = ObdScanSummary.SCAN_TASK_NAME),
            record(5, category = MaintenanceCategory.INSPECTION, taskName = ObdScanSummary.CLEAR_TASK_NAME),
        )
        assertTrue(records.drop(1).all { it.isAutoEntry })
        assertEquals(listOf(1L), TimelineFilter(hideAutoEntries = true).apply(records, now).map { it.id })
    }

    @Test
    fun `date ranges filter by record date`() {
        val records = listOf(
            record(1, daysAgo = 5),
            record(2, daysAgo = 100),   // early March 2024
            record(3, daysAgo = 300),   // August 2023
            record(4, daysAgo = 500),   // early 2023
        )
        fun ids(range: TimelineDateRange) = TimelineFilter(dateRange = range).apply(records, now).map { it.id }

        assertEquals(listOf(1L, 2L, 3L, 4L), ids(TimelineDateRange.ALL))
        assertEquals(listOf(1L), ids(TimelineDateRange.LAST_30_DAYS))
        assertEquals(listOf(1L, 2L, 3L), ids(TimelineDateRange.LAST_12_MONTHS))
        assertEquals(listOf(1L, 2L), ids(TimelineDateRange.THIS_YEAR))
        assertEquals(listOf(3L, 4L), ids(TimelineDateRange.LAST_YEAR))
    }

    @Test
    fun `filters combine with AND semantics`() {
        val records = listOf(
            record(1, category = MaintenanceCategory.FLUIDS, receiptUri = "r.pdf"),
            record(2, category = MaintenanceCategory.FLUIDS),
            record(3, category = MaintenanceCategory.BRAKES, receiptUri = "r.pdf"),
        )
        val result = TimelineFilter(
            categories = setOf(MaintenanceCategory.FLUIDS),
            receiptsOnly = true,
        ).apply(records, now)
        assertEquals(listOf(1L), result.map { it.id })
    }
}
