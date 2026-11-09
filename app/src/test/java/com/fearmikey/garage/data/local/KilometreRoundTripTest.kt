package com.fearmikey.garage.data.local

import com.fearmikey.garage.data.local.entity.FuelRecord
import com.fearmikey.garage.data.remote.lubelogger.formatSyncMiles
import com.fearmikey.garage.data.remote.lubelogger.fromLubeLoggerOdometer
import com.fearmikey.garage.data.remote.lubelogger.syncFingerprint
import com.fearmikey.garage.data.remote.lubelogger.toLubeLoggerOdometer
import com.fearmikey.garage.ui.util.UnitConverter
import com.fearmikey.garage.ui.util.UnitSystem
import org.junit.Assert.assertEquals
import org.junit.Test

/** Kilometre input must survive storage (fractional canonical miles) and LubeLogger sync unchanged. */
class KilometreRoundTripTest {

    @Test
    fun everyWholeKmValueRoundTripsThroughCanonicalMiles() {
        for (km in 0..250_000) {
            val stored = UnitConverter.canonicalMilesFromInput(km, UnitSystem.METRIC)
            assertEquals("km=$km", km, UnitConverter.displayDistanceValue(stored, UnitSystem.METRIC))
        }
    }

    @Test
    fun reportedExampleNoLongerLosesAKilometre() {
        // Whole-mile storage turned 3599 km into 2236 mi, which displayed as 3598 km.
        val stored = UnitConverter.canonicalMilesFromInput(3599, UnitSystem.METRIC)
        assertEquals(3599, UnitConverter.displayDistanceValue(stored, UnitSystem.METRIC))
        assertEquals("3,599 km", UnitConverter.formatDistance(stored, UnitSystem.METRIC))
    }

    @Test
    fun metricLubeLoggerOdometerRoundTripsExactly() {
        for (km in 0..250_000 step 7) {
            assertEquals(km, toLubeLoggerOdometer(fromLubeLoggerOdometer(km, isMetric = true), isMetric = true))
        }
    }

    @Test
    fun imperialFingerprintKeepsLegacyWholeMileFormat() {
        // Hashes stored by pre-v34 builds used "$mileage" on an Int; whole values must match.
        val fp = fuel(2236.0).syncFingerprint("imperial")
        assertEquals("2236", fp.split("|")[1])
    }

    @Test
    fun crossUnitRoundTripDoesNotLookLikeAnEdit() {
        // Garage in miles, LubeLogger in km: 2236 mi -> 3598 km -> 2235.69 mi must fingerprint the same.
        val local = fuel(2236.0)
        val pulledBack = fuel(fromLubeLoggerOdometer(toLubeLoggerOdometer(local.mileage, true), true))
        assertEquals(local.syncFingerprint("metric"), pulledBack.syncFingerprint("metric"))
    }

    @Test
    fun syncMilesFormatIsStableForWholeAndFractionalValues() {
        assertEquals("2236", formatSyncMiles(2236.0))
        val km = 3599 / UnitConverter.KM_PER_MILE
        assertEquals(km, formatSyncMiles(km).toDouble(), 0.0)
    }

    private fun fuel(mileage: Double) = FuelRecord(
        vehicleId = 1,
        date = 0L,
        mileage = mileage,
        gallons = 10.0,
        totalCost = 30.0,
        pricePerGallon = 3.0,
    )
}
