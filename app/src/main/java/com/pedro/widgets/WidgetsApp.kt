package com.pedro.widgets

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.pedro.widgets.data.storage.WidgetPreferences
import com.pedro.widgets.widget.calendar.CalendarWidget
import com.pedro.widgets.widget.photo.PhotoWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WidgetsApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var calendarObserver: ContentObserver? = null

    override fun onCreate() {
        super.onCreate()
        registerCalendarObserver()
        registerUserPresentReceiver()
    }

    private fun registerCalendarObserver() {
        try {
            val handler = Handler(Looper.getMainLooper())
            calendarObserver = object : ContentObserver(handler) {
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)
                    Log.d("WidgetsApp", "Agenda alterada no sistema, atualizando widgets...")
                    refreshCalendarWidgets()
                }
            }

            contentResolver.registerContentObserver(
                CalendarContract.Events.CONTENT_URI,
                true,
                calendarObserver!!
            )
        } catch (e: Exception) {
            Log.e("WidgetsApp", "Falha ao registrar observer de calendário", e)
        }
    }

    private fun registerUserPresentReceiver() {
        try {
            val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
            registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action == Intent.ACTION_USER_PRESENT) {
                        Log.d("WidgetsApp", "Dispositivo desbloqueado (USER_PRESENT)")
                        rotatePhotoWidgetsOnUnlock()
                        refreshCalendarWidgets()
                    }
                }
            }, filter)
        } catch (e: Exception) {
            Log.e("WidgetsApp", "Falha ao registrar receiver de desbloqueio", e)
        }
    }

    fun refreshCalendarWidgets() {
        applicationScope.launch {
            try {
                val glanceManager = GlanceAppWidgetManager(this@WidgetsApp)
                val glanceIds = glanceManager.getGlanceIds(CalendarWidget::class.java)
                glanceIds.forEach { id ->
                    CalendarWidget().update(this@WidgetsApp, id)
                }
            } catch (e: Exception) {
                Log.e("WidgetsApp", "Erro ao atualizar CalendarWidget", e)
            }
        }
    }

    private fun rotatePhotoWidgetsOnUnlock() {
        applicationScope.launch {
            try {
                val prefs = WidgetPreferences(this@WidgetsApp)
                val glanceManager = GlanceAppWidgetManager(this@WidgetsApp)
                val glanceIds = glanceManager.getGlanceIds(PhotoWidget::class.java)

                glanceIds.forEach { glanceId ->
                    val appWidgetId = glanceManager.getAppWidgetId(glanceId)
                    val config = prefs.getPhotoConfig(appWidgetId)
                    if (config.photoPaths.size > 1 && config.changeOnUnlock) {
                        val nextIndex = (config.currentIndex + 1) % config.photoPaths.size
                        prefs.savePhotoConfig(config.copy(currentIndex = nextIndex))
                        PhotoWidget().update(this@WidgetsApp, glanceId)
                    }
                }
            } catch (e: Exception) {
                Log.e("WidgetsApp", "Erro ao rotacionar fotos no desbloqueio", e)
            }
        }
    }
}
