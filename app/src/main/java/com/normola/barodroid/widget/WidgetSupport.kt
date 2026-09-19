package com.normola.barodroid.widget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import com.normola.barodroid.BaroGraph
import com.normola.barodroid.data.BaroSettings
import com.normola.barodroid.domain.BarometerSnapshot
import com.normola.barodroid.render.DialTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** Shared plumbing for the widgets: current data, colours and bitmap sizing. */
internal object WidgetSupport {

    /** Keeps widget bitmaps inside the launcher's RemoteViews budget. */
    private const val MAX_DIMENSION_PX = 900

    data class WidgetData(val snapshot: BarometerSnapshot, val settings: BaroSettings)

    /**
     * Loads history, and tries for a fresh reading — which only arrives when the
     * app is in the foreground or background logging is running, since Android
     * cuts background apps off from continuous sensors.
     */
    suspend fun load(context: Context, allowSensorRead: Boolean = true): WidgetData {
        val settings = BaroGraph.settings(context).current()
        val history = BaroGraph.history(context)
        val sensor = BaroGraph.sensor(context)

        val live = if (allowSensorRead && sensor.isAvailable) {
            sensor.readOnce(timeoutMillis = 2_500L)?.also { history.record(it) }
        } else {
            null
        }
        val samples = history.load()
        return WidgetData(BarometerSnapshot.build(samples, live, settings), settings)
    }

    fun isNight(context: Context): Boolean =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    fun theme(context: Context): DialTheme =
        if (isNight(context)) DialTheme.Dark else DialTheme.Light

    /** Creates a bitmap sized in dp and draws [block] into it using px units. */
    fun bitmap(
        context: Context,
        widthDp: Float,
        heightDp: Float,
        block: (Canvas, Float, Float) -> Unit,
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val width = (widthDp * density).roundToInt().coerceIn(1, MAX_DIMENSION_PX)
        val height = (heightDp * density).roundToInt().coerceIn(1, MAX_DIMENSION_PX)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        block(Canvas(bitmap), width.toFloat(), height.toFloat())
        return bitmap
    }

    fun caption(snapshot: BarometerSnapshot): String? {
        if (!snapshot.hasReading || snapshot.updatedAt <= 0L) return null
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        return formatter.format(Date(snapshot.updatedAt))
    }
}
