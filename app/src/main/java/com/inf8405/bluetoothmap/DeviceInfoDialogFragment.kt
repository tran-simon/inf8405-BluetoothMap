package com.inf8405.bluetoothmap

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class DeviceInfoDialogFragment(private val device: DeviceData) : DialogFragment(), OnClickListener {

    private lateinit var activity: MainActivity
    private lateinit var dialogView: View
    private lateinit var dialogTitle: TextView
    private lateinit var dialogText: TextView
    private lateinit var btnStar: ImageButton

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity = requireActivity() as MainActivity

        dialogView = View.inflate(activity, R.layout.dialog_device_info, null)
        dialogTitle = dialogView.findViewById(R.id.textView_title)
        dialogText = dialogView.findViewById(R.id.textView_deviceDescription)

        btnStar = dialogView.findViewById(R.id.btn_toggleStar)
        val btnNavigate = dialogView.findViewById<Button>(R.id.btn_navigate)
        val btnShare = dialogView.findViewById<ImageButton>(R.id.btn_share)

        btnStar.setOnClickListener(this)
        btnNavigate.setOnClickListener(this)
        btnShare.setOnClickListener(this)

        if (device.starred) {
            btnStar.setImageResource(R.drawable.baseline_star_24)
        }

        dialogTitle.text = device.toString()
        dialogText.text = device.getDeviceInfo()

        val builder = AlertDialog.Builder(activity)
        builder.setView(dialogView)
        builder.setPositiveButton(R.string.btn_ok, null)
        return builder.create()
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.btn_toggleStar -> {
                star()
            }
            R.id.btn_navigate -> {
                navigate()
            }
            R.id.btn_share -> {
                share()
            }
        }
    }

    private fun star() {
        device.starred = !device.starred
        btnStar.setImageResource(if (device.starred) R.drawable.baseline_star_24 else R.drawable.baseline_star_outline_24)
        activity.devicesListAdapter.notifyDataSetChanged()
    }

    private fun share() {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, device.getDeviceInfo())
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

    private fun navigate() {
        val gmmIntentUri: Uri = Uri.parse("google.navigation:q=${device.latLng.latitude},${device.latLng.longitude}&mode=w")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        startActivity(mapIntent)
    }
}
