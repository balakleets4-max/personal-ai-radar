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
            message = "Saved Capture #${result.captureId}; Radar card #${result.cardId} created.",
            cards = cards
        )
    }
}

data class CaptureRadarScreenState(
    val message: String,
    val cards: List<RadarCardEntity>
)
