// ui/login/LoginFragment.kt
package com.example.skoolswap.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.skoolswap.R
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

        // In LoginFragment.kt, modify the error observer:
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collectLatest { error ->
                error?.let {
                    val message = when {
                        it.contains("No internet connection", ignoreCase = true) -> {
                            "📡 No internet connection. Please check your Wi-Fi or mobile data."
                        }
                        it.contains("Google accounts", ignoreCase = true) -> {
                            "👤 No Google accounts found. Please add a Google account in device Settings."
                        }
                        it.contains("cancelled", ignoreCase = true) -> {
                            "Sign-in was cancelled. Please try again."
                        }
                        else -> it
                    }
                    Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
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
        viewModel.clearError() // Clear any stale errors
    }
}