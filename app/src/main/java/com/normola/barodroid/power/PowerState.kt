package com.normola.barodroid.power

import android.content.Context
import android.os.BatteryManager
import android.os.PowerManager

/**
 * Whether the device is asking apps to go easy. Background sampling pauses while
 * it is, which is the behaviour a user expects from a barometer: a gap in the
 * graph is a fair price for the battery lasting the evening.
 */
object PowerState {

    /** Below this, sampling waits for the charger. */
    private const val LOW_BATTERY_PERCENT = 15

    fun isConserving(context: Context): Boolean = isPowerSaveMode(context) || isBatteryLow(context)

    fun isPowerSaveMode(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true

    fun isBatteryLow(context: Context): Boolean {
        if (isCharging(context)) return false
        val manager = context.getSystemService(BatteryManager::class.java) ?: return false
        val level = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return level in 1..<LOW_BATTERY_PERCENT
    }

    fun isCharging(context: Context): Boolean =
        context.getSystemService(BatteryManager::class.java)?.isCharging == true
}
