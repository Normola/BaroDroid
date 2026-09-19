package com.normola.barodroid.domain

import com.normola.barodroid.core.PressureSample
import com.normola.barodroid.core.PressureTrend
import com.normola.barodroid.data.BaroSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BarometerSnapshotTest {

    private val now = 1_700_000_000_000L
    private val minute = 60_000L

    private fun history(vararg pairs: Pair<Long, Double>) =
        pairs.map { (minutesAgo, hPa) -> PressureSample(now - minutesAgo * minute, hPa) }

    @Test
    fun `no samples and no live reading gives the empty snapshot`() {
        val snapshot = BarometerSnapshot.build(emptyList(), null, BaroSettings(), now)
        assertFalse(snapshot.hasReading)
        assertNull(snapshot.displayHpa)
        assertNull(snapshot.forecast)
    }

    @Test
    fun `the live reading wins over the newest stored sample`() {
        val snapshot = BarometerSnapshot.build(
            history(30L to 1000.0),
            liveStationHpa = 1005.0,
            settings = BaroSettings(),
            now = now,
        )
        assertEquals(1005.0, snapshot.stationHpa!!, 1e-9)
        assertEquals(now, snapshot.updatedAt)
    }

    @Test
    fun `sea level correction is applied to the reading and the whole history`() {
        val settings = BaroSettings(seaLevelCorrection = true, altitudeMetres = 100.0)
        val snapshot = BarometerSnapshot.build(history(60L to 1000.0, 0L to 1000.0), null, settings, now)
        assertTrue(snapshot.displayHpa!! > 1011.0)
        assertTrue(snapshot.samples.all { it.hPa > 1011.0 })
    }

    @Test
    fun `without the correction the reading is left as the sensor gave it`() {
        val settings = BaroSettings(seaLevelCorrection = false, altitudeMetres = 100.0)
        val snapshot = BarometerSnapshot.build(history(0L to 1000.0), null, settings, now)
        assertEquals(1000.0, snapshot.displayHpa!!, 1e-9)
        // The forecast still works off sea level, whatever is on screen.
        assertTrue(snapshot.seaLevelHpa!! > 1011.0)
    }

    @Test
    fun `a forecast appears once there is enough history`() {
        val snapshot = BarometerSnapshot.build(
            history(180L to 1002.0, 120L to 1004.0, 60L to 1006.0, 0L to 1008.0),
            null,
            BaroSettings(),
            now,
            month = 1,
        )
        assertTrue(snapshot.trend.isReliable)
        assertEquals(PressureTrend.RISING_RAPIDLY, snapshot.trendKind)
        assertNotNull(snapshot.forecast)
        assertNotNull(snapshot.referenceHpa)
    }

    @Test
    fun `a single sample is not enough for a forecast or a set-hand`() {
        val snapshot = BarometerSnapshot.build(history(0L to 1013.0), null, BaroSettings(), now)
        assertTrue(snapshot.hasReading)
        assertFalse(snapshot.trend.isReliable)
        assertNull(snapshot.forecast)
        assertNull(snapshot.referenceHpa)
    }
}
