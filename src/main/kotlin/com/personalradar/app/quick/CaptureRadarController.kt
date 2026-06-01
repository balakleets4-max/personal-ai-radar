package com.personalradar.app.quick

import android.content.Context
import com.personalradar.app.ai.AiCaptureResolutionResult
import com.personalradar.app.ai.AiResolutionCard
import com.personalradar.app.ai.AiSettingsStore
import com.personalradar.app.ai.YandexAiClient
import com.personalradar.app.calendar.CalendarBackgroundScheduler
import com.personalradar.app.core.database.AppDatabase
import com.personalradar.app.core.database.entity.RadarCardEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CaptureRadarController(
    private val context: Context,
    private val database: AppDatabase,
    private val repository: QuickCaptureRepository,
    private val resolutionEngine: CaptureResolutionEngine = CaptureResolutionEngine(),
    private val aiSettingsStore: AiSettingsStore? = null,
    private val yandexAiClient: YandexAiClient? = null
) {
    suspend fun loadRadarCards(mode: RadarCardViewMode): List<RadarCardEntity> {
        return when (mode) {
            RadarCardViewMode.ACTIVE -> database.radarCardDao().getActiveCardsSnapshot()
            RadarCardViewMode.HIDDEN -> database.radarCardDao().getHiddenCardsSnapshot()
            RadarCardViewMode.DONE -> database.radarCardDao().getDoneCardsSnapshot()
        }
    }

    suspend fun loadRadarCounters(): RadarCounters {
        val dao = database.radarCardDao()
        return RadarCounters(
            active = dao.countActiveCards(),
            hidden = dao.countHiddenCards(),
            done = dao.countDoneCards()
        )
    }

    suspend fun loadRadarSnapshot(mode: RadarCardViewMode): RadarSnapshot {
        return RadarSnapshot(
            cards = loadRadarCards(mode),
            counters = loadRadarCounters()
        )
    }

    suspend fun loadRadarCards(showHidden: Boolean = false): List<RadarCardEntity> {
        return loadRadarCards(if (showHidden) RadarCardViewMode.HIDDEN else RadarCardViewMode.ACTIVE)
    }

    suspend fun findSimilarManualCard(text: String): ManualDuplicateCandidate? {
        val activeCards = database.radarCardDao().getActiveCardsSnapshot()
        tryAiResolution(text, activeCards)?.let { return it }
        return resolutionEngine.findSimilarCapture(
            newText = text,
            activeCards = activeCards
        )
    }

    private fun tryAiResolution(text: String, activeCards: List<RadarCardEntity>): ManualDuplicateCandidate? {
        val settings = aiSettingsStore?.getSettings() ?: return null
        val client = yandexAiClient ?: return null
        if (!settings.canUseCloud || activeCards.isEmpty()) return null

        val candidateCards = activeCards
            .take(10)
            .map { card ->
                AiResolutionCard(
                    id = card.id,
                    title = card.title,
                    sourceQuote = card.sourceQuote,
                    description = card.description,
                    type = card.type,
                    dueText = card.dueAt?.let { formatDueForAi(it) }
                )
            }

        val result = client.resolveCapture(text, candidateCards, settings) ?: return null
        val decision = result.decision.lowercase(Locale.getDefault()).trim()
        if (decision == "create_new") return null
        if (result.confidence < AI_RESOLUTION_MIN_CONFIDENCE) return null
        val targetId = result.targetCardId ?: return null
        val targetCard = activeCards.firstOrNull { it.id == targetId } ?: return null
        return ManualDuplicateCandidate(
            existingCard = targetCard,
            existingText = buildAiResolutionDialogText(targetCard, result),
            newText = text.trim(),
            debugText = buildAiResolutionDebugText(result)
        )
    }

    private fun buildAiResolutionDialogText(card: RadarCardEntity, result: AiCaptureResolutionResult): String {
        val liveMessage = result.userMessage.ifBlank {
            "Yandex AI считает, что новая фраза может относиться к этой карточке."
        }
        return liveMessage + "\n\n" +
            "Карточка:\n${card.sourceQuote.ifBlank { card.title }}\n\n" +
            buildAiResolutionDebugText(result)
    }

    private fun buildAiResolutionDebugText(result: AiCaptureResolutionResult): String {
        return "AI Resolution:\n" +
            "- решение: ${result.decision}\n" +
            "- уверенность: ${"%.2f".format(Locale.US, result.confidence)}\n" +
            "- причина: ${result.developerReason.ifBlank { "—" }}\n" +
            "- намерение: ${result.normalizedIntent.ifBlank { "—" }}\n" +
            "- объект: ${result.normalizedObject.ifBlank { "—" }}\n" +
            "- человек: ${result.normalizedPerson.ifBlank { "—" }}\n" +
            "- место: ${result.normalizedPlace.ifBlank { "—" }}\n" +
            "- старое время: ${result.normalizedOldTime.ifBlank { "—" }}\n" +
            "- новое время: ${result.normalizedNewTime.ifBlank { "—" }}"
    }

    private fun formatDueForAi(dueAt: Long): String {
        return SimpleDateFormat("EEEE dd.MM HH:mm", Locale("ru", "RU")).format(Date(dueAt))
    }

    suspend fun saveCaptureAndLoadRadar(text: String, mode: RadarCardViewMode): CaptureRadarScreenState {
        val result = repository.addCapture(text)
        val snapshot = loadRadarSnapshot(mode)
        val createdCard = database.radarCardDao().getCardById(result.cardId)
        return CaptureRadarScreenState(
            message = "Захват #${result.captureId} сохранён. Карточка #${result.cardId} создана.",
            cards = snapshot.cards,
            counters = snapshot.counters,
            createdCard = createdCard
        )
    }

    suspend fun replaceManualCardAndLoadRadar(text: String, replaceCardId: Long, mode: RadarCardViewMode): CaptureRadarScreenState {
        val result = repository.addCapture(text)
        database.radarCardDao().archiveCard(replaceCardId, System.currentTimeMillis())
        val snapshot = loadRadarSnapshot(mode)
        val createdCard = database.radarCardDao().getCardById(result.cardId)
        return CaptureRadarScreenState(
            message = "Похожая карточка #$replaceCardId заменена новой карточкой #${result.cardId}.",
            cards = snapshot.cards,
            counters = snapshot.counters,
            createdCard = createdCard,
            cancelledReminderCardId = replaceCardId
        )
    }

    suspend fun saveCaptureAndLoadRadar(text: String, showHidden: Boolean): CaptureRadarScreenState {
        return saveCaptureAndLoadRadar(
            text = text,
            mode = if (showHidden) RadarCardViewMode.HIDDEN else RadarCardViewMode.ACTIVE
        )
    }

    suspend fun saveCaptureAndLoadRadar(text: String): CaptureRadarScreenState {
        return saveCaptureAndLoadRadar(text, RadarCardViewMode.ACTIVE)
    }

    suspend fun markCardDoneAndLoadRadar(cardId: Long, mode: RadarCardViewMode): CaptureRadarScreenState {
        database.radarCardDao().markDone(cardId, System.currentTimeMillis())
        val snapshot = loadRadarSnapshot(mode)
        return CaptureRadarScreenState("Карточка #$cardId отмечена как готовая.", snapshot.cards, snapshot.counters, cancelledReminderCardId = cardId)
    }

    suspend fun markCardDoneAndLoadRadar(cardId: Long, showHidden: Boolean): CaptureRadarScreenState {
        return markCardDoneAndLoadRadar(cardId, if (showHidden) RadarCardViewMode.HIDDEN else RadarCardViewMode.ACTIVE)
    }

    suspend fun markCardDoneAndLoadRadar(cardId: Long): CaptureRadarScreenState {
        return markCardDoneAndLoadRadar(cardId, RadarCardViewMode.ACTIVE)
    }

    suspend fun hideCardAndLoadRadar(cardId: Long): CaptureRadarScreenState {
        database.radarCardDao().hideCard(cardId, System.currentTimeMillis())
        val snapshot = loadRadarSnapshot(RadarCardViewMode.ACTIVE)
        return CaptureRadarScreenState("Карточка #$cardId скрыта. Её можно вернуть из раздела скрытых.", snapshot.cards, snapshot.counters, cancelledReminderCardId = cardId)
    }

    suspend fun restoreHiddenCardAndLoadRadar(cardId: Long, showHidden: Boolean): CaptureRadarScreenState {
        database.radarCardDao().restoreCardToActive(cardId, System.currentTimeMillis())
        val restoredCard = database.radarCardDao().getCardById(cardId)
        val mode = if (showHidden) RadarCardViewMode.HIDDEN else RadarCardViewMode.ACTIVE
        val snapshot = loadRadarSnapshot(mode)
        return CaptureRadarScreenState("Карточка #$cardId возвращена.", snapshot.cards, snapshot.counters, restoredCard = restoredCard)
    }

    suspend fun restoreCardToActiveAndLoadRadar(cardId: Long, mode: RadarCardViewMode): CaptureRadarScreenState {
        database.radarCardDao().restoreCardToActive(cardId, System.currentTimeMillis())
        val restoredCard = database.radarCardDao().getCardById(cardId)
        val snapshot = loadRadarSnapshot(mode)
        return CaptureRadarScreenState("Карточка #$cardId возвращена.", snapshot.cards, snapshot.counters, restoredCard = restoredCard)
    }

    suspend fun deleteCardAndLoadRadar(cardId: Long, mode: RadarCardViewMode): CaptureRadarScreenState {
        val deletedCard = database.radarCardDao().getCardById(cardId)
        database.radarCardDao().archiveCard(cardId, System.currentTimeMillis())
        if (deletedCard?.type == "CALENDAR") {
            CalendarBackgroundScheduler(context).runOnceSoon()
        }
        val snapshot = loadRadarSnapshot(mode)
        return CaptureRadarScreenState(
            message = "Карточка #$cardId убрана.",
            cards = snapshot.cards,
            counters = snapshot.counters,
            deletedCard = deletedCard,
            deletedCardId = cardId,
            cancelledReminderCardId = cardId,
            requestCalendarSync = deletedCard?.type == "CALENDAR"
        )
    }

    companion object {
        private const val AI_RESOLUTION_MIN_CONFIDENCE = 0.55
    }
}

enum class RadarCardViewMode {
    ACTIVE,
    HIDDEN,
    DONE
}

data class RadarCounters(val active: Int, val hidden: Int, val done: Int)

data class RadarSnapshot(val cards: List<RadarCardEntity>, val counters: RadarCounters)

data class ManualDuplicateCandidate(
    val existingCard: RadarCardEntity,
    val existingText: String,
    val newText: String,
    val debugText: String = ""
)

data class CaptureRadarScreenState(
    val message: String,
    val cards: List<RadarCardEntity>,
    val counters: RadarCounters,
    val createdCard: RadarCardEntity? = null,
    val restoredCard: RadarCardEntity? = null,
    val deletedCard: RadarCardEntity? = null,
    val deletedCardId: Long? = null,
    val cancelledReminderCardId: Long? = null,
    val requestCalendarSync: Boolean = false
)
