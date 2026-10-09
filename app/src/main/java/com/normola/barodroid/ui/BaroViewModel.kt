package com.normola.barodroid.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.normola.barodroid.BaroGraph
import com.normola.barodroid.core.PressureSmoother
import com.normola.barodroid.core.PressureUnit
import com.normola.barodroid.core.Zambretti
import com.normola.barodroid.data.BaroSettings
import com.normola.barodroid.domain.BarometerSnapshot
import com.normola.barodroid.service.BaroLoggingService
import com.normola.barodroid.widget.BaroWidgets
import com.normola.barodroid.work.SamplingScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BaroUiState(
    val snapshot: BarometerSnapshot = BarometerSnapshot.Empty,
    val settings: BaroSettings = BaroSettings(),
    val sensorAvailable: Boolean = true,
    val sensorName: String? = null,
)

class BaroViewModel(application: Application) : AndroidViewModel(application) {

    private val app: Application get() = getApplication()

    private val history = BaroGraph.history(application)
    private val settingsRepository = BaroGraph.settings(application)
    private val sensor = BaroGraph.sensor(application)

    private val smoother = PressureSmoother()

    /**
     * The live reading, sampled once a second and only while something is
     * collecting it. Everything downstream hangs off [state], so the sensor is
     * registered while the screen shows the dial and unregistered a few seconds
     * after it stops — not for as long as the view model happens to live.
     */
    private val liveReading: Flow<Double?> = flow {
        emit(history.latest?.hPa)
        emitAll(
            sensor.readings()
                .mapNotNull { smoother.offer(it) }
                .onEach { reading ->
                    // The store keeps this to one write a minute; everything in
                    // between costs a single comparison.
                    if (history.record(reading)) {
                        BaroWidgets.updateAll(app)
                    }
                },
        )
    }.onCompletion { smoother.reset() }

    /** Moves "now" along so the trend window and the graph scroll by themselves. */
    private val clock: Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(CLOCK_TICK_MILLIS)
        }
    }

    val state: StateFlow<BaroUiState> = combine(
        history.samples,
        settingsRepository.settings,
        liveReading,
        clock,
    ) { samples, settings, live, now ->
        BaroUiState(
            snapshot = BarometerSnapshot.build(samples, live, settings, now),
            settings = settings,
            sensorAvailable = sensor.isAvailable,
            sensorName = sensor.name,
        )
    }.stateIn(
        scope = viewModelScope,
        // Short grace period so a rotation does not drop the sensor and pick it
        // straight back up, but a screen-off does let go of it.
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_GRACE_MILLIS),
        initialValue = BaroUiState(sensorAvailable = sensor.isAvailable, sensorName = sensor.name),
    )

    init {
        viewModelScope.launch {
            history.load()

            // Opening the app is the signal that background sampling is worth
            // another try after it stood itself down.
            val settings = settingsRepository.current()
            settingsRepository.setEmptyBackgroundReads(0)
            if (settings.backgroundSamplingPaused) {
                settingsRepository.setBackgroundSamplingPaused(false)
            }
            SamplingScheduler.schedule(app, settings.sampleIntervalMinutes)
        }
    }

    fun setUnit(unit: PressureUnit) {
        update { settingsRepository.setUnit(unit) }
    }

    fun setSeaLevelCorrection(enabled: Boolean) {
        update { settingsRepository.setSeaLevelCorrection(enabled) }
    }

    fun setAltitudeMetres(metres: Double) {
        update { settingsRepository.setAltitudeMetres(metres) }
    }

    fun setHemisphere(hemisphere: Zambretti.Hemisphere) {
        update { settingsRepository.setHemisphere(hemisphere) }
    }

    fun setSampleIntervalMinutes(minutes: Int) {
        update {
            settingsRepository.setSampleIntervalMinutes(minutes)
            SamplingScheduler.schedule(app, minutes)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        update { settingsRepository.setDynamicColor(enabled) }
    }

    fun setBackgroundLogging(enabled: Boolean) {
        update {
            settingsRepository.setBackgroundLogging(enabled)
            if (enabled) {
                BaroLoggingService.start(app)
            } else {
                BaroLoggingService.stop(app)
            }
        }
    }

    fun clearHistory() {
        update { history.clear() }
    }

    /** Takes a reading right now, for the refresh action in the app bar. */
    fun refreshNow() {
        update {
            sensor.readOnce()?.let { reading ->
                history.record(reading)
            }
        }
    }

    private fun update(block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
            BaroWidgets.updateAll(app)
        }
    }

    private companion object {
        const val CLOCK_TICK_MILLIS = 60_000L
        const val SUBSCRIPTION_GRACE_MILLIS = 3_000L
    }
}
