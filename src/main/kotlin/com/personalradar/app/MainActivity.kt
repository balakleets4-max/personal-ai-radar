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
import androidx.room.Room
import com.personalradar.app.core.database.AppDatabase
import com.personalradar.app.quick.QuickCaptureRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : Activity() {
    private lateinit var repository: QuickCaptureRepository
    private lateinit var input: EditText
    private lateinit var status: TextView
    private lateinit var radarList: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "personal-ai-radar.db"
        ).build()
        repository = QuickCaptureRepository(database)

        setContentView(buildScreen())
        refreshRadarCards(database)
    }

    private fun buildScreen(): ScrollView {
        input = EditText(this).apply {
            hint = "Enter Capture / Захват памяти"
            minLines = 3
            setPadding(24, 24, 24, 24)
        }

        status = TextView(this).apply {
            text = "Ready. Add a Capture to create a Radar card."
            textSize = 16f
            setPadding(24, 16, 24, 16)
        }

        radarList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 16, 24, 24)
        }

        val saveButton = Button(this).apply {
            text = "Save Capture"
            setOnClickListener { saveCapture() }
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 32, 24, 32)
            addView(TextView(this@MainActivity).apply {
                text = "Personal AI Radar"
                textSize = 26f
                setPadding(0, 0, 0, 16)
            })
            addView(input, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(saveButton, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(status, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(TextView(this@MainActivity).apply {
                text = "Radar"
                textSize = 22f
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
                val result = repository.addCapture(text)
                val cards = repositoryCardsSnapshot()
                withContext(Dispatchers.Main) {
                    input.setText("")
                    status.text = "Saved Capture #${result.captureId}; Radar card #${result.cardId} created."
                    renderCards(cards)
                }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, t.message ?: "Capture failed", Toast.LENGTH_LONG).show()
                    status.text = "Could not save Capture: ${t.message ?: "unknown error"}"
                }
            }
        }
    }

    private fun refreshRadarCards(database: AppDatabase) {
        CoroutineScope(Dispatchers.IO).launch {
            val cards = database.radarCardDao().getActiveCardsSnapshot()
            withContext(Dispatchers.Main) { renderCards(cards) }
        }
    }

    private suspend fun repositoryCardsSnapshot() =
        RoomHolder.database.radarCardDao().getActiveCardsSnapshot()

    private fun renderCards(cards: List<com.personalradar.app.core.database.entity.RadarCardEntity>) {
        radarList.removeAllViews()
        if (cards.isEmpty()) {
            radarList.addView(TextView(this).apply {
                text = "No Radar cards yet."
                textSize = 16f
            })
            return
        }

        cards.forEach { card ->
            radarList.addView(TextView(this).apply {
                text = "${card.title}\n${card.description}\nWhy I see this: ${card.whyText}\nPriority: ${card.priority}"
                textSize = 16f
                setPadding(0, 16, 0, 16)
            })
        }
    }

    private object RoomHolder {
        lateinit var database: AppDatabase
    }
}
