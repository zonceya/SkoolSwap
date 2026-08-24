package za.co.skoolswap.ui.schoolonboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import za.co.skoolswap.R
import za.co.skoolswap.databinding.FragmentSchoolOnboardingProvinceBinding
import za.co.skoolswap.domain.model.Province
import za.co.skoolswap.ui.component.ProvincePickerBottomSheet
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

        // Province Spinner click - open bottom sheet
        binding.provinceSpinner.setOnClickListener {
            val provinces = viewModel.provinces.value
            if (provinces.isNotEmpty()) {
                showProvincePicker(provinces)
            } else {
                Toast.makeText(requireContext(), "Loading provinces...", Toast.LENGTH_SHORT).show()
            }
        }

        binding.continueButton.setOnClickListener {
            val province = viewModel.selectedProvince.value
            if (province != null) {
                Timber.d("✅ Navigating to School screen with province: ${province.name} (ID: ${province.id})")

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

    private fun showProvincePicker(provinces: List<Province>) {
        val selectedProvince = viewModel.selectedProvince.value
        val bottomSheet = ProvincePickerBottomSheet(
            provinces = provinces,
            selectedProvinceId = selectedProvince?.id,
            onProvinceSelected = { province ->
                viewModel.selectProvince(province)
                binding.provinceSpinner.setText(province.name, false)
                binding.errorMessage.visibility = View.GONE
                Timber.tag("ProvinceFragment").d("Province selected: ${province.name}")
            }
        )
        bottomSheet.show(parentFragmentManager, "ProvincePickerBottomSheet")
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.provinces.collect { provinces ->
                        if (provinces.isNotEmpty()) {
                            // Update the spinner text if a province is selected
                            viewModel.selectedProvince.value?.let { province ->
                                binding.provinceSpinner.setText(province.name, false)
                            }
                        }
                    }
                }

                launch {
                    viewModel.selectedProvince.collect { province ->
                        binding.continueButton.isEnabled = province != null
                        if (province != null) {
                            binding.errorMessage.visibility = View.GONE
                            binding.provinceSpinner.setText(province.name, false)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}