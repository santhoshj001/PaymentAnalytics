package com.sjdroid.paymentanalytics.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sjdroid.paymentanalytics.domain.repository.ConnectionState
import com.sjdroid.paymentanalytics.domain.usecase.CheckDeviceReadinessUseCase
import com.sjdroid.paymentanalytics.domain.usecase.GetAnalyticsStatsUseCase
import com.sjdroid.paymentanalytics.domain.usecase.ManageServiceConnectionUseCase
import com.sjdroid.paymentanalytics.presentation.model.AnalyticsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
class AnalyticsViewModel constructor(
    private val getAnalyticsStatsUseCase: GetAnalyticsStatsUseCase,
    private val checkDeviceReadinessUseCase: CheckDeviceReadinessUseCase,
    private val manageServiceConnectionUseCase: ManageServiceConnectionUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "AnalyticsViewModel"
    }

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()
    
    private var hasInitialDataLoaded = false

    init {
        observeConnectionState()
        bindToService()
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            manageServiceConnectionUseCase.getConnectionState()
                .combine(_uiState) { connectionState, currentState ->
                    currentState.copy(connectionState = connectionState)
                }
                .collect { newState ->
                    _uiState.value = newState
                    
                    // Only refresh on initial connection, not on every state change
                    if (connectionState is ConnectionState.Connected && !hasInitialDataLoaded) {
                        hasInitialDataLoaded = true
                        refreshAnalyticsData()
                    }
                }
        }
    }

    fun bindToService() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            manageServiceConnectionUseCase.bindToService()
                .onSuccess {
                    Log.d(TAG, "Successfully bound to service")
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to bind to service", error)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to connect to analytics service: ${error.message}"
                    )
                }
        }
    }

    fun refreshAnalyticsData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val statsResult = getAnalyticsStatsUseCase()
                val readinessResult = checkDeviceReadinessUseCase()
                
                if (statsResult.isSuccess && readinessResult.isSuccess) {
                    val analyticsData = statsResult.getOrNull()
                    val isReady = readinessResult.getOrNull() ?: false
                    
                    _uiState.value = _uiState.value.copy(
                        analyticsData = analyticsData,
                        isDeviceReady = isReady,
                        isLoading = false,
                        error = null
                    )
                } else {
                    val error = statsResult.exceptionOrNull() ?: readinessResult.exceptionOrNull()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to get analytics data: ${error?.message}"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing analytics data", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Unexpected error: ${e.message}"
                )
            }
        }
    }

    fun simulateTransactionStart(): String {
        val currentState = _uiState.value
        return when {
            currentState.connectionState !is ConnectionState.Connected -> 
                "Cannot start transaction: Service not connected"
            !currentState.isDeviceReady -> 
                "Cannot start transaction: Device not ready (check battery, memory, CPU)"
            else -> {
                Log.d(TAG, "Transaction started - device ready")
                "Transaction started successfully"
            }
        }
    }

    fun simulatePostTransaction() {
        if (_uiState.value.connectionState is ConnectionState.Connected) {
            Log.d(TAG, "Uploading aggregated performance logs...")
            refreshAnalyticsData()
        }
    }

    fun performMaintenanceDiagnostics() {
        if (_uiState.value.connectionState is ConnectionState.Connected) {
            Log.d(TAG, "Performing maintenance diagnostics...")
            refreshAnalyticsData()
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            manageServiceConnectionUseCase.unbindFromService()
        }
    }

    private val connectionState: ConnectionState
        get() = _uiState.value.connectionState
}