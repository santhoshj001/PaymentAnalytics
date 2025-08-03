package com.sjdroid.paymentanalytics.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sjdroid.paymentanalytics.di.AppModule
import com.sjdroid.paymentanalytics.domain.usecase.CheckDeviceReadinessUseCase
import com.sjdroid.paymentanalytics.domain.usecase.GetAnalyticsStatsUseCase
import com.sjdroid.paymentanalytics.domain.usecase.ManageServiceConnectionUseCase

class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AnalyticsViewModel::class.java) -> {
                val repository = AppModule.provideAnalyticsRepository(context)
                AnalyticsViewModel(
                    GetAnalyticsStatsUseCase(repository),
                    CheckDeviceReadinessUseCase(repository),
                    ManageServiceConnectionUseCase(repository)
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}