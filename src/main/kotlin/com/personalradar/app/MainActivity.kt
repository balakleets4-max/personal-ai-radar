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
        handleIncomingShare(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingShare(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != VOICE_INPUT_REQUEST_CODE) return

        if (resultCode != RESULT_OK) {
            status.text = "Голосовой захват отменён."
            return
        }

        val spokenText = data
            ?.getStringExtra(com.personalradar.app.voice.OfflineVoiceCaptureActivity.EXTRA_RECOGNIZED_TEXT)
            ?.trim()
            .orEmpty()

        if (spokenText.isBlank()) {
            status.text = "Не удалось распознать голос. Попробуйте ещё раз."
            return
        }

        input.setText(spokenText)
        status.text = "Голос распознан. Обрабатываю: $spokenText"
        saveCaptureText(spokenText, fromShare = false)
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
        val voiceButton = Button(this).apply {
            text = "🎙 Сказать в Радар"
            setOnClickListener { startVoiceCapture() }
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
            addView(voiceButton, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(saveButton, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(status, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(buildSourcesSection(), ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(buildAiSettingsSection(), ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
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
                text = "Здесь видно, откуда помощник уже умеет принимать важные сигналы."
                textSize = 14f
                setTextColor(Color.rgb(80, 80, 88))
                setPadding(0, 0, 0, 12)
            })
            addView(sourceRow("Ручной ввод", "включён", "Можно вручную добавить мысль, дело или напоминание."))
            addView(sourceRow("Голосовой захват", "улучшен", "Можно сказать мысль или напоминание голосом — аудио не сохраняется."))
            addView(sourceRow("Поделиться в Радар", "включено", "Из другого приложения нажмите Поделиться и выберите Личный ИИ-Радар."))
            addView(sourceRow("Уведомления Радара", "частично", "Приложение уже умеет отправлять собственные напоминания."))
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

    private fun buildAiSettingsSection(): LinearLayout {
        val store = AppContainer.get(applicationContext).aiSettingsStore
        val settings = store.getSettings()
        val apiKeyInput = EditText(this).apply {
            hint = if (settings.hasApiKey) "API-ключ сохранён. Вставьте новый для замены." else "Вставьте секретный API-ключ Yandex AI"
            minLines = 1
            setSingleLine(true)
            setPadding(18, 14, 18, 14)
        }
        val catalogInput = EditText(this).apply {
            hint = if (settings.hasCatalogId) "Catalog ID сохранён. Вставьте новый для замены." else "Вставьте Catalog ID Yandex Cloud"
            minLines = 1
            setSingleLine(true)
            setPadding(18, 14, 18, 14)
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = softPanelBackground()
            setPadding(20, 18, 20, 18)
            addView(TextView(this@MainActivity).apply {
                text = "ИИ-анализ"
                textSize = 21f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, 6)
            })
            addView(TextView(this@MainActivity).apply {
                text = aiConnectionStatusText()
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(aiConnectionStatusColor())
                setPadding(0, 0, 0, 8)
            })
            addView(TextView(this@MainActivity).apply {
                text = aiSettingsSummary()
                textSize = 14f
                setTextColor(Color.rgb(80, 80, 88))
                setPadding(0, 0, 0, 10)
            })
            addView(apiKeyInput, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(catalogInput, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(Button(this@MainActivity).apply {
                text = "Сохранить подключение Yandex AI"
                setOnClickListener {
                    val key = apiKeyInput.text.toString().trim()
                    val catalog = catalogInput.text.toString().trim()
                    if (key.isBlank() && catalog.isBlank()) {
                        status.text = "Вставьте секретный API-ключ и Catalog ID."
                    } else {
                        if (key.isNotBlank()) store.saveApiKey(key)
                        if (catalog.isNotBlank()) store.saveCatalogId(catalog)
                        rebuildScreenAfterSettingsChange("Подключение Yandex AI сохранено на устройстве.")
                    }
                }
            }, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(Button(this@MainActivity).apply {
                text = if (settings.cloudAnalysisEnabled) "Выключить облачный анализ" else "Включить облачный анализ"
                setOnClickListener {
                    val current = store.getSettings()
                    if (!current.cloudAnalysisEnabled && !current.hasApiKey) {
                        status.text = "Сначала сохраните секретный API-ключ Yandex AI."
                    } else if (!current.cloudAnalysisEnabled && !current.hasCatalogId) {
                        status.text = "Сначала сохраните Catalog ID Yandex Cloud."
                    } else {
                        store.setCloudAnalysisEnabled(!current.cloudAnalysisEnabled)
                        rebuildScreenAfterSettingsChange(
                            if (current.cloudAnalysisEnabled) "Облачный ИИ-анализ выключен." else "Облачный ИИ-анализ включён."
                        )
                    }
                }
            }, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(Button(this@MainActivity).apply {
                text = "Удалить подключение Yandex AI"
                setOnClickListener {
                    store.clearConnectionData()
                    rebuildScreenAfterSettingsChange("Подключение Yandex AI удалено. Облачный анализ выключен.")
                }
            }, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }.also { panel ->
            panel.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 8, 0, 8) }
        }
    }

    private fun aiConnectionStatusText(): String {
        val settings = AppContainer.get(applicationContext).aiSettingsStore.getSettings()
        return when {
            settings.canUseCloud -> "Yandex AI подключён и включён"
            settings.hasApiKey && settings.hasCatalogId -> "Yandex AI подключён, но облачный анализ выключен"
            settings.hasApiKey -> "Нужен Catalog ID для Yandex AI"
            settings.hasCatalogId -> "Нужен секретный API-ключ для Yandex AI"
            else -> "Yandex AI не подключён"
        }
    }

    private fun aiConnectionStatusColor(): Int {
        val settings = AppContainer.get(applicationContext).aiSettingsStore.getSettings()
        return when {
            settings.canUseCloud -> Color.rgb(42, 120, 70)
            settings.hasApiKey || settings.hasCatalogId -> Color.rgb(160, 100, 30)
            else -> Color.rgb(90, 90, 100)
        }
    }

    private fun aiSettingsSummary(): String {
        val settings = AppContainer.get(applicationContext).aiSettingsStore.getSettings()
        val cloud = if (settings.cloudAnalysisEnabled) "включён" else "выключен"
        val key = if (settings.hasApiKey) "ключ сохранён" else "ключ не задан"
        val catalog = if (settings.hasCatalogId) "Catalog ID сохранён" else "Catalog ID не задан"
        return "Провайдер: ${settings.provider}. Облачный анализ: $cloud. API: $key. $catalog. Данные отправляются наружу только после включения владельцем."
    }

    private fun rebuildScreenAfterSettingsChange(message: String) {
        setContentView(buildScreen())
        status.text = message
        refreshRadarCards()
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

    private fun startVoiceCapture() {
        status.text = "Открываю офлайн-голос. Говорите спокойно, завершите кнопкой Готово."
        startActivityForResult(
            Intent(this, com.personalradar.app.voice.OfflineVoiceCaptureActivity::class.java),
            VOICE_INPUT_REQUEST_CODE
        )
    }

    private fun handleIncomingShare(incomingIntent: Intent?) {
        val text = extractSharedText(incomingIntent)?.trim() ?: return
        if (text.isBlank()) return
        input.setText(text)
        saveCaptureText(text, fromShare = true)
    }

    private fun extractSharedText(incomingIntent: Intent?): String? {
        if (incomingIntent?.action != Intent.ACTION_SEND) return null
        if (incomingIntent.type != "text/plain") return null
        return incomingIntent.getStringExtra(Intent.EXTRA_TEXT)
            ?: incomingIntent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
    }

    private fun saveCapture() {
        saveCaptureText(input.text.toString(), fromShare = false)
    }

    private fun saveCaptureText(text: String, fromShare: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val state = controller.saveCaptureAndLoadRadar(text, RadarCardViewMode.ACTIVE)
                withContext(Dispatchers.Main) {
                    viewMode = RadarCardViewMode.ACTIVE
                    input.setText("")
                    renderState(state)
                    scheduleCreatedReminder(state)
                    if (fromShare && state.createdCard?.dueAt == null) {
                        status.text = "Текст принят через Поделиться. Карточка создана."
                    }
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
            .replace("; действие найдено", "; действие")
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
        private const val VOICE_INPUT_REQUEST_CODE = 2002
    }
}
