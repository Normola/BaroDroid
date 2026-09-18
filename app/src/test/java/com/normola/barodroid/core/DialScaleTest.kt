package com.normola.barodroid.core

import org.junit.Assert.assertEquals
import org.junit.Test

class DialScaleTest {

    @Test
    fun `the scale ends line up with the sweep`() {
        assertEquals(DialScale.START_ANGLE, DialScale.angleFor(950.0), 1e-4f)
        assertEquals(DialScale.START_ANGLE + DialScale.SWEEP_ANGLE, DialScale.angleFor(1050.0), 1e-4f)
    }

    @Test
    fun `the midpoint of the scale points straight up`() {
        // 135 + 135 = 270 degrees in canvas terms, i.e. 12 o'clock.
        assertEquals(270f, DialScale.angleFor(1000.0), 1e-4f)
    }

    @Test
    fun `values beyond the scale are clamped onto it`() {
        assertEquals(0f, DialScale.fractionFor(800.0), 1e-4f)
        assertEquals(1f, DialScale.fractionFor(1100.0), 1e-4f)
    }

    @Test
    fun `zones follow the traditional lettering`() {
        assertEquals(DialScale.Zone.STORMY, DialScale.zoneFor(955.0))
        assertEquals(DialScale.Zone.RAIN, DialScale.zoneFor(980.0))
        assertEquals(DialScale.Zone.CHANGE, DialScale.zoneFor(1000.0))
        assertEquals(DialScale.Zone.FAIR, DialScale.zoneFor(1020.0))
        assertEquals(DialScale.Zone.VERY_DRY, DialScale.zoneFor(1040.0))
    }

    @Test
    fun `zone boundaries are contiguous and cover the dial`() {
        val zones = DialScale.Zone.entries
        assertEquals(DialScale.MIN_HPA, zones.first().startHpa, 1e-9)
        assertEquals(DialScale.MAX_HPA, zones.last().endHpa, 1e-9)
        zones.zipWithNext { a, b -> assertEquals(a.endHpa, b.startHpa, 1e-9) }
    }
}
