package com.fearmikey.garage.data.repository

import com.fearmikey.garage.data.local.PreferencesManager
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val unitsType: Flow<String>
    val themeType: Flow<String>
    suspend fun setUnitsType(units: String)
    suspend fun setThemeType(theme: String)
}

class PreferencesRepositoryImpl(private val preferencesManager: PreferencesManager) : PreferencesRepository {
    override val unitsType: Flow<String> = preferencesManager.unitsType
    override val themeType: Flow<String> = preferencesManager.themeType

    override suspend fun setUnitsType(units: String) {
        preferencesManager.setUnitsType(units)
    }

    override suspend fun setThemeType(theme: String) {
        preferencesManager.setThemeType(theme)
    }
}
