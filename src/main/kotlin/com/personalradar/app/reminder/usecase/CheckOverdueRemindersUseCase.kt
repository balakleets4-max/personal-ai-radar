package com.personalradar.app.reminder.usecase

import com.personalradar.app.core.database.dao.ReminderDao
import com.personalradar.app.core.time.TimeProvider
import com.personalradar.app.core.transaction.TransactionRunner

data class OverdueReminderCheckResult(
    val overdueCount: Int,
    val markedMissedCount: Int
)

class CheckOverdueRemindersUseCase(
    private val reminderDao: ReminderDao,
    private val transactionRunner: TransactionRunner,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke(): OverdueReminderCheckResult {
        val now = timeProvider.nowMillis()
        val missedThreshold = now - 24L * 60L * 60L * 1000L
        return transactionRunner.runInTransaction {
            val due = reminderDao.getDueReminders(now)
            val missed = due.filter { it.scheduledAt < missedThreshold }
            missed.forEach { reminderDao.markMissed(it.id, now) }
            OverdueReminderCheckResult(due.size, missed.size)
        }
    }
}
