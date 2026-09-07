package com.fearmikey.garage.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fearmikey.garage.ui.util.UnitSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "garage_preferences")

class PreferencesManager(private val context: Context) {

    companion object {
        val UNITS_KEY = stringPreferencesKey("units_type")
        val THEME_KEY = stringPreferencesKey("theme_type")
        val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
    }

    val unitsType: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[UNITS_KEY] ?: "metric"
        }

    val unitSystem: Flow<UnitSystem> = unitsType.map { UnitSystem.fromString(it) }

    val themeType: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[THEME_KEY] ?: "system"
        }

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[ONBOARDING_COMPLETED_KEY] ?: false
        }

    suspend fun setUnitsType(units: String) {
        context.dataStore.edit { preferences ->
            preferences[UNITS_KEY] = units
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
}
