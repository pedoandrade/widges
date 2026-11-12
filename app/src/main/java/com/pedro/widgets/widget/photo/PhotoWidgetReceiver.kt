package com.pedro.widgets.widget.photo

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.pedro.widgets.data.storage.WidgetPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class PhotoWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = PhotoWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        Log.d("PhotoWidgetReceiver", "Action recebida: $action")

        if (action == ACTION_ROTATE_PHOTO || action == Intent.ACTION_USER_PRESENT) {
            val appWidgetId = intent.getIntExtra(EXTRA_APPWIDGET_ID, -1)
            CoroutineScope(Dispatchers.IO).launch {
                val prefs = WidgetPreferences(context)
                val glanceManager = GlanceAppWidgetManager(context)

                if (appWidgetId != -1) {
                    rotateWidget(context, prefs, glanceManager, appWidgetId)
                } else {
                    val glanceIds = glanceManager.getGlanceIds(PhotoWidget::class.java)
                    glanceIds.forEach { id ->
                        val widgetId = glanceManager.getAppWidgetId(id)
                        rotateWidget(context, prefs, glanceManager, widgetId)
                    }
                }
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        // Remove as cópias das fotos quando o widget é excluído
        CoroutineScope(Dispatchers.IO).launch {
            appWidgetIds.forEach { widgetId ->
                File(context.filesDir, "widget_photos/$widgetId").deleteRecursively()
            }
        }
    }

    private suspend fun rotateWidget(
        context: Context,
        prefs: WidgetPreferences,
        glanceManager: GlanceAppWidgetManager,
        widgetId: Int
    ) {
        val config = prefs.getPhotoConfig(widgetId)
        if (config.photoPaths.size > 1) {
            val nextIndex = (config.currentIndex + 1) % config.photoPaths.size
            prefs.savePhotoConfig(config.copy(currentIndex = nextIndex))
            val glanceId = glanceManager.getGlanceIdBy(widgetId)
            PhotoWidget().update(context, glanceId)
        }
    }

    companion object {
        const val ACTION_ROTATE_PHOTO = "com.pedro.widgets.ACTION_ROTATE_PHOTO"
        const val EXTRA_APPWIDGET_ID = "appWidgetId"

        fun schedulePhotoRotation(context: Context, widgetId: Int, intervalMinutes: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, PhotoWidgetReceiver::class.java).apply {
                action = ACTION_ROTATE_PHOTO
                putExtra(EXTRA_APPWIDGET_ID, widgetId)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                widgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (intervalMinutes > 0) {
                val intervalMillis = intervalMinutes * 60 * 1000L
                alarmManager.setInexactRepeating(
                    AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime() + intervalMillis,
                    intervalMillis,
                    pendingIntent
                )
                Log.d("PhotoWidgetReceiver", "Alarme agendado para widget $widgetId a cada $intervalMinutes min")
            } else {
                alarmManager.cancel(pendingIntent)
                Log.d("PhotoWidgetReceiver", "Alarme cancelado para widget $widgetId")
            }
        }
    }
}
