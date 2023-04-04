package com.inf8405.bluetoothmap

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton


class MainActivity : AppCompatActivity(), OnMapReadyCallback,
    ActivityCompat.OnRequestPermissionsResultCallback,
    OnInfoWindowClickListener {
    companion object {
        private const val TAG = "BluetoothMap_Log"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val BLUETOOTH_PERMISSION_REQUEST_CODE = 2
        private const val DEFAULT_ZOOM = 15f
        private val DEFAULT_LOCATION = LatLng(45.50520841701728, -73.6131208357828)
    }

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var toggleButton: ToggleButton
    private lateinit var deviceListView: ListView
    private lateinit var arrayAdapter: ArrayAdapter<DeviceData>

    private var locationPermissionGranted = false
    private var discovering = true

    private var deviceList: MutableList<DeviceData> = mutableListOf()

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    @Suppress("DEPRECATION") val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

                    if (device != null) {

                        fusedLocationProviderClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val location = task.result
                                if (location != null) {
                                    Log.i(TAG, "Found device $device at location $location")

                                    addDevice(device, LatLng(location.latitude, location.longitude))
                                    return@addOnCompleteListener
                                }
                            }
                            Log.e(TAG, "Could not get current location", task.exception)
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.i(TAG, "Bluetooth discovery finished. Restarting? $discovering")
                    if (discovering) {
                        bluetoothAdapter.startDiscovery()
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    Log.i(TAG, "Bluetooth discovery started")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        toggleButton = findViewById(R.id.toggleButton)
        deviceListView = findViewById(R.id.listView)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        mapFragment.getMapAsync(this)

        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter
        enableBluetooth()

        registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
        registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED))

        arrayAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceList)
        deviceListView.adapter = arrayAdapter
        deviceListView.setOnItemClickListener { adapterView, _, i, _ ->
            val deviceData = adapterView.getItemAtPosition(i) as DeviceData
            deviceData.marker.showInfoWindow()
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(deviceData.latLng, DEFAULT_ZOOM))
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, DEFAULT_ZOOM))
        map.setOnInfoWindowClickListener(this)

        enableMyLocation()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                enableMyLocation()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH
                ) == PackageManager.PERMISSION_GRANTED && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED)
            ) {
                enableBluetooth()
            } else {
                Toast.makeText(this, "Bluetooth permission denied", Toast.LENGTH_SHORT).show()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onInfoWindowClick(marker: Marker) {
        deviceList.find { it.marker == marker }?.let { DeviceInfoDialogFragment(it).show(supportFragmentManager, "device_info") }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    private fun enableMyLocation() {
        // 1. Check if permissions are granted, if so, enable the my location layer
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true
            locationPermissionGranted = true
            centerCamera()
            return
        }

        // 2. Otherwise, request location permissions from the user.
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun enableBluetooth() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH),
                BLUETOOTH_PERMISSION_REQUEST_CODE
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                BLUETOOTH_PERMISSION_REQUEST_CODE
            )
        } else {
            toggleButton.isEnabled = true
        }
    }

    @SuppressLint("MissingPermission")
    private fun centerCamera() {
        if (locationPermissionGranted) {
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), DEFAULT_ZOOM))
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun toggleScanning(view: View) {
        discovering = if ((view as ToggleButton).isChecked) {
            bluetoothAdapter.startDiscovery()
            true
        } else {
            bluetoothAdapter.cancelDiscovery()
            false
        }
    }

    fun toggleDeviceList(view: View) {
        deviceListView.visibility = if (deviceListView.visibility == View.GONE) View.VISIBLE else View.GONE
        val fab = (view as FloatingActionButton)
        fab.setImageResource(if (deviceListView.visibility == View.VISIBLE) R.drawable.baseline_expand_less_24 else R.drawable.baseline_expand_more_24)
    }

    fun addDevice(bluetoothDevice: BluetoothDevice, location: LatLng) {
        var existingDeviceData = deviceList.find { deviceData -> deviceData.device.address == bluetoothDevice.address }
        if (existingDeviceData == null) {
            val marker: Marker = map.addMarker(
                MarkerOptions()
                    .title(DeviceData.getDeviceName(bluetoothDevice))
                    .position(location)
            ) ?: throw Exception("Could not add marker")

            existingDeviceData = DeviceData(bluetoothDevice, marker, location)
            deviceList += existingDeviceData
        }

        arrayAdapter.notifyDataSetChanged()
    }
}
