package com.personalradar.app.calendar

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.personalradar.app.di.AppContainer
import com.personalradar.app.reminder.ReminderScheduleResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CalendarImportActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var preview: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildScreen())
        if (hasCalendarPermission()) {
            scanCalendar()
        } else {
            requestPermissions(arrayOf(Manifest.permission.READ_CALENDAR), READ_CALENDAR_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == READ_CALENDAR_REQUEST_CODE && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            scanCalendar()
        } else {
            status.text = "Доступ к календарю не выдан. Радар не будет читать события без разрешения владельца."
        }
    }

    private fun buildScreen(): LinearLayout {
        status = TextView(this).apply {
            text = "Готовлю чтение календаря..."
            textSize = 18f
            setPadding(20, 20, 20, 12)
        }
        preview = TextView(this).apply {
            text = "Радар прочитает события календаря на 14 дней: первая неделя — активный контроль, вторая — средний."
            textSize = 15f
            setPadding(20, 12, 20, 16)
        }

        val previewScroll = ScrollView(this).apply {
            isFillViewport = false
            addView(
                preview,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 24, 18, 24)
            addView(TextView(this@CalendarImportActivity).apply {
                text = "Календарь → Радар"
                textSize = 24f
                setPadding(20, 6, 20, 12)
            }, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(status, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(
                previewScroll,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            )
            addView(Button(this@CalendarImportActivity).apply {
                text = "Сканировать календарь ещё раз"
                setOnClickListener { scanCalendar() }
            }, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(Button(this@CalendarImportActivity).apply {
                text = "Вернуться"
                setOnClickListener { finish() }
            }, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun hasCalendarPermission(): Boolean {
        return checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
    }

    private fun scanCalendar() {
        if (!hasCalendarPermission()) {
            requestPermissions(arrayOf(Manifest.permission.READ_CALENDAR), READ_CALENDAR_REQUEST_CODE)
            return
        }

        status.text = "Читаю события календаря на ближайшие 14 дней..."
        preview.text = "1–7 дней: активный контроль. 8–14 дней: средний контроль."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appContainer = AppContainer.get(applicationContext)
                val reader = CalendarSourceReader(applicationContext)
                val events = reader.readUpcomingEvents(daysAhead = 14, limit = 60)
                val result = appContainer.calendarRadarImporter.importEvents(events)
                val scheduledCount = scheduleNewCalendarReminders(appContainer, result)
                val previewEvents = events.deduplicatedForPreview()

                withContext(Dispatchers.Main) {
                    if (events.isEmpty()) {
                        status.text = "Ближайших событий календаря не найдено."
                        preview.text = "Создайте тестовое мероприятие в календаре и повторите сканирование. Важно: Google Tasks/Задачи — отдельный источник, они пока не читаются этим календарным модулем."
                    } else {
                        status.text = buildStatusText(result, scheduledCount, previewEvents.size)
                        preview.text = previewEvents.take(12).joinToString("\n\n") { event -> event.toPreviewText() }
                    }
                }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CalendarImportActivity, t.message ?: "Не удалось прочитать календарь", Toast.LENGTH_LONG).show()
                    status.text = "Не удалось прочитать календарь: ${t.message ?: "неизвестная ошибка"}"
                }
            }
        }
    }

    private suspend fun scheduleNewCalendarReminders(appContainer: AppContainer, result: CalendarImportResult): Int {
        var scheduled = 0
        result.createdCardIds.forEach { cardId ->
            val card = appContainer.database.radarCardDao().getCardById(cardId) ?: return@forEach
            val scheduleResult = appContainer.reminderScheduler.schedule(card)
            if (scheduleResult is ReminderScheduleResult.Scheduled) scheduled++
        }
        return scheduled
    }

    private fun buildStatusText(result: CalendarImportResult, scheduledCount: Int, previewCount: Int): String {
        val mergedDuplicates = (result.total - previewCount).coerceAtLeast(0)
        val mergedText = if (mergedDuplicates > 0) " Объединено дублей: $mergedDuplicates." else ""
        return "Календарь просканирован. Найдено в календаре: ${result.total}. Показано после объединения: $previewCount.$mergedText Новых карточек: ${result.created}. Уже были в Радаре: ${result.alreadyKnown}. Уведомлений запланировано: $scheduledCount. Активный контроль: ${result.activeCount}. Средний контроль: ${result.mediumCount}."
    }

    private fun List<CalendarSourceEvent>.deduplicatedForPreview(): List<CalendarSourceEvent> {
        val seen = linkedSetOf<String>()
        return filter { event -> seen.add(event.previewDedupeKey()) }
    }

    private fun CalendarSourceEvent.previewDedupeKey(): String {
        if (!allDay) return stableKey()
        val normalizedTitle = title
            .trim()
            .lowercase(Locale.getDefault())
            .replace(Regex("[^а-яёa-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "event" }
        val dayKey = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(beginMillis))
        return "calendar-preview:all-day:$normalizedTitle:$dayKey"
    }

    companion object {
        private const val READ_CALENDAR_REQUEST_CODE = 3001
    }
}
