package com.personalradar.app.appevent.usecase

import com.personalradar.app.core.database.dao.AppEventLogDao
import com.personalradar.app.core.time.TimeProvider

class CleanOldAppEventLogsUseCase(
    private val appEventLogDao: AppEventLogDao,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke(retentionDays: Int = 30) {
        val before = timeProvider.nowMillis() - retentionDays * 24L * 60L * 60L * 1000L
        appEventLogDao.deleteOldEvents(before)
    }
}
