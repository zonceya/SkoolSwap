package com.example.skoolswap.ui.products

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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
import com.example.skoolswap.domain.model.FilterOption
import com.example.skoolswap.ui.products.adapter.ProductsAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import timber.log.Timber

@AndroidEntryPoint
class ProductsFragment : Fragment() {

    private var _binding: FragmentProductsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProductsViewModel by activityViewModels()
    private lateinit var productsAdapter: ProductsAdapter

    // Search state
    private var isInSearchMode = false
    private var searchJob: Job? = null

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

        // ✅ SHOW SHIMMER IMMEDIATELY - before any async work
        binding.shimmerLayout.visibility = View.VISIBLE
        binding.productsRecycler.visibility = View.GONE
        binding.errorLayout.visibility = View.GONE

        val sectionType = arguments?.getString("SECTION_TYPE") ?: "all"
        val sectionTitle = arguments?.getString("SECTION_TITLE") ?: "All Items"
        val period = arguments?.getString("PERIOD")
        val categoryId = arguments?.getInt("CATEGORY_ID")
        val sportTypeId = arguments?.getInt("SPORT_TYPE_ID", -1)
        val gearType = arguments?.getString("GEAR_TYPE")

        // Show ActionBar
        (requireActivity() as AppCompatActivity).supportActionBar?.show()
        (requireActivity() as AppCompatActivity).supportActionBar?.title = sectionTitle
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // ✅ Reset first load flag BEFORE loading new section
        // (Even though we're not using it in shimmer condition anymore, keep for other logic)
        viewModel.resetFirstLoadFlag()

        if (sectionType == "sport") {
            viewModel.clearSavedCategory()
        }

        viewModel.setSectionType(sectionType, period, categoryId)

        // Show/hide sort/filter bar
        binding.sortFilterBar.visibility = View.VISIBLE

        binding.retryButton.setOnClickListener {
            viewModel.reloadCurrentSection()
        }

        setupRecyclerView()
        setupSortFilterBar()
        setupDrawer()
        setupBackButton()
        observeViewModel()
        observeFilterConfig()
        debugThemeColors()
        setupSwipeRefresh()

        viewModel.loadProducts(sectionType, period, categoryId, sportTypeId, gearType)
    }

    // ==================== SEARCH METHODS ====================

    fun performLiveSearch(query: String) {
        Log.d(TAG, "🔍 performLiveSearch called with: $query")

        if (query.length >= 2) {
            isInSearchMode = true
            enterSearchMode()

            searchJob?.cancel()
            searchJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(300)
                viewModel.searchInCurrentSection(query)
            }
        } else if (query.isEmpty()) {
            exitSearchMode()
        }
    }

    fun exitSearchMode() {
        isInSearchMode = false
        binding.searchResultsContainer.visibility = View.GONE
        binding.productsRecycler.visibility = View.VISIBLE
        binding.emptySearchResults.visibility = View.GONE

        viewModel.clearSearchResults()
        viewModel.reloadCurrentSection()
    }

    private fun enterSearchMode() {
        binding.searchResultsContainer.visibility = View.VISIBLE
        binding.productsRecycler.visibility = View.GONE
        binding.emptySearchResults.visibility = View.GONE
    }

    // ==================== SETUP METHODS ====================

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.apply {
            setColorSchemeColors(
                ContextCompat.getColor(requireContext(), R.color.teal_200)
            )
            setOnRefreshListener {
                viewModel.reloadCurrentSection()
                isRefreshing = false
            }
        }
    }

    private fun setupRecyclerView() {
        productsAdapter = ProductsAdapter { item ->
            viewModel.trackClick(item.id, arguments?.getString("SECTION_TYPE") ?: "all", 0)
            val bundle = Bundle().apply {
                putString("itemId", item.id)
            }
            findNavController().navigate(R.id.itemDetailFragment, bundle)
        }
        binding.productsRecycler.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = productsAdapter
        }
    }

    private fun setupSortFilterBar() {
        binding.sortContainer.setOnClickListener { showSortMenu() }
        binding.filterContainer.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.END)
        }
    }

    private fun showSortMenu() {
        val popup = PopupMenu(requireContext(), binding.sortContainer)
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
            viewModel.filterConfig.value?.let { rebuildFilterDrawer(it) }
        }

        binding.applyFilters.setOnClickListener {
            val values = binding.priceSlider.values
            if (values.size >= 2) {
                viewModel.updatePriceRange(values[0], values[1])
            }
            viewModel.applyFilters()
            binding.drawerLayout.closeDrawers()
            viewModel.filterConfig.value?.let { rebuildFilterDrawer(it) }
        }
    }

    private fun setupPriceSlider() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.filterConfig.collect { filterConfig ->
                    val priceGroup = filterConfig?.filterGroups?.find { it.id == "price" } ?: return@collect

                    binding.priceSlider.apply {
                        valueFrom = priceGroup.min ?: 0f
                        valueTo = priceGroup.max ?: 1000f

                        val applied = viewModel.appliedFilters.value
                        setValues(
                            applied.minPrice ?: (priceGroup.min ?: 0f),
                            applied.maxPrice ?: (priceGroup.max ?: 1000f)
                        )

                        addOnChangeListener { _, _, _ ->
                            val values = values
                            if (values.size >= 2) {
                                binding.selectedPriceRange.text = "R${values[0].toInt()} - R${values[1].toInt()}"
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updatePriceDisplay() {
        if (_binding == null) return
        val values = binding.priceSlider.values
        if (values.size >= 2) {
            binding.selectedPriceRange.text = "R${values[0].toInt()} - R${values[1].toInt()}"
        }
    }

    private fun observeFilterConfig() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.filterConfig.collect { filterConfig ->
                    if (filterConfig != null && _binding != null) {
                        rebuildFilterDrawer(filterConfig)
                    }
                }
            }
        }
    }

    private fun rebuildFilterDrawer(filterConfig: FilterConfig) {
        if (_binding == null) return

        Log.d("ProductsFragment", "=== rebuildFilterDrawer ===")
        Log.d("ProductsFragment", "filterConfig groups: ${filterConfig.filterGroups.size}")

        binding.dynamicFilterContainer.removeAllViews()

        val applied = viewModel.appliedFilters.value
        val priceDisplay = if (applied.minPrice != null && applied.maxPrice != null)
            "R${applied.minPrice?.toInt() ?: 0} - R${applied.maxPrice?.toInt() ?: 1000}"
        else null

        addFilterItem("Price", priceDisplay) {
            showPriceDialog()
        }

        val selectedCategoryId = viewModel.appliedFilters.value.categoryId ?: viewModel.getSavedCategoryId()
        if (selectedCategoryId != null) {
            val categoryGroup = viewModel.getGlobalFilterGroupById("category")
            val categoryName = categoryGroup?.options?.find { it.id == selectedCategoryId }?.name
                ?: viewModel.getSavedCategoryName()

            addFilterItem("Category", categoryName) {
                categoryGroup?.let { showOptionsDrawer("category", "Category", it.options, "single_select") }
            }
        }

        filterConfig.filterGroups.forEach { group ->
            if (group.id == "price") return@forEach
            if (group.options.isEmpty()) return@forEach

            val selectedValue = getSelectedValueDisplay(group.id)
            Log.d("ProductsFragment", "Adding filter: ${group.name}, selected: $selectedValue")
            addFilterItem(group.name, selectedValue) {
                showOptionsDrawer(group.id, group.name, group.options, group.filterType)
            }
        }
    }

    private fun addFilterItem(title: String, selectedValue: String?, onClick: () -> Unit) {
        if (_binding == null) return
        val itemView = layoutInflater.inflate(R.layout.item_filter_section, binding.dynamicFilterContainer, false)
        val titleView = itemView.findViewById<TextView>(R.id.filterTitle)
        val valueView = itemView.findViewById<TextView>(R.id.filterValue)

        titleView.text = title

        val typedValue = android.util.TypedValue()
        requireContext().theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true)
        val textColor = typedValue.data

        titleView.setTextColor(textColor)

        if (!selectedValue.isNullOrEmpty()) {
            valueView.text = selectedValue
            valueView.visibility = View.VISIBLE
            valueView.setTextColor(textColor)
            valueView.alpha = 0.7f
        } else {
            valueView.visibility = View.GONE
        }

        itemView.setOnClickListener { onClick() }
        binding.dynamicFilterContainer.addView(itemView)
    }

    private fun debugThemeColors() {
        val typedValue = android.util.TypedValue()

        requireContext().theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true)
        Log.d("ProductsFragment", "colorSurface = #${Integer.toHexString(typedValue.data)}")

        requireContext().theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true)
        Log.d("ProductsFragment", "colorOnSurface = #${Integer.toHexString(typedValue.data)}")

        val nightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        Timber.tag("ProductsFragment").d("Dark mode active: ${nightMode == Configuration.UI_MODE_NIGHT_YES}")
    }

    private fun showOptionsDrawer(groupId: String, groupName: String, options: List<FilterOption>, filterType: String) {
        if (_binding == null) return

        binding.filterHeaderTitle.visibility = View.GONE
        binding.optionsHeader.visibility = View.VISIBLE
        binding.optionsTitle.text = groupName
        binding.dynamicFilterContainer.visibility = View.GONE
        binding.optionsContainer.visibility = View.VISIBLE

        val container = binding.optionsContainer
        container.removeAllViews()

        val typedValue = android.util.TypedValue()
        requireContext().theme.resolveAttribute(
            com.google.android.material.R.attr.colorOnSurface, typedValue, true
        )
        val textColor = typedValue.data

        options.forEach { option ->
            val optionView = layoutInflater.inflate(R.layout.item_filter_option, container, false)
            val textView = optionView.findViewById<TextView>(R.id.optionName)
            val checkIcon = optionView.findViewById<ImageView>(R.id.checkIcon)

            textView.text = option.name
            textView.setTextColor(textColor)

            val isSelected = isOptionSelected(groupId, option.id)
            checkIcon.visibility = if (isSelected) View.VISIBLE else View.GONE
            if (isSelected) {
                checkIcon.imageTintList = android.content.res.ColorStateList.valueOf(textColor)
            }

            optionView.setOnClickListener {
                viewModel.updateFilter(groupId, option.id)
                viewModel.applyFilters()
                binding.filterHeaderTitle.visibility = View.VISIBLE
                binding.optionsHeader.visibility = View.GONE
                binding.dynamicFilterContainer.visibility = View.VISIBLE
                binding.optionsContainer.visibility = View.GONE
                viewModel.filterConfig.value?.let { rebuildFilterDrawer(it) }
            }

            container.addView(optionView)
        }
    }

    private fun setupBackButton() {
        binding.backToMain.setOnClickListener {
            binding.filterHeaderTitle.visibility = View.VISIBLE
            binding.optionsHeader.visibility = View.GONE
            binding.dynamicFilterContainer.visibility = View.VISIBLE
            binding.optionsContainer.visibility = View.GONE
        }
    }

    private fun showPriceDialog() {
        val priceGroup = viewModel.filterConfig.value?.filterGroups?.find { it.id == "price" }
        val globalMin = priceGroup?.min ?: 0f
        val globalMax = priceGroup?.max ?: 1000f

        val dialog = PriceRangeDialogFragment.newInstance(
            min = globalMin,
            max = globalMax,
            currentMin = viewModel.appliedFilters.value.minPrice ?: globalMin,
            currentMax = viewModel.appliedFilters.value.maxPrice ?: globalMax
        )

        dialog.setOnPriceRangeAppliedListener { min, max ->
            viewModel.updatePriceRange(min, max)
            viewModel.applyFilters()
        }

        dialog.show(parentFragmentManager, PriceRangeDialogFragment.TAG)
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

    // ==================== OBSERVE VIEWMODEL ====================

    private fun observeViewModel() {
        // ========== COMBINED LOADING STATE - Controls Shimmer ==========
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Collect both loading states and combine them
                viewModel.isLoading.collect { isLoading ->
                    val isNewLoading = viewModel.isNewSectionLoading.value
                    val shouldShowShimmer = isLoading || isNewLoading

                    Log.d("ProductsFragment", "✨ Shimmer state - isLoading: $isLoading, isNewLoading: $isNewLoading, showShimmer: $shouldShowShimmer")

                    if (shouldShowShimmer) {
                        binding.shimmerLayout.visibility = View.VISIBLE
                        binding.productsRecycler.visibility = View.GONE
                        binding.errorLayout.visibility = View.GONE
                    } else {
                        binding.shimmerLayout.visibility = View.GONE
                        binding.swipeRefreshLayout.isRefreshing = false

                        // Show products if we have them
                        val products = viewModel.products.value
                        if (products.isNotEmpty() && !isInSearchMode) {
                            binding.productsRecycler.visibility = View.VISIBLE
                            productsAdapter.submitList(products)
                        }
                    }
                }
            }
        }

        // ========== NEW SECTION LOADING STATE - Only for manual trigger ==========
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isNewSectionLoading.collect { isLoading ->
                    Log.d("ProductsFragment", "📦 isNewSectionLoading: $isLoading")
                    if (!isLoading && !viewModel.isLoading.value) {
                        // Section loading complete AND main loading is done
                        val products = viewModel.products.value
                        if (products.isNotEmpty() && !isInSearchMode) {
                            Log.d("ProductsFragment", "✅ Manually showing ${products.size} products")
                            binding.productsRecycler.visibility = View.VISIBLE
                            productsAdapter.submitList(products)
                        }
                    }
                }
            }
        }

        // ========== ERROR STATE ==========
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.error.collect { error ->
                    if (error != null && viewModel.products.value.isEmpty()) {
                        binding.errorLayout.visibility = View.VISIBLE
                        binding.errorMessage.text = error
                        binding.productsRecycler.visibility = View.GONE
                        binding.shimmerLayout.visibility = View.GONE
                        binding.swipeRefreshLayout.isRefreshing = false
                    } else {
                        binding.errorLayout.visibility = View.GONE
                    }
                }
            }
        }

        // ========== PRODUCTS DATA ==========
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.products.collect { products ->
                    if (_binding == null || isInSearchMode) return@collect

                    Log.d("ProductsFragment", "📦 Products collected: ${products.size} items, isLoading: ${viewModel.isLoading.value}, isNewLoading: ${viewModel.isNewSectionLoading.value}")

                    // Only show products if not loading
                    if (!viewModel.isLoading.value && !viewModel.isNewSectionLoading.value) {
                        if (products.isNotEmpty()) {
                            binding.productsRecycler.visibility = View.VISIBLE
                            binding.errorLayout.visibility = View.GONE
                            productsAdapter.submitList(products)
                            Log.d("ProductsFragment", "✅ Products displayed: ${products.size}")
                        }
                    }
                }
            }
        }

        // ========== SEARCH RESULTS ==========
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.searchResults.collect { results ->
                    if (!isInSearchMode) return@collect

                    if (results.isEmpty()) {
                        binding.searchResultsContainer.visibility = View.VISIBLE
                        binding.emptySearchResults.visibility = View.VISIBLE
                        binding.productsRecycler.visibility = View.GONE
                        productsAdapter.submitList(emptyList())
                    } else {
                        binding.searchResultsContainer.visibility = View.VISIBLE
                        binding.emptySearchResults.visibility = View.GONE
                        binding.productsRecycler.visibility = View.VISIBLE
                        productsAdapter.submitList(results)
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