package com.sjdroid.paymentanalytics.domain.repository

import com.sjdroid.paymentanalytics.AnalyticsData
import kotlinx.coroutines.flow.Flow

interface AnalyticsRepository {
    
    suspend fun getCurrentStats(): Result<AnalyticsData>
    
    suspend fun isServiceReady(): Result<Boolean>
    
    suspend fun resetStats(): Result<Unit>
    
    fun getConnectionState(): Flow<ConnectionState>
    
    suspend fun bindToService(): Result<Unit>
    
    suspend fun unbindFromService()
}

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}