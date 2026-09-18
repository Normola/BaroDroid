package com.normola.barodroid.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrendCalculatorTest {

    private val now = 1_700_000_000_000L
    private val minute = 60_000L

    private fun samples(vararg pairs: Pair<Long, Double>): List<PressureSample> =
        pairs.map { (minutesAgo, hPa) -> PressureSample(now - minutesAgo * minute, hPa) }

    @Test
    fun `too little history is not reliable`() {
        val result = TrendCalculator.calculate(samples(0L to 1013.0), now)
        assertFalse(result.isReliable)
        assertEquals(PressureTrend.STEADY, result.trend)
    }

    @Test
    fun `a short span is reported but flagged unreliable`() {
        val result = TrendCalculator.calculate(samples(5L to 1010.0, 0L to 1011.0), now)
        assertFalse(result.isReliable)
        assertEquals(PressureTrend.STEADY, result.trend)
    }

    @Test
    fun `a steady three hours reads as steady`() {
        val result = TrendCalculator.calculate(
            samples(180L to 1013.0, 120L to 1013.2, 60L to 1012.9, 0L to 1013.1),
            now,
        )
        assertTrue(result.isReliable)
        assertEquals(PressureTrend.STEADY, result.trend)
        assertEquals(0.0, result.deltaPer3h, 0.5)
    }

    @Test
    fun `a three hPa rise over three hours reads as rising`() {
        val result = TrendCalculator.calculate(
            samples(180L to 1000.0, 120L to 1001.0, 60L to 1002.0, 0L to 1003.0),
            now,
        )
        assertTrue(result.isReliable)
        assertEquals(3.0, result.deltaPer3h, 0.1)
        assertEquals(PressureTrend.RISING, result.trend)
        assertEquals(1000.0, result.referenceHpa!!, 0.1)
    }

    @Test
    fun `a steep fall reads as falling rapidly`() {
        val result = TrendCalculator.calculate(
            samples(180L to 1005.0, 90L to 1001.0, 0L to 997.0),
            now,
        )
        assertEquals(PressureTrend.FALLING_RAPIDLY, result.trend)
        assertTrue(result.deltaPer3h < -6.0)
    }

    @Test
    fun `the slope is scaled to three hours even from a shorter window`() {
        // 1 hPa over one hour is 3 hPa over three hours.
        val result = TrendCalculator.calculate(
            samples(60L to 1010.0, 30L to 1010.5, 0L to 1011.0),
            now,
        )
        assertEquals(3.0, result.deltaPer3h, 0.1)
    }

    @Test
    fun `samples outside the window are ignored`() {
        val stale = PressureSample(now - 10 * 60 * minute, 900.0)
        val result = TrendCalculator.calculate(
            listOf(stale) + samples(120L to 1012.0, 0L to 1012.2),
            now,
        )
        assertEquals(PressureTrend.STEADY, result.trend)
    }

    @Test
    fun `noise does not swamp the fitted slope`() {
        val jitter = listOf(0.1, -0.1, 0.15, -0.05, 0.0, -0.12, 0.08)
        val list = jitter.mapIndexed { index, noise ->
            val minutesAgo = 180L - index * 30L
            PressureSample(now - minutesAgo * minute, 1000.0 + index * 0.5 + noise)
        }
        val result = TrendCalculator.calculate(list, now)
        assertEquals(3.0, result.deltaPer3h, 0.4)
        assertEquals(PressureTrend.RISING, result.trend)
    }
}
