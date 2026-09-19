package com.normola.barodroid.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

/** One place to poke every widget the app publishes. */
object BaroWidgets {

    suspend fun updateAll(context: Context) {
        runCatching { BaroDialWidget().updateAll(context) }
        runCatching { BaroStripWidget().updateAll(context) }
    }
}
