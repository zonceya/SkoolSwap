package com.example.skoolswap.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.skoolswap.databinding.FragmentFilterOptionsBinding

class FilterOptionsFragment : Fragment() {

    private var _binding: FragmentFilterOptionsBinding? = null
    private val binding get() = _binding!!

    private lateinit var filterType: String
    private lateinit var options: List<String>
    private val selectedOptions = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFilterOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get arguments from navigation
        filterType = arguments?.getString("FILTER_TYPE") ?: "category"
        options = arguments?.getStringArrayList("OPTIONS") ?: arrayListOf()

        setupToolbar()
        setupOptionsList()
        setupApplyButton()
    }

    private fun setupToolbar() {
        binding.filterTitle.text = filterType.replaceFirstChar { it.uppercase() }
        binding.backBtn.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupOptionsList() {
        val optionsLayout = binding.optionsContainer

        // Clear any existing views
        optionsLayout.removeAllViews()

        // Add options as checkboxes
        options.forEach { option ->
            val checkBox = CheckBox(requireContext()).apply {
                text = option
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(16, 8, 16, 8)
                }

                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedOptions.add(option)
                    } else {
                        selectedOptions.remove(option)
                    }
                }
            }
            optionsLayout.addView(checkBox)
        }
    }

    private fun setupApplyButton() {
        binding.applyBtn.setOnClickListener {
            // Send selected options back to ProductsFragment
            val bundle = Bundle().apply {
                putString("FILTER_TYPE", filterType)
                putStringArrayList("SELECTED_OPTIONS", ArrayList(selectedOptions))
            }

            // Navigate back with result
            parentFragmentManager.setFragmentResult("filter_request", bundle)
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}