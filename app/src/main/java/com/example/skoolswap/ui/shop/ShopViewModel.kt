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

    // Current shop from repository
    val currentShop: StateFlow<Shop?> = shopRepository.currentShop

    // Loading state for operations
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Separate loading state for initial load
    private val _isInitialLoading = MutableStateFlow(false)
    val isInitialLoading: StateFlow<Boolean> = _isInitialLoading.asStateFlow()

    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()


    // Update success state
    private val _updateSuccess = MutableStateFlow(false)
    val updateSuccess: StateFlow<Boolean> = _updateSuccess.asStateFlow()
    private val _isLoadingItems = MutableStateFlow(false)
    val isLoadingItems: StateFlow<Boolean> = _isLoadingItems.asStateFlow()
    private val _shopItems = MutableStateFlow<List<Item>>(emptyList())
    val shopItems: StateFlow<List<Item>> = _shopItems.asStateFlow()
    init {
        // Load shop data when ViewModel is created
        loadMyShop(showLoading = false)
        loadMyShopItems()
    }

    // MODIFIED: Added showLoading parameter
    fun loadMyShop(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) {
                _isLoading.value = true
            }
            _error.value = null

            val result = shopRepository.getMyShop()
            result.onFailure { throwable ->
                _error.value = "Failed to load shop: ${throwable.message}"
            }

            if (showLoading) {
                _isLoading.value = false
            }
        }
    }
    fun loadMyShopItems() {
        viewModelScope.launch {
            _isLoadingItems.value = true
            val result = itemRepository.getMyShopItems() // You'll need to add this to ItemRepository
            result.onSuccess { items ->
                _shopItems.value = items
            }.onFailure { error ->
                _error.value = "Failed to load items: ${error.message}"
            }
            _isLoadingItems.value = false
        }
    }
    // FIXED: Clear loading states properly
    suspend fun updateShopDisplayName(displayName: String): Result<Unit> {
        return try {
            _isLoading.value = true
            _error.value = null
            _updateSuccess.value = false

            val result = shopRepository.updateShopDisplayName(displayName)

            if (result.isSuccess) {
                _updateSuccess.value = true
                // Refresh data after update
                loadMyShop(showLoading = false)
                Result.success(Unit)
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to update shop"
                Result.failure(Exception(_error.value))
            }
        } catch (e: Exception) {
            _error.value = "Update failed: ${e.message}"
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSuccess() {
        _updateSuccess.value = false
    }

    fun refresh() {
        loadMyShop(showLoading = true)
    }
}