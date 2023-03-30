package com.inf8405.bluetoothmap

import android.bluetooth.BluetoothDevice
import com.google.android.gms.maps.model.Marker

data class DevicesData(val marker: Marker, val devices: MutableList<BluetoothDevice>)
