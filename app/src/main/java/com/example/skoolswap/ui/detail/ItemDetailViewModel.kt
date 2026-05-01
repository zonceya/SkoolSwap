package com.example.skoolswap.ui.detail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skoolswap.data.local.database.dao.BrandDao
import com.example.skoolswap.data.local.database.dao.ColorDao
import com.example.skoolswap.data.local.database.dao.SchoolDao
import com.example.skoolswap.data.local.database.dao.SizeDao
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.repository.FavoriteRepositoryInterface
import com.example.skoolswap.domain.repository.ItemRepositoryInterface
import com.example.skoolswap.domain.repository.ProductsRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ItemDetailViewModel @Inject constructor(
    private val itemRepository: ItemRepositoryInterface,
    private val productsRepository: ProductsRepositoryInterface,
    private val sizeDao: SizeDao,
    private val favoriteRepository: FavoriteRepositoryInterface,
    private val schoolDao: SchoolDao,
    private val colorDao: ColorDao,
    private val brandDao: BrandDao

) : ViewModel() {

    private val _itemState = MutableStateFlow<ItemDetailState>(ItemDetailState.Loading)
    val itemState: StateFlow<ItemDetailState> = _itemState.asStateFlow()

    private val _similarItems = MutableStateFlow<List<Item>>(emptyList())
    val similarItems: StateFlow<List<Item>> = _similarItems.asStateFlow()

    private val _sizeName = MutableStateFlow<String?>(null)
    val sizeName: StateFlow<String?> = _sizeName.asStateFlow()
    private var cachedItem: Item? = null
    private val _schoolName = MutableStateFlow<String?>(null)
    val schoolName: StateFlow<String?> = _schoolName.asStateFlow()
    private val _colorName = MutableStateFlow<String?>(null)
    val colorName: StateFlow<String?> = _colorName.asStateFlow()
    private val _brandName = MutableStateFlow<String?>(null)
    val brandName: StateFlow<String?> = _brandName.asStateFlow()
    private val _conditionName = MutableStateFlow<String?>(null)
    val conditionName: StateFlow<String?> = _conditionName.asStateFlow()
    private var currentItemId: String? = null
    // Add this with your other private val declarations (around line 30)
    private val _imageUrls = MutableStateFlow<List<String>>(emptyList())
    val imageUrls: StateFlow<List<String>> = _imageUrls.asStateFlow()
    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()


    fun loadItem(itemId: String, source: String) {
        viewModelScope.launch {
            currentItemId = itemId

            // Always show loading when reopening (prevents stale empty data)
            _itemState.value = ItemDetailState.Loading
            checkFavoriteStatus(itemId)
            trackView(itemId, source)
            fetchItem(itemId)

        }
    }
    private suspend fun checkFavoriteStatus(itemId: String) {
        _isFavorite.value = favoriteRepository.isFavorite(itemId)
        Log.d("ItemDetailVM", "Favorite status for $itemId: ${_isFavorite.value}")
    }
    fun toggleFavorite() {
        viewModelScope.launch {
            currentItemId?.let { itemId ->
                val newStatus = favoriteRepository.toggleFavorite(itemId)
                _isFavorite.value = newStatus
                Log.d("ItemDetailVM", "Toggled favorite for $itemId: $newStatus")

                // Show feedback
                val message = if (newStatus) "Added to favorites" else "Removed from favorites"
                // You can show a snackbar or toast here
            }
        }
    }
    private suspend fun fetchItem(itemId: String) {
        val result = itemRepository.getItem(itemId)

        if (result.isSuccess) {
            val item = result.getOrNull()
            if (item != null) {
                cachedItem = item
                _itemState.value = ItemDetailState.Success(item)
                loadReferenceData(item)
                loadSimilarItems(item)
            } else {
                _itemState.value = ItemDetailState.Error("Item data is null")
            }
        } else {
            _itemState.value = ItemDetailState.Error(
                "Unable to load item. Please check your internet connection."
            )
        }
    }

    private suspend fun loadReferenceData(item: Item) {
        Timber.tag("ItemDetailVM").d("Images count: ${item.images.size}")
        item.images.forEachIndexed { index, image ->
            Timber.tag("ItemDetailVM").d("Image $index: ${image.url}")
        }
        Timber.tag("ItemDetailVM").d("=== IMAGES DEBUG ===")
        Timber.tag("ItemDetailVM").d("Images count: ${item.images.size}")
        item.images.forEachIndexed { index, image ->
            Timber.tag("ItemDetailVM").d("Image $index: ${image.url}")
        }
        Timber.tag("ItemDetailVM").d("==================")

        // ADD THIS LINE - Extract image URLs
        _imageUrls.value = item.images.map { it.url }

        _sizeName.value = item.sizeName
        _colorName.value = item.colorName
        _brandName.value = item.brandName
        _conditionName.value = item.conditionName

        // Load school name from Room
        item.schoolId?.let { schoolId ->
            val school = schoolDao.getById(schoolId)
            _schoolName.value = school?.name
        }
    }

    private suspend fun loadSimilarItems(currentItem: Item) {
        val result = productsRepository.getRecommendedAll(
            page = 1,
            perPage = 10,
            categoryId = currentItem.mainCategoryId,
            conditionId = null,
            minPrice = null,
            maxPrice = null
        )

        if (result.isSuccess) {
            val paginatedResponse = result.getOrNull()
            val similar = paginatedResponse?.items
                ?.filter { it.id != currentItem.id }
                ?.take(6) ?: emptyList()
            _similarItems.value = similar
        } else {
            _similarItems.value = emptyList()
        }
    }

    private fun trackView(itemId: String, source: String) {
        viewModelScope.launch {
            productsRepository.trackClick(itemId, source, 0)
        }
    }
    fun getImageUrls(): List<String> {
        val urls = _imageUrls.value
        Timber.tag("ItemDetailVM").d("getImageUrls returning ${urls.size} URLs")
        return urls
    }
    // Add this function to your ItemDetailViewModel
    fun clearState() {
        _itemState.value = ItemDetailState.Loading
        _similarItems.value = emptyList()
        _sizeName.value = null
        _schoolName.value = null
        _colorName.value = null
        _brandName.value = null
        _conditionName.value = null
        Timber.tag("ItemDetailVM").d("State cleared - ready for fresh load")
    }
    sealed class ItemDetailState {
        object Loading : ItemDetailState()
        data class Success(val item: Item) : ItemDetailState()
        data class Error(val message: String) : ItemDetailState()
    }
    private fun refreshItemInBackground(itemId: String) {
        viewModelScope.launch {
            try {
                val result = itemRepository.getItem(itemId)
                if (result.isSuccess) {
                    val freshItem = result.getOrNull() ?: return@launch

                    // Only update UI if something actually changed
                    if (freshItem.updatedAt != cachedItem?.updatedAt) {
                        Timber.tag("ItemDetailVM").d("Item changed, updating UI")
                        cachedItem = freshItem
                        _itemState.value = ItemDetailState.Success(freshItem)
                        loadReferenceData(freshItem)
                        loadSimilarItems(freshItem)
                    } else {
                        Timber.tag("ItemDetailVM").d("Item unchanged, keeping cache")
                    }
                }
            } catch (e: Exception) {
                Timber.tag("ItemDetailVM").e("Background refresh failed: ${e.message}")
            }
        }

    }
}