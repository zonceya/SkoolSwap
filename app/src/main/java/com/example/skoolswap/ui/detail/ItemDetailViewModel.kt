package com.example.skoolswap.ui.detail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skoolswap.data.local.database.dao.BrandDao
import com.example.skoolswap.data.local.database.dao.ColorDao
import com.example.skoolswap.data.local.database.dao.SchoolDao
import com.example.skoolswap.data.local.database.dao.SizeDao
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
import com.example.skoolswap.domain.repository.FavoriteRepositoryInterface
import com.example.skoolswap.domain.repository.ItemRepositoryInterface
import com.example.skoolswap.domain.repository.ProductsRepositoryInterface
import com.example.skoolswap.utils.Result
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
    private val authRepository: AuthRepositoryInterface,
    private val schoolDao: SchoolDao,
    private val colorDao: ColorDao,
    private val brandDao: BrandDao
) : ViewModel() {

    private val _itemState = MutableStateFlow<ItemDetailState>(ItemDetailState.Loading)
    val itemState: StateFlow<ItemDetailState> = _itemState.asStateFlow()

    private val _similarItems = MutableStateFlow<List<Item>>(emptyList())
    val similarItems: StateFlow<List<Item>> = _similarItems.asStateFlow()

    // Add these for fallback UI
    private val _similarSectionTitle = MutableStateFlow("Similar Items")
    val similarSectionTitle: StateFlow<String> = _similarSectionTitle.asStateFlow()

    private val _isLoadingSimilar = MutableStateFlow(false)
    val isLoadingSimilar: StateFlow<Boolean> = _isLoadingSimilar.asStateFlow()

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
    private val _imageUrls = MutableStateFlow<List<String>>(emptyList())
    val imageUrls: StateFlow<List<String>> = _imageUrls.asStateFlow()
    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _sellerMobile = MutableStateFlow<String?>(null)
    val sellerMobile: StateFlow<String?> = _sellerMobile.asStateFlow()

    private val TAG = "ItemDetailVM"

    fun loadItem(itemId: String, source: String) {
        viewModelScope.launch {
            currentItemId = itemId
            _itemState.value = ItemDetailState.Loading
            checkFavoriteStatus(itemId)
            trackView(itemId, source)
            fetchItem(itemId)
        }
    }

    // This MUST be suspend because favoriteRepository.isFavorite() is suspend
    private suspend fun checkFavoriteStatus(itemId: String) {
        _isFavorite.value = favoriteRepository.isFavorite(itemId)
        Timber.tag(TAG).d("Favorite status for $itemId: ${_isFavorite.value}")
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            currentItemId?.let { itemId ->
                val newStatus = favoriteRepository.toggleFavorite(itemId)
                _isFavorite.value = newStatus
                Log.d(TAG, "Toggled favorite for $itemId: $newStatus")
            }
        }
    }

    // This MUST be suspend because authRepository.getUserById() is suspend
    private suspend fun loadSellerContact(sellerUserId: Long) {
        val result = authRepository.getUserById(sellerUserId)
        if (result.isSuccess) {
            val seller = result.getOrNull()
            _sellerMobile.value = seller?.mobile
            Log.d(TAG, "Loaded seller mobile: ${_sellerMobile.value}")
        } else {
            Log.e(TAG, "Failed to load seller contact: ${result.exceptionOrNull()?.message}")
        }
    }

    // This MUST be suspend because itemRepository.getItem() is suspend
    private suspend fun fetchItem(itemId: String) {
        val result = itemRepository.getItem(itemId)

        if (result.isSuccess) {
            val item = result.getOrNull()
            if (item != null) {
                cachedItem = item
                _itemState.value = ItemDetailState.Success(item)
                loadReferenceData(item)
                loadSimilarItems(item) // This calls suspend function

                _sellerMobile.value = item.shop?.sellerMobile
                Log.d(TAG, "Seller mobile: ${_sellerMobile.value}")
            }
        } else {
            _itemState.value = ItemDetailState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
        }
    }

    // This MUST be suspend because database operations are suspend
    private suspend fun loadReferenceData(item: Item) {
        Timber.tag("ItemDetailVM").d("Images count: ${item.images.size}")
        item.images.forEachIndexed { index, image ->
            Timber.tag("ItemDetailVM").d("Image $index: ${image.url}")
        }

        _imageUrls.value = item.images.map { it.url }

        _sizeName.value = item.sizeName
        _colorName.value = item.colorName
        _brandName.value = item.brandName
        _conditionName.value = item.conditionName

        // Load school name from Room (suspend)
        item.schoolId?.let { schoolId ->
            val school = schoolDao.getById(schoolId)
            _schoolName.value = school?.name
        }
    }

    // UPDATED: This now has multiple fallback strategies
    private suspend fun loadSimilarItems(currentItem: Item) {
        Log.d(TAG, "========== LOAD SIMILAR ITEMS START ==========")
        _isLoadingSimilar.value = true

        var items = emptyList<Item>()
        var fallbackLevel = 0

        // Try 1: Same category — always exclude current item
        if (currentItem.mainCategoryId != null) {
            items = fetchItemsByCategory(currentItem.mainCategoryId, currentItem.id)
            if (items.isNotEmpty()) {
                _similarSectionTitle.value = "Similar Items"
                fallbackLevel = 1
            }
        }

        // Try 2: Trending — exclude current item
        if (items.isEmpty()) {
            items = fetchTrendingItems(excludeItemId = currentItem.id, period = "today")
            if (items.isNotEmpty()) {
                _similarSectionTitle.value = "Trending Today"
                fallbackLevel = 2
            }
        }

        // Try 3: Recent — exclude current item
        if (items.isEmpty()) {
            items = fetchRecentItems(excludeItemId = currentItem.id, period = "week")
            if (items.isNotEmpty()) {
                _similarSectionTitle.value = "Just Added"
                fallbackLevel = 3
            }
        }

        // Try 4: Any popular — exclude current item
        if (items.isEmpty()) {
            items = fetchAnyPopularItems(excludeItemId = currentItem.id)
            if (items.isNotEmpty()) {
                _similarSectionTitle.value = "Popular Items"
                fallbackLevel = 4
            }
        }

        Log.d(TAG, "Fallback level $fallbackLevel used, found ${items.size} items")
        // ✅ Double-filter at the end as safety net
        _similarItems.value = items
            .filter { it.id != currentItem.id }
            .take(6)
        _isLoadingSimilar.value = false
    }

    // Helper suspend functions for each fallback
    private suspend fun fetchItemsByCategory(categoryId: Int, excludeItemId: String): List<Item> {
        return try {
            val result = productsRepository.getRecommendedAll(
                page = 1, perPage = 10, categoryId = categoryId,
                conditionId = null, minPrice = null, maxPrice = null
            )
            when (result) {
                is Result.Success -> result.data.items.filter { it.id != excludeItemId }
                is Result.Error -> emptyList()
            }
        } catch (e: Exception) { emptyList() }
    }

    private suspend fun fetchTrendingItems(excludeItemId: String, period: String = "today"): List<Item> {
        return try {
            val result = productsRepository.getTrendingAll(
                period = period, page = 1, perPage = 10,
                categoryId = null, conditionId = null, minPrice = null, maxPrice = null
            )
            when (result) {
                is Result.Success -> result.data.items.filter { it.id != excludeItemId }
                is Result.Error -> emptyList()
            }
        } catch (e: Exception) { emptyList() }
    }

    private suspend fun fetchRecentItems(excludeItemId: String, period: String = "week"): List<Item> {
        return try {
            val result = productsRepository.getRecentAll(
                period = period, page = 1, perPage = 10,
                categoryId = null, conditionId = null, minPrice = null, maxPrice = null
            )
            when (result) {
                is Result.Success -> result.data.items.filter { it.id != excludeItemId }
                is Result.Error -> emptyList()
            }
        } catch (e: Exception) { emptyList() }
    }

    private suspend fun fetchAnyPopularItems(excludeItemId: String): List<Item> {
        return try {
            val result = productsRepository.getRecommendedAll(
                page = 1, perPage = 10, categoryId = null,
                conditionId = null, minPrice = null, maxPrice = null
            )
            when (result) {
                is Result.Success -> result.data.items.filter { it.id != excludeItemId }
                is Result.Error -> emptyList()
            }
        } catch (e: Exception) { emptyList() }
    }

    fun trackView(itemId: String, source: String) {
        viewModelScope.launch {
            productsRepository.trackClick(itemId, source, 0)
        }
    }

    fun getImageUrls(): List<String> {
        val urls = _imageUrls.value
        Timber.tag("ItemDetailVM").d("getImageUrls returning ${urls.size} URLs")
        return urls
    }

    fun clearState() {
        _itemState.value = ItemDetailState.Loading
        _similarItems.value = emptyList()
        _similarSectionTitle.value = "Similar Items"
        _isLoadingSimilar.value = false
        _sizeName.value = null
        _schoolName.value = null
        _colorName.value = null
        _brandName.value = null
        _conditionName.value = null
        _isFavorite.value = false
        _sellerMobile.value = null
        Timber.tag("ItemDetailVM").d("State cleared - ready for fresh load")
    }

    sealed class ItemDetailState {
        object Loading : ItemDetailState()
        data class Success(val item: Item) : ItemDetailState()
        data class Error(val message: String) : ItemDetailState()
    }
}