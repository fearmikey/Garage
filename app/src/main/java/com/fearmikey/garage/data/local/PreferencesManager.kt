package com.fearmikey.garage.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fearmikey.garage.ui.util.AppCurrency
import com.fearmikey.garage.ui.util.UnitSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "garage_preferences")

class PreferencesManager(private val context: Context) {

    companion object {
        val UNITS_KEY = stringPreferencesKey("units_type")
        val CURRENCY_KEY = stringPreferencesKey("currency_code")
        val THEME_KEY = stringPreferencesKey("theme_type")
        val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
        val DEFAULT_VEHICLE_ID_KEY = longPreferencesKey("default_vehicle_id")
        val MAINTENANCE_MILEAGE_WINDOW_KEY = intPreferencesKey("maintenance_mileage_window")
        val MAINTENANCE_DAYS_WINDOW_KEY = intPreferencesKey("maintenance_days_window")
        val APP_OPEN_COUNT_KEY = intPreferencesKey("app_open_count")
        val BUY_ME_A_COFFEE_DONT_ASK_AGAIN_KEY = booleanPreferencesKey("buy_me_a_coffee_dont_ask_again")
        val BUY_ME_A_COFFEE_NEXT_PROMPT_OPEN_COUNT_KEY = intPreferencesKey("buy_me_a_coffee_next_prompt_open_count")
        val MODS_GRID_VIEW_KEY = booleanPreferencesKey("is_mods_grid_view")
        val INCLUDE_MODS_IN_COST_KEY = booleanPreferencesKey("include_mods_in_cost")
        val AFFILIATE_LINKS_ENABLED_KEY = booleanPreferencesKey("affiliate_links_enabled")
        val DRIVERS_LICENSE_NUMBER_KEY = stringPreferencesKey("drivers_license_number")
        val DRIVERS_LICENSE_STATE_KEY = stringPreferencesKey("drivers_license_state")
        val DRIVERS_LICENSE_EXPIRATION_KEY = longPreferencesKey("drivers_license_expiration")
        val DRIVERS_LICENSE_NOTES_KEY = stringPreferencesKey("drivers_license_notes")
        val DRIVERS_LICENSE_IMAGE_FRONT_KEY = stringPreferencesKey("drivers_license_image_front")
        val DRIVERS_LICENSE_IMAGE_BACK_KEY = stringPreferencesKey("drivers_license_image_back")
        val DOCUMENT_EXPIRATION_REMINDERS_ENABLED_KEY = booleanPreferencesKey("document_expiration_reminders_enabled")
        val DOCUMENT_EXPIRATION_DAYS_WINDOW_KEY = intPreferencesKey("document_expiration_days_window")
        val SHOW_FUEL_TREND_GRAPH_KEY = booleanPreferencesKey("show_fuel_trend_graph")
        val SHOW_FLEET_OVERVIEW_KEY = booleanPreferencesKey("show_fleet_overview")

        const val DEFAULT_MAINTENANCE_MILEAGE_WINDOW = 500
        const val DEFAULT_MAINTENANCE_DAYS_WINDOW = 10
        const val DEFAULT_DOCUMENT_EXPIRATION_DAYS_WINDOW = 30
    }

    val unitsType: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[UNITS_KEY] ?: "metric"
        }

    val unitSystem: Flow<UnitSystem> = unitsType.map { UnitSystem.fromString(it) }

    val currencyCode: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[CURRENCY_KEY] ?: "USD"
        }

    val appCurrency: Flow<AppCurrency> = currencyCode.map { AppCurrency.fromCode(it) }

    val themeType: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[THEME_KEY] ?: "system"
        }

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[ONBOARDING_COMPLETED_KEY] ?: false
        }

    val appOpenCount: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[APP_OPEN_COUNT_KEY] ?: 0
        }

    val buyMeACoffeeDontAskAgain: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[BUY_ME_A_COFFEE_DONT_ASK_AGAIN_KEY] ?: false
        }

    val buyMeACoffeeNextPromptOpenCount: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[BUY_ME_A_COFFEE_NEXT_PROMPT_OPEN_COUNT_KEY] ?: 2
        }

    /**
     * The vehicle the user has explicitly chosen as their "default" (e.g. for the
     * home screen widget's Log Service/Log Fuel shortcuts). `null` means no explicit
     * choice has been made, and callers should fall back to some other default
     * (such as the first vehicle added).
     */
    val defaultVehicleId: Flow<Long?> = context.dataStore.data
        .map { preferences ->
            preferences[DEFAULT_VEHICLE_ID_KEY]
        }

    val maintenanceMileageWindow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[MAINTENANCE_MILEAGE_WINDOW_KEY] ?: DEFAULT_MAINTENANCE_MILEAGE_WINDOW
        }

    val maintenanceDaysWindow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[MAINTENANCE_DAYS_WINDOW_KEY] ?: DEFAULT_MAINTENANCE_DAYS_WINDOW
        }

    val isModsGridView: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[MODS_GRID_VIEW_KEY] ?: true
        }

    val includeModsInCost: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[INCLUDE_MODS_IN_COST_KEY] ?: false
        }

    val affiliateLinksEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[AFFILIATE_LINKS_ENABLED_KEY] ?: false
        }

    val showFuelTrendGraph: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SHOW_FUEL_TREND_GRAPH_KEY] ?: true
        }

    val showFleetOverview: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SHOW_FLEET_OVERVIEW_KEY] ?: false
        }

    val documentExpirationRemindersEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[DOCUMENT_EXPIRATION_REMINDERS_ENABLED_KEY] ?: true
        }

    val documentExpirationDaysWindow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[DOCUMENT_EXPIRATION_DAYS_WINDOW_KEY] ?: DEFAULT_DOCUMENT_EXPIRATION_DAYS_WINDOW
        }

    val driversLicenseNumber: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[DRIVERS_LICENSE_NUMBER_KEY]
        }

    val driversLicenseState: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[DRIVERS_LICENSE_STATE_KEY]
        }

    val driversLicenseExpiration: Flow<Long?> = context.dataStore.data
        .map { preferences ->
            preferences[DRIVERS_LICENSE_EXPIRATION_KEY]
        }

    val driversLicenseNotes: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[DRIVERS_LICENSE_NOTES_KEY]
        }

    val driversLicenseImageFront: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[DRIVERS_LICENSE_IMAGE_FRONT_KEY]
        }

    val driversLicenseImageBack: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[DRIVERS_LICENSE_IMAGE_BACK_KEY]
        }

    suspend fun incrementAppOpenCount(): Int {
        var newCount = 1
        context.dataStore.edit { preferences ->
            val current = preferences[APP_OPEN_COUNT_KEY] ?: 0
            newCount = current + 1
            preferences[APP_OPEN_COUNT_KEY] = newCount
        }
        return newCount
    }

    suspend fun setBuyMeACoffeeDontAskAgain(dontAskAgain: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BUY_ME_A_COFFEE_DONT_ASK_AGAIN_KEY] = dontAskAgain
        }
    }

    suspend fun setBuyMeACoffeeNextPromptOpenCount(openCount: Int) {
        context.dataStore.edit { preferences ->
            preferences[BUY_ME_A_COFFEE_NEXT_PROMPT_OPEN_COUNT_KEY] = openCount
        }
    }

    suspend fun setUnitsType(units: String) {
        context.dataStore.edit { preferences ->
            preferences[UNITS_KEY] = units
        }
    }

    suspend fun setCurrencyCode(currencyCode: String) {
        context.dataStore.edit { preferences ->
            preferences[CURRENCY_KEY] = currencyCode
        }
    }

    suspend fun setThemeType(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED_KEY] = completed
        }
    }

    suspend fun setDefaultVehicleId(vehicleId: Long?) {
        context.dataStore.edit { preferences ->
            if (vehicleId != null) {
                preferences[DEFAULT_VEHICLE_ID_KEY] = vehicleId
            } else {
                preferences.remove(DEFAULT_VEHICLE_ID_KEY)
            }
        }
    }

    suspend fun setMaintenanceMileageWindow(miles: Int) {
        context.dataStore.edit { preferences ->
            preferences[MAINTENANCE_MILEAGE_WINDOW_KEY] = miles
        }
    }

    suspend fun setMaintenanceDaysWindow(days: Int) {
        context.dataStore.edit { preferences ->
            preferences[MAINTENANCE_DAYS_WINDOW_KEY] = days
        }
    }

    suspend fun setModsGridView(isGrid: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[MODS_GRID_VIEW_KEY] = isGrid
        }
    }

    suspend fun setIncludeModsInCost(includeMods: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[INCLUDE_MODS_IN_COST_KEY] = includeMods
        }
    }

    suspend fun setAffiliateLinksEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AFFILIATE_LINKS_ENABLED_KEY] = enabled
        }
    }

    suspend fun setShowFuelTrendGraph(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_FUEL_TREND_GRAPH_KEY] = enabled
        }
    }

    suspend fun setShowFleetOverview(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_FLEET_OVERVIEW_KEY] = enabled
        }
    }

    suspend fun setDocumentExpirationRemindersEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DOCUMENT_EXPIRATION_REMINDERS_ENABLED_KEY] = enabled
        }
    }

    suspend fun setDocumentExpirationDaysWindow(days: Int) {
        context.dataStore.edit { preferences ->
            preferences[DOCUMENT_EXPIRATION_DAYS_WINDOW_KEY] = days
        }
    }

    suspend fun setDriversLicense(
        number: String?,
        state: String?,
        expiration: Long?,
        notes: String?,
        imageFront: String?,
        imageBack: String?,
    ) {
        context.dataStore.edit { preferences ->
            if (!number.isNullOrBlank()) preferences[DRIVERS_LICENSE_NUMBER_KEY] = number else preferences.remove(DRIVERS_LICENSE_NUMBER_KEY)
            if (!state.isNullOrBlank()) preferences[DRIVERS_LICENSE_STATE_KEY] = state else preferences.remove(DRIVERS_LICENSE_STATE_KEY)
            if (expiration != null) preferences[DRIVERS_LICENSE_EXPIRATION_KEY] = expiration else preferences.remove(DRIVERS_LICENSE_EXPIRATION_KEY)
            if (!notes.isNullOrBlank()) preferences[DRIVERS_LICENSE_NOTES_KEY] = notes else preferences.remove(DRIVERS_LICENSE_NOTES_KEY)
            if (!imageFront.isNullOrBlank()) preferences[DRIVERS_LICENSE_IMAGE_FRONT_KEY] = imageFront else preferences.remove(DRIVERS_LICENSE_IMAGE_FRONT_KEY)
            if (!imageBack.isNullOrBlank()) preferences[DRIVERS_LICENSE_IMAGE_BACK_KEY] = imageBack else preferences.remove(DRIVERS_LICENSE_IMAGE_BACK_KEY)
        }
    }
}
