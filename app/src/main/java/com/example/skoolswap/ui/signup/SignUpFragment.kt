package com.example.skoolswap.ui.signup

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.skoolswap.R
import com.example.skoolswap.databinding.FragmentSignUpBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignUpFragment : Fragment() {

    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SignUpViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.buttonRegister.setOnClickListener {
            val name = binding.editName.text.toString().trim()
            val email = binding.editEmail.text.toString().trim()
            val password = binding.editPassword.text.toString()
            val confirmPassword = binding.editConfirmPassword.text.toString()

            when {
                name.isEmpty() -> {
                    binding.editName.error = "Name is required"
                    return@setOnClickListener
                }
                email.isEmpty() -> {
                    binding.editEmail.error = "Email is required"
                    return@setOnClickListener
                }
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    binding.editEmail.error = "Invalid email format"
                    return@setOnClickListener
                }
                password.isEmpty() -> {
                    binding.editPassword.error = "Password is required"
                    return@setOnClickListener
                }
                password.length < 6 -> {
                    binding.editPassword.error = "Password must be at least 6 characters"
                    return@setOnClickListener
                }
                password != confirmPassword -> {
                    binding.editConfirmPassword.error = "Passwords do not match"
                    return@setOnClickListener
                }
                else -> {
                    // ✅ Call Firebase sign up instead of OTP
                    viewModel.signUpWithEmail(name, email, password, confirmPassword)
                }
            }
        }

        binding.textLoginLink.setOnClickListener {
            findNavController().navigate(R.id.action_signUpFragment_to_loginFragment)
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            Log.e("SignUpFragment", "Loading state: $isLoading")
            binding.buttonRegister.isEnabled = !isLoading
            binding.buttonRegister.text = if (isLoading) "Creating account..." else "Sign Up"
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Log.e("SignUpFragment", "Error: $error")
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
            }
        }

        // ✅ Changed from otpToken to user object
        viewModel.signUpSuccess.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                Log.e("SignUpFragment", "✅ Sign up successful!")
                Log.e("SignUpFragment", "👤 User: ${user.name}")
                Log.e("SignUpFragment", "🏫 schoolMapped: ${user.schoolMapped}")
                Log.e("SignUpFragment", "🏫 schoolId: ${user.schoolId}")

                // Navigate directly - no OTP screen
                if (user.schoolMapped == true && user.schoolId != null) {
                    Log.e("SignUpFragment", "➡️ User has school - navigating to HOME")
                    val bundle = Bundle().apply {
                        putInt("schoolId", user.schoolId)
                    }
                    findNavController().navigate(R.id.action_signUpFragment_to_nav_home, bundle)
                } else {
                    Log.e("SignUpFragment", "➡️ User has NO school - navigating to ONBOARDING")
                    findNavController().navigate(R.id.action_signUpFragment_to_schoolOnboardingFragment)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}