package com.normola.barodroid.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.normola.barodroid.core.PressureUnit
import com.normola.barodroid.core.Zambretti
import com.normola.barodroid.data.BaroSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: BaroUiState,
    onBack: () -> Unit,
    onUnitChange: (PressureUnit) -> Unit,
    onSeaLevelChange: (Boolean) -> Unit,
    onAltitudeChange: (Double) -> Unit,
    onHemisphereChange: (Zambretti.Hemisphere) -> Unit,
    onIntervalChange: (Int) -> Unit,
    onBackgroundLoggingChange: (Boolean) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onClearHistory: () -> Unit,
) {
    val settings = state.settings
    val notificationPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { _ ->
        // The service runs either way; without the permission the ongoing
        // notification is simply not shown to the user.
        onBackgroundLoggingChange(true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SettingsSection("Units") {
                PressureUnit.entries.forEach { unit ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = settings.unit == unit,
                                onClick = { onUnitChange(unit) },
                            )
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = settings.unit == unit, onClick = { onUnitChange(unit) })
                        Text(
                            text = "${unit.symbol} · ${unit.format(1013.25)}",
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }

            SettingsSection("Altitude") {
                SwitchRow(
                    title = "Show sea-level pressure",
                    subtitle = "Weather reports and the classic dial are calibrated to sea level.",
                    checked = settings.seaLevelCorrection,
                    onCheckedChange = onSeaLevelChange,
                )
                AltitudeField(
                    settings = settings,
                    onAltitudeChange = onAltitudeChange,
                )
            }

            SettingsSection("Readings") {
                Text(
                    text = "How often the background sample runs",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BaroSettings.SAMPLE_INTERVAL_OPTIONS.forEach { minutes ->
                        FilterChip(
                            selected = settings.sampleIntervalMinutes == minutes,
                            onClick = { onIntervalChange(minutes) },
                            label = { Text("$minutes min") },
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SwitchRow(
                    title = "Keep logging in the background",
                    subtitle = "Android stops feeding the barometer to apps that are not " +
                        "visible, so an unbroken graph needs a quiet ongoing notification.",
                    checked = settings.backgroundLogging,
                    onCheckedChange = { enabled ->
                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            onBackgroundLoggingChange(enabled)
                        }
                    },
                )
            }

            SettingsSection("Forecast") {
                Text(
                    text = "The Zambretti forecaster shifts with the seasons, so it needs to " +
                        "know which half of the world you are in.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Zambretti.Hemisphere.entries.forEach { hemisphere ->
                        FilterChip(
                            selected = settings.hemisphere == hemisphere,
                            onClick = { onHemisphereChange(hemisphere) },
                            label = {
                                Text(if (hemisphere == Zambretti.Hemisphere.NORTHERN) "Northern" else "Southern")
                            },
                        )
                    }
                }
            }

            SettingsSection("Appearance") {
                SwitchRow(
                    title = "Match system colours",
                    subtitle = "Uses the wallpaper palette on Android 12 and later.",
                    checked = settings.dynamicColor,
                    onCheckedChange = onDynamicColorChange,
                )
            }

            SettingsSection("Data") {
                Text(
                    text = "${state.snapshot.samples.size} readings stored, up to 48 hours.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onClearHistory) { Text("Clear history") }
            }

            SettingsSection("About") {
                Text(
                    text = state.sensorName?.let { "Sensor: $it" } ?: "No pressure sensor found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "The dial reads 950–1050 hPa on the outer scale and 28–31 inHg " +
                        "inside it, with the brass hand marking where the needle stood three " +
                        "hours ago — the same way a set-hand works on a real barometer.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun AltitudeField(
    settings: BaroSettings,
    onAltitudeChange: (Double) -> Unit,
) {
    var value by remember(settings.altitudeMetres) {
        mutableStateOf(
            if (settings.altitudeMetres == 0.0) "" else settings.altitudeMetres.toInt().toString(),
        )
    }
    OutlinedTextField(
        value = value,
        onValueChange = { entered ->
            value = entered.filter { it.isDigit() || it == '-' }
            value.toDoubleOrNull()?.let(onAltitudeChange) ?: run {
                if (value.isBlank()) onAltitudeChange(0.0)
            }
        },
        label = { Text("Altitude in metres") },
        supportingText = { Text("Roughly 12 hPa for every 100 m above sea level.") },
        enabled = settings.seaLevelCorrection,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content()
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
