package com.fearmikey.garage.data.local.entity

/**
 * Broad categories used to classify a [MaintenanceRecord].
 *
 * Persisted by [name] (see Converters), so entries may be reordered freely, but existing
 * names must never be renamed or removed. Declaration order is the order shown in pickers.
 */
enum class MaintenanceCategory(val displayName: String) {
    FLUIDS("Fluids"),
    FILTERS("Filters"),
    TIRES("Tires"),
    BRAKES("Brakes"),
    BATTERY("Battery"),
    ENGINE("Engine & Ignition"),
    DRIVETRAIN("Drivetrain & Chain"),
    SUSPENSION("Suspension & Steering"),
    ELECTRICAL("Electrical & Lighting"),
    HVAC("A/C & Heating"),
    BODY("Body, Glass & Wipers"),
    INSPECTION("Inspection"),
    REPAIR("Repair"),
    OTHER("Other"),
    ;

    companion object {
        /**
         * Categories that were later carved out of [OTHER]. Built-in tasks now filed under
         * one of these used to be [OTHER], so legacy records saved as [OTHER] must still be allowed
         * to match them (see MaintenanceScheduleEngine).
         */
        val SPLIT_FROM_OTHER: Set<MaintenanceCategory> = setOf(
            FILTERS, ENGINE, DRIVETRAIN, SUSPENSION, ELECTRICAL, HVAC, BODY,
        )
    }
}
