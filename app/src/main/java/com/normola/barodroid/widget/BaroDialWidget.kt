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
import com.normola.barodroid.render.DialRenderer
import com.normola.barodroid.render.DialState

/**
 * The barometer face as a widget. The dial is drawn straight into a bitmap by
 * the same renderer the app uses, so the widget and the app never drift apart,
 * and it sits on the wallpaper without a panel behind it.
 */
class BaroDialWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetSupport.load(context)
        provideContent { Content(data) }
    }

    @Composable
    private fun Content(data: WidgetSupport.WidgetData) {
        val context = LocalContext.current
        val size = LocalSize.current
        val side = minOf(size.width.value, size.height.value)
        val compact = side < 150f

        val snapshot = data.snapshot
        val bitmap = WidgetSupport.bitmap(context, side, side) { canvas, width, height ->
            DialRenderer().draw(
                canvas,
                width,
                height,
                DialState(
                    pressureHpa = snapshot.displayHpa,
                    referenceHpa = snapshot.referenceHpa,
                    unit = data.settings.unit,
                    trendLabel = if (compact) null else snapshot.trend.takeIf { it.isReliable }?.trend?.label,
                    trendArrow = snapshot.trend.takeIf { it.isReliable }?.trend?.arrow,
                    caption = WidgetSupport.caption(snapshot),
                    brand = if (compact) null else "BARODROID",
                    compact = compact,
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
        val trend = snapshot.trend.takeIf { it.isReliable }?.trend?.label.orEmpty()
        return "Barometer: ${data.settings.unit.formatWithSymbol(pressure)} $trend".trim()
    }
}

class BaroDialWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BaroDialWidget()
}
