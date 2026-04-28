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

        // CRITICAL: Check onboarding status FIRST
        checkOnboardingStatus()

        // Setup video background
        setupVideoBackground()

        // Set callback
        setupCallback()
        setupUI()
    }

    private fun checkOnboardingStatus() {
        Log.e("LoginFragment", "🔍 checkOnboardingStatus() called")
        lifecycleScope.launch {
            try {
                val isOnboardingFinished = appPreferences.isOnboardingFinished.first()
                Log.e("LoginFragment", "🔍 Onboarding finished: $isOnboardingFinished")

                if (!isOnboardingFinished) {
                    // User hasn't completed onboarding, redirect to onboarding
                    Log.e("LoginFragment", "🚀 Redirecting to onboarding")
                    findNavController().navigate(R.id.action_loginFragment_to_onboarding)
                } else {
                    Log.e("LoginFragment", "✅ Onboarding completed, showing login")
                    // Continue with login UI setup
                }
            } catch (e: Exception) {
                Log.e("LoginFragment", "❌ Error checking onboarding status: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun setupVideoBackground() {
        // Option 1: Using raw resource
        val videoPath = "android.resource://${requireContext().packageName}/${R.raw.login_background}"

        videoBackgroundManager = VideoBackgroundManager(binding.videoView, videoPath)
        videoBackgroundManager?.setupVideo()
    }

    private fun setupCallback() {
        Log.e("LoginFragment", "🔥 Setting up callback...")

        viewModel.onLoginSuccess = { user ->
            Log.e("LoginFragment", "🎯🎯🎯 CALLBACK EXECUTING at ${System.currentTimeMillis()}!")
            Log.e("LoginFragment", "🎯 User email: ${user.email}")
            Log.e("LoginFragment", "🎯 schoolMapped: ${user.schoolMapped}")

            // ✅ Check if fragment is still attached
            if (isAdded && !isDetached) {
                try {
                    if (user.schoolMapped) {
                        Log.e("LoginFragment", "🚀 Navigating to HOME")
                        findNavController().navigate(R.id.action_loginFragment_to_nav_home)
                    } else {
                        Log.e("LoginFragment", "🚀 Navigating to PROFILE")
                        findNavController().navigate(R.id.action_loginFragment_to_profileFragment)
                    }
                    Log.e("LoginFragment", "✅ Navigation call completed")
                } catch (e: Exception) {
                    Log.e("LoginFragment", "❌ Navigation failed: ${e.message}")
                    e.printStackTrace()
                }
            } else {
                Log.e("LoginFragment", "⚠️ Fragment not attached, skipping navigation")
            }
        }
    }

    private fun setupUI() {
        binding.signInButton.setOnClickListener {
            // Prevent multiple clicks
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
                isSigningIn = false
                binding.signInButton.isEnabled = true
            }, 5000)
        }
    }

    override fun onResume() {
        super.onResume()
        Log.e("LoginFragment", "🔥 onResume - callback exists: ${viewModel.onLoginSuccess != null}")
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