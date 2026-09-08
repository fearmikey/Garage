package com.fearmikey.garage.data.local.entity

/** Broad categories used to classify a [MaintenanceRecord]. */
enum class MaintenanceCategory(val displayName: String) {
    TIRES("Tires"),
    BRAKES("Brakes"),
    BATTERY("Battery"),
    FLUIDS("Fluids"),
    INSPECTION("Inspection"),
    REPAIR("Repair"),
    OTHER("Other"),
}
