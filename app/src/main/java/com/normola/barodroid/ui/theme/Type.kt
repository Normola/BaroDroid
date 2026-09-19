package com.normola.barodroid.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Slightly tighter, lighter headlines than the Material default — instrument-like. */
internal val BaroTypography = Typography().let { base ->
    base.copy(
        displaySmall = base.displaySmall.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Light,
            letterSpacing = (-0.5).sp,
        ),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Light),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Medium),
        labelLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            letterSpacing = 0.6.sp,
        ),
    )
}
