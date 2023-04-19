package com.inf8405.bluetoothmap

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.*
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.content.res.AppCompatResources
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
import com.google.android.gms.maps.model.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage


class MainActivity : AppCompatActivity(), OnMapReadyCallback,
    ActivityCompat.OnRequestPermissionsResultCallback,
    OnInfoWindowClickListener {
    companion object {
        const val TAG = "BluetoothMap_Log"
        const val CURRENT_THEME = "CURRENT_THEME"
        const val LIGHT_THEME = "LIGHT"
        const val DARK_THEME = "DARK"
        const val SHARED_PREFERENCES_NAME = "BluetoothMap"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val BLUETOOTH_PERMISSION_REQUEST_CODE = 2
        private const val DEFAULT_ZOOM = 15f
        private val DEFAULT_LOCATION = LatLng(45.50520841701728, -73.6131208357828)
    }

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var toggleButton: ToggleButton
    private lateinit var swapButton: ToggleButton
    private lateinit var deviceListView: ListView
    private lateinit var bandwidthRxTextView: TextView
    private lateinit var bandwidthTxTextView: TextView
    lateinit var userImagebutton: ImageButton
    var hasProfilePicture = false
    lateinit var usernameTextView: TextView
    lateinit var devicesListAdapter: DevicesListAdapter

    lateinit var auth: FirebaseAuth

    private var locationPermissionGranted = false
    private var discovering = false

    private var deviceList: MutableList<DeviceData> = mutableListOf()
    private var markers: HashMap<String, Marker> = hashMapOf()
    private val devicesCollection = Firebase.firestore.collection("devices")
    val usersCollection = Firebase.firestore.collection("users")

    private var currentUser: FirebaseUser? = null

    val storageRef = Firebase.storage.reference

    private val bandwidthHandler = BandwidthHandler()

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
        sharedPreferences = applicationContext.getSharedPreferences(
            SHARED_PREFERENCES_NAME,
            MODE_PRIVATE
        )
        if (isDarkMode()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        toggleButton = findViewById(R.id.toggleButton)
        swapButton = findViewById(R.id.swapTheme)

        deviceListView = findViewById(R.id.listView)
        userImagebutton = findViewById(R.id.btn_userPicture)
        usernameTextView = findViewById(R.id.txt_username)
        bandwidthRxTextView = findViewById(R.id.txt_bandwidth_rx)
        bandwidthTxTextView = findViewById(R.id.txt_bandwidth_tx)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        mapFragment.getMapAsync(this)

        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter

        registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
        registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED))

        enableBluetooth()

        devicesListAdapter = DevicesListAdapter(this, deviceList)
        deviceListView.adapter = devicesListAdapter
        deviceListView.setOnItemClickListener { adapterView, _, i, _ ->
            val deviceData = adapterView.getItemAtPosition(i) as DeviceData
            markers[deviceData.address]?.showInfoWindow()
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(deviceData.latLng, DEFAULT_ZOOM))
        }

        swapButton.setOnCheckedChangeListener { _, isChecked ->
            swapTheme(isChecked)
        }

        auth = Firebase.auth

        auth.addAuthStateListener { auth ->
            currentUser = auth.currentUser
            updateUsername()
            updateProfileImage()
            initialiseDevices()
        }

        userImagebutton.setOnClickListener {
            if (currentUser == null) {
                CreateUserDialogFragment().show(supportFragmentManager, "create_user")
            } else {
                UserInfoDialogFragment(currentUser!!).show(supportFragmentManager, "user_info")
            }
        }

        bandwidthHandler.startUpdating { rx, tx ->
            bandwidthRxTextView.text = getString(R.string.bandwidth_rx, rx)
            bandwidthTxTextView.text = getString(R.string.bandwidth_tx, tx)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, DEFAULT_ZOOM))
        map.setOnInfoWindowClickListener(this)
        enableMyLocation()
        swapButton.isEnabled = true
        setMapTheme()
        initialiseDevices()
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
        deviceList.find { markers[it.address] == marker }
            ?.let { DeviceInfoDialogFragment(it, devicesCollection.document(it.address)).show(supportFragmentManager, "device_info") }
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

    private fun initialiseDevices() {
        if (currentUser == null) {
            // Reset markers and devices
            if (this@MainActivity::map.isInitialized) {
                map.clear()
            }
            markers.clear()
            deviceList.clear()
            devicesListAdapter.notifyDataSetChanged()
            return
        }

        devicesCollection
            .whereEqualTo("user", currentUser!!.uid)
            .get()
            .addOnSuccessListener { result ->
                for (device in result) {
                    val latLng = LatLng(
                        device["latLng.latitude"] as Double,
                        device["latLng.longitude"] as Double,
                    )

                    @Suppress("UNCHECKED_CAST") val deviceData = DeviceData(
                        latLng,
                        device["user"] as String,
                        device["alias"] as String?,
                        device["address"] as String,
                        device["name"] as String?,
                        (device["type"] as Long).toInt(),
                        device["uuids"] as Array<ParcelUuid>?,
                        (device["bondState"] as Long).toInt(),
                        device["bluetoothClass"] as String?,
                        device["starred"] as Boolean
                    )

                    val marker = map.addMarker(
                        MarkerOptions()
                            .title(deviceData.name ?: deviceData.address)
                            .position(deviceData.latLng)
                    ) ?: throw Exception("Could not add marker")

                    markers[deviceData.address] = marker
                    deviceList += deviceData
                }

                devicesListAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents.", exception)
            }
    }

    fun addDevice(bluetoothDevice: BluetoothDevice, location: LatLng) {
        var existingDeviceData = deviceList.find { deviceData -> deviceData.address == bluetoothDevice.address }
        if (existingDeviceData == null) {
            val marker: Marker = map.addMarker(
                MarkerOptions()
                    .title(bluetoothDevice.getDeviceName())
                    .position(location)
            ) ?: throw Exception("Could not add marker")

            existingDeviceData = DeviceData(bluetoothDevice, location, currentUser?.uid)
            markers[existingDeviceData.address] = marker
            deviceList += existingDeviceData

            if (existingDeviceData.user != null) {
                devicesCollection.document(existingDeviceData.address).set(existingDeviceData)
            }
        }

        devicesListAdapter.notifyDataSetChanged()
    }

    private fun swapTheme(isChecked: Boolean) {
        if (this@MainActivity::map.isInitialized) {
            val editor = sharedPreferences.edit()
            if (isChecked) {
                editor.putString(CURRENT_THEME, LIGHT_THEME)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            } else {
                editor.putString(CURRENT_THEME, DARK_THEME)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            editor.apply()
        }
    }

    private fun setMapTheme() {
        if (isDarkMode()) {
            map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    this, R.raw.dark_style
                )
            )
        } else {
            map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    this, R.raw.light_style
                )
            )
        }
    }

    private fun isDarkMode(): Boolean {
        return sharedPreferences.getString(CURRENT_THEME, "").equals(DARK_THEME)
    }

    fun showSensors(@Suppress("UNUSED_PARAMETER") view: View) {
        SensorsDialogFragment().show(supportFragmentManager, "sensors")
    }

    fun signup() {
        auth.signInAnonymously()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    currentUser = auth.currentUser
                    if (currentUser != null) {
                        Log.d(TAG, "signInAnonymously:success $currentUser")
                        UserInfoDialogFragment(currentUser!!).show(supportFragmentManager, "user_info")
                    } else {
                        Log.e(TAG, "signInAnonymously:success but currentUser is null")
                    }
                } else {
                    Log.e(TAG, "signInAnonymously:failure", task.exception)
                    Toast.makeText(
                        baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    fun updateUsername() {
        if (currentUser == null) {
            usernameTextView.text = null
        } else {
            usersCollection.document(currentUser!!.uid).get().addOnSuccessListener { document ->
                document["username"]?.let { usernameTextView.text = it as String }
            }
        }
    }

    fun updateProfileImage() {
        if (currentUser == null) {
            if (hasProfilePicture) {
                userImagebutton.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.baseline_person_24))
            }
            hasProfilePicture = false
        } else {
            hasProfilePicture = true
            storageRef.child("images/${currentUser!!.uid}").getBytes(1024 * 1024 * 50).addOnSuccessListener { bytes ->
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                userImagebutton.setImageBitmap(bitmap)
            }
        }
    }
}
