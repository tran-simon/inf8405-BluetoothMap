package com.inf8405.bluetoothmap

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import java.util.*


class AnalyticsHandler {
    companion object {
        @Volatile
        var instance: AnalyticsHandler = AnalyticsHandler()
    }

    var batteryStatus: Intent? = Intent()
    var mBatteryManager: BatteryManager? = null

    private var appBatteryLevel: Int = 0
    private var appEnergyLevel: Long = 0
    var initialAppBatteryLevel: Int = 0
    var initialAppEnergyLevel: Long = 0

    private var scanBatteryLevel: Int = 0
    private var scanEnergyLevel: Long = 0
    var previousScanBatteryLevel: Int = 0
    var previousScanEnergyLevel: Long = 0
    var initialScanBatteryLevel: Int = 0
    var initialScanEnergyLevel: Long = 0


    fun getInstance(): AnalyticsHandler {
        return instance
    }

    fun getAppBatteryLevel(): Int {
        return appBatteryLevel
    }

    fun getAppEnergyLevel(): Long {
        return appEnergyLevel
    }

    fun getScanBatteryLevel(): Int {
        return scanBatteryLevel
    }

    fun getScanEnergyLevel(): Long {
        return scanEnergyLevel
    }

    fun getBatteryPct(): Int {

       return mBatteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 0
    }

    fun getChargeCounter(): Long {
       return  mBatteryManager?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) ?: 0
    }

    fun setAppBatteryLevels() {
        appBatteryLevel = initialAppBatteryLevel - getBatteryPct()
        appEnergyLevel =
            (initialAppEnergyLevel - getChargeCounter()) / 1000

    }

    fun setScanBatteryLevels() {
        scanBatteryLevel = previousScanBatteryLevel + initialScanBatteryLevel - getBatteryPct()
        scanEnergyLevel =
            previousScanEnergyLevel + (initialScanEnergyLevel - getChargeCounter()) / 1000
    }
}


