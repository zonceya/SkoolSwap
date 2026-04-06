package com.example.skoolswap.ui.products

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skoolswap.domain.model.AppliedFilters
import com.example.skoolswap.domain.model.FilterConfig
import com.example.skoolswap.domain.model.FilterGroup
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.repository.FilterRepositoryInterface
import com.example.skoolswap.domain.repository.ProductsRepositoryInterface
import com.example.skoolswap.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductsViewModel @Inject constructor(
    private val productsRepository: ProductsRepositoryInterface,
    private val filterRepository: FilterRepositoryInterface
) : ViewModel() {

    private val _products = MutableStateFlow<List<Item>>(emptyList())
    val products: StateFlow<List<Item>> = _products

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

    private var loadProductsJob: Job? = null
    private var loadFilterJob: Job? = null

    // ==================== Public Methods ====================

    fun getSavedCategoryId(): Int? = savedCategoryId
    fun getSavedCategoryName(): String? = savedCategoryName

    fun setSavedCategory(categoryId: Int, categoryName: String) {
        savedCategoryId = categoryId
        savedCategoryName = categoryName
        Log.d("ProductsViewModel", "Saved category: $categoryName (ID: $categoryId)")
    }

    fun clearSavedCategory() {
        savedCategoryId = null
        savedCategoryName = null
        Log.d("ProductsViewModel", "Cleared saved category")
    }

    fun loadProducts(sectionType: String, period: String? = null, navCategoryId: Int? = null) {
        val incomingCategoryId = if (navCategoryId == -1) null else navCategoryId

        // Priority: saved category > applied filters > last loaded > navigation argument
        val effectiveCategoryId = savedCategoryId
            ?: _appliedFilters.value.categoryId
            ?: lastLoadedCategoryId
            ?: incomingCategoryId

        currentSectionType = sectionType
        currentPeriod = period
        if (effectiveCategoryId != null) {
            lastLoadedCategoryId = effectiveCategoryId
        }

        Log.d("ProductsViewModel", "loadProducts → effectiveCategoryId=$effectiveCategoryId (saved=$savedCategoryId, applied=${_appliedFilters.value.categoryId}, last=$lastLoadedCategoryId, nav=$incomingCategoryId)")

        loadFilterConfigIfNeeded(effectiveCategoryId)
        loadProductsInternal(effectiveCategoryId)
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
                    Log.d("ProductsViewModel", "Loaded global filter config: ${result.data.filterGroups.size} groups")
                }
                is Result.Error -> {
                    Log.e("ProductsViewModel", "Failed to load global filter config: ${result.exception.message}")
                }
            }
        }
    }

    fun updateFilter(key: String, value: Any?) {
        val updated = when (key) {
            "category" -> {
                val categoryId = value as? Int
                if (categoryId != null && categoryId > 0) {
                    // Find category name
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

        loadProductsInternal(updated.categoryId)
    }

    fun updatePriceRange(min: Float, max: Float) {
        _appliedFilters.value = _appliedFilters.value.copy(minPrice = min, maxPrice = max)
        loadProductsInternal(_appliedFilters.value.categoryId)
    }

    fun applyFilters() {
        loadProductsInternal(_appliedFilters.value.categoryId)
    }

    fun resetFilters() {
        _appliedFilters.value = AppliedFilters()
        lastLoadedCategoryId = null
        clearSavedCategory()
        loadFilterConfigIfNeeded(null)
        loadProductsInternal(null)
    }

    fun sortBy(sortType: String) {
        val sorted = when (sortType) {
            "price_low" -> _products.value.sortedBy { it.price }
            "price_high" -> _products.value.sortedByDescending { it.price }
            "newest" -> _products.value.sortedByDescending { it.createdAt }
            else -> _products.value
        }
        _products.value = sorted
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
        // Check if we already have the config loaded
        val currentConfig = _filterConfig.value
        if (categoryId != null && categoryId > 0 &&
            currentConfig?.categoryId == categoryId) {
            Log.d("ProductsViewModel", "Filter config already loaded for category $categoryId")
            return
        }
        if (categoryId == null && currentConfig?.categoryId == null && currentConfig != null) {
            Log.d("ProductsViewModel", "Global filter config already loaded")
            return
        }

        loadFilterJob?.cancel()
        loadFilterJob = viewModelScope.launch {
            val result = if (categoryId != null && categoryId > 0) {
                Log.d("ProductsViewModel", "Loading CATEGORY-SPECIFIC filters for ID: $categoryId")
                filterRepository.getFilterConfig(categoryId)
            } else {
                Log.d("ProductsViewModel", "Loading GLOBAL filters")
                filterRepository.getGlobalFilterConfig()
            }

            when (result) {
                is Result.Success -> {
                    _filterConfig.value = result.data
                    if (categoryId == null) {
                        globalFilterConfig = result.data
                    }
                    Log.d("ProductsViewModel", "Filter config loaded: ${result.data.filterGroups.size} groups")
                    result.data.filterGroups.forEach { group ->
                        Log.d("ProductsViewModel", "  - ${group.name}: ${group.options.size} options")
                    }
                }
                is Result.Error -> {
                    Log.e("ProductsViewModel", "Filter config failed: ${result.exception.message}")
                }
            }
        }
    }

    private fun loadProductsInternal(categoryId: Int?) {
        loadProductsJob?.cancel()
        loadProductsJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val result = when (currentSectionType) {
                "recommended" -> productsRepository.getRecommendedAll(
                    page = 1,
                    categoryId = categoryId ?: _appliedFilters.value.categoryId,
                    conditionId = _appliedFilters.value.condition,
                    minPrice = _appliedFilters.value.minPrice,
                    maxPrice = _appliedFilters.value.maxPrice
                )
                "trending" -> productsRepository.getTrendingAll(
                    period = currentPeriod ?: "today",
                    page = 1,
                    categoryId = categoryId ?: _appliedFilters.value.categoryId,
                    conditionId = _appliedFilters.value.condition,
                    minPrice = _appliedFilters.value.minPrice,
                    maxPrice = _appliedFilters.value.maxPrice
                )
                "recent" -> productsRepository.getRecentAll(
                    period = currentPeriod ?: "all",
                    page = 1,
                    categoryId = categoryId ?: _appliedFilters.value.categoryId,
                    conditionId = _appliedFilters.value.condition,
                    minPrice = _appliedFilters.value.minPrice,
                    maxPrice = _appliedFilters.value.maxPrice
                )
                else -> productsRepository.getRecommendedAll(
                    page = 1,
                    categoryId = categoryId ?: _appliedFilters.value.categoryId,
                    conditionId = _appliedFilters.value.condition,
                    minPrice = _appliedFilters.value.minPrice,
                    maxPrice = _appliedFilters.value.maxPrice
                )
            }

            when (result) {
                is Result.Success -> {
                    _products.value = result.data.items
                    Log.d("ProductsViewModel", "Loaded ${result.data.items.size} items")
                }
                is Result.Error -> {
                    _error.value = result.exception.message
                    Log.e("ProductsViewModel", "Error loading products: ${result.exception.message}")
                }
            }
            _isLoading.value = false
        }
    }

    override fun onCleared() {
        loadProductsJob?.cancel()
        loadFilterJob?.cancel()
        super.onCleared()
    }
}