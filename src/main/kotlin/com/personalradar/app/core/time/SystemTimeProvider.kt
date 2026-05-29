package com.personalradar.app.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class SystemTimeProvider(
    private val zoneId: ZoneId = ZoneId.systemDefault()
) : TimeProvider {
    override fun nowMillis(): Long = System.currentTimeMillis()

    override fun todayStartMillis(): Long = LocalDate.now(zoneId)
        .atStartOfDay(zoneId)
        .toInstant()
        .toEpochMilli()

    override fun todayEndMillis(): Long = LocalDate.now(zoneId)
        .plusDays(1)
        .atStartOfDay(zoneId)
        .toInstant()
        .toEpochMilli() - 1L

    override fun dateString(timestamp: Long): String = Instant.ofEpochMilli(timestamp)
        .atZone(zoneId)
        .toLocalDate()
        .format(DateTimeFormatter.ISO_LOCAL_DATE)
}
