package com.example.skoolswap.ui.item

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.model.ItemType
import com.example.skoolswap.domain.repository.ItemRepositoryInterface
import com.example.skoolswap.domain.repository.ItemTypeRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateItemViewModel @Inject constructor(
    private val itemRepository: ItemRepositoryInterface,
    private val itemTypeRepository: ItemTypeRepositoryInterface
) : ViewModel() {

    private val _uiState = MutableStateFlow<CreateItemUiState>(CreateItemUiState.Idle)
    val uiState: StateFlow<CreateItemUiState> = _uiState.asStateFlow()

    private val _images = MutableStateFlow<List<Uri>>(emptyList())
    val images: StateFlow<List<Uri>> = _images.asStateFlow()

    private val _createdItem = MutableStateFlow<ItemState?>(null)
    val createdItem: StateFlow<ItemState?> = _createdItem.asStateFlow()
    private val _itemTypes = MutableStateFlow<List<ItemType>>(emptyList())
    val itemTypes: StateFlow<List<ItemType>> = _itemTypes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()


    init {
        loadItemTypes()
    }
    private fun loadItemTypes() {
        viewModelScope.launch {
            // FIX 1: Remove .flowOn(Dispatchers.IO) from here
            // The repository already handles threading
            itemTypeRepository.getItemTypes().collect { itemTypes ->
                _itemTypes.value = itemTypes

                // Refresh from API if empty
                if (itemTypes.isEmpty()) {
                    refreshFromApi()
                }
            }
        }
    }
    suspend fun refreshFromApi() {
        _isLoading.value = true
        // FIX 2: Use the result properly
        val result = itemTypeRepository.refreshItemTypes()
        result.onFailure { error ->
            // Handle error
            Log.e("CreateItemViewModel", "Failed to refresh item types", error)
        }
        _isLoading.value = false
    }
    fun addImage(uri: Uri) {
        val currentImages = _images.value.toMutableList()
        if (currentImages.size < 3) { // Max 3 images as per API
            currentImages.add(uri)
            _images.value = currentImages
        }
    }

    fun removeImage(uri: Uri) {
        val currentImages = _images.value.toMutableList()
        currentImages.remove(uri)
        _images.value = currentImages
    }

    fun clearImages() {
        _images.value = emptyList()
    }

    fun createItem(
        context: Context,
        name: String,
        description: String,
        price: Double,
        quantity: Int,
        itemTypeId: Int,
        brandId: Int,
        sizeId: Int,
        schoolId: Int,
        conditionId: Int,
        locationId: Int,
        provinceId: Int,
        genderId: Int,
        color: String? = null,
        sizeMeta: String? = null,
        tagIds: List<Int>? = null
    ) {
        _uiState.value = CreateItemUiState.Loading

        viewModelScope.launch {
            val result = itemRepository.createItemWithImages(
                context = context,
                name = name,
                description = description,
                itemTypeId = itemTypeId,
                brandId = brandId,
                price = price,
                quantity = quantity,
                itemConditionId = conditionId,
                provinceId = provinceId,
                locationId = locationId,
                genderId = genderId,
                schoolId = schoolId,
                sizeId = sizeId,
                color = color,
                sizeMeta = sizeMeta,
                tagIds = tagIds,
                imageUris = _images.value
            )

            result.fold(
                onSuccess = { item ->
                    _createdItem.value = ItemState.Success(item)
                    _uiState.value = CreateItemUiState.Success("Item created successfully!")
                    clearImages() // Clear images after successful creation
                },
                onFailure = { error ->
                    _createdItem.value = ItemState.Error(error.message ?: "Failed to create item")
                    _uiState.value = CreateItemUiState.Error(error.message ?: "Failed to create item")
                }
            )
        }
    }

    fun resetState() {
        _uiState.value = CreateItemUiState.Idle
        _createdItem.value = null
    }
}

sealed class CreateItemUiState {
    object Idle : CreateItemUiState()
    object Loading : CreateItemUiState()
    data class Success(val message: String) : CreateItemUiState()
    data class Error(val message: String) : CreateItemUiState()
}

sealed class ItemState {
    data class Success(val item: Item) : ItemState()
    data class Error(val message: String) : ItemState()
}