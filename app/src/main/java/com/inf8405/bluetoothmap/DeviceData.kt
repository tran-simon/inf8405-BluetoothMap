package com.inf8405.bluetoothmap

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Build
import android.os.ParcelUuid
import com.google.android.gms.maps.model.LatLng

data class DeviceData(
    var latLng: LatLng,
    var user: String?,
    var alias: String?,
    var address: String,
    var name: String?,
    var type: Int,
    var uuids: Array<ParcelUuid>?,
    var bondState: Int,
    var bluetoothClass: String?,
    var starred: Boolean = false,
) {

    @SuppressLint("MissingPermission")
    constructor(bluetoothDevice: BluetoothDevice, latLng: LatLng, user: String?) : this(
        latLng,
        user,
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                bluetoothDevice.alias
            }
            else -> {
                null
            }
        },
        bluetoothDevice.address,
        bluetoothDevice.name,
        bluetoothDevice.type,
        bluetoothDevice.uuids,
        bluetoothDevice.bondState,
        bluetoothDevice.bluetoothClass?.toString(),
    )

    fun getDeviceInfo(): String {
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DeviceData

        if (latLng != other.latLng) return false
        if (user != other.user) return false
        if (alias != other.alias) return false
        if (address != other.address) return false
        if (name != other.name) return false
        if (type != other.type) return false
        if (!uuids.contentEquals(other.uuids)) return false
        if (bondState != other.bondState) return false
        if (bluetoothClass != other.bluetoothClass) return false
        if (starred != other.starred) return false

        return true
    }

    override fun hashCode(): Int {
        var result = latLng.hashCode()
        result = 31 * result + (user?.hashCode() ?: 0)
        result = 31 * result + (alias?.hashCode() ?: 0)
        result = 31 * result + address.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + type
        result = 31 * result + uuids.contentHashCode()
        result = 31 * result + bondState
        result = 31 * result + bluetoothClass.hashCode()
        result = 31 * result + starred.hashCode()
        return result
    }

    override fun toString(): String {
        return this.name ?: this.address
    }
}

@SuppressLint("MissingPermission")
fun BluetoothDevice.getDeviceName(): String {
    this.toString()
    return this.name ?: this.address
}
