package com.example.skoolswap.ui.login

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.skoolswap.R
import com.example.skoolswap.databinding.FragmentLoginBinding
import com.example.skoolswap.data.local.datastore.AppPreferences
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!  // Safe after onViewCreated
    private val viewModel: LoginViewModel by viewModels()

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var authRepository: AuthRepositoryInterface

    private var isSigningIn = false
    private var isSendingOtp = false
    // REMOVED: private var videoBackgroundManager: VideoBackgroundManager? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.e("LoginFragment", "🔥 onViewCreated")

        checkExistingSession()
        // REMOVED: setupVideoBackground()
        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        if (_binding == null) return
        setBoxStrokeColor(binding.emailLayout, ContextCompat.getColor(requireContext(), R.color.white))
        setBoxStrokeColor(binding.passwordLayout, ContextCompat.getColor(requireContext(), R.color.white))
        binding.signInButton.setOnClickListener {
            if (isSigningIn) return@setOnClickListener

            isSigningIn = true
            binding.signInButton.isEnabled = false
            viewModel.signInWithGoogle(requireActivity())

            binding.signInButton.postDelayed({
                if (isSigningIn) {
                    isSigningIn = false
                    if (_binding != null) {
                        binding.signInButton.isEnabled = true
                    }
                }
            }, 5000)
        }

        binding.loginButton.setOnClickListener {
            if (isSendingOtp) return@setOnClickListener

            val email = binding.emailInput.text.toString().trim()

            if (email.isEmpty()) {
                binding.emailLayout.error = "Email is required"
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.emailLayout.error = "Invalid email format"
                return@setOnClickListener
            }

            binding.emailLayout.error = null
            isSendingOtp = true
            binding.loginButton.isEnabled = false

            viewModel.sendLoginOtp(email)

            binding.loginButton.postDelayed({
                if (isSendingOtp) {
                    isSendingOtp = false
                    if (_binding != null) {
                        binding.loginButton.isEnabled = true
                    }
                }
            }, 10000)
        }

        binding.textSignUpLink.setOnClickListener {
            if (_binding != null) {
                findNavController().navigate(R.id.action_loginFragment_to_signUpFragment)
            }
        }
    }
    private fun setBoxStrokeColor(textInputLayout: TextInputLayout, color: Int) {
        try {
            val states = arrayOf(
                intArrayOf(android.R.attr.state_focused),
                intArrayOf(android.R.attr.state_hovered),
                intArrayOf(-android.R.attr.state_enabled),
                intArrayOf() // default
            )
            val colors = intArrayOf(color, color, color, color)
            val colorStateList = ColorStateList(states, colors)

            textInputLayout.setBoxStrokeColorStateList(colorStateList)
            textInputLayout.hintTextColor = colorStateList
            textInputLayout.defaultHintTextColor = colorStateList
        } catch (e: Exception) {
            Log.e("LoginFragment", "Error setting box stroke color: ${e.message}")
        }
    }
    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (_binding == null) return@observe

            if (isLoading) {
                binding.signInButton.isEnabled = false
                binding.loginButton.isEnabled = false
                binding.progressBar.visibility = View.VISIBLE
            } else {
                binding.signInButton.isEnabled = !isSigningIn
                binding.loginButton.isEnabled = !isSendingOtp
                binding.progressBar.visibility = View.GONE
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null && _binding != null) {
                com.google.android.material.snackbar.Snackbar.make(
                    binding.root,
                    error,
                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                ).show()

                isSigningIn = false
                isSendingOtp = false
                binding.signInButton.isEnabled = true
                binding.loginButton.isEnabled = true
                binding.progressBar.visibility = View.GONE
            }
        }

        viewModel.loginSuccess.observe(viewLifecycleOwner) { user ->
            if (user != null && _binding != null) {
                navigateAfterLogin(user)
            }
        }

        viewModel.otpSent.observe(viewLifecycleOwner) { otpToken ->
            if (otpToken != null && _binding != null) {
                val email = binding.emailInput.text.toString().trim()
                val bundle = Bundle().apply {
                    putString("email", email)
                    putString("otp_token", otpToken)
                    putString("purpose", "LOGIN")
                }
                isSendingOtp = false
                findNavController().navigate(R.id.action_loginFragment_to_otpFragment, bundle)
                viewModel.clearOtpSent()
            }
        }
    }

    private suspend fun isUserLoggedIn(): Boolean {
        return try {
            val isLoggedIn = appPreferences.isLoggedIn.first()
            val authToken = appPreferences.authToken.first()
            val userId = appPreferences.getUserId()
            isLoggedIn && !authToken.isNullOrEmpty() && userId != null
        } catch (e: Exception) {
            false
        }
    }

    private fun checkExistingSession() {
        lifecycleScope.launch {
            try {
                val isOnboardingFinished = appPreferences.isOnboardingFinished.first()

                if (!isAdded || isDetached || _binding == null) {
                    Log.d("LoginFragment", "Fragment not in valid state, skipping session check")
                    return@launch
                }

                if (!isOnboardingFinished) {
                    findNavController().navigate(R.id.action_loginFragment_to_onboarding)
                    return@launch
                }

                val restored = authRepository.restoreSession()

                if (!isAdded || isDetached || _binding == null) {
                    Log.d("LoginFragment", "Fragment state changed during restore, aborting")
                    return@launch
                }

                if (restored) {
                    binding.root.visibility = View.INVISIBLE
                    val user = authRepository.getServerUser().first()

                    if (!isAdded || isDetached || _binding == null) return@launch

                    if (user == null) {
                        binding.root.visibility = View.VISIBLE
                        return@launch
                    }
                    navigateAfterLogin(user)
                } else {
                    binding.root.visibility = View.VISIBLE
                }

            } catch (e: Exception) {
                Log.e("LoginFragment", "❌ Error checking session: ${e.message}")
                if (isAdded && !isDetached && _binding != null) {
                    binding.root.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun navigateAfterLogin(user: com.example.skoolswap.domain.model.User) {
        if (!isAdded || isDetached) return

        try {
            if (user.schoolMapped) {
                findNavController().navigate(R.id.action_loginFragment_to_nav_home)
            } else {
                findNavController().navigate(R.id.action_loginFragment_to_profileFragment)
            }
        } catch (e: Exception) {
            Log.e("LoginFragment", "Navigation failed: ${e.message}")
        }
    }

    // REMOVED: setupVideoBackground() method

    // REMOVED: onResume, onPause video calls

    override fun onDestroyView() {
        super.onDestroyView()
        // REMOVED: videoBackgroundManager?.release()
        // REMOVED: videoBackgroundManager = null
        _binding = null
    }
}