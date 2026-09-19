package com.normola.barodroid.core

import kotlin.math.roundToInt

/**
 * The Zambretti forecaster — the algorithm behind the Negretti & Zambra
 * "weather forecasting slide rule" of 1915, which is exactly the kind of
 * prediction an old-school barometer face implies.
 *
 * It needs three things: sea-level pressure, which way the needle is moving,
 * and the season. Accuracy is famously around 90% for a 12-hour outlook in
 * temperate latitudes, which is far better than it has any right to be.
 */
object Zambretti {

    /** The 26 classic forecast strings, A through Z. */
    private val FORECASTS = arrayOf(
        "Settled fine",
        "Fine weather",
        "Becoming fine",
        "Fine, becoming less settled",
        "Fine, possible showers",
        "Fairly fine, improving",
        "Fairly fine, possible showers early",
        "Fairly fine, showery later",
        "Showery early, improving",
        "Changeable, mending",
        "Fairly fine, showers likely",
        "Rather unsettled, clearing later",
        "Unsettled, probably improving",
        "Showery, bright intervals",
        "Showery, becoming less settled",
        "Changeable, some rain",
        "Unsettled, short fine intervals",
        "Unsettled, rain later",
        "Unsettled, rain at times",
        "Very unsettled, finer at times",
        "Rain at times, worse later",
        "Rain at times, becoming very unsettled",
        "Rain at frequent intervals",
        "Very unsettled, rain",
        "Stormy, possibly improving",
        "Stormy, much rain",
    )

    // Lookup tables, indexed from the lowest pressure step upwards; each entry
    // is an index into FORECASTS.
    private val RISING = intArrayOf(25, 25, 25, 24, 24, 19, 16, 12, 11, 9, 8, 6, 5, 2, 1, 1, 0, 0, 0, 0, 0, 0)
    private val STEADY = intArrayOf(25, 25, 25, 25, 25, 25, 23, 23, 22, 18, 15, 13, 10, 4, 1, 1, 0, 0, 0, 0, 0, 0)
    private val FALLING = intArrayOf(25, 25, 25, 25, 25, 25, 25, 25, 23, 23, 21, 20, 17, 14, 7, 3, 1, 1, 1, 0, 0, 0)

    private const val BOTTOM_HPA = 950.0
    private const val TOP_HPA = 1050.0
    private const val STEP = (TOP_HPA - BOTTOM_HPA) / 22.0

    /** Seasonal nudge from the original algorithm, in hPa. */
    private const val SEASONAL_ADJUSTMENT = 3.2

    enum class Hemisphere(val id: String) {
        NORTHERN("north"),
        SOUTHERN("south"),
        ;

        companion object {
            fun fromId(id: String?): Hemisphere = entries.firstOrNull { it.id == id } ?: NORTHERN
        }
    }

    data class Forecast(val text: String, val index: Int)

    /**
     * @param seaLevelHpa pressure reduced to sea level
     * @param direction which way the needle is going
     * @param month 1-12, used for the seasonal correction
     * @param hemisphere shifts the seasons by six months in the south
     */
    fun forecast(
        seaLevelHpa: Double,
        direction: PressureTrend.Direction,
        month: Int,
        hemisphere: Hemisphere = Hemisphere.NORTHERN,
    ): Forecast {
        val localMonth = if (hemisphere == Hemisphere.SOUTHERN) ((month + 5) % 12) + 1 else month
        val isSummer = localMonth in 4..9

        var pressure = seaLevelHpa
        if (isSummer) {
            when (direction) {
                PressureTrend.Direction.RISING -> pressure += SEASONAL_ADJUSTMENT
                PressureTrend.Direction.FALLING -> pressure -= SEASONAL_ADJUSTMENT
                PressureTrend.Direction.STEADY -> Unit
            }
        }

        val table = when (direction) {
            PressureTrend.Direction.RISING -> RISING
            PressureTrend.Direction.STEADY -> STEADY
            PressureTrend.Direction.FALLING -> FALLING
        }
        val step = ((pressure - BOTTOM_HPA) / STEP).roundToInt().coerceIn(0, table.lastIndex)
        val forecastIndex = table[step]
        return Forecast(FORECASTS[forecastIndex], forecastIndex)
    }
}
