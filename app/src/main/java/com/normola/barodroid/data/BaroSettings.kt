package com.normola.barodroid.data

import com.normola.barodroid.core.PressureUnit
import com.normola.barodroid.core.Zambretti

/** Everything the user can change, in one immutable snapshot. */
data class BaroSettings(
    val unit: PressureUnit = PressureUnit.HECTOPASCAL,
    /** Reduce the sensor reading to sea level before showing it. */
    val seaLevelCorrection: Boolean = false,
    val altitudeMetres: Double = 0.0,
    val hemisphere: Zambretti.Hemisphere = Zambretti.Hemisphere.NORTHERN,
    /** How often the background worker samples the sensor, in minutes. */
    val sampleIntervalMinutes: Int = 15,
    val dynamicColor: Boolean = true,
    /**
     * Keep a foreground service running so readings continue while the app is
     * closed. Android restricts background access to continuous sensors, so this
     * is the only reliable way to keep the graph and the widgets fed.
     */
    val backgroundLogging: Boolean = false,
) {
    companion object {
        val SAMPLE_INTERVAL_OPTIONS = listOf(15, 30, 60)
    }
}
