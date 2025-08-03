package com.sjdroid.paymentanalytics.presentation.model

import com.sjdroid.paymentanalytics.AnalyticsData
import com.sjdroid.paymentanalytics.domain.repository.ConnectionState

data class AnalyticsUiState(
    val analyticsData: AnalyticsData? = null,
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isDeviceReady: Boolean = false
) {
    val canPerformTransaction: Boolean
        get() = connectionState is ConnectionState.Connected && isDeviceReady
}