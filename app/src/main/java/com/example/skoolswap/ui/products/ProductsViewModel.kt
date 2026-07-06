package com.example.skoolswap.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skoolswap.common.constants.AppConstants
import com.example.skoolswap.common.constants.AppConstants.LogTags
import com.example.skoolswap.data.local.datastore.AppPreferences
import com.example.skoolswap.data.remote.models.response.home.PaginatedResponse
import com.example.skoolswap.domain.model.RelevanceGroups
import com.example.skoolswap.domain.model.AppliedFilters
import com.example.skoolswap.domain.model.FilterConfig
import com.example.skoolswap.domain.model.FilterGroup
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.repository.FilterRepositoryInterface
import com.example.skoolswap.domain.repository.ProductsRepositoryInterface
import com.example.skoolswap.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import javax.inject.Inject

// Private constants - internal to this file only
private const val SEARCH_DEBOUNCE_DELAY_MS = 300L
private const val SEARCH_TIMEOUT_MS = 10000L
private const val MIN_SEARCH_LENGTH = 2
private const val PER_PAGE_DEFAULT = 30
private const val PAGE_DEFAULT = 1
private const val PRICE_MIN = 0f
private const val PRICE_MAX = 100000f
private const val CATEGORY_ID_SPORT = 2

@HiltViewModel
class ProductsViewModel @Inject constructor(
    private val productsRepository: ProductsRepositoryInterface,
    private val appPreferences: AppPreferences,
    private val filterRepository: FilterRepositoryInterface
) : ViewModel() {

    private val _isNewSectionLoading = MutableStateFlow(false)
    val isNewSectionLoading: StateFlow<Boolean> = _isNewSectionLoading.asStateFlow()

    private val _products = MutableStateFlow<List<Item>>(emptyList())
    val products: StateFlow<List<Item>> = _products

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
    private var currentNavCategoryId: Int? = null

    // User school context
    private var userSchoolId: Int? = null
    private var nearbySchoolIds: List<Int> = emptyList()

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

    private val itemsCache = mutableMapOf<String, List<Item>>()

    // ==================== Public Methods ====================

    fun getSavedCategoryId(): Int? = savedCategoryId
    fun getSavedCategoryName(): String? = savedCategoryName

    fun setSavedCategory(categoryId: Int, categoryName: String) {
        savedCategoryId = categoryId
        savedCategoryName = categoryName
        Timber.tag(LogTags.VIEW_MODEL).d("Saved category: $categoryName (ID: $categoryId)")
    }

    fun setSectionType(sectionType: String, period: String? = null, categoryId: Int? = null) {
        currentSectionType = sectionType
        currentPeriod = period
        currentCategoryId = categoryId
    }

    fun setSubCategoryId(subCategoryId: Int?) {
        currentSubCategoryId = subCategoryId
    }

    fun resetFirstLoadFlag() {
        _error.value = null
    }

    fun clearSavedCategory() {
        savedCategoryId = null
        savedCategoryName = null
        Timber.tag(LogTags.VIEW_MODEL).d("Cleared saved category")
    }

    fun setUserSchoolContext(schoolId: Int, nearbyIds: List<Int>) {
        userSchoolId = schoolId
        nearbySchoolIds = nearbyIds
    }

    fun getUserSchoolId(): Int? = userSchoolId
    fun getNearbySchoolIds(): List<Int> = nearbySchoolIds

    // ==================== Search Methods ====================

    fun searchInCurrentSection(query: String) {
        Timber.tag(LogTags.VIEW_MODEL).d("🔍 searchInCurrentSection called with query: '$query'")
        searchJob?.cancel()

        _searchQuery.value = query

        if (query.length < MIN_SEARCH_LENGTH) {
            Timber.tag(LogTags.VIEW_MODEL).d("Query too short, clearing results")
            _searchResults.value = emptyList()
            _isShowingLocalResults.value = false
            _serverItemsCount.value = 0
            return
        }

        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_DELAY_MS)

            Timber.tag(LogTags.VIEW_MODEL).d("🔍 Searching for: $query")

            _serverItemsCount.value = 0

            val localResults = searchLocalCache(query, getValidCategoryId())

            if (localResults.isNotEmpty()) {
                Timber.tag(LogTags.VIEW_MODEL).d("⚡ ${localResults.size} results from LOCAL CACHE")
                _searchResults.value = localResults
                _isShowingLocalResults.value = true
            } else {
                _searchResults.value = emptyList()
                _isShowingLocalResults.value = false
            }

            _isLoadingMore.value = true

            val validCategoryId = getValidCategoryId()

            val result = productsRepository.searchItems(
                query = query,
                categoryId = validCategoryId,
                genderId = _appliedFilters.value.gender,
                brandId = _appliedFilters.value.brand?.firstOrNull(),
                sizeId = _appliedFilters.value.size,
                colorId = _appliedFilters.value.color?.firstOrNull(),
                conditionId = _appliedFilters.value.condition,
                minPrice = _appliedFilters.value.minPrice,
                maxPrice = _appliedFilters.value.maxPrice,
                sort = _currentSortType,
                page = PAGE_DEFAULT,
                perPage = PER_PAGE_DEFAULT
            )

            _isLoadingMore.value = false

            when (result) {
                is Result.Success -> {
                    val serverItems = result.data.items
                    _serverItemsCount.value = serverItems.size
                    Timber.tag(LogTags.VIEW_MODEL).d("✅ Server returned ${serverItems.size} results")

                    if (serverItems.isNotEmpty()) {
                        val currentResults = _searchResults.value.toMutableList()
                        val existingIds = currentResults.map { it.id }.toSet()
                        val newItems = serverItems.filter { it.id !in existingIds }

                        if (newItems.isNotEmpty()) {
                            val merged = currentResults + newItems
                            _searchResults.value = merged
                            Timber.tag(LogTags.VIEW_MODEL).d("📦 Added ${newItems.size} new items")
                        }
                        _isShowingLocalResults.value = false
                    } else if (!_isShowingLocalResults.value && _searchResults.value.isEmpty()) {
                        _searchResults.value = emptyList()
                    }
                }
                is Result.Error -> {
                    Timber.tag(LogTags.VIEW_MODEL).e("❌ Server search failed: ${result.exception.message}")
                    if (!_isShowingLocalResults.value && _searchResults.value.isEmpty()) {
                        _error.value = "Failed to load results"
                    }
                }
            }
        }
    }

    fun searchItemsRanked(query: String, categoryId: Int?) {
        _searchQuery.value = query
        searchJob?.cancel()

        if (query.length < MIN_SEARCH_LENGTH) {
            Timber.tag(LogTags.VIEW_MODEL).d("Query too short, clearing results")
            _searchResults.value = emptyList()
            _searchRelevanceGroups.value = RelevanceGroups(emptyList(), emptyList(), emptyList())
            _isShowingLocalResults.value = false
            _serverItemsCount.value = 0
            return
        }

        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_DELAY_MS)

            Timber.tag(LogTags.VIEW_MODEL).d("🔍 Ranked search for: $query")

            val localResults = searchLocalCache(query, getValidCategoryId())
            if (localResults.isNotEmpty()) {
                Timber.tag(LogTags.VIEW_MODEL).d("⚡ ${localResults.size} results from LOCAL CACHE")
                _searchResults.value = localResults
                _isShowingLocalResults.value = true
            } else {
                _searchResults.value = emptyList()
                _isShowingLocalResults.value = false
            }

            _isLoadingMore.value = true

            try {
                val schoolId = getSchoolId()
                if (schoolId == null) {
                    Timber.tag(LogTags.VIEW_MODEL).e("❌ No school ID available")
                    _isLoadingMore.value = false
                    return@launch
                }

                val validCategoryId = getValidCategoryId()
                val serverResult = withTimeout(SEARCH_TIMEOUT_MS) {
                    productsRepository.searchItemsRanked(
                        query = query,
                        schoolId = schoolId,
                        categoryId = validCategoryId,
                        subCategoryId = currentSubCategoryId,
                        minPrice = _appliedFilters.value.minPrice,
                        maxPrice = _appliedFilters.value.maxPrice,
                        page = PAGE_DEFAULT,
                        perPage = PER_PAGE_DEFAULT
                    )
                }

                _isLoadingMore.value = false

                when (serverResult) {
                    is Result.Success -> {
                        val rankedResult = serverResult.data
                        val serverItems = rankedResult.items
                        _serverItemsCount.value = serverItems.size

                        Timber.tag(LogTags.VIEW_MODEL).d("✅ Ranked search returned ${serverItems.size} items")

                        if (serverItems.isNotEmpty()) {
                            val currentResults = _searchResults.value.toMutableList()
                            val existingIds = currentResults.map { it.id }.toSet()
                            val newItems = serverItems.filter { it.id !in existingIds }

                            if (newItems.isNotEmpty()) {
                                val merged = currentResults + newItems
                                _searchResults.value = merged
                                Timber.tag(LogTags.VIEW_MODEL).d("📦 Added ${newItems.size} new items")
                            }
                            _isShowingLocalResults.value = false
                        } else if (!_isShowingLocalResults.value && _searchResults.value.isEmpty()) {
                            _searchResults.value = emptyList()
                        }
                    }
                    is Result.Error -> {
                        Timber.tag(LogTags.VIEW_MODEL).e("❌ Server search failed: ${serverResult.exception.message}")
                        if (!_isShowingLocalResults.value && _searchResults.value.isEmpty()) {
                            _error.value = "Failed to load results"
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                _isLoadingMore.value = false
                Timber.tag(LogTags.VIEW_MODEL).e("⏱️ Server search timed out")
                if (!_isShowingLocalResults.value && _searchResults.value.isEmpty()) {
                    _error.value = "Search timed out. Please try again."
                }
            } catch (e: Exception) {
                _isLoadingMore.value = false
                Timber.tag(LogTags.VIEW_MODEL).e("❌ Search error: ${e.message}")
                if (!_isShowingLocalResults.value && _searchResults.value.isEmpty()) {
                    _error.value = "Failed to load results: ${e.message}"
                }
            }
        }
    }

    private suspend fun getSchoolId(): Int? {
        return appPreferences.schoolId.first()
    }

    private fun getValidCategoryId(): Int? {
        val categoryId = currentCategoryId ?: _appliedFilters.value.categoryId
        return if (categoryId == null || categoryId <= 0) null else categoryId
    }

    private fun searchLocalCache(query: String, categoryId: Int?): List<Item> {
        val currentProducts = _products.value
        if (currentProducts.isEmpty()) {
            Timber.tag(LogTags.VIEW_MODEL).d("No cached products available")
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
            .take(PER_PAGE_DEFAULT)

        Timber.tag(LogTags.VIEW_MODEL).d("🔍 Local search found ${results.size} matches from ${currentProducts.size} cached items")
        return results
    }

    // ==================== Load Products Methods ====================

    fun loadProducts(
        sectionType: String,
        period: String? = null,
        navCategoryId: Int? = null,
        subCategoryId: Int? = null,
        preSelectedSportTypeId: Int? = null,
        preSelectedGearType: String? = null
    ) {
        Timber.tag(LogTags.VIEW_MODEL).d("🔄 loadProducts: section='$sectionType', subCategoryId=$subCategoryId, navCategoryId=$navCategoryId")

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
            normalizedType == "sports" || normalizedType == "sport" -> incomingCategoryId ?: CATEGORY_ID_SPORT
            else -> savedCategoryId ?: _appliedFilters.value.categoryId ?: incomingCategoryId
        }

        currentSectionType = normalizedType
        currentPeriod = period
        if (effectiveCategoryId != null) lastLoadedCategoryId = effectiveCategoryId

        loadFilterConfigIfNeeded(effectiveCategoryId)
        loadProductsInternal(effectiveCategoryId, normalizedType)
    }

    fun reloadCurrentSection() {
        currentSearchQuery = null
        _searchResults.value = emptyList()
        loadProductsInternal(_appliedFilters.value.categoryId, currentSectionType)
    }

    fun reloadLocalFiltersOnly() {
        viewModelScope.launch {
            _products.value = _products.value
        }
    }

    private fun loadProductsInternal(categoryId: Int?, sectionType: String? = null) {
        val effectiveSubCategoryId = currentSubCategoryId

        Timber.tag(LogTags.VIEW_MODEL).d("🚀 loadProductsInternal START → section=$sectionType, subCategoryId=$effectiveSubCategoryId, categoryId=$categoryId")

        loadProductsJob?.cancel()

        loadProductsJob = viewModelScope.launch {
            _isNewSectionLoading.value = true
            _isLoading.value = true
            _error.value = null
            _products.value = emptyList()

            val result: Result<PaginatedResponse<Item>> = when {
                sectionType == "uniform" || sectionType == "uniforms" -> {
                    Timber.tag(LogTags.VIEW_MODEL).d("👕 Loading Uniforms with subCategoryId=$effectiveSubCategoryId")
                    productsRepository.getEssentialsAll(
                        page = PAGE_DEFAULT,
                        category = "Uniforms",
                        subCategoryId = effectiveSubCategoryId,
                        perPage = PER_PAGE_DEFAULT,
                        conditionId = _appliedFilters.value.condition,
                        minPrice = _appliedFilters.value.minPrice,
                        maxPrice = _appliedFilters.value.maxPrice
                    )
                }
                sectionType == "sports" || sectionType == "sport" -> {
                    Timber.tag(LogTags.VIEW_MODEL).d("🏅 Loading Sports with subCategoryId=$effectiveSubCategoryId")
                    productsRepository.getEssentialsAll(
                        page = PAGE_DEFAULT,
                        category = "Sports",
                        subCategoryId = effectiveSubCategoryId,
                        perPage = PER_PAGE_DEFAULT,
                        conditionId = _appliedFilters.value.condition,
                        minPrice = _appliedFilters.value.minPrice,
                        maxPrice = _appliedFilters.value.maxPrice
                    )
                }
                sectionType == "accessories" -> {
                    Timber.tag(LogTags.VIEW_MODEL).d("🎒 Loading Accessories")
                    productsRepository.getEssentialsAll(
                        page = PAGE_DEFAULT,
                        category = "Accessories",
                        subCategoryId = null,
                        perPage = PER_PAGE_DEFAULT,
                        conditionId = _appliedFilters.value.condition,
                        minPrice = _appliedFilters.value.minPrice,
                        maxPrice = _appliedFilters.value.maxPrice
                    )
                }
                else -> {
                    Timber.tag(LogTags.VIEW_MODEL).d("✨ Loading Recommended")
                    productsRepository.getRecommendedAll(
                        page = PAGE_DEFAULT,
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
                    Timber.tag(LogTags.VIEW_MODEL).d("✅ SUCCESS: Loaded ${items.size} items | section=$sectionType | subCategory=$effectiveSubCategoryId")
                }
                is Result.Error -> {
                    _error.value = res.exception.message ?: "Failed to load items"
                    Timber.tag(LogTags.VIEW_MODEL).e(res.exception, "❌ Failed to load products")
                }
            }

            _isLoading.value = false
            _isNewSectionLoading.value = false
            Timber.tag(LogTags.VIEW_MODEL).d("🏁 Loading finished")
        }
    }

    // ==================== Filter Methods ====================

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
                    Timber.tag(LogTags.VIEW_MODEL).d("Loaded global filter config: ${result.data.filterGroups.size} groups")
                }
                is Result.Error -> {
                    Timber.tag(LogTags.VIEW_MODEL).e("Failed to load global filter config: ${result.exception.message}")
                }
            }
        }
    }

    private fun loadFilterConfigIfNeeded(categoryId: Int?) {
        val currentConfig = _filterConfig.value
        if (categoryId != null && categoryId > 0 && currentConfig?.categoryId == categoryId) {
            Timber.tag(LogTags.VIEW_MODEL).d("Filter config already loaded for category $categoryId")
            return
        }
        if (categoryId == null && currentConfig?.categoryId == null && currentConfig != null) {
            Timber.tag(LogTags.VIEW_MODEL).d("Global filter config already loaded")
            return
        }

        loadFilterJob?.cancel()
        loadFilterJob = viewModelScope.launch {
            val result = if (categoryId != null && categoryId > 0) {
                Timber.tag(LogTags.VIEW_MODEL).d("Loading CATEGORY-SPECIFIC filters for ID: $categoryId")
                filterRepository.getFilterConfig(categoryId)
            } else {
                Timber.tag(LogTags.VIEW_MODEL).d("Loading GLOBAL filters")
                filterRepository.getGlobalFilterConfig()
            }

            when (result) {
                is Result.Success -> {
                    _filterConfig.value = result.data
                    if (categoryId == null) {
                        globalFilterConfig = result.data
                    }
                    Timber.tag(LogTags.VIEW_MODEL).d("Filter config loaded: ${result.data.filterGroups.size} groups")
                    result.data.filterGroups.forEach { group ->
                        Timber.tag(LogTags.VIEW_MODEL).d("  - ${group.name}: ${group.options.size} options")
                    }
                }
                is Result.Error -> {
                    Timber.tag(LogTags.VIEW_MODEL).e("Filter config failed: ${result.exception.message}")
                }
            }
        }
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

        if (currentSearchQuery != null) {
            searchInCurrentSection(currentSearchQuery!!)
        } else {
            loadProductsInternal(updated.categoryId)
        }
    }

    fun updatePriceRange(min: Float, max: Float) {
        val newFilters = _appliedFilters.value.copy(
            minPrice = if (min > PRICE_MIN) min else null,
            maxPrice = if (max < PRICE_MAX) max else null
        )
        _appliedFilters.value = newFilters

        Timber.tag(LogTags.VIEW_MODEL).d("📊 Price filter updated → R${min.toInt()} - R${max.toInt()}")

        if (currentSearchQuery != null) {
            searchInCurrentSection(currentSearchQuery!!)
        } else {
            loadProductsInternal(_appliedFilters.value.categoryId)
        }
    }

    fun applyFilters() {
        Timber.tag(LogTags.VIEW_MODEL).d("Applying all filters including price")

        if (currentSearchQuery != null) {
            searchInCurrentSection(currentSearchQuery!!)
        } else {
            loadProductsInternal(_appliedFilters.value.categoryId)
        }
    }

    fun resetFilters() {
        _appliedFilters.value = AppliedFilters()
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
            searchInCurrentSection(currentSearchQuery!!)
        } else {
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

    fun clearSearchResults() {
        _searchResults.value = emptyList()
        currentSearchQuery = null
    }

    private fun getCacheKey(): String {
        return "${currentSectionType}_${currentCategoryId}_${_appliedFilters.value}"
    }

    override fun onCleared() {
        loadProductsJob?.cancel()
        loadFilterJob?.cancel()
        searchJob?.cancel()
        super.onCleared()
    }
}