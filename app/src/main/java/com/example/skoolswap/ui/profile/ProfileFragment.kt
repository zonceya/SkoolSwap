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
import android.widget.Toast
import androidx.core.content.ContextCompat
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
    private var isSettingTextProgrammatically = false
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

        (binding.provinceSpinner as? AutoCompleteTextView)?.apply {
            setAdapter(provinceAdapter)
            threshold = 1  // Show dropdown after 1 character
        }

        binding.provinceSpinner.setOnItemClickListener { _, _, position, _ ->
            val province = viewModel.provinces.value[position]
            viewModel.selectProvince(province)

            // Immediately show selected province
            binding.provinceSpinner.setText(province.name, false)

            // Clear any existing school when province changes
            binding.selectedSchoolText.visibility = View.GONE
            binding.schoolSearch.text?.clear()

            // Clear error when province is selected
            binding.provinceTextInputLayout.error = null

            binding.schoolSearch.isEnabled = true
            binding.schoolSearchLayout.hint = "Search schools in ${province.name}"
            binding.schoolSearchLayout.placeholderText = "Type at least 2 characters"
        }
    }

    private fun setupSchoolSearch() {
        binding.schoolSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Don't search if we're setting text programmatically
                if (isSettingTextProgrammatically) {
                    isSettingTextProgrammatically = false
                    return
                }

                // Clear error when user starts typing
                binding.schoolSearchLayout.error = null

                searchDebounceJob?.cancel()

                val query = s?.toString()?.trim() ?: ""

                if (query.length >= 2) {
                    searchDebounceJob = lifecycleScope.launch {
                        delay(500)
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

    private fun setupObservers() {
        // ========== PROVINCE OBSERVER ==========
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.provinces.collectLatest { provinces ->
                Log.d("ProfileFragment", "📋 Provinces loaded: ${provinces.size}")

                // Create and set adapter
                val provinceNames = provinces.map { it.name }
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    provinceNames
                )

                (binding.provinceSpinner as? AutoCompleteTextView)?.apply {
                    setAdapter(adapter)
                    Log.d("ProfileFragment", "✅ Adapter set with ${provinces.size} items")

                    // Try to set province from ViewModel first
                    var provinceSet = false

                    viewModel.selectedProvince.value?.let { province ->
                        Log.d("ProfileFragment", "🔄 Setting spinner to selected province: ${province.name}")
                        setText(province.name, false)
                        provinceSet = true
                    }

                    // If no selected province but we have a school with provinceId, try to find it
                    if (!provinceSet) {
                        viewModel.selectedSchool.value?.let { school ->
                            if (school.provinceId != null) {
                                val matchingProvince = provinces.find { it.id == school.provinceId }
                                matchingProvince?.let {
                                    Log.d("ProfileFragment", "🔄 Setting spinner from school provinceId: ${it.name}")
                                    setText(it.name, false)

                                    // Also update the ViewModel so it's saved for next time
                                    viewModel.selectProvince(it)
                                }
                            }
                        }
                    }
                }
            }
        }
        // ========== SELECTED PROVINCE OBSERVER ==========
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedProvince.collectLatest { province ->
                province?.let {
                    Log.d("ProfileFragment", "📍 Selected province changed to: ${it.name}")

                    // Force the spinner to show the province name
                    binding.provinceSpinner.setText(it.name, false)

                    // Enable school search
                    binding.schoolSearch.isEnabled = true
                    binding.schoolSearchLayout.hint = "Search schools in ${it.name}"
                    binding.schoolSearchLayout.placeholderText = "Type at least 2 characters"

                    // Clear any province error
                    binding.provinceTextInputLayout.error = null
                }
            }
        }

        // ========== SCHOOL SEARCH RESULTS OBSERVER ==========
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

        // ========== SELECTED SCHOOL OBSERVER ==========
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedSchool.collectLatest { school ->
                if (school != null) {
                    // Show in the selected text view
                    binding.selectedSchoolText.text = "Selected: ${school.name}"
                    binding.selectedSchoolText.visibility = View.VISIBLE
                    binding.selectedSchoolText.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))

                    // Set text programmatically without triggering search
                    isSettingTextProgrammatically = true
                    binding.schoolSearch.setText(school.name)

                    // Clear any search results when school is selected
                    binding.schoolResultsRecyclerView.visibility = View.GONE

                    // Enable submit button
                    binding.submitButton.isEnabled = true
                    binding.submitButton.alpha = 1.0f

                    // Clear any school error
                    binding.schoolSearchLayout.error = null

                    Log.d("ProfileFragment", "🏫 Selected school: ${school.name}")

                    // If provinces are already loaded, try to set the province
                    if (viewModel.provinces.value.isNotEmpty() && school.provinceId != null) {
                        val matchingProvince = viewModel.provinces.value.find { it.id == school.provinceId }
                        matchingProvince?.let {
                            Log.d("ProfileFragment", "🔄 Setting province from school: ${it.name}")
                            viewModel.selectProvince(it)
                        }
                    }
                } else {
                    binding.selectedSchoolText.visibility = View.GONE
                    binding.schoolSearch.text?.clear()
                    binding.submitButton.isEnabled = false
                    binding.submitButton.alpha = 0.5f
                }
            }
        }
        // ========== SEARCH ACTIVE STATE OBSERVER ==========
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isSearchActive.collectLatest { isActive ->
                if (isActive) {
                    binding.schoolResultsRecyclerView.visibility = View.VISIBLE
                }
            }
        }

        // ========== LOADING STATE OBSERVER ==========
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.profileProgressLayout.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.submitButton.isEnabled = !isLoading && viewModel.selectedSchool.value != null
            }
        }

        // ========== SEARCHING INDICATOR OBSERVER ==========
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isSearching.collectLatest { isSearching ->
                // Optional: Show a progress bar in search field
            }
        }

        // ========== ERROR HANDLING OBSERVER ==========
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collectLatest { error ->
                error?.let {
                    Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }

        // ========== MOBILE UPDATE SUCCESS OBSERVER ==========
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.updateSuccess.collectLatest { success ->
                if (success) {
                    Snackbar.make(binding.root, "Mobile number updated!", Snackbar.LENGTH_SHORT).show()
                    viewModel.clearSuccess()
                }
            }
        }

        // ========== HAS EXISTING SCHOOL OBSERVER (SHOWS DELETE SECTION) ==========
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.hasExistingSchool.collectLatest { hasExisting ->
                Log.d("ProfileFragment", "🔥 hasExistingSchool: $hasExisting")
                if (hasExisting) {
                    binding.submitButton.text = "Update Profile"
                    binding.deleteWarningTitle.visibility = View.VISIBLE
                    binding.deleteWarningText.visibility = View.VISIBLE
                    binding.deleteButton.visibility = View.VISIBLE
                } else {
                    binding.submitButton.text = "Submit"
                    binding.deleteWarningTitle.visibility = View.GONE
                    binding.deleteWarningText.visibility = View.GONE
                    binding.deleteButton.visibility = View.GONE
                }
            }
        }

        // ========== PROFILE COMPLETE OBSERVER (NAVIGATE TO HOME) ==========
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.profileComplete.collectLatest { isComplete ->
                if (isComplete) {
                    binding.profileProgressLayout.visibility = View.GONE
                    val message = if (viewModel.hasExistingSchool.value) {
                        "School updated successfully!"
                    } else {
                        "Profile completed! Welcome to SkoolSwap!"
                    }
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()

                    // Navigate to home
                    findNavController().navigate(R.id.nav_home)
                    viewModel.resetNavigation()
                }
            }
        }

        // ========== LOADING STATE FOR FIRST LOAD ==========
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                if (isLoading) {
                    // Show loading only for first load
                    if (!viewModel.hasExistingSchool.value && viewModel.selectedSchool.value == null) {
                        binding.profileProgressLayout.visibility = View.VISIBLE
                    }
                } else {
                    binding.profileProgressLayout.visibility = View.GONE
                }
            }
        }

        // ========== CURRENT SCHOOL MAPPING OBSERVER (DEBUGGING) ==========
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentSchoolMapping.collectLatest { mapping ->
                mapping?.let {
                    Log.d("ProfileFragment", "📋 Current mapping: ${it.schoolName} (${it.mappingId})")
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
    private fun validateAndSubmit() {
        val mobile = binding.contactNumber.text.toString().trim()

        // Check if school search field is filled but no school selected
        val searchText = binding.schoolSearch.text.toString().trim()
        if (searchText.isNotEmpty() && viewModel.selectedSchool.value == null) {
            Toast.makeText(
                requireContext(),
                "Please select a school from the search results",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Validate mobile if provided (but don't auto-update)
        if (mobile.isNotEmpty()) {
            if (!validateMobileNumber(mobile)) {
                binding.contactNumber.requestFocus()
                return
            }
            // We'll update mobile after school is saved
        }

        // Check if school is selected
        if (viewModel.selectedSchool.value == null) {
            Toast.makeText(
                requireContext(),
                "Please select a school",
                Toast.LENGTH_LONG
            ).show()
            binding.schoolSearch.requestFocus()
            return
        }

        // Show loading
        binding.profileProgressLayout.visibility = View.VISIBLE

        // Update mobile if provided
        if (mobile.isNotEmpty()) {
            viewModel.updateMobile(mobile)
        }

        // Submit school selection
        viewModel.submitSchoolSelection()
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
    private fun setupSchoolResults() {
        schoolAdapter = SchoolAdapter { school ->
            // Just select the school, don't save yet
            viewModel.selectSchool(school)
            binding.selectedSchoolText.text = "Selected: ${school.name}"
            binding.selectedSchoolText.visibility = View.VISIBLE
            binding.selectedSchoolText.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
            binding.schoolResultsRecyclerView.visibility = View.GONE
            binding.schoolSearch.text?.clear()
            hideKeyboard()

            // Enable submit button
            binding.submitButton.isEnabled = true

            // Clear any search field errors
            binding.schoolSearchLayout.error = null

            Log.d("ProfileFragment", "✅ School selected: ${school.name}")
        }

        binding.schoolResultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = schoolAdapter
        }
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