package com.normola.barodroid.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PressureSmootherTest {

    @Test
    fun `the first reading is always published`() {
        val smoother = PressureSmoother()
        assertEquals(1013.0, smoother.offer(1013.0)!!, 1e-9)
    }

    @Test
    fun `sensor jitter is swallowed`() {
        val smoother = PressureSmoother()
        smoother.offer(1013.00)
        val jitter = listOf(1013.01, 1012.99, 1013.02, 1012.98, 1013.01, 1013.00)
        for (reading in jitter) {
            assertNull("jitter of ${reading - 1013.0} hPa should not redraw", smoother.offer(reading))
        }
    }

    @Test
    fun `a real change gets through`() {
        val smoother = PressureSmoother()
        smoother.offer(1013.0)
        // A front moving in: repeated readings half a hectopascal lower.
        var published: Double? = null
        repeat(5) { published = smoother.offer(1012.5) ?: published }
        assertNotNull(published)
        assertTrue("expected the needle to move down, got $published", published!! < 1013.0)
    }

    @Test
    fun `the average converges on a sustained reading`() {
        val smoother = PressureSmoother(smoothing = 0.5)
        smoother.offer(1000.0)
        repeat(40) { smoother.offer(1005.0) }
        assertEquals(1005.0, smoother.current!!, 0.01)
    }

    @Test
    fun `implausible readings are ignored and do not disturb the average`() {
        val smoother = PressureSmoother()
        smoother.offer(1013.0)
        assertNull(smoother.offer(0.0))
        assertNull(smoother.offer(Double.NaN))
        assertEquals(1013.0, smoother.current!!, 1e-9)
    }

    @Test
    fun `a reset starts over`() {
        val smoother = PressureSmoother()
        smoother.offer(1013.0)
        smoother.reset()
        assertNull(smoother.current)
        assertEquals(990.0, smoother.offer(990.0)!!, 1e-9)
    }

    @Test
    fun `a steady signal redraws once, not continuously`() {
        val smoother = PressureSmoother()
        var redraws = 0
        repeat(600) { index ->
            // Ten minutes of once-a-second readings wobbling within the deadband.
            val noise = if (index % 2 == 0) 0.012 else -0.012
            if (smoother.offer(1008.4 + noise) != null) redraws++
        }
        assertEquals(1, redraws)
    }
}
