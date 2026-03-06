// ui/profile/ProfileFragment.kt
package com.example.skoolswap.ui.profile

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.skoolswap.R
import com.example.skoolswap.databinding.FragmentProfileBinding
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
import com.example.skoolswap.utils.extensions.MobileValidator
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
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

    private lateinit var schoolAdapter: SchoolAdapter
    private var searchDebounceJob: kotlinx.coroutines.Job? = null

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
        Log.e("ProfileFragment", "🔥 onViewCreated")
        Thread.dumpStack()
        hideFab()
        setupUI()
        setupObservers()
        loadUserData()
    }

    private fun setupUI() {
        // Mobile number input
        setupMobileInput()

        // Province dropdown
        setupProvinceDropdown()

        // School search
        setupSchoolSearch()

        // School results RecyclerView
        setupSchoolResults()

        // Submit button
        binding.submitButton.setOnClickListener {
            completeProfile()
        }

        // Delete button (hidden for first-time setup)
        binding.deleteButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun setupMobileInput() {
        binding.contactNumber.apply {
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    validateMobileNumber(s.toString())
                }
            })
        }
    }

    private fun setupProvinceDropdown() {
        val provinceAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            mutableListOf<String>()
        )

        (binding.provinceSpinner as? AutoCompleteTextView)?.setAdapter(provinceAdapter)

        binding.provinceSpinner.setOnItemClickListener { _, _, position, _ ->
            val province = viewModel.provinces.value[position]
            viewModel.selectProvince(province)

            // Enable school search
            binding.schoolSearch.isEnabled = true
            binding.schoolSearchLayout.hint = "Search schools in ${province.name}"
            binding.schoolSearchLayout.placeholderText = "Type at least 2 characters"
            binding.schoolSearch.text?.clear()
        }
    }

    private fun setupSchoolSearch() {
        binding.schoolSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchDebounceJob?.cancel()

                val query = s?.toString()?.trim() ?: ""

                if (query.length >= 2) {
                    searchDebounceJob = lifecycleScope.launch {
                        delay(500) // Debounce
                        viewModel.searchSchools(query)
                    }
                } else {
                    viewModel.clearSearch()
                }
            }
        })

        binding.schoolSearch.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && viewModel.selectedProvince.value != null) {
                viewModel.clearSearch()
            }
        }
    }

    private fun setupSchoolResults() {
        schoolAdapter = SchoolAdapter { school ->
            viewModel.selectSchool(school)
            binding.selectedSchoolText.text = "Selected: ${school.name}"
            binding.selectedSchoolText.visibility = View.VISIBLE
            binding.schoolResultsRecyclerView.visibility = View.GONE
            binding.schoolSearch.text?.clear()
            hideKeyboard()
        }

        binding.schoolResultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = schoolAdapter
        }
    }

    private fun setupObservers() {
        // Provinces
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.provinces.collectLatest { provinces ->
                val provinceNames = provinces.map { it.name }
                (binding.provinceSpinner as? AutoCompleteTextView)?.setAdapter(
                    ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, provinceNames)
                )
            }
        }

        // Selected Province
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedProvince.collectLatest { province ->
                province?.let {
                    binding.schoolSearch.isEnabled = true
                    binding.schoolSearchLayout.hint = "Search schools in ${it.name}"
                }
            }
        }

        // School Search Results
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.schools.collectLatest { schools ->
                if (schools.isNotEmpty()) {
                    schoolAdapter.submitList(schools)
                    binding.schoolResultsRecyclerView.visibility = View.VISIBLE
                    binding.noResultsText.visibility = View.GONE
                } else {
                    binding.schoolResultsRecyclerView.visibility = View.GONE
                    if (viewModel.isSearchActive.value) {
                        binding.noResultsText.visibility = View.VISIBLE
                    }
                }
            }
        }

        // Selected School
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedSchool.collectLatest { school ->
                binding.submitButton.isEnabled = school != null
                if (school != null) {
                    binding.selectedSchoolText.text = "Selected: ${school.name}"
                    binding.selectedSchoolText.visibility = View.VISIBLE
                } else {
                    binding.selectedSchoolText.visibility = View.GONE
                }
            }
        }

        // Search Active State
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isSearchActive.collectLatest { isActive ->
                if (isActive) {
                    binding.schoolResultsRecyclerView.visibility = View.VISIBLE
                }
            }
        }

        // Loading States
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.profileProgressLayout.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.submitButton.isEnabled = !isLoading && viewModel.selectedSchool.value != null
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isSearching.collectLatest { isSearching ->
                // Show searching indicator if needed
            }
        }

        // Error Handling
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collectLatest { error ->
                error?.let {
                    Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }

        // Mobile Update Success
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.updateSuccess.collectLatest { success ->
                if (success) {
                    Snackbar.make(binding.root, "Mobile number updated!", Snackbar.LENGTH_SHORT).show()
                    viewModel.clearSuccess()
                }
            }
        }

        // Profile Complete - Navigate to Home
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.profileComplete.collectLatest { isComplete ->
                if (isComplete) {
                    Snackbar.make(binding.root, "Profile completed! Welcome to SkoolSwap!", Snackbar.LENGTH_LONG).show()
                    viewModel.resetNavigation()
                  //  findNavController().navigate(R.id.nav_home)
                }
            }
        }
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
        return error == null
    }

    private fun completeProfile() {
        val mobile = binding.contactNumber.text.toString().trim()

        // Update mobile if provided
        if (mobile.isNotEmpty()) {
            if (!validateMobileNumber(mobile)) {
                return
            }
            viewModel.updateMobile(mobile)
        }

        // Complete profile with school selection
        viewModel.completeProfile()
    }

    private fun loadProfilePicture(url: String?) {
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
                deleteProfile()
            }
            onCancel = {
                Snackbar.make(binding.root, "Account deletion cancelled", Snackbar.LENGTH_SHORT).show()
            }
        }
        dialog.show(parentFragmentManager, "delete_profile_dialog")
    }

    private fun deleteProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = authRepository.deleteProfile()

            if (result.isSuccess) {
                Snackbar.make(
                    binding.root,
                    "Account deleted successfully. You have been signed out.",
                    Snackbar.LENGTH_LONG
                ).show()
              findNavController().navigate(R.id.loginFragment)
            } else {
                Snackbar.make(
                    binding.root,
                    "Failed to delete account: ${result.exceptionOrNull()?.message}",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun hideFab() {
        val fab = activity?.findViewById<FloatingActionButton>(R.id.fab)
        fab?.visibility = View.GONE
    }

    private fun hideKeyboard() {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(binding.contactNumber.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchDebounceJob?.cancel()
        _binding = null
    }
    override fun onResume() {
        super.onResume()
        Log.e("ProfileFragment", "🔥 ProfileFragment onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.e("ProfileFragment", "🔥 ProfileFragment onPause - navigating away?")
    }
    override fun onStop() {
        super.onStop()
        Log.e("ProfileFragment", "🔥 onStop")
    }
}