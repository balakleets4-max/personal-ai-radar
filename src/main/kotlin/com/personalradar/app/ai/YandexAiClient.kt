package com.personalradar.app.ai

import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class YandexAiClient {
    fun analyzeText(text: String, settings: AiSettings): AiAnalysisResult? {
        if (!settings.canUseCloud) return null
        val connection = URL(ENDPOINT).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 25_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Api-Key ${settings.apiKey}")

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(buildRequestBody(text, settings.catalogId))
            }

            val responseText = if (connection.responseCode in 200..299) {
                connection.inputStream.bufferedReader(Charsets.UTF_8).use(BufferedReader::readText)
            } else {
                connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use(BufferedReader::readText)
                return null
            }
            parseAnalysisResponse(responseText)
        } catch (_: Throwable) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun buildRequestBody(text: String, catalogId: String): String {
        val systemPrompt = """
            Ты локальный помощник приложения Личный ИИ-Радар.
            Разбери текст владельца или фрагмент переписки.
            Найди смысл, действие, дату/время, важность и короткое уведомление.
            Верни только JSON без markdown.
            Поля: type, action, due_text, importance, notification, reason.
            type: reminder, task, risk или thought.
            importance: число от 1 до 5.
            Если времени нет, due_text пустой.
        """.trimIndent()

        val root = JSONObject()
        root.put("modelUri", "gpt://$catalogId/yandexgpt-lite")
        root.put(
            "completionOptions",
            JSONObject()
                .put("stream", false)
                .put("temperature", 0.2)
                .put("maxTokens", 600)
        )
        root.put(
            "messages",
            listOf(
                JSONObject().put("role", "system").put("text", systemPrompt),
                JSONObject().put("role", "user").put("text", text.take(6000))
            )
        )
        return root.toString()
    }

    private fun parseAnalysisResponse(responseText: String): AiAnalysisResult? {
        val response = JSONObject(responseText)
        val alternatives = response.optJSONObject("result")?.optJSONArray("alternatives") ?: return null
        val message = alternatives.optJSONObject(0)?.optJSONObject("message") ?: return null
        val raw = message.optString("text").trim()
        val jsonText = raw.substringAfter("```json", raw).substringAfter("```", raw).substringBeforeLast("```").trim()
        val parsed = JSONObject(jsonText)
        return AiAnalysisResult(
            type = parsed.optString("type", "thought"),
            action = parsed.optString("action", ""),
            dueText = parsed.optString("due_text", ""),
            importance = parsed.optInt("importance", 3).coerceIn(1, 5),
            notification = parsed.optString("notification", ""),
            reason = parsed.optString("reason", "")
        )
    }

    companion object {
        private const val ENDPOINT = "https://llm.api.cloud.yandex.net/foundationModels/v1/completion"
    }
}

data class AiAnalysisResult(
    val type: String,
    val action: String,
    val dueText: String,
    val importance: Int,
    val notification: String,
    val reason: String
)
