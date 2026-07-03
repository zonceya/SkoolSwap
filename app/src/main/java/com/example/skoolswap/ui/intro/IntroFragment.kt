package com.example.skoolswap.ui.intro

import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.skoolswap.R
import com.example.skoolswap.databinding.FragmentIntroBinding
import com.example.skoolswap.data.local.datastore.AppPreferences
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class IntroFragment : Fragment() {

    private var _binding: FragmentIntroBinding? = null
    private val binding get() = _binding!!
    private val TAG = "IntroFragment"

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var authRepository: AuthRepositoryInterface

    // Gate variables to ensure both conditions are met before navigation
    private var videoFinished = false
    private var destinationReady = false
    private var targetDestination: Int? = null

    // Track video start time for minimum display time
    private var videoStartTime: Long = 0
    private val MIN_DISPLAY_MS = 3000L // 3 seconds minimum
    private val FORCE_SCHOOL_ONBOARDING = false

    // Absolute ceiling on how long we wait for the video pipeline
    private val VIDEO_WATCHDOG_TIMEOUT_MS = 8000L
    private var videoWatchdogJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Timber.tag(TAG).e("onCreateView called")
        _binding = FragmentIntroBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.tag(TAG).e("onViewCreated called")

        // ✅ SHOW STATUS BAR IMMEDIATELY on initial load
        showSystemUI()

        // Show placeholder splash immediately
        binding.splashPlaceholder.visibility = View.VISIBLE

        // Run checks in parallel with video playback
        determineDestination()

        // Start video
        setupVideo()
        startVideoWatchdog()
    }

    // ==================== SYSTEM UI CONTROLS ====================

    /**
     * Show both status bar (network/battery/time) and navigation bar
     * This ensures users can see network and battery indicators
     */
    private fun showSystemUI() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                requireActivity().window.insetsController?.let { controller ->
                    controller.show(android.view.WindowInsets.Type.statusBars())
                    controller.show(android.view.WindowInsets.Type.navigationBars())
                    controller.systemBarsBehavior =
                        android.view.WindowInsetsController.BEHAVIOR_SHOW_BARS_BY_TOUCH
                }
            } else {
                @Suppress("DEPRECATION")
                requireActivity().window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_VISIBLE
            }
            Timber.tag(TAG).e("✅ System UI shown (status bar visible)")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error showing system UI")
        }
    }

    /**
     * Hide system UI for immersive video experience
     * Only used if you want full-screen video
     */
    private fun hideSystemUIForVideo() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                requireActivity().window.insetsController?.let { controller ->
                    controller.hide(android.view.WindowInsets.Type.statusBars())
                    controller.hide(android.view.WindowInsets.Type.navigationBars())
                    controller.systemBarsBehavior =
                        android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                requireActivity().window.decorView.systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                or View.SYSTEM_UI_FLAG_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        )
            }
            Timber.tag(TAG).e("🎬 System UI hidden for video")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error hiding system UI")
        }
    }

    // ==================== DESTINATION LOGIC ====================

    private fun determineDestination() {
        lifecycleScope.launch {
            try {
                // Quick check: If user is already properly logged in → go straight to home
                val isLoggedIn = appPreferences.isLoggedIn.first()
                val roomUser = authRepository.getRoomUser()

                if (isLoggedIn && roomUser != null && !roomUser.token.isNullOrEmpty()) {
                    Timber.tag(TAG).i("✅ User already logged in (Room + Preferences)")

                    val destination = if (roomUser.schoolMapped) {
                        R.id.nav_home
                    } else {
                        R.id.nav_profile
                    }
                    onDestinationDetermined(destination)
                    return@launch
                }

                // Fallback: Try full session recovery
                val restored = authRepository.restoreSession()
                if (restored) {
                    val updatedUser = authRepository.getRoomUser()
                    val destination = if (updatedUser?.schoolMapped == true) {
                        R.id.nav_home
                    } else {
                        R.id.nav_profile
                    }
                    onDestinationDetermined(destination)
                    return@launch
                }

                // No valid session → go to login
                Timber.tag(TAG).i("❌ No valid session found → login")
                onDestinationDetermined(R.id.loginFragment)

            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error determining destination")
                onDestinationDetermined(R.id.loginFragment)
            }
        }
    }

    private fun onDestinationDetermined(destination: Int) {
        Timber.tag(TAG).e("Destination determined: $destination")
        targetDestination = destination
        destinationReady = true
        maybeNavigate()
    }

    private fun onVideoFinished() {
        Timber.tag(TAG).e("Video finished or failed")
        videoWatchdogJob?.cancel()
        videoWatchdogJob = null
        if (videoFinished) {
            // Already finished (e.g. watchdog fired first) — ignore late callback.
            return
        }
        videoFinished = true
        // ✅ Show status bar when video finishes
        showSystemUI()
        maybeNavigate()
    }

    private fun maybeNavigate() {
        Timber.tag(TAG)
            .e("maybeNavigate - videoFinished: $videoFinished, destinationReady: $destinationReady")
        if (videoFinished && destinationReady) {
            Timber.tag(TAG).e("Both conditions met, navigating to destination")
            navigateToDestination()
        } else {
            Timber.tag(TAG).e("Waiting for both conditions to be true")
        }
    }

    // ==================== VIDEO WATCHDOG ====================

    private fun startVideoWatchdog() {
        videoWatchdogJob?.cancel()
        videoWatchdogJob = lifecycleScope.launch {
            delay(VIDEO_WATCHDOG_TIMEOUT_MS)
            if (!videoFinished) {
                Timber.tag(TAG).e(
                    "⏱️ Video watchdog fired after ${VIDEO_WATCHDOG_TIMEOUT_MS}ms — " +
                            "video pipeline did not call back, forcing navigation"
                )
                if (_binding != null) {
                    binding.splashPlaceholder.visibility = View.GONE
                    binding.progressBar.visibility = View.GONE
                }
                // ✅ Show status bar when watchdog fires
                showSystemUI()
                onVideoFinished()
            }
        }
    }

    // ==================== VIDEO SETUP ====================

    @RequiresApi(Build.VERSION_CODES.R)
    private fun setupVideo() {
        Timber.tag(TAG).e("setupVideo started")

        try {
            // Record start time for minimum display time
            videoStartTime = SystemClock.elapsedRealtime()

            // Check if video file exists in raw resources
            val videoPath = "android.resource://${requireContext().packageName}/${R.raw.intro_video}"
            Timber.tag(TAG).e("Video path: $videoPath")

            val uri = videoPath.toUri()
            Timber.tag(TAG).e("URI parsed: $uri")

            binding.videoView.setVideoURI(uri)
            Timber.tag(TAG).e("Video URI set on VideoView")

            binding.videoView.setOnPreparedListener { mediaPlayer ->
                mediaPlayer.isLooping = false

                // Fix scaling — force video to fill the screen
                val display = requireActivity().windowManager.currentWindowMetrics.bounds
                val screenWidth = display.width()
                val screenHeight = display.height()

                val videoWidth = mediaPlayer.videoWidth.takeIf { it > 0 } ?: screenWidth
                val videoHeight = mediaPlayer.videoHeight.takeIf { it > 0 } ?: screenHeight

                val scaleX = screenWidth.toFloat() / videoWidth
                val scaleY = screenHeight.toFloat() / videoHeight
                val scale = maxOf(scaleX, scaleY)

                val params = binding.videoView.layoutParams
                params.width = (videoWidth * scale).toInt()
                params.height = (videoHeight * scale).toInt()
                binding.videoView.layoutParams = params
                binding.videoView.requestLayout()

                binding.splashPlaceholder.visibility = View.GONE
                binding.progressBar.visibility = View.GONE
                binding.videoView.start()
            }

            binding.videoView.setOnCompletionListener {
                Timber.tag(TAG).e("Video completed")
                val elapsed = SystemClock.elapsedRealtime() - videoStartTime
                val remaining = MIN_DISPLAY_MS - elapsed

                Timber.tag(TAG)
                    .e("Video displayed for ${elapsed}ms, minimum required: ${MIN_DISPLAY_MS}ms")

                // ✅ Show status bar when video completes
                showSystemUI()

                if (remaining > 0) {
                    Timber.tag(TAG).e("Waiting additional ${remaining}ms before finishing")
                    binding.root.postDelayed({
                        onVideoFinished()
                    }, remaining)
                } else {
                    onVideoFinished()
                }
            }

            binding.videoView.setOnErrorListener { _, what, extra ->
                Timber.tag(TAG).e("Video error occurred!")
                Timber.tag(TAG).e("Error code what: $what")
                Timber.tag(TAG).e("Error code extra: $extra")

                // Hide placeholder and progress bar on error
                binding.splashPlaceholder.visibility = View.GONE
                binding.progressBar.visibility = View.GONE

                // ✅ Show status bar on error
                showSystemUI()

                // Apply minimum display time even on error
                val elapsed = SystemClock.elapsedRealtime() - videoStartTime
                val remaining = MIN_DISPLAY_MS - elapsed

                if (remaining > 0) {
                    binding.root.postDelayed({
                        onVideoFinished()
                    }, remaining)
                } else {
                    onVideoFinished()
                }
                true
            }

            // Show progress bar while loading
            binding.progressBar.visibility = View.VISIBLE
            Timber.tag(TAG).e("Progress bar shown, waiting for video to prepare")

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Exception in setupVideo: ${e.message}")
            binding.splashPlaceholder.visibility = View.GONE
            binding.progressBar.visibility = View.GONE
            // ✅ Show status bar on exception
            showSystemUI()
            onVideoFinished()
        }
    }

    // ==================== NAVIGATION ====================

    private fun navigateToDestination() {
        Timber.tag(TAG).e("navigateToDestination called")
        Timber.tag(TAG).e("isAdded: $isAdded, isDetached: $isDetached")

        if (!isAdded || isDetached) {
            Timber.tag(TAG).e("Fragment not in valid state, skipping navigation")
            return
        }

        // ✅ Show system UI before navigation
        showSystemUI()

        val destination = targetDestination ?: R.id.loginFragment
        Timber.tag(TAG).e("Target destination ID: $destination")

        // Map destination ID to readable name for logging
        val destName = when (destination) {
            R.id.loginFragment -> "loginFragment"
            R.id.viewPagerFragment -> "viewPagerFragment (onboarding)"
            R.id.schoolOnboardingFragment -> "schoolOnboardingFragment"
            R.id.nav_home -> "nav_home"
            R.id.nav_profile -> "nav_profile"
            else -> "unknown"
        }
        Timber.tag(TAG).e("Will navigate to: $destName")

        try {
            when (destination) {
                R.id.loginFragment -> {
                    Timber.tag(TAG).e("Navigating to loginFragment")
                    val bundle = Bundle().apply {
                        putBoolean("from_intro", true)
                    }
                    findNavController().navigate(R.id.action_introFragment_to_loginFragment, bundle)
                }
                R.id.viewPagerFragment -> {
                    Timber.tag(TAG).e("Navigating to onboarding")
                    findNavController().navigate(R.id.action_introFragment_to_onboarding)
                }
                R.id.schoolOnboardingFragment -> {
                    Timber.tag(TAG).e("🔴 Navigating to school onboarding")
                    findNavController().navigate(R.id.action_introFragment_to_schoolOnboardingFragment)
                }
                R.id.nav_home -> {
                    Timber.tag(TAG).e("Navigating to home")
                    findNavController().navigate(R.id.action_introFragment_to_nav_home)
                }
                R.id.nav_profile -> {
                    Timber.tag(TAG).e("Navigating to profile")
                    findNavController().navigate(R.id.action_introFragment_to_profileFragment)
                }
                else -> {
                    Timber.tag(TAG).e("Unknown destination, falling back to login")
                    val bundle = Bundle().apply {
                        putBoolean("from_intro", true)
                    }
                    findNavController().navigate(R.id.action_introFragment_to_loginFragment, bundle)
                }
            }
            Timber.tag(TAG).e("Navigation command executed successfully")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Navigation failed: ${e.message}")
            // Fallback to login
            try {
                val bundle = Bundle().apply {
                    putBoolean("from_intro", true)
                }
                findNavController().navigate(R.id.action_introFragment_to_loginFragment, bundle)
            } catch (e2: Exception) {
                Timber.tag(TAG).e(e2, "Fallback navigation also failed: ${e2.message}")
            }
        }
    }

    // ==================== LIFECYCLE ====================

    override fun onResume() {
        super.onResume()
        Timber.tag(TAG).e("onResume called")

        // ✅ ALWAYS show status bar when resuming - this ensures network/battery signs appear
        showSystemUI()
    }

    override fun onPause() {
        super.onPause()
        Timber.tag(TAG).e("onPause called")

        // ✅ Show system UI before pausing (when navigating away)
        showSystemUI()

        if (_binding != null && binding.videoView.isPlaying) {
            binding.videoView.pause()
            Timber.tag(TAG).e("Video paused")
        }
    }

    override fun onStop() {
        super.onStop()
        Timber.tag(TAG).e("onStop called")

        // ✅ Ensure status bar is visible when stopped
        showSystemUI()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        videoWatchdogJob?.cancel()
        videoWatchdogJob = null

        // ✅ Restore system UI when destroying view
        showSystemUI()

        binding.videoView.suspend()
        _binding = null

        Timber.tag(TAG).e("onDestroyView completed")
    }
}