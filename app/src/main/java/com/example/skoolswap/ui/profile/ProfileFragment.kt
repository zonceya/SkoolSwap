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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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
import androidx.activity.OnBackPressedCallback

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
        Log.d("ProfileFragment", "🔥 onViewCreated")
        hideFab()
        setupUI()
        setupObservers()
        loadUserData()
        setupBackButton()
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

    private fun setupBackButton() {
        // Handle system back button
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.hasExistingSchool.value) {
                    // Returning user - go back
                    findNavController().navigateUp()
                } else {
                    // First-time user - block back
                    Toast.makeText(requireContext(), "Please complete your profile setup first", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun setupMobileInput() {
        binding.contactNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val mobile = s.toString().trim()

                // Only validate - NO API calls
                validateMobileNumber(mobile)
                viewModel.previewMobile(mobile) // Just store pending
                showUnsavedIndicator()
            }
        })
    }

    private fun setupProvinceDropdown() {
        val provinceAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            mutableListOf<String>()
        )

        (binding.provinceSpinner as? AutoCompleteTextView)?.apply {
            setAdapter(provinceAdapter)
            threshold = 1
        }

        binding.provinceSpinner.setOnItemClickListener { _, _, position, _ ->
            val province = viewModel.provinces.value[position]

            // Don't clear school if user already has one selected
            val shouldClear = viewModel.selectedSchool.value == null
            viewModel.selectProvince(province, shouldClearSchool = shouldClear)

            // Immediately show selected province
            binding.provinceSpinner.setText(province.name, false)

            // Only clear UI if we're actually clearing the school
            if (shouldClear) {
                binding.selectedSchoolText.visibility = View.GONE
                binding.schoolSearch.text?.clear()
            }

            binding.provinceTextInputLayout.error = null
            binding.schoolSearch.isEnabled = true
            binding.schoolSearchLayout.hint = "Search schools in ${province.name}"
            binding.schoolSearchLayout.placeholderText = "Type at least 2 characters"

            showUnsavedIndicator()
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

                // If user is typing and there was a selected school, clear the selection
                if (viewModel.selectedSchool.value != null) {
                    Log.d("ProfileFragment", "User typing - clearing school selection")
                    viewModel.clearSchoolSelection()
                    binding.selectedSchoolText.visibility = View.GONE
                    showUnsavedIndicator()
                }

                // Clear error when user starts typing
                binding.schoolSearchLayout.error = null

                searchDebounceJob?.cancel()

                val query = s?.toString()?.trim() ?: ""

                if (query.length >= 2 && viewModel.selectedProvince.value != null) {
                    searchDebounceJob = lifecycleScope.launch {
                        delay(500)
                        viewModel.searchSchools(query)
                    }
                } else {
                    viewModel.clearSearch()
                }
            }
        })
    }

    private fun setupSchoolResults() {
        schoolAdapter = SchoolAdapter { school ->
            // Just preview - don't save yet
            viewModel.previewSchool(school)

            // Show visual feedback
            binding.selectedSchoolText.text = "Selected: ${school.name}"
            binding.selectedSchoolText.visibility = View.VISIBLE
            binding.selectedSchoolText.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.orange)
            )

            // Clear search results
            binding.schoolResultsRecyclerView.visibility = View.GONE
            isSettingTextProgrammatically = true
            binding.schoolSearch.setText(school.name)
            hideKeyboard()

            // Show that there are unsaved changes
            showUnsavedIndicator()
        }

        binding.schoolResultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = schoolAdapter
        }
    }
    private fun showUnsavedIndicator() {
        if (_binding == null) return

        if (viewModel.checkForChanges()) {
            binding.submitButton.text = "Save Changes*"
            binding.submitButton.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.black))
            binding.unsavedBadge.visibility = View.VISIBLE

            // Set correct warning icon based on theme
            val isNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
            val warningIcon = if (isNightMode) {
                R.drawable.ic_warning_night
            } else {
                R.drawable.ic_warning
            }
            binding.unsavedBadge.setCompoundDrawablesWithIntrinsicBounds(warningIcon, 0, 0, 0)

        } else {
            binding.submitButton.text = if (viewModel.hasExistingSchool.value) {
                "Update Profile"
            } else {
                "Submit"
            }
            binding.submitButton.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.black))
            binding.unsavedBadge.visibility = View.GONE
        }
    }

    private fun setupObservers() {
        // ========== PROVINCE OBSERVER ==========
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.provinces.collectLatest { provinces ->
                Log.d("ProfileFragment", "📋 Provinces loaded: ${provinces.size}")

                val provinceNames = provinces.map { it.name }
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    provinceNames
                )

                (binding.provinceSpinner as? AutoCompleteTextView)?.apply {
                    setAdapter(adapter)

                    var provinceSet = false

                    viewModel.selectedProvince.value?.let { province ->
                        setText(province.name, false)
                        provinceSet = true
                    }

                    if (!provinceSet) {
                        viewModel.selectedSchool.value?.let { school ->
                            if (school.provinceId != null) {
                                val matchingProvince = provinces.find { it.id == school.provinceId }
                                matchingProvince?.let {
                                    setText(it.name, false)
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
                    binding.provinceSpinner.setText(it.name, false)
                    binding.schoolSearch.isEnabled = true
                    binding.schoolSearchLayout.hint = "Search schools in ${it.name}"
                    binding.schoolSearchLayout.placeholderText = "Type at least 2 characters"
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
                    binding.selectedSchoolText.text = "Selected: ${school.name}"
                    binding.selectedSchoolText.visibility = View.VISIBLE
                    binding.selectedSchoolText.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.red)
                    )

                    isSettingTextProgrammatically = true
                    binding.schoolSearch.setText(school.name)
                    binding.schoolResultsRecyclerView.visibility = View.GONE
                    binding.schoolSearchLayout.error = null

                    if (viewModel.provinces.value.isNotEmpty() && school.provinceId != null) {
                        val matchingProvince = viewModel.provinces.value.find { it.id == school.provinceId }
                        matchingProvince?.let {
                            viewModel.selectProvince(it)
                        }
                    }
                } else {
                    binding.selectedSchoolText.visibility = View.GONE
                }
            }
        }

        // ========== CONFIRMATION DIALOG OBSERVER ==========
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.showConfirmationDialog.collectLatest { show ->
                if (show) {
                    viewModel.changesSummary.value?.let { summary ->
                        showUpdateConfirmationDialog(summary)
                    }
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

        // ========== UPDATE SUCCESS OBSERVER ==========
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.updateSuccess.collectLatest { success ->
                if (success) {
                    Snackbar.make(binding.root, "Profile updated successfully!", Snackbar.LENGTH_SHORT).show()
                    viewModel.clearSuccess()
                }
            }
        }

        // ========== HAS EXISTING SCHOOL OBSERVER ==========
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.hasExistingSchool.collectLatest { hasExisting ->
                if (hasExisting) {
                    binding.deleteWarningTitle.visibility = View.VISIBLE
                    binding.deleteWarningText.visibility = View.VISIBLE
                    binding.deleteButton.visibility = View.VISIBLE
                } else {
                    binding.deleteWarningTitle.visibility = View.GONE
                    binding.deleteWarningText.visibility = View.GONE
                    binding.deleteButton.visibility = View.GONE
                }
            }
        }

        // ========== PROFILE COMPLETE OBSERVER ==========
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
                    findNavController().navigate(R.id.nav_home)
                    viewModel.resetNavigation()
                }
            }
        }

        // ========== CURRENT SCHOOL MAPPING OBSERVER ==========
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentSchoolMapping.collectLatest { mapping ->
                mapping?.let {
                    Log.d("ProfileFragment", "📋 Current mapping: ${it.schoolName} (${it.mappingId})")
                }
            }
        }
    }

    private fun showUpdateConfirmationDialog(changes: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Profile Update")
            .setMessage("You are about to update:\n\n$changes\n\nDo you want to continue?")
            .setPositiveButton("Update") { _, _ ->
                viewModel.confirmAndSave()
            }
            .setNegativeButton("Cancel") { _, _ ->
                viewModel.cancelConfirmation()
                // Reload original data to revert UI
                loadUserData()
            }
            .show()
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

                    // Initialize ViewModel with original values
                    val school = viewModel.selectedSchool.value
                    viewModel.initializeProfile(displayMobile, school)
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
        // Check if any changes were made
        if (!viewModel.checkForChanges()) {
            Toast.makeText(requireContext(), "No changes to save", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate mobile if provided
        val mobile = binding.contactNumber.text.toString().trim()
        if (mobile.isNotEmpty() && !validateMobileNumber(mobile)) {
            binding.mobileTextInputLayout.error = "Invalid mobile number"
            return
        }

        // Check if school is selected
        if (viewModel.pendingSchool.value == null && viewModel.selectedSchool.value == null) {
            Toast.makeText(requireContext(), "Please select a school", Toast.LENGTH_LONG).show()
            binding.schoolSearch.requestFocus()
            return
        }

        // Show confirmation dialog with changes summary
        viewModel.prepareConfirmationDialog()
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
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteProfile()
            }
            .setNegativeButton("Cancel") { _, _ ->
                Snackbar.make(binding.root, "Account deletion cancelled", Snackbar.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun deleteProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.profileProgressLayout.visibility = View.VISIBLE
            val result = authRepository.deleteProfile()
            binding.profileProgressLayout.visibility = View.GONE

            if (result.isSuccess) {
                Toast.makeText(
                    requireContext(),
                    "Account deleted successfully",
                    Toast.LENGTH_LONG
                ).show()
                findNavController().navigate(R.id.loginFragment)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Failed to delete account: ${result.exceptionOrNull()?.message}",
                    Toast.LENGTH_LONG
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
        Log.d("ProfileFragment", "🔥 ProfileFragment onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d("ProfileFragment", "🔥 ProfileFragment onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d("ProfileFragment", "🔥 onStop")
    }
}