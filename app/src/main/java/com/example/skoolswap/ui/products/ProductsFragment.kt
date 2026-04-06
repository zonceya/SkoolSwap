package com.example.skoolswap.ui.products

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.skoolswap.R
import com.example.skoolswap.databinding.FragmentProductsBinding
import com.example.skoolswap.domain.model.FilterConfig
import com.example.skoolswap.domain.model.FilterGroup
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

        Log.d(TAG, "=== ProductsFragment created ===")
        Log.d(TAG, "sectionType: $sectionType, categoryId: $categoryId")

        setupToolbar(sectionTitle)
        setupRecyclerView()
        setupSortFilterBar()
        setupDrawer()
        setupFilterResultListener()
        setupCategorySelectionListener()
        observeViewModel()
        observeFilterConfig()

        viewModel.loadProducts(sectionType, period, categoryId)
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called - restoring filter state")

        val savedCategoryId = viewModel.getSavedCategoryId()
        if (savedCategoryId != null && savedCategoryId > 0) {
            Log.d(TAG, "onResume: Restoring category filters for ID: $savedCategoryId")
            viewModel.loadFilterConfig(savedCategoryId)
            updateFilterPreview("category", viewModel.getSavedCategoryName() ?: "")
        } else {
            val currentConfig = viewModel.filterConfig.value
            if (currentConfig?.filterGroups?.any { it.id == "gender" || it.id == "type" } == true) {
                Log.d(TAG, "onResume: Have category filters but no saved category, reloading global")
                viewModel.loadGlobalFilterConfig()
            }
        }
    }

    // ==================== UI Setup Methods ====================

    private fun setupToolbar(title: String) {
        binding.sectionTitle.text = title
        binding.backBtn.setOnClickListener { findNavController().popBackStack() }
        binding.searchBtn.setOnClickListener { /* Handle search */ }
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
        binding.dynamicFilterContainer.visibility = View.VISIBLE
        setupPriceSlider()

        binding.resetFilters.setOnClickListener {
            Log.d(TAG, "Reset filters clicked")
            viewModel.resetFilters()
            val priceGroup = viewModel.filterConfig.value?.filterGroups?.find { it.id == "price" }
            binding.priceSlider.setValues(
                priceGroup?.min ?: 0f,
                priceGroup?.max ?: 1000f
            )
            updatePriceDisplay()

            for (i in 0 until binding.dynamicFilterContainer.childCount) {
                val sectionView = binding.dynamicFilterContainer.getChildAt(i)
                val previewView = sectionView.findViewById<TextView>(R.id.filterPreview)
                previewView.text = ""
                previewView.visibility = View.GONE
            }
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
        viewLifecycleOwner.lifecycleScope.launch {
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

    // ==================== Filter Drawer Methods ====================

    private fun rebuildDrawerWithConfig(filterConfig: FilterConfig) {
        Log.d(TAG, "rebuildDrawerWithConfig: ${filterConfig.filterGroups.size} groups")

        binding.dynamicFilterContainer.removeAllViews()

        val hasCategorySpecificFilters = filterConfig.filterGroups.any {
            it.id == "gender" || it.id == "type" || it.id == "brand"
        }
        val selectedCategoryId = viewModel.appliedFilters.value.categoryId
            ?: viewModel.getSavedCategoryId()

        if (hasCategorySpecificFilters && selectedCategoryId != null) {
            val categoryGroup = viewModel.getGlobalFilterGroupById("category")

            // Use try-catch to handle inflation errors
            val categorySection = try {
                layoutInflater.inflate(
                    R.layout.item_filter_section,
                    binding.dynamicFilterContainer,
                    false
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to inflate item_filter_section: ${e.message}")
                return
            }

            val titleView = categorySection.findViewById<TextView>(R.id.filterSectionTitle)
            val previewView = categorySection.findViewById<TextView>(R.id.filterPreview)

            if (titleView == null || previewView == null) {
                Log.e(TAG, "filterSectionTitle or filterPreview not found in layout")
                return
            }

            titleView.text = "Category"

            val categoryName = categoryGroup?.options?.find { it.id == selectedCategoryId }?.name
                ?: viewModel.getSavedCategoryName() ?: "Select Category"
            previewView.text = categoryName
            previewView.visibility = View.VISIBLE

            categorySection.setOnClickListener {
                Log.d(TAG, "Category clicked - opening category selector")
                if (categoryGroup != null) {
                    navigateToCategoryFilterOptions(categoryGroup)
                } else {
                    Log.e(TAG, "Category group not found")
                }
            }

            binding.dynamicFilterContainer.addView(categorySection)
        }

        filterConfig.filterGroups.forEach { group ->
            if (group.id == "price") return@forEach
            if (group.options.isEmpty()) return@forEach

            val sectionView = try {
                layoutInflater.inflate(
                    R.layout.item_filter_section,
                    binding.dynamicFilterContainer,
                    false
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to inflate item_filter_section: ${e.message}")
                return@forEach
            }

            val titleView = sectionView.findViewById<TextView>(R.id.filterSectionTitle)
            val previewView = sectionView.findViewById<TextView>(R.id.filterPreview)

            if (titleView == null || previewView == null) {
                Log.e(TAG, "filterSectionTitle or filterPreview not found in layout")
                return@forEach
            }

            titleView.text = group.name

            sectionView.setOnClickListener {
                navigateToFilterOptions(group.id)
            }

            val selectedValue = getSelectedValueDisplay(group.id)
            if (selectedValue != null) {
                previewView.text = selectedValue
                previewView.visibility = View.VISIBLE
            } else {
                previewView.visibility = View.GONE
            }

            binding.dynamicFilterContainer.addView(sectionView)
        }
    }

    private fun observeFilterConfig() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.filterConfig.collect { filterConfig ->
                if (filterConfig == null) return@collect
                rebuildDrawerWithConfig(filterConfig)
            }
        }
    }

    // ==================== Navigation Methods ====================

    private fun navigateToFilterOptions(groupId: String) {
        Log.d(TAG, "navigateToFilterOptions: groupId = $groupId")

        val filterGroup = viewModel.getFilterGroupById(groupId)
            ?: run {
                Log.e(TAG, "Filter group not found for id: $groupId")
                return
            }

        if (filterGroup.options.isEmpty()) {
            Log.e(TAG, "Filter group $groupId has no options")
            return
        }

        val optionNames = filterGroup.options.map { it.name }.toTypedArray()
        val optionIds = filterGroup.options.map { it.id }.toIntArray()

        val bundle = Bundle().apply {
            putString("FILTER_GROUP_ID", groupId)
            putString("FILTER_TYPE", filterGroup.filterType)
            putStringArray("FILTER_OPTIONS", optionNames)
            putIntArray("FILTER_OPTION_IDS", optionIds)
        }

        findNavController().navigate(R.id.filterOptionsFragment, bundle)
    }

    private fun navigateToCategoryFilterOptions(group: FilterGroup) {
        val optionNames = group.options.map { it.name }.toTypedArray()
        val optionIds = group.options.map { it.id }.toIntArray()

        val bundle = Bundle().apply {
            putString("FILTER_GROUP_ID", group.id)
            putString("FILTER_TYPE", "category")
            putStringArray("FILTER_OPTIONS", optionNames)
            putIntArray("FILTER_OPTION_IDS", optionIds)
        }
        findNavController().navigate(R.id.filterOptionsFragment, bundle)
    }

    // ==================== Filter Result Listeners ====================

    private fun setupFilterResultListener() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                parentFragmentManager.setFragmentResultListener(
                    "filter_request",
                    viewLifecycleOwner
                ) { _, result ->
                    val filterGroupId = result.getString("FILTER_GROUP_ID") ?: return@setFragmentResultListener

                    Log.d(TAG, "Filter result received: groupId=$filterGroupId")

                    when (val selected = result.getSerializable("SELECTED_IDS")) {
                        is Int -> {
                            viewModel.updateFilter(filterGroupId, selected)
                            val name = viewModel.getFilterGroupById(filterGroupId)
                                ?.options?.find { it.id == selected }?.name
                            name?.let { updateFilterPreview(filterGroupId, it) }
                        }
                        is List<*> -> {
                            @Suppress("UNCHECKED_CAST")
                            val list = selected as List<Int>
                            viewModel.updateFilter(filterGroupId, list)
                            list.firstOrNull()?.let { id ->
                                val name = viewModel.getFilterGroupById(filterGroupId)
                                    ?.options?.find { it.id == id }?.name
                                name?.let { updateFilterPreview(filterGroupId, it) }
                            }
                        }
                    }
                    viewModel.applyFilters()
                }
            }
        }
    }

    private fun setupCategorySelectionListener() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                parentFragmentManager.setFragmentResultListener(
                    "category_selected",
                    viewLifecycleOwner
                ) { _, result ->
                    val categoryId = result.getInt("SELECTED_CATEGORY_ID")
                    val categoryName = result.getString("SELECTED_CATEGORY_NAME") ?: ""

                    Log.d(TAG, "Category selected: id=$categoryId, name=$categoryName")

                    if (categoryId > 0) {
                        viewModel.setSavedCategory(categoryId, categoryName)
                        updateFilterPreview("category", categoryName)
                        viewModel.updateFilter("category", categoryId)
                        viewModel.loadFilterConfig(categoryId)
                        viewModel.applyFilters()
                    }
                }
            }
        }
    }

    // ==================== Helper Methods ====================

    private fun updateFilterPreview(groupId: String, selectedValue: String) {
        for (i in 0 until binding.dynamicFilterContainer.childCount) {
            val sectionView = binding.dynamicFilterContainer.getChildAt(i)
            val titleView = sectionView.findViewById<TextView>(R.id.filterSectionTitle)
            val expectedTitle = when (groupId) {
                "category" -> "Category"
                "condition" -> "Condition"
                "gender" -> "Gender"
                "size" -> "Size / Age"
                "type" -> "Type"
                "brand" -> "Brand"
                "color" -> "Color"
                else -> null
            }
            if (titleView.text == expectedTitle) {
                val previewView = sectionView.findViewById<TextView>(R.id.filterPreview)
                previewView.text = selectedValue
                previewView.visibility = View.VISIBLE
                break
            }
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
            "size" -> applied.size?.let { sizeId ->
                viewModel.getFilterGroupById("size")?.options?.find { it.id == sizeId }?.name
            }
            "type" -> applied.type?.firstOrNull()?.let { typeId ->
                viewModel.getFilterGroupById("type")?.options?.find { it.id == typeId }?.name
            }
            "brand" -> applied.brand?.firstOrNull()?.let { brandId ->
                viewModel.getFilterGroupById("brand")?.options?.find { it.id == brandId }?.name
            }
            "color" -> applied.color?.firstOrNull()?.let { colorId ->
                viewModel.getFilterGroupById("color")?.options?.find { it.id == colorId }?.name
            }
            "category" -> applied.categoryId?.let { catId ->
                viewModel.getGlobalFilterGroupById("category")?.options?.find { it.id == catId }?.name
            } ?: viewModel.getSavedCategoryName()
            else -> null
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.products.collect { products ->
                productsAdapter.submitList(products)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                // Show/hide loading indicator if needed
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { error ->
                if (error != null) {
                    Log.e(TAG, "Error: $error")
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}