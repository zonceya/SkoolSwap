package com.example.skoolswap.ui.login

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.skoolswap.R
import com.example.skoolswap.databinding.FragmentLoginBinding
import com.example.skoolswap.ui.main.MainActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels {
        LoginViewModelFactory(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupObservers()
        viewModel.checkCurrentUser()
    }

    private fun setupUI() {
        // Fixed Google Sign-In button reference
        binding.signInButton.setOnClickListener {
            viewModel.clearError()
            viewModel.signInWithGoogle()
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentUser.collectLatest { user ->
                user?.let {
                    viewModel.clearError()
                    Snackbar.make(binding.root, "Login successful!", Snackbar.LENGTH_SHORT).show()
                    navigateToHome()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loading.collectLatest { isLoading ->
                binding.loadingIndicator.visibility =
                    if (isLoading) View.VISIBLE else View.GONE

                // Fixed button reference
                binding.signInButton.isEnabled = !isLoading
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collectLatest { error ->
                error?.let {
                    Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun navigateToHome() {
        // Use your app's R class, not navigation R class
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