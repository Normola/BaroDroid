package com.normola.barodroid.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.normola.barodroid.core.PressureMath
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** Thin wrapper over [Sensor.TYPE_PRESSURE], which reports station pressure in hPa. */
class BarometerSensor(context: Context) {

    private val sensorManager =
        context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

    val isAvailable: Boolean get() = sensor != null

    /** Human-readable sensor name, for the settings screen. */
    val name: String? get() = sensor?.let { "${it.vendor} ${it.name}".trim() }

    /**
     * Live readings in hPa. Implausible values are dropped — some devices emit a
     * zero or a wild spike on the first event after registering.
     */
    fun readings(samplingPeriodUs: Int = SensorManager.SENSOR_DELAY_UI): Flow<Double> = callbackFlow {
        val pressureSensor = sensor
        if (pressureSensor == null) {
            close()
            return@callbackFlow
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val value = event.values.firstOrNull()?.toDouble() ?: return
                if (PressureMath.isPlausible(value)) trySend(value)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager.registerListener(listener, pressureSensor, samplingPeriodUs)
        awaitClose { sensorManager.unregisterListener(listener) }
    }

    /**
     * Takes a single reading, for background work where keeping the sensor
     * registered would be wasteful. Returns null if the device has no barometer
     * or the sensor stays silent.
     */
    suspend fun readOnce(timeoutMillis: Long = 10_000L): Double? {
        if (!isAvailable) return null
        return withTimeoutOrNull(timeoutMillis) {
            readings(SensorManager.SENSOR_DELAY_NORMAL).first()
        }
    }
}
