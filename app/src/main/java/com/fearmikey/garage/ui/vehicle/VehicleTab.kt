package com.fearmikey.garage.ui.vehicle

import androidx.annotation.StringRes
import com.fearmikey.garage.R

/**
 * Represents the tabs available on the Vehicle Detail screen.
 */
enum class VehicleTab(@StringRes val titleRes: Int) {
    TIMELINE(R.string.tab_timeline),
    SCHEDULE(R.string.tab_schedule),
    FUEL(R.string.tab_fuel),
    EXPENSES(R.string.tab_expenses),
    SPECS(R.string.tab_specs),
    PARTS(R.string.tab_parts),
    MODS(R.string.tab_mods),
    DOCUMENTS(R.string.tab_documents),
    RECALLS(R.string.tab_recalls);

    companion object {
        val entriesList = entries
        fun fromOrdinal(ordinal: Int): VehicleTab = entriesList.getOrElse(ordinal) { TIMELINE }
    }
}
