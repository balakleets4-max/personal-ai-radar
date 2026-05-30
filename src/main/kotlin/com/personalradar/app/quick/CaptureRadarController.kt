package com.personalradar.app.quick

import com.personalradar.app.core.database.AppDatabase
import com.personalradar.app.core.database.entity.RadarCardEntity

class CaptureRadarController(
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

    suspend fun saveCaptureAndLoadRadar(text: String, mode: RadarCardViewMode): CaptureRadarScreenState {
        val result = repository.addCapture(text)
        val snapshot = loadRadarSnapshot(mode)
        val createdCard = database.radarCardDao().getCardById(result.cardId)
        return CaptureRadarScreenState(
            message = "Захват #${result.captureId} сохранён. Карточка Радара #${result.cardId} создана.",
            cards = snapshot.cards,
            counters = snapshot.counters,
            createdCard = createdCard
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
        return CaptureRadarScreenState(
            message = "Карточка #$cardId отмечена как готовая.",
            cards = snapshot.cards,
            counters = snapshot.counters
        )
    }

    suspend fun markCardDoneAndLoadRadar(cardId: Long, showHidden: Boolean): CaptureRadarScreenState {
        return markCardDoneAndLoadRadar(
            cardId = cardId,
            mode = if (showHidden) RadarCardViewMode.HIDDEN else RadarCardViewMode.ACTIVE
        )
    }

    suspend fun markCardDoneAndLoadRadar(cardId: Long): CaptureRadarScreenState {
        return markCardDoneAndLoadRadar(cardId, RadarCardViewMode.ACTIVE)
    }

    suspend fun hideCardAndLoadRadar(cardId: Long): CaptureRadarScreenState {
        database.radarCardDao().hideCard(cardId, System.currentTimeMillis())
        val snapshot = loadRadarSnapshot(RadarCardViewMode.ACTIVE)
        return CaptureRadarScreenState(
            message = "Карточка #$cardId скрыта. Её можно вернуть из раздела скрытых.",
            cards = snapshot.cards,
            counters = snapshot.counters
        )
    }

    suspend fun restoreHiddenCardAndLoadRadar(cardId: Long, showHidden: Boolean): CaptureRadarScreenState {
        database.radarCardDao().restoreCardToActive(cardId, System.currentTimeMillis())
        val mode = if (showHidden) RadarCardViewMode.HIDDEN else RadarCardViewMode.ACTIVE
        val snapshot = loadRadarSnapshot(mode)
        return CaptureRadarScreenState(
            message = "Карточка #$cardId возвращена в Радар.",
            cards = snapshot.cards,
            counters = snapshot.counters
        )
    }

    suspend fun restoreCardToActiveAndLoadRadar(cardId: Long, mode: RadarCardViewMode): CaptureRadarScreenState {
        database.radarCardDao().restoreCardToActive(cardId, System.currentTimeMillis())
        val snapshot = loadRadarSnapshot(mode)
        return CaptureRadarScreenState(
            message = "Карточка #$cardId возвращена в Радар.",
            cards = snapshot.cards,
            counters = snapshot.counters
        )
    }
}

enum class RadarCardViewMode {
    ACTIVE,
    HIDDEN,
    DONE
}

data class RadarCounters(
    val active: Int,
    val hidden: Int,
    val done: Int
)

data class RadarSnapshot(
    val cards: List<RadarCardEntity>,
    val counters: RadarCounters
)

data class CaptureRadarScreenState(
    val message: String,
    val cards: List<RadarCardEntity>,
    val counters: RadarCounters,
    val createdCard: RadarCardEntity? = null
)
