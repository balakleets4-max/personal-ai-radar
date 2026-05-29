package com.personalradar.app.reminder.domain

data class CreateReminderResult(
    val reminderId: Long,
    val deliveryMode: String,
    val message: String,
    val warnings: List<CreateReminderWarning>
)
