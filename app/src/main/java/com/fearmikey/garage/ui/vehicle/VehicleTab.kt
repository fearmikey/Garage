package com.fearmikey.garage.ui.vehicle

/**
 * Represents the tabs available on the Vehicle Detail screen.
 */
enum class VehicleTab(val title: String) {
    TIMELINE("Timeline"),
    SCHEDULE("Schedule"),
    FUEL("Fuel"),
    EXPENSES("Expenses"),
    SPECS("Specs"),
    PARTS("Parts"),
    MODS("Mods"),
    DOCUMENTS("Docs & Insurance"),
    RECALLS("Recalls");

    companion object {
        val entriesList = entries
        fun fromOrdinal(ordinal: Int): VehicleTab = entriesList.getOrElse(ordinal) { TIMELINE }
    }
}
