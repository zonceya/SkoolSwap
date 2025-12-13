package com.example.skoolswap.ui.profile

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.skoolswap.databinding.DialogDeleteProfileBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DeleteProfileDialog : DialogFragment() {

    private var _binding: DialogDeleteProfileBinding? = null
    private val binding get() = _binding!!

    var onConfirm: (() -> Unit)? = null
    var onCancel: (() -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogDeleteProfileBinding.inflate(layoutInflater)

        // Setup button click listeners
        binding.btnKeep.setOnClickListener {
            onCancel?.invoke()
            dismiss()
        }

        binding.btnDelete.setOnClickListener {
            onConfirm?.invoke()
            dismiss()
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setCancelable(true)
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}