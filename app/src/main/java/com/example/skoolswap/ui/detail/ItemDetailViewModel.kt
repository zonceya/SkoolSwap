package com.example.skoolswap.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skoolswap.data.local.database.dao.BrandDao
import com.example.skoolswap.data.local.database.dao.ColorDao
import com.example.skoolswap.data.local.database.dao.SchoolDao
import com.example.skoolswap.data.local.database.dao.SizeDao
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.repository.ItemRepositoryInterface
import com.example.skoolswap.domain.repository.ProductsRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ItemDetailViewModel @Inject constructor(
    private val itemRepository: ItemRepositoryInterface,
    private val productsRepository: ProductsRepositoryInterface,
    private val sizeDao: SizeDao,
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

    private val _schoolName = MutableStateFlow<String?>(null)
    val schoolName: StateFlow<String?> = _schoolName.asStateFlow()
    private val _colorName = MutableStateFlow<String?>(null)
    val colorName: StateFlow<String?> = _colorName.asStateFlow()
    private val _brandName = MutableStateFlow<String?>(null)
    val brandName: StateFlow<String?> = _brandName.asStateFlow()
    fun loadItem(itemId: String, source: String) {
        viewModelScope.launch {
            _itemState.value = ItemDetailState.Loading
            trackView(itemId, source)

            val result = itemRepository.getItem(itemId)

            // ADD THESE LOGS
            android.util.Log.d("ItemDetailVM", "Result isSuccess: ${result.isSuccess}")

            if (result.isSuccess) {
                val item = result.getOrNull()
                android.util.Log.d("ItemDetailVM", "Item name: ${item?.name}")
                android.util.Log.d("ItemDetailVM", "Item price: ${item?.price}")
                android.util.Log.d("ItemDetailVM", "Item brandId: ${item?.brandId}")
                android.util.Log.d("ItemDetailVM", "Item colorId: ${item?.colorId}")
                android.util.Log.d("ItemDetailVM", "Item schoolId: ${item?.schoolId}")

                if (item != null) {
                    _itemState.value = ItemDetailState.Success(item)
                    loadReferenceData(item)
                    loadSimilarItems(item)
                } else {
                    _itemState.value = ItemDetailState.Error("Item data is null")
                }
            } else {
                val exception = result.exceptionOrNull()
                android.util.Log.e("ItemDetailVM", "Error: ${exception?.message}")
                _itemState.value = ItemDetailState.Error(exception?.message ?: "Failed to load item")
            }
        }
    }

    private suspend fun loadReferenceData(item: Item) {
        // Load size name from Room
        item.sizeId?.let { sizeId ->
            val size = sizeDao.getById(sizeId)
            _sizeName.value = size?.name
        }
        item.brandId?.let { brandId ->
            val brand = brandDao.getById(brandId)
            _brandName.value = brand?.name
        }
        // Load school name from Room
        item.schoolId?.let { schoolId ->
            val school = schoolDao.getById(schoolId)
            _schoolName.value = school?.name
        }
        item.colorId?.let { colorId ->
            val color = colorDao.getById(colorId)  // You'll need to inject ColorDao
            _colorName.value = color?.name
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

    sealed class ItemDetailState {
        object Loading : ItemDetailState()
        data class Success(val item: Item) : ItemDetailState()
        data class Error(val message: String) : ItemDetailState()
    }
}