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

    suspend fun loadRadarCards(showHidden: Boolean = false): List<RadarCardEntity> {
        return loadRadarCards(if (showHidden) RadarCardViewMode.HIDDEN else RadarCardViewMode.ACTIVE)
    }

    suspend fun saveCaptureAndLoadRadar(text: String, mode: RadarCardViewMode): CaptureRadarScreenState {
        val result = repository.addCapture(text)
        return CaptureRadarScreenState(
            message = "Захват #${result.captureId} сохранён. Карточка Радара #${result.cardId} создана.",
            cards = loadRadarCards(mode)
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
        return CaptureRadarScreenState(
            message = "Карточка #$cardId отмечена как готовая.",
            cards = loadRadarCards(mode)
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
        return CaptureRadarScreenState(
            message = "Карточка #$cardId скрыта. Её можно вернуть из раздела скрытых.",
            cards = loadRadarCards(RadarCardViewMode.ACTIVE)
        )
    }

    suspend fun restoreHiddenCardAndLoadRadar(cardId: Long, showHidden: Boolean): CaptureRadarScreenState {
        database.radarCardDao().restoreCardToActive(cardId, System.currentTimeMillis())
        return CaptureRadarScreenState(
            message = "Карточка #$cardId возвращена в Радар.",
            cards = loadRadarCards(if (showHidden) RadarCardViewMode.HIDDEN else RadarCardViewMode.ACTIVE)
        )
    }

    suspend fun restoreCardToActiveAndLoadRadar(cardId: Long, mode: RadarCardViewMode): CaptureRadarScreenState {
        database.radarCardDao().restoreCardToActive(cardId, System.currentTimeMillis())
        return CaptureRadarScreenState(
            message = "Карточка #$cardId возвращена в Радар.",
            cards = loadRadarCards(mode)
        )
    }
}

enum class RadarCardViewMode {
    ACTIVE,
    HIDDEN,
    DONE
}

data class CaptureRadarScreenState(
    val message: String,
    val cards: List<RadarCardEntity>
)
