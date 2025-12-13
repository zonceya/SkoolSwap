// Create this file: ui/profile/DeleteProfileDialog.kt
package com.example.skoolswap.ui.profile

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.skoolswap.R
import com.example.skoolswap.databinding.DialogDeleteProfileBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DeleteProfileDialog : DialogFragment() {

    private var _binding: DialogDeleteProfileBinding? = null
    private val binding get() = _binding!!

    var onConfirm: (() -> Unit)? = null
    var onCancel: (() -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogDeleteProfileBinding.inflate(layoutInflater)

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setCancelable(false)
            .setPositiveButton("Delete Profile") { _, _ ->
                onConfirm?.invoke()
            }
            .setNegativeButton("Cancel") { _, _ ->
                onCancel?.invoke()
            }
            .create()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up the warning message
        binding.warningTitle.text = "⚠️ Warning: Account Deletion"
        binding.warningMessage.text = """
            Deleting your account is permanent and cannot be undone!
            
            ❌ All your data will be permanently lost
            ❌ Your listings will be removed
            ❌ Your chat history will be deleted
            ❌ You will lose access to all features
            
            If you need to reactivate your account later, you'll need to contact our support team.
            
            Are you sure you want to proceed?
        """.trimIndent()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}