package com.fearmikey.garage.data.local

import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * One-time data fix-up run with the 31 -> 32 auto-migration.
 *
 * Several built-in maintenance tasks (and schedule-template tasks) used to be filed under
 * `OTHER` and now belong to one of the finer categories (FILTERS, ENGINE, BODY, ...). Records
 * and custom rules saved before the split keep `OTHER`, which would hide them from category
 * filters and Cost of Ownership breakdowns. This re-files any `OTHER` row whose task name is
 * one of those known tasks. Free-form `OTHER` entries without a known task are left alone.
 *
 * The mapping is frozen here on purpose (rather than read from MaintenanceScheduleRules) so the
 * migration stays deterministic even if the rules change later.
 */
class MaintenanceCategorySplitSpec : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        for ((category, taskNames) in TASK_CATEGORY_SPLIT) {
            val placeholders = taskNames.joinToString(",") { "?" }
            val args: Array<Any> = (listOf(category) + taskNames.map { it.lowercase() }).toTypedArray()
            for (table in listOf("maintenance_records", "custom_maintenance_rules")) {
                db.execSQL(
                    "UPDATE $table SET category = ? " +
                        "WHERE category = 'OTHER' AND lower(taskName) IN ($placeholders)",
                    args,
                )
            }
        }
    }

    companion object {
        /** Category enum name -> task names (matched case-insensitively) moved out of OTHER. */
        internal val TASK_CATEGORY_SPLIT: Map<String, List<String>> = mapOf(
            "FILTERS" to listOf(
                "Cabin air filter replacement",
                "Engine air filter replacement",
                "Cabin & Engine Air Filters",
                "Cabin Air Filter & HV Battery Duct Check",
            ),
            "ENGINE" to listOf(
                "Spark plug replacement",
                "Serpentine belt replacement",
                "Valve clearance check",
                "Ignition, Spark Plugs & Distributer",
                "V-Belts & Tensioner Check",
            ),
            "BODY" to listOf(
                "Rust prevention",
                "Paint protection",
                "Wiper blade replacement",
            ),
            "DRIVETRAIN" to listOf(
                "Drive chain clean & lube",
                "Chain & sprocket replacement",
                "Clutch & throttle cable adjustment",
                "Chain Clean, Lube & Slack Check",
            ),
            "SUSPENSION" to listOf(
                "Chassis Greasing & Ball Joints",
                "Chassis & Steering Greasing",
            ),
        )
    }
}
