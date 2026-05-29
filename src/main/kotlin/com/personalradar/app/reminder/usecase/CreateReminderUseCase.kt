package com.personalradar.app.reminder.usecase

import com.personalradar.app.core.database.dao.*
import com.personalradar.app.core.database.entity.*
import com.personalradar.app.core.model.*
import com.personalradar.app.core.time.TimeProvider
import com.personalradar.app.core.transaction.TransactionRunner
import com.personalradar.app.reminder.domain.*

class CreateReminderUseCase(
    private val radarCardDao: RadarCardDao,
    private val reminderDao: ReminderDao,
    private val actionDao: ActionDao,
    private val deviceCapabilityDao: DeviceCapabilityDao,
    private val pendingSystemActionDao: PendingSystemActionDao,
    private val transactionRunner: TransactionRunner,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke(input: CreateReminderInput): CreateReminderResult {
        val now = timeProvider.nowMillis()
        require(input.scheduledAt > now) { "Reminder time must be in the future" }

        val card = radarCardDao.getCardById(input.cardId)
            ?: error("RadarCard not found: ${input.cardId}")

        val notifications = deviceCapabilityDao.getCapabilityState(DeviceCapabilityName.NOTIFICATIONS)
        val exactAlarm = deviceCapabilityDao.getCapabilityState(DeviceCapabilityName.EXACT_ALARM)

        val warnings = mutableListOf<CreateReminderWarning>()
        val deliveryMode = when {
            notifications != CapabilityState.AVAILABLE -> {
                warnings.add(CreateReminderWarning.NotificationsDenied)
                ReminderDeliveryMode.IN_APP_ONLY
            }
            exactAlarm == CapabilityState.AVAILABLE -> ReminderDeliveryMode.EXACT
            else -> {
                warnings.add(CreateReminderWarning.ExactAlarmUnavailable)
                ReminderDeliveryMode.APPROXIMATE
            }
        }

        val schedulerState = if (deliveryMode == ReminderDeliveryMode.IN_APP_ONLY) {
            ReminderSchedulerState.NOT_REQUIRED
        } else {
            ReminderSchedulerState.PENDING
        }

        val reminderId = transactionRunner.runInTransaction {
            val id = reminderDao.insertReminder(
                ReminderEntity(
                    radarCardId = card.id,
                    captureId = card.captureId,
                    title = card.title,
                    scheduledAt = input.scheduledAt,
                    status = ReminderStatus.SCHEDULED,
                    deliveryMode = deliveryMode,
                    schedulerState = schedulerState,
                    permissionStateAtCreation = "{\"notifications\":\"$notifications\",\"exactAlarm\":\"$exactAlarm\"}",
                    createdAt = now,
                    updatedAt = now
                )
            )
            radarCardDao.setHasReminder(card.id, true, now)
            actionDao.insertAction(
                ActionEntity(
                    radarCardId = card.id,
                    captureId = card.captureId,
                    type = ActionType.CREATE_REMINDER,
                    createdAt = now,
                    value = input.scheduledAt.toString()
                )
            )
            if (deliveryMode != ReminderDeliveryMode.IN_APP_ONLY) {
                pendingSystemActionDao.insertPendingAction(
                    PendingSystemActionEntity(
                        type = PendingSystemActionType.SCHEDULE_REMINDER,
                        status = PendingSystemActionStatus.PENDING,
                        createdAt = now,
                        updatedAt = now,
                        relatedReminderId = id,
                        relatedCardId = card.id,
                        payloadJson = "{\"title\":\"${jsonEscape(card.title)}\",\"scheduledAt\":${input.scheduledAt},\"deliveryMode\":\"$deliveryMode\"}"
                    )
                )
            }
            id
        }

        return CreateReminderResult(
            reminderId = reminderId,
            deliveryMode = deliveryMode,
            message = when (deliveryMode) {
                ReminderDeliveryMode.EXACT -> "Напоминание создано. Я смогу напомнить в указанное время."
                ReminderDeliveryMode.APPROXIMATE -> "Напоминание создано. Точная минута может быть недоступна из-за настроек Android."
                else -> "Напоминание сохранено внутри приложения. Уведомления выключены, поэтому оно будет видно на экране “Сегодня”."
            },
            warnings = warnings
        )
    }


    private fun jsonEscape(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", " ")
            .replace("\r", " ")
    }
}
