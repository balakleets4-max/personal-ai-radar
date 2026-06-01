package com.personalradar.app.ai

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class YandexAiClient {
    var lastErrorMessage: String? = null
        private set

    fun analyzeText(text: String, settings: AiSettings): AiAnalysisResult? {
        lastErrorMessage = null
        val responseText = executeCompletion(buildAnalysisRequestBody(text, settings.catalogId), settings) ?: return null
        return parseAnalysisResponse(responseText)
    }

    fun resolveCapture(
        newText: String,
        candidateCards: List<AiResolutionCard>,
        settings: AiSettings
    ): AiCaptureResolutionResult? {
        lastErrorMessage = null
        if (candidateCards.isEmpty()) return null
        val responseText = executeCompletion(buildResolutionRequestBody(newText, candidateCards, settings.catalogId), settings) ?: return null
        return parseResolutionResponse(responseText)
    }

    private fun executeCompletion(requestBody: String, settings: AiSettings): String? {
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
                writer.write(requestBody)
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                connection.inputStream.bufferedReader(Charsets.UTF_8).use(BufferedReader::readText)
            } else {
                val errorText = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use(BufferedReader::readText).orEmpty()
                lastErrorMessage = buildHttpErrorMessage(responseCode, errorText)
                null
            }
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

    private fun buildAnalysisRequestBody(text: String, catalogId: String): String {
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

        return buildCompletionRoot(catalogId, messages, maxTokens = "600", temperature = 0.15).toString()
    }

    private fun buildResolutionRequestBody(
        newText: String,
        candidateCards: List<AiResolutionCard>,
        catalogId: String
    ): String {
        val systemPrompt = """
            Ты семантический модуль приложения «AI Радар».
            Твоя задача — понять, новая фраза владельца создаёт новую карточку или относится к одной из уже активных карточек.

            Важно:
            - Понимай живой русский язык, а не отдельные ключевые слова.
            - Если пользователь явно переносит, уточняет, отменяет или исправляет старое событие — найди подходящую старую карточку.
            - Если фраза просто стала более конкретной, например «встреча» -> «встреча с мэром», это часто новая карточка, а не замена.
            - Если уверенности мало или подходят несколько карточек — decision = ask_user.
            - Не привязывай фразу к карточке только по одному общему слову «встреча», «звонок», «дело».
            - Верни только JSON без markdown.

            Формат JSON:
            {
              "decision": "create_new | update_existing | ask_user",
              "target_card_id": null,
              "confidence": 0.0,
              "user_message": "живое короткое объяснение для владельца",
              "developer_reason": "коротко почему принято решение",
              "normalized_meaning": {
                "intent": "create | update | reschedule | cancel | clarify | unknown",
                "object": "",
                "person": "",
                "place": "",
                "old_time": "",
                "new_time": ""
              }
            }
        """.trimIndent()

        val cardsJson = JSONArray()
        candidateCards.take(10).forEach { card ->
            cardsJson.put(
                JSONObject()
                    .put("id", card.id)
                    .put("title", card.title.take(160))
                    .put("source", card.sourceQuote.take(220))
                    .put("description", card.description.take(220))
                    .put("type", card.type)
                    .put("due", card.dueText.orEmpty())
            )
        }

        val userPayload = JSONObject()
            .put("new_text", newText.take(1200))
            .put("active_cards", cardsJson)
            .toString()

        val messages = JSONArray()
            .put(JSONObject().put("role", "system").put("text", systemPrompt))
            .put(JSONObject().put("role", "user").put("text", userPayload))

        return buildCompletionRoot(catalogId, messages, maxTokens = "900", temperature = 0.05).toString()
    }

    private fun buildCompletionRoot(catalogId: String, messages: JSONArray, maxTokens: String, temperature: Double): JSONObject {
        return JSONObject()
            .put("modelUri", "gpt://$catalogId/yandexgpt-lite")
            .put(
                "completionOptions",
                JSONObject()
                    .put("stream", false)
                    .put("temperature", temperature)
                    .put("maxTokens", maxTokens)
            )
            .put("messages", messages)
    }

    private fun parseAnalysisResponse(responseText: String): AiAnalysisResult? {
        return try {
            val raw = extractAssistantText(responseText) ?: return null
            val parsed = JSONObject(cleanJsonText(raw))
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

    private fun parseResolutionResponse(responseText: String): AiCaptureResolutionResult? {
        return try {
            val raw = extractAssistantText(responseText) ?: return null
            val parsed = JSONObject(cleanJsonText(raw))
            val meaning = parsed.optJSONObject("normalized_meaning")
            AiCaptureResolutionResult(
                decision = parsed.optString("decision", "create_new").lowercase(Locale.getDefault()).trim(),
                targetCardId = parsed.optNullableLong("target_card_id"),
                confidence = parsed.optDouble("confidence", 0.0).coerceIn(0.0, 1.0),
                userMessage = parsed.optString("user_message", ""),
                developerReason = parsed.optString("developer_reason", parsed.optString("reason", "")),
                normalizedIntent = meaning?.optString("intent", "").orEmpty(),
                normalizedObject = meaning?.optString("object", "").orEmpty(),
                normalizedPerson = meaning?.optString("person", "").orEmpty(),
                normalizedPlace = meaning?.optString("place", "").orEmpty(),
                normalizedOldTime = meaning?.optString("old_time", "").orEmpty(),
                normalizedNewTime = meaning?.optString("new_time", "").orEmpty()
            )
        } catch (t: Throwable) {
            lastErrorMessage = "ошибка разбора resolution-ответа Yandex AI: ${t.javaClass.simpleName}: ${t.message ?: "без описания"}"
            null
        }
    }

    private fun extractAssistantText(responseText: String): String? {
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
        return raw
    }

    private fun cleanJsonText(raw: String): String {
        return raw
            .substringAfter("```json", raw)
            .substringAfter("```", raw)
            .substringBeforeLast("```")
            .trim()
    }

    private fun JSONObject.optNullableLong(name: String): Long? {
        if (!has(name) || isNull(name)) return null
        return when (val value = opt(name)) {
            is Number -> value.toLong()
            is String -> value.trim().toLongOrNull()
            else -> null
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

data class AiResolutionCard(
    val id: Long,
    val title: String,
    val sourceQuote: String,
    val description: String,
    val type: String,
    val dueText: String?
)

data class AiCaptureResolutionResult(
    val decision: String,
    val targetCardId: Long?,
    val confidence: Double,
    val userMessage: String,
    val developerReason: String,
    val normalizedIntent: String,
    val normalizedObject: String,
    val normalizedPerson: String,
    val normalizedPlace: String,
    val normalizedOldTime: String,
    val normalizedNewTime: String
)
