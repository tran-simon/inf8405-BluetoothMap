package com.inf8405.bluetoothmap

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Build
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker

data class DeviceData(var device: BluetoothDevice, val marker: Marker, var latLng: LatLng) {
    companion object {
        @SuppressLint("MissingPermission")
        fun getDeviceName(bluetoothDevice: BluetoothDevice): String {
            return bluetoothDevice.name ?: bluetoothDevice.toString()
        }
    }

    override fun toString(): String {
        return getDeviceName(device)
    }

    @SuppressLint("MissingPermission")
    fun getDeviceInfo(): String {
        val alias = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                device.alias
            }
            else -> {
                null
            }
        }
        val address = device.address
        val name = device.name
        val type = device.type
        val uuids = device.uuids
        val bondState = device.bondState
        val bluetoothClass = device.bluetoothClass

        return """
                Alias: $alias
                Address: $address
                Name: $name
                Type: $type
                UUIDs: $uuids
                Bond state: $bondState
                Bluetooth class: $bluetoothClass
            """.trimIndent()
    }
}
