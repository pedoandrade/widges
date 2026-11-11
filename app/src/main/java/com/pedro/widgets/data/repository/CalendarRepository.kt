package com.pedro.widgets.data.repository

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.pedro.widgets.data.model.CalendarEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

data class CalendarAccountInfo(
    val id: Long,
    val name: String,
    val accountName: String,
    val accountType: String,
    val color: Int,
    val isVisible: Boolean,
    val isSynced: Boolean
)

class CalendarRepository(private val context: Context) {

    private val tag = "CalendarRepository"

    fun hasCalendarPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Retorna a lista de contas de calendário sincronizadas no dispositivo.
     */
    suspend fun getCalendarAccounts(): List<CalendarAccountInfo> = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission()) return@withContext emptyList()

        val list = mutableListOf<CalendarAccountInfo>()
        val uri = CalendarContract.Calendars.CONTENT_URI
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.VISIBLE,
            CalendarContract.Calendars.SYNC_EVENTS
        )

        try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                val nameIdx = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val accNameIdx = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)
                val accTypeIdx = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_TYPE)
                val colorIdx = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_COLOR)
                val visIdx = cursor.getColumnIndex(CalendarContract.Calendars.VISIBLE)
                val syncIdx = cursor.getColumnIndex(CalendarContract.Calendars.SYNC_EVENTS)

                while (cursor.moveToNext()) {
                    val id = if (idIdx >= 0) cursor.getLong(idIdx) else 0L
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "Sem nome" else "Sem nome"
                    val accName = if (accNameIdx >= 0) cursor.getString(accNameIdx) ?: "" else ""
                    val accType = if (accTypeIdx >= 0) cursor.getString(accTypeIdx) ?: "" else ""
                    val color = if (colorIdx >= 0) cursor.getInt(colorIdx) else 0xFF4285F4.toInt()
                    val visible = if (visIdx >= 0) cursor.getInt(visIdx) == 1 else true
                    val synced = if (syncIdx >= 0) cursor.getInt(syncIdx) == 1 else true

                    list.add(
                        CalendarAccountInfo(
                            id = id,
                            name = name,
                            accountName = accName,
                            accountType = accType,
                            color = color,
                            isVisible = visible,
                            isSynced = synced
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Erro ao listar calendários", e)
        }
        list
    }

    suspend fun getUpcomingEvents(
        maxEvents: Int = 10,
        includePastToday: Boolean = true
    ): List<CalendarEvent> = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission()) {
            Log.w(tag, "Sem permissão READ_CALENDAR")
            return@withContext emptyList()
        }

        val events = mutableListOf<CalendarEvent>()

        val now = System.currentTimeMillis()
        // Começa sempre a partir das 00:00 de hoje para não perder nenhum evento do dia
        val startCal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = startCal.timeInMillis

        // Próximos 30 dias
        val endCal = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, 30)
        }
        val endTime = endCal.timeInMillis

        // Método 1: CalendarContract.Instances (expansão automática de eventos recorrentes)
        try {
            val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(builder, startTime)
            ContentUris.appendId(builder, endTime)

            // Usando projeção segura universalmente compatível
            val projection = arrayOf(
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.EVENT_LOCATION,
                CalendarContract.Instances.CALENDAR_COLOR
            )

            // SEM filtro SQL de status, para evitar rejeitar linhas onde status é NULL
            context.contentResolver.query(
                builder.build(),
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )?.use { cursor ->
                parseInstancesCursor(cursor, events, now, includePastToday, maxEvents)
            }
            Log.d(tag, "Instances retornou ${events.size} eventos")
        } catch (e: Exception) {
            Log.e(tag, "Erro na consulta de Instances, tentando fallback em Events", e)
        }

        // Método 2 (Fallback): se Instances retornou vazio ou falhou, consulta CalendarContract.Events diretamente
        if (events.isEmpty()) {
            try {
                val eventsUri = CalendarContract.Events.CONTENT_URI
                val projection = arrayOf(
                    CalendarContract.Events._ID,
                    CalendarContract.Events.TITLE,
                    CalendarContract.Events.DTSTART,
                    CalendarContract.Events.DTEND,
                    CalendarContract.Events.ALL_DAY,
                    CalendarContract.Events.EVENT_LOCATION,
                    CalendarContract.Events.CALENDAR_COLOR
                )

                val selection = "(${CalendarContract.Events.DELETED} = 0) AND " +
                        "((${CalendarContract.Events.DTSTART} >= ?) OR (${CalendarContract.Events.DTEND} >= ?))"
                val selectionArgs = arrayOf(startTime.toString(), startTime.toString())

                context.contentResolver.query(
                    eventsUri,
                    projection,
                    selection,
                    selectionArgs,
                    "${CalendarContract.Events.DTSTART} ASC"
                )?.use { cursor ->
                    parseEventsCursor(cursor, events, now, includePastToday, maxEvents)
                }
                Log.d(tag, "Fallback em Events retornou ${events.size} eventos")
            } catch (e: Exception) {
                Log.e(tag, "Erro no fallback de Events", e)
            }
        }

        // Ordenar por horário de início
        events.sortedBy { it.startTimeMillis }.take(maxEvents)
    }

    private fun parseInstancesCursor(
        cursor: Cursor,
        events: MutableList<CalendarEvent>,
        now: Long,
        includePastToday: Boolean,
        maxEvents: Int
    ) {
        val idIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID)
        val titleIdx = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
        val beginIdx = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
        val endIdx = cursor.getColumnIndex(CalendarContract.Instances.END)
        val allDayIdx = cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY)
        val locationIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)
        val colorIdx = cursor.getColumnIndex(CalendarContract.Instances.CALENDAR_COLOR)

        while (cursor.moveToNext() && events.size < maxEvents * 2) {
            val id = if (idIdx >= 0) cursor.getLong(idIdx) else 0L
            val rawTitle = if (titleIdx >= 0) cursor.getString(titleIdx) else null
            val title = if (rawTitle.isNullOrBlank()) "Compromisso" else rawTitle
            val begin = if (beginIdx >= 0) cursor.getLong(beginIdx) else 0L
            val end = if (endIdx >= 0) cursor.getLong(endIdx) else begin
            val isAllDay = if (allDayIdx >= 0) cursor.getInt(allDayIdx) == 1 else false
            val location = if (locationIdx >= 0) cursor.getString(locationIdx) else null
            val color = if (colorIdx >= 0) cursor.getInt(colorIdx) else 0xFF4285F4.toInt()

            // Se includePastToday for false e o evento já terminou antes de agora, pula
            if (!includePastToday && !isAllDay && end < now) {
                continue
            }

            events.add(
                CalendarEvent(
                    id = id,
                    title = title,
                    startTimeMillis = begin,
                    endTimeMillis = end,
                    isAllDay = isAllDay,
                    location = location,
                    calendarColor = color,
                    calendarName = ""
                )
            )
        }
    }

    private fun parseEventsCursor(
        cursor: Cursor,
        events: MutableList<CalendarEvent>,
        now: Long,
        includePastToday: Boolean,
        maxEvents: Int
    ) {
        val idIdx = cursor.getColumnIndex(CalendarContract.Events._ID)
        val titleIdx = cursor.getColumnIndex(CalendarContract.Events.TITLE)
        val dtstartIdx = cursor.getColumnIndex(CalendarContract.Events.DTSTART)
        val dtendIdx = cursor.getColumnIndex(CalendarContract.Events.DTEND)
        val allDayIdx = cursor.getColumnIndex(CalendarContract.Events.ALL_DAY)
        val locationIdx = cursor.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)
        val colorIdx = cursor.getColumnIndex(CalendarContract.Events.CALENDAR_COLOR)

        while (cursor.moveToNext() && events.size < maxEvents * 2) {
            val id = if (idIdx >= 0) cursor.getLong(idIdx) else 0L
            val rawTitle = if (titleIdx >= 0) cursor.getString(titleIdx) else null
            val title = if (rawTitle.isNullOrBlank()) "Compromisso" else rawTitle
            val begin = if (dtstartIdx >= 0) cursor.getLong(dtstartIdx) else 0L
            val end = if (dtendIdx >= 0) cursor.getLong(dtendIdx) else begin
            val isAllDay = if (allDayIdx >= 0) cursor.getInt(allDayIdx) == 1 else false
            val location = if (locationIdx >= 0) cursor.getString(locationIdx) else null
            val color = if (colorIdx >= 0) cursor.getInt(colorIdx) else 0xFF4285F4.toInt()

            if (!includePastToday && !isAllDay && end < now) {
                continue
            }

            events.add(
                CalendarEvent(
                    id = id,
                    title = title,
                    startTimeMillis = begin,
                    endTimeMillis = end,
                    isAllDay = isAllDay,
                    location = location,
                    calendarColor = color,
                    calendarName = ""
                )
            )
        }
    }
}
