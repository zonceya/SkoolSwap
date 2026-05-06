package com.example.skoolswap.ui.home

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.example.skoolswap.R
import com.example.skoolswap.databinding.FragmentHomeBinding
import com.example.skoolswap.domain.model.BannerItem
import com.example.skoolswap.domain.model.FilterOption
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.model.homefeed.Section
import com.example.skoolswap.ui.home.adapter.BannerAdapter
import com.example.skoolswap.ui.home.adapter.HomeFeedAdapter
import com.example.skoolswap.ui.shop.ProductAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var homeAdapter: HomeFeedAdapter
    private lateinit var searchResultsAdapter: ProductAdapter
    private lateinit var bannerAdapter: BannerAdapter
    private lateinit var autoScrollHelper: BannerAutoScrollHelper
    private var currentTabId: Int = R.id.tabHome
    internal var searchJob: Job? = null
    private var isInSearchMode = false
    private var currentSearchResults = mutableListOf<Item>()

    // Filter selection variables - ADDED GENDER
    private var selectedGender: String? = null
    private var selectedCondition: String? = null
    private var selectedSize: String? = null
    private var selectedColor: String? = null
    private var selectedBrand: String? = null

    private val bannerItems = listOf(
        BannerItem(imageUrl = "https://cdn.skoolswap.co.za/banners/home_1.jpg"),
        BannerItem(imageUrl = "https://cdn.skoolswap.co.za/banners/home_2.jpg"),
        BannerItem(imageUrl = "https://cdn.skoolswap.co.za/banners/home_3.jpg")
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        val fab = activity?.findViewById<FloatingActionButton>(R.id.fab)
        fab?.visibility = View.VISIBLE

        setupCustomTabs()
        setupBannerSlider()
        setupIndicatorDots()
        setupRecyclerView()
        setupFilterBar()
        setupFilterDrawer()
        setupBackButton()
        observeViewModel()

        if (viewModel.homeFeed.value == null) {
            viewModel.loadHomeFeed()
        }

        return binding.root
    }

    fun performLiveSearch(query: String) {
        Timber.tag("HomeFragment").d("🔍 performLiveSearch called with: $query")
        isInSearchMode = true
        enterSearchMode()
        viewModel.searchItems(query, categoryId = null)
    }

    fun exitSearchMode() {
        isInSearchMode = false
        binding.topTabs.visibility = View.VISIBLE
        binding.filterBar.visibility = View.GONE
        binding.homeRecycler.visibility = View.VISIBLE
        binding.bannerViewPager.visibility = View.VISIBLE
        binding.indicatorDots.visibility = View.VISIBLE
        binding.searchResultsContainer.visibility = View.GONE  // ← ADD THIS
        binding.searchResultsRecycler.visibility = View.GONE
        binding.emptySearchResults.visibility = View.GONE
        selectedGender = null
        selectedCondition = null
        selectedSize = null
        selectedColor = null
        selectedBrand = null
    }

    private fun enterSearchMode() {
        binding.topTabs.visibility = View.GONE
        binding.filterBar.visibility = View.VISIBLE
        binding.homeRecycler.visibility = View.GONE
        binding.bannerViewPager.visibility = View.GONE
        binding.indicatorDots.visibility = View.GONE
        binding.searchResultsContainer.visibility = View.VISIBLE  // ← ADD THIS
        binding.searchResultsRecycler.visibility = View.VISIBLE
        binding.emptySearchResults.visibility = View.GONE
    }

    private fun setupFilterBar() {
        binding.sortBtn.setOnClickListener { showSortMenu() }
        binding.filterBtn.setOnClickListener {
            rebuildLocalFilters()
            binding.drawerLayout.openDrawer(GravityCompat.END)
        }
    }

    private fun showSortMenu() {
        val popup = PopupMenu(requireContext(), binding.sortBtn)
        popup.menuInflater.inflate(R.menu.menu_sort, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.sort_recommended -> sortSearchResults("recommended")
                R.id.sort_newest -> sortSearchResults("newest")
                R.id.sort_price_low -> sortSearchResults("price_low")
                R.id.sort_price_high -> sortSearchResults("price_high")
            }
            binding.sortBtn.text = item.title
            true
        }
        popup.show()
    }

    private fun sortSearchResults(sortType: String) {
        val sorted = when (sortType) {
            "price_low" -> currentSearchResults.sortedBy { it.price }
            "price_high" -> currentSearchResults.sortedByDescending { it.price }
            "newest" -> currentSearchResults.sortedByDescending { it.createdAt }
            else -> currentSearchResults
        }
        currentSearchResults.clear()
        currentSearchResults.addAll(sorted)
        searchResultsAdapter.submitList(currentSearchResults)
    }

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
    }

    private fun setupPriceSlider() {
        binding.priceSlider.setValues(0f, 1000f)
        binding.selectedPriceRange.text = "R0 - R1000"

        binding.priceSlider.addOnChangeListener { slider, _, _ ->
            val values = slider.values
            if (values.size >= 2) {
                binding.selectedPriceRange.text = "R${values[0].toInt()} - R${values[1].toInt()}"
            }
        }
    }

    private fun rebuildLocalFilters() {
        binding.dynamicFilterContainer.removeAllViews()

        // Get unique values from current search results

        val conditions = currentSearchResults.mapNotNull { it.conditionName }.distinct()
        val sizes = currentSearchResults.mapNotNull { it.sizeName }.distinct()
        val colors = currentSearchResults.mapNotNull { it.colorName }.distinct()
        val brands = currentSearchResults.mapNotNull { it.brandName }.distinct()

        // Add Gender filter
        val genders = currentSearchResults.mapNotNull { item ->
            when (item.genderId) {
                42 -> "Boys"
                43 -> "Girls"
                27 -> "Unisex"
                in 1..26 -> if (item.genderId?.rem(2) == 0) "Girls" else "Boys"
                else -> item.gender
            }
        }.distinct()

        Log.d("HomeFragment", "Genders found: $genders")

        if (genders.isNotEmpty()) {
            addFilterItem("Gender", selectedGender) {
                showOptionsDrawer("gender", "Gender", genders.map { FilterOption(it.hashCode(), it) }, selectedGender)
            }
        }

        // Add Condition filter
        if (conditions.isNotEmpty()) {
            addFilterItem("Condition", selectedCondition) {
                showOptionsDrawer("condition", "Condition", conditions.map { FilterOption(it.hashCode(), it) }, selectedCondition)
            }
        }

        // Add Size filter
        if (sizes.isNotEmpty()) {
            addFilterItem("Size", selectedSize) {
                showOptionsDrawer("size", "Size", sizes.map { FilterOption(it.hashCode(), it) }, selectedSize)
            }
        }

        // Add Color filter
        if (colors.isNotEmpty()) {
            addFilterItem("Color", selectedColor) {
                showOptionsDrawer("color", "Color", colors.map { FilterOption(it.hashCode(), it) }, selectedColor)
            }
        }

        // Add Brand filter
        if (brands.isNotEmpty()) {
            addFilterItem("Brand", selectedBrand) {
                showOptionsDrawer("brand", "Brand", brands.map { FilterOption(it.hashCode(), it) }, selectedBrand)
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

    private fun showOptionsDrawer(groupId: String, groupName: String, options: List<FilterOption>, currentSelection: String?) {
        binding.filterHeaderTitle.visibility = View.GONE
        binding.optionsHeader.visibility = View.VISIBLE
        binding.optionsTitle.text = groupName
        binding.dynamicFilterContainer.visibility = View.GONE
        binding.optionsContainer.visibility = View.VISIBLE

        val container = binding.optionsContainer
        container.removeAllViews()

        options.forEach { option ->
            val optionView = layoutInflater.inflate(R.layout.item_filter_option, container, false)
            val textView = optionView.findViewById<TextView>(R.id.optionName)
            val checkIcon = optionView.findViewById<ImageView>(R.id.checkIcon)

            textView.text = option.name
            checkIcon.visibility = if (option.name == currentSelection) View.VISIBLE else View.GONE

            optionView.setOnClickListener {
                when (groupId) {
                    "gender" -> selectedGender = option.name
                    "condition" -> selectedCondition = option.name
                    "size" -> selectedSize = option.name
                    "color" -> selectedColor = option.name
                    "brand" -> selectedBrand = option.name
                }

                applyAllFilters()

                binding.filterHeaderTitle.visibility = View.VISIBLE
                binding.optionsHeader.visibility = View.GONE
                binding.dynamicFilterContainer.visibility = View.VISIBLE
                binding.optionsContainer.visibility = View.GONE

                rebuildLocalFilters()
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

    private fun applyAllFilters() {
        var filtered = currentSearchResults.toList()

        val minPrice = binding.priceSlider.values[0]
        val maxPrice = binding.priceSlider.values[1]
        filtered = filtered.filter { it.price in minPrice..maxPrice }

        // Apply gender filter
        if (selectedGender != null) {
            filtered = filtered.filter { item ->
                val itemGender = when (item.genderId) {
                    42 -> "Boys"
                    43 -> "Girls"
                    27 -> "Unisex"
                    in 1..26 -> if (item.genderId?.rem(2) == 0) "Girls" else "Boys"
                    else -> item.gender
                }
                itemGender == selectedGender
            }
        }

        if (selectedCondition != null) {
            filtered = filtered.filter { it.conditionName == selectedCondition }
        }

        if (selectedSize != null) {
            filtered = filtered.filter { it.sizeName == selectedSize }
        }

        if (selectedColor != null) {
            filtered = filtered.filter { it.colorName == selectedColor }
        }

        if (selectedBrand != null) {
            filtered = filtered.filter { it.brandName == selectedBrand }
        }

        currentSearchResults.clear()
        currentSearchResults.addAll(filtered)
        searchResultsAdapter.submitList(currentSearchResults)
    }

    private fun resetLocalFilters() {
        binding.priceSlider.setValues(0f, 1000f)
        binding.selectedPriceRange.text = "R0 - R1000"

        selectedGender = null
        selectedCondition = null
        selectedSize = null
        selectedColor = null
        selectedBrand = null

        rebuildLocalFilters()

        val currentQuery = viewModel.searchQuery.value
        if (currentQuery != null && currentQuery.isNotEmpty()) {
            viewModel.searchItems(currentQuery, null)
        }
    }

    private fun navigateToItemDetail(itemId: String, source: String) {
        val bundle = Bundle().apply {
            putString("itemId", itemId)
            putString("source", source)
        }
        findNavController().navigate(R.id.itemDetailFragment, bundle)
    }

    private fun setupRecyclerView() {
        homeAdapter = HomeFeedAdapter(
            onItemClick = { item, source ->
                navigateToItemDetail(item.id, source)
            },
            onViewAllClick = { sectionType ->
                when (sectionType) {
                    "recommended" -> navigateToProducts("recommended", "Recommended For You", null, null)
                    "essentials" -> navigateToProducts("essentials", "School Essentials", null, null)
                    "trending" -> navigateToProducts("trending", "Trending", "today", null)
                    "recent" -> navigateToProducts("recent", "Recently Added", "all", null)
                }
            }
        )

        binding.homeRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = homeAdapter
        }

        searchResultsAdapter = ProductAdapter { itemId ->
            navigateToItemDetail(itemId, "search")
        }
        binding.searchResultsRecycler.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.searchResultsRecycler.adapter = searchResultsAdapter
    }

    private fun navigateToProducts(
        sectionType: String,
        title: String,
        period: String? = null,
        categoryId: Int? = null
    ) {
        val bundle = Bundle().apply {
            putString("SECTION_TYPE", sectionType)
            putString("SECTION_TITLE", title)
            period?.let { putString("PERIOD", it) }
            if (categoryId != null) {
                putInt("CATEGORY_ID", categoryId)
            }
        }
        findNavController().navigate(R.id.action_homeFragment_to_productsFragment, bundle)
    }

    private fun setupCustomTabs() {
        val tabs = listOf(binding.tabHome, binding.tabUniform, binding.tabSport, binding.tabRecent)
        tabs.forEach { tab ->
            tab.setOnClickListener { selectTab(tab) }
        }
        selectTab(binding.tabHome)
    }

    fun getCurrentCategoryId(): Int? {
        return when (currentTabId) {
            R.id.tabUniform -> 6
            R.id.tabSport -> 7
            else -> null
        }
    }

    private fun selectTab(selectedTab: TextView) {
        currentTabId = selectedTab.id
        val tabs = listOf(binding.tabHome, binding.tabUniform, binding.tabSport, binding.tabRecent)

        tabs.forEach { tab ->
            if (tab == selectedTab) {
                tab.setBackgroundResource(R.drawable.tablayout_selector)
                tab.setTextColor(resources.getColor(android.R.color.white, null))
                tab.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                tab.setBackgroundResource(R.drawable.tablayout_unselected)
                tab.setTextColor(resources.getColor(android.R.color.black, null))
                tab.setTypeface(null, android.graphics.Typeface.NORMAL)
            }
        }

        when (selectedTab.id) {
            R.id.tabHome -> {
                if (viewModel.homeFeed.value == null) {
                    viewModel.loadHomeFeed()
                }
            }
            R.id.tabUniform -> navigateToUniformTab()
            R.id.tabSport -> navigateToSportTab()
            R.id.tabRecent -> navigateToRecentTab()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.homeFeed.collect { feed ->
                feed?.let {
                    homeAdapter.submitList(it.sections)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.searchResults.collect { results ->
                Log.d("HomeFragment", "=== SEARCH RESULTS CALLBACK ===")
                Log.d("HomeFragment", "results.size: ${results.size}")
                Log.d("HomeFragment", "isInSearchMode: $isInSearchMode")
                Log.d("HomeFragment", "isLoadingMore: ${viewModel.isLoadingMore.value}")

                // Print each result
                results.forEachIndexed { index, item ->
                    Log.d("HomeFragment", "Result[$index]: ${item.name}, gender: ${item.gender}")
                }

                if (isInSearchMode) {
                    if (results.isEmpty()) {
                        Log.d("HomeFragment", "Case: Empty results - showing empty state")
                        binding.searchResultsContainer.visibility = View.VISIBLE  // ← ADD THIS
                        binding.emptySearchResults.visibility = View.VISIBLE
                        binding.searchResultsRecycler.visibility = View.GONE
                    } else {
                        Log.d("HomeFragment", "Case: Has ${results.size} results - showing recycler")
                        binding.searchResultsContainer.visibility = View.VISIBLE  // ← ADD THIS
                        binding.emptySearchResults.visibility = View.GONE
                        binding.searchResultsRecycler.visibility = View.VISIBLE
                        currentSearchResults.clear()
                        currentSearchResults.addAll(results)

                        Log.d("HomeFragment", "Calling searchResultsAdapter.submitList with ${results.size} items")
                        searchResultsAdapter.submitList(results)

                        Log.d("HomeFragment", "Calling rebuildLocalFilters")
                        rebuildLocalFilters()
                    }
                }
            }
        }
    }

    private fun setupBannerSlider() {
        bannerAdapter = BannerAdapter(bannerItems)
        binding.bannerViewPager.apply {
            adapter = bannerAdapter
            offscreenPageLimit = 1
            setCurrentItem(Int.MAX_VALUE / 2, false)
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    updateIndicatorDots(position % bannerItems.size)
                }
            })
        }
        autoScrollHelper = BannerAutoScrollHelper(binding.bannerViewPager, 8000)
        autoScrollHelper.startAutoScroll()
    }

    private fun setupIndicatorDots() {
        binding.indicatorDots.removeAllViews()
        bannerItems.forEachIndexed { index, _ ->
            val dot = ImageView(requireContext()).apply {
                setImageResource(if (index == 0) R.drawable.dot_active else R.drawable.dot_inactive)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setPadding(8, 0, 8, 0)
            }
            binding.indicatorDots.addView(dot)
        }
    }

    private fun updateIndicatorDots(currentIndex: Int) {
        for (i in 0 until binding.indicatorDots.childCount) {
            val dot = binding.indicatorDots.getChildAt(i) as ImageView
            dot.setImageResource(if (i == currentIndex) R.drawable.dot_active else R.drawable.dot_inactive)
        }
    }

    override fun onResume() {
        super.onResume()
        autoScrollHelper?.resumeAutoScroll()
        if (viewModel.homeFeed.value == null && !isInSearchMode) {
            viewModel.loadHomeFeed()
        }
    }

    override fun onPause() {
        super.onPause()
        autoScrollHelper?.pauseAutoScroll()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        autoScrollHelper?.stopAutoScroll()
        _binding = null
    }

    private fun navigateToUniformTab() {
        findNavController().navigate(R.id.action_homeFragment_to_uniformFragment)
    }

    private fun navigateToSportTab() {
        findNavController().navigate(R.id.action_homeFragment_to_sportFragment)
    }

    private fun navigateToRecentTab() {
        navigateToProducts("recent", "Recently Added", "all", null)
    }
}