package com.example.skoolswap.ui.login

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.skoolswap.R
import com.example.skoolswap.databinding.FragmentLoginBinding
import com.example.skoolswap.data.local.datastore.AppPreferences
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
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

        Timber.tag("LoginFragment").e("🔥 onViewCreated")

        // Check if we came from IntroFragment
        val fromIntro = arguments?.getBoolean("from_intro", false) ?: false

        if (fromIntro) {
            Timber.tag("LoginFragment").d("Coming from IntroFragment - session already validated")
            binding.root.visibility = View.VISIBLE
        } else {
            Timber.tag("LoginFragment").d("Direct navigation - checking session")
            checkExistingSession()
        }

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        if (_binding == null) return

        setBoxStrokeColor(binding.emailLayout, ContextCompat.getColor(requireContext(), R.color.white))
        setBoxStrokeColor(binding.passwordLayout, ContextCompat.getColor(requireContext(), R.color.white))

        // Google Sign-In Button
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

        // Email/Password Login Button
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

            // Call Firebase email sign in
            viewModel.signInWithEmail(email, password)

            binding.loginButton.postDelayed({
                if (isSigningIn) {
                    isSigningIn = false
                    if (_binding != null) {
                        binding.loginButton.isEnabled = true
                    }
                }
            }, 10000)
        }

        // Forgot Password Link - Add this TextView to your login XML
        binding.textForgotPassword?.setOnClickListener {
            navigateToForgotPassword()
        }

        binding.textSignUpLink.setOnClickListener {
            if (_binding != null) {
                findNavController().navigate(R.id.action_loginFragment_to_signUpFragment)
            }
        }
    }

    private fun navigateToForgotPassword() {
        findNavController().navigate(R.id.action_loginFragment_to_forgotPasswordFragment)
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
            Timber.tag("LoginFragment").e("Error setting box stroke color: ${e.message}")
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
                binding.loginButton.isEnabled = !isSigningIn
                binding.progressBar.visibility = View.GONE
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null && _binding != null) {
                Snackbar.make(
                    binding.root,
                    error,
                    Snackbar.LENGTH_LONG
                ).show()

                isSigningIn = false
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
    }

    private fun checkExistingSession() {
        Timber.tag("LoginFragment").d("LoginFragment - no session restore, just showing UI")
        binding.root.visibility = View.VISIBLE
    }

    private fun navigateAfterLogin(user: com.example.skoolswap.domain.model.User) {
        if (!isAdded || isDetached) return
        try {
            if (user.schoolMapped) {
                val bundle = Bundle().apply {
                    user.schoolId?.let { putInt("schoolId", it) }
                }
                findNavController().navigate(R.id.action_loginFragment_to_nav_home, bundle)
            } else {
                findNavController().navigate(R.id.action_loginFragment_to_schoolOnboardingFragment)
            }
        } catch (e: Exception) {
            Timber.tag("LoginFragment").e("Navigation failed: ${e.message}")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}