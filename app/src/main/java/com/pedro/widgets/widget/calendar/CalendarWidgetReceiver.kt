package com.pedro.widgets.widget.calendar

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CalendarWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = CalendarWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == "com.pedro.widgets.CALENDAR_UPDATED" ||
            intent.action == Intent.ACTION_PROVIDER_CHANGED) {
            CoroutineScope(Dispatchers.IO).launch {
                val glanceManager = GlanceAppWidgetManager(context)
                val glanceIds = glanceManager.getGlanceIds(CalendarWidget::class.java)
                glanceIds.forEach { id ->
                    CalendarWidget().update(context, id)
                }
            }
        }
    }
}
