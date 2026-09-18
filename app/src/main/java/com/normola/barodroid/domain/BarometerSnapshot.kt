package com.normola.barodroid.domain

import com.normola.barodroid.core.PressureMath
import com.normola.barodroid.core.PressureSample
import com.normola.barodroid.core.PressureTrend
import com.normola.barodroid.core.TrendCalculator
import com.normola.barodroid.core.TrendResult
import com.normola.barodroid.core.Zambretti
import com.normola.barodroid.data.BaroSettings
import java.util.Calendar

/**
 * Everything the dial, the readouts and the widgets need, derived once from the
 * raw samples plus the user's settings.
 */
data class BarometerSnapshot(
    /** Latest raw sensor reading, before any sea-level correction. */
    val stationHpa: Double?,
    /** What should be shown and plotted: corrected if the user asked for it. */
    val displayHpa: Double?,
    /** Pressure reduced to sea level; the Zambretti forecast is based on this. */
    val seaLevelHpa: Double?,
    /** History in display terms, oldest first. */
    val samples: List<PressureSample>,
    val trend: TrendResult,
    val forecast: Zambretti.Forecast?,
    val updatedAt: Long,
    val hasReading: Boolean,
) {
    val trendKind: PressureTrend get() = trend.trend

    /** Where the brass set-hand goes: the reading three hours ago. */
    val referenceHpa: Double? get() = trend.referenceHpa?.takeIf { trend.isReliable }

    companion object {
        val Empty = BarometerSnapshot(
            stationHpa = null,
            displayHpa = null,
            seaLevelHpa = null,
            samples = emptyList(),
            trend = TrendResult.Unknown,
            forecast = null,
            updatedAt = 0L,
            hasReading = false,
        )

        /**
         * Builds a snapshot. [rawSamples] and [liveStationHpa] are raw station
         * readings; the correction and forecast are applied here so that the app
         * and the widgets can never disagree.
         */
        fun build(
            rawSamples: List<PressureSample>,
            liveStationHpa: Double?,
            settings: BaroSettings,
            now: Long = System.currentTimeMillis(),
            month: Int = monthOf(now),
        ): BarometerSnapshot {
            val station = liveStationHpa ?: rawSamples.lastOrNull()?.hPa
            if (station == null) return Empty

            val correct = { value: Double ->
                if (settings.seaLevelCorrection) {
                    PressureMath.toSeaLevel(value, settings.altitudeMetres)
                } else {
                    value
                }
            }

            val displaySamples = rawSamples.map { PressureSample(it.timestamp, correct(it.hPa)) }
            val latestTimestamp = if (liveStationHpa != null) now else rawSamples.lastOrNull()?.timestamp ?: now

            // Include the live reading in the trend fit so the needle and the
            // tendency stay in step between background samples.
            val forTrend = if (liveStationHpa != null) {
                displaySamples.filter { it.timestamp < now } + PressureSample(now, correct(station))
            } else {
                displaySamples
            }
            val trend = TrendCalculator.calculate(forTrend, now)

            val seaLevel = PressureMath.toSeaLevel(station, settings.altitudeMetres)
            val forecast = if (trend.isReliable) {
                Zambretti.forecast(seaLevel, trend.trend.direction, month, settings.hemisphere)
            } else {
                null
            }

            return BarometerSnapshot(
                stationHpa = station,
                displayHpa = correct(station),
                seaLevelHpa = seaLevel,
                samples = displaySamples,
                trend = trend,
                forecast = forecast,
                updatedAt = latestTimestamp,
                hasReading = true,
            )
        }

        private fun monthOf(millis: Long): Int =
            Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.MONTH) + 1
    }
}
