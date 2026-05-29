package com.personalradar.app.device.usecase

import com.personalradar.app.core.database.dao.DeviceCapabilityDao
import com.personalradar.app.core.database.entity.DeviceCapabilityEntity
import com.personalradar.app.core.time.TimeProvider
import com.personalradar.app.device.capability.CapabilityChecker
import com.personalradar.app.device.domain.CapabilityCheckResult

class RefreshDeviceCapabilitiesUseCase(
    private val capabilityChecker: CapabilityChecker,
    private val deviceCapabilityDao: DeviceCapabilityDao,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke(): Int {
        val results = buildList {
            add(capabilityChecker.checkNotifications())
            add(capabilityChecker.checkExactAlarm())
            add(capabilityChecker.checkInternet())
            add(capabilityChecker.checkBatteryOptimization())
            add(capabilityChecker.checkBackgroundWork())
            addAll(capabilityChecker.checkAppPolicies())
        }

        val now = timeProvider.nowMillis()
        for (result in results) {
            upsert(result, now)
        }
        return results.size
    }

    private suspend fun upsert(
        result: CapabilityCheckResult,
        checkedAt: Long
    ) {
        val existing = deviceCapabilityDao.getCapability(result.capability)
        if (existing == null) {
            deviceCapabilityDao.insertCapability(
                DeviceCapabilityEntity(
                    capability = result.capability,
                    category = result.category,
                    state = result.state,
                    checkedAt = checkedAt,
                    explanation = result.explanation,
                    canRequest = result.canRequest
                )
            )
        } else {
            deviceCapabilityDao.updateCapabilityState(
                capability = result.capability,
                state = result.state,
                checkedAt = checkedAt,
                explanation = result.explanation,
                canRequest = result.canRequest
            )
        }
    }
}
