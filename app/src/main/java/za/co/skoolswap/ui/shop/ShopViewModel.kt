package za.co.skoolswap.ui.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import za.co.skoolswap.common.constants.AppConstants.LogTags
import za.co.skoolswap.domain.model.Item
import za.co.skoolswap.domain.model.Shop
import za.co.skoolswap.domain.repository.ItemRepositoryInterface
import za.co.skoolswap.domain.repository.ShopRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ShopViewModel @Inject constructor(
    private val shopRepository: ShopRepositoryInterface,
    private val itemRepository: ItemRepositoryInterface
) : ViewModel() {

    companion object {
        private const val ALL_ITEMS = "All Items"
        private const val STATUS_SOLD = "sold"
        private const val STATUS_AVAILABLE = "available"
    }

    val currentShop: StateFlow<Shop?> = shopRepository.currentShop

    private val _allItems = MutableStateFlow<List<Item>>(emptyList())
    val allItems: StateFlow<List<Item>> = _allItems.asStateFlow()

    private val _filteredItems = MutableStateFlow<List<Item>>(emptyList())
    val filteredItems: StateFlow<List<Item>> = _filteredItems.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    private val _selectedCategory = MutableStateFlow(ALL_ITEMS)
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
        Timber.tag(LogTags.VIEW_MODEL).d("loadMyShopItems called")
        viewModelScope.launch {
            _isLoading.value = true
            val result = itemRepository.getMyShopItems()
            result.onSuccess { items ->
                _allItems.value = items
                extractCategoriesFromItems(items)
                filterItemsByCategory()
                Timber.tag(LogTags.VIEW_MODEL).d("Loaded ${items.size} items")
                _error.value = null
            }.onFailure { e ->
                _error.value = e.message
                Timber.tag(LogTags.VIEW_MODEL).e(e, "Failed to load items")
            }
            _isLoading.value = false
        }
    }

    fun removeItemLocally(itemId: String) {
        val currentItems = _allItems.value
        val updatedItems = currentItems.filter { it.id != itemId }
        _allItems.value = updatedItems
        Timber.tag(LogTags.VIEW_MODEL).d("Removed item $itemId locally, remaining: ${updatedItems.size}")
    }

    private fun extractCategoriesFromItems(items: List<Item>) {
        val actualCategories = items.mapNotNull { item ->
            getCategoryFromTypeId(item.itemTypeId)
        }.distinct().sorted()

        val categoryList = mutableListOf(ALL_ITEMS)
        categoryList.addAll(actualCategories)

        if (actualCategories.isEmpty()) {
            categoryList.addAll(listOf("Uniform", "Sport", "Stationary", "Accessories", "Books"))
        }

        _categories.value = categoryList.distinct()
    }

    private fun filterItemsByCategory() {
        val selected = _selectedCategory.value
        val items = _allItems.value

        val filtered = if (selected == ALL_ITEMS) {
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
        filterItemsByCategory()
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

    fun toggleItemSoldStatus(itemId: String, markAsSold: Boolean) {
        viewModelScope.launch {
            Timber.tag(LogTags.VIEW_MODEL).d("🔄 Toggling item $itemId to sold=$markAsSold")

            val currentItems = _allItems.value.toMutableList()
            val index = currentItems.indexOfFirst { it.id == itemId }

            if (index != -1) {
                val newStatus = if (markAsSold) STATUS_SOLD else STATUS_AVAILABLE
                val newQuantity = if (markAsSold) 0 else 1

                val updatedItem = currentItems[index].copy(
                    status = newStatus,
                    quantity = newQuantity
                )
                currentItems[index] = updatedItem
                _allItems.value = currentItems

                itemRepository.updateItemStatus(itemId, newStatus)
                    .onSuccess {
                        Timber.tag(LogTags.VIEW_MODEL).d("✅ Successfully updated status to $newStatus")
                    }
                    .onFailure { error ->
                        Timber.tag(LogTags.VIEW_MODEL).e(error, "❌ Failed to update status")
                        loadMyShopItems()
                    }
            }
        }
    }
    init {
        // ✅ Preload shop data when ViewModel is created
        viewModelScope.launch {
            try {
                Timber.tag(LogTags.VIEW_MODEL).d("🔄 Preloading shop data from init...")
                loadMyShop(showLoading = false)
                loadMyShopItems()
                Timber.tag(LogTags.VIEW_MODEL).d("✅ Shop data preloaded from init")
            } catch (e: Exception) {
                Timber.tag(LogTags.VIEW_MODEL).e(e, "⚠️ Failed to preload shop from init")
            }
        }
    }
    fun clearError() {
        _error.value = null
    }
}