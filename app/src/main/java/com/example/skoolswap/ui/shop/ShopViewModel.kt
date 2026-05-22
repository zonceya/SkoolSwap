package com.example.skoolswap.ui.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.model.Shop
import com.example.skoolswap.domain.repository.ItemRepositoryInterface
import com.example.skoolswap.domain.repository.ShopRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShopViewModel @Inject constructor(
    private val shopRepository: ShopRepositoryInterface,
    private val itemRepository: ItemRepositoryInterface
) : ViewModel() {

    val currentShop: StateFlow<Shop?> = shopRepository.currentShop

    private val _allItems = MutableStateFlow<List<Item>>(emptyList())
    val allItems: StateFlow<List<Item>> = _allItems.asStateFlow()

    private val _filteredItems = MutableStateFlow<List<Item>>(emptyList())
    val filteredItems: StateFlow<List<Item>> = _filteredItems.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All Items")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadMyShop(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) _isLoading.value = true
            shopRepository.getMyShop()
            if (showLoading) _isLoading.value = false
        }
    }

    fun loadMyShopItems() {
        viewModelScope.launch {
            val result = itemRepository.getMyShopItems()
            result.onSuccess { items ->
                if (items.isNotEmpty()) {
                    _allItems.value = items
                } else {
                    // Use sample data if API returns empty
                    _allItems.value = getSampleItems()
                }
                extractCategoriesFromItems(_allItems.value)
                filterItemsByCategory()
            }.onFailure { error ->
                // Use sample data on error
                _allItems.value = getSampleItems()
                extractCategoriesFromItems(_allItems.value)
                filterItemsByCategory()
                _error.value = error.message
            }
        }
    }

    private fun extractCategoriesFromItems(items: List<Item>) {
        // Get actual categories from items based on itemTypeId
        val actualCategories = items.mapNotNull { item ->
            getCategoryFromTypeId(item.itemTypeId)
        }.distinct().sorted()

        val categoryList = mutableListOf("All Items")
        categoryList.addAll(actualCategories)

        // If no categories found, use predefined ones
        if (actualCategories.isEmpty()) {
            categoryList.addAll(listOf("Uniform", "Sport", "Stationary", "Accessories", "Books"))
        }

        _categories.value = categoryList.distinct()
    }

    // FIXED: Filter using itemTypeId, not category field
    private fun filterItemsByCategory() {
        val selected = _selectedCategory.value
        val items = _allItems.value

        val filtered = if (selected == "All Items") {
            items
        } else {
            items.filter { item ->
                val categoryName = getCategoryFromTypeId(item.itemTypeId)
                categoryName == selected
            }
        }

        _filteredItems.value = filtered
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        filterItemsByCategory()  // Instant client-side filter
    }

    fun getCategoryFromTypeId(typeId: Int?): String? {
        return when (typeId) {
            1 -> "Uniform"
            2 -> "Sport"
            3 -> "Stationary"
            4 -> "Accessories"
            5 -> "Books"
            else -> null
        }
    }

    suspend fun updateShopDisplayName(displayName: String): Result<Unit> {
        return try {
            _isLoading.value = true
            val result = shopRepository.updateShopDisplayName(displayName)
            if (result.isSuccess) {
                loadMyShop(showLoading = false)
                Result.success(Unit)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Failed"))
            }
        } finally {
            _isLoading.value = false
        }
    }

    fun refresh() {
        loadMyShop(showLoading = true)
        loadMyShopItems()
    }

    fun clearError() { _error.value = null }

    // ============ ADD THESE TWO FUNCTIONS ============

    private fun getSampleItems(): List<Item> {
        return listOf(
            createSampleItem("1", "School Bag", 59.00, 4, "Medium", "Good"),
            createSampleItem("2", "Acer Laptop", 900.00, 4, "15 inch", "Used"),
            createSampleItem("3", "History Book - Grade 11", 200.00, 5, "Paperback", "Good"),
            createSampleItem("4", "Math Textbook", 150.00, 5, "Hardcover", "Like New"),
            createSampleItem("5", "Soccer Ball", 25.00, 2, "Size 5", "Good"),
            createSampleItem("6", "Notebook Pack", 45.00, 3, "A4", "New"),
            createSampleItem("7", "Wireless Headphones", 120.00, 4, "Wireless", "Good"),
            createSampleItem("8", "School Uniform Shirt", 85.00, 1, "Large", "Excellent")
        )
    }

    private fun createSampleItem(
        id: String,
        name: String,
        price: Double,
        typeId: Int,
        size: String,
        condition: String
    ): Item {
        return Item(
            id = id,
            shopId = 1L,
            name = name,
            description = "$size, Condition: $condition",
            price = price,
            quantity = 1,
            status = "active",
            createdAt = "",
            itemTypeId = typeId,  // This is key for category filtering!
            images = emptyList()
        )
    }

}