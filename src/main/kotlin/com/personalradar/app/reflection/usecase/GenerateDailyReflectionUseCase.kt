package com.personalradar.app.reflection.usecase

import com.personalradar.app.core.database.dao.ActionDao
import com.personalradar.app.core.database.dao.CaptureDao
import com.personalradar.app.core.database.dao.RadarCardDao
import com.personalradar.app.core.database.dao.ReflectionDao
import com.personalradar.app.core.database.dao.TopicDao
import com.personalradar.app.core.database.entity.ReflectionLogEntity
import com.personalradar.app.core.model.ActionType
import com.personalradar.app.core.time.TimeProvider

class GenerateDailyReflectionUseCase(
    private val captureDao: CaptureDao,
    private val radarCardDao: RadarCardDao,
    private val actionDao: ActionDao,
    private val topicDao: TopicDao,
    private val reflectionDao: ReflectionDao,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke(
        date: String,
        dayStart: Long,
        dayEnd: Long
    ): ReflectionLogEntity {
        val topTopics = topicDao.getTopicsSeenBetween(
            from = dayStart,
            to = dayEnd,
            limit = 5
        ).joinToString(
            prefix = "[",
            postfix = "]"
        ) { topic -> "\"" + topic.name.replace("\"", "'") + "\"" }

        val log = ReflectionLogEntity(
            date = date,
            captureCount = captureDao.countCapturesBetween(dayStart, dayEnd),
            radarCardCount = radarCardDao.countCardsCreatedBetween(dayStart, dayEnd),
            completedActionCount = actionDao.countActionsByTypeBetween(ActionType.MARK_DONE, dayStart, dayEnd),
            hiddenCardCount = actionDao.countActionsByTypeBetween(ActionType.HIDE, dayStart, dayEnd),
            topTopicsJson = topTopics,
            createdAt = timeProvider.nowMillis()
        )
        reflectionDao.upsertReflectionLog(log)
        return log
    }
}
