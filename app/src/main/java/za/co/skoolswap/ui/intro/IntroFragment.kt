package za.co.skoolswap.ui.intro

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
import za.co.skoolswap.R
import za.co.skoolswap.databinding.FragmentIntroBinding
import za.co.skoolswap.data.local.datastore.AppPreferences
import za.co.skoolswap.domain.repository.AuthRepositoryInterface
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

        // Show placeholder splash immediately
        binding.splashPlaceholder.visibility = View.VISIBLE

        // Run checks in parallel with video playback
        determineDestination()

        // Start video
        setupVideo()
        startVideoWatchdog()
    }

    // ==================== DESTINATION LOGIC ====================
    private fun determineDestination() {
        lifecycleScope.launch {
            try {
                // ✅ Use sync getter for logged in state
                val isLoggedIn = appPreferences.getLoggedInState()
                val roomUser = authRepository.getRoomUser()

                Timber.tag(TAG).d("🔍 Session check: loggedIn=$isLoggedIn, user=${roomUser?.name}, schoolMapped=${roomUser?.schoolMapped}")

                // NOT logged in
                if (!isLoggedIn) {
                    Timber.tag(TAG).i("❌ Not logged in → login")
                    onDestinationDetermined(R.id.loginFragment)
                    return@launch
                }

                // Logged in - check if user data exists
                if (roomUser != null && !roomUser.token.isNullOrEmpty()) {
                    // ✅ SCHOOL MAPPED → HOME
                    if (roomUser.schoolMapped) {
                        Timber.tag(TAG).i("🏠 School mapped → HOME")
                        onDestinationDetermined(R.id.nav_home)
                        return@launch
                    }

                    // ✅ SCHOOL NOT MAPPED → Onboarding
                    Timber.tag(TAG).i("🏫 School NOT mapped → school onboarding")
                    onDestinationDetermined(R.id.schoolOnboardingProvinceFragment)
                    return@launch
                }

                // Try restore
                val restored = authRepository.restoreSession()
                if (restored) {
                    val updatedUser = authRepository.getRoomUser()
                    if (updatedUser?.schoolMapped == true) {
                        onDestinationDetermined(R.id.nav_home)
                    } else {
                        onDestinationDetermined(R.id.schoolOnboardingProvinceFragment)
                    }
                    return@launch
                }

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
            return
        }
        videoFinished = true
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
            videoStartTime = SystemClock.elapsedRealtime()

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

                binding.splashPlaceholder.visibility = View.GONE
                binding.progressBar.visibility = View.GONE

                showSystemUI()

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

            binding.progressBar.visibility = View.VISIBLE
            Timber.tag(TAG).e("Progress bar shown, waiting for video to prepare")

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Exception in setupVideo: ${e.message}")
            binding.splashPlaceholder.visibility = View.GONE
            binding.progressBar.visibility = View.GONE
            showSystemUI()
            onVideoFinished()
        }
    }

    // ==================== SYSTEM UI CONTROLS ====================

    private fun showSystemUI() {
        try {
            requireActivity().window.apply {
                statusBarColor = android.graphics.Color.BLACK
                navigationBarColor = android.graphics.Color.BLACK
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                requireActivity().window.insetsController?.let { controller ->
                    controller.show(android.view.WindowInsets.Type.statusBars())
                    controller.show(android.view.WindowInsets.Type.navigationBars())
                    controller.systemBarsBehavior =
                        android.view.WindowInsetsController.BEHAVIOR_DEFAULT

                    controller.setSystemBarsAppearance(
                        0,
                        android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                                android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                    )
                }
            } else {
                @Suppress("DEPRECATION")
                requireActivity().window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_VISIBLE
            }
            Timber.tag(TAG).e("✅ System UI shown")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error showing system UI")
        }
    }

    // ==================== NAVIGATION ====================

    private fun navigateToDestination() {
        Timber.tag(TAG).e("navigateToDestination called")

        if (!isAdded || isDetached) {
            Timber.tag(TAG).e("Fragment not in valid state, skipping navigation")
            return
        }

        showSystemUI()

        val destination = targetDestination ?: R.id.loginFragment
        Timber.tag(TAG).e("Target destination ID: $destination")

        val destName = when (destination) {
            R.id.loginFragment -> "loginFragment"
            R.id.viewPagerFragment -> "viewPagerFragment (onboarding)"
            R.id.schoolOnboardingProvinceFragment -> "schoolOnboardingProvinceFragment"  // ✅ Updated
            R.id.schoolOnboardingSchoolFragment -> "schoolOnboardingSchoolFragment"      // ✅ Updated
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
                R.id.schoolOnboardingProvinceFragment -> {  // ✅ Updated
                    Timber.tag(TAG).e("🔴 Navigating to province selection")
                    findNavController().navigate(R.id.action_introFragment_to_schoolOnboardingProvinceFragment)
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
    }

    override fun onPause() {
        super.onPause()
        Timber.tag(TAG).e("onPause called")

        if (_binding != null && binding.videoView.isPlaying) {
            binding.videoView.pause()
            Timber.tag(TAG).e("Video paused")
        }
    }

    override fun onStop() {
        super.onStop()
        Timber.tag(TAG).e("onStop called")
    }

    override fun onDestroyView() {
        super.onDestroyView()

        videoWatchdogJob?.cancel()
        videoWatchdogJob = null

        binding.videoView.suspend()
        _binding = null

        Timber.tag(TAG).e("onDestroyView completed")
    }
}