package com.example.skoolswap.ui.detail

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import com.example.skoolswap.R
import com.example.skoolswap.common.constants.AppConstants.LogTags
import com.example.skoolswap.databinding.DialogContactOptionsBinding
import com.example.skoolswap.domain.model.Item
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import timber.log.Timber

class ContactOptionsBottomSheet(
    private val item: Item,
    private val sellerMobile: String
) : BottomSheetDialogFragment() {

    private var _binding: DialogContactOptionsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogContactOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)

        binding.optionWhatsApp.setOnClickListener {
            openWhatsApp()
            dismiss()
        }

        binding.optionCall.setOnClickListener {
            makePhoneCall()
            dismiss()
        }

        binding.optionSms.setOnClickListener {
            sendSms()
            dismiss()
        }

        binding.optionCancel.setOnClickListener {
            dismiss()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.peekHeight = 0

                it.background = null
                it.setBackgroundResource(R.drawable.bottom_sheet_rounded)
            }
        }

        return dialog
    }

    private fun openWhatsApp() {
        try {
            val phoneNumber = sellerMobile.replace(Regex("[^0-9+]"), "")
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://wa.me/$phoneNumber")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Timber.tag(LogTags.UI).e(e, "WhatsApp not installed")
            Toast.makeText(requireContext(), "WhatsApp not installed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun makePhoneCall() {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$sellerMobile")
        }
        startActivity(intent)
    }

    private fun sendSms() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("sms:$sellerMobile")
            putExtra("sms_body", "Hi, I'm interested in your ${item.name} on SkoolSwap")
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}