package com.example.skoolswap.ui.otp

import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.skoolswap.R
import com.example.skoolswap.data.local.datastore.AppPreferences
import com.example.skoolswap.databinding.FragmentOtpBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OTPFragment : Fragment() {

    private var _binding: FragmentOtpBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OTPViewModel by viewModels()

    @Inject
    lateinit var appPreferences: AppPreferences

    private var countDownTimer: CountDownTimer? = null
    private var isResendEnabled = false
    private val otpInputs = mutableListOf<EditText>()

    // Store arguments
    private var email: String = ""
    private var otpToken: String = ""
    private var purpose: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOtpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get arguments
        email = arguments?.getString("email") ?: ""
        otpToken = arguments?.getString("otp_token") ?: ""
        purpose = arguments?.getString("purpose") ?: "LOGIN"

        // ✅ Handle back button press
        setupBackButton()

        updateDescriptionWithEmail(email)
        initializeOtpInputs()
        setupOtpAutoMove()
        setupClickListeners()
        observeViewModel()
        startResendTimer()
    }

    // ✅ Handle back button navigation
    private fun setupBackButton() {
        // Using OnBackPressedCallback for AndroidX
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    navigateBackToLogin()
                }
            }
        )
    }

    private fun navigateBackToLogin() {
        try {
            if (purpose == "SIGNUP") {
                findNavController().navigate(R.id.action_otpFragment_to_signUpFragment)
            } else {
                findNavController().navigate(R.id.action_otpFragment_to_loginFragment)
            }
        } catch (e: Exception) {
            findNavController().popBackStack()
        }
    }

    private fun updateDescriptionWithEmail(email: String) {
        val htmlText = "Enter the security code we just sent to<br><font color='#000000'>$email</font>"
        binding.descriptionText.text = HtmlCompat.fromHtml(htmlText, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }

    private fun initializeOtpInputs() {
        otpInputs.apply {
            add(binding.otpInput1)
            add(binding.otpInput2)
            add(binding.otpInput3)
            add(binding.otpInput4)
            add(binding.otpInput5)
            add(binding.otpInput6)
        }
        binding.otpInput1.requestFocus()
        showCursor(binding.otpInput1)
    }

    private fun setupOtpAutoMove() {
        otpInputs.forEachIndexed { index, editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s?.length == 1) {
                        if (index < otpInputs.size - 1) {
                            otpInputs[index + 1].requestFocus()
                            showCursor(otpInputs[index + 1])
                        } else {
                            editText.clearFocus()
                        }
                    } else if (s?.isEmpty() == true && index > 0) {
                        otpInputs[index - 1].requestFocus()
                        showCursor(otpInputs[index - 1])
                        otpInputs[index - 1].text?.clear()
                    }
                    updateVerifyButtonState()
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            editText.setOnClickListener {
                editText.requestFocus()
                showCursor(editText)
            }
        }
    }

    private fun showCursor(editText: EditText) {
        editText.isCursorVisible = true
        editText.setSelection(editText.text?.length ?: 0)
    }

    private fun updateVerifyButtonState() {
        val isOtpComplete = otpInputs.all { it.text?.length == 1 }
        binding.buttonVerify.isEnabled = isOtpComplete
        binding.buttonVerify.alpha = if (isOtpComplete) 1.0f else 0.5f
    }

    private fun setupClickListeners() {
        binding.buttonVerify.setOnClickListener {
            val otpCode = otpInputs.joinToString("") { it.text.toString() }
            if (otpCode.length == 6) {
                // Use the current token (from initial or resend)
                val currentToken = viewModel.getCurrentOtpToken().takeIf { it.isNotEmpty() } ?: otpToken
                viewModel.verifyOtp(email, currentToken, otpCode, purpose)
            } else {
                Toast.makeText(requireContext(), "Please enter complete OTP", Toast.LENGTH_SHORT).show()
            }
        }

        binding.textResendOtpButton.setOnClickListener {
            if (isResendEnabled) {
                viewModel.resendOtp(email, purpose)
            }
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                binding.progressBar.visibility = View.VISIBLE
                binding.buttonVerify.isEnabled = false
                binding.buttonVerify.text = "Verifying..."
            } else {
                binding.progressBar.visibility = View.GONE
                binding.buttonVerify.isEnabled = true
                binding.buttonVerify.text = "Verify"
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                resetOtpFields()
            }
        }

        viewModel.verificationSuccess.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                Toast.makeText(requireContext(), "Verification Successful!", Toast.LENGTH_SHORT).show()
                navigateAfterVerification(user)
            }
        }

        viewModel.resendSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "OTP sent successfully!", Toast.LENGTH_SHORT).show()
                resetTimer()
                resetOtpFields()
            } else {
                Toast.makeText(requireContext(), "Failed to resend OTP", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateAfterVerification(user: com.example.skoolswap.domain.model.User) {
        if (!isAdded || isDetached) return

        try {
            if (user.schoolMapped) {
                findNavController().navigate(R.id.action_otpFragment_to_nav_home)
            } else {
                findNavController().navigate(R.id.action_otpFragment_to_nav_profile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startResendTimer() {
        val totalTime = 60 * 1000L
        val interval = 1000L

        countDownTimer = object : CountDownTimer(totalTime, interval) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = millisUntilFinished / 1000
                binding.textTimer.text = "${secondsRemaining}s"
            }

            override fun onFinish() {
                isResendEnabled = true
                binding.textResendOtp.visibility = View.GONE
                binding.textTimer.visibility = View.GONE
                binding.textResendOtpButton.visibility = View.VISIBLE
            }
        }.start()
    }

    private fun resetTimer() {
        countDownTimer?.cancel()
        isResendEnabled = false
        binding.textResendOtp.visibility = View.VISIBLE
        binding.textTimer.visibility = View.VISIBLE
        binding.textResendOtpButton.visibility = View.GONE
        startResendTimer()
    }

    private fun resetOtpFields() {
        otpInputs.forEach {
            it.text?.clear()
            it.isCursorVisible = false
        }
        binding.otpInput1.requestFocus()
        showCursor(binding.otpInput1)
        updateVerifyButtonState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
        _binding = null
    }
}