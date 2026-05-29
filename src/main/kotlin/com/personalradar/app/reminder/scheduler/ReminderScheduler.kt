package com.personalradar.app.reminder.scheduler

import com.personalradar.app.reminder.domain.ReminderScheduleWarning

interface ReminderScheduler {
    suspend fun schedule(reminderId: Long, title: String, scheduledAt: Long, deliveryMode: String): ReminderScheduleWarning?
    suspend fun cancel(reminderId: Long)
}
