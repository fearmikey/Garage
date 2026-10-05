package com.fearmikey.garage.data.fuel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrendStatsTest {

    @Test
    fun `rollingAverage uses partial window at the start`() {
        val result = TrendStats.rollingAverage(listOf(10.0, 20.0, 30.0, 40.0), window = 3)
        assertEquals(listOf(10.0, 15.0, 20.0, 30.0), result)
    }

    @Test
    fun `rollingAverage with window 1 is identity`() {
        val values = listOf(1.0, 5.0, 3.0)
        assertEquals(values, TrendStats.rollingAverage(values, window = 1))
    }

    @Test
    fun `rollingAverage of empty list is empty`() {
        assertTrue(TrendStats.rollingAverage(emptyList()).isEmpty())
    }

    @Test
    fun `outlierIndices flags a single bad reading`() {
        val values = listOf(24.1, 25.3, 23.8, 26.0, 10.0, 25.1, 24.7, 26.4)
        assertEquals(setOf(4), TrendStats.outlierIndices(values))
    }

    @Test
    fun `outlierIndices flags high and low extremes`() {
        val values = listOf(25.0, 24.5, 55.0, 25.5, 24.8, 9.0, 25.2)
        assertEquals(setOf(2, 5), TrendStats.outlierIndices(values))
    }

    @Test
    fun `outlierIndices ignores normal variation`() {
        val values = listOf(22.0, 24.0, 26.0, 23.0, 25.0, 27.0, 21.0)
        assertTrue(TrendStats.outlierIndices(values).isEmpty())
    }

    @Test
    fun `outlierIndices needs a minimum sample size`() {
        assertTrue(TrendStats.outlierIndices(listOf(25.0, 25.0, 60.0)).isEmpty())
    }

    @Test
    fun `outlierIndices handles identical values with one deviation`() {
        val values = listOf(30.0, 30.0, 30.0, 30.0, 50.0)
        assertEquals(setOf(4), TrendStats.outlierIndices(values))
    }

    @Test
    fun `outlierIndices ignores tiny deviations when data is identical`() {
        val values = listOf(30.0, 30.0, 30.0, 30.0, 31.0)
        assertTrue(TrendStats.outlierIndices(values).isEmpty())
    }

    @Test
    fun `median handles odd and even sizes`() {
        assertEquals(3.0, TrendStats.median(listOf(5.0, 1.0, 3.0)), 0.0)
        assertEquals(2.5, TrendStats.median(listOf(4.0, 1.0, 3.0, 2.0)), 0.0)
    }
}
