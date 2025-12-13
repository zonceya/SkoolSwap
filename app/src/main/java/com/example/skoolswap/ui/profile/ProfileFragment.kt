package com.example.skoolswap.ui.profile

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.skoolswap.R
import com.example.skoolswap.databinding.FragmentProfileBinding
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
import com.example.skoolswap.utils.extensions.MobileValidator
import com.google.android.material.floatingactionbutton.FloatingActionButton
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
        hideFab()
        setupUI()
        setupObservers()
        loadUserData()
    }

    private fun setupUI() {
        binding.contactNumber.apply {
            inputType = InputType.TYPE_CLASS_PHONE

            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    selectAll()
                }
            }

            setOnClickListener {
                requestFocus()
            }

            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    validateMobileNumber(s.toString())
                }
            })
        }

        // ADD THIS: Setup submit button click listener
        binding.submitButton.setOnClickListener {
            submitMobileNumber()
        }
        binding.deleteButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                if (isLoading) {
                    binding.profileProgressLayout.visibility = View.VISIBLE
                    binding.submitButton.isEnabled = false
                    binding.contactNumber.isEnabled = false
                } else {
                    binding.profileProgressLayout.visibility = View.GONE
                    binding.submitButton.isEnabled = true
                    binding.contactNumber.isEnabled = true
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collectLatest { error ->
                error?.let {
                    Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.updateSuccess.collectLatest { success ->
                if (success) {
                    Snackbar.make(binding.root, "Mobile number updated successfully!", Snackbar.LENGTH_SHORT).show()
                    viewModel.clearSuccess()

                    // Hide keyboard after successful update
                    hideKeyboard()

                    // Clear focus from EditText
                    binding.contactNumber.clearFocus()
                }
            }
        }
    }
    private  fun hideFab() {
    val fab = activity?.findViewById<FloatingActionButton>(R.id.fab)
    // Hide the FAB
    fab?.visibility = View.GONE
}
    private fun loadUserData() {
        viewLifecycleOwner.lifecycleScope.launch {
            authRepository.getServerUser().collect { user ->
                user?.let {
                    binding.profileName.setText(it.name)

                    val displayMobile = it.mobile?.let { mobile ->
                        if (MobileValidator.formatToLocal(mobile) != mobile) {
                            MobileValidator.formatToLocal(mobile)
                        } else {
                            mobile
                        }
                    } ?: ""

                    binding.contactNumber.setText(displayMobile)
                    loadProfilePicture(it.profilePictureUrl)
                }
            }
        }
    }

    private fun validateMobileNumber(mobile: String): Boolean {
        val error = MobileValidator.getErrorMessage(mobile)
        binding.contactNumber.error = error
        binding.submitButton.isEnabled = error == null
        return error == null
    }

    private fun submitMobileNumber() {
        val mobile = binding.contactNumber.text.toString().trim()

        if (mobile.isEmpty()) {
            Snackbar.make(binding.root, "Please enter mobile number", Snackbar.LENGTH_SHORT).show()
            return
        }

        if (!validateMobileNumber(mobile)) {
            return
        }

        viewModel.updateMobile(mobile)
    }

    private fun loadProfilePicture(url: String) {
        try {
            Glide.with(requireContext())
                .load(url)
                .placeholder(R.drawable.ic_user)
                .error(R.drawable.ic_user)
                .circleCrop()
                .timeout(10000)
                .into(binding.profileImage)
        } catch (e: Exception) {
            // Handle error silently
        }
    }
    private fun showDeleteConfirmationDialog() {
        val dialog = DeleteProfileDialog().apply {
            onConfirm = {
                // User confirmed deletion
                deleteProfile()
            }
            onCancel = {
                // User cancelled
                Snackbar.make(binding.root, "Account deletion cancelled", Snackbar.LENGTH_SHORT).show()
            }
        }

        dialog.show(parentFragmentManager, "delete_profile_dialog")
    }
    private fun deleteProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                if (isLoading) {
                    binding.profileProgressLayout.visibility = View.VISIBLE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val result = authRepository.deleteProfile()

            if (result.isSuccess) {
                Snackbar.make(
                    binding.root,
                    "Account deleted successfully. You have been signed out.",
                    Snackbar.LENGTH_LONG
                ).show()

                // Navigate to login screen
                findNavController().navigate(R.id.loginFragment)
            } else {
                Snackbar.make(
                    binding.root,
                    "Failed to delete account: ${result.exceptionOrNull()?.message}",
                    Snackbar.LENGTH_LONG
                ).show()
            }

            binding.profileProgressLayout.visibility = View.GONE
        }
    }
    private fun hideKeyboard() {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(binding.contactNumber.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}