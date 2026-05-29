package com.personalradar.app.reminder.domain

sealed class CreateReminderWarning {
    data object NotificationsDenied : CreateReminderWarning()
    data object ExactAlarmUnavailable : CreateReminderWarning()
    data class SchedulerFailed(val reason: String) : CreateReminderWarning()
}
