package com.sjdroid.paymentanalytics.domain.usecase

import com.sjdroid.paymentanalytics.domain.repository.AnalyticsRepository
import com.sjdroid.paymentanalytics.domain.repository.ConnectionState
import kotlinx.coroutines.flow.Flow
class ManageServiceConnectionUseCase constructor(
    private val repository: AnalyticsRepository
) {
    suspend fun bindToService(): Result<Unit> {
        return repository.bindToService()
    }
    
    suspend fun unbindFromService() {
        repository.unbindFromService()
    }
    
    fun getConnectionState(): Flow<ConnectionState> {
        return repository.getConnectionState()
    }
}