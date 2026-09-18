package com.normola.barodroid.core

/**
 * Geometry and lettering of the barometer face.
 *
 * The scale runs 950–1050 hPa (28.05–31.00 inHg) over a 270° sweep with the gap
 * at the bottom, which is how a traditional aneroid dial is laid out. Angles use
 * the Android canvas convention: 0° points right (3 o'clock) and grows clockwise.
 */
object DialScale {

    const val MIN_HPA = 950.0
    const val MAX_HPA = 1050.0

    /** 7:30 position — the left edge of the classic gap. */
    const val START_ANGLE = 135f
    const val SWEEP_ANGLE = 270f

    /** Where the tick labels change from 10 hPa steps: every 10 hPa is labelled. */
    const val MAJOR_STEP = 10.0
    const val MEDIUM_STEP = 5.0
    const val MINOR_STEP = 1.0

    /** The traditional weather-glass lettering, in pressure order. */
    enum class Zone(val label: String, val startHpa: Double, val endHpa: Double) {
        STORMY("STORMY", 950.0, 965.0),
        RAIN("RAIN", 965.0, 990.0),
        CHANGE("CHANGE", 990.0, 1010.0),
        FAIR("FAIR", 1010.0, 1030.0),
        VERY_DRY("VERY DRY", 1030.0, 1050.0),
    }

    fun clamp(hPa: Double): Double = hPa.coerceIn(MIN_HPA, MAX_HPA)

    /** 0f at the low end of the scale, 1f at the high end. */
    fun fractionFor(hPa: Double): Float =
        ((clamp(hPa) - MIN_HPA) / (MAX_HPA - MIN_HPA)).toFloat()

    /** Canvas angle in degrees for a pressure reading. */
    fun angleFor(hPa: Double): Float = START_ANGLE + SWEEP_ANGLE * fractionFor(hPa)

    fun zoneFor(hPa: Double): Zone {
        val value = clamp(hPa)
        return Zone.entries.lastOrNull { value >= it.startHpa } ?: Zone.STORMY
    }
}
