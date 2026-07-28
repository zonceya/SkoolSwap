package com.example.skoolswap.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.skoolswap.R
import com.example.skoolswap.common.constants.AppConstants
import com.example.skoolswap.common.constants.AppConstants.LogTags
import com.example.skoolswap.data.local.datastore.AppPreferences
import com.example.skoolswap.databinding.FragmentHomeBinding
import com.example.skoolswap.domain.model.BannerItem
import com.example.skoolswap.domain.model.FilterOption
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.model.homefeed.Section
import com.example.skoolswap.domain.repository.HomeRepositoryInterface
import com.example.skoolswap.domain.repository.UserSchoolRepositoryInterface
import com.example.skoolswap.ui.home.adapter.BannerAdapter
import com.example.skoolswap.ui.home.adapter.HomeFeedAdapter
import com.example.skoolswap.ui.shop.CategoryGridAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var homeAdapter: HomeFeedAdapter
    private lateinit var bannerAdapter: BannerAdapter
    private lateinit var autoScrollHelper: BannerAutoScrollHelper
    private lateinit var categorySearchAdapter: CategoryGridAdapter

    private var currentTabId: Int = R.id.tabHome
    internal var searchJob: Job? = null
    private var isInSearchMode = false

    // Search result state — single source of truth
    private var originalSearchResults = mutableListOf<Item>()
    private var currentSearchResults = mutableListOf<Item>()

    // Filter state
    private var selectedGender: String? = null
    private var selectedCondition: String? = null
    private var selectedSize: String? = null
    private var selectedColor: String? = null
    private var selectedBrand: String? = null

    @Inject
    lateinit var appPreferences: AppPreferences
    @Inject
    lateinit var userSchoolRepository: UserSchoolRepositoryInterface
    companion object {
        private const val BANNER_AUTO_SCROLL_INTERVAL_MS = 8000L
        private const val SEARCH_DEBOUNCE_DELAY_MS = 400L
        private const val MIN_SEARCH_LENGTH = 2
        private const val PRICE_SLIDER_MIN = 0f
        private const val PRICE_SLIDER_MAX = 1000f
        private const val GRID_SPAN_COUNT = 2
        private const val TAB_UNIFORM_ID = 6
        private const val TAB_SPORT_ID = 7
        private const val BANNER_INITIAL_POSITION_DIVIDER = 2
        private const val GENDER_BOYS_ID = 42
        private const val GENDER_GIRLS_ID = 43
        private const val GENDER_UNISEX_ID = 27
        private const val GENDER_ODD_ID = 1
        private const val GENDER_EVEN_ID = 26
        private const val GENDER_REM_CHECK = 2
        private const val GENDER_REM_RESULT = 0
    }

    private val bannerItems = listOf(
        BannerItem(imageUrl = "https://cdn.skoolswap.co.za/banners/home_1.jpg"),
        BannerItem(imageUrl = "https://cdn.skoolswap.co.za/banners/home_2.jpg"),
        BannerItem(imageUrl = "https://cdn.skoolswap.co.za/banners/home_3.jpg")
    )
    private val bannerHideListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                binding.emptyBanner.visibility = View.GONE
            }
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        val fab = activity?.findViewById<FloatingActionButton>(R.id.fab)
        fab?.visibility = View.VISIBLE
        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        setupCustomTabs()
        setupBannerSlider()
        setupIndicatorDots()
        setupRecyclerView()
        setupFilterBar()
        setupFilterDrawer()
        setupBackButton()
        observeViewModel()
        setupSwipeRefresh()
        binding.homeRecycler.addOnScrollListener(bannerHideListener)
        binding.searchResultsRecycler.addOnScrollListener(bannerHideListener)
        lifecycleScope.launch {
            val schoolId = appPreferences.schoolId.first()?.takeIf { it > 0 }
            if (schoolId != null) {

                val nearbyIds = userSchoolRepository.getNearbySchoolIds(schoolId)
                viewModel.setNearbySchoolIds(nearbyIds)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val argSchoolId = arguments?.getInt(AppConstants.ARG_SCHOOL_ID, -1)?.takeIf { it > 0 }
            val prefSchoolId = appPreferences.schoolId.first()?.takeIf { it > 0 }
            val schoolId = argSchoolId ?: prefSchoolId
            Timber.tag(LogTags.UI)
                .d("🔑 schoolId: arg=$argSchoolId prefs=$prefSchoolId using=$schoolId")

            if (schoolId != null) {
                viewModel.setKnownSchoolId(schoolId)
            }

            if (viewModel.homeFeed.value == null && !viewModel.isLoading.value) {
                viewModel.loadHomeFeed()
            }
        }

        return binding.root
    }

    fun performLiveSearch(query: String) {
        Timber.tag(LogTags.UI).d("🔍 performLiveSearch: $query")

        searchJob?.cancel()

        if (query.length < MIN_SEARCH_LENGTH) {
            exitSearchMode()
            return
        }

        isInSearchMode = true
        enterSearchMode()
        categorySearchAdapter.submitList(emptyList())
        searchJob = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.searchLocalOnly(query, arguments?.getInt("CATEGORY_ID"))

            delay(SEARCH_DEBOUNCE_DELAY_MS)

            if (query != viewModel.searchQuery.value || !isActive) return@launch

            viewModel.searchServer(query, arguments?.getInt("CATEGORY_ID"))
        }
    }

    private fun enterSearchMode() {
        isInSearchMode = true

        binding.topTabs.visibility = View.GONE
        binding.bannerViewPager.visibility = View.GONE
        binding.indicatorDots.visibility = View.GONE
        binding.homeRecycler.visibility = View.GONE
        binding.swipeRefreshLayout.visibility = View.GONE

        binding.filterBar.visibility = View.VISIBLE
        binding.searchResultsContainer.visibility = View.VISIBLE
        binding.searchResultsRecycler.visibility = View.VISIBLE
        binding.emptySearchResults.visibility = View.GONE

        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
    }

    fun exitSearchMode() {
        isInSearchMode = false

        binding.topTabs.visibility = View.VISIBLE
        binding.bannerViewPager.visibility = View.VISIBLE
        binding.indicatorDots.visibility = View.VISIBLE
        binding.swipeRefreshLayout.visibility = View.VISIBLE
        binding.homeRecycler.visibility = View.VISIBLE

        binding.filterBar.visibility = View.GONE
        binding.searchResultsContainer.visibility = View.GONE
        binding.searchResultsRecycler.visibility = View.GONE
        binding.emptySearchResults.visibility = View.GONE
        binding.emptyBanner.visibility = View.GONE
        categorySearchAdapter.submitList(emptyList())
        selectedGender = null
        selectedCondition = null
        selectedSize = null
        selectedColor = null
        selectedBrand = null

        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        binding.drawerLayout.closeDrawer(GravityCompat.END)

        viewModel.clearSearch()
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

        categorySearchAdapter = CategoryGridAdapter(
            onItemClick = { itemId ->
                navigateToItemDetail(itemId, "search")
            },
            onSoldToggle = null,
            isShopMode = false
        )
        binding.searchResultsRecycler.layoutManager = GridLayoutManager(requireContext(), GRID_SPAN_COUNT)
        binding.searchResultsRecycler.adapter = categorySearchAdapter
    }

    private fun setupFilterBar() {
        binding.sortBtn.setOnClickListener {
            if (isInSearchMode) {
                showSortMenu()
            }
        }

        binding.filterBtn.setOnClickListener {
            if (isInSearchMode) {
                rebuildLocalFilters()
                binding.drawerLayout.openDrawer(GravityCompat.END)
            }
        }

        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        val isDarkMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES

        if (isDarkMode) {
            binding.sortBtn.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.dark_surface))
            binding.filterBtn.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.dark_surface))
            binding.sortBtn.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            binding.filterBtn.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        } else {
            binding.sortBtn.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_background))
            binding.filterBtn.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_background))
            binding.sortBtn.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            binding.filterBtn.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
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
            else -> currentSearchResults.toList()
        }
        currentSearchResults.clear()
        currentSearchResults.addAll(sorted)
        categorySearchAdapter.submitList(currentSearchResults.toList())
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
        binding.priceSlider.setValues(PRICE_SLIDER_MIN, PRICE_SLIDER_MAX)
        binding.selectedPriceRange.text = "R${PRICE_SLIDER_MIN.toInt()} - R${PRICE_SLIDER_MAX.toInt()}"

        binding.priceSlider.addOnChangeListener { slider, _, _ ->
            val values = slider.values
            if (values.size >= 2) {
                binding.selectedPriceRange.text = "R${values[0].toInt()} - R${values[1].toInt()}"
            }
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

    private fun getGender(item: Item): String? {
        return when (item.genderId) {
            GENDER_BOYS_ID -> "Boys"
            GENDER_GIRLS_ID -> "Girls"
            GENDER_UNISEX_ID -> "Unisex"
            in GENDER_ODD_ID..GENDER_EVEN_ID ->
                if (item.genderId?.rem(GENDER_REM_CHECK) == GENDER_REM_RESULT) "Girls" else "Boys"
            else -> item.gender?.takeIf { it.isNotBlank() }
        }
    }

    private fun setupBannerSlider() {
        bannerAdapter = BannerAdapter(bannerItems)
        binding.bannerViewPager.apply {
            adapter = bannerAdapter
            offscreenPageLimit = ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT
            setCurrentItem(Int.MAX_VALUE / BANNER_INITIAL_POSITION_DIVIDER, false)
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    updateIndicatorDots(position % bannerItems.size)
                }
            })
        }
        autoScrollHelper = BannerAutoScrollHelper(binding.bannerViewPager, BANNER_AUTO_SCROLL_INTERVAL_MS)
        autoScrollHelper.startAutoScroll()
    }

    private fun setupIndicatorDots() {
        binding.indicatorDots.removeAllViews()
        bannerItems.forEachIndexed { index, _ ->
            val dot = ImageView(requireContext()).apply {
                setImageResource(R.drawable.dot_selector)
                isSelected = (index == 0)
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
            dot.isSelected = (i == currentIndex)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.apply {
            setColorSchemeColors(
                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.teal_200)
            )
            setOnRefreshListener {
                viewModel.refreshHomeFeed()
            }
        }
    }

    private fun observeViewModel() {
        // ✅ 1. Loading state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                if (isLoading && viewModel.homeFeed.value == null) {
                    binding.shimmerLayout.visibility = View.VISIBLE
                    binding.homeRecycler.visibility = View.GONE
                    binding.errorLayout.visibility = View.GONE
                    binding.noSchoolLayout.visibility = View.GONE
                } else {
                    binding.shimmerLayout.visibility = View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false
                    if (viewModel.homeFeed.value != null && !isInSearchMode) {
                        binding.homeRecycler.visibility = View.VISIBLE
                    }
                }
            }
        }

        // ✅ 2. Error state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { errorMsg ->
                val isTechnicalError = errorMsg?.contains("401") == true ||
                        errorMsg?.contains("token") == true ||
                        errorMsg?.contains("Authorization") == true ||
                        errorMsg?.contains("session") == true ||
                        errorMsg?.contains("retry") == true ||
                        errorMsg.isNullOrEmpty()

                if (!isTechnicalError && viewModel.homeFeed.value == null) {
                    binding.errorLayout.visibility = View.VISIBLE
                    binding.errorMessage.text = errorMsg
                    binding.homeRecycler.visibility = View.GONE
                    binding.shimmerLayout.visibility = View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false
                } else {
                    binding.errorLayout.visibility = View.GONE
                }
            }
        }


        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.homeFeed.collect { feed ->
                feed?.let {
                    // ✅ Sort sections: Recent → Essentials → Trending → Recommended
                    val sortedSections = it.sections.sortedBy { section ->
                        when (section) {
                            is Section.Recent -> 0      // Recent FIRST (most important)
                            is Section.Essentials -> 1   // Essentials SECOND
                            is Section.Trending -> 2     // Trending THIRD
                            is Section.Recommended -> 3  // Recommended LAST
                            else -> 4
                        }
                    }

                    Timber.tag(LogTags.UI).d("=== HOME FEED RECEIVED ===")
                    Timber.tag(LogTags.UI).d("Sections count: ${it.sections.size}")
                    Timber.tag(LogTags.UI).d("Sorted order:")
                    sortedSections.forEachIndexed { index, section ->
                        Timber.tag(LogTags.UI).d("  $index: ${section::class.simpleName}")
                    }

                    binding.shimmerLayout.visibility = View.GONE
                    binding.homeRecycler.visibility = View.VISIBLE
                    binding.errorLayout.visibility = View.GONE
                    binding.noSchoolLayout.visibility = View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false

                    // ✅ FIX: Use sortedSections
                    homeAdapter.submitList(sortedSections)

                    // ✅ SCROLL TO TOP - Important!
                    binding.homeRecycler.postDelayed({
                        binding.homeRecycler.scrollToPosition(0)
                    }, 200)
                }
            }
        }


        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.feedUpdateEvent.collect { message ->
                if (isAdded && view != null && binding.root.isAttachedToWindow) {
                    try {
                        Timber.tag(LogTags.UI).d("📢 Showing update snackbar: $message")
                        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Timber.tag(LogTags.UI).e("Failed to show Snackbar: ${e.message}")
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.rankedSearchState.collect { state ->
                if (!isInSearchMode) return@collect

                val results = state.items
                val query = state.query

                Timber.tag(LogTags.UI).d("📊 Search state: query='$query', results=${results.size}")

                originalSearchResults.clear()
                currentSearchResults.clear()

                if (results.isEmpty()) {
                    // ✅ Show empty state with the actual query
                    binding.searchResultsRecycler.visibility = View.GONE
                    binding.emptySearchResults.visibility = View.VISIBLE
                    binding.emptySearchResults.text = if (query.isNotEmpty()) {
                        "No items found for '$query'"
                    } else {
                        "No items found"
                    }
                    binding.emptyBanner.visibility = View.GONE
                    categorySearchAdapter.submitList(emptyList())

                    Timber.tag(LogTags.UI).d("📊 Showing empty state for '$query'")
                } else {
                    // ✅ Show results
                    binding.emptySearchResults.visibility = View.GONE
                    binding.searchResultsRecycler.visibility = View.VISIBLE

                    originalSearchResults.clear()
                    originalSearchResults.addAll(results)
                    clearFilterState()
                    currentSearchResults.clear()
                    currentSearchResults.addAll(results)

                    categorySearchAdapter.submitList(currentSearchResults.toList())
                    rebuildLocalFilters()

                    // ✅ Banner logic
                    val userSchoolId = viewModel.knownSchoolId
                    val hasSchoolItems = if (userSchoolId != null) {
                        results.any { it.schoolId == userSchoolId }
                    } else {
                        false
                    }

                    val nearbyIds = viewModel.getNearbySchoolIds()
                    val hasNearbyItems = if (nearbyIds.isNotEmpty()) {
                        results.any { nearbyIds.contains(it.schoolId) }
                    } else {
                        false
                    }

                    if (results.isNotEmpty() && !hasSchoolItems) {
                        val bannerMessage = if (hasNearbyItems) {
                            "No items found at your school. Showing nearby items."
                        } else {
                            "No items found at your school or nearby. Showing other schools."
                        }
                        binding.bannerMessage.text = bannerMessage
                        binding.emptyBanner.visibility = View.VISIBLE
                        binding.btnCloseBanner.setOnClickListener {
                            binding.emptyBanner.visibility = View.GONE
                        }
                    } else {
                        binding.emptyBanner.visibility = View.GONE
                    }

                    Timber.tag(LogTags.UI).d("📊 Showing ${results.size} results for '$query'")
                }
            }
        }

        // ✅ 6. Relevance groups (logging only)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.searchRelevanceGroups.collect { groups ->
                Timber.tag(LogTags.UI).d("📊 Relevance groups: school=${groups.schoolMatch.size}, nearby=${groups.nearbyMatch.size}, other=${groups.other.size}")
            }
        }
    }

    private fun rebuildLocalFilters() {
        binding.dynamicFilterContainer.removeAllViews()

        fun itemsExcluding(excludeGroup: String): List<Item> {
            var items = originalSearchResults.toList()
            if (excludeGroup != "gender" && selectedGender != null)
                items = items.filter { getGender(it) == selectedGender }
            if (excludeGroup != "condition" && selectedCondition != null)
                items = items.filter { it.conditionName == selectedCondition }
            if (excludeGroup != "size" && selectedSize != null)
                items = items.filter { it.sizeName == selectedSize }
            if (excludeGroup != "color" && selectedColor != null)
                items = items.filter { it.colorName == selectedColor }
            if (excludeGroup != "brand" && selectedBrand != null)
                items = items.filter { it.brandName == selectedBrand }
            return items
        }

        val genders = itemsExcluding("gender").mapNotNull { getGender(it) }.distinct()
        val conditions = itemsExcluding("condition").mapNotNull { it.conditionName }.distinct()
        val sizes = itemsExcluding("size").mapNotNull { it.sizeName }.distinct()
        val colors = itemsExcluding("color").mapNotNull { it.colorName }.distinct()
        val brands = itemsExcluding("brand").mapNotNull { it.brandName }.distinct()

        if (genders.isNotEmpty()) {
            addFilterItem("Gender", selectedGender) {
                showOptionsDrawer("gender", "Gender", genders.map { FilterOption(it.hashCode(), it) }, selectedGender)
            }
        }
        if (conditions.isNotEmpty()) {
            addFilterItem("Condition", selectedCondition) {
                showOptionsDrawer("condition", "Condition", conditions.map { FilterOption(it.hashCode(), it) }, selectedCondition)
            }
        }
        if (sizes.isNotEmpty()) {
            addFilterItem("Size", selectedSize) {
                showOptionsDrawer("size", "Size", sizes.map { FilterOption(it.hashCode(), it) }, selectedSize)
            }
        }
        if (colors.isNotEmpty()) {
            addFilterItem("Color", selectedColor) {
                showOptionsDrawer("color", "Color", colors.map { FilterOption(it.hashCode(), it) }, selectedColor)
            }
        }
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
        val clickableRow = itemView.findViewById<LinearLayout>(R.id.filterRow)

        titleView.text = title
        if (!selectedValue.isNullOrEmpty()) {
            valueView.text = selectedValue
            valueView.visibility = View.VISIBLE
        } else {
            valueView.visibility = View.GONE
        }

        clickableRow.setOnClickListener { onClick() }
        itemView.setOnClickListener { onClick() }
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
                    "condition" -> selectedCondition = if (selectedCondition == option.name) null else option.name
                    "size" -> selectedSize = if (selectedSize == option.name) null else option.name
                    "color" -> selectedColor = if (selectedColor == option.name) null else option.name
                    "brand" -> selectedBrand = if (selectedBrand == option.name) null else option.name
                }

                applyAllFilters()
                showOptionsDrawer(groupId, groupName, getUpdatedOptions(groupId), getSelection(groupId))
            }

            container.addView(optionView)
        }
    }

    private fun getUpdatedOptions(groupId: String): List<FilterOption> {
        var items = originalSearchResults.toList()
        if (groupId != "gender" && selectedGender != null)
            items = items.filter { getGender(it) == selectedGender }
        if (groupId != "condition" && selectedCondition != null)
            items = items.filter { it.conditionName == selectedCondition }
        if (groupId != "size" && selectedSize != null)
            items = items.filter { it.sizeName == selectedSize }
        if (groupId != "color" && selectedColor != null)
            items = items.filter { it.colorName == selectedColor }
        if (groupId != "brand" && selectedBrand != null)
            items = items.filter { it.brandName == selectedBrand }

        val values = when (groupId) {
            "gender" -> items.mapNotNull { getGender(it) }.distinct()
            "condition" -> items.mapNotNull { it.conditionName }.distinct()
            "size" -> items.mapNotNull { it.sizeName }.distinct()
            "color" -> items.mapNotNull { it.colorName }.distinct()
            "brand" -> items.mapNotNull { it.brandName }.distinct()
            else -> emptyList()
        }
        return values.map { FilterOption(it.hashCode(), it) }
    }

    private fun getSelection(groupId: String): String? = when (groupId) {
        "gender" -> selectedGender
        "condition" -> selectedCondition
        "size" -> selectedSize
        "color" -> selectedColor
        "brand" -> selectedBrand
        else -> null
    }

    private fun applyAllFilters() {
        var filtered = originalSearchResults.toList()

        val minPrice = binding.priceSlider.values[0]
        val maxPrice = binding.priceSlider.values[1]
        filtered = filtered.filter { it.price in minPrice..maxPrice }

        if (selectedGender != null) filtered = filtered.filter { getGender(it) == selectedGender }
        if (selectedCondition != null) filtered = filtered.filter { it.conditionName == selectedCondition }
        if (selectedSize != null) filtered = filtered.filter { it.sizeName == selectedSize }
        if (selectedColor != null) filtered = filtered.filter { it.colorName == selectedColor }
        if (selectedBrand != null) filtered = filtered.filter { it.brandName == selectedBrand }

        currentSearchResults.clear()
        currentSearchResults.addAll(filtered)
        categorySearchAdapter.submitList(currentSearchResults.toList())
    }

    private fun resetLocalFilters() {
        binding.priceSlider.setValues(PRICE_SLIDER_MIN, PRICE_SLIDER_MAX)
        binding.selectedPriceRange.text = "R${PRICE_SLIDER_MIN.toInt()} - R${PRICE_SLIDER_MAX.toInt()}"
        clearFilterState()
        currentSearchResults.clear()
        currentSearchResults.addAll(originalSearchResults)
        categorySearchAdapter.submitList(currentSearchResults.toList())
        rebuildLocalFilters()
    }

    private fun clearFilterState() {
        selectedGender = null
        selectedCondition = null
        selectedSize = null
        selectedColor = null
        selectedBrand = null
    }

    private fun navigateToItemDetail(itemId: String, source: String) {
        val bundle = Bundle().apply {
            putString("itemId", itemId)
            putString("source", source)
        }
        findNavController().navigate(R.id.itemDetailFragment, bundle)
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
            if (categoryId != null) putInt("CATEGORY_ID", categoryId)
        }
        findNavController().navigate(R.id.action_homeFragment_to_productsFragment, bundle)
    }

    private fun setupCustomTabs() {
        val tabs = listOf(binding.tabHome, binding.tabUniform, binding.tabSport, binding.tabRecent)
        val isDarkMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        val unselectedDrawable = if (isDarkMode) R.drawable.tablayout_unselected_night else R.drawable.tablayout_unselected

        tabs.forEach { tab ->
            tab.setBackgroundResource(unselectedDrawable)
            tab.setOnClickListener { selectTab(tab) }
        }
        highlightTab(binding.tabHome)
    }

    private fun highlightTab(selectedTab: TextView) {
        val tabs = listOf(binding.tabHome, binding.tabUniform, binding.tabSport, binding.tabRecent)
        val isDarkMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES

        tabs.forEach { tab ->
            if (tab == selectedTab) {
                tab.setBackgroundResource(R.drawable.tablayout_selector)
                tab.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.white))
                tab.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                if (isDarkMode) {
                    tab.setBackgroundResource(R.drawable.tablayout_unselected_night)
                    tab.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.white_70))
                } else {
                    tab.setBackgroundResource(R.drawable.tablayout_unselected)
                    tab.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.black_70))
                }
                tab.setTypeface(null, android.graphics.Typeface.NORMAL)
            }
        }
    }

    fun getCurrentCategoryId(): Int? {
        return when (currentTabId) {
            R.id.tabUniform -> TAB_UNIFORM_ID
            R.id.tabSport -> TAB_SPORT_ID
            else -> null
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        val isDarkMode = (newConfig.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        val unselectedDrawable = if (isDarkMode) R.drawable.tablayout_unselected_night else R.drawable.tablayout_unselected
        val tabs = listOf(binding.tabHome, binding.tabUniform, binding.tabSport, binding.tabRecent)
        tabs.forEach { tab ->
            if (tab.id != currentTabId) tab.setBackgroundResource(unselectedDrawable)
        }
    }

    private fun selectTab(selectedTab: TextView) {
        currentTabId = selectedTab.id
        highlightTab(selectedTab)

        when (selectedTab.id) {
            R.id.tabHome -> {
                // ✅ Only load if feed is null
                if (viewModel.homeFeed.value == null && !viewModel.isLoading.value) {
                    viewModel.loadHomeFeed()
                }
            }
            R.id.tabUniform -> navigateToUniformTab()
            R.id.tabSport -> navigateToSportTab()
            R.id.tabRecent -> navigateToRecentTab()
        }
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

    // ✅ FIXED: Simplified onResume - only auto-scroll, no reload
    override fun onResume() {
        super.onResume()
        autoScrollHelper?.resumeAutoScroll()
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
}