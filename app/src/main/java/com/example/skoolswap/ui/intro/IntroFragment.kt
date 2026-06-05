package com.example.skoolswap.ui.intro


import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.skoolswap.R
import com.example.skoolswap.databinding.FragmentIntroBinding
import com.example.skoolswap.data.local.datastore.AppPreferences
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import androidx.core.net.toUri

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
        requireActivity().window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
        // Show placeholder splash immediately
        binding.splashPlaceholder.visibility = View.VISIBLE

        // Run checks in parallel with video playback
        determineDestination()

        // Start video
        setupVideo()
    }

    // In IntroFragment.kt - modify determineDestination()
    private fun determineDestination() {
        lifecycleScope.launch {
            try {
                val isOnboardingFinished = appPreferences.isOnboardingFinished.first()
                if (!isOnboardingFinished) {
                    onDestinationDetermined(R.id.viewPagerFragment)
                    return@launch
                }

                val isLoggedIn = appPreferences.isLoggedIn.first()
                val authToken = appPreferences.authToken.first()
                val userId = appPreferences.getUserId()
                val isValidSession = isLoggedIn && !authToken.isNullOrEmpty() && userId != null

                if (!isValidSession) {
                    onDestinationDetermined(R.id.loginFragment)
                    return@launch
                }

                // ✅ Treat 401 as expired session → send to login
                val refreshResult = authRepository.refreshUserProfile()
                if (refreshResult.isFailure) {
                    Timber.tag(TAG).e("Token rejected by server (401?) — clearing session")
                    appPreferences.clearUserData()           // ← wipe the stale token
                    onDestinationDetermined(R.id.loginFragment)
                    return@launch
                }

                val user = authRepository.getServerUser().first()
                if (user != null) {
                    val destination = if (user.schoolMapped == true) R.id.nav_home else R.id.nav_profile
                    onDestinationDetermined(destination)
                } else {
                    appPreferences.clearUserData()
                    onDestinationDetermined(R.id.loginFragment)
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error in determineDestination")
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
        videoFinished = true
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
            onVideoFinished()
        }
    }

    private fun navigateToDestination() {
        Timber.tag(TAG).e("navigateToDestination called")
        Timber.tag(TAG).e("isAdded: $isAdded, isDetached: $isDetached")

        if (!isAdded || isDetached) {
            Timber.tag(TAG).e("Fragment not in valid state, skipping navigation")
            return
        }

        val destination = targetDestination ?: R.id.loginFragment
        Timber.tag(TAG).e("Target destination ID: $destination")

        // Map destination ID to readable name for logging
        val destName = when (destination) {
            R.id.loginFragment -> "loginFragment"
            R.id.viewPagerFragment -> "viewPagerFragment (onboarding)"
            R.id.nav_home -> "nav_home"
            R.id.nav_profile -> "nav_profile"
            else -> "unknown"
        }
        Timber.tag(TAG).e("Will navigate to: $destName")

        try {
            when (destination) {
                R.id.loginFragment -> {
                    Timber.tag(TAG).e("Navigating to loginFragment")
                    // Create bundle with from_intro flag
                    val bundle = Bundle().apply {
                        putBoolean("from_intro", true)
                    }
                    findNavController().navigate(R.id.action_introFragment_to_loginFragment, bundle)
                }
                R.id.viewPagerFragment -> {
                    Timber.tag(TAG).e("Navigating to onboarding")
                    findNavController().navigate(R.id.action_introFragment_to_onboarding)
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

    override fun onResume() {
        super.onResume()
        Timber.tag(TAG).e("onResume called")
    }

    override fun onPause() {
        super.onPause()
        Timber.tag(TAG).e("onPause called")
        if (binding.videoView.isPlaying) {
            binding.videoView.pause()
            Timber.tag(TAG).e("Video paused")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Restore system UI
        @Suppress("DEPRECATION")
        requireActivity().window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE

        binding.videoView.suspend()
        _binding = null
    }
}