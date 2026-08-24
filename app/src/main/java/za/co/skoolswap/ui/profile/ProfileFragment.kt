package za.co.skoolswap.ui.profile

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import za.co.skoolswap.R
import za.co.skoolswap.common.constants.AppConstants.LogTags
import za.co.skoolswap.databinding.FragmentProfileBinding
import za.co.skoolswap.domain.repository.AuthRepositoryInterface
import za.co.skoolswap.utils.extensions.MobileValidator
import za.co.skoolswap.ui.component.ProvincePickerBottomSheet
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.activity.OnBackPressedCallback
import za.co.skoolswap.utils.DialogAction
import za.co.skoolswap.utils.DialogHelper
import timber.log.Timber
import za.co.skoolswap.domain.model.Province

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

    companion object {
        private const val SEARCH_DEBOUNCE_DELAY_MS = 500L
        private const val MOBILE_LENGTH_REQUIRED = 10
    }

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
        Timber.tag(LogTags.UI).d("🔥 onViewCreated")
        hideFab()
        setupUI()
        setupObservers()
        loadUserData()
        setupBackButton()
    }

    private fun setupUI() {
        setupMobileInput()
        setupProvinceDropdown()
        setupSchoolSearch()
        setupSchoolResults()

        binding.submitButton.setOnClickListener {
            completeProfile()
        }

        binding.deleteButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun setupBackButton() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.hasExistingSchool.value) {
                    findNavController().navigateUp()
                } else {
                    Toast.makeText(requireContext(), getString(R.string.profile_complete_setup), Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun setupProvinceDropdown() {
        // Province Spinner click - open bottom sheet
        binding.provinceSpinner.setOnClickListener {
            val provinces = viewModel.provinces.value
            if (provinces.isNotEmpty()) {
                showProvincePicker(provinces)
            } else {
                Toast.makeText(requireContext(), "Loading provinces...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showProvincePicker(provinces: List<Province>) {
        val selectedProvince = viewModel.selectedProvince.value
        val bottomSheet = ProvincePickerBottomSheet(
            provinces = provinces,
            selectedProvinceId = selectedProvince?.id,
            onProvinceSelected = { province ->
                val shouldClear = viewModel.selectedSchool.value == null
                viewModel.selectProvince(province, shouldClearSchool = shouldClear)

                binding.provinceSpinner.setText(province.name, false)

                if (shouldClear) {
                    binding.selectedSchoolCard.visibility = View.GONE
                    binding.selectedSchoolText.visibility = View.VISIBLE
                    binding.schoolSearch.text?.clear()
                }

                binding.provinceTextInputLayout.error = null
                binding.schoolSearch.isEnabled = true
                binding.schoolSearchLayout.hint = getString(R.string.profile_search_in_province, province.name)
                binding.schoolSearchLayout.placeholderText = getString(R.string.profile_search_hint)

                showUnsavedIndicator()
            }
        )
        bottomSheet.show(parentFragmentManager, "ProvincePickerBottomSheet")
    }

    private fun setupMobileInput() {
        binding.contactNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val mobile = s.toString().trim()

                when {
                    mobile.isEmpty() -> {
                        binding.contactNumber.error = null
                        binding.mobileTextInputLayout.error = null
                        binding.mobileTextInputLayout.isErrorEnabled = false
                    }
                    mobile.length < MOBILE_LENGTH_REQUIRED -> {
                        val message = getString(R.string.profile_mobile_incomplete, MOBILE_LENGTH_REQUIRED - mobile.length)
                        binding.contactNumber.error = message
                        binding.mobileTextInputLayout.error = message
                        binding.mobileTextInputLayout.isErrorEnabled = true
                    }
                    mobile.length == MOBILE_LENGTH_REQUIRED -> {
                        if (MobileValidator.isValidSouthAfricanMobile(mobile)) {
                            binding.contactNumber.error = null
                            binding.mobileTextInputLayout.error = null
                            binding.mobileTextInputLayout.isErrorEnabled = false
                        } else {
                            val error = MobileValidator.getErrorMessage(mobile)
                            binding.contactNumber.error = error
                            binding.mobileTextInputLayout.error = error
                            binding.mobileTextInputLayout.isErrorEnabled = true
                        }
                    }
                    mobile.length > MOBILE_LENGTH_REQUIRED -> {
                        val message = getString(R.string.profile_mobile_required)
                        binding.contactNumber.error = message
                        binding.mobileTextInputLayout.error = message
                        binding.mobileTextInputLayout.isErrorEnabled = true
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {
                val mobile = s.toString().trim()
                if (mobile != viewModel._originalMobile.value) {
                    if (mobile.isEmpty() || (mobile.length == MOBILE_LENGTH_REQUIRED && MobileValidator.isValidSouthAfricanMobile(mobile))) {
                        viewModel.previewMobile(mobile)
                        showUnsavedIndicator()
                    } else {
                        viewModel.previewMobile(mobile)
                    }
                }
            }
        })

        binding.contactNumber.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val mobile = binding.contactNumber.text.toString().trim()
                when {
                    mobile.isEmpty() -> {
                        binding.contactNumber.error = null
                        binding.mobileTextInputLayout.error = null
                        binding.mobileTextInputLayout.isErrorEnabled = false
                    }
                    mobile.length == MOBILE_LENGTH_REQUIRED && MobileValidator.isValidSouthAfricanMobile(mobile) -> {
                        binding.contactNumber.error = null
                        binding.mobileTextInputLayout.error = null
                        binding.mobileTextInputLayout.isErrorEnabled = false
                    }
                    mobile.length in 1..9 -> {
                        val message = getString(R.string.profile_mobile_incomplete, MOBILE_LENGTH_REQUIRED - mobile.length)
                        binding.contactNumber.error = message
                        binding.mobileTextInputLayout.error = message
                        binding.mobileTextInputLayout.isErrorEnabled = true
                    }
                    mobile.length > MOBILE_LENGTH_REQUIRED -> {
                        val message = getString(R.string.profile_mobile_required)
                        binding.contactNumber.error = message
                        binding.mobileTextInputLayout.error = message
                        binding.mobileTextInputLayout.isErrorEnabled = true
                    }
                }
            }
        }
    }

    private fun setupSchoolSearch() {
        binding.schoolSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isSettingTextProgrammatically) {
                    isSettingTextProgrammatically = false
                    return
                }

                if (viewModel.selectedSchool.value != null) {
                    Timber.tag(LogTags.UI).d("User typing - clearing school selection")
                    viewModel.clearSchoolSelection()
                    binding.selectedSchoolCard.visibility = View.GONE
                    binding.selectedSchoolText.visibility = View.VISIBLE
                    showUnsavedIndicator()
                }

                binding.schoolSearchLayout.error = null
                searchDebounceJob?.cancel()

                val query = s?.toString()?.trim() ?: ""

                if (query.length >= 2 && viewModel.selectedProvince.value != null) {
                    searchDebounceJob = lifecycleScope.launch {
                        delay(SEARCH_DEBOUNCE_DELAY_MS)
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
            viewModel.previewSchool(school)

            binding.selectedSchoolCard.visibility = View.VISIBLE
            binding.selectedSchoolName.text = school.name
            binding.selectedSchoolLocation.text = school.provinceName ?: getString(R.string.profile_selected_school)
            binding.selectedSchoolText.visibility = View.GONE

            // Load school logo
            loadSchoolLogo(school)

            binding.schoolResultsRecyclerView.visibility = View.GONE
            isSettingTextProgrammatically = true
            binding.schoolSearch.setText(school.name)
            hideKeyboard()

            showUnsavedIndicator()
        }

        binding.schoolResultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = schoolAdapter
        }
    }

    private fun loadSchoolLogo(school: za.co.skoolswap.domain.model.School) {
        if (!school.logoUrl.isNullOrEmpty()) {
            Glide.with(requireContext())
                .load(school.logoUrl)
                .placeholder(R.drawable.ic_school_placeholder)
                .error(R.drawable.ic_school_placeholder)
                .fallback(R.drawable.ic_school_placeholder)
                .circleCrop()
                .into(binding.schoolLogo)
            binding.schoolLogo.visibility = View.VISIBLE
        } else {
            binding.schoolLogo.setImageResource(R.drawable.ic_school_placeholder)
            binding.schoolLogo.visibility = View.VISIBLE
        }
    }

    private fun showUnsavedIndicator() {
        if (_binding == null) return
        if (!viewModel.isInitialized.value) return

        if (viewModel.checkForChanges()) {
            binding.submitButton.text = getString(R.string.profile_save_changes)
            binding.submitButton.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.black))
            binding.unsavedBadge.visibility = View.VISIBLE

            val isNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
            val warningIcon = if (isNightMode) {
                R.drawable.ic_warning_night
            } else {
                R.drawable.ic_warning
            }
            binding.unsavedBadge.setCompoundDrawablesWithIntrinsicBounds(warningIcon, 0, 0, 0)

        } else {
            binding.submitButton.text = if (viewModel.hasExistingSchool.value) {
                getString(R.string.profile_update)
            } else {
                getString(R.string.profile_submit)
            }
            binding.submitButton.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.black))
            binding.unsavedBadge.visibility = View.GONE
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.provinces.collectLatest { provinces ->
                Timber.tag(LogTags.UI).d("📋 Provinces loaded: ${provinces.size}")

                // Update spinner text if province is selected
                viewModel.selectedProvince.value?.let { province ->
                    binding.provinceSpinner.setText(province.name, false)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedProvince.collectLatest { province ->
                province?.let {
                    binding.provinceSpinner.setText(it.name, false)
                    binding.schoolSearch.isEnabled = true
                    binding.schoolSearchLayout.hint = getString(R.string.profile_search_in_province, it.name)
                    binding.schoolSearchLayout.placeholderText = getString(R.string.profile_search_hint)
                    binding.provinceTextInputLayout.error = null
                }
            }
        }

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
                        binding.noResultsText.text = getString(R.string.profile_no_results)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedSchool.collectLatest { school ->
                if (school != null) {
                    binding.selectedSchoolCard.visibility = View.VISIBLE
                    binding.selectedSchoolName.text = school.name
                    binding.selectedSchoolLocation.text = school.provinceName ?: getString(R.string.profile_selected_school)
                    binding.selectedSchoolText.visibility = View.GONE

                    loadSchoolLogo(school)

                    isSettingTextProgrammatically = true
                    binding.schoolSearch.setText(school.name)
                    binding.schoolResultsRecyclerView.visibility = View.GONE
                    binding.schoolSearchLayout.error = null
                } else {
                    binding.selectedSchoolCard.visibility = View.GONE
                    binding.selectedSchoolText.visibility = View.VISIBLE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.showConfirmationDialog.collectLatest { show ->
                if (show) {
                    viewModel.changesSummary.value?.let { summary ->
                        showUpdateConfirmationDialog(summary)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isSearchActive.collectLatest { isActive ->
                if (isActive) {
                    binding.schoolResultsRecyclerView.visibility = View.VISIBLE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.profileProgressLayout.visibility = if (isLoading) View.VISIBLE else View.GONE
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
                    Snackbar.make(binding.root, getString(R.string.profile_update_success), Snackbar.LENGTH_SHORT).show()
                    viewModel.clearSuccess()
                }
            }
        }

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

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.profileComplete.collectLatest { isComplete ->
                if (isComplete) {
                    binding.profileProgressLayout.visibility = View.GONE
                    val message = if (viewModel.hasExistingSchool.value) {
                        getString(R.string.profile_school_updated)
                    } else {
                        getString(R.string.profile_complete)
                    }
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    findNavController().navigate(R.id.nav_home)
                    viewModel.resetNavigation()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentSchoolMapping.collectLatest { mapping ->
                mapping?.let {
                    Timber.tag(LogTags.UI).d("📋 Current mapping: ${it.schoolName} (${it.mappingId})")
                }
            }
        }
    }

    private fun showUpdateConfirmationDialog(changes: String) {
        DialogHelper.showConfirmationDialog(
            context = requireContext(),
            action = DialogAction.SaveChanges(changes),
            onConfirm = {
                viewModel.confirmAndSave()
            }
        )
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

                    viewModel.initializeProfile(displayMobile, null)
                }
            }
        }
    }

    private fun validateMobileNumber(mobile: String): Boolean {
        binding.contactNumber.error = null
        binding.mobileTextInputLayout.error = null
        binding.mobileTextInputLayout.isErrorEnabled = false

        if (mobile.isEmpty()) {
            return true
        }

        if (mobile.length != MOBILE_LENGTH_REQUIRED) {
            val message = if (mobile.length < MOBILE_LENGTH_REQUIRED) {
                getString(R.string.profile_mobile_incomplete, MOBILE_LENGTH_REQUIRED - mobile.length)
            } else {
                getString(R.string.profile_mobile_required)
            }
            binding.contactNumber.error = message
            binding.mobileTextInputLayout.error = message
            binding.mobileTextInputLayout.isErrorEnabled = true
            return false
        }

        if (!mobile.all { it.isDigit() }) {
            val message = getString(R.string.profile_mobile_numbers_only)
            binding.contactNumber.error = message
            binding.mobileTextInputLayout.error = message
            binding.mobileTextInputLayout.isErrorEnabled = true
            return false
        }

        if (!MobileValidator.isValidSouthAfricanMobile(mobile)) {
            val prefix = mobile.substring(0, 3)
            val message = getString(R.string.profile_mobile_prefix_invalid, prefix)
            binding.contactNumber.error = message
            binding.mobileTextInputLayout.error = message
            binding.mobileTextInputLayout.isErrorEnabled = true
            return false
        }

        binding.contactNumber.error = null
        binding.mobileTextInputLayout.error = null
        binding.mobileTextInputLayout.isErrorEnabled = false
        return true
    }

    private fun completeProfile() {
        if (!viewModel.checkForChanges()) {
            Toast.makeText(requireContext(), getString(R.string.profile_no_changes), Toast.LENGTH_SHORT).show()
            return
        }

        val mobile = binding.contactNumber.text.toString().trim()
        if (mobile.isNotEmpty() && !validateMobileNumber(mobile)) {
            binding.mobileTextInputLayout.error = getString(R.string.profile_mobile_invalid)
            binding.mobileTextInputLayout.isErrorEnabled = true
            binding.contactNumber.requestFocus()
            Toast.makeText(requireContext(), getString(R.string.profile_mobile_invalid), Toast.LENGTH_LONG).show()
            return
        }

        if (viewModel.pendingSchool.value == null && viewModel.selectedSchool.value == null) {
            Toast.makeText(requireContext(), getString(R.string.profile_select_school_first), Toast.LENGTH_LONG).show()
            binding.schoolSearch.requestFocus()
            return
        }

        viewModel.prepareConfirmationDialog()
    }

    private fun loadProfilePicture(url: String?) {
        try {
            Glide.with(requireContext())
                .load(url)
                .placeholder(R.drawable.ic_user)
                .error(R.drawable.ic_user)
                .circleCrop()
                .override(240, 240)
                .timeout(10000)
                .into(binding.profileImage)
        } catch (e: Exception) {
            // Handle error silently
        }
    }

    private fun showDeleteConfirmationDialog() {
        DialogHelper.showConfirmationDialog(
            context = requireContext(),
            action = DialogAction.DeleteAccount,
            onConfirm = {
                deleteProfile()
            }
        )
    }

    private fun deleteProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.profileProgressLayout.visibility = View.VISIBLE
            val result = authRepository.deleteProfile()
            binding.profileProgressLayout.visibility = View.GONE

            if (result.isSuccess) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.profile_delete_success),
                    Toast.LENGTH_LONG
                ).show()
                findNavController().navigate(R.id.loginFragment)
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: ""
                Toast.makeText(
                    requireContext(),
                    getString(R.string.profile_delete_failed, errorMsg),
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
        Timber.tag(LogTags.UI).d("🔥 ProfileFragment onResume")
    }

    override fun onPause() {
        super.onPause()
        Timber.tag(LogTags.UI).d("🔥 ProfileFragment onPause")
    }

    override fun onStop() {
        super.onStop()
        Timber.tag(LogTags.UI).d("🔥 onStop")
    }
}