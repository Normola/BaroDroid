package com.normola.barodroid.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PressureMathTest {

    @Test
    fun `sea level correction is identity at sea level`() {
        assertEquals(1013.25, PressureMath.toSeaLevel(1013.25, 0.0), 1e-9)
    }

    @Test
    fun `sea level correction adds roughly 12 hPa per 100 m`() {
        val corrected = PressureMath.toSeaLevel(1000.0, 100.0)
        assertEquals(1012.0, corrected, 0.5)
    }

    @Test
    fun `station and sea level conversions round trip`() {
        val station = 943.2
        val sea = PressureMath.toSeaLevel(station, 600.0)
        assertEquals(station, PressureMath.toStation(sea, 600.0), 1e-6)
    }

    @Test
    fun `altitude is recovered from a standard atmosphere reading`() {
        val station = PressureMath.toStation(PressureMath.STANDARD_SEA_LEVEL_HPA, 500.0)
        assertEquals(500.0, PressureMath.altitudeFor(station), 1.0)
    }

    @Test
    fun `plausibility rejects nonsense readings`() {
        assertTrue(PressureMath.isPlausible(1013.0))
        assertFalse(PressureMath.isPlausible(0.0))
        assertFalse(PressureMath.isPlausible(Double.NaN))
        assertFalse(PressureMath.isPlausible(5000.0))
    }
}
