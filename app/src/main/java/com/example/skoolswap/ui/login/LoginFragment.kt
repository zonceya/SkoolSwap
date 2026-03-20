package com.example.skoolswap.ui.login

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.VideoView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.skoolswap.R
import com.example.skoolswap.databinding.FragmentLoginBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginViewModel by viewModels()

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

        // Setup video background
        setupVideoBackground()

        // Set callback
        setupCallback()
        setupUI()
    }

    private fun setupVideoBackground() {
        // Option 1: Using raw resource
        val videoPath = "android.resource://${requireContext().packageName}/${R.raw.login_background}"

        // Option 2: Using file from assets (uncomment if you prefer this method)
        // val videoPath = "file:///android_asset/login_background.mp4"

        videoBackgroundManager = VideoBackgroundManager(binding.videoView, videoPath)
        videoBackgroundManager?.setupVideo()
    }

    private fun setupCallback() {
        Log.e("LoginFragment", "🔥 Setting up callback...")

        viewModel.onLoginSuccess = { user ->
            Log.e("LoginFragment", "🎯🎯🎯 CALLBACK EXECUTING at ${System.currentTimeMillis()}!")
            Log.e("LoginFragment", "🎯 User email: ${user.email}")
            Log.e("LoginFragment", "🎯 schoolMapped: ${user.schoolMapped}")

            try {
                if (user.schoolMapped) {
                    Log.e("LoginFragment", "🚀 Navigating to HOME using ID: ${R.id.nav_home}")
                    findNavController().navigate(R.id.nav_home)
                } else {
                    Log.e("LoginFragment", "🚀 Navigating to PROFILE using ID: ${R.id.nav_profile}")
                    findNavController().navigate(R.id.nav_profile)
                }
                Log.e("LoginFragment", "✅ Navigation call completed")
            } catch (e: Exception) {
                Log.e("LoginFragment", "❌ Navigation failed: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun setupUI() {
        binding.signInButton.setOnClickListener {
            Log.e("LoginFragment", "🔥 SIGN IN CLICKED at ${System.currentTimeMillis()}")
            viewModel.signInWithGoogle(requireActivity())
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