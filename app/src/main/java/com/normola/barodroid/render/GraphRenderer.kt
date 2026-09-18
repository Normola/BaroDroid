package com.normola.barodroid.render

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.graphics.ColorUtils
import com.normola.barodroid.core.PressureSample
import com.normola.barodroid.core.PressureUnit
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

/** Input for the pressure history graph. Pressures are in hPa. */
data class GraphState(
    val samples: List<PressureSample>,
    val unit: PressureUnit = PressureUnit.HECTOPASCAL,
    val now: Long = System.currentTimeMillis(),
    val windowMillis: Long = 24 * 60 * 60 * 1000L,
    val compact: Boolean = false,
    val emptyMessage: String = "Collecting readings…",
)

/**
 * Draws the recent pressure trace: a plain line with a soft fill, the sort of
 * trace a barograph drum would have scratched out on paper.
 */
class GraphRenderer {

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePath = Path()
    private val fillPath = Path()

    fun draw(canvas: Canvas, width: Float, height: Float, state: GraphState, theme: DialTheme) {
        if (width <= 0f || height <= 0f) return

        val labelSize = if (state.compact) height * 0.16f else minOf(height * 0.11f, width * 0.045f)
        val leftInset = if (state.compact) 0f else labelSize * 3.2f
        val bottomInset = if (state.compact) 0f else labelSize * 1.9f
        val top = height * 0.06f
        val bottom = height - bottomInset - height * 0.04f
        val left = leftInset
        val right = width - (if (state.compact) 0f else labelSize * 0.6f)

        val from = state.now - state.windowMillis
        val points = state.samples.filter { it.timestamp in from..state.now }.sortedBy { it.timestamp }
        if (points.size < 2 || right <= left || bottom <= top) {
            drawEmpty(canvas, width, height, state, theme, labelSize)
            return
        }

        val values = points.map { state.unit.fromHpa(it.hPa) }
        val (minValue, maxValue) = paddedRange(values.min(), values.max(), state.unit)
        val span = (maxValue - minValue).takeIf { it > 0.0 } ?: 1.0

        fun xFor(timestamp: Long): Float =
            left + ((timestamp - from).toFloat() / state.windowMillis.toFloat()) * (right - left)

        fun yFor(value: Double): Float =
            (bottom - ((value - minValue) / span).toFloat() * (bottom - top))

        if (!state.compact) {
            drawGuides(canvas, left, right, top, bottom, minValue, maxValue, state, theme, labelSize)
        }

        linePath.reset()
        points.forEachIndexed { index, sample ->
            val x = xFor(sample.timestamp)
            val y = yFor(state.unit.fromHpa(sample.hPa))
            if (index == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
        }

        fillPath.reset()
        fillPath.addPath(linePath)
        fillPath.lineTo(xFor(points.last().timestamp), bottom)
        fillPath.lineTo(xFor(points.first().timestamp), bottom)
        fillPath.close()

        fill.shader = LinearGradient(
            0f,
            top,
            0f,
            bottom,
            ColorUtils.setAlphaComponent(theme.accent, 110),
            ColorUtils.setAlphaComponent(theme.accent, 0),
            Shader.TileMode.CLAMP,
        )
        canvas.drawPath(fillPath, fill)
        fill.shader = null

        stroke.color = theme.accent
        stroke.strokeWidth = if (state.compact) height * 0.055f else height * 0.035f
        canvas.drawPath(linePath, stroke)

        val lastX = xFor(points.last().timestamp)
        val lastY = yFor(state.unit.fromHpa(points.last().hPa))
        fill.color = ColorUtils.setAlphaComponent(theme.accent, 70)
        canvas.drawCircle(lastX, lastY, stroke.strokeWidth * 2.2f, fill)
        fill.color = theme.accent
        canvas.drawCircle(lastX, lastY, stroke.strokeWidth * 1.1f, fill)
    }

    private fun drawGuides(
        canvas: Canvas,
        left: Float,
        right: Float,
        top: Float,
        bottom: Float,
        minValue: Double,
        maxValue: Double,
        state: GraphState,
        theme: DialTheme,
        labelSize: Float,
    ) {
        stroke.color = ColorUtils.setAlphaComponent(theme.tick, 90)
        stroke.strokeWidth = labelSize * 0.05f
        text.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        text.textSize = labelSize
        text.color = theme.caption

        // Pressure guides: bottom, middle and top of the plotted range.
        text.textAlign = Paint.Align.RIGHT
        listOf(0f, 0.5f, 1f).forEach { fraction ->
            val y = bottom - fraction * (bottom - top)
            canvas.drawLine(left, y, right, y, stroke)
            val value = minValue + (maxValue - minValue) * fraction
            canvas.drawText(
                PressureUnit.formatValue(value, state.unit.decimals),
                left - labelSize * 0.45f,
                y - (text.descent() + text.ascent()) / 2f,
                text,
            )
        }

        // Time guides every six hours.
        text.textAlign = Paint.Align.CENTER
        val hours = (state.windowMillis / 3_600_000L).toInt()
        val step = if (hours >= 24) 6 else 3
        var hoursAgo = hours
        while (hoursAgo >= 0) {
            val fraction = 1f - hoursAgo.toFloat() / hours.toFloat()
            val x = left + fraction * (right - left)
            if (hoursAgo != hours && hoursAgo != 0) {
                canvas.drawLine(x, top, x, bottom, stroke)
            }
            val label = if (hoursAgo == 0) "now" else "-${hoursAgo}h"
            canvas.drawText(label, x, bottom + labelSize * 1.5f, text)
            hoursAgo -= step
        }
    }

    private fun drawEmpty(
        canvas: Canvas,
        width: Float,
        height: Float,
        state: GraphState,
        theme: DialTheme,
        labelSize: Float,
    ) {
        text.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        text.textAlign = Paint.Align.CENTER
        text.color = theme.caption
        text.textSize = labelSize
        canvas.drawText(
            state.emptyMessage,
            width / 2f,
            height / 2f - (text.descent() + text.ascent()) / 2f,
            text,
        )
    }

    /**
     * Rounds the plotted range out to tidy numbers and keeps a floor on the span
     * so that a calm day does not turn sensor noise into a mountain range.
     */
    private fun paddedRange(min: Double, max: Double, unit: PressureUnit): Pair<Double, Double> {
        val minimumSpan = unit.fromHpa(4.0) - unit.fromHpa(0.0)
        val centre = (min + max) / 2.0
        var low = min
        var high = max
        if (abs(high - low) < abs(minimumSpan)) {
            low = centre - abs(minimumSpan) / 2.0
            high = centre + abs(minimumSpan) / 2.0
        } else {
            val padding = (high - low) * 0.15
            low -= padding
            high += padding
        }
        val step = niceStep(high - low)
        return floor(low / step) * step to ceil(high / step) * step
    }

    private fun niceStep(span: Double): Double {
        val candidates = doubleArrayOf(0.01, 0.02, 0.05, 0.1, 0.2, 0.5, 1.0, 2.0, 5.0, 10.0, 20.0)
        return candidates.firstOrNull { span / it <= 6 } ?: 50.0
    }
}
