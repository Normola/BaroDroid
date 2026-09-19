package com.normola.barodroid.core

import kotlin.math.abs

/**
 * Classic barometric tendency buckets, expressed as a change over three hours,
 * which is the window meteorologists (and old barometer set-hands) use.
 */
enum class PressureTrend(val label: String, val arrow: String) {
    FALLING_RAPIDLY("Falling rapidly", "↓↓"),
    FALLING_QUICKLY("Falling quickly", "↓"),
    FALLING("Falling", "↘"),
    FALLING_SLOWLY("Falling slowly", "↘"),
    STEADY("Steady", "→"),
    RISING_SLOWLY("Rising slowly", "↗"),
    RISING("Rising", "↗"),
    RISING_QUICKLY("Rising quickly", "↑"),
    RISING_RAPIDLY("Rising rapidly", "↑↑"),
    ;

    val isRising: Boolean get() = ordinal > STEADY.ordinal
    val isFalling: Boolean get() = ordinal < STEADY.ordinal

    /** Coarse direction used by the Zambretti forecaster. */
    val direction: Direction
        get() = when {
            isRising -> Direction.RISING
            isFalling -> Direction.FALLING
            else -> Direction.STEADY
        }

    enum class Direction { FALLING, STEADY, RISING }

    companion object {
        /**
         * Maps a three-hour change in hPa onto a tendency. Thresholds follow the
         * usual synoptic convention (0.5 / 1.6 / 3.6 / 6.0 hPa per three hours).
         */
        fun fromDeltaPer3h(deltaHpa: Double): PressureTrend {
            val magnitude = abs(deltaHpa)
            val rising = deltaHpa > 0
            return when {
                magnitude < 0.5 -> STEADY
                magnitude < 1.6 -> if (rising) RISING_SLOWLY else FALLING_SLOWLY
                magnitude < 3.6 -> if (rising) RISING else FALLING
                magnitude < 6.0 -> if (rising) RISING_QUICKLY else FALLING_QUICKLY
                else -> if (rising) RISING_RAPIDLY else FALLING_RAPIDLY
            }
        }
    }
}
