package com.personalradar.app.reminder.usecase

import com.personalradar.app.core.database.dao.PendingSystemActionDao
import com.personalradar.app.core.database.dao.ReminderDao
import com.personalradar.app.core.model.PendingSystemActionStatus
import com.personalradar.app.core.model.PendingSystemActionType
import com.personalradar.app.core.model.ReminderSchedulerState
import com.personalradar.app.core.time.TimeProvider
import com.personalradar.app.reminder.scheduler.ReminderScheduler

class ProcessPendingSystemActionsUseCase(
    private val pendingSystemActionDao: PendingSystemActionDao,
    private val reminderDao: ReminderDao,
    private val reminderScheduler: ReminderScheduler,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke(
        limit: Int = 20,
        maxAttempts: Int = 3
    ): Int {
        val actions = pendingSystemActionDao.getRunnableActions(
            limit = limit,
            maxAttempts = maxAttempts
        )
        var done = 0

        for (action in actions) {
            val now = timeProvider.nowMillis()
            try {
                pendingSystemActionDao.updateStatus(
                    id = action.id,
                    status = PendingSystemActionStatus.IN_PROGRESS,
                    now = now,
                    attemptDelta = 0,
                    lastError = null
                )

                when (action.type) {
                    PendingSystemActionType.SCHEDULE_REMINDER -> {
                        val reminderId = action.relatedReminderId ?: continue
                        val reminder = reminderDao.getReminderById(reminderId) ?: continue
                        val warning = reminderScheduler.schedule(
                            reminderId = reminder.id,
                            title = reminder.title,
                            scheduledAt = reminder.scheduledAt,
                            deliveryMode = reminder.deliveryMode
                        )

                        if (warning == null) {
                            val doneAt = timeProvider.nowMillis()
                            reminderDao.updateSchedulerState(
                                reminderId = reminder.id,
                                schedulerState = ReminderSchedulerState.SCHEDULED,
                                now = doneAt
                            )
                            pendingSystemActionDao.updateStatus(
                                id = action.id,
                                status = PendingSystemActionStatus.DONE,
                                now = doneAt,
                                attemptDelta = 0,
                                lastError = null
                            )
                            done++
                        } else {
                            markFailed(action.id, action.relatedReminderId, warning.reason)
                        }
                    }
                }
            } catch (t: Throwable) {
                markFailed(action.id, action.relatedReminderId, t.message ?: t::class.simpleName.orEmpty())
            }
        }

        return done
    }

    private suspend fun markFailed(
        actionId: Long,
        reminderId: Long?,
        reason: String
    ) {
        val failedAt = timeProvider.nowMillis()
        if (reminderId != null) {
            reminderDao.updateSchedulerState(
                reminderId = reminderId,
                schedulerState = ReminderSchedulerState.FAILED,
                now = failedAt
            )
        }
        pendingSystemActionDao.updateStatus(
            id = actionId,
            status = PendingSystemActionStatus.FAILED,
            now = failedAt,
            attemptDelta = 1,
            lastError = reason.take(240)
        )
    }
}
