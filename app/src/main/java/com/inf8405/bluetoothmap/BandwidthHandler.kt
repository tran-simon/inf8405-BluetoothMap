package com.inf8405.bluetoothmap

import android.net.TrafficStats
import android.os.Handler

class BandwidthHandler {
    @Suppress("DEPRECATION")
    private val handler = Handler()
    private var lastTime = -1L
    private var lastRxBytes = -1L
    private var lastTxBytes = -1L

    private var rxBandwidth = -1L
    private var txBandwidth = -1L

    init {
        update()
    }

    fun startUpdating(callback: (rx: String, tx: String) -> Unit) {
        handler.postDelayed(object : Runnable {
            override fun run() {
                update()
                callback(formatBytes(rxBandwidth), formatBytes(txBandwidth))

                handler.postDelayed(this, 1000)
            }
        }, 1000)
    }

    private fun update() {
        val currentRxBytes = TrafficStats.getTotalRxBytes()
        val currentTxBytes = TrafficStats.getTotalTxBytes()
        val currentTime = System.currentTimeMillis()
        if (lastTime == -1L) {
            lastTime = currentTime
            lastRxBytes = currentRxBytes
            lastTxBytes = currentTxBytes

            return
        }

        val secondsBetweenUpdates = (currentTime - lastTime) / 1000L

        rxBandwidth = (currentRxBytes - lastRxBytes) / secondsBetweenUpdates
        txBandwidth = (currentTxBytes - lastTxBytes) / secondsBetweenUpdates

        lastTime = currentTime
        lastRxBytes = currentRxBytes
        lastTxBytes = currentTxBytes
    }

    private fun formatBytes(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB")

        var size = bytes.toDouble()
        var index = 0
        while (size > 1024 && index < 3) {
            size /= 1024
            index++
        }

        return String.format("%.2f %s/s", size, units[index])
    }
}
