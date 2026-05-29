package com.personalradar.app.radar.usecase

import com.personalradar.app.core.database.dao.ActionDao
import com.personalradar.app.core.database.dao.MemoryFactDao
import com.personalradar.app.core.database.dao.RadarCardDao
import com.personalradar.app.core.database.entity.ActionEntity
import com.personalradar.app.core.database.entity.MemoryFactEntity
import com.personalradar.app.core.model.ActionType
import com.personalradar.app.core.model.MemoryFactType
import com.personalradar.app.core.model.RadarCardStatus
import com.personalradar.app.core.model.RadarCardType
import com.personalradar.app.core.time.TimeProvider
import com.personalradar.app.core.transaction.TransactionRunner

sealed class CardActionValue {
    data class SnoozeUntil(val until: Long) : CardActionValue()
    data class DueAt(val dueAt: Long?) : CardActionValue()
    data object None : CardActionValue()
}

data class PerformCardActionResult(
    val cardId: Long,
    val actionId: Long,
    val newStatus: String
)

class PerformCardActionUseCase(
    private val radarCardDao: RadarCardDao,
    private val actionDao: ActionDao,
    private val memoryFactDao: MemoryFactDao,
    private val transactionRunner: TransactionRunner,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke(
        cardId: Long,
        actionType: String,
        value: CardActionValue = CardActionValue.None
    ): PerformCardActionResult {
        val now = timeProvider.nowMillis()
        return transactionRunner.runInTransaction {
            val card = radarCardDao.getCardById(cardId) ?: error("RadarCard not found: $cardId")
            when (actionType) {
                ActionType.MARK_DONE -> radarCardDao.markDone(cardId, now)
                ActionType.HIDE -> radarCardDao.hideCard(cardId, now)
                ActionType.SNOOZE -> {
                    val until = (value as? CardActionValue.SnoozeUntil)?.until
                        ?: error("SNOOZE requires SnoozeUntil")
                    require(until > now) { "Snooze time must be in the future" }
                    radarCardDao.snoozeCard(cardId, until, now)
                }
                ActionType.EDIT_DUE_DATE -> {
                    val dueAt = (value as? CardActionValue.DueAt)?.dueAt
                    radarCardDao.updateDueDate(cardId, dueAt, now)
                    if (card.type == RadarCardType.TASK_HIDDEN && dueAt != null) {
                        radarCardDao.updateCardType(cardId, RadarCardType.TASK_EXPLICIT, now)
                    }
                }
                ActionType.MAKE_TASK -> radarCardDao.updateCardType(cardId, RadarCardType.TASK_EXPLICIT, now)
                ActionType.ARCHIVE -> radarCardDao.archiveCard(cardId, now)
                else -> error("Unsupported card action in v0.1.4: $actionType")
            }

            val actionId = actionDao.insertAction(
                ActionEntity(
                    radarCardId = card.id,
                    captureId = card.captureId,
                    type = actionType,
                    createdAt = now,
                    value = when (value) {
                        is CardActionValue.SnoozeUntil -> value.until.toString()
                        is CardActionValue.DueAt -> value.dueAt?.toString()
                        CardActionValue.None -> null
                    }
                )
            )

            updateMemoryFact(card.type, actionType, now)

            val newStatus = when (actionType) {
                ActionType.MARK_DONE -> RadarCardStatus.DONE
                ActionType.HIDE -> RadarCardStatus.HIDDEN
                ActionType.SNOOZE -> RadarCardStatus.SNOOZED
                ActionType.ARCHIVE -> RadarCardStatus.ARCHIVED
                else -> RadarCardStatus.ACTIVE
            }
            PerformCardActionResult(cardId, actionId, newStatus)
        }
    }

    private suspend fun updateMemoryFact(cardType: String, actionType: String, now: Long) {
        val key = "card_type:$cardType"
        val value = when (actionType) {
            ActionType.HIDE -> "often_hidden"
            ActionType.MARK_DONE -> "useful"
            ActionType.MAKE_TASK -> "useful_as_task"
            else -> "useful"
        }
        val delta = when (actionType) {
            ActionType.HIDE -> -0.02f
            else -> 0.03f
        }
        val existing = memoryFactDao.getMemoryFact(MemoryFactType.CARD_PATTERN, key)
        if (existing == null) {
            memoryFactDao.upsertMemoryFact(
                MemoryFactEntity(
                    type = MemoryFactType.CARD_PATTERN,
                    key = key,
                    value = value,
                    confidence = (0.5f + delta).coerceIn(0f, 1f),
                    evidenceCount = 1,
                    createdAt = now,
                    updatedAt = now
                )
            )
        } else {
            memoryFactDao.upsertMemoryFact(
                existing.copy(
                    value = value,
                    confidence = (existing.confidence + delta).coerceIn(0f, 1f),
                    evidenceCount = existing.evidenceCount + 1,
                    updatedAt = now
                )
            )
        }
    }
}
