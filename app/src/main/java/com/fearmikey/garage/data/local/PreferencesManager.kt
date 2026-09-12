package com.fearmikey.garage.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
        val TERMS_ACCEPTED_KEY = booleanPreferencesKey("terms_accepted")
        val DEFAULT_VEHICLE_ID_KEY = longPreferencesKey("default_vehicle_id")
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

    val termsAccepted: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[TERMS_ACCEPTED_KEY] ?: false
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

    suspend fun setTermsAccepted(accepted: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[TERMS_ACCEPTED_KEY] = accepted
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
}
