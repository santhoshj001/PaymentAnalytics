package com.sjdroid.paymentanalytics.service

import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.sjdroid.paymentanalytics.IAnalyticsService
import com.sjdroid.paymentanalytics.AnalyticsData
import com.sjdroid.paymentanalytics.service.metrics.MetricsCollector
import java.util.concurrent.atomic.AtomicInteger

class AnalyticsService : Service() {

    companion object {
        private const val TAG = "AnalyticsService"
        private const val PERMISSION_ANALYTICS = "com.sjdroid.paymentanalytics.permission.ACCESS_ANALYTICS"
        private const val VALIDATION_CACHE_TTL = 30_000L // 30 seconds
    }

    private lateinit var metricsCollector: MetricsCollector
    private val transactionCount = AtomicInteger(0)
    private val successfulTransactions = AtomicInteger(0)
    private val failedTransactions = AtomicInteger(0)
    private var lastTransactionTime = 0L
    
    // Security validation cache
    private val validatedCallers = mutableMapOf<Int, Long>()
    private val validationLock = Any()

    private val binder = object : IAnalyticsService.Stub() {
        
        override fun getCurrentStats(): AnalyticsData {
            enforcePermission()
            
            Log.d(TAG, "getCurrentStats() called from PID: ${Binder.getCallingPid()}")
            
            return AnalyticsData(
                batteryLevel = metricsCollector.getBatteryLevel(),
                memoryUsagePercent = metricsCollector.getMemoryUsage(),
                cpuUsagePercent = metricsCollector.getCpuUsage(),
                transactionCount = transactionCount.get(),
                successfulTransactions = successfulTransactions.get(),
                failedTransactions = failedTransactions.get(),
                lastTransactionTime = lastTransactionTime,
                timestamp = System.currentTimeMillis()
            )
        }

        override fun isServiceReady(): Boolean {
            enforcePermission()
            
            val stats = getCurrentStats()
            return stats.isDeviceReady
        }

        override fun resetStats() {
            enforcePermission()
            
            Log.d(TAG, "resetStats() called")
            transactionCount.set(0)
            successfulTransactions.set(0)
            failedTransactions.set(0)
            lastTransactionTime = 0L
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AnalyticsService created in process: ${android.os.Process.myPid()}")
        
        metricsCollector = MetricsCollector(this)
        metricsCollector.startCollecting()
        
        simulateTransactionActivity()
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "onBind called from package: ${intent?.`package`}")
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "AnalyticsService destroyed")
        metricsCollector.stopCollecting()
    }

    private fun enforcePermission() {
        val callingUid = Binder.getCallingUid()
        val currentTime = System.currentTimeMillis()
        
        // Check cache first
        synchronized(validationLock) {
            val lastValidation = validatedCallers[callingUid]
            if (lastValidation != null && (currentTime - lastValidation) < VALIDATION_CACHE_TTL) {
                // Still valid, skip expensive validation
                return
            }
        }
        
        // Perform full validation
        val callingPackage = packageManager.getNameForUid(callingUid)
        Log.d(TAG, "Performing security validation for package: $callingPackage")
        
        if (checkCallingPermission(PERMISSION_ANALYTICS) != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("Access denied: Missing $PERMISSION_ANALYTICS permission")
        }
        
        if (!isSignatureValid()) {
            throw SecurityException("Access denied: Invalid signature")
        }
        
        // Cache successful validation
        synchronized(validationLock) {
            validatedCallers[callingUid] = currentTime
            // Clean old entries
            val iterator = validatedCallers.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (currentTime - entry.value > VALIDATION_CACHE_TTL) {
                    iterator.remove()
                }
            }
        }
        
        Log.d(TAG, "Security validation passed and cached for UID: $callingUid")
    }

    private fun isSignatureValid(): Boolean {
        return try {
            val callingUid = Binder.getCallingUid()
            val callingPackages = packageManager.getPackagesForUid(callingUid) ?: return false
            
            val mySignature = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            
            for (packageName in callingPackages) {
                val callingSignature = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                
                if (mySignature.signatures.contentEquals(callingSignature.signatures)) {
                    Log.d(TAG, "Signature validation passed for package: $packageName")
                    return true
                }
            }
            
            Log.w(TAG, "Signature validation failed")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error validating signature", e)
            false
        }
    }

    private fun simulateTransactionActivity() {
        Thread {
            while (true) {
                Thread.sleep(15000) // Simulate transaction every 15 seconds
                
                transactionCount.incrementAndGet()
                lastTransactionTime = System.currentTimeMillis()
                
                // Simulate success/failure
                if (Math.random() > 0.1) { // 90% success rate
                    successfulTransactions.incrementAndGet()
                } else {
                    failedTransactions.incrementAndGet()
                }
                
                Log.d(TAG, "Simulated transaction. Total: ${transactionCount.get()}")
            }
        }.start()
    }
}