package com.fearmikey.garage.data.repository

import com.fearmikey.garage.data.local.PreferencesManager
import com.fearmikey.garage.ui.util.UnitSystem
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val unitsType: Flow<String>
    val unitSystem: Flow<UnitSystem>
    val themeType: Flow<String>
    val onboardingCompleted: Flow<Boolean>
    /** The user's explicitly chosen default vehicle, or `null` if none has been chosen. */
    val defaultVehicleId: Flow<Long?>
    suspend fun setUnitsType(units: String)
    suspend fun setThemeType(theme: String)
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun setDefaultVehicleId(vehicleId: Long?)
}

class PreferencesRepositoryImpl(private val preferencesManager: PreferencesManager) : PreferencesRepository {
    override val unitsType: Flow<String> = preferencesManager.unitsType
    override val unitSystem: Flow<UnitSystem> = preferencesManager.unitSystem
    override val themeType: Flow<String> = preferencesManager.themeType
    override val onboardingCompleted: Flow<Boolean> = preferencesManager.onboardingCompleted
    override val defaultVehicleId: Flow<Long?> = preferencesManager.defaultVehicleId

    override suspend fun setUnitsType(units: String) {
        preferencesManager.setUnitsType(units)
    }

    override suspend fun setThemeType(theme: String) {
        preferencesManager.setThemeType(theme)
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        preferencesManager.setOnboardingCompleted(completed)
    }

    override suspend fun setDefaultVehicleId(vehicleId: Long?) {
        preferencesManager.setDefaultVehicleId(vehicleId)
    }
}
