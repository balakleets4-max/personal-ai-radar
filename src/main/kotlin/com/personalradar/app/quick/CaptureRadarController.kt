package com.personalradar.app.quick

import android.content.Context
import com.personalradar.app.calendar.CalendarBackgroundScheduler
import com.personalradar.app.core.database.AppDatabase
import com.personalradar.app.core.database.entity.RadarCardEntity
import java.util.Locale

class CaptureRadarController(
    private val context: Context,
    private val database: AppDatabase,
    private val repository: QuickCaptureRepository
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
        val newKey = manualSemanticKey(text)
        if (newKey.length < 4) return null
        val best = database.radarCardDao()
            .getActiveCardsSnapshot()
            .filter { it.type != "CALENDAR" }
            .mapNotNull { card ->
                val cardKey = manualSemanticKey(card.sourceQuote.ifBlank { card.title })
                val score = similarityScore(newKey, cardKey)
                if (score >= MANUAL_DUPLICATE_SCORE_THRESHOLD) card to score else null
            }
            .maxByOrNull { it.second }
            ?.first
            ?: return null
        return ManualDuplicateCandidate(best, best.sourceQuote.ifBlank { best.title }, text.trim())
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

    private fun similarityScore(left: String, right: String): Double {
        if (left.isBlank() || right.isBlank()) return 0.0
        if (left == right) return 1.0
        val leftTokens = left.split(' ').filter { it.length > 2 }.toSet()
        val rightTokens = right.split(' ').filter { it.length > 2 }.toSet()
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) return 0.0
        val overlap = leftTokens.intersect(rightTokens).size.toDouble()
        val union = leftTokens.union(rightTokens).size.toDouble()
        return overlap / union
    }

    private fun manualSemanticKey(text: String): String {
        return text
            .lowercase(Locale.getDefault())
            .replace(Regex("\\b\\d{1,2}[:.]\\d{2}\\b"), " ")
            .replace(Regex("\\b(сегодня|завтра|послезавтра|понедельник|понедельника|вторник|вторника|среда|среду|четверг|четверга|пятница|пятницу|суббота|субботу|воскресенье|воскресенья)\\b"), " ")
            .replace(Regex("\\b(утром|днём|днем|вечером|ночью|время|было|поставлено|поставить|напомни|напомнить|мне|пожалуйста|надо|нужно|задача|напоминание|риск|мысль)\\b"), " ")
            .replace(Regex("[^а-яёa-z0-9]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    companion object {
        private const val MANUAL_DUPLICATE_SCORE_THRESHOLD = 0.62
    }
}

enum class RadarCardViewMode {
    ACTIVE,
    HIDDEN,
    DONE
}

data class RadarCounters(val active: Int, val hidden: Int, val done: Int)

data class RadarSnapshot(val cards: List<RadarCardEntity>, val counters: RadarCounters)

data class ManualDuplicateCandidate(val existingCard: RadarCardEntity, val existingText: String, val newText: String)

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
