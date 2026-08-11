package za.co.skoolswap.ui.schoolonboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import za.co.skoolswap.R
import za.co.skoolswap.databinding.FragmentSchoolOnboardingProvinceBinding
import za.co.skoolswap.domain.model.Province
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class SchoolOnboardingProvinceFragment : Fragment() {

    private var _binding: FragmentSchoolOnboardingProvinceBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SchoolOnboardingViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSchoolOnboardingProvinceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeViewModel()
        viewModel.loadProvinces()
    }

    private fun setupUI() {
        binding.continueButton.isEnabled = false
        binding.continueButton.setOnClickListener {
            val province = viewModel.selectedProvince.value
            if (province != null) {
                Timber.d("✅ Navigating to School screen with province: ${province.name} (ID: ${province.id})")

                // ✅ Pass province data as arguments
                val bundle = Bundle().apply {
                    putInt("province_id", province.id)
                    putString("province_name", province.name)
                }

                findNavController().navigate(
                    R.id.action_provinceFragment_to_schoolFragment,
                    bundle
                )
            } else {
                binding.errorMessage.visibility = View.VISIBLE
                binding.errorMessage.text = "Please select a province"
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.provinces.collect { provinces ->
                        if (provinces.isNotEmpty()) {
                            setupProvinceSpinner(provinces)
                        }
                    }
                }

                launch {
                    viewModel.selectedProvince.collect { province ->
                        binding.continueButton.isEnabled = province != null
                        if (province != null) {
                            binding.errorMessage.visibility = View.GONE
                            Timber.tag("ProvinceFragment").d("Province selected: ${province.name}")
                        }
                    }
                }

                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.error.collect { error ->
                        if (error != null) {
                            binding.errorMessage.visibility = View.VISIBLE
                            binding.errorMessage.text = error
                            Timber.e("Error: $error")
                            viewModel.clearError()
                        }
                    }
                }
            }
        }
    }

    private fun setupProvinceSpinner(provinces: List<Province>) {
        val provinceNames = provinces.map { it.name }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            provinceNames
        )

        val autoCompleteTextView = binding.provinceSpinner
        autoCompleteTextView.setAdapter(adapter)
        autoCompleteTextView.threshold = 1
        autoCompleteTextView.hint = "Select your province"

        autoCompleteTextView.setOnItemClickListener { _, _, position, _ ->
            val selectedProvince = provinces[position]
            viewModel.selectProvince(selectedProvince)
            // Hide error when user selects
            binding.errorMessage.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}