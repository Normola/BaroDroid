package com.normola.barodroid.render

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.normola.barodroid.core.DialScale
import com.normola.barodroid.core.PressureUnit
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Everything the dial needs to draw itself. Pressures are in hPa. */
data class DialState(
    val pressureHpa: Double?,
    /** The brass set-hand: where the needle stood three hours ago. */
    val referenceHpa: Double? = null,
    val unit: PressureUnit = PressureUnit.HECTOPASCAL,
    val trendLabel: String? = null,
    val trendArrow: String? = null,
    /** Small line under the maker's name, e.g. "Sea level · 14:32". */
    val caption: String? = null,
    val brand: String? = "BARODROID",
    val emptyMessage: String = "No reading yet",
    /** Drops the finer detail so the face still reads at widget size. */
    val compact: Boolean = false,
)

/**
 * Draws an aneroid barometer face onto a [Canvas].
 *
 * The layout is the traditional one — a 270° scale in hPa, the classic
 * STORMY/RAIN/CHANGE/FAIR/VERY DRY lettering, a secondary inches-of-mercury
 * ring and a brass set-hand — rendered with flat colour, hairline ticks and a
 * digital readout in the gap at the bottom so it reads as a modern instrument
 * rather than a skeuomorphic one.
 *
 * Instances are not thread safe: keep one per consumer (the app screen keeps
 * one, each widget render makes its own).
 */
class DialRenderer {

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val needlePath = Path()
    private val arcRect = RectF()

    private val light = Typeface.create("sans-serif-light", Typeface.NORMAL)
    private val regular = Typeface.create("sans-serif", Typeface.NORMAL)
    private val medium = Typeface.create("sans-serif-medium", Typeface.NORMAL)

    fun draw(canvas: Canvas, width: Float, height: Float, state: DialState, theme: DialTheme) {
        if (width <= 0f || height <= 0f) return
        val cx = width / 2f
        val cy = height / 2f
        val r = min(width, height) / 2f * 0.98f

        drawFace(canvas, cx, cy, r, theme)
        drawZoneBand(canvas, cx, cy, r, theme)
        drawTicks(canvas, cx, cy, r, state, theme)
        drawZoneLettering(canvas, cx, cy, r, theme)
        if (!state.compact) {
            drawInchesScale(canvas, cx, cy, r, theme)
            drawBrand(canvas, cx, cy, r, state, theme)
        }

        val pressure = state.pressureHpa
        if (pressure != null) {
            drawTravelArc(canvas, cx, cy, r, pressure, state.referenceHpa, theme)
            state.referenceHpa?.let { drawSetHand(canvas, cx, cy, r, it, theme) }
            drawNeedle(canvas, cx, cy, r, pressure, theme)
        }
        drawHub(canvas, cx, cy, r, theme)
        drawReadout(canvas, cx, cy, r, state, theme)
    }

    private fun drawFace(canvas: Canvas, cx: Float, cy: Float, r: Float, theme: DialTheme) {
        fill.shader = RadialGradient(
            cx,
            cy - r * 0.25f,
            r * 1.25f,
            theme.faceCentre,
            theme.faceEdge,
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, r, fill)
        fill.shader = null

        stroke.color = theme.rim
        stroke.strokeWidth = r * 0.016f
        canvas.drawCircle(cx, cy, r - stroke.strokeWidth / 2f, stroke)
    }

    /** The coloured band that replaces the printed weather zones of an old face. */
    private fun drawZoneBand(canvas: Canvas, cx: Float, cy: Float, r: Float, theme: DialTheme) {
        val bandRadius = r * 0.935f
        stroke.strokeWidth = r * 0.038f
        stroke.strokeCap = Paint.Cap.BUTT
        arcRect.set(cx - bandRadius, cy - bandRadius, cx + bandRadius, cy + bandRadius)
        for (zone in DialScale.Zone.entries) {
            val start = DialScale.angleFor(zone.startHpa)
            val end = DialScale.angleFor(zone.endHpa)
            stroke.color = theme.colorFor(zone)
            stroke.alpha = 150
            // A hairline gap between zones keeps the band from reading as one blur.
            canvas.drawArc(arcRect, start + 0.6f, (end - start) - 1.2f, false, stroke)
        }
        stroke.alpha = 255
    }

    private fun drawTicks(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        r: Float,
        state: DialState,
        theme: DialTheme,
    ) {
        val outer = r * 0.885f
        stroke.strokeCap = Paint.Cap.ROUND

        var value = DialScale.MIN_HPA
        while (value <= DialScale.MAX_HPA + 1e-6) {
            val isMajor = value % DialScale.MAJOR_STEP == 0.0
            val isMedium = value % DialScale.MEDIUM_STEP == 0.0
            if (state.compact && !isMedium) {
                value += DialScale.MINOR_STEP
                continue
            }
            val length = when {
                isMajor -> r * 0.075f
                isMedium -> r * 0.05f
                else -> r * 0.03f
            }
            stroke.color = if (isMajor) theme.tickMajor else theme.tick
            stroke.strokeWidth = if (isMajor) r * 0.013f else r * 0.007f
            drawRadialLine(canvas, cx, cy, DialScale.angleFor(value), outer - length, outer, stroke)
            value += DialScale.MINOR_STEP
        }

        // Major numbers, always upright so they stay readable at a glance.
        text.typeface = medium
        text.color = theme.scaleLabel
        text.textSize = r * (if (state.compact) 0.115f else 0.098f)
        text.letterSpacing = 0.02f
        val labelRadius = r * 0.745f
        var major = DialScale.MIN_HPA
        while (major <= DialScale.MAX_HPA + 1e-6) {
            // The 950 and 1050 labels sit on top of each other at the gap, so the
            // low end is dropped and the high end kept.
            if (major > DialScale.MIN_HPA) {
                val angle = DialScale.angleFor(major).toRadians()
                val x = cx + labelRadius * cos(angle)
                val y = cy + labelRadius * sin(angle)
                canvas.drawText(major.toInt().toString(), x, y - (text.descent() + text.ascent()) / 2f, text)
            }
            major += if (state.compact) DialScale.MAJOR_STEP * 2 else DialScale.MAJOR_STEP
        }
        text.letterSpacing = 0f
    }

    /** STORMY … VERY DRY, set tangentially like the lettering on a weather glass. */
    private fun drawZoneLettering(canvas: Canvas, cx: Float, cy: Float, r: Float, theme: DialTheme) {
        text.typeface = medium
        text.color = theme.zoneLabel
        text.textSize = r * 0.072f
        text.letterSpacing = 0.14f
        val radius = r * 0.615f
        for (zone in DialScale.Zone.entries) {
            val midHpa = (zone.startHpa + zone.endHpa) / 2.0
            val angle = DialScale.angleFor(midHpa)
            canvas.save()
            canvas.translate(
                cx + radius * cos(angle.toRadians()),
                cy + radius * sin(angle.toRadians()),
            )
            var rotation = angle + 90f
            // Keep words the right way up on the lower flanks of the dial.
            if (rotation.normalisedDegrees() in 90f..270f) rotation += 180f
            canvas.rotate(rotation)
            canvas.drawText(zone.label, 0f, -(text.descent() + text.ascent()) / 2f, text)
            canvas.restore()
        }
        text.letterSpacing = 0f
    }

    /** The secondary inches-of-mercury ring that dual-scale barometers carry. */
    private fun drawInchesScale(canvas: Canvas, cx: Float, cy: Float, r: Float, theme: DialTheme) {
        val outer = r * 0.50f
        stroke.color = theme.tick
        stroke.strokeCap = Paint.Cap.ROUND
        text.typeface = regular
        text.color = theme.zoneLabel
        text.textSize = r * 0.062f

        var inches = 28.0
        while (inches <= 31.0 + 1e-6) {
            val hPa = PressureUnit.INCHES_HG.toHpa(inches)
            if (hPa in DialScale.MIN_HPA..DialScale.MAX_HPA) {
                val isLabelled = (inches * 10).toInt() % 10 == 0
                val angle = DialScale.angleFor(hPa)
                val length = if (isLabelled) r * 0.042f else r * 0.022f
                stroke.strokeWidth = if (isLabelled) r * 0.009f else r * 0.005f
                drawRadialLine(canvas, cx, cy, angle, outer - length, outer, stroke)
                if (isLabelled) {
                    val labelRadius = outer - length - r * 0.055f
                    val radians = angle.toRadians()
                    canvas.drawText(
                        inches.toInt().toString(),
                        cx + labelRadius * cos(radians),
                        cy + labelRadius * sin(radians) - (text.descent() + text.ascent()) / 2f,
                        text,
                    )
                }
            }
            inches += 0.1
        }
    }

    private fun drawBrand(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        r: Float,
        state: DialState,
        theme: DialTheme,
    ) {
        val brand = state.brand ?: return
        text.typeface = medium
        text.color = theme.caption
        text.textSize = r * 0.058f
        text.letterSpacing = 0.3f
        canvas.drawText(brand, cx, cy - r * 0.30f, text)
        text.letterSpacing = 0f

        text.typeface = regular
        text.textSize = r * 0.05f
        canvas.drawText("HPA / MILLIBARS", cx, cy - r * 0.205f, text)
    }

    /** A faint arc between where the needle was three hours ago and where it is now. */
    private fun drawTravelArc(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        r: Float,
        pressure: Double,
        reference: Double?,
        theme: DialTheme,
    ) {
        if (reference == null) return
        val from = DialScale.angleFor(reference)
        val to = DialScale.angleFor(pressure)
        if (kotlin.math.abs(to - from) < 0.4f) return
        val radius = r * 0.845f
        arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius)
        stroke.color = theme.accent
        stroke.alpha = 190
        stroke.strokeWidth = r * 0.022f
        stroke.strokeCap = Paint.Cap.ROUND
        canvas.drawArc(arcRect, from, to - from, false, stroke)
        stroke.alpha = 255
    }

    private fun drawSetHand(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        r: Float,
        reference: Double,
        theme: DialTheme,
    ) {
        val angle = DialScale.angleFor(reference)
        stroke.color = theme.setHand
        stroke.strokeWidth = r * 0.012f
        stroke.strokeCap = Paint.Cap.ROUND
        drawRadialLine(canvas, cx, cy, angle, r * 0.08f, r * 0.80f, stroke)

        val radians = angle.toRadians()
        fill.color = theme.setHand
        canvas.drawCircle(
            cx + r * 0.80f * cos(radians),
            cy + r * 0.80f * sin(radians),
            r * 0.022f,
            fill,
        )
    }

    private fun drawNeedle(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        r: Float,
        pressure: Double,
        theme: DialTheme,
    ) {
        val tip = r * 0.815f
        val tail = r * 0.17f
        val halfBase = r * 0.026f
        val halfTail = r * 0.015f

        canvas.save()
        canvas.rotate(DialScale.angleFor(pressure), cx, cy)

        needlePath.reset()
        needlePath.moveTo(cx + tip, cy)
        needlePath.lineTo(cx + tip * 0.12f, cy + halfBase)
        needlePath.lineTo(cx - tail, cy + halfTail)
        needlePath.lineTo(cx - tail, cy - halfTail)
        needlePath.lineTo(cx + tip * 0.12f, cy - halfBase)
        needlePath.close()

        fill.color = theme.needle
        canvas.drawPath(needlePath, fill)

        fill.color = theme.needleTail
        canvas.drawCircle(cx - tail, cy, r * 0.032f, fill)
        canvas.restore()
    }

    private fun drawHub(canvas: Canvas, cx: Float, cy: Float, r: Float, theme: DialTheme) {
        fill.color = theme.needle
        canvas.drawCircle(cx, cy, r * 0.052f, fill)
        fill.color = theme.hub
        canvas.drawCircle(cx, cy, r * 0.022f, fill)
    }

    /** The digital readout, sitting in the gap the scale leaves at the bottom. */
    private fun drawReadout(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        r: Float,
        state: DialState,
        theme: DialTheme,
    ) {
        val pressure = state.pressureHpa
        text.typeface = light
        text.color = theme.readout
        text.textSize = r * (if (state.compact) 0.26f else 0.225f)
        val value = pressure?.let { state.unit.format(it) } ?: "--"
        canvas.drawText(value, cx, cy + r * 0.47f, text)

        text.typeface = regular
        text.color = theme.caption
        text.textSize = r * (if (state.compact) 0.10f else 0.082f)
        val subtitle = when {
            pressure == null -> state.emptyMessage
            state.trendLabel != null ->
                listOfNotNull(state.unit.symbol, state.trendArrow, state.trendLabel).joinToString(" ")
            else -> state.unit.symbol
        }
        canvas.drawText(subtitle, cx, cy + r * 0.62f, text)

        val caption = state.caption
        if (caption != null && !state.compact) {
            text.textSize = r * 0.062f
            canvas.drawText(caption, cx, cy + r * 0.735f, text)
        }
    }

    private fun drawRadialLine(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        angleDegrees: Float,
        innerRadius: Float,
        outerRadius: Float,
        paint: Paint,
    ) {
        val radians = angleDegrees.toRadians()
        val cosA = cos(radians)
        val sinA = sin(radians)
        canvas.drawLine(
            cx + innerRadius * cosA,
            cy + innerRadius * sinA,
            cx + outerRadius * cosA,
            cy + outerRadius * sinA,
            paint,
        )
    }

    private fun Float.toRadians(): Float = (this * Math.PI / 180.0).toFloat()

    private fun Float.normalisedDegrees(): Float = ((this % 360f) + 360f) % 360f
}
