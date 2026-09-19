package com.normola.barodroid

import android.content.Context
import com.normola.barodroid.data.PressureHistoryStore
import com.normola.barodroid.data.SettingsRepository
import com.normola.barodroid.sensor.BarometerSensor

/**
 * The app is small enough that a hand-rolled holder beats a dependency
 * injection framework. Everything here is process-wide and cheap to keep.
 */
object BaroGraph {

    @Volatile
    private var history: PressureHistoryStore? = null

    @Volatile
    private var settings: SettingsRepository? = null

    @Volatile
    private var sensor: BarometerSensor? = null

    fun history(context: Context): PressureHistoryStore =
        history ?: synchronized(this) {
            history ?: PressureHistoryStore(context).also { history = it }
        }

    fun settings(context: Context): SettingsRepository =
        settings ?: synchronized(this) {
            settings ?: SettingsRepository(context).also { settings = it }
        }

    fun sensor(context: Context): BarometerSensor =
        sensor ?: synchronized(this) {
            sensor ?: BarometerSensor(context).also { sensor = it }
        }
}
