package com.inf8405.bluetoothmap

import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.DialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseUser
import java.io.ByteArrayOutputStream


const val REQUEST_IMAGE_CAPTURE = 1

class UserInfoDialogFragment(private val currentUser: FirebaseUser) : DialogFragment() {
    private lateinit var activity: MainActivity
    private lateinit var imageUploadButton: ImageButton
    private lateinit var usernameTextInput: TextInputEditText
    private lateinit var saveButton: Button
    private lateinit var deleteUserButton: Button

    private var hasTakenPicture = false
    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        @Suppress("DEPRECATION")
        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val bitmap = data?.extras?.get("data") as Bitmap
            imageUploadButton.setImageBitmap(bitmap)
            hasTakenPicture = true
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity = requireActivity() as MainActivity
        val dialogView = View.inflate(activity, R.layout.fragment_user_info, null)

        imageUploadButton = dialogView.findViewById(R.id.btn_image_upload)
        if (activity.hasProfilePicture) {
            imageUploadButton.setImageDrawable(activity.userImagebutton.drawable)
        }

        imageUploadButton.setOnClickListener {
            dispatchTakePictureIntent()
        }

        usernameTextInput = dialogView.findViewById(R.id.txt_username_input)
        usernameTextInput.setText(activity.usernameTextView.text.toString())

        saveButton = dialogView.findViewById(R.id.btn_save)
        saveButton.setOnClickListener {
            val username = usernameTextInput.text.toString()
            activity.usersCollection.document(currentUser.uid).set(
                hashMapOf(
                    "username" to username
                )
            ).continueWith {
                activity.updateUsername()
            }

            if (hasTakenPicture) {
                val outputStream = ByteArrayOutputStream()
                imageUploadButton.drawable.toBitmap().compress(Bitmap.CompressFormat.PNG, 90, outputStream)
                val byteArray = outputStream.toByteArray()

                activity.storageRef.child("images/${currentUser.uid}").putBytes(byteArray).continueWith {
                    activity.updateProfileImage()
                }
            }
        }

        deleteUserButton = dialogView.findViewById(R.id.btn_delete_user)
        deleteUserButton.setOnClickListener {
            dismiss()
            SignoutDialogFragment().show(activity.supportFragmentManager, "signout")
        }


        val builder = AlertDialog.Builder(activity)
        builder.setView(dialogView)
        return builder.create()
    }
}

class CreateUserDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = requireActivity() as MainActivity
        val builder = AlertDialog.Builder(activity)
            .setTitle(R.string.create_user_dialog_title)
            .setMessage(R.string.create_user_dialog_message)
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_yes) { _, _ ->
                activity.signup()
            }
        return builder.create()
    }
}

class SignoutDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = requireActivity() as MainActivity
        val builder = AlertDialog.Builder(activity)
            .setTitle(R.string.signout_dialog_title)
            .setMessage(R.string.signout_dialog_message)
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_yes) { _, _ ->
                activity.auth.signOut()
            }
        return builder.create()
    }
}
