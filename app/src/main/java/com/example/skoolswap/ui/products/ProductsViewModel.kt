package com.example.skoolswap.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skoolswap.data.repository.ProductsRepository
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.repository.ProductsRepositoryInterface
import com.example.skoolswap.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductsViewModel @Inject constructor(
    private val productsRepository: ProductsRepositoryInterface
) : ViewModel() {

    private val _products = MutableStateFlow<List<Item>>(emptyList())
    val products: StateFlow<List<Item>> = _products

    private val _pagination = MutableStateFlow<Pagination>(Pagination())
    val pagination: StateFlow<Pagination> = _pagination

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    var currentMinPrice = 0f
    var currentMaxPrice = 1000f
    private var currentSectionType = ""
    private var currentPeriod = ""
    private var currentPage = 1
    private var currentSort = "recommended"
    private val selectedFilters = mutableMapOf<String, Any>()

    fun loadProducts(sectionType: String, period: String? = null, page: Int = 1) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            currentSectionType = sectionType
            currentPeriod = period ?: ""
            currentPage = page

            val result = when (sectionType) {
                "recommended" -> productsRepository.getRecommendedAll(page)
                "essentials" -> productsRepository.getEssentialsAll(
                    page,
                    selectedFilters["category"] as? String
                )
                "trending" -> productsRepository.getTrendingAll(
                    period ?: "today",
                    page
                )
                "recent" -> productsRepository.getRecentAll(
                    period ?: "all",
                    page
                )
                else -> productsRepository.getRecommendedAll(page)
            }

            when (result) {
                is Result.Success -> {
                    if (page == 1) {
                        _products.value = result.data.items
                    } else {
                        _products.value = _products.value + result.data.items
                    }
                    _pagination.value = Pagination(
                        currentPage = result.data.pagination.currentPage,
                        totalPages = result.data.pagination.totalPages,
                        totalCount = result.data.pagination.totalCount,
                        perPage = result.data.pagination.perPage
                    )
                }
                is Result.Error -> {
                    _error.value = result.exception.message
                }
            }

            _isLoading.value = false
        }
    }

    fun loadMore() {
        if (currentPage < _pagination.value.totalPages) {
            loadProducts(currentSectionType, currentPeriod, currentPage + 1)
        }
    }

    fun sortBy(sortType: String) {
        currentSort = sortType
        val sorted = when (sortType) {
            "price_low" -> _products.value.sortedBy { it.price }
            "price_high" -> _products.value.sortedByDescending { it.price }
            "newest" -> _products.value.sortedByDescending { it.createdAt }
            else -> _products.value
        }
        _products.value = sorted
    }

    fun applyFilters() {
        // Reload products with current filters
        loadProducts(currentSectionType, currentPeriod, 1)
    }

    fun resetFilters() {
        selectedFilters.clear()
        currentMinPrice = 0f
        currentMaxPrice = 1000f
        loadProducts(currentSectionType, currentPeriod, 1)
    }

    fun updateFilter(key: String, value: Any) {
        selectedFilters[key] = value
    }

    fun trackClick(itemId: String, source: String, position: Int) {
        viewModelScope.launch {
            productsRepository.trackClick(itemId, source, position)
        }
    }
    fun updatePriceRange(min: Float, max: Float) {
        currentMinPrice = min
        currentMaxPrice = max
        selectedFilters["min_price"] = min
        selectedFilters["max_price"] = max

        // Uncomment if you want to auto-reload when price range changes
        // applyFilters()
    }
    // Mock data methods - replace with actual repository calls
    fun getCategories(): List<String> = listOf("Uniforms", "Sports", "Accessories")
    fun getSchools(): List<String> = listOf("Springfield Primary", "Riverside Primary")
    fun getConditions(): List<String> = listOf("New", "Like New", "Good", "Fair")
    fun getColors(): List<String> = listOf("Red", "Blue", "Green", "Black", "White")
    fun getBrands(): List<String> = listOf("Nike", "Adidas", "Puma", "Reebok")

    data class Pagination(
        val currentPage: Int = 1,
        val totalPages: Int = 1,
        val totalCount: Int = 0,
        val perPage: Int = 20
    )
}