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
import com.example.skoolswap.common.constants.ErrorConstants
import com.example.skoolswap.databinding.FragmentLoginBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginViewModel by viewModels()

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
        requireActivity().setTheme(R.style.Theme_SkoolSwap_Login);
        setupUI()
        setupObservers()
    }

    private fun setupUI() {
        binding.signInButton.setOnClickListener {
            viewModel.clearError()
            viewModel.signInWithGoogle(requireActivity())
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.user.collectLatest { user ->
                user?.let {
                    navigateToHome()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.loadingIndicator.visibility =
                    if (isLoading) View.VISIBLE else View.GONE
                binding.signInButton.isEnabled = !isLoading
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collectLatest { error ->
                error?.let { errorMessage ->
                    // Log the actual error for debugging
                    Log.e("LoginFragment", "Auth Error: $errorMessage")

                    val userFriendlyMessage = getFriendlyErrorMessage(errorMessage)

                    // Create a longer-lasting Snackbar for important errors
                    val duration = if (errorMessage.contains("network", ignoreCase = true) ||
                        errorMessage.contains("internet", ignoreCase = true)) {
                        Snackbar.LENGTH_INDEFINITE
                    } else {
                        Snackbar.LENGTH_LONG
                    }

                    val snackbar = Snackbar.make(binding.root, userFriendlyMessage, duration)

                    // Add action for network errors
                    if (errorMessage.contains("network", ignoreCase = true) ||
                        errorMessage.contains("internet", ignoreCase = true)) {
                        snackbar.setAction("RETRY") {
                            viewModel.signInWithGoogle(requireActivity())
                        }
                    }

                    snackbar.show()
                    viewModel.clearError()
                }
            }
        }
    }

    private fun getFriendlyErrorMessage(errorMessage: String): String {
        return when {
            // Network-related errors
            errorMessage.contains(ErrorConstants.Auth.NO_INTERNET, ignoreCase = true) -> {
                "${ErrorConstants.UserFriendly.NO_INTERNET_TITLE}\n${ErrorConstants.UserFriendly.NO_INTERNET_MESSAGE}"
            }
            errorMessage.contains(ErrorConstants.Auth.UNSTABLE_CONNECTION, ignoreCase = true) -> {
                "${ErrorConstants.UserFriendly.UNSTABLE_CONNECTION_TITLE}\n${ErrorConstants.UserFriendly.UNSTABLE_CONNECTION_MESSAGE}"
            }
            errorMessage.contains(ErrorConstants.Network.CONNECTION_UNSTABLE, ignoreCase = true) -> {
                "${ErrorConstants.UserFriendly.UNSTABLE_CONNECTION_TITLE}\n${ErrorConstants.UserFriendly.UNSTABLE_CONNECTION_MESSAGE}"
            }

            // Google account errors
            errorMessage.contains(ErrorConstants.Auth.NO_GOOGLE_ACCOUNTS, ignoreCase = true) -> {
                "${ErrorConstants.UserFriendly.NO_GOOGLE_ACCOUNTS_TITLE}\n${ErrorConstants.UserFriendly.NO_GOOGLE_ACCOUNTS_MESSAGE}"
            }
            errorMessage.contains("Google services", ignoreCase = true) -> {
                "${ErrorConstants.UserFriendly.GOOGLE_SERVICES_DOWN_TITLE}\n${ErrorConstants.UserFriendly.GOOGLE_SERVICES_DOWN_MESSAGE}"
            }

            // Cancellation errors
            errorMessage.contains(ErrorConstants.Auth.SIGN_IN_CANCELLED, ignoreCase = true) -> {
                "${ErrorConstants.UserFriendly.SIGN_IN_CANCELLED_TITLE}\n${ErrorConstants.UserFriendly.SIGN_IN_CANCELLED_MESSAGE}"
            }

            // Server errors
            errorMessage.contains("server", ignoreCase = true) -> {
                "🔧 Server Error\nOur servers are having issues. Please try again in a few moments."
            }

            // Generic errors
            else -> "❌ Error\n$errorMessage"
        }
    }

    private fun navigateToHome() {
        findNavController().navigate(R.id.nav_home)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        viewModel.clearError()
    }
}