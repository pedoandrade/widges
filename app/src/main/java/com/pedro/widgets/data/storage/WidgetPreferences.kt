package com.pedro.widgets.data.storage

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pedro.widgets.data.model.CalendarViewMode
import com.pedro.widgets.data.model.CalendarWidgetConfig
import com.pedro.widgets.data.model.PhotoShape
import com.pedro.widgets.data.model.PhotoWidgetConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "widget_preferences")

class WidgetPreferences(private val context: Context) {

    // --- Calendar Config ---

    suspend fun saveCalendarConfig(config: CalendarWidgetConfig) {
        val id = config.widgetId
        context.dataStore.edit { prefs ->
            prefs[booleanPreferencesKey("cal_dynamic_$id")] = config.useDynamicColors
            prefs[longPreferencesKey("cal_bg_color_$id")] = config.backgroundColor
            prefs[floatPreferencesKey("cal_bg_alpha_$id")] = config.backgroundAlpha
            prefs[intPreferencesKey("cal_corner_radius_$id")] = config.cornerRadiusDp
            prefs[intPreferencesKey("cal_border_width_$id")] = config.borderWidthDp
            prefs[longPreferencesKey("cal_border_color_$id")] = config.borderColor
            prefs[stringPreferencesKey("cal_view_mode_$id")] = config.viewMode.name
            prefs[intPreferencesKey("cal_max_events_$id")] = config.maxEvents
            prefs[booleanPreferencesKey("cal_show_past_$id")] = config.showPastEventsToday
        }
    }

    suspend fun getCalendarConfig(widgetId: Int): CalendarWidgetConfig {
        return calendarConfigFlow(widgetId).first()
    }

    /**
     * Fluxo reativo da configuração. O widget o observa para refletir ajustes (opacidade,
     * cores, formato) imediatamente, sem depender de uma nova sessão do Glance.
     */
    fun calendarConfigFlow(widgetId: Int): Flow<CalendarWidgetConfig> {
        return context.dataStore.data
            .map { prefs -> prefs.toCalendarConfig(widgetId) }
            .distinctUntilChanged()
    }

    private fun Preferences.toCalendarConfig(widgetId: Int): CalendarWidgetConfig {
        val dynamic = this[booleanPreferencesKey("cal_dynamic_$widgetId")] ?: true
        val bgColor = this[longPreferencesKey("cal_bg_color_$widgetId")] ?: 0xFF1C1B1FL
        val bgAlpha = this[floatPreferencesKey("cal_bg_alpha_$widgetId")] ?: 0.85f
        val cornerRadius = this[intPreferencesKey("cal_corner_radius_$widgetId")] ?: 24
        val borderWidth = this[intPreferencesKey("cal_border_width_$widgetId")] ?: 1
        val borderColor = this[longPreferencesKey("cal_border_color_$widgetId")] ?: 0x33FFFFFFL
        val viewModeStr = this[stringPreferencesKey("cal_view_mode_$widgetId")]
        val viewMode = if (viewModeStr != null) {
            try { CalendarViewMode.valueOf(viewModeStr) } catch (e: Exception) { CalendarViewMode.AGENDA_LIST }
        } else CalendarViewMode.AGENDA_LIST
        val maxEvents = this[intPreferencesKey("cal_max_events_$widgetId")] ?: 5
        val showPast = this[booleanPreferencesKey("cal_show_past_$widgetId")] ?: false

        return CalendarWidgetConfig(
            widgetId = widgetId,
            useDynamicColors = dynamic,
            backgroundColor = bgColor,
            backgroundAlpha = bgAlpha,
            cornerRadiusDp = cornerRadius,
            borderWidthDp = borderWidth,
            borderColor = borderColor,
            viewMode = viewMode,
            maxEvents = maxEvents,
            showPastEventsToday = showPast
        )
    }

    // --- Photo Config ---

    suspend fun savePhotoConfig(config: PhotoWidgetConfig) {
        val id = config.widgetId
        context.dataStore.edit { prefs ->
            prefs[stringPreferencesKey("photo_paths_$id")] = config.photoPaths.joinToString(";")
            prefs[intPreferencesKey("photo_current_idx_$id")] = config.currentIndex
            prefs[stringPreferencesKey("photo_shape_$id")] = config.shape.name
            prefs[intPreferencesKey("photo_corner_radius_$id")] = config.cornerRadiusDp
            prefs[intPreferencesKey("photo_border_width_$id")] = config.borderWidthDp
            prefs[longPreferencesKey("photo_border_color_$id")] = config.borderColor
            prefs[booleanPreferencesKey("photo_dynamic_$id")] = config.useDynamicColors
            prefs[intPreferencesKey("photo_padding_$id")] = config.paddingDp
            prefs[longPreferencesKey("photo_frame_color_$id")] = config.frameColor
            prefs[booleanPreferencesKey("photo_change_unlock_$id")] = config.changeOnUnlock
            prefs[intPreferencesKey("photo_rotation_interval_$id")] = config.rotationIntervalMinutes
        }
    }

    suspend fun getPhotoConfig(widgetId: Int): PhotoWidgetConfig {
        return context.dataStore.data.map { prefs ->
            val pathsStr = prefs[stringPreferencesKey("photo_paths_$widgetId")] ?: ""
            val paths = if (pathsStr.isBlank()) emptyList() else pathsStr.split(";").filter { it.isNotBlank() }
            val currentIndex = prefs[intPreferencesKey("photo_current_idx_$widgetId")] ?: 0
            val shapeStr = prefs[stringPreferencesKey("photo_shape_$widgetId")]
            val shape = if (shapeStr != null) {
                try { PhotoShape.valueOf(shapeStr) } catch (e: Exception) { PhotoShape.ROUNDED_RECTANGLE }
            } else PhotoShape.ROUNDED_RECTANGLE
            val cornerRadius = prefs[intPreferencesKey("photo_corner_radius_$widgetId")] ?: 24
            val borderWidth = prefs[intPreferencesKey("photo_border_width_$widgetId")] ?: 0
            val borderColor = prefs[longPreferencesKey("photo_border_color_$widgetId")] ?: 0xFFFFFFFFL
            val dynamic = prefs[booleanPreferencesKey("photo_dynamic_$widgetId")] ?: false
            val padding = prefs[intPreferencesKey("photo_padding_$widgetId")] ?: 0
            val frameColor = prefs[longPreferencesKey("photo_frame_color_$widgetId")] ?: 0xFF1C1B1FL
            val changeOnUnlock = prefs[booleanPreferencesKey("photo_change_unlock_$widgetId")] ?: true
            val rotationInterval = prefs[intPreferencesKey("photo_rotation_interval_$widgetId")] ?: 0

            PhotoWidgetConfig(
                widgetId = widgetId,
                photoPaths = paths,
                currentIndex = currentIndex,
                shape = shape,
                cornerRadiusDp = cornerRadius,
                borderWidthDp = borderWidth,
                borderColor = borderColor,
                useDynamicColors = dynamic,
                paddingDp = padding,
                frameColor = frameColor,
                changeOnUnlock = changeOnUnlock,
                rotationIntervalMinutes = rotationInterval
            )
        }.first()
    }
}
