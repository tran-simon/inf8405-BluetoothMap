package com.inf8405.bluetoothmap

import android.app.Dialog
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.textfield.TextInputEditText


class SensorsDialogFragment : DialogFragment(), SensorEventListener {
    private lateinit var activity: MainActivity
    private lateinit var dialogView: View
    private lateinit var txtAccelerometerX: TextInputEditText
    private lateinit var txtAccelerometerY: TextInputEditText
    private lateinit var txtAccelerometerZ: TextInputEditText
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity = requireActivity() as MainActivity
        dialogView = View.inflate(activity, R.layout.fragment_sensors_dialog, null)

        txtAccelerometerX = dialogView.findViewById(R.id.txt_accelerometer_x)
        txtAccelerometerY = dialogView.findViewById(R.id.txt_accelerometer_y)
        txtAccelerometerZ = dialogView.findViewById(R.id.txt_accelerometer_z)

        sensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val builder = AlertDialog.Builder(activity)
        builder.setView(dialogView)
        return builder.create()
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(sensorEvent: SensorEvent?) {
        if(sensorEvent?.sensor?.type == Sensor.TYPE_ACCELEROMETER){
            val x = sensorEvent.values[0]
            val y = sensorEvent.values[1]
            val z = sensorEvent.values[2]

            txtAccelerometerX.setText(getString(R.string.txt_accelerometer_value, x))
            txtAccelerometerY.setText(getString(R.string.txt_accelerometer_value, y))
            txtAccelerometerZ.setText(getString(R.string.txt_accelerometer_value, z))
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
}
