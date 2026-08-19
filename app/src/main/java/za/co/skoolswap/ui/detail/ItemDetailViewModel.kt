package za.co.skoolswap.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import za.co.skoolswap.common.constants.AppConstants.LogTags
import za.co.skoolswap.data.local.database.dao.BrandDao
import za.co.skoolswap.data.local.database.dao.ColorDao
import za.co.skoolswap.data.local.database.dao.SchoolDao
import za.co.skoolswap.data.local.database.dao.SizeDao
import za.co.skoolswap.domain.model.Item
import za.co.skoolswap.domain.repository.AuthRepositoryInterface
import za.co.skoolswap.domain.repository.FavoriteRepositoryInterface
import za.co.skoolswap.domain.repository.ItemRepositoryInterface
import za.co.skoolswap.domain.repository.ProductsCacheRepositoryInterface
import za.co.skoolswap.domain.repository.ProductsRepositoryInterface
import za.co.skoolswap.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import za.co.skoolswap.data.repository.SchoolRepository
import javax.inject.Inject

// Private constants
private const val TAG = "ItemDetailVM"
private const val SIMILAR_ITEMS_LIMIT = 6
private const val RECOMMENDED_PAGE = 1
private const val RECOMMENDED_PER_PAGE = 10
private const val TRENDING_PERIOD = "today"
private const val RECENT_PERIOD = "week"

@HiltViewModel
class ItemDetailViewModel @Inject constructor(
    private val itemRepository: ItemRepositoryInterface,
    private val productsRepository: ProductsRepositoryInterface,
    private val sizeDao: SizeDao,
    private val favoriteRepository: FavoriteRepositoryInterface,
    private val authRepository: AuthRepositoryInterface,
    private val schoolDao: SchoolDao,
    private val schoolRepository: SchoolRepository,
    private val colorDao: ColorDao,
    private val brandDao: BrandDao,
    private val productsCacheRepository: ProductsCacheRepositoryInterface
) : ViewModel() {

    private val _itemState = MutableStateFlow<ItemDetailState>(ItemDetailState.Loading)
    val itemState: StateFlow<ItemDetailState> = _itemState.asStateFlow()

    private val _similarItems = MutableStateFlow<List<Item>>(emptyList())
    val similarItems: StateFlow<List<Item>> = _similarItems.asStateFlow()

    private val _similarSectionTitle = MutableStateFlow("Similar Items")
    val similarSectionTitle: StateFlow<String> = _similarSectionTitle.asStateFlow()

    private val _isLoadingSimilar = MutableStateFlow(false)
    val isLoadingSimilar: StateFlow<Boolean> = _isLoadingSimilar.asStateFlow()

    private val _sizeName = MutableStateFlow<String?>(null)
    val sizeName: StateFlow<String?> = _sizeName.asStateFlow()

    private val _schoolName = MutableStateFlow<String?>(null)
    val schoolName: StateFlow<String?> = _schoolName.asStateFlow()

    private val _colorName = MutableStateFlow<String?>(null)
    val colorName: StateFlow<String?> = _colorName.asStateFlow()

    private val _brandName = MutableStateFlow<String?>(null)
    val brandName: StateFlow<String?> = _brandName.asStateFlow()

    private val _conditionName = MutableStateFlow<String?>(null)
    val conditionName: StateFlow<String?> = _conditionName.asStateFlow()

    private val _imageUrls = MutableStateFlow<List<String>>(emptyList())
    val imageUrls: StateFlow<List<String>> = _imageUrls.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _sellerMobile = MutableStateFlow<String?>(null)
    val sellerMobile: StateFlow<String?> = _sellerMobile.asStateFlow()

    private val _similarItemsShimmer = MutableStateFlow(true)
    val similarItemsShimmer: StateFlow<Boolean> = _similarItemsShimmer.asStateFlow()

    private var cachedItem: Item? = null
    private var currentItemId: String? = null
    private var currentUserId: Int? = null

    private val similarItemsCache = mutableMapOf<String, List<Item>>()
    private val _schoolLogoUrl = MutableStateFlow<String?>(null)
    val schoolLogoUrl: StateFlow<String?> = _schoolLogoUrl.asStateFlow()
    fun loadItem(itemId: String, source: String) {
        viewModelScope.launch {
            currentItemId = itemId
            currentUserId = authRepository.getCurrentUserId()

            _itemState.value = ItemDetailState.Loading
            checkFavoriteStatus(itemId)

            val similarJob = launch { loadSimilarItemsEarly(itemId) }
            val itemJob = launch { fetchItem(itemId) }

            launch { productsRepository.trackClick(itemId, source, 0) }
        }
    }

    private suspend fun checkFavoriteStatus(itemId: String) {
        val userId = currentUserId ?: authRepository.getCurrentUserId()
        if (userId != null) {
            _isFavorite.value = favoriteRepository.isFavorite(userId, itemId)
            Timber.tag(LogTags.VIEW_MODEL).d("Favorite status for $itemId (user $userId): ${_isFavorite.value}")
        } else {
            Timber.tag(LogTags.VIEW_MODEL).w("Cannot check favorite - no user logged in")
            _isFavorite.value = false
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            currentItemId?.let { itemId ->
                val userId = currentUserId ?: authRepository.getCurrentUserId()
                if (userId != null) {
                    val newStatus = favoriteRepository.toggleFavorite(userId, itemId)
                    _isFavorite.value = newStatus
                    Timber.tag(LogTags.VIEW_MODEL).d("Toggled favorite for $itemId (user $userId): $newStatus")
                } else {
                    Timber.tag(LogTags.VIEW_MODEL).w("Cannot toggle favorite - no user logged in")
                }
            }
        }
    }



    private suspend fun loadSimilarItemsEarly(itemId: String) {
        // ================================================================
        // STEP 1: Show shimmer while checking cache
        // ================================================================
        _similarItemsShimmer.value = true  // ✅ Show shimmer immediately

        // ================================================================
        // STEP 2: Check memory cache
        // ================================================================
        similarItemsCache[itemId]?.let { cached ->
            Timber.tag(LogTags.VIEW_MODEL).d("✅ Similar items from MEMORY cache: ${cached.size} items")
            _similarItemsShimmer.value = false  // ✅ Hide shimmer
            _similarItems.value = cached
            return
        }

        // ================================================================
        // STEP 3: Check Room database cache
        // ================================================================
        val cachedSimilar = productsCacheRepository.getCachedSimilarItems(itemId)
        if (cachedSimilar != null && cachedSimilar.isNotEmpty()) {
            Timber.tag(LogTags.VIEW_MODEL).d("✅ Similar items from ROOM cache: ${cachedSimilar.size} items")
            _similarItemsShimmer.value = false  // ✅ Hide shimmer
            _similarItems.value = cachedSimilar
            similarItemsCache[itemId] = cachedSimilar
            return
        }

        // ================================================================
        // STEP 4: Check if we have the main item's category (but still keep shimmer)
        // ================================================================
        val currentItem = (itemState.value as? ItemDetailState.Success)?.item
        if (currentItem?.mainCategoryId != null) {
            Timber.tag(LogTags.VIEW_MODEL).d("📂 Using main item category: ${currentItem.mainCategoryId}")

            // Try category-based items (but keep shimmer going)
            val categoryItems = fetchItemsByCategory(currentItem.mainCategoryId!!, itemId)
            if (categoryItems.isNotEmpty()) {
                Timber.tag(LogTags.VIEW_MODEL).d("✅ Category-based items found: ${categoryItems.size}")
                _similarSectionTitle.value = "Similar Items"
                val result = categoryItems.take(SIMILAR_ITEMS_LIMIT)
                similarItemsCache[itemId] = result
                productsCacheRepository.cacheSimilarItems(itemId, result)
                _similarItemsShimmer.value = false  // ✅ Hide shimmer
                _similarItems.value = result
                return
            }
        }

        // ================================================================
        // STEP 5: NO CACHE - Keep shimmer and fetch from network
        // ================================================================
        Timber.tag(LogTags.VIEW_MODEL).d("🔄 No cache found, fetching from network for: $itemId")

        _isLoadingSimilar.value = true
        // _similarItemsShimmer.value is already true ✅

        try {
            // Try trending first
            var items = fetchTrendingItems(excludeItemId = itemId, period = TRENDING_PERIOD)

            // Fallback to recent
            if (items.isEmpty()) {
                Timber.tag(LogTags.VIEW_MODEL).d("No trending items, trying recent...")
                items = fetchRecentItems(excludeItemId = itemId, period = RECENT_PERIOD)
            }

            // Final fallback to any popular items
            if (items.isEmpty()) {
                Timber.tag(LogTags.VIEW_MODEL).d("No recent items, trying any popular...")
                items = fetchAnyPopularItems(excludeItemId = itemId)
            }

            Timber.tag(LogTags.VIEW_MODEL).d("📦 Found ${items.size} similar items from network")

            // Always turn off shimmer after network request
            _similarItemsShimmer.value = false
            _isLoadingSimilar.value = false

            if (items.isNotEmpty()) {
                _similarSectionTitle.value = "Similar Items"
                val result = items.take(SIMILAR_ITEMS_LIMIT)
                similarItemsCache[itemId] = result
                productsCacheRepository.cacheSimilarItems(itemId, result)
                _similarItems.value = result
            } else {
                _similarItems.value = emptyList()
            }

        } catch (e: Exception) {
            Timber.tag(LogTags.VIEW_MODEL).e("Failed to fetch similar items: ${e.message}")
            _similarItemsShimmer.value = false  // ✅ Always hide shimmer on error
            _isLoadingSimilar.value = false
            _similarItems.value = emptyList()
        }
    }

    private suspend fun fetchItem(itemId: String) {
        val cachedItem = productsCacheRepository.getCachedItemDetail(itemId)
        if (cachedItem != null) {
            Timber.tag(LogTags.VIEW_MODEL).d("📦 Loading item from cache: ${cachedItem.name}")
            _itemState.value = ItemDetailState.Success(cachedItem)
            _sellerMobile.value = cachedItem.shop?.sellerMobile
            loadReferenceData(cachedItem)
        }

        val result = itemRepository.getItem(itemId)

        if (result.isSuccess) {
            val item = result.getOrNull() ?: return

            _itemState.value = ItemDetailState.Success(item)
            _sellerMobile.value = item.shop?.sellerMobile
            Timber.tag(LogTags.VIEW_MODEL).d("Seller mobile: ${_sellerMobile.value}")

            loadReferenceData(item)
            productsCacheRepository.cacheItemDetail(itemId, item)

            if (item.mainCategoryId != null) {
                loadSimilarItemsWithCategory(item)
            }
        } else {
            if (_itemState.value !is ItemDetailState.Success) {
                _itemState.value = ItemDetailState.Error(
                    result.exceptionOrNull()?.message ?: "Unknown error"
                )
            }
        }
    }

    private suspend fun loadSimilarItemsWithCategory(item: Item) {
        val cacheKey = item.id
        val categoryItems = fetchItemsByCategory(item.mainCategoryId!!, item.id)

        if (categoryItems.isNotEmpty()) {
            Timber.tag(LogTags.VIEW_MODEL).d("Upgraded similar items to category-matched: ${categoryItems.size}")
            _similarSectionTitle.value = "Similar Items"
            val result = categoryItems.take(SIMILAR_ITEMS_LIMIT)
            similarItemsCache[cacheKey] = result
            _similarItems.value = result
        }
    }

    private suspend fun loadReferenceData(item: Item) {
        // Set images and other basic info
        _imageUrls.value = item.images.map { it.url }
        _sizeName.value = item.sizeName
        _colorName.value = item.colorName
        _brandName.value = item.brandName
        _conditionName.value = item.conditionName

        // ================================================================
        // ✅ GET SCHOOL INFO - Use schoolId from item
        // ================================================================

        var schoolName = item.schoolName
        var schoolLogo = item.schoolLogoUrl

        // If item doesn't have school info, try SchoolDao
        if (schoolName.isNullOrBlank() || schoolLogo.isNullOrBlank()) {
            item.schoolId?.let { schoolId ->
                val school = schoolDao.getById(schoolId)
                if (school != null) {
                    schoolName = school.name
                    schoolLogo = school.logoUrl
                    Timber.tag(LogTags.VIEW_MODEL).d("🏫 School from Database → name: $schoolName | logo: $schoolLogo")
                }
            }
        }

        _schoolName.value = schoolName
        _schoolLogoUrl.value = schoolLogo
    }

    private suspend fun fetchItemsByCategory(categoryId: Int, excludeItemId: String): List<Item> {
        return try {
            val result = productsRepository.getRecommendedAll(
                page = RECOMMENDED_PAGE,
                perPage = RECOMMENDED_PER_PAGE,
                categoryId = categoryId,
                conditionId = null,
                minPrice = null,
                maxPrice = null
            )
            when (result) {
                is Result.Success -> result.data.items.filter { it.id != excludeItemId }
                is Result.Error -> emptyList()
            }
        } catch (e: Exception) { emptyList() }
    }

    private suspend fun fetchTrendingItems(excludeItemId: String, period: String = TRENDING_PERIOD): List<Item> {
        return try {
            val result = productsRepository.getTrendingAll(
                period = period,
                page = RECOMMENDED_PAGE,
                perPage = RECOMMENDED_PER_PAGE,
                categoryId = null,
                conditionId = null,
                minPrice = null,
                maxPrice = null
            )
            when (result) {
                is Result.Success -> result.data.items.filter { it.id != excludeItemId }
                is Result.Error -> emptyList()
            }
        } catch (e: Exception) { emptyList() }
    }

    private suspend fun fetchRecentItems(excludeItemId: String, period: String = RECENT_PERIOD): List<Item> {
        return try {
            val result = productsRepository.getRecentAll(
                period = period,
                page = RECOMMENDED_PAGE,
                perPage = RECOMMENDED_PER_PAGE,
                categoryId = null,
                conditionId = null,
                minPrice = null,
                maxPrice = null
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
                page = RECOMMENDED_PAGE,
                perPage = RECOMMENDED_PER_PAGE,
                categoryId = null,
                conditionId = null,
                minPrice = null,
                maxPrice = null
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
        Timber.tag(LogTags.VIEW_MODEL).d("getImageUrls returning ${urls.size} URLs")
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
        currentUserId = null
        Timber.tag(LogTags.VIEW_MODEL).d("State cleared")
    }


}