package com.normola.barodroid.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ZambrettiTest {

    private val january = 1
    private val july = 7

    @Test
    fun `high and rising is settled`() {
        val forecast = Zambretti.forecast(1032.0, PressureTrend.Direction.RISING, january)
        assertEquals("Settled fine", forecast.text)
    }

    @Test
    fun `low and falling is stormy`() {
        val forecast = Zambretti.forecast(962.0, PressureTrend.Direction.FALLING, january)
        assertTrue(forecast.text.startsWith("Stormy"))
    }

    @Test
    fun `falling is never a better outlook than rising at the same pressure`() {
        for (pressure in 950..1050 step 2) {
            val rising = Zambretti.forecast(pressure.toDouble(), PressureTrend.Direction.RISING, january)
            val steady = Zambretti.forecast(pressure.toDouble(), PressureTrend.Direction.STEADY, january)
            val falling = Zambretti.forecast(pressure.toDouble(), PressureTrend.Direction.FALLING, january)
            assertTrue(
                "rising should be at least as good as steady at $pressure hPa",
                rising.index <= steady.index,
            )
            assertTrue(
                "steady should be at least as good as falling at $pressure hPa",
                steady.index <= falling.index,
            )
        }
    }

    @Test
    fun `the outlook improves monotonically with pressure`() {
        for (direction in PressureTrend.Direction.entries) {
            var previous = Int.MAX_VALUE
            for (pressure in 950..1050) {
                val index = Zambretti.forecast(pressure.toDouble(), direction, january).index
                assertTrue("$direction at $pressure hPa got worse as pressure rose", index <= previous)
                previous = index
            }
        }
    }

    @Test
    fun `out of range pressures are clamped rather than crashing`() {
        Zambretti.forecast(700.0, PressureTrend.Direction.FALLING, july)
        Zambretti.forecast(1200.0, PressureTrend.Direction.RISING, july)
    }

    @Test
    fun `the seasonal correction only applies in local summer`() {
        val winter = Zambretti.forecast(1000.0, PressureTrend.Direction.RISING, january)
        val summer = Zambretti.forecast(1000.0, PressureTrend.Direction.RISING, july)
        assertTrue("summer rising should be no worse than winter", summer.index <= winter.index)
    }

    @Test
    fun `southern hemisphere seasons are shifted by six months`() {
        val northernJuly = Zambretti.forecast(1000.0, PressureTrend.Direction.RISING, july, Zambretti.Hemisphere.NORTHERN)
        val southernJanuary = Zambretti.forecast(1000.0, PressureTrend.Direction.RISING, january, Zambretti.Hemisphere.SOUTHERN)
        assertEquals(northernJuly.index, southernJanuary.index)
    }
}
