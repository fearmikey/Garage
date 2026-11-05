package com.fearmikey.garage.ui.vehicle

import androidx.annotation.StringRes
import com.fearmikey.garage.R

/**
 * Represents the tabs available on the Vehicle Detail screen.
 *
 * Deep links (widgets, notifications, dashboard) refer to tabs by [ordinal], so the
 * declaration order must stay stable. Per-vehicle visibility is persisted by [name]
 * in [com.fearmikey.garage.data.local.entity.Vehicle.hiddenTabs].
 */
enum class VehicleTab(
    @StringRes val titleRes: Int,
    /** Whether the user may hide this tab. The Timeline always stays visible. */
    val canHide: Boolean = true,
) {
    TIMELINE(R.string.tab_timeline, canHide = false),
    SCHEDULE(R.string.tab_schedule),
    FUEL(R.string.tab_fuel),
    EXPENSES(R.string.tab_expenses),
    SPECS(R.string.tab_specs),
    PARTS(R.string.tab_parts),
    MODS(R.string.tab_mods),
    DOCUMENTS(R.string.tab_documents),
    RECALLS(R.string.tab_recalls),
    KNOWLEDGE(R.string.tab_knowledge),
    TRIPS(R.string.tab_trips);

    companion object {
        val entriesList = entries
        fun fromOrdinal(ordinal: Int): VehicleTab = entriesList.getOrElse(ordinal) { TIMELINE }

        /** Parses custom ordered list of ALL tabs. Any tabs omitted in [rawOrder] default to standard order at end. */
        fun parseOrder(rawOrder: String?): List<VehicleTab> {
            if (rawOrder.isNullOrBlank()) return entriesList
            val parsed = rawOrder.split(',')
                .mapNotNull { name -> entriesList.firstOrNull { it.name == name.trim() } }
            val missing = entriesList.filter { it !in parsed }
            return parsed + missing
        }

        /** Serializes tab order to a comma-separated string of enum names. */
        fun serializeOrder(order: List<VehicleTab>): String =
            order.joinToString(",") { it.name }

        /** Parses the comma-separated tab names stored on a vehicle, ignoring unknown/non-hideable entries. */
        fun parseHidden(raw: String?): Set<VehicleTab> {
            if (raw.isNullOrBlank()) return emptySet()
            return raw.split(',')
                .mapNotNull { name -> entriesList.firstOrNull { it.name == name.trim() } }
                .filter { it.canHide }
                .toSet()
        }

        /** Serializes hidden tabs into a stable, comma-separated string of enum names. */
        fun serializeHidden(hidden: Set<VehicleTab>): String =
            entriesList.filter { it in hidden && it.canHide }.joinToString(",") { it.name }
    }
}
