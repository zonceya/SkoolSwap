// com/example/skoolswap/ui/help/HelpFragment.kt
package com.example.skoolswap.ui.help

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.skoolswap.R
import com.example.skoolswap.common.constants.ErrorConstants
import com.example.skoolswap.common.constants.ErrorConstantsHelper
import com.example.skoolswap.databinding.FragmentHelpBinding
import com.example.skoolswap.domain.repository.HelpRepositoryInterface
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class HelpFragment : Fragment() {

    private var _binding: FragmentHelpBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HelpViewModel by viewModels()

    @Inject
    lateinit var helpRepository: HelpRepositoryInterface

    companion object {
        private const val MAX_DESCRIPTION_LENGTH = 400
        private const val SUCCESS_DELAY_MS = 1500L // Delay before navigating back
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHelpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCharacterCounter()
        setupSendButton()
        loadUser()
        checkApiStatus()
    }

    private fun setupCharacterCounter() {
        binding.etDescription.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val length = s?.length ?: 0
                binding.tvCharCounter.text = "$length/$MAX_DESCRIPTION_LENGTH"

                if (length > MAX_DESCRIPTION_LENGTH * 0.8) {
                    binding.tvCharCounter.setTextColor(requireContext().getColor(R.color.yellow))
                } else {
                    binding.tvCharCounter.setTextColor(requireContext().getColor(R.color.darker_grey_light))
                }
            }

            override fun afterTextChanged(s: Editable?) {
                if (s?.length ?: 0 > MAX_DESCRIPTION_LENGTH) {
                    s?.delete(MAX_DESCRIPTION_LENGTH, s.length)
                }
            }
        })
    }

    private fun loadUser() {
        lifecycleScope.launch {
            viewModel.user.collect { user ->
                if (user != null) {
                    binding.etName.setText(user.name ?: "")
                    binding.etEmail.setText(user.email ?: "")
                    binding.etMobileNumber.setText(user.mobile ?: "")
                }
            }
        }
    }

    private fun checkApiStatus() {
        lifecycleScope.launch {
            try {
                val result = helpRepository.getHelpStatus()
                result.onSuccess { status ->
                    Timber.d("Help API is operational")
                    Timber.d("Support email: ${status.support_email}")
                }.onFailure { error ->
                    Timber.w("Help API status check failed: ${error.message}")
                }
            } catch (e: Exception) {
                Timber.e(e, "API status check error")
            }
        }
    }

    private fun setupSendButton() {
        binding.btnSend.setOnClickListener {
            val subject = binding.etSubject.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()

            if (validateForm(subject, description)) {
                sendSupportRequest(subject, description)
            }
        }
    }

    private fun validateForm(subject: String, description: String): Boolean {
        if (subject.isEmpty()) {
            binding.etSubject.error = "Please enter a subject"
            binding.etSubject.requestFocus()
            return false
        }

        if (description.isEmpty()) {
            binding.etDescription.error = "Please describe your issue"
            binding.etDescription.requestFocus()
            return false
        }

        if (description.length > MAX_DESCRIPTION_LENGTH) {
            binding.etDescription.error = "Description cannot exceed $MAX_DESCRIPTION_LENGTH characters"
            return false
        }

        return true
    }

    private fun sendSupportRequest(subject: String, description: String) {
        // Show loading state
        setLoadingState(true)

        lifecycleScope.launch {
            try {
                val result = helpRepository.sendSupportRequest(subject, description)

                result.onSuccess { response ->
                    // ✅ Show success message
                    Toast.makeText(
                        requireContext(),
                        response.message,
                        Toast.LENGTH_LONG
                    ).show()

                    // ✅ Clear form fields
                    binding.etSubject.text?.clear()
                    binding.etDescription.text?.clear()

                    // ✅ Reset button state BEFORE navigating
                    setLoadingState(false)

                    // ✅ Wait a moment so user sees the success state
                    delay(SUCCESS_DELAY_MS)

                    // ✅ Navigate back to previous screen
                    findNavController().popBackStack()

                }.onFailure { error ->
                    // Show error and reset state
                    Toast.makeText(
                        requireContext(),
                        error.message ?: "Failed to send support request",
                        Toast.LENGTH_LONG
                    ).show()
                    setLoadingState(false)
                }

            } catch (e: Exception) {
                Timber.e(e, "Error sending support request")
                Toast.makeText(
                    requireContext(),
                    ErrorConstantsHelper.getErrorMessage(e),
                    Toast.LENGTH_LONG
                ).show()
                setLoadingState(false)
            }
        }
    }

    /**
     * Manages UI state during loading
     */
    private fun setLoadingState(isLoading: Boolean) {
        binding.btnSend.isEnabled = !isLoading
        binding.btnSend.text = if (isLoading) "Sending..." else "SEND"
        binding.helpProgressLayout.visibility = if (isLoading) View.VISIBLE else View.GONE

        // Disable inputs while loading
        binding.etSubject.isEnabled = !isLoading
        binding.etDescription.isEnabled = !isLoading

        // Update button color based on state
        if (isLoading) {
            binding.btnSend.backgroundTintList =
                requireContext().getColorStateList(R.color.darker_grey_light)
        } else {
            binding.btnSend.backgroundTintList =
                requireContext().getColorStateList(R.color.black)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}