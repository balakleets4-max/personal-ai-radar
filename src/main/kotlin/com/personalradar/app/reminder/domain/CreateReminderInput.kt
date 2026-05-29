package com.personalradar.app.reminder.domain

data class CreateReminderInput(
    val cardId: Long,
    val scheduledAt: Long
)
