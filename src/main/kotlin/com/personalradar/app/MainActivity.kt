package com.personalradar.app

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.personalradar.app.core.database.entity.RadarCardEntity
import com.personalradar.app.di.AppContainer
import com.personalradar.app.quick.CaptureRadarController
import com.personalradar.app.quick.CaptureRadarScreenState
import com.personalradar.app.quick.RadarCardViewMode
import com.personalradar.app.quick.RadarCounters
import com.personalradar.app.reminder.ReminderScheduleResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var controller: CaptureRadarController
    private lateinit var input: EditText
    private lateinit var status: TextView
    private lateinit var radarList: LinearLayout
    private lateinit var activeButton: Button
    private lateinit var hiddenButton: Button
    private lateinit var doneButton: Button
    private var viewMode = RadarCardViewMode.ACTIVE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controller = AppContainer.get(applicationContext).captureRadarController
        setContentView(buildScreen())
        requestNotificationPermissionIfNeeded()
        requestExactAlarmPermissionIfNeeded()
        refreshRadarCards()
    }

    private fun buildScreen(): ScrollView {
        input = EditText(this).apply {
            hint = "Введите захват памяти"
            minLines = 3
            setPadding(24, 24, 24, 24)
        }
        status = TextView(this).apply {
            text = "Готово. Введите мысль, задачу, риск или напоминание."
            textSize = 15f
            setPadding(18, 12, 18, 12)
        }
        radarList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 12, 0, 18)
        }
        val saveButton = Button(this).apply {
            text = "Сохранить захват"
            setOnClickListener { saveCapture() }
        }
        activeButton = Button(this).apply {
            text = "Активные (0)"
            setOnClickListener { switchMode(RadarCardViewMode.ACTIVE) }
        }
        hiddenButton = Button(this).apply {
            text = "Скрытые (0)"
            setOnClickListener { switchMode(RadarCardViewMode.HIDDEN) }
        }
        doneButton = Button(this).apply {
            text = "Готовые (0)"
            setOnClickListener { switchMode(RadarCardViewMode.DONE) }
        }
        val firstModeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(activeButton, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(hiddenButton, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        val secondModeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(doneButton, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        val modeButtons = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(firstModeRow, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(secondModeRow, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 28, 22, 28)
            addView(TextView(this@MainActivity).apply {
                text = "Личный ИИ-Радар"
                textSize = 26f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, 8)
            })
            addView(TextView(this@MainActivity).apply {
                text = "Тихий помощник: собирает важное из разрешённых источников и напоминает вовремя."
                textSize = 15f
                setTextColor(Color.rgb(80, 80, 88))
                setPadding(0, 0, 0, 14)
            })
            addView(input, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(saveButton, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(status, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(buildSourcesSection(), ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(TextView(this@MainActivity).apply {
                text = "Радар"
                textSize = 23f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 18, 0, 6)
            })
            addView(modeButtons, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(radarList, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        return ScrollView(this).apply { addView(root) }
    }

    private fun buildSourcesSection(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = softPanelBackground()
            setPadding(20, 18, 20, 18)
            addView(TextView(this@MainActivity).apply {
                text = "Источники Радара"
                textSize = 21f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, 6)
            })
            addView(TextView(this@MainActivity).apply {
                text = "Здесь будет управление тем, откуда помощник берёт важные сигналы. Сейчас это первый экран-подготовка."
                textSize = 14f
                setTextColor(Color.rgb(80, 80, 88))
                setPadding(0, 0, 0, 12)
            })
            addView(sourceRow("Ручной ввод", "включён", "Можно вручную добавить мысль, дело или напоминание."))
            addView(sourceRow("Уведомления Радара", "частично", "Приложение уже умеет отправлять собственные напоминания."))
            addView(sourceRow("Поделиться в Радар", "следующий шаг", "Текст из Telegram, браузера, заметок и почты можно будет отправлять в Радар."))
            addView(sourceRow("Календарь", "скоро", "Радар сможет читать события календаря с разрешения владельца."))
            addView(sourceRow("Уведомления телефона", "позже", "Будущий источник для поиска важных сообщений и событий."))
            addView(sourceRow("Контакты, ссылки, картинки", "позже", "Будут подключаться осторожно, только с явным разрешением."))
        }.also { panel ->
            panel.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 14, 0, 8) }
        }
    }

    private fun sourceRow(title: String, state: String, description: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 8, 0, 8)
            addView(TextView(this@MainActivity).apply {
                text = "$title — $state"
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.rgb(45, 45, 52))
            })
            addView(TextView(this@MainActivity).apply {
                text = description
                textSize = 13f
                setTextColor(Color.rgb(90, 90, 100))
                setPadding(0, 2, 0, 0)
            })
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST_CODE)
            }
        }
    }

    private fun requestExactAlarmPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                status.text = "Для точных напоминаний разрешите будильники и напоминания для приложения."
                try {
                    startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:$packageName")
                        }
                    )
                } catch (_: Throwable) {
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:$packageName")
                    })
                }
            }
        }
    }

    private fun saveCapture() {
        val text = input.text.toString()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val state = controller.saveCaptureAndLoadRadar(text, viewMode)
                withContext(Dispatchers.Main) {
                    input.setText("")
                    renderState(state)
                    scheduleCreatedReminder(state)
                }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, t.message ?: "Не удалось сохранить захват", Toast.LENGTH_LONG).show()
                    status.text = "Не удалось сохранить захват: ${t.message ?: "неизвестная ошибка"}"
                }
            }
        }
    }

    private fun scheduleCreatedReminder(state: CaptureRadarScreenState) {
        val card = state.createdCard ?: return
        if (card.dueAt == null) return
        val result = AppContainer.get(applicationContext).reminderScheduler.schedule(card)
        status.text = when (result) {
            is ReminderScheduleResult.Scheduled -> {
                val mode = if (result.exact) "точное" else "примерное"
                "${state.message}\nУведомление запланировано ($mode): ${formatDueAt(result.dueAt)}"
            }
            is ReminderScheduleResult.NotScheduled -> "${state.message}\nУведомление не запланировано: ${result.reason}"
        }
    }

    private fun scheduleRestoredReminder(state: CaptureRadarScreenState) {
        val card = state.restoredCard ?: return
        if (card.dueAt == null) return
        val result = AppContainer.get(applicationContext).reminderScheduler.schedule(card)
        status.text = when (result) {
            is ReminderScheduleResult.Scheduled -> {
                val mode = if (result.exact) "точное" else "примерное"
                "${state.message}\nУведомление снова запланировано ($mode): ${formatDueAt(result.dueAt)}"
            }
            is ReminderScheduleResult.NotScheduled -> "${state.message}\nУведомление не запланировано: ${result.reason}"
        }
    }

    private fun cancelReminderIfNeeded(state: CaptureRadarScreenState) {
        val cardId = state.cancelledReminderCardId ?: return
        AppContainer.get(applicationContext).reminderScheduler.cancel(cardId)
        status.text = "${state.message}\nЗапланированное уведомление отменено."
    }

    private fun switchMode(mode: RadarCardViewMode) {
        viewMode = mode
        status.text = when (mode) {
            RadarCardViewMode.ACTIVE -> "Активный Радар."
            RadarCardViewMode.HIDDEN -> "Скрытые карточки."
            RadarCardViewMode.DONE -> "Готовые карточки."
        }
        refreshRadarCards()
    }

    private fun restoreCard(cardId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val state = controller.restoreCardToActiveAndLoadRadar(cardId, RadarCardViewMode.ACTIVE)
                withContext(Dispatchers.Main) { renderState(state) }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, t.message ?: "Не удалось вернуть карточку", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun deleteCard(cardId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val state = controller.deleteCardAndLoadRadar(cardId, viewMode)
                withContext(Dispatchers.Main) { renderState(state) }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, t.message ?: "Не удалось удалить карточку", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun refreshRadarCards() {
        CoroutineScope(Dispatchers.IO).launch {
            val snapshot = controller.loadRadarSnapshot(viewMode)
            withContext(Dispatchers.Main) {
                renderCounters(snapshot.counters)
                renderCards(snapshot.cards)
            }
        }
    }

    private fun runCardAction(action: suspend () -> CaptureRadarScreenState) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val state = action()
                withContext(Dispatchers.Main) { renderState(state) }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, t.message ?: "Действие не выполнено", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun renderState(state: CaptureRadarScreenState) {
        status.text = state.message
        renderCounters(state.counters)
        renderCards(state.cards)
        cancelReminderIfNeeded(state)
        scheduleRestoredReminder(state)
    }

    private fun renderCounters(counters: RadarCounters) {
        activeButton.text = "Активные (${counters.active})"
        hiddenButton.text = "Скрытые (${counters.hidden})"
        doneButton.text = "Готовые (${counters.done})"
    }

    private fun emptyText(): String {
        return when (viewMode) {
            RadarCardViewMode.ACTIVE -> "Активных карточек пока нет."
            RadarCardViewMode.HIDDEN -> "Скрытых карточек пока нет."
            RadarCardViewMode.DONE -> "Готовых карточек пока нет."
        }
    }

    private fun softPanelBackground(): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.rgb(248, 248, 252))
            setStroke(2, Color.rgb(224, 224, 232))
            cornerRadius = 20f
        }
    }

    private fun cardBackground(): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.rgb(255, 255, 255))
            setStroke(2, Color.rgb(218, 218, 226))
            cornerRadius = 20f
        }
    }

    private fun typeLabel(type: String): String {
        return when (type) {
            "REMINDER" -> "Напоминание"
            "TASK" -> "Задача"
            "RISK" -> "Риск"
            else -> "Мысль"
        }
    }

    private fun compactWhy(text: String): String {
        return text
            .replace("язык: RU; ", "")
            .replace("язык: EN; ", "")
            .replace("тип: ", "")
            .replace("; есть сигнал действия", "; действие")
            .replace("; есть сигнал времени/напоминания", "; время")
            .replace("; когда: ", " · когда: ")
            .replace("; ", " · ")
    }

    private fun normalizeCardText(text: String): String {
        return text
            .lowercase()
            .replace("задача:", "")
            .replace("напоминание:", "")
            .replace("риск:", "")
            .replace("мысль:", "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun shouldShowDescription(card: RadarCardEntity): Boolean {
        val title = normalizeCardText(card.title)
        val description = normalizeCardText(card.description)
        return description.isNotBlank() && description != title && !title.contains(description) && !description.contains(title)
    }

    private fun formatDueAt(dueAt: Long): String {
        return SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(dueAt))
    }

    private fun renderCards(cards: List<RadarCardEntity>) {
        radarList.removeAllViews()
        if (cards.isEmpty()) {
            radarList.addView(TextView(this).apply {
                text = emptyText()
                textSize = 16f
                setPadding(0, 14, 0, 14)
            })
            return
        }
        cards.forEach { card ->
            val box = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = cardBackground()
                setPadding(20, 18, 20, 18)
            }
            box.addView(TextView(this).apply {
                text = "${typeLabel(card.type)} · Приоритет ${card.priority}"
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.rgb(92, 64, 180))
                setPadding(0, 0, 0, 6)
            })
            box.addView(TextView(this).apply {
                text = card.title
                textSize = 19f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, if (shouldShowDescription(card)) 6 else 8)
            })
            if (shouldShowDescription(card)) {
                box.addView(TextView(this).apply {
                    text = card.description
                    textSize = 15f
                    setTextColor(Color.rgb(45, 45, 52))
                    setPadding(0, 0, 0, 8)
                })
            }
            if (card.dueAt != null) {
                box.addView(TextView(this).apply {
                    text = "Когда: ${formatDueAt(card.dueAt)}"
                    textSize = 15f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.rgb(42, 100, 70))
                    setPadding(0, 0, 0, 8)
                })
            }
            box.addView(TextView(this).apply {
                text = compactWhy(card.whyText)
                textSize = 13f
                setTextColor(Color.rgb(90, 90, 100))
                setPadding(0, 0, 0, 10)
            })
            val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            when (viewMode) {
                RadarCardViewMode.ACTIVE -> {
                    buttons.addView(Button(this).apply {
                        text = "Готово"
                        setOnClickListener { runCardAction { controller.markCardDoneAndLoadRadar(card.id, viewMode) } }
                    })
                    buttons.addView(Button(this).apply {
                        text = "Скрыть"
                        setOnClickListener { runCardAction { controller.hideCardAndLoadRadar(card.id) } }
                    })
                }
                RadarCardViewMode.HIDDEN -> {
                    buttons.addView(Button(this).apply {
                        text = "Вернуть"
                        setOnClickListener { restoreCard(card.id) }
                    })
                }
                RadarCardViewMode.DONE -> {
                    buttons.addView(Button(this).apply {
                        text = "Вернуть"
                        setOnClickListener { restoreCard(card.id) }
                    })
                }
            }
            buttons.addView(Button(this).apply {
                text = "Удалить"
                setOnClickListener { deleteCard(card.id) }
            })
            box.addView(buttons)
            radarList.addView(
                box,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 14) }
            )
        }
    }

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 2001
    }
}
