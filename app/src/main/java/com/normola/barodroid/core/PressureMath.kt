package com.normola.barodroid.core

import kotlin.math.pow

/** Physical conversions around station pressure. */
object PressureMath {

    /** Standard atmosphere at sea level. */
    const val STANDARD_SEA_LEVEL_HPA = 1013.25

    /** Sensible bounds for anything we accept from the sensor or draw on the dial. */
    const val MIN_PLAUSIBLE_HPA = 800.0
    const val MAX_PLAUSIBLE_HPA = 1100.0

    /**
     * Converts a station (absolute) reading to the equivalent sea-level pressure
     * using the barometric formula, which is what weather reports and the classic
     * barometer scale are calibrated against.
     *
     * @param stationHpa reading straight from the sensor
     * @param altitudeMetres height of the sensor above sea level
     * @param temperatureC ambient temperature; 15 °C (ISA) is a fine default
     */
    fun toSeaLevel(
        stationHpa: Double,
        altitudeMetres: Double,
        temperatureC: Double = 15.0,
    ): Double {
        if (altitudeMetres == 0.0) return stationHpa
        val tempK = temperatureC + 273.15
        val factor = 1.0 - (0.0065 * altitudeMetres) / (tempK + 0.0065 * altitudeMetres)
        return stationHpa * factor.pow(-5.257)
    }

    /** Inverse of [toSeaLevel]: what the sensor should read at [altitudeMetres]. */
    fun toStation(
        seaLevelHpa: Double,
        altitudeMetres: Double,
        temperatureC: Double = 15.0,
    ): Double {
        if (altitudeMetres == 0.0) return seaLevelHpa
        val tempK = temperatureC + 273.15
        val factor = 1.0 - (0.0065 * altitudeMetres) / (tempK + 0.0065 * altitudeMetres)
        return seaLevelHpa * factor.pow(5.257)
    }

    /**
     * Approximate height above sea level implied by a station reading, assuming
     * [seaLevelHpa] at sea level. This is the exact inverse of [toStation], so
     * the "calibrate from the current sea-level pressure" helper in settings and
     * the correction applied to readings always agree.
     */
    fun altitudeFor(
        stationHpa: Double,
        seaLevelHpa: Double = STANDARD_SEA_LEVEL_HPA,
        temperatureC: Double = 15.0,
    ): Double {
        val ratio = (stationHpa / seaLevelHpa).pow(1.0 / 5.257)
        if (ratio <= 0.0) return 0.0
        val tempK = temperatureC + 273.15
        return tempK * (1.0 - ratio) / (0.0065 * ratio)
    }

    fun isPlausible(hPa: Double): Boolean =
        !hPa.isNaN() && hPa in MIN_PLAUSIBLE_HPA..MAX_PLAUSIBLE_HPA
}
