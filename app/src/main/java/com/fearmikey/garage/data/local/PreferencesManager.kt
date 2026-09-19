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
        val APP_OPEN_COUNT_KEY = intPreferencesKey("app_open_count")
        val BUY_ME_A_COFFEE_DONT_ASK_AGAIN_KEY = booleanPreferencesKey("buy_me_a_coffee_dont_ask_again")
        val BUY_ME_A_COFFEE_NEXT_PROMPT_OPEN_COUNT_KEY = intPreferencesKey("buy_me_a_coffee_next_prompt_open_count")

        const val DEFAULT_MAINTENANCE_MILEAGE_WINDOW = 500
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
}
