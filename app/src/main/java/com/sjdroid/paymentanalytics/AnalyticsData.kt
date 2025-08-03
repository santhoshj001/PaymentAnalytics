package com.sjdroid.paymentanalytics

import android.os.Parcel
import android.os.Parcelable

data class AnalyticsData(
    val batteryLevel: Int = 0,
    val memoryUsagePercent: Float = 0f,
    val cpuUsagePercent: Float = 0f,
    val transactionCount: Int = 0,
    val successfulTransactions: Int = 0,
    val failedTransactions: Int = 0,
    val lastTransactionTime: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable {
    
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readFloat(),
        parcel.readFloat(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readLong(),
        parcel.readLong()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(batteryLevel)
        parcel.writeFloat(memoryUsagePercent)
        parcel.writeFloat(cpuUsagePercent)
        parcel.writeInt(transactionCount)
        parcel.writeInt(successfulTransactions)
        parcel.writeInt(failedTransactions)
        parcel.writeLong(lastTransactionTime)
        parcel.writeLong(timestamp)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<AnalyticsData> {
        override fun createFromParcel(parcel: Parcel): AnalyticsData {
            return AnalyticsData(parcel)
        }

        override fun newArray(size: Int): Array<AnalyticsData?> {
            return arrayOfNulls(size)
        }
    }
    
    val isDeviceReady: Boolean
        get() = batteryLevel > 20 && memoryUsagePercent < 85f && cpuUsagePercent < 90f
}