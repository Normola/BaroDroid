package com.normola.barodroid.core

import org.junit.Assert.assertEquals
import org.junit.Test

class PressureUnitTest {

    @Test
    fun `standard atmosphere converts to the familiar values`() {
        assertEquals(29.92, PressureUnit.INCHES_HG.fromHpa(1013.25), 0.005)
        assertEquals(760.0, PressureUnit.MILLIMETRES_HG.fromHpa(1013.25), 0.1)
        assertEquals(101.325, PressureUnit.KILOPASCAL.fromHpa(1013.25), 1e-6)
    }

    @Test
    fun `conversions round trip`() {
        for (unit in PressureUnit.entries) {
            assertEquals(998.7, unit.toHpa(unit.fromHpa(998.7)), 1e-6)
        }
    }

    @Test
    fun `formatting honours each unit's precision`() {
        assertEquals("1013.2", PressureUnit.HECTOPASCAL.format(1013.24))
        assertEquals("29.92", PressureUnit.INCHES_HG.format(1013.25))
        assertEquals("760", PressureUnit.MILLIMETRES_HG.format(1013.25))
        assertEquals("1013.2 hPa", PressureUnit.HECTOPASCAL.formatWithSymbol(1013.24))
    }

    @Test
    fun `deltas are always signed`() {
        assertEquals("+1.4", PressureUnit.HECTOPASCAL.formatDelta(1.44))
        assertEquals("-1.4", PressureUnit.HECTOPASCAL.formatDelta(-1.44))
        assertEquals("+0.0", PressureUnit.HECTOPASCAL.formatDelta(0.01))
        assertEquals("-0.06", PressureUnit.INCHES_HG.formatDelta(-2.0))
    }

    @Test
    fun `unknown unit ids fall back to hPa`() {
        assertEquals(PressureUnit.HECTOPASCAL, PressureUnit.fromId(null))
        assertEquals(PressureUnit.HECTOPASCAL, PressureUnit.fromId("furlongs"))
        assertEquals(PressureUnit.INCHES_HG, PressureUnit.fromId("inhg"))
    }
}
