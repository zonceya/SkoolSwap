package com.example.skoolswap.ui.profile

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.skoolswap.R
import com.example.skoolswap.databinding.FragmentProfileBinding
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

    @Inject
    lateinit var authRepository: AuthRepositoryInterface

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupObservers()
        loadUserData()
    }

    private fun setupUI() {
        // Make contact number editable
        binding.contactNumber.apply {
            isClickable = true
            isFocusable = true
            isCursorVisible = true
            isEnabled = true // Enable editing

            setOnClickListener {
                // Optional: Show keyboard
                showKeyboard()
            }
        }

        binding.submitButton.setOnClickListener {
            val mobile = binding.contactNumber.text.toString().trim()
            if (mobile.isNotEmpty()) {
                viewModel.updateMobile(mobile)
            } else {
                Snackbar.make(binding.root, "Please enter a mobile number", Snackbar.LENGTH_SHORT).show()
            }
        }

        // Handle delete profile
        binding.deleteProfile.setOnClickListener {
            // Handle delete profile logic
        }

        // Handle sign out
        binding.signOutButton.setOnClickListener {
            // Handle sign out logic
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.profileProgressLayout.visibility =
                    if (isLoading) View.VISIBLE else View.GONE
                binding.submitButton.isEnabled = !isLoading
                binding.contactNumber.isEnabled = !isLoading
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.error.collectLatest { error ->
                error?.let {
                    Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.updateSuccess.collectLatest { success ->
                if (success) {
                    Snackbar.make(binding.root, "Mobile number updated successfully!", Snackbar.LENGTH_SHORT).show()
                    viewModel.clearSuccess()
                }
            }
        }
    }

    private fun loadUserData() {
        viewLifecycleOwner.lifecycleScope.launch {
            authRepository.getServerUser().collect { user ->
                user?.let {
                    // FIX: Use setText() instead of .text assignment
                    binding.profileName.setText(it.name)
                    binding.contactNumber.setText(it.mobile ?: "")

                    // Load profile picture
                    loadProfilePicture(it.profilePictureUrl)
                }
            }
        }
    }

    private fun loadProfilePicture(url: String) {
        try {
            Glide.with(requireContext())
                .load(url)
                .placeholder(R.drawable.ic_user)
                .error(R.drawable.ic_user)
                .circleCrop()
                .into(binding.profileImage)
        } catch (e: Exception) {
            // Handle error
        }
    }

    private fun showKeyboard() {
        binding.contactNumber.requestFocus()
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(binding.contactNumber, InputMethodManager.SHOW_IMPLICIT)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}