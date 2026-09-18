package com.normola.barodroid.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.normola.barodroid.BaroGraph
import com.normola.barodroid.MainActivity
import com.normola.barodroid.R
import com.normola.barodroid.core.PressureUnit
import com.normola.barodroid.widget.BaroWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Keeps the barograph running while the app is closed.
 *
 * Since Android 9 an app in the background gets no events from continuous
 * sensors, so an unbroken pressure history needs a foreground service. It wakes
 * up every few minutes, takes one reading, and goes back to sleep — the sensor
 * itself draws well under a milliamp, and the notification makes the trade
 * visible to the user, who switches this on themselves.
 */
class BaroLoggingService : Service() {

    private val job: Job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)
    private var loopJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundCompat(buildNotification(null))
        if (loopJob?.isActive != true) {
            loopJob = scope.launch { sampleLoop() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun sampleLoop() {
        val sensor = BaroGraph.sensor(this)
        if (!sensor.isAvailable) {
            stopSelf()
            return
        }
        val history = BaroGraph.history(this)
        val settings = BaroGraph.settings(this)
        while (scope.isActive) {
            val intervalMinutes = runCatching { settings.current().sampleIntervalMinutes }
                .getOrDefault(15)
            val reading = sensor.readOnce(timeoutMillis = 15_000L)
            if (reading != null) {
                history.record(reading)
                notify(buildNotification(reading))
                BaroWidgets.updateAll(this)
            }
            delay(intervalMinutes.toLong().coerceAtLeast(1L) * 60_000L)
        }
    }

    private fun startForegroundCompat(notification: Notification) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    private fun notify(notification: Notification) {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(readingHpa: Double?): Notification {
        ensureChannel()

        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, BaroLoggingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val title = readingHpa
            ?.let { getString(R.string.logging_notification_reading, PressureUnit.HECTOPASCAL.formatWithSymbol(it)) }
            ?: getString(R.string.logging_notification_starting)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_barometer)
            .setContentTitle(title)
            .setContentText(getString(R.string.logging_notification_body))
            .setContentIntent(open)
            .addAction(0, getString(R.string.action_stop), stop)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.logging_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.logging_channel_description)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "barodroid_logging"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_STOP = "com.normola.barodroid.action.STOP_LOGGING"

        /**
         * Starts logging. Android 12+ refuses foreground service starts from the
         * background, so the caller must be visible; failures are swallowed
         * rather than crashing a boot receiver.
         */
        fun start(context: Context) {
            val intent = Intent(context, BaroLoggingService::class.java)
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        fun stop(context: Context) {
            runCatching {
                context.stopService(Intent(context, BaroLoggingService::class.java))
            }
        }
    }
}
