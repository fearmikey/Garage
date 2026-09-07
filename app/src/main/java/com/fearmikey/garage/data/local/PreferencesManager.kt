package com.fearmikey.garage.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "garage_preferences")

class PreferencesManager(private val context: Context) {

    companion object {
        val UNITS_KEY = stringPreferencesKey("units_type")
        val THEME_KEY = stringPreferencesKey("theme_type")
    }

    val unitsType: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[UNITS_KEY] ?: "metric"
        }

    val themeType: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[THEME_KEY] ?: "system"
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
}
