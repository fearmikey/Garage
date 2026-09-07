package com.fearmikey.garage.data.local.entity

/** Broad categories used to classify a [MaintenanceRecord]. */
enum class MaintenanceCategory(val displayName: String) {
    OIL_CHANGE("Oil Change"),
    TIRE_ROTATION("Tire Rotation"),
    TIRES("Tires"),
    BRAKES("Brakes"),
    BATTERY("Battery"),
    FLUIDS("Fluids"),
    INSPECTION("Inspection"),
    REPAIR("Repair"),
    OTHER("Other"),
}
