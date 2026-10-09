package com.normola.barodroid.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.normola.barodroid.BaroGraph
import com.normola.barodroid.power.PowerState
import com.normola.barodroid.service.BaroLoggingService
import com.normola.barodroid.widget.BaroWidgets

/**
 * Takes a periodic reading and refreshes the widgets.
 *
 * Android stops delivering events from continuous sensors — the barometer
 * included — to apps in the background, so this worker succeeds when the app was
 * recently in use and comes back empty otherwise. An empty read still costs a
 * wakeup and a few seconds of CPU, so after a handful in a row the worker takes
 * the hint, stands itself down, and waits for the app to be opened again.
 */
class SamplingWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val sensor = BaroGraph.sensor(applicationContext)
        if (!sensor.isAvailable) return Result.success()

        // The foreground logging service is already sampling on its own clock;
        // duplicating it here would just wake the sensor twice as often.
        if (BaroLoggingService.isRunning) return Result.success()

        val settings = BaroGraph.settings(applicationContext)
        if (PowerState.isConserving(applicationContext)) {
            // Power save or a nearly flat battery: skip this round entirely.
            return Result.success()
        }

        val reading = sensor.readOnce()
        if (reading == null) {
            val empties = settings.emptyBackgroundReads() + 1
            settings.setEmptyBackgroundReads(empties)
            if (empties >= MAX_EMPTY_READS) {
                settings.setBackgroundSamplingPaused(true)
                SamplingScheduler.cancel(applicationContext)
            }
            // Nothing changed, so there is nothing for the widgets to redraw.
            return Result.success()
        }

        settings.setEmptyBackgroundReads(0)
        val stored = BaroGraph.history(applicationContext).record(reading)
        if (stored) {
            BaroWidgets.updateAll(applicationContext)
        }
        return Result.success()
    }

    companion object {
        /** Empty reads in a row before the worker gives up until the app reopens. */
        private const val MAX_EMPTY_READS = 4
    }
}
