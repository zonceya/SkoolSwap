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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.skoolswap.R
import com.example.skoolswap.databinding.FragmentSchoolOnboardingBinding
import com.example.skoolswap.domain.model.Province
import com.example.skoolswap.ui.profile.SchoolAdapter
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SchoolOnboardingFragment : Fragment() {

    private var _binding: FragmentSchoolOnboardingBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SchoolOnboardingViewModel by viewModels()

    private lateinit var schoolAdapter: SchoolAdapter

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

        // Initialize progress - only PROVINCE active
        resetProgress()

        // Load provinces
        viewModel.loadProvinces()
    }

    private fun resetProgress() {
        // Only PROVINCE is active
        binding.progressStep1Icon.setImageResource(R.drawable.dot_province_active)
        binding.progressStep1Line.setBackgroundResource(R.drawable.progress_line_active)

        binding.progressStep2Icon.setImageResource(R.drawable.dot_school_inactive)
        binding.progressStep2Line.setBackgroundResource(R.drawable.progress_line_inactive)

        binding.progressStep3Icon.setImageResource(R.drawable.dot_end_inactive)
        binding.progressStep3Line.setBackgroundResource(R.drawable.progress_line_inactive)

        Log.d("SchoolOnboardingFragment", "📊 Progress reset: Only PROVINCE active")
    }

    private fun setupUI() {
        binding.submitButton.isEnabled = false
        binding.schoolSearch.isEnabled = false

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
            updateProgress(2) // School selected
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
                val query = s?.toString() ?: ""

                if (query.isNotEmpty()) {
                    if (viewModel.selectedProvince.value != null) {
                        viewModel.searchSchoolsLocally(query)
                        binding.schoolResultsRecyclerView.visibility = View.VISIBLE
                        binding.noResultsText.visibility = View.GONE
                        binding.provinceEmptyMessage.visibility = View.GONE
                    } else {
                        binding.schoolResultsRecyclerView.visibility = View.GONE
                        binding.provinceEmptyMessage.visibility = View.VISIBLE
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
                // Observe provinces - THIS IS CRITICAL
                launch {
                    viewModel.provinces.collect { provinces ->
                        Log.d("SchoolOnboardingFragment", "📋 Provinces collected: ${provinces.size}")
                        if (provinces.isNotEmpty()) {
                            setupProvinceSpinner(provinces)
                        }
                    }
                }

                // Observe selected province
                launch {
                    viewModel.selectedProvince.collect { province ->
                        if (province != null) {
                            Log.d("SchoolOnboardingFragment", "📍 Province selected: ${province.name}")
                            binding.schoolSearch.isEnabled = true
                            binding.schoolSearchLayout.placeholderText = "Start typing..."
                            binding.provinceEmptyMessage.visibility = View.GONE
                            viewModel.loadAllSchoolsForProvince(province)
                            updateProgress(1)
                        }
                    }
                }

                // Observe school search results
                launch {
                    viewModel.schools.collect { schools ->
                        schoolAdapter.submitList(schools)
                        if (schools.isEmpty() && binding.schoolSearch.text?.isNotEmpty() == true) {
                            binding.noResultsText.visibility = View.VISIBLE
                        } else {
                            binding.noResultsText.visibility = View.GONE
                        }
                    }
                }

                // Observe loading states
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    }
                }

                // Observe schools loading
                launch {
                    viewModel.isLoadingSchools.collect { isLoading ->
                        binding.schoolProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                        if (!isLoading && viewModel.cachedSchools.value.isNotEmpty()) {
                            Log.d("SchoolOnboardingFragment", "✅ Schools loaded: ${viewModel.cachedSchools.value.size}")
                            binding.schoolSearch.requestFocus()
                            showKeyboard(binding.schoolSearch)
                        }
                    }
                }

                // Observe selected school
                launch {
                    viewModel.selectedSchool.collect { school ->
                        if (school != null) {
                            binding.selectedSchoolCard.visibility = View.VISIBLE
                            binding.selectedSchoolName.text = school.name
                            binding.submitButton.isEnabled = true
                        }
                    }
                }

                // Observe update success
                launch {
                    viewModel.updateSuccess.collect { success ->
                        if (success) {
                            showSuccessAndNavigate()
                            viewModel.clearSuccess()
                        }
                    }
                }

                // Observe errors
                launch {
                    viewModel.error.collect { error ->
                        if (error != null) {
                            Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
                            viewModel.clearError()
                        }
                    }
                }

                // Observe profile completion - COMMENTED OUT (handled in showSuccessAndNavigate)
                // launch {
                //     viewModel.profileComplete.collect { isComplete ->
                //         if (isComplete) {
                //             findNavController().navigate(R.id.action_schoolOnboardingFragment_to_nav_home)
                //             viewModel.resetNavigation()
                //         }
                //     }
                // }
            }
        }
    }

    private fun setupProvinceSpinner(provinces: List<Province>) {
        Log.d("SchoolOnboardingFragment", "🔧 Setting up province spinner with ${provinces.size} provinces")

        val provinceNames = provinces.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, provinceNames)

        val autoCompleteTextView = binding.provinceSpinner as? AutoCompleteTextView
        autoCompleteTextView?.setAdapter(adapter)
        autoCompleteTextView?.threshold = 1

        autoCompleteTextView?.setOnItemClickListener { _, _, position, _ ->
            val selectedProvince = provinces[position]
            Log.d("SchoolOnboardingFragment", "👆 Province clicked: ${selectedProvince.name}")
            viewModel.selectProvince(selectedProvince, shouldClearSchool = true)

            binding.schoolSearch.setText("")
            binding.schoolSearch.isEnabled = true
            binding.selectedSchoolCard.visibility = View.GONE
            binding.submitButton.isEnabled = false
            viewModel.clearSchoolSelection()
        }
    }

    private fun updateProgress(step: Int) {
        Log.d("SchoolOnboardingFragment", "📊 updateProgress called with step: $step")

        when (step) {
            1 -> {
                Log.d("SchoolOnboardingFragment", "🎨 STEP 1: Province selected")
                Log.d("SchoolOnboardingFragment", "   - PROVINCE dot: dot_province_active (Black + White center)")
                Log.d("SchoolOnboardingFragment", "   - PROVINCE line: progress_line_active (Black)")
                Log.d("SchoolOnboardingFragment", "   - SCHOOL dot: dot_school_inactive (Gray outline + Gray center)")
                Log.d("SchoolOnboardingFragment", "   - SCHOOL line: progress_line_active (Black - guiding to SCHOOL)")
                Log.d("SchoolOnboardingFragment", "   - DONE dot: dot_end_inactive (Gray outline + Gray center)")
                Log.d("SchoolOnboardingFragment", "   - DONE line: progress_line_inactive (Light Gray)")

                // Province selected - PROVINCE active, line to SCHOOL becomes bold
                binding.progressStep1Icon.setImageResource(R.drawable.dot_province_active)
                binding.progressStep1Line.setBackgroundResource(R.drawable.progress_line_active)

                binding.progressStep2Icon.setImageResource(R.drawable.dot_school_inactive)
                binding.progressStep2Line.setBackgroundResource(R.drawable.progress_line_active)

                binding.progressStep3Icon.setImageResource(R.drawable.dot_end_inactive)
                binding.progressStep3Line.setBackgroundResource(R.drawable.progress_line_inactive)

                verifyProgressDrawables()
            }
            2 -> {
                Log.d("SchoolOnboardingFragment", "🎨 STEP 2: School selected")
                Log.d("SchoolOnboardingFragment", "   - PROVINCE dot: dot_province_active (Black + White center)")
                Log.d("SchoolOnboardingFragment", "   - PROVINCE line: progress_line_active (Black)")
                Log.d("SchoolOnboardingFragment", "   - SCHOOL dot: dot_school_active (Black + White center)")
                Log.d("SchoolOnboardingFragment", "   - SCHOOL line: progress_line_active (Black)")
                Log.d("SchoolOnboardingFragment", "   - DONE dot: dot_end_inactive (Gray outline + Gray center)")
                Log.d("SchoolOnboardingFragment", "   - DONE line: progress_line_active (Black - guiding to DONE)")

                // School selected - PROVINCE + SCHOOL active, line to DONE becomes bold
                binding.progressStep1Icon.setImageResource(R.drawable.dot_province_active)
                binding.progressStep1Line.setBackgroundResource(R.drawable.progress_line_active)

                binding.progressStep2Icon.setImageResource(R.drawable.dot_school_active)
                binding.progressStep2Line.setBackgroundResource(R.drawable.progress_line_active)

                binding.progressStep3Icon.setImageResource(R.drawable.dot_end_inactive)
                binding.progressStep3Line.setBackgroundResource(R.drawable.progress_line_active)

                verifyProgressDrawables()
            }
            3 -> {
                Log.d("SchoolOnboardingFragment", "🎨 STEP 3: Complete - ALL active")
                Log.d("SchoolOnboardingFragment", "   - PROVINCE dot: dot_province_active (Black + White center)")
                Log.d("SchoolOnboardingFragment", "   - PROVINCE line: progress_line_active (Black)")
                Log.d("SchoolOnboardingFragment", "   - SCHOOL dot: dot_school_active (Black + White center)")
                Log.d("SchoolOnboardingFragment", "   - SCHOOL line: progress_line_active (Black)")
                Log.d("SchoolOnboardingFragment", "   - DONE dot: dot_end_active (Black + White center)")
                Log.d("SchoolOnboardingFragment", "   - DONE line: progress_line_active (Black)")

                // Complete - ALL active
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
        val step1Icon = binding.progressStep1Icon.drawable
        val step1Line = binding.progressStep1Line.background
        val step2Icon = binding.progressStep2Icon.drawable
        val step2Line = binding.progressStep2Line.background
        val step3Icon = binding.progressStep3Icon.drawable
        val step3Line = binding.progressStep3Line.background

        Log.d("SchoolOnboardingFragment", "🔍 VERIFYING DRAWABLES:")
        Log.d("SchoolOnboardingFragment", "   Step1 Icon: ${step1Icon?.constantState}")
        Log.d("SchoolOnboardingFragment", "   Step1 Line: ${step1Line?.constantState}")
        Log.d("SchoolOnboardingFragment", "   Step2 Icon: ${step2Icon?.constantState}")
        Log.d("SchoolOnboardingFragment", "   Step2 Line: ${step2Line?.constantState}")
        Log.d("SchoolOnboardingFragment", "   Step3 Icon: ${step3Icon?.constantState}")
        Log.d("SchoolOnboardingFragment", "   Step3 Line: ${step3Line?.constantState}")
    }

    private fun checkDrawableExists(drawableName: String): Boolean {
        val resourceId = resources.getIdentifier(drawableName, "drawable", requireContext().packageName)
        val exists = resourceId != 0
        Log.d("SchoolOnboardingFragment", "📁 Drawable '$drawableName' exists: $exists")
        return exists
    }

    private fun verifyAllDrawables() {
        Log.d("SchoolOnboardingFragment", "🔍 CHECKING ALL DRAWABLES:")
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
        val schoolName = viewModel.selectedSchool.value?.name ?: "your school"

        updateProgress(3)

        // Show Snackbar (replaces Lottie)
        Snackbar.make(
            binding.root,
            "🎉 Well done! You have successfully chosen $schoolName",
            Snackbar.LENGTH_LONG
        ).show()

        // Navigate ONCE after delay
        lifecycleScope.launch {
            delay(2000)
            try {
                findNavController().navigate(R.id.action_schoolOnboardingFragment_to_nav_home)
            } catch (e: Exception) {
                Log.e("SchoolOnboarding", "Navigation error: ${e.message}")
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