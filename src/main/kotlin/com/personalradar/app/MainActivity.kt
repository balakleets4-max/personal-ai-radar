package com.personalradar.app

import android.app.Activity
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : Activity() {
    private lateinit var controller: CaptureRadarController
    private lateinit var input: EditText
    private lateinit var status: TextView
    private lateinit var radarList: LinearLayout

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
            setPadding(24, 16, 24, 24)
        }

        val saveButton = Button(this).apply {
            text = "Сохранить захват"
            setOnClickListener { saveCapture() }
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 32, 24, 32)
            addView(TextView(this@MainActivity).apply {
                text = "Личный ИИ-Радар"
                textSize = 28f
                setPadding(0, 0, 0, 12)
            })
            addView(TextView(this@MainActivity).apply {
                text = "Запишите обычной фразой то, что нужно не потерять."
                textSize = 16f
                setPadding(0, 0, 0, 20)
            })
            addView(input, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(saveButton, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(status, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(TextView(this@MainActivity).apply {
                text = "Радар"
                textSize = 24f
                setPadding(0, 24, 0, 8)
            })
            addView(radarList, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        return ScrollView(this).apply { addView(root) }
    }

    private fun saveCapture() {
        val text = input.text.toString()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val state = controller.saveCaptureAndLoadRadar(text)
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

    private fun refreshRadarCards() {
        CoroutineScope(Dispatchers.IO).launch {
            val cards = controller.loadRadarCards()
            withContext(Dispatchers.Main) { renderCards(cards) }
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
        renderCards(state.cards)
    }

    private fun renderCards(cards: List<RadarCardEntity>) {
        radarList.removeAllViews()
        if (cards.isEmpty()) {
            radarList.addView(TextView(this).apply {
                text = "Активных карточек пока нет."
                textSize = 16f
                setPadding(0, 12, 0, 12)
            })
            return
        }

        cards.forEach { card ->
            val box = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 16, 0, 20)
            }
            box.addView(TextView(this).apply {
                text = "${card.title}\n${card.description}\nПочему в Радаре: ${card.whyText}\nПриоритет: ${card.priority}"
                textSize = 16f
                setPadding(0, 0, 0, 8)
            })
            val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            buttons.addView(Button(this).apply {
                text = "Готово"
                setOnClickListener { runCardAction { controller.markCardDoneAndLoadRadar(card.id) } }
            })
            buttons.addView(Button(this).apply {
                text = "Скрыть"
                setOnClickListener { runCardAction { controller.hideCardAndLoadRadar(card.id) } }
            })
            box.addView(buttons)
            radarList.addView(box, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }
}
