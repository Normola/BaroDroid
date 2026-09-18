package com.normola.barodroid.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object SamplingScheduler {

    private const val PERIODIC_WORK = "barodroid-periodic-sampling"
    private const val ONE_SHOT_WORK = "barodroid-sample-now"

    fun schedule(context: Context, intervalMinutes: Int) {
        val interval = intervalMinutes.toLong().coerceAtLeast(15L)
        val request = PeriodicWorkRequestBuilder<SamplingWorker>(interval, TimeUnit.MINUTES)
            .setInitialDelay(interval, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    /** Used by the widget's refresh action and after a settings change. */
    fun sampleNow(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            ONE_SHOT_WORK,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<SamplingWorker>().build(),
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK)
    }
}
