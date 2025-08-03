package com.sjdroid.paymentanalytics.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import com.sjdroid.paymentanalytics.IAnalyticsService
import com.sjdroid.paymentanalytics.AnalyticsData
import com.sjdroid.paymentanalytics.domain.repository.AnalyticsRepository
import com.sjdroid.paymentanalytics.domain.repository.ConnectionState
import com.sjdroid.paymentanalytics.service.AnalyticsService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AnalyticsRepositoryImpl(
    private val context: Context
) : AnalyticsRepository {

    private var analyticsService: IAnalyticsService? = null
    private var isBound = false

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    private val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    // Rate limiting
    private var lastStatsCall = 0L
    private var cachedStats: AnalyticsData? = null
    private val minCallInterval = 1000L // 1 second minimum between calls

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            analyticsService = IAnalyticsService.Stub.asInterface(service)
            isBound = true
            _connectionState.value = ConnectionState.Connected
            
            service.linkToDeath({
                _connectionState.value = ConnectionState.Error("Service died")
                analyticsService = null
                isBound = false
            }, 0)
        }

        override fun onServiceDisconnected(className: ComponentName) {
            analyticsService = null
            isBound = false
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    override suspend fun getCurrentStats(): Result<AnalyticsData> {
        return try {
            val service = analyticsService ?: return Result.failure(
                IllegalStateException("Service not connected")
            )
            
            val currentTime = System.currentTimeMillis()
            
            // Rate limiting - return cached data if called too frequently
            if (cachedStats != null && (currentTime - lastStatsCall) < minCallInterval) {
                return Result.success(cachedStats!!)
            }
            
            val stats = service.getCurrentStats()
            lastStatsCall = currentTime
            cachedStats = stats
            
            Result.success(stats)
        } catch (e: RemoteException) {
            Result.failure(e)
        }
    }

    override suspend fun isServiceReady(): Result<Boolean> {
        return try {
            val service = analyticsService ?: return Result.failure(
                IllegalStateException("Service not connected")
            )
            val ready = service.isServiceReady()
            Result.success(ready)
        } catch (e: RemoteException) {
            Result.failure(e)
        }
    }

    override suspend fun resetStats(): Result<Unit> {
        return try {
            val service = analyticsService ?: return Result.failure(
                IllegalStateException("Service not connected")
            )
            service.resetStats()
            Result.success(Unit)
        } catch (e: RemoteException) {
            Result.failure(e)
        }
    }

    override fun getConnectionState(): Flow<ConnectionState> = connectionState

    override suspend fun bindToService(): Result<Unit> {
        return suspendCancellableCoroutine { continuation ->
            try {
                _connectionState.value = ConnectionState.Connecting
                
                val intent = Intent(context, AnalyticsService::class.java)
                val bound = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
                
                if (bound) {
                    continuation.resume(Result.success(Unit))
                } else {
                    _connectionState.value = ConnectionState.Error("Failed to bind to service")
                    continuation.resume(Result.failure(Exception("Failed to bind to service")))
                }
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.Error(e.message ?: "Unknown error")
                continuation.resume(Result.failure(e))
            }
        }
    }

    override suspend fun unbindFromService() {
        if (isBound) {
            context.unbindService(serviceConnection)
            analyticsService = null
            isBound = false
            _connectionState.value = ConnectionState.Disconnected
        }
    }
}