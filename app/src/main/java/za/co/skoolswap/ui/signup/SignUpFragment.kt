package za.co.skoolswap.ui.signup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import za.co.skoolswap.R
import za.co.skoolswap.databinding.FragmentSignUpBinding
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

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
            binding.buttonRegister.isEnabled = !isLoading
            binding.buttonRegister.text = if (isLoading) "Creating account..." else "Sign Up"
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Timber.e("Error: $error")
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
            }
        }

        // ✅ Updated navigation to use Province Fragment first
        viewModel.signUpSuccess.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                Timber.tag("SignUpFragment").e("✅ Sign up successful!")
                Timber.tag("SignUpFragment").e("👤 User: ${user.name}")
                Timber.tag("SignUpFragment").e("🏫 schoolMapped: ${user.schoolMapped}")
                Timber.tag("SignUpFragment").e("🏫 schoolId: ${user.schoolId}")

                if (user.schoolMapped == true && user.schoolId != null) {
                    // ✅ User has school - go to Home
                    Timber.tag("SignUpFragment").e("➡️ User has school - navigating to HOME")
                    val bundle = Bundle().apply {
                        putInt("schoolId", user.schoolId)
                    }
                    findNavController().navigate(R.id.action_signUpFragment_to_nav_home, bundle)
                } else {
                    // ✅ User has NO school - go to Province Selection first
                    Timber.tag("SignUpFragment").e("➡️ User has NO school - navigating to PROVINCE SELECTION")
                    findNavController().navigate(R.id.action_signUpFragment_to_schoolOnboardingProvinceFragment)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}