package com.normola.barodroid.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.normola.barodroid.render.DialRenderer
import com.normola.barodroid.render.DialState
import com.normola.barodroid.render.DialTheme
import com.normola.barodroid.render.GraphRenderer
import com.normola.barodroid.render.GraphState

/**
 * The barometer face. The needle is animated towards the live reading so it
 * settles like a real hand rather than snapping between samples.
 */
@Composable
fun BarometerDial(
    state: DialState,
    theme: DialTheme,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val renderer = remember { DialRenderer() }
    val target = state.pressureHpa?.toFloat()
    val animated by animateFloatAsState(
        targetValue = target ?: 0f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "needle",
    )
    val reference = state.referenceHpa?.toFloat()
    val animatedReference by animateFloatAsState(
        targetValue = reference ?: 0f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "set-hand",
    )

    Canvas(
        modifier = modifier.semantics {
            contentDescription?.let { this.contentDescription = it }
        },
    ) {
        drawIntoCanvas { canvas ->
            renderer.draw(
                canvas.nativeCanvas,
                size.width,
                size.height,
                state.copy(
                    pressureHpa = target?.let { animated.toDouble() },
                    referenceHpa = reference?.let { animatedReference.toDouble() },
                ),
                theme,
            )
        }
    }
}

/** The recent pressure trace. */
@Composable
fun PressureGraph(
    state: GraphState,
    theme: DialTheme,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val renderer = remember { GraphRenderer() }
    Canvas(
        modifier = modifier.semantics {
            contentDescription?.let { this.contentDescription = it }
        },
    ) {
        drawIntoCanvas { canvas ->
            renderer.draw(canvas.nativeCanvas, size.width, size.height, state, theme)
        }
    }
}
