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
    val maintenanceDaysWindow: Flow<Int>
        get() = flowOf(10)
    val appOpenCount: Flow<Int>
    val buyMeACoffeeDontAskAgain: Flow<Boolean>
    val buyMeACoffeeNextPromptOpenCount: Flow<Int>
    val isModsGridView: Flow<Boolean>
        get() = flowOf(false)
    val includeModsInCost: Flow<Boolean>
        get() = flowOf(false)
    val affiliateLinksEnabled: Flow<Boolean>
        get() = flowOf(true)
    val showFuelTrendGraph: Flow<Boolean>
        get() = flowOf(true)
    val showFleetOverview: Flow<Boolean>
        get() = flowOf(false)
    val documentExpirationRemindersEnabled: Flow<Boolean>
        get() = flowOf(true)
    val documentExpirationDaysWindow: Flow<Int>
        get() = flowOf(30)
    val driversLicenseNumber: Flow<String?>
        get() = flowOf(null)
    val driversLicenseState: Flow<String?>
        get() = flowOf(null)
    val driversLicenseExpiration: Flow<Long?>
        get() = flowOf(null)
    val driversLicenseNotes: Flow<String?>
        get() = flowOf(null)
    val driversLicenseImageFront: Flow<String?>
        get() = flowOf(null)
    val driversLicenseImageBack: Flow<String?>
        get() = flowOf(null)

    suspend fun setUnitsType(units: String)
    suspend fun setCurrencyCode(currencyCode: String)
    suspend fun setThemeType(theme: String)
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun setDefaultVehicleId(vehicleId: Long?)
    suspend fun setMaintenanceMileageWindow(miles: Int)
    suspend fun setMaintenanceDaysWindow(days: Int) {}
    suspend fun incrementAppOpenCount(): Int
    suspend fun setBuyMeACoffeeDontAskAgain(dontAskAgain: Boolean)
    suspend fun setBuyMeACoffeeNextPromptOpenCount(openCount: Int)
    suspend fun setModsGridView(isGrid: Boolean) {}
    suspend fun setIncludeModsInCost(includeMods: Boolean) {}
    suspend fun setAffiliateLinksEnabled(enabled: Boolean) {}
    suspend fun setShowFuelTrendGraph(enabled: Boolean) {}
    suspend fun setShowFleetOverview(enabled: Boolean) {}
    suspend fun setDocumentExpirationRemindersEnabled(enabled: Boolean) {}
    suspend fun setDocumentExpirationDaysWindow(days: Int) {}
    suspend fun setDriversLicense(
        number: String?,
        state: String?,
        expiration: Long?,
        notes: String?,
        imageFront: String?,
        imageBack: String?,
    ) {}
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
    override val maintenanceDaysWindow: Flow<Int> = preferencesManager.maintenanceDaysWindow
    override val appOpenCount: Flow<Int> = preferencesManager.appOpenCount
    override val buyMeACoffeeDontAskAgain: Flow<Boolean> = preferencesManager.buyMeACoffeeDontAskAgain
    override val buyMeACoffeeNextPromptOpenCount: Flow<Int> = preferencesManager.buyMeACoffeeNextPromptOpenCount
    override val isModsGridView: Flow<Boolean> = preferencesManager.isModsGridView
    override val includeModsInCost: Flow<Boolean> = preferencesManager.includeModsInCost
    override val affiliateLinksEnabled: Flow<Boolean> = preferencesManager.affiliateLinksEnabled
    override val showFuelTrendGraph: Flow<Boolean> = preferencesManager.showFuelTrendGraph
    override val showFleetOverview: Flow<Boolean> = preferencesManager.showFleetOverview
    override val documentExpirationRemindersEnabled: Flow<Boolean> = preferencesManager.documentExpirationRemindersEnabled
    override val documentExpirationDaysWindow: Flow<Int> = preferencesManager.documentExpirationDaysWindow
    override val driversLicenseNumber: Flow<String?> = preferencesManager.driversLicenseNumber
    override val driversLicenseState: Flow<String?> = preferencesManager.driversLicenseState
    override val driversLicenseExpiration: Flow<Long?> = preferencesManager.driversLicenseExpiration
    override val driversLicenseNotes: Flow<String?> = preferencesManager.driversLicenseNotes
    override val driversLicenseImageFront: Flow<String?> = preferencesManager.driversLicenseImageFront
    override val driversLicenseImageBack: Flow<String?> = preferencesManager.driversLicenseImageBack

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

    override suspend fun setMaintenanceDaysWindow(days: Int) {
        preferencesManager.setMaintenanceDaysWindow(days)
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

    override suspend fun setIncludeModsInCost(includeMods: Boolean) {
        preferencesManager.setIncludeModsInCost(includeMods)
    }

    override suspend fun setAffiliateLinksEnabled(enabled: Boolean) {
        preferencesManager.setAffiliateLinksEnabled(enabled)
    }

    override suspend fun setShowFuelTrendGraph(enabled: Boolean) {
        preferencesManager.setShowFuelTrendGraph(enabled)
    }

    override suspend fun setShowFleetOverview(enabled: Boolean) {
        preferencesManager.setShowFleetOverview(enabled)
    }

    override suspend fun setDocumentExpirationRemindersEnabled(enabled: Boolean) {
        preferencesManager.setDocumentExpirationRemindersEnabled(enabled)
    }

    override suspend fun setDocumentExpirationDaysWindow(days: Int) {
        preferencesManager.setDocumentExpirationDaysWindow(days)
    }

    override suspend fun setDriversLicense(
        number: String?,
        state: String?,
        expiration: Long?,
        notes: String?,
        imageFront: String?,
        imageBack: String?,
    ) {
        preferencesManager.setDriversLicense(number, state, expiration, notes, imageFront, imageBack)
    }
}
