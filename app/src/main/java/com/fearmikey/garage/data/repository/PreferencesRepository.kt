package com.fearmikey.garage.data.repository

import com.fearmikey.garage.data.local.PreferencesManager
import com.fearmikey.garage.ui.util.UnitSystem
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val unitsType: Flow<String>
    val unitSystem: Flow<UnitSystem>
    val themeType: Flow<String>
    val onboardingCompleted: Flow<Boolean>
    suspend fun setUnitsType(units: String)
    suspend fun setThemeType(theme: String)
    suspend fun setOnboardingCompleted(completed: Boolean)
}

class PreferencesRepositoryImpl(private val preferencesManager: PreferencesManager) : PreferencesRepository {
    override val unitsType: Flow<String> = preferencesManager.unitsType
    override val unitSystem: Flow<UnitSystem> = preferencesManager.unitSystem
    override val themeType: Flow<String> = preferencesManager.themeType
    override val onboardingCompleted: Flow<Boolean> = preferencesManager.onboardingCompleted

    override suspend fun setUnitsType(units: String) {
        preferencesManager.setUnitsType(units)
    }

    override suspend fun setThemeType(theme: String) {
        preferencesManager.setThemeType(theme)
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        preferencesManager.setOnboardingCompleted(completed)
    }
}
