package com.sjdroid.paymentanalytics.domain.usecase

import com.sjdroid.paymentanalytics.AnalyticsData
import com.sjdroid.paymentanalytics.domain.repository.AnalyticsRepository
class GetAnalyticsStatsUseCase constructor(
    private val repository: AnalyticsRepository
) {
    suspend operator fun invoke(): Result<AnalyticsData> {
        return repository.getCurrentStats()
    }
}