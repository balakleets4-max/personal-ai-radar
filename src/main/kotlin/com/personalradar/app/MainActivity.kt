package com.personalradar.app

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            textSize = 16f
            setPadding(24, 16, 24, 16)
        }
        radarList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 16, 0, 24)
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
        val modeButtons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(activeButton)
            addView(hiddenButton)
            addView(doneButton)
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 32, 24, 32)
            addView(TextView(this@MainActivity).apply {
                text = "Личный ИИ-Радар"
                textSize = 28f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, 12)
            })
            addView(TextView(this@MainActivity).apply {
                text = "Запишите обычной фразой то, что нужно не потерять."
                textSize = 16f
                setTextColor(Color.rgb(80, 80, 88))
                setPadding(0, 0, 0, 20)
            })
            addView(input, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(saveButton, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(status, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(TextView(this@MainActivity).apply {
                text = "Радар"
                textSize = 24f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 24, 0, 8)
            })
            addView(modeButtons, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(radarList, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        return ScrollView(this).apply { addView(root) }
    }

    private fun saveCapture() {
        val text = input.text.toString()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val state = controller.saveCaptureAndLoadRadar(text, viewMode)
                withContext(Dispatchers.Main) {
                    input.setText("")
                    renderState(state)
                }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, t.message ?: "Не удалось сохранить захват", Toast.LENGTH_LONG).show()
                    status.text = "Не удалось сохранить захват: ${t.message ?: "неизвестная ошибка"}"
                }
            }
        }
    }

    private fun switchMode(mode: RadarCardViewMode) {
        viewMode = mode
        status.text = when (mode) {
            RadarCardViewMode.ACTIVE -> "Показан активный Радар."
            RadarCardViewMode.HIDDEN -> "Показаны скрытые карточки."
            RadarCardViewMode.DONE -> "Показаны готовые карточки."
        }
        refreshRadarCards()
    }

    private fun switchToActiveRadar(message: String) {
        viewMode = RadarCardViewMode.ACTIVE
        status.text = message
        refreshRadarCards()
    }

    private fun restoreCard(cardId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val state = controller.restoreCardToActiveAndLoadRadar(cardId, RadarCardViewMode.ACTIVE)
                withContext(Dispatchers.Main) { switchToActiveRadar(state.message) }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, t.message ?: "Не удалось вернуть карточку", Toast.LENGTH_LONG).show()
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

    private fun cardBackground(): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.rgb(255, 255, 255))
            setStroke(2, Color.rgb(210, 210, 220))
            cornerRadius = 24f
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

    private fun renderCards(cards: List<RadarCardEntity>) {
        radarList.removeAllViews()
        if (cards.isEmpty()) {
            radarList.addView(TextView(this).apply {
                text = emptyText()
                textSize = 16f
                setPadding(0, 16, 0, 16)
            })
            return
        }
        cards.forEach { card ->
            val box = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = cardBackground()
                setPadding(24, 22, 24, 22)
            }

            box.addView(TextView(this).apply {
                text = typeLabel(card.type)
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.rgb(92, 64, 180))
                setPadding(0, 0, 0, 8)
            })
            box.addView(TextView(this).apply {
                text = card.title
                textSize = 20f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, 8)
            })
            box.addView(TextView(this).apply {
                text = card.description
                textSize = 16f
                setTextColor(Color.rgb(45, 45, 52))
                setPadding(0, 0, 0, 12)
            })
            box.addView(TextView(this).apply {
                text = "Почему в Радаре: ${card.whyText}"
                textSize = 14f
                setTextColor(Color.rgb(90, 90, 100))
                setPadding(0, 0, 0, 8)
            })
            box.addView(TextView(this).apply {
                text = "Приоритет: ${card.priority}"
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.rgb(70, 70, 80))
                setPadding(0, 0, 0, 12)
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
            box.addView(buttons)
            radarList.addView(
                box,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 18) }
            )
        }
    }
}
