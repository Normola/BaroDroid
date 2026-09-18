package com.normola.barodroid.core

import kotlin.math.roundToInt

/**
 * Pressure units the app can display. Everything inside the app is stored and
 * calculated in hectopascals (hPa, identical to millibars); units are a
 * presentation concern only.
 */
enum class PressureUnit(
    val id: String,
    val symbol: String,
    val decimals: Int,
    private val perHpa: Double,
) {
    HECTOPASCAL("hpa", "hPa", 1, 1.0),
    MILLIBAR("mbar", "mb", 1, 1.0),
    INCHES_HG("inhg", "inHg", 2, 0.0295299830714),
    MILLIMETRES_HG("mmhg", "mmHg", 0, 0.750061683),
    KILOPASCAL("kpa", "kPa", 2, 0.1),
    ;

    fun fromHpa(hPa: Double): Double = hPa * perHpa

    fun toHpa(value: Double): Double = value / perHpa

    /** Formats a hPa value in this unit, without the symbol. */
    fun format(hPa: Double): String = formatValue(fromHpa(hPa), decimals)

    /** Formats a hPa value in this unit, with the symbol appended. */
    fun formatWithSymbol(hPa: Double): String = "${format(hPa)} $symbol"

    /**
     * Formats a *difference* in this unit, always signed, e.g. "+1.4" or "-0.02".
     * Deltas in mmHg get one decimal so that small changes stay visible.
     */
    fun formatDelta(deltaHpa: Double): String {
        val decimals = if (this == MILLIMETRES_HG) 1 else this.decimals
        val converted = fromHpa(deltaHpa)
        val sign = if (converted >= 0) "+" else "-"
        return sign + formatValue(kotlin.math.abs(converted), decimals)
    }

    companion object {
        fun fromId(id: String?): PressureUnit =
            entries.firstOrNull { it.id == id } ?: HECTOPASCAL

        /**
         * Rounds to [decimals] places without relying on java.text formatting,
         * so the same code can run in unit tests and on device with identical results.
         */
        internal fun formatValue(value: Double, decimals: Int): String {
            if (value.isNaN() || value.isInfinite()) return "--"
            if (decimals == 0) return value.roundToInt().toString()
            var factor = 1L
            repeat(decimals) { factor *= 10 }
            val scaled = (value * factor).roundToInt().toLong()
            val whole = scaled / factor
            val fraction = kotlin.math.abs(scaled % factor).toString().padStart(decimals, '0')
            val sign = if (scaled < 0 && whole == 0L) "-" else ""
            return "$sign$whole.$fraction"
        }
    }
}
