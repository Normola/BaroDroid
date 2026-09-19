package com.normola.barodroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.normola.barodroid.ui.BaroViewModel
import com.normola.barodroid.ui.HomeScreen
import com.normola.barodroid.ui.SettingsScreen
import com.normola.barodroid.ui.theme.BaroTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: BaroViewModel = viewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()

            BaroTheme(dynamicColor = state.settings.dynamicColor) {
                var showSettings by rememberSaveable { mutableStateOf(false) }

                BackHandler(enabled = showSettings) { showSettings = false }

                if (showSettings) {
                    SettingsScreen(
                        state = state,
                        onBack = { showSettings = false },
                        onUnitChange = viewModel::setUnit,
                        onSeaLevelChange = viewModel::setSeaLevelCorrection,
                        onAltitudeChange = viewModel::setAltitudeMetres,
                        onHemisphereChange = viewModel::setHemisphere,
                        onIntervalChange = viewModel::setSampleIntervalMinutes,
                        onBackgroundLoggingChange = viewModel::setBackgroundLogging,
                        onDynamicColorChange = viewModel::setDynamicColor,
                        onClearHistory = viewModel::clearHistory,
                    )
                } else {
                    HomeScreen(
                        state = state,
                        onOpenSettings = { showSettings = true },
                        onRefresh = viewModel::refreshNow,
                    )
                }
            }
        }
    }
}
