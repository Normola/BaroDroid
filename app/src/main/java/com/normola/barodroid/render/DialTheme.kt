package com.normola.barodroid.render

import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.normola.barodroid.core.DialScale

/**
 * Colours for the barometer face. Kept as plain ARGB ints so the same renderer
 * can draw into a Compose canvas and into a widget bitmap.
 */
data class DialTheme(
    @ColorInt val faceCentre: Int,
    @ColorInt val faceEdge: Int,
    @ColorInt val rim: Int,
    @ColorInt val tick: Int,
    @ColorInt val tickMajor: Int,
    @ColorInt val scaleLabel: Int,
    @ColorInt val zoneLabel: Int,
    @ColorInt val needle: Int,
    @ColorInt val needleTail: Int,
    @ColorInt val setHand: Int,
    @ColorInt val hub: Int,
    @ColorInt val readout: Int,
    @ColorInt val caption: Int,
    @ColorInt val accent: Int,
    @ColorInt val stormy: Int,
    @ColorInt val rain: Int,
    @ColorInt val change: Int,
    @ColorInt val fair: Int,
    @ColorInt val veryDry: Int,
) {
    @ColorInt
    fun colorFor(zone: DialScale.Zone): Int = when (zone) {
        DialScale.Zone.STORMY -> stormy
        DialScale.Zone.RAIN -> rain
        DialScale.Zone.CHANGE -> change
        DialScale.Zone.FAIR -> fair
        DialScale.Zone.VERY_DRY -> veryDry
    }

    companion object {
        /** The default night face: graphite glass with a warm brass set-hand. */
        val Dark = DialTheme(
            faceCentre = 0xFF23262B.toInt(),
            faceEdge = 0xFF14161A.toInt(),
            rim = 0xFF3A3F47.toInt(),
            tick = 0xFF6D7481.toInt(),
            tickMajor = 0xFFD7DBE2.toInt(),
            scaleLabel = 0xFFE6E9EF.toInt(),
            zoneLabel = 0xFF9AA2B1.toInt(),
            needle = 0xFFE8EAEE.toInt(),
            needleTail = 0xFF7A8290.toInt(),
            setHand = 0xFFC9A227.toInt(),
            hub = 0xFF0E1013.toInt(),
            readout = 0xFFF5F7FA.toInt(),
            caption = 0xFF9AA2B1.toInt(),
            accent = 0xFF4FA3FF.toInt(),
            stormy = 0xFF7E57C2.toInt(),
            rain = 0xFF4285F4.toInt(),
            change = 0xFF26A69A.toInt(),
            fair = 0xFF9CCC65.toInt(),
            veryDry = 0xFFFFB300.toInt(),
        )

        /** The day face: warm paper, as printed dials tend to be. */
        val Light = DialTheme(
            faceCentre = 0xFFFFFFFF.toInt(),
            faceEdge = 0xFFEDE8DF.toInt(),
            rim = 0xFFD5CDBF.toInt(),
            tick = 0xFF9A958C.toInt(),
            tickMajor = 0xFF3C3A36.toInt(),
            scaleLabel = 0xFF2B2A27.toInt(),
            zoneLabel = 0xFF7A756C.toInt(),
            needle = 0xFF1C1B1A.toInt(),
            needleTail = 0xFF8C8880.toInt(),
            setHand = 0xFFB08319.toInt(),
            hub = 0xFF1C1B1A.toInt(),
            readout = 0xFF15140F.toInt(),
            caption = 0xFF6F6A61.toInt(),
            accent = 0xFF1668D6.toInt(),
            stormy = 0xFF5E35B1.toInt(),
            rain = 0xFF1A73E8.toInt(),
            change = 0xFF00897B.toInt(),
            fair = 0xFF7CB342.toInt(),
            veryDry = 0xFFF09300.toInt(),
        )

        /**
         * Tints a base theme with the app's accent colour, so the dial follows
         * Material You when dynamic colour is on.
         */
        fun tinted(base: DialTheme, accent: Color, onSurface: Color): DialTheme = base.copy(
            accent = accent.toArgb(),
            needle = onSurface.toArgb(),
            readout = onSurface.toArgb(),
        )
    }
}
