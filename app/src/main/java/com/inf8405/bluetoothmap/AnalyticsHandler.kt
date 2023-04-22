package com.inf8405.bluetoothmap

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.os.BatteryManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModelProvider.NewInstanceFactory.Companion.instance
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
        val batteryLevel = initialAppBatteryLevel - getBatteryPct()
        if(batteryLevel > 0)
            appBatteryLevel = batteryLevel
        val energyLevel = (initialAppEnergyLevel - getChargeCounter()) / 1000
        if(energyLevel > 0)
            appEnergyLevel = energyLevel
    }

    fun setScanBatteryLevels() {
        val batteryLevel = previousScanBatteryLevel + initialScanBatteryLevel - getBatteryPct()
        if(batteryLevel > 0)
            scanBatteryLevel = batteryLevel
        val energyLevel =  previousScanEnergyLevel + (initialScanEnergyLevel - getChargeCounter()) / 1000
        if(energyLevel > 0)
            scanEnergyLevel = energyLevel
    }

    fun saveAppBatteryLevels() {
        val editor =  MainActivity.sharedPreferences.edit()
        editor.putInt(MainActivity.APP_BATTERY_PERCENTAGE, initialAppBatteryLevel)
        editor.putLong(MainActivity.APP_BATTERY_CHARGE, initialAppEnergyLevel)
        editor.apply()
    }

    fun resetAnalytics() {
        val editor = MainActivity.sharedPreferences.edit()
        editor.putInt(MainActivity.APP_BATTERY_PERCENTAGE, 0)
        editor.putLong(MainActivity.APP_BATTERY_CHARGE, 0)
        editor.apply()

        val initialBatteryLevel = getBatteryPct()
        val initialEnergyLevel = getChargeCounter()

        initialAppBatteryLevel = initialBatteryLevel
        initialScanBatteryLevel = initialBatteryLevel
        initialAppEnergyLevel = initialEnergyLevel
        initialScanEnergyLevel = initialEnergyLevel
    }
}


