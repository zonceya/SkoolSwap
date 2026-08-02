package za.co.skoolswap.ui.login

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import za.co.skoolswap.R
import za.co.skoolswap.common.constants.AppConstants
import za.co.skoolswap.common.constants.AppConstants.LogTags
import za.co.skoolswap.common.constants.ErrorConstants
import za.co.skoolswap.databinding.FragmentLoginBinding
import za.co.skoolswap.data.local.datastore.AppPreferences
import za.co.skoolswap.domain.repository.AuthRepositoryInterface
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginViewModel by viewModels()

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var authRepository: AuthRepositoryInterface

    private var isSigningIn = false
    private var isGoogleSignIn = false

    companion object {
        private const val GOOGLE_SIGN_IN_TIMEOUT_MS = 15000L
        private const val EMAIL_SIGN_IN_TIMEOUT_MS = 10000L
        private const val FADE_ANIMATION_DURATION_MS = 300L
        private const val NAVIGATION_DELAY_MS = 600L
    }

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

        val fromIntro = arguments?.getBoolean("from_intro", false) ?: false
        if (!fromIntro) {
            checkExistingSession()
        }

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        if (_binding == null) return

        setBoxStrokeColor(binding.emailLayout, ContextCompat.getColor(requireContext(), R.color.white))
        setBoxStrokeColor(binding.passwordLayout, ContextCompat.getColor(requireContext(), R.color.white))

        // Google Sign-In
        binding.signInButton.setOnClickListener {
            if (isSigningIn) return@setOnClickListener

            isSigningIn = true
            isGoogleSignIn = true
            binding.signInButton.isEnabled = false

            // Show loading overlay immediately
            showLoadingOverlay(true, "Signing in with Google...")

            viewModel.signInWithGoogle(requireActivity())

            binding.signInButton.postDelayed({
                if (isSigningIn) {
                    isSigningIn = false
                    isGoogleSignIn = false
                    if (_binding != null) {
                        binding.signInButton.isEnabled = true
                    }
                }
            }, GOOGLE_SIGN_IN_TIMEOUT_MS)
        }

        // Email/Password Login
        binding.loginButton.setOnClickListener {
            if (isSigningIn) return@setOnClickListener

            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString()

            if (email.isEmpty()) {
                binding.emailLayout.error = "Email is required"
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.emailLayout.error = "Invalid email format"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                binding.passwordLayout.error = "Password is required"
                return@setOnClickListener
            }

            binding.emailLayout.error = null
            binding.passwordLayout.error = null

            isSigningIn = true
            binding.loginButton.isEnabled = false

            showLoadingOverlay(true, "Signing in...")

            viewModel.signInWithEmail(email, password)

            binding.loginButton.postDelayed({
                if (isSigningIn) {
                    isSigningIn = false
                    if (_binding != null) {
                        binding.loginButton.isEnabled = true
                    }
                }
            }, EMAIL_SIGN_IN_TIMEOUT_MS)
        }

        binding.textForgotPassword?.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_forgotPasswordFragment)
        }

        binding.textSignUpLink.setOnClickListener {
            if (_binding != null) {
                findNavController().navigate(R.id.action_loginFragment_to_signUpFragment)
            }
        }
    }

    private fun showLoadingOverlay(show: Boolean, message: String? = null) {
        if (_binding == null) return

        val overlay = binding.loadingOverlay

        message?.let {
            overlay.findViewById<TextView>(R.id.loadingMessage)?.text = it
        }

        if (show) {
            // 🔥 KEY FIX: Bring to front
            overlay.bringToFront()

            overlay.visibility = View.VISIBLE
            overlay.alpha = 0f
            overlay.animate()
                .alpha(1f)
                .setDuration(300)
                .start()
        } else {
            overlay.animate()
                .alpha(0f)
                .setDuration(300)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        overlay.visibility = View.GONE
                    }
                })
                .start()
        }
    }

    private fun setBoxStrokeColor(textInputLayout: TextInputLayout, color: Int) {
        try {
            val states = arrayOf(
                intArrayOf(android.R.attr.state_focused),
                intArrayOf(android.R.attr.state_hovered),
                intArrayOf(-android.R.attr.state_enabled),
                intArrayOf()
            )
            val colors = intArrayOf(color, color, color, color)
            val colorStateList = ColorStateList(states, colors)

            textInputLayout.setBoxStrokeColorStateList(colorStateList)
            textInputLayout.hintTextColor = colorStateList
            textInputLayout.defaultHintTextColor = colorStateList
        } catch (e: Exception) {
            Timber.tag(LogTags.UI).e(e, "Error setting box stroke color")
        }
    }

    private fun observeViewModel() {
        // For email/password loading
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (_binding == null) return@observe

            if (!isGoogleSignIn) {
                if (isLoading) {
                    binding.loginButton.isEnabled = false
                } else {
                    binding.loginButton.isEnabled = !isSigningIn
                }
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null && _binding != null) {
                showLoadingOverlay(false)
                binding.progressBar.visibility = View.GONE

                // Use ErrorConstantsHelper for user-friendly message
                val userMessage = when {
                    error.contains("530") -> ErrorConstants.Messages.UserFriendly.SESSION_EXPIRED
                    error.contains("500") || error.contains("503") -> ErrorConstants.Messages.UserFriendly.SERVER_DOWN
                    error.contains("timeout") || error.contains("timed out") -> ErrorConstants.Messages.UserFriendly.TIMEOUT
                    error.contains("network") || error.contains("internet") -> ErrorConstants.Messages.UserFriendly.NO_INTERNET
                    else -> error
                }

                Snackbar.make(
                    binding.root,
                    userMessage,
                    Snackbar.LENGTH_LONG
                ).show()

                isSigningIn = false
                isGoogleSignIn = false
                binding.signInButton.isEnabled = true
                binding.loginButton.isEnabled = true
            }
        }

        viewModel.loginSuccess.observe(viewLifecycleOwner) { user ->
            if (user != null && _binding != null) {
                showLoadingOverlay(false)

                binding.loadingOverlay.postDelayed({
                    navigateAfterLogin(user)
                }, NAVIGATION_DELAY_MS)
            }
        }
    }

    private fun checkExistingSession() {
        binding.root.visibility = View.VISIBLE
    }

    private fun navigateAfterLogin(user: za.co.skoolswap.domain.model.User) {
        if (!isAdded || isDetached) return
        try {
            if (user.schoolMapped) {
                val bundle = Bundle().apply {
                    user.schoolId?.let { putInt(AppConstants.ARG_SCHOOL_ID, it) }
                }
                findNavController().navigate(R.id.action_loginFragment_to_nav_home, bundle)
            } else {
                findNavController().navigate(R.id.action_loginFragment_to_schoolOnboardingFragment)
            }
        } catch (e: Exception) {
            Timber.tag(LogTags.UI).e(e, "Navigation failed")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        showLoadingOverlay(false)
        _binding = null
    }
}