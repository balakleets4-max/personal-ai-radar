package com.personalradar.app.core.time

interface TimeProvider {
    fun nowMillis(): Long
    fun todayStartMillis(): Long
    fun todayEndMillis(): Long
    fun dateString(timestamp: Long): String
}
