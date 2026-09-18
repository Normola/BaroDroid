package com.normola.barodroid

import android.app.Application
import com.normola.barodroid.work.SamplingScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BaroApp : Application() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            val settings = BaroGraph.settings(this@BaroApp).current()
            SamplingScheduler.schedule(this@BaroApp, settings.sampleIntervalMinutes)
        }
    }
}
