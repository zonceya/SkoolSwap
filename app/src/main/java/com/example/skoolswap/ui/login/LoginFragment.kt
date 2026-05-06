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
    private var isSigningIn = false
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

        Log.e("LoginFragment", "🔥 onViewCreated at ${System.currentTimeMillis()}")

        // CRITICAL: Check if user is already logged in
        checkExistingSession()

        // Setup video background
        setupVideoBackground()

        // Setup UI
        setupUI()

        // Observe ViewModel state (no callback needed anymore)
        observeViewModel()
    }

    private suspend fun isUserLoggedIn(): Boolean {
        return try {
            val isLoggedIn = appPreferences.isLoggedIn.first()
            val authToken = appPreferences.authToken.first()
            val userId = appPreferences.getUserId()

            // Check if we have a valid session
            isLoggedIn && !authToken.isNullOrEmpty() && userId != null
        } catch (e: Exception) {
            Log.e("LoginFragment", "❌ Error checking login status: ${e.message}")
            false
        }
    }

    private fun checkExistingSession() {
        Log.e("LoginFragment", "🔍 checkExistingSession() called")
        lifecycleScope.launch {
            try {
                // First check onboarding
                val isOnboardingFinished = appPreferences.isOnboardingFinished.first()
                Log.e("LoginFragment", "🔍 Onboarding finished: $isOnboardingFinished")

                if (!isOnboardingFinished) {
                    // User hasn't completed onboarding, redirect to onboarding
                    Log.e("LoginFragment", "🚀 Redirecting to onboarding")
                    findNavController().navigate(R.id.action_loginFragment_to_onboarding)
                    return@launch
                }

                // Check if already logged in
                val isLoggedIn = isUserLoggedIn()
                Log.e("LoginFragment", "🔍 Already logged in: $isLoggedIn")

                if (isLoggedIn) {
                    // User is already logged in, restore session and navigate
                    Log.e("LoginFragment", "🚀 User already logged in, checking school status...")

                    val schoolMapped = appPreferences.hasSchoolMapped()
                    Log.e("LoginFragment", "🔍 School mapped: $schoolMapped")

                    // Restore user data to ViewModel
                    val userId = appPreferences.getUserId() ?: 0
                    val userName = appPreferences.userName.first() ?: ""
                    val userEmail = appPreferences.userEmail.first() ?: ""
                    val userProfileImage = appPreferences.userProfileImage.first() ?: ""
                    val authToken = appPreferences.authToken.first() ?: ""

                    // Create restored user with proper types
                    val restoredUser = com.example.skoolswap.domain.model.User(
                        id = userId.toInt(),
                        name = userName,
                        email = userEmail,
                        mobile = null,  // This is nullable String? so null is fine
                        username = userName,
                        profilePictureUrl = userProfileImage,
                        authMode = "google",
                        role = "user",
                        token = authToken,
                        createdAt = "",  // Use empty string instead of null
                        updatedAt = "",  // Use empty string instead of null
                        schoolMapped = schoolMapped,
                        schoolId = if (schoolMapped) appPreferences.schoolId.first() else null,
                        schoolName = if (schoolMapped) appPreferences.schoolName.first() else null
                    )

                    // Set user in ViewModel to maintain state
                    viewModel.setRestoredUser(restoredUser)

                    // Navigate directly
                    navigateAfterLogin(restoredUser)
                } else {
                    Log.e("LoginFragment", "✅ No existing session, showing login UI")
                    // Continue with normal login flow
                }
            } catch (e: Exception) {
                Log.e("LoginFragment", "❌ Error checking session: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun setupVideoBackground() {
        val videoPath = "android.resource://${requireContext().packageName}/${R.raw.login_background}"
        videoBackgroundManager = VideoBackgroundManager(binding.videoView, videoPath)
        videoBackgroundManager?.setupVideo()
    }

    private fun observeViewModel() {
        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                binding.signInButton.isEnabled = false
                // Show progress indicator if you have one
            } else {
                binding.signInButton.isEnabled = !isSigningIn
            }
        }

        // Observe error state
        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                // Show error to user
                com.google.android.material.snackbar.Snackbar.make(
                    binding.root,
                    error,
                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                ).show()

                // Reset signing in state
                isSigningIn = false
                binding.signInButton.isEnabled = true
            }
        }

        // Observe login success
        viewModel.loginSuccess.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                Log.e("LoginFragment", "🎉 Login success observed, navigating...")
                navigateAfterLogin(user)
            }
        }
    }

    private fun navigateAfterLogin(user: com.example.skoolswap.domain.model.User) {
        if (!isAdded || isDetached) {
            Log.e("LoginFragment", "⚠️ Fragment not attached, skipping navigation")
            return
        }

        try {
            val currentDestination = findNavController().currentDestination?.id

            if (user.schoolMapped) {
                // Only navigate if we're not already on home
                if (currentDestination != R.id.nav_home) {
                    Log.e("LoginFragment", "🚀 Navigating to HOME")
                    findNavController().navigate(R.id.action_loginFragment_to_nav_home)
                } else {
                    Log.e("LoginFragment", "✅ Already on HOME, no navigation needed")
                }
            } else {
                // Only navigate to profile if we're not already there
                if (currentDestination != R.id.nav_profile) {
                    Log.e("LoginFragment", "🚀 Navigating to PROFILE")
                    findNavController().navigate(R.id.action_loginFragment_to_profileFragment)
                } else {
                    Log.e("LoginFragment", "✅ Already on PROFILE, no navigation needed")
                }
            }
        } catch (e: Exception) {
            Log.e("LoginFragment", "❌ Navigation failed: ${e.message}")
            e.printStackTrace()
        }
    }
    private fun setupUI() {
        binding.signInButton.setOnClickListener {
            if (isSigningIn) {
                Log.d("LoginFragment", "⚠️ Already signing in, ignoring click")
                return@setOnClickListener
            }

            isSigningIn = true
            binding.signInButton.isEnabled = false

            Log.e("LoginFragment", "🔥 SIGN IN CLICKED at ${System.currentTimeMillis()}")
            viewModel.signInWithGoogle(requireActivity())

            // Reset after 5 seconds in case of error
            binding.signInButton.postDelayed({
                if (isSigningIn) {
                    isSigningIn = false
                    binding.signInButton.isEnabled = true
                }
            }, 5000)
        }
    }

    override fun onResume() {
        super.onResume()
        Log.e("LoginFragment", "🔥 onResume")
        videoBackgroundManager?.resumeVideo()
    }

    override fun onPause() {
        super.onPause()
        Log.e("LoginFragment", "🔥 onPause")
        videoBackgroundManager?.pauseVideo()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.e("LoginFragment", "🔥 onDestroyView - clearing resources")
        videoBackgroundManager?.release()
        videoBackgroundManager = null
        _binding = null
    }
}