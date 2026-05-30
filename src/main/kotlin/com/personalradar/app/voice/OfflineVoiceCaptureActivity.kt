package com.personalradar.app.voice

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService

class OfflineVoiceCaptureActivity : Activity(), RecognitionListener {
    private lateinit var status: TextView
    private lateinit var partialText: TextView
    private lateinit var finalText: TextView
    private lateinit var doneButton: Button
    private val mainHandler = Handler(Looper.getMainLooper())
    private var model: Model? = null
    private var speechService: SpeechService? = null
    private var lastPartial: String = ""
    private var finalResult: String = ""
    private var isFinishingWithText = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildScreen())
        if (hasAudioPermission()) {
            loadModelAndStart()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                AUDIO_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechService?.stop()
        mainHandler.removeCallbacksAndMessages(null)
        speechService?.shutdown()
        speechService = null
        model?.close()
        model = null
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == AUDIO_PERMISSION_REQUEST_CODE && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            loadModelAndStart()
        } else {
            status.text = "Без доступа к микрофону офлайн-голос не работает."
        }
    }

    override fun onPartialResult(hypothesis: String?) {
        val text = extractText(hypothesis, "partial")
        if (text.isNotBlank()) {
            lastPartial = text
            partialText.text = text
        }
    }

    override fun onResult(hypothesis: String?) {
        val text = extractText(hypothesis, "text")
        if (text.isNotBlank()) {
            finalResult = mergeText(finalResult, text)
            finalText.text = finalResult
        }
    }

    override fun onFinalResult(hypothesis: String?) {
        val text = extractText(hypothesis, "text")
        if (text.isNotBlank()) {
            finalResult = mergeText(finalResult, text)
            finalText.text = finalResult
        }
    }

    override fun onError(exception: Exception?) {
        status.text = "Ошибка офлайн-распознавания: ${exception?.message ?: "без описания"}"
    }

    override fun onTimeout() {
        status.text = "Офлайн-распознавание остановлено по таймауту."
    }

    private fun buildScreen(): LinearLayout {
        status = TextView(this).apply {
            text = "Готовлю офлайн-распознавание..."
            textSize = 18f
            setPadding(20, 20, 20, 12)
        }
        partialText = TextView(this).apply {
            text = "Говорите. Можно делать паузы — запись не сохранится автоматически."
            textSize = 16f
            setPadding(20, 12, 20, 12)
        }
        finalText = TextView(this).apply {
            text = ""
            textSize = 20f
            setPadding(20, 12, 20, 18)
        }

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 24, 18, 24)
            addView(status, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(partialText, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(finalText, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            doneButton = Button(this@OfflineVoiceCaptureActivity).apply {
                text = "Готово — отправить в Радар"
                setOnClickListener { finishWithText() }
            }
            addView(doneButton, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(Button(this@OfflineVoiceCaptureActivity).apply {
                text = "Отмена"
                setOnClickListener { finish() }
            }, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun loadModelAndStart() {
        status.text = "Загружаю локальную модель речи..."
        StorageService.unpack(
            this,
            MODEL_ASSET_DIR,
            MODEL_STORAGE_DIR,
            { loadedModel ->
                model = loadedModel
                status.text = "Готовлю микрофон..."
                partialText.text = "Начинайте говорить после сообщения “Говорите”."
                mainHandler.postDelayed({ startListening(loadedModel) }, 800L)
            },
            { exception ->
                status.text = "Локальная модель не найдена. Добавьте Vosk-модель в assets/$MODEL_ASSET_DIR. Ошибка: ${exception.message ?: "без описания"}"
            }
        )
    }

    private fun startListening(loadedModel: Model) {
        val recognizer = Recognizer(loadedModel, SAMPLE_RATE)
        speechService = SpeechService(recognizer, SAMPLE_RATE).also { service ->
            status.text = "Говорите. Можно делать паузы, завершите кнопкой Готово."
            partialText.text = "Слушаю..."
            service.startListening(this)
        }
    }

    private fun finishWithText() {
        if (isFinishingWithText) return
        val text = cleanRecognizedText(finalResult.ifBlank { lastPartial })
        if (text.isBlank()) {
            status.text = "Пока нечего отправлять. Скажите фразу или нажмите Отмена."
            return
        }
        isFinishingWithText = true
        doneButton.isEnabled = false
        status.text = "Обрабатываю голос..."
        partialText.text = text
        speechService?.stop()
        setResult(RESULT_OK, Intent().putExtra(EXTRA_RECOGNIZED_TEXT, text))
        finish()
    }

    private fun extractText(json: String?, key: String): String {
        if (json.isNullOrBlank()) return ""
        return try {
            JSONObject(json).optString(key, "").trim()
        } catch (_: Throwable) {
            ""
        }
    }

    private fun mergeText(current: String, next: String): String {
        val cleanNext = cleanRecognizedText(next)
        if (cleanNext.isBlank()) return current
        if (current.isBlank()) return cleanNext
        if (current.endsWith(cleanNext, ignoreCase = true)) return current
        return "$current $cleanNext".replace(Regex("\\s+"), " ").trim()
    }


    private fun cleanRecognizedText(text: String): String {
        val normalized = text
            .lowercase()
            .replace('ё', 'е')
            .replace(Regex("\\s+"), " ")
            .trim()
        if (normalized.isBlank()) return ""

        val words = normalized.split(" ").filter { it.isNotBlank() }.toMutableList()
        if (words.size >= 2 && looksLikeStartGarbage(words.first())) {
            words.removeAt(0)
        }
        return words.joinToString(" ").trim()
    }

    private fun looksLikeStartGarbage(word: String): Boolean {
        if (word.length <= 1) return true
        if (word.length <= 3 && word !in ALLOWED_SHORT_FIRST_WORDS) return true
        val vowels = word.count { it in "аеёиоуыэюя" }
        return word.length >= 5 && vowels == 0
    }

    companion object {
        const val EXTRA_RECOGNIZED_TEXT = "offline_voice_recognized_text"
        private const val AUDIO_PERMISSION_REQUEST_CODE = 4101
        private const val SAMPLE_RATE = 16_000.0f
        private const val MODEL_ASSET_DIR = "model-ru"
        private const val MODEL_STORAGE_DIR = "vosk-model-ru"
        private val ALLOWED_SHORT_FIRST_WORDS = setOf("я", "и", "в", "к", "на", "за", "до", "от", "по", "ну", "да", "не")
    }
}
