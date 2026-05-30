package com.personalradar.app.quick

import com.personalradar.app.core.database.AppDatabase
import com.personalradar.app.core.database.entity.RadarCardEntity

class CaptureRadarController(
    private val database: AppDatabase,
    private val repository: QuickCaptureRepository
) {
    suspend fun loadRadarCards(showHidden: Boolean = false): List<RadarCardEntity> {
        return if (showHidden) {
            database.radarCardDao().getHiddenCardsSnapshot()
        } else {
            database.radarCardDao().getActiveCardsSnapshot()
        }
    }

    suspend fun saveCaptureAndLoadRadar(text: String, showHidden: Boolean): CaptureRadarScreenState {
        val result = repository.addCapture(text)
        return CaptureRadarScreenState(
            message = "Захват #${result.captureId} сохранён. Карточка Радара #${result.cardId} создана.",
            cards = loadRadarCards(showHidden)
        )
    }

    suspend fun saveCaptureAndLoadRadar(text: String): CaptureRadarScreenState {
        return saveCaptureAndLoadRadar(text, showHidden = false)
    }

    suspend fun markCardDoneAndLoadRadar(cardId: Long, showHidden: Boolean): CaptureRadarScreenState {
        database.radarCardDao().markDone(cardId, System.currentTimeMillis())
        return CaptureRadarScreenState(
            message = "Карточка #$cardId отмечена как готовая.",
            cards = loadRadarCards(showHidden)
        )
    }

    suspend fun markCardDoneAndLoadRadar(cardId: Long): CaptureRadarScreenState {
        return markCardDoneAndLoadRadar(cardId, showHidden = false)
    }

    suspend fun hideCardAndLoadRadar(cardId: Long): CaptureRadarScreenState {
        database.radarCardDao().hideCard(cardId, System.currentTimeMillis())
        return CaptureRadarScreenState(
            message = "Карточка #$cardId скрыта. Её можно вернуть из раздела скрытых.",
            cards = loadRadarCards(showHidden = false)
        )
    }

    suspend fun restoreHiddenCardAndLoadRadar(cardId: Long, showHidden: Boolean): CaptureRadarScreenState {
        database.radarCardDao().restoreHiddenCard(cardId, System.currentTimeMillis())
        return CaptureRadarScreenState(
            message = "Карточка #$cardId возвращена в Радар.",
            cards = loadRadarCards(showHidden)
        )
    }
}

data class CaptureRadarScreenState(
    val message: String,
    val cards: List<RadarCardEntity>
)
