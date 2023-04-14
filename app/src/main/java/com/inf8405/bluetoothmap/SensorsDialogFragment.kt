package com.inf8405.bluetoothmap

import android.app.Dialog
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment


class SensorsDialogFragment : DialogFragment(), SensorEventListener {
    private lateinit var activity: MainActivity
    private lateinit var dialogView: View

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity = requireActivity() as MainActivity
        dialogView = View.inflate(activity, R.layout.fragment_sensors_dialog, null)

        val builder = AlertDialog.Builder(activity)
        builder.setView(dialogView)
        return builder.create()
    }

    override fun onSensorChanged(sensorEvent: SensorEvent?) {
        if(sensorEvent?.sensor?.type == Sensor.TYPE_ACCELEROMETER){
            val x = sensorEvent.values[0]
            val y = sensorEvent.values[1]
            val z = sensorEvent.values[2]
            TODO("complete")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
}
