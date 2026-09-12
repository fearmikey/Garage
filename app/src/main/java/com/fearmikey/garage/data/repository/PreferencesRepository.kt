package com.fearmikey.garage.data.repository

import com.fearmikey.garage.data.local.PreferencesManager
import com.fearmikey.garage.ui.util.AppCurrency
import com.fearmikey.garage.ui.util.UnitSystem
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val unitsType: Flow<String>
    val unitSystem: Flow<UnitSystem>
    val currencyCode: Flow<String>
    val appCurrency: Flow<AppCurrency>
    val themeType: Flow<String>
    val onboardingCompleted: Flow<Boolean>
    val termsAccepted: Flow<Boolean>
    /** The user's explicitly chosen default vehicle, or `null` if none has been chosen. */
    val defaultVehicleId: Flow<Long?>
    suspend fun setUnitsType(units: String)
    suspend fun setCurrencyCode(currencyCode: String)
    suspend fun setThemeType(theme: String)
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun setTermsAccepted(accepted: Boolean)
    suspend fun setDefaultVehicleId(vehicleId: Long?)
}

class PreferencesRepositoryImpl(private val preferencesManager: PreferencesManager) : PreferencesRepository {
    override val unitsType: Flow<String> = preferencesManager.unitsType
    override val unitSystem: Flow<UnitSystem> = preferencesManager.unitSystem
    override val currencyCode: Flow<String> = preferencesManager.currencyCode
    override val appCurrency: Flow<AppCurrency> = preferencesManager.appCurrency
    override val themeType: Flow<String> = preferencesManager.themeType
    override val onboardingCompleted: Flow<Boolean> = preferencesManager.onboardingCompleted
    override val termsAccepted: Flow<Boolean> = preferencesManager.termsAccepted
    override val defaultVehicleId: Flow<Long?> = preferencesManager.defaultVehicleId

    override suspend fun setUnitsType(units: String) {
        preferencesManager.setUnitsType(units)
    }

    override suspend fun setCurrencyCode(currencyCode: String) {
        preferencesManager.setCurrencyCode(currencyCode)
    }

    override suspend fun setThemeType(theme: String) {
        preferencesManager.setThemeType(theme)
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        preferencesManager.setOnboardingCompleted(completed)
    }

    override suspend fun setTermsAccepted(accepted: Boolean) {
        preferencesManager.setTermsAccepted(accepted)
    }

    override suspend fun setDefaultVehicleId(vehicleId: Long?) {
        preferencesManager.setDefaultVehicleId(vehicleId)
    }
}
