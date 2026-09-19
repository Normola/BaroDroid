package com.normola.barodroid.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.normola.barodroid.BaroGraph
import com.normola.barodroid.widget.BaroWidgets

/**
 * Takes a periodic reading and refreshes the widgets.
 *
 * Android stops delivering events from continuous sensors — the barometer
 * included — to apps in the background, so this worker succeeds when the app was
 * recently in use and quietly gives up otherwise. Background logging (the
 * foreground service) is what keeps the history unbroken.
 */
class SamplingWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val sensor = BaroGraph.sensor(applicationContext)
        if (!sensor.isAvailable) return Result.success()

        val reading = sensor.readOnce(timeoutMillis = 8_000L)
        if (reading != null) {
            BaroGraph.history(applicationContext).record(reading)
        }
        // Even without a fresh reading the widgets are redrawn so the "last
        // updated" caption stays honest.
        BaroWidgets.updateAll(applicationContext)
        return Result.success()
    }
}
