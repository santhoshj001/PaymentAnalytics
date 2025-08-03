package com.sjdroid.paymentanalytics;

import com.sjdroid.paymentanalytics.AnalyticsData;

interface IAnalyticsService {
    AnalyticsData getCurrentStats();
    
    boolean isServiceReady();
    
    void resetStats();
}