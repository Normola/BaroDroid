package com.normola.barodroid.ui

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.normola.barodroid.core.DialScale
import com.normola.barodroid.core.PressureUnit
import com.normola.barodroid.render.DialState
import com.normola.barodroid.render.GraphState
import com.normola.barodroid.ui.components.BarometerDial
import com.normola.barodroid.ui.components.PressureGraph

private val GRAPH_WINDOWS = listOf(6, 24, 48)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: BaroUiState,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
) {
    val dialTheme = rememberDialTheme()
    var windowHours by rememberSaveable { mutableIntStateOf(24) }
    val snapshot = state.snapshot
    val unit = state.settings.unit
    val trend = snapshot.trend.takeIf { it.isReliable }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BaroDroid") },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Take a reading now")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
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
            if (!state.sensorAvailable) {
                NoticeCard(
                    title = "No barometer on this device",
                    body = "BaroDroid needs a pressure sensor. The dial stays empty, but any " +
                        "history already recorded is still shown.",
                )
            }

            BarometerDial(
                state = DialState(
                    pressureHpa = snapshot.displayHpa,
                    referenceHpa = snapshot.referenceHpa,
                    unit = unit,
                    trendLabel = trend?.trend?.label,
                    trendArrow = trend?.trend?.arrow,
                    caption = captionFor(state),
                ),
                theme = dialTheme,
                contentDescription = snapshot.displayHpa?.let {
                    "Barometer reading ${unit.formatWithSymbol(it)}, ${trend?.trend?.label ?: "trend unknown"}"
                } ?: "No barometer reading yet",
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .aspectRatio(1f)
                    .align(Alignment.CenterHorizontally),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    label = "3 h change",
                    value = trend?.let { "${unit.formatDelta(it.deltaPer3h)} ${unit.symbol}" } ?: "—",
                    caption = trend?.trend?.label ?: "Needs ~20 min of readings",
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = "${windowHours} h range",
                    value = rangeLabel(state, windowHours),
                    caption = "low to high",
                    modifier = Modifier.weight(1f),
                )
            }

            ForecastCard(state)

            Card(colors = CardDefaults.cardColors()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("History", style = MaterialTheme.typography.titleMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            GRAPH_WINDOWS.forEach { hours ->
                                FilterChip(
                                    selected = windowHours == hours,
                                    onClick = { windowHours = hours },
                                    label = { Text("${hours}h") },
                                    modifier = Modifier.padding(start = 6.dp),
                                    colors = FilterChipDefaults.filterChipColors(),
                                )
                            }
                        }
                    }
                    PressureGraph(
                        state = GraphState(
                            samples = snapshot.samples,
                            unit = unit,
                            windowMillis = windowHours * 60L * 60L * 1000L,
                        ),
                        theme = rememberDialTheme(),
                        contentDescription = "Pressure over the last $windowHours hours",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                    )
                }
            }

            DetailCard(state)
        }
    }
}

@Composable
private fun ForecastCard(state: BaroUiState) {
    val snapshot = state.snapshot
    val zone = snapshot.displayHpa?.let { DialScale.zoneFor(it) }
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "OUTLOOK",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = snapshot.forecast?.text ?: "Collecting readings — the forecast needs a " +
                    "little history before it can say anything.",
                style = MaterialTheme.typography.headlineSmall,
            )
            if (zone != null) {
                Text(
                    text = "Dial reads ${zone.label.lowercase()}" +
                        (snapshot.seaLevelHpa?.let {
                            " · ${PressureUnit.HECTOPASCAL.formatWithSymbol(it)} at sea level"
                        } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DetailCard(state: BaroUiState) {
    val snapshot = state.snapshot
    val unit = state.settings.unit
    Card(modifier = Modifier.padding(bottom = 24.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DetailRow("Sensor reading", snapshot.stationHpa?.let { unit.formatWithSymbol(it) } ?: "—")
            DetailRow("At sea level", snapshot.seaLevelHpa?.let { unit.formatWithSymbol(it) } ?: "—")
            DetailRow(
                "Samples stored",
                snapshot.samples.size.toString(),
            )
            DetailRow(
                "Background logging",
                if (state.settings.backgroundLogging) "On" else "Off",
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    caption: String,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = value, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NoticeCard(title: String, body: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Start)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun captionFor(state: BaroUiState): String? {
    val snapshot = state.snapshot
    if (!snapshot.hasReading) return null
    val correction = if (state.settings.seaLevelCorrection) "sea level" else "station"
    val updated = if (snapshot.updatedAt > 0L) {
        DateUtils.getRelativeTimeSpanString(
            snapshot.updatedAt,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
        ).toString()
    } else {
        null
    }
    return listOfNotNull(correction, updated).joinToString(" · ")
}

private fun rangeLabel(state: BaroUiState, windowHours: Int): String {
    val unit = state.settings.unit
    val from = System.currentTimeMillis() - windowHours * 60L * 60L * 1000L
    val values = state.snapshot.samples.filter { it.timestamp >= from }.map { it.hPa }
    if (values.isEmpty()) return "—"
    return "${unit.format(values.min())}–${unit.format(values.max())}"
}
