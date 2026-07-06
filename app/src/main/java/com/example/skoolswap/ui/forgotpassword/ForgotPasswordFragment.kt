package com.example.skoolswap.ui.forgotpassword

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.skoolswap.R
import com.example.skoolswap.common.constants.AppConstants.LogTags
import com.example.skoolswap.databinding.FragmentForgotPasswordBinding
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class ForgotPasswordFragment : Fragment() {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ForgotPasswordViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeViewModel()
        setupStrings()
    }

    private fun setupStrings() {
        // Set strings from resources
        binding.buttonSendReset.text = getString(R.string.forgot_password_send_button)
        binding.editEmail.hint = getString(R.string.forgot_password_email_hint)
        binding.textBackToLogin.text = getString(R.string.forgot_password_back_to_login)
    }

    private fun setupClickListeners() {
        binding.buttonSendReset.setOnClickListener {
            val email = binding.editEmail.text.toString().trim()

            if (email.isEmpty()) {
                binding.editEmail.error = getString(R.string.error_email_required)
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.editEmail.error = getString(R.string.error_invalid_email)
                return@setOnClickListener
            }

            viewModel.sendPasswordResetEmail(email)
        }

        binding.textBackToLogin.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.buttonSendReset.isEnabled = !isLoading
            binding.buttonSendReset.text = if (isLoading) {
                getString(R.string.forgot_password_sending)
            } else {
                getString(R.string.forgot_password_send_button)
            }
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.resetSent.observe(viewLifecycleOwner) { sent ->
            if (sent) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.reset_email_sent),
                    Toast.LENGTH_LONG
                ).show()
                findNavController().popBackStack()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}