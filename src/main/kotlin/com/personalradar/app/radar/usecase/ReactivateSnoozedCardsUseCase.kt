package com.personalradar.app.radar.usecase

import com.personalradar.app.core.database.dao.RadarCardDao
import com.personalradar.app.core.time.TimeProvider
import com.personalradar.app.core.transaction.TransactionRunner

class ReactivateSnoozedCardsUseCase(
    private val radarCardDao: RadarCardDao,
    private val transactionRunner: TransactionRunner,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke(): Int {
        val now = timeProvider.nowMillis()
        return transactionRunner.runInTransaction {
            val dueCards = radarCardDao.getDueSnoozedCards(now)
            if (dueCards.isNotEmpty()) {
                radarCardDao.reactivateSnoozedCards(dueCards.map { it.id }, now)
            }
            dueCards.size
        }
    }
}
