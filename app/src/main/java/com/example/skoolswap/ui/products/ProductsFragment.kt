package com.example.skoolswap.ui.products

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.skoolswap.R
import com.example.skoolswap.databinding.FragmentProductsBinding
import com.example.skoolswap.domain.model.FilterOption
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.ui.products.adapter.ProductsAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class ProductsFragment : Fragment() {

    private var _binding: FragmentProductsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProductsViewModel by activityViewModels()
    private lateinit var productsAdapter: ProductsAdapter

    private var isInSearchMode = false
    private var searchJob: Job? = null

    // Original unfiltered base — set once per load/search, never mutated
    private var originalItems = mutableListOf<Item>()

    // Filter state
    private var selectedGender: String? = null
    private var selectedCondition: String? = null
    private var selectedSize: String? = null
    private var selectedColor: String? = null
    private var selectedBrand: String? = null

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

        // Show shimmer initially
        binding.shimmerLayout.visibility = View.VISIBLE
        binding.shimmerLayout.startShimmer()
        binding.productsRecycler.visibility = View.GONE
        binding.errorLayout.visibility = View.GONE

        val rawSectionType = arguments?.getString("SECTION_TYPE") ?: "all"
        val sectionType = if (rawSectionType == "sports") "sports" else rawSectionType
        val sectionTitle = arguments?.getString("SECTION_TITLE") ?: "All Items"
        val period = arguments?.getString("PERIOD")
        val categoryId = arguments?.getInt("CATEGORY_ID")
        val sportTypeId = arguments?.getInt("SPORT_TYPE_ID", -1)
        val gearType = arguments?.getString("GEAR_TYPE")

        Timber.tag("ProductsViewModel").d("Using endpoint for section: $sectionType")
        val subCategoryName = arguments?.getString("SUB_CATEGORY_NAME")   // ← Add this
        val mainCategoryName = arguments?.getString("MAIN_CATEGORY_NAME") // ← Add this

        Timber.tag(TAG).d("📌 subCategoryName: $subCategoryName")
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            show()
            title = sectionTitle
            setDisplayHomeAsUpEnabled(true)
        }
        val subCategoryId = arguments?.getInt("SUB_CATEGORY_ID")
        Log.d(TAG, "📌 subCategoryId from arguments: $subCategoryId")
        viewModel.setSubCategoryId(subCategoryId)
        viewModel.resetFirstLoadFlag()
        if (sectionType == "sport") viewModel.clearSavedCategory()
        viewModel.setSectionType(sectionType, period, categoryId)

        binding.sortFilterBar.visibility = View.VISIBLE

        setupRecyclerView()
        setupSortFilterBar()
        setupFilterDrawer()
        setupSwipeRefresh()
        observeViewModel()

        viewModel.loadProducts(
            sectionType = sectionType,
            period = period,
            navCategoryId = categoryId,
            subCategoryId = subCategoryId,
            preSelectedSportTypeId = sportTypeId,
            preSelectedGearType = gearType
        )
        // After viewModel.loadProducts(...)

        if (subCategoryId != null && subCategoryId > 0) {
            // Just ensure the ViewModel knows the subCategory again
            viewModel.setSubCategoryId(subCategoryId)

            // Optional: Small delay only to rebuild filters, NOT reload data
            viewLifecycleOwner.lifecycleScope.launch {
                delay(300)
                if (_binding != null) {
                    viewModel.reloadLocalFiltersOnly()   // We'll create this
                }
            }
        }
    }

    // ====================== SEARCH ======================

    // ProductsFragment.kt - Update performLiveSearch

    fun performLiveSearch(query: String) {
        Timber.tag(TAG).d("🔍 performLiveSearch called with: $query")
        if (query.length >= 2) {
            isInSearchMode = true
            enterSearchMode()
            searchJob?.cancel()
            searchJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(300)
                // 🔥 Use ranked search instead of local search
                viewModel.searchItemsRanked(
                    query = query,
                    categoryId = arguments?.getInt("CATEGORY_ID")
                )
            }
        } else if (query.isEmpty()) {
            exitSearchMode()
        }
    }

    private fun enterSearchMode() {
        binding.searchResultsContainer.visibility = View.VISIBLE
        binding.productsRecycler.visibility = View.GONE
        binding.emptySearchResults.visibility = View.GONE
        // Hide shimmer during search
        binding.shimmerLayout.visibility = View.GONE
        binding.shimmerLayout.stopShimmer()
    }

    fun exitSearchMode() {
        isInSearchMode = false
        binding.searchResultsContainer.visibility = View.GONE
        binding.productsRecycler.visibility = View.VISIBLE
        binding.emptySearchResults.visibility = View.GONE
        clearFilterState()
        viewModel.clearSearchResults()
        viewModel.reloadCurrentSection()
    }

    // ====================== FILTER CORE ======================

    private fun getOptionsFor(groupId: String): List<String> {
        var items = originalItems.toList()
        if (groupId != "gender" && selectedGender != null)
            items = items.filter { getGenderLabel(it) == selectedGender }
        if (groupId != "condition" && selectedCondition != null)
            items = items.filter { it.conditionName == selectedCondition }
        if (groupId != "size" && selectedSize != null)
            items = items.filter { it.sizeName == selectedSize }
        if (groupId != "color" && selectedColor != null)
            items = items.filter { it.colorName == selectedColor }
        if (groupId != "brand" && selectedBrand != null)
            items = items.filter { it.brandName == selectedBrand }

        return when (groupId) {
            "gender" -> items.mapNotNull { getGenderLabel(it) }.distinct().sorted()
            "condition" -> items.mapNotNull { it.conditionName }.distinct().sorted()
            "size" -> items.mapNotNull { it.sizeName }.distinct().sorted()
            "color" -> items.mapNotNull { it.colorName }.distinct().sorted()
            "brand" -> items.mapNotNull { it.brandName }.distinct().sorted()
            else -> emptyList()
        }
    }

    private fun getSelection(groupId: String): String? = when (groupId) {
        "gender" -> selectedGender
        "condition" -> selectedCondition
        "size" -> selectedSize
        "color" -> selectedColor
        "brand" -> selectedBrand
        else -> null
    }

    private fun getGenderLabel(item: Item): String? = when (item.genderId) {
        42 -> "Boys"
        43 -> "Girls"
        27 -> "Unisex"
        in 1..26 -> if (item.genderId?.rem(2) == 0) "Girls" else "Boys"
        else -> item.gender?.takeIf { it.isNotBlank() }
    }

    // ====================== applyAllFilters with price ======================

    private fun applyAllFilters() {
        Timber.tag(TAG).d("🔧 applyAllFilters() called - originalItems size: ${originalItems.size}")

        var filtered = originalItems.toList()

        // ✅ Price filter from slider
        val minPrice = binding.priceSlider.values[0]
        val maxPrice = binding.priceSlider.values[1]

        Timber.tag(TAG).d("💰 Price filter: R${minPrice.toInt()} - R${maxPrice.toInt()}")
        filtered = filtered.filter { it.price in minPrice..maxPrice }
        Timber.tag(TAG).d("💰 After price filter: ${filtered.size} items")

        // Other filters
        if (selectedGender != null) filtered = filtered.filter { getGenderLabel(it) == selectedGender }
        if (selectedCondition != null) filtered = filtered.filter { it.conditionName == selectedCondition }
        if (selectedSize != null) filtered = filtered.filter { it.sizeName == selectedSize }
        if (selectedColor != null) filtered = filtered.filter { it.colorName == selectedColor }
        if (selectedBrand != null) filtered = filtered.filter { it.brandName == selectedBrand }

        Timber.tag(TAG).d("🔧 After all filters: ${filtered.size} items")
        productsAdapter.submitList(filtered)
    }

    private fun clearFilterState() {
        selectedGender = null
        selectedCondition = null
        selectedSize = null
        selectedColor = null
        selectedBrand = null
    }

    // ====================== resetLocalFilters ======================

    private fun resetLocalFilters() {
        // ✅ Reset price slider
        binding.priceSlider.setValues(0f, 1000f)
        binding.selectedPriceRange.text = "R0 - R1000"

        clearFilterState()
        productsAdapter.submitList(originalItems.toList())
        rebuildLocalFilters()
    }

    // ====================== FILTER DRAWER ======================

    private fun setupFilterDrawer() {
        setupPriceSlider()

        binding.resetFilters.setOnClickListener {
            resetLocalFilters()
            binding.drawerLayout.closeDrawers()
        }

        binding.applyFilters.setOnClickListener {
            applyAllFilters()
            binding.drawerLayout.closeDrawers()
        }

        binding.backToMain.setOnClickListener {
            binding.filterHeaderTitle.visibility = View.VISIBLE
            binding.optionsHeader.visibility = View.GONE
            binding.dynamicFilterContainer.visibility = View.VISIBLE
            binding.optionsContainer.visibility = View.GONE
        }
    }

    // ====================== setupPriceSlider ======================

    private fun setupPriceSlider() {
        binding.priceSlider.setValues(0f, 1000f)
        binding.selectedPriceRange.text = "R0 - R1000"
        binding.priceSlider.addOnChangeListener { slider, _, _ ->
            val values = slider.values
            if (values.size >= 2) {
                binding.selectedPriceRange.text = "R${values[0].toInt()} - R${values[1].toInt()}"
                // ✅ Live filter when slider changes
                applyAllFilters()
            }
        }
    }

    private fun setupSortFilterBar() {
        binding.sortContainer.setOnClickListener { showSortMenu() }
        binding.filterContainer.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                delay(80)
                binding.filterHeaderTitle.visibility = View.VISIBLE
                binding.optionsHeader.visibility = View.GONE
                binding.dynamicFilterContainer.visibility = View.VISIBLE
                binding.optionsContainer.visibility = View.GONE
                // ✅ Rebuild filters when drawer opens
                rebuildLocalFilters()
                binding.drawerLayout.openDrawer(GravityCompat.END)
            }
        }
    }

    // ====================== rebuildLocalFilters ======================

    private fun rebuildLocalFilters() {
        if (_binding == null) return
        binding.dynamicFilterContainer.removeAllViews()

        if (originalItems.isEmpty()) {
            Timber.tag(TAG).d("⚠️ originalItems is EMPTY, showing 'No items available'")
            addEmptyFilterMessage("No items available")
            return
        }

        val genders = getOptionsFor("gender")
        val conditions = getOptionsFor("condition")
        val sizes = getOptionsFor("size")
        val colors = getOptionsFor("color")
        val brands = getOptionsFor("brand")

        Timber.tag(TAG)
            .d("🔧 Filter counts - genders: ${genders.size}, conditions: ${conditions.size}, sizes: ${sizes.size}, colors: ${colors.size}, brands: ${brands.size}")

        if (genders.isNotEmpty()) {
            addFilterItem("Gender", selectedGender) {
                showOptionsDrawer("gender", "Gender",
                    genders.map { FilterOption(it.hashCode(), it) }, selectedGender)
            }
        }
        if (conditions.isNotEmpty()) {
            addFilterItem("Condition", selectedCondition) {
                showOptionsDrawer("condition", "Condition",
                    conditions.map { FilterOption(it.hashCode(), it) }, selectedCondition)
            }
        }
        if (sizes.isNotEmpty()) {
            addFilterItem("Size", selectedSize) {
                showOptionsDrawer("size", "Size",
                    sizes.map { FilterOption(it.hashCode(), it) }, selectedSize)
            }
        }
        if (colors.isNotEmpty()) {
            addFilterItem("Color", selectedColor) {
                showOptionsDrawer("color", "Color",
                    colors.map { FilterOption(it.hashCode(), it) }, selectedColor)
            }
        }
        if (brands.isNotEmpty()) {
            addFilterItem("Brand", selectedBrand) {
                showOptionsDrawer("brand", "Brand",
                    brands.map { FilterOption(it.hashCode(), it) }, selectedBrand)
            }
        }

        if (genders.isEmpty() && conditions.isEmpty() && sizes.isEmpty()
            && colors.isEmpty() && brands.isEmpty()) {
            Timber.tag(TAG).d("⚠️ All filters are EMPTY, showing 'No filters available'")
            addEmptyFilterMessage("No filters available")
        }
    }

    private fun addFilterItem(title: String, selectedValue: String?, onClick: () -> Unit) {
        val itemView = layoutInflater.inflate(
            R.layout.item_filter_section, binding.dynamicFilterContainer, false)
        val titleView = itemView.findViewById<TextView>(R.id.filterTitle)
        val valueView = itemView.findViewById<TextView>(R.id.filterValue)
        val filterRow = itemView.findViewById<View>(R.id.filterRow)

        titleView.text = title
        if (!selectedValue.isNullOrEmpty()) {
            valueView.text = selectedValue
            valueView.visibility = View.VISIBLE
        } else {
            valueView.visibility = View.GONE
        }

        filterRow.setOnClickListener { onClick() }
        binding.dynamicFilterContainer.addView(itemView)
    }

    private fun showOptionsDrawer(
        groupId: String,
        groupName: String,
        options: List<FilterOption>,
        currentSelection: String?
    ) {
        binding.filterHeaderTitle.visibility = View.GONE
        binding.optionsHeader.visibility = View.VISIBLE
        binding.optionsTitle.text = groupName
        binding.dynamicFilterContainer.visibility = View.GONE
        binding.optionsContainer.visibility = View.VISIBLE

        val container = binding.optionsContainer
        container.removeAllViews()

        val sortedOptions = options.sortedBy { if (it.name == currentSelection) 0 else 1 }

        sortedOptions.forEach { option ->
            val optionView = layoutInflater.inflate(R.layout.item_filter_option, container, false)
            val textView = optionView.findViewById<TextView>(R.id.optionName)
            val checkIcon = optionView.findViewById<ImageView>(R.id.checkIcon)

            textView.text = option.name
            checkIcon.visibility = if (option.name == currentSelection) View.VISIBLE else View.GONE

            optionView.setOnClickListener {
                when (groupId) {
                    "gender" -> {
                        selectedGender = if (selectedGender == option.name) null else option.name
                        if (selectedGender != null) {
                            selectedCondition = null
                            selectedSize = null
                            selectedColor = null
                            selectedBrand = null
                        }
                    }
                    "condition" -> selectedCondition =
                        if (selectedCondition == option.name) null else option.name
                    "size" -> selectedSize =
                        if (selectedSize == option.name) null else option.name
                    "color" -> selectedColor =
                        if (selectedColor == option.name) null else option.name
                    "brand" -> selectedBrand =
                        if (selectedBrand == option.name) null else option.name
                }

                applyAllFilters()
                showOptionsDrawer(
                    groupId, groupName,
                    getOptionsFor(groupId).map { FilterOption(it.hashCode(), it) },
                    getSelection(groupId)
                )
            }

            container.addView(optionView)
        }
    }

    private fun addEmptyFilterMessage(msg: String) {
        val tv = TextView(requireContext()).apply {
            text = msg
            setPadding(32, 48, 32, 48)
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(requireContext(), R.color.darker_grey_light))
        }
        binding.dynamicFilterContainer.addView(tv)
    }

    // ====================== RECYCLER ======================

    private fun setupRecyclerView() {
        productsAdapter = ProductsAdapter { item ->
            viewModel.trackClick(item.id, arguments?.getString("SECTION_TYPE") ?: "all", 0)
            val bundle = Bundle().apply { putString("itemId", item.id) }
            findNavController().navigate(R.id.itemDetailFragment, bundle)
        }
        binding.productsRecycler.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = productsAdapter

            // ✅ Auto-hide banner when user starts scrolling
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                        binding.emptyBanner.visibility = View.GONE
                    }
                }
            })
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

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.apply {
            setColorSchemeColors(ContextCompat.getColor(requireContext(), R.color.teal_200))
            setOnRefreshListener {
                viewModel.reloadCurrentSection()
                isRefreshing = false
            }
        }
    }

    // ====================== COMBINED OBSERVERS ======================

    private fun observeViewModel() {
        // 1. Products observer - SINGLE SOURCE OF TRUTH for data and error states
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.products.collect { products ->
                    if (isInSearchMode || _binding == null) return@collect

                    if (products.isEmpty() && (viewModel.isLoading.value || viewModel.isNewSectionLoading.value)) {
                        Timber.tag(TAG).d("⏳ Ignoring empty products list (still loading)")
                        return@collect
                    }

                    originalItems.clear()
                    originalItems.addAll(products)
                    clearFilterState()

                    binding.swipeRefreshLayout.isRefreshing = false

                    val subCategoryName = arguments?.getString("SUB_CATEGORY_NAME")
                        ?: arguments?.getString("SECTION_TITLE") ?: "this category"

                    if (products.isNotEmpty()) {
                        binding.shimmerLayout.visibility = View.GONE
                        binding.shimmerLayout.stopShimmer()
                        binding.productsRecycler.visibility = View.VISIBLE
                        binding.errorLayout.visibility = View.GONE

                        productsAdapter.submitList(products.toList())
                        rebuildLocalFilters()

                        // ✅ Check if we're in fallback mode (sub-category requested but no matching items)
                        val subCategoryId = arguments?.getInt("SUB_CATEGORY_ID")
                        val hasSubCategoryFilter = subCategoryId != null && subCategoryId > 0
                        val hasSubCategoryItems = if (hasSubCategoryFilter) {
                            products.any { it.subCategoryId == subCategoryId }
                        } else {
                            false
                        }

                        if (hasSubCategoryFilter && !hasSubCategoryItems) {
                            Timber.tag(TAG).d("🔔 Showing fallback banner for '$subCategoryName'")
                            binding.bannerMessage.text = "No items found for '$subCategoryName'. Similar items are displayed."
                            binding.emptyBanner.visibility = View.VISIBLE

                            // ✅ ONLY close button dismisses banner (NO auto-hide)
                            binding.btnCloseBanner.setOnClickListener {
                                binding.emptyBanner.visibility = View.GONE
                            }
                        } else {
                            // ✅ Hide banner when items match the sub-category
                            binding.emptyBanner.visibility = View.GONE
                        }

                        Timber.tag(TAG).d("✅ Products displayed: ${products.size} items")

                        // Debug logging
                        if (subCategoryId != null && subCategoryId > 0) {
                            val matchingCount = products.count { it.subCategoryId == subCategoryId }
                            Timber.tag(TAG).d("🔍 Sub-category $subCategoryId: $matchingCount/${products.size} items match")
                        }
                    } else {
                        // ✅ Truly empty - show error layout
                        binding.shimmerLayout.visibility = View.GONE
                        binding.shimmerLayout.stopShimmer()
                        binding.productsRecycler.visibility = View.GONE
                        binding.emptyBanner.visibility = View.GONE
                        binding.errorLayout.visibility = View.VISIBLE
                        binding.errorText.text = "No items found for $subCategoryName"
                    }
                }
            }
        }

        // 2. Search results observer
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.searchResults.collect { results ->
                    if (!isInSearchMode || _binding == null) return@collect

                    // ✅ IGNORE transient empty list during loading
                    if (results.isEmpty() && (viewModel.isLoading.value || viewModel.isNewSectionLoading.value)) {
                        Timber.tag(TAG).d("⏳ Ignoring empty search results (still loading)")
                        return@collect
                    }

                    Timber.tag(TAG).d("🔍 Search results collector received ${results.size} items")

                    originalItems.clear()
                    originalItems.addAll(results)
                    clearFilterState()

                    // ✅ Always stop swipe refresh for search
                    binding.swipeRefreshLayout.isRefreshing = false

                    if (results.isEmpty()) {
                        // ✅ Show empty search state, hide shimmer
                        binding.shimmerLayout.visibility = View.GONE
                        binding.shimmerLayout.stopShimmer()
                        binding.emptySearchResults.visibility = View.VISIBLE
                        binding.searchResultsContainer.visibility = View.VISIBLE
                        binding.productsRecycler.visibility = View.GONE
                    } else {
                        // ✅ Show search results, hide shimmer
                        binding.shimmerLayout.visibility = View.GONE
                        binding.shimmerLayout.stopShimmer()
                        binding.emptySearchResults.visibility = View.GONE
                        binding.searchResultsContainer.visibility = View.VISIBLE
                        binding.productsRecycler.visibility = View.VISIBLE
                        productsAdapter.submitList(results.toList())

                        // ✅ CRITICAL: Rebuild filters when search results load
                        Timber.tag(TAG).d("🔧 Calling rebuildLocalFilters() from search collector")
                        rebuildLocalFilters()

                        Timber.tag(TAG).d("✅ Search results displayed: ${results.size} items")
                    }
                }
            }
        }

        // 3. Loading state observer - CONTROLS SHIMMER ONLY, NEVER SHOWS ERROR
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    if (_binding == null) return@collect
                    val isNew = viewModel.isNewSectionLoading.value

                    Timber.tag(TAG)
                        .d("🔄 Loading state: isLoading=$isLoading, isNew=$isNew, itemCount=${productsAdapter.itemCount}")

                    if (isLoading || isNew) {
                        // Only show shimmer if we don't have data yet AND not in search mode
                        if (productsAdapter.itemCount == 0 && !isInSearchMode) {
                            Timber.tag(TAG).d("✨ Showing shimmer (no data yet)")
                            binding.shimmerLayout.visibility = View.VISIBLE
                            binding.shimmerLayout.startShimmer()
                            binding.productsRecycler.visibility = View.GONE
                            binding.errorLayout.visibility = View.GONE
                        }
                    }
                    // ❌ REMOVED else branch that shows error - let products collector handle it
                }
            }
        }

        // 4. Error state observer
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.error.collect { errorMsg ->
                    if (_binding == null) return@collect

                    Log.d(TAG, "❌ Error state: errorMsg=$errorMsg, itemCount=${productsAdapter.itemCount}")

                    // ✅ Always stop swipe refresh on error
                    binding.swipeRefreshLayout.isRefreshing = false

                    if (!errorMsg.isNullOrEmpty() && productsAdapter.itemCount == 0) {
                        binding.shimmerLayout.visibility = View.GONE
                        binding.shimmerLayout.stopShimmer()
                        binding.productsRecycler.visibility = View.GONE
                        binding.errorLayout.visibility = View.VISIBLE
                        binding.errorText.text = errorMsg
                    } else {
                        binding.errorLayout.visibility = View.GONE
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}