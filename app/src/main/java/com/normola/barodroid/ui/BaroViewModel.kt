package com.normola.barodroid.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.normola.barodroid.BaroGraph
import com.normola.barodroid.core.PressureUnit
import com.normola.barodroid.core.Zambretti
import com.normola.barodroid.data.BaroSettings
import com.normola.barodroid.domain.BarometerSnapshot
import com.normola.barodroid.service.BaroLoggingService
import com.normola.barodroid.widget.BaroWidgets
import com.normola.barodroid.work.SamplingScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

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

    private val liveReading = MutableStateFlow<Double?>(null)
    private val clock = MutableStateFlow(System.currentTimeMillis())

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
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BaroUiState(sensorAvailable = sensor.isAvailable, sensorName = sensor.name),
    )

    init {
        viewModelScope.launch { history.load() }

        // While the app is open the sensor is the source of truth; the store
        // throttles writes, so this quietly fills the history a sample a minute.
        viewModelScope.launch {
            sensor.readings().collect { reading ->
                liveReading.value = reading
                if (history.record(reading)) {
                    BaroWidgets.updateAll(app)
                }
            }
        }

        // Keeps "now" moving so the trend window and the graph scroll on their own.
        viewModelScope.launch {
            while (isActive) {
                delay(30_000L)
                clock.value = System.currentTimeMillis()
            }
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
            sensor.readOnce(timeoutMillis = 5_000L)?.let { reading ->
                liveReading.value = reading
                history.record(reading)
            }
            clock.value = System.currentTimeMillis()
            SamplingScheduler.sampleNow(app)
        }
    }

    private fun update(block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
            BaroWidgets.updateAll(app)
        }
    }
}
