package com.sjdroid.paymentanalytics.domain.usecase

import com.sjdroid.paymentanalytics.domain.repository.AnalyticsRepository
class CheckDeviceReadinessUseCase constructor(
    private val repository: AnalyticsRepository
) {
    suspend operator fun invoke(): Result<Boolean> {
        return repository.isServiceReady()
    }
}