package com.normola.barodroid.core

/** A single pressure sample: milliseconds since the epoch plus a station reading. */
data class PressureSample(val timestamp: Long, val hPa: Double)

/** The result of looking at the recent history of samples. */
data class TrendResult(
    /** Change in hPa normalised to a three-hour window. */
    val deltaPer3h: Double,
    val trend: PressureTrend,
    /** The reading the three-hour-ago "set hand" should point at, if known. */
    val referenceHpa: Double?,
    /** Span actually covered by the samples used, in milliseconds. */
    val coveredMillis: Long,
    /** True when there was enough history to say anything meaningful. */
    val isReliable: Boolean,
) {
    companion object {
        val Unknown = TrendResult(0.0, PressureTrend.STEADY, null, 0L, false)
    }
}

/**
 * Derives the barometric tendency from recorded samples.
 *
 * Rather than differencing two arbitrary readings (which is noisy on phone
 * sensors) this fits a least-squares line through everything inside the window
 * and reports the slope scaled to three hours.
 */
object TrendCalculator {

    private const val THREE_HOURS_MS = 3 * 60 * 60 * 1000L

    /** Minimum span of samples before a trend is treated as reliable. */
    private const val MIN_SPAN_MS = 20 * 60 * 1000L

    fun calculate(
        samples: List<PressureSample>,
        now: Long,
        windowMillis: Long = THREE_HOURS_MS,
    ): TrendResult {
        val window = samples
            .filter { it.timestamp in (now - windowMillis)..now }
            .sortedBy { it.timestamp }
        if (window.size < 2) return TrendResult.Unknown

        val span = window.last().timestamp - window.first().timestamp
        if (span <= 0L) return TrendResult.Unknown

        // Least-squares slope in hPa per millisecond, with time centred to keep
        // the numbers well conditioned.
        val meanT = window.sumOf { it.timestamp.toDouble() } / window.size
        val meanP = window.sumOf { it.hPa } / window.size
        var sxx = 0.0
        var sxy = 0.0
        for (sample in window) {
            val dt = sample.timestamp - meanT
            sxx += dt * dt
            sxy += dt * (sample.hPa - meanP)
        }
        if (sxx == 0.0) return TrendResult.Unknown

        val slopePerMs = sxy / sxx
        val deltaPer3h = slopePerMs * THREE_HOURS_MS
        val reliable = span >= MIN_SPAN_MS

        val latest = window.last().hPa
        val reference = latest - slopePerMs * span

        return TrendResult(
            deltaPer3h = deltaPer3h,
            trend = if (reliable) PressureTrend.fromDeltaPer3h(deltaPer3h) else PressureTrend.STEADY,
            referenceHpa = reference,
            coveredMillis = span,
            isReliable = reliable,
        )
    }
}
