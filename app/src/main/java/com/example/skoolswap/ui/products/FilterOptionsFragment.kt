package com.example.skoolswap.ui.products

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.skoolswap.R
import com.example.skoolswap.databinding.FragmentFilterOptionsBinding
import com.example.skoolswap.domain.model.FilterGroup
import com.example.skoolswap.domain.model.FilterOption

class FilterOptionsFragment : Fragment() {

    private var _binding: FragmentFilterOptionsBinding? = null
    private val binding get() = _binding!!

    // Shared ViewModel with ProductsFragment
    private val viewModel: ProductsViewModel by activityViewModels()

    private lateinit var filterGroup: FilterGroup
    private val selectedIds = mutableListOf<Int>()

    companion object {
        private const val TAG = "FilterOptionsFragment"
    }

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

        val groupId = arguments?.getString("FILTER_GROUP_ID") ?: run {
            Log.e(TAG, "FILTER_GROUP_ID is missing")
            findNavController().popBackStack()
            return
        }

        val filterType = arguments?.getString("FILTER_TYPE") ?: "single_select"

        val options = arguments?.getStringArray("FILTER_OPTIONS")?.toList() ?: emptyList()
        val optionIds = arguments?.getIntArray("FILTER_OPTION_IDS")?.toList() ?: emptyList()

        Log.d(TAG, "FilterOptionsFragment opened → groupId=$groupId, filterType=$filterType")
        Log.d(TAG, "Options count: ${options.size}, OptionIds count: ${optionIds.size}")

        if (options.isEmpty()) {
            Log.e(TAG, "No options received for group $groupId")
            findNavController().popBackStack()
            return
        }

        filterGroup = FilterGroup(
            id = groupId,
            name = when (groupId) {
                "category" -> "Category"
                else -> viewModel.getFilterGroupById(groupId)?.name ?: groupId
            },
            filterType = filterType,
            options = options.mapIndexed { index, name ->
                FilterOption(
                    id = optionIds.getOrElse(index) { index + 1 },
                    name = name
                )
            }
        )

        setupToolbar()
        loadPreviouslySelected()
        setupOptionsList()
        setupApplyButton()
    }

    private fun setupToolbar() {
        binding.filterTitle.text = filterGroup.name
        binding.backBtn.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun loadPreviouslySelected() {
        val applied = viewModel.appliedFilters.value
        selectedIds.clear()

        Log.d(TAG, "Loading previous selections for ${filterGroup.id}")

        when (filterGroup.id) {
            "condition" -> applied.condition?.let { selectedIds.add(it) }
            "gender" -> applied.gender?.let { selectedIds.add(it) }
            "size" -> applied.size?.let { selectedIds.add(it) }
            "sport_type" -> applied.sportType?.let { selectedIds.add(it) }
            "grade" -> applied.grade?.let { selectedIds.add(it) }
            "type" -> applied.type?.let { selectedIds.addAll(it) }
            "brand" -> applied.brand?.let { selectedIds.addAll(it) }
            "color" -> applied.color?.let { selectedIds.addAll(it) }
            "category" -> applied.categoryId?.let { selectedIds.add(it) }
        }
    }

    private fun setupOptionsList() {
        val optionsLayout = binding.optionsContainer
        optionsLayout.removeAllViews()

        filterGroup.options.forEach { option ->
            val checkBox = CheckBox(requireContext()).apply {
                text = option.name
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(16, 8, 16, 8)
                }

                isChecked = selectedIds.contains(option.id)

                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        if (filterGroup.filterType == "single_select") {
                            selectedIds.clear()
                            selectedIds.add(option.id)
                            updateCheckboxes(optionsLayout, option.id)
                        } else {
                            selectedIds.add(option.id)
                        }
                    } else {
                        selectedIds.remove(option.id)
                    }
                }
            }
            optionsLayout.addView(checkBox)
        }
    }

    private fun updateCheckboxes(layout: LinearLayout, selectedId: Int) {
        for (i in 0 until layout.childCount) {
            val checkBox = layout.getChildAt(i) as? CheckBox
            val optionId = filterGroup.options.getOrNull(i)?.id
            if (optionId != null && optionId != selectedId) {
                checkBox?.isChecked = false
            }
        }
    }

    private fun setupApplyButton() {
        binding.applyBtn.setOnClickListener {
            val result: Any = if (filterGroup.filterType == "single_select" && selectedIds.isNotEmpty()) {
                selectedIds.first()
            } else {
                selectedIds.toList()
            }

            val bundle = Bundle().apply {
                putString("FILTER_GROUP_ID", filterGroup.id)
                putSerializable("SELECTED_IDS", result as java.io.Serializable)
            }
            parentFragmentManager.setFragmentResult("filter_request", bundle)

            // Extra safety for category
            if (filterGroup.id == "category" && selectedIds.isNotEmpty()) {
                val catBundle = Bundle().apply {
                    putInt("SELECTED_CATEGORY_ID", selectedIds.first())
                    putString("SELECTED_CATEGORY_NAME",
                        filterGroup.options.find { it.id == selectedIds.first() }?.name)
                }
                parentFragmentManager.setFragmentResult("category_selected", catBundle)
            }

            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}