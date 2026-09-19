package com.normola.barodroid.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.normola.barodroid.BaroGraph
import com.normola.barodroid.service.BaroLoggingService
import com.normola.barodroid.work.SamplingScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Puts sampling back in place after a reboot or an app update. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val settings = BaroGraph.settings(appContext).current()
                SamplingScheduler.schedule(appContext, settings.sampleIntervalMinutes)
                if (settings.backgroundLogging) {
                    BaroLoggingService.start(appContext)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
