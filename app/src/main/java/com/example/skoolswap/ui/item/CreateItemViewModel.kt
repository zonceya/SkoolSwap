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
        viewModelScope.launch {
            _isLoading.value = true

            // STEP 1: Start collecting from DB FIRST (so we catch any updates)
            launch {
                referenceRepository.getMainCategories().collect {
                    _mainCategories.value = it
                    Log.d("CreateItemVM", "Loaded ${it.size} main categories from DB")
                }
            }
            launch {
                referenceRepository.getColors().collect {
                    _colors.value = it
                    Log.d("CreateItemVM", "Loaded ${it.size} colors from DB")
                }
            }
            launch {
                referenceRepository.getSizes().collect {
                    _sizes.value = it
                    Log.d("CreateItemVM", "Loaded ${it.size} sizes from DB")
                }
            }
            launch {
                referenceRepository.getBrands().collect {
                    _brands.value = it
                    Log.d("CreateItemVM", "Loaded ${it.size} brands from DB")
                }
            }
            launch {
                referenceRepository.getConditions().collect {
                    _conditions.value = it
                    Log.d("CreateItemVM", "Loaded ${it.size} conditions from DB")
                }
            }
            launch {
                referenceRepository.getProvinces().collect {
                    _provinces.value = it
                    Log.d("CreateItemVM", "Loaded ${it.size} provinces from DB")
                }
            }
            launch {
                referenceRepository.getSchools().collect {
                    _schools.value = it
                    Log.d("CreateItemVM", "Loaded ${it.size} schools from DB")
                }
            }
            launch {
                referenceRepository.getGenders().collect {
                    _genders.value = it
                    Log.d("CreateItemVM", "Loaded ${it.size} genders from DB")
                }
            }
            launch {
                referenceRepository.getTags().collect {
                    _tags.value = it
                    Log.d("CreateItemVM", "Loaded ${it.size} tags from DB")
                }
            }
            launch {
                referenceRepository.getLocations().collect {
                    _locations.value = it
                    Log.d("CreateItemVM", "Loaded ${it.size} locations from DB")
                }
            }

            // STEP 2: Set up observers
            observeSubCategories()
            observeTowns()

            // STEP 3: Now trigger the refresh (ONCE) - DB flows will update automatically
            try {
                val result = referenceRepository.refreshAllReferenceDataBulk(forceRefresh = false)
                if (result.isSuccess) {
                    Log.d("CreateItemVM", "✅ Reference data refreshed successfully")
                } else {
                    Log.w("CreateItemVM", "⚠️ Refresh failed, using cached: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.w("CreateItemVM", "⚠️ Refresh error: ${e.message}")
            }

            _isLoading.value = false
        }
    }

    // ============ OBSERVE DEPENDENT DATA ============
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeSubCategories() {
        viewModelScope.launch {
            _selectedMainCategoryId
                .filterNotNull()
                .flatMapLatest { mainCategoryId ->
                    referenceRepository.getSubCategories(mainCategoryId)
                }
                .collect { subCats ->
                    Log.d("CreateItemVM", "📦 Received ${subCats.size} subcategories")
                    _subCategories.value = subCats
                }
        }
    }

    private fun observeTowns() {
        viewModelScope.launch {
            _selectedProvinceId
                .filterNotNull()
                .collect { provinceId ->
                    Log.d("CreateItemVM", "🔍 Province selected: $provinceId")
                    referenceRepository.refreshTowns(provinceId)
                    referenceRepository.getTowns(provinceId).collect { townList ->
                        _towns.value = townList
                        Log.d("CreateItemVM", "Loaded ${townList.size} towns for province $provinceId")
                    }
                }
        }
    }

    // ============ SELECTION METHODS ============
    fun onMainCategorySelected(mainCategoryId: Int?) {
        _selectedMainCategoryId.value = mainCategoryId
    }

    fun onProvinceSelected(provinceId: Int?) {
        _selectedProvinceId.value = provinceId
    }

    // ============ IMAGE METHODS ============
    fun addImage(uri: Uri) {
        val currentImages = _images.value.toMutableList()
        if (currentImages.size < 3) {
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

    fun removeImageAt(index: Int) {
        val currentImages = _images.value.toMutableList()
        if (index < currentImages.size) {
            currentImages.removeAt(index)
            _images.value = currentImages
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
        _uiState.value = CreateItemUiState.Loading

        viewModelScope.launch {
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

            result.fold(
                onSuccess = { item ->
                    _createdItem.value = ItemState.Success(item)
                    _uiState.value = CreateItemUiState.Success("Item created successfully!")
                    clearImages()
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