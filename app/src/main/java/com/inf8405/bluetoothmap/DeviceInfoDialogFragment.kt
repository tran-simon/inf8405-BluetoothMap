package com.inf8405.bluetoothmap

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class DeviceInfoDialogFragment(private val device: DeviceData) : DialogFragment() {

    private lateinit var activity: Activity
    private lateinit var dialogView: View
    private lateinit var dialogTitle: TextView
    private lateinit var dialogText: TextView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity = requireActivity()

        dialogView = View.inflate(activity, R.layout.dialog_device_info, null)
        dialogTitle = dialogView.findViewById(R.id.textView_title)
        dialogText = dialogView.findViewById(R.id.textView_deviceDescription)
        dialogTitle.text = device.toString()
        dialogText.text = device.getDeviceInfo()

        val builder = AlertDialog.Builder(activity)
        builder.setView(dialogView)
        builder.setPositiveButton(R.string.btn_ok, null)
        return builder.create()
    }
}
