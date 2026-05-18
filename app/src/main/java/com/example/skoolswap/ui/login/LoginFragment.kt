package com.example.skoolswap.ui.login

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.skoolswap.R
import com.example.skoolswap.databinding.FragmentLoginBinding
import com.example.skoolswap.data.local.datastore.AppPreferences
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginViewModel by viewModels()

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var authRepository: AuthRepositoryInterface  // ADD THIS

    private var isSigningIn = false
    private var isSendingOtp = false
    private var videoBackgroundManager: VideoBackgroundManager? = null

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
      //  setupVideoBackground()
        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.signInButton.setOnClickListener {
            if (isSigningIn) return@setOnClickListener

            isSigningIn = true
            binding.signInButton.isEnabled = false
            viewModel.signInWithGoogle(requireActivity())

            binding.signInButton.postDelayed({
                if (isSigningIn) {
                    isSigningIn = false
                    binding.signInButton.isEnabled = true
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
                    binding.loginButton.isEnabled = true
                }
            }, 10000)
        }

        binding.textSignUpLink.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_signUpFragment)
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
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
            if (error != null) {
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
            if (user != null) {
                navigateAfterLogin(user)
            }
        }

        viewModel.otpSent.observe(viewLifecycleOwner) { otpToken ->
            if (otpToken != null) {
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

                if (!isOnboardingFinished) {
                    findNavController().navigate(R.id.action_loginFragment_to_onboarding)
                    return@launch
                }

                val isLoggedIn = isUserLoggedIn()

                if (isLoggedIn) {
                    val authToken = appPreferences.authToken.first() ?: ""
                    val isValid = authRepository.validateToken(authToken)

                    if (isValid) {
                        // Only hide AFTER we know we're navigating away
                        binding.root.visibility = View.INVISIBLE

                        val schoolMapped = appPreferences.hasSchoolMapped()
                        val userId = appPreferences.getUserId() ?: 0
                        val userName = appPreferences.userName.first() ?: ""
                        val userEmail = appPreferences.userEmail.first() ?: ""
                        val userProfileImage = appPreferences.userProfileImage.first() ?: ""

                        val restoredUser = com.example.skoolswap.domain.model.User(
                            id = userId.toInt(),
                            name = userName,
                            email = userEmail,
                            mobile = null,
                            username = userName,
                            profilePictureUrl = userProfileImage,
                            authMode = "google",
                            role = "user",
                            token = authToken,
                            createdAt = "",
                            updatedAt = "",
                            schoolMapped = schoolMapped,
                            schoolId = if (schoolMapped) appPreferences.schoolId.first() else null,
                            schoolName = if (schoolMapped) appPreferences.schoolName.first() else null
                        )

                        viewModel.setRestoredUser(restoredUser)
                        navigateAfterLogin(restoredUser)
                        return@launch
                    } else {
                        appPreferences.clearUserData()
                    }
                }

                // Not logged in — show login UI normally
                binding.root.visibility = View.VISIBLE

            } catch (e: Exception) {
                Log.e("LoginFragment", "❌ Error checking session: ${e.message}")
                binding.root.visibility = View.VISIBLE
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

    private fun setupVideoBackground() {
        val videoPath = "android.resource://${requireContext().packageName}/${R.raw.login_background}"
        videoBackgroundManager = VideoBackgroundManager(binding.videoView, videoPath)
        videoBackgroundManager?.setupVideo()
    }

    override fun onResume() {
        super.onResume()
        videoBackgroundManager?.resumeVideo()
    }

    override fun onPause() {
        super.onPause()
        videoBackgroundManager?.pauseVideo()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        videoBackgroundManager?.release()
        videoBackgroundManager = null
        _binding = null
    }
}