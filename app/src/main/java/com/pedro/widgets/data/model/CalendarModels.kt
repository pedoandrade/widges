package com.pedro.widgets.data.model

enum class CalendarViewMode {
    AGENDA_LIST,
    NEXT_EVENT
}

data class CalendarEvent(
    val id: Long,
    val title: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val isAllDay: Boolean,
    val location: String?,
    val calendarColor: Int,
    val calendarName: String
)

data class CalendarWidgetConfig(
    val widgetId: Int = 0,
    val useDynamicColors: Boolean = true,
    val backgroundColor: Long = 0xFF1C1B1FL,
    val backgroundAlpha: Float = 0.85f,
    val cornerRadiusDp: Int = 24,
    val borderWidthDp: Int = 1,
    val borderColor: Long = 0x33FFFFFFL,
    val viewMode: CalendarViewMode = CalendarViewMode.AGENDA_LIST,
    val maxEvents: Int = 5,
    val showPastEventsToday: Boolean = false
)
