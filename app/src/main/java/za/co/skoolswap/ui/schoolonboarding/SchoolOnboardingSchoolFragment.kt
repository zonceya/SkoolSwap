package za.co.skoolswap.ui.schoolonboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ScrollView
import android.widget.Toast  // ✅ Add this import
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import za.co.skoolswap.R
import za.co.skoolswap.databinding.FragmentSchoolOnboardingSchoolBinding
import za.co.skoolswap.domain.model.Province
import za.co.skoolswap.domain.model.School
import za.co.skoolswap.ui.schoolonboarding.SchoolOnboardingAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class SchoolOnboardingSchoolFragment : Fragment() {

    private var _binding: FragmentSchoolOnboardingSchoolBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SchoolOnboardingViewModel by viewModels()

    private lateinit var schoolAdapter: SchoolOnboardingAdapter
    private var isSearching = false
    private var searchJob: Job? = null
    private var isProgrammaticTextChange = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSchoolOnboardingSchoolBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val provinceId = arguments?.getInt("province_id")
        val provinceName = arguments?.getString("province_name")

        if (provinceId != null && provinceName != null) {
            val province = Province(id = provinceId, name = provinceName)
            viewModel.selectProvince(province)
            viewModel.loadAllSchoolsForProvince(province)
        } else {
            findNavController().popBackStack()
            return
        }

        setupUI()
        setupRecyclerView()
        setupSearchListener()
        observeViewModel()
        updateDots()
    }

    private fun setupUI() {
        binding.doneButton.isEnabled = false

        binding.schoolSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }

        // ✅ Done button with Toast
        binding.doneButton.setOnClickListener {
            val selectedSchool = viewModel.selectedSchool.value
            if (selectedSchool != null) {
                viewModel.submitSchoolSelection()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Please select your school first",
                    Toast.LENGTH_SHORT
                ).show()

                binding.schoolSearch.requestFocus()
                showKeyboard()
            }
        }

        binding.schoolSearch.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                scrollToSearchField()
            }
        }

        binding.schoolSearch.postDelayed({
            binding.schoolSearch.requestFocus()
            showKeyboard()
        }, 500)
    }

    private fun setupRecyclerView() {
        schoolAdapter = SchoolOnboardingAdapter { school ->
            selectSchool(school)
        }

        binding.schoolResultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = schoolAdapter
        }
    }

    private fun setupSearchListener() {
        binding.schoolSearch.doOnTextChanged { text, _, _, _ ->
            if (isProgrammaticTextChange) return@doOnTextChanged

            val query = text?.toString()?.trim() ?: ""
            searchJob?.cancel()

            when {
                query.isEmpty() -> {
                    binding.schoolResultsRecyclerView.visibility = View.GONE
                    binding.noResultsText.visibility = View.GONE
                    schoolAdapter.submitList(emptyList())
                    isSearching = false
                }
                query.length >= 2 -> {
                    isSearching = true
                    searchJob = viewLifecycleOwner.lifecycleScope.launch {
                        delay(300)
                        performSearch()
                    }
                }
                else -> {
                    binding.schoolResultsRecyclerView.visibility = View.GONE
                    binding.noResultsText.visibility = View.GONE
                    schoolAdapter.submitList(emptyList())
                    isSearching = false
                }
            }
        }
    }

    private fun performSearch() {
        val query = binding.schoolSearch.text?.toString()?.trim() ?: ""
        if (query.length >= 2) {
            viewModel.searchSchoolsLocally(query)
            binding.schoolResultsRecyclerView.visibility = View.VISIBLE
            binding.noResultsText.visibility = View.GONE
            scrollToSearchField()
        }
    }

    private fun selectSchool(school: School) {
        viewModel.selectSchool(school)

        isProgrammaticTextChange = true
        binding.schoolSearch.setText(school.name)
        isProgrammaticTextChange = false

        binding.schoolSearch.clearFocus()
        hideKeyboard()

        binding.schoolResultsRecyclerView.visibility = View.GONE
        binding.noResultsText.visibility = View.GONE
        isSearching = false

        // Show selected school card
        binding.selectedSchoolCard.visibility = View.VISIBLE
        binding.selectedSchoolName.text = school.name

        // Load logo
        if (!school.logoUrl.isNullOrEmpty()) {
            Glide.with(requireContext())
                .load(school.logoUrl)
                .placeholder(R.drawable.ic_school_placeholder)
                .error(R.drawable.ic_school_placeholder)
                .circleCrop()
                .into(binding.schoolLogo)
            binding.schoolLogo.visibility = View.VISIBLE
        } else {
            binding.schoolLogo.setImageResource(R.drawable.ic_school_placeholder)
            binding.schoolLogo.visibility = View.VISIBLE
        }

        binding.doneButton.isEnabled = true
        updateDots()

        binding.selectedSchoolCard.post {
            val scrollView = binding.root as? ScrollView
            scrollView?.smoothScrollTo(0, binding.selectedSchoolCard.top - 80)
        }
    }

    private fun updateDots() {
        binding.dotProvince.setImageResource(R.drawable.dot_active)

        val isSchoolSelected = viewModel.selectedSchool.value != null
        binding.dotSchool.setImageResource(
            if (isSchoolSelected) R.drawable.dot_active else R.drawable.dot_inactive
        )

        binding.lineProvinceSchool.setBackgroundColor(
            ContextCompat.getColor(requireContext(), R.color.black)
        )
    }

    private fun scrollToSearchField() {
        val scrollView = binding.root as? ScrollView ?: return
        binding.schoolSearch.postDelayed({
            val targetY = binding.schoolSearchLayout.top - 50
            scrollView.smoothScrollTo(0, targetY.coerceAtLeast(0))
            Timber.d("📜 Scrolled to search field")
        }, 100)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.schools.collect { schools ->
                        if (schools.isEmpty() && binding.schoolSearch.text?.isNotEmpty() == true) {
                            val query = binding.schoolSearch.text?.toString()?.trim() ?: ""
                            if (query.length >= 2) {
                                binding.noResultsText.visibility = View.VISIBLE
                                binding.noResultsText.text = "No schools found matching '$query'"
                                binding.schoolResultsRecyclerView.visibility = View.GONE
                            }
                        } else if (schools.isNotEmpty()) {
                            binding.noResultsText.visibility = View.GONE
                            binding.schoolResultsRecyclerView.visibility = View.VISIBLE
                            schoolAdapter.submitList(schools)
                            scrollToSearchField()
                        }
                    }
                }

                launch {
                    viewModel.isLoadingSchools.collect { isLoading ->
                        binding.schoolProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                        if (!isLoading) {
                            binding.schoolSearch.requestFocus()
                            showKeyboard()
                        }
                    }
                }

                launch {
                    viewModel.selectedSchool.collect { school ->
                        if (school != null && binding.selectedSchoolCard.visibility == View.GONE) {
                            binding.selectedSchoolCard.visibility = View.VISIBLE
                            binding.selectedSchoolName.text = school.name
                            binding.doneButton.isEnabled = true
                            updateDots()
                        }
                    }
                }

                launch {
                    viewModel.updateSuccess.collect { success ->
                        if (success) {
                            findNavController().navigate(R.id.action_schoolFragment_to_nav_home)
                            viewModel.clearSuccess()
                        }
                    }
                }

                launch {
                    viewModel.error.collect { error ->
                        if (error != null) {
                            Timber.e("❌ Error: $error")
                            viewModel.clearError()
                        }
                    }
                }
            }
        }
    }

    private fun showKeyboard() {
        val imm = requireContext().getSystemService(android.app.Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.schoolSearch, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(android.app.Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
        _binding = null
    }
}