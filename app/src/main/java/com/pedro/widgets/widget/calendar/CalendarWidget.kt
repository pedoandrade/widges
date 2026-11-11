package com.pedro.widgets.widget.calendar

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.pedro.widgets.MainActivity
import com.pedro.widgets.data.model.CalendarEvent
import com.pedro.widgets.data.model.CalendarViewMode
import com.pedro.widgets.data.model.CalendarWidgetConfig
import com.pedro.widgets.data.repository.CalendarRepository
import com.pedro.widgets.data.storage.WidgetPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CalendarWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val prefs = WidgetPreferences(context)
        val config = prefs.getCalendarConfig(appWidgetId)
        val repository = CalendarRepository(context)

        val hasPermission = repository.hasCalendarPermission()
        val events = if (hasPermission) {
            repository.getUpcomingEvents(
                maxEvents = config.maxEvents,
                includePastToday = true
            )
        } else {
            emptyList()
        }

        provideContent {
            GlanceTheme {
                CalendarWidgetContent(
                    context = context,
                    config = config,
                    events = events,
                    hasPermission = hasPermission
                )
            }
        }
    }
}

class RefreshCalendarActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        CalendarWidget().update(context, glanceId)
    }
}

@Composable
fun CalendarWidgetContent(
    context: Context,
    config: CalendarWidgetConfig,
    events: List<CalendarEvent>,
    hasPermission: Boolean
) {
    val backgroundColor = if (config.useDynamicColors) {
        GlanceTheme.colors.surface.getColor(context).copy(alpha = config.backgroundAlpha)
    } else {
        Color(config.backgroundColor).copy(alpha = config.backgroundAlpha)
    }

    val primaryTextColor = if (config.useDynamicColors) {
        GlanceTheme.colors.onSurface
    } else {
        androidx.glance.unit.ColorProvider(Color.White)
    }

    val secondaryTextColor = if (config.useDynamicColors) {
        GlanceTheme.colors.onSurfaceVariant
    } else {
        androidx.glance.unit.ColorProvider(Color(0xFFB0B0B0))
    }

    val accentColor = if (config.useDynamicColors) {
        GlanceTheme.colors.primary
    } else {
        androidx.glance.unit.ColorProvider(Color(0xFFD0BCFF))
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(config.cornerRadiusDp.dp)
            .background(backgroundColor)
            .padding(14.dp)
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize()
        ) {
            // Cabeçalho com data atual e botão de atualizar
            CalendarHeader(
                primaryTextColor = primaryTextColor,
                secondaryTextColor = secondaryTextColor,
                accentColor = accentColor
            )

            Spacer(modifier = GlanceModifier.height(10.dp))

            when {
                !hasPermission -> {
                    PermissionNotice(accentColor = accentColor)
                }
                events.isEmpty() -> {
                    EmptyEventsNotice(
                        secondaryTextColor = secondaryTextColor,
                        accentColor = accentColor
                    )
                }
                config.viewMode == CalendarViewMode.NEXT_EVENT -> {
                    NextEventView(
                        event = events.first(),
                        primaryTextColor = primaryTextColor,
                        secondaryTextColor = secondaryTextColor,
                        accentColor = accentColor
                    )
                }
                else -> {
                    AgendaListView(
                        events = events,
                        primaryTextColor = primaryTextColor,
                        secondaryTextColor = secondaryTextColor
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarHeader(
    primaryTextColor: androidx.glance.unit.ColorProvider,
    secondaryTextColor: androidx.glance.unit.ColorProvider,
    accentColor: androidx.glance.unit.ColorProvider
) {
    val now = Date()
    val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale("pt", "BR"))
    val dayAndMonthFormat = SimpleDateFormat("d 'de' MMMM", Locale("pt", "BR"))

    val dayOfWeek = dayOfWeekFormat.format(now).replaceFirstChar { it.uppercase() }
    val dayAndMonth = dayAndMonthFormat.format(now)

    val openCalendarIntent = Intent(Intent.ACTION_VIEW).apply {
        data = CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build()
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Indicador vertical colorido
        Box(
            modifier = GlanceModifier
                .width(4.dp)
                .height(28.dp)
                .cornerRadius(2.dp)
                .background(accentColor)
        ) {}

        Spacer(modifier = GlanceModifier.width(8.dp))

        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .clickable(actionStartActivity(openCalendarIntent))
        ) {
            Text(
                text = dayOfWeek,
                style = TextStyle(
                    color = primaryTextColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = dayAndMonth,
                style = TextStyle(
                    color = secondaryTextColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            )
        }

        // Botão de Atualizar / Sincronizar
        Box(
            modifier = GlanceModifier
                .cornerRadius(14.dp)
                .padding(6.dp)
                .clickable(actionRunCallback<RefreshCalendarActionCallback>())
        ) {
            Text(
                text = "🔄",
                style = TextStyle(fontSize = 14.sp)
            )
        }
    }
}

@Composable
private fun AgendaListView(
    events: List<CalendarEvent>,
    primaryTextColor: androidx.glance.unit.ColorProvider,
    secondaryTextColor: androidx.glance.unit.ColorProvider
) {
    LazyColumn(
        modifier = GlanceModifier.fillMaxSize()
    ) {
        items(events) { event ->
            EventRowItem(
                event = event,
                primaryTextColor = primaryTextColor,
                secondaryTextColor = secondaryTextColor
            )
            Spacer(modifier = GlanceModifier.height(6.dp))
        }
    }
}

@Composable
private fun EventRowItem(
    event: CalendarEvent,
    primaryTextColor: androidx.glance.unit.ColorProvider,
    secondaryTextColor: androidx.glance.unit.ColorProvider
) {
    val eventIntent = Intent(Intent.ACTION_VIEW).apply {
        data = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.id)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    val timeText = if (event.isAllDay) {
        "Dia todo"
    } else {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val start = timeFormat.format(Date(event.startTimeMillis))
        val end = timeFormat.format(Date(event.endTimeMillis))
        "$start - $end"
    }

    val dayLabel = formatRelativeDay(event.startTimeMillis)

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(actionStartActivity(eventIntent))
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = GlanceModifier
                .size(10.dp)
                .cornerRadius(5.dp)
                .background(Color(event.calendarColor))
        ) {}

        Spacer(modifier = GlanceModifier.width(8.dp))

        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = event.title,
                maxLines = 1,
                style = TextStyle(
                    color = primaryTextColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            Text(
                text = if (dayLabel.isNotEmpty()) "$dayLabel • $timeText" else timeText,
                maxLines = 1,
                style = TextStyle(
                    color = secondaryTextColor,
                    fontSize = 11.sp
                )
            )
        }
    }
}

@Composable
private fun NextEventView(
    event: CalendarEvent,
    primaryTextColor: androidx.glance.unit.ColorProvider,
    secondaryTextColor: androidx.glance.unit.ColorProvider,
    accentColor: androidx.glance.unit.ColorProvider
) {
    val eventIntent = Intent(Intent.ACTION_VIEW).apply {
        data = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.id)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val start = timeFormat.format(Date(event.startTimeMillis))
    val end = timeFormat.format(Date(event.endTimeMillis))
    val timeRange = if (event.isAllDay) "Dia todo" else "$start até $end"
    val dayLabel = formatRelativeDay(event.startTimeMillis)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity(eventIntent))
    ) {
        Column {
            Text(
                text = if (dayLabel.isNotEmpty()) "PRÓXIMO ($dayLabel)" else "PRÓXIMO COMPROMISSO",
                style = TextStyle(
                    color = accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = event.title,
                maxLines = 2,
                style = TextStyle(
                    color = primaryTextColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = timeRange,
                style = TextStyle(
                    color = secondaryTextColor,
                    fontSize = 13.sp
                )
            )
            if (!event.location.isNullOrBlank()) {
                Spacer(modifier = GlanceModifier.height(2.dp))
                Text(
                    text = "📍 ${event.location}",
                    maxLines = 1,
                    style = TextStyle(
                        color = secondaryTextColor,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun EmptyEventsNotice(
    secondaryTextColor: androidx.glance.unit.ColorProvider,
    accentColor: androidx.glance.unit.ColorProvider
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionRunCallback<RefreshCalendarActionCallback>()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Sem eventos nos próximos dias",
                style = TextStyle(
                    color = secondaryTextColor,
                    fontSize = 12.sp
                )
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = "Toque para sincronizar 🔄",
                style = TextStyle(
                    color = accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun PermissionNotice(accentColor: androidx.glance.unit.ColorProvider) {
    val mainIntent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity(mainIntent)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Permissão de Agenda necessária",
                style = TextStyle(
                    color = accentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = "Toque para abrir e autorizar no aplicativo",
                style = TextStyle(
                    fontSize = 11.sp
                )
            )
        }
    }
}

private fun formatRelativeDay(timeMillis: Long): String {
    val eventCal = Calendar.getInstance().apply { timeInMillis = timeMillis }
    val nowCal = Calendar.getInstance()

    val sameDay = eventCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
            eventCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)

    if (sameDay) return "Hoje"

    nowCal.add(Calendar.DAY_OF_YEAR, 1)
    val tomorrow = eventCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
            eventCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)

    if (tomorrow) return "Amanhã"

    val dayFormat = SimpleDateFormat("EEE, d MMM", Locale("pt", "BR"))
    return dayFormat.format(Date(timeMillis))
}
