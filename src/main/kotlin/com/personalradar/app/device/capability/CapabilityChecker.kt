package com.personalradar.app.device.capability

import com.personalradar.app.device.domain.CapabilityCheckResult

interface CapabilityChecker {
    suspend fun checkNotifications(): CapabilityCheckResult
    suspend fun checkExactAlarm(): CapabilityCheckResult
    suspend fun checkInternet(): CapabilityCheckResult
    suspend fun checkBatteryOptimization(): CapabilityCheckResult
    suspend fun checkBackgroundWork(): CapabilityCheckResult
    suspend fun checkAppPolicies(): List<CapabilityCheckResult>
}
