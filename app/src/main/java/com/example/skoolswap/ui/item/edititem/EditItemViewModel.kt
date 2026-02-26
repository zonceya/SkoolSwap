package com.example.skoolswap.ui.item.edititem

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skoolswap.domain.model.EditImage
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
class EditItemViewModel @Inject constructor(
    private val itemRepository: ItemRepositoryInterface,
    private val referenceRepository: ReferenceDataRepositoryInterface,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val TAG = "EditItemViewModel"
    private val itemId: String = savedStateHandle.get<String>("itemId") ?: ""

    // ============ UI STATES ============
    private val _uiState = MutableStateFlow<EditItemUiState>(EditItemUiState.Idle)
    val uiState: StateFlow<EditItemUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _item = MutableStateFlow<Item?>(null)
    val item: StateFlow<Item?> = _item.asStateFlow()

    // ============ UNIFIED IMAGE STATE ============
    private val _images = MutableStateFlow<List<EditImage>>(emptyList())
    val images: StateFlow<List<EditImage>> = _images.asStateFlow()

    // Track which existing images to delete
    private val _imagesToDelete = MutableStateFlow<Set<Long>>(emptySet())
    val imagesToDelete: StateFlow<Set<Long>> = _imagesToDelete.asStateFlow()

    // ============ DELETE CONFIRMATION ============
    private val _showDeleteConfirmation = MutableStateFlow(false)
    val showDeleteConfirmation: StateFlow<Boolean> = _showDeleteConfirmation.asStateFlow()

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

    // ============ SELECTED VALUES ============
    private val _selectedMainCategoryId = MutableStateFlow<Int?>(null)
    val selectedMainCategoryId: StateFlow<Int?> = _selectedMainCategoryId.asStateFlow()

    private val _selectedProvinceId = MutableStateFlow<Int?>(null)
    val selectedProvinceId: StateFlow<Int?> = _selectedProvinceId.asStateFlow()

    // ============ INIT ============
    init {
        viewModelScope.launch {
            // Use the existing bulk method to load ALL reference data at once
            val result = referenceRepository.refreshAllReferenceDataBulk()

            if (result.isSuccess) {
                Log.d(TAG, "✅ Successfully loaded all reference data in bulk")

                // Now collect each flow to populate the StateFlows
                launch { referenceRepository.getMainCategories().collect { _mainCategories.value = it } }

                // ✅ FIXED: Use getSubCategories() for subcategories
                launch { referenceRepository.getSubCategories().collect { _subCategories.value = it } }

                launch { referenceRepository.getConditions().collect { _conditions.value = it } }
                launch { referenceRepository.getGenders().collect { _genders.value = it } }
                launch { referenceRepository.getSchools().collect { _schools.value = it } }
                launch { referenceRepository.getSizes().collect { _sizes.value = it } }
                launch { referenceRepository.getBrands().collect { _brands.value = it } }
                launch { referenceRepository.getColors().collect { _colors.value = it } }
                launch { referenceRepository.getProvinces().collect { _provinces.value = it } }
            } else {
                Log.e(TAG, "❌ Failed to load reference data")
            }

            // Set up observers
            observeSubCategories()
            observeTowns()

            // Load the item
            if (itemId.isNotEmpty()) {
                loadItem(itemId)
            }
        }
    }

    fun loadItem(itemId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            Log.d(TAG, "🔄 Loading item for edit: $itemId")

            // CHANGE THIS LINE - use the new method
            val result = itemRepository.getShopItemForEdit(itemId)

            result.fold(
                onSuccess = { item ->
                    Log.d(TAG, "✅ Item loaded successfully with ${item.images.size} images")
                    _item.value = item
                    // Convert item images to EditImage.Existing
                    val existingImages = item.images.map { image ->
                        EditImage.Existing(image.id, image.url)
                    }
                    setExistingImages(existingImages)
                },
                onFailure = { error ->
                    Log.e(TAG, "❌ Failed to load item", error)
                    _uiState.value = EditItemUiState.Error("Failed to load item: ${error.message}")
                }
            )
            _isLoading.value = false
        }
    }

    private fun loadReferenceData() {
        viewModelScope.launch {
            // Load main categories
            referenceRepository.getMainCategories().collect {
                _mainCategories.value = it
            }

            // Load colors
            referenceRepository.getColors().collect {
                _colors.value = it
            }

            // Load sizes
            referenceRepository.getSizes().collect {
                _sizes.value = it
            }

            // Load brands
            referenceRepository.getBrands().collect {
                _brands.value = it
            }

            // Load conditions
            referenceRepository.getConditions().collect {
                _conditions.value = it
            }

            // Load provinces
            referenceRepository.getProvinces().collect {
                _provinces.value = it
            }

            // Load schools
            referenceRepository.getSchools().collect {
                _schools.value = it
            }

            // Load genders
            referenceRepository.getGenders().collect {
                _genders.value = it
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeSubCategories() {
        viewModelScope.launch {
            _selectedMainCategoryId
                .filterNotNull()
                .flatMapLatest { mainCategoryId ->
                    referenceRepository.refreshSubCategories(mainCategoryId)
                    referenceRepository.getSubCategories(mainCategoryId)
                }
                .collect { subCats ->
                    _subCategories.value = subCats
                }
        }
    }

    private fun observeTowns() {
        viewModelScope.launch {
            _selectedProvinceId
                .filterNotNull()
                .collect { provinceId ->
                    referenceRepository.refreshTowns(provinceId)
                    referenceRepository.getTowns(provinceId).collect { townList ->
                        _towns.value = townList
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

    // ============ UNIFIED IMAGE METHODS ============
    fun setExistingImages(existingImages: List<EditImage>) {
        Log.d(TAG, "setExistingImages called with ${existingImages.size} images")
        existingImages.forEachIndexed { index, image ->
            if (image is EditImage.Existing) {
                Log.d(TAG, "Image $index: ID=${image.id}, URL=${image.url}")
            }
        }

        val imageList = existingImages.toMutableList()
        while (imageList.size < 3) {
            imageList.add(EditImage.Empty)
        }

        _images.value = imageList
        Log.d(TAG, "Final image list size: ${imageList.size}")
    }

    fun addImage(uri: Uri, position: Int) {
        val currentList = _images.value.toMutableList()
        if (position in 0 until currentList.size) {
            // Replace whatever is at that position with a new image
            currentList[position] = EditImage.New(uri, isUploading = false)
            _images.value = currentList
        }
    }

    fun replaceImage(uri: Uri, position: Int) {
        val currentList = _images.value.toMutableList()
        if (position in 0 until currentList.size) {
            val currentImage = currentList[position]

            // If it was an existing image, mark it for deletion
            if (currentImage is EditImage.Existing && !currentImage.isMarkedForDeletion) {
                _imagesToDelete.value += currentImage.id
            }

            // Replace with new image
            currentList[position] = EditImage.New(uri, isUploading = false)
            _images.value = currentList
        }
    }

    fun removeImage(position: Int) {
        val currentList = _images.value.toMutableList()
        if (position in 0 until currentList.size) {
            val image = currentList[position]

            // If it's an existing image, mark for deletion
            if (image is EditImage.Existing && !image.isMarkedForDeletion) {
                _imagesToDelete.value += image.id
            }

            // Replace with empty slot
            currentList[position] = EditImage.Empty
            _images.value = currentList
        }
    }

    fun getImagesForUpload(): List<Uri> {
        return _images.value.filterIsInstance<EditImage.New>()
            .map { it.uri }
    }

    fun getDeletionIds(): List<Long> {
        return _imagesToDelete.value.toList()
    }

    // ============ UPDATE ITEM ============
    fun updateItem(
        context: Context,
        itemId: String,
        name: String?,
        description: String?,
        price: Double?,
        quantity: Int?,
        mainCategoryId: Int?,
        subCategoryId: Int?,
        brandId: Int?,
        sizeId: Int?,
        schoolId: Int?,
        conditionId: Int?,
        locationId: Int?,
        provinceId: Int?,
        genderId: Int?,
        colorId: Int?,
        addImageUris: List<Uri>,
        removeImageIds: List<Long>
    ) {
        _uiState.value = EditItemUiState.Loading

        viewModelScope.launch {
            val result = itemRepository.updateItemWithImages(
                context = context,
                itemId = itemId,
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
                addImageUris = addImageUris,
                removeImageIds = removeImageIds,
                replaceAllImages = null
            )

            result.fold(
                onSuccess = { updatedItem ->
                    _item.value = updatedItem
                    _uiState.value = EditItemUiState.Success("Item updated successfully!")

                    // Clear deletion set after successful update
                    _imagesToDelete.value = emptySet()
                },
                onFailure = { error ->
                    _uiState.value = EditItemUiState.Error(error.message ?: "Failed to update item")
                }
            )
        }
    }

    // ============ DELETE ITEM ============
    fun showDeleteConfirmation() {
        _showDeleteConfirmation.value = true
    }

    fun deleteConfirmationShown() {
        _showDeleteConfirmation.value = false
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            _uiState.value = EditItemUiState.Loading

            val result = itemRepository.deleteItem(itemId)

            result.fold(
                onSuccess = {
                    _uiState.value = EditItemUiState.Success("Item deleted successfully!")
                },
                onFailure = { error ->
                    _uiState.value = EditItemUiState.Error(error.message ?: "Failed to delete item")
                }
            )
        }
    }
}