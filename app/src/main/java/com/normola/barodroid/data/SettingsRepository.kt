package com.normola.barodroid.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.normola.barodroid.core.PressureUnit
import com.normola.barodroid.core.Zambretti
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "barodroid_settings")

class SettingsRepository(context: Context) {

    private val store = context.applicationContext.settingsDataStore

    val settings: Flow<BaroSettings> = store.data
        .catch { error ->
            // A corrupt preferences file should not take the app down with it.
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs ->
            BaroSettings(
                unit = PressureUnit.fromId(prefs[Keys.UNIT]),
                seaLevelCorrection = prefs[Keys.SEA_LEVEL] ?: false,
                altitudeMetres = prefs[Keys.ALTITUDE] ?: 0.0,
                hemisphere = Zambretti.Hemisphere.fromId(prefs[Keys.HEMISPHERE]),
                sampleIntervalMinutes = prefs[Keys.SAMPLE_INTERVAL] ?: 15,
                dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: true,
                backgroundLogging = prefs[Keys.BACKGROUND_LOGGING] ?: false,
            )
        }

    /** One-shot read, for the widget and the background worker. */
    suspend fun current(): BaroSettings = settings.first()

    suspend fun setUnit(unit: PressureUnit) = edit { it[Keys.UNIT] = unit.id }

    suspend fun setSeaLevelCorrection(enabled: Boolean) = edit { it[Keys.SEA_LEVEL] = enabled }

    suspend fun setAltitudeMetres(metres: Double) = edit {
        it[Keys.ALTITUDE] = metres.coerceIn(-500.0, 9000.0)
    }

    suspend fun setHemisphere(hemisphere: Zambretti.Hemisphere) = edit {
        it[Keys.HEMISPHERE] = hemisphere.id
    }

    suspend fun setSampleIntervalMinutes(minutes: Int) = edit {
        it[Keys.SAMPLE_INTERVAL] = minutes.coerceAtLeast(15)
    }

    suspend fun setDynamicColor(enabled: Boolean) = edit { it[Keys.DYNAMIC_COLOR] = enabled }

    suspend fun setBackgroundLogging(enabled: Boolean) = edit { it[Keys.BACKGROUND_LOGGING] = enabled }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        store.edit(block)
    }

    private object Keys {
        val UNIT = stringPreferencesKey("unit")
        val SEA_LEVEL = booleanPreferencesKey("sea_level_correction")
        val ALTITUDE = doublePreferencesKey("altitude_metres")
        val HEMISPHERE = stringPreferencesKey("hemisphere")
        val SAMPLE_INTERVAL = intPreferencesKey("sample_interval_minutes")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val BACKGROUND_LOGGING = booleanPreferencesKey("background_logging")
    }
}
