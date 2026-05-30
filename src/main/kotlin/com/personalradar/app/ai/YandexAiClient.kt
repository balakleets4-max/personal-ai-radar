package com.personalradar.app.ai

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class YandexAiClient {
    var lastErrorMessage: String? = null
        private set

    fun analyzeText(text: String, settings: AiSettings): AiAnalysisResult? {
        lastErrorMessage = null
        if (!settings.canUseCloud) {
            lastErrorMessage = when {
                !settings.cloudAnalysisEnabled -> "облачный анализ выключен"
                !settings.hasApiKey -> "не сохранён API-ключ"
                !settings.hasCatalogId -> "не сохранён Catalog ID"
                else -> "облачный анализ недоступен"
            }
            return null
        }
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

            val responseCode = connection.responseCode
            val responseText = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader(Charsets.UTF_8).use(BufferedReader::readText)
            } else {
                val errorText = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use(BufferedReader::readText).orEmpty()
                lastErrorMessage = buildHttpErrorMessage(responseCode, errorText)
                return null
            }
            parseAnalysisResponse(responseText)
        } catch (t: Throwable) {
            lastErrorMessage = "ошибка запроса: ${t.javaClass.simpleName}: ${t.message ?: "без описания"}"
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun buildHttpErrorMessage(code: Int, body: String): String {
        val shortBody = body.replace(Regex("\\s+"), " ").take(220)
        val hint = when (code) {
            400 -> "плохой запрос: проверьте Catalog ID и модель"
            401 -> "неверный API-ключ или формат авторизации"
            403 -> "нет доступа: проверьте права ключа, каталог и биллинг"
            404 -> "модель или каталог не найдены"
            429 -> "превышен лимит запросов"
            else -> "HTTP $code"
        }
        return if (shortBody.isBlank()) hint else "$hint; ответ: $shortBody"
    }

    private fun buildRequestBody(text: String, catalogId: String): String {
        val systemPrompt = """
            Ты помощник приложения «Личный ИИ-Радар».
            Твоя задача — превратить сырой текст владельца в аккуратную карточку действия.

            Верни только JSON без markdown и без пояснений вне JSON.
            Поля JSON: type, action, due_text, importance, notification, reason.

            Правила:
            - type: reminder, task, risk или thought.
            - action: короткое действие без слов «напомни», «мне», «надо», «нужно». Например: «позвонить бабушке», «купить цветы», «заварить чай».
            - due_text: исходное время/дату человеческими словами, если оно есть: «через 5 минут», «завтра в 12:30», «сегодня в 18:00». Если времени нет — пустая строка.
            - importance: число от 1 до 5. Напоминания и задачи обычно 4, риски 5, обычные мысли 2-3.
            - notification: готовый текст уведомления от первого лица приложения. Например: «Напоминаю: пора позвонить бабушке.»
            - reason: коротко почему это важно, без длинных рассуждений.
            - Не выдумывай дату/время, если её нет.
            - Пиши естественно по-русски, если текст на русском.
        """.trimIndent()

        val messages = JSONArray()
            .put(JSONObject().put("role", "system").put("text", systemPrompt))
            .put(JSONObject().put("role", "user").put("text", text.take(6000)))

        val root = JSONObject()
            .put("modelUri", "gpt://$catalogId/yandexgpt-lite")
            .put(
                "completionOptions",
                JSONObject()
                    .put("stream", false)
                    .put("temperature", 0.15)
                    .put("maxTokens", "600")
            )
            .put("messages", messages)

        return root.toString()
    }

    private fun parseAnalysisResponse(responseText: String): AiAnalysisResult? {
        return try {
            val response = JSONObject(responseText)
            val alternatives = response.optJSONObject("result")?.optJSONArray("alternatives")
            if (alternatives == null || alternatives.length() == 0) {
                lastErrorMessage = "Yandex AI ответил без alternatives"
                return null
            }
            val message = alternatives.optJSONObject(0)?.optJSONObject("message")
            if (message == null) {
                lastErrorMessage = "Yandex AI ответил без message"
                return null
            }
            val raw = message.optString("text").trim()
            if (raw.isBlank()) {
                lastErrorMessage = "Yandex AI вернул пустой текст"
                return null
            }
            val jsonText = raw
                .substringAfter("```json", raw)
                .substringAfter("```", raw)
                .substringBeforeLast("```")
                .trim()
            val parsed = JSONObject(jsonText)
            AiAnalysisResult(
                type = parsed.optString("type", "thought"),
                action = parsed.optString("action", ""),
                dueText = parsed.optString("due_text", ""),
                importance = parsed.optInt("importance", 3).coerceIn(1, 5),
                notification = parsed.optString("notification", ""),
                reason = parsed.optString("reason", "")
            )
        } catch (t: Throwable) {
            lastErrorMessage = "ошибка разбора ответа Yandex AI: ${t.javaClass.simpleName}: ${t.message ?: "без описания"}"
            null
        }
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
