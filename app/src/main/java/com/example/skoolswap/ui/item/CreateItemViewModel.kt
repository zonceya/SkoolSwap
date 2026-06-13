package com.example.skoolswap.ui.item

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.model.reference.*
import com.example.skoolswap.domain.repository.ItemRepositoryInterface
import com.example.skoolswap.domain.repository.ReferenceDataRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateItemViewModel @Inject constructor(
    private val itemRepository: ItemRepositoryInterface,
    private val referenceRepository: ReferenceDataRepositoryInterface
) : ViewModel() {

    companion object {
        private const val TAG = "CreateItemViewModel"
    }

    // ============ UI STATES ============
    private val _uiState = MutableStateFlow<CreateItemUiState>(CreateItemUiState.Idle)
    val uiState: StateFlow<CreateItemUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _createdItem = MutableStateFlow<ItemState?>(null)
    val createdItem: StateFlow<ItemState?> = _createdItem.asStateFlow()

    private val _images = MutableStateFlow<List<Uri>>(emptyList())
    val images: StateFlow<List<Uri>> = _images.asStateFlow()

    // ============ REFERENCE DATA STATES ============
    private val _mainCategories = MutableStateFlow<List<MainCategory>>(emptyList())
    val mainCategories: StateFlow<List<MainCategory>> = _mainCategories.asStateFlow()

    private val _subCategories = MutableStateFlow<List<SubCategory>>(emptyList())
    val subCategories: StateFlow<List<SubCategory>> = _subCategories.asStateFlow()

    private val _colors = MutableStateFlow<List<Color>>(emptyList())
    val colors: StateFlow<List<Color>> = _colors.asStateFlow()

    private val _sizes = MutableStateFlow<List<Size>>(emptyList())
    val sizes: StateFlow<List<Size>> = _sizes.asStateFlow()

    private val _brands = MutableStateFlow<List<Brand>>(emptyList())
    val brands: StateFlow<List<Brand>> = _brands.asStateFlow()

    private val _conditions = MutableStateFlow<List<Condition>>(emptyList())
    val conditions: StateFlow<List<Condition>> = _conditions.asStateFlow()

    private val _provinces = MutableStateFlow<List<Province>>(emptyList())
    val provinces: StateFlow<List<Province>> = _provinces.asStateFlow()

    private val _towns = MutableStateFlow<List<Town>>(emptyList())
    val towns: StateFlow<List<Town>> = _towns.asStateFlow()

    private val _schools = MutableStateFlow<List<School>>(emptyList())
    val schools: StateFlow<List<School>> = _schools.asStateFlow()

    private val _genders = MutableStateFlow<List<Gender>>(emptyList())
    val genders: StateFlow<List<Gender>> = _genders.asStateFlow()

    private val _tags = MutableStateFlow<List<Tag>>(emptyList())
    val tags: StateFlow<List<Tag>> = _tags.asStateFlow()

    private val _locations = MutableStateFlow<List<Location>>(emptyList())
    val locations: StateFlow<List<Location>> = _locations.asStateFlow()

    // ============ SELECTED VALUES ============
    private val _selectedMainCategoryId = MutableStateFlow<Int?>(null)
    val selectedMainCategoryId: StateFlow<Int?> = _selectedMainCategoryId.asStateFlow()

    private val _selectedProvinceId = MutableStateFlow<Int?>(null)
    val selectedProvinceId: StateFlow<Int?> = _selectedProvinceId.asStateFlow()

    init {
        Log.d(TAG, "=== ViewModel INIT ===")
        viewModelScope.launch {
            _isLoading.value = true
            Log.d(TAG, "Loading started")

            // STEP 1: Start collecting from DB FIRST (so we catch any updates)
            Log.d(TAG, "Setting up database collectors")
            launch {
                referenceRepository.getMainCategories().collect {
                    _mainCategories.value = it
                    Log.d(TAG, "📦 Loaded ${it.size} main categories from DB")
                    it.forEach { category ->
                        Log.d(TAG, "  - Category: ${category.name} (ID: ${category.id})")
                    }
                }
            }
            launch {
                referenceRepository.getColors().collect {
                    _colors.value = it
                    Log.d(TAG, "🎨 Loaded ${it.size} colors from DB")
                }
            }
            launch {
                referenceRepository.getSizes().collect {
                    _sizes.value = it
                    Log.d(TAG, "📏 Loaded ${it.size} sizes from DB")
                }
            }
            launch {
                referenceRepository.getBrands().collect {
                    _brands.value = it
                    Log.d(TAG, "🏷️ Loaded ${it.size} brands from DB")
                }
            }
            launch {
                referenceRepository.getConditions().collect {
                    _conditions.value = it
                    Log.d(TAG, "✅ Loaded ${it.size} conditions from DB")
                }
            }
            launch {
                referenceRepository.getProvinces().collect {
                    _provinces.value = it
                    Log.d(TAG, "🗺️ Loaded ${it.size} provinces from DB")
                }
            }
            launch {
                referenceRepository.getSchools().collect {
                    _schools.value = it
                    Log.d(TAG, "🏫 Loaded ${it.size} schools from DB")
                }
            }
            launch {
                referenceRepository.getGenders().collect {
                    _genders.value = it
                    Log.d(TAG, "👥 Loaded ${it.size} genders from DB")
                }
            }
            launch {
                referenceRepository.getTags().collect {
                    _tags.value = it
                    Log.d(TAG, "🏷️ Loaded ${it.size} tags from DB")
                }
            }
            launch {
                referenceRepository.getLocations().collect {
                    _locations.value = it
                    Log.d(TAG, "📍 Loaded ${it.size} locations from DB")
                }
            }

            // STEP 2: Set up observers
            Log.d(TAG, "Setting up observers for subcategories and towns")
            observeSubCategories()
            observeTowns()

            // STEP 3: Now trigger the refresh (ONCE) - DB flows will update automatically
            Log.d(TAG, "Triggering reference data refresh")
            try {
                val result = referenceRepository.refreshAllReferenceDataBulk(forceRefresh = false)
                if (result.isSuccess) {
                    Log.d(TAG, "✅ Reference data refreshed successfully")
                } else {
                    Log.w(TAG, "⚠️ Refresh failed, using cached: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "⚠️ Refresh error: ${e.message}", e)
            }

            _isLoading.value = false
            Log.d(TAG, "Loading completed, isLoading=false")
            Log.d(TAG, "=== ViewModel INIT COMPLETE ===")
        }
    }

    // ============ OBSERVE DEPENDENT DATA ============
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeSubCategories() {
        viewModelScope.launch {
            Log.d(TAG, "observeSubCategories: Starting observation")
            _selectedMainCategoryId
                .filterNotNull()
                .flatMapLatest { mainCategoryId ->
                    Log.d(TAG, "observeSubCategories: Main category ID changed to $mainCategoryId")
                    referenceRepository.getSubCategories(mainCategoryId)
                }
                .collect { subCats ->
                    Log.d(TAG, "📦 Received ${subCats.size} subcategories")
                    subCats.forEach { subCat ->
                        Log.d(TAG, "  - Subcategory: ${subCat.name} (ID: ${subCat.id})")
                    }
                    _subCategories.value = subCats
                }
        }
    }

    private fun observeTowns() {
        viewModelScope.launch {
            Log.d(TAG, "observeTowns: Starting observation")
            _selectedProvinceId
                .filterNotNull()
                .collect { provinceId ->
                    Log.d(TAG, "🔍 Province selected: $provinceId, refreshing towns")
                    referenceRepository.refreshTowns(provinceId)
                    referenceRepository.getTowns(provinceId).collect { townList ->
                        _towns.value = townList
                        Log.d(TAG, "Loaded ${townList.size} towns for province $provinceId")
                        townList.forEach { town ->
                            Log.d(TAG, "  - Town: ${town.name} (ID: ${town.id})")
                        }
                    }
                }
        }
    }

    // ============ SELECTION METHODS ============
    fun onMainCategorySelected(mainCategoryId: Int?) {
        Log.d(TAG, "onMainCategorySelected: $mainCategoryId")
        _selectedMainCategoryId.value = mainCategoryId
    }

    fun onProvinceSelected(provinceId: Int?) {
        Log.d(TAG, "onProvinceSelected: $provinceId")
        _selectedProvinceId.value = provinceId
    }

    // ============ IMAGE METHODS ============
    fun addImage(uri: Uri) {
        Log.d(TAG, "addImage: $uri")
        val currentImages = _images.value.toMutableList()
        Log.d(TAG, "Current images count: ${currentImages.size}")

        if (currentImages.size < 3) {
            currentImages.add(uri)
            _images.value = currentImages
            Log.d(TAG, "✅ Image added. New count: ${currentImages.size}")
        } else {
            Log.w(TAG, "Cannot add image, max limit reached (3/3)")
        }
    }

    fun removeImage(uri: Uri) {
        Log.d(TAG, "removeImage: $uri")
        val currentImages = _images.value.toMutableList()
        val removed = currentImages.remove(uri)
        _images.value = currentImages
        Log.d(TAG, "Image removed: $removed. Remaining count: ${currentImages.size}")
    }

    fun clearImages() {
        Log.d(TAG, "clearImages: Clearing all images")
        _images.value = emptyList()
    }

    fun removeImageAt(index: Int) {
        Log.d(TAG, "removeImageAt: index=$index")
        val currentImages = _images.value.toMutableList()
        if (index < currentImages.size) {
            val removed = currentImages.removeAt(index)
            _images.value = currentImages
            Log.d(TAG, "✅ Removed image at index $index: $removed. Remaining: ${currentImages.size}")
        } else {
            Log.w(TAG, "Invalid index $index, current size: ${currentImages.size}")
        }
    }

    // ============ CREATE ITEM ============
    fun createItem(
        context: Context,
        name: String,
        description: String,
        price: Double,
        quantity: Int,
        mainCategoryId: Int,
        subCategoryId: Int,
        brandId: Int?,
        sizeId: Int?,
        schoolId: Int?,
        conditionId: Int?,
        locationId: Int?,
        provinceId: Int?,
        genderId: Int?,
        colorId: Int?,
        tagIds: List<Int>? = null
    ) {
        Log.d(TAG, "=== createItem START ===")
        Log.d(TAG, "Name: '$name'")
        Log.d(TAG, "Description: '$description'")
        Log.d(TAG, "Price: $price")
        Log.d(TAG, "Quantity: $quantity")
        Log.d(TAG, "mainCategoryId: $mainCategoryId")
        Log.d(TAG, "subCategoryId: $subCategoryId")
        Log.d(TAG, "brandId: $brandId")
        Log.d(TAG, "sizeId: $sizeId")
        Log.d(TAG, "schoolId: $schoolId")
        Log.d(TAG, "conditionId: $conditionId")
        Log.d(TAG, "locationId: $locationId")
        Log.d(TAG, "provinceId: $provinceId")
        Log.d(TAG, "genderId: $genderId")
        Log.d(TAG, "colorId: $colorId")
        Log.d(TAG, "tagIds: $tagIds")
        Log.d(TAG, "Images to upload: ${_images.value.size}")

        _images.value.forEachIndexed { index, uri ->
            Log.d(TAG, "  Image $index: $uri")
        }

        Log.d(TAG, "Setting UI state to Loading")
        _uiState.value = CreateItemUiState.Loading

        viewModelScope.launch {
            Log.d(TAG, "Launching coroutine for createItemWithImages")

            val result = itemRepository.createItemWithImages(
                context = context,
                name = name,
                description = description,
                mainCategoryId = mainCategoryId,
                subCategoryId = subCategoryId,
                brandId = brandId,
                price = price,
                quantity = quantity,
                itemConditionId = conditionId,
                provinceId = provinceId,
                locationId = locationId,
                genderId = genderId,
                schoolId = schoolId,
                sizeId = sizeId,
                colorId = colorId,
                tagIds = tagIds,
                imageUris = _images.value
            )

            Log.d(TAG, "createItemWithImages completed")
            Log.d(TAG, "Result isSuccess: ${result.isSuccess}")

            result.fold(
                onSuccess = { item ->
                    Log.d(TAG, "✅ Item created successfully!")
                    Log.d(TAG, "Item details:")
                    Log.d(TAG, "  - ID: ${item.id}")
                    Log.d(TAG, "  - Name: ${item.name}")
                    Log.d(TAG, "  - Status: ${item.status}")
                    Log.d(TAG, "  - Price: ${item.price}")
                    Log.d(TAG, "  - Quantity: ${item.quantity}")
                    Log.d(TAG, "  - Images count: ${item.images.size}")
                    Log.d(TAG, "  - Created at: ${item.createdAt}")

                    item.images.forEachIndexed { index, image ->
                        Log.d(TAG, "  - Image $index URL: ${image.url}")
                    }

                    _createdItem.value = ItemState.Success(item)
                    _uiState.value = CreateItemUiState.Success("Item created successfully!")
                    Log.d(TAG, "UI state set to Success")

                    clearImages()
                    Log.d(TAG, "Images cleared")
                    Log.d(TAG, "=== createItem END (SUCCESS) ===")
                },
                onFailure = { error ->
                    Log.e(TAG, "❌ Item creation failed!", error)
                    Log.e(TAG, "Error message: ${error.message}")
                    Log.e(TAG, "Error cause: ${error.cause}")

                    error.stackTrace.forEach { stack ->
                        Log.e(TAG, "  at $stack")
                    }

                    _createdItem.value = ItemState.Error(error.message ?: "Failed to create item")
                    _uiState.value = CreateItemUiState.Error(error.message ?: "Failed to create item")
                    Log.d(TAG, "UI state set to Error")
                    Log.d(TAG, "=== createItem END (FAILURE) ===")
                }
            )
        }
        Log.d(TAG, "createItem function completed (coroutine launched)")
    }

    fun resetState() {
        Log.d(TAG, "resetState: Resetting to Idle")
        _uiState.value = CreateItemUiState.Idle
        _createdItem.value = null
    }
}