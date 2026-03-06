package com.example.skoolswap.ui.login

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.skoolswap.R
import com.example.skoolswap.common.constants.ErrorConstants
import com.example.skoolswap.databinding.FragmentLoginBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

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

        Log.e("LoginFragment", "🔥 onViewCreated at ${System.currentTimeMillis()}")

        // Set callback and verify it's set
        setupCallback()

        setupUI()
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
                    findNavController().navigate(R.id.nav_home)  // ← Direct destination
                } else {
                    Log.e("LoginFragment", "🚀 Navigating to PROFILE using ID: ${R.id.nav_profile}")
                    findNavController().navigate(R.id.nav_profile)  // ← Direct destination, NOT action
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
    }

    override fun onPause() {
        super.onPause()
        Log.e("LoginFragment", "🔥 onPause")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.e("LoginFragment", "🔥 onDestroyView - clearing callback reference")

        // Don't clear the callback here! Let the ViewModel handle it
        // viewModel.onLoginSuccess = null // ← DON'T DO THIS

        _binding = null
    }
}