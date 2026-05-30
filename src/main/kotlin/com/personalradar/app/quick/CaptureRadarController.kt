package com.personalradar.app.quick

import com.personalradar.app.core.database.AppDatabase
import com.personalradar.app.core.database.entity.RadarCardEntity

class CaptureRadarController(
    private val database: AppDatabase,
    private val repository: QuickCaptureRepository
) {
    suspend fun loadRadarCards(): List<RadarCardEntity> {
        return database.radarCardDao().getActiveCardsSnapshot()
    }

    suspend fun saveCaptureAndLoadRadar(text: String): CaptureRadarScreenState {
        val result = repository.addCapture(text)
        val cards = loadRadarCards()
        return CaptureRadarScreenState(
            message = "Захват #${result.captureId} сохранён. Карточка Радара #${result.cardId} создана.",
            cards = cards
        )
    }

    suspend fun markCardDoneAndLoadRadar(cardId: Long): CaptureRadarScreenState {
        database.radarCardDao().markDone(cardId, System.currentTimeMillis())
        return CaptureRadarScreenState(
            message = "Карточка #$cardId отмечена как готовая.",
            cards = loadRadarCards()
        )
    }

    suspend fun hideCardAndLoadRadar(cardId: Long): CaptureRadarScreenState {
        database.radarCardDao().hideCard(cardId, System.currentTimeMillis())
        return CaptureRadarScreenState(
            message = "Карточка #$cardId скрыта.",
            cards = loadRadarCards()
        )
    }
}

data class CaptureRadarScreenState(
    val message: String,
    val cards: List<RadarCardEntity>
)
