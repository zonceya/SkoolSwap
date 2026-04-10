package com.example.skoolswap.ui.products

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.skoolswap.R
import com.example.skoolswap.databinding.FragmentProductsBinding
import com.example.skoolswap.domain.model.FilterConfig
import com.example.skoolswap.domain.model.FilterGroup
import com.example.skoolswap.domain.model.FilterOption
import com.example.skoolswap.ui.products.adapter.ProductsAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProductsFragment : Fragment() {

    private var _binding: FragmentProductsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProductsViewModel by activityViewModels()
    private lateinit var productsAdapter: ProductsAdapter

    companion object {
        private const val TAG = "ProductsFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sectionType = arguments?.getString("SECTION_TYPE") ?: "all"
        val sectionTitle = arguments?.getString("SECTION_TITLE") ?: "All Items"
        val period = arguments?.getString("PERIOD")
        val categoryId = arguments?.getInt("CATEGORY_ID")

        setupToolbar(sectionTitle)
        setupRecyclerView()
        setupSortFilterBar()
        setupDrawer()
        setupOptionsDrawer()
        observeViewModel()

        viewModel.loadProducts(sectionType, period, categoryId)

        // Observe filter config to rebuild drawer
        lifecycleScope.launch {
            viewModel.filterConfig.collect { filterConfig ->
                if (filterConfig != null) {
                    rebuildFilterDrawer(filterConfig)
                }
            }
        }
    }

    private fun setupToolbar(title: String) {
        binding.sectionTitle.text = title
        binding.backBtn.setOnClickListener { findNavController().popBackStack() }
        binding.searchBtn.setOnClickListener { }
    }

    private fun setupRecyclerView() {
        productsAdapter = ProductsAdapter { item ->
            viewModel.trackClick(item.id, arguments?.getString("SECTION_TYPE") ?: "all", 0)
        }
        binding.productsRecycler.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = productsAdapter
        }
    }

    private fun setupSortFilterBar() {
        binding.sortBtn.setOnClickListener { showSortMenu() }
        binding.filterBtn.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.END)
        }
    }

    private fun showSortMenu() {
        val popup = PopupMenu(requireContext(), binding.sortBtn)
        popup.menuInflater.inflate(R.menu.menu_sort, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.sort_recommended -> viewModel.sortBy("recommended")
                R.id.sort_newest -> viewModel.sortBy("newest")
                R.id.sort_price_low -> viewModel.sortBy("price_low")
                R.id.sort_price_high -> viewModel.sortBy("price_high")
            }
            binding.sortText.text = item.title
            true
        }
        popup.show()
    }

    private fun setupDrawer() {
        setupPriceSlider()

        binding.resetFilters.setOnClickListener {
            viewModel.resetFilters()
            val priceGroup = viewModel.filterConfig.value?.filterGroups?.find { it.id == "price" }
            binding.priceSlider.setValues(
                priceGroup?.min ?: 0f,
                priceGroup?.max ?: 1000f
            )
            updatePriceDisplay()
            binding.drawerLayout.closeDrawers()
        }

        binding.applyFilters.setOnClickListener {
            val values = binding.priceSlider.values
            if (values.size >= 2) {
                viewModel.updatePriceRange(values[0], values[1])
            }
            viewModel.applyFilters()
            binding.drawerLayout.closeDrawers()
        }
    }

    private fun setupPriceSlider() {
        lifecycleScope.launch {
            viewModel.filterConfig.collect { filterConfig ->
                if (filterConfig == null) return@collect
                val priceGroup = filterConfig.filterGroups.find { it.id == "price" } ?: return@collect

                binding.priceSlider.apply {
                    clearOnChangeListeners()
                    valueFrom = priceGroup.min ?: 0f
                    valueTo = priceGroup.max ?: 1000f
                    setValues(
                        viewModel.appliedFilters.value.minPrice ?: (priceGroup.min ?: 0f),
                        viewModel.appliedFilters.value.maxPrice ?: (priceGroup.max ?: 1000f)
                    )
                    addOnChangeListener { slider, _, _ ->
                        val values = slider.values
                        if (values.size >= 2) {
                            binding.selectedPriceRange.text = "R${values[0].toInt()} - R${values[1].toInt()}"
                        }
                    }
                }
                updatePriceDisplay()
            }
        }
    }

    private fun updatePriceDisplay() {
        val values = binding.priceSlider.values
        if (values.size >= 2) {
            binding.selectedPriceRange.text = "R${values[0].toInt()} - R${values[1].toInt()}"
        }
    }

    private fun rebuildFilterDrawer(filterConfig: FilterConfig) {
        binding.dynamicFilterContainer.removeAllViews()

        // Add Category if we have a selected category
        val selectedCategoryId = viewModel.appliedFilters.value.categoryId ?: viewModel.getSavedCategoryId()
        if (selectedCategoryId != null) {
            val categoryGroup = viewModel.getGlobalFilterGroupById("category")
            val categoryName = categoryGroup?.options?.find { it.id == selectedCategoryId }?.name
                ?: viewModel.getSavedCategoryName()

            addFilterItem("Category", categoryName) {
                categoryGroup?.let { showOptionsDrawer("category", "Category", it.options, "single_select") }
            }
        }

        // Add all other filters
        filterConfig.filterGroups.forEach { group ->
            if (group.id == "price") return@forEach
            if (group.options.isEmpty()) return@forEach

            val selectedValue = getSelectedValueDisplay(group.id)
            addFilterItem(group.name, selectedValue) {
                showOptionsDrawer(group.id, group.name, group.options, group.filterType)
            }
        }
    }

    private fun addFilterItem(title: String, selectedValue: String?, onClick: () -> Unit) {
        val itemView = layoutInflater.inflate(R.layout.item_filter_section, binding.dynamicFilterContainer, false)
        val titleView = itemView.findViewById<TextView>(R.id.filterTitle)
        val valueView = itemView.findViewById<TextView>(R.id.filterValue)

        titleView.text = title

        if (!selectedValue.isNullOrEmpty()) {
            valueView.text = selectedValue
            valueView.visibility = View.VISIBLE
        } else {
            valueView.visibility = View.GONE
        }

        itemView.setOnClickListener { onClick() }
        binding.dynamicFilterContainer.addView(itemView)
    }

    private fun showOptionsDrawer(groupId: String, groupName: String, options: List<FilterOption>, filterType: String) {
        // Hide filter header, show options header
        binding.filterHeaderTitle.visibility = View.GONE
        binding.optionsHeader.visibility = View.VISIBLE
        binding.optionsTitle.text = groupName

        // Hide filter list, show options container
        binding.dynamicFilterContainer.visibility = View.GONE
        binding.optionsContainer.visibility = View.VISIBLE

        val container = binding.optionsContainer
        container.removeAllViews()

        options.forEach { option ->
            val optionView = layoutInflater.inflate(R.layout.item_filter_option, container, false)
            val textView = optionView.findViewById<TextView>(R.id.optionName)
            val checkIcon = optionView.findViewById<ImageView>(R.id.checkIcon)

            textView.text = option.name

            val isSelected = isOptionSelected(groupId, option.id)
            checkIcon.visibility = if (isSelected) View.VISIBLE else View.GONE

            optionView.setOnClickListener {
                viewModel.updateFilter(groupId, option.id)
                viewModel.applyFilters()

                // Go back to main filter view
                binding.filterHeaderTitle.visibility = View.VISIBLE
                binding.optionsHeader.visibility = View.GONE
                binding.dynamicFilterContainer.visibility = View.VISIBLE
                binding.optionsContainer.visibility = View.GONE

                // Rebuild to show updated value
                viewModel.filterConfig.value?.let { rebuildFilterDrawer(it) }
            }

            container.addView(optionView)
        }
    }
    private fun setupOptionsDrawer() {
        binding.backToMain.setOnClickListener {
            // Go back to main filter view
            binding.filterHeaderTitle.visibility = View.VISIBLE
            binding.optionsHeader.visibility = View.GONE
            binding.dynamicFilterContainer.visibility = View.VISIBLE
            binding.optionsContainer.visibility = View.GONE
        }
    }

    private fun isOptionSelected(groupId: String, optionId: Int): Boolean {
        val applied = viewModel.appliedFilters.value
        return when (groupId) {
            "condition" -> applied.condition == optionId
            "gender" -> applied.gender == optionId
            "size" -> applied.size == optionId
            "type" -> applied.type?.contains(optionId) == true
            "brand" -> applied.brand?.contains(optionId) == true
            "category" -> applied.categoryId == optionId
            else -> false
        }
    }

    private fun getSelectedValueDisplay(groupId: String): String? {
        val applied = viewModel.appliedFilters.value
        return when (groupId) {
            "condition" -> applied.condition?.let { conditionId ->
                viewModel.getFilterGroupById("condition")?.options?.find { it.id == conditionId }?.name
            }
            "gender" -> applied.gender?.let { genderId ->
                viewModel.getFilterGroupById("gender")?.options?.find { it.id == genderId }?.name
            }
            "type" -> applied.type?.firstOrNull()?.let { typeId ->
                viewModel.getFilterGroupById("type")?.options?.find { it.id == typeId }?.name
            }
            "brand" -> applied.brand?.firstOrNull()?.let { brandId ->
                viewModel.getFilterGroupById("brand")?.options?.find { it.id == brandId }?.name
            }
            else -> null
        }
    }
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.products.collect { products ->
                productsAdapter.submitList(products)
            }
        }
        lifecycleScope.launch {
            viewModel.error.collect { error ->
                if (error != null) Log.e(TAG, "Error: $error")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}