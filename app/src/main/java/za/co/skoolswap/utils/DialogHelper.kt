package za.co.skoolswap.utils

import android.content.Context
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import za.co.skoolswap.R
import com.google.android.material.button.MaterialButton

sealed class DialogAction {
    object Logout : DialogAction()
    object DeleteAccount : DialogAction()
    object DeleteShop : DialogAction()
    object DeleteItem : DialogAction()
    object MissingContactNumber : DialogAction()
    data class UpdateShop(val currentName: String, val newName: String) : DialogAction()
    data class UpdateItem(val itemName: String, val changes: String) : DialogAction()
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
        onConfirm: () -> Unit,
        onCancel: (() -> Unit)? = null  // ADD THIS PARAMETER
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
            is DialogAction.MissingContactNumber -> {
                logoImage.visibility = android.view.View.VISIBLE
                titleView.text = "MISSING CONTACT NUMBER"
                messageView.text = "We have noticed you don't have a contact number under your profile.\n\nBuyers won't be able to reach you without a contact number.\n\nWould you like to add your contact number now?"
                confirmButton.text = "Add Number"
                cancelButton.text = "Not Now"
            }
            is DialogAction.DeleteShop -> {
                logoImage.visibility = android.view.View.VISIBLE
                titleView.text = "DELETE SHOP"
                messageView.text = "Are you sure you want to delete your shop? This will remove all your items and cannot be undone."
                confirmButton.text = "Delete Shop"
            }
            is DialogAction.DeleteItem -> {
                logoImage.visibility = android.view.View.VISIBLE
                titleView.text = "DELETE ITEM"
                messageView.text = "Are you sure you want to delete this item? This action cannot be undone."
                confirmButton.text = "Delete Item"
            }
            is DialogAction.UpdateShop -> {
                logoImage.visibility = android.view.View.VISIBLE
                titleView.text = "UPDATE SHOP"
                messageView.text = """
                    You are about to update your shop name from:
                    
                    "${action.currentName}"
                    
                    to:
                    
                    "${action.newName}"
                    
                    Do you want to continue?
                """.trimIndent()
                confirmButton.text = "Update Shop"
            }
            is DialogAction.UpdateItem -> {
                logoImage.visibility = android.view.View.VISIBLE
                titleView.text = "UPDATE ITEM"
                messageView.text = """
                    You are about to update:
                    
                    ${action.itemName}
                    
                    Changes:
                    ${action.changes}
                    
                    Do you want to continue?
                """.trimIndent()
                confirmButton.text = "Update Item"
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
            is DialogAction.DeleteAccount, is DialogAction.DeleteShop, is DialogAction.DeleteItem -> {
                // Delete buttons - use red for danger
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

        // UPDATE cancel button to use onCancel callback
        cancelButton.setOnClickListener {
            dialog.dismiss()
            onCancel?.invoke()  // Call onCancel if provided
        }

        confirmButton.setOnClickListener {
            dialog.dismiss()
            onConfirm()
        }

        dialog.show()
    }
}