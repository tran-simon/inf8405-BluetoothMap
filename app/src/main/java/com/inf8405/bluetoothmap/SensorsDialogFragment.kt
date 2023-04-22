package com.inf8405.bluetoothmap

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.widget.ToggleButton
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.registerReceiver
import androidx.fragment.app.DialogFragment

import com.google.android.material.textfield.TextInputEditText
import java.util.ArrayList;
import java.util.Date;


class SensorsDialogFragment : DialogFragment(), SensorEventListener {
    private lateinit var activity: MainActivity
    private lateinit var dialogView: View
    private lateinit var txtAccelerometerX: TextInputEditText
    private lateinit var txtAccelerometerY: TextInputEditText
    private lateinit var txtAccelerometerZ: TextInputEditText
    private lateinit var txtGyroscopeX: TextInputEditText
    private lateinit var txtGyroscopeY: TextInputEditText
    private lateinit var txtGyroscopeZ: TextInputEditText
    private lateinit var sensorManager: SensorManager
    private lateinit var txtAppBatteryUsage: TextInputEditText
    private lateinit var txtAppEnergyUsage: TextInputEditText
    private lateinit var txtScanBatteryUsage: TextInputEditText
    private lateinit var txtScanEnergyUsage: TextInputEditText
    private lateinit var scanToggleButton: ToggleButton


    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity = requireActivity() as MainActivity
        dialogView = View.inflate(activity, R.layout.fragment_sensors_dialog, null)

        txtAccelerometerX = dialogView.findViewById(R.id.txt_accelerometer_x)
        txtAccelerometerY = dialogView.findViewById(R.id.txt_accelerometer_y)
        txtAccelerometerZ = dialogView.findViewById(R.id.txt_accelerometer_z)

        txtGyroscopeX = dialogView.findViewById(R.id.txt_gyroscope_x)
        txtGyroscopeY = dialogView.findViewById(R.id.txt_gyroscope_y)
        txtGyroscopeZ = dialogView.findViewById(R.id.txt_gyroscope_z)

        sensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        scanToggleButton = activity.findViewById(R.id.toggleButton)

        txtAppBatteryUsage = dialogView.findViewById(R.id.txt_app_battery_usage)
        txtAppEnergyUsage = dialogView.findViewById(R.id.txt_app_energy_usage)
        txtScanBatteryUsage = dialogView.findViewById(R.id.txt_scan_battery_usage)
        txtScanEnergyUsage = dialogView.findViewById(R.id.txt_scan_energy_usage)
        setAppBatteryUsage()
        setScanBatteryUsage()

        val builder = AlertDialog.Builder(activity)
        builder.setView(dialogView)
        return builder.create()
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL)
        activity.registerReceiver(mBatteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        activity.unregisterReceiver(mBatteryReceiver)
    }

    private val mBatteryReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(arg0: Context?, intent: Intent) {
            setAppBatteryUsage()
            setScanBatteryUsage()
        }
    }

    override fun onSensorChanged(sensorEvent: SensorEvent?) {
        if (sensorEvent == null) {
            return
        }

        val x = sensorEvent.values[0]
        val y = sensorEvent.values[1]
        val z = sensorEvent.values[2]

        when (sensorEvent.sensor?.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                txtAccelerometerX.setText(getString(R.string.txt_accelerometer_value, x))
                txtAccelerometerY.setText(getString(R.string.txt_accelerometer_value, y))
                txtAccelerometerZ.setText(getString(R.string.txt_accelerometer_value, z))
            }
            Sensor.TYPE_GYROSCOPE -> {
                txtGyroscopeX.setText(getString(R.string.txt_gyroscope_value, x))
                txtGyroscopeY.setText(getString(R.string.txt_gyroscope_value, y))
                txtGyroscopeZ.setText(getString(R.string.txt_gyroscope_value, z))
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    @SuppressLint("SetTextI18n")
    private fun setAppBatteryUsage() {
        val instance: AnalyticsHandler = AnalyticsHandler().getInstance()
        getActivity()?.let { instance.setAppBatteryLevels() }
        txtAppBatteryUsage.setText(instance.getAppBatteryLevel().toString() + " %")
        txtAppEnergyUsage.setText(instance.getAppEnergyLevel().toString() + " mAh")
    }

    @SuppressLint("SetTextI18n")
    private fun setScanBatteryUsage() {
        val instance: AnalyticsHandler = AnalyticsHandler().getInstance()
        if(scanToggleButton.isChecked) getActivity()?.let { instance.setScanBatteryLevels() }
        txtScanBatteryUsage.setText(instance.getScanBatteryLevel().toString()+ " %")
        txtScanEnergyUsage.setText(instance.getScanEnergyLevel().toString() + " mAh")
    }
}
