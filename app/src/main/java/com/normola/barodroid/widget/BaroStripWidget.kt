package com.normola.barodroid.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import com.normola.barodroid.MainActivity
import com.normola.barodroid.render.StripRenderer
import com.normola.barodroid.render.StripState

/**
 * The wide widget: current pressure, tendency and the last 24 hours as a trace.
 */
class BaroStripWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetSupport.load(context)
        provideContent { Content(data) }
    }

    @Composable
    private fun Content(data: WidgetSupport.WidgetData) {
        val context = LocalContext.current
        val size = LocalSize.current
        val snapshot = data.snapshot
        val unit = data.settings.unit
        val trend = snapshot.trend.takeIf { it.isReliable }

        val bitmap = WidgetSupport.bitmap(context, size.width.value, size.height.value) { canvas, width, height ->
            StripRenderer().draw(
                canvas,
                width,
                height,
                StripState(
                    pressureHpa = snapshot.displayHpa,
                    samples = snapshot.samples,
                    unit = unit,
                    trendLabel = trend?.trend?.label,
                    trendArrow = trend?.trend?.arrow,
                    deltaLabel = trend?.let { "${unit.formatDelta(it.deltaPer3h)}/3h" },
                    forecast = snapshot.forecast?.text,
                    caption = WidgetSupport.caption(snapshot),
                ),
                WidgetSupport.theme(context),
            )
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .clickable(actionStartActivity<MainActivity>()),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = contentDescription(data),
                contentScale = ContentScale.Fit,
                modifier = GlanceModifier.fillMaxSize(),
            )
        }
    }

    private fun contentDescription(data: WidgetSupport.WidgetData): String {
        val snapshot = data.snapshot
        val pressure = snapshot.displayHpa ?: return "Barometer: no reading yet"
        val forecast = snapshot.forecast?.text.orEmpty()
        return "Barometer: ${data.settings.unit.formatWithSymbol(pressure)}. $forecast".trim()
    }
}

class BaroStripWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BaroStripWidget()
}
