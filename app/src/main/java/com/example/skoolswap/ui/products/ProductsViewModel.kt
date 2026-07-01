package com.example.skoolswap.ui.products

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skoolswap.common.constants.ItemConstants.getSchoolId
import com.example.skoolswap.data.local.datastore.AppPreferences
import com.example.skoolswap.data.remote.models.response.home.PaginatedResponse
import com.example.skoolswap.domain.model.RelevanceGroups
import com.example.skoolswap.domain.model.AppliedFilters
import com.example.skoolswap.domain.model.FilterConfig
import com.example.skoolswap.domain.model.FilterGroup
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.repository.FilterRepositoryInterface
import com.example.skoolswap.domain.repository.ProductsRepositoryInterface
import com.example.skoolswap.ui.home.HomeViewModel
import com.example.skoolswap.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import javax.inject.Inject
import kotlinx.coroutines.flow.first
@HiltViewModel
class ProductsViewModel @Inject constructor(
    private val productsRepository: ProductsRepositoryInterface,
    private val appPreferences: AppPreferences,
    private val filterRepository: FilterRepositoryInterface
) : ViewModel() {
    // Add this flag to block stale data replay
    private val _isNewSectionLoading = MutableStateFlow(false)
    val isNewSectionLoading: StateFlow<Boolean> = _isNewSectionLoading.asStateFlow()
    private val _products = MutableStateFlow<List<Item>>(emptyList())
    val products: StateFlow<List<Item>> = _products

    // ADDED: Search results state (like HomeViewModel)
    private val _searchResults = MutableStateFlow<List<Item>>(emptyList())
    val searchResults: StateFlow<List<Item>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _filterConfig = MutableStateFlow<FilterConfig?>(null)
    val filterConfig: StateFlow<FilterConfig?> = _filterConfig

    private val _appliedFilters = MutableStateFlow(AppliedFilters())
    val appliedFilters: StateFlow<AppliedFilters> = _appliedFilters

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Store global filter config separately for category lookups
    private var globalFilterConfig: FilterConfig? = null

    // Saved category state for persistence across navigation
    private var savedCategoryId: Int? = null
    private var savedCategoryName: String? = null

    // Remember state across fragment recreation
    private var currentSectionType = "recommended"
    private var currentPeriod: String? = null
    private var lastLoadedCategoryId: Int? = null

    // Sport-specific filters (for pre-filtering before ProductsFragment opens)
    private var preSelectedSportTypeId: Int? = null
    private var preSelectedGearType: String? = null

    private var loadProductsJob: Job? = null
    private var loadFilterJob: Job? = null

    // Search state
    private var currentSearchQuery: String? = null
    private var currentCategoryId: Int? = null
    private var searchJob: Job? = null

    // Sort state
    private var _currentSortType: String? = null
    private var currentSubCategoryId: Int? = null
    // ==================== Public Methods ====================
// ProductsViewModel.kt - Add these with other state variables

    private val _isShowingLocalResults = MutableStateFlow(false)
    val isShowingLocalResults: StateFlow<Boolean> = _isShowingLocalResults.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _serverItemsCount = MutableStateFlow(0)
    val serverItemsCount: StateFlow<Int> = _serverItemsCount.asStateFlow()
    private val _searchRelevanceGroups = MutableStateFlow(RelevanceGroups(emptyList(), emptyList(), emptyList()))
    val searchRelevanceGroups: StateFlow<RelevanceGroups> = _searchRelevanceGroups.asStateFlow()
    private val _searchQuery = MutableStateFlow<String?>(null)
    val searchQuery: StateFlow<String?> = _searchQuery.asStateFlow()
    fun getSavedCategoryId(): Int? = savedCategoryId
    fun getSavedCategoryName(): String? = savedCategoryName
    private var currentNavCategoryId: Int? = null  // ← ADD THIS
    fun setSavedCategory(categoryId: Int, categoryName: String) {
        savedCategoryId = categoryId
        savedCategoryName = categoryName
        Timber.tag("ProductsViewModel").d("Saved category: $categoryName (ID: $categoryId)")
    }

    fun setSectionType(sectionType: String, period: String? = null, categoryId: Int? = null) {
        currentSectionType = sectionType
        currentPeriod = period
        currentCategoryId = categoryId
    }

    // UPDATED: Search with debounce like HomeViewModel
    fun searchInCurrentSection(query: String) {
        Timber.tag("ProductsViewModel").d("🔍 searchInCurrentSection called with query: '$query'")
        searchJob?.cancel()

        _searchQuery.value = query

        if (query.length < 2) {
            Timber.tag("ProductsViewModel").d("Query too short, clearing results")
            _searchResults.value = emptyList()
            _isShowingLocalResults.value = false
            _serverItemsCount.value = 0
            return
        }

        searchJob = viewModelScope.launch {
            delay(300)

            Timber.tag("ProductsViewModel").d("🔍 Searching for: $query")

            _serverItemsCount.value = 0


            val localResults = searchLocalCache(query, getValidCategoryId())

            if (localResults.isNotEmpty()) {
                Timber.tag("ProductsViewModel").d("⚡ ${localResults.size} results from LOCAL CACHE")
                _searchResults.value = localResults
                _isShowingLocalResults.value = true
            } else {
                _searchResults.value = emptyList()
                _isShowingLocalResults.value = false
            }

            // STEP 2: Search server in background
            _isLoadingMore.value = true

            // ✅ FIX: Convert -1 to null, and ensure we don't send invalid category IDs
            val validCategoryId = getValidCategoryId()

            val result = productsRepository.searchItems(
                query = query,
                categoryId = validCategoryId,  // ← Now properly null for "all categories"
                genderId = _appliedFilters.value.gender,
                brandId = _appliedFilters.value.brand?.firstOrNull(),
                sizeId = _appliedFilters.value.size,
                colorId = _appliedFilters.value.color?.firstOrNull(),
                conditionId = _appliedFilters.value.condition,
                minPrice = _appliedFilters.value.minPrice,
                maxPrice = _appliedFilters.value.maxPrice,
                sort = _currentSortType,
                page = 1,
                perPage = 30
            )

            _isLoadingMore.value = false

            when (result) {
                is Result.Success -> {
                    val serverItems = result.data.items
                    _serverItemsCount.value = serverItems.size
                    Timber.tag("ProductsViewModel").d("✅ Server returned ${serverItems.size} results")

                    if (serverItems.isNotEmpty()) {
                        val currentResults = _searchResults.value.toMutableList()
                        val existingIds = currentResults.map { it.id }.toSet()
                        val newItems = serverItems.filter { it.id !in existingIds }

                        if (newItems.isNotEmpty()) {
                            val merged = currentResults + newItems
                            _searchResults.value = merged
                            Timber.tag("ProductsViewModel").d("📦 Added ${newItems.size} new items")
                        }
                        _isShowingLocalResults.value = false
                    } else if (!_isShowingLocalResults.value && _searchResults.value.isEmpty()) {
                        _searchResults.value = emptyList()
                    }
                }
                is Result.Error -> {
                    Timber.tag("ProductsViewModel").e("❌ Server search failed: ${result.exception.message}")
                    if (!_isShowingLocalResults.value && _searchResults.value.isEmpty()) {
                        _error.value = "Failed to load results"
                    }
                }
            }
        }
    }
    private fun getValidCategoryId(): Int? {
        val categoryId = currentCategoryId ?: _appliedFilters.value.categoryId
        // Convert -1 or 0 to null (meaning "all categories")
        return if (categoryId == null || categoryId <= 0) null else categoryId
    }
    fun setSubCategoryId(subCategoryId: Int?) {
        currentSubCategoryId = subCategoryId
    }
    fun resetFirstLoadFlag() {
        // This helps reset any stale state
        _error.value = null
        // Don't reset products here - let loading handle it
    }
    private fun searchLocalCache(query: String, categoryId: Int?): List<Item> {
        val currentProducts = _products.value
        if (currentProducts.isEmpty()) {
            Timber.tag("ProductsViewModel").d("No cached products available")
            return emptyList()
        }

        val filteredByCategory = if (categoryId != null && categoryId > 0) {
            currentProducts.filter { it.mainCategoryId == categoryId }
        } else {
            currentProducts
        }

        val searchLower = query.lowercase()
        val results = filteredByCategory
            .filter { item ->
                item.name.lowercase().contains(searchLower) ||
                        item.description.lowercase().contains(searchLower)
            }
            .distinctBy { it.id }
            .take(30)

        Timber.tag("ProductsViewModel").d("🔍 Local search found ${results.size} matches from ${currentProducts.size} cached items")
        return results
    }

    fun clearSavedCategory() {
        savedCategoryId = null
        savedCategoryName = null
        Timber.tag("ProductsViewModel").d("Cleared saved category")
    }

    fun loadProducts(
        sectionType: String,
        period: String? = null,
        navCategoryId: Int? = null,
        subCategoryId: Int? = null,
        preSelectedSportTypeId: Int? = null,
        preSelectedGearType: String? = null
    ) {
        Timber.tag("ProductsViewModel").d("🔄 loadProducts: section='$sectionType', subCategoryId=$subCategoryId, navCategoryId=$navCategoryId")

        currentSubCategoryId = subCategoryId
        currentNavCategoryId = navCategoryId
        currentSectionType = sectionType.lowercase()

        currentSearchQuery = null
        _searchResults.value = emptyList()
        _products.value = emptyList()

        loadProductsJob?.cancel()
        searchJob?.cancel()


        this.preSelectedSportTypeId = preSelectedSportTypeId
        this.preSelectedGearType = preSelectedGearType

        val normalizedType = sectionType.lowercase()
        val incomingCategoryId = if (navCategoryId == -1) null else navCategoryId

        val effectiveCategoryId = when {
            normalizedType == "sports" || normalizedType == "sport" -> incomingCategoryId ?: 2
            else -> savedCategoryId ?: _appliedFilters.value.categoryId ?: incomingCategoryId
        }

        currentSectionType = normalizedType
        currentPeriod = period
        if (effectiveCategoryId != null) lastLoadedCategoryId = effectiveCategoryId

        loadFilterConfigIfNeeded(effectiveCategoryId)
        loadProductsInternal(effectiveCategoryId, normalizedType)
    }


    fun loadFilterConfig(categoryId: Int) {
        if (categoryId <= 0) return
        lastLoadedCategoryId = categoryId
        loadFilterConfigIfNeeded(categoryId)
    }

    fun loadGlobalFilterConfig() {
        viewModelScope.launch {
            when (val result = filterRepository.getGlobalFilterConfig()) {
                is Result.Success -> {
                    globalFilterConfig = result.data
                    _filterConfig.value = result.data
                    Timber.tag("ProductsViewModel")
                        .d("Loaded global filter config: ${result.data.filterGroups.size} groups")
                }
                is Result.Error -> {
                    Timber.tag("ProductsViewModel")
                        .e("Failed to load global filter config: ${result.exception.message}")
                }
            }
        }
    }
    fun searchItemsRanked(query: String, categoryId: Int?) {
        _searchQuery.value = query
        searchJob?.cancel()

        if (query.length < 2) {
            Timber.tag("ProductsViewModel").d("Query too short, clearing results")
            _searchResults.value = emptyList()
            _searchRelevanceGroups.value =
                RelevanceGroups(emptyList(), emptyList(), emptyList())
            _isShowingLocalResults.value = false
            _serverItemsCount.value = 0
            return
        }

        searchJob = viewModelScope.launch {
            delay(300)

            Timber.tag("ProductsViewModel").d("🔍 Ranked search for: $query")

            // STEP 1: Search local cache first (instant results)
            val localResults = searchLocalCache(query, getValidCategoryId())
            if (localResults.isNotEmpty()) {
                Timber.tag("ProductsViewModel").d("⚡ ${localResults.size} results from LOCAL CACHE")
                _searchResults.value = localResults
                _isShowingLocalResults.value = true
            } else {
                _searchResults.value = emptyList()
                _isShowingLocalResults.value = false
            }

            // STEP 2: Fetch from server with ranking
            _isLoadingMore.value = true

            try {
                // Get school ID (you need to inject UserSchoolRepository or pass it)
                val schoolId = getSchoolId() // You'll need to add this method
                if (schoolId == null) {
                    Timber.tag("ProductsViewModel").e("❌ No school ID available")
                    _isLoadingMore.value = false
                    return@launch
                }

                val validCategoryId = getValidCategoryId()
                val serverResult = withTimeout(10_000L) {
                    productsRepository.searchItemsRanked(
                        query = query,
                        schoolId = schoolId,
                        categoryId = validCategoryId,
                        subCategoryId = currentSubCategoryId,
                        minPrice = _appliedFilters.value.minPrice,
                        maxPrice = _appliedFilters.value.maxPrice,
                        page = 1,
                        perPage = 30
                    )
                }

                _isLoadingMore.value = false

                when (serverResult) {
                    is Result.Success -> {
                        val rankedResult = serverResult.data
                        val serverItems = rankedResult.items
                        _serverItemsCount.value = serverItems.size

                        Timber.tag("ProductsViewModel").d("✅ Ranked search returned ${serverItems.size} items")

                        if (serverItems.isNotEmpty()) {
                            val currentResults = _searchResults.value.toMutableList()
                            val existingIds = currentResults.map { it.id }.toSet()
                            val newItems = serverItems.filter { it.id !in existingIds }

                            if (newItems.isNotEmpty()) {
                                val merged = currentResults + newItems
                                _searchResults.value = merged
                                Timber.tag("ProductsViewModel").d("📦 Added ${newItems.size} new items")
                            }
                            _isShowingLocalResults.value = false
                        } else if (!_isShowingLocalResults.value && _searchResults.value.isEmpty()) {
                            _searchResults.value = emptyList()
                        }
                    }
                    is Result.Error -> {
                        Timber.tag("ProductsViewModel").e("❌ Server search failed: ${serverResult.exception.message}")
                        if (!_isShowingLocalResults.value && _searchResults.value.isEmpty()) {
                            _error.value = "Failed to load results"
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                _isLoadingMore.value = false
                Timber.tag("ProductsViewModel").e("⏱️ Server search timed out")
                if (!_isShowingLocalResults.value && _searchResults.value.isEmpty()) {
                    _error.value = "Search timed out. Please try again."
                }
            } catch (e: Exception) {
                _isLoadingMore.value = false
                Timber.tag("ProductsViewModel").e("❌ Search error: ${e.message}")
                if (!_isShowingLocalResults.value && _searchResults.value.isEmpty()) {
                    _error.value = "Failed to load results: ${e.message}"
                }
            }
        }
    }
    private suspend fun getSchoolId(): Int? {
        return appPreferences.schoolId.first()
    }
    fun updateFilter(key: String, value: Any?) {
        val updated = when (key) {
            "category" -> {
                val categoryId = value as? Int
                if (categoryId != null && categoryId > 0) {
                    val categoryName = getGlobalFilterGroupById("category")
                        ?.options?.find { it.id == categoryId }?.name ?: ""
                    setSavedCategory(categoryId, categoryName)
                } else {
                    clearSavedCategory()
                }
                _appliedFilters.value.copy(categoryId = categoryId)
            }
            "condition" -> _appliedFilters.value.copy(condition = value as? Int)
            "gender" -> _appliedFilters.value.copy(gender = value as? Int)
            "size" -> _appliedFilters.value.copy(size = value as? Int)
            "type" -> _appliedFilters.value.copy(type = value as? List<Int>)
            "brand" -> _appliedFilters.value.copy(brand = value as? List<Int>)
            "color" -> _appliedFilters.value.copy(color = value as? List<Int>)
            else -> _appliedFilters.value
        }

        _appliedFilters.value = updated

        if (key == "category") {
            lastLoadedCategoryId = value as? Int
            loadFilterConfigIfNeeded(value as? Int)
        }

        // If we're in search mode, re-run search with new filters
        if (currentSearchQuery != null) {
            searchInCurrentSection(currentSearchQuery!!)
        } else {
            loadProductsInternal(updated.categoryId)
        }
    }

    fun updatePriceRange(min: Float, max: Float) {
        val newFilters = _appliedFilters.value.copy(
            minPrice = if (min > 0f) min else null,
            maxPrice = if (max < 100000f) max else null
        )
        _appliedFilters.value = newFilters

        Timber.tag("ProductsViewModel").d("📊 Price filter updated → R${min.toInt()} - R${max.toInt()}")

        // Re-load data with new price filter
        if (currentSearchQuery != null) {
            searchInCurrentSection(currentSearchQuery!!)
        } else {
            loadProductsInternal(_appliedFilters.value.categoryId)
        }
    }


    fun applyFilters() {
        Timber.tag("ProductsViewModel").d("Applying all filters including price")

        if (currentSearchQuery != null) {
            searchInCurrentSection(currentSearchQuery!!)
        } else {
            loadProductsInternal(_appliedFilters.value.categoryId)
        }
    }

    fun resetFilters() {
        _appliedFilters.value = AppliedFilters()   // This should clear minPrice/maxPrice
        clearSavedCategory()

        if (currentSearchQuery != null) {
            searchInCurrentSection(currentSearchQuery!!)
        } else {
            loadProductsInternal(null)
        }
    }

    fun sortBy(sortType: String) {
        _currentSortType = when (sortType) {
            "price_low" -> "price_low"
            "price_high" -> "price_high"
            "newest" -> "newest"
            else -> null
        }

        if (currentSearchQuery != null) {
            // Re-run search with new sort
            searchInCurrentSection(currentSearchQuery!!)
        } else {
            // Regular sorting on current products
            val sorted = when (sortType) {
                "price_low" -> _products.value.sortedBy { it.price }
                "price_high" -> _products.value.sortedByDescending { it.price }
                "newest" -> _products.value.sortedByDescending { it.createdAt }
                else -> _products.value
            }
            _products.value = sorted
        }
    }

    fun getFilterGroupById(groupId: String): FilterGroup? {
        return _filterConfig.value?.filterGroups?.find { it.id == groupId }
    }

    fun getGlobalFilterGroupById(groupId: String): FilterGroup? {
        return globalFilterConfig?.filterGroups?.find { it.id == groupId }
    }

    fun trackClick(itemId: String, source: String, position: Int) {
        viewModelScope.launch {
            productsRepository.trackClick(itemId, source, position)
        }
    }

    // ==================== Private Methods ====================

    private fun loadFilterConfigIfNeeded(categoryId: Int?) {
        val currentConfig = _filterConfig.value
        if (categoryId != null && categoryId > 0 &&
            currentConfig?.categoryId == categoryId) {
            Timber.tag("ProductsViewModel")
                .d("Filter config already loaded for category $categoryId")
            return
        }
        if (categoryId == null && currentConfig?.categoryId == null && currentConfig != null) {
            Timber.tag("ProductsViewModel").d("Global filter config already loaded")
            return
        }

        loadFilterJob?.cancel()
        loadFilterJob = viewModelScope.launch {
            val result = if (categoryId != null && categoryId > 0) {
                Timber.tag("ProductsViewModel")
                    .d("Loading CATEGORY-SPECIFIC filters for ID: $categoryId")
                filterRepository.getFilterConfig(categoryId)
            } else {
                Timber.tag("ProductsViewModel").d("Loading GLOBAL filters")
                filterRepository.getGlobalFilterConfig()
            }

            when (result) {
                is Result.Success -> {
                    _filterConfig.value = result.data
                    if (categoryId == null) {
                        globalFilterConfig = result.data
                    }
                    Timber.tag("ProductsViewModel")
                        .d("Filter config loaded: ${result.data.filterGroups.size} groups")
                    result.data.filterGroups.forEach { group ->
                        Timber.tag("ProductsViewModel")
                            .d("  - ${group.name}: ${group.options.size} options")
                    }
                }
                is Result.Error -> {
                    Timber.tag("ProductsViewModel")
                        .e("Filter config failed: ${result.exception.message}")
                }
            }
        }
    }

    private fun loadProductsInternal(categoryId: Int?, sectionType: String? = null) {
        // ✅ Use the stored currentSubCategoryId (set in loadProducts)
        val effectiveSubCategoryId = currentSubCategoryId

        Timber.tag("ProductsViewModel").d("🚀 loadProductsInternal START → section=$sectionType, subCategoryId=$effectiveSubCategoryId, categoryId=$categoryId")

        // Cancel previous job
        loadProductsJob?.cancel()

        loadProductsJob = viewModelScope.launch {
            _isNewSectionLoading.value = true
            _isLoading.value = true
            _error.value = null
            _products.value = emptyList()

            // ✅ Use effectiveSubCategoryId in all calls
            val result: Result<PaginatedResponse<Item>> = when {
                sectionType == "uniform" || sectionType == "uniforms" -> {
                    Timber.tag("ProductsViewModel").d("👕 Loading Uniforms with subCategoryId=$effectiveSubCategoryId")
                    productsRepository.getEssentialsAll(
                        page = 1,
                        category = "Uniforms",
                        subCategoryId = effectiveSubCategoryId,  // ← USE STORED VALUE
                        perPage = 30,
                        conditionId = _appliedFilters.value.condition,
                        minPrice = _appliedFilters.value.minPrice,
                        maxPrice = _appliedFilters.value.maxPrice
                    )
                }

                sectionType == "sports" || sectionType == "sport" -> {
                    Timber.tag("ProductsViewModel").d("🏅 Loading Sports with subCategoryId=$effectiveSubCategoryId")
                    productsRepository.getEssentialsAll(
                        page = 1,
                        category = "Sports",
                        subCategoryId = effectiveSubCategoryId,  // ← USE STORED VALUE
                        perPage = 30,
                        conditionId = _appliedFilters.value.condition,
                        minPrice = _appliedFilters.value.minPrice,
                        maxPrice = _appliedFilters.value.maxPrice
                    )
                }

                sectionType == "accessories" -> {
                    Timber.tag("ProductsViewModel").d("🎒 Loading Accessories")
                    productsRepository.getEssentialsAll(
                        page = 1,
                        category = "Accessories",
                        subCategoryId = null,
                        perPage = 30,
                        conditionId = _appliedFilters.value.condition,
                        minPrice = _appliedFilters.value.minPrice,
                        maxPrice = _appliedFilters.value.maxPrice
                    )
                }

                else -> {
                    Timber.tag("ProductsViewModel").d("✨ Loading Recommended")
                    productsRepository.getRecommendedAll(
                        page = 1,
                        categoryId = getValidCategoryId(),
                        conditionId = _appliedFilters.value.condition,
                        minPrice = _appliedFilters.value.minPrice,
                        maxPrice = _appliedFilters.value.maxPrice
                    )
                }
            }

            when (val res = result) {
                is Result.Success -> {
                    val items = res.data.items
                    _products.value = items
                    Timber.tag("ProductsViewModel")
                        .d("✅ SUCCESS: Loaded ${items.size} items | section=$sectionType | subCategory=$effectiveSubCategoryId")
                }
                is Result.Error -> {
                    _error.value = res.exception.message ?: "Failed to load items"
                    Timber.e(res.exception, "❌ Failed to load products")
                }
            }

            _isLoading.value = false
            _isNewSectionLoading.value = false
            Timber.tag("ProductsViewModel").d("🏁 Loading finished")
        }
    }



    // Add this helper if you don't have it
    fun reloadLocalFiltersOnly() {
        // Just trigger filter rebuild without new network call
        viewModelScope.launch {
            // You can emit the current products again to trigger collector
            _products.value = _products.value
        }
    }
    override fun onCleared() {
        loadProductsJob?.cancel()
        loadFilterJob?.cancel()
        searchJob?.cancel()
        super.onCleared()
    }
    fun clearSearchResults() {
        _searchResults.value = emptyList()
        currentSearchQuery = null
    }

    fun reloadCurrentSection() {
        currentSearchQuery = null
        _searchResults.value = emptyList()
        // ✅ Pass the current section type to preserve sub-category filter
        loadProductsInternal(_appliedFilters.value.categoryId, currentSectionType)
    }

    // Add items cache
    private val itemsCache = mutableMapOf<String, List<Item>>()

    private fun getCacheKey(): String {
        return "${currentSectionType}_${currentCategoryId}_${_appliedFilters.value}"
    }
}