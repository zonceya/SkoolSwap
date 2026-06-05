package com.example.skoolswap.utils

import android.content.Context
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.skoolswap.R
import com.google.android.material.button.MaterialButton

sealed class DialogAction {
    object Logout : DialogAction()
    object DeleteAccount : DialogAction()
    data class SaveChanges(val changesSummary: String) : DialogAction()
    data class Custom(
        val title: String,
        val message: String,
        val confirmText: String = "Confirm",
        val showLogo: Boolean = false
    ) : DialogAction()
}

object DialogHelper {

    fun showConfirmationDialog(
        context: Context,
        action: DialogAction,
        onConfirm: () -> Unit
    ) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_logout, null)

        val logoImage = dialogView.findViewById<ImageView>(R.id.logoImage)
        val titleView = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val messageView = dialogView.findViewById<TextView>(R.id.dialogMessage)
        val cancelButton = dialogView.findViewById<MaterialButton>(R.id.btnCancel)
        val confirmButton = dialogView.findViewById<MaterialButton>(R.id.btnLogout)

        // Configure based on action type
        when (action) {
            is DialogAction.Logout -> {
                logoImage.visibility = android.view.View.VISIBLE
                titleView.text = "LOG OUT"
                messageView.text = "Are you sure you want to logout?"
                confirmButton.text = "Logout"
            }
            is DialogAction.DeleteAccount -> {
                logoImage.visibility = android.view.View.VISIBLE
                titleView.text = "DELETE ACCOUNT"
                messageView.text = "Are you sure you want to delete your account? This action cannot be undone."
                confirmButton.text = "Delete"
            }
            is DialogAction.SaveChanges -> {
                logoImage.visibility = android.view.View.VISIBLE
                titleView.text = "SAVE CHANGES"
                messageView.text = "You are about to update:\n\n${action.changesSummary}\n\nDo you want to continue?"
                confirmButton.text = "Update"
            }
            is DialogAction.Custom -> {
                logoImage.visibility = if (action.showLogo) android.view.View.VISIBLE else android.view.View.GONE
                titleView.text = action.title.uppercase()
                messageView.text = action.message
                confirmButton.text = action.confirmText
            }
        }

        // Style the confirm button based on action type
        val isDarkMode = (context.resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES

        when (action) {
            is DialogAction.DeleteAccount -> {
                // Delete button - use red for danger
                confirmButton.backgroundTintList = ContextCompat.getColorStateList(context, R.color.red)
                confirmButton.setTextColor(ContextCompat.getColor(context, R.color.white))
            }
            else -> {
                // Normal confirm button - use theme colors
                if (isDarkMode) {
                    confirmButton.backgroundTintList = ContextCompat.getColorStateList(context, R.color.white)
                    confirmButton.setTextColor(ContextCompat.getColor(context, R.color.black))
                } else {
                    confirmButton.backgroundTintList = ContextCompat.getColorStateList(context, R.color.black)
                    confirmButton.setTextColor(ContextCompat.getColor(context, R.color.white))
                }
            }
        }

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        confirmButton.setOnClickListener {
            dialog.dismiss()
            onConfirm()
        }

        dialog.show()
    }
}