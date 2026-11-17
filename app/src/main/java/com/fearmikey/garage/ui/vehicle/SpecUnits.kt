package com.fearmikey.garage.ui.vehicle

import com.fearmikey.garage.ui.util.UnitSystem
import com.fearmikey.garage.ui.util.parseToDoubleOrNull
import java.math.BigDecimal
import kotlin.math.roundToLong

/**
 * Unit handling for the free-text vehicle spec fields. Specs are stored as text; a bare number is
 * interpreted in the user's unit system (weights) or the vehicle type (displacement), while text
 * that already carries a unit (e.g. NHTSA's "4480 lbs") is shown exactly as stored.
 *
 * Displacement is always stored in litres (what NHTSA returns and what PartsEstimator expects);
 * motorcycles just show and edit it as cc.
 */
object SpecUnits {

    /** Bare numbers at or above this are already cc on a motorcycle; below it they're litres. */
    private const val CC_THRESHOLD = 20.0

    /** True for a bare number, accepting both "1.6" and "1,6" decimal separators. */
    fun isBareNumber(value: String): Boolean = value.trim().parseToDoubleOrNull() != null

    /** Appends [unit] only to a bare number (so "1.8L" doesn't become "1.8L L"). */
    fun withUnit(value: String, unit: String): String =
        if (isBareNumber(value)) "${value.trim()} $unit" else value

    fun weightUnit(unitSystem: UnitSystem): String =
        if (unitSystem == UnitSystem.IMPERIAL) "lbs" else "kg"

    fun formatWeight(value: String, unitSystem: UnitSystem): String {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return value
        return withUnit(trimmed, weightUnit(unitSystem))
    }

    fun displacementUnit(isMotorcycle: Boolean): String = if (isMotorcycle) "cc" else "L"

    /** Stored litres -> display text: "1,6 L" for cars, "452 cc" for motorcycles. */
    fun formatDisplacement(stored: String, isMotorcycle: Boolean): String {
        if (!isBareNumber(stored)) return stored
        if (!isMotorcycle) return withUnit(stored, "L")
        return "${toCc(stored.trim().parseToDoubleOrNull()!!)} cc"
    }

    /** Stored litres -> text for the edit field (cc number for motorcycles). */
    fun displacementForInput(stored: String?, isMotorcycle: Boolean): String {
        if (stored.isNullOrBlank() || !isMotorcycle) return stored.orEmpty()
        val number = stored.trim().parseToDoubleOrNull() ?: return stored
        return toCc(number).toString()
    }

    /** Edit-field text -> stored litres (motorcycle cc is divided by 1000). */
    fun displacementFromInput(input: String, isMotorcycle: Boolean): String {
        if (!isMotorcycle) return input
        val number = input.trim().parseToDoubleOrNull() ?: return input
        if (number < CC_THRESHOLD) return input
        return BigDecimal(number).divide(BigDecimal(1000)).stripTrailingZeros().toPlainString()
    }

    private fun toCc(value: Double): Long =
        if (value < CC_THRESHOLD) (value * 1000).roundToLong() else value.roundToLong()
}
