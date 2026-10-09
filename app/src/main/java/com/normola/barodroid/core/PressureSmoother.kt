package com.normola.barodroid.core

import kotlin.math.abs

/**
 * Takes the jitter out of a stream of sensor readings, and — just as important
 * for battery life — says when a reading is not worth acting on.
 *
 * Phone barometers wobble by a few hundredths of a hectopascal from sample to
 * sample. Pushed straight at the UI that wobble re-animates the needle
 * continuously; pushed at the history store it causes pointless writes. An
 * exponential average plus a deadband means the dial only moves when the weather
 * does.
 *
 * Not thread safe: keep one per consumer.
 */
class PressureSmoother(
    /** Weight given to each new sample; lower is smoother but slower to settle. */
    private val smoothing: Double = 0.25,
    /** How far the average has to move before it is worth redrawing, in hPa. */
    private val deadbandHpa: Double = 0.03,
) {
    private var average: Double? = null
    private var published: Double? = null

    /** The value last considered worth showing, if any. */
    val current: Double? get() = published

    /**
     * Feeds in a raw reading.
     *
     * @return the value to show when it has moved far enough to matter, or null
     *   when the reading is indistinguishable from what is already on screen.
     */
    fun offer(readingHpa: Double): Double? {
        if (!PressureMath.isPlausible(readingHpa)) return null

        val previous = average
        val updated = if (previous == null) readingHpa else previous + smoothing * (readingHpa - previous)
        average = updated

        val shown = published
        if (shown == null || abs(updated - shown) >= deadbandHpa) {
            published = updated
            return updated
        }
        return null
    }

    fun reset() {
        average = null
        published = null
    }
}
