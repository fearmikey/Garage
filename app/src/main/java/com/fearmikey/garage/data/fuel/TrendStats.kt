package com.fearmikey.garage.data.fuel

import kotlin.math.abs

/** Pure helpers for trend charts: smoothing and outlier detection. */
object TrendStats {

    /** Minimum number of samples before outlier detection is attempted. */
    const val MIN_SAMPLES_FOR_OUTLIERS = 4

    /**
     * Trailing rolling average with the given [window]. The first `window - 1` values
     * average over however many samples are available, so the result has the same size as [values].
     */
    fun rollingAverage(values: List<Double>, window: Int = 3): List<Double> {
        require(window >= 1) { "window must be >= 1" }
        return values.indices.map { i ->
            val from = (i - window + 1).coerceAtLeast(0)
            values.subList(from, i + 1).average()
        }
    }

    /**
     * Indices of values that sit far from the norm, using the modified z-score
     * (median absolute deviation), which is robust to the outliers themselves.
     *
     * A value is flagged only if its modified z-score exceeds [zThreshold] **and** it differs
     * from the median by more than [minRelativeDeviation], so tight, consistent data doesn't
     * produce noisy flags for trivially small differences.
     */
    fun outlierIndices(
        values: List<Double>,
        zThreshold: Double = 3.5,
        minRelativeDeviation: Double = 0.50,
    ): Set<Int> {
        if (values.size < MIN_SAMPLES_FOR_OUTLIERS) return emptySet()
        val median = median(values)
        if (median == 0.0) return emptySet()
        val mad = median(values.map { abs(it - median) })

        return values.indices.filter { i ->
            val deviation = abs(values[i] - median)
            val relative = deviation / abs(median)
            if (relative <= minRelativeDeviation) return@filter false
            if (mad < 1e-9) {
                // Almost every value is identical; anything meaningfully different stands out.
                true
            } else {
                (0.6745 * deviation / mad) > zThreshold
            }
        }.toSet()
    }

    fun median(values: List<Double>): Double {
        require(values.isNotEmpty()) { "values must not be empty" }
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) (sorted[mid - 1] + sorted[mid]) / 2.0 else sorted[mid]
    }
}
