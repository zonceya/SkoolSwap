package com.example.skoolswap.ui.schoolonboarding

import android.app.Activity
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.skoolswap.R
import com.example.skoolswap.common.constants.AppConstants
import com.example.skoolswap.common.constants.AppConstants.LogTags
import com.example.skoolswap.databinding.FragmentSchoolOnboardingBinding
import com.example.skoolswap.domain.model.Province
import com.example.skoolswap.ui.profile.SchoolAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class SchoolOnboardingFragment : Fragment() {

    private var _binding: FragmentSchoolOnboardingBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SchoolOnboardingViewModel by viewModels()

    private lateinit var schoolAdapter: SchoolAdapter

    companion object {
        private const val NAVIGATION_DELAY_MS = 2000L
        private const val SEARCH_MIN_LENGTH = 2
        private const val PROVINCE_SPINNER_THRESHOLD = 1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSchoolOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        verifyAllDrawables()
        setupUI()
        setupRecyclerView()
        setupSearchListener()
        observeViewModel()

        resetProgress()
        viewModel.loadProvinces()
    }

    private fun resetProgress() {
        binding.progressStep1Icon.setImageResource(R.drawable.dot_province_active)
        binding.progressStep1Line.setBackgroundResource(R.drawable.progress_line_active)

        binding.progressStep2Icon.setImageResource(R.drawable.dot_school_inactive)
        binding.progressStep2Line.setBackgroundResource(R.drawable.progress_line_inactive)

        binding.progressStep3Icon.setImageResource(R.drawable.dot_end_inactive)
        binding.progressStep3Line.setBackgroundResource(R.drawable.progress_line_inactive)

        Timber.tag(LogTags.UI).d("📊 Progress reset: Only PROVINCE active")
    }

    private fun setupUI() {
        binding.submitButton.isEnabled = false
        binding.submitButton.text = getString(R.string.school_onboarding_submit)
        binding.schoolSearch.isEnabled = false
        binding.schoolSearch.hint = getString(R.string.school_onboarding_search_school)

        binding.submitButton.setOnClickListener {
            viewModel.submitSchoolSelection()
        }
    }

    private fun setupRecyclerView() {
        schoolAdapter = SchoolAdapter { school ->
            viewModel.selectSchool(school)
            binding.schoolResultsRecyclerView.visibility = View.GONE
            binding.schoolSearch.setText(school.name)
            binding.schoolSearch.clearFocus()
            hideKeyboard()

            binding.selectedSchoolCard.visibility = View.VISIBLE
            binding.selectedSchoolName.text = school.name
            binding.submitButton.isEnabled = true
            updateProgress(2)
        }

        binding.schoolResultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = schoolAdapter
        }
    }

    private fun setupSearchListener() {
        binding.schoolSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: AppConstants.EMPTY_STRING

                if (query.isNotEmpty()) {
                    if (viewModel.selectedProvince.value != null) {
                        viewModel.searchSchoolsLocally(query)
                        binding.schoolResultsRecyclerView.visibility = View.VISIBLE
                        binding.noResultsText.visibility = View.GONE
                        binding.provinceEmptyMessage.visibility = View.GONE
                    } else {
                        binding.schoolResultsRecyclerView.visibility = View.GONE
                        binding.provinceEmptyMessage.visibility = View.VISIBLE
                        binding.provinceEmptyMessage.text = getString(R.string.school_onboarding_select_province_first)
                    }
                } else {
                    binding.schoolResultsRecyclerView.visibility = View.GONE
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.provinces.collect { provinces ->
                        Timber.tag(LogTags.UI).d("📋 Provinces collected: ${provinces.size}")
                        if (provinces.isNotEmpty()) {
                            setupProvinceSpinner(provinces)
                        }
                    }
                }

                launch {
                    viewModel.selectedProvince.collect { province ->
                        if (province != null) {
                            Timber.tag(LogTags.UI).d("📍 Province selected: ${province.name}")
                            binding.schoolSearch.isEnabled = true
                            binding.schoolSearchLayout.placeholderText = getString(R.string.school_onboarding_start_typing)
                            binding.provinceEmptyMessage.visibility = View.GONE
                            viewModel.loadAllSchoolsForProvince(province)
                            updateProgress(1)
                        }
                    }
                }

                launch {
                    viewModel.schools.collect { schools ->
                        schoolAdapter.submitList(schools)
                        if (schools.isEmpty() && binding.schoolSearch.text?.isNotEmpty() == true) {
                            binding.noResultsText.visibility = View.VISIBLE
                            binding.noResultsText.text = getString(R.string.school_onboarding_no_results)
                        } else {
                            binding.noResultsText.visibility = View.GONE
                        }
                    }
                }

                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.isLoadingSchools.collect { isLoading ->
                        binding.schoolProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                        if (!isLoading && viewModel.cachedSchools.value.isNotEmpty()) {
                            Timber.tag(LogTags.UI).d("✅ Schools loaded: ${viewModel.cachedSchools.value.size}")
                            binding.schoolSearch.requestFocus()
                            showKeyboard(binding.schoolSearch)
                        }
                    }
                }

                launch {
                    viewModel.selectedSchool.collect { school ->
                        if (school != null) {
                            binding.selectedSchoolCard.visibility = View.VISIBLE
                            binding.selectedSchoolName.text = school.name
                            binding.submitButton.isEnabled = true
                        }
                    }
                }

                launch {
                    viewModel.updateSuccess.collect { success ->
                        if (success) {
                            showSuccessAndNavigate()
                            viewModel.clearSuccess()
                        }
                    }
                }

                launch {
                    viewModel.error.collect { error ->
                        if (error != null) {
                            Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
                            viewModel.clearError()
                        }
                    }
                }
            }
        }
    }

    private fun setupProvinceSpinner(provinces: List<Province>) {
        Timber.tag(LogTags.UI).d("🔧 Setting up province spinner with ${provinces.size} provinces")

        val provinceNames = provinces.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, provinceNames)

        val autoCompleteTextView = binding.provinceSpinner as? AutoCompleteTextView
        autoCompleteTextView?.setAdapter(adapter)
        autoCompleteTextView?.threshold = PROVINCE_SPINNER_THRESHOLD
        autoCompleteTextView?.hint = getString(R.string.school_onboarding_select_province)

        autoCompleteTextView?.setOnItemClickListener { _, _, position, _ ->
            val selectedProvince = provinces[position]
            Timber.tag(LogTags.UI).d("👆 Province clicked: ${selectedProvince.name}")
            viewModel.selectProvince(selectedProvince, shouldClearSchool = true)

            binding.schoolSearch.setText(AppConstants.EMPTY_STRING)
            binding.schoolSearch.isEnabled = true
            binding.selectedSchoolCard.visibility = View.GONE
            binding.submitButton.isEnabled = false
            viewModel.clearSchoolSelection()
        }
    }

    private fun updateProgress(step: Int) {
        Timber.tag(LogTags.UI).d("📊 updateProgress called with step: $step")

        when (step) {
            1 -> {
                Timber.tag(LogTags.UI).d("🎨 STEP 1: Province selected")
                binding.progressStep1Icon.setImageResource(R.drawable.dot_province_active)
                binding.progressStep1Line.setBackgroundResource(R.drawable.progress_line_active)

                binding.progressStep2Icon.setImageResource(R.drawable.dot_school_inactive)
                binding.progressStep2Line.setBackgroundResource(R.drawable.progress_line_active)

                binding.progressStep3Icon.setImageResource(R.drawable.dot_end_inactive)
                binding.progressStep3Line.setBackgroundResource(R.drawable.progress_line_inactive)

                verifyProgressDrawables()
            }
            2 -> {
                Timber.tag(LogTags.UI).d("🎨 STEP 2: School selected")
                binding.progressStep1Icon.setImageResource(R.drawable.dot_province_active)
                binding.progressStep1Line.setBackgroundResource(R.drawable.progress_line_active)

                binding.progressStep2Icon.setImageResource(R.drawable.dot_school_active)
                binding.progressStep2Line.setBackgroundResource(R.drawable.progress_line_active)

                binding.progressStep3Icon.setImageResource(R.drawable.dot_end_inactive)
                binding.progressStep3Line.setBackgroundResource(R.drawable.progress_line_active)

                verifyProgressDrawables()
            }
            3 -> {
                Timber.tag(LogTags.UI).d("🎨 STEP 3: Complete - ALL active")
                binding.progressStep1Icon.setImageResource(R.drawable.dot_province_active)
                binding.progressStep1Line.setBackgroundResource(R.drawable.progress_line_active)

                binding.progressStep2Icon.setImageResource(R.drawable.dot_school_active)
                binding.progressStep2Line.setBackgroundResource(R.drawable.progress_line_active)

                binding.progressStep3Icon.setImageResource(R.drawable.dot_end_active)
                binding.progressStep3Line.setBackgroundResource(R.drawable.progress_line_active)

                verifyProgressDrawables()
            }
        }
    }

    private fun verifyProgressDrawables() {
        Timber.tag(LogTags.UI).d("🔍 VERIFYING DRAWABLES:")
        Timber.tag(LogTags.UI).d("   Step1 Icon: ${binding.progressStep1Icon.drawable?.constantState}")
        Timber.tag(LogTags.UI).d("   Step1 Line: ${binding.progressStep1Line.background?.constantState}")
        Timber.tag(LogTags.UI).d("   Step2 Icon: ${binding.progressStep2Icon.drawable?.constantState}")
        Timber.tag(LogTags.UI).d("   Step2 Line: ${binding.progressStep2Line.background?.constantState}")
        Timber.tag(LogTags.UI).d("   Step3 Icon: ${binding.progressStep3Icon.drawable?.constantState}")
        Timber.tag(LogTags.UI).d("   Step3 Line: ${binding.progressStep3Line.background?.constantState}")
    }

    private fun checkDrawableExists(drawableName: String): Boolean {
        val resourceId = resources.getIdentifier(drawableName, "drawable", requireContext().packageName)
        val exists = resourceId != 0
        Timber.tag(LogTags.UI).d("📁 Drawable '$drawableName' exists: $exists")
        return exists
    }

    private fun verifyAllDrawables() {
        Timber.tag(LogTags.UI).d("🔍 CHECKING ALL DRAWABLES:")
        checkDrawableExists("dot_province_active")
        checkDrawableExists("dot_province_inactive")
        checkDrawableExists("dot_school_active")
        checkDrawableExists("dot_school_inactive")
        checkDrawableExists("dot_end_active")
        checkDrawableExists("dot_end_inactive")
        checkDrawableExists("progress_line_active")
        checkDrawableExists("progress_line_inactive")
    }

    private fun showSuccessAndNavigate() {
        val schoolName = viewModel.selectedSchool.value?.name ?: AppConstants.EMPTY_STRING

        updateProgress(3)

        val successMessage = getString(R.string.school_onboarding_success_message, schoolName)
        Snackbar.make(
            binding.root,
            successMessage,
            Snackbar.LENGTH_LONG
        ).show()

        lifecycleScope.launch {
            delay(NAVIGATION_DELAY_MS)
            try {
                findNavController().navigate(R.id.action_schoolOnboardingFragment_to_nav_home)
            } catch (e: Exception) {
                Timber.tag(LogTags.UI).e(e, "Navigation error")
                findNavController().popBackStack()
            }
        }
    }

    private fun showKeyboard(view: View) {
        val imm = requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}