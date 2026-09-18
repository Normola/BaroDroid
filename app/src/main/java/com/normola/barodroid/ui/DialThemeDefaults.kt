package com.normola.barodroid.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import com.normola.barodroid.render.DialTheme

/**
 * Dresses the instrument faces in the current Material colours, so the dial
 * follows Material You while keeping the brass set-hand it inherited from the
 * real thing.
 */
@Composable
fun rememberDialTheme(dark: Boolean = isSystemInDarkTheme()): DialTheme {
    val scheme = MaterialTheme.colorScheme
    return remember(dark, scheme) {
        val base = if (dark) DialTheme.Dark else DialTheme.Light
        base.copy(
            faceCentre = scheme.surfaceVariant.toArgb(),
            faceEdge = scheme.surface.toArgb(),
            rim = scheme.outlineVariant.toArgb(),
            tick = scheme.outline.toArgb(),
            tickMajor = scheme.onSurface.toArgb(),
            scaleLabel = scheme.onSurface.toArgb(),
            zoneLabel = scheme.onSurfaceVariant.toArgb(),
            needle = scheme.onSurface.toArgb(),
            needleTail = scheme.outline.toArgb(),
            hub = scheme.surface.toArgb(),
            readout = scheme.onSurface.toArgb(),
            caption = scheme.onSurfaceVariant.toArgb(),
            accent = scheme.primary.toArgb(),
        )
    }
}
