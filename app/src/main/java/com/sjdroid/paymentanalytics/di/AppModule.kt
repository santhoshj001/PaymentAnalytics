package com.sjdroid.paymentanalytics.di

import android.content.Context
import com.sjdroid.paymentanalytics.data.repository.AnalyticsRepositoryImpl
import com.sjdroid.paymentanalytics.domain.repository.AnalyticsRepository

object AppModule {
    
    @Volatile
    private var analyticsRepository: AnalyticsRepository? = null
    
    fun provideAnalyticsRepository(context: Context): AnalyticsRepository {
        return analyticsRepository ?: synchronized(this) {
            analyticsRepository ?: AnalyticsRepositoryImpl(context.applicationContext).also {
                analyticsRepository = it
            }
        }
    }
}