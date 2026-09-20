package com.fearmikey.garage.data.repository

import com.fearmikey.garage.data.local.PreferencesManager
import com.fearmikey.garage.ui.util.AppCurrency
import com.fearmikey.garage.ui.util.UnitSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface PreferencesRepository {
    val unitsType: Flow<String>
    val unitSystem: Flow<UnitSystem>
    val currencyCode: Flow<String>
    val appCurrency: Flow<AppCurrency>
    val themeType: Flow<String>
    val onboardingCompleted: Flow<Boolean>
    /** The user's explicitly chosen default vehicle, or `null` if none has been chosen. */
    val defaultVehicleId: Flow<Long?>
    val maintenanceMileageWindow: Flow<Int>
    val appOpenCount: Flow<Int>
    val buyMeACoffeeDontAskAgain: Flow<Boolean>
    val buyMeACoffeeNextPromptOpenCount: Flow<Int>
    val isModsGridView: Flow<Boolean>
        get() = flowOf(false)

    suspend fun setUnitsType(units: String)
    suspend fun setCurrencyCode(currencyCode: String)
    suspend fun setThemeType(theme: String)
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun setDefaultVehicleId(vehicleId: Long?)
    suspend fun setMaintenanceMileageWindow(miles: Int)
    suspend fun incrementAppOpenCount(): Int
    suspend fun setBuyMeACoffeeDontAskAgain(dontAskAgain: Boolean)
    suspend fun setBuyMeACoffeeNextPromptOpenCount(openCount: Int)
    suspend fun setModsGridView(isGrid: Boolean) {}
}

class PreferencesRepositoryImpl(private val preferencesManager: PreferencesManager) : PreferencesRepository {
    override val unitsType: Flow<String> = preferencesManager.unitsType
    override val unitSystem: Flow<UnitSystem> = preferencesManager.unitSystem
    override val currencyCode: Flow<String> = preferencesManager.currencyCode
    override val appCurrency: Flow<AppCurrency> = preferencesManager.appCurrency
    override val themeType: Flow<String> = preferencesManager.themeType
    override val onboardingCompleted: Flow<Boolean> = preferencesManager.onboardingCompleted
    override val defaultVehicleId: Flow<Long?> = preferencesManager.defaultVehicleId
    override val maintenanceMileageWindow: Flow<Int> = preferencesManager.maintenanceMileageWindow
    override val appOpenCount: Flow<Int> = preferencesManager.appOpenCount
    override val buyMeACoffeeDontAskAgain: Flow<Boolean> = preferencesManager.buyMeACoffeeDontAskAgain
    override val buyMeACoffeeNextPromptOpenCount: Flow<Int> = preferencesManager.buyMeACoffeeNextPromptOpenCount
    override val isModsGridView: Flow<Boolean> = preferencesManager.isModsGridView

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

    override suspend fun setDefaultVehicleId(vehicleId: Long?) {
        preferencesManager.setDefaultVehicleId(vehicleId)
    }

    override suspend fun setMaintenanceMileageWindow(miles: Int) {
        preferencesManager.setMaintenanceMileageWindow(miles)
    }

    override suspend fun incrementAppOpenCount(): Int {
        return preferencesManager.incrementAppOpenCount()
    }

    override suspend fun setBuyMeACoffeeDontAskAgain(dontAskAgain: Boolean) {
        preferencesManager.setBuyMeACoffeeDontAskAgain(dontAskAgain)
    }

    override suspend fun setBuyMeACoffeeNextPromptOpenCount(openCount: Int) {
        preferencesManager.setBuyMeACoffeeNextPromptOpenCount(openCount)
    }

    override suspend fun setModsGridView(isGrid: Boolean) {
        preferencesManager.setModsGridView(isGrid)
    }
}
