package com.inf8405.bluetoothmap

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseUser


class UserInfoDialogFragment(private val currentUser: FirebaseUser) : DialogFragment() {
    private lateinit var activity: MainActivity
    private lateinit var usernameTextInput: TextInputEditText
    private lateinit var saveButton: Button
    private lateinit var deleteUserButton: Button

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity = requireActivity() as MainActivity
        val dialogView = View.inflate(activity, R.layout.fragment_user_info, null)

        usernameTextInput = dialogView.findViewById(R.id.txt_username_input)
        usernameTextInput.setText(activity.userData.username)

        saveButton = dialogView.findViewById(R.id.btn_save)
        saveButton.setOnClickListener {
            val username = usernameTextInput.text.toString()
            // TODO: picture
            activity.usersCollection.document(currentUser.uid).set(UserData(username)).continueWith {
                activity.retrieveUserData()
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

class SignoutDialogFragment: DialogFragment() {
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
