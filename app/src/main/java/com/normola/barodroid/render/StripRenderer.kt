package com.normola.barodroid.render

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.graphics.ColorUtils
import com.normola.barodroid.core.DialScale
import com.normola.barodroid.core.PressureSample
import com.normola.barodroid.core.PressureUnit

/** Input for the wide "pressure strip" widget. */
data class StripState(
    val pressureHpa: Double?,
    val samples: List<PressureSample>,
    val unit: PressureUnit = PressureUnit.HECTOPASCAL,
    val trendLabel: String? = null,
    val trendArrow: String? = null,
    val deltaLabel: String? = null,
    val forecast: String? = null,
    val caption: String? = null,
    val now: Long = System.currentTimeMillis(),
    val emptyMessage: String = "No reading yet",
)

/**
 * A horizontal readout: the number on the left, the last 24 hours traced on the
 * right. Everything is drawn into one bitmap so the widget looks identical to
 * the app, whatever the launcher does to layouts.
 */
class StripRenderer {

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val graph = GraphRenderer()

    private val light = Typeface.create("sans-serif-light", Typeface.NORMAL)
    private val regular = Typeface.create("sans-serif", Typeface.NORMAL)
    private val medium = Typeface.create("sans-serif-medium", Typeface.NORMAL)

    fun draw(canvas: Canvas, width: Float, height: Float, state: StripState, theme: DialTheme) {
        if (width <= 0f || height <= 0f) return

        val corner = minOf(height * 0.22f, width * 0.08f)
        rect.set(0f, 0f, width, height)
        fill.color = ColorUtils.setAlphaComponent(theme.faceEdge, 242)
        canvas.drawRoundRect(rect, corner, corner, fill)

        fill.color = ColorUtils.setAlphaComponent(theme.rim, 120)
        canvas.drawRoundRect(rect, corner, corner, strokeOf(fill, height * 0.012f))

        val padding = height * 0.12f
        val roomy = height > width * 0.35f
        val textWidth = if (roomy) width * 0.42f else width * 0.46f

        drawReadout(canvas, padding, padding, textWidth - padding, height - padding * 2f, state, theme)

        val graphLeft = textWidth
        canvas.save()
        canvas.translate(graphLeft, padding)
        graph.draw(
            canvas,
            width - graphLeft - padding,
            height - padding * 2f,
            GraphState(
                samples = state.samples,
                unit = state.unit,
                now = state.now,
                compact = true,
                emptyMessage = "",
            ),
            theme,
        )
        canvas.restore()
    }

    private fun strokeOf(source: Paint, width: Float): Paint = Paint(source).apply {
        style = Paint.Style.STROKE
        strokeWidth = width
    }

    private fun drawReadout(
        canvas: Canvas,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        state: StripState,
        theme: DialTheme,
    ) {
        text.textAlign = Paint.Align.LEFT
        val pressure = state.pressureHpa

        text.typeface = light
        text.color = theme.readout
        text.textSize = minOf(height * 0.46f, width * 0.36f)
        val value = pressure?.let { state.unit.format(it) } ?: "--"
        val valueBaseline = top + text.textSize * 0.82f
        canvas.drawText(value, left, valueBaseline, text)
        val valueWidth = text.measureText(value)

        text.typeface = medium
        text.color = theme.caption
        text.textSize = minOf(height * 0.17f, width * 0.14f)
        canvas.drawText(state.unit.symbol, left + valueWidth + width * 0.03f, valueBaseline, text)

        text.typeface = regular
        text.textSize = minOf(height * 0.17f, width * 0.13f)
        val trendLine = when {
            pressure == null -> state.emptyMessage
            else -> listOfNotNull(state.trendArrow, state.trendLabel, state.deltaLabel)
                .joinToString(" ")
        }
        text.color = if (pressure == null) theme.caption else theme.readout
        canvas.drawText(trendLine, left, valueBaseline + height * 0.26f, text)

        val third = state.forecast ?: pressure?.let { DialScale.zoneFor(it).label }
        if (third != null && height > width * 0.3f) {
            text.color = theme.caption
            text.textSize = minOf(height * 0.15f, width * 0.115f)
            canvas.drawText(ellipsise(third, width), left, valueBaseline + height * 0.47f, text)
        }
    }

    private fun ellipsise(value: String, maxWidth: Float): String {
        if (text.measureText(value) <= maxWidth) return value
        var candidate = value
        while (candidate.length > 1 && text.measureText("$candidate…") > maxWidth) {
            candidate = candidate.dropLast(1)
        }
        return "$candidate…"
    }
}
